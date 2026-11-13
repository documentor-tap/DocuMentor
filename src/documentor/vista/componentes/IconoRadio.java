package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;

/** Indicador circular para los JRadioButton del examen, coherente con la paleta. */
public class IconoRadio implements Icon {

    private static final int TAMANO = 18;
    private final boolean seleccionado;

    public IconoRadio(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        Paleta p = GestorTema.actual();
        boolean activo = c == null || c.isEnabled();
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(p.fondo);
        g2.fill(new Ellipse2D.Double(x + 1, y + 1, TAMANO - 2, TAMANO - 2));
        g2.setColor(seleccionado ? p.acento : GestorTema.mezclar(p.borde, p.textoSecundario, 0.4f));
        g2.draw(new Ellipse2D.Double(x + 1, y + 1, TAMANO - 2, TAMANO - 2));
        if (seleccionado) {
            g2.setColor(activo ? p.acento : GestorTema.mezclar(p.acento, p.superficie, 0.4f));
            g2.fill(new Ellipse2D.Double(x + 5, y + 5, TAMANO - 10, TAMANO - 10));
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return TAMANO;
    }

    @Override
    public int getIconHeight() {
        return TAMANO;
    }
}
