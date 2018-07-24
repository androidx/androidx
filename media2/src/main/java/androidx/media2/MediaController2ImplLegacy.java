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

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

import static androidx.media2.MediaConstants2.ARGUMENT_COMMAND_CODE;
import static androidx.media2.MediaConstants2.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media2.MediaConstants2.ARGUMENT_PACKAGE_NAME;
import static androidx.media2.MediaConstants2.ARGUMENT_PID;
import static androidx.media2.MediaConstants2.ARGUMENT_ROUTE_BUNDLE;
import static androidx.media2.MediaConstants2.ARGUMENT_UID;
import static androidx.media2.MediaConstants2.CONTROLLER_COMMAND_BY_COMMAND_CODE;
import static androidx.media2.MediaPlayerConnector.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.MediaPlayerConnector.PLAYER_STATE_IDLE;
import static androidx.media2.MediaPlayerConnector.UNKNOWN_TIME;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.BundleCompat;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaController2.MediaController2Impl;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaController2.VolumeDirection;
import androidx.media2.MediaController2.VolumeFlags;
import androidx.media2.MediaPlayerConnector.BuffState;
import androidx.media2.MediaPlaylistAgent.RepeatMode;
import androidx.media2.MediaPlaylistAgent.ShuffleMode;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class MediaController2ImplLegacy implements MediaController2Impl {

    private static final String TAG = "MC2ImplLegacy";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final long POSITION_DIFF_TOLERANCE = 100;
    private static final String SESSION_COMMAND_ON_EXTRA_CHANGED =
            "android.media.session.command.ON_EXTRA_CHANGED";
    private static final String SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED =
            "android.media.session.command.ON_CAPTIONING_ENALBED_CHANGED";

    // Note: Using {@code null} doesn't helpful here because MediaBrowserServiceCompat always wraps
    //       the rootHints so it becomes non-null.
    static final Bundle sDefaultRootExtras = new Bundle();
    static {
        sDefaultRootExtras.putBoolean(MediaConstants2.ROOT_EXTRA_DEFAULT, true);
    }

    final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionToken2 mToken;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ControllerCallback mCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mCallbackExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final HandlerThread mHandlerThread;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Handler mHandler;

    final Object mLock = new Object();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaController2 mInstance;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaBrowserCompat mBrowserCompat;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<MediaItem2> mPlaylist;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<QueueItem> mQueue;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @RepeatMode int mRepeatMode;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @ShuffleMode int mShuffleMode;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mPlayerState;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2 mCurrentMediaItem;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mBufferingState;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mCurrentMediaItemIndex;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItem2 mSkipToPlaylistItem;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mBufferedPosition;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SessionCommandGroup2 mAllowedCommands;

    // Media 1.0 variables
    @GuardedBy("mLock")
    private MediaControllerCompat mControllerCompat;
    @GuardedBy("mLock")
    private ControllerCompatCallback mControllerCompatCallback;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaMetadataCompat mMediaMetadataCompat;

    // Assignment should be used with the lock hold, but should be used without a lock to prevent
    // potential deadlock.
    @GuardedBy("mLock")
    private volatile boolean mConnected;

    MediaController2ImplLegacy(@NonNull Context context, @NonNull MediaController2 instance,
            @NonNull SessionToken2 token, @NonNull Executor executor,
            @NonNull ControllerCallback callback) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mToken = token;
        mCallback = callback;
        mCallbackExecutor = executor;

        if (mToken.getType() == SessionToken2.TYPE_SESSION) {
            synchronized (mLock) {
                mBrowserCompat = null;
            }
            connectToSession((MediaSessionCompat.Token) mToken.getBinder());
        } else {
            connectToService();
        }
    }

    @Override
    public void close() {
        if (DEBUG) {
            Log.d(TAG, "release from " + mToken);
        }
        synchronized (mLock) {
            if (mIsReleased) {
                // Prevent re-enterance from the ControllerCallback.onDisconnected()
                return;
            }
            mHandler.removeCallbacksAndMessages(null);

            if (Build.VERSION.SDK_INT >= 18) {
                mHandlerThread.quitSafely();
            } else {
                mHandlerThread.quit();
            }

            mIsReleased = true;

            if (mBrowserCompat != null) {
                mBrowserCompat.disconnect();
                mBrowserCompat = null;
            }
            if (mControllerCompat != null) {
                mControllerCompat.unregisterCallback(mControllerCompatCallback);
                mControllerCompat = null;
            }
            mConnected = false;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onDisconnected(mInstance);
            }
        });
    }

    @Override
    public @NonNull SessionToken2 getSessionToken() {
        return mToken;
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mConnected;
        }
    }

    @Override
    public void play() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().play();
        }
    }

    @Override
    public void pause() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().pause();
        }
    }

    @Override
    public void reset() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().stop();
        }
    }

    @Override
    public void prepare() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().prepare();
        }
    }

    @Override
    public void fastForward() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().fastForward();
        }
    }

    @Override
    public void rewind() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().rewind();
        }
    }

    @Override
    public void seekTo(long pos) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().seekTo(pos);
        }
    }

    @Override
    public void skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
    }

    @Override
    public void skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
    }

    @Override
    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().playFromMediaId(mediaId, extras);
        }
    }

    @Override
    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().playFromSearch(query, extras);
        }
    }

    @Override
    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().playFromUri(uri, extras);
        }
    }

    @Override
    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().prepareFromMediaId(mediaId, extras);
        }
    }

    @Override
    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().prepareFromSearch(query, extras);
        }
    }

    @Override
    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().prepareFromUri(uri, extras);
        }
    }

    @Override
    public void setVolumeTo(int value, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.setVolumeTo(value, flags);
        }
    }

    @Override
    public void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.adjustVolume(direction, flags);
        }
    }

    @Override
    public @Nullable PendingIntent getSessionActivity() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mControllerCompat.getSessionActivity();
        }
    }

    @Override
    public int getPlayerState() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return MediaPlayerConnector.PLAYER_STATE_ERROR;
            }
            return mPlayerState;
        }
    }

    @Override
    public long getDuration() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mMediaMetadataCompat != null
                    && mMediaMetadataCompat.containsKey(METADATA_KEY_DURATION)) {
                return mMediaMetadataCompat.getLong(METADATA_KEY_DURATION);
            }
        }
        return UNKNOWN_TIME;
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mPlaybackStateCompat != null) {
                return mPlaybackStateCompat.getCurrentPosition(mInstance.mTimeDiff);
            }
            return UNKNOWN_TIME;
        }
    }

    @Override
    public float getPlaybackSpeed() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0f;
            }
            return (mPlaybackStateCompat == null) ? 0f : mPlaybackStateCompat.getPlaybackSpeed();
        }
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        // Unsupported action
    }

    @Override
    public @BuffState int getBufferingState() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mPlaybackStateCompat == null ? MediaPlayerConnector.BUFFERING_STATE_UNKNOWN
                    : MediaUtils2.toBufferingState(mPlaybackStateCompat.getState());
        }
    }

    @Override
    public long getBufferedPosition() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            return (mPlaybackStateCompat == null) ? UNKNOWN_TIME
                    : mPlaybackStateCompat.getBufferedPosition();
        }
    }

    @Override
    public @Nullable PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaybackInfo;
        }
    }

    @Override
    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            if (mCurrentMediaItem != null && mediaId.equals(mCurrentMediaItem.getMediaId())) {
                mControllerCompat.getTransportControls().setRating(
                        MediaUtils2.convertToRatingCompat(rating));
            }
        }
    }

    @Override
    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver cb) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.sendCommand(command.getCustomCommand(), args, cb);
        }
    }

    @Override
    public @Nullable List<MediaItem2> getPlaylist() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaylist;
        }
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        // Unsupported action.
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        // Unsupported action.
    }

    @Override
    public @Nullable MediaMetadata2 getPlaylistMetadata() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaylistMetadata;
        }
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.addQueueItem(
                    MediaUtils2.convertToMediaMetadataCompat(item.getMetadata()).getDescription(),
                    index);
        }
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.removeQueueItem(
                    MediaUtils2.convertToQueueItem(item).getDescription());
        }
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            if (mPlaylist == null || mPlaylist.size() <= index) {
                return;
            }
            removePlaylistItem(mPlaylist.get(index));
            addPlaylistItem(index, item);
        }
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mCurrentMediaItem;
        }
    }

    @Override
    public void skipToPreviousItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().skipToPrevious();
        }
    }

    @Override
    public void skipToNextItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mControllerCompat.getTransportControls().skipToNext();
        }
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            mSkipToPlaylistItem = item;
            mControllerCompat.getTransportControls().skipToQueueItem(
                    MediaUtils2.convertToQueueItem(item).getQueueId());
        }
    }

    @Override
    public @RepeatMode int getRepeatMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return MediaPlaylistAgent.REPEAT_MODE_NONE;
            }
            return mRepeatMode;
        }
    }

    @Override
    public void setRepeatMode(@RepeatMode int repeatMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            // MediaPlaylistAgent.RepeatMode has the same values with
            // PlaybackStateCompat.RepeatMode.
            mControllerCompat.getTransportControls().setRepeatMode(repeatMode);
        }
    }

    @Override
    public @ShuffleMode int getShuffleMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return MediaPlaylistAgent.SHUFFLE_MODE_NONE;
            }
            return mShuffleMode;
        }
    }

    @Override
    public void setShuffleMode(@ShuffleMode int shuffleMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            // MediaPlaylistAgent.ShuffleMode has the same values with
            // PlaybackStateCompat.ShuffleMode.
            mControllerCompat.getTransportControls().setShuffleMode(shuffleMode);
        }
    }

    @Override
    public void subscribeRoutesInfo() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
        }
        sendCommand(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
    }

    @Override
    public void unsubscribeRoutesInfo() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
        }
        sendCommand(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
    }

    @Override
    public void selectRoute(@NonNull Bundle route) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
        }
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_ROUTE_BUNDLE, route);
        sendCommand(COMMAND_CODE_SESSION_SELECT_ROUTE, args);
    }

    @Override
    public @NonNull Context getContext() {
        return mContext;
    }

    @Override
    public @NonNull ControllerCallback getCallback() {
        return mCallback;
    }

    @Override
    public @NonNull Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Override
    public @Nullable MediaBrowserCompat getBrowserCompat() {
        synchronized (mLock) {
            return mBrowserCompat;
        }
    }

    @Override
    public @NonNull MediaController2 getInstance() {
        return mInstance;
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked() {
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked token=" + mToken);
        }
        final SessionCommandGroup2.Builder commandsBuilder = new SessionCommandGroup2.Builder();

        synchronized (mLock) {
            if (mIsReleased || mConnected) {
                return;
            }
            long sessionFlags = mControllerCompat.getFlags();
            commandsBuilder.addAllPlaybackCommands();
            commandsBuilder.addAllVolumeCommands();
            commandsBuilder.addAllSessionCommands();

            commandsBuilder.removeCommand(COMMAND_CODE_PLAYBACK_SET_SPEED);
            commandsBuilder.removeCommand(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
            commandsBuilder.removeCommand(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
            commandsBuilder.removeCommand(COMMAND_CODE_SESSION_SELECT_ROUTE);

            if ((sessionFlags & FLAG_HANDLES_QUEUE_COMMANDS) != 0) {
                commandsBuilder.addAllPlaylistCommands();
                commandsBuilder.removeCommand(COMMAND_CODE_PLAYLIST_SET_LIST);
                commandsBuilder.removeCommand(COMMAND_CODE_PLAYLIST_SET_LIST_METADATA);
            }

            commandsBuilder.addCommand(new SessionCommand2(SESSION_COMMAND_ON_EXTRA_CHANGED, null));
            commandsBuilder.addCommand(
                    new SessionCommand2(SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED, null));

            mAllowedCommands = commandsBuilder.build();

            mPlaybackStateCompat = mControllerCompat.getPlaybackState();
            if (mPlaybackStateCompat == null) {
                mPlayerState = PLAYER_STATE_IDLE;
                mBufferedPosition = UNKNOWN_TIME;
            } else {
                mPlayerState = MediaUtils2.convertToPlayerState(mPlaybackStateCompat.getState());
                mBufferedPosition = mPlaybackStateCompat.getBufferedPosition();
            }

            mPlaybackInfo = MediaUtils2.toPlaybackInfo2(mControllerCompat.getPlaybackInfo());

            mRepeatMode = mControllerCompat.getRepeatMode();
            mShuffleMode = mControllerCompat.getShuffleMode();

            mPlaylist = MediaUtils2.convertQueueItemListToMediaItem2List(
                    mControllerCompat.getQueue());
            mPlaylistMetadata = MediaUtils2.convertToMediaMetadata2(
                    mControllerCompat.getQueueTitle());

            // Call this after set playlist.
            setCurrentMediaItemLocked(mControllerCompat.getMetadata());
            mConnected = true;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onConnected(mInstance, commandsBuilder.build());
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void connectToSession(MediaSessionCompat.Token sessionCompatToken) {
        MediaControllerCompat controllerCompat = null;
        try {
            controllerCompat = new MediaControllerCompat(mContext, sessionCompatToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        synchronized (mLock) {
            mControllerCompat = controllerCompat;
            mControllerCompatCallback = new ControllerCompatCallback();
            mControllerCompat.registerCallback(mControllerCompatCallback, mHandler);
        }
    }

    private void connectToService() {
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    mBrowserCompat = new MediaBrowserCompat(mContext, mToken.getComponentName(),
                            new ConnectionCallback(), sDefaultRootExtras);
                    mBrowserCompat.connect();
                }
            }
        });
    }

    private void sendCommand(int commandCode) {
        sendCommand(commandCode, null);
    }

    private void sendCommand(int commandCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(ARGUMENT_COMMAND_CODE, commandCode);
        sendCommand(CONTROLLER_COMMAND_BY_COMMAND_CODE, args, null);
    }

    private void sendCommand(String command) {
        sendCommand(command, null, null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendCommand(String command, ResultReceiver receiver) {
        sendCommand(command, null, receiver);
    }

    private void sendCommand(String command, Bundle args, ResultReceiver receiver) {
        if (args == null) {
            args = new Bundle();
        }
        MediaControllerCompat controller;
        ControllerCompatCallback callback;
        synchronized (mLock) {
            controller = mControllerCompat;
            callback = mControllerCompatCallback;
        }
        BundleCompat.putBinder(args, ARGUMENT_ICONTROLLER_CALLBACK,
                callback.getIControllerCallback().asBinder());
        args.putString(ARGUMENT_PACKAGE_NAME, mContext.getPackageName());
        args.putInt(ARGUMENT_UID, Process.myUid());
        args.putInt(ARGUMENT_PID, Process.myPid());
        controller.sendCommand(command, args, receiver);
    }

    @SuppressWarnings({"GuardedBy", "WeakerAccess"}) /* WeakerAccess for synthetic access */
    void setCurrentMediaItemLocked(MediaMetadataCompat metadata) {
        mMediaMetadataCompat = metadata;
        if (metadata == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = null;
            return;
        }

        if (mPlaylist == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils2.convertToMediaItem2(metadata);
            return;
        }

        String mediaId = metadata.getString(METADATA_KEY_MEDIA_ID);
        if (mPlaybackStateCompat != null) {
            // If playback state is updated before, compare UUID using queue id and media id.
            UUID uuid = MediaUtils2.createUuidByQueueIdAndMediaId(
                    mPlaybackStateCompat.getActiveQueueItemId(), mediaId);
            for (int i = 0; i < mPlaylist.size(); ++i) {
                MediaItem2 item = mPlaylist.get(i);
                if (item != null && uuid.equals(item.getUuid())) {
                    mCurrentMediaItem = item;
                    mCurrentMediaItemIndex = i;
                    return;
                }
            }
        }

        if (mediaId == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils2.convertToMediaItem2(metadata);
            return;
        }

        // Need to find the media item in the playlist using mediaId.
        // Note that there can be multiple media items with the same media id.
        if (mSkipToPlaylistItem != null && mediaId.equals(mSkipToPlaylistItem.getMediaId())) {
            // metadata changed after skipToPlaylistIItem() was called.
            mCurrentMediaItem = mSkipToPlaylistItem;
            mCurrentMediaItemIndex = mPlaylist.indexOf(mSkipToPlaylistItem);
            mSkipToPlaylistItem = null;
            return;
        }

        MediaItem2 item;
        // Find mediaId from the playlist.
        for (int i = 0; i < mPlaylist.size(); ++i) {
            item = mPlaylist.get(i);
            if (item != null && mediaId.equals(item.getMediaId())) {
                mCurrentMediaItemIndex = i;
                mCurrentMediaItem = item;
                return;
            }
        }

        // Failed to find media item from the playlist.
        mCurrentMediaItemIndex = -1;
        mCurrentMediaItem = MediaUtils2.convertToMediaItem2(mMediaMetadataCompat);
    }

    private class ConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        ConnectionCallback() {
        }

        @Override
        public void onConnected() {
            MediaBrowserCompat browser = getBrowserCompat();
            if (browser != null) {
                connectToSession(browser.getSessionToken());
            } else if (DEBUG) {
                Log.d(TAG, "Controller is closed prematually", new IllegalStateException());
            }
        }

        @Override
        public void onConnectionSuspended() {
            close();
        }

        @Override
        public void onConnectionFailed() {
            close();
        }
    }

    private final class ControllerCompatCallback extends MediaControllerCompat.Callback {
        ControllerCompatCallback() {
        }

        @Override
        public void onSessionReady() {
            onConnectedNotLocked();
        }

        @Override
        public void onSessionDestroyed() {
            close();
        }

        @Override
        public void onSessionEvent(final String event, final Bundle extras) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCustomCommand(mInstance, new SessionCommand2(event, null), extras,
                            null);
                }
            });
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            final PlaybackStateCompat prevState;
            final MediaItem2 currentItem;
            synchronized (mLock) {
                prevState = mPlaybackStateCompat;
                mPlaybackStateCompat = state;
                mPlayerState = MediaUtils2.convertToPlayerState(state.getState());
                mBufferedPosition = state.getBufferedPosition();

                if (mQueue != null) {
                    for (int i = 0; i < mQueue.size(); ++i) {
                        if (mQueue.get(i).getQueueId() == state.getActiveQueueItemId()) {
                            mCurrentMediaItemIndex = i;
                            mCurrentMediaItem = mPlaylist.get(i);
                        }
                    }
                }
                currentItem = mCurrentMediaItem;
            }
            if (state == null) {
                if (prevState != null) {
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlayerStateChanged(mInstance,
                                    PLAYER_STATE_IDLE);
                        }
                    });
                }
                return;
            }
            if (prevState == null || prevState.getState() != state.getState()) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onPlayerStateChanged(
                                mInstance, MediaUtils2.convertToPlayerState(state.getState()));
                    }
                });
            }
            if (prevState == null || prevState.getPlaybackSpeed() != state.getPlaybackSpeed()) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onPlaybackSpeedChanged(mInstance, state.getPlaybackSpeed());
                    }
                });
            }

            if (prevState != null) {
                final long currentPosition = state.getCurrentPosition(mInstance.mTimeDiff);
                long positionDiff = Math.abs(currentPosition
                        - prevState.getCurrentPosition(mInstance.mTimeDiff));
                if (positionDiff > POSITION_DIFF_TOLERANCE) {
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onSeekCompleted(mInstance, currentPosition);
                        }
                    });
                }
            }

            // Update buffering state if needed
            final int bufferingState = MediaUtils2.toBufferingState(state.getState());
            final int prevBufferingState = prevState == null
                    ? MediaPlayerConnector.BUFFERING_STATE_UNKNOWN
                    : MediaUtils2.toBufferingState(prevState.getState());
            if (bufferingState != prevBufferingState) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onBufferingStateChanged(mInstance, currentItem, bufferingState);
                    }
                });
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (mLock) {
                setCurrentMediaItemLocked(metadata);
            }
        }

        @Override
        public void onQueueChanged(List<QueueItem> queue) {
            final List<MediaItem2> playlist;
            final MediaMetadata2 playlistMetadata;
            synchronized (mLock) {
                mQueue = queue;
                mPlaylist = MediaUtils2.convertQueueItemListToMediaItem2List(queue);
                playlist = mPlaylist;
                playlistMetadata = mPlaylistMetadata;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaylistChanged(mInstance, playlist, playlistMetadata);
                }
            });
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            final MediaMetadata2 playlistMetadata;
            synchronized (mLock) {
                mPlaylistMetadata = MediaUtils2.convertToMediaMetadata2(title);
                playlistMetadata = mPlaylistMetadata;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaylistMetadataChanged(mInstance, playlistMetadata);
                }
            });
        }

        @Override
        public void onExtrasChanged(final Bundle extras) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCustomCommand(mInstance,
                            new SessionCommand2(SESSION_COMMAND_ON_EXTRA_CHANGED, null),
                            extras, null);
                }
            });
        }

        @Override
        public void onAudioInfoChanged(final MediaControllerCompat.PlaybackInfo info) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaybackInfoChanged(mInstance, MediaUtils2.toPlaybackInfo2(info));
                }
            });
        }

        @Override
        public void onCaptioningEnabledChanged(boolean enabled) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCustomCommand(mInstance,
                            new SessionCommand2(SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED,
                                    null), null, null);
                }
            });
        }

        @Override
        public void onRepeatModeChanged(@PlaybackStateCompat.RepeatMode final int repeatMode) {
            synchronized (mLock) {
                mRepeatMode = repeatMode;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onRepeatModeChanged(mInstance, repeatMode);
                }
            });
        }

        @Override
        public void onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode final int shuffleMode) {
            synchronized (mLock) {
                mShuffleMode = shuffleMode;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onShuffleModeChanged(mInstance, shuffleMode);
                }
            });
        }
    }
}
