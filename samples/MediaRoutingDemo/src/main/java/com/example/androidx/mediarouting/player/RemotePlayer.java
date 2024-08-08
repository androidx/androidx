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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import androidx.mediarouter.media.MediaSessionStatus;
import androidx.mediarouter.media.RemotePlaybackClient;
import androidx.mediarouter.media.RemotePlaybackClient.ItemActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.SessionActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.StatusCallback;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.data.PlaylistItem;
import com.example.androidx.mediarouting.providers.SampleMediaRouteProvider;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles playback of media items using a remote route.
 *
 * This class is used as a backend by PlaybackManager to feed media items to
 * the remote route. When the remote route doesn't support queuing, media items
 * are fed one-at-a-time; otherwise media items are enqueued to the remote side.
 */
public class RemotePlayer extends Player {
    private static final String TAG = "RemotePlayer";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private final SurfaceView mSurfaceView;
    private final FrameLayout mLayout;
    private RouteInfo mRoute;
    private boolean mEnqueuePending;
    private Bitmap mSnapshot;
    private List<PlaylistItem> mTempQueue = new ArrayList<>();

    private RemotePlaybackClient mClient;
    private StatusCallback mStatusCallback = new StatusCallback() {
        @Override
        public void onItemStatusChanged(Bundle data,
                @NonNull String sessionId, MediaSessionStatus sessionStatus,
                @NonNull String itemId, @NonNull MediaItemStatus itemStatus) {
            logStatus("onItemStatusChanged", sessionId, sessionStatus, itemId, itemStatus);
            if (mCallback != null) {
                mCallback.onPlaylistChanged();
                int state = itemStatus.getPlaybackState();
                if (state == MediaItemStatus.PLAYBACK_STATE_FINISHED) {
                    mCallback.onCompletion();
                } else if (state == MediaItemStatus.PLAYBACK_STATE_ERROR) {
                    mCallback.onError();
                }
            }
        }

        @Override
        public void onSessionStatusChanged(Bundle data,
                @NonNull String sessionId, MediaSessionStatus sessionStatus) {
            logStatus("onSessionStatusChanged", sessionId, sessionStatus, null, null);
            if (mCallback != null) {
                mCallback.onPlaylistChanged();
            }
        }

        @Override
        public void onSessionChanged(String sessionId) {
            if (DEBUG) {
                Log.d(TAG, "onSessionChanged: sessionId=" + sessionId);
            }
        }
    };

    public RemotePlayer(@NonNull Activity activity) {
        mContext = activity;
        mLayout = activity.findViewById(R.id.player);
        mSurfaceView = activity.findViewById(R.id.surface_view);
    }

    @Override
    public boolean isRemotePlayback() {
        return true;
    }

    @Override
    public boolean isQueuingSupported() {
        return mClient.isQueuingSupported();
    }

    @Override
    public void connect(@NonNull RouteInfo route) {
        mRoute = route;
        mClient = new RemotePlaybackClient(mContext, route);
        mClient.setStatusCallback(mStatusCallback);

        if (DEBUG) {
            Log.d(TAG, "connected to: " + route
                    + ", isRemotePlaybackSupported: " + mClient.isRemotePlaybackSupported()
                    + ", isQueuingSupported: " + mClient.isQueuingSupported());
        }
    }

    @Override
    public void updatePresentation() {
        mSurfaceView.setVisibility(View.GONE);
        mLayout.setVisibility(View.GONE);
    }

    @Override
    public void release() {
        mClient.release();

        if (DEBUG) {
            Log.d(TAG, "released.");
        }

        super.release();
    }

