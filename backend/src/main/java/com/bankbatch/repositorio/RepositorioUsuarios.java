package com.bankbatch.repositorio;

import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface RepositorioUsuarios extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    @Query("select u.id from Usuario u where u.nombreUsuario = :nombreUsuario")
    Optional<Long> buscarIdPorNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    @Query("select u from Usuario u where u.estado in :estados "
            + "and (lower(u.nombre) like :texto or lower(u.apellido) like :texto "
            + "or u.nombreUsuario like :texto or u.correo like :texto)")
    Page<Usuario> buscar(@Param("texto") String texto,
                         @Param("estados") Collection<EstadoUsuario> estados,
                         Pageable paginacion);

    @Query("select count(distinct u) from Usuario u join u.roles r "
            + "where r.nombre = 'ADMINISTRADOR' and u.estado = com.bankbatch.entidad.EstadoUsuario.ACTIVO")
    long contarAdministradoresActivos();
}
