package pl.ac666tion.lamaniec41;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

/** A minimal, single-purpose Lamaniec launcher powered by an embedded Gecko engine. */
public final class MainActivity extends Activity {
    private static final String START_URL = "https://www.lamaniec.pl/";

    // GeckoRuntime is process-wide and must only be created once.
    private static GeckoRuntime runtime;
    private GeckoSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        GeckoView geckoView = new GeckoView(this);
        geckoView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);
        setContentView(geckoView);

        if (runtime == null) {
            runtime = GeckoRuntime.create(getApplicationContext());
        }

        session = new GeckoSession();
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
