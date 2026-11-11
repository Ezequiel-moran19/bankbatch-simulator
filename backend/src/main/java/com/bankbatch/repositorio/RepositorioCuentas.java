package com.bankbatch.repositorio;

import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.EstadoCuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface RepositorioCuentas extends JpaRepository<Cuenta, Long> {

    Page<Cuenta> findByEstadoIn(Collection<EstadoCuenta> estados, Pageable paginacion);

    Page<Cuenta> findByIdClienteAndEstadoIn(Long idCliente, Collection<EstadoCuenta> estados, Pageable paginacion);

    @Query(value = "select nextval('seq_numero_cuenta')", nativeQuery = true)
    long siguienteNumeroDeCuenta();
}
