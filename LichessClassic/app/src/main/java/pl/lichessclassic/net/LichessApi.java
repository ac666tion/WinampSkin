package pl.lichessclassic.net;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Initial wrapper for a few Lichess Board API operations. */
public class LichessApi {
    public interface Result {
        void onSuccess(String body);
        void onError(String message);
    }

    private static final String BASE = "https://lichess.org";
    private final OkHttpClient client;
    private final String token;

    public LichessApi(String accessToken) {
        this.token = accessToken;
        this.client = HttpClients.secureBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build();
    }

    private Request.Builder authorized(String url) {
        return new Request.Builder().url(url)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "LichessClassic/0.2.1 (Android)")
                .header("Accept", "application/json");
    }

    private void post(String path, RequestBody body, final Result result) {
        Request request = authorized(BASE + path).post(body).build();
        client.newCall(request).enqueue(callback(result));
    }

    private Callback callback(final Result result) {
        return new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                result.onError(e.getMessage() == null ? "Network error" : e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (response.isSuccessful()) result.onSuccess(body);
                else result.onError("HTTP " + response.code() + ": " + body);
                response.close();
            }
        };
    }

    public void seek(int minutes, int incrementSeconds, boolean rated, String color, final Result result) {
        FormBody body = new FormBody.Builder()
                .add("time", String.valueOf(minutes))
                .add("increment", String.valueOf(incrementSeconds))
                .add("rated", String.valueOf(rated))
                .add("variant", "standard")
                .add("color", color == null ? "random" : color).build();
        post("/api/board/seek", body, result);
    }

    public void challengeUser(String username, int minutes, int incrementSeconds,
                              boolean rated, String color, Result result) {
        try {
            String user = URLEncoder.encode(username, "UTF-8");
            FormBody body = new FormBody.Builder()
                    .add("clock.limit", String.valueOf(minutes * 60))
                    .add("clock.increment", String.valueOf(incrementSeconds))
                    .add("rated", String.valueOf(rated))
                    .add("variant", "standard")
                    .add("color", color == null ? "random" : color).build();
            post("/api/challenge/" + user, body, result);
        } catch (Exception e) {
            result.onError(e.getMessage());
        }
    }

    public void makeMove(String gameId, String uci, Result result) {
        try {
            String id = URLEncoder.encode(gameId, "UTF-8");
            String move = URLEncoder.encode(uci, "UTF-8");
            post("/api/board/game/" + id + "/move/" + move,
                    new FormBody.Builder().build(), result);
        } catch (Exception e) {
            result.onError(e.getMessage());
        }
    }

    public void account(Result result) {
        Request request = authorized(BASE + "/api/account").get().build();
        client.newCall(request).enqueue(callback(result));
    }

    public Call openGameStream(String gameId) {
        String id;
        try { id = URLEncoder.encode(gameId, "UTF-8"); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
        Request request = authorized(BASE + "/api/board/game/stream/" + id)
                .header("Accept", "application/x-ndjson").get().build();
        return client.newCall(request);
    }
}
