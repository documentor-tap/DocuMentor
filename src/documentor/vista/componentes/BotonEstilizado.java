package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Botón plano con esquinas redondeadas que se pinta con la paleta activa.
 * Variantes: PRIMARIO (azul de acento), SECUNDARIO (superficie con borde) y
 * FANTASMA (transparente, para iconos de editar/eliminar/zoom).
 */
public class BotonEstilizado extends JButton {

    public enum Variante { PRIMARIO, SECUNDARIO, FANTASMA }

    private static final int RADIO = 10;

    private Variante variante;

    public BotonEstilizado(String texto, Icon icono, Variante variante) {
        super(texto, icono);
        this.variante = variante;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        // El clic con el ratón no roba el foco (los atajos de teclado siguen funcionando),
        // pero el botón sigue siendo accesible con la tecla Tab.
        setRequestFocusEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(GestorTema.fuente(Font.BOLD, 14f));
        setIconTextGap(8);
        boolean soloIcono = texto == null || texto.isEmpty();
        setBorder(soloIcono ? new EmptyBorder(7, 7, 7, 7) : new EmptyBorder(10, 18, 10, 18));
    }

    public Variante getVariante() {
        return variante;
    }

    public void setVariante(Variante variante) {
        this.variante = variante;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        ButtonModel modelo = getModel();
        boolean activo = isEnabled();
        boolean encima = activo && modelo.isRollover();
        boolean presionado = activo && modelo.isArmed() && modelo.isPressed();

        Color fondo = null;
        Color borde = null;
        Color colorTexto;
        switch (variante) {
            case PRIMARIO:
                fondo = presionado ? p.acentoHover.darker() : encima ? p.acentoHover : p.acento;
                if (!activo) {
                    fondo = GestorTema.mezclar(p.acento, p.superficie, 0.55f);
                }
                colorTexto = activo ? Color.WHITE : GestorTema.conAlfa(Color.WHITE, 150);
                break;
            case SECUNDARIO:
                fondo = presionado ? p.borde : encima ? p.superficieHover : p.superficie;
                borde = encima ? GestorTema.mezclar(p.borde, p.textoSecundario, 0.35f) : p.borde;
                colorTexto = activo ? p.texto : p.textoSecundario;
                break;
            case FANTASMA:
            default:
                fondo = presionado ? p.borde : encima ? p.superficieHover : null;
                colorTexto = activo ? p.texto : p.textoSecundario;
                break;
        }

        int w = getWidth();
        int h = getHeight();
        if (fondo != null) {
            g2.setColor(fondo);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, RADIO, RADIO));
        }
        if (borde != null) {
            g2.setColor(borde);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2f, h - 2f, RADIO, RADIO));
        }

        // Icono + texto centrados (se calcula igual que JLabel)
        FontMetrics fm = g2.getFontMetrics(getFont());
        Insets ins = getInsets();
        Rectangle area = new Rectangle(ins.left, ins.top, w - ins.left - ins.right, h - ins.top - ins.bottom);
        Rectangle zonaIcono = new Rectangle();
        Rectangle zonaTexto = new Rectangle();
        String texto = SwingUtilities.layoutCompoundLabel(this, fm, getText(), getIcon(),
                getVerticalAlignment(), getHorizontalAlignment(),
                getVerticalTextPosition(), getHorizontalTextPosition(),
                area, zonaIcono, zonaTexto, getText() == null || getText().isEmpty() ? 0 : getIconTextGap());

        if (getIcon() != null) {
            Graphics2D gi = (Graphics2D) g2.create();
            if (!activo) {
                gi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            }
            getIcon().paintIcon(this, gi, zonaIcono.x, zonaIcono.y);
            gi.dispose();
        }
        if (texto != null && !texto.isEmpty()) {
            g2.setFont(getFont());
            g2.setColor(colorTexto);
            g2.drawString(texto, zonaTexto.x, zonaTexto.y + fm.getAscent());
        }
        g2.dispose();
    }
}
