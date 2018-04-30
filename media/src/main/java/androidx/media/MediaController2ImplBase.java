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

package androidx.media;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;

import static androidx.media.MediaConstants2.ARGUMENT_ALLOWED_COMMANDS;
import static androidx.media.MediaConstants2.ARGUMENT_ARGUMENTS;
import static androidx.media.MediaConstants2.ARGUMENT_BUFFERING_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_COMMAND_BUTTONS;
import static androidx.media.MediaConstants2.ARGUMENT_COMMAND_CODE;
import static androidx.media.MediaConstants2.ARGUMENT_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.ARGUMENT_ERROR_CODE;
import static androidx.media.MediaConstants2.ARGUMENT_EXTRAS;
import static androidx.media.MediaConstants2.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media.MediaConstants2.ARGUMENT_ITEM_COUNT;
import static androidx.media.MediaConstants2.ARGUMENT_MEDIA_ID;
import static androidx.media.MediaConstants2.ARGUMENT_MEDIA_ITEM;
import static androidx.media.MediaConstants2.ARGUMENT_PACKAGE_NAME;
import static androidx.media.MediaConstants2.ARGUMENT_PID;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_INFO;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_SPEED;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_STATE_COMPAT;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYER_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST_INDEX;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST_METADATA;
import static androidx.media.MediaConstants2.ARGUMENT_QUERY;
import static androidx.media.MediaConstants2.ARGUMENT_RATING;
import static androidx.media.MediaConstants2.ARGUMENT_REPEAT_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_RESULT_RECEIVER;
import static androidx.media.MediaConstants2.ARGUMENT_ROUTE_BUNDLE;
import static androidx.media.MediaConstants2.ARGUMENT_SEEK_POSITION;
import static androidx.media.MediaConstants2.ARGUMENT_SHUFFLE_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_UID;
import static androidx.media.MediaConstants2.ARGUMENT_URI;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME_DIRECTION;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME_FLAGS;
import static androidx.media.MediaConstants2.CONNECT_RESULT_CONNECTED;
import static androidx.media.MediaConstants2.CONNECT_RESULT_DISCONNECTED;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_BY_COMMAND_CODE;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_BY_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_CONNECT;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_DISCONNECT;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_BUFFERING_STATE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_CHILDREN_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ERROR;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYER_STATE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_REPEAT_MODE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ROUTES_INFO_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_SEARCH_RESULT_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_SEEK_COMPLETED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_SEND_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.SESSION_EVENT_SET_CUSTOM_LAYOUT;
import static androidx.media.MediaPlayerInterface.BUFFERING_STATE_UNKNOWN;
import static androidx.media.MediaPlayerInterface.UNKNOWN_TIME;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_RESET;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_REWIND;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SET_RATING;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.BundleCompat;
import androidx.media.MediaController2.ControllerCallback;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaController2.VolumeDirection;
import androidx.media.MediaController2.VolumeFlags;
import androidx.media.MediaPlaylistAgent.RepeatMode;
import androidx.media.MediaPlaylistAgent.ShuffleMode;
import androidx.media.MediaSession2.CommandButton;

