package documentor.modelo.ia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector/escritor JSON mínimo (sin dependencias externas) para hablar con la API.
 * Objetos → {@code Map<String,Object>}, arreglos → {@code List<Object>},
 * números → {@code Double}, booleanos → {@code Boolean}, null → {@code null}.
 */
public final class JsonSimple {

    private final String texto;
    private int pos;

    private JsonSimple(String texto) {
        this.texto = texto;
    }

    public static Object parsear(String texto) {
        JsonSimple lector = new JsonSimple(texto);
        lector.saltarEspacios();
        Object valor = lector.leerValor();
        lector.saltarEspacios();
        if (lector.pos != texto.length()) {
            throw lector.error("Texto sobrante después del JSON");
        }
        return valor;
    }

    /** Convierte un texto Java en literal JSON entre comillas. */
    public static String cadena(String valor) {
        StringBuilder sb = new StringBuilder(valor.length() + 16).append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------ Lectura

    private Object leerValor() {
        if (pos >= texto.length()) {
            throw error("JSON incompleto");
        }
        char c = texto.charAt(pos);
        switch (c) {
            case '{': return leerObjeto();
            case '[': return leerArreglo();
            case '"': return leerCadena();
            case 't': return literal("true", Boolean.TRUE);
            case 'f': return literal("false", Boolean.FALSE);
            case 'n': return literal("null", null);
            default:
                if (c == '-' || Character.isDigit(c)) {
                    return leerNumero();
                }
                throw error("Carácter inesperado '" + c + "'");
        }
    }

    private Map<String, Object> leerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        pos++;
        saltarEspacios();
        if (consumir('}')) {
            return objeto;
        }
        while (true) {
            saltarEspacios();
            if (pos >= texto.length() || texto.charAt(pos) != '"') {
                throw error("Se esperaba una clave");
            }
            String clave = leerCadena();
            saltarEspacios();
            if (!consumir(':')) {
                throw error("Se esperaba ':'");
            }
            saltarEspacios();
            objeto.put(clave, leerValor());
            saltarEspacios();
            if (consumir('}')) {
                return objeto;
            }
            if (!consumir(',')) {
                throw error("Se esperaba ',' o '}'");
            }
        }
    }

    private List<Object> leerArreglo() {
        List<Object> arreglo = new ArrayList<>();
        pos++;
        saltarEspacios();
        if (consumir(']')) {
            return arreglo;
        }
        while (true) {
            saltarEspacios();
            arreglo.add(leerValor());
            saltarEspacios();
            if (consumir(']')) {
                return arreglo;
            }
            if (!consumir(',')) {
                throw error("Se esperaba ',' o ']'");
            }
        }
    }

    private String leerCadena() {
        StringBuilder sb = new StringBuilder();
        pos++;
        while (pos < texto.length()) {
            char c = texto.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= texto.length()) {
                break;
            }
            char e = texto.charAt(pos++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    if (pos + 4 > texto.length()) {
                        throw error("Escape \\u incompleto");
                    }
                    sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                    pos += 4;
                    break;
                default:
                    sb.append(e);   // \" \\ \/
            }
        }
        throw error("Cadena sin cerrar");
    }

    private Double leerNumero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
            pos++;
        }
        try {
            return Double.valueOf(texto.substring(inicio, pos));
        } catch (NumberFormatException e) {
            throw error("Número inválido");
        }
    }

    private Object literal(String palabra, Object valor) {
        if (!texto.startsWith(palabra, pos)) {
            throw error("Literal inválido");
        }
        pos += palabra.length();
        return valor;
    }

    private boolean consumir(char c) {
        if (pos < texto.length() && texto.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String mensaje) {
        return new IllegalArgumentException("JSON inválido (posición " + pos + "): " + mensaje);
    }
}
