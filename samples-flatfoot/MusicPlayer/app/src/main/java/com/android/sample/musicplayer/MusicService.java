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
import android.support.annotation.RawRes;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import com.android.sample.musicplayer.MusicRepository.TrackMetadata;
import com.android.support.lifecycle.LifecycleService;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;

import java.io.IOException;
import java.util.List;

/**
 * Music playback service.
 */
public class MusicService extends LifecycleService implements OnCompletionListener,
        OnPreparedListener {
    // Note that only START action is an entry point "exposed" to the rest of the
    // application. The rest are actions set on the notification intents fired off
    // by this service itself.
    public static final String ACTION_START = "com.android.sample.musicplayer.action.START";
    private static final String ACTION_PLAY = "com.android.sample.musicplayer.action.PLAY";
    private static final String ACTION_PAUSE = "com.android.sample.musicplayer.action.PAUSE";
    private static final String ACTION_STOP = "com.android.sample.musicplayer.action.STOP";
    private static final String ACTION_NEXT = "com.android.sample.musicplayer.action.NEXT";
    private static final String ACTION_PREV = "com.android.sample.musicplayer.action.PREV";

    private static final String RESOURCE_PREFIX =
            "android.resource://com.android.sample.musicplayer/";

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    private static final int NOTIFICATION_ID = 1;

    private MediaSessionCompat mMediaSession;

    private MediaPlayer mMediaPlayer = null;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private MusicRepository mMusicRepository;
    private int mCurrPlaybackState;
    private int mCurrActiveTrackIndex;
    private List<TrackMetadata> mTracks;

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaSession = new MediaSessionCompat(this, MusicService.class.getSimpleName());
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);

        mMusicRepository = MusicRepository.getInstance();

        mTracks = mMusicRepository.getTracks();

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
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                processPreviousRequest();
            }

            @Override
            public void onStop() {
                super.onStop();
                processStopRequest();
            }
        });

        mNotificationManager = NotificationManagerCompat.from(this);

        // Register self as the observer on the LiveData object that wraps the currently
        // active track index.
        LiveData<Integer> currentlyActiveTrackData = mMusicRepository.getCurrentlyActiveTrackData();
        mCurrActiveTrackIndex = currentlyActiveTrackData.getValue();
        currentlyActiveTrackData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                mCurrActiveTrackIndex = integer;
                if (mCurrActiveTrackIndex < 0) {
                    return;
                }

                // Create the media player if necessary, set its data to the currently active track
                // and call prepare(). This will eventually result in an asynchronous call to
                // our onPrepared() method which will transition from PREPARING into PLAYING state.
                createMediaPlayerIfNeeded();
                try {
                    mMusicRepository.setState(MusicRepository.STATE_PREPARING);
                    @RawRes int trackRawRes = mTracks.get(mCurrActiveTrackIndex).getTrackRes();
                    mMediaPlayer.setDataSource(getBaseContext(),
                            Uri.parse(RESOURCE_PREFIX + trackRawRes));
                    mMediaPlayer.prepare();
                } catch (IOException ioe) {
                }
                // As the media player is preparing the track, update the media session and the
                // notification with the metadata of that track.
                updateAudioMetadata();
                updateNotification();
            }
        });

        // Register self as the observer on the LiveData object that wraps the playback state.
        LiveData<Integer> stateData = mMusicRepository.getStateData();
        mCurrPlaybackState = stateData.getValue();
        stateData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                mCurrPlaybackState = integer;
                switch (mCurrPlaybackState) {
                    case MusicRepository.STATE_INITIAL:
                        createMediaPlayerIfNeeded();
                        break;
                    case MusicRepository.STATE_PLAYING:
                        // Start the media player and update the ongoing notification
                        configAndStartMediaPlayer();
                        updateNotification();
                        break;
                    case MusicRepository.STATE_PAUSED:
                        // Pause the media player and update the ongoing notification
                        mMediaPlayer.pause();
                        updateNotification();
                }
            }
        });
    }

    private void createMediaPlayerIfNeeded() {
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
        super.onStartCommand(intent, flags, startId);

        // Note that we don't do anything for the START action. The purpose of that action
        // is to start the service. As the service registers itself to observe changes to
        // playback state and current track, it will start the matching flows as a response
        // to those changes.
        // Here we handle service-internal actions that are registered on notification intents.
        if (intent.getAction().equals(ACTION_PLAY)) {
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

    private void processPlayRequest() {
        // The logic here is different depending on our current state
        if (mCurrPlaybackState == MusicRepository.STATE_STOPPED) {
            // If we're stopped, just go ahead to the next song and start playing.
            playNextSong();
        } else if (mCurrPlaybackState == MusicRepository.STATE_PAUSED) {
            // If we're paused, just continue playback. We are registered to listen to the changes
            // in LiveData that tracks the playback state, and that observer will update our ongoing
            // notification and resume the playback.
            mMusicRepository.setState(MusicRepository.STATE_PLAYING);
        }
    }

    private void processPauseRequest() {
        if (mCurrPlaybackState == MusicRepository.STATE_PLAYING) {
            // Move to the paused state. We are registered
            // to listen to the changes in LiveData that tracks the playback state,
            // and that observer will update our ongoing notification and pause the media
            // player.
            mMusicRepository.setState(MusicRepository.STATE_PAUSED);
        }
    }

    private void processStopRequest() {
        processStopRequest(false);
    }

    private void processStopRequest(boolean force) {
        if (mCurrPlaybackState != MusicRepository.STATE_STOPPED || force) {
            mMusicRepository.setState(MusicRepository.STATE_STOPPED);
            // let go of all resources...
            relaxResources(true);
            // cancel the notification
            mNotificationManager.cancel(NOTIFICATION_ID);
            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    private void processNextRequest() {
        if (mCurrPlaybackState != MusicRepository.STATE_STOPPED) {
            playNextSong();
        }
    }

    private void processPreviousRequest() {
        if (mCurrPlaybackState != MusicRepository.STATE_STOPPED) {
            playPrevSong();
        }
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
    }

    /**
     * Starts playing the next song in our repository.
     */
    private void playNextSong() {
        relaxResources(false); // release everything except MediaPlayer

        // Ask the repository to go to the next track. We are registered to listen to the
        // changes in LiveData that tracks the current track, and that observer will point the
        // media player to the right URI
        mMusicRepository.goToNextTrack();
    }

    /**
     * Starts playing the previous song in our repository.
     */
    private void playPrevSong() {
        relaxResources(false); // release everything except MediaPlayer

        // Ask the repository to go to the next track. We are registered to listen to the
        // changes in LiveData that tracks the current track, and that observer will point the
        // media player to the right URI
        mMusicRepository.goToPreviousTrack();
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
        // We are registered to listen to the changes in LiveData that tracks the playback state,
        // and that observer will update our ongoing notification
        mMusicRepository.setState(MusicRepository.STATE_PLAYING);
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

        boolean isPlaying = (mCurrPlaybackState == MusicRepository.STATE_PLAYING);

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

    private void updateNotification() {
        if (mCurrPlaybackState == MusicRepository.STATE_INITIAL) {
            return;
        }

        if (mNotificationBuilder == null) {
            // This is the very first time we're creating our ongoing notification, and marking
            // the service to be in the foreground.
            populateNotificationBuilderContent("Initializing...");
            startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
            return;
        }

        TrackMetadata currTrack = mTracks.get(mCurrActiveTrackIndex);
        populateNotificationBuilderContent(currTrack.getTitle()
                + " by " + currTrack.getArtist());
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void updateAudioMetadata() {
        if (mCurrPlaybackState == MusicRepository.STATE_INITIAL) {
            return;
        }
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.nougat_bg_2x);
        // Update the current metadata
        TrackMetadata current = mTracks.get(mCurrActiveTrackIndex);
        mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, current.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track #" + current.getTitle())
                .build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mNotificationManager.cancel(NOTIFICATION_ID);
            stopForeground(true);
        }
    }
}
