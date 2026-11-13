package documentor.modelo;

import java.util.List;

/**
 * Contrato del "motor de IA". La aplicación sólo depende de esta interfaz,
 * así que el simulador local y el cliente de OpenRouter son intercambiables
 * sin tocar la Vista.
 *
 * <p>Las implementaciones pueden ser lentas: el Controlador siempre las invoca
 * desde un {@code SwingWorker}, nunca desde el hilo de eventos de Swing.</p>
 */
public interface GeneradorMaterial {

    /** Genera hasta {@code cantidad} flashcards a partir del texto. */
    List<Flashcard> generarFlashcards(String texto, int cantidad);

    /** Genera hasta {@code cantidad} preguntas de opción múltiple a partir del texto. */
    List<PreguntaExamen> generarExamen(String texto, int cantidad);

    /**
     * Cantidad máxima razonable de elementos para este texto.
     * Por defecto depende de su extensión (ver {@link LimitesGeneracion}).
     */
    default int calcularMaximo(String texto) {
        return LimitesGeneracion.maximoPorExtension(texto);
    }

    // ------------------------------------------------------------------ HU-18: PDF de contexto

    /**
     * Genera flashcards usando además PDF "puros" como contexto. Por defecto (simulador)
     * se usa el texto que se pudo extraer localmente de esos PDF; la IA en línea los envía
     * completos.
     */
    default List<Flashcard> generarFlashcards(String texto, int cantidad, List<ArchivoContexto> contextos) {
        return generarFlashcards(ArchivoContexto.textoCombinado(texto, contextos), cantidad);
    }

    /** Igual que {@link #generarFlashcards(String, int, List)} pero para el examen. */
    default List<PreguntaExamen> generarExamen(String texto, int cantidad, List<ArchivoContexto> contextos) {
        return generarExamen(ArchivoContexto.textoCombinado(texto, contextos), cantidad);
    }

    /** Máximo de elementos considerando también los PDF de contexto. */
    default int calcularMaximo(String texto, List<ArchivoContexto> contextos) {
        return calcularMaximo(ArchivoContexto.textoCombinado(texto, contextos));
    }

    /** {@code true} si el motor envía los PDF completos (sin depender de la extracción local). */
    default boolean leePdfCompletos() {
        return false;
    }

    /** Nombre legible del motor (se muestra en el título de la ventana y en la pantalla de carga). */
    default String getNombre() {
        return "Simulador local";
    }

    /** {@code true} si el motor es local/instantáneo y el Controlador debe simular la latencia. */
    default boolean esSimulado() {
        return true;
    }

    /**
     * Devuelve (y borra) un aviso de la última generación para mostrarlo al usuario,
     * por ejemplo "el texto se recortó" o "se usó el generador local de respaldo".
     * @return el aviso o {@code null} si no hay nada que informar.
     */
    default String consumirAviso() {
        return null;
    }
}
