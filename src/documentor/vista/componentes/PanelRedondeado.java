package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;
import javax.swing.JPanel;

/**
 * Panel con esquinas redondeadas. Los colores se definen como funciones de la
 * paleta, así el panel se adapta automáticamente al cambiar de tema.
 */
public class PanelRedondeado extends JPanel {

    private final int radio;
    private Function<Paleta, Color> colorFondo;
    private Function<Paleta, Color> colorBorde;
    private float grosorBorde = 1f;

    public PanelRedondeado(LayoutManager layout, int radio,
                           Function<Paleta, Color> colorFondo, Function<Paleta, Color> colorBorde) {
        super(layout);
        this.radio = radio;
        this.colorFondo = colorFondo;
        this.colorBorde = colorBorde;
        setOpaque(false);
    }

    public void setColorFondo(Function<Paleta, Color> colorFondo) {
        this.colorFondo = colorFondo;
        repaint();
    }

    public void setColorBorde(Function<Paleta, Color> colorBorde) {
        this.colorBorde = colorBorde;
        repaint();
    }

    public void setGrosorBorde(float grosorBorde) {
        this.grosorBorde = grosorBorde;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        float w = getWidth();
        float h = getHeight();
        if (colorFondo != null) {
            g2.setColor(colorFondo.apply(p));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, radio, radio));
        }
        if (colorBorde != null) {
            float m = grosorBorde / 2f;
            g2.setStroke(new java.awt.BasicStroke(grosorBorde));
            g2.setColor(colorBorde.apply(p));
            g2.draw(new RoundRectangle2D.Float(m, m, w - 1 - grosorBorde, h - 1 - grosorBorde, radio, radio));
        }
        g2.dispose();
    }
}
