package documentor.modelo.ia;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Hace que Java confíe en los mismos certificados que Windows.
 *
 * <p>Java trae su propio almacén de certificados (cacerts) e ignora el de Windows.
 * Los antivirus que inspeccionan HTTPS (Kaspersky, Avast, ESET...) instalan su
 * certificado raíz solo en Windows, por eso Java rechaza la conexión con
 * "PKIX path building failed". Esta clase acepta un certificado si lo valida el
 * almacén de Java <b>o</b> el de Windows. No desactiva la seguridad: la cadena
 * de certificados y el nombre del servidor se siguen verificando.</p>
 */
final class ConfianzaCertificados {

    private static volatile SSLSocketFactory fabrica;

    private ConfianzaCertificados() {
    }

    /** Fábrica de sockets TLS con la confianza combinada (Java + Windows). */
    static SSLSocketFactory fabrica() {
        if (fabrica == null) {
            synchronized (ConfianzaCertificados.class) {
                if (fabrica == null) {
                    try {
                        fabrica = construir(almacenesDelSistema());
                    } catch (GeneralSecurityException e) {
                        fabrica = HttpsURLConnection.getDefaultSSLSocketFactory();
                    }
                }
            }
        }
        return fabrica;
    }

    /** En Windows devuelve el almacén "Windows-ROOT" (entidades raíz de confianza del sistema). */
    private static List<KeyStore> almacenesDelSistema() {
        List<KeyStore> almacenes = new ArrayList<>();
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            try {
                KeyStore windows = KeyStore.getInstance("Windows-ROOT");
                windows.load(null, null);
                almacenes.add(windows);
            } catch (Exception e) {
                // Proveedor no disponible: se usa solo el almacén de Java
            }
        }
        return almacenes;
    }

    /** Visible para pruebas: combina el almacén de Java con almacenes adicionales. */
    static SSLSocketFactory construir(List<KeyStore> adicionales) throws GeneralSecurityException {
        List<X509TrustManager> gestores = new ArrayList<>();
        gestores.add(gestorDe(null));   // null = cacerts de Java
        for (KeyStore almacen : adicionales) {
            gestores.add(gestorDe(almacen));
        }
        X509TrustManager combinado = new X509TrustManager() {
            @Override
            public void checkServerTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
                CertificateException ultimo = null;
                for (X509TrustManager gestor : gestores) {
                    try {
                        gestor.checkServerTrusted(cadena, tipo);
                        return;   // basta con que un almacén confíe en la cadena
                    } catch (CertificateException e) {
                        ultimo = e;
                    }
                }
                throw ultimo;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
                gestores.get(0).checkClientTrusted(cadena, tipo);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                List<X509Certificate> todos = new ArrayList<>();
                for (X509TrustManager gestor : gestores) {
                    for (X509Certificate c : gestor.getAcceptedIssuers()) {
                        todos.add(c);
                    }
                }
                return todos.toArray(new X509Certificate[0]);
            }
        };
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, new TrustManager[]{combinado}, null);
        return contexto.getSocketFactory();
    }

    private static X509TrustManager gestorDe(KeyStore almacen) throws GeneralSecurityException {
        TrustManagerFactory fabricaGestores = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        fabricaGestores.init(almacen);
        for (TrustManager gestor : fabricaGestores.getTrustManagers()) {
            if (gestor instanceof X509TrustManager) {
                return (X509TrustManager) gestor;
            }
        }
        throw new GeneralSecurityException("No hay un gestor X509 disponible");
    }
}
