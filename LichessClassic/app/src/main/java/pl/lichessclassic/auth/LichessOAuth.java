package pl.lichessclassic.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pl.lichessclassic.net.HttpClients;

/** OAuth Authorization Code + PKCE using a loopback redirect. */
public final class LichessOAuth {
    public interface Listener { void onSuccess(String username); void onError(String message); }
    private static final String CLIENT_ID = "pl.lichessclassic.android";
    private static final String REDIRECT_URI = "http://127.0.0.1:43821/callback";
    private static final int PORT = 43821;
    private static final String PREF_TOKEN = "lichess_access_token";
    private static final String PREF_VERIFIER = "oauth_verifier";
    private static final String PREF_STATE = "oauth_state";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private LichessOAuth() { }
    public static String getToken(Activity a) {
        return PreferenceManager.getDefaultSharedPreferences(a).getString(PREF_TOKEN, null);
    }
    public static void clearToken(Activity a) {
        PreferenceManager.getDefaultSharedPreferences(a).edit().remove(PREF_TOKEN).apply();
    }

    public static void begin(final Activity activity, final Listener listener) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        try {
            final String verifier = randomUrlSafe(64);
            final String state = randomUrlSafe(32);
            final String challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes("US-ASCII")));
            prefs.edit().putString(PREF_VERIFIER, verifier).putString(PREF_STATE, state).apply();
            final CountDownLatch serverReady = new CountDownLatch(1);
            Thread callbackThread = new Thread(new Runnable() {
                @Override public void run() { listenForCallback(listener, prefs, serverReady); }
            }, "LichessOAuthCallback");
            callbackThread.setDaemon(true);
            callbackThread.start();
            if (!serverReady.await(5, TimeUnit.SECONDS))
                throw new Exception("Nie udało się uruchomić lokalnego odbiornika logowania.");
            String url = "https://lichess.org/oauth?response_type=code"
                    + "&client_id=" + enc(CLIENT_ID) + "&redirect_uri=" + enc(REDIRECT_URI)
                    + "&scope=" + enc("board:play challenge:read challenge:write")
                    + "&state=" + enc(state) + "&code_challenge=" + enc(challenge)
                    + "&code_challenge_method=S256";
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            prefs.edit().remove(PREF_VERIFIER).remove(PREF_STATE).apply();
            listener.onError("Nie udało się rozpocząć logowania: " + safeMessage(e));
        }
    }

    private static void listenForCallback(final Listener listener, final SharedPreferences prefs,
                                          CountDownLatch ready) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(240000);
            ready.countDown();
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String requestLine = in.readLine();
            String line;
            while ((line = in.readLine()) != null && line.length() > 0) { }
            String target = "/";
            if (requestLine != null) {
                String[] parts = requestLine.split(" ");
                if (parts.length > 1) target = parts[1];
            }
            Uri callback = Uri.parse("http://127.0.0.1" + target);
            final String code = callback.getQueryParameter("code");
            final String returnedState = callback.getQueryParameter("state");
            final String error = callback.getQueryParameter("error");
            byte[] body = ("<html><meta charset='utf-8'><body style='font-family:sans-serif;text-align:center;padding:30px'>"
                    + "<h2>Możesz wrócić do Lichess Classic</h2><p>Logowanie przekazano do aplikacji.</p></body></html>").getBytes("UTF-8");
            OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\nContent-Length: "
                    + body.length + "\r\n\r\n").getBytes("US-ASCII"));
            out.write(body);
            out.flush();
            socket.close();
            if (error != null) throw new Exception("Logowanie anulowane (" + error + ").");
            String expected = prefs.getString(PREF_STATE, null);
            if (code == null || expected == null || !expected.equals(returnedState))
                throw new Exception("Nieprawidłowa odpowiedź logowania (kontrola state).");
            exchangeCode(listener, prefs, code);
        } catch (final Exception e) {
            ready.countDown();
            MAIN.post(new Runnable() {
                @Override public void run() {
                    prefs.edit().remove(PREF_VERIFIER).remove(PREF_STATE).apply();
                    listener.onError("Logowanie nie powiodło się: " + safeMessage(e));
                }
            });
        } finally {
            if (server != null) try { server.close(); } catch (Exception ignored) { }
        }
    }

    private static void exchangeCode(final Listener listener, final SharedPreferences prefs, final String code) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String verifier = prefs.getString(PREF_VERIFIER, null);
                    if (verifier == null) throw new Exception("Brak klucza PKCE. Rozpocznij logowanie ponownie.");
                    OkHttpClient client = HttpClients.secureBuilder().build();
                    Request request = new Request.Builder().url("https://lichess.org/api/token")
                            .post(new FormBody.Builder().add("grant_type", "authorization_code").add("code", code)
                                    .add("code_verifier", verifier).add("client_id", CLIENT_ID)
                                    .add("redirect_uri", REDIRECT_URI).build())
                            .header("Accept", "application/json")
                            .header("User-Agent", "LichessClassic/0.2.1 (Android)").build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() == null ? "" : response.body().string();
                    int status = response.code();
                    response.close();
                    if (status < 200 || status >= 300)
                        throw new Exception("Serwer OAuth zwrócił HTTP " + status + ".");
                    final String token = new org.json.JSONObject(responseBody).optString("access_token", "");
                    if (token.length() == 0) throw new Exception("Odpowiedź Lichess nie zawiera tokenu.");

                    Request account = new Request.Builder().url("https://lichess.org/api/account")
                            .header("Authorization", "Bearer " + token).header("Accept", "application/json").get().build();
                    Response accountResponse = client.newCall(account).execute();
                    String accountBody = accountResponse.body() == null ? "" : accountResponse.body().string();
                    int accountStatus = accountResponse.code();
                    accountResponse.close();
                    if (accountStatus < 200 || accountStatus >= 300)
                        throw new Exception("Weryfikacja konta zwróciła HTTP " + accountStatus + ".");
                    final String username = new org.json.JSONObject(accountBody).optString("username", "Lichess");
                    prefs.edit().putString(PREF_TOKEN, token).remove(PREF_VERIFIER).remove(PREF_STATE).apply();
                    MAIN.post(new Runnable() {
                        @Override public void run() { listener.onSuccess(username); }
                    });
                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        @Override public void run() {
                            prefs.edit().remove(PREF_VERIFIER).remove(PREF_STATE).apply();
                            listener.onError("Nie udało się wymienić kodu na token: " + safeMessage(e));
                        }
                    });
                }
            }
        }, "LichessOAuthExchange").start();
    }

    private static String randomUrlSafe(int bytes) throws Exception {
        byte[] data = new byte[bytes];
        new SecureRandom().nextBytes(data);
        return base64Url(data);
    }
    private static String base64Url(byte[] data) {
        return android.util.Base64.encodeToString(data,
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
    }
    private static String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }
    private static String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
