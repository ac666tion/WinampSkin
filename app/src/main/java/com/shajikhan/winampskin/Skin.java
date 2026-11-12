package com.shajikhan.winampskin;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.HashMap;

public class Skin {
    String TAG = "Skin";
    public String skinDir, defaultSkinDir ;
    HashMap bitmaps ;
    View spinnerBox ;
    WinampSkin winampSkin ;
    public enum ResourceType {
        RESOURCE,
        FILE
    }

    Context context ;
    ResourceType resourceType ;

    Skin (Context _context, boolean def) {
        context = _context;
        defaultSkinDir = context.getFilesDir().toString() + "/skins/current/";
        if (! new File (defaultSkinDir).exists()) {
            Log.w(TAG, "Skin: Forcing default skin, as skin dir doesnt exist" );
            def = true ;
        }
        if (def) {
            bitmaps = new HashMap <String, Integer>();
            resourceType = ResourceType.RESOURCE;
            loadDefault();
        }
        else {
            bitmaps =  new HashMap <String, String> ();
            resourceType = ResourceType.FILE;
            loadfromDir(null);
        }
    }

    public void loadDefault () {
//        bitmaps.put("avs", R.drawable.avs);
        bitmaps.put("balance", R.drawable.balance);
        bitmaps.put("cbuttons", R.drawable.cbuttons);
        bitmaps.put("eq_bg", R.drawable.eq_bg);
        bitmaps.put("eq_ex", R.drawable.eq_ex);
        bitmaps.put("eqmain", R.drawable.eqmain);
//        bitmaps.put("gen", R.drawable.gen);
//        bitmaps.put("genex", R.drawable.genex);
        bitmaps.put("main", R.drawable.main);
//        bitmaps.put("mb", R.drawable.mb);
        bitmaps.put("monoster", R.drawable.monoster);
        bitmaps.put("numbers", R.drawable.numbers);
        bitmaps.put("playpaus", R.drawable.playpaus);
        bitmaps.put("pledit", R.drawable.pledit);
        bitmaps.put("posbar", R.drawable.posbar);
        bitmaps.put("seekbar", R.drawable.seekbar);
        bitmaps.put("seekbg", R.drawable.seekbg);
        bitmaps.put("shufrep", R.drawable.shufrep);
        bitmaps.put("text", R.drawable.text);
        bitmaps.put("thumb", R.drawable.thumb);
        bitmaps.put("titlebar", R.drawable.titlebar);
//        bitmaps.put("video", R.drawable.video);
        bitmaps.put("volume", R.drawable.volume);
        bitmaps.put("volume_thumb", R.drawable.volume_thumb);
    }

    public void loadfromDir (String dir) {
        if (dir == null)
             dir = defaultSkinDir ;

        bitmaps = new HashMap <String, Integer>();
        resourceType = ResourceType.RESOURCE;

        File f = new File(dir);
        File[] files = f.listFiles();
        if (files == null) {
            Log.e(TAG, "Cannot read requested skin: " + dir + "! Fallback to inbuilt default." );
            loadDefault();
            return;
        }

        resourceType = ResourceType.FILE;
        skinDir = dir ;
        bitmaps.put("avs", dir + "avs");
        bitmaps.put("balance", dir + "balance");
        bitmaps.put("cbuttons", dir + "cbuttons");
        bitmaps.put("eq_bg", dir + "eq_bg");
        bitmaps.put("eq_ex", dir + "eq_ex");
        bitmaps.put("eqmain", dir + "eqmain");
        bitmaps.put("gen", dir + "gen");
        bitmaps.put("genex", dir + "genex");
        bitmaps.put("main", dir + "main");
        bitmaps.put("mb", dir + "mb");
        bitmaps.put("monoster", dir + "monoster");
        bitmaps.put("numbers", dir + "numbers");
        bitmaps.put("playpaus", dir + "playpaus");
        bitmaps.put("pledit", dir + "pledit");
        bitmaps.put("posbar", dir + "posbar");
        bitmaps.put("seekbar", dir + "seekbar");
        bitmaps.put("seekbg", dir + "seekbg");
        bitmaps.put("shufrep", dir + "shufrep");
        bitmaps.put("text", dir + "text");
        bitmaps.put("thumb", dir + "thumb");
        bitmaps.put("titlebar", dir + "titlebar");
        bitmaps.put("video", dir + "video");
        bitmaps.put("volume", dir + "volume");
        bitmaps.put("volume_thumb", dir + "volume_thumb");
    }

