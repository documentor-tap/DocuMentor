package documentor.vista;

import static documentor.vista.GestorTema.conFondo;

import documentor.vista.componentes.DivisorModerno;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Ventana principal. Ensambla las vistas modulares, el divisor redimensionable
 * documento/resultados y el menú lateral de configuración. No contiene lógica de negocio.
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final double PROPORCION_INICIAL = 0.4;
    private static final int PASO_ANIMACION_MS = 12;

    private final HeaderView pnlBarraSuperior;
    private final DocumentView pnlIzquierdo;
    private final InteractiveView pnlDerecho;
    private final JSplitPane pnlDivisor;
    private final PanelConfiguracion pnlConfiguracion;
    private final VeloOscuro pnlVelo;

    private boolean divisorInicializado;
    private int ultimaPosicionDivisor = -1;

    private boolean configuracionAbierta;
    private float progresoConfiguracion;   // 0 = oculto, 1 = visible
    private Timer animacionConfiguracion;

    public VentanaPrincipal() {
        super("DocuMentor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(Arrays.asList(
                new IconoVectorial(Tipo.LIBROS, 16).comoImagen(),
                new IconoVectorial(Tipo.LIBROS, 32).comoImagen(),
                new IconoVectorial(Tipo.LIBROS, 64).comoImagen()));
        GestorTema.configurarTooltips();

        pnlBarraSuperior = new HeaderView();
        pnlIzquierdo = new DocumentView();
        pnlDerecho = new InteractiveView();

        // ---------- Divisor documento | resultados (arrastrable, doble clic = ampliar)
        pnlIzquierdo.setMinimumSize(new Dimension(0, 0));
        pnlDerecho.setMinimumSize(new Dimension(420, 0));
        pnlDivisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlIzquierdo, pnlDerecho);
        pnlDivisor.setUI(new DivisorModerno());
        pnlDivisor.setBorder(null);
        pnlDivisor.setDividerSize(10);
        pnlDivisor.setContinuousLayout(true);
        pnlDivisor.setResizeWeight(PROPORCION_INICIAL);
        conFondo(pnlDivisor, p -> p.fondo);
        // (el divisor de Swing no es JComponent, así que su ayuda está en el botón "Ampliar resultados")
        java.awt.Component divisor = ((BasicSplitPaneUI) pnlDivisor.getUI()).getDivider();
        divisor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    alternarAmpliacionResultados();
                }
            }
        });
        pnlDivisor.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                e -> pnlBarraSuperior.actualizarBotonAmpliar(isResultadosAmpliados()));
        pnlDivisor.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!divisorInicializado && pnlDivisor.getWidth() > 0) {
                    divisorInicializado = true;
                    pnlDivisor.setDividerLocation(PROPORCION_INICIAL);
                }
            }
        });

        JPanel pnlRaiz = conFondo(new JPanel(new BorderLayout()), p -> p.fondo);
        pnlRaiz.add(pnlBarraSuperior, BorderLayout.NORTH);
        pnlRaiz.add(pnlDivisor, BorderLayout.CENTER);
        setContentPane(pnlRaiz);

        // ---------- Menú lateral de configuración (capa superior, se desliza desde la derecha)
        pnlConfiguracion = new PanelConfiguracion();
        pnlConfiguracion.actualizarTema(GestorTema.esOscuro());
        pnlVelo = new VeloOscuro();
        pnlVelo.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                cerrarConfiguracion();
            }
        });
        getLayeredPane().add(pnlVelo, JLayeredPane.PALETTE_LAYER);
        getLayeredPane().add(pnlConfiguracion, JLayeredPane.MODAL_LAYER);
        pnlVelo.setVisible(false);
        pnlConfiguracion.setVisible(false);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                posicionarConfiguracion();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarConfiguracion");
        getRootPane().getActionMap().put("cerrarConfiguracion", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                cerrarConfiguracion();
            }
        });
        pnlConfiguracion.getBtnCerrarConfiguracion().addActionListener(e -> cerrarConfiguracion());

        setMinimumSize(new Dimension(1100, 700));
        setSize(1400, 820);
        setLocationRelativeTo(null);
    }

    /** Recolorea toda la ventana (incluido el menú lateral) con la paleta activa. */
    public void aplicarTema() {
        GestorTema.configurarTooltips();
        GestorTema.aplicarTema(getRootPane());
        GestorTema.aplicarTema(pnlConfiguracion);
        pnlConfiguracion.actualizarTema(GestorTema.esOscuro());
        repaint();
    }

    // ------------------------------------------------------------------ Vista de resultados

    public boolean isResultadosAmpliados() {
        return divisorInicializado && pnlDivisor.getDividerLocation() <= 5;
    }

    /** Oculta/muestra el panel del documento para que los resultados ocupen toda la ventana. */
    public void alternarAmpliacionResultados() {
        if (isResultadosAmpliados()) {
            if (ultimaPosicionDivisor > 5) {
                pnlDivisor.setDividerLocation(ultimaPosicionDivisor);
            } else {
                pnlDivisor.setDividerLocation(PROPORCION_INICIAL);
            }
        } else {
            ultimaPosicionDivisor = pnlDivisor.getDividerLocation();
            pnlDivisor.setDividerLocation(0);
        }
        pnlBarraSuperior.actualizarBotonAmpliar(isResultadosAmpliados());
    }

    // ------------------------------------------------------------------ Menú de configuración

    public boolean isConfiguracionAbierta() {
        return configuracionAbierta;
    }

    public void alternarConfiguracion() {
        if (configuracionAbierta) {
            cerrarConfiguracion();
        } else {
            abrirConfiguracion();
        }
    }

    public void abrirConfiguracion() {
        if (!configuracionAbierta) {
            configuracionAbierta = true;
            pnlConfiguracion.mostrarResultadoClave("", PanelConfiguracion.TipoMensaje.INFORMACION);
            animarConfiguracion();
        }
    }

    public void cerrarConfiguracion() {
        if (configuracionAbierta) {
            configuracionAbierta = false;
            animarConfiguracion();
        }
    }

    private void animarConfiguracion() {
        if (animacionConfiguracion != null) {
            animacionConfiguracion.stop();
        }
        pnlVelo.setVisible(true);
        pnlConfiguracion.setVisible(true);
        animacionConfiguracion = new Timer(PASO_ANIMACION_MS, e -> {
            float objetivo = configuracionAbierta ? 1f : 0f;
            progresoConfiguracion += (objetivo - progresoConfiguracion) * 0.28f;
            if (Math.abs(objetivo - progresoConfiguracion) < 0.01f) {
                progresoConfiguracion = objetivo;
                ((Timer) e.getSource()).stop();
                if (!configuracionAbierta) {
                    pnlVelo.setVisible(false);
                    pnlConfiguracion.setVisible(false);
                } else {
                    pnlConfiguracion.enfocarCampoClave();
                }
            }
            posicionarConfiguracion();
        });
        animacionConfiguracion.start();
    }

    private void posicionarConfiguracion() {
        int ancho = getLayeredPane().getWidth();
        int alto = getLayeredPane().getHeight();
        int arriba = pnlBarraSuperior.getHeight();
        int anchoPanel = Math.min(PanelConfiguracion.ANCHO, ancho);
        int x = ancho - Math.round(anchoPanel * progresoConfiguracion);
        pnlConfiguracion.setBounds(x, arriba, anchoPanel, Math.max(0, alto - arriba));
        pnlConfiguracion.revalidate();
        pnlVelo.setBounds(0, arriba, ancho, Math.max(0, alto - arriba));
        pnlVelo.setOpacidad(0.45f * progresoConfiguracion);
    }

    public PanelConfiguracion getPanelConfiguracion() {
        return pnlConfiguracion;
    }

    /** Capa semitransparente detrás del menú; un clic sobre ella lo cierra. */
    private static final class VeloOscuro extends JComponent {
        private static final long serialVersionUID = 1L;
        private float opacidad;

        void setOpacidad(float opacidad) {
            this.opacidad = opacidad;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, opacidad))));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ Selectores de archivo

    public File seleccionarDocumento() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Abrir documento de estudio");
        selector.setAcceptAllFileFilterUsed(false);
        selector.addChoosableFileFilter(new FileNameExtensionFilter("Documentos (*.txt, *.pdf)", "txt", "pdf"));
        selector.addChoosableFileFilter(new FileNameExtensionFilter("Texto plano (*.txt)", "txt"));
        selector.addChoosableFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        selector.setFileFilter(selector.getChoosableFileFilters()[0]);
        return selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? selector.getSelectedFile() : null;
    }

    /** HU-18: selector de PDF de contexto (solo .pdf, selección múltiple). */
    public java.util.List<File> seleccionarPdfsContexto() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Agregar PDF de contexto");
        selector.setMultiSelectionEnabled(true);
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter("Documentos PDF (*.pdf)", "pdf"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return java.util.Collections.emptyList();
        }
        return Arrays.asList(selector.getSelectedFiles());
    }

    /** Pide destino de exportación y garantiza la extensión .txt o .csv según el filtro elegido. */
    public File seleccionarDestinoExportacion(String nombreSugerido) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar material");
        selector.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filtroTxt = new FileNameExtensionFilter("Texto (*.txt)", "txt");
        FileNameExtensionFilter filtroCsv = new FileNameExtensionFilter("CSV para Excel / Anki (*.csv)", "csv");
        selector.addChoosableFileFilter(filtroTxt);
        selector.addChoosableFileFilter(filtroCsv);
        selector.setFileFilter(filtroTxt);
        selector.setSelectedFile(new File(nombreSugerido + ".txt"));
        selector.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, e -> {
            FileFilter filtro = (FileFilter) e.getNewValue();
            String extension = filtro == filtroCsv ? ".csv" : ".txt";
            selector.setSelectedFile(new File(nombreSugerido + extension));
        });

        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File destino = selector.getSelectedFile();
        String nombre = destino.getName().toLowerCase(Locale.ROOT);
        if (!nombre.endsWith(".txt") && !nombre.endsWith(".csv")) {
            String extension = selector.getFileFilter() == filtroCsv ? ".csv" : ".txt";
            destino = new File(destino.getParentFile(), destino.getName() + extension);
        }
        if (destino.exists() && !confirmar("El archivo «" + destino.getName() + "» ya existe. ¿Deseas reemplazarlo?",
                "Confirmar sobrescritura")) {
            return null;
        }
        return destino;
    }

    // ------------------------------------------------------------------ Mensajes y diálogos

    /** Mini menú de cantidad. @return la cantidad elegida o {@code null} si se canceló. */
    public Integer pedirCantidad(String sustantivo, int maximo, int predeterminado, String detalle) {
        return DialogoCantidad.preguntar(this, sustantivo, maximo, predeterminado, detalle);
    }

    /** Abre una página en el navegador. @return {@code false} si el sistema no lo permite. */
    public boolean abrirEnNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception e) {
            // se informa con el valor de retorno
        }
        return false;
    }

    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarAdvertencia(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.WARNING_MESSAGE);
    }

    public void mostrarInformacion(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmar(String mensaje, String titulo) {
        return JOptionPane.showConfirmDialog(this, mensaje, titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------------ Acceso a subvistas

    public HeaderView getHeaderView() {
        return pnlBarraSuperior;
    }

    public DocumentView getDocumentView() {
        return pnlIzquierdo;
    }

    public InteractiveView getInteractiveView() {
        return pnlDerecho;
    }
}
