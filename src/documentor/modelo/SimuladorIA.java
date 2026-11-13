package documentor.modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación simulada del motor de IA basada en heurísticas de texto en español.
 *
 * <p>Detecta cuatro tipos de conocimiento en el documento:</p>
 * <ul>
 *   <li><b>Listas</b> ("X consta de: 1. ... 2. ...") → "¿De qué consta X?" + orden de los elementos.</li>
 *   <li><b>Causas</b> ("A porque B") → "¿Por qué A?".</li>
 *   <li><b>Definiciones</b> ("X es Y", "X consiste en Y") → "¿Qué es X?".</li>
 *   <li><b>Datos numéricos y palabras clave</b> → preguntas para completar.</li>
 * </ul>
 * Para el examen genera distractores plausibles a partir del propio documento.
 */
public class SimuladorIA implements GeneradorMaterial {

    private static final int MAX_ELEMENTOS = LimitesGeneracion.MAXIMO_ABSOLUTO;
    private static final int LONGITUD_MINIMA_ORACION = 20;
    private static final int LONGITUD_MAXIMA_ORACION = 400;

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern PATRON_ELEMENTO_LISTA =
            Pattern.compile("^\\s*(?:\\d{1,2}\\s*[.)\\-]|[a-zA-Z][.)]|[-•*·▪●–])\\s+(.+)$");
    private static final Pattern PATRON_INTRO_LISTA = Pattern.compile(
            "^(.+?)\\s+(consta de|se compone de|está formad[oa] por|incluye|comprende|tiene|se divide en|abarca|son)"
                    + "(?:\\s+(.*?))?\\s*:$", FLAGS);
    private static final Pattern PATRON_DEFINICION = Pattern.compile(
            "^(.{3,70}?)\\s+(se define como|consiste en|se refiere a|es|son)\\s+(.{8,})$", FLAGS);
    private static final Pattern PATRON_NUMERO =
            Pattern.compile("(?<![\\p{L}\\d])(\\d+(?:[.,]\\d+)?)(\\s*%)?(?![\\p{L}\\d])");
    private static final Pattern PATRON_PALABRA = Pattern.compile("\\p{L}{6,}");
    private static final Pattern PATRON_DIVISOR_ORACIONES = Pattern.compile("(?<=[.!?…])\\s+|\\n+");

    private static final Set<String> PALABRAS_VACIAS = new HashSet<>(Arrays.asList(
            "también", "además", "porque", "durante", "mientras", "siempre", "través", "cuando", "debido",
            "pueden", "embargo", "existen", "tienen", "dentro", "contra", "aunque", "nuestro", "nuestra",
            "entonces", "después", "cualquier", "algunos", "algunas", "muchos", "muchas", "general",
            "importante", "diferentes", "mediante", "permite", "permiten", "manera", "nuestros", "aquellos",
            "aquellas", "estos", "estas", "siguiente", "siguientes", "ejemplo", "respecto", "acerca",
            "solamente", "únicamente", "realizar", "primero", "segundo", "tercero", "cuarto", "principales",
            "principal", "etapa", "etapas", "partir", "cuales", "cuáles", "parte", "veces", "puede"));

    private static final Set<String> SUJETOS_INVALIDOS = new HashSet<>(Arrays.asList(
            "esto", "eso", "esta", "este", "ello", "lo", "que", "aquí", "así", "también", "ahora", "hoy", "no"));

    private static final List<String> CAUSAS_GENERICAS = Arrays.asList(
            "Porque es el paso más rápido de completar.",
            "Porque reemplaza todas las demás etapas del proceso.",
            "Porque solo es un requisito administrativo sin impacto real.",
            "Porque define el lenguaje de programación a usar.",
            "Porque elimina la necesidad de realizar pruebas.",
            "Porque únicamente afecta la apariencia del resultado final.");

    private static final List<String> DEFINICIONES_GENERICAS = Arrays.asList(
            "Una actividad opcional que se realiza solo al final del proyecto.",
            "Un elemento decorativo sin impacto en el resultado.",
            "Una herramienta exclusiva para el diseño visual.",
            "Un trámite administrativo independiente del contenido.");

