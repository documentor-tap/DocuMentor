package documentor.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado de la aplicación (memoria temporal): documento actual, material generado,
 * posición de navegación y respuestas del examen. No conoce nada de Swing.
 */
public class SesionEstudio {

    /** Pantalla lógica en la que se encuentra el usuario. */
    public enum Modo { VACIO, FLASHCARDS, EXAMEN, RESULTADO }

    private Documento documento;
    private Modo modo = Modo.VACIO;

    private final List<Flashcard> listaFlashcards = new ArrayList<>();
    private int indiceFlashcard;
    private boolean respuestaVisible;

    private final List<PreguntaExamen> listaPreguntas = new ArrayList<>();
    private int indicePregunta;

    private final List<ArchivoContexto> listaContextos = new ArrayList<>();

    // ------------------------------------------------------------------ Documento

    /** Carga un nuevo documento y descarta el material del documento anterior. */
    public void cargarDocumento(Documento documento) {
        this.documento = documento;
        listaFlashcards.clear();
        listaPreguntas.clear();
        indiceFlashcard = 0;
        indicePregunta = 0;
        respuestaVisible = false;
        modo = Modo.VACIO;
    }

    public Documento getDocumento() {
        return documento;
    }

    public boolean tieneDocumento() {
        return documento != null && documento.tieneContenido();
    }

    public Modo getModo() {
        return modo;
    }

    // ------------------------------------------------------------------ Flashcards

    public void cargarFlashcards(List<Flashcard> nuevas) {
        listaFlashcards.clear();
        listaFlashcards.addAll(nuevas);
        indiceFlashcard = 0;
        respuestaVisible = false;
        modo = listaFlashcards.isEmpty() ? Modo.VACIO : Modo.FLASHCARDS;
    }

    public List<Flashcard> getListaFlashcards() {
        return Collections.unmodifiableList(listaFlashcards);
    }

    public Flashcard getFlashcardActual() {
        return listaFlashcards.isEmpty() ? null : listaFlashcards.get(indiceFlashcard);
    }

    public int getIndiceFlashcard() {
        return indiceFlashcard;
    }

    public int getTotalFlashcards() {
        return listaFlashcards.size();
    }

    public boolean isRespuestaVisible() {
        return respuestaVisible;
    }

    public void voltearFlashcard() {
        if (!listaFlashcards.isEmpty()) {
            respuestaVisible = !respuestaVisible;
        }
    }

    public boolean siguienteFlashcard() {
        if (indiceFlashcard < listaFlashcards.size() - 1) {
            indiceFlashcard++;
            respuestaVisible = false;
            return true;
        }
        return false;
    }

    public boolean anteriorFlashcard() {
        if (indiceFlashcard > 0) {
            indiceFlashcard--;
            respuestaVisible = false;
            return true;
        }
        return false;
    }

    public void editarFlashcardActual(String pregunta, String respuesta) {
        Flashcard actual = getFlashcardActual();
        if (actual == null) {
            return;
        }
        actual.setPregunta(pregunta);
        actual.setRespuesta(respuesta);
    }

    public void eliminarFlashcardActual() {
        if (listaFlashcards.isEmpty()) {
            return;
        }
        listaFlashcards.remove(indiceFlashcard);
        indiceFlashcard = Math.min(indiceFlashcard, Math.max(0, listaFlashcards.size() - 1));
        respuestaVisible = false;
        if (listaFlashcards.isEmpty()) {
            modo = Modo.VACIO;
        }
    }

    // ------------------------------------------------------------------ Examen

    public void cargarExamen(List<PreguntaExamen> nuevas) {
        listaPreguntas.clear();
        listaPreguntas.addAll(nuevas);
        indicePregunta = 0;
        modo = listaPreguntas.isEmpty() ? Modo.VACIO : Modo.EXAMEN;
    }

    public List<PreguntaExamen> getListaPreguntas() {
        return Collections.unmodifiableList(listaPreguntas);
    }

    public PreguntaExamen getPreguntaActual() {
        return listaPreguntas.isEmpty() ? null : listaPreguntas.get(indicePregunta);
    }

    public int getIndicePregunta() {
        return indicePregunta;
    }

    public int getTotalPreguntas() {
        return listaPreguntas.size();
    }

    public boolean esUltimaPregunta() {
        return indicePregunta >= listaPreguntas.size() - 1;
    }

