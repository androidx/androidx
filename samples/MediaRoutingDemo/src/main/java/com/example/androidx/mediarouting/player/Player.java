/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.player;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.data.PlaylistItem;

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
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING_FOR_PLAY = 1;
    public static final int STATE_PREPARING_FOR_PAUSE = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_PLAYING = 4;
    public static final int STATE_PAUSED = 5;

    protected static final String NOTIFICATION_CHANNEL_ID =
            "com.example.androidx.media.channel";
    protected static final int NOTIFICATION_ID = 1;

    private static final long PLAYBACK_ACTIONS = ACTION_PAUSE
            | ACTION_PLAY;
    private static final PlaybackStateCompat INIT_PLAYBACK_STATE = new PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_NONE, 0, .0f).build();

    @NonNull
    protected Context mContext;
    @NonNull
    protected Callback mCallback;
    @NonNull
    protected MediaSessionCompat mMediaSession;

    @NonNull
    protected String mNotificationChannelId;
    private NotificationCompat.Action mPlayAction;
    private NotificationCompat.Action mPauseAction;

    /**
     * Check if player is playing a remote playback.
     * @return
     */
    public abstract boolean isRemotePlayback();

    /**
     * Returns whether the queuing is supported.
     * @return
     */
    public abstract boolean isQueuingSupported();

    /**
     * Connects the player with a route info.
     * @param route
     */
    public abstract void connect(@NonNull RouteInfo route);

    /**
     * Release the player resources.
     */
    @CallSuper
    public void release() {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(mContext);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    // basic operations that are always supported

    /**
     * Player play operation
     * @param item
     */
    public abstract void play(@NonNull PlaylistItem item);

    /**
     * Player seek operation.
     * @param item
     */
    public abstract void seek(@NonNull PlaylistItem item);

    /**
     * Get player status of an item.
     *
     * @param item
     * @param shouldUpdate
     */
    public abstract void getPlaylistItemStatus(@NonNull PlaylistItem item, boolean shouldUpdate);

    /**
     * Player pause operation.
     */
    public abstract void pause();

    /**
     * Player resume operation.
     */
    public abstract void resume();

    /**
     * Player step operation.
     */
    public abstract void stop();

    // advanced queuing (enqueue & remove) are only supported
    // if isQueuingSupported() returns true

    /**
     * Enqueue an item in the playlist.
     * @param item
     */
    public abstract void enqueue(@NonNull PlaylistItem item);

    /**
     * Remove an item for the playlist.
     * @param iid
     * @return
     */
    @NonNull
    public abstract PlaylistItem remove(@NonNull String iid);

    /**
     * Takes player snapshot.
     */
    public void takeSnapshot() {
    }

    @Nullable
    public Bitmap getSnapshot() {
        return null;
    }

    /**
     * presentation display
     */
    public void updatePresentation() {
    }

    public void setCallback(@NonNull Callback callback) {
        mCallback = callback;
    }

    /**
     * Creates a {@link Player} for the given {@code route}, whose UI is hosted by the given {@code
     * activity}.
     */
    @NonNull
    public static Player createPlayerForActivity(
            @NonNull Activity activity,
            @NonNull RouteInfo route,
            @NonNull MediaSessionCompat session) {
        Player player;
        if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
            player = new RemotePlayer(activity);
        } else {
            player = new LocalPlayer.SurfaceViewPlayer(activity);
        }
        player.setPlayPauseNotificationAction();
        player.setMediaSession(session);
        player.initMediaSession();
        player.connect(route);
        return player;
    }

    /** Creates a {@link Player} for playback on an overlay. */
    @NonNull
    public static Player createPlayerForOverlay(@NonNull Context context) {
        Player player = new LocalPlayer.OverlayPlayer(context);
        player.setPlayPauseNotificationAction();
        return player;
    }

    /**
     * Initialize the media session.
     */
    public void initMediaSession() {
        if (mMediaSession == null) {
            return;
        }
        mMediaSession.setMetadata(null);
        mMediaSession.setPlaybackState(INIT_PLAYBACK_STATE);
    }

    /**
     * Update the player metadata.
     * @param currentItem
     */
    public void updateMetadata(@NonNull PlaylistItem currentItem) {
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
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                            mContext,
                            "POST_NOTIFICATIONS permission not available",
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }
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
            NotificationChannel channel = Api26Impl.createNotificationChannel(
                    mNotificationChannelId, name.toString(), importance);

            Api26Impl.setDescription(channel, description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager =
                    Api26Impl.getSystemServiceReturnsNotificationManager(mContext);
            Api26Impl.createNotificationChannel(notificationManager, channel);
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
                MediaButtonReceiver.class.getName()));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));

        return PendingIntent.getBroadcast(mContext, keyCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Publish the player state.
     * @param state
     */
    public void publishState(int state) {
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

    private void setMediaSession(@NonNull MediaSessionCompat session) {
        mMediaSession = session;
    }

    private void setPlayPauseNotificationAction() {
        mPlayAction = createNotificationAction(
                R.drawable.ic_media_play, "play", ACTION_PLAY);
        mPauseAction = createNotificationAction(
                R.drawable.ic_media_pause, "pause", ACTION_PAUSE);
    }

    /**
     * The player callback
     */
    public interface Callback {
        /**
         * On player error.
         */
        void onError();

        /**
         * On Playlist play completed.
         */
        void onCompletion();

        /**
         * On Playlist changed.
         */
        void onPlaylistChanged();

        /**
         * On playlist ready.
         */
        void onPlaylistReady();
    }
    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static NotificationChannel createNotificationChannel(String notificationChannelId,
                String name, int importance) {
            return new NotificationChannel(notificationChannelId, name, importance);
        }

        static void createNotificationChannel(NotificationManager notificationManager,
                NotificationChannel channel) {
            notificationManager.createNotificationChannel(channel);
        }

        static void setDescription(NotificationChannel notificationChannel, String description) {
            notificationChannel.setDescription(description);
        }

        static NotificationManager getSystemServiceReturnsNotificationManager(Context context) {
            return context.getSystemService(NotificationManager.class);
        }
    }
}
