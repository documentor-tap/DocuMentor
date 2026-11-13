package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BordeRedondeado;
import documentor.vista.componentes.CardLayoutAjustado;
import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoRadio;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.LineaDivisoria;
import documentor.vista.componentes.PanelRedondeado;
import documentor.vista.componentes.ScrollBarModerno;
import documentor.vista.componentes.TextoAjustable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/**
 * Estado 3 del CardLayout: pregunta de opción múltiple con retroalimentación.
 * Incluye un modo de edición en línea para el enunciado, las opciones y la respuesta correcta.
 */
public class ExamenView extends JPanel {

    private static final int NUMERO_OPCIONES = 4;
    private static final String MODO_RESPONDER = "RESPONDER";
    private static final String MODO_EDITAR = "EDITAR";

    private final JLabel lblModo;
    private final BotonEstilizado btnEditarPregunta;
    private final BotonEstilizado btnEliminarPregunta;
    private final TextoAjustable lblPregunta;
    private final ButtonGroup grpOpciones = new ButtonGroup();
    private final List<PanelOpcion> listaPanelesOpcion = new ArrayList<>();
    private final TextoAjustable lblRetroalimentacion;
    private final BotonEstilizado btnConfirmar;
    private final BotonEstilizado btnSiguiente;
    private final JLabel lblProgreso;

    // Edición en línea
    private final JTextArea txtEditarEnunciado;
    private final List<JTextField> listaTxtOpciones = new ArrayList<>();
    private final List<JRadioButton> listaRdbCorrecta = new ArrayList<>();
    private final ButtonGroup grpCorrecta = new ButtonGroup();
    private final BotonEstilizado btnGuardarPregunta;
    private final BotonEstilizado btnCancelarEdicion;
    private final CardLayout cardCuerpo = new CardLayoutAjustado();
    private final CardLayout cardPie = new CardLayoutAjustado();
    private final JPanel pnlCuerpoCambiante;
    private final JPanel pnlPieCambiante;
    private boolean editando;

