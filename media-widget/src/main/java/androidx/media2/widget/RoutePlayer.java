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

package androidx.media2.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_SUCCESS;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_UNKNOWN_ERROR;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.RemoteSessionPlayer;
import androidx.media2.SessionPlayer;
import androidx.media2.UriMediaItem;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaSessionStatus;
import androidx.mediarouter.media.RemotePlaybackClient;
import androidx.mediarouter.media.RemotePlaybackClient.ItemActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.SessionActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.StatusCallback;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RoutePlayer extends RemoteSessionPlayer {
    private static final String TAG = "RoutePlayer";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    String mItemId;
    int mCurrentPlayerState;
    long mDuration;
    long mLastStatusChangedTime;
    long mPosition;
    boolean mCanResume;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaRouter.RouteInfo mSelectedRoute;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<ResolvableFuture<PlayerResult>> mPendingVolumeResult = new ArrayList<>();

    private MediaItem mItem;
    private MediaRouter mMediaRouter;
    private RemotePlaybackClient mClient;

    private MediaRouter.Callback mRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (TextUtils.equals(route.getId(), mSelectedRoute.getId())) {
                final int volume = route.getVolume();
                for (int i = 0; i < mPendingVolumeResult.size(); i++) {
                    mPendingVolumeResult.get(i).set(new PlayerResult(
                            RESULT_CODE_SUCCESS, getCurrentMediaItem()));
                }
                mPendingVolumeResult.clear();
                List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
                for (Pair<PlayerCallback, Executor> pair : callbacks) {
                    if (pair.first instanceof RemoteSessionPlayer.Callback) {
                        final RemoteSessionPlayer.PlayerCallback callback = pair.first;
                        pair.second.execute(new Runnable() {
                            @Override
                            public void run() {
                                ((RemoteSessionPlayer.Callback) callback)
                                        .onVolumeChanged(RoutePlayer.this, volume);
                            }
                        });
                    }
                }
            }
        }
    };

    private StatusCallback mStatusCallback = new StatusCallback() {
        @Override
        public void onItemStatusChanged(Bundle data,
                String sessionId, MediaSessionStatus sessionStatus,
                String itemId, MediaItemStatus itemStatus) {
            if (DEBUG && !isSessionActive(sessionStatus)) {
                Log.v(TAG, "onItemStatusChanged() is called, but session is not active.");
            }
            mLastStatusChangedTime = SystemClock.elapsedRealtime();
            mPosition = itemStatus.getContentPosition();
            mCurrentPlayerState = convertPlaybackStateToPlayerState(itemStatus.getPlaybackState());

            List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
            for (Pair<PlayerCallback, Executor> pair : callbacks) {
                final PlayerCallback callback = pair.first;
                pair.second.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onPlayerStateChanged(RoutePlayer.this, mCurrentPlayerState);
                    }
                });
            }
        }
    };

    public RoutePlayer(Context context, MediaRouteSelector selector,
            MediaRouter.RouteInfo route) {
        mMediaRouter = MediaRouter.getInstance(context);
        mMediaRouter.addCallback(selector, mRouterCallback);
        mSelectedRoute = route;

        mClient = new RemotePlaybackClient(context, route);
        mClient.setStatusCallback(mStatusCallback);
        if (mClient.isSessionManagementSupported()) {
            mClient.startSession(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "RoutePlayer has been initialized, but session is not"
                                + "active.");
                    }
                }
            });
        }
    }

    @Override
    public ListenableFuture<PlayerResult> play() {
        if (mItem == null) {
            return createResult(RESULT_CODE_BAD_VALUE);
        }

        // RemotePlaybackClient cannot call resume(..) without calling pause(..) first.
        if (!mCanResume) {
            return playInternal();
        }

        if (mClient.isSessionManagementSupported()) {
            final ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
            mClient.resume(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "play() is called, but session is not active.");
                    }
                    // Do nothing since this returns the buffering state--
                    // StatusCallback#onItemStatusChanged is called when the session reaches the
                    // play state.
                    result.set(new PlayerResult(RESULT_CODE_SUCCESS, getCurrentMediaItem()));
                }
            });
        }
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> prepare() {
        return createResult();
    }

    @Override
    public ListenableFuture<PlayerResult> pause() {
        if (mClient.isSessionManagementSupported()) {
            final ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
            mClient.pause(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "pause() is called, but session is not active.");
                    }
                    mCanResume = true;
                    // Do not update playback state here since this returns the buffering state--
                    // StatusCallback#onItemStatusChanged is called when the session reaches the
                    // pause state.
                    result.set(new PlayerResult(RESULT_CODE_SUCCESS, getCurrentMediaItem()));
                }
            });
        }
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> seekTo(long pos) {
        if (mClient.isSessionManagementSupported()) {
            final ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
            mClient.seek(mItemId, pos, null, new ItemActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus,
                        String itemId, final MediaItemStatus itemStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "seekTo(long) is called, but session is not active.");
                    }
                    if (itemStatus != null) {
                        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
                        for (Pair<PlayerCallback, Executor> pair : callbacks) {
                            final PlayerCallback callback = pair.first;
                            pair.second.execute(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSeekCompleted(RoutePlayer.this,
                                            itemStatus.getContentPosition());
                                }
                            });
                        }
                    } else {
                        result.set(new PlayerResult(RESULT_CODE_UNKNOWN_ERROR,
                                getCurrentMediaItem()));
                    }
                }
            });
        }
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public long getCurrentPosition() {
        long expectedPosition = mPosition;
        if (mCurrentPlayerState == PLAYER_STATE_PLAYING) {
            expectedPosition = mPosition + (SystemClock.elapsedRealtime() - mLastStatusChangedTime);
        }
        return expectedPosition;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public int getPlayerState() {
        return mCurrentPlayerState;
    }

    @Override
    public int getBufferingState() {
        return SessionPlayer.BUFFERING_STATE_UNKNOWN;
    }

    @Override
    public ListenableFuture<PlayerResult> setAudioAttributes(AudioAttributesCompat attributes) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return null;
    }

    @Override
    public ListenableFuture<PlayerResult> setMediaItem(MediaItem item) {
        mItem = item;
        return createResult();
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        return mItem;
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaybackSpeed(float speed) {
        // Do nothing
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public float getPlaybackSpeed() {
        return 1.0f;
    }

    @Override
    public int getVolume() {
        return mSelectedRoute.getVolume();
    }

    @Override
    public Future<PlayerResult> adjustVolume(int direction) {
        mSelectedRoute.requestUpdateVolume(direction);

        ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
        mPendingVolumeResult.add(result);
        return result;
    }

    @Override
    public Future<PlayerResult> setVolume(int volume) {
        mSelectedRoute.requestSetVolume(volume);

        ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
        mPendingVolumeResult.add(result);
        return result;
    }

    @Override
    public int getMaxVolume() {
        return mSelectedRoute.getVolumeMax();
    }

    @Override
    public int getVolumeControlType() {
        return mSelectedRoute.getVolumeHandling();
    }

    @Override
    public ListenableFuture<PlayerResult> setPlaylist(List<MediaItem> list,
            MediaMetadata metadata) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> addPlaylistItem(int index, MediaItem item) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> removePlaylistItem(int index) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> replacePlaylistItem(int index, MediaItem item) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPreviousPlaylistItem() {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToNextPlaylistItem() {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> skipToPlaylistItem(int index) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(MediaMetadata metadata) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> setRepeatMode(int repeatMode) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public ListenableFuture<PlayerResult> setShuffleMode(int shuffleMode) {
        // TODO: implement
        return createResult(RESULT_CODE_INVALID_STATE);
    }

    @Override
    public List<MediaItem> getPlaylist() {
        List<MediaItem> list = new ArrayList<>();
        list.add(mItem);
        return list;
    }

    @Override
    public MediaMetadata getPlaylistMetadata() {
        return null;
    }

    @Override
    public int getRepeatMode() {
        return SessionPlayer.REPEAT_MODE_NONE;
    }

    @Override
    public int getShuffleMode() {
        return SessionPlayer.SHUFFLE_MODE_NONE;
    }

    @Override
    public void close() {
        if (mClient != null) {
            try {
                mClient.release();
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver not registered");
            }
            mClient = null;
        }
        mMediaRouter.removeCallback(mRouterCallback);
    }

    void setCurrentPosition(long position) {
        mPosition = position;
    }

    boolean isSessionActive(MediaSessionStatus status) {
        if (status == null || status.getSessionState() == MediaSessionStatus.SESSION_STATE_ENDED
                || status.getSessionState() == MediaSessionStatus.SESSION_STATE_INVALIDATED) {
            return false;
        }
        return true;
    }

    int convertPlaybackStateToPlayerState(int playbackState) {
        int playerState = PLAYER_STATE_IDLE;
        switch (playbackState) {
            case MediaItemStatus.PLAYBACK_STATE_PENDING:
            case MediaItemStatus.PLAYBACK_STATE_FINISHED:
            case MediaItemStatus.PLAYBACK_STATE_CANCELED:
                playerState = PLAYER_STATE_IDLE;
                break;
            case MediaItemStatus.PLAYBACK_STATE_PLAYING:
                playerState = PLAYER_STATE_PLAYING;
                break;
            case MediaItemStatus.PLAYBACK_STATE_PAUSED:
            case MediaItemStatus.PLAYBACK_STATE_BUFFERING:
                playerState = PLAYER_STATE_PAUSED;
                break;
            case MediaItemStatus.PLAYBACK_STATE_INVALIDATED:
            case MediaItemStatus.PLAYBACK_STATE_ERROR:
                playerState =  PLAYER_STATE_ERROR;
                break;
        }
        return playerState;
    }

    private ListenableFuture<PlayerResult> playInternal() {
        if (!(mItem instanceof UriMediaItem)) {
            Log.w(TAG, "Data source type is not Uri." + mItem);
            return createResult(RESULT_CODE_BAD_VALUE);
        }
        final ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
        mClient.play(((UriMediaItem) mItem).getUri(), "video/mp4", null, mPosition, null,
                new ItemActionCallback() {
                    @Override
                    public void onResult(Bundle data, String sessionId,
                            MediaSessionStatus sessionStatus,
                            String itemId, MediaItemStatus itemStatus) {
                        if (DEBUG && !isSessionActive(sessionStatus)) {
                            Log.v(TAG, "play() is called, but session is not active.");
                        }
                        mItemId = itemId;
                        if (itemStatus != null) {
                            mDuration = itemStatus.getContentDuration();
                        }
                        // Do not update playback state here since this returns the buffering state.
                        // StatusCallback#onItemStatusChanged is called when the session reaches the
                        // play state.
                        result.set(new PlayerResult(RESULT_CODE_SUCCESS, getCurrentMediaItem()));
                    }
                });
        return result;
    }

    private ListenableFuture<PlayerResult> createResult() {
        return createResult(RESULT_CODE_SUCCESS);
    }

    private ListenableFuture<PlayerResult> createResult(int code) {
        ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
        result.set(new PlayerResult(code, getCurrentMediaItem()));
        return result;
    }
}
