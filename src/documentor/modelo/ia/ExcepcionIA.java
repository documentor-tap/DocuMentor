package documentor.modelo.ia;

/** Error al consultar el servicio de IA, con un mensaje apto para mostrarse al usuario. */
public class ExcepcionIA extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Si es {@code false}, no tiene sentido reintentar con otro modelo (p. ej. clave inválida). */
    private final boolean reintentable;

    public ExcepcionIA(String mensaje, boolean reintentable) {
        super(mensaje);
        this.reintentable = reintentable;
    }

    public ExcepcionIA(String mensaje, boolean reintentable, Throwable causa) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    public boolean isReintentable() {
        return reintentable;
    }
}
