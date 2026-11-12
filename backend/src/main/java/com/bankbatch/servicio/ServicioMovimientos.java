package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaMovimiento;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.SolicitudMovimiento;
import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.EstadoCuenta;
import com.bankbatch.entidad.Movimiento;
import com.bankbatch.entidad.TipoMovimiento;
import com.bankbatch.excepcion.SolicitudInvalidaExcepcion;
import com.bankbatch.repositorio.RepositorioCuentas;
import com.bankbatch.repositorio.RepositorioMovimientos;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ServicioMovimientos {

    private static final int LONGITUD_MAXIMA_REFERENCIA = 100;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 250;
    private static final int DIGITOS_ENTEROS_MAXIMOS = 16;
    private static final int TAMANO_MAXIMO_DE_PAGINA = 100;
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final RepositorioCuentas repositorioCuentas;
    private final RepositorioMovimientos repositorioMovimientos;

    public ServicioMovimientos(RepositorioCuentas repositorioCuentas,
                               RepositorioMovimientos repositorioMovimientos) {
        this.repositorioCuentas = repositorioCuentas;
        this.repositorioMovimientos = repositorioMovimientos;
    }

    /**
     * Aplica saldo y crea el movimiento en la misma transacción. Los rechazos funcionales se devuelven
     * como resultado. Un conflicto de @Version o una violación de la UNIQUE por carrera propagan
     * excepción y revierten la transacción: el llamador decide si reintenta.
     */
    @Transactional
    public ResultadoMovimiento aplicar(SolicitudMovimiento solicitud) {
        if (!camposValidos(solicitud)) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.CAMPOS_INVALIDOS);
        }
        if (!importeValido(solicitud.importe())) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.IMPORTE_INVALIDO);
        }

        Optional<Cuenta> encontrada = repositorioCuentas.findByNumeroCuenta(solicitud.numeroCuenta());
        if (encontrada.isEmpty()) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.CUENTA_INEXISTENTE);
        }
        Cuenta cuenta = encontrada.get();
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.CUENTA_NO_ACTIVA);
        }
        if (repositorioMovimientos.existsByIdCuentaAndReferenciaAndTipoMovimiento(
            cuenta.getId(), solicitud.referencia(), solicitud.tipoMovimiento())) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.OPERACION_DUPLICADA);
        }

        BigDecimal importe = solicitud.importe().setScale(2);
        if (solicitud.tipoMovimiento() == TipoMovimiento.DEBITO && cuenta.getSaldo().compareTo(importe) < 0) {
            return ResultadoMovimiento.rechazado(MotivoRechazoMovimiento.SALDO_INSUFICIENTE);
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        if (solicitud.tipoMovimiento() == TipoMovimiento.CREDITO) {
            cuenta.acreditar(importe);
        } else {
            cuenta.debitar(importe);
        }
        repositorioCuentas.saveAndFlush(cuenta);

        Movimiento movimiento = repositorioMovimientos.saveAndFlush(new Movimiento(
            cuenta.getId(), solicitud.idEjecucion(), solicitud.referencia(), solicitud.tipoMovimiento(),
            importe, saldoAnterior, cuenta.getSaldo(), solicitud.descripcion()));
        return ResultadoMovimiento.exitoso(movimiento);
    }

    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaMovimiento> buscar(Long idCuenta, Long idEjecucion, TipoMovimiento tipo,
                                                       LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new SolicitudInvalidaExcepcion("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        Pageable paginacion = PageRequest.of(
            Math.max(pagina, 0),
            Math.min(Math.max(tamano, 1), TAMANO_MAXIMO_DE_PAGINA),
            Sort.by(Sort.Order.desc("fechaMovimiento"), Sort.Order.desc("id")));

        Specification<Movimiento> especificacion = (raiz, consulta, constructor) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (idCuenta != null) {
                predicados.add(constructor.equal(raiz.get("idCuenta"), idCuenta));
            }
            if (idEjecucion != null) {
                predicados.add(constructor.equal(raiz.get("idEjecucion"), idEjecucion));
            }
            if (tipo != null) {
                predicados.add(constructor.equal(raiz.get("tipoMovimiento"), tipo));
            }
            if (desde != null) {
                Instant inicio = desde.atStartOfDay(ZONA).toInstant();
                predicados.add(constructor.greaterThanOrEqualTo(raiz.<Instant>get("fechaMovimiento"), inicio));
            }
            if (hasta != null) {
                Instant fin = hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
                predicados.add(constructor.lessThan(raiz.<Instant>get("fechaMovimiento"), fin));
            }
            return constructor.and(predicados.toArray(Predicate[]::new));
        };

        Page<Movimiento> resultado = repositorioMovimientos.findAll(especificacion, paginacion);
        return RespuestaPagina.desde(resultado.map(RespuestaMovimiento::desde));
    }

    private boolean camposValidos(SolicitudMovimiento solicitud) {
        return solicitud != null
            && solicitud.numeroCuenta() != null && !solicitud.numeroCuenta().isBlank()
            && solicitud.idEjecucion() != null
            && solicitud.tipoMovimiento() != null
            && solicitud.referencia() != null && !solicitud.referencia().isBlank()
            && solicitud.referencia().length() <= LONGITUD_MAXIMA_REFERENCIA
            && solicitud.descripcion() != null && !solicitud.descripcion().isBlank()
            && solicitud.descripcion().length() <= LONGITUD_MAXIMA_DESCRIPCION;
    }

    private boolean importeValido(BigDecimal importe) {
        if (importe == null || importe.signum() <= 0) {
            return false;
        }
        BigDecimal reducido = importe.stripTrailingZeros();
        return reducido.scale() <= 2 && reducido.precision() - reducido.scale() <= DIGITOS_ENTEROS_MAXIMOS;
    }
}
