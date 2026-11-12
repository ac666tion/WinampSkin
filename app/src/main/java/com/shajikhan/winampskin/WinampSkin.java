package com.shajikhan.winampskin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.shajikhan.winampskin.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class WinampSkin {
    Skin skin;
    Button  prev, next, play, pause, stop, eject, shuffle, repeat, about,
            eq_pl, on, auto, preset;
    SeekBar seek, volume, balance ;
    LinearLayout mainWindow ;
    MainActivity mainActivity ;
    WinampSkin self ;
    boolean shuffleState, repeatState ;
    Context context ;
    DisplayMetrics displayMetrics ;
    String TAG = "WinampSkin";
    // FIXME: 7/24/21 Determine the following automagically
    float UPSCALE_FACTOR = 1.43f ;
    float density ;
    public ArrayAdapter playlistAdapter ;

    List <String> playlistElements ;
    HashMap playlistUri ;
    TextView bigClock, trackTitle ;

    String playlistTextColor = "#ffffff";

    Canvas mainCanvas ;
    Drawable mainDrawable ;
    Paint paint ;
    ListView playlistView ;

    Button btnHit;
    TextView txtJson;

    SharedPreferences sharedPreferences ;

    public void SkinBrowserDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        WebView webView = new WebView(mainActivity) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true;
            }

        } ;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://skins.webamp.org");
        webView.setMinimumHeight(1000);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.weight = 2.0f ;
        linearLayout.setLayoutParams(layoutParams);
        webView.setLayoutParams(layoutParams);
        linearLayout.addView(webView);
        linearLayout.setMinimumHeight(1000);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        LinearLayout box = new LinearLayout(context);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        TextView textView = new TextView(context);
        textView.setText("Loading ...");
        box.addView(progressBar);
        Button button = new Button(context);
        button.setText("Download Skin");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = webView.getUrl();
                Log.d(TAG, "onClick: " + url);
                try {
                    String skinUrl = "https://cdn.webampskins.org/skins/" + url.toString().split("https://skins.webamp.org/skin/")[1].split("/")[0] + ".wsz";
                    Log.d(TAG, "shouldOverrideUrlLoading: Selected skin " + skinUrl);
                    box.setVisibility(View.VISIBLE);
                    textView.setText("Downloading skin ...");
                    skin.downloadSkin(skinUrl, self, box);
                } catch (ArrayIndexOutOfBoundsException e) {
                    alert("Select a skin to download.");
                    return;
                }
            }
        });

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams1.setMargins(10,10,10,10);
        progressBar.setLayoutParams(layoutParams1);
        textView.setLayoutParams(layoutParams1);
        box.setLayoutParams(layoutParams1);
        box.addView(textView);
        linearLayout.addView(box);
        linearLayout.addView(button);

        box.setVisibility(View.INVISIBLE);

        linearLayout.setMinimumWidth(context.getResources().getDisplayMetrics().widthPixels);
        linearLayout.setMinimumHeight(context.getResources().getDisplayMetrics().heightPixels);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.d(TAG, "onLoadResource: " + url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted: " + url);
            }



            private boolean shouldOverrideUrlLoading(final WebView view, final Uri request) {
                if(request.getScheme().equals("blob")) {
                    // do your special handling for blob urls here
                    String skinUrl = "https://cdn.webampskins.org/skins/" + request.toString().split ("blob:https://skins.webamp.org")[1] + ".wsz";
                    Log.d(TAG, "shouldOverrideUrlLoading: Selected skin " + skinUrl);
                    box.setVisibility(View.VISIBLE);
                    textView.setText("Downloading skin ...");
                    skin.downloadSkin (skinUrl, self, box);
                }
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                String skinUrl = url.split ("\\?skinUrl=")[1];
                Log.d(TAG, "shouldOverrideUrlLoading: Selected skin " + skinUrl);
                box.setVisibility(View.VISIBLE);
                textView.setText("Downloading skin ...");
                skin.downloadSkin (skinUrl, self, box);
//                skin.renameSkinFiles(skin.defaultSkinDir);
//                textView.setText("Applying skin ...");
//                applySkin(box);
//                textView.setText("Loading ...");
//                box.setVisibility(View.INVISIBLE);
                return true;
            }
        });

        webView.addJavascriptInterface(new Object() {
            public void performClick()
            {
                // Deal with a click on the OK button
                String url = webView.getUrl();
                String skinUrl = url.split ("\\?skinUrl=")[1];
                Log.d(TAG, "shouldOverrideUrlLoading: Selected skin " + skinUrl);
                box.setVisibility(View.VISIBLE);
                textView.setText("Downloading skin ...");
                skin.downloadSkin (skinUrl, self, box);

            }

        }, "ok");

        AlertDialog alertDialog = builder
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }

                })
                .setView(linearLayout)
                .create();

        alertDialog.setTitle("Select Skin");
//        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.getWindow().setDimAmount(0f);
        alertDialog.show();

    }

    void applySkin (View view) {
        Log.d(TAG, "applySkin: starting");
        skin = new Skin(context,false);
        Log.d(TAG, "applySkin: mainwindow");
        setupMainWindow(skin, false, false);
        Log.d(TAG, "applySkin: playlist");
        setupPlaylist();
        Log.d(TAG, "applySkin: equalizer");
        setupEqualizer();

        Log.d(TAG, "applySkin: complete");
        if (view != null)
            view.setVisibility(View.INVISIBLE);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context, "Skin Applied", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void setDefaultSkin () {
        deleteRecursive(new File(skin.defaultSkinDir));
        skin = new Skin(context, true) ;
        applySkin(null);
    }

    public class BitmapAsync extends AsyncTask<String, Void, Bitmap>{

        @Override
        protected Bitmap doInBackground(String... params) {

            try {
                URL url = new URL(params [0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
    }

    public void playlistSelectNone () {
        for (int i = 0 ; i < playlistView.getChildCount() ; i ++) {
            playlistView.setItemChecked(i, false);
        }
    }

    public void playlistSelectAll () {
        for (int i = 0 ; i < playlistView.getChildCount() ; i ++) {
            playlistView.setItemChecked(i, true);
        }
    }
}
