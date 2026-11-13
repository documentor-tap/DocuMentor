package documentor.vista;

import static documentor.vista.GestorTema.conFondo;
import static documentor.vista.GestorTema.conTexto;

import documentor.vista.componentes.BotonEstilizado;
import documentor.vista.componentes.BotonEstilizado.Variante;
import documentor.vista.componentes.IconoVectorial;
import documentor.vista.componentes.IconoVectorial.Tipo;
import documentor.vista.componentes.PanelRedondeado;
import documentor.vista.componentes.ScrollBarModerno;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicMenuItemUI;

/**
 * Panel izquierdo (40 %): nombre del archivo, controles de zoom, texto del
 * documento en solo lectura y botones de generación.
 */
public class DocumentView extends JPanel {

    private static final String TEXTO_SIN_DOCUMENTO = "Ningún documento cargado";
    private static final String PLACEHOLDER = "Abre un documento .txt o .pdf para ver aquí su contenido.";

    private final JLabel lblNombreArchivo;
    private final JLabel lblTamanoFuente;
    private final BotonEstilizado btnZoomOut;
    private final BotonEstilizado btnZoomIn;
    private final JTextArea txtContenidoPDF;
    private final JScrollPane scrContenido;
    private final BotonEstilizado btnGenerarFlashcards;
    private final BotonEstilizado btnGenerarExamen;
    private final BotonEstilizado btnGenerarSeleccion;
    private final JPopupMenu popGeneracionSeleccion;
    private final JMenuItem mniFlashcardsSeleccion;
    private final JMenuItem mniExamenSeleccion;

    // HU-18: PDF de contexto
    private final JLabel lblContexto;
    private final BotonEstilizado btnAgregarContexto;
    private final JPanel pnlListaContexto;
    private java.util.function.IntConsumer accionQuitarContexto = i -> { };

