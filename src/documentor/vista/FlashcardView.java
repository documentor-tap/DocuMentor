package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BordeRedondeado;
import documentor.vista.componentes.CardLayoutAjustado;
import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.LineaDivisoria;
import documentor.vista.componentes.PanelRedondeado;
import documentor.vista.componentes.ScrollBarModerno;
import documentor.vista.componentes.TextoAjustable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Estado 2 del CardLayout: tarjeta de repaso con navegación inferior.
 * Tiene un modo de edición en línea: el texto visible se convierte en un área editable.
 */
public class FlashcardView extends JPanel {

    private static final String MODO_LECTURA = "LECTURA";
    private static final String MODO_EDICION = "EDICION";

    private final JLabel lblModo;
    private final BotonEstilizado btnEditarTarjeta;
    private final BotonEstilizado btnEliminarTarjeta;
    private final TextoAjustable lblContenidoTarjeta;
    private final JTextArea txtEdicionTarjeta;
    private final BotonEstilizado btnVoltear;
    private final BotonEstilizado btnGuardarEdicion;
    private final BotonEstilizado btnCancelarEdicion;
    private final BotonEstilizado btnAnterior;
    private final BotonEstilizado btnSiguiente;
    private final JLabel lblContador;
    private final CardLayout cardContenido = new CardLayoutAjustado();
    private final CardLayout cardAcciones = new CardLayoutAjustado();
    private final JPanel pnlContenido;
    private final JPanel pnlAcciones;
    private boolean editando;
    private boolean mostrandoRespuesta;
    private int numeroActual;
    private int totalActual;