    private static final List<String> ELEMENTOS_FICTICIOS = Arrays.asList(
            "Implementación del código", "Despliegue en producción", "Diseño de la interfaz",
            "Gestión de riesgos", "Pruebas de aceptación", "Documentación técnica",
            "Planificación del presupuesto", "Capacitación de usuarios");

    private static final List<String> OPCIONES_COMODIN = Arrays.asList(
            "Ninguna de las anteriores", "Todas las anteriores", "No se menciona en el documento");

    private final Random aleatorio;

    public SimuladorIA() {
        this(new Random());
    }

    /** Constructor para pruebas: permite fijar la semilla y obtener resultados repetibles. */
    public SimuladorIA(Random aleatorio) {
        this.aleatorio = aleatorio;
    }

    // ------------------------------------------------------------------ API pública

    @Override
    public List<Flashcard> generarFlashcards(String texto, int cantidad) {
        Analisis analisis = analizar(texto);
        List<Flashcard> listaFlashcards = new ArrayList<>();
        for (Concepto concepto : primeros(analisis.conceptos, cantidad)) {
            listaFlashcards.add(new Flashcard(concepto.pregunta, concepto.respuesta));
        }
        return listaFlashcards;
    }

    @Override
    public List<PreguntaExamen> generarExamen(String texto, int cantidad) {
        Analisis analisis = analizar(texto);
        List<PreguntaExamen> listaPreguntas = new ArrayList<>();
        for (Concepto concepto : primeros(analisis.conceptos, cantidad)) {
            String correcta = concepto.tipo == Concepto.Tipo.LISTA
                    ? String.join(", ", concepto.elementos)
                    : concepto.respuesta;
            List<String> distractores = generarDistractores(concepto, correcta, analisis);

            List<String> opciones = new ArrayList<>(distractores);
            opciones.add(correcta);
            Collections.shuffle(opciones, aleatorio);
            listaPreguntas.add(new PreguntaExamen(concepto.pregunta, opciones, opciones.indexOf(correcta)));
        }
        return listaPreguntas;
    }

    /** El simulador solo puede generar tantos elementos como conceptos detecte en el texto. */
    @Override
    public int calcularMaximo(String texto) {
        return analizar(texto).conceptos.size();
    }

    private static <T> List<T> primeros(List<T> lista, int cantidad) {
        return lista.subList(0, Math.max(0, Math.min(cantidad, lista.size())));
    }

    // ------------------------------------------------------------------ Análisis del texto

    /** Resultado intermedio del análisis: conceptos detectados + vocabulario para distractores. */
    private static final class Analisis {
        final List<Concepto> conceptos = new ArrayList<>();
        final List<String> vocabulario = new ArrayList<>();
    }

    private Analisis analizar(String texto) {
        Analisis analisis = new Analisis();
        if (texto == null || texto.trim().isEmpty()) {
            return analisis;
        }
        List<Concepto> principales = new ArrayList<>();
        List<Concepto> secundarios = new ArrayList<>();   // preguntas de orden: menor prioridad
        Set<String> vocabulario = new LinkedHashSet<>();

        String[] lineas = texto.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder prosa = new StringBuilder();
        int i = 0;
        while (i < lineas.length) {
            String linea = lineas[i].trim();
            if (linea.endsWith(":")) {
                List<String> elementos = new ArrayList<>();
                int j = i + 1;
                while (j < lineas.length) {
                    Matcher m = PATRON_ELEMENTO_LISTA.matcher(lineas[j]);
                    if (!m.matches()) {
                        break;
                    }
                    elementos.add(limpiarElemento(m.group(1)));
                    j++;
                }
                if (elementos.size() >= 2) {
                    procesarProsa(prosa.toString(), principales, vocabulario);
                    prosa.setLength(0);
                    agregarConceptosLista(linea, elementos, principales, secundarios);
                    i = j;
                    continue;
                }
            }
            Matcher vineta = PATRON_ELEMENTO_LISTA.matcher(linea);
            String contenido = vineta.matches() ? asegurarPunto(vineta.group(1).trim()) : linea;
            prosa.append(contenido).append(linea.isEmpty() ? "\n" : " ");
            i++;
        }
        procesarProsa(prosa.toString(), principales, vocabulario);

        Set<String> preguntasVistas = new HashSet<>();
        List<Concepto> todos = new ArrayList<>(principales);
        todos.addAll(secundarios);
        for (Concepto concepto : todos) {
            if (analisis.conceptos.size() >= MAX_ELEMENTOS) {
                break;
            }
            if (preguntasVistas.add(concepto.pregunta.toLowerCase(Locale.ROOT))) {
                analisis.conceptos.add(concepto);
            }
        }
        analisis.vocabulario.addAll(vocabulario);
        return analisis;
    }

