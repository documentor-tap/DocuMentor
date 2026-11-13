package documentor.modelo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Exporta flashcards o exámenes a archivos locales .txt o .csv usando {@link FileWriter}.
 * Compatible con Java 8+: usa la codificación del sistema, como Excel y el Bloc de notas.
 */
public class ExportadorMaterial {

    public enum Formato {
        TXT, CSV;

        public static Formato desdeArchivo(File archivo) {
            return archivo.getName().toLowerCase(Locale.ROOT).endsWith(".csv") ? CSV : TXT;
        }
    }

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportarFlashcards(List<Flashcard> listaFlashcards, File destino, String nombreDocumento)
            throws IOException {
        Formato formato = Formato.desdeArchivo(destino);
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(destino))) {
            if (formato == Formato.CSV) {
                escritor.write('﻿');
                escribirLineaCsv(escritor, "Numero", "Pregunta", "Respuesta");
                for (int i = 0; i < listaFlashcards.size(); i++) {
                    Flashcard f = listaFlashcards.get(i);
                    escribirLineaCsv(escritor, String.valueOf(i + 1), f.getPregunta(), f.getRespuesta());
                }
            } else {
                escribirEncabezadoTxt(escritor, "FLASHCARDS", nombreDocumento, listaFlashcards.size());
                for (int i = 0; i < listaFlashcards.size(); i++) {
                    Flashcard f = listaFlashcards.get(i);
                    escritor.write("Tarjeta " + (i + 1));
                    escritor.newLine();
                    escritor.write("P: " + f.getPregunta());
                    escritor.newLine();
                    escritor.write("R: " + f.getRespuesta().replace("\n", "\n   "));
                    escritor.newLine();
                    escritor.newLine();
                }
            }
        }
    }

    public void exportarExamen(List<PreguntaExamen> listaPreguntas, File destino, String nombreDocumento)
            throws IOException {
        Formato formato = Formato.desdeArchivo(destino);
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(destino))) {
            if (formato == Formato.CSV) {
                escritor.write('﻿');
                escribirLineaCsv(escritor, "Numero", "Enunciado", "Opcion A", "Opcion B", "Opcion C", "Opcion D",
                        "Respuesta correcta", "Texto de la respuesta correcta");
                for (int i = 0; i < listaPreguntas.size(); i++) {
                    PreguntaExamen p = listaPreguntas.get(i);
                    List<String> o = p.getOpciones();
                    escribirLineaCsv(escritor, String.valueOf(i + 1), p.getEnunciado(), o.get(0), o.get(1), o.get(2),
                            o.get(3), String.valueOf(PreguntaExamen.letraOpcion(p.getIndiceCorrecto())), p.getOpcionCorrecta());
                }
            } else {
                escribirEncabezadoTxt(escritor, "EXAMEN", nombreDocumento, listaPreguntas.size());
                escritor.write("Las respuestas correctas van marcadas con [X].");
                escritor.newLine();
                escritor.newLine();
                for (int i = 0; i < listaPreguntas.size(); i++) {
                    PreguntaExamen p = listaPreguntas.get(i);
                    escritor.write((i + 1) + ". " + p.getEnunciado());
                    escritor.newLine();
                    for (int k = 0; k < p.getOpciones().size(); k++) {
                        boolean correcta = k == p.getIndiceCorrecto();
                        // Solo caracteres ASCII: se ven bien con cualquier codificación de FileWriter
                        escritor.write((correcta ? "  [X] " : "  [ ] ") + PreguntaExamen.letraOpcion(k) + ") "
                                + p.getOpciones().get(k) + (correcta ? "   <-- RESPUESTA CORRECTA" : ""));
                        escritor.newLine();
                    }
                    escritor.newLine();
                }
                escritor.write("CLAVE DE RESPUESTAS");
                escritor.newLine();
                for (int i = 0; i < listaPreguntas.size(); i++) {
                    PreguntaExamen pregunta = listaPreguntas.get(i);
                    escritor.write((i + 1) + ") " + PreguntaExamen.letraOpcion(pregunta.getIndiceCorrecto())
                            + "  " + pregunta.getOpcionCorrecta());
                    escritor.newLine();
                }
            }
        }
    }

    private void escribirEncabezadoTxt(BufferedWriter escritor, String titulo, String nombreDocumento, int total)
            throws IOException {
        escritor.write("DocuMentor - " + titulo);
        escritor.newLine();
        escritor.write("Documento: " + nombreDocumento);
        escritor.newLine();
        escritor.write("Generado: " + LocalDateTime.now().format(FORMATO_FECHA) + "  |  Elementos: " + total);
        escritor.newLine();
        escritor.write("============================================================");
        escritor.newLine();
        escritor.newLine();
    }

    /**
     * FileWriter usa la codificación del sistema (en Windows suele ser windows-1252, que Excel
     * abre correctamente). Si el sistema usa UTF-8 se añade el BOM para que Excel lo detecte.
     */
    private void escribirMarcaUtf8(BufferedWriter escritor) throws IOException {
        if (StandardCharsets.UTF_8.equals(Charset.defaultCharset())) {
            escritor.write('\uFEFF');
        }
    }

    private void escribirLineaCsv(BufferedWriter escritor, String... campos) throws IOException {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linea.append(',');
            }
            linea.append('"').append(campos[i].replace("\"", "\"\"")).append('"');
        }
        escritor.write(linea.toString());
        escritor.write("\r\n");
    }
}
