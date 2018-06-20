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

package androidx.media.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.media.AudioAttributesCompat;
import androidx.media2.BaseMediaPlayer;
import androidx.media2.DataSourceDesc2;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaSessionStatus;
import androidx.mediarouter.media.RemotePlaybackClient;
import androidx.mediarouter.media.RemotePlaybackClient.ItemActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.SessionActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.StatusCallback;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RoutePlayer2 extends BaseMediaPlayer {
    private static final String TAG = "RoutePlayer2";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    String mItemId;
    int mCurrentPlayerState;
    long mDuration;
    long mLastStatusChangedTime;
    long mPosition;
    boolean mCanResume;
    ArrayMap<PlayerEventCallback, Executor> mPlayerEventCallbackMap =
            new ArrayMap<>();
    private RemotePlaybackClient mClient;
    private DataSourceDesc2 mDsd;

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
            if (mPlayerEventCallbackMap.size() > 0) {
                for (PlayerEventCallback callback : mPlayerEventCallbackMap.keySet()) {
                    callback.onPlayerStateChanged(RoutePlayer2.this, mCurrentPlayerState);
                }
            }
        }
    };

    public RoutePlayer2(Context context, MediaRouter.RouteInfo route) {
        mClient = new RemotePlaybackClient(context, route);
        mClient.setStatusCallback(mStatusCallback);
        if (mClient.isSessionManagementSupported()) {
            mClient.startSession(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "RoutePlayer2 has been initialized, but session is not"
                                + "active.");
                    }
                }
            });
        }
    }

    @Override
    public void play() {
        if (mDsd == null) {
            return;
        }

        // RemotePlaybackClient cannot call resume(..) without calling pause(..) first.
        if (!mCanResume) {
            playInternal();
            return;
        }

        if (mClient.isSessionManagementSupported()) {
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
                }
            });
        }
    }

    @Override
    public void prepare() {
        // Do nothing
    }

    @Override
    public void pause() {
        if (mClient.isSessionManagementSupported()) {
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
                }
            });
        }
    }

    @Override
    public void reset() {
        if (mClient.isSessionManagementSupported()) {
            mClient.stop(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "reset() is called, but session is not active.");
                    }
                }
            });
        }
    }

    @Override
    public void skipToNext() {
        // TODO: implement
    }

    @Override
    public void seekTo(long pos) {
        if (mClient.isSessionManagementSupported()) {
            mClient.seek(mItemId, pos, null, new ItemActionCallback() {
                @Override
                public void onResult(Bundle data,
                        String sessionId, MediaSessionStatus sessionStatus,
                        String itemId, MediaItemStatus itemStatus) {
                    if (DEBUG && !isSessionActive(sessionStatus)) {
                        Log.v(TAG, "seekTo(long) is called, but session is not active.");
                    }
                    if (itemStatus != null) {
                        if (mPlayerEventCallbackMap.size() > 0) {
                            for (PlayerEventCallback callback : mPlayerEventCallbackMap.keySet()) {
                                callback.onSeekCompleted(RoutePlayer2.this,
                                        itemStatus.getContentPosition());
                            }
                        }
                    }
                }
            });
        }
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
        return BaseMediaPlayer.BUFFERING_STATE_UNKNOWN;
    }

    @Override
    public void setAudioAttributes(AudioAttributesCompat attributes) {
        // TODO: implement
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return null;
    }

    @Override
    public void setDataSource(DataSourceDesc2 dsd) {
        mDsd = dsd;
        if (dsd.getUri() == null) {
            return;
        }
    }

    @Override
    public void setNextDataSource(DataSourceDesc2 dsd) {
        // TODO: implement
    }

    @Override
    public void setNextDataSources(List<DataSourceDesc2> dsds) {
        // TODO: implement
    }

    @Override
    public DataSourceDesc2 getCurrentDataSource() {
        return mDsd;
    }

    @Override
    public void loopCurrent(boolean loop) {
        // TODO: implement
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        // Do nothing
    }

    @Override
    public float getPlaybackSpeed() {
        return 1.0f;
    }

    @Override
    public void setPlayerVolume(float volume) {
        // TODO: implement
    }

    @Override
    public float getPlayerVolume() {
        return 0;
    }

    @Override
    public void registerPlayerEventCallback(Executor e, BaseMediaPlayer.PlayerEventCallback cb) {
        mPlayerEventCallbackMap.put(cb, e);
    }

    @Override
    public void unregisterPlayerEventCallback(BaseMediaPlayer.PlayerEventCallback cb) {
        mPlayerEventCallbackMap.remove(cb);
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
        mPlayerEventCallbackMap.clear();
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

    private void playInternal() {
        mClient.play(mDsd.getUri(), "video/mp4", null, mPosition, null,
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
                    }
                });
    }
}