    private void agregarConceptosLista(String intro, List<String> elementos,
                                       List<Concepto> principales, List<Concepto> secundarios) {
        String introSinDosPuntos = intro.substring(0, intro.length() - 1).trim();
        String pregunta;
        Matcher m = PATRON_INTRO_LISTA.matcher(intro);
        if (m.matches()) {
            String sujeto = minusculaInicial(m.group(1).trim());
            String verbo = m.group(2).toLowerCase(Locale.ROOT);
            String objeto = m.group(3) == null ? "" : m.group(3).trim();
            String base;
            switch (verbo) {
                case "consta de":
                    base = "¿De qué consta " + sujeto + "?";
                    break;
                case "se compone de":
                    base = "¿De qué se compone " + sujeto + "?";
                    break;
                case "se divide en":
                    base = "¿En qué se divide " + sujeto + "?";
                    break;
                case "son":
                    base = "¿Cuáles son " + sujeto + "?";
                    break;
                default:
                    base = verbo.startsWith("está formad")
                            ? "¿Qué elementos forman " + sujeto + "?"
                            : "¿Qué " + verbo + " " + sujeto + "?";
            }
            pregunta = objeto.isEmpty() ? base : base + " (" + objeto + ")";
        } else {
            pregunta = "Enumera los elementos de: «" + introSinDosPuntos + "»";
        }

        StringBuilder respuesta = new StringBuilder();
        for (int k = 0; k < elementos.size(); k++) {
            if (k > 0) {
                respuesta.append('\n');
            }
            respuesta.append(k + 1).append(". ").append(elementos.get(k)).append('.');
        }
        principales.add(new Concepto(Concepto.Tipo.LISTA, pregunta, respuesta.toString(), elementos));

        for (int k = 0; k < elementos.size(); k++) {
            String preguntaOrden = "En la lista «" + introSinDosPuntos + "», ¿qué elemento ocupa la posición "
                    + (k + 1) + "?";
            secundarios.add(new Concepto(Concepto.Tipo.ORDEN, preguntaOrden, elementos.get(k), elementos));
        }
    }

    private void procesarProsa(String prosa, List<Concepto> conceptos, Set<String> vocabulario) {
        if (prosa.trim().isEmpty()) {
            return;
        }
        for (String fragmento : PATRON_DIVISOR_ORACIONES.split(prosa)) {
            String oracion = fragmento.trim();
            Matcher palabras = PATRON_PALABRA.matcher(oracion);
            while (palabras.find()) {
                String palabra = palabras.group();
                if (!PALABRAS_VACIAS.contains(palabra.toLowerCase(Locale.ROOT))) {
                    vocabulario.add(palabra);
                }
            }
            if (oracion.length() >= LONGITUD_MINIMA_ORACION && oracion.length() <= LONGITUD_MAXIMA_ORACION) {
                agregarConceptosOracion(oracion, conceptos);
            }
        }
    }

