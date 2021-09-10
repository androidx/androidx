/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;

import static androidx.media2.common.BaseResult.RESULT_ERROR_BAD_VALUE;
import static androidx.media2.common.BaseResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.common.BaseResult.RESULT_INFO_SKIPPED;
import static androidx.media2.common.SessionPlayer.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE;
import static androidx.media2.common.SessionPlayer.UNKNOWN_TIME;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_ID;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_QUERY;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_URI;
import static androidx.media2.session.MediaConstants.MEDIA_URI_SET_MEDIA_URI_PREFIX;
import static androidx.media2.session.SessionResult.RESULT_ERROR_NOT_SUPPORTED;
import static androidx.media2.session.SessionResult.RESULT_ERROR_SESSION_DISCONNECTED;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.media2.common.ClassVerificationHelper;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.BuffState;
import androidx.media2.common.SessionPlayer.RepeatMode;
import androidx.media2.common.SessionPlayer.ShuffleMode;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaController.ControllerCallbackRunnable;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaController.VolumeDirection;
import androidx.media2.session.MediaController.VolumeFlags;
import androidx.media2.session.MediaSession.CommandButton;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Find better way to return listenable future.
class MediaControllerImplLegacy implements MediaController.MediaControllerImpl {
    private static final String TAG = "MC2ImplLegacy";
    private static final int ITEM_NONE = -1;
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final long POSITION_DIFF_TOLERANCE = 100;
    static final String SESSION_COMMAND_ON_EXTRAS_CHANGED =
            "android.media.session.command.ON_EXTRAS_CHANGED";
    static final String SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED =
            "android.media.session.command.ON_CAPTIONING_ENALBED_CHANGED";

    final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionToken mToken;

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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mClosed;
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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaControllerCompat mControllerCompat;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ControllerCompatCallback mControllerCompatCallback;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaMetadataCompat mMediaMetadataCompat;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mConnected;

    @GuardedBy("mLock")
    private SetMediaUriRequest mPendingSetMediaUriRequest;

