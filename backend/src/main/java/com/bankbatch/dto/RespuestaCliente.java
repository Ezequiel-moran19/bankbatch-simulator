package com.bankbatch.dto;

import com.bankbatch.entidad.Cliente;
import com.bankbatch.entidad.EstadoCliente;

import java.time.Instant;

public record RespuestaCliente(
    Long id,
    String nombre,
    String apellido,
    String documento,
    String correo,
    String telefono,
    EstadoCliente estado,
    Instant fechaAlta) {

    public static RespuestaCliente desde(Cliente cliente) {
        return new RespuestaCliente(
            cliente.getId(),
            cliente.getNombre(),
            cliente.getApellido(),
            cliente.getDocumento(),
            cliente.getCorreo(),
            cliente.getTelefono(),
            cliente.getEstado(),
            cliente.getFechaAlta());
    }
}
