package documentor.modelo.ia;

import documentor.modelo.ArchivoContexto;
import documentor.modelo.Flashcard;
import documentor.modelo.GeneradorMaterial;
import documentor.modelo.LimitesGeneracion;
import documentor.modelo.PreguntaExamen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * Generador de material que usa modelos GRATUITOS de OpenRouter.
 *
 * <p>Estrategia de modelos: primero los configurados (por defecto el enrutador
 * {@code openrouter/free}); si fallan por saturación, límite o respuesta inválida, consulta
 * el catálogo y prueba otros modelos {@code :free}. Nunca usa modelos de pago.</p>
 */
public class GeneradorOpenRouter implements GeneradorMaterial {

    private static final int MAX_MODELOS_DESCUBIERTOS = 3;

    private static final String PROMPT_SISTEMA =
            "Eres un tutor universitario experto en crear material de estudio en español. "
                    + "Te basas EXCLUSIVAMENTE en el texto y los documentos que te da el usuario; no inventes datos externos. "
                    + "Respondes SOLO con un objeto JSON válido, sin explicaciones, sin markdown y sin bloques ```.";

    private final ConfiguracionIA configuracion;
    private final ClienteOpenRouter cliente;
    private final Random aleatorio = new Random();

    private volatile String aviso;
    private volatile String ultimoModelo;
    private long limiteTiempo;
    private List<String> modelosDescubiertos;   // caché del catálogo

    public GeneradorOpenRouter(ConfiguracionIA configuracion) {
        this(configuracion, new ClienteOpenRouter(configuracion));
    }

    GeneradorOpenRouter(ConfiguracionIA configuracion, ClienteOpenRouter cliente) {
        this.configuracion = configuracion;
        this.cliente = cliente;
    }

    // ------------------------------------------------------------------ GeneradorMaterial

    @Override
    public List<Flashcard> generarFlashcards(String texto, int cantidad) {
        return generarFlashcards(texto, cantidad, Collections.<ArchivoContexto>emptyList());
    }

    @Override
    public List<PreguntaExamen> generarExamen(String texto, int cantidad) {
        return generarExamen(texto, cantidad, Collections.<ArchivoContexto>emptyList());
    }

    /** HU-18: los PDF de contexto se envían completos (sin convertir) junto con el texto. */
    @Override
    public List<Flashcard> generarFlashcards(String texto, int cantidad, List<ArchivoContexto> contextos) {
        String prompt = "Crea EXACTAMENTE " + cantidad + " flashcards distintas "
                + "que cubran los conceptos más importantes " + describirFuente(texto, contextos)
                + ". Preguntas claras y respuestas breves.\n"
                + "Formato EXACTO:\n"
                + "{\"flashcards\":[{\"pregunta\":\"...\",\"respuesta\":\"...\"}]}"
                + bloqueTexto(texto);
        return generar(prompt, contextos, json -> convertirFlashcards(json, cantidad), cantidad);
    }

    @Override
    public List<PreguntaExamen> generarExamen(String texto, int cantidad, List<ArchivoContexto> contextos) {
        String prompt = "Crea EXACTAMENTE " + cantidad + " preguntas de opción múltiple distintas "
                + describirFuente(texto, contextos) + ". "
                + "Cada pregunta tiene EXACTAMENTE 4 opciones distintas y solo una correcta; los distractores "
                + "deben ser plausibles pero claramente incorrectos según la fuente. "
                + "\"correcta\" es el índice (0 a 3) de la opción correcta.\n"
                + "Formato EXACTO:\n"
                + "{\"preguntas\":[{\"enunciado\":\"...\",\"opciones\":[\"...\",\"...\",\"...\",\"...\"],\"correcta\":0}]}"
                + bloqueTexto(texto);
        return generar(prompt, contextos, json -> convertirPreguntas(json, cantidad), cantidad);
    }

    /** Explica al modelo de dónde sale la información (texto, PDF adjuntos o ambos). */
    private static String describirFuente(String texto, List<ArchivoContexto> contextos) {
        boolean hayTexto = texto != null && !texto.trim().isEmpty();
        if (contextos == null || contextos.isEmpty()) {
            return "del TEXTO";
        }
        List<String> nombres = new ArrayList<>();
        for (ArchivoContexto c : contextos) {
            nombres.add("«" + c.getNombre() + "»");
        }
        String pdfs = "de los documentos PDF adjuntos (" + String.join(", ", nombres) + ")";
        return hayTexto ? "del TEXTO y " + pdfs + ", que sirven como contexto adicional" : pdfs;
    }

