package documentor.vista.componentes;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Coloca un único componente centrado, con un ancho máximo y márgenes.
 * Se usa para que las tarjetas del panel derecho no se estiren de más
 * en monitores anchos.
 */
public class LayoutCentrado implements LayoutManager {

    private final int anchoMaximo;
    private final int margen;

    public LayoutCentrado(int anchoMaximo, int margen) {
        this.anchoMaximo = anchoMaximo;
        this.margen = margen;
    }

    @Override
    public void layoutContainer(Container padre) {
        if (padre.getComponentCount() == 0) {
            return;
        }
        Component hijo = padre.getComponent(0);
        Insets ins = padre.getInsets();
        int anchoDisponible = padre.getWidth() - ins.left - ins.right - 2 * margen;
        int altoDisponible = padre.getHeight() - ins.top - ins.bottom - 2 * margen;
        int ancho = Math.max(0, Math.min(anchoMaximo, anchoDisponible));
        int alto = Math.max(0, Math.min(hijo.getPreferredSize().height, altoDisponible));
        int x = ins.left + margen + (anchoDisponible - ancho) / 2;
        int y = ins.top + margen + (altoDisponible - alto) / 2;
        hijo.setBounds(x, y, ancho, alto);
    }

    @Override
    public Dimension preferredLayoutSize(Container padre) {
        Insets ins = padre.getInsets();
        Dimension d = padre.getComponentCount() == 0 ? new Dimension() : padre.getComponent(0).getPreferredSize();
        return new Dimension(Math.min(d.width, anchoMaximo) + 2 * margen + ins.left + ins.right,
                d.height + 2 * margen + ins.top + ins.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container padre) {
        return new Dimension(0, 0);
    }

    @Override
    public void addLayoutComponent(String nombre, Component componente) {
        // no requiere registro
    }

    @Override
    public void removeLayoutComponent(Component componente) {
        // no requiere registro
    }
}