    public DocumentView() {
        super(new BorderLayout(0, 14));
        conFondo(this, p -> p.superficie);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // ---------- Cabecera: nombre de archivo + zoom
        lblNombreArchivo = conTexto(new JLabel(TEXTO_SIN_DOCUMENTO, new IconoVectorial(Tipo.DOCUMENTO, 18), JLabel.LEFT),
                p -> p.texto);
        lblNombreArchivo.setFont(GestorTema.fuente(Font.BOLD, 15f));
        lblNombreArchivo.setIconTextGap(8);

        btnZoomOut = new BotonEstilizado("", new IconoVectorial(Tipo.ZOOM_MENOS, 16), Variante.FANTASMA);
        btnZoomOut.setToolTipText("Reducir texto (−2 pt)");
        btnZoomIn = new BotonEstilizado("", new IconoVectorial(Tipo.ZOOM_MAS, 16), Variante.FANTASMA);
        btnZoomIn.setToolTipText("Ampliar texto (+2 pt)");
        lblTamanoFuente = conTexto(new JLabel("16 pt"), p -> p.textoSecundario);
        lblTamanoFuente.setFont(GestorTema.fuente(Font.PLAIN, 12f));
        lblTamanoFuente.setToolTipText("Tamaño actual del texto del documento");

        JPanel pnlZoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        pnlZoom.setOpaque(false);
        pnlZoom.add(btnZoomOut);
        pnlZoom.add(lblTamanoFuente);
        pnlZoom.add(btnZoomIn);

        JPanel pnlCabecera = new JPanel(new BorderLayout(8, 0));
        pnlCabecera.setOpaque(false);
        pnlCabecera.add(lblNombreArchivo, BorderLayout.CENTER);
        pnlCabecera.add(pnlZoom, BorderLayout.EAST);

        // ---------- Cuerpo: área de texto de solo lectura
        txtContenidoPDF = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    GestorTema.prepararGraficos(g2);
                    g2.setColor(GestorTema.actual().textoSecundario);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    Insets ins = getInsets();
                    g2.drawString(PLACEHOLDER, ins.left, ins.top + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        conFondo(txtContenidoPDF, p -> p.fondo);
        conTexto(txtContenidoPDF, p -> p.texto);
        txtContenidoPDF.setEditable(false);
        txtContenidoPDF.setLineWrap(true);
        txtContenidoPDF.setWrapStyleWord(true);
        txtContenidoPDF.setFont(GestorTema.fuente(Font.PLAIN, 16f));
        txtContenidoPDF.setBorder(new EmptyBorder(14, 16, 14, 16));
        txtContenidoPDF.getCaret().setVisible(false);
        txtContenidoPDF.setToolTipText("Texto del documento (solo lectura). Resalta un fragmento para generar material solo con él");
        GestorTema.aplicarTema(txtContenidoPDF);

        scrContenido = conFondo(new JScrollPane(txtContenidoPDF), p -> p.fondo);
        scrContenido.setBorder(BorderFactory.createEmptyBorder());
        scrContenido.setViewportBorder(null);
        scrContenido.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ScrollBarModerno.aplicar(scrContenido);

        PanelRedondeado pnlMarcoTexto = new PanelRedondeado(new BorderLayout(), 12, p -> p.fondo, p -> p.borde);
        pnlMarcoTexto.setBorder(new EmptyBorder(4, 4, 4, 4));
        pnlMarcoTexto.add(scrContenido, BorderLayout.CENTER);

        // ---------- Pie: botones de generación
        btnGenerarFlashcards = new BotonEstilizado("Generar Flashcards", new IconoVectorial(Tipo.RAYO, 16),
                Variante.PRIMARIO);
        btnGenerarExamen = new BotonEstilizado("Generar Examen", new IconoVectorial(Tipo.DOCUMENTO, 16),
                Variante.SECUNDARIO);
        btnGenerarFlashcards.setToolTipText("Crear tarjetas de repaso con todo el documento (te pregunta cuántas)");
        btnGenerarExamen.setToolTipText("Crear un examen de opción múltiple con todo el documento (te pregunta cuántas)");
        btnGenerarSeleccion = new BotonEstilizado("Generar desde selección", new IconoVectorial(Tipo.SELECCION, 16),
                Variante.SECUNDARIO);
        btnGenerarSeleccion.setToolTipText("Resalta un fragmento del texto y genera material solo con él");

        JPanel pnlAcciones = new JPanel(new GridBagLayout());
        pnlAcciones.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 10, 6);
        pnlAcciones.add(btnGenerarFlashcards, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 6, 10, 0);
        pnlAcciones.add(btnGenerarExamen, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnlAcciones.add(btnGenerarSeleccion, gbc);

        // ---------- Menú emergente de "Generar desde selección"
        popGeneracionSeleccion = new JPopupMenu();
        mniFlashcardsSeleccion = crearItemMenu("Flashcards del fragmento", Tipo.RAYO);
        mniExamenSeleccion = crearItemMenu("Examen del fragmento", Tipo.DOCUMENTO);
        mniFlashcardsSeleccion.setToolTipText("Flashcards solo con el texto resaltado");
        mniExamenSeleccion.setToolTipText("Examen solo con el texto resaltado");
        popGeneracionSeleccion.add(mniFlashcardsSeleccion);
        popGeneracionSeleccion.add(mniExamenSeleccion);

        // ---------- HU-18: PDF de contexto (se envían completos a la IA)
        lblContexto = conTexto(new JLabel(), p -> p.texto);
        lblContexto.setFont(GestorTema.fuente(Font.BOLD, 13f));
        lblContexto.setToolTipText("PDF originales que la IA usa como contexto, además del documento abierto");
        btnAgregarContexto = new BotonEstilizado("Agregar PDF", new IconoVectorial(Tipo.CLIP, 15), Variante.FANTASMA);
        btnAgregarContexto.setToolTipText("Adjuntar uno o varios PDF completos (máx. "
                + documentor.modelo.ArchivoContexto.MAXIMO_ARCHIVOS + ", 10 MB c/u). La IA en línea los lee "
                + "tal cual, aunque el texto no se vea aquí");
        JPanel pnlEncabezadoContexto = new JPanel(new BorderLayout(8, 0));
        pnlEncabezadoContexto.setOpaque(false);
        pnlEncabezadoContexto.add(lblContexto, BorderLayout.CENTER);
        pnlEncabezadoContexto.add(btnAgregarContexto, BorderLayout.EAST);

        pnlListaContexto = new JPanel();
        pnlListaContexto.setOpaque(false);
        pnlListaContexto.setLayout(new javax.swing.BoxLayout(pnlListaContexto, javax.swing.BoxLayout.Y_AXIS));

        JPanel pnlContexto = new JPanel(new BorderLayout(0, 6));
        pnlContexto.setOpaque(false);
        pnlContexto.setBorder(new EmptyBorder(0, 0, 12, 0));
        pnlContexto.add(pnlEncabezadoContexto, BorderLayout.NORTH);
        pnlContexto.add(pnlListaContexto, BorderLayout.CENTER);
        mostrarContextos(java.util.Collections.<String>emptyList(), java.util.Collections.<String>emptyList());

        JPanel pnlSur = new JPanel(new BorderLayout());
        pnlSur.setOpaque(false);
        pnlSur.add(pnlContexto, BorderLayout.NORTH);
        pnlSur.add(pnlAcciones, BorderLayout.SOUTH);

        add(pnlCabecera, BorderLayout.NORTH);
        add(pnlMarcoTexto, BorderLayout.CENTER);
        add(pnlSur, BorderLayout.SOUTH);

        habilitarGeneracion(false);
        habilitarGeneracionSeleccion(false);
    }

    private JMenuItem crearItemMenu(String texto, Tipo icono) {
        JMenuItem item = new JMenuItem(texto, new IconoVectorial(icono, 16));
        item.setUI(new BasicMenuItemUI());   // respeta los colores del tema en cualquier Look&Feel
        item.setFont(GestorTema.fuente(Font.PLAIN, 14f));
        item.setBorder(new EmptyBorder(8, 10, 8, 18));
        item.setIconTextGap(10);
        conFondo(item, p -> p.superficie);
        conTexto(item, p -> p.texto);
        return item;
    }

    // ------------------------------------------------------------------ API para el Controlador

    public void mostrarDocumento(String nombreArchivo, String contenido) {
        lblNombreArchivo.setText(nombreArchivo);
        lblNombreArchivo.setToolTipText("Documento abierto: " + nombreArchivo);
        txtContenidoPDF.setText(contenido);
        txtContenidoPDF.setCaretPosition(0);
    }

    public String getTextoCompleto() {
        return txtContenidoPDF.getText();
    }

    public String getTextoSeleccionado() {
        return txtContenidoPDF.getSelectedText();
    }

    public int getTamanoFuente() {
        return txtContenidoPDF.getFont().getSize();
    }

    public void actualizarZoom(int tamano, boolean puedeAmpliar, boolean puedeReducir) {
        txtContenidoPDF.setFont(txtContenidoPDF.getFont().deriveFont((float) tamano));
        lblTamanoFuente.setText(tamano + " pt");
        btnZoomIn.setEnabled(puedeAmpliar);
        btnZoomOut.setEnabled(puedeReducir);
    }

    // ------------------------------------------------------------------ HU-18: PDF de contexto

    /**
     * Muestra la lista de PDF de contexto (una fila por archivo con botón para quitarlo).
     * @param nombres      nombre de cada archivo
     * @param descripciones tamaño y si es legible sin Internet
     */
    public void mostrarContextos(java.util.List<String> nombres, java.util.List<String> descripciones) {
        pnlListaContexto.removeAll();
        lblContexto.setText("PDF de contexto (" + nombres.size() + "/"
                + documentor.modelo.ArchivoContexto.MAXIMO_ARCHIVOS + ")");
        if (nombres.isEmpty()) {
            JLabel lblVacio = conTexto(new JLabel("Opcional: adjunta PDF completos para que la IA los use como contexto."),
                    p -> p.textoSecundario);
            lblVacio.setFont(GestorTema.fuente(Font.PLAIN, 12f));
            lblVacio.setAlignmentX(LEFT_ALIGNMENT);
            pnlListaContexto.add(lblVacio);
        }
        for (int i = 0; i < nombres.size(); i++) {
            final int indice = i;
            PanelRedondeado pnlFila = new PanelRedondeado(new BorderLayout(8, 0), 10, p -> p.fondo, p -> p.borde);
            pnlFila.setBorder(new EmptyBorder(5, 10, 5, 4));
            JLabel lblNombre = conTexto(new JLabel(nombres.get(i), new IconoVectorial(Tipo.DOCUMENTO, 15), JLabel.LEFT),
                    p -> p.texto);
            lblNombre.setFont(GestorTema.fuente(Font.BOLD, 12f));
            lblNombre.setToolTipText(nombres.get(i) + " — " + descripciones.get(i));
            JLabel lblDetalle = conTexto(new JLabel(descripciones.get(i)), p -> p.textoSecundario);
            lblDetalle.setFont(GestorTema.fuente(Font.PLAIN, 11f));
            JPanel pnlTextos = new JPanel(new java.awt.GridLayout(2, 1));
            pnlTextos.setOpaque(false);
            pnlTextos.add(lblNombre);
            pnlTextos.add(lblDetalle);
            BotonEstilizado btnQuitar = new BotonEstilizado("", new IconoVectorial(Tipo.CERRAR, 12, p -> p.textoSecundario),
                    Variante.FANTASMA);
            btnQuitar.setToolTipText("Quitar «" + nombres.get(i) + "» del contexto");
            btnQuitar.addActionListener(e -> accionQuitarContexto.accept(indice));
            pnlFila.add(pnlTextos, BorderLayout.CENTER);
            pnlFila.add(btnQuitar, BorderLayout.EAST);
            pnlFila.setAlignmentX(LEFT_ALIGNMENT);
            pnlFila.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, pnlFila.getPreferredSize().height));
            pnlListaContexto.add(pnlFila);
            pnlListaContexto.add(javax.swing.Box.createVerticalStrut(4));
        }
        btnAgregarContexto.setEnabled(nombres.size() < documentor.modelo.ArchivoContexto.MAXIMO_ARCHIVOS);
        GestorTema.aplicarTema(pnlListaContexto);
        revalidate();
        repaint();
    }

    /** El Controlador indica qué hacer al pulsar la ✕ de una fila (recibe el índice). */
    public void setAccionQuitarContexto(java.util.function.IntConsumer accion) {
        this.accionQuitarContexto = accion;
    }

    public void habilitarAgregarContexto(boolean habilitar, int cantidadActual) {
        btnAgregarContexto.setEnabled(habilitar && cantidadActual < documentor.modelo.ArchivoContexto.MAXIMO_ARCHIVOS);
    }

    public JButton getBtnAgregarContexto() {
        return btnAgregarContexto;
    }

    public void habilitarGeneracion(boolean habilitar) {
        btnGenerarFlashcards.setEnabled(habilitar);
        btnGenerarExamen.setEnabled(habilitar);
    }

    public void habilitarGeneracionSeleccion(boolean habilitar) {
        btnGenerarSeleccion.setEnabled(habilitar);
    }

    /** Pinta en azul el botón del modo activo (como en el diseño de referencia). */
    public void resaltarModo(boolean flashcardsActivo, boolean examenActivo) {
        boolean ninguno = !flashcardsActivo && !examenActivo;
        btnGenerarFlashcards.setVariante(flashcardsActivo || ninguno ? Variante.PRIMARIO : Variante.SECUNDARIO);
        btnGenerarExamen.setVariante(examenActivo ? Variante.PRIMARIO : Variante.SECUNDARIO);
    }

    public void mostrarMenuSeleccion() {
        GestorTema.Paleta p = GestorTema.actual();
        popGeneracionSeleccion.setBorder(BorderFactory.createLineBorder(p.borde));
        conFondo(popGeneracionSeleccion, pal -> pal.superficie);
        GestorTema.aplicarTema(popGeneracionSeleccion);
        popGeneracionSeleccion.show(btnGenerarSeleccion, 0, btnGenerarSeleccion.getHeight() + 4);
    }

    public JTextArea getTxtContenidoPDF() {
        return txtContenidoPDF;
    }

    public JButton getBtnZoomIn() {
        return btnZoomIn;
    }

    public JButton getBtnZoomOut() {
        return btnZoomOut;
    }

    public JButton getBtnGenerarFlashcards() {
        return btnGenerarFlashcards;
    }

    public JButton getBtnGenerarExamen() {
        return btnGenerarExamen;
    }

    public JButton getBtnGenerarSeleccion() {
        return btnGenerarSeleccion;
    }

    public JMenuItem getMniFlashcardsSeleccion() {
        return mniFlashcardsSeleccion;
    }

    public JMenuItem getMniExamenSeleccion() {
        return mniExamenSeleccion;
    }
}
