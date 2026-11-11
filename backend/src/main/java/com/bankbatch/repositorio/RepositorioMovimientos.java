package com.bankbatch.repositorio;

import com.bankbatch.entidad.Movimiento;
import com.bankbatch.entidad.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RepositorioMovimientos
    extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    boolean existsByIdCuentaAndReferenciaAndTipoMovimiento(Long idCuenta, String referencia,
                                                           TipoMovimiento tipoMovimiento);
}
