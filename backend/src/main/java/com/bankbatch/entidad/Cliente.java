package com.bankbatch.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "documento", nullable = false, length = 20, updatable = false)
    private String documento;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCliente estado;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private Instant fechaAlta;

    protected Cliente() {
    }

    public Cliente(String nombre, String apellido, String documento, String correo, String telefono) {
        this.nombre = nombre.strip();
        this.apellido = apellido.strip();
        this.documento = documento.strip();
        this.correo = normalizarCorreo(correo);
        this.telefono = normalizarTelefono(telefono);
        this.estado = EstadoCliente.ACTIVO;
        this.fechaAlta = Instant.now();
    }

    public boolean estaActivo() {
        return estado == EstadoCliente.ACTIVO;
    }

    public void actualizarDatos(String nombre, String apellido, String correo, String telefono) {
        this.nombre = nombre.strip();
        this.apellido = apellido.strip();
        this.correo = normalizarCorreo(correo);
        this.telefono = normalizarTelefono(telefono);
    }

    public void darDeBaja() {
        this.estado = EstadoCliente.INACTIVO;
    }

    private static String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizarTelefono(String telefono) {
        return (telefono == null || telefono.isBlank()) ? null : telefono.strip();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public EstadoCliente getEstado() {
        return estado;
    }

    public Instant getFechaAlta() {
        return fechaAlta;
    }
}
