package documentor.vista;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

/**
 * Gestor de colores y tipografía (regla 60-30-10).
 *
 * <ul>
 *   <li>60 % fondo dominante  → {@code #181A20}</li>
 *   <li>30 % superficies       → {@code #262A34}</li>
 *   <li>10 % acento/interacción → {@code #3B82F6}</li>
 * </ul>
 *
 * Cada componente se "etiqueta" con el rol de color que debe usar
 * ({@link #conFondo}, {@link #conTexto}); al cambiar de tema basta con llamar a
 * {@link #aplicarTema(Component)} sobre la raíz y se recolorea todo el árbol.
 * Los componentes personalizados (botones, paneles redondeados, iconos) leen
 * {@link #actual()} directamente al pintarse.
 */
public final class GestorTema {

    /** Paleta de colores de un tema. */
    public static final class Paleta {
        public final boolean oscura;
        public final Color fondo;            // 60 %
        public final Color superficie;       // 30 %
        public final Color superficieHover;
        public final Color borde;
        public final Color texto;
        public final Color textoSecundario;
        public final Color acento;           // 10 %
        public final Color acentoHover;
        public final Color advertencia;      // acento secundario (edición / modo claro)
        public final Color peligro;
        public final Color exito;

        Paleta(boolean oscura, String fondo, String superficie, String superficieHover, String borde,
               String texto, String textoSecundario, String acento, String acentoHover,
               String advertencia, String peligro, String exito) {
            this.oscura = oscura;
            this.fondo = Color.decode(fondo);
            this.superficie = Color.decode(superficie);
            this.superficieHover = Color.decode(superficieHover);
            this.borde = Color.decode(borde);
            this.texto = Color.decode(texto);
            this.textoSecundario = Color.decode(textoSecundario);
            this.acento = Color.decode(acento);
            this.acentoHover = Color.decode(acentoHover);
            this.advertencia = Color.decode(advertencia);
            this.peligro = Color.decode(peligro);
            this.exito = Color.decode(exito);
        }
    }

    public static final Paleta OSCURA = new Paleta(true,
            "#181A20", "#262A34", "#303541", "#3A3F4C",
            "#E5E7EB", "#9CA3AF", "#3B82F6", "#2563EB",
            "#F59E0B", "#EF4444", "#22C55E");

    public static final Paleta CLARA = new Paleta(false,
            "#F3F4F6", "#FFFFFF", "#F1F5F9", "#D8DCE3",
            "#111827", "#6B7280", "#3B82F6", "#2563EB",
            "#D97706", "#DC2626", "#16A34A");

    private static final String PROP_FONDO = "documentor.tema.fondo";
    private static final String PROP_TEXTO = "documentor.tema.texto";
    private static final String FAMILIA_FUENTE = elegirFamilia();

    private static Paleta actual = OSCURA;

    private GestorTema() {
    }

    public static Paleta actual() {
        return actual;
    }

    public static boolean esOscuro() {
        return actual.oscura;
    }

    public static void alternar() {
        actual = actual.oscura ? CLARA : OSCURA;
    }

    // ------------------------------------------------------------------ Etiquetado de roles

    public static <T extends JComponent> T conFondo(T componente, Function<Paleta, Color> rol) {
        componente.putClientProperty(PROP_FONDO, rol);
        componente.setBackground(rol.apply(actual));
        return componente;
    }

    public static <T extends JComponent> T conTexto(T componente, Function<Paleta, Color> rol) {
        componente.putClientProperty(PROP_TEXTO, rol);
        componente.setForeground(rol.apply(actual));
        return componente;
    }

    /** Recorre el árbol de componentes y aplica los colores del tema actual. */
    @SuppressWarnings("unchecked")
    public static void aplicarTema(Component raiz) {
        if (raiz instanceof JComponent) {
            JComponent c = (JComponent) raiz;
            Object fondo = c.getClientProperty(PROP_FONDO);
            Object texto = c.getClientProperty(PROP_TEXTO);
            if (fondo instanceof Function) {
                c.setBackground(((Function<Paleta, Color>) fondo).apply(actual));
            }
            if (texto instanceof Function) {
                c.setForeground(((Function<Paleta, Color>) texto).apply(actual));
            }
            if (c instanceof JTextComponent) {
                JTextComponent t = (JTextComponent) c;
                t.setCaretColor(actual.texto);
                t.setSelectionColor(new Color(actual.acento.getRed(), actual.acento.getGreen(),
                        actual.acento.getBlue(), 110));
                t.setSelectedTextColor(actual.texto);
            }
            if (c instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) c;
                s.getViewport().setBackground(c.getBackground());
            }
        }
        if (raiz instanceof Container) {
            for (Component hijo : ((Container) raiz).getComponents()) {
                aplicarTema(hijo);
            }
        }
        raiz.repaint();
    }

    // ------------------------------------------------------------------ Tooltips

    /**
     * Estilo de los tooltips según el tema. Swing crea un JToolTip nuevo cada vez que
     * muestra uno, así que basta con actualizar UIManager al cambiar de tema.
     */
    public static void configurarTooltips() {
        Paleta p = actual;
        Color fondo = p.oscura ? Color.decode("#0F1115") : Color.decode("#1F2937");
        UIManager.put("ToolTip.background", new ColorUIResource(fondo));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.decode("#F3F4F6")));
        UIManager.put("ToolTip.font", new FontUIResource(fuente(Font.PLAIN, 13f)));
        UIManager.put("ToolTip.border", new BorderUIResource(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(p.oscura ? p.borde : fondo), BorderFactory.createEmptyBorder(6, 9, 6, 9))));
        ToolTipManager gestor = ToolTipManager.sharedInstance();
        gestor.setInitialDelay(450);
        gestor.setDismissDelay(9000);
    }

    // ------------------------------------------------------------------ Tipografía y pintura

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA_FUENTE, estilo, Math.round(tamano)).deriveFont(tamano);
    }

    public static String getFamiliaFuente() {
        return FAMILIA_FUENTE;
    }

    /** Activa antialiasing de figuras y usa la configuración de texto del sistema operativo. */
    public static void prepararGraficos(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Object pistas = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (pistas instanceof Map) {
            g2.addRenderingHints((Map<?, ?>) pistas);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    public static Color conAlfa(Color color, int alfa) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alfa);
    }

    /** Mezcla lineal de dos colores (0 = a, 1 = b). */
    public static Color mezclar(Color a, Color b, float proporcion) {
        float p = Math.max(0f, Math.min(1f, proporcion));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * p),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * p),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * p));
    }

    private static String elegirFamilia() {
        String[] preferidas = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu", "DejaVu Sans"};
        try {
            String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Arrays.sort(disponibles);
            for (String familia : preferidas) {
                if (Arrays.binarySearch(disponibles, familia) >= 0) {
                    return familia;
                }
            }
        } catch (Exception e) {
            // entorno sin fuentes: se usa la lógica
        }
        return Font.SANS_SERIF;
    }
}
