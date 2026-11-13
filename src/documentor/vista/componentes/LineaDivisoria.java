package documentor.vista.componentes;

import documentor.vista.GestorTema;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/** Línea horizontal de 1 px con el color de borde del tema. */
public class LineaDivisoria extends JComponent {

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(10, 1);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(GestorTema.actual().borde);
        g.fillRect(0, 0, getWidth(), 1);
    }
}
