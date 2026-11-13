package documentor.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pregunta de opción múltiple (4 opciones, una correcta).
 * Además del contenido guarda el estado de la respuesta del usuario.
 */
public class PreguntaExamen {

    public static final int NUMERO_OPCIONES = 4;

    private String enunciado;
    private final List<String> opciones = new ArrayList<>();
    private int indiceCorrecto;

    private int indiceSeleccionado = -1;
    private boolean confirmada;

    public PreguntaExamen(String enunciado, List<String> opciones, int indiceCorrecto) {
        actualizar(enunciado, opciones, indiceCorrecto);
    }

    /** Reemplaza el contenido de la pregunta (edición) y reinicia la respuesta del usuario. */
    public final void actualizar(String enunciado, List<String> opciones, int indiceCorrecto) {
        if (enunciado == null || enunciado.trim().isEmpty()) {
            throw new IllegalArgumentException("El enunciado no puede estar vacío.");
        }
        if (opciones == null || opciones.size() != NUMERO_OPCIONES) {
            throw new IllegalArgumentException("La pregunta debe tener exactamente " + NUMERO_OPCIONES + " opciones.");
        }
        for (String opcion : opciones) {
            if (opcion == null || opcion.trim().isEmpty()) {
                throw new IllegalArgumentException("Ninguna opción puede quedar vacía.");
            }
        }
        if (indiceCorrecto < 0 || indiceCorrecto >= NUMERO_OPCIONES) {
            throw new IllegalArgumentException("Índice de respuesta correcta fuera de rango.");
        }
        this.enunciado = enunciado.trim();
        this.opciones.clear();
        for (String opcion : opciones) {
            this.opciones.add(opcion.trim());
        }
        this.indiceCorrecto = indiceCorrecto;
        reiniciarRespuesta();
    }

    /**
     * Registra la respuesta del usuario.
     * @return {@code true} si la opción elegida es la correcta.
     */
    public boolean responder(int indice) {
        if (indice < 0 || indice >= NUMERO_OPCIONES) {
            throw new IllegalArgumentException("Opción inválida: " + indice);
        }
        if (confirmada) {
            throw new IllegalStateException("La pregunta ya fue respondida.");
        }
        indiceSeleccionado = indice;
        confirmada = true;
        return esCorrecta();
    }

    public final void reiniciarRespuesta() {
        indiceSeleccionado = -1;
        confirmada = false;
    }

    public boolean esCorrecta() {
        return confirmada && indiceSeleccionado == indiceCorrecto;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public List<String> getOpciones() {
        return Collections.unmodifiableList(opciones);
    }

    public int getIndiceCorrecto() {
        return indiceCorrecto;
    }

    public String getOpcionCorrecta() {
        return opciones.get(indiceCorrecto);
    }

    public int getIndiceSeleccionado() {
        return indiceSeleccionado;
    }

    public boolean isConfirmada() {
        return confirmada;
    }

    /** Convierte 0..3 en A..D. */
    public static char letraOpcion(int indice) {
        return (char) ('A' + indice);
    }
}
