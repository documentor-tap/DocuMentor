package documentor.modelo;

/** Tipos de material de estudio que puede producir el generador. */
public enum TipoMaterial {
    FLASHCARDS("flashcards"),
    EXAMEN("examen");

    private final String sufijoArchivo;

    TipoMaterial(String sufijoArchivo) {
        this.sufijoArchivo = sufijoArchivo;
    }

    public String getSufijoArchivo() {
        return sufijoArchivo;
    }
}
