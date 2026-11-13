package com.shajikhan.winampskin;

import static android.app.Activity.RESULT_OK;
import static android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC;
import static android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION;
import static android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;
import java.util.ArrayList;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;


import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WinampMedia {
    private int notificationId = 69;
    private PlayerNotificationManager playerNotificationManager;

    private PlayerNotificationManager.MediaDescriptionAdapter mediaDescriptionAdapter = new PlayerNotificationManager.MediaDescriptionAdapter() {
        @Override
        public String getCurrentSubText(Player player) {
            if (exoPlayer.isPlaying())
                return mainActivity.getResources().getString(R.string.app_name) + " playing" ;
            else
                return mainActivity.getResources().getString(R.string.app_name) + " stopped" ;
        }

        @Override
        public String getCurrentContentTitle(Player player) {
            return (String) winampSkin.trackTitle.getText();
        }

        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
//            Intent intent = new Intent(mainActivity, MainActivity.class);
            return PendingIntent.getActivity(mainActivity.context, 0,
                    mainActivity.getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

        }

        @Override
        public String getCurrentContentText(Player player) {
            return (String) winampSkin.bigClock.getText();
        }

        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            return BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.winamp_logo);
        }
    };

    String TAG = "WinampMedia";
    Context context ;
    MainActivity mainActivity ;
    WinampSkin winampSkin ;
    SimpleExoPlayer exoPlayer = null;
    PopupMenu playlistMenuAdd, playlistMenuRemove,
            playlistMenuSelect,  getPlaylistMenuLoad,
            playlistMenuMisc, getPlaylistMenuFab;
    Handler handler ;
    WinampEqualizer winampEqualizer ;
    int OPEN_FILE = 1,
        OPEN_FOLDER = 2 ;


    WinampMedia (Context _context, MainActivity _mainActivity) {
        context = _context ;
        mainActivity = _mainActivity ;
        winampSkin = mainActivity.winampSkin;
        setupPlayer();
        setupPlaylist();
        setupPlaylistMenu();
        setupEqualizer();
//        AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
//        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(mainActivity.context, "Xenamp", R.string.app_name, notificationId, mediaDescriptionAdapter, new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            }

            @Override
            public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            }

        });

//        playerNotificationManager.setUsePreviousActionInCompactView(true);
//        playerNotificationManager.setUseNextActionInCompactView(true);
        playerNotificationManager.setUseChronometer(true);
        playerNotificationManager.setPlayer(exoPlayer);

    }

    void setupPlayer () {
        if (exoPlayer == null) {
            exoPlayer = new SimpleExoPlayer.Builder(context).build();

            // Let ExoPlayer request, lose and regain audio focus using the
            // platform's media-playback rules. This is supported on API 16+.
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build();
            exoPlayer.setAudioAttributes(audioAttributes, true);

            winampEqualizer = new WinampEqualizer(exoPlayer.getAudioComponent().getAudioSessionId());
        }

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                int sourceIndex = exoPlayer.getCurrentWindowIndex();
//                TrackSelection trackSelection = trackSelections.get(sourceIndex);
                MediaItem mediaItem = exoPlayer.getCurrentMediaItem() ;
                if (mediaItem == null) return ;
                String uri = String.valueOf(exoPlayer.getCurrentMediaItem().mediaMetadata.title);
                Log.d(TAG, "onTracksChanged: " + uri);

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                winampSkin.alert("Unable to start playback", error.getMessage());
                switch (error.type) {
                    case ExoPlaybackException.TYPE_SOURCE:
                        Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_RENDERER:
                        Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_UNEXPECTED:
                        Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                        break;
                }
            }

        });

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                int sourceIndex = exoPlayer.getCurrentWindowIndex();
                MediaItem mediaItem =  exoPlayer.getCurrentMediaItem() ;
                if (mediaItem == null) return ;

                Log.d(TAG, "onPositionDiscontinuity: " + mediaItem.playbackProperties.uri.getLastPathSegment());
                winampSkin.trackTitle.setText(mediaItem.playbackProperties.uri.getLastPathSegment());


            }
        });

        if (handler == null) handler = new Handler();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                updateProgressBar();
            }
        });

        winampSkin.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (! fromUser) return;
                exoPlayer.seekTo((long) ((float)exoPlayer.getDuration() * (float) (progress / 100.0)));
                Log.d (TAG, String.valueOf(exoPlayer.getDuration() + ' ' + (progress / 100)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        winampSkin.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                    exoPlayer.prepare();
                }
                exoPlayer.play();
            }
        });

        winampSkin.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoPlayer.pause();
            }
        });

        winampSkin.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoPlayer.stop();
            }
        });

        winampSkin.prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoPlayer.previous();
            }
        });

        winampSkin.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoPlayer.next();
            }
        });

        winampSkin.eq_pl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent eqIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                eqIntent.putExtra(EXTRA_CONTENT_TYPE, CONTENT_TYPE_MUSIC);
                eqIntent.putExtra(EXTRA_AUDIO_SESSION, CONTENT_TYPE_MUSIC);

                mainActivity.startActivityForResult(eqIntent, 0);

            }
        });

        winampSkin.about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                winampSkin.SkinBrowserDialog();
