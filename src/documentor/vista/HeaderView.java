package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Barra superior: título (izquierda); ampliar resultados, exportar, abrir documento y
 * botón de configuración (derecha). El cambio de tema vive en el panel de configuración.
 */
public class HeaderView extends JPanel {

    private final JLabel lblTitulo;
    private final BotonEstilizado btnAmpliarResultados;
    private final BotonEstilizado btnExportar;
    private final BotonEstilizado btnAbrirDocumento;
    private final BotonEstilizado btnConfiguracion;

    public HeaderView() {
        super(new BorderLayout());
        conFondo(this, p -> p.superficie);
        setBorder(new EmptyBorder(12, 24, 12, 18));

        lblTitulo = conTexto(new JLabel("DocuMentor", new IconoVectorial(Tipo.LIBROS, 24), JLabel.LEFT), p -> p.acento);
        lblTitulo.setFont(GestorTema.fuente(Font.BOLD, 22f));
        lblTitulo.setIconTextGap(10);
        lblTitulo.setToolTipText("DocuMentor: convierte tus documentos en flashcards y exámenes");

        btnAmpliarResultados = new BotonEstilizado("", null, Variante.FANTASMA);
        actualizarBotonAmpliar(false);

        btnExportar = new BotonEstilizado("Exportar", new IconoVectorial(Tipo.EXPORTAR, 16), Variante.SECUNDARIO);
        btnExportar.setToolTipText("Guardar las flashcards o el examen visibles como .txt o .csv");
        btnExportar.setEnabled(false);

        btnAbrirDocumento = new BotonEstilizado("Abrir Documento", new IconoVectorial(Tipo.CARPETA, 18),
                Variante.PRIMARIO);
        btnAbrirDocumento.setToolTipText("Abrir un archivo .txt o .pdf para estudiar");

        btnConfiguracion = new BotonEstilizado("", new IconoVectorial(Tipo.ENGRANE, 20, p -> p.textoSecundario),
                Variante.FANTASMA);
        btnConfiguracion.setToolTipText("Configuración: modo claro/oscuro y API key de OpenRouter");

        JPanel pnlIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        pnlIzquierda.setOpaque(false);
        pnlIzquierda.add(lblTitulo);

        JPanel pnlDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlDerecha.setOpaque(false);
        pnlDerecha.add(btnAmpliarResultados);
        pnlDerecha.add(btnExportar);
        pnlDerecha.add(btnAbrirDocumento);
        pnlDerecha.add(btnConfiguracion);

        add(pnlIzquierda, BorderLayout.WEST);
        add(pnlDerecha, BorderLayout.EAST);
    }

    /** Muestra la acción disponible según si los resultados ocupan toda la ventana. */
    public final void actualizarBotonAmpliar(boolean resultadosAmpliados) {
        btnAmpliarResultados.setText(resultadosAmpliados ? "Mostrar documento" : "Ampliar resultados");
        btnAmpliarResultados.setIcon(new IconoVectorial(resultadosAmpliados ? Tipo.CONTRAER : Tipo.EXPANDIR, 16));
        btnAmpliarResultados.setToolTipText(resultadosAmpliados
                ? "Volver a mostrar el panel del documento"
                : "Ocultar el documento para que los resultados ocupen toda la ventana "
                        + "(también puedes arrastrar el divisor o darle doble clic)");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(GestorTema.actual().borde);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }

    public JButton getBtnAmpliarResultados() {
        return btnAmpliarResultados;
    }

    public JButton getBtnExportar() {
        return btnExportar;
    }

    public JButton getBtnAbrirDocumento() {
        return btnAbrirDocumento;
    }

    public JButton getBtnConfiguracion() {
        return btnConfiguracion;
    }
}
