package documentor.vista.componentes;

import documentor.vista.GestorTema;
import documentor.vista.GestorTema.Paleta;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import javax.swing.Icon;

/**
 * Iconos dibujados con Java2D sobre una cuadrícula de 16x16 (escalables y
 * nítidos en pantallas HiDPI). Evita depender de emojis, que Swing no pinta a color.
 */
public class IconoVectorial implements Icon {

    public enum Tipo {
        LIBROS, CARPETA, SOL, LUNA, LAPIZ, PAPELERA, RAYO, DOCUMENTO,
        VOLTEAR, EXPORTAR, ZOOM_MAS, ZOOM_MENOS, SELECCION,
        ENGRANE, CERRAR, MAS, MENOS, CLIP, OJO, OJO_TACHADO, LLAVE, EXPANDIR, CONTRAER, GUARDAR, ESCUDO
    }

    private static final Color AMARILLO = Color.decode("#FBBF24");
    private static final Color NARANJA = Color.decode("#F97316");

    private final Tipo tipo;
    private final int tamano;
    private final Function<Paleta, Color> color;

    public IconoVectorial(Tipo tipo, int tamano) {
        this(tipo, tamano, p -> p.texto);
    }

    public IconoVectorial(Tipo tipo, int tamano, Function<Paleta, Color> color) {
        this.tipo = tipo;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return tamano;
    }

    @Override
    public int getIconHeight() {
        return tamano;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        GestorTema.prepararGraficos(g2);
        g2.translate(x, y);
        double escala = tamano / 16.0;
        g2.scale(escala, escala);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color base = color.apply(GestorTema.actual());
        g2.setColor(base);
        dibujar(g2, base);
        g2.dispose();
    }