    MediaControllerImplLegacy(@NonNull Context context, @NonNull MediaController instance,
            @NonNull SessionToken token) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaController_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mToken = token;

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
            Log.d(TAG, "close from " + mToken);
        }
        synchronized (mLock) {
            if (mClosed) {
                // Prevent re-enterance from the ControllerCallback.onDisconnected()
                return;
            }
            mHandler.removeCallbacksAndMessages(null);

            if (Build.VERSION.SDK_INT >= 18) {
                ClassVerificationHelper.HandlerThread.Api18.quitSafely(mHandlerThread);
            } else {
                mHandlerThread.quit();
            }

            mClosed = true;

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
        mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                callback.onDisconnected(mInstance);
            }
        });
    }

    @Override
    @Nullable
    public SessionToken getConnectedToken() {
        synchronized (mLock) {
            return mConnected ? mToken : null;
        }
    }

    private ListenableFuture<SessionResult> createFutureWithResult(int resultCode) {
        ResolvableFuture<SessionResult> result = ResolvableFuture.create();
        setFutureResult(result, resultCode);
        return result;
    }

    private void setFutureResult(ResolvableFuture<SessionResult> result, int resultCode) {
        final MediaItem item;
        synchronized (mLock) {
            item = mCurrentMediaItem;
        }
        result.set(new SessionResult(resultCode, null, item));
    }

    @Override
    public boolean isConnected() {
        synchronized (mLock) {
            return mConnected;
        }
    }

    @Override
    public ListenableFuture<SessionResult> play() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mPendingSetMediaUriRequest == null) {
                mControllerCompat.getTransportControls().play();
            } else {
                switch (mPendingSetMediaUriRequest.type) {
                    case MEDIA_URI_QUERY_ID:
                        mControllerCompat.getTransportControls()
                                .playFromMediaId(
                                        mPendingSetMediaUriRequest.value,
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    case MEDIA_URI_QUERY_QUERY:
                        mControllerCompat.getTransportControls()
                                .playFromSearch(
                                        mPendingSetMediaUriRequest.value,
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    case MEDIA_URI_QUERY_URI:
                        mControllerCompat.getTransportControls()
                                .playFromUri(
                                        Uri.parse(mPendingSetMediaUriRequest.value),
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    default:
                        // Impossible.
                        mPendingSetMediaUriRequest = null;
                        return createFutureWithResult(RESULT_ERROR_INVALID_STATE);
                }
                setFutureResult(mPendingSetMediaUriRequest.result, RESULT_SUCCESS);
                mPendingSetMediaUriRequest = null;
            }
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> pause() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().pause();
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> prepare() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mPendingSetMediaUriRequest == null) {
                mControllerCompat.getTransportControls().prepare();
            } else {
                switch (mPendingSetMediaUriRequest.type) {
                    case MEDIA_URI_QUERY_ID:
                        mControllerCompat.getTransportControls()
                                .prepareFromMediaId(
                                        mPendingSetMediaUriRequest.value,
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    case MEDIA_URI_QUERY_QUERY:
                        mControllerCompat.getTransportControls()
                                .prepareFromSearch(
                                        mPendingSetMediaUriRequest.value,
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    case MEDIA_URI_QUERY_URI:
                        mControllerCompat.getTransportControls()
                                .prepareFromUri(
                                        Uri.parse(mPendingSetMediaUriRequest.value),
                                        mPendingSetMediaUriRequest.extras);
                        break;
                    default:
                        // Impossible.
                        mPendingSetMediaUriRequest = null;
                        return createFutureWithResult(RESULT_ERROR_INVALID_STATE);
                }
                setFutureResult(mPendingSetMediaUriRequest.result, RESULT_SUCCESS);
                mPendingSetMediaUriRequest = null;
            }
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> fastForward() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().fastForward();
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> rewind() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().rewind();
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> skipForward() {
        // Unsupported action
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<SessionResult> skipBackward() {
        // Unsupported action
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<SessionResult> seekTo(long pos) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().seekTo(pos);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> setVolumeTo(int value, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.setVolumeTo(value, flags);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> adjustVolume(@VolumeDirection int direction,
            @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.adjustVolume(direction, flags);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    @Nullable
    public PendingIntent getSessionActivity() {
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
    @BuffState
    public int getBufferingState() {
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
    @Nullable
    public PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaybackInfo;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setRating(@NonNull String mediaId,
            @NonNull Rating rating) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mCurrentMediaItem != null && mediaId.equals(mCurrentMediaItem.getMediaId())) {
                mControllerCompat.getTransportControls().setRating(
                        MediaUtils.convertToRatingCompat(rating));
            }
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> setPlaybackSpeed(float speed) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().setPlaybackSpeed(speed);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> sendCustomCommand(@NonNull SessionCommand command,
            @Nullable Bundle args) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mAllowedCommands.hasCommand(command)) {
                mControllerCompat.getTransportControls().sendCustomAction(
                        command.getCustomAction(), args);
                return createFutureWithResult(RESULT_SUCCESS);
            }
            final ResolvableFuture<SessionResult> result = ResolvableFuture.create();
            ResultReceiver cb = new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    result.set(new SessionResult(resultCode, resultData));
                }
            };
            mControllerCompat.sendCommand(command.getCustomAction(), args, cb);
            return result;
        }
    }

    @Override
    @Nullable
    public List<MediaItem> getPlaylist() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return (mPlaylist == null || mPlaylist.size() == 0) ? null : new ArrayList<>(mPlaylist);
        }
    }

    @Override
    public ListenableFuture<SessionResult> setPlaylist(@NonNull List<String> list,
            @Nullable MediaMetadata metadata) {
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<SessionResult> setMediaItem(@NonNull String mediaId) {
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    public ListenableFuture<SessionResult> setMediaUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }

            if (mPendingSetMediaUriRequest != null) {
                Log.w(TAG, "SetMediaUri() is called multiple times without prepare() nor play()."
                        + " Previous call will be skipped.");
                setFutureResult(mPendingSetMediaUriRequest.result, RESULT_INFO_SKIPPED);
                mPendingSetMediaUriRequest = null;
            }
            ResolvableFuture<SessionResult> result = ResolvableFuture.create();
            if (uri.toString().startsWith(MEDIA_URI_SET_MEDIA_URI_PREFIX)
                    && uri.getQueryParameterNames().size() == 1) {
                String queryParameterName = uri.getQueryParameterNames().iterator().next();
                if (TextUtils.equals(queryParameterName, MEDIA_URI_QUERY_ID)
                        || TextUtils.equals(queryParameterName, MEDIA_URI_QUERY_QUERY)
                        || TextUtils.equals(queryParameterName, MEDIA_URI_QUERY_URI)) {
                    mPendingSetMediaUriRequest =
                            new SetMediaUriRequest(
                                    queryParameterName,
                                    uri.getQueryParameter(queryParameterName),
                                    extras,
                                    result);
                }
            }
            if (mPendingSetMediaUriRequest == null) {
                mPendingSetMediaUriRequest =
                        new SetMediaUriRequest(MEDIA_URI_QUERY_URI, uri.toString(), extras, result);
            }
            return result;
        }
    }

    @Override
    public ListenableFuture<SessionResult> updatePlaylistMetadata(
            @Nullable MediaMetadata metadata) {
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    @Nullable
    public MediaMetadata getPlaylistMetadata() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mPlaylistMetadata;
        }
    }

    @Override
    public ListenableFuture<SessionResult> addPlaylistItem(int index, @NonNull String mediaId) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.addQueueItem(
                    MediaUtils.createMediaDescriptionCompat(mediaId), index);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> removePlaylistItem(int index) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mQueue == null || index < 0 || index >= mQueue.size()) {
                return createFutureWithResult(RESULT_ERROR_BAD_VALUE);
            }
            mControllerCompat.removeQueueItem(mQueue.get(index).getDescription());
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> replacePlaylistItem(int index, @NonNull String mediaId) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            if (mQueue == null || index < 0 || index >= mQueue.size()) {
                return createFutureWithResult(RESULT_ERROR_BAD_VALUE);
            }
            mControllerCompat.removeQueueItem(mQueue.get(index).getDescription());
            mControllerCompat.addQueueItem(MediaUtils.createMediaDescriptionCompat(mediaId), index);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> movePlaylistItem(int fromIndex, int toIndex) {
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
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
    public int getCurrentMediaItemIndex() {
        return mCurrentMediaItemIndex;
    }

    @Override
    public int getPreviousMediaItemIndex() {
        return ITEM_NONE;
    }

    @Override
    public int getNextMediaItemIndex() {
        return ITEM_NONE;
    }

    @Override
    public ListenableFuture<SessionResult> skipToPreviousItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().skipToPrevious();
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> skipToNextItem() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mControllerCompat.getTransportControls().skipToNext();
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    public ListenableFuture<SessionResult> skipToPlaylistItem(int index) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            mSkipToPlaylistIndex = index;
            mControllerCompat.getTransportControls().skipToQueueItem(
                    mQueue.get(index).getQueueId());
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    @RepeatMode
    public int getRepeatMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return SessionPlayer.REPEAT_MODE_NONE;
            }
            return mRepeatMode;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setRepeatMode(@RepeatMode int repeatMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            // SessionPlayer.RepeatMode has the same values with
            // PlaybackStateCompat.RepeatMode.
            mControllerCompat.getTransportControls().setRepeatMode(repeatMode);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    @ShuffleMode
    public int getShuffleMode() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return SessionPlayer.SHUFFLE_MODE_NONE;
            }
            return mShuffleMode;
        }
    }

    @Override
    public ListenableFuture<SessionResult> setShuffleMode(@ShuffleMode int shuffleMode) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return createFutureWithResult(RESULT_ERROR_SESSION_DISCONNECTED);
            }
            // SessionPlayer.ShuffleMode has the same values with
            // PlaybackStateCompat.ShuffleMode.
            mControllerCompat.getTransportControls().setShuffleMode(shuffleMode);
        }
        return createFutureWithResult(RESULT_SUCCESS);
    }

    @Override
    @NonNull
    public VideoSize getVideoSize() {
        Log.w(TAG, "Session doesn't support getting VideoSize");
        return new VideoSize(0, 0);
    }

    @Override
    public ListenableFuture<SessionResult> setSurface(@Nullable Surface surface) {
        Log.w(TAG, "Session doesn't support setting Surface");
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    @NonNull
    public List<TrackInfo> getTracks() {
        Log.w(TAG, "Session doesn't support getting TrackInfo");
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public ListenableFuture<SessionResult> selectTrack(@NonNull TrackInfo trackInfo) {
        Log.w(TAG, "Session doesn't support selecting track");
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    @NonNull
    public ListenableFuture<SessionResult> deselectTrack(
            @NonNull TrackInfo trackInfo) {
        Log.w(TAG, "Session doesn't support deselecting track");
        return createFutureWithResult(RESULT_ERROR_NOT_SUPPORTED);
    }

    @Override
    @Nullable
    public TrackInfo getSelectedTrack(int trackType) {
        Log.w(TAG, "Session doesn't support getting selected track");
        return null;
    }

    @Override
    public SessionCommandGroup getAllowedCommands() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mAllowedCommands;
        }
    }

    @Override
    @NonNull
    public Context getContext() {
        return mContext;
    }

    @Override
    @Nullable
    public MediaBrowserCompat getBrowserCompat() {
        synchronized (mLock) {
            return mBrowserCompat;
        }
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked() {
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked token=" + mToken);
        }
        final SessionCommandGroup allowedCommands;
        final List<CommandButton> customLayout;

        synchronized (mLock) {
            if (mClosed || mConnected) {
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
        mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                callback.onConnected(mInstance, allowedCommands);
            }
        });
        if (!customLayout.isEmpty()) {
            mInstance.notifyPrimaryControllerCallback(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onSetCustomLayout(mInstance, customLayout);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void connectToSession(MediaSessionCompat.Token sessionCompatToken) {
        boolean isSessionReady;
        MediaControllerCompat controllerCompat = new MediaControllerCompat(mContext,
                sessionCompatToken);
        synchronized (mLock) {
            mControllerCompat = controllerCompat;
            mControllerCompatCallback = new ControllerCompatCallback();
            isSessionReady = mControllerCompat.isSessionReady();
            mControllerCompat.registerCallback(mControllerCompatCallback, mHandler);
        }
        if (!isSessionReady) {
            // If the session not ready here, then call onConnectedNotLocked() immediately. The
            // session would be higly likely framework MediaSession, so wouldn't be ready forever.
            // Just FYI, previous attempt to make session ready (i.e. request extra binder) had been
            // failed already in MediaController's constructor via SessionToken#createSessionToken.
            onConnectedNotLocked();
        }
    }

    private void connectToService() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    mBrowserCompat = new MediaBrowserCompat(mContext, mToken.getComponentName(),
                            new ConnectionCallback(), null);
                    mBrowserCompat.connect();
                }
            }
        });
    }

    @SuppressWarnings({"GuardedBy", "WeakerAccess"}) /* WeakerAccess for synthetic access */
    void setCurrentMediaItemLocked(MediaMetadataCompat metadata) {
        mMediaMetadataCompat = metadata;
        int ratingType = mControllerCompat.getRatingType();
        if (metadata == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = null;
            return;
        }

        if (mQueue == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata, ratingType);
            return;
        }

        if (mPlaybackStateCompat != null) {
            // If playback state is updated before, compare use queue id and media id.
            long queueId = mPlaybackStateCompat.getActiveQueueItemId();
            for (int i = 0; i < mQueue.size(); ++i) {
                QueueItem item = mQueue.get(i);
                if (item.getQueueId() == queueId) {
                    mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata, ratingType);
                    mCurrentMediaItemIndex = i;
                    return;
                }
            }
        }

        String mediaId = metadata.getString(METADATA_KEY_MEDIA_ID);
        if (mediaId == null) {
            mCurrentMediaItemIndex = -1;
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata, ratingType);
            return;
        }

        // Need to find the media item in the playlist using mediaId.
        // Note that there can be multiple media items with the same media id.
        if (mSkipToPlaylistIndex >= 0 && mSkipToPlaylistIndex < mQueue.size()
                && TextUtils.equals(mediaId,
                        mQueue.get(mSkipToPlaylistIndex).getDescription().getMediaId())) {
            // metadata changed after skipToPlaylistIItem() was called.
            mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata, ratingType);
            mCurrentMediaItemIndex = mSkipToPlaylistIndex;
            mSkipToPlaylistIndex = -1;
            return;
        }

        // Find mediaId from the playlist.
        for (int i = 0; i < mQueue.size(); ++i) {
            QueueItem item = mQueue.get(i);
            if (TextUtils.equals(mediaId, item.getDescription().getMediaId())) {
                mCurrentMediaItemIndex = i;
                mCurrentMediaItem = MediaUtils.convertToMediaItem(metadata, ratingType);
                return;
            }
        }

        // Failed to find media item from the playlist.
        mCurrentMediaItemIndex = -1;
        mCurrentMediaItem = MediaUtils.convertToMediaItem(mMediaMetadataCompat, ratingType);
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
            boolean connected;
            synchronized (mLock) {
                connected = mConnected;
            }
            if (!connected) {
                onConnectedNotLocked();
            } else {
                // Handle rare occasion that extra binder becomes available lately.
                // See connectToSession() for detail.
                PlaybackStateCompat state;
                int shuffleMode;
                int repeatMode;
                boolean isCaptioningEnabled;
                synchronized (mLock) {
                    state = mControllerCompat.getPlaybackState();
                    shuffleMode = mControllerCompat.getShuffleMode();
                    repeatMode = mControllerCompat.getRepeatMode();
                    isCaptioningEnabled = mControllerCompat.isCaptioningEnabled();
                }
                onPlaybackStateChanged(state);
                onShuffleModeChanged(shuffleMode);
                onRepeatModeChanged(repeatMode);
                onCaptioningEnabledChanged(isCaptioningEnabled);
            }
        }

        @Override
        public void onSessionDestroyed() {
            close();
        }

        @Override
        public void onSessionEvent(final String event, final Bundle extras) {
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
            }
            mInstance.notifyPrimaryControllerCallback(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    // Ignore return because legacy session cannot get result back.
                    callback.onCustomCommand(mInstance, new SessionCommand(event, null), extras);
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
                if (mClosed || !mConnected) {
                    return;
                }
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
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onCurrentMediaItemChanged(mInstance, currentItem);
                    }
                });
            }

            if (state == null) {
                if (prevState != null) {
                    mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                        @Override
                        public void run(@NonNull ControllerCallback callback) {
                            callback.onPlayerStateChanged(mInstance, PLAYER_STATE_IDLE);
                        }
                    });
                }
                return;
            }
            if (prevState == null || prevState.getState() != state.getState()) {
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onPlayerStateChanged(
                                mInstance, MediaUtils.convertToPlayerState(state));
                    }
                });
            }
            if (prevState == null || prevState.getPlaybackSpeed() != state.getPlaybackSpeed()) {
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onPlaybackSpeedChanged(mInstance, state.getPlaybackSpeed());
                    }
                });
            }

            if (prevState != null) {
                final long currentPosition = state.getCurrentPosition(mInstance.mTimeDiff);
                long positionDiff = Math.abs(currentPosition
                        - prevState.getCurrentPosition(mInstance.mTimeDiff));
                if (positionDiff > POSITION_DIFF_TOLERANCE) {
                    mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                        @Override
                        public void run(@NonNull ControllerCallback callback) {
                            callback.onSeekCompleted(mInstance, currentPosition);
                        }
                    });
                }
            }

            if (!prevAllowedCommands.equals(currentAllowedCommands)) {
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onAllowedCommandsChanged(mInstance, currentAllowedCommands);
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
                mInstance.notifyPrimaryControllerCallback(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onSetCustomLayout(mInstance, currentLayout);
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
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onBufferingStateChanged(mInstance, currentItem, bufferingState);
                    }
                });
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            final MediaItem prevItem;
            final MediaItem currentItem;
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
                prevItem = mCurrentMediaItem;
                setCurrentMediaItemLocked(metadata);
                currentItem = mCurrentMediaItem;
            }
            if (prevItem != currentItem) {
                mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                    @Override
                    public void run(@NonNull ControllerCallback callback) {
                        callback.onCurrentMediaItemChanged(mInstance, currentItem);
                    }
                });
            }
        }

        @Override
        public void onQueueChanged(List<QueueItem> queue) {
            final List<MediaItem> playlist;
            final MediaMetadata playlistMetadata;
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
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
            mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onPlaylistChanged(mInstance, playlist, playlistMetadata);
                }
            });
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            final MediaMetadata playlistMetadata;
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
                mPlaylistMetadata = MediaUtils.convertToMediaMetadata(title);
                playlistMetadata = mPlaylistMetadata;
            }
            mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onPlaylistMetadataChanged(mInstance, playlistMetadata);
                }
            });
        }

        @Override
        public void onExtrasChanged(final Bundle extras) {
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
            }
            mInstance.notifyPrimaryControllerCallback(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onCustomCommand(mInstance, new SessionCommand(
                            SESSION_COMMAND_ON_EXTRAS_CHANGED, null), extras);
                }
            });
        }

        @Override
        public void onAudioInfoChanged(final MediaControllerCompat.PlaybackInfo infoCompat) {
            final PlaybackInfo info = MediaUtils.toPlaybackInfo2(infoCompat);
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
                mPlaybackInfo = info;
            }
            mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onPlaybackInfoChanged(mInstance, info);
                }
            });
        }

        @Override
        public void onCaptioningEnabledChanged(final boolean enabled) {
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
            }
            mInstance.notifyPrimaryControllerCallback(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    Bundle args = new Bundle();
                    args.putBoolean(MediaConstants.ARGUMENT_CAPTIONING_ENABLED, enabled);
                    callback.onCustomCommand(mInstance, new SessionCommand(
                            SESSION_COMMAND_ON_CAPTIONING_ENABLED_CHANGED, null), args);
                }
            });
        }

        @Override
        public void onRepeatModeChanged(@PlaybackStateCompat.RepeatMode final int repeatMode) {
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
                mRepeatMode = repeatMode;
            }
            mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onRepeatModeChanged(mInstance, repeatMode);
                }
            });
        }

        @Override
        public void onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode final int shuffleMode) {
            synchronized (mLock) {
                if (mClosed || !mConnected) {
                    return;
                }
                mShuffleMode = shuffleMode;
            }
            mInstance.notifyAllControllerCallbacks(new ControllerCallbackRunnable() {
                @Override
                public void run(@NonNull ControllerCallback callback) {
                    callback.onShuffleModeChanged(mInstance, shuffleMode);
                }
            });
        }
    }

    private static final class SetMediaUriRequest {
        public final String type;
        public final String value;
        public final Bundle extras;
        public final ResolvableFuture<SessionResult> result;

        SetMediaUriRequest(
                @NonNull String type,
                @NonNull String value,
                @Nullable Bundle extras,
                @NonNull ResolvableFuture<SessionResult> result) {
            this.type = type;
            this.value = value;
            this.extras = extras;
            this.result = result;
        }
    }
}
