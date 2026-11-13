package documentor.modelo;

/**
 * Reglas para decidir cuántas preguntas/flashcards tiene sentido pedir según
 * la cantidad de información del texto (evita pedir 20 preguntas de un párrafo).
 */
public final class LimitesGeneracion {

    /** Tope absoluto por generación (también protege el límite de tokens del modelo). */
    public static final int MAXIMO_ABSOLUTO = 25;
    /** Aproximadamente una pregunta por cada 20 palabras de texto. */
    public static final int PALABRAS_POR_PREGUNTA = 20;
    private static final int PREDETERMINADO = 8;

    private LimitesGeneracion() {
    }

    public static int contarPalabras(String texto) {
        if (texto == null) {
            return 0;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? 0 : recortado.split("\\s+").length;
    }

    /** Máximo permitido según la extensión del texto (entre 1 y {@link #MAXIMO_ABSOLUTO}). */
    public static int maximoPorExtension(String texto) {
        return maximoPorPalabras(contarPalabras(texto));
    }

    /** Máximo permitido para una cantidad de palabras (0 si no hay texto). */
    public static int maximoPorPalabras(int palabras) {
        if (palabras <= 0) {
            return 0;
        }
        int maximo = (int) Math.ceil(palabras / (double) PALABRAS_POR_PREGUNTA);
        return Math.max(1, Math.min(MAXIMO_ABSOLUTO, maximo));
    }

    /** Valor inicial sugerido en el selector de cantidad. */
    public static int predeterminado(int maximo) {
        return Math.max(1, Math.min(PREDETERMINADO, maximo));
    }

    /** Ajusta una cantidad pedida al rango válido [1, maximo]. */
    public static int ajustar(int cantidad, int maximo) {
        return Math.max(1, Math.min(cantidad, Math.max(1, maximo)));
    }
}
