package documentor.modelo;

/**
 * Tarjeta de repaso: una pregunta en el anverso y su respuesta en el reverso.
 * Es mutable porque el usuario puede editarla desde la interfaz.
 */
public class Flashcard {

    private String pregunta;
    private String respuesta;

    public Flashcard(String pregunta, String respuesta) {
        setPregunta(pregunta);
        setRespuesta(respuesta);
    }

    public String getPregunta() {
        return pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public final void setPregunta(String pregunta) {
        this.pregunta = validarTexto(pregunta, "La pregunta");
    }

    public final void setRespuesta(String respuesta) {
        this.respuesta = validarTexto(respuesta, "La respuesta");
    }

    private static String validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacía.");
        }
        return texto.trim();
    }

    @Override
    public String toString() {
        return "Flashcard{pregunta='" + pregunta + "', respuesta='" + respuesta + "'}";
    }
}
