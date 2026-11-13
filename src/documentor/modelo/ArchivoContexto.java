package documentor.modelo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * PDF "puro" que el usuario adjunta como contexto (HU-18).
 *
 * <p>El archivo original se envía tal cual (sin convertir) a la IA en línea. Además, al
 * agregarlo se intenta extraer su texto localmente: ese texto se usa con el simulador
 * (sin Internet) y para estimar cuántas preguntas permite. El archivo nunca se modifica.</p>
 */
public final class ArchivoContexto {

    /** Máximo de PDF de contexto por sesión. */
    public static final int MAXIMO_ARCHIVOS = 5;
    /** Tamaño máximo por archivo (10 MB). */
    public static final long MAXIMO_BYTES_ARCHIVO = 10L * 1024 * 1024;
    /** Tamaño máximo sumando todos los archivos (20 MB), para no exceder la petición HTTP. */
    public static final long MAXIMO_BYTES_TOTAL = 20L * 1024 * 1024;
    /** Estimación para PDF que no se pudieron leer localmente: palabras por cada 50 KB. */
    private static final int PALABRAS_ESTIMADAS_POR_50KB = 400;

    private final File archivo;
    private final long tamanoBytes;
    private final String textoLocal;   // null si no se pudo extraer en la computadora

    private ArchivoContexto(File archivo, long tamanoBytes, String textoLocal) {
        this.archivo = archivo;
        this.tamanoBytes = tamanoBytes;
        this.textoLocal = textoLocal;
    }

    /**
     * Valida el archivo y prepara el contexto. Puede tardar (extrae el texto del PDF),
     * por eso el Controlador lo llama desde un SwingWorker.
     * @throws IOException si no es un PDF válido o excede los límites
     */
    public static ArchivoContexto crear(File archivo, LectorDocumentos lector) throws IOException {
        if (archivo == null || !archivo.isFile() || !archivo.canRead()) {
            throw new IOException("No se puede leer el archivo seleccionado.");
        }
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("«" + archivo.getName() + "» no es un PDF. Como contexto solo se aceptan archivos .pdf.");
        }
        long tamano = archivo.length();
        if (tamano > MAXIMO_BYTES_ARCHIVO) {
            throw new IOException("«" + archivo.getName() + "» pesa " + formatearTamano(tamano)
                    + "; el máximo por archivo es " + formatearTamano(MAXIMO_BYTES_ARCHIVO) + ".");
        }
        byte[] cabecera = new byte[5];
        try (java.io.InputStream entrada = Files.newInputStream(archivo.toPath())) {
            if (entrada.read(cabecera) < 5 || !new String(cabecera, "ISO-8859-1").equals("%PDF-")) {
                throw new IOException("«" + archivo.getName() + "» no es un PDF válido o está dañado.");
            }
        }
        String texto;
        try {
            texto = lector.leer(archivo).getContenido();
        } catch (IOException e) {
            texto = null;   // PDF escaneado, protegido o con fuentes no estándar: solo lo leerá la IA en línea
        }
        return new ArchivoContexto(archivo, tamano, texto);
    }

    /** Lee los bytes originales del PDF (para enviarlo sin modificar). */
    public byte[] leerBytes() throws IOException {
        return Files.readAllBytes(archivo.toPath());
    }

    public File getArchivo() {
        return archivo;
    }

    public String getNombre() {
        return archivo.getName();
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public boolean esLegibleLocalmente() {
        return textoLocal != null && !textoLocal.trim().isEmpty();
    }

    public String getTextoLocal() {
        return textoLocal;
    }

    /** Palabras del PDF: reales si se leyó localmente; estimadas por tamaño si no. */
    public int getPalabrasEstimadas() {
        if (esLegibleLocalmente()) {
            return LimitesGeneracion.contarPalabras(textoLocal);
        }
        return (int) Math.max(PALABRAS_ESTIMADAS_POR_50KB, tamanoBytes / (50 * 1024) * PALABRAS_ESTIMADAS_POR_50KB);
    }

    /** Texto para la vista, p. ej. "1.2 MB · texto legible sin Internet". */
    public String getDescripcion() {
        return formatearTamano(tamanoBytes) + " · " + (esLegibleLocalmente()
                ? LimitesGeneracion.contarPalabras(textoLocal) + " palabras legibles sin Internet"
                : "solo lo puede leer la IA en línea");
    }

    /**
     * Une el texto principal con el texto local de los PDF de contexto
     * (lo usa el simulador, que no puede leer PDF "puros").
     */
    public static String textoCombinado(String textoPrincipal, List<ArchivoContexto> contextos) {
        StringBuilder sb = new StringBuilder(textoPrincipal == null ? "" : textoPrincipal.trim());
        if (contextos != null) {
            for (ArchivoContexto c : contextos) {
                if (c.esLegibleLocalmente()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(c.getTextoLocal().trim());
                }
            }
        }
        return sb.toString();
    }

    public static String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