//                Intent eqIntent = new Intent(mainActivity, SkinBrowser.class);
//                mainActivity.startActivityForResult(eqIntent, 0);
            }
        });

        winampSkin.volume.setProgress((int) exoPlayer.getVolume() * 1000);
        winampSkin.volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                exoPlayer.setVolume((float)progress / 1000.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        winampSkin.shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoPlayer.setShuffleModeEnabled(!exoPlayer.getShuffleModeEnabled());
                winampSkin.paintShuffle(exoPlayer.getShuffleModeEnabled());
            }
        });

        winampSkin.repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatMode = exoPlayer.getRepeatMode() ;
                if (repeatMode == 0) {
                    winampSkin.paintRepeat(true);
                    repeatMode = Player.REPEAT_MODE_ALL ;
                } else {
                    repeatMode = 0;
                    winampSkin.paintRepeat(false);
                }

                exoPlayer.setRepeatMode(repeatMode);
            }
        });
    }

    void setupPlaylist () {

        // if there is no saved playilst, graciously give the user somethign to listen to
        JSONObject savedPlaylist = winampSkin.loadPlaylist(null);
        if (savedPlaylist != null) {
            JSONArray keys = savedPlaylist.names();
            for (int i = 0 ; i < keys.length() ; i ++) {
                try {
                    winampSkin.playlistAdd(keys.getString(i), savedPlaylist.getString(keys.getString(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Restore the saved playlist into ExoPlayer as well as the visible
        // list, so the physical PLAY button works immediately after launch.
        exoPlayer.clearMediaItems();
        for (String track : winampSkin.playlistElements) {
            Object savedUri = winampSkin.playlistUri.get(track);
            if (savedUri != null) {
                exoPlayer.addMediaItem(MediaItem.fromUri(String.valueOf(savedUri)));
            }
        }
        if (exoPlayer.getMediaItemCount() > 0) exoPlayer.prepare();

        Log.d(TAG, "setupPlaylist: " +
                winampSkin.playlistElements.toString() + "\n"+
                winampSkin.playlistUri.toString() + "\n"
                );

        winampSkin.playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                winampSkin.playlistSelectNone();
                if (exoPlayer.isPlaying())
                    exoPlayer.stop();

                Log.d(TAG, String.format("onItemClick: Playing %s", winampSkin.playlistUri.get(parent.getAdapter().getItem(position).toString()).toString()));
                winampSkin.seek.setVisibility(View.VISIBLE);
                winampSkin.bigClock.setVisibility(View.VISIBLE);
                winampSkin.trackTitle.setVisibility(View.VISIBLE);
                winampSkin.trackTitle.setText(winampSkin.playlistElements.get(position));
                // Build the media item.
//                MediaItem mediaItem = MediaItem.fromUri(winampSkin.playlistUri.get(parent.getAdapter().getItem(position).toString()).toString());
//                exoPlayer.addMediaItem(mediaItem);
                // Set the media item to be played.

                // Prepare the player.
                // Start the playback.
                try {
                    exoPlayer.clearMediaItems();

                    for (int i = position ; i < winampSkin.playlistElements.size() ; i ++) {
                        exoPlayer.addMediaItem(MediaItem.fromUri(winampSkin.playlistUri.get(parent.getAdapter().getItem(i).toString()).toString()));
                    }

                    for (int i = 0 ; i < position ; i ++) {
                        exoPlayer.addMediaItem(MediaItem.fromUri(winampSkin.playlistUri.get(parent.getAdapter().getItem(i).toString()).toString()));
                    }

                    exoPlayer.prepare();
                    exoPlayer.play();
                } catch (Exception e) {
                    Log.e(TAG, "onItemClick: Playback Error", e);
//                    e.printStackTrace();
                    winampSkin.alert(e.getMessage());
                }
//                for (int i = 0 ; i < winampSkin.playlistView.getChildCount() ; i ++) {
//                    if (i != position)
//                        wOverrideinampSkin.playlistView.setItemChecked(i, false);
//                }

            }
        });
    }

    public void destroy () {
        if (handler != null) handler.removeCallbacks(updateProgressAction);
        if (playerNotificationManager != null) playerNotificationManager.setPlayer(null);
        if (exoPlayer != null) exoPlayer.release();
        if (winampEqualizer != null) winampEqualizer.destroy();
    }

    private void updateProgressBar() {
        long duration = exoPlayer == null ? 0 : exoPlayer.getDuration();
        long position = exoPlayer == null ? 0 : exoPlayer.getCurrentPosition();
        float p =  ((float)position /(float) duration) * 100.0f ;
        long time = position / 1000 ;
        int min = (int) (time / 60);
        int sec = (int) (time - (60*min));
        String sec_ = String.valueOf(sec);
        if (sec < 10) {
            sec_ = '0' + sec_ ;
        }
        String text = min +"  " + sec_ ;
        winampSkin.bigClock.setText(text);
        Log.d(TAG, "updateProgressBar: " + time + " " + time/60);
        winampSkin.seek.setProgress((int)(p));
        long bufferedPosition = exoPlayer == null ? 0 : exoPlayer.getBufferedPosition();
        Log.d(TAG, "updateProgressBar: " + p);
        winampSkin.seek.setSecondaryProgress((int) bufferedPosition);
        // Remove scheduled updates.
        handler.removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        int playbackState = exoPlayer == null ? Player.STATE_IDLE : exoPlayer.getPlaybackState();
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            long delayMs;
            if (exoPlayer.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            handler.postDelayed(updateProgressAction, delayMs);
        } else {
            winampSkin.bigClock.setVisibility(View.INVISIBLE);
            winampSkin.seek.setVisibility(View.INVISIBLE);
            winampSkin.trackTitle.setVisibility(View.INVISIBLE);
        }
    }

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgressBar();
        }
    };

    void play (String track, String uri) {
        winampSkin.seek.setVisibility(View.VISIBLE);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

    }

    public void setupEqualizer () {
        int[] bars = {
                R.id.equalizer_60, R.id.equalizer_170, R.id.equalizer_310,
                R.id.equalizer_600, R.id.equalizer_1000, R.id.equalizer_3000,
                R.id.equalizer_6000, R.id.equalizer_12000, R.id.equalizer_14000,
                R.id.equalizer_16000
        };

        for (int i = 0; i < bars.length; i++) {
            SeekBar seekBar = mainActivity.findViewById(bars[i]);
            if (seekBar == null) continue;
            final int bandIndex = i;
            seekBar.setMax(100);
            seekBar.setProgress(winampEqualizer == null ? 50 :
                    winampEqualizer.getSliderProgress(i));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    if (fromUser && winampEqualizer != null) {
                        winampEqualizer.setSliderProgress(bandIndex, progress);
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar bar) { }
                @Override public void onStopTrackingTouch(SeekBar bar) { }
            });
        }
    }

    private void openAudioPicker() {
        // Android 4.1–4.3 file pickers usually return only one URI even when
        // EXTRA_ALLOW_MULTIPLE is supplied. Use the media library's native
        // multi-choice list on those releases instead.
        if (Build.VERSION.SDK_INT <= 18) {
            showLegacyMultiSelect();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 18) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        try {
            if (intent.resolveActivity(mainActivity.getPackageManager()) == null
                    && Build.VERSION.SDK_INT >= 19) {
                // Some older Android builds have no document picker provider.
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (Build.VERSION.SDK_INT >= 18) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
            }
            if (intent.resolveActivity(mainActivity.getPackageManager()) == null) {
                winampSkin.alert("No audio file browser is installed. Install or enable a file manager first.");
                return;
            }
            mainActivity.startActivityForResult(
                    Intent.createChooser(intent, "Choose audio files"), OPEN_FILE);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.e(TAG, "Could not launch the audio picker", e);
            winampSkin.alert("Could not open the audio file browser: " + e.getMessage());
        }
    }

    private void showLegacyMultiSelect() {
        final ArrayList<Uri> trackUris = new ArrayList<>();
        final ArrayList<String> trackLabels = new ArrayList<>();
        Cursor cursor = null;
        try {
            String[] columns = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST
            };
            cursor = mainActivity.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    columns,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0",
                    null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC");
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    if (title == null || title.trim().length() == 0) title = "Audio track " + id;
                    if (artist == null || artist.trim().length() == 0
                            || "<unknown>".equalsIgnoreCase(artist)) {
                        trackLabels.add(title);
                    } else {
                        trackLabels.add(title + " — " + artist);
                    }
                    trackUris.add(Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to read the device music library", e);
            winampSkin.alert("Unable to read music files: " + e.getMessage());
            return;
        } finally {
            if (cursor != null) cursor.close();
        }

        if (trackUris.isEmpty()) {
            new AlertDialog.Builder(mainActivity)
                    .setTitle("Add music")
                    .setMessage("No indexed music files were found. Copy music to the tablet or use a file manager to open an audio file.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        final boolean[] checked = new boolean[trackUris.size()];
        final ArrayList<Integer> selected = new ArrayList<>();
        new AlertDialog.Builder(mainActivity)
                .setTitle("Select tracks (" + trackUris.size() + ")")
                .setMultiChoiceItems(trackLabels.toArray(new String[0]), checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) selected.add(which);
                                else selected.remove(Integer.valueOf(which));
                            }
                        })
                .setPositiveButton("Add selected", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selected.isEmpty()) {
                            winampSkin.alert("Select at least one track.");
                            return;
                        }
                        for (Integer index : selected) {
                            mainActivity.addSelectedUri(trackUris.get(index), trackLabels.get(index));
                        }
                        Log.i(TAG, "Added " + selected.size() + " tracks to playlist");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFolderPicker() {
        if (Build.VERSION.SDK_INT >= 21) {
            mainActivity.startActivityForResult(
                    new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), OPEN_FOLDER);
        } else {
            winampSkin.alert("Folder selection requires Android 5.0 or newer. Use Open File instead.");
        }
    }

    void setupPlaylistMenu () {
        Button  b = mainActivity.findViewById(R.id.playlist_add),
                r = mainActivity.findViewById(R.id.playlist_remove),
                s = mainActivity.findViewById(R.id.playlist_select),
                m = mainActivity.findViewById(R.id.playlist_misc),
                l = mainActivity.findViewById(R.id.playlist_load) ;
        FloatingActionButton fab = mainActivity.findViewById(R.id.fab);

        playlistMenuAdd = new PopupMenu(mainActivity.context, b);
        MenuInflater inflater = playlistMenuAdd.getMenuInflater();
        inflater.inflate(R.menu.playlist_add, playlistMenuAdd.getMenu());
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistMenuAdd.show();
            }
        });

        playlistMenuRemove = new PopupMenu(context, r);
        playlistMenuRemove.inflate(R.menu.playlist_remove);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistMenuRemove.show();
            }
        });

        playlistMenuSelect = new PopupMenu(context, s);
        playlistMenuSelect.inflate(R.menu.playlist_select);
        s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistMenuSelect.show();
            }
        });

        playlistMenuSelect = new PopupMenu(context, s);
        playlistMenuSelect.inflate(R.menu.playlist_select);
        s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistMenuSelect.show();
            }
        });

        // The visible LOAD button opens the audio file browser directly.
        // The old popup contained placeholder actions without handlers, making
        // the button appear to do nothing.
        l.setContentDescription("Load audio files");
        l.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAudioPicker();
            }
        });

        playlistMenuMisc = new PopupMenu(context, m);
        playlistMenuMisc.inflate(R.menu.playlist_misc);
        m.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistMenuMisc.show();
            }
        });

        getPlaylistMenuFab = new PopupMenu(context, fab);
        getPlaylistMenuFab.inflate(R.menu.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlaylistMenuFab.show();
            }
        });

        getPlaylistMenuFab.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent ;
                String url ;
                switch (item.getItemId()) {
                    default:
                        break ;
                    case R.id.menu_about:
                        url = "https://github.com/ac666tion/WinampSkin";
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        mainActivity.startActivity(intent);
                        break;
                    case R.id.menu_report_bug:
                        url = "https://github.com/ac666tion/WinampSkin/issues/new/choose";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        mainActivity.startActivity(i);
                        break ;
                    case R.id.menu_idea:
                        url = "https://github.com/ac666tion/WinampSkin/discussions/categories/ideas";
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        mainActivity.startActivity(intent);
                        break ;
                    case R.id.menu_forum:
                        url = "https://github.com/ac666tion/WinampSkin/discussions";
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        mainActivity.startActivity(intent);
                        break ;
                    case R.id.menu_skin:
                        winampSkin.SkinBrowserDialog();
                        break ;
                    case R.id.menu_skin_default:
                        winampSkin.setDefaultSkin();
                        break ;
                    case R.id.menu_open_folder:
                        openFolderPicker();
                        break;
                    case R.id.menu_open_file:
                        openAudioPicker();
                        break;
                }
                return false;
            }
        });

        // intents for all the menu items
        playlistMenuRemove.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                SparseBooleanArray checked = winampSkin.playlistView.getCheckedItemPositions();
                switch (item.getItemId()) {
                    default:
                        break;
                    case R.id.playlist_remove_all:
                        winampSkin.playlistClear();
                        exoPlayer.clearMediaItems();
                        break ;
                    case R.id.playlist_remove_selected:
//                        for (int i = 0 ; i < winampSkin.playlistView.getChildCount() ; i ++) {
//                            if (winampSkin.playlistView.isItemChecked(i))
//                                winampSkin.playlistRemove(i);
//                        }

                        for (int i = 0; i < winampSkin.playlistView.getAdapter().getCount(); i++) {
                            if (checked.get(i)) {
                                winampSkin.playlistRemove(i);
                                // Do something
                            }
                        }

                        break ;
                    case R.id.playlist_remove_unselected:
                        for (int i = 0; i < winampSkin.playlistView.getAdapter().getCount(); i++) {
                            if (!checked.get(i)) {
                                winampSkin.playlistRemove(i);
                                // Do something
                            }
                        }

                        break ;
                }
                    return true;
            }
        });

        playlistMenuAdd.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent ;
                switch (item.getItemId()) {
                    default:
                        break ;
                    case R.id.add_local_folder:
                        openFolderPicker();
                        break;

                    case R.id.add_local_file:
                        openAudioPicker();
                        break;

                    case R.id.add_url:
                        addUrl();
                        break;
                }
                return false;
            }
        });

        playlistMenuMisc.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    default:
                        break ;
                    case R.id.misc_randomize:
                        winampSkin.playlistShuffle();
                        break ;
                    case R.id.misc_reverse:
                        winampSkin.playlistReverse();
                        break ;
                }
                return false;
            }
        });

        playlistMenuSelect.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    default:
                        break ;
                    case R.id.playlist_select_all:
                        for (int i = 0 ; i < winampSkin.playlistView.getChildCount() ; i ++) {
                            winampSkin.playlistView.setItemChecked(i, true);
                        }
                        break ;
                    case R.id.playlist_select_none:
                        for (int i = 0 ; i < winampSkin.playlistView.getChildCount() ; i ++) {
                            winampSkin.playlistView.setItemChecked(i, false);
                        }
                        break ;
                    case R.id.playlist_select_invert:
                        for (int i = 0 ; i < winampSkin.playlistView.getChildCount() ; i ++) {
                            winampSkin.playlistView.setItemChecked(i, ! winampSkin.playlistView.isItemChecked(i));
                        }
                        break ;
                }

                return false;
            }
        });
    }

    public void addUrl () {
        AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
        final EditText edittext = new EditText(mainActivity);
        alert.setMessage("Add URL");
        alert.setTitle(R.string.app_name);

        alert.setView(edittext);
        edittext.setPadding(10,10,10,10);

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //What ever you want to do with the value
//                Editable YouEditTextValue = edittext.getText();
                //OR
                String YouEditTextValue = edittext.getText().toString();
                winampSkin.playlistAdd(Uri.parse(YouEditTextValue).getLastPathSegment(), YouEditTextValue.toString());
                exoPlayer.addMediaItem(MediaItem.fromUri(YouEditTextValue));

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();

    }
}
