package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.RespuestaPerfil;
import com.bankbatch.dto.RespuestaUsuario;
import com.bankbatch.dto.SolicitudAltaUsuario;
import com.bankbatch.dto.SolicitudModificacionUsuario;
import com.bankbatch.entidad.AccionAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.NombreRol;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.excepcion.ConflictoNegocioExcepcion;
import com.bankbatch.excepcion.CredencialesInvalidasExcepcion;
import com.bankbatch.excepcion.RecursoNoEncontradoExcepcion;
import com.bankbatch.excepcion.SolicitudInvalidaExcepcion;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ServicioUsuarios {

    private static final int TAMANO_MAXIMO_DE_PAGINA = 100;
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRoles repositorioRoles;
    private final PasswordEncoder codificador;
    private final ServicioAuditoria servicioAuditoria;

    public ServicioUsuarios(RepositorioUsuarios repositorioUsuarios, RepositorioRoles repositorioRoles,
                            PasswordEncoder codificador, ServicioAuditoria servicioAuditoria) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioRoles = repositorioRoles;
        this.codificador = codificador;
        this.servicioAuditoria = servicioAuditoria;
    }

    @Transactional(readOnly = true)
    public RespuestaPerfil obtenerPerfil(String nombreUsuario) {
        Usuario usuario = repositorioUsuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(CredencialesInvalidasExcepcion::new);
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new RespuestaPerfil(
                usuario.getNombreUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                roles,
                usuario.getUltimoAcceso());
    }

    @Transactional
    public RespuestaUsuario crear(SolicitudAltaUsuario solicitud) {
        if (ValidadorContrasena.excedeElLimite(solicitud.contrasena())) {
            throw new SolicitudInvalidaExcepcion("La contraseña supera los 72 bytes que admite el cifrado");
        }

        String nombreUsuario = solicitud.nombreUsuario().strip().toLowerCase(Locale.ROOT);
        String correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);
        if (repositorioUsuarios.existsByNombreUsuario(nombreUsuario)) {
            throw new ConflictoNegocioExcepcion("Ya existe un usuario con el nombre " + nombreUsuario);
        }
        if (repositorioUsuarios.existsByCorreo(correo)) {
            throw new ConflictoNegocioExcepcion("Ya existe un usuario con el correo " + correo);
        }

        Usuario usuario = new Usuario(solicitud.nombre().strip(), solicitud.apellido().strip(), correo,
                nombreUsuario, codificador.encode(solicitud.contrasena()));
        resolverRoles(solicitud.roles()).forEach(usuario::agregarRol);
        usuario = repositorioUsuarios.saveAndFlush(usuario);

        RespuestaUsuario respuesta = RespuestaUsuario.desde(usuario);
        servicioAuditoria.registrar(EntidadAuditada.USUARIO, usuario.getId(), AccionAuditoria.CREACION, null, respuesta);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaUsuario> buscar(String texto, EstadoUsuario estado, int pagina, int tamano) {
        String patron = (texto == null || texto.isBlank()) ? "%" : "%" + texto.strip().toLowerCase(Locale.ROOT) + "%";
        Collection<EstadoUsuario> estados = estado == null ? List.of(EstadoUsuario.values()) : List.of(estado);
        Pageable paginacion = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamano, 1), TAMANO_MAXIMO_DE_PAGINA),
                Sort.by("apellido", "nombre"));

        return RespuestaPagina.desde(repositorioUsuarios.buscar(patron, estados, paginacion)
                .map(RespuestaUsuario::desde));
    }

    @Transactional(readOnly = true)
    public RespuestaUsuario obtener(Long id) {
        return RespuestaUsuario.desde(obtenerEntidad(id));
    }

    @Transactional
    public RespuestaUsuario modificar(Long id, SolicitudModificacionUsuario solicitud) {
        Usuario usuario = obtenerEntidad(id);

        String correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);
        if (!correo.equals(usuario.getCorreo()) && repositorioUsuarios.existsByCorreo(correo)) {
            throw new ConflictoNegocioExcepcion("Ya existe un usuario con el correo " + correo);
        }

        List<Rol> nuevosRoles = resolverRoles(solicitud.roles());
        boolean seQuedaSinRolAdministrador = nuevosRoles.stream()
                .noneMatch(rol -> rol.getNombre().equals(ROL_ADMINISTRADOR));
        if (usuario.tieneRol(ROL_ADMINISTRADOR) && seQuedaSinRolAdministrador) {
            asegurarOtroAdministradorActivo(usuario);
        }

        RespuestaUsuario antes = RespuestaUsuario.desde(usuario);
        usuario.actualizarDatos(solicitud.nombre(), solicitud.apellido(), correo);
        usuario.reemplazarRoles(nuevosRoles);
        repositorioUsuarios.saveAndFlush(usuario);

        RespuestaUsuario despues = RespuestaUsuario.desde(usuario);
        servicioAuditoria.registrar(EntidadAuditada.USUARIO, id, AccionAuditoria.MODIFICACION, antes, despues);
        return despues;
    }

    @Transactional
    public RespuestaUsuario cambiarEstado(Long id, EstadoUsuario nuevoEstado) {
        Usuario usuario = obtenerEntidad(id);
        if (usuario.getEstado() == nuevoEstado) {
            throw new ConflictoNegocioExcepcion("El usuario ya está en estado " + nuevoEstado);
        }
        if (nuevoEstado != EstadoUsuario.ACTIVO && usuario.tieneRol(ROL_ADMINISTRADOR)) {
            asegurarOtroAdministradorActivo(usuario);
        }

        RespuestaUsuario antes = RespuestaUsuario.desde(usuario);
        usuario.cambiarEstado(nuevoEstado);
        repositorioUsuarios.saveAndFlush(usuario);

        RespuestaUsuario despues = RespuestaUsuario.desde(usuario);
        servicioAuditoria.registrar(EntidadAuditada.USUARIO, id, AccionAuditoria.MODIFICACION, antes, despues);
        return despues;
    }

    private void asegurarOtroAdministradorActivo(Usuario usuario) {
        if (usuario.estaActivo() && repositorioUsuarios.contarAdministradoresActivos() <= 1) {
            throw new ConflictoNegocioExcepcion("No se puede dejar al sistema sin un administrador activo");
        }
    }

    private List<Rol> resolverRoles(Set<NombreRol> nombres) {
        return nombres.stream()
                .map(nombre -> repositorioRoles.findByNombre(nombre.name())
                        .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No existe el rol " + nombre)))
                .toList();
    }

    private Usuario obtenerEntidad(Long id) {
        return repositorioUsuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No existe el usuario con id " + id));
    }
}
