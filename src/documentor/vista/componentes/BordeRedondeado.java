package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Borde redondeado para campos de texto. Lee la paleta al pintarse (se adapta al
 * tema) y se pinta en color de acento cuando el campo tiene el foco.
 */
public class BordeRedondeado extends AbstractBorder {

    private static final long serialVersionUID = 1L;
    private final int margenVertical;
    private final int margenHorizontal;

    public BordeRedondeado(int margenVertical, int margenHorizontal) {
        this.margenVertical = margenVertical;
        this.margenHorizontal = margenHorizontal;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int ancho, int alto) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        Color color = c.hasFocus() ? p.acento : p.borde;
        g2.setColor(color);
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, ancho - 1.5f, alto - 1.5f, 10, 10));
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(margenVertical, margenHorizontal, margenVertical, margenHorizontal);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(margenVertical, margenHorizontal, margenVertical, margenHorizontal);
        return insets;
    }
}
