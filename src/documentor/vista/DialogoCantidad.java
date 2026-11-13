package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.SliderModerno;
import documentor.vista.componentes.TextoAjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Mini menú que pregunta cuántas flashcards/preguntas generar antes de empezar.
 * El máximo lo calcula el Modelo según la información del texto; esta vista solo lo respeta.
 */
public final class DialogoCantidad extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JSlider sldCantidad;
    private final JLabel lblValor;
    private Integer resultado;

    private DialogoCantidad(Window padre, String sustantivo, int maximo, int predeterminado, String detalle) {
        super(padre, "Cantidad a generar", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel pnlRaiz = conFondo(new JPanel(new BorderLayout(0, 18)), p -> p.superficie);
        pnlRaiz.setBorder(new EmptyBorder(22, 26, 20, 26));

        JLabel lblTitulo = conTexto(new JLabel("¿Cuántas " + sustantivo + " quieres generar?"), p -> p.texto);
        lblTitulo.setFont(GestorTema.fuente(Font.BOLD, 18f));

        lblValor = conTexto(new JLabel(String.valueOf(predeterminado), JLabel.CENTER), p -> p.acento);
        lblValor.setFont(GestorTema.fuente(Font.BOLD, 40f));
        lblValor.setPreferredSize(new Dimension(80, 50));

        sldCantidad = SliderModerno.crear(1, maximo, predeterminado);
        sldCantidad.setToolTipText("Arrastra o usa las flechas del teclado (máximo " + maximo + ")");
        sldCantidad.setPreferredSize(new Dimension(300, 30));
        sldCantidad.addChangeListener(e -> lblValor.setText(String.valueOf(sldCantidad.getValue())));
        sldCantidad.setEnabled(maximo > 1);

        BotonEstilizado btnMenos = new BotonEstilizado("", new IconoVectorial(Tipo.MENOS, 16), Variante.FANTASMA);
        btnMenos.setToolTipText("Una menos");
        btnMenos.addActionListener(e -> sldCantidad.setValue(sldCantidad.getValue() - 1));
        BotonEstilizado btnMas = new BotonEstilizado("", new IconoVectorial(Tipo.MAS, 16), Variante.FANTASMA);
        btnMas.setToolTipText("Una más");
        btnMas.addActionListener(e -> sldCantidad.setValue(sldCantidad.getValue() + 1));

        JPanel pnlSelector = new JPanel(new BorderLayout(10, 0));
        pnlSelector.setOpaque(false);
        JPanel pnlSlider = new JPanel(new BorderLayout(4, 0));
        pnlSlider.setOpaque(false);
        pnlSlider.add(btnMenos, BorderLayout.WEST);
        pnlSlider.add(sldCantidad, BorderLayout.CENTER);
        pnlSlider.add(btnMas, BorderLayout.EAST);
        pnlSelector.add(lblValor, BorderLayout.WEST);
        pnlSelector.add(pnlSlider, BorderLayout.CENTER);

        TextoAjustable lblDetalle = conTexto(new TextoAjustable(detalle + "\nMínimo 1 · máximo " + maximo + ".",
                GestorTema.fuente(Font.PLAIN, 13f), false), p -> p.textoSecundario);

        JPanel pnlCentro = new JPanel();
        pnlCentro.setOpaque(false);
        pnlCentro.setLayout(new BoxLayout(pnlCentro, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{pnlSelector, lblDetalle}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        pnlCentro.add(pnlSelector);
        pnlCentro.add(Box.createVerticalStrut(12));
        pnlCentro.add(lblDetalle);

        BotonEstilizado btnCancelar = new BotonEstilizado("Cancelar", null, Variante.SECUNDARIO);
        btnCancelar.setToolTipText("No generar nada (Esc)");
        btnCancelar.addActionListener(e -> dispose());
        BotonEstilizado btnGenerar = new BotonEstilizado("Generar", new IconoVectorial(Tipo.RAYO, 16), Variante.PRIMARIO);
        btnGenerar.setToolTipText("Generar con la cantidad elegida (Enter)");
        btnGenerar.addActionListener(e -> {
            resultado = sldCantidad.getValue();
            dispose();
        });

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBotones.setOpaque(false);
        pnlBotones.add(btnCancelar);
        pnlBotones.add(btnGenerar);

        pnlRaiz.add(lblTitulo, BorderLayout.NORTH);
        pnlRaiz.add(pnlCentro, BorderLayout.CENTER);
        pnlRaiz.add(pnlBotones, BorderLayout.SOUTH);
        setContentPane(pnlRaiz);

        // Enter = Generar, Esc = Cancelar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "generar");
        getRootPane().getActionMap().put("generar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                btnGenerar.doClick(0);
            }
        });

        GestorTema.aplicarTema(pnlRaiz);
        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(padre);
    }

    /**
     * Muestra el diálogo y espera la respuesta.
     * @param sustantivo  "flashcards" o "preguntas"
     * @param maximo      máximo permitido según el texto (≥ 1)
     * @param predeterminado valor inicial
     * @param detalle     explicación del límite
     * @return la cantidad elegida o {@code null} si se canceló
     */
    public static Integer preguntar(Component padre, String sustantivo, int maximo, int predeterminado, String detalle) {
        Window ventana = padre instanceof Window ? (Window) padre : javax.swing.SwingUtilities.getWindowAncestor(padre);
        DialogoCantidad dialogo = new DialogoCantidad(ventana, sustantivo, Math.max(1, maximo),
                Math.max(1, Math.min(predeterminado, maximo)), detalle);
        dialogo.setVisible(true);   // modal: bloquea hasta que se cierra
        return dialogo.resultado;
    }
}
