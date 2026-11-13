package documentor.vista.componentes;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * CardLayout que mide solo la tarjeta visible (el estándar usa la más grande),
 * así el contenedor se encoge o crece al cambiar entre "lectura" y "edición".
 */
public class CardLayoutAjustado extends CardLayout {

    private static final long serialVersionUID = 1L;

    @Override
    public Dimension preferredLayoutSize(Container padre) {
        Insets ins = padre.getInsets();
        for (Component c : padre.getComponents()) {
            if (c.isVisible()) {
                Dimension d = c.getPreferredSize();
                return new Dimension(d.width + ins.left + ins.right, d.height + ins.top + ins.bottom);
            }
        }
        return super.preferredLayoutSize(padre);
    }
}
