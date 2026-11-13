package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BordeRedondeado;
import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.LineaDivisoria;
import documentor.vista.componentes.ScrollBarModerno;
import documentor.vista.componentes.TextoAjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.font.TextAttribute;
import java.util.Collections;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

/**
 * Menú lateral de configuración (se desliza desde la derecha):
 * apariencia (modo claro/oscuro) y API key de OpenRouter.
 * Solo muestra datos y expone sus botones; la lógica vive en el Controlador.
 */
public class PanelConfiguracion extends JPanel {

    public static final int ANCHO = 370;

    /** Tipo de mensaje de resultado (define el color). */
    public enum TipoMensaje { EXITO, ERROR, INFORMACION }

    private final BotonEstilizado btnCerrarConfiguracion;
    private final JLabel lblTemaActual;
    private final BotonEstilizado btnModoTema;
    private final TextoAjustable lblEstadoIA;
    private final TextoAjustable lblClaveGuardada;
    private final JPasswordField txtApiKey;
    private final BotonEstilizado btnMostrarClave;
    private final BotonEstilizado btnGuardarClave;
    private final BotonEstilizado btnVerificarClave;
    private final BotonEstilizado btnEliminarClave;
    private final BotonEstilizado btnObtenerClave;
    private final TextoAjustable lblResultadoClave;
    private final TextoAjustable lblUbicacionArchivo;
    private final char caracterOculto;
    private boolean claveVisible;