    private void agregarConceptosOracion(String oracion, List<Concepto> conceptos) {
        boolean generada = false;

        // 1) Relación causa-efecto: "A porque B"
        int indicePorque = oracion.toLowerCase(Locale.ROOT).indexOf(" porque ");
        if (indicePorque > 8) {
            String sujeto = quitarPuntuacionFinal(oracion.substring(0, indicePorque));
            String razon = oracion.substring(indicePorque + " porque ".length()).trim();
            if (contarPalabras(sujeto) <= 14 && razon.length() > 5) {
                conceptos.add(new Concepto(Concepto.Tipo.CAUSA,
                        "¿Por qué " + minusculaInicial(sujeto) + "?",
                        "Porque " + asegurarPunto(razon), null));
                generada = true;
            }
        }

        // 2) Definición: "X es Y", "X consiste en Y"...
        if (!generada) {
            Matcher m = PATRON_DEFINICION.matcher(quitarPuntuacionFinal(oracion));
            if (m.matches()) {
                String sujeto = m.group(1).trim();
                String verbo = m.group(2).toLowerCase(Locale.ROOT);
                String predicado = m.group(3).trim();
                String primeraPalabra = sujeto.split("\\s+")[0].toLowerCase(Locale.ROOT);
                if (contarPalabras(sujeto) <= 7 && !sujeto.contains(",") && contarPalabras(predicado) >= 3
                        && !SUJETOS_INVALIDOS.contains(primeraPalabra)) {
                    String sujetoMin = minusculaInicial(sujeto);
                    String pregunta;
                    switch (verbo) {
                        case "consiste en":
                            pregunta = "¿En qué consiste " + sujetoMin + "?";
                            break;
                        case "se define como":
                            pregunta = "¿Cómo se define " + sujetoMin + "?";
                            break;
                        case "se refiere a":
                            pregunta = "¿A qué se refiere " + sujetoMin + "?";
                            break;
                        case "son":
                            pregunta = "¿Qué son " + sujetoMin + "?";
                            break;
                        default:
                            pregunta = "¿Qué es " + sujetoMin + "?";
                    }
                    conceptos.add(new Concepto(Concepto.Tipo.DEFINICION, pregunta,
                            asegurarPunto(mayusculaInicial(predicado)), null));
                    generada = true;
                }
            }
        }

        // 3) Dato numérico: "Completa: ... ____ ..."
        Matcher numero = PATRON_NUMERO.matcher(oracion);
        if (numero.find()) {
            String valor = numero.group(1) + (numero.group(2) != null ? "%" : "");
            String huecos = oracion.substring(0, numero.start()) + "____" + oracion.substring(numero.end());
            conceptos.add(new Concepto(Concepto.Tipo.NUMERO, "Completa: " + huecos, valor, null));
            generada = true;
        }

        // 4) Respaldo: palabra clave más larga de la oración
        if (!generada) {
            String clave = null;
            Matcher palabras = PATRON_PALABRA.matcher(oracion);
            while (palabras.find()) {
                String palabra = palabras.group();
                if (!PALABRAS_VACIAS.contains(palabra.toLowerCase(Locale.ROOT))
                        && (clave == null || palabra.length() > clave.length())) {
                    clave = palabra;
                }
            }
            if (clave != null) {
                String huecos = oracion.replaceFirst("(?<!\\p{L})" + Pattern.quote(clave) + "(?!\\p{L})", "____");
                conceptos.add(new Concepto(Concepto.Tipo.CLAVE, "Completa: " + huecos, clave, null));
            }
        }
    }

    // ------------------------------------------------------------------ Distractores

    private List<String> generarDistractores(Concepto concepto, String correcta, Analisis analisis) {
        List<String> candidatos = new ArrayList<>();
        switch (concepto.tipo) {
            case ORDEN:
                candidatos.addAll(mezclar(concepto.elementos));
                break;
            case LISTA:
                for (int intento = 0; intento < 8; intento++) {
                    List<String> copia = new ArrayList<>(concepto.elementos);
                    copia.set(aleatorio.nextInt(copia.size()), elementoFicticio(copia, analisis));
                    candidatos.add(String.join(", ", copia));
                }
                break;
            case CAUSA:
                candidatos.addAll(mezclar(respuestasDelTipo(analisis, Concepto.Tipo.CAUSA)));
                candidatos.addAll(mezclar(CAUSAS_GENERICAS));
                break;
            case NUMERO:
                candidatos.addAll(variantesNumericas(correcta));
                break;
            case DEFINICION:
                candidatos.addAll(mezclar(respuestasDelTipo(analisis, Concepto.Tipo.DEFINICION)));
                candidatos.addAll(mezclar(DEFINICIONES_GENERICAS));
                break;
            case CLAVE:
            default:
                candidatos.addAll(mezclar(analisis.vocabulario));
                break;
        }
        candidatos.addAll(OPCIONES_COMODIN);

        List<String> distractores = new ArrayList<>();
        Set<String> usadas = new HashSet<>();
        usadas.add(normalizar(correcta));
        for (String candidato : candidatos) {
            if (distractores.size() == PreguntaExamen.NUMERO_OPCIONES - 1) {
                break;
            }
            if (candidato != null && !candidato.trim().isEmpty() && usadas.add(normalizar(candidato))) {
                distractores.add(candidato);
            }
        }
        return distractores;
    }

