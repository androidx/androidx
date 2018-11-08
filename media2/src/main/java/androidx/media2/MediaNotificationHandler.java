/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;

import static androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.util.List;

/**
 * Provides default media notification for {@link MediaSessionService}, and set the service as
 * foreground/background according to the player state.
 *
 * @hide
 */
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MediaNotificationHandler extends
        MediaSession.SessionCallback.ForegroundServiceEventCallback {
    private static final int NOTIFICATION_ID = 1001;
    private static final String NOTIFICATION_CHANNEL_ID = "default_channel_id";

    private final MediaSessionService mServiceInstance;
    private final NotificationManager mNotificationManager;
    private final String mNotificationChannelName;

    private final Intent mStartSelfIntent;
    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mSkipToPrevAction;
    private final NotificationCompat.Action mSkipToNextAction;

    public MediaNotificationHandler(MediaSessionService service) {
        mServiceInstance = service;
        mStartSelfIntent = new Intent(mServiceInstance, mServiceInstance.getClass());

        mNotificationManager = (NotificationManager)
                mServiceInstance.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationChannelName = mServiceInstance.getResources().getString(
                R.string.default_notification_channel_name);

        mPlayAction = createNotificationAction(R.drawable.ic_play,
                R.string.play_button_content_description, ACTION_PLAY);
        mPauseAction = createNotificationAction(R.drawable.ic_pause,
                R.string.pause_button_content_description, ACTION_PAUSE);
        mSkipToPrevAction = createNotificationAction(R.drawable.ic_skip_to_previous,
                R.string.skip_to_previous_item_button_content_description, ACTION_SKIP_TO_PREVIOUS);
        mSkipToNextAction = createNotificationAction(R.drawable.ic_skip_to_next,
                R.string.skip_to_next_item_button_content_description, ACTION_SKIP_TO_NEXT);
    }

    /**
     * Sets the service as foreground/background according to the player state.
     * This will be called when the player state is changed.
     *
     * @param state player state
     */
    @Override
    public void onPlayerStateChanged(MediaSession session,
            @SessionPlayer.PlayerState int state) {
        MediaSessionService.MediaNotification mediaNotification =
                mServiceInstance.onUpdateNotification(session);
        if (mediaNotification == null) {
            // The service implementation doesn't want to use the automatic start/stopForeground
            // feature.
            return;
        }

        int id = mediaNotification.getNotificationId();
        Notification notification = mediaNotification.getNotification();

        if (isPlaybackStopped(state)) {
            stopForegroundServiceIfNeeded();
            mNotificationManager.notify(id, notification);
            return;
        }

        // state == SessionPlayer.PLAYER_STATE_PLAYING
        ContextCompat.startForegroundService(mServiceInstance, mStartSelfIntent);
        mServiceInstance.startForeground(id, notification);
    }

    @Override
    public void onSessionClosed(MediaSession session) {
        mServiceInstance.removeSession(session);
        stopForegroundServiceIfNeeded();
    }

    private void stopForegroundServiceIfNeeded() {
        List<MediaSession> sessions = mServiceInstance.getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            if (!isPlaybackStopped(sessions.get(i).getPlayer().getPlayerState())) {
                return;
            }
        }
        // Calling stopForeground(true) is a workaround for pre-L devices which prevents
        // the media notification from being undismissable.
        boolean shouldRemoveNotification =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
        mServiceInstance.stopForeground(shouldRemoveNotification);
    }

    /**
     * Creates a default media style notification for {@link MediaSessionService}.
     */
    public MediaSessionService.MediaNotification onUpdateNotification(MediaSession session) {
        ensureNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mServiceInstance, NOTIFICATION_CHANNEL_ID);

        // TODO: Filter actions when SessionPlayer#getSupportedActions() is introduced.
        builder.addAction(mSkipToPrevAction);
        if (session.getPlayer().getPlayerState() == SessionPlayer.PLAYER_STATE_PLAYING) {
            builder.addAction(mPauseAction);
        } else {
            builder.addAction(mPlayAction);
        }
        builder.addAction(mSkipToNextAction);

        // Set metadata info in the notification.
        if (session.getPlayer().getCurrentMediaItem() != null) {
            MediaMetadata metadata = session.getPlayer().getCurrentMediaItem().getMetadata();
            if (metadata != null) {
                CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                if (title == null) {
                    title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                }
                builder.setContentTitle(title)
                        .setContentText(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST))
                        .setLargeIcon(metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
            }
        }

        MediaStyle mediaStyle = new MediaStyle()
                .setCancelButtonIntent(createPendingIntent(ACTION_STOP))
                .setMediaSession(session.getSessionCompat().getSessionToken())
                .setShowActionsInCompactView(1 /* Show play/pause button only in compact view */);

        Notification notification = builder
                .setContentIntent(session.getImpl().getSessionActivity())
                .setDeleteIntent(createPendingIntent(ACTION_STOP))
                .setOnlyAlertOnce(true)
                .setSmallIcon(getSmallIconResId())
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .build();

        return new MediaSessionService.MediaNotification(NOTIFICATION_ID, notification);
    }

    private NotificationCompat.Action createNotificationAction(int iconResId, int titleResId,
            @PlaybackStateCompat.Actions long action) {
        CharSequence title = mServiceInstance.getResources().getText(titleResId);
        return new NotificationCompat.Action(iconResId, title, createPendingIntent(action));
    }

    private PendingIntent createPendingIntent(@PlaybackStateCompat.Actions long action) {
        int keyCode = PlaybackStateCompat.toKeyCode(action);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(new ComponentName(mServiceInstance, mServiceInstance.getClass()));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action != ACTION_PAUSE) {
            return PendingIntent.getForegroundService(
                    mServiceInstance, keyCode /* requestCode */, intent, 0 /* flags */);
        } else {
            return PendingIntent.getService(
                    mServiceInstance, keyCode /* requestCode */, intent, 0 /* flags */);
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return;
        }
        // Need to create a notification channel.
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                mNotificationChannelName, NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(channel);
    }

    private int getSmallIconResId() {
        int appIcon = mServiceInstance.getApplicationInfo().icon;
        if (appIcon != 0) {
            return appIcon;
        } else {
            // App icon is not set.
            return R.drawable.ic_music_note;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean isPlaybackStopped(int state) {
        return state == SessionPlayer.PLAYER_STATE_PAUSED
                || state == SessionPlayer.PLAYER_STATE_IDLE
                || state == SessionPlayer.PLAYER_STATE_ERROR;
    }
}
