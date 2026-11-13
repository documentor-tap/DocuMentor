package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.LayoutCentrado;
import documentor.vista.componentes.PanelRedondeado;
import documentor.vista.componentes.TextoAjustable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * Panel derecho (60 %) con CardLayout:
 * VACIO → CARGA → FLASHCARDS | EXAMEN → RESULTADO.
 */
public class InteractiveView extends JPanel {

    public static final String TARJETA_VACIA = "VACIO";
    public static final String TARJETA_CARGA = "CARGA";
    public static final String TARJETA_FLASHCARDS = "FLASHCARDS";
    public static final String TARJETA_EXAMEN = "EXAMEN";
    public static final String TARJETA_RESULTADO = "RESULTADO";

    /** La tarjeta crece con el panel (al ampliar resultados) hasta este ancho. */
    private static final int ANCHO_MAXIMO_TARJETA = 1100;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pnlTarjetas;
    private final FlashcardView pnlFlashcards;
    private final ExamenView pnlExamen;

    private final JLabel lblTituloCarga;
    private final JLabel lblPasoCarga;
    private final JProgressBar prgGeneracion;

    private final JLabel lblPuntaje;
    private final JLabel lblPorcentaje;
    private final TextoAjustable lblMensajeResultado;
    private final BotonEstilizado btnRepetirExamen;

    private String tarjetaVisible = TARJETA_VACIA;