    // basic playback operations that are always supported
    @Override
    public void play(final @NonNull PlaylistItem item) {
        if (DEBUG) {
            Log.d(TAG, "play: item=" + item);
        }
        mClient.play(item.getUri(), item.getMime(), null, item.getPosition(), null,
                new ItemActionCallback() {
                    @Override
                    public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                            MediaSessionStatus sessionStatus,
                            @NonNull String itemId, @NonNull MediaItemStatus itemStatus) {
                        logStatus("play: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                        item.setRemoteItemId(itemId);
                        if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                            pause();
                        } else {
                            publishState(STATE_PLAYING);
                        }
                        if (mCallback != null) {
                            mCallback.onPlaylistChanged();
                        }
                    }

                    @Override
                    public void onError(String error, int code, Bundle data) {
                        logError("play: failed", error, code);
                    }
                });
    }

    @Override
    public void seek(final @NonNull PlaylistItem item) {
        seekInternal(item);
    }

    @Override
    public void getPlaylistItemStatus(
            final @NonNull PlaylistItem item, final boolean shouldUpdate) {
        if (!mClient.hasSession() || item.getRemoteItemId() == null) {
            // if session is not valid or item id not assigend yet.
            // just return, it's not fatal
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "getStatus: item=" + item + ", shouldUpdate=" + shouldUpdate);
        }
        updateStatus(item, shouldUpdate);
    }

    private void updateStatus(@NonNull PlaylistItem item, boolean shouldUpdate) {

        ListenableFuture<MediaItemStatus> remoteStatusFuture = getRemoteItemStatus(item);
        Futures.addCallback(remoteStatusFuture, new FutureCallback<MediaItemStatus>() {
            @Override
            public void onSuccess(MediaItemStatus itemStatus) {
                int state = itemStatus.getPlaybackState();
                if (state == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || state == MediaItemStatus.PLAYBACK_STATE_PAUSED
                        || state == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                    item.setState(state);
                    item.setPosition(itemStatus.getContentPosition());
                    item.setDuration(itemStatus.getContentDuration());
                    item.setTimestamp(itemStatus.getTimestamp());
                }
                if (shouldUpdate && mCallback != null) {
                    mCallback.onPlaylistReady();
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (shouldUpdate && mCallback != null) {
                    mCallback.onPlaylistReady();
                }
            }
        }, Runnable::run);
    }

    @Override
    public void pause() {
        if (!mClient.hasSession()) {
            // ignore if no session
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "pause");
        }
        mClient.pause(null, new SessionActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus) {
                logStatus("pause: succeeded", sessionId, sessionStatus, null, null);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
                publishState(STATE_PAUSED);
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("pause: failed", error, code);
            }
        });
    }

    @Override
    public void resume() {
        if (!mClient.hasSession()) {
            // ignore if no session
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "resume");
        }
        mClient.resume(null, new SessionActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus) {
                logStatus("resume: succeeded", sessionId, sessionStatus, null, null);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
                publishState(STATE_PLAYING);
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("resume: failed", error, code);
            }
        });
    }

    @Override
    public void stop() {
        if (!mClient.hasSession()) {
            // ignore if no session
            return;
        }
        publishState(STATE_IDLE);
        if (DEBUG) {
            Log.d(TAG, "stop");
        }
        mClient.stop(null, new SessionActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus) {
                logStatus("stop: succeeded", sessionId, sessionStatus, null, null);
                if (mClient.isSessionManagementSupported()) {
                    endSession();
                }
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("stop: failed", error, code);
            }
        });
    }

    // enqueue & remove are only supported if isQueuingSupported() returns true
    @Override
    public void enqueue(final @NonNull PlaylistItem item) {
        throwIfQueuingUnsupported();

        if (!mClient.hasSession() && !mEnqueuePending) {
            mEnqueuePending = true;
            if (mClient.isSessionManagementSupported()) {
                startSession(item);
            } else {
                enqueueInternal(item);
            }
        } else if (mEnqueuePending) {
            mTempQueue.add(item);
        } else {
            enqueueInternal(item);
        }
    }

    @NonNull
    @Override
    public PlaylistItem remove(@NonNull String itemId) {
        throwIfNoSession();
        throwIfQueuingUnsupported();

        if (DEBUG) {
            Log.d(TAG, "remove: itemId=" + itemId);
        }
        mClient.remove(itemId, null, new ItemActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus, @NonNull String itemId,
                    @NonNull MediaItemStatus itemStatus) {
                logStatus("remove: succeeded", sessionId, sessionStatus, itemId, itemStatus);
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("remove: failed", error, code);
            }
        });

        return null;
    }

    @Override
    public void takeSnapshot() {
        mSnapshot = null;

        Intent intent = new Intent(SampleMediaRouteProvider.ACTION_TAKE_SNAPSHOT);
        intent.addCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE);

        if (mRoute != null && mRoute.supportsControlRequest(intent)) {
            ControlRequestCallback callback = new ControlRequestCallback() {
                @Override
                public void onResult(Bundle data) {
                    if (DEBUG) {
                        Log.d(TAG, "takeSnapshot: succeeded: data=" + data);
                    }
                    if (data != null) {
                        mSnapshot = data.getParcelable(SampleMediaRouteProvider.EXTRA_SNAPSHOT);
                    }
                }

                @Override
                public void onError(String error, Bundle data) {
                    Log.d(TAG, "takeSnapshot: failed: error=" + error + ", data=" + data);
                }
            };

            mRoute.sendControlRequest(intent, callback);
        }
    }

    @NonNull
    @Override
    public Bitmap getSnapshot() {
        return mSnapshot;
    }

    /**
     * Caches the remote state of the given playlist item and returns an updated copy through a
     * {@link ListenableFuture}.
     */
    @NonNull
    public ListenableFuture<PlaylistItem> cacheRemoteState(@NonNull PlaylistItem item) {
        ListenableFuture<MediaItemStatus> remoteStatus = getRemoteItemStatus(item);

        return Futures.transform(
                remoteStatus,
                (itemStatus) -> convertMediaItemStatusToPlayListItem(item, itemStatus),
                Runnable::run);
    }

    private static PlaylistItem convertMediaItemStatusToPlayListItem(
            @NonNull PlaylistItem playlistItem, @NonNull MediaItemStatus itemStatus) {
        PlaylistItem updatedPlaylistItem = new PlaylistItem(playlistItem);
        updatedPlaylistItem.setState(itemStatus.getPlaybackState());
        updatedPlaylistItem.setPosition(itemStatus.getContentPosition());
        updatedPlaylistItem.setDuration(itemStatus.getContentDuration());
        updatedPlaylistItem.setTimestamp(itemStatus.getTimestamp());

        return updatedPlaylistItem;
    }

    private void enqueueInternal(final PlaylistItem item) {
        throwIfQueuingUnsupported();

        if (DEBUG) {
            Log.d(TAG, "enqueue: item=" + item);
        }
        mClient.enqueue(item.getUri(), item.getMime(), null, 0, null, new ItemActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus, @NonNull String itemId,
                    @NonNull MediaItemStatus itemStatus) {
                logStatus("enqueue: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                item.setRemoteItemId(itemId);
                if (item.getPosition() > 0) {
                    seekInternal(item);
                }
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    pause();
                } else if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
                    publishState(STATE_PLAYING);
                }
                if (mEnqueuePending) {
                    mEnqueuePending = false;
                    for (PlaylistItem item : mTempQueue) {
                        enqueueInternal(item);
                    }
                    mTempQueue.clear();
                }
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("enqueue: failed", error, code);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }
        });
    }

    private ListenableFuture<MediaItemStatus> getRemoteItemStatus(
            @NonNull PlaylistItem playlistItem) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            mClient.getStatus(playlistItem.getRemoteItemId(), null, new ItemActionCallback() {
                @Override
                public void onError(@Nullable String error, int code, @Nullable Bundle data) {
                    logError("getStatus: failed", error, code);
                    completer.setException(new RuntimeException(error));
                }

                @Override
                public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                        @Nullable MediaSessionStatus sessionStatus, @NonNull String itemId,
                        @NonNull MediaItemStatus itemStatus) {
                    logStatus("getStatus: succeeded", sessionId, sessionStatus, itemId,
                            itemStatus);
                    completer.set(itemStatus);
                }
            });

            return "RemotePlayer.getRemoteItemStatus()";
        });
    }

    private void seekInternal(final PlaylistItem item) {
        throwIfNoSession();

        if (DEBUG) {
            Log.d(TAG, "seek: item=" + item);
        }
        mClient.seek(item.getRemoteItemId(), item.getPosition(), null, new ItemActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus, @NonNull String itemId,
                    @NonNull MediaItemStatus itemStatus) {
                logStatus("seek: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("seek: failed", error, code);
            }
        });
    }

    private void startSession(final PlaylistItem item) {
        mClient.startSession(null, new SessionActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus) {
                logStatus("startSession: succeeded", sessionId, sessionStatus, null, null);
                enqueueInternal(item);
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("startSession: failed", error, code);
            }
        });
    }

    private void endSession() {
        mClient.endSession(null, new SessionActionCallback() {
            @Override
            public void onResult(@NonNull Bundle data, @NonNull String sessionId,
                    MediaSessionStatus sessionStatus) {
                logStatus("endSession: succeeded", sessionId, sessionStatus, null, null);
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("endSession: failed", error, code);
            }
        });
    }

    private void logStatus(String message,
            String sessionId, MediaSessionStatus sessionStatus,
            String itemId, MediaItemStatus itemStatus) {
        if (DEBUG) {
            String result = "";
            if (sessionId != null && sessionStatus != null) {
                result += "sessionId=" + sessionId + ", sessionStatus=" + sessionStatus;
            }
            if (itemId != null && itemStatus != null) {
                result += (result.isEmpty() ? "" : ", ")
                        + "itemId=" + itemId + ", itemStatus=" + itemStatus;
            }
            Log.d(TAG, message + ": " + result);
        }
    }

    private void logError(String message, String error, int code) {
        Log.d(TAG, message + ": error=" + error + ", code=" + code);
    }

    private void throwIfNoSession() {
        if (!mClient.hasSession()) {
            throw new IllegalStateException("Session is invalid");
        }
    }

    private void throwIfQueuingUnsupported() {
        if (!isQueuingSupported()) {
            throw new UnsupportedOperationException("Queuing is unsupported");
        }
    }
}