    public ExamenView() {
        super(new BorderLayout(0, 16));
        setOpaque(false);

        // ---------- Encabezado (común a ambos modos)
        lblModo = conTexto(new JLabel("MODO EXAMEN"), p -> p.acento);
        lblModo.setFont(GestorTema.fuente(Font.BOLD, 13f)
                .deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.08f)));
        btnEditarPregunta = new BotonEstilizado("", new IconoVectorial(Tipo.LAPIZ, 20), Variante.FANTASMA);
        btnEditarPregunta.setToolTipText("Editar enunciado, opciones y respuesta correcta (también: doble clic en el enunciado)");
        btnEliminarPregunta = new BotonEstilizado("", new IconoVectorial(Tipo.PAPELERA, 20, p -> p.textoSecundario),
                Variante.FANTASMA);
        btnEliminarPregunta.setToolTipText("Eliminar esta pregunta del examen");

        JPanel pnlIconos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlIconos.setOpaque(false);
        pnlIconos.add(btnEditarPregunta);
        pnlIconos.add(btnEliminarPregunta);

        JPanel pnlEncabezado = new JPanel(new BorderLayout());
        pnlEncabezado.setOpaque(false);
        pnlEncabezado.add(lblModo, BorderLayout.WEST);
        pnlEncabezado.add(pnlIconos, BorderLayout.EAST);

        JPanel pnlSuperior = new JPanel();
        pnlSuperior.setOpaque(false);
        pnlSuperior.setLayout(new BoxLayout(pnlSuperior, BoxLayout.Y_AXIS));
        agregarAlineado(pnlSuperior, pnlEncabezado);
        pnlSuperior.add(Box.createVerticalStrut(12));
        agregarAlineado(pnlSuperior, new LineaDivisoria());
        pnlSuperior.add(Box.createVerticalStrut(18));

        // ---------- Modo responder: enunciado y opciones
        lblPregunta = conTexto(new TextoAjustable("", GestorTema.fuente(Font.BOLD, 19f), false), p -> p.texto);
        lblPregunta.setToolTipText("Doble clic para editar la pregunta");

        JPanel pnlCuerpo = new JPanel();
        pnlCuerpo.setOpaque(false);
        pnlCuerpo.setLayout(new BoxLayout(pnlCuerpo, BoxLayout.Y_AXIS));
        agregarAlineado(pnlCuerpo, lblPregunta);
        pnlCuerpo.add(Box.createVerticalStrut(16));
        for (int i = 0; i < NUMERO_OPCIONES; i++) {
            PanelOpcion opcion = new PanelOpcion((char) ('A' + i));
            grpOpciones.add(opcion.rdbOpcion);
            listaPanelesOpcion.add(opcion);
            agregarAlineado(pnlCuerpo, opcion);
            pnlCuerpo.add(Box.createVerticalStrut(10));
        }

        lblRetroalimentacion = conTexto(new TextoAjustable("", GestorTema.fuente(Font.BOLD, 14f), false),
                p -> p.textoSecundario);
        btnConfirmar = new BotonEstilizado("Confirmar", null, Variante.PRIMARIO);
        btnConfirmar.setToolTipText("Revisar si la opción elegida es correcta");
        btnSiguiente = new BotonEstilizado("Siguiente >", null, Variante.SECUNDARIO);
        btnSiguiente.setToolTipText("Pasar a la siguiente pregunta (puedes saltarla sin responder)");

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBotones.setOpaque(false);
        pnlBotones.add(btnConfirmar);
        pnlBotones.add(btnSiguiente);

        JPanel pnlPie = new JPanel(new BorderLayout(12, 0));
        pnlPie.setOpaque(false);
        pnlPie.setBorder(new EmptyBorder(12, 0, 0, 0));
        pnlPie.add(lblRetroalimentacion, BorderLayout.CENTER);
        pnlPie.add(pnlBotones, BorderLayout.EAST);

        // ---------- Modo editar
        txtEditarEnunciado = new JTextArea(3, 30);
        txtEditarEnunciado.setLineWrap(true);
        txtEditarEnunciado.setWrapStyleWord(true);
        estilizarCampo(txtEditarEnunciado, 16f);
        txtEditarEnunciado.setBorder(new EmptyBorder(8, 10, 8, 10));
        txtEditarEnunciado.setToolTipText("Texto de la pregunta");
        JScrollPane scrEnunciado = new JScrollPane(txtEditarEnunciado);
        scrEnunciado.setBorder(new BordeRedondeado(2, 2));
        scrEnunciado.setPreferredSize(new Dimension(300, 86));
        ScrollBarModerno.aplicar(scrEnunciado);

        JPanel pnlEdicion = new JPanel();
        pnlEdicion.setOpaque(false);
        pnlEdicion.setLayout(new BoxLayout(pnlEdicion, BoxLayout.Y_AXIS));
        agregarAlineado(pnlEdicion, etiqueta("Enunciado"));
        pnlEdicion.add(Box.createVerticalStrut(6));
        agregarAlineado(pnlEdicion, scrEnunciado);
        pnlEdicion.add(Box.createVerticalStrut(14));
        agregarAlineado(pnlEdicion, etiqueta("Opciones  (marca el círculo de la correcta)"));
        pnlEdicion.add(Box.createVerticalStrut(6));
        for (int i = 0; i < NUMERO_OPCIONES; i++) {
            char letra = (char) ('A' + i);
            JRadioButton rdbCorrecta = new JRadioButton();
            rdbCorrecta.setOpaque(false);
            rdbCorrecta.setIcon(new IconoRadio(false));
            rdbCorrecta.setSelectedIcon(new IconoRadio(true));
            rdbCorrecta.setToolTipText("Marcar la opción " + letra + " como la correcta");
            grpCorrecta.add(rdbCorrecta);
            listaRdbCorrecta.add(rdbCorrecta);

            JLabel lblLetra = conTexto(new JLabel(letra + ")"), p -> p.texto);
            lblLetra.setFont(GestorTema.fuente(Font.BOLD, 15f));
            JTextField txtOpcion = new JTextField();
            estilizarCampo(txtOpcion, 15f);
            txtOpcion.setBorder(new BordeRedondeado(7, 10));
            txtOpcion.setToolTipText("Texto de la opción " + letra);
            listaTxtOpciones.add(txtOpcion);

            JPanel pnlIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            pnlIzq.setOpaque(false);
            pnlIzq.add(rdbCorrecta);
            pnlIzq.add(lblLetra);
            JPanel pnlFila = new JPanel(new BorderLayout(6, 0));
            pnlFila.setOpaque(false);
            pnlFila.add(pnlIzq, BorderLayout.WEST);
            pnlFila.add(txtOpcion, BorderLayout.CENTER);
            pnlFila.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlFila.getPreferredSize().height));
            agregarAlineado(pnlEdicion, pnlFila);
            pnlEdicion.add(Box.createVerticalStrut(8));
        }
        JLabel lblAyuda = conTexto(new JLabel("Ctrl + Enter = guardar  ·  Esc = cancelar"), p -> p.textoSecundario);
        lblAyuda.setFont(GestorTema.fuente(Font.PLAIN, 12f));
        agregarAlineado(pnlEdicion, lblAyuda);

        btnGuardarPregunta = new BotonEstilizado("Guardar cambios", new IconoVectorial(Tipo.GUARDAR, 16,
                p -> Color.WHITE), Variante.PRIMARIO);
        btnGuardarPregunta.setToolTipText("Guardar la pregunta editada (Ctrl + Enter)");
        btnCancelarEdicion = new BotonEstilizado("Cancelar", null, Variante.SECUNDARIO);
        btnCancelarEdicion.setToolTipText("Descartar los cambios (Esc)");
        btnCancelarEdicion.addActionListener(e -> terminarEdicion());   // solo visual: no toca el modelo
        JPanel pnlPieEdicion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlPieEdicion.setOpaque(false);
        pnlPieEdicion.setBorder(new EmptyBorder(12, 0, 0, 0));
        pnlPieEdicion.add(btnCancelarEdicion);
        pnlPieEdicion.add(btnGuardarPregunta);

        // ---------- Ensamblado con CardLayout (responder / editar)
        pnlCuerpoCambiante = new JPanel(cardCuerpo);
        pnlCuerpoCambiante.setOpaque(false);
        pnlCuerpoCambiante.add(pnlCuerpo, MODO_RESPONDER);
        pnlCuerpoCambiante.add(pnlEdicion, MODO_EDITAR);
        pnlPieCambiante = new JPanel(cardPie);
        pnlPieCambiante.setOpaque(false);
        pnlPieCambiante.add(pnlPie, MODO_RESPONDER);
        pnlPieCambiante.add(pnlPieEdicion, MODO_EDITAR);

        PanelRedondeado pnlTarjeta = new PanelRedondeado(new BorderLayout(), 18, p -> p.superficie, p -> p.borde);
        pnlTarjeta.setBorder(new EmptyBorder(22, 38, 26, 38));
        pnlTarjeta.add(pnlSuperior, BorderLayout.NORTH);
        pnlTarjeta.add(pnlCuerpoCambiante, BorderLayout.CENTER);
        pnlTarjeta.add(pnlPieCambiante, BorderLayout.SOUTH);

        lblProgreso = conTexto(new JLabel("Pregunta 0 de 0", JLabel.CENTER), p -> p.texto);
        lblProgreso.setFont(GestorTema.fuente(Font.PLAIN, 15f));
        lblProgreso.setToolTipText("Avance del examen");

        add(pnlTarjeta, BorderLayout.CENTER);
        add(lblProgreso, BorderLayout.SOUTH);

        // Atajos del formulario de edición
        pnlEdicion.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "guardar");
        pnlEdicion.getActionMap().put("guardar", accion(() -> btnGuardarPregunta.doClick(0)));
        pnlEdicion.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        pnlEdicion.getActionMap().put("cancelar", accion(() -> btnCancelarEdicion.doClick(0)));
    }

    private static void agregarAlineado(JPanel contenedor, JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        contenedor.add(componente);
    }

    private static JLabel etiqueta(String texto) {
        JLabel lbl = conTexto(new JLabel(texto), p -> p.textoSecundario);
        lbl.setFont(GestorTema.fuente(Font.BOLD, 13f));
        return lbl;
    }

    private static void estilizarCampo(JTextComponent campo, float tamano) {
        conFondo(campo, p -> p.fondo);
        conTexto(campo, p -> p.texto);
        campo.setFont(GestorTema.fuente(Font.PLAIN, tamano));
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repintarBorde(campo);
            }

            @Override
            public void focusLost(FocusEvent e) {
                repintarBorde(campo);
            }
        });
    }

    private static void repintarBorde(JComponent campo) {
        java.awt.Container padre = campo.getParent();
        (padre != null && padre.getParent() instanceof JScrollPane ? (JComponent) padre.getParent() : campo).repaint();
    }

    private static AbstractAction accion(Runnable r) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    // ------------------------------------------------------------------ API para el Controlador

    /**
     * Muestra una pregunta. Si ya fue confirmada se dibuja con su retroalimentación.
     * @param indiceSeleccionado opción elegida por el usuario o -1
     */
    public void mostrarPregunta(String enunciado, List<String> opciones, int numero, int total,
                                boolean confirmada, int indiceSeleccionado, int indiceCorrecto) {
        terminarEdicion();
        lblPregunta.setTexto(enunciado);
        grpOpciones.clearSelection();
        for (int i = 0; i < NUMERO_OPCIONES; i++) {
            PanelOpcion panel = listaPanelesOpcion.get(i);
            panel.lblTextoOpcion.setTexto((char) ('A' + i) + ") " + (i < opciones.size() ? opciones.get(i) : ""));
            panel.rdbOpcion.setEnabled(!confirmada);
            panel.setCursor(Cursor.getPredefinedCursor(confirmada ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
            panel.establecerEstado(EstadoOpcion.NORMAL);
        }
        lblProgreso.setText("Pregunta " + numero + " de " + total);
        btnSiguiente.setText(numero >= total ? "Ver resultados" : "Siguiente >");

        if (confirmada) {
            if (indiceSeleccionado >= 0) {
                listaPanelesOpcion.get(indiceSeleccionado).rdbOpcion.setSelected(true);
            }
            listaPanelesOpcion.get(indiceCorrecto).establecerEstado(EstadoOpcion.CORRECTA);
            boolean acierto = indiceSeleccionado == indiceCorrecto;
            if (!acierto && indiceSeleccionado >= 0) {
                listaPanelesOpcion.get(indiceSeleccionado).establecerEstado(EstadoOpcion.INCORRECTA);
            }
            char letra = (char) ('A' + indiceCorrecto);
            lblRetroalimentacion.setTexto(acierto ? "¡Correcto! Bien hecho."
                    : "Incorrecto. La respuesta correcta es la " + letra + ").");
            conTexto(lblRetroalimentacion, acierto ? p -> p.exito : p -> p.peligro);
        } else {
            lblRetroalimentacion.setTexto("Selecciona una opción y pulsa Confirmar.");
            conTexto(lblRetroalimentacion, p -> p.textoSecundario);
        }
        btnConfirmar.setEnabled(false);
        revalidate();
        repaint();
    }

    /** Cambia al formulario de edición con los datos actuales de la pregunta. */
    public void iniciarEdicion(String enunciado, List<String> opciones, int indiceCorrecto) {
        editando = true;
        txtEditarEnunciado.setText(enunciado);
        for (int i = 0; i < NUMERO_OPCIONES; i++) {
            listaTxtOpciones.get(i).setText(i < opciones.size() ? opciones.get(i) : "");
            listaTxtOpciones.get(i).setCaretPosition(0);   // mostrar el inicio del texto
        }
        listaRdbCorrecta.get(Math.max(0, Math.min(NUMERO_OPCIONES - 1, indiceCorrecto))).setSelected(true);
        lblModo.setText("EDITANDO PREGUNTA");
        conTexto(lblModo, p -> p.advertencia);
        btnEditarPregunta.setEnabled(false);
        btnEliminarPregunta.setEnabled(false);
        cardCuerpo.show(pnlCuerpoCambiante, MODO_EDITAR);
        cardPie.show(pnlPieCambiante, MODO_EDITAR);
        GestorTema.aplicarTema(this);
        revalidate();
        txtEditarEnunciado.requestFocusInWindow();
    }

    /** Regresa al modo responder sin modificar nada. */
    public void terminarEdicion() {
        if (!editando) {
            return;
        }
        editando = false;
        lblModo.setText("MODO EXAMEN");
        conTexto(lblModo, p -> p.acento);
        btnEditarPregunta.setEnabled(true);
        btnEliminarPregunta.setEnabled(true);
        cardCuerpo.show(pnlCuerpoCambiante, MODO_RESPONDER);
        cardPie.show(pnlPieCambiante, MODO_RESPONDER);
        revalidate();
    }

    public boolean estaEditando() {
        return editando;
    }

    public String getEnunciadoEditado() {
        return txtEditarEnunciado.getText().trim();
    }

    public List<String> getOpcionesEditadas() {
        List<String> opciones = new ArrayList<>();
        for (JTextField txt : listaTxtOpciones) {
            opciones.add(txt.getText().trim());
        }
        return opciones;
    }

    public int getCorrectaEditada() {
        for (int i = 0; i < listaRdbCorrecta.size(); i++) {
            if (listaRdbCorrecta.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    public int getIndiceSeleccionado() {
        for (int i = 0; i < listaPanelesOpcion.size(); i++) {
            if (listaPanelesOpcion.get(i).rdbOpcion.isSelected()) {
                return i;
            }
        }
        return -1;
    }

    /** Resalta la opción elegida (antes de confirmar) y habilita Confirmar. */
    public void marcarSeleccion(int indice) {
        for (int i = 0; i < listaPanelesOpcion.size(); i++) {
            listaPanelesOpcion.get(i).establecerEstado(i == indice ? EstadoOpcion.SELECCIONADA : EstadoOpcion.NORMAL);
        }
        btnConfirmar.setEnabled(indice >= 0);
    }

    /** Componente sobre el que el Controlador escucha el doble clic para editar. */
    public JComponent getComponenteEnunciado() {
        return lblPregunta;
    }

    public List<JRadioButton> getRdbOpciones() {
        List<JRadioButton> radios = new ArrayList<>();
        for (PanelOpcion panel : listaPanelesOpcion) {
            radios.add(panel.rdbOpcion);
        }
        return radios;
    }

    public JButton getBtnConfirmar() {
        return btnConfirmar;
    }

    public JButton getBtnSiguiente() {
        return btnSiguiente;
    }

    public JButton getBtnEditarPregunta() {
        return btnEditarPregunta;
    }

    public JButton getBtnEliminarPregunta() {
        return btnEliminarPregunta;
    }

    public JButton getBtnGuardarPregunta() {
        return btnGuardarPregunta;
    }

    // ------------------------------------------------------------------ Caja de opción

    private enum EstadoOpcion { NORMAL, SELECCIONADA, CORRECTA, INCORRECTA }

    /** Caja redondeada con un JRadioButton y su texto ajustable. Clic en toda la caja = seleccionar. */
    private static final class PanelOpcion extends PanelRedondeado {

        private static final long serialVersionUID = 1L;
        final JRadioButton rdbOpcion;
        final TextoAjustable lblTextoOpcion;

        PanelOpcion(char letra) {
            super(new BorderLayout(12, 0), 10, p -> p.superficie, p -> p.borde);
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setToolTipText("Elegir la opción " + letra);

            rdbOpcion = new JRadioButton();
            rdbOpcion.setOpaque(false);
            rdbOpcion.setFocusPainted(false);
            rdbOpcion.setIcon(new IconoRadio(false));
            rdbOpcion.setSelectedIcon(new IconoRadio(true));
            rdbOpcion.setDisabledIcon(new IconoRadio(false));
            rdbOpcion.setDisabledSelectedIcon(new IconoRadio(true));
            rdbOpcion.setBorder(new EmptyBorder(0, 0, 0, 0));
            rdbOpcion.setToolTipText("Elegir la opción " + letra);

            lblTextoOpcion = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 16f), false),
                    p -> p.texto);

            JPanel pnlRadio = new JPanel(new BorderLayout());
            pnlRadio.setOpaque(false);
            pnlRadio.add(rdbOpcion, BorderLayout.NORTH);
            add(pnlRadio, BorderLayout.WEST);
            add(lblTextoOpcion, BorderLayout.CENTER);

            MouseAdapter seleccionar = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (rdbOpcion.isEnabled() && !rdbOpcion.isSelected()) {
                        rdbOpcion.doClick(0);
                    }
                }
            };
            addMouseListener(seleccionar);
            lblTextoOpcion.addMouseListener(seleccionar);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        void establecerEstado(EstadoOpcion estado) {
            switch (estado) {
                case SELECCIONADA:
                    setColorFondo(p -> GestorTema.mezclar(p.superficie, p.acento, 0.10f));
                    setColorBorde(p -> p.acento);
                    break;
                case CORRECTA:
                    setColorFondo(p -> GestorTema.mezclar(p.superficie, p.exito, 0.14f));
                    setColorBorde(p -> p.exito);
                    break;
                case INCORRECTA:
                    setColorFondo(p -> GestorTema.mezclar(p.superficie, p.peligro, 0.14f));
                    setColorBorde(p -> p.peligro);
                    break;
                case NORMAL:
                default:
                    setColorFondo(p -> p.superficie);
                    setColorBorde(p -> p.borde);
            }
        }
    }
}
