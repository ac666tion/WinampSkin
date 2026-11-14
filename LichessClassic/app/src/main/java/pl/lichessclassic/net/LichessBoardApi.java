package pl.lichessclassic.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Minimal Lichess Board API client for public random seeks.
 * Keeps the global event stream open before creating the seek, as required by Lichess.
 */
public final class LichessBoardApi {
    public interface Listener {
        void onStatus(String message);
        void onGameFound(String gameId);
        void onError(String message);
    }

    private final OkHttpClient client;
    private volatile boolean searching;
    private volatile boolean gameFound;
    private volatile Call eventCall;
    private volatile Call seekCall;
    private volatile Listener listener;

    public LichessBoardApi() {
        client = HttpClients.secureBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void startRandomSeek(final String token, Listener callback) {
        cancel();
        listener = callback;
        searching = true;
        gameFound = false;
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                readEvents(token);
            }
        }, "LichessEvents");
        thread.setDaemon(true);
        thread.start();
    }

    public void cancel() {
        searching = false;
        Call currentSeek = seekCall;
        if (currentSeek != null) currentSeek.cancel();
        Call currentEvents = eventCall;
        if (currentEvents != null) currentEvents.cancel();
    }

    private void readEvents(final String token) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url("https://lichess.org/api/stream/event")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/x-ndjson")
                    .header("User-Agent", "LichessClassic/0.2.2 (Android)")
                    .get().build();
            eventCall = client.newCall(request);
            response = eventCall.execute();
            if (!response.isSuccessful()) {
                throw new Exception("Strumień zdarzeń Lichess: HTTP " + response.code() + ".");
            }
            notifyStatus("Połączono z Lichess. Szukam przeciwnika (10+0, partia nierankingowa)…");
            startSeek(token);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), "UTF-8"));
            String line;
            while (searching && (line = reader.readLine()) != null) {
                if (line.length() == 0) continue;
                try {
                    JSONObject event = new JSONObject(line);
                    if ("gameStart".equals(event.optString("type"))) {
                        JSONObject game = event.optJSONObject("game");
                        if (game != null) {
                            String id = game.optString("gameId", game.optString("id", ""));
                            if (id.length() > 0) {
                                gameFound = true;
                                searching = false;
                                Call currentSeek = seekCall;
                                if (currentSeek != null) currentSeek.cancel();
                                notifyGameFound(id);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Ignore malformed or future event types; keep reading the stream.
                }
            }
            if (searching && !gameFound) {
                searching = false;
                Call currentSeek = seekCall;
                if (currentSeek != null) currentSeek.cancel();
                notifyStatus("Połączenie z Lichess zostało przerwane. Spróbuj ponownie.");
            }
        } catch (final Exception e) {
            if (searching) {
                searching = false;
                Call currentSeek = seekCall;
                if (currentSeek != null) currentSeek.cancel();
                notifyError("Nie udało się połączyć z Lichess: " + safeMessage(e));
            }
        } finally {
            if (response != null) response.close();
        }
    }

    private void startSeek(final String token) {
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                Response response = null;
                try {
                    if (!searching) return;
                    FormBody form = new FormBody.Builder()
                            .add("time", "10")
                            .add("increment", "0")
                            .add("rated", "false")
                            .add("variant", "standard")
                            .build();
                    Request request = new Request.Builder()
                            .url("https://lichess.org/api/board/seek")
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "application/x-ndjson")
                            .header("User-Agent", "LichessClassic/0.2.2 (Android)")
                            .post(form).build();
                    seekCall = client.newCall(request);
                    if (!searching) {
                        seekCall.cancel();
                        return;
                    }
                    response = seekCall.execute();
                    if (!response.isSuccessful()) {
                        throw new Exception("Wyszukiwanie przeciwnika: HTTP " + response.code() + ".");
                    }
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream(), "UTF-8"));
                    while (searching && reader.readLine() != null) {
                        // Empty lines are Lichess keep-alive messages. The open response keeps the seek active.
                    }
                    if (searching && !gameFound) {
                        // Give the global event stream a brief moment to deliver gameStart.
                        Thread.sleep(1500);
                        if (searching && !gameFound) {
                            searching = false;
                            Call currentEvents = eventCall;
                            if (currentEvents != null) currentEvents.cancel();
                            notifyStatus("Wyszukiwanie zakończyło się bez rozpoczęcia partii. Spróbuj ponownie.");
                        }
                    }
                } catch (final Exception e) {
                    if (searching) {
                        searching = false;
                        Call currentEvents = eventCall;
                        if (currentEvents != null) currentEvents.cancel();
                        notifyError("Nie udało się rozpocząć wyszukiwania: " + safeMessage(e));
                    }
                } finally {
                    if (response != null) response.close();
                }
            }
        }, "LichessSeek");
        thread.setDaemon(true);
        thread.start();
    }

    private void notifyStatus(String message) {
        Listener current = listener;
        if (current != null) current.onStatus(message);
    }

    private void notifyGameFound(String gameId) {
        Listener current = listener;
        if (current != null) current.onGameFound(gameId);
    }

    private void notifyError(String message) {
        Listener current = listener;
        if (current != null) current.onError(message);
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