    public boolean responderPreguntaActual(int indiceOpcion) {
        PreguntaExamen actual = getPreguntaActual();
        if (actual == null) {
            throw new IllegalStateException("No hay pregunta activa.");
        }
        return actual.responder(indiceOpcion);
    }

    /** Avanza a la siguiente pregunta; al pasar la última cambia el modo a RESULTADO. */
    public void avanzarPregunta() {
        if (listaPreguntas.isEmpty()) {
            return;
        }
        if (esUltimaPregunta()) {
            modo = Modo.RESULTADO;
        } else {
            indicePregunta++;
        }
    }

    public void editarPreguntaActual(String enunciado, List<String> opciones, int indiceCorrecto) {
        PreguntaExamen actual = getPreguntaActual();
        if (actual != null) {
            actual.actualizar(enunciado, opciones, indiceCorrecto);
        }
    }

    public void eliminarPreguntaActual() {
        if (listaPreguntas.isEmpty()) {
            return;
        }
        listaPreguntas.remove(indicePregunta);
        indicePregunta = Math.min(indicePregunta, Math.max(0, listaPreguntas.size() - 1));
        if (listaPreguntas.isEmpty()) {
            modo = Modo.VACIO;
        }
    }

    public void reiniciarExamen() {
        for (PreguntaExamen pregunta : listaPreguntas) {
            pregunta.reiniciarRespuesta();
        }
        indicePregunta = 0;
        modo = listaPreguntas.isEmpty() ? Modo.VACIO : Modo.EXAMEN;
    }

    public int contarAciertos() {
        int aciertos = 0;
        for (PreguntaExamen pregunta : listaPreguntas) {
            if (pregunta.esCorrecta()) {
                aciertos++;
            }
        }
        return aciertos;
    }

    public int contarSinResponder() {
        int sinResponder = 0;
        for (PreguntaExamen pregunta : listaPreguntas) {
            if (!pregunta.isConfirmada()) {
                sinResponder++;
            }
        }
        return sinResponder;
    }

    // ------------------------------------------------------------------ PDF de contexto (HU-18)

    /**
     * Agrega un PDF de contexto. Se conserva aunque se abra otro documento o se regenere material.
     * @throws IllegalArgumentException si se repite o se exceden los límites
     */
    public void agregarContexto(ArchivoContexto contexto) {
        if (listaContextos.size() >= ArchivoContexto.MAXIMO_ARCHIVOS) {
            throw new IllegalArgumentException("Solo se permiten " + ArchivoContexto.MAXIMO_ARCHIVOS
                    + " PDF de contexto. Quita alguno para agregar otro.");
        }
        for (ArchivoContexto existente : listaContextos) {
            if (existente.getArchivo().getAbsoluteFile().equals(contexto.getArchivo().getAbsoluteFile())) {
                throw new IllegalArgumentException("«" + contexto.getNombre() + "» ya está en el contexto.");
            }
        }
        long total = contexto.getTamanoBytes();
        for (ArchivoContexto existente : listaContextos) {
            total += existente.getTamanoBytes();
        }
        if (total > ArchivoContexto.MAXIMO_BYTES_TOTAL) {
            throw new IllegalArgumentException("Con «" + contexto.getNombre() + "» el contexto sumaría "
                    + ArchivoContexto.formatearTamano(total) + "; el máximo total es "
                    + ArchivoContexto.formatearTamano(ArchivoContexto.MAXIMO_BYTES_TOTAL) + ".");
        }
        listaContextos.add(contexto);
    }

    public void quitarContexto(int indice) {
        if (indice >= 0 && indice < listaContextos.size()) {
            listaContextos.remove(indice);
        }
    }

    public List<ArchivoContexto> getContextos() {
        return Collections.unmodifiableList(listaContextos);
    }

    public boolean tieneContextos() {
        return !listaContextos.isEmpty();
    }

    /** Hay algo de qué generar: un documento con texto o al menos un PDF de contexto. */
    public boolean tieneFuenteDeGeneracion() {
        return tieneDocumento() || tieneContextos();
    }

    // ------------------------------------------------------------------ Exportación

    /** Material que corresponde exportar según la pantalla activa, o null si no hay. */
    public TipoMaterial getMaterialExportable() {
        if (modo == Modo.FLASHCARDS && !listaFlashcards.isEmpty()) {
            return TipoMaterial.FLASHCARDS;
        }
        if ((modo == Modo.EXAMEN || modo == Modo.RESULTADO) && !listaPreguntas.isEmpty()) {
            return TipoMaterial.EXAMEN;
        }
        return null;
    }
}
