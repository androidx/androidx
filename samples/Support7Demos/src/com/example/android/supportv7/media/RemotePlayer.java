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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaSessionStatus;
import android.support.v7.media.RemotePlaybackClient;
import android.support.v7.media.RemotePlaybackClient.ItemActionCallback;
import android.support.v7.media.RemotePlaybackClient.SessionActionCallback;
import android.support.v7.media.RemotePlaybackClient.StatusCallback;
import android.util.Log;

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
    private Context mContext;
    private RouteInfo mRoute;
    private boolean mEnqueuePending;
    private String mTrackInfo = "";
    private Bitmap mSnapshot;
    private List<PlaylistItem> mTempQueue = new ArrayList<PlaylistItem>();

    private RemotePlaybackClient mClient;
    private StatusCallback mStatusCallback = new StatusCallback() {
        @Override
        public void onItemStatusChanged(Bundle data,
                String sessionId, MediaSessionStatus sessionStatus,
                String itemId, MediaItemStatus itemStatus) {
            logStatus("onItemStatusChanged", sessionId, sessionStatus, itemId, itemStatus);
            if (mCallback != null) {
                if (itemStatus.getPlaybackState() ==
                        MediaItemStatus.PLAYBACK_STATE_FINISHED) {
                    mCallback.onCompletion();
                } else if (itemStatus.getPlaybackState() ==
                        MediaItemStatus.PLAYBACK_STATE_ERROR) {
                    mCallback.onError();
                }
            }
        }

        @Override
        public void onSessionStatusChanged(Bundle data,
                String sessionId, MediaSessionStatus sessionStatus) {
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

    public RemotePlayer(Context context) {
        mContext = context;
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
    public void connect(RouteInfo route) {
        mRoute = route;
        mClient = new RemotePlaybackClient(mContext, route);
        mClient.setStatusCallback(mStatusCallback);

        if (DEBUG) {
            Log.d(TAG, "connected to: " + route
                    + ", isRemotePlaybackSupported: " + mClient.isRemotePlaybackSupported()
                    + ", isQueuingSupported: "+ mClient.isQueuingSupported());
        }
    }

    @Override
    public void release() {
        mClient.release();

        if (DEBUG) {
            Log.d(TAG, "released.");
        }
    }

    // basic playback operations that are always supported
    @Override
    public void play(final PlaylistItem item) {
        if (DEBUG) {
            Log.d(TAG, "play: item=" + item);
        }
        mClient.play(item.getUri(), "video/mp4", null, 0, null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                    String itemId, MediaItemStatus itemStatus) {
                logStatus("play: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                item.setRemoteItemId(itemId);
                if (item.getPosition() > 0) {
                    seekInternal(item);
                }
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    pause();
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
    public void seek(final PlaylistItem item) {
        seekInternal(item);
    }

    @Override
    public void getStatus(final PlaylistItem item, final boolean update) {
        if (!mClient.hasSession() || item.getRemoteItemId() == null) {
            // if session is not valid or item id not assigend yet.
            // just return, it's not fatal
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "getStatus: item=" + item + ", update=" + update);
        }
        mClient.getStatus(item.getRemoteItemId(), null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                    String itemId, MediaItemStatus itemStatus) {
                logStatus("getStatus: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                int state = itemStatus.getPlaybackState();
                if (state == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || state == MediaItemStatus.PLAYBACK_STATE_PAUSED
                        || state == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                    item.setState(state);
                    item.setPosition(itemStatus.getContentPosition());
                    item.setDuration(itemStatus.getContentDuration());
                    item.setTimestamp(itemStatus.getTimestamp());
                }
                if (update && mCallback != null) {
                    mCallback.onPlaylistReady();
                }
            }

            @Override
            public void onError(String error, int code, Bundle data) {
                logError("getStatus: failed", error, code);
                if (update && mCallback != null) {
                    mCallback.onPlaylistReady();
                }
            }
        });
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
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
                logStatus("pause: succeeded", sessionId, sessionStatus, null, null);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
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
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
                logStatus("resume: succeeded", sessionId, sessionStatus, null, null);
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
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
        if (DEBUG) {
            Log.d(TAG, "stop");
        }
        mClient.stop(null, new SessionActionCallback() {
            @Override
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
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
    public void enqueue(final PlaylistItem item) {
        throwIfQueuingUnsupported();

        if (!mClient.hasSession() && !mEnqueuePending) {
            mEnqueuePending = true;
            if (mClient.isSessionManagementSupported()) {
                startSession(item);
            } else {
                enqueueInternal(item);
            }
        } else if (mEnqueuePending){
            mTempQueue.add(item);
        } else {
            enqueueInternal(item);
        }
    }

    @Override
    public PlaylistItem remove(String itemId) {
        throwIfNoSession();
        throwIfQueuingUnsupported();

        if (DEBUG) {
            Log.d(TAG, "remove: itemId=" + itemId);
        }
        mClient.remove(itemId, null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                    String itemId, MediaItemStatus itemStatus) {
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
    public void updateTrackInfo() {
        // clear stats info first
        mTrackInfo = "";
        mSnapshot = null;

        Intent intent = new Intent(SampleMediaRouteProvider.ACTION_GET_TRACK_INFO);
        intent.addCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE);

        if (mRoute != null && mRoute.supportsControlRequest(intent)) {
            ControlRequestCallback callback = new ControlRequestCallback() {
                @Override
                public void onResult(Bundle data) {
                    if (DEBUG) {
                        Log.d(TAG, "getStatistics: succeeded: data=" + data);
                    }
                    if (data != null) {
                        mTrackInfo = data.getString(SampleMediaRouteProvider.TRACK_INFO_DESC);
                        mSnapshot = data.getParcelable(
                                SampleMediaRouteProvider.TRACK_INFO_SNAPSHOT);
                    }
                }

                @Override
                public void onError(String error, Bundle data) {
                    Log.d(TAG, "getStatistics: failed: error=" + error + ", data=" + data);
                }
            };

            mRoute.sendControlRequest(intent, callback);
        }
    }

    @Override
    public String getDescription() {
        return mTrackInfo;
    }

    @Override
    public Bitmap getSnapshot() {
        return mSnapshot;
    }

    private void enqueueInternal(final PlaylistItem item) {
        throwIfQueuingUnsupported();

        if (DEBUG) {
            Log.d(TAG, "enqueue: item=" + item);
        }
        mClient.enqueue(item.getUri(), "video/mp4", null, 0, null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                    String itemId, MediaItemStatus itemStatus) {
                logStatus("enqueue: succeeded", sessionId, sessionStatus, itemId, itemStatus);
                item.setRemoteItemId(itemId);
                if (item.getPosition() > 0) {
                    seekInternal(item);
                }
                if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    pause();
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

    private void seekInternal(final PlaylistItem item) {
        throwIfNoSession();

        if (DEBUG) {
            Log.d(TAG, "seek: item=" + item);
        }
        mClient.seek(item.getRemoteItemId(), item.getPosition(), null, new ItemActionCallback() {
           @Override
           public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                   String itemId, MediaItemStatus itemStatus) {
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
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
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
            public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
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
            if (itemId != null & itemStatus != null) {
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
