package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Barra de desplazamiento delgada y redondeada que sigue la paleta activa. */
public class ScrollBarModerno extends BasicScrollBarUI {

    private static final int GROSOR = 10;

    public static void aplicar(JScrollPane scroll) {
        configurar(scroll.getVerticalScrollBar());
        configurar(scroll.getHorizontalScrollBar());
    }

    private static void configurar(JScrollBar barra) {
        barra.setUI(new ScrollBarModerno());
        barra.setOpaque(false);
        barra.setUnitIncrement(16);
        barra.setPreferredSize(barra.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(GROSOR, 0) : new Dimension(0, GROSOR));
    }

    @Override
    protected JButton createDecreaseButton(int orientacion) {
        return botonInvisible();
    }

    @Override
    protected JButton createIncreaseButton(int orientacion) {
        return botonInvisible();
    }

    private static JButton botonInvisible() {
        JButton boton = new JButton();
        boton.setPreferredSize(new Dimension(0, 0));
        boton.setMinimumSize(new Dimension(0, 0));
        boton.setMaximumSize(new Dimension(0, 0));
        return boton;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle zona) {
        // pista transparente
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle zona) {
        if (zona.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        g2.setColor(isThumbRollover() ? p.textoSecundario : p.borde);
        g2.fill(new RoundRectangle2D.Float(zona.x + 2, zona.y + 2, zona.width - 4, zona.height - 4, 6, 6));
        g2.dispose();
    }
}
