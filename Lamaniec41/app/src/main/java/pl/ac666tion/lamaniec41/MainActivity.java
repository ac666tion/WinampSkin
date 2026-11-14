package pl.ac666tion.lamaniec41;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

/** Lamaniec launcher with a built-in viewport compatibility extension. */
public final class MainActivity extends Activity {
    private static final String START_URL = "https://www.lamaniec.pl/";
    private static final String VIEWPORT_EXTENSION =
            "resource://android/assets/viewportfix/";

    private static GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        geckoView = new GeckoView(this);
        geckoView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);
        setContentView(geckoView);

        if (runtime == null) {
            runtime = GeckoRuntime.create(getApplicationContext());
        }

        // Install the page fix before navigating. If installation fails, still
        // open the site so the app remains usable.
        try {
            runtime.getWebExtensionController()
                    .installBuiltIn(VIEWPORT_EXTENSION)
                    .accept(new GeckoResult.Consumer<WebExtension>() {
                        @Override
                        public void accept(WebExtension extension) {
                            openLamaniec();
                        }
                    }, new GeckoResult.Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable error) {
                            openLamaniec();
                        }
                    });
        } catch (Throwable error) {
            openLamaniec();
        }
    }

    private void openLamaniec() {
        if (isFinishing() || session != null) {
            return;
        }

        GeckoSessionSettings settings = new GeckoSessionSettings();
        settings.setUserAgentMode(
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        settings.setViewportMode(
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        settings.setDisplayMode(
                GeckoSessionSettings.DISPLAY_MODE_STANDALONE);

        session = new GeckoSession(settings);
        session.setContentDelegate(new GeckoSession.ContentDelegate() { });
        session.open(runtime);
        geckoView.setSession(session);
        session.loadUri(START_URL);
    }

    @Override
    public void onBackPressed() {
        if (session != null && session.isOpen()) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
