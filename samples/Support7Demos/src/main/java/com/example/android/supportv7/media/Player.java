/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv7.media;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import com.example.android.supportv7.R;
/**
 * Abstraction of common playback operations of media items, such as play,
 * seek, etc. Used by PlaybackManager as a backend to handle actual playback
 * of media items.
 *
 * TODO: Introduce prepare() method and refactor subclasses accordingly.
 */
public abstract class Player {
    private static final String TAG = "SampleMediaRoutePlayer";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_PREPARING_FOR_PLAY = 1;
    protected static final int STATE_PREPARING_FOR_PAUSE = 2;
    protected static final int STATE_READY = 3;
    protected static final int STATE_PLAYING = 4;
    protected static final int STATE_PAUSED = 5;

    protected static final String NOTIFICATION_CHANNEL_ID =
            "com.example.android.supportv7.media.channel";
    protected static final int NOTIFICATION_ID = 1;

    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY;
    private static final PlaybackStateCompat INIT_PLAYBACK_STATE = new PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_NONE, 0, .0f).build();

    protected Context mContext;
    protected Callback mCallback;
    protected MediaSessionCompat mMediaSession;

    protected String mNotificationChannelId;
    private NotificationCompat.Action mPlayAction;
    private NotificationCompat.Action mPauseAction;

    public abstract boolean isRemotePlayback();
    public abstract boolean isQueuingSupported();

    public abstract void connect(RouteInfo route);
    public abstract void release();

    // basic operations that are always supported
    public abstract void play(final PlaylistItem item);
    public abstract void seek(final PlaylistItem item);
    public abstract void getStatus(final PlaylistItem item, final boolean update);
    public abstract void pause();
    public abstract void resume();
    public abstract void stop();

    // advanced queuing (enqueue & remove) are only supported
    // if isQueuingSupported() returns true
    public abstract void enqueue(final PlaylistItem item);
    public abstract PlaylistItem remove(String iid);

    public void takeSnapshot() {}
    public Bitmap getSnapshot() { return null; }

    /**
     * presentation display
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void updatePresentation() {}

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public static Player create(Context context, RouteInfo route, MediaSessionCompat session) {
        Player player;
        if (route != null && route.supportsControlCategory(
                MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
            player = new RemotePlayer(context);
        } else if (route != null) {
            player = new LocalPlayer.SurfaceViewPlayer(context);
        } else {
            player = new LocalPlayer.OverlayPlayer(context);
        }
        player.setPlayPauseNotificationAction();
        player.setMediaSession(session);
        player.initMediaSession();
        player.connect(route);
        return player;
    }

    protected void initMediaSession() {
        if (mMediaSession == null) {
            return;
        }
        mMediaSession.setMetadata(null);
        mMediaSession.setPlaybackState(INIT_PLAYBACK_STATE);
    }

    protected void updateMetadata(PlaylistItem currentItem) {
        if (mMediaSession == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Update metadata: currentItem=" + currentItem);
        }
        if (currentItem == null) {
            mMediaSession.setMetadata(null);
            return;
        }
        MediaMetadataCompat.Builder bob = new MediaMetadataCompat.Builder();
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentItem.getTitle());
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Subtitle of the thing");
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                "Description of the thing");
        bob.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, getSnapshot());
        mMediaSession.setMetadata(bob.build());

        maybeCreateNotificationChannel();
        boolean isPlaying = currentItem.getState() == STATE_PREPARING_FOR_PLAY
                || currentItem.getState() == STATE_PLAYING;
        Notification notification = new NotificationCompat.Builder(mContext,
                mNotificationChannelId)
                .addAction(isPlaying ? mPauseAction : mPlayAction)
                .setSmallIcon(R.drawable.app_sample_code)
                .setContentTitle(currentItem.getTitle())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new androidx.media.app.NotificationCompat
                        .DecoratedMediaCustomViewStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void maybeCreateNotificationChannel() {
        if (mNotificationChannelId != null) {
            return;
        }
        mNotificationChannelId = NOTIFICATION_CHANNEL_ID;
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel";
            String description = "Description";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    mNotificationChannelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager =
                    mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Action createNotificationAction(int iconResId, CharSequence title,
            @PlaybackStateCompat.Actions long action) {
        return new NotificationCompat.Action(iconResId, title, createPendingIntent(action));
    }

    private PendingIntent createPendingIntent(@PlaybackStateCompat.Actions long action) {
        int keyCode = PlaybackStateCompat.toKeyCode(action);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(new ComponentName(mContext.getPackageName(),
                SampleMediaButtonReceiver.class.getName()));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));

        return PendingIntent.getBroadcast(mContext, keyCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    protected void publishState(int state) {
        if (mMediaSession == null) {
            return;
        }
        PlaybackStateCompat.Builder bob = new PlaybackStateCompat.Builder();
        bob.setActions(PLAYBACK_ACTIONS);
        switch (state) {
            case STATE_PLAYING:
                bob.setState(PlaybackStateCompat.STATE_PLAYING, -1, 1);
                break;
            case STATE_READY:
            case STATE_PAUSED:
                bob.setState(PlaybackStateCompat.STATE_PAUSED, -1, 0);
                break;
            case STATE_PREPARING_FOR_PLAY:
            case STATE_PREPARING_FOR_PAUSE:
                bob.setState(PlaybackStateCompat.STATE_BUFFERING, -1, 0);
                break;
            case STATE_IDLE:
                bob.setState(PlaybackStateCompat.STATE_STOPPED, -1, 0);
                break;
        }
        PlaybackStateCompat pbState = bob.build();
        Log.d(TAG, "Setting state to " + pbState);
        mMediaSession.setPlaybackState(pbState);
        if (state != STATE_IDLE) {
            mMediaSession.setActive(true);
        } else {
            mMediaSession.setActive(false);
        }
    }

    private void setMediaSession(MediaSessionCompat session) {
        mMediaSession = session;
    }

    private void setPlayPauseNotificationAction() {
        mPlayAction = createNotificationAction(
                R.drawable.ic_media_play, "play", ACTION_PLAY);
        mPauseAction = createNotificationAction(
                R.drawable.ic_media_pause, "pause", ACTION_PAUSE);
    }

    public interface Callback {
        void onError();
        void onCompletion();
        void onPlaylistChanged();
        void onPlaylistReady();
    }
}
