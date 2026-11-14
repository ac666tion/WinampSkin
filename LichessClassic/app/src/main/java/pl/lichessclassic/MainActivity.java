package pl.lichessclassic;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AppCompatActivity;

import pl.lichessclassic.auth.LichessOAuth;

public class MainActivity extends AppCompatActivity {
    private TextView accountStatus;
    private Button loginButton;

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(26), dp(20), dp(20));
        page.setBackgroundColor(Color.rgb(243, 245, 247));

        TextView brand = new TextView(this); brand.setText("LICHESS CLASSIC"); brand.setTextSize(13);
        brand.setTextColor(Color.rgb(93, 112, 128)); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD); page.addView(brand);
        TextView title = new TextView(this); title.setText("Szachy. Bez zbędnych dodatków."); title.setTextSize(25);
        title.setTextColor(Color.rgb(28, 41, 54)); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2); titleLp.topMargin = dp(12); page.addView(title, titleLp);
        TextView subtitle = new TextView(this); subtitle.setText("Graj z ludźmi na Lichess albo ćwicz offline z komputerem.");
        subtitle.setTextSize(15); subtitle.setTextColor(Color.rgb(91, 105, 119));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2); subLp.topMargin = dp(8); subLp.bottomMargin = dp(18); page.addView(subtitle, subLp);

        page.addView(section("KONTO LICHESS"));
        accountStatus = new TextView(this); accountStatus.setTextSize(14); accountStatus.setTextColor(Color.rgb(65, 80, 93));
        accountStatus.setText("Sprawdzanie stanu konta…"); page.addView(accountStatus);
        loginButton = actionButton("Połącz konto Lichess", true, new View.OnClickListener() {
            @Override public void onClick(View v) { startLogin(); }
        });
        LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(-1, dp(56)); loginLp.topMargin = dp(10); loginLp.bottomMargin = dp(20); page.addView(loginButton, loginLp);

        page.addView(section("GRA ONLINE"));
        page.addView(actionButton("Znajdź losowego przeciwnika", true, new View.OnClickListener() {
            @Override public void onClick(View v) { notReady("Kolejka gry online będzie kolejnym etapem po sprawdzeniu logowania i połączenia."); }
        }));
        page.addView(actionButton("Zaproś znajomego", false, new View.OnClickListener() {
            @Override public void onClick(View v) { notReady("Wyzwania znajomych będą podłączone po uruchomieniu modułu partii."); }
        }));
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(-1, -2); secLp.topMargin = dp(22); page.addView(section("GRA OFFLINE"), secLp);
        page.addView(actionButton("Graj z komputerem", false, new View.OnClickListener() {
            @Override public void onClick(View v) { notReady("Silnik Stockfish jest zaplanowany na kolejny etap."); }
        }));
        TextView footer = new TextView(this); footer.setText("WERSJA 0.2.1  •  ANDROID 4.1+  •  BEZ REKLAM");
        footer.setTextSize(10); footer.setTextColor(Color.rgb(125, 137, 148)); footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2); footLp.topMargin = dp(24); page.addView(footer, footLp);
        setContentView(page);
        refreshAccount();
    }

    private void refreshAccount() {
        String token = LichessOAuth.getToken(this);
        if (token == null) { accountStatus.setText("Konto nie jest jeszcze połączone."); loginButton.setText("Połącz konto Lichess"); }
        else { accountStatus.setText("Token logowania zapisany na tym urządzeniu."); loginButton.setText("Połącz ponownie konto"); }
    }

    private void startLogin() {
        loginButton.setEnabled(false); loginButton.setText("Otwieranie logowania…");
        LichessOAuth.begin(this, new LichessOAuth.Listener() {
            @Override public void onSuccess(String username) {
                loginButton.setEnabled(true); loginButton.setText("Połącz ponownie konto");
                accountStatus.setText("Połączono z kontem: " + username);
                Toast.makeText(MainActivity.this, "Logowanie do Lichess zakończone.", Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String message) {
                loginButton.setEnabled(true); loginButton.setText("Spróbuj ponownie");
                accountStatus.setText(message); Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private TextView section(String text) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(11); t.setTextColor(Color.rgb(90, 112, 127));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setPadding(0, dp(4), 0, dp(10)); return t;
    }
    private Button actionButton(String text, boolean primary, View.OnClickListener click) {
        Button b = new Button(this); b.setText(text); b.setTextSize(15); b.setAllCaps(false); b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setTextColor(primary ? Color.WHITE : Color.rgb(36, 52, 66));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(primary ? Color.rgb(54, 113, 83) : Color.WHITE);
        bg.setCornerRadius(dp(10)); bg.setStroke(dp(1), primary ? Color.rgb(54, 113, 83) : Color.rgb(218, 225, 230));
        b.setBackgroundDrawable(bg); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56)); lp.bottomMargin = dp(9);
        b.setOnClickListener(click); b.setGravity(Gravity.CENTER); b.setTransformationMethod(null); b.setLayoutParams(lp); return b;
    }
    private void notReady(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
