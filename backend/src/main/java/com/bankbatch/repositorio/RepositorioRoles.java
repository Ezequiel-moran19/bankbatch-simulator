package com.bankbatch.repositorio;

import com.bankbatch.entidad.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioRoles extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
