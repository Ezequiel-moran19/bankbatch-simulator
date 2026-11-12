package com.bankbatch.servicio;

import com.bankbatch.apoyo.AyudanteAutenticacion;
import com.bankbatch.dto.SolicitudMovimiento;
import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.TipoCuenta;
import com.bankbatch.entidad.TipoMovimiento;
import com.bankbatch.repositorio.RepositorioCuentas;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ServicioMovimientosIntegracionTests {

    private static final String NUMERO = "CA-9900000001";

    private static final String INSERCION_MOVIMIENTO = """
            INSERT INTO movimientos (id_cuenta, id_ejecucion, referencia, tipo_movimiento, importe,
                                     saldo_anterior, saldo_posterior, descripcion)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Prueba')
            """;

    @Autowired
    private ServicioMovimientos servicioMovimientos;

    @Autowired
    private RepositorioCuentas repositorioCuentas;

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @Autowired
    private com.bankbatch.servicio.ServicioJwt servicioJwt;

    @Autowired
    private JdbcTemplate jdbc;

    private Cuenta cuenta;
    private Long idEjecucion;

    @BeforeEach
    void preparar() {
        new AyudanteAutenticacion(repositorioUsuarios, repositorioRoles, servicioJwt).tokenAdministrador();
        Long idUsuario = repositorioUsuarios.buscarIdPorNombreUsuario("administrador_prueba").orElseThrow();

        idEjecucion = jdbc.queryForObject("""
                INSERT INTO ejecuciones_proceso (id_proceso, id_usuario, nombre_archivo, hash_archivo, estado, fecha_fin)
                SELECT id_proceso, ?, 'prueba.csv', repeat('a', 64), 'COMPLETADA', CURRENT_TIMESTAMP
                FROM procesos WHERE codigo = 'ACREDITACION_HABERES'
                RETURNING id_ejecucion
                """, Long.class, idUsuario);

        Long idCliente = jdbc.queryForObject("""
                INSERT INTO clientes (nombre, apellido, documento, correo)
                VALUES ('Ana', 'Gomez', '92000001', 'ana@correo.test')
                RETURNING id_cliente
                """, Long.class);

        Cuenta nueva = new Cuenta(idCliente, TipoCuenta.CAJA_AHORRO, NUMERO);
        nueva.acreditar(new BigDecimal("100.00"));
        cuenta = repositorioCuentas.saveAndFlush(nueva);
    }

    private SolicitudMovimiento solicitud(String referencia, TipoMovimiento tipo, String importe) {
        return new SolicitudMovimiento(NUMERO, idEjecucion, referencia, tipo,
            new BigDecimal(importe), "Prueba de integración");
    }

    private BigDecimal saldoEnBase() {
        return jdbc.queryForObject("SELECT saldo FROM cuentas WHERE id_cuenta = ?", BigDecimal.class, cuenta.getId());
    }

    private int movimientosEnBase() {
        return jdbc.queryForObject("SELECT count(*) FROM movimientos WHERE id_cuenta = ?",
            Integer.class, cuenta.getId());
    }

    @Test
    void unCreditoActualizaElSaldoYPersisteElMovimiento() {
        ResultadoMovimiento resultado = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "50.00"));

        assertThat(resultado.aplicado()).isTrue();
        assertThat(saldoEnBase()).isEqualByComparingTo("150.00");
        assertThat(movimientosEnBase()).isEqualTo(1);
        assertThat(resultado.movimiento().getId()).isNotNull();
    }

    @Test
    void unDebitoActualizaElSaldoYPersisteElMovimiento() {
        ResultadoMovimiento resultado = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.DEBITO, "30.00"));

        assertThat(resultado.aplicado()).isTrue();
        assertThat(saldoEnBase()).isEqualByComparingTo("70.00");
        assertThat(resultado.movimiento().getSaldoAnterior()).isEqualByComparingTo("100.00");
        assertThat(resultado.movimiento().getSaldoPosterior()).isEqualByComparingTo("70.00");
    }

    @Test
    void unDebitoConSaldoInsuficienteSeRechazaSinCambiosEnBase() {
        ResultadoMovimiento resultado = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.DEBITO, "100.01"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.SALDO_INSUFICIENTE);
        assertThat(saldoEnBase()).isEqualByComparingTo("100.00");
        assertThat(movimientosEnBase()).isZero();
    }

    @Test
    void laMismaOperacionAplicadaDosVecesSoloImpactaUnaVez() {
        servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "50.00"));

        ResultadoMovimiento segundo = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "50.00"));

        assertThat(segundo.motivo()).isEqualTo(MotivoRechazoMovimiento.OPERACION_DUPLICADA);
        assertThat(saldoEnBase()).isEqualByComparingTo("150.00");
        assertThat(movimientosEnBase()).isEqualTo(1);
    }

    @Test
    void laMismaReferenciaConOtroTipoSiSeAcepta() {
        servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "50.00"));

        ResultadoMovimiento debito = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.DEBITO, "20.00"));

        assertThat(debito.aplicado()).isTrue();
        assertThat(saldoEnBase()).isEqualByComparingTo("130.00");
        assertThat(movimientosEnBase()).isEqualTo(2);
    }

    @Test
    void unaCuentaBloqueadaNoAdmiteMovimientos() {
        cuenta.bloquear();
        repositorioCuentas.saveAndFlush(cuenta);

        ResultadoMovimiento resultado = servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "10.00"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.CUENTA_NO_ACTIVA);
        assertThat(movimientosEnBase()).isZero();
    }

    @Test
    void laBaseRechazaUnMovimientoDuplicadoAunSiSeSaltaLaValidacionDelServicio() {
        servicioMovimientos.aplicar(solicitud("REF-1", TipoMovimiento.CREDITO, "50.00"));

        assertThatThrownBy(() -> jdbc.update(INSERCION_MOVIMIENTO,
            cuenta.getId(), idEjecucion, "REF-1", "CREDITO",
            new BigDecimal("10.00"), new BigDecimal("150.00"), new BigDecimal("160.00")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_movimientos_cuenta_referencia_tipo");
    }

    @Test
    void laBaseRechazaUnMovimientoConSaldosIncoherentes() {
        assertThatThrownBy(() -> jdbc.update(INSERCION_MOVIMIENTO,
            cuenta.getId(), idEjecucion, "REF-X", "CREDITO",
            new BigDecimal("10.00"), new BigDecimal("100.00"), new BigDecimal("500.00")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_movimientos_coherencia");
    }

    @Test
    void laBaseRechazaUnSaldoNegativoEnLaCuenta() {
        assertThatThrownBy(() -> jdbc.update("UPDATE cuentas SET saldo = -1 WHERE id_cuenta = ?", cuenta.getId()))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ck_cuentas_saldo");
    }

    @Test
    void unConflictoDeVersionImpideSobrescribirElSaldo() {
        Cuenta cargada = repositorioCuentas.findById(cuenta.getId()).orElseThrow();
        jdbc.update("UPDATE cuentas SET saldo = saldo + 50, version = version + 1 WHERE id_cuenta = ?",
            cuenta.getId());

        cargada.acreditar(new BigDecimal("10.00"));

        assertThatThrownBy(() -> repositorioCuentas.saveAndFlush(cargada))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
