package com.bankbatch.repositorio;

import com.bankbatch.entidad.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RepositorioUsuarios extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    @Query("select u.id from Usuario u where u.nombreUsuario = :nombreUsuario")
    Optional<Long> buscarIdPorNombreUsuario(@Param("nombreUsuario") String nombreUsuario);
}
