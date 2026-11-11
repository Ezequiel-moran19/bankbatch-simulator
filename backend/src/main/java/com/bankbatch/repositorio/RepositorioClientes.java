package com.bankbatch.repositorio;

import com.bankbatch.entidad.Cliente;
import com.bankbatch.entidad.EstadoCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface RepositorioClientes extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    @Query("select c from Cliente c where c.estado in :estados "
        + "and (lower(c.nombre) like :texto or lower(c.apellido) like :texto or c.documento like :texto)")
    Page<Cliente> buscar(@Param("texto") String texto,
                         @Param("estados") Collection<EstadoCliente> estados,
                         Pageable paginacion);

    @Query(value = "select count(*) from cuentas where id_cliente = :idCliente and estado <> 'CERRADA'",
        nativeQuery = true)
    long contarCuentasNoCerradas(@Param("idCliente") Long idCliente);
}
