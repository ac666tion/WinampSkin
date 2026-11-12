package com.shajikhan.winampskin;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.exoplayer2.MediaItem;
import com.shajikhan.winampskin.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int OPEN_FILE = 1;
    private static final int OPEN_FOLDER = 2;

    public Context context;
    ActivityMainBinding binding;
    WinampSkin winampSkin;
    WinampMedia winampMedia;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        winampSkin = new WinampSkin(getApplicationContext(), this);
        winampSkin.setup();
        winampMedia = new WinampMedia(getApplicationContext(), this);

        Intent launchIntent = getIntent();
        if (launchIntent != null && Intent.ACTION_VIEW.equals(launchIntent.getAction())) {
            addSelectedUri(launchIntent.getData());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Keep the ExoPlayer instance alive while the device rotates. Refresh
        // only the skin views and their geometry; do not recreate the player.
        if (winampSkin != null) {
            winampSkin.refreshForConfiguration();
        }
        if (winampMedia != null) {
            winampMedia.setupEqualizer();
        }
    }

    @Override
    protected void onDestroy() {
        if (winampMedia != null) {
            winampMedia.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || winampMedia == null) return;

        if (requestCode == OPEN_FILE) {
            ClipData clipData = data.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri selected = clipData.getItemAt(i).getUri();
                    persistReadPermissionIfAvailable(data, selected);
                    addSelectedUri(selected);
                }
            } else {
                persistReadPermissionIfAvailable(data, data.getData());
                addSelectedUri(data.getData());
            }
        } else if (requestCode == OPEN_FOLDER && Build.VERSION.SDK_INT >= 21) {
            Uri treeUri = data.getData();
            persistReadPermissionIfAvailable(data, treeUri);
            if (treeUri != null) {
                DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
                if (folder != null) loadFolder(folder);
            }
        }
    }

    private void persistReadPermissionIfAvailable(Intent data, Uri uri) {
        if (Build.VERSION.SDK_INT < 19 || uri == null) return;
        int flags = data.getFlags();
        int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if ((takeFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
            try {
                getContentResolver().takePersistableUriPermission(
                        uri, takeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Log.w(TAG, "Provider did not grant persistable access", e);
            }
        }
    }

    private void addSelectedUri(Uri selectedUri) {
        if (selectedUri == null || winampSkin == null || winampMedia == null) return;

        Uri playableUri = selectedUri;
        if (Build.VERSION.SDK_INT < 19 && "content".equalsIgnoreCase(selectedUri.getScheme())) {
            playableUri = copyToPrivateStorage(selectedUri);
            if (playableUri == null) {
                winampSkin.alert("Unable to import the selected audio file.");
                return;
            }
        }

        String title = selectedUri.getLastPathSegment();
        if (title == null || title.length() == 0) title = "Audio track";
        winampSkin.playlistAdd(title, playableUri.toString());
        winampMedia.exoPlayer.addMediaItem(MediaItem.fromUri(playableUri));
    }

    /**
     * Android 4.1's ACTION_GET_CONTENT grant is temporary. Copy the selected
     * stream into app-private storage so playlist entries remain playable.
     */
    private Uri copyToPrivateStorage(Uri source) {
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = getContentResolver().openInputStream(source);
            if (input == null) return null;

            File directory = new File(getFilesDir(), "imported-audio");
            if (!directory.exists() && !directory.mkdirs()) return null;

            String name = source.getLastPathSegment();
            if (name == null) name = "track";
            name = name.replaceAll("[^A-Za-z0-9._-]", "_");
            File destination = new File(directory, System.currentTimeMillis() + "_" + name);

            output = new FileOutputStream(destination);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
            return Uri.fromFile(destination);
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Could not import audio file " + source, e);
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (IOException ignored) { }
            try { if (output != null) output.close(); } catch (IOException ignored) { }
        }
    }

    private void loadFolder(DocumentFile folder) {
        DocumentFile[] files;
        try {
            files = folder.listFiles();
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to list selected folder", e);
            return;
        }
        if (files == null) return;

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                loadFolder(file);
            } else {
                String mime = file.getType();
                if (mime == null || mime.startsWith("audio/")) {
                    addSelectedUri(file.getUri());
                }
            }
        }
    }
}
