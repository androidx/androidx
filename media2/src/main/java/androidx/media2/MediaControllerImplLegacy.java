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

import static androidx.media2.BaseResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaConstants.ARGUMENT_COMMAND_CODE;
import static androidx.media2.MediaConstants.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media2.MediaConstants.ARGUMENT_PACKAGE_NAME;
import static androidx.media2.MediaConstants.ARGUMENT_PID;
import static androidx.media2.MediaConstants.ARGUMENT_UID;
import static androidx.media2.MediaConstants.CONTROLLER_COMMAND_BY_COMMAND_CODE;
import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_DISCONNECTED;
import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_NOT_SUPPORTED;
import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_SUCCESS;
import static androidx.media2.SessionPlayer.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.SessionPlayer.PLAYER_STATE_IDLE;
import static androidx.media2.SessionPlayer.UNKNOWN_TIME;

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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.app.BundleCompat;
import androidx.core.util.ObjectsCompat;
import androidx.media2.MediaController.ControllerCallback;
import androidx.media2.MediaController.ControllerResult;
import androidx.media2.MediaController.PlaybackInfo;
import androidx.media2.MediaController.VolumeDirection;
import androidx.media2.MediaController.VolumeFlags;
import androidx.media2.MediaSession.CommandButton;
import androidx.media2.SessionCommand.CommandCode;
import androidx.media2.SessionPlayer.BuffState;
import androidx.media2.SessionPlayer.RepeatMode;
import androidx.media2.SessionPlayer.ShuffleMode;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

// TODO: Find better way to return listenable future.
class MediaControllerImplLegacy implements MediaController.MediaControllerImpl {

    private static final String TAG = "MC2ImplLegacy";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final long POSITION_DIFF_TOLERANCE = 100;
    static final String SESSION_COMMAND_ON_EXTRAS_CHANGED =
            "android.media.session.command.ON_EXTRAS_CHANGED";
    static final String SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED =
            "android.media.session.command.ON_CAPTIONING_ENALBED_CHANGED";

    // Note: Using {@code null} doesn't helpful here because MediaBrowserServiceCompat always wraps
    //       the rootHints so it becomes non-null.
    static final Bundle sDefaultRootExtras = new Bundle();
    static {
        sDefaultRootExtras.putBoolean(MediaConstants.ROOT_EXTRA_DEFAULT, true);
    }

    final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionToken mToken;
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
            MediaController mInstance;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaBrowserCompat mBrowserCompat;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<MediaItem> mPlaylist;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<QueueItem> mQueue;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaMetadata mPlaylistMetadata;
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
            MediaItem mCurrentMediaItem;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mBufferingState;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mCurrentMediaItemIndex;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mSkipToPlaylistIndex = -1;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mBufferedPosition;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SessionCommandGroup mAllowedCommands;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<CommandButton> mCustomLayout;

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

    MediaControllerImplLegacy(@NonNull Context context, @NonNull MediaController instance,
            @NonNull SessionToken token, @NonNull Executor executor,
            @NonNull ControllerCallback callback) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaController_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mToken = token;
        mCallback = callback;
        mCallbackExecutor = executor;

