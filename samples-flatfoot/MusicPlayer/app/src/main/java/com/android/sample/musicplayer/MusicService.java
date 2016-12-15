/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.sample.musicplayer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import com.android.sample.musicplayer.MusicRepository.TrackMetadata;

import java.io.IOException;
import java.util.List;

/**
 * Music playback service.
 */
public class MusicService extends Service implements OnCompletionListener, OnPreparedListener {
    public static final String ACTION_INITIALIZE =
            "com.android.sample.musicplayer.action.INITIALIZE";
    public static final String ACTION_PLAY = "com.android.sample.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.android.sample.musicplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.android.sample.musicplayer.action.STOP";
    public static final String ACTION_NEXT = "com.android.sample.musicplayer.action.NEXT";
    public static final String ACTION_PREV = "com.android.sample.musicplayer.action.PREV";

    public static final String BROADCAST_ACTION = "com.android.sample.musicplayer.status.REPORT";

    private static final String RESOURCE_PREFIX =
            "android.resource://com.android.sample.musicplayer/";

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    private static final int NOTIFICATION_ID = 1;


    private MediaSessionCompat mMediaSession;

    private MediaPlayer mMediaPlayer = null;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;

    private MusicRepository mMusicRepository;
    private List<TrackMetadata> mTracks;

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaSession = new MediaSessionCompat(this, MusicService.class.getSimpleName());
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);

        mMusicRepository = MusicRepository.getInstance();
        mTracks = mMusicRepository.getTracks();
        updateAudioMetadata();

        // Attach Callback to receive MediaSession updates
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                processPlayRequest();
            }

            @Override
            public void onPause() {
                super.onPause();
                processPauseRequest();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                processNextRequest();
                updateAudioMetadata();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                processPreviousRequest();
                updateAudioMetadata();
                updateNotification();
            }

            @Override
            public void onStop() {
                super.onStop();
                mNotificationManager.cancel(NOTIFICATION_ID);
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });

        mNotificationManager = NotificationManagerCompat.from(this);
    }

    void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_INITIALIZE)) {
            processInitializeRequest();
        } else if (intent.getAction().equals(ACTION_PLAY)) {
            processPlayRequest();
        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            processPauseRequest();
        } else if (intent.getAction().equals(ACTION_STOP)) {
            processStopRequest();
        } else if (intent.getAction().equals(ACTION_NEXT)) {
            processNextRequest();
        } else if (intent.getAction().equals(ACTION_PREV)) {
            processPreviousRequest();
        }

        return START_NOT_STICKY;
    }

    private void processInitializeRequest() {
        if (mMusicRepository.getCurrentlyActiveTrack() >= 0) {
            sendBroadcast();
            return;
        }
        mMusicRepository.setCurrentlyActiveTrack(-1);
        setUpAsForeground("Ready to play");
    }

    private void processPlayRequest() {
        // actually play the song
        int currState = mMusicRepository.getState();
        if (currState == MusicRepository.STATE_STOPPED) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong();
        } else if (currState == MusicRepository.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mMusicRepository.setState(MusicRepository.STATE_PLAYING);
            updateNotification();
            configAndStartMediaPlayer();
        }
    }

    private void processPauseRequest() {
        if (mMusicRepository.getState() == MusicRepository.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mMusicRepository.setState(MusicRepository.STATE_PAUSED);
            mMediaPlayer.pause();
            updateNotification();
            //relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus

            sendBroadcast();
        }
    }

    private void processStopRequest() {
        processStopRequest(false);
    }

    private void processStopRequest(boolean force) {
        int currState = mMusicRepository.getState();
        if (currState == MusicRepository.STATE_PLAYING || currState == MusicRepository.STATE_PAUSED
                || force) {
            mMusicRepository.setState(MusicRepository.STATE_STOPPED);
            // let go of all resources...
            relaxResources(true);
            // service is no longer necessary. Will be started again if needed.
            stopSelf();

            sendBroadcast();
        }
    }

    private void processNextRequest() {
        int currState = mMusicRepository.getState();
        if (currState == MusicRepository.STATE_PLAYING
                || currState == MusicRepository.STATE_PAUSED) {
            playNextSong();
        }
    }

    private void processPreviousRequest() {
        int currState = mMusicRepository.getState();
        if (currState == MusicRepository.STATE_PLAYING
                || currState == MusicRepository.STATE_PAUSED) {
            playPrevSong();
        }
    }

    private void sendBroadcast() {
        Intent localIntent = new Intent(BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        //stopForeground(true);
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    private void configAndStartMediaPlayer() {
        mMediaPlayer.setVolume(1.0f, 1.0f); // we can be loud
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        sendBroadcast();
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    private void playNextSong() {
        mMusicRepository.setState(MusicRepository.STATE_STOPPED);
        relaxResources(false); // release everything except MediaPlayer
        try {
            // set the source of the media player to a manual URL or path
            createMediaPlayerIfNeeded();

            int nextSourceIndex = mMusicRepository.getCurrentlyActiveTrack() + 1;
            if (nextSourceIndex == mTracks.size()) {
                nextSourceIndex = 0;
            }
            mMusicRepository.setCurrentlyActiveTrack(nextSourceIndex);
            mMediaPlayer.setDataSource(getBaseContext(),
                    Uri.parse(RESOURCE_PREFIX + mTracks.get(nextSourceIndex).getTrackRes()));
            mMediaPlayer.prepare();
        } catch (IOException ioe) {
        }
    }

    /**
     * Starts playing the previous song. If manualUrl is null, the next song will be randomly
     * selected from our Media Retriever (that is, it will be a random song in the user's device).
     * If manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    private void playPrevSong() {
        mMusicRepository.setState(MusicRepository.STATE_STOPPED);
        relaxResources(false); // release everything except MediaPlayer
        try {
            // set the source of the media player to a manual URL or path
            createMediaPlayerIfNeeded();

            int prevSourceIndex = mMusicRepository.getCurrentlyActiveTrack() - 1;
            if (prevSourceIndex == -1) {
                prevSourceIndex = mTracks.size() - 1;
            }
            mMusicRepository.setCurrentlyActiveTrack(prevSourceIndex);
            mMediaPlayer.setDataSource(getBaseContext(),
                    Uri.parse(RESOURCE_PREFIX + mTracks.get(prevSourceIndex).getTrackRes()));
            mMediaPlayer.prepare();
        } catch (IOException ioe) {
        }
    }

    /**
     * Called when media player is done playing current song.
     */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong();
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mMusicRepository.setState(MusicRepository.STATE_PLAYING);
        updateNotification();
        configAndStartMediaPlayer();
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    private void populateNotificationBuilderContent(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
                (int) (System.currentTimeMillis() & 0xfffffff),
                new Intent().setClass(getApplicationContext(), MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT);

        boolean isPlaying = mMusicRepository.getState() == MusicRepository.STATE_PLAYING;

        // Build the notification object.
        mNotificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        mNotificationBuilder.setSmallIcon(R.drawable.ic_play_arrow_white_24dp);
        mNotificationBuilder.setTicker(text);
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setContentTitle("RandomMusicPlayer");
        mNotificationBuilder.setContentText(text);
        mNotificationBuilder.setContentIntent(pi);
        mNotificationBuilder.setOngoing(isPlaying);

        int primaryActionDrawable = isPlaying ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
        PendingIntent primaryActionIntent = isPlaying
                ? PendingIntent.getService(this, 12,
                        new Intent(this, MusicService.class).setAction(ACTION_PAUSE), 0)
                : PendingIntent.getService(this, 13,
                        new Intent(this, MusicService.class).setAction(ACTION_PLAY), 0);
        String primaryActionName = isPlaying ? "pause" : "play";

        mNotificationBuilder.addAction(android.R.drawable.ic_media_previous, "previous",
                PendingIntent.getService(this, 10,
                        new Intent(this, MusicService.class).setAction(ACTION_PREV), 0));
        mNotificationBuilder.addAction(primaryActionDrawable, primaryActionName,
                primaryActionIntent);
        mNotificationBuilder.addAction(android.R.drawable.ic_media_next, "next",
                PendingIntent.getService(this, 11,
                        new Intent(this, MusicService.class).setAction(ACTION_NEXT), 0));

        mNotificationBuilder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mMediaSession.getSessionToken()));
    }

    private void setUpAsForeground(String text) {
        populateNotificationBuilderContent(text);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void updateNotification() {
        TrackMetadata currTrack = mTracks.get(mMusicRepository.getCurrentlyActiveTrack());
        populateNotificationBuilderContent(currTrack.getTitle()
                + " by " + currTrack.getArtist());
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void updateAudioMetadata() {
        if (mMusicRepository.getCurrentlyActiveTrack() < 0) {
            return;
        }
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.nougat_bg_2x);
        // Update the current metadata
        TrackMetadata current = mTracks.get(mMusicRepository.getCurrentlyActiveTrack());
        mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, current.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track #" + current.getTitle())
                .build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mNotificationManager.cancel(NOTIFICATION_ID);
            stopForeground(true);
        }
    }
}
