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
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaSessionStatus;
import androidx.mediarouter.media.RemotePlaybackClient;
import androidx.mediarouter.media.RemotePlaybackClient.ItemActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.SessionActionCallback;
import androidx.mediarouter.media.RemotePlaybackClient.StatusCallback;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RoutePlayer extends MediaSession.Callback {
    public static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SEEK_TO
            | PlaybackStateCompat.ACTION_FAST_FORWARD | PlaybackStateCompat.ACTION_REWIND;

    private RemotePlaybackClient mClient;
    private String mSessionId;
    private String mItemId;
    private PlayerEventCallback mCallback;

    private StatusCallback mStatusCallback = new StatusCallback() {
        @Override
        public void onItemStatusChanged(Bundle data,
                                        String sessionId, MediaSessionStatus sessionStatus,
                                        String itemId, MediaItemStatus itemStatus) {
            updateSessionStatus(sessionId, sessionStatus);
            updateItemStatus(itemId, itemStatus);
        }
    };

    public RoutePlayer(Context context, MediaRouter.RouteInfo route) {
        mClient = new RemotePlaybackClient(context, route);
        mClient.setStatusCallback(mStatusCallback);
        if (mClient.isSessionManagementSupported()) {
            mClient.startSession(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                                     String sessionId, MediaSessionStatus sessionStatus) {
                    updateSessionStatus(sessionId, sessionStatus);
                }
            });
        }
    }

    @Override
    public void onPlay() {
        if (mClient.isSessionManagementSupported()) {
            mClient.resume(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                                     String sessionId, MediaSessionStatus sessionStatus) {
                    updateSessionStatus(sessionId, sessionStatus);
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (mClient.isSessionManagementSupported()) {
            mClient.pause(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                                     String sessionId, MediaSessionStatus sessionStatus) {
                    updateSessionStatus(sessionId, sessionStatus);
                }
            });
        }
    }

    @Override
    public void onSeekTo(long pos) {
        if (mClient.isSessionManagementSupported()) {
            mClient.seek(mItemId, pos, null, new ItemActionCallback() {
                @Override
                public void onResult(Bundle data,
                                     String sessionId, MediaSessionStatus sessionStatus,
                                     String itemId, MediaItemStatus itemStatus) {
                    updateSessionStatus(sessionId, sessionStatus);
                    updateItemStatus(itemId, itemStatus);
                }
            });
        }
    }

    @Override
    public void onStop() {
        if (mClient.isSessionManagementSupported()) {
            mClient.stop(null, new SessionActionCallback() {
                @Override
                public void onResult(Bundle data,
                                     String sessionId, MediaSessionStatus sessionStatus) {
                    updateSessionStatus(sessionId, sessionStatus);
                }
            });
        }
    }

    /**
     * Sets a callback to be notified of events for this player.
     * @param callback the callback to receive the events.
     */
    public void setPlayerEventCallback(PlayerEventCallback callback) {
        mCallback = callback;
    }

    // b/77556429
//    public void openVideo(DataSourceDesc dsd) {
//        mClient.play(dsd.getUri(), "video/mp4", null, 0, null, new ItemActionCallback() {
//            @Override
//            public void onResult(Bundle data,
//                                 String sessionId, MediaSessionStatus sessionStatus,
//                                 String itemId, MediaItemStatus itemStatus) {
//                updateSessionStatus(sessionId, sessionStatus);
//                updateItemStatus(itemId, itemStatus);
//                playInternal(dsd.getUri());
//            }
//        });
//    }

    /**
     * Opens the video based on the given uri and updates the media session and item statuses.
     * @param uri link to the video
     */
    public void openVideo(Uri uri) {
        mClient.play(uri, "video/mp4", null, 0, null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data,
                                 String sessionId, MediaSessionStatus sessionStatus,
                                 String itemId, MediaItemStatus itemStatus) {
                updateSessionStatus(sessionId, sessionStatus);
                updateItemStatus(itemId, itemStatus);
            }
        });
    }

    /**
     * Releases the {@link RemotePlaybackClient} and {@link PlayerEventCallback} instances.
     */
    public void release() {
        if (mClient != null) {
            mClient.release();
            mClient = null;
        }
        if (mCallback != null) {
            mCallback = null;
        }
    }

    private void playInternal(Uri uri) {
        mClient.play(uri, "video/mp4", null, 0, null, new ItemActionCallback() {
            @Override
            public void onResult(Bundle data,
                                 String sessionId, MediaSessionStatus sessionStatus,
                                 String itemId, MediaItemStatus itemStatus) {
                updateSessionStatus(sessionId, sessionStatus);
                updateItemStatus(itemId, itemStatus);
            }
        });
    }

    private void updateSessionStatus(String sessionId, MediaSessionStatus sessionStatus) {
        mSessionId = sessionId;
    }

    private void updateItemStatus(String itemId, MediaItemStatus itemStatus) {
        mItemId = itemId;
        if (itemStatus == null || mCallback == null) return;
        mCallback.onPlayerStateChanged(itemStatus);
    }

    /**
     * A callback class to receive notifications for events on the route player.
     */
    public abstract static class PlayerEventCallback {
        /**
         * Override to handle changes in playback state.
         *
         * @param itemStatus The new MediaItemStatus of the RoutePlayer
         */
        public void onPlayerStateChanged(MediaItemStatus itemStatus) { }
    }
}
