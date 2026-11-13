package documentor;

import documentor.controlador.ControladorPrincipal;
import documentor.modelo.ExportadorMaterial;
import documentor.modelo.LectorDocumentos;
import documentor.modelo.SesionEstudio;
import documentor.modelo.ia.ConfiguracionIA;
import documentor.vista.VentanaPrincipal;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada: ensambla Modelo, Vista y Controlador en el hilo de eventos de Swing. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        // Respeta el proxy configurado en Windows (redes escolares/empresariales)
        System.setProperty("java.net.useSystemProxies", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                // Look & Feel nativo para los diálogos (JFileChooser, JOptionPane);
                // el resto de la interfaz se pinta con GestorTema.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Si falla se usa el Look & Feel por defecto (Metal)
            }

            // La configuración (API key) se lee de documentor.properties; el Controlador
            // decide el motor (OpenRouter o simulador) y puede cambiarlo en caliente.
            ConfiguracionIA configuracion = ConfiguracionIA.cargar(Main.class);
            VentanaPrincipal vista = new VentanaPrincipal();
            ControladorPrincipal controlador = new ControladorPrincipal(
                    vista, new SesionEstudio(), configuracion, new LectorDocumentos(), new ExportadorMaterial());
            controlador.iniciar();
        });
    }
}