    public PanelConfiguracion() {
        super(new BorderLayout());
        conFondo(this, p -> p.superficie);
        setBorder(new EmptyBorder(0, 1, 0, 0));   // espacio para la línea izquierda

        // ---------- Encabezado
        JLabel lblTitulo = conTexto(new JLabel("Configuración", new IconoVectorial(Tipo.ENGRANE, 20,
                p -> p.textoSecundario), JLabel.LEFT), p -> p.texto);
        lblTitulo.setFont(GestorTema.fuente(Font.BOLD, 18f));
        lblTitulo.setIconTextGap(10);
        btnCerrarConfiguracion = new BotonEstilizado("", new IconoVectorial(Tipo.CERRAR, 16), Variante.FANTASMA);
        btnCerrarConfiguracion.setToolTipText("Cerrar configuración (Esc)");

        JPanel pnlEncabezado = new JPanel(new BorderLayout());
        pnlEncabezado.setOpaque(false);
        pnlEncabezado.setBorder(new EmptyBorder(18, 22, 14, 14));
        pnlEncabezado.add(lblTitulo, BorderLayout.CENTER);
        pnlEncabezado.add(btnCerrarConfiguracion, BorderLayout.EAST);

        // ---------- Apariencia
        lblTemaActual = conTexto(new JLabel(), p -> p.textoSecundario);
        lblTemaActual.setFont(GestorTema.fuente(Font.PLAIN, 14f));
        btnModoTema = new BotonEstilizado("", null, Variante.SECUNDARIO);
        btnModoTema.setToolTipText("Alterna entre modo oscuro y modo claro");

        // ---------- Inteligencia artificial
        lblEstadoIA = conTexto(new TextoAjustable("", GestorTema.fuente(Font.BOLD, 14f), false), p -> p.texto);
        lblClaveGuardada = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 13f), false),
                p -> p.textoSecundario);

        JLabel lblCampoClave = conTexto(new JLabel("Nueva API key de OpenRouter"), p -> p.texto);
        lblCampoClave.setFont(GestorTema.fuente(Font.BOLD, 13f));

        txtApiKey = new JPasswordField();
        caracterOculto = txtApiKey.getEchoChar();
        conFondo(txtApiKey, p -> p.fondo);
        conTexto(txtApiKey, p -> p.texto);
        txtApiKey.setFont(GestorTema.fuente(Font.PLAIN, 14f));
        txtApiKey.setBorder(new BordeRedondeado(8, 10));
        txtApiKey.setToolTipText("Pega aquí tu clave (empieza con sk-or-). Enter = guardar");
        txtApiKey.addFocusListener(new java.awt.event.FocusAdapter() {   // repinta el borde de foco
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                txtApiKey.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                txtApiKey.repaint();
            }
        });

        btnMostrarClave = new BotonEstilizado("", new IconoVectorial(Tipo.OJO, 18), Variante.FANTASMA);
        btnMostrarClave.setToolTipText("Mostrar u ocultar la clave escrita");
        btnMostrarClave.addActionListener(e -> alternarVisibilidadClave());   // detalle puramente visual

        JPanel pnlCampoClave = new JPanel(new BorderLayout(6, 0));
        pnlCampoClave.setOpaque(false);
        pnlCampoClave.add(txtApiKey, BorderLayout.CENTER);
        pnlCampoClave.add(btnMostrarClave, BorderLayout.EAST);

        btnGuardarClave = new BotonEstilizado("Guardar", new IconoVectorial(Tipo.GUARDAR, 16,
                p -> java.awt.Color.WHITE), Variante.PRIMARIO);
        btnGuardarClave.setToolTipText("Verifica la clave con OpenRouter y la guarda en documentor.properties");
        btnVerificarClave = new BotonEstilizado("Verificar", new IconoVectorial(Tipo.ESCUDO, 16), Variante.SECUNDARIO);
        btnVerificarClave.setToolTipText("Comprueba la clave escrita (o la guardada) y muestra tus peticiones "
                + "gratis restantes de hoy. No gasta peticiones.");

        JPanel pnlBotonesClave = new JPanel(new GridLayout(1, 2, 8, 0));
        pnlBotonesClave.setOpaque(false);
        pnlBotonesClave.add(btnGuardarClave);
        pnlBotonesClave.add(btnVerificarClave);

        lblResultadoClave = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 13f), false),
                p -> p.textoSecundario);

        btnEliminarClave = new BotonEstilizado("Quitar clave y usar el simulador local",
                new IconoVectorial(Tipo.PAPELERA, 15, p -> p.textoSecundario), Variante.FANTASMA);
        btnEliminarClave.setHorizontalAlignment(JButton.LEFT);
        btnEliminarClave.setToolTipText("Borra la clave de documentor.properties; la app funcionará sin Internet");
        btnObtenerClave = new BotonEstilizado("Obtener una clave gratis (openrouter.ai/keys)",
                new IconoVectorial(Tipo.LLAVE, 15), Variante.FANTASMA);
        btnObtenerClave.setHorizontalAlignment(JButton.LEFT);
        btnObtenerClave.setToolTipText("Abre openrouter.ai/keys en tu navegador");

        lblUbicacionArchivo = conTexto(new TextoAjustable("", GestorTema.fuente(Font.PLAIN, 12f), false),
                p -> p.textoSecundario);

        // ---------- Atajos
        TextoAjustable lblAtajos = conTexto(new TextoAjustable(
                "Espacio: voltear flashcard\n← / →: tarjeta anterior / siguiente\n"
                        + "Doble clic en el texto: editarlo\nCtrl + Enter: guardar edición  ·  Esc: cancelar\n"
                        + "Doble clic en el divisor: ampliar resultados",
                GestorTema.fuente(Font.PLAIN, 13f), false), p -> p.textoSecundario);

        // ---------- Ensamblado
        JPanel pnlContenido = new JPanel();
        pnlContenido.setOpaque(false);
        pnlContenido.setLayout(new BoxLayout(pnlContenido, BoxLayout.Y_AXIS));
        pnlContenido.setBorder(new EmptyBorder(4, 22, 22, 22));

        agregar(pnlContenido, titulo("APARIENCIA"), 10);
        agregar(pnlContenido, lblTemaActual, 8);
        agregar(pnlContenido, btnModoTema, 20);
        agregar(pnlContenido, new LineaDivisoria(), 18);
        agregar(pnlContenido, titulo("INTELIGENCIA ARTIFICIAL"), 10);
        agregar(pnlContenido, lblEstadoIA, 4);
        agregar(pnlContenido, lblClaveGuardada, 14);
        agregar(pnlContenido, lblCampoClave, 6);
        agregar(pnlContenido, pnlCampoClave, 10);
        agregar(pnlContenido, pnlBotonesClave, 8);
        agregar(pnlContenido, lblResultadoClave, 8);
        agregar(pnlContenido, btnEliminarClave, 2);
        agregar(pnlContenido, btnObtenerClave, 10);
        agregar(pnlContenido, lblUbicacionArchivo, 20);
        agregar(pnlContenido, new LineaDivisoria(), 18);
        agregar(pnlContenido, titulo("ATAJOS"), 8);
        agregar(pnlContenido, lblAtajos, 0);

        JPanel pnlEnvoltura = new PanelAjustadoAlAncho();
        pnlEnvoltura.setOpaque(false);
        pnlEnvoltura.add(pnlContenido, BorderLayout.NORTH);
        JScrollPane scrContenido = new JScrollPane(pnlEnvoltura);
        scrContenido.setOpaque(false);
        scrContenido.getViewport().setOpaque(false);
        scrContenido.setBorder(null);
        scrContenido.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ScrollBarModerno.aplicar(scrContenido);

        add(pnlEncabezado, BorderLayout.NORTH);
        add(scrContenido, BorderLayout.CENTER);
        setPreferredSize(new Dimension(ANCHO, 600));
    }

    /** Panel que siempre usa el ancho del área visible (solo desplazamiento vertical). */
    private static final class PanelAjustadoAlAncho extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;

        PanelAjustadoAlAncho() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return visible.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static JLabel titulo(String texto) {
        JLabel lbl = conTexto(new JLabel(texto), p -> p.acento);
        lbl.setFont(GestorTema.fuente(Font.BOLD, 12f)
                .deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.08f)));
        return lbl;
    }

    private static void agregar(JPanel contenedor, JComponent componente, int espacioDespues) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (componente instanceof JButton || componente instanceof JPanel) {
            componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, componente.getPreferredSize().height));
        }
        contenedor.add(componente);
        if (espacioDespues > 0) {
            contenedor.add(Box.createVerticalStrut(espacioDespues));
        }
    }

    private void alternarVisibilidadClave() {
        claveVisible = !claveVisible;
        txtApiKey.setEchoChar(claveVisible ? (char) 0 : caracterOculto);
        btnMostrarClave.setIcon(new IconoVectorial(claveVisible ? Tipo.OJO_TACHADO : Tipo.OJO, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(GestorTema.actual().borde);
        g.fillRect(0, 0, 1, getHeight());
    }

    // ------------------------------------------------------------------ API para Vista/Controlador

    public void actualizarTema(boolean modoOscuro) {
        lblTemaActual.setText("Tema actual: " + (modoOscuro ? "oscuro" : "claro"));
        btnModoTema.setText(modoOscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
        btnModoTema.setIcon(new IconoVectorial(modoOscuro ? Tipo.SOL : Tipo.LUNA, 18));
    }

    /**
     * @param motor            nombre del motor activo
     * @param iaEnLinea        {@code true} si se usa OpenRouter
     * @param claveEnmascarada clave guardada (enmascarada) o {@code null}
     * @param ubicacion        archivo donde se guarda la configuración
     */
    public void mostrarEstadoIA(String motor, boolean iaEnLinea, String claveEnmascarada, String ubicacion) {
        lblEstadoIA.setTexto((iaEnLinea ? "● En línea: " : "● Sin conexión: ") + motor);
        conTexto(lblEstadoIA, iaEnLinea ? p -> p.exito : p -> p.advertencia);
        lblClaveGuardada.setTexto(claveEnmascarada != null
                ? "Clave guardada: " + claveEnmascarada
                : "No hay clave guardada: se usa el simulador local (sin Internet).");
        lblUbicacionArchivo.setTexto("La clave se guarda solo en tu computadora, en:\n" + ubicacion
                + "\nNo compartas ese archivo ni lo subas a GitHub.");
        btnEliminarClave.setVisible(claveEnmascarada != null);
        revalidate();
        repaint();
    }

    public void mostrarResultadoClave(String texto, TipoMensaje tipo) {
        lblResultadoClave.setTexto(texto == null ? "" : texto);
        switch (tipo) {
            case EXITO:
                conTexto(lblResultadoClave, p -> p.exito);
                break;
            case ERROR:
                conTexto(lblResultadoClave, p -> p.peligro);
                break;
            default:
                conTexto(lblResultadoClave, p -> p.textoSecundario);
        }
        revalidate();
    }

    /** Deshabilita los botones de la clave mientras se consulta a OpenRouter. */
    public void setOcupado(boolean ocupado) {
        btnGuardarClave.setEnabled(!ocupado);
        btnVerificarClave.setEnabled(!ocupado);
        btnEliminarClave.setEnabled(!ocupado);
        txtApiKey.setEnabled(!ocupado);
    }

    public String getClaveEscrita() {
        return new String(txtApiKey.getPassword()).trim();
    }

    public void limpiarCampoClave() {
        txtApiKey.setText("");
    }

    public void enfocarCampoClave() {
        txtApiKey.requestFocusInWindow();
    }

    public JPasswordField getTxtApiKey() {
        return txtApiKey;
    }

    public JButton getBtnCerrarConfiguracion() {
        return btnCerrarConfiguracion;
    }

    public JButton getBtnModoTema() {
        return btnModoTema;
    }

    public JButton getBtnGuardarClave() {
        return btnGuardarClave;
    }

    public JButton getBtnVerificarClave() {
        return btnVerificarClave;
    }

    public JButton getBtnEliminarClave() {
        return btnEliminarClave;
    }

    public JButton getBtnObtenerClave() {
        return btnObtenerClave;
    }
}
