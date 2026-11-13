package documentor.modelo.ia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import documentor.modelo.ArchivoContexto;

/**
 * Cliente HTTP de la API de OpenRouter (formato compatible con OpenAI).
 * Usa {@link HttpURLConnection} para seguir siendo compatible con Java 8.
 */
public class ClienteOpenRouter {

    /** Respuesta de un chat: texto generado y modelo que realmente respondió. */
    public static final class Respuesta {
        public final String contenido;
        public final String modelo;

        Respuesta(String contenido, String modelo) {
            this.contenido = contenido;
            this.modelo = modelo;
        }
    }

    private static final String TITULO_APP = "DocuMentor";
    /** Motor de OpenRouter para leer PDF que es gratuito (los otros, mistral-ocr y native, cobran). */
    public static final String MOTOR_PDF_GRATIS = "cloudflare-ai";

    private final ConfiguracionIA configuracion;

    public ClienteOpenRouter(ConfiguracionIA configuracion) {
        this.configuracion = configuracion;
    }

    /** Envía un chat (mensaje de sistema + usuario) a un modelo gratuito. */
    public Respuesta completar(String modelo, String mensajeSistema, String mensajeUsuario) {
        return completar(modelo, mensajeSistema, mensajeUsuario, java.util.Collections.<ArchivoContexto>emptyList());
    }

