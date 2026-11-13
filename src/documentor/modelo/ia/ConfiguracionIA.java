package documentor.modelo.ia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Configuración de OpenRouter. La clave NUNCA se escribe en el código fuente; se toma de:
 * <ol>
 *   <li>el archivo {@code documentor.properties} (se puede editar desde el panel de
 *       Configuración de la app; vive junto al .jar, en la carpeta de trabajo o en
 *       {@code ~/.documentor/}), o</li>
 *   <li>la variable de entorno {@code OPENROUTER_API_KEY}, si el archivo no tiene clave.</li>
 * </ol>
 * Sólo se aceptan modelos gratuitos: los que terminan en {@code :free} y el enrutador
 * {@code openrouter/free}. Cualquier otro identificador se descarta.
 */
public final class ConfiguracionIA {

    public static final String NOMBRE_ARCHIVO = "documentor.properties";
    public static final String ROUTER_GRATIS = "openrouter/free";
    private static final String URL_BASE_PREDETERMINADA = "https://openrouter.ai/api/v1";

    private final String apiKey;
    private final List<String> modelos;
    private final List<String> modelosDescartados;
    private final boolean descubrirModelos;
    private final int timeoutSegundos;
    private final int maxCaracteres;
    private final String urlBase;
    private final File archivoOrigen;
    private final String origenClave;
    private final Properties propiedades;