    private List<String> respuestasDelTipo(Analisis analisis, Concepto.Tipo tipo) {
        List<String> respuestas = new ArrayList<>();
        for (Concepto c : analisis.conceptos) {
            if (c.tipo == tipo) {
                respuestas.add(c.respuesta);
            }
        }
        return respuestas;
    }

    private String elementoFicticio(List<String> actuales, Analisis analisis) {
        List<String> pool = new ArrayList<>();
        for (Concepto c : analisis.conceptos) {
            if (c.tipo == Concepto.Tipo.LISTA && c.elementos != actuales) {
                pool.addAll(c.elementos);
            }
        }
        pool.addAll(ELEMENTOS_FICTICIOS);
        for (String opcion : mezclar(pool)) {
            if (!actuales.contains(opcion)) {
                return opcion;
            }
        }
        return ELEMENTOS_FICTICIOS.get(0);
    }

    private List<String> variantesNumericas(String correcta) {
        boolean porcentaje = correcta.endsWith("%");
        String limpio = correcta.replace("%", "").replace(',', '.');
        List<String> variantes = new ArrayList<>();
        double valor;
        try {
            valor = Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return variantes;
        }
        boolean entero = valor == Math.rint(valor) && !limpio.contains(".");
        double[] factores = {2, 0.5, 10, 1.5, 0.25, 3};
        for (double factor : mezclarFactores(factores)) {
            double v = valor * factor;
            if (valor == 0) {
                v = factor * 5;
            }
            String texto = entero ? String.valueOf(Math.max(1, Math.round(v))) : String.format(Locale.ROOT, "%.1f", v);
            variantes.add(porcentaje ? texto + "%" : texto);
        }
        return variantes;
    }

    private double[] mezclarFactores(double[] factores) {
        double[] copia = factores.clone();
        for (int i = copia.length - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            double t = copia[i];
            copia[i] = copia[j];
            copia[j] = t;
        }
        return copia;
    }

    private <T> List<T> mezclar(List<T> lista) {
        List<T> copia = new ArrayList<>(lista);
        Collections.shuffle(copia, aleatorio);
        return copia;
    }

    // ------------------------------------------------------------------ Utilidades de texto

    private static String limpiarElemento(String elemento) {
        return elemento.trim().replaceAll("[.;,]+$", "").trim();
    }

    private static String quitarPuntuacionFinal(String texto) {
        return texto.trim().replaceAll("[\\s.,;:!?…]+$", "");
    }

    private static String asegurarPunto(String texto) {
        String t = texto.trim();
        return t.isEmpty() || ".!?…".indexOf(t.charAt(t.length() - 1)) >= 0 ? t : t + ".";
    }

    private static String minusculaInicial(String texto) {
        if (texto.length() < 2 || Character.isUpperCase(texto.charAt(1))) {
            return texto;   // respeta siglas como "UML" o "IEEE"
        }
        return Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String mayusculaInicial(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static int contarPalabras(String texto) {
        String t = texto.trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    private static String normalizar(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT).replaceAll("[.\\s]+$", "");
    }

    // ------------------------------------------------------------------ Concepto interno

    /** Unidad de conocimiento extraída del texto (uso interno del simulador). */
    private static final class Concepto {
        enum Tipo { LISTA, ORDEN, CAUSA, DEFINICION, NUMERO, CLAVE }

        final Tipo tipo;
        final String pregunta;
        final String respuesta;
        final List<String> elementos;

        Concepto(Tipo tipo, String pregunta, String respuesta, List<String> elementos) {
            this.tipo = tipo;
            this.pregunta = pregunta;
            this.respuesta = respuesta;
            this.elementos = elementos == null ? Collections.<String>emptyList() : elementos;
        }
    }
}