    public InteractiveView() {
        super(new BorderLayout());
        conFondo(this, p -> p.fondo);

        pnlTarjetas = new JPanel(cardLayout);
        pnlTarjetas.setOpaque(false);

        pnlFlashcards = new FlashcardView();
        pnlExamen = new ExamenView();

        // ---------- Estado 1: carga
        lblTituloCarga = conTexto(new JLabel("Generando material...", JLabel.CENTER), p -> p.texto);
        lblTituloCarga.setFont(GestorTema.fuente(Font.BOLD, 20f));
        prgGeneracion = new JProgressBar();
        prgGeneracion.setUI(new BasicProgressBarUI());   // respeta colores en cualquier Look&Feel
        prgGeneracion.setIndeterminate(true);
        prgGeneracion.setBorderPainted(false);
        prgGeneracion.setPreferredSize(new Dimension(380, 8));
        prgGeneracion.setMaximumSize(new Dimension(380, 8));
        conFondo(prgGeneracion, p -> p.borde);
        conTexto(prgGeneracion, p -> p.acento);
        lblPasoCarga = conTexto(new JLabel(" ", JLabel.CENTER), p -> p.textoSecundario);
        lblPasoCarga.setFont(GestorTema.fuente(Font.PLAIN, 14f));

        // ---------- Estado 4: resultado
        lblPuntaje = conTexto(new JLabel("0 / 0", JLabel.CENTER), p -> p.acento);
        lblPuntaje.setFont(GestorTema.fuente(Font.BOLD, 52f));
        lblPorcentaje = conTexto(new JLabel(" ", JLabel.CENTER), p -> p.texto);
        lblPorcentaje.setFont(GestorTema.fuente(Font.BOLD, 18f));
        lblMensajeResultado = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 16f), true),
                p -> p.textoSecundario);
        btnRepetirExamen = new BotonEstilizado("Repetir examen", new IconoVectorial(Tipo.VOLTEAR, 16,
                p -> java.awt.Color.WHITE), Variante.PRIMARIO);
        btnRepetirExamen.setToolTipText("Volver a responder el mismo examen desde la primera pregunta");
        prgGeneracion.setToolTipText("Generando material, espera un momento");

        pnlTarjetas.add(centrar(crearPanelVacio()), TARJETA_VACIA);
        pnlTarjetas.add(centrar(crearPanelCarga()), TARJETA_CARGA);
        pnlTarjetas.add(centrar(pnlFlashcards), TARJETA_FLASHCARDS);
        pnlTarjetas.add(centrar(pnlExamen), TARJETA_EXAMEN);
        pnlTarjetas.add(centrar(crearPanelResultado()), TARJETA_RESULTADO);
        add(pnlTarjetas, BorderLayout.CENTER);
    }

    private static JPanel centrar(JComponent contenido) {
        JPanel contenedor = new JPanel(new LayoutCentrado(ANCHO_MAXIMO_TARJETA, 36));
        contenedor.setOpaque(false);
        contenedor.add(contenido);
        return contenedor;
    }

    private static PanelRedondeado crearTarjeta() {
        PanelRedondeado tarjeta = new PanelRedondeado(null, 18, p -> p.superficie, p -> p.borde);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(new EmptyBorder(36, 44, 36, 44));
        return tarjeta;
    }

    private static void agregarCentrado(JPanel contenedor, JComponent componente) {
        componente.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(componente);
    }

    private JPanel crearPanelVacio() {
        PanelRedondeado tarjeta = crearTarjeta();
        JLabel lblIcono = new JLabel(new IconoVectorial(Tipo.LIBROS, 64));
        JLabel lblTitulo = conTexto(new JLabel("Comienza a estudiar con DocuMentor", JLabel.CENTER), p -> p.texto);
        lblTitulo.setFont(GestorTema.fuente(Font.BOLD, 22f));
        TextoAjustable lblInstrucciones = conTexto(new TextoAjustable(
                "1.  Pulsa «Abrir Documento» y elige un archivo .txt o .pdf.\n"
                        + "2.  Usa «Generar Flashcards» o «Generar Examen».\n"
                        + "3.  ¿Solo te interesa un fragmento? Resáltalo en el panel izquierdo "
                        + "y pulsa «Generar desde selección».\n"
                        + "4.  Opcional: con «Agregar PDF» adjunta PDF completos como contexto para la IA.\n"
                        + "5.  Edita o elimina el material y expórtalo a .txt o .csv.",
                GestorTema.fuente(Font.PLAIN, 15f), false), p -> p.textoSecundario);
        JLabel lblAtajos = conTexto(new JLabel("Atajos en flashcards: Espacio = voltear  ·  ← / → = navegar",
                JLabel.CENTER), p -> p.acento);
        lblAtajos.setFont(GestorTema.fuente(Font.PLAIN, 13f));

        agregarCentrado(tarjeta, lblIcono);
        tarjeta.add(Box.createVerticalStrut(18));
        agregarCentrado(tarjeta, lblTitulo);
        tarjeta.add(Box.createVerticalStrut(20));
        agregarCentrado(tarjeta, lblInstrucciones);
        tarjeta.add(Box.createVerticalStrut(20));
        agregarCentrado(tarjeta, lblAtajos);
        return tarjeta;
    }

    private JPanel crearPanelCarga() {
        PanelRedondeado tarjeta = crearTarjeta();
        tarjeta.setBorder(new EmptyBorder(48, 44, 48, 44));
        agregarCentrado(tarjeta, lblTituloCarga);
        tarjeta.add(Box.createVerticalStrut(22));
        agregarCentrado(tarjeta, prgGeneracion);
        tarjeta.add(Box.createVerticalStrut(16));
        agregarCentrado(tarjeta, lblPasoCarga);
        return tarjeta;
    }

    private JPanel crearPanelResultado() {
        PanelRedondeado tarjeta = crearTarjeta();
        JLabel lblTitulo = conTexto(new JLabel("RESULTADO DEL EXAMEN", JLabel.CENTER), p -> p.textoSecundario);
        lblTitulo.setFont(GestorTema.fuente(Font.BOLD, 13f));
        JPanel pnlBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlBoton.setOpaque(false);
        pnlBoton.add(btnRepetirExamen);

        agregarCentrado(tarjeta, lblTitulo);
        tarjeta.add(Box.createVerticalStrut(14));
        agregarCentrado(tarjeta, lblPuntaje);
        tarjeta.add(Box.createVerticalStrut(4));
        agregarCentrado(tarjeta, lblPorcentaje);
        tarjeta.add(Box.createVerticalStrut(14));
        agregarCentrado(tarjeta, lblMensajeResultado);
        tarjeta.add(Box.createVerticalStrut(22));
        agregarCentrado(tarjeta, pnlBoton);
        return tarjeta;
    }

    // ------------------------------------------------------------------ Cambios de estado

    private void mostrar(String tarjeta) {
        tarjetaVisible = tarjeta;
        cardLayout.show(pnlTarjetas, tarjeta);
    }

    public void mostrarVacio() {
        mostrar(TARJETA_VACIA);
    }

    public void mostrarCarga(String titulo, String paso) {
        lblTituloCarga.setText(titulo);
        lblPasoCarga.setText(paso);
        mostrar(TARJETA_CARGA);
    }

    public void actualizarPasoCarga(String paso) {
        lblPasoCarga.setText(paso);
    }

    public void mostrarFlashcards() {
        mostrar(TARJETA_FLASHCARDS);
    }

    public void mostrarExamen() {
        mostrar(TARJETA_EXAMEN);
    }

    public void mostrarResultado(int aciertos, int total, int sinResponder) {
        int porcentaje = total == 0 ? 0 : Math.round(aciertos * 100f / total);
        lblPuntaje.setText(aciertos + " / " + total);
        lblPorcentaje.setText(porcentaje + " % de aciertos");
        String mensaje;
        if (porcentaje >= 90) {
            mensaje = "¡Excelente! Dominas el contenido del documento.";
        } else if (porcentaje >= 70) {
            mensaje = "¡Buen trabajo! Repasa los detalles que fallaste.";
        } else if (porcentaje >= 50) {
            mensaje = "Vas por buen camino. Te conviene repasar con las flashcards.";
        } else {
            mensaje = "Te recomendamos volver a leer el documento y practicar con las flashcards.";
        }
        if (sinResponder > 0) {
            mensaje += "\n(" + sinResponder + (sinResponder == 1 ? " pregunta quedó" : " preguntas quedaron")
                    + " sin responder.)";
        }
        lblMensajeResultado.setTexto(mensaje);
        mostrar(TARJETA_RESULTADO);
    }

    public String getTarjetaVisible() {
        return tarjetaVisible;
    }

    public FlashcardView getFlashcardView() {
        return pnlFlashcards;
    }

    public ExamenView getExamenView() {
        return pnlExamen;
    }

    public JButton getBtnRepetirExamen() {
        return btnRepetirExamen;
    }
}