    public FlashcardView() {
        super(new BorderLayout(0, 22));
        setOpaque(false);
        setFocusable(true);   // recibe el foco para que los atajos de teclado funcionen

        // ---------- Encabezado de la tarjeta
        lblModo = new JLabel();
        lblModo.setFont(GestorTema.fuente(Font.BOLD, 13f)
                .deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.08f)));

        btnEditarTarjeta = new BotonEstilizado("", new IconoVectorial(Tipo.LAPIZ, 20), Variante.FANTASMA);
        btnEditarTarjeta.setToolTipText("Editar el texto visible de la tarjeta (también: doble clic sobre el texto)");
        btnEliminarTarjeta = new BotonEstilizado("", new IconoVectorial(Tipo.PAPELERA, 20, p -> p.textoSecundario),
                Variante.FANTASMA);
        btnEliminarTarjeta.setToolTipText("Eliminar esta flashcard");

        JPanel pnlIconos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlIconos.setOpaque(false);
        pnlIconos.add(btnEditarTarjeta);
        pnlIconos.add(btnEliminarTarjeta);

        JPanel pnlEncabezado = new JPanel(new BorderLayout());
        pnlEncabezado.setOpaque(false);
        pnlEncabezado.add(lblModo, BorderLayout.WEST);
        pnlEncabezado.add(pnlIconos, BorderLayout.EAST);

        JPanel pnlSuperior = new JPanel();
        pnlSuperior.setOpaque(false);
        pnlSuperior.setLayout(new BoxLayout(pnlSuperior, BoxLayout.Y_AXIS));
        pnlEncabezado.setAlignmentX(LEFT_ALIGNMENT);
        pnlSuperior.add(pnlEncabezado);
        pnlSuperior.add(Box.createVerticalStrut(12));
        LineaDivisoria linea = new LineaDivisoria();
        linea.setAlignmentX(LEFT_ALIGNMENT);
        pnlSuperior.add(linea);

        // ---------- Contenido: lectura (texto centrado) o edición (área de texto)
        lblContenidoTarjeta = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 22f), true),
                p -> p.texto);
        lblContenidoTarjeta.setBorder(new EmptyBorder(28, 10, 28, 10));
        lblContenidoTarjeta.setAlturaMinima(190);
        lblContenidoTarjeta.setToolTipText("Doble clic para editar este texto");

        txtEdicionTarjeta = new JTextArea(5, 30);
        conFondo(txtEdicionTarjeta, p -> p.fondo);
        conTexto(txtEdicionTarjeta, p -> p.texto);
        txtEdicionTarjeta.setFont(GestorTema.fuente(Font.PLAIN, 18f));
        txtEdicionTarjeta.setLineWrap(true);
        txtEdicionTarjeta.setWrapStyleWord(true);
        txtEdicionTarjeta.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scrEdicion = new JScrollPane(txtEdicionTarjeta);
        scrEdicion.setBorder(new BordeRedondeado(2, 2));
        scrEdicion.setPreferredSize(new Dimension(300, 190));
        ScrollBarModerno.aplicar(scrEdicion);
        JLabel lblAyudaEdicion = conTexto(new JLabel("Ctrl + Enter = guardar  ·  Esc = cancelar"), p -> p.textoSecundario);
        lblAyudaEdicion.setFont(GestorTema.fuente(Font.PLAIN, 12f));
        JPanel pnlEdicion = new JPanel(new BorderLayout(0, 8));
        pnlEdicion.setOpaque(false);
        pnlEdicion.setBorder(new EmptyBorder(18, 0, 18, 0));
        pnlEdicion.add(scrEdicion, BorderLayout.CENTER);
        pnlEdicion.add(lblAyudaEdicion, BorderLayout.SOUTH);

        pnlContenido = new JPanel(cardContenido);
        pnlContenido.setOpaque(false);
        pnlContenido.add(lblContenidoTarjeta, MODO_LECTURA);
        pnlContenido.add(pnlEdicion, MODO_EDICION);

        // ---------- Acciones inferiores: voltear, o guardar/cancelar al editar
        btnVoltear = new BotonEstilizado("Voltear Tarjeta", new IconoVectorial(Tipo.VOLTEAR, 16, p -> Color.WHITE),
                Variante.PRIMARIO);
        btnVoltear.setToolTipText("Mostrar el otro lado de la tarjeta (Barra espaciadora)");
        btnGuardarEdicion = new BotonEstilizado("Guardar cambios", new IconoVectorial(Tipo.GUARDAR, 16,
                p -> Color.WHITE), Variante.PRIMARIO);
        btnGuardarEdicion.setToolTipText("Guardar el texto editado (Ctrl + Enter)");
        btnCancelarEdicion = new BotonEstilizado("Cancelar", null, Variante.SECUNDARIO);
        btnCancelarEdicion.setToolTipText("Descartar los cambios (Esc)");
        btnCancelarEdicion.addActionListener(e -> terminarEdicion());   // solo visual: no toca el modelo

        JPanel pnlBotonesEdicion = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlBotonesEdicion.setOpaque(false);
        pnlBotonesEdicion.add(btnCancelarEdicion);
        pnlBotonesEdicion.add(btnGuardarEdicion);

        pnlAcciones = new JPanel(cardAcciones);
        pnlAcciones.setOpaque(false);
        pnlAcciones.add(btnVoltear, MODO_LECTURA);
        pnlAcciones.add(pnlBotonesEdicion, MODO_EDICION);

        PanelRedondeado pnlTarjeta = new PanelRedondeado(new BorderLayout(0, 0), 18, p -> p.superficie, p -> p.borde);
        pnlTarjeta.setBorder(new EmptyBorder(22, 38, 30, 38));
        pnlTarjeta.add(pnlSuperior, BorderLayout.NORTH);
        pnlTarjeta.add(pnlContenido, BorderLayout.CENTER);
        pnlTarjeta.add(pnlAcciones, BorderLayout.SOUTH);

        // ---------- Navegación inferior
        btnAnterior = new BotonEstilizado("< Anterior", null, Variante.SECUNDARIO);
        btnAnterior.setToolTipText("Tarjeta anterior (Flecha izquierda)");
        btnSiguiente = new BotonEstilizado("Siguiente >", null, Variante.SECUNDARIO);
        btnSiguiente.setToolTipText("Tarjeta siguiente (Flecha derecha)");
        lblContador = conTexto(new JLabel("Tarjeta 0 de 0"), p -> p.texto);
        lblContador.setFont(GestorTema.fuente(Font.PLAIN, 15f));
        lblContador.setBorder(new EmptyBorder(0, 18, 0, 18));
        lblContador.setToolTipText("Posición de la tarjeta actual");

        JPanel pnlNavegacion = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        pnlNavegacion.setOpaque(false);
        pnlNavegacion.add(btnAnterior);
        pnlNavegacion.add(lblContador);
        pnlNavegacion.add(btnSiguiente);

        JLabel lblAtajos = conTexto(new JLabel("Espacio = voltear  ·  ← / → = navegar  ·  doble clic = editar",
                JLabel.CENTER), p -> p.textoSecundario);
        lblAtajos.setFont(GestorTema.fuente(Font.PLAIN, 12f));

        JPanel pnlInferior = new JPanel(new BorderLayout(0, 12));
        pnlInferior.setOpaque(false);
        pnlInferior.add(pnlNavegacion, BorderLayout.CENTER);
        pnlInferior.add(lblAtajos, BorderLayout.SOUTH);

        add(pnlTarjeta, BorderLayout.CENTER);
        add(pnlInferior, BorderLayout.SOUTH);

        // Clic en la tarjeta → foco aquí (para que Espacio/flechas no los capture otro componente)
        MouseAdapter enfocar = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!editando) {
                    requestFocusInWindow();
                }
            }
        };
        addMouseListener(enfocar);
        pnlTarjeta.addMouseListener(enfocar);
        lblContenidoTarjeta.addMouseListener(enfocar);

        // Atajos propios del área de edición (tienen prioridad porque el área tiene el foco)
        txtEdicionTarjeta.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "guardarEdicion");
        txtEdicionTarjeta.getActionMap().put("guardarEdicion", accion(() -> btnGuardarEdicion.doClick(0)));
        txtEdicionTarjeta.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelarEdicion");
        txtEdicionTarjeta.getActionMap().put("cancelarEdicion", accion(() -> btnCancelarEdicion.doClick(0)));
    }

    private static Action accion(Runnable r) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    // ------------------------------------------------------------------ API para el Controlador

    public void mostrarTarjeta(String texto, boolean esRespuesta, int numero, int total) {
        terminarEdicion();
        mostrandoRespuesta = esRespuesta;
        lblModo.setText(esRespuesta ? "MODO REPASO - RESPUESTA" : "MODO REPASO - PREGUNTA");
        conTexto(lblModo, esRespuesta ? p -> p.exito : p -> p.acento);
        lblContenidoTarjeta.setTexto(texto);
        btnVoltear.setText(esRespuesta ? "Ver Pregunta" : "Voltear Tarjeta");
        numeroActual = numero;
        totalActual = total;
        lblContador.setText("Tarjeta " + numero + " de " + total);
        btnAnterior.setEnabled(numero > 1);
        btnSiguiente.setEnabled(numero < total);
        revalidate();
        repaint();
    }

    /** Cambia a modo edición con el texto actual del lado visible. */
    public void iniciarEdicion(String textoActual) {
        editando = true;
        txtEdicionTarjeta.setText(textoActual);
        txtEdicionTarjeta.selectAll();
        lblModo.setText(mostrandoRespuesta ? "EDITANDO - RESPUESTA" : "EDITANDO - PREGUNTA");
        conTexto(lblModo, p -> p.advertencia);
        cardContenido.show(pnlContenido, MODO_EDICION);
        cardAcciones.show(pnlAcciones, MODO_EDICION);
        habilitarNavegacion(false);
        GestorTema.aplicarTema(this);
        txtEdicionTarjeta.requestFocusInWindow();
    }

    /** Vuelve al modo lectura sin modificar nada. */
    public void terminarEdicion() {
        if (!editando) {
            return;
        }
        editando = false;
        lblModo.setText(mostrandoRespuesta ? "MODO REPASO - RESPUESTA" : "MODO REPASO - PREGUNTA");
        conTexto(lblModo, mostrandoRespuesta ? p -> p.exito : p -> p.acento);
        cardContenido.show(pnlContenido, MODO_LECTURA);
        cardAcciones.show(pnlAcciones, MODO_LECTURA);
        habilitarNavegacion(true);
        requestFocusInWindow();
    }

    private void habilitarNavegacion(boolean habilitar) {
        btnEditarTarjeta.setEnabled(habilitar);
        btnEliminarTarjeta.setEnabled(habilitar);
        if (!habilitar) {
            btnAnterior.setEnabled(false);
            btnSiguiente.setEnabled(false);
        } else {
            btnAnterior.setEnabled(numeroActual > 1);
            btnSiguiente.setEnabled(numeroActual < totalActual);
        }
    }

    public boolean estaEditando() {
        return editando;
    }

    public String getTextoEditado() {
        return txtEdicionTarjeta.getText().trim();
    }

    /** Componente sobre el que el Controlador escucha el doble clic para editar. */
    public JComponent getComponenteContenido() {
        return lblContenidoTarjeta;
    }

    /** Registra un atajo de teclado (KeyBinding) activo mientras esta vista esté visible. */
    public void registrarAtajo(String nombre, KeyStroke tecla, Action accion) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tecla, nombre);
        getActionMap().put(nombre, accion);
    }

    public void solicitarFoco() {
        if (!editando) {
            requestFocusInWindow();
        }
    }

    public JButton getBtnVoltear() {
        return btnVoltear;
    }

    public JButton getBtnAnterior() {
        return btnAnterior;
    }

    public JButton getBtnSiguiente() {
        return btnSiguiente;
    }

    public JButton getBtnEditarTarjeta() {
        return btnEditarTarjeta;
    }

    public JButton getBtnEliminarTarjeta() {
        return btnEliminarTarjeta;
    }

    public JButton getBtnGuardarEdicion() {
        return btnGuardarEdicion;
    }
}
