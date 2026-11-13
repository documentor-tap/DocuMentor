package documentor.modelo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Lee archivos .txt y .pdf y los convierte en un {@link Documento}.
 *
 * <p>Para PDF intenta primero usar Apache PDFBox (2.x o 3.x) si su .jar está en el
 * classpath (se invoca por reflexión, así el proyecto compila sin dependencias).
 * Si no está disponible usa {@link ExtractorPdfBasico}, que funciona con PDFs
 * sencillos generados con fuentes estándar.</p>
 */
public class LectorDocumentos {

    private static final long TAMANO_MAXIMO_BYTES = 50L * 1024 * 1024;

    public Documento leer(File archivo) throws IOException {
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("El archivo seleccionado no existe.");
        }
        if (!archivo.canRead()) {
            throw new IOException("No hay permisos para leer el archivo.");
        }
        if (archivo.length() > TAMANO_MAXIMO_BYTES) {
            throw new IOException("El archivo supera el límite de 50 MB.");
        }

        String extension = obtenerExtension(archivo.getName());
        String texto;
        switch (extension) {
            case "txt":
                texto = leerTextoPlano(archivo);
                break;
            case "pdf":
                texto = leerPdf(archivo);
                break;
            default:
                throw new IOException("Formato no soportado: ." + extension + " (usa .txt o .pdf).");
        }

        texto = normalizar(texto);
        if (texto.trim().isEmpty()) {
            throw new IOException("El documento no contiene texto legible.");
        }
        return new Documento(archivo.getName(), texto, archivo);
    }

    private String leerTextoPlano(File archivo) throws IOException {
        byte[] bytes = Files.readAllBytes(archivo.toPath());
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            // Archivos guardados con el Bloc de notas antiguo suelen venir en ANSI (Windows-1252)
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private String leerPdf(File archivo) throws IOException {
        String texto = intentarConPdfBox(archivo);
        return texto != null ? texto : new ExtractorPdfBasico().extraer(archivo);
    }

    /** Usa PDFBox si está en el classpath. Devuelve null si la librería no está disponible. */
    private String intentarConPdfBox(File archivo) throws IOException {
        Object documentoPdf = null;
        try {
            Class<?> clasePdDocument = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            try {   // PDFBox 3.x
                Class<?> loader = Class.forName("org.apache.pdfbox.Loader");
                documentoPdf = loader.getMethod("loadPDF", File.class).invoke(null, archivo);
            } catch (ClassNotFoundException e) {   // PDFBox 2.x
                documentoPdf = clasePdDocument.getMethod("load", File.class).invoke(null, archivo);
            }
            Class<?> claseStripper = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object stripper = claseStripper.getConstructor().newInstance();
            Method getText = claseStripper.getMethod("getText", clasePdDocument);
            return (String) getText.invoke(stripper, documentoPdf);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            throw new IOException("PDFBox no pudo leer el PDF: " + (causa != null ? causa.getMessage() : e.getMessage()), causa);
        } catch (ReflectiveOperationException e) {
            return null;
        } finally {
            if (documentoPdf instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) documentoPdf).close();
                } catch (Exception ignorada) {
                    // nada que hacer al cerrar
                }
            }
        }
    }

    private static String normalizar(String texto) {
        String t = texto.replace("﻿", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .replaceAll("[ \\t\\x0B\\f]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }

    private static String obtenerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
