package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Divisor del JSplitPane con el estilo del tema: una línea fina con un "agarre"
 * que se resalta en azul al pasar el ratón.
 */
public class DivisorModerno extends BasicSplitPaneUI {

    private boolean encima;

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        BasicSplitPaneDivider divisor = new BasicSplitPaneDivider(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GestorTema.prepararGraficos(g2);
                Paleta p = GestorTema.actual();
                int w = getWidth();
                int h = getHeight();
                g2.setColor(p.fondo);
                g2.fillRect(0, 0, w, h);
                g2.setColor(encima ? p.acento : p.borde);
                g2.fillRect(w / 2, 0, 1, h);
                // Agarre central
                float cx = w / 2f;
                float cy = h / 2f;
                g2.setColor(encima ? p.acento : p.superficie);
                g2.fill(new RoundRectangle2D.Float(cx - 4, cy - 22, 8, 44, 8, 8));
                g2.setColor(encima ? java.awt.Color.WHITE : p.textoSecundario);
                for (int i = -1; i <= 1; i++) {
                    g2.fill(new Ellipse2D.Float(cx - 1.5f, cy + i * 9 - 1.5f, 3, 3));
                }
                g2.dispose();
            }
        };
        divisor.setBorder(new EmptyBorder(0, 0, 0, 0));
        divisor.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        divisor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                divisor.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                divisor.repaint();
            }
        });
        return divisor;
    }
}