import java.util.List;
import java.util.concurrent.Executor;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class MediaController2ImplBase implements MediaController2.SupportLibraryImpl {

    private static final String TAG = "MC2ImplBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Note: Using {@code null} doesn't helpful here because MediaBrowserServiceCompat always wraps
    //       the rootHints so it becomes non-null.
    static final Bundle sDefaultRootExtras = new Bundle();
    static {
        sDefaultRootExtras.putBoolean(MediaConstants2.ROOT_EXTRA_DEFAULT, true);
    }

    private final Context mContext;
    private final Object mLock = new Object();

    private final SessionToken2 mToken;
    private final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private final IBinder.DeathRecipient mDeathRecipient;

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    private MediaController2 mInstance;

    @GuardedBy("mLock")
    private MediaBrowserCompat mBrowserCompat;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    private List<MediaItem2> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    private @RepeatMode int mRepeatMode;
    @GuardedBy("mLock")
    private @ShuffleMode int mShuffleMode;
    @GuardedBy("mLock")
    private int mPlayerState;
    @GuardedBy("mLock")
    private MediaItem2 mCurrentMediaItem;
    @GuardedBy("mLock")
    private int mBufferingState;
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private SessionCommandGroup2 mAllowedCommands;

    // Media 1.0 variables
    @GuardedBy("mLock")
    private MediaControllerCompat mControllerCompat;
    @GuardedBy("mLock")
    private ControllerCompatCallback mControllerCompatCallback;
    @GuardedBy("mLock")
    private PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    private MediaMetadataCompat mMediaMetadataCompat;

    // Assignment should be used with the lock hold, but should be used without a lock to prevent
    // potential deadlock.
    @GuardedBy("mLock")
    private volatile boolean mConnected;

    MediaController2ImplBase(@NonNull Context context, @NonNull SessionToken2 token,
            @NonNull Executor executor, @NonNull ControllerCallback callback) {
        super();
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        mContext = context;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mToken = token;
        mCallback = callback;
        mCallbackExecutor = executor;
        mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                MediaController2ImplBase.this.close();
            }
        };

        initialize();
    }

    @Override
    public void setInstance(MediaController2 controller) {
        mInstance = controller;
    }

    @Override
    public void close() {
        if (DEBUG) {
            //Log.d(TAG, "release from " + mToken, new IllegalStateException());
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

            // Send command before the unregister callback to use mIControllerCallback in the
            // callback.
            sendCommand(CONTROLLER_COMMAND_DISCONNECT);
            if (mControllerCompat != null) {
                mControllerCompat.unregisterCallback(mControllerCompatCallback);
            }
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
            sendCommand(COMMAND_CODE_PLAYBACK_PLAY);
        }
    }

    @Override
    public void pause() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_PAUSE);
        }
    }

    @Override
    public void reset() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_RESET);
        }
    }

    @Override
    public void prepare() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_PREPARE);
        }
    }

    @Override
    public void fastForward() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_SESSION_FAST_FORWARD);
        }
    }

    @Override
    public void rewind() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_SESSION_REWIND);
        }
    }

    @Override
    public void seekTo(long pos) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putLong(ARGUMENT_SEEK_POSITION, pos);
            sendCommand(COMMAND_CODE_PLAYBACK_SEEK_TO, args);
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
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID, args);
        }
    }

    @Override
    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_QUERY, query);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH, args);
        }
    }

    @Override
    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putParcelable(ARGUMENT_URI, uri);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_URI, args);
        }
    }

    @Override
    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID, args);
        }
    }

    @Override
    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_QUERY, query);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH, args);
        }
    }

    @Override
    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putParcelable(ARGUMENT_URI, uri);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_URI, args);
        }
    }

    @Override
    public void setVolumeTo(int value, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putInt(ARGUMENT_VOLUME, value);
            args.putInt(ARGUMENT_VOLUME_FLAGS, flags);
            sendCommand(COMMAND_CODE_VOLUME_SET_VOLUME, args);
        }
    }

    @Override
    public void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putInt(ARGUMENT_VOLUME_DIRECTION, direction);
            args.putInt(ARGUMENT_VOLUME_FLAGS, flags);
            sendCommand(COMMAND_CODE_VOLUME_ADJUST_VOLUME, args);
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
            return mPlayerState;
        }
    }

    @Override
    public long getDuration() {
        synchronized (mLock) {
            if (mMediaMetadataCompat != null
                    && mMediaMetadataCompat.containsKey(METADATA_KEY_DURATION)) {
                return mMediaMetadataCompat.getLong(METADATA_KEY_DURATION);
            }
        }
        return MediaPlayerInterface.UNKNOWN_TIME;
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mPlaybackStateCompat != null) {
                long timeDiff = (mInstance.mTimeDiff != null) ? mInstance.mTimeDiff
                        : SystemClock.elapsedRealtime()
                                - mPlaybackStateCompat.getLastPositionUpdateTime();
                long expectedPosition = mPlaybackStateCompat.getPosition()
                        + (long) (mPlaybackStateCompat.getPlaybackSpeed() * timeDiff);
                return Math.max(0, expectedPosition);
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
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putFloat(ARGUMENT_PLAYBACK_SPEED, speed);
            sendCommand(COMMAND_CODE_PLAYBACK_SET_SPEED, args);
        }
    }

    @Override
    public @MediaPlayerInterface.BuffState int getBufferingState() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mBufferingState;
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
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_RATING, rating.toBundle());
            sendCommand(COMMAND_CODE_SESSION_SET_RATING, args);
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
            Bundle bundle = new Bundle();
            bundle.putBundle(ARGUMENT_CUSTOM_COMMAND, command.toBundle());
            bundle.putBundle(ARGUMENT_ARGUMENTS, args);
            sendCommand(CONTROLLER_COMMAND_BY_CUSTOM_COMMAND, bundle, cb);
        }
    }

    @Override
    public @Nullable List<MediaItem2> getPlaylist() {
        synchronized (mLock) {
            return mPlaylist;
        }
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        if (list == null) {
            throw new IllegalArgumentException("list shouldn't be null");
        }
        Bundle args = new Bundle();
        args.putParcelableArray(ARGUMENT_PLAYLIST, MediaUtils2.toMediaItem2ParcelableArray(list));
        args.putBundle(ARGUMENT_PLAYLIST_METADATA, metadata == null ? null : metadata.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SET_LIST, args);
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_PLAYLIST_METADATA, metadata == null ? null : metadata.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SET_LIST_METADATA, args);
    }

    @Override
    public @Nullable MediaMetadata2 getPlaylistMetadata() {
        synchronized (mLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_PLAYLIST_INDEX, index);
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_ADD_ITEM, args);
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_REMOVE_ITEM, args);
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_PLAYLIST_INDEX, index);
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_REPLACE_ITEM, args);
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            return mCurrentMediaItem;
        }
    }

    @Override
    public void skipToPreviousItem() {
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM);
    }

    @Override
    public void skipToNextItem() {
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM);
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM, args);
    }

    @Override
    public @RepeatMode int getRepeatMode() {
        synchronized (mLock) {
            return mRepeatMode;
        }
    }

    @Override
    public void setRepeatMode(@RepeatMode int repeatMode) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_REPEAT_MODE, repeatMode);
        sendCommand(COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE, args);
    }

    @Override
    public @ShuffleMode int getShuffleMode() {
        synchronized (mLock) {
            return mShuffleMode;
        }
    }

    @Override
    public void setShuffleMode(@ShuffleMode int shuffleMode) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_SHUFFLE_MODE, shuffleMode);
        sendCommand(COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE, args);
    }

    @Override
    public void subscribeRoutesInfo() {
        sendCommand(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
    }

    @Override
    public void unsubscribeRoutesInfo() {
        sendCommand(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
    }

    @Override
    public void selectRoute(@NonNull Bundle route) {
        if (route == null) {
            throw new IllegalArgumentException("route shouldn't be null");
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

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked(Bundle data) {
        data.setClassLoader(MediaSession2.class.getClassLoader());
        // is enough or should we pass it while connecting?
        final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(
                data.getBundle(ARGUMENT_ALLOWED_COMMANDS));
        final int playerState = data.getInt(ARGUMENT_PLAYER_STATE);
        final int bufferingState = data.getInt(ARGUMENT_BUFFERING_STATE);
        final PlaybackStateCompat playbackStateCompat = data.getParcelable(
                ARGUMENT_PLAYBACK_STATE_COMPAT);
        final int repeatMode = data.getInt(ARGUMENT_REPEAT_MODE);
        final int shuffleMode = data.getInt(ARGUMENT_SHUFFLE_MODE);
        final List<MediaItem2> playlist = MediaUtils2.fromMediaItem2ParcelableArray(
                data.getParcelableArray(ARGUMENT_PLAYLIST));
        final MediaItem2 currentMediaItem = MediaItem2.fromBundle(
                data.getBundle(ARGUMENT_MEDIA_ITEM));
        final PlaybackInfo playbackInfo =
                PlaybackInfo.fromBundle(data.getBundle(ARGUMENT_PLAYBACK_INFO));
        final MediaMetadata2 metadata = MediaMetadata2.fromBundle(
                data.getBundle(ARGUMENT_PLAYLIST_METADATA));
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked sessionCompatToken=" + mToken.getSessionCompatToken()
                    + ", allowedCommands=" + allowedCommands);
        }
        boolean close = false;
        try {
            synchronized (mLock) {
                if (mIsReleased) {
                    return;
                }
                if (mConnected) {
                    Log.e(TAG, "Cannot be notified about the connection result many times."
                            + " Probably a bug or malicious app.");
                    close = true;
                    return;
                }
                mAllowedCommands = allowedCommands;
                mPlayerState = playerState;
                mBufferingState = bufferingState;
                mPlaybackStateCompat = playbackStateCompat;
                mRepeatMode = repeatMode;
                mShuffleMode = shuffleMode;
                mPlaylist = playlist;
                mCurrentMediaItem = currentMediaItem;
                mPlaylistMetadata = metadata;
                mConnected = true;
                mPlaybackInfo = playbackInfo;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // Note: We may trigger ControllerCallbacks with the initial values
                    // But it's hard to define the order of the controller callbacks
                    // Only notify about the
                    mCallback.onConnected(mInstance, allowedCommands);
                }
            });
        } finally {
            if (close) {
                // Trick to call release() without holding the lock, to prevent potential deadlock
                // with the developer's custom lock within the ControllerCallback.onDisconnected().
                close();
            }
        }
    }

    private void initialize() {
        if (mToken.getType() == SessionToken2.TYPE_SESSION) {
            synchronized (mLock) {
                mBrowserCompat = null;
            }
            connectToSession(mToken.getSessionCompatToken());
        } else {
            connectToService();
        }
    }

    private void connectToSession(MediaSessionCompat.Token sessionCompatToken) {
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

        if (controllerCompat.isSessionReady()) {
            sendCommand(CONTROLLER_COMMAND_CONNECT, new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (!mHandlerThread.isAlive()) {
                        return;
                    }
                    switch (resultCode) {
                        case CONNECT_RESULT_CONNECTED:
                            onConnectedNotLocked(resultData);
                            break;
                        case CONNECT_RESULT_DISCONNECTED:
                            mCallbackExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    mCallback.onDisconnected(mInstance);
                                }
                            });
                            close();
                            break;
                    }
                }
            });
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

    private void sendCommand(String command, ResultReceiver receiver) {
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

    private class ConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
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
        @Override
        public void onSessionReady() {
            sendCommand(CONTROLLER_COMMAND_CONNECT, new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (!mHandlerThread.isAlive()) {
                        return;
                    }
                    switch (resultCode) {
                        case CONNECT_RESULT_CONNECTED:
                            onConnectedNotLocked(resultData);
                            break;
                        case CONNECT_RESULT_DISCONNECTED:
                            mCallbackExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    mCallback.onDisconnected(mInstance);
                                }
                            });
                            close();
                            break;
                    }
                }
            });
        }

        @Override
        public void onSessionDestroyed() {
            close();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (mLock) {
                mPlaybackStateCompat = state;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (mLock) {
                mMediaMetadataCompat = metadata;
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            if (extras != null) {
                extras.setClassLoader(MediaSession2.class.getClassLoader());
            }
            switch (event) {
                case SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED: {
                    final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(
                            extras.getBundle(ARGUMENT_ALLOWED_COMMANDS));
                    synchronized (mLock) {
                        mAllowedCommands = allowedCommands;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onAllowedCommandsChanged(mInstance, allowedCommands);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_PLAYER_STATE_CHANGED: {
                    final int playerState = extras.getInt(ARGUMENT_PLAYER_STATE);
                    PlaybackStateCompat state =
                            extras.getParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT);
                    if (state == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlayerState = playerState;
                        mPlaybackStateCompat = state;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlayerStateChanged(mInstance, playerState);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED: {
                    final MediaItem2 item = MediaItem2.fromBundle(
                            extras.getBundle(ARGUMENT_MEDIA_ITEM));
                    synchronized (mLock) {
                        mCurrentMediaItem = item;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onCurrentMediaItemChanged(mInstance, item);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_ERROR: {
                    final int errorCode = extras.getInt(ARGUMENT_ERROR_CODE);
                    final Bundle errorExtras = extras.getBundle(ARGUMENT_EXTRAS);
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onError(mInstance, errorCode, errorExtras);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_ROUTES_INFO_CHANGED: {
                    final List<Bundle> routes = MediaUtils2.toBundleList(
                            extras.getParcelableArray(ARGUMENT_ROUTE_BUNDLE));
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onRoutesInfoChanged(mInstance, routes);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_PLAYLIST_CHANGED: {
                    final MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYLIST_METADATA));
                    final List<MediaItem2> playlist = MediaUtils2.fromMediaItem2ParcelableArray(
                            extras.getParcelableArray(ARGUMENT_PLAYLIST));
                    synchronized (mLock) {
                        mPlaylist = playlist;
                        mPlaylistMetadata = playlistMetadata;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlaylistChanged(mInstance, playlist, playlistMetadata);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED: {
                    final MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYLIST_METADATA));
                    synchronized (mLock) {
                        mPlaylistMetadata = playlistMetadata;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlaylistMetadataChanged(mInstance, playlistMetadata);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_REPEAT_MODE_CHANGED: {
                    final int repeatMode = extras.getInt(ARGUMENT_REPEAT_MODE);
                    synchronized (mLock) {
                        mRepeatMode = repeatMode;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onRepeatModeChanged(mInstance, repeatMode);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED: {
                    final int shuffleMode = extras.getInt(ARGUMENT_SHUFFLE_MODE);
                    synchronized (mLock) {
                        mShuffleMode = shuffleMode;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onShuffleModeChanged(mInstance, shuffleMode);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_SEND_CUSTOM_COMMAND: {
                    Bundle commandBundle = extras.getBundle(ARGUMENT_CUSTOM_COMMAND);
                    if (commandBundle == null) {
                        return;
                    }
                    final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
                    final Bundle args = extras.getBundle(ARGUMENT_ARGUMENTS);
                    final ResultReceiver receiver = extras.getParcelable(ARGUMENT_RESULT_RECEIVER);
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onCustomCommand(mInstance, command, args, receiver);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_SET_CUSTOM_LAYOUT: {
                    final List<CommandButton> layout = MediaUtils2.fromCommandButtonParcelableArray(
                            extras.getParcelableArray(ARGUMENT_COMMAND_BUTTONS));
                    if (layout == null) {
                        return;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onCustomLayoutChanged(mInstance, layout);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED: {
                    final PlaybackInfo info = PlaybackInfo.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYBACK_INFO));
                    if (info == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlaybackInfo = info;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlaybackInfoChanged(mInstance, info);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED: {
                    final PlaybackStateCompat state =
                            extras.getParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT);
                    if (state == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlaybackStateCompat = state;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlaybackSpeedChanged(mInstance, state.getPlaybackSpeed());
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_BUFFERING_STATE_CHANGED: {
                    final MediaItem2 item = MediaItem2.fromBundle(
                            extras.getBundle(ARGUMENT_MEDIA_ITEM));
                    final int bufferingState = extras.getInt(ARGUMENT_BUFFERING_STATE);
                    PlaybackStateCompat state =
                            extras.getParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT);
                    if (item == null || state == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mBufferingState = bufferingState;
                        mPlaybackStateCompat = state;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onBufferingStateChanged(mInstance, item, bufferingState);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_SEEK_COMPLETED: {
                    final long position = extras.getLong(ARGUMENT_SEEK_POSITION);
                    PlaybackStateCompat state =
                            extras.getParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT);
                    if (state == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlaybackStateCompat = state;
                    }
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onSeekCompleted(mInstance, position);
                        }
                    });
                    break;
                }
                case SESSION_EVENT_ON_CHILDREN_CHANGED: {
                    String parentId = extras.getString(ARGUMENT_MEDIA_ID);
                    if (parentId == null || !(mInstance instanceof MediaBrowser2)) {
                        return;
                    }
                    int itemCount = extras.getInt(ARGUMENT_ITEM_COUNT, -1);
                    Bundle childrenExtras = extras.getBundle(ARGUMENT_EXTRAS);
                    ((MediaBrowser2.BrowserCallback) mCallback).onChildrenChanged(
                            (MediaBrowser2) mInstance, parentId, itemCount, childrenExtras);
                    break;
                }
                case SESSION_EVENT_ON_SEARCH_RESULT_CHANGED: {
                    final String query = extras.getString(ARGUMENT_QUERY);
                    if (query == null || !(mInstance instanceof MediaBrowser2)) {
                        return;
                    }
                    final int itemCount = extras.getInt(ARGUMENT_ITEM_COUNT, -1);
                    final Bundle searchExtras = extras.getBundle(ARGUMENT_EXTRAS);
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ((MediaBrowser2.BrowserCallback) mCallback).onSearchResultChanged(
                                    (MediaBrowser2) mInstance, query, itemCount, searchExtras);
                        }
                    });
                    break;
                }
            }
        }
    }
}
