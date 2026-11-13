package documentor.modelo;

import java.io.File;
import java.util.Objects;

/**
 * Entidad que representa un documento de estudio cargado por el usuario.
 * Es inmutable: si el usuario abre otro archivo se crea un nuevo Documento.
 */
public class Documento {

    private final String nombreArchivo;
    private final String contenido;
    private final File archivoOrigen;

    public Documento(String nombreArchivo, String contenido, File archivoOrigen) {
        this.nombreArchivo = Objects.requireNonNull(nombreArchivo, "nombreArchivo");
        this.contenido = contenido == null ? "" : contenido;
        this.archivoOrigen = archivoOrigen;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getContenido() {
        return contenido;
    }

    public File getArchivoOrigen() {
        return archivoOrigen;
    }

    /** Nombre del archivo sin extensión (útil para sugerir nombres al exportar). */
    public String getNombreBase() {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto > 0 ? nombreArchivo.substring(0, punto) : nombreArchivo;
    }

    public boolean tieneContenido() {
        return !contenido.trim().isEmpty();
    }

    public int contarPalabras() {
        String recortado = contenido.trim();
        return recortado.isEmpty() ? 0 : recortado.split("\\s+").length;
    }
}
