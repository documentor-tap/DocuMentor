package documentor.modelo.ia;

import documentor.modelo.GeneradorMaterial;
import documentor.modelo.SimuladorIA;

/**
 * Decide qué motor usar según la configuración:
 * con API key → OpenRouter (modelos gratis) con el simulador como respaldo;
 * sin API key → simulador local (funciona sin Internet).
 */
public final class FabricaGeneradores {

    private FabricaGeneradores() {
    }

    public static GeneradorMaterial crear(ConfiguracionIA configuracion) {
        if (configuracion == null || !configuracion.tieneClave()) {
            return new SimuladorIA();
        }
        return new GeneradorConRespaldo(new GeneradorOpenRouter(configuracion), new SimuladorIA());
    }
}
