package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.emun.TipoDocumento;
import java.time.LocalDate;

// Tabla USUARIO. Rol_usuario decide la subclase: Administrador, Arrendador o Arrendatario.
public abstract class Usuario {

    private TipoDocumento tipoDocumento;
    private Integer numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Integer telefono;
    private String correo;
    private LocalDate fechaRegistro;
    private EstadoUsuario estado;
    private String contrasena;   // columna Contraseña (hash)
    private final Rol rol;       // columna Rol_usuario (la fija cada subclase)

    protected Usuario(Rol rol) {
        this.rol = rol;
    }

    // Crea la subclase que corresponde al rol.
    public static Usuario segunRol(Rol rol) {
        return switch (rol) {
            case ADMIN -> new Administrador();
            case ARRENDADOR -> new Arrendador();
            case ARRENDATARIO -> new Arrendatario();
        };
    }

    // Primer y segundo nombre juntos (el segundo es opcional).
    public String nombres() {
        return unir(primerNombre, segundoNombre);
    }

    // Primer y segundo apellido juntos (el segundo es opcional).
    public String apellidos() {
        return unir(primerApellido, segundoApellido);
    }

    // Igual que fn_nombre_completo.
    public String nombreCompleto() {
        return nombres() + " " + apellidos();
    }

    private String unir(String primero, String segundo) {
        return segundo == null || segundo.isBlank() ? primero : primero + " " + segundo;
    }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Integer getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(Integer numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

    public Integer getTelefono() { return telefono; }
    public void setTelefono(Integer telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Rol getRol() { return rol; }
}