    /**
     * Envía un chat con PDF "puros" adjuntos (HU-18). Cada PDF viaja sin modificar, codificado
     * en base64, como parte {@code "type":"file"}. Se fuerza el motor de lectura GRATUITO
     * {@value #MOTOR_PDF_GRATIS}: si no se indicara, OpenRouter podría usar uno de pago (mistral-ocr).
     */
    public Respuesta completar(String modelo, String mensajeSistema, String mensajeUsuario,
                               List<ArchivoContexto> pdfs) {
        if (!ConfiguracionIA.esModeloGratis(modelo)) {
            // Salvaguarda: este cliente jamás envía peticiones a modelos de pago.
            throw new ExcepcionIA("El modelo «" + modelo + "» no es gratuito y fue bloqueado.", true);
        }
        StringBuilder cuerpo = new StringBuilder(256);
        cuerpo.append("{\"model\":").append(JsonSimple.cadena(modelo))
                .append(",\"temperature\":0.3,\"max_tokens\":6000,\"messages\":[")
                .append("{\"role\":\"system\",\"content\":").append(JsonSimple.cadena(mensajeSistema)).append("},")
                .append("{\"role\":\"user\",\"content\":");
        if (pdfs == null || pdfs.isEmpty()) {
            cuerpo.append(JsonSimple.cadena(mensajeUsuario)).append("}]");
        } else {
            cuerpo.append("[{\"type\":\"text\",\"text\":").append(JsonSimple.cadena(mensajeUsuario)).append("}");
            for (ArchivoContexto pdf : pdfs) {
                byte[] bytes;
                try {
                    bytes = pdf.leerBytes();
                } catch (java.io.IOException e) {
                    throw new ExcepcionIA("No se pudo leer el PDF de contexto «" + pdf.getNombre() + "»: "
                            + e.getMessage(), false, e);
                }
                cuerpo.append(",{\"type\":\"file\",\"file\":{\"filename\":").append(JsonSimple.cadena(pdf.getNombre()))
                        .append(",\"file_data\":\"data:application/pdf;base64,")
                        .append(java.util.Base64.getEncoder().encodeToString(bytes)).append("\"}}");
            }
            cuerpo.append("]}]")
                    .append(",\"plugins\":[{\"id\":\"file-parser\",\"pdf\":{\"engine\":\"")
                    .append(MOTOR_PDF_GRATIS).append("\"}}]");
        }
        cuerpo.append("}");
        Map<String, Object> json = comoObjeto(peticion("POST", "/chat/completions", cuerpo.toString()));
        Object errorEnCuerpo = json.get("error");
        if (errorEnCuerpo != null) {
            throw new ExcepcionIA("El modelo devolvió un error: " + mensajeDeError(errorEnCuerpo), true);
        }
        Object choices = json.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
            throw new ExcepcionIA("La respuesta de " + modelo + " no contiene resultados.", true);
        }
        Object primera = ((List<?>) choices).get(0);
        Object mensaje = primera instanceof Map ? ((Map<?, ?>) primera).get("message") : null;
        Object contenido = mensaje instanceof Map ? ((Map<?, ?>) mensaje).get("content") : null;
        if (!(contenido instanceof String) || ((String) contenido).trim().isEmpty()) {
            throw new ExcepcionIA("El modelo " + modelo + " devolvió una respuesta vacía.", true);
        }
        Object modeloUsado = json.get("model");
        return new Respuesta((String) contenido, modeloUsado instanceof String ? (String) modeloUsado : modelo);
    }

    /** Datos de la clave devueltos por {@code GET /key} (no consume peticiones de modelos). */
    public static final class InfoClave {
        public final String nombre;
        public final boolean planGratis;
        public final int usadasHoy;
        public final int limiteHoy;      // -1 si OpenRouter no lo informa
        public final int restantesHoy;   // -1 si OpenRouter no lo informa

        InfoClave(String nombre, boolean planGratis, int usadasHoy, int limiteHoy, int restantesHoy) {
            this.nombre = nombre;
            this.planGratis = planGratis;
            this.usadasHoy = usadasHoy;
            this.limiteHoy = limiteHoy;
            this.restantesHoy = restantesHoy;
        }

        /** Resumen legible para el usuario. */
        public String resumen() {
            StringBuilder sb = new StringBuilder("Clave válida");
            if (nombre != null && !nombre.isEmpty()) {
                sb.append(" («").append(nombre).append("»)");
            }
            sb.append(".");
            if (limiteHoy >= 0) {
                sb.append(" Peticiones gratis hoy: ").append(usadasHoy).append(" de ").append(limiteHoy)
                        .append(" usadas (quedan ").append(Math.max(0, restantesHoy)).append(").");
            }
            return sb.toString();
        }
    }

    /** Verifica la API key sin gastar peticiones de modelos. Lanza {@link ExcepcionIA} si no es válida. */
    public InfoClave verificarClave() {
        if (configuracion.getApiKey().isEmpty()) {
            throw new ExcepcionIA("Escribe una API key para verificarla.", false);
        }
        Map<String, Object> json = comoObjeto(peticion("GET", "/key", null));
        Object datos = json.get("data");
        if (!(datos instanceof Map)) {
            throw new ExcepcionIA("OpenRouter no devolvió información de la clave.", true);
        }
        Map<?, ?> d = (Map<?, ?>) datos;
        Object label = d.get("label");
        Object gratis = d.get("is_free_tier");
        int usadas = 0;
        int limite = -1;
        int restantes = -1;
        Object diario = d.get("free_model_daily_requests");
        if (diario instanceof Map) {
            usadas = entero(((Map<?, ?>) diario).get("used"), 0);
            limite = entero(((Map<?, ?>) diario).get("limit"), -1);
            restantes = entero(((Map<?, ?>) diario).get("remaining"), limite - usadas);
        }
        return new InfoClave(label instanceof String ? (String) label : "",
                Boolean.TRUE.equals(gratis), usadas, limite, restantes);
    }

    private static int entero(Object valor, int predeterminado) {
        return valor instanceof Double ? ((Double) valor).intValue() : predeterminado;
    }

    /**
     * Consulta el catálogo público y devuelve los modelos gratuitos de texto disponibles
     * (id terminado en ":free" y precio 0), ordenados por tamaño de contexto.
     */
    public List<String> listarModelosGratis() {
        Map<String, Object> json = comoObjeto(peticion("GET", "/models", null));
        List<Object[]> encontrados = new ArrayList<>();
        Object datos = json.get("data");
        if (datos instanceof List) {
            for (Object elemento : (List<?>) datos) {
                if (!(elemento instanceof Map)) {
                    continue;
                }
                Map<?, ?> modelo = (Map<?, ?>) elemento;
                Object id = modelo.get("id");
                if (!(id instanceof String) || !((String) id).endsWith(":free") || !esPrecioCero(modelo.get("pricing"))
                        || !generaTexto(modelo.get("architecture")) || ((String) id).matches(".*(safety|guard).*")) {
                    continue;
                }
                Object contexto = modelo.get("context_length");
                double tamano = contexto instanceof Double ? (Double) contexto : 0;
                encontrados.add(new Object[]{id, tamano});
            }
        }
        encontrados.sort((a, b) -> {
            int prioridad = Integer.compare(prioridad((String) a[0]), prioridad((String) b[0]));
            return prioridad != 0 ? prioridad : Double.compare((Double) b[1], (Double) a[1]);
        });
        List<String> ids = new ArrayList<>();
        for (Object[] par : encontrados) {
            ids.add((String) par[0]);
        }
        return ids;
    }

    /** Familias de modelos de chat generales que suelen responder bien en JSON y en español. */
    private static final String[] FAMILIAS_PREFERIDAS = {
        "gemma", "llama", "qwen", "mistral", "deepseek", "gpt-oss", "nemotron-3-super", "glm", "kimi"
    };

    /** 0 = familia preferida, 1 = desconocida, 2 = probablemente inadecuada (código, visión, agentes). */
    private static int prioridad(String id) {
        String minusculas = id.toLowerCase(java.util.Locale.ROOT);
        if (minusculas.matches(".*(code|coder|vision|-vl|omni|audio|inkling|lightning).*")) {
            return 2;
        }
        for (String familia : FAMILIAS_PREFERIDAS) {
            if (minusculas.contains(familia)) {
                return 0;
            }
        }
        return 1;
    }

    private static boolean esPrecioCero(Object precios) {
        if (!(precios instanceof Map)) {
            return false;
        }
        Map<?, ?> p = (Map<?, ?>) precios;
        return esCero(p.get("prompt")) && esCero(p.get("completion"));
    }

    private static boolean esCero(Object valor) {
        try {
            return valor != null && Double.parseDouble(valor.toString()) == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean generaTexto(Object arquitectura) {
        if (!(arquitectura instanceof Map)) {
            return true;   // sin información: se acepta
        }
        Object salidas = ((Map<?, ?>) arquitectura).get("output_modalities");
        return !(salidas instanceof List) || ((List<?>) salidas).contains("text");
    }

    // ------------------------------------------------------------------ HTTP

    private String peticion(String metodo, String ruta, String cuerpo) {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(configuracion.getUrlBase() + ruta).openConnection();
            if (conexion instanceof HttpsURLConnection) {
                // Confía también en los certificados de Windows (antivirus que inspeccionan HTTPS)
                ((HttpsURLConnection) conexion).setSSLSocketFactory(ConfianzaCertificados.fabrica());
            }
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(15_000);
            conexion.setReadTimeout(configuracion.getTimeoutSegundos() * 1000);
            conexion.setRequestProperty("Authorization", "Bearer " + configuracion.getApiKey());
            conexion.setRequestProperty("X-Title", TITULO_APP);
            conexion.setRequestProperty("Accept", "application/json");
            if (cuerpo != null) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                }
            }
            int codigo = conexion.getResponseCode();
            String respuesta = leerTodo(codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream());
            if (codigo >= 400) {
                throw errorHttp(codigo, respuesta, configuracion);
            }
            return respuesta;
        } catch (ExcepcionIA e) {
            throw e;
        } catch (UnknownHostException e) {
            throw new ExcepcionIA("Sin conexión a Internet (no se encontró " + e.getMessage() + ").", false, e);
        } catch (SocketTimeoutException e) {
            throw new ExcepcionIA("El modelo tardó demasiado en responder (más de "
                    + configuracion.getTimeoutSegundos() + " s).", true, e);
        } catch (SSLException e) {
            throw new ExcepcionIA("No se pudo verificar el certificado de seguridad de OpenRouter.\n"
                    + "Normalmente lo causa un antivirus que revisa las conexiones HTTPS (Kaspersky, Avast, ESET...) "
                    + "o una versión de Java muy antigua.\n"
                    + "Solución: agrega openrouter.ai a las exclusiones del antivirus o actualiza Java.\n"
                    + "Detalle técnico: " + e.getMessage(), false, e);
        } catch (IOException e) {
            throw new ExcepcionIA("Error de red al contactar OpenRouter: " + e.getMessage(), true, e);
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private static ExcepcionIA errorHttp(int codigo, String cuerpo, ConfiguracionIA config) {
        String detalle = cuerpo;
        try {
            Object json = JsonSimple.parsear(cuerpo);
            if (json instanceof Map && ((Map<?, ?>) json).get("error") != null) {
                detalle = mensajeDeError(((Map<?, ?>) json).get("error"));
            }
        } catch (IllegalArgumentException e) {
            // cuerpo no JSON: se muestra tal cual (recortado)
        }
        if (detalle != null && detalle.length() > 300) {
            detalle = detalle.substring(0, 300) + "…";
        }
        switch (codigo) {
            case 401:
                String pista = config.getApiKey().contains("PEGA_AQUI")
                        ? "\n⚠ Sigue el texto de ejemplo: reemplázalo por tu clave real."
                        : config.getApiKey().startsWith("sk-or-")
                        ? "" : "\n⚠ La clave no empieza con «sk-or-»: no parece una clave de OpenRouter.";
                return new ExcepcionIA("La API key de OpenRouter no es válida o fue revocada (401)."
                        + "\nClave usada: " + config.getClaveEnmascarada()
                        + "\nLeída de: " + config.getOrigenClave() + pista
                        + "\nRespuesta de OpenRouter: " + detalle, false);
            case 402:
                return new ExcepcionIA("OpenRouter pide saldo para esta petición (402): " + detalle, true);
            case 403:
                return new ExcepcionIA(detalle != null && detalle.contains("agentic")
                        ? "Este modelo gratis solo se permite en apps de agentes (403)."
                        : "Petición rechazada (403): " + detalle, true);
            case 404:
                if (detalle != null && (detalle.contains("ZDR") || detalle.contains("data policy"))) {
                    return new ExcepcionIA("Bloqueado por tu configuración de privacidad (404): tu cuenta exige "
                            + "\"Zero Data Retention\" y los proveedores gratuitos no la cumplen. "
                            + "Desactívalo en openrouter.ai/settings/privacy (Guardrails).", false);
                }
                return new ExcepcionIA("Modelo no disponible (404): " + detalle, true);
            case 413:
                return new ExcepcionIA("Los PDF de contexto son demasiado grandes para una sola petición (413). "
                        + "Quita alguno o usa archivos más ligeros.", false);
            case 429:
                return new ExcepcionIA("Modelo gratis saturado o límite alcanzado (429). Límites: ~20 peticiones/min "
                        + "y 50/día sin créditos. Detalle: " + detalle, true);
            default:
                return new ExcepcionIA("OpenRouter respondió " + codigo + ": " + detalle, codigo >= 500 || codigo == 408);
        }
    }

    private static String mensajeDeError(Object error) {
        if (error instanceof Map) {
            Object mensaje = ((Map<?, ?>) error).get("message");
            return mensaje != null ? mensaje.toString() : error.toString();
        }
        return String.valueOf(error);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoObjeto(String texto) {
        try {
            Object json = JsonSimple.parsear(texto.trim());
            if (json instanceof Map) {
                return (Map<String, Object>) json;
            }
        } catch (IllegalArgumentException e) {
            throw new ExcepcionIA("OpenRouter devolvió una respuesta ilegible.", true, e);
        }
        throw new ExcepcionIA("OpenRouter devolvió una respuesta inesperada.", true);
    }

    private static String leerTodo(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
        try (InputStream in = entrada) {
            ByteArrayOutputStream bufer = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            int leidos;
            while ((leidos = in.read(bloque)) != -1) {
                bufer.write(bloque, 0, leidos);
            }
            return new String(bufer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
