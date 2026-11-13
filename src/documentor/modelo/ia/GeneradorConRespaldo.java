package documentor.modelo.ia;

import documentor.modelo.ArchivoContexto;
import documentor.modelo.Flashcard;
import documentor.modelo.GeneradorMaterial;
import documentor.modelo.PreguntaExamen;
import java.util.List;
import java.util.function.Function;

/**
 * Decorador: intenta con el generador principal (OpenRouter) y, si falla
 * (sin Internet, límite diario, clave inválida...), usa el generador de respaldo
 * (simulador local) para que la aplicación nunca se quede sin funcionar.
 * El motivo se informa al usuario mediante {@link #consumirAviso()}.
 */
public class GeneradorConRespaldo implements GeneradorMaterial {

    private final GeneradorMaterial principal;
    private final GeneradorMaterial respaldo;
    private volatile String aviso;

    public GeneradorConRespaldo(GeneradorMaterial principal, GeneradorMaterial respaldo) {
        this.principal = principal;
        this.respaldo = respaldo;
    }

    @Override
    public List<Flashcard> generarFlashcards(String texto, int cantidad) {
        return conRespaldo(g -> g.generarFlashcards(texto, cantidad));
    }

    @Override
    public List<PreguntaExamen> generarExamen(String texto, int cantidad) {
        return conRespaldo(g -> g.generarExamen(texto, cantidad));
    }

    @Override
    public int calcularMaximo(String texto) {
        return principal.calcularMaximo(texto);
    }

    @Override
    public List<Flashcard> generarFlashcards(String texto, int cantidad, List<ArchivoContexto> contextos) {
        return conRespaldo(g -> g.generarFlashcards(texto, cantidad, contextos));
    }

    @Override
    public List<PreguntaExamen> generarExamen(String texto, int cantidad, List<ArchivoContexto> contextos) {
        return conRespaldo(g -> g.generarExamen(texto, cantidad, contextos));
    }

    @Override
    public int calcularMaximo(String texto, List<ArchivoContexto> contextos) {
        return principal.calcularMaximo(texto, contextos);
    }

    @Override
    public boolean leePdfCompletos() {
        return principal.leePdfCompletos();
    }

    private <T> List<T> conRespaldo(Function<GeneradorMaterial, List<T>> operacion) {
        try {
            return operacion.apply(principal);
        } catch (RuntimeException e) {
            aviso = "No se pudo usar la IA en línea, así que el material se generó con el simulador local.\n\n"
                    + "Motivo: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return operacion.apply(respaldo);
        }
    }

    @Override
    public String getNombre() {
        return principal.getNombre();
    }

    @Override
    public boolean esSimulado() {
        return false;
    }

    @Override
    public String consumirAviso() {
        String propio = aviso;
        aviso = null;
        String delPrincipal = principal.consumirAviso();
        if (propio != null && delPrincipal != null) {
            return delPrincipal + "\n\n" + propio;
        }
        return propio != null ? propio : delPrincipal;
    }
}
