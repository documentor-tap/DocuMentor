package documentor.vista.componentes;

import documentor.vista.GestorTema;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Etiqueta de varias líneas con ajuste automático de palabras, alineación
 * izquierda o centrada y centrado vertical opcional. Sustituye a los JLabel
 * con HTML, que no se adaptan bien al ancho disponible.
 */
public class TextoAjustable extends JComponent {

    private static final int ANCHO_REFERENCIA = 480;

    private String texto = "";
    private final boolean centrado;
    private int alturaMinima;
    private int ultimaAlturaCalculada = -1;

    public TextoAjustable(String texto, Font fuente, boolean centrado) {
        this.centrado = centrado;
        setFont(fuente);
        setTexto(texto);
    }

    public void setTexto(String texto) {
        this.texto = texto == null ? "" : texto;
        revalidate();
        repaint();
    }

    public String getTexto() {
        return texto;
    }

    /** Altura mínima reservada (útil para que la tarjeta no "salte" de tamaño). */
    public void setAlturaMinima(int alturaMinima) {
        this.alturaMinima = alturaMinima;
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int ancho = getWidth() > 0 ? getWidth() : ANCHO_REFERENCIA;
        return new Dimension(Math.min(ancho, ANCHO_REFERENCIA), calcularAltura(ancho));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(40, getPreferredSize().height);
    }

    @Override
    public void setBounds(int x, int y, int ancho, int alto) {
        int anchoAnterior = getWidth();
        super.setBounds(x, y, ancho, alto);
        if (ancho != anchoAnterior && ancho > 0) {
            ultimaAlturaCalculada = calcularAltura(ancho);
            // El alto depende del ancho: cuando el ancho cambia se pide un segundo pase de
            // layout a todos los contenedores, para que crezcan o se encojan según el texto.
            SwingUtilities.invokeLater(this::revalidarAncestros);
        }
    }

    private void revalidarAncestros() {
        for (java.awt.Container c = getParent(); c != null; c = c.getParent()) {
            c.invalidate();
            if (c instanceof javax.swing.JRootPane) {
                break;
            }
        }
        revalidate();
    }

    private int calcularAltura(int ancho) {
        FontMetrics fm = getFontMetrics(getFont());
        Insets ins = getInsets();
        int lineas = partirEnLineas(fm, ancho - ins.left - ins.right).size();
        int alto = lineas * altoLinea(fm) + ins.top + ins.bottom;
        return Math.max(alto, alturaMinima);
    }

    private static int altoLinea(FontMetrics fm) {
        return Math.round(fm.getHeight() * 1.18f);
    }

    private List<String> partirEnLineas(FontMetrics fm, int anchoDisponible) {
        List<String> lineas = new ArrayList<>();
        int ancho = Math.max(20, anchoDisponible);
        for (String parrafo : texto.split("\n", -1)) {
            if (parrafo.isEmpty()) {
                lineas.add("");
                continue;
            }
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String candidata = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (fm.stringWidth(candidata) <= ancho) {
                    actual.setLength(0);
                    actual.append(candidata);
                    continue;
                }
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                // Palabra más larga que el ancho: se corta por caracteres
                String resto = palabra;
                while (fm.stringWidth(resto) > ancho && resto.length() > 1) {
                    int corte = resto.length() - 1;
                    while (corte > 1 && fm.stringWidth(resto.substring(0, corte)) > ancho) {
                        corte--;
                    }
                    lineas.add(resto.substring(0, corte));
                    resto = resto.substring(corte);
                }
                actual.append(resto);
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        Insets ins = getInsets();
        int anchoUtil = getWidth() - ins.left - ins.right;
        List<String> lineas = partirEnLineas(fm, anchoUtil);
        int alto = altoLinea(fm);
        int altoTotal = lineas.size() * alto;
        int y = centrado
                ? ins.top + Math.max(0, (getHeight() - ins.top - ins.bottom - altoTotal) / 2)
                : ins.top;
        int ajusteBase = (alto - fm.getHeight()) / 2 + fm.getAscent();
        for (String linea : lineas) {
            int x = centrado ? ins.left + (anchoUtil - fm.stringWidth(linea)) / 2 : ins.left;
            g2.drawString(linea, x, y + ajusteBase);
            y += alto;
        }
        g2.dispose();
    }
}