    private ConfiguracionIA(Properties p, String apiKeyEntorno, File archivoOrigen) {
        this.propiedades = new Properties();
        this.propiedades.putAll(p);
        // Prioridad: la clave guardada desde la app (archivo) gana a la variable de entorno
        String claveArchivo = limpiarClave(p.getProperty("openrouter.api_key", ""));
        boolean desdeEntorno = claveArchivo.isEmpty() && apiKeyEntorno != null && !apiKeyEntorno.trim().isEmpty();
        this.apiKey = desdeEntorno ? limpiarClave(apiKeyEntorno) : claveArchivo;
        this.archivoOrigen = archivoOrigen;
        this.origenClave = desdeEntorno ? "variable de entorno OPENROUTER_API_KEY"
                : archivoOrigen != null ? archivoOrigen.getAbsolutePath() : "configuración en memoria";

        Set<String> aceptados = new LinkedHashSet<>();
        List<String> descartados = new ArrayList<>();
        for (String modelo : p.getProperty("openrouter.modelos", ROUTER_GRATIS).split(",")) {
            String m = modelo.trim();
            if (m.isEmpty()) {
                continue;
            }
            if (esModeloGratis(m)) {
                aceptados.add(m);
            } else {
                descartados.add(m);
            }
        }
        if (aceptados.isEmpty()) {
            aceptados.add(ROUTER_GRATIS);
        }
        this.modelos = Collections.unmodifiableList(new ArrayList<>(aceptados));
        this.modelosDescartados = Collections.unmodifiableList(descartados);
        this.descubrirModelos = Boolean.parseBoolean(p.getProperty("openrouter.descubrir_modelos", "true").trim());
        this.timeoutSegundos = entero(p, "openrouter.timeout_segundos", 90, 10, 600);
        this.maxCaracteres = entero(p, "openrouter.max_caracteres", 15000, 1000, 200000);
        String url = p.getProperty("openrouter.url_base", URL_BASE_PREDETERMINADA).trim();
        this.urlBase = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Busca la configuración en las ubicaciones conocidas. Nunca lanza excepción. */
    public static ConfiguracionIA cargar(Class<?> claseDeReferencia) {
        Properties propiedades = new Properties();
        File encontrado = null;
        for (File candidato : ubicacionesCandidatas(claseDeReferencia)) {
            if (candidato.isFile()) {
                try (InputStream entrada = new FileInputStream(candidato)) {
                    propiedades.load(entrada);
                    encontrado = candidato;
                    break;
                } catch (IOException e) {
                    // se intenta la siguiente ubicación
                }
            }
        }
        return new ConfiguracionIA(propiedades, System.getenv("OPENROUTER_API_KEY"), encontrado);
    }

    /** Copia de esta configuración con otra clave (para verificarla antes de guardarla). */
    public ConfiguracionIA conClave(String clave) {
        Properties copia = new Properties();
        copia.putAll(propiedades);
        copia.setProperty("openrouter.api_key", limpiarClave(clave));
        return new ConfiguracionIA(copia, null, archivoOrigen);
    }

    /**
     * Guarda (o borra, si {@code clave} está vacía) la API key en {@code documentor.properties},
     * conservando el resto del archivo. Usa el archivo actual o la primera ubicación con permisos
     * de escritura. Después hay que volver a {@link #cargar(Class)} la configuración.
     * @return el archivo donde se guardó.
     */
    public static File guardarClave(Class<?> claseDeReferencia, ConfiguracionIA actual, String clave)
            throws IOException {
        String limpia = limpiarClave(clave);
        List<File> destinos = new ArrayList<>();
        if (actual != null && actual.archivoOrigen != null) {
            destinos.add(actual.archivoOrigen);
        }
        destinos.addAll(ubicacionesCandidatas(claseDeReferencia));
        IOException ultimoError = null;
        for (File destino : destinos) {
            try {
                escribirClave(destino, limpia);
                return destino;
            } catch (IOException e) {
                ultimoError = e;   // sin permisos (p. ej. Archivos de programa): se prueba la siguiente
            }
        }
        throw ultimoError != null ? ultimoError : new IOException("No hay ubicación para guardar la configuración.");
    }

    private static void escribirClave(File archivo, String clave) throws IOException {
        Path ruta = archivo.toPath();
        List<String> lineas = new ArrayList<>();
        boolean reemplazada = false;
        if (archivo.isFile()) {
            // ISO-8859-1 conserva byte a byte el resto del archivo (es la codificación de Properties)
            for (String linea : Files.readAllLines(ruta, StandardCharsets.ISO_8859_1)) {
                if (linea.trim().matches("openrouter\\.api_key\\s*[=:].*|openrouter\\.api_key")) {
                    linea = "openrouter.api_key=" + clave;
                    reemplazada = true;
                }
                lineas.add(linea);
            }
        } else {
            lineas.add("# DocuMentor - configuracion de IA (OpenRouter, solo modelos GRATIS)");
            lineas.add("# Archivo creado desde el panel de Configuracion. NO lo compartas ni lo subas a GitHub.");
            lineas.add("openrouter.modelos=" + ROUTER_GRATIS);
            lineas.add("openrouter.descubrir_modelos=true");
            lineas.add("openrouter.timeout_segundos=90");
            lineas.add("openrouter.max_caracteres=15000");
        }
        if (!reemplazada) {
            lineas.add("openrouter.api_key=" + clave);
        }
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta);
        }
        Files.write(ruta, lineas, StandardCharsets.ISO_8859_1);
    }

    /** Para pruebas: crea la configuración a partir de propiedades en memoria. */
    public static ConfiguracionIA desdePropiedades(Properties propiedades) {
        return new ConfiguracionIA(propiedades, null, null);
    }

    private static List<File> ubicacionesCandidatas(Class<?> claseDeReferencia) {
        List<File> ubicaciones = new ArrayList<>();
        try {
            File origen = new File(claseDeReferencia.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (origen.isFile()) {
                // Ejecutando DocuMentor.jar → junto al .jar
                ubicaciones.add(new File(origen.getParentFile(), NOMBRE_ARCHIVO));
            } else {
                // Ejecutando desde Eclipse/NetBeans (bin/ o build/classes) → raíz del proyecto primero
                File raiz = origen.getParentFile();
                if (raiz != null && raiz.getName().equals("build")) {
                    raiz = raiz.getParentFile();
                }
                if (raiz != null) {
                    ubicaciones.add(new File(raiz, NOMBRE_ARCHIVO));
                }
                ubicaciones.add(new File(origen, NOMBRE_ARCHIVO));
            }
        } catch (Exception e) {
            // sin acceso a la ubicación del .jar: se usan las demás
        }
        ubicaciones.add(new File(System.getProperty("user.dir"), NOMBRE_ARCHIVO));
        ubicaciones.add(new File(new File(System.getProperty("user.home"), ".documentor"), NOMBRE_ARCHIVO));
        return ubicaciones;
    }

    /**
     * Quita errores comunes al pegar la clave: espacios, saltos de línea, caracteres
     * invisibles, comillas y el prefijo "Bearer".
     */
    static String limpiarClave(String clave) {
        String c = clave == null ? "" : clave.replaceAll("[\\s\\u200B-\\u200D\\uFEFF\\u00A0]+", "");
        c = c.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
        if (c.regionMatches(true, 0, "Bearer", 0, 6)) {
            c = c.substring(6);
        }
        return c;
    }

    /** Clave enmascarada para mostrar en mensajes, p. ej. "sk-or-v1-ab12…9f3c (73 caracteres)". */
    public String getClaveEnmascarada() {
        if (apiKey.length() <= 14) {
            return "«" + apiKey + "» (" + apiKey.length() + " caracteres)";
        }
        return apiKey.substring(0, 13) + "…" + apiKey.substring(apiKey.length() - 4)
                + " (" + apiKey.length() + " caracteres)";
    }

    /** De dónde se leyó la clave (ruta del archivo o variable de entorno). */
    public String getOrigenClave() {
        return origenClave;
    }

    public static boolean esModeloGratis(String idModelo) {
        return idModelo != null && (idModelo.endsWith(":free") || ROUTER_GRATIS.equals(idModelo));
    }

    private static int entero(Properties p, String clave, int predeterminado, int min, int max) {
        try {
            int valor = Integer.parseInt(p.getProperty(clave, String.valueOf(predeterminado)).trim());
            return Math.max(min, Math.min(max, valor));
        } catch (NumberFormatException e) {
            return predeterminado;
        }
    }

    public boolean tieneClave() {
        return !apiKey.isEmpty();
    }

    public String getApiKey() {
        return apiKey;
    }

    public List<String> getModelos() {
        return modelos;
    }

    public List<String> getModelosDescartados() {
        return modelosDescartados;
    }

    public boolean isDescubrirModelos() {
        return descubrirModelos;
    }

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public int getMaxCaracteres() {
        return maxCaracteres;
    }

    public String getUrlBase() {
        return urlBase;
    }

    /** Archivo del que se leyó la configuración, o {@code null} si vino de la variable de entorno. */
    public File getArchivoOrigen() {
        return archivoOrigen;
    }
}
