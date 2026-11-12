package com.bankbatch.servicio;

import com.bankbatch.dto.SolicitudMovimiento;
import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.Movimiento;
import com.bankbatch.entidad.TipoCuenta;
import com.bankbatch.entidad.TipoMovimiento;
import com.bankbatch.repositorio.RepositorioCuentas;
import com.bankbatch.repositorio.RepositorioMovimientos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioMovimientosTests {

    private static final String NUMERO = "CA-0000000001";

    @Mock
    private RepositorioCuentas repositorioCuentas;

    @Mock
    private RepositorioMovimientos repositorioMovimientos;

    @InjectMocks
    private ServicioMovimientos servicio;

    private Cuenta cuentaConSaldo(String saldo) {
        Cuenta cuenta = new Cuenta(1L, TipoCuenta.CAJA_AHORRO, NUMERO);
        ReflectionTestUtils.setField(cuenta, "id", 10L);
        BigDecimal monto = new BigDecimal(saldo);
        if (monto.signum() > 0) {
            cuenta.acreditar(monto);
        }
        return cuenta;
    }

    private SolicitudMovimiento solicitud(TipoMovimiento tipo, String importe) {
        return new SolicitudMovimiento(NUMERO, 5L, "REF-1", tipo, new BigDecimal(importe), "Prueba");
    }

    @Test
    void acreditaYRegistraElMovimiento() {
        Cuenta cuenta = cuentaConSaldo("100.00");
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
        when(repositorioMovimientos.saveAndFlush(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.CREDITO, "50.00"));

        assertThat(resultado.aplicado()).isTrue();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        assertThat(resultado.movimiento().getSaldoAnterior()).isEqualByComparingTo("100.00");
        assertThat(resultado.movimiento().getSaldoPosterior()).isEqualByComparingTo("150.00");
        assertThat(resultado.movimiento().getTipoMovimiento()).isEqualTo(TipoMovimiento.CREDITO);
        verify(repositorioCuentas).saveAndFlush(cuenta);
    }

    @Test
    void debitaYRegistraElMovimiento() {
        Cuenta cuenta = cuentaConSaldo("100.00");
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
        when(repositorioMovimientos.saveAndFlush(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.DEBITO, "30.00"));

        assertThat(resultado.aplicado()).isTrue();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(resultado.movimiento().getSaldoPosterior()).isEqualByComparingTo("70.00");
    }

    @Test
    void permiteDebitarTodoElSaldo() {
        Cuenta cuenta = cuentaConSaldo("100.00");
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
        when(repositorioMovimientos.saveAndFlush(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.DEBITO, "100.00"));

        assertThat(resultado.aplicado()).isTrue();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    void rechazaDebitoConSaldoInsuficienteSinTocarLaCuenta() {
        Cuenta cuenta = cuentaConSaldo("20.00");
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.DEBITO, "20.01"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.SALDO_INSUFICIENTE);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("20.00");
        verify(repositorioCuentas, never()).saveAndFlush(any());
        verify(repositorioMovimientos, never()).saveAndFlush(any());
    }

    @Test
    void rechazaCuentaInexistente() {
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.empty());

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.CREDITO, "10.00"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.CUENTA_INEXISTENTE);
    }

    @Test
    void rechazaCuentaBloqueada() {
        Cuenta cuenta = cuentaConSaldo("100.00");
        cuenta.bloquear();
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.CREDITO, "10.00"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.CUENTA_NO_ACTIVA);
        verify(repositorioMovimientos, never()).saveAndFlush(any());
    }

    @Test
    void rechazaOperacionDuplicada() {
        Cuenta cuenta = cuentaConSaldo("100.00");
        when(repositorioCuentas.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
        when(repositorioMovimientos.existsByIdCuentaAndReferenciaAndTipoMovimiento(
            10L, "REF-1", TipoMovimiento.CREDITO)).thenReturn(true);

        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.CREDITO, "10.00"));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.OPERACION_DUPLICADA);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("100.00");
        verify(repositorioMovimientos, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5.00", "10.005"})
    void rechazaImportesInvalidos(String importe) {
        ResultadoMovimiento resultado = servicio.aplicar(solicitud(TipoMovimiento.CREDITO, importe));

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.IMPORTE_INVALIDO);
        verify(repositorioCuentas, never()).findByNumeroCuenta(any());
    }

    @Test
    void rechazaReferenciaEnBlanco() {
        SolicitudMovimiento solicitud = new SolicitudMovimiento(
            NUMERO, 5L, "  ", TipoMovimiento.CREDITO, new BigDecimal("10.00"), "Prueba");

        ResultadoMovimiento resultado = servicio.aplicar(solicitud);

        assertThat(resultado.motivo()).isEqualTo(MotivoRechazoMovimiento.CAMPOS_INVALIDOS);
    }
}
