package documentor.modelo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Extractor de texto PDF mínimo, sin dependencias externas.
 *
 * <p>Descomprime los flujos /FlateDecode e interpreta los operadores de texto
 * (Tj, TJ, ', ", Td, TD, T*, Tm). Funciona con PDFs sencillos que usan fuentes
 * estándar (WinAnsi). PDFs escaneados o con fuentes CID (muy comunes al exportar
 * desde Word) requieren PDFBox; en ese caso se lanza un error descriptivo.</p>
 */
final class ExtractorPdfBasico {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final Object MARCA_ARREGLO = new Object();
    private static final Pattern PATRON_FUENTE_INCRUSTADA = Pattern.compile("/Length[123]\\s");
    private static final Pattern PATRON_FILTROS = Pattern.compile("/Filter(?:\\[((?:/\\w+)+)\\]|((?:/\\w+)))");

    /** Cadena literal de PDF (para distinguirla de nombres como /F1). */
    private static final class Cadena {
        final String valor;

        Cadena(String valor) {
            this.valor = valor;
        }
    }

    private Double ultimaY;

    String extraer(File archivo) throws IOException {
        byte[] datos = Files.readAllBytes(archivo.toPath());
        String crudo = new String(datos, StandardCharsets.ISO_8859_1);
        if (!crudo.startsWith("%PDF")) {
            throw new IOException("El archivo no es un PDF válido.");
        }
        if (crudo.contains("/Encrypt")) {
            throw new IOException("El PDF está protegido/cifrado y no se puede leer.");
        }

        StringBuilder texto = new StringBuilder();
        int posicion = 0;
        while (true) {
            int inicioStream = crudo.indexOf("stream", posicion);
            if (inicioStream < 0) {
                break;
            }
            if (inicioStream >= 3 && crudo.startsWith("end", inicioStream - 3)) {
                posicion = inicioStream + 6;
                continue;
            }
            int inicioDatos = inicioStream + 6;
            if (inicioDatos < crudo.length() && crudo.charAt(inicioDatos) == '\r') {
                inicioDatos++;
            }
            if (inicioDatos < crudo.length() && crudo.charAt(inicioDatos) == '\n') {
                inicioDatos++;
            }
            int finDatos = crudo.indexOf("endstream", inicioDatos);
            if (finDatos < 0) {
                break;
            }
            posicion = finDatos + 9;

            String diccionario = crudo.substring(Math.max(0, inicioStream - 600), inicioStream);
            int inicioObjeto = diccionario.lastIndexOf(" obj");
            if (inicioObjeto >= 0) {
                diccionario = diccionario.substring(inicioObjeto);
            }
            String dicc = diccionario.replaceAll("\\s+", "");
            // /Length1, /Length2 y /Length3 solo aparecen en fuentes incrustadas. Se revisan sobre el
            // diccionario ORIGINAL: sin espacios, "/Length 269" se confundiría con "/Length2".
            if (esFlujoNoTextual(dicc) || PATRON_FUENTE_INCRUSTADA.matcher(diccionario).find()) {
                continue;
            }

            byte[] contenido = aplicarFiltros(Arrays.copyOfRange(datos, inicioDatos, finDatos), dicc);
            if (contenido == null) {
                continue;   // filtro no soportado (LZW, RunLength...) o datos dañados
            }

            String operaciones = new String(contenido, StandardCharsets.ISO_8859_1);
            if (operaciones.contains("BT") && (operaciones.contains("Tj") || operaciones.contains("TJ"))) {
                ultimaY = null;
                texto.append(interpretarContenido(operaciones)).append('\n');
            }
        }

        String resultado = texto.toString()
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFD]", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
        if (!esTextoLegible(resultado)) {
            throw new IOException("No fue posible extraer texto legible de este PDF (puede estar escaneado "
                    + "o usar fuentes incrustadas).\nAgrega Apache PDFBox al classpath o convierte el documento a .txt.");
        }
        return resultado;
    }

    private static boolean esFlujoNoTextual(String dicc) {
        String[] marcas = {"/Subtype/Image", "/Type/ObjStm", "/Type/XRef", "/Subtype/Type1C",
                "/Subtype/CIDFontType0C", "/Subtype/OpenType", "/Type/Metadata",
                "/DCTDecode", "/JPXDecode", "/CCITTFaxDecode", "/JBIG2Decode"};
        for (String marca : marcas) {
            if (dicc.contains(marca)) {
                return true;
            }
        }
        return false;
    }

    /** Aplica en orden los filtros declarados en /Filter (Flate, ASCII85, ASCIIHex). */
    private static byte[] aplicarFiltros(byte[] datos, String dicc) {
        Matcher m = PATRON_FILTROS.matcher(dicc);
        if (!m.find()) {
            return datos;
        }
        String declaracion = m.group(1) != null ? m.group(1) : m.group(2);
        byte[] resultado = datos;
        for (String filtro : declaracion.split("/")) {
            if (filtro.isEmpty()) {
                continue;
            }
            switch (filtro) {
                case "FlateDecode":
                case "Fl":
                    resultado = descomprimir(resultado);
                    break;
                case "ASCII85Decode":
                case "A85":
                    resultado = decodificarAscii85(resultado);
                    break;
                case "ASCIIHexDecode":
                case "AHx":
                    resultado = decodificarHex(new String(resultado, StandardCharsets.ISO_8859_1).replace(">", ""))
                            .getBytes(StandardCharsets.ISO_8859_1);
                    break;
                default:
                    return null;
            }
            if (resultado == null) {
                return null;
            }
        }
        return resultado;
    }

    private static byte[] decodificarAscii85(byte[] datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream(datos.length);
        long tupla = 0;
        int cuenta = 0;
        for (byte b : datos) {
            char c = (char) (b & 0xFF);
            if (c == '~') {
                break;   // fin de datos "~>"
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == 'z' && cuenta == 0) {
                salida.write(0);
                salida.write(0);
                salida.write(0);
                salida.write(0);
                continue;
            }
            if (c < '!' || c > 'u') {
                return null;
            }
            tupla = tupla * 85 + (c - '!');
            if (++cuenta == 5) {
                for (int k = 3; k >= 0; k--) {
                    salida.write((int) (tupla >> (8 * k)) & 0xFF);
                }
                tupla = 0;
                cuenta = 0;
            }
        }
        if (cuenta > 1) {
            for (int k = cuenta; k < 5; k++) {
                tupla = tupla * 85 + 84;
            }
            for (int k = 3; k >= 5 - cuenta; k--) {
                salida.write((int) (tupla >> (8 * k)) & 0xFF);
            }
        }
        return salida.toByteArray();
    }

    private static byte[] descomprimir(byte[] comprimido) {
        Inflater inflater = new Inflater();
        inflater.setInput(comprimido);
        ByteArrayOutputStream salida = new ByteArrayOutputStream(comprimido.length * 3);
        byte[] bufer = new byte[8192];
        try {
            while (!inflater.finished()) {
                int leidos = inflater.inflate(bufer);
                if (leidos == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                salida.write(bufer, 0, leidos);
            }
        } catch (DataFormatException e) {
            if (salida.size() == 0) {
                return null;
            }
        } finally {
            inflater.end();
        }
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------ Intérprete de operadores

    private String interpretarContenido(String ops) {
        StringBuilder salida = new StringBuilder();
        List<Object> operandos = new ArrayList<>();
        int i = 0;
        int n = ops.length();
        while (i < n) {
            char c = ops.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '%') {
                while (i < n && ops.charAt(i) != '\n' && ops.charAt(i) != '\r') {
                    i++;
                }
            } else if (c == '(') {
                StringBuilder cadena = new StringBuilder();
                i = leerCadenaLiteral(ops, i, cadena);
                operandos.add(new Cadena(cadena.toString()));
            } else if (c == '<') {
                if (i + 1 < n && ops.charAt(i + 1) == '<') {
                    i = saltarDiccionario(ops, i);
                } else {
                    int fin = ops.indexOf('>', i + 1);
                    if (fin < 0) {
                        break;
                    }
                    operandos.add(new Cadena(decodificarHex(ops.substring(i + 1, fin))));
                    i = fin + 1;
                }
            } else if (c == '[') {
                operandos.add(MARCA_ARREGLO);
                i++;
            } else if (c == ']') {
                int marca = operandos.lastIndexOf(MARCA_ARREGLO);
                List<Object> arreglo = new ArrayList<>();
                if (marca >= 0) {
                    arreglo.addAll(operandos.subList(marca + 1, operandos.size()));
                    operandos.subList(marca, operandos.size()).clear();
                }
                operandos.add(arreglo);
                i++;
            } else if (c == '/') {
                int j = i + 1;
                while (j < n && !esDelimitador(ops.charAt(j))) {
                    j++;
                }
                operandos.add(ops.substring(i, j));
                i = j;
            } else if (c == '{' || c == '}' || c == '>' || c == ')') {
                i++;
            } else {
                int j = i;
                while (j < n && !esDelimitador(ops.charAt(j))) {
                    j++;
                }
                if (j == i) {
                    i++;
                    continue;
                }
                String token = ops.substring(i, j);
                i = j;
                Double numero = aNumero(token);
                if (numero != null) {
                    operandos.add(numero);
                    continue;
                }
                if ("BI".equals(token)) {   // imagen en línea: se salta completa
                    int fin = ops.indexOf("EI", i);
                    i = fin < 0 ? n : fin + 2;
                } else {
                    ejecutarOperador(token, operandos, salida);
                }
                operandos.clear();
            }
        }
        return salida.toString();
    }

    private void ejecutarOperador(String operador, List<Object> operandos, StringBuilder salida) {
        switch (operador) {
            case "Tj":
                agregarCadena(salida, ultimaCadena(operandos));
                break;
            case "'":
            case "\"":
                nuevaLinea(salida);
                agregarCadena(salida, ultimaCadena(operandos));
                break;
            case "TJ":
                if (!operandos.isEmpty() && operandos.get(operandos.size() - 1) instanceof List) {
                    for (Object elemento : (List<?>) operandos.get(operandos.size() - 1)) {
                        if (elemento instanceof Cadena) {
                            agregarCadena(salida, ((Cadena) elemento).valor);
                        } else if (elemento instanceof Double && (Double) elemento < -180) {
                            espacio(salida);
                        }
                    }
                }
                break;
            case "Td":
            case "TD": {
                double ty = numeroEn(operandos, 1);
                double tx = numeroEn(operandos, 2);
                if (Math.abs(ty) > 0.1) {
                    nuevaLinea(salida);
                } else if (tx > 0) {
                    espacio(salida);
                }
                break;
            }
            case "T*":
                nuevaLinea(salida);
                break;
            case "Tm": {
                double y = numeroEn(operandos, 1);
                if (ultimaY != null && Math.abs(y - ultimaY) > 0.1) {
                    nuevaLinea(salida);
                } else {
                    espacio(salida);
                }
                ultimaY = y;
                break;
            }
            case "ET":
                espacio(salida);
                break;
            default:
                // Operadores gráficos: no aportan texto
        }
    }

    private static String ultimaCadena(List<Object> operandos) {
        for (int k = operandos.size() - 1; k >= 0; k--) {
            if (operandos.get(k) instanceof Cadena) {
                return ((Cadena) operandos.get(k)).valor;
            }
        }
        return "";
    }

    /** Devuelve el operando numérico en la posición contada desde el final (1 = último). */
    private static double numeroEn(List<Object> operandos, int desdeElFinal) {
        int indice = operandos.size() - desdeElFinal;
        if (indice >= 0 && operandos.get(indice) instanceof Double) {
            return (Double) operandos.get(indice);
        }
        return 0;
    }

    private static void agregarCadena(StringBuilder salida, String bytesLatin1) {
        if (bytesLatin1.isEmpty()) {
            return;
        }
        salida.append(new String(bytesLatin1.getBytes(StandardCharsets.ISO_8859_1), WINDOWS_1252));
    }

    private static void espacio(StringBuilder salida) {
        int largo = salida.length();
        if (largo > 0 && !Character.isWhitespace(salida.charAt(largo - 1))) {
            salida.append(' ');
        }
    }

    private static void nuevaLinea(StringBuilder salida) {
        int largo = salida.length();
        while (largo > 0 && salida.charAt(largo - 1) == ' ') {
            salida.setLength(--largo);
        }
        if (largo > 0 && salida.charAt(largo - 1) != '\n') {
            salida.append('\n');
        }
    }

    private static int leerCadenaLiteral(String ops, int inicio, StringBuilder destino) {
        int profundidad = 0;
        int i = inicio;
        int n = ops.length();
        while (i < n) {
            char c = ops.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char siguiente = ops.charAt(i + 1);
                i += 2;
                switch (siguiente) {
                    case 'n': destino.append('\n'); break;
                    case 'r': destino.append('\r'); break;
                    case 't': destino.append('\t'); break;
                    case 'b': destino.append('\b'); break;
                    case 'f': destino.append('\f'); break;
                    case '\r':
                        if (i < n && ops.charAt(i) == '\n') {
                            i++;
                        }
                        break;
                    case '\n':
                        break;
                    default:
                        if (siguiente >= '0' && siguiente <= '7') {
                            int valor = siguiente - '0';
                            for (int k = 0; k < 2 && i < n && ops.charAt(i) >= '0' && ops.charAt(i) <= '7'; k++) {
                                valor = valor * 8 + (ops.charAt(i++) - '0');
                            }
                            destino.append((char) (valor & 0xFF));
                        } else {
                            destino.append(siguiente);   // \( \) \\
                        }
                }
                continue;
            }
            if (c == '(') {
                if (profundidad > 0) {
                    destino.append(c);
                }
                profundidad++;
            } else if (c == ')') {
                profundidad--;
                if (profundidad == 0) {
                    return i + 1;
                }
                destino.append(c);
            } else {
                destino.append(c);
            }
            i++;
        }
        return n;
    }

    private static int saltarDiccionario(String ops, int inicio) {
        int profundidad = 0;
        int i = inicio;
        while (i < ops.length() - 1) {
            if (ops.charAt(i) == '<' && ops.charAt(i + 1) == '<') {
                profundidad++;
                i += 2;
            } else if (ops.charAt(i) == '>' && ops.charAt(i + 1) == '>') {
                profundidad--;
                i += 2;
                if (profundidad == 0) {
                    return i;
                }
            } else {
                i++;
            }
        }
        return ops.length();
    }

    private static String decodificarHex(String hex) {
        String limpio = hex.replaceAll("\\s+", "");
        if (limpio.length() % 2 != 0) {
            limpio += "0";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < limpio.length(); i += 2) {
            try {
                sb.append((char) Integer.parseInt(limpio.substring(i, i + 2), 16));
            } catch (NumberFormatException e) {
                return "";
            }
        }
        return sb.toString();
    }

    private static boolean esDelimitador(char c) {
        return Character.isWhitespace(c) || "()<>[]{}/%".indexOf(c) >= 0;
    }

    private static Double aNumero(String token) {
        if (token.isEmpty()) {
            return null;
        }
        char primero = token.charAt(0);
        if (!(Character.isDigit(primero) || primero == '-' || primero == '+' || primero == '.')) {
            return null;
        }
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean esTextoLegible(String texto) {
        if (texto.length() < 20) {
            return false;
        }
        int letras = 0;
        int validos = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isLetter(c)) {
                letras++;
            }
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || ".,;:¿?¡!()\"'-–—«»%/".indexOf(c) >= 0) {
                validos++;
            }
        }
        return letras >= 15 && validos >= texto.length() * 0.85;
    }
}