    private String bloqueTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        return "\n\nTEXTO:\n\"\"\"\n" + prepararTexto(texto) + "\n\"\"\"";
    }

    /** Con PDF de contexto, el límite suma sus palabras (reales o estimadas por tamaño). */
    @Override
    public int calcularMaximo(String texto, List<ArchivoContexto> contextos) {
        String t = texto == null ? "" : texto.trim();
        int limite = configuracion.getMaxCaracteres();
        int palabras = LimitesGeneracion.contarPalabras(t.length() > limite ? t.substring(0, limite) : t);
        if (contextos != null) {
            for (ArchivoContexto c : contextos) {
                palabras += c.getPalabrasEstimadas();
            }
        }
        return LimitesGeneracion.maximoPorPalabras(palabras);
    }

    @Override
    public boolean leePdfCompletos() {
        return true;
    }

    /** El límite se calcula sobre la parte del texto que realmente se envía a la IA. */
    @Override
    public int calcularMaximo(String texto) {
        String t = texto == null ? "" : texto.trim();
        int limite = configuracion.getMaxCaracteres();
        return LimitesGeneracion.maximoPorExtension(t.length() > limite ? t.substring(0, limite) : t);
    }

    @Override
    public String getNombre() {
        String modelo = ultimoModelo;
        return "OpenRouter (gratis)" + (modelo != null ? " · " + modelo : "");
    }

    @Override
    public boolean esSimulado() {
        return false;
    }

    private synchronized void agregarAviso(String texto) {
        aviso = aviso == null ? texto : aviso + "\n\n" + texto;
    }

    @Override
    public String consumirAviso() {
        String a = aviso;
        aviso = null;
        return a;
    }

    // ------------------------------------------------------------------ Orquestación de modelos

    private <T> List<T> generar(String prompt, List<ArchivoContexto> pdfs, Function<Map<?, ?>, List<T>> conversor,
                                int cantidad) {
        Set<String> probados = new HashSet<>();
        List<String> errores = new ArrayList<>();
        // Presupuesto total: como máximo el doble del timeout de una petición
        limiteTiempo = System.currentTimeMillis() + configuracion.getTimeoutSegundos() * 2000L;

        List<T> resultado = intentarModelos(configuracion.getModelos(), prompt, pdfs, conversor, probados, errores);
        if (resultado == null && configuracion.isDescubrirModelos()) {
            List<String> extra = new ArrayList<>();
            for (String modelo : obtenerModelosDescubiertos(errores)) {
                if (!probados.contains(modelo) && extra.size() < MAX_MODELOS_DESCUBIERTOS) {
                    extra.add(modelo);
                }
            }
            resultado = intentarModelos(extra, prompt, pdfs, conversor, probados, errores);
        }
        if (resultado == null) {
            throw new ExcepcionIA("Ningún modelo gratuito pudo generar el material:\n• "
                    + String.join("\n• ", errores), true);
        }
        if (resultado.size() < cantidad) {
            agregarAviso("Pediste " + cantidad + " y la IA generó " + resultado.size()
                    + " válidos (se descartan los que vienen incompletos o mal formados).");
        }
        return resultado;
    }

    /** @return la lista generada, o {@code null} si todos los modelos fallaron de forma recuperable. */
    private <T> List<T> intentarModelos(List<String> modelos, String prompt, List<ArchivoContexto> pdfs,
                                        Function<Map<?, ?>, List<T>> conversor,
                                        Set<String> probados, List<String> errores) {
        for (String modelo : modelos) {
            if (System.currentTimeMillis() > limiteTiempo) {
                errores.add("se agotó el tiempo total de espera; no se probaron más modelos");
                return null;
            }
            probados.add(modelo);
            try {
                ClienteOpenRouter.Respuesta respuesta = completarConReintento(modelo, prompt, pdfs);
                List<T> lista = conversor.apply(extraerObjetoJson(respuesta.contenido));
                if (lista.isEmpty()) {
                    throw new ExcepcionIA("respuesta sin elementos válidos", true);
                }
                ultimoModelo = respuesta.modelo;
                return lista;
            } catch (ExcepcionIA e) {
                if (!e.isReintentable()) {
                    throw e;   // p. ej. clave inválida o sin Internet: no tiene sentido seguir
                }
                errores.add(modelo + ": " + e.getMessage());
            } catch (RuntimeException e) {
                errores.add(modelo + ": respuesta con formato inválido (" + e.getMessage() + ")");
            }
        }
        return null;
    }

    /** Los modelos gratis suelen dar 429 por saturación momentánea: se reintenta una vez tras 4 s. */
    private ClienteOpenRouter.Respuesta completarConReintento(String modelo, String prompt,
                                                               List<ArchivoContexto> pdfs) {
        try {
            return cliente.completar(modelo, PROMPT_SISTEMA, prompt, pdfs);
        } catch (ExcepcionIA e) {
            if (e.getMessage() == null || !e.getMessage().contains("(429)")) {
                throw e;
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return cliente.completar(modelo, PROMPT_SISTEMA, prompt, pdfs);
        }
    }

    private synchronized List<String> obtenerModelosDescubiertos(List<String> errores) {
        if (modelosDescubiertos == null) {
            try {
                modelosDescubiertos = cliente.listarModelosGratis();
            } catch (ExcepcionIA e) {
                errores.add("catálogo de modelos: " + e.getMessage());
                return Collections.emptyList();
            }
        }
        return modelosDescubiertos;
    }

    private String prepararTexto(String texto) {
        String limpio = texto.trim();
        int limite = configuracion.getMaxCaracteres();
        if (limpio.length() <= limite) {
            return limpio;
        }
        agregarAviso("El texto tiene " + limpio.length() + " caracteres; se enviaron solo los primeros " + limite
                + " a la IA.\nPara cubrir el resto, selecciona otro fragmento y usa «Generar desde selección».");
        return limpio.substring(0, limite);
    }

    // ------------------------------------------------------------------ Interpretación de la respuesta

    /** Tolera texto extra, bloques ```json y razonamientos <think> alrededor del JSON. */
    static Map<?, ?> extraerObjetoJson(String contenido) {
        String texto = contenido.replaceAll("(?s)<think>.*?</think>", "").trim();
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            throw new ExcepcionIA("la respuesta no contiene JSON", true);
        }
        Object json = JsonSimple.parsear(texto.substring(inicio, fin + 1));
        if (!(json instanceof Map)) {
            throw new ExcepcionIA("la respuesta no es un objeto JSON", true);
        }
        return (Map<?, ?>) json;
    }

    private List<Flashcard> convertirFlashcards(Map<?, ?> json, int cantidad) {
        List<Flashcard> lista = new ArrayList<>();
        for (Object elemento : arreglo(json, "flashcards")) {
            if (!(elemento instanceof Map) || lista.size() >= cantidad) {
                continue;
            }
            String pregunta = texto(((Map<?, ?>) elemento).get("pregunta"));
            String respuesta = texto(((Map<?, ?>) elemento).get("respuesta"));
            if (!pregunta.isEmpty() && !respuesta.isEmpty()) {
                lista.add(new Flashcard(pregunta, respuesta));
            }
        }
        return lista;
    }

    private List<PreguntaExamen> convertirPreguntas(Map<?, ?> json, int cantidad) {
        List<PreguntaExamen> lista = new ArrayList<>();
        for (Object elemento : arreglo(json, "preguntas")) {
            if (!(elemento instanceof Map) || lista.size() >= cantidad) {
                continue;
            }
            Map<?, ?> p = (Map<?, ?>) elemento;
            String enunciado = texto(p.get("enunciado"));
            List<String> opciones = new ArrayList<>();
            Object crudas = p.get("opciones");
            if (crudas instanceof List) {
                for (Object o : (List<?>) crudas) {
                    opciones.add(quitarLetra(texto(o)));
                }
            }
            Set<String> distintas = new LinkedHashSet<>();
            for (String o : opciones) {
                distintas.add(o.toLowerCase(Locale.ROOT));
            }
            int correcta = indiceCorrecto(p.get("correcta"), opciones);
            if (enunciado.isEmpty() || opciones.size() != PreguntaExamen.NUMERO_OPCIONES
                    || distintas.size() != opciones.size() || distintas.contains("") || correcta < 0) {
                continue;   // pregunta mal formada: se descarta en lugar de mostrar algo erróneo
            }
            // Se barajan las opciones: los modelos tienden a poner la correcta siempre en A
            String textoCorrecto = opciones.get(correcta);
            Collections.shuffle(opciones, aleatorio);
            lista.add(new PreguntaExamen(enunciado, opciones, opciones.indexOf(textoCorrecto)));
        }
        return lista;
    }

    /** Acepta 0-3, 1-4 (si viene "4"), "B", "b)" o el texto de la opción. */
    private static int indiceCorrecto(Object valor, List<String> opciones) {
        if (valor instanceof Double) {
            int i = ((Double) valor).intValue();
            if (i >= 0 && i < opciones.size()) {
                return i;
            }
            return i == opciones.size() ? i - 1 : -1;
        }
        String t = texto(valor);
        if (t.matches("(?i)[A-D]\\)?\\.?")) {
            return Character.toUpperCase(t.charAt(0)) - 'A';
        }
        if (t.matches("\\d")) {
            return indiceCorrecto(Double.valueOf(t), opciones);
        }
        String buscado = quitarLetra(t);
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).equalsIgnoreCase(buscado)) {
                return i;
            }
        }
        return -1;
    }

    private static List<?> arreglo(Map<?, ?> json, String clave) {
        Object valor = json.get(clave);
        if (valor instanceof List) {
            return (List<?>) valor;
        }
        // Algunos modelos usan otra clave: se toma el primer arreglo que aparezca
        for (Object v : json.values()) {
            if (v instanceof List) {
                return (List<?>) v;
            }
        }
        return Collections.emptyList();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    /** "B) Texto" → "Texto" (la vista ya antepone la letra). */
    private static String quitarLetra(String opcion) {
        return opcion.replaceFirst("^[A-Da-d][).:-]\\s+", "").trim();
    }
}