    public void renameSkinFiles (String dir) {
        Log.e(TAG, "renameSkinFiles: Renaming files in " + dir);
        File f = new File(dir);
        File[] files = f.listFiles();
        if (files == null) {
            Log.e(TAG, "renameSkinFiles: unable to get directory listing!" );
            return ;
        }
        Log.e(TAG, "renameSkinFiles: Files found:"+ files.toString());
        for (File inFile : files) {
            if (! inFile.isDirectory()) {
                Log.d(TAG, "renameSkinFiles: Rename: " + inFile.toString() + " to " + inFile.toString().toLowerCase());
                inFile.renameTo(new File (inFile.toString().toLowerCase()));
            }
        }

    }

    private void unzipSkinArchive(File archive, File destination) throws IOException {
        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Cannot create skin directory");
        }
        String root = destination.getCanonicalPath() + File.separator;
        byte[] buffer = new byte[8192];
        ZipInputStream zip = new ZipInputStream(new java.io.FileInputStream(archive));
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File output = new File(destination, entry.getName());
                String canonical = output.getCanonicalPath();
                if (!canonical.startsWith(root)) {
                    throw new IOException("Unsafe path in skin archive");
                }
                if (entry.isDirectory()) {
                    if (!output.exists() && !output.mkdirs()) {
                        throw new IOException("Cannot create skin subdirectory");
                    }
                } else {
                    File parent = output.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Cannot create skin subdirectory");
                    }
                    FileOutputStream stream = new FileOutputStream(output);
                    try {
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            stream.write(buffer, 0, count);
                        }
                        stream.flush();
                    } finally {
                        stream.close();
                    }
                }
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }
    }

    void unzipCallback () {
        Log.d(TAG, "unzipCallback: Complete!");
    }

    public void downloadSkin (String url, WinampSkin _winampSkin, View view) {
        winampSkin = _winampSkin ;
        spinnerBox = view ;
        new DownloadSkin().execute(url);
    }

    public class DownloadSkin extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected Void doInBackground(String... urls) {
            InputStream input = null;
            File file = new File(context.getFilesDir(), "downloadedSkin.zip");
            if (file.exists()) file.delete();

            try {
                Log.d(TAG, "downloadSkin: Opening file");
                input = new URL(urls[0]).openStream();
            } catch (IOException e) {
                Log.e(TAG, "Unable to download skin: " + urls[0], e);
                return null;
            }

            OutputStream output = null;
            try {
                Log.d(TAG, "downloadSkin: Downloading skin: " + urls[0]);
                output = new FileOutputStream(file);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            } catch (IOException e) {
                Log.e(TAG, "Unable to save downloaded skin", e);
                if (file.exists()) file.delete();
            } finally {
                try {
                    if (output != null) output.close();
                } catch (IOException ignored) { }
                try {
                    if (input != null) input.close();
                } catch (IOException ignored) { }
            }

            return null;
        }

        void unzipComplete () {
            Log.d(TAG, "unzipComplete: Done!");
        }

        protected void onPostExecute(Void params) {
            File file = new File(context.getFilesDir(), "downloadedSkin.zip");
            try {
                if (!file.exists()) throw new IOException("Downloaded skin archive is missing");
                unzipSkinArchive(file, new File(defaultSkinDir));
                renameSkinFiles(defaultSkinDir);
                winampSkin.applySkin(spinnerBox);
            } catch (IOException e) {
                Log.e(TAG, "Unable to install skin archive", e);
                if (winampSkin != null) {
                    winampSkin.alert("Unable to install skin: " + e.getMessage());
                }
            }
        }

    }

}
