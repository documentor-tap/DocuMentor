package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/** Slider plano: pista redondeada con el tramo recorrido en color de acento. */
public class SliderModerno extends BasicSliderUI {

    public SliderModerno(JSlider slider) {
        super(slider);
    }

    public static JSlider crear(int minimo, int maximo, int valor) {
        JSlider slider = new JSlider(minimo, maximo, valor);
        slider.setUI(new SliderModerno(slider));
        slider.setOpaque(false);
        slider.setFocusable(true);
        return slider;
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(18, 18);
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        Rectangle pista = trackRect;
        int y = pista.y + pista.height / 2 - 3;
        g2.setColor(p.borde);
        g2.fill(new RoundRectangle2D.Float(pista.x, y, pista.width, 6, 6, 6));
        int recorrido = thumbRect.x + thumbRect.width / 2 - pista.x;
        g2.setColor(slider.isEnabled() ? p.acento : p.textoSecundario);
        g2.fill(new RoundRectangle2D.Float(pista.x, y, Math.max(0, recorrido), 6, 6, 6));
        g2.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        Rectangle r = thumbRect;
        g2.setColor(java.awt.Color.WHITE);
        g2.fill(new Ellipse2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
        g2.setColor(p.acento);
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.draw(new Ellipse2D.Float(r.x + 2.5f, r.y + 2.5f, r.width - 5, r.height - 5));
        g2.dispose();
    }

    @Override
    public void paintFocus(Graphics g) {
        // sin rectángulo punteado de foco
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }
}