        if (mToken.getType() == SessionToken.TYPE_SESSION) {
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
    public @Nullable SessionToken getConnectedSessionToken() {
        synchronized (mLock) {
            return mConnected ? mToken : null;
        }
    }

    private ListenableFuture<ControllerResult> createFutureWithResult(int resultCode) {
        final MediaItem item;
        synchronized (mLock) {
            item = mCurrentMediaItem;
        }
        ResolvableFuture<ControllerResult> result = ResolvableFuture.create();
        result.set(new ControllerResult(resultCode, null, item));
        return result;
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mConnected;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> play() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().play();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> pause() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().pause();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> prepare() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().prepare();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> fastForward() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().fastForward();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> rewind() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().rewind();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> skipForward() {
        // Unsupported action
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<ControllerResult> skipBackward() {
        // Unsupported action
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<ControllerResult> seekTo(long pos) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().seekTo(pos);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> playFromMediaId(@NonNull String mediaId,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().playFromMediaId(mediaId, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> playFromSearch(@NonNull String query,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().playFromSearch(query, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> playFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().playFromUri(uri, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromMediaId(@NonNull String mediaId,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().prepareFromMediaId(mediaId, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromSearch(@NonNull String query,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().prepareFromSearch(query, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> prepareFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().prepareFromUri(uri, extras);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> setVolumeTo(int value, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.setVolumeTo(value, flags);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> adjustVolume(@VolumeDirection int direction,
            @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.adjustVolume(direction, flags);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
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
                return SessionPlayer.PLAYER_STATE_ERROR;
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
    public ListenableFuture<ControllerResult> setPlaybackSpeed(float speed) {
        // Unsupported action
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public @BuffState int getBufferingState() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mPlaybackStateCompat == null ? SessionPlayer.BUFFERING_STATE_UNKNOWN
                    : MediaUtils.toBufferingState(mPlaybackStateCompat.getState());
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
    public ListenableFuture<ControllerResult> setRating(@NonNull String mediaId,
            @NonNull Rating rating) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            if (mCurrentMediaItem != null && mediaId.equals(mCurrentMediaItem.getMediaId())) {
                mControllerCompat.getTransportControls().setRating(
                        MediaUtils.convertToRatingCompat(rating));
            }
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> sendCustomCommand(@NonNull SessionCommand command,
            @Nullable Bundle args) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            if (mAllowedCommands.hasCommand(command)) {
                mControllerCompat.getTransportControls().sendCustomAction(
                        command.getCustomCommand(), args);
                return createFutureWithResult(RESULT_CODE_SUCCESS);
            }
            final ResolvableFuture<ControllerResult> result = ResolvableFuture.create();
            ResultReceiver cb = new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    result.set(new ControllerResult(resultCode, resultData));
                }
            };
            mControllerCompat.sendCommand(command.getCustomCommand(), args, cb);
            return result;
        }
    }

    @Override
    public @Nullable List<MediaItem> getPlaylist() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return (mPlaylist == null || mPlaylist.size() == 0) ? null : mPlaylist;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setPlaylist(@NonNull List<String> list,
            @Nullable MediaMetadata metadata) {
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<ControllerResult> setMediaItem(String mediaId) {
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<ControllerResult> updatePlaylistMetadata(
            @Nullable MediaMetadata metadata) {
        return createFutureWithResult(RESULT_CODE_NOT_SUPPORTED);
    }

    @Override
    public @Nullable MediaMetadata getPlaylistMetadata() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaylistMetadata;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> addPlaylistItem(int index, @NonNull String mediaId) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.addQueueItem(
                    MediaUtils.createMediaDescriptionCompat(mediaId), index);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> removePlaylistItem(@NonNull int index) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            if (mQueue == null || index < 0 || index >= mQueue.size()) {
                return createFutureWithResult(RESULT_CODE_BAD_VALUE);
            }
            mControllerCompat.removeQueueItem(mQueue.get(index).getDescription());
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> replacePlaylistItem(int index,
            @NonNull String mediaId) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            if (mPlaylist == null || index < 0 || mPlaylist.size() <= index) {
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            removePlaylistItem(index);
            addPlaylistItem(index, mediaId);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mCurrentMediaItem;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> skipToPreviousItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().skipToPrevious();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> skipToNextItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().skipToNext();
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public ListenableFuture<ControllerResult> skipToPlaylistItem(@NonNull int index) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            mSkipToPlaylistIndex = index;
            mControllerCompat.getTransportControls().skipToQueueItem(
                    mQueue.get(index).getQueueId());
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public @RepeatMode int getRepeatMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return SessionPlayer.REPEAT_MODE_NONE;
            }
            return mRepeatMode;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setRepeatMode(@RepeatMode int repeatMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            // SessionPlayer.RepeatMode has the same values with
            // PlaybackStateCompat.RepeatMode.
            mControllerCompat.getTransportControls().setRepeatMode(repeatMode);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
    }

    @Override
    public @ShuffleMode int getShuffleMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return SessionPlayer.SHUFFLE_MODE_NONE;
            }
            return mShuffleMode;
        }
    }

    @Override
    public ListenableFuture<ControllerResult> setShuffleMode(@ShuffleMode int shuffleMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_CODE_DISCONNECTED);
            }
            // SessionPlayer.ShuffleMode has the same values with
            // PlaybackStateCompat.ShuffleMode.
            mControllerCompat.getTransportControls().setShuffleMode(shuffleMode);
        }
        return createFutureWithResult(RESULT_CODE_SUCCESS);
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
    public @NonNull MediaController getInstance() {
        return mInstance;
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked() {
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked token=" + mToken);
        }
        final SessionCommandGroup allowedCommands;
        final List<CommandButton> customLayout;

        synchronized (mLock) {
            if (mIsReleased || mConnected) {
                return;
            }
            mPlaybackStateCompat = mControllerCompat.getPlaybackState();
            mAllowedCommands = MediaUtils.convertToSessionCommandGroup(
                    mControllerCompat.getFlags(), mPlaybackStateCompat);
            mPlayerState = MediaUtils.convertToPlayerState(mPlaybackStateCompat);
            mBufferedPosition = mPlaybackStateCompat == null
                    ? UNKNOWN_TIME : mPlaybackStateCompat.getBufferedPosition();
            mCustomLayout = MediaUtils.convertToCustomLayout(mPlaybackStateCompat);

            allowedCommands = mAllowedCommands;
            customLayout = mCustomLayout;

            mPlaybackInfo = MediaUtils.toPlaybackInfo2(mControllerCompat.getPlaybackInfo());

            mRepeatMode = mControllerCompat.getRepeatMode();
            mShuffleMode = mControllerCompat.getShuffleMode();

            mQueue = MediaUtils.removeNullElements(mControllerCompat.getQueue());
            if (mQueue == null || mQueue.size() == 0) {
                // MediaSessionCompat can set queue as null or empty. However, SessionPlayer should
                // not set playlist as null or empty. Therefore, we treat them as the same.
                mQueue = null;
                mPlaylist = null;
            } else {
                mPlaylist = MediaUtils.convertQueueItemListToMediaItemList(mQueue);
            }
            mPlaylistMetadata = MediaUtils.convertToMediaMetadata(
                    mControllerCompat.getQueueTitle());

            // Call this after set playlist.
            setCurrentMediaItemLocked(mControllerCompat.getMetadata());
            mConnected = true;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onConnected(mInstance, allowedCommands);
            }
        });
        if (!customLayout.isEmpty()) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onSetCustomLayout(mInstance, customLayout);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void connectToSession(MediaSessionCompat.Token sessionCompatToken) {
        MediaControllerCompat controllerCompat = null;
        try {
            controllerCompat = new MediaControllerCompat(mContext, sessionCompatToken);
        } catch (RemoteException e) {
            // TODO: Handle connection error
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

    private void sendCommand(@CommandCode int commandCode) {
        sendCommand(commandCode, null);
    }

    private void sendCommand(@CommandCode int commandCode, Bundle args) {
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

        if (mQueue == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata);
            return;
        }

        if (mPlaybackStateCompat != null) {
            // If playback state is updated before, compare use queue id and media id.
            long queueId = mPlaybackStateCompat.getActiveQueueItemId();
            for (int i = 0; i < mQueue.size(); ++i) {
                QueueItem item = mQueue.get(i);
                if (item.getQueueId() == queueId) {
                    mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata);
                    mCurrentMediaItemIndex = i;
                    return;
                }
            }
        }

        String mediaId = metadata.getString(METADATA_KEY_MEDIA_ID);
        if (mediaId == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata);
            return;
        }

        // Need to find the media item in the playlist using mediaId.
        // Note that there can be multiple media items with the same media id.
        if (mSkipToPlaylistIndex >= 0 && mSkipToPlaylistIndex < mQueue.size()
                && TextUtils.equals(mediaId,
                        mQueue.get(mSkipToPlaylistIndex).getDescription().getMediaId())) {
            // metadata changed after skipToPlaylistIItem() was called.
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata);
            mCurrentMediaItemIndex = mSkipToPlaylistIndex;
            mSkipToPlaylistIndex = -1;
            return;
        }

        // Find mediaId from the playlist.
        for (int i = 0; i < mQueue.size(); ++i) {
            QueueItem item = mQueue.get(i);
            if (TextUtils.equals(mediaId, item.getDescription().getMediaId())) {
                mCurrentMediaItemIndex = i;
                mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata);
                return;
            }
        }

        // Failed to find media item from the playlist.
        mCurrentMediaItemIndex = -1;
        mCurrentMediaItem = MediaUtils.convertToMediaItem(mMediaMetadataCompat);
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
                    // Ignore return because legacy session cannot get result back.
                    mCallback.onCustomCommand(mInstance, new SessionCommand(event, null), extras);
                }
            });
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            final PlaybackStateCompat prevState;
            final MediaItem prevItem;
            final MediaItem currentItem;
            final List<CommandButton> prevLayout;
            final List<CommandButton> currentLayout;
            final SessionCommandGroup prevAllowedCommands;
            final SessionCommandGroup currentAllowedCommands;
            synchronized (mLock) {
                prevItem = mCurrentMediaItem;
                prevState = mPlaybackStateCompat;
                mPlaybackStateCompat = state;
                mPlayerState = MediaUtils.convertToPlayerState(state);
                mBufferedPosition = state == null ? UNKNOWN_TIME
                        : state.getBufferedPosition();

                if (mQueue != null && state != null) {
                    for (int i = 0; i < mQueue.size(); ++i) {
                        if (mQueue.get(i).getQueueId() == state.getActiveQueueItemId()) {
                            mCurrentMediaItemIndex = i;
                            mCurrentMediaItem = mPlaylist.get(i);
                        }
                    }
                }
                currentItem = mCurrentMediaItem;

                prevLayout = mCustomLayout;
                mCustomLayout = MediaUtils.convertToCustomLayout(state);
                currentLayout = mCustomLayout;

                prevAllowedCommands = mAllowedCommands;
                mAllowedCommands = MediaUtils.convertToSessionCommandGroup(
                        mControllerCompat.getFlags(), mPlaybackStateCompat);
                currentAllowedCommands = mAllowedCommands;
            }

            if (prevItem != currentItem) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCurrentMediaItemChanged(mInstance, currentItem);
                    }
                });
            }

            if (state == null) {
                if (prevState != null) {
                    mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPlayerStateChanged(mInstance, PLAYER_STATE_IDLE);
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
                                mInstance, MediaUtils.convertToPlayerState(state));
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

            Set<SessionCommand> prevCommands = prevAllowedCommands.getCommands();
            Set<SessionCommand> currentCommands = currentAllowedCommands.getCommands();
            if (prevCommands.size() != currentCommands.size()
                    || !prevCommands.containsAll(currentCommands)) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onAllowedCommandsChanged(mInstance, currentAllowedCommands);
                    }
                });
            }
            boolean layoutChanged;
            if (prevLayout.size() == currentLayout.size()) {
                layoutChanged = false;
                for (int i = 0; i < currentLayout.size(); i++) {
                    if (!ObjectsCompat.equals(prevLayout.get(i).getCommand(),
                            currentLayout.get(i).getCommand())) {
                        layoutChanged = true;
                        break;
                    }
                }
            } else {
                layoutChanged = true;
            }
            if (layoutChanged) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSetCustomLayout(mInstance, currentLayout);
                    }
                });
            }

            if (currentItem == null) {
                return;
            }
            // Update buffering state if needed
            final int bufferingState = MediaUtils.toBufferingState(state.getState());
            final int prevBufferingState = prevState == null
                    ? SessionPlayer.BUFFERING_STATE_UNKNOWN
                    : MediaUtils.toBufferingState(prevState.getState());
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
            final MediaItem prevItem;
            final MediaItem currentItem;
            synchronized (mLock) {
                prevItem = mCurrentMediaItem;
                setCurrentMediaItemLocked(metadata);
                currentItem = mCurrentMediaItem;
            }
            if (prevItem != currentItem) {
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCurrentMediaItemChanged(mInstance, currentItem);
                    }
                });
            }
        }

        @Override
        public void onQueueChanged(List<QueueItem> queue) {
            final List<MediaItem> playlist;
            final MediaMetadata playlistMetadata;
            synchronized (mLock) {
                mQueue = MediaUtils.removeNullElements(queue);
                if (mQueue == null || mQueue.size() == 0) {
                    // MediaSessionCompat can set queue as null or empty. However, SessionPlayer
                    // should not set playlist as null or empty. Therefore, we treat them as the
                    // same.
                    mQueue = null;
                    mPlaylist = null;
                } else {
                    mPlaylist = MediaUtils.convertQueueItemListToMediaItemList(mQueue);
                }
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
            final MediaMetadata playlistMetadata;
            synchronized (mLock) {
                mPlaylistMetadata = MediaUtils.convertToMediaMetadata(title);
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
                    mCallback.onCustomCommand(mInstance, new SessionCommand(
                            SESSION_COMMAND_ON_EXTRAS_CHANGED, null), extras);
                }
            });
        }

        @Override
        public void onAudioInfoChanged(final MediaControllerCompat.PlaybackInfo info) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPlaybackInfoChanged(mInstance, MediaUtils.toPlaybackInfo2(info));
                }
            });
        }

        @Override
        public void onCaptioningEnabledChanged(final boolean enabled) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCustomCommand(mInstance, new SessionCommand(
                            SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED, null), null);
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
