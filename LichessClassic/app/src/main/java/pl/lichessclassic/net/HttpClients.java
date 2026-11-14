package pl.lichessclassic.net;

import android.os.Build;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;

public final class HttpClients {
    private HttpClients() { }

    public static OkHttpClient.Builder secureBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS);

        // Enable TLS 1.2 on Android 4.1-4.4 while retaining system CA and hostname validation.
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT <= 19) {
            try {
                TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init((KeyStore) null);
                X509TrustManager trustManager = null;
                for (TrustManager manager : trustFactory.getTrustManagers()) {
                    if (manager instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) manager;
                        break;
                    }
                }
                if (trustManager == null) throw new GeneralSecurityException("No system X509TrustManager");

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[] { trustManager }, null);
                builder.sslSocketFactory(new Tls12SocketFactory(context.getSocketFactory()), trustManager);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("TLS provider unavailable", e);
            }
        }
        return builder;
    }
}
