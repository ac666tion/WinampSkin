package pl.lichessclassic.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** Enables TLS 1.2 on older Android while preserving standard validation. */
public final class Tls12SocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;
    public Tls12SocketFactory(SSLSocketFactory delegate) { this.delegate = delegate; }
    private Socket enable(Socket socket) {
        if (socket instanceof SSLSocket) ((SSLSocket) socket).setEnabledProtocols(new String[] { "TLSv1.2" });
        return socket;
    }
    @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
    @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
    @Override public Socket createSocket(Socket s, String h, int p, boolean a) throws IOException { return enable(delegate.createSocket(s, h, p, a)); }
    @Override public Socket createSocket(String h, int p) throws IOException { return enable(delegate.createSocket(h, p)); }
    @Override public Socket createSocket(String h, int p, InetAddress l, int lp) throws IOException { return enable(delegate.createSocket(h, p, l, lp)); }
    @Override public Socket createSocket(InetAddress h, int p) throws IOException { return enable(delegate.createSocket(h, p)); }
    @Override public Socket createSocket(InetAddress h, int p, InetAddress l, int lp) throws IOException { return enable(delegate.createSocket(h, p, l, lp)); }
    @Override public Socket createSocket() throws IOException { return enable(delegate.createSocket()); }
}