    private void dibujar(Graphics2D g2, Color base) {
        switch (tipo) {
            case LIBROS:
                g2.setColor(Color.decode("#22C55E"));
                g2.fill(new RoundRectangle2D.Double(1.5, 3, 3.5, 11.5, 1, 1));
                g2.setColor(Color.decode("#EF4444"));
                g2.fill(new RoundRectangle2D.Double(5.5, 1.5, 3.5, 13, 1, 1));
                g2.setColor(Color.decode("#3B82F6"));
                Graphics2D gr = (Graphics2D) g2.create();
                gr.rotate(Math.toRadians(-14), 12, 14.5);
                gr.fill(new RoundRectangle2D.Double(10, 3, 3.5, 11.5, 1, 1));
                gr.dispose();
                g2.setColor(Color.WHITE);
                g2.fill(new Rectangle2D.Double(1.5, 5, 3.5, 1));
                g2.fill(new Rectangle2D.Double(5.5, 4, 3.5, 1));
                break;
            case CARPETA: {
                Path2D carpeta = new Path2D.Double();
                carpeta.moveTo(1.5, 3.5);
                carpeta.lineTo(6, 3.5);
                carpeta.lineTo(7.5, 5);
                carpeta.lineTo(14.5, 5);
                carpeta.lineTo(14.5, 13.5);
                carpeta.lineTo(1.5, 13.5);
                carpeta.closePath();
                g2.setColor(AMARILLO);
                g2.fill(carpeta);
                g2.setColor(Color.decode("#F59E0B"));
                g2.fill(new Rectangle2D.Double(1.5, 6.5, 13, 7));
                break;
            }
            case SOL:
                g2.setColor(AMARILLO);
                g2.fill(new Ellipse2D.Double(4.75, 4.75, 6.5, 6.5));
                for (int i = 0; i < 8; i++) {
                    double ang = Math.toRadians(i * 45);
                    g2.draw(new Line2D.Double(8 + Math.cos(ang) * 5.4, 8 + Math.sin(ang) * 5.4,
                            8 + Math.cos(ang) * 7.2, 8 + Math.sin(ang) * 7.2));
                }
                break;
            case LUNA: {
                Area luna = new Area(new Ellipse2D.Double(2, 2, 12, 12));
                luna.subtract(new Area(new Ellipse2D.Double(6, 0, 11, 11)));
                g2.setColor(Color.decode("#818CF8"));
                g2.fill(luna);
                break;
            }
            case LAPIZ: {
                Graphics2D gl = (Graphics2D) g2.create();
                gl.rotate(Math.toRadians(-45), 8, 8);
                gl.setColor(Color.decode("#F59E0B"));
                gl.fill(new Rectangle2D.Double(3.5, 6.2, 8, 3.6));
                gl.setColor(Color.decode("#FDE3B5"));
                Path2D punta = new Path2D.Double();
                punta.moveTo(11.5, 6.2);
                punta.lineTo(15, 8);
                punta.lineTo(11.5, 9.8);
                punta.closePath();
                gl.fill(punta);
                gl.setColor(Color.decode("#7C2D12"));
                gl.fill(new Ellipse2D.Double(13.6, 7.3, 1.4, 1.4));
                gl.setColor(Color.decode("#F472B6"));
                gl.fill(new RoundRectangle2D.Double(1, 6.2, 2.5, 3.6, 1.2, 1.2));
                gl.dispose();
                break;
            }
            case PAPELERA:
                g2.draw(new Line2D.Double(2.5, 4, 13.5, 4));
                g2.draw(new RoundRectangle2D.Double(6, 1.8, 4, 2.2, 1, 1));
                Path2D cuerpo = new Path2D.Double();
                cuerpo.moveTo(3.7, 4);
                cuerpo.lineTo(4.6, 14);
                cuerpo.lineTo(11.4, 14);
                cuerpo.lineTo(12.3, 4);
                g2.draw(cuerpo);
                g2.draw(new Line2D.Double(6.6, 6.8, 6.8, 11.5));
                g2.draw(new Line2D.Double(9.4, 6.8, 9.2, 11.5));
                break;
            case RAYO: {
                Path2D rayo = new Path2D.Double();
                rayo.moveTo(9.5, 1);
                rayo.lineTo(3, 9.2);
                rayo.lineTo(7.4, 9.2);
                rayo.lineTo(6.3, 15);
                rayo.lineTo(13, 6.6);
                rayo.lineTo(8.6, 6.6);
                rayo.closePath();
                g2.setColor(NARANJA);
                g2.fill(rayo);
                break;
            }
            case DOCUMENTO: {
                Path2D hoja = new Path2D.Double();
                hoja.moveTo(3, 1.5);
                hoja.lineTo(9.5, 1.5);
                hoja.lineTo(13, 5);
                hoja.lineTo(13, 14.5);
                hoja.lineTo(3, 14.5);
                hoja.closePath();
                g2.setColor(Color.decode("#F1F5F9"));
                g2.fill(hoja);
                g2.setColor(Color.decode("#CBD5E1"));
                Path2D doblez = new Path2D.Double();
                doblez.moveTo(9.5, 1.5);
                doblez.lineTo(9.5, 5);
                doblez.lineTo(13, 5);
                doblez.closePath();
                g2.fill(doblez);
                g2.setColor(Color.decode("#EF4444"));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(5, 8, 11, 8));
                g2.draw(new Line2D.Double(5, 10.3, 11, 10.3));
                g2.draw(new Line2D.Double(5, 12.6, 9, 12.6));
                break;
            }
            case VOLTEAR: {
                g2.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 20, 150, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 200, 150, Arc2D.OPEN));
                Path2D f1 = new Path2D.Double();
                f1.moveTo(13.6, 3.6);
                f1.lineTo(13.4, 6.6);
                f1.lineTo(10.5, 6.2);
                g2.draw(f1);
                Path2D f2 = new Path2D.Double();
                f2.moveTo(2.4, 12.4);
                f2.lineTo(2.6, 9.4);
                f2.lineTo(5.5, 9.8);
                g2.draw(f2);
                break;
            }
            case EXPORTAR: {
                g2.draw(new Line2D.Double(8, 1.8, 8, 10));
                Path2D flecha = new Path2D.Double();
                flecha.moveTo(4.6, 6.8);
                flecha.lineTo(8, 10.2);
                flecha.lineTo(11.4, 6.8);
                g2.draw(flecha);
                Path2D bandeja = new Path2D.Double();
                bandeja.moveTo(2.2, 10.5);
                bandeja.lineTo(2.2, 14);
                bandeja.lineTo(13.8, 14);
                bandeja.lineTo(13.8, 10.5);
                g2.draw(bandeja);
                break;
            }
            case ZOOM_MAS:
            case ZOOM_MENOS:
                g2.draw(new Ellipse2D.Double(1.8, 1.8, 9, 9));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(10, 10, 14.2, 14.2));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(4.4, 6.3, 8.2, 6.3));
                if (tipo == Tipo.ZOOM_MAS) {
                    g2.draw(new Line2D.Double(6.3, 4.4, 6.3, 8.2));
                }
                break;
            case SELECCION:
                g2.setColor(GestorTema.conAlfa(GestorTema.actual().acento, 120));
                g2.fill(new RoundRectangle2D.Double(1.5, 5, 9, 6, 2, 2));
                g2.setColor(base);
                g2.draw(new Line2D.Double(12.5, 2.5, 12.5, 13.5));
                g2.draw(new Line2D.Double(10.8, 2.5, 14.2, 2.5));
                g2.draw(new Line2D.Double(10.8, 13.5, 14.2, 13.5));
                break;
            case ENGRANE: {
                Area engrane = new Area(new Ellipse2D.Double(3.2, 3.2, 9.6, 9.6));
                for (int i = 0; i < 8; i++) {
                    java.awt.geom.AffineTransform giro = java.awt.geom.AffineTransform.getRotateInstance(
                            Math.toRadians(i * 45), 8, 8);
                    engrane.add(new Area(giro.createTransformedShape(new RoundRectangle2D.Double(6.7, 0.8, 2.6, 3.6, 1, 1))));
                }
                engrane.subtract(new Area(new Ellipse2D.Double(5.6, 5.6, 4.8, 4.8)));
                g2.fill(engrane);
                break;
            }
            case MAS:
            case MENOS:
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(3, 8, 13, 8));
                if (tipo == Tipo.MAS) {
                    g2.draw(new Line2D.Double(8, 3, 8, 13));
                }
                break;
            case CLIP: {
                // Clip para "adjuntar"
                Path2D clip = new Path2D.Double();
                clip.moveTo(10.5, 5);
                clip.lineTo(10.5, 11.5);
                clip.quadTo(10.5, 14.5, 7.75, 14.5);
                clip.quadTo(5, 14.5, 5, 11.5);
                clip.lineTo(5, 3.8);
                clip.quadTo(5, 1.5, 7, 1.5);
                clip.quadTo(9, 1.5, 9, 3.8);
                clip.lineTo(9, 11);
                clip.quadTo(9, 12.3, 7.75, 12.3);
                clip.quadTo(6.5, 12.3, 6.5, 11);
                clip.lineTo(6.5, 5);
                g2.draw(clip);
                break;
            }
            case CERRAR:
                g2.draw(new Line2D.Double(3.5, 3.5, 12.5, 12.5));
                g2.draw(new Line2D.Double(12.5, 3.5, 3.5, 12.5));
                break;
            case OJO:
            case OJO_TACHADO: {
                Path2D ojo = new Path2D.Double();
                ojo.moveTo(1.2, 8);
                ojo.quadTo(8, 1.2, 14.8, 8);
                ojo.quadTo(8, 14.8, 1.2, 8);
                g2.draw(ojo);
                g2.fill(new Ellipse2D.Double(5.8, 5.8, 4.4, 4.4));
                if (tipo == Tipo.OJO_TACHADO) {
                    g2.draw(new Line2D.Double(2.5, 13.5, 13.5, 2.5));
                }
                break;
            }
            case LLAVE:
                g2.setColor(AMARILLO);
                g2.draw(new Ellipse2D.Double(1.5, 5, 6, 6));
                g2.draw(new Line2D.Double(7.5, 8, 14.5, 8));
                g2.draw(new Line2D.Double(12, 8, 12, 10.8));
                g2.draw(new Line2D.Double(14.2, 8, 14.2, 10));
                break;
            case EXPANDIR:
            case CONTRAER: {
                // Rectángulo (vista) con una flecha que apunta hacia fuera o hacia dentro
                g2.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 2, 2));
                g2.draw(new Line2D.Double(6, 2.5, 6, 13.5));
                Path2D flecha = new Path2D.Double();
                if (tipo == Tipo.EXPANDIR) {
                    flecha.moveTo(4.3, 6.2);
                    flecha.lineTo(2.8, 8);
                    flecha.lineTo(4.3, 9.8);
                } else {
                    flecha.moveTo(3.2, 6.2);
                    flecha.lineTo(4.7, 8);
                    flecha.lineTo(3.2, 9.8);
                }
                g2.draw(flecha);
                break;
            }
            case GUARDAR:
                g2.draw(new Line2D.Double(3, 8.5, 6.5, 12));
                g2.draw(new Line2D.Double(6.5, 12, 13, 4.5));
                break;
            case ESCUDO: {
                Path2D escudo = new Path2D.Double();
                escudo.moveTo(8, 1.5);
                escudo.lineTo(13.5, 3.5);
                escudo.curveTo(13.5, 9.5, 11, 12.8, 8, 14.5);
                escudo.curveTo(5, 12.8, 2.5, 9.5, 2.5, 3.5);
                escudo.closePath();
                g2.draw(escudo);
                g2.draw(new Line2D.Double(5.5, 8, 7.4, 10));
                g2.draw(new Line2D.Double(7.4, 10, 10.8, 6));
                break;
            }
            default:
                break;
        }
    }

    /** Genera una imagen del icono (para el icono de la ventana). */
    public BufferedImage comoImagen() {
        BufferedImage imagen = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagen.createGraphics();
        paintIcon(null, g2, 0, 0);
        g2.dispose();
        return imagen;
    }
}
