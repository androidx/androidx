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
package androidx.mediarouter.media;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.util.ObjectsCompat;

/**
 * A helper class for playing media on remote routes using the remote playback protocol
 * defined by {@link MediaControlIntent}.
 * <p>
 * The client maintains session state and offers a simplified interface for issuing
 * remote playback media control intents to a single route.
 * </p>
 */
public class RemotePlaybackClient {
    static final String TAG = "RemotePlaybackClient";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;
    private final MediaRouter.RouteInfo mRoute;
    private final ActionReceiver mActionReceiver;
    private final PendingIntent mItemStatusPendingIntent;
    private final PendingIntent mSessionStatusPendingIntent;
    private final PendingIntent mMessagePendingIntent;

    private boolean mRouteSupportsRemotePlayback;
    private boolean mRouteSupportsQueuing;
    private boolean mRouteSupportsSessionManagement;
    private boolean mRouteSupportsMessaging;

    String mSessionId;
    StatusCallback mStatusCallback;
    OnMessageReceivedListener mOnMessageReceivedListener;

    /**
     * Creates a remote playback client for a route.
     *
     * @param route The media route.
     */
    public RemotePlaybackClient(Context context, MediaRouter.RouteInfo route) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }

        mContext = context;
        mRoute = route;

        IntentFilter actionFilter = new IntentFilter();
        actionFilter.addAction(ActionReceiver.ACTION_ITEM_STATUS_CHANGED);
        actionFilter.addAction(ActionReceiver.ACTION_SESSION_STATUS_CHANGED);
        actionFilter.addAction(ActionReceiver.ACTION_MESSAGE_RECEIVED);
        mActionReceiver = new ActionReceiver();
        context.registerReceiver(mActionReceiver, actionFilter);

        Intent itemStatusIntent = new Intent(ActionReceiver.ACTION_ITEM_STATUS_CHANGED);
        itemStatusIntent.setPackage(context.getPackageName());
        mItemStatusPendingIntent = PendingIntent.getBroadcast(
                context, 0, itemStatusIntent, 0);

        Intent sessionStatusIntent = new Intent(ActionReceiver.ACTION_SESSION_STATUS_CHANGED);
        sessionStatusIntent.setPackage(context.getPackageName());
        mSessionStatusPendingIntent = PendingIntent.getBroadcast(
                context, 0, sessionStatusIntent, 0);

        Intent messageIntent = new Intent(ActionReceiver.ACTION_MESSAGE_RECEIVED);
        messageIntent.setPackage(context.getPackageName());
        mMessagePendingIntent = PendingIntent.getBroadcast(
                context, 0, messageIntent, 0);
        detectFeatures();
    }

    /**
     * Releases resources owned by the client.
     */
    public void release() {
        mContext.unregisterReceiver(mActionReceiver);
    }

    /**
     * Returns true if the route supports remote playback.
     * <p>
     * If the route does not support remote playback, then none of the functionality
     * offered by the client will be available.
     * </p><p>
     * This method returns true if the route supports all of the following
     * actions: {@link MediaControlIntent#ACTION_PLAY play},
     * {@link MediaControlIntent#ACTION_SEEK seek},
     * {@link MediaControlIntent#ACTION_GET_STATUS get status},
     * {@link MediaControlIntent#ACTION_PAUSE pause},
     * {@link MediaControlIntent#ACTION_RESUME resume},
     * {@link MediaControlIntent#ACTION_STOP stop}.
     * </p>
     *
     * @return True if remote playback is supported.
     */
    public boolean isRemotePlaybackSupported() {
        return mRouteSupportsRemotePlayback;
    }

    /**
     * Returns true if the route supports queuing features.
     * <p>
     * If the route does not support queuing, then at most one media item can be played
     * at a time and the {@link #enqueue} method will not be available.
     * </p><p>
     * This method returns true if the route supports all of the basic remote playback
     * actions and all of the following actions:
     * {@link MediaControlIntent#ACTION_ENQUEUE enqueue},
     * {@link MediaControlIntent#ACTION_REMOVE remove}.
     * </p>
     *
     * @return True if queuing is supported.  Implies {@link #isRemotePlaybackSupported}
     * is also true.
     *
     * @see #isRemotePlaybackSupported
     */
    public boolean isQueuingSupported() {
        return mRouteSupportsQueuing;
    }

    /**
     * Returns true if the route supports session management features.
     * <p>
     * If the route does not support session management, then the session will
     * not be created until the first media item is played.
     * </p><p>
     * This method returns true if the route supports all of the basic remote playback
     * actions and all of the following actions:
     * {@link MediaControlIntent#ACTION_START_SESSION start session},
     * {@link MediaControlIntent#ACTION_GET_SESSION_STATUS get session status},
     * {@link MediaControlIntent#ACTION_END_SESSION end session}.
     * </p>
     *
     * @return True if session management is supported.
     * Implies {@link #isRemotePlaybackSupported} is also true.
     *
     * @see #isRemotePlaybackSupported
     */
    public boolean isSessionManagementSupported() {
        return mRouteSupportsSessionManagement;
    }

    /**
     * Returns true if the route supports messages.
     * <p>
     * This method returns true if the route supports all of the basic remote playback
     * actions and all of the following actions:
     * {@link MediaControlIntent#ACTION_START_SESSION start session},
     * {@link MediaControlIntent#ACTION_SEND_MESSAGE send message},
     * {@link MediaControlIntent#ACTION_END_SESSION end session}.
     * </p>
     *
     * @return True if session management is supported.
     * Implies {@link #isRemotePlaybackSupported} is also true.
     *
     * @see #isRemotePlaybackSupported
     */
    public boolean isMessagingSupported() {
        return mRouteSupportsMessaging;
    }

    /**
     * Gets the current session id if there is one.
     *
     * @return The current session id, or null if none.
     */
    public String getSessionId() {
        return mSessionId;
    }

    /**
     * Sets the current session id.
     * <p>
     * It is usually not necessary to set the session id explicitly since
     * it is created as a side-effect of other requests such as
     * {@link #play}, {@link #enqueue}, and {@link #startSession}.
     * </p>
     *
     * @param sessionId The new session id, or null if none.
     */
    public void setSessionId(String sessionId) {
        if (!ObjectsCompat.equals(mSessionId, sessionId)) {
            if (DEBUG) {
                Log.d(TAG, "Session id is now: " + sessionId);
            }
            mSessionId = sessionId;
            if (mStatusCallback != null) {
                mStatusCallback.onSessionChanged(sessionId);
            }
        }
    }

    /**
     * Returns true if the client currently has a session.
     * <p>
     * Equivalent to checking whether {@link #getSessionId} returns a non-null result.
     * </p>
     *
     * @return True if there is a current session.
     */
    public boolean hasSession() {
        return mSessionId != null;
    }

    /**
     * Sets a callback that should receive status updates when the state of
     * media sessions or media items created by this instance of the remote
     * playback client changes.
     * <p>
     * The callback should be set before the session is created or any play
     * commands are issued.
     * </p>
     *
     * @param callback The callback to set.  May be null to remove the previous callback.
     */
    public void setStatusCallback(StatusCallback callback) {
        mStatusCallback = callback;
    }

    /**
     * Sets a callback that should receive messages when a message is sent from
     * media sessions created by this instance of the remote playback client changes.
     * <p>
     * The callback should be set before the session is created.
     * </p>
     *
     * @param listener The callback to set.  May be null to remove the previous callback.
     */
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        mOnMessageReceivedListener = listener;
    }

    /**
     * Sends a request to play a media item.
     * <p>
     * Clears the queue and starts playing the new item immediately.  If the queue
     * was previously paused, then it is resumed as a side-effect of this request.
     * </p><p>
     * The request is issued in the current session.  If no session is available, then
     * one is created implicitly.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_PLAY ACTION_PLAY} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param contentUri The content Uri to play.
     * @param mimeType The mime type of the content, or null if unknown.
     * @param positionMillis The initial content position for the item in milliseconds,
     * or <code>0</code> to start at the beginning.
     * @param metadata The media item metadata bundle, or null if none.
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_PLAY} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws UnsupportedOperationException if the route does not support remote playback.
     *
     * @see MediaControlIntent#ACTION_PLAY
     * @see #isRemotePlaybackSupported
     */
    public void play(Uri contentUri, String mimeType, Bundle metadata,
            long positionMillis, Bundle extras, ItemActionCallback callback) {
        playOrEnqueue(contentUri, mimeType, metadata, positionMillis,
                extras, callback, MediaControlIntent.ACTION_PLAY);
    }

    /**
     * Sends a request to enqueue a media item.
     * <p>
     * Enqueues a new item to play.  If the queue was previously paused, then will
     * remain paused.
     * </p><p>
     * The request is issued in the current session.  If no session is available, then
     * one is created implicitly.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_ENQUEUE ACTION_ENQUEUE} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param contentUri The content Uri to enqueue.
     * @param mimeType The mime type of the content, or null if unknown.
     * @param positionMillis The initial content position for the item in milliseconds,
     * or <code>0</code> to start at the beginning.
     * @param metadata The media item metadata bundle, or null if none.
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_ENQUEUE} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws UnsupportedOperationException if the route does not support queuing.
     *
     * @see MediaControlIntent#ACTION_ENQUEUE
     * @see #isRemotePlaybackSupported
     * @see #isQueuingSupported
     */
    public void enqueue(Uri contentUri, String mimeType, Bundle metadata,
            long positionMillis, Bundle extras, ItemActionCallback callback) {
        playOrEnqueue(contentUri, mimeType, metadata, positionMillis,
                extras, callback, MediaControlIntent.ACTION_ENQUEUE);
    }

    private void playOrEnqueue(Uri contentUri, String mimeType, Bundle metadata,
            long positionMillis, Bundle extras,
            final ItemActionCallback callback, String action) {
        if (contentUri == null) {
            throw new IllegalArgumentException("contentUri must not be null");
        }
        throwIfRemotePlaybackNotSupported();
        if (action.equals(MediaControlIntent.ACTION_ENQUEUE)) {
            throwIfQueuingNotSupported();
        }

        Intent intent = new Intent(action);
        intent.setDataAndType(contentUri, mimeType);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER,
                mItemStatusPendingIntent);
        if (metadata != null) {
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_METADATA, metadata);
        }
        if (positionMillis != 0) {
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, positionMillis);
        }
        performItemAction(intent, mSessionId, null, extras, callback);
    }

    /**
     * Sends a request to seek to a new position in a media item.
     * <p>
     * Seeks to a new position.  If the queue was previously paused then it
     * remains paused but the item's new position is still remembered.
     * </p><p>
     * The request is issued in the current session.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_SEEK ACTION_SEEK} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param itemId The item id.
     * @param positionMillis The new content position for the item in milliseconds,
     * or <code>0</code> to start at the beginning.
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_SEEK} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     *
     * @see MediaControlIntent#ACTION_SEEK
     * @see #isRemotePlaybackSupported
     */
    public void seek(String itemId, long positionMillis, Bundle extras,
            ItemActionCallback callback) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_SEEK);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, positionMillis);
        performItemAction(intent, mSessionId, itemId, extras, callback);
    }

    /**
     * Sends a request to get the status of a media item.
     * <p>
     * The request is issued in the current session.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_GET_STATUS ACTION_GET_STATUS} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param itemId The item id.
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_GET_STATUS} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     *
     * @see MediaControlIntent#ACTION_GET_STATUS
     * @see #isRemotePlaybackSupported
     */
    public void getStatus(String itemId, Bundle extras, ItemActionCallback callback) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_GET_STATUS);
        performItemAction(intent, mSessionId, itemId, extras, callback);
    }

    /**
     * Sends a request to remove a media item from the queue.
     * <p>
     * The request is issued in the current session.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_REMOVE ACTION_REMOVE} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param itemId The item id.
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_REMOVE} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     * @throws UnsupportedOperationException if the route does not support queuing.
     *
     * @see MediaControlIntent#ACTION_REMOVE
     * @see #isRemotePlaybackSupported
     * @see #isQueuingSupported
     */
    public void remove(String itemId, Bundle extras, ItemActionCallback callback) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        throwIfQueuingNotSupported();
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_REMOVE);
        performItemAction(intent, mSessionId, itemId, extras, callback);
    }

    /**
     * Sends a request to pause media playback.
     * <p>
     * The request is issued in the current session.  If playback is already paused
     * then the request has no effect.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_PAUSE ACTION_PAUSE} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_PAUSE} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     *
     * @see MediaControlIntent#ACTION_PAUSE
     * @see #isRemotePlaybackSupported
     */
    public void pause(Bundle extras, SessionActionCallback callback) {
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
        performSessionAction(intent, mSessionId, extras, callback);
    }

    /**
     * Sends a request to resume (unpause) media playback.
     * <p>
     * The request is issued in the current session.  If playback is not paused
     * then the request has no effect.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_RESUME ACTION_RESUME} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_RESUME} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     *
     * @see MediaControlIntent#ACTION_RESUME
     * @see #isRemotePlaybackSupported
     */
    public void resume(Bundle extras, SessionActionCallback callback) {
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_RESUME);
        performSessionAction(intent, mSessionId, extras, callback);
    }

    /**
     * Sends a request to stop media playback and clear the media playback queue.
     * <p>
     * The request is issued in the current session.  If the queue is already
     * empty then the request has no effect.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_STOP ACTION_STOP} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_STOP} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     *
     * @see MediaControlIntent#ACTION_STOP
     * @see #isRemotePlaybackSupported
     */
    public void stop(Bundle extras, SessionActionCallback callback) {
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
        performSessionAction(intent, mSessionId, extras, callback);
    }

    /**
     * Sends a request to start a new media playback session.
     * <p>
     * The application must wait for the callback to indicate that this request
     * is complete before issuing other requests that affect the session.  If this
     * request is successful then the previous session will be invalidated.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_START_SESSION ACTION_START_SESSION}
     * for more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_START_SESSION} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws UnsupportedOperationException if the route does not support session management.
     *
     * @see MediaControlIntent#ACTION_START_SESSION
     * @see #isRemotePlaybackSupported
     * @see #isSessionManagementSupported
     */
    public void startSession(Bundle extras, SessionActionCallback callback) {
        throwIfSessionManagementNotSupported();

        Intent intent = new Intent(MediaControlIntent.ACTION_START_SESSION);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER,
                mSessionStatusPendingIntent);
        if (mRouteSupportsMessaging) {
            intent.putExtra(MediaControlIntent.EXTRA_MESSAGE_RECEIVER, mMessagePendingIntent);
        }
        performSessionAction(intent, null, extras, callback);
    }

    /**
     * Sends a message.
     * <p>
     * The request is issued in the current session.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_SEND_MESSAGE} for
     * more information about the semantics of this request.
     * </p>
     *
     * @param message A bundle message denoting {@link MediaControlIntent#EXTRA_MESSAGE}.
     * @param callback A callback to invoke when the request has been processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     * @throws UnsupportedOperationException if the route does not support messages.
     *
     * @see MediaControlIntent#ACTION_SEND_MESSAGE
     * @see #isMessagingSupported
     */
    public void sendMessage(Bundle message, SessionActionCallback callback) {
        throwIfNoCurrentSession();
        throwIfMessageNotSupported();

        Intent intent = new Intent(MediaControlIntent.ACTION_SEND_MESSAGE);
        performSessionAction(intent, mSessionId, message, callback);
    }

    /**
     * Sends a request to get the status of the media playback session.
     * <p>
     * The request is issued in the current session.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_GET_SESSION_STATUS
     * ACTION_GET_SESSION_STATUS} for more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_GET_SESSION_STATUS} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     * @throws UnsupportedOperationException if the route does not support session management.
     *
     * @see MediaControlIntent#ACTION_GET_SESSION_STATUS
     * @see #isRemotePlaybackSupported
     * @see #isSessionManagementSupported
     */
    public void getSessionStatus(Bundle extras, SessionActionCallback callback) {
        throwIfSessionManagementNotSupported();
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        performSessionAction(intent, mSessionId, extras, callback);
    }

    /**
     * Sends a request to end the media playback session.
     * <p>
     * The request is issued in the current session.  If this request is successful,
     * the {@link #getSessionId session id property} will be set to null after
     * the callback is invoked.
     * </p><p>
     * Please refer to {@link MediaControlIntent#ACTION_END_SESSION ACTION_END_SESSION}
     * for more information about the semantics of this request.
     * </p>
     *
     * @param extras A bundle of extra arguments to be added to the
     * {@link MediaControlIntent#ACTION_END_SESSION} intent, or null if none.
     * @param callback A callback to invoke when the request has been
     * processed, or null if none.
     *
     * @throws IllegalStateException if there is no current session.
     * @throws UnsupportedOperationException if the route does not support session management.
     *
     * @see MediaControlIntent#ACTION_END_SESSION
     * @see #isRemotePlaybackSupported
     * @see #isSessionManagementSupported
     */
    public void endSession(Bundle extras, SessionActionCallback callback) {
        throwIfSessionManagementNotSupported();
        throwIfNoCurrentSession();

        Intent intent = new Intent(MediaControlIntent.ACTION_END_SESSION);
        performSessionAction(intent, mSessionId, extras, callback);
    }

    private void performItemAction(final Intent intent,
            final String sessionId, final String itemId,
            Bundle extras, final ItemActionCallback callback) {
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        if (sessionId != null) {
            intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, sessionId);
        }
        if (itemId != null) {
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, itemId);
        }
        if (extras != null) {
            intent.putExtras(extras);
        }
        logRequest(intent);
        mRoute.sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                if (data != null) {
                    String sessionIdResult = inferMissingResult(sessionId,
                            data.getString(MediaControlIntent.EXTRA_SESSION_ID));
                    MediaSessionStatus sessionStatus = MediaSessionStatus.fromBundle(
                            data.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
                    String itemIdResult = inferMissingResult(itemId,
                            data.getString(MediaControlIntent.EXTRA_ITEM_ID));
                    MediaItemStatus itemStatus = MediaItemStatus.fromBundle(
                            data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
                    adoptSession(sessionIdResult);
                    if (sessionIdResult != null && itemIdResult != null && itemStatus != null) {
                        if (DEBUG) {
                            Log.d(TAG, "Received result from " + intent.getAction()
                                    + ": data=" + bundleToString(data)
                                    + ", sessionId=" + sessionIdResult
                                    + ", sessionStatus=" + sessionStatus
                                    + ", itemId=" + itemIdResult
                                    + ", itemStatus=" + itemStatus);
                        }
                        callback.onResult(data, sessionIdResult, sessionStatus,
                                itemIdResult, itemStatus);
                        return;
                    }
                }
                handleInvalidResult(intent, callback, data);
            }

            @Override
            public void onError(String error, Bundle data) {
                handleError(intent, callback, error, data);
            }
        });
    }

    private void performSessionAction(final Intent intent, final String sessionId,
            Bundle extras, final SessionActionCallback callback) {
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        if (sessionId != null) {
            intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, sessionId);
        }
        if (extras != null) {
            intent.putExtras(extras);
        }
        logRequest(intent);
        mRoute.sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                if (data != null) {
                    String sessionIdResult = inferMissingResult(sessionId,
                            data.getString(MediaControlIntent.EXTRA_SESSION_ID));
                    MediaSessionStatus sessionStatus = MediaSessionStatus.fromBundle(
                            data.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
                    adoptSession(sessionIdResult);
                    if (sessionIdResult != null) {
                        if (DEBUG) {
                            Log.d(TAG, "Received result from " + intent.getAction()
                                    + ": data=" + bundleToString(data)
                                    + ", sessionId=" + sessionIdResult
                                    + ", sessionStatus=" + sessionStatus);
                        }
                        try {
                            callback.onResult(data, sessionIdResult, sessionStatus);
                        } finally {
                            if (intent.getAction().equals(MediaControlIntent.ACTION_END_SESSION)
                                    && sessionIdResult.equals(mSessionId)) {
                                setSessionId(null);
                            }
                        }
                        return;
                    }
                }
                handleInvalidResult(intent, callback, data);
            }

            @Override
            public void onError(String error, Bundle data) {
                handleError(intent, callback, error, data);
            }
        });
    }

    void adoptSession(String sessionId) {
        if (sessionId != null) {
            setSessionId(sessionId);
        }
    }

    void handleInvalidResult(Intent intent, ActionCallback callback,
            Bundle data) {
        Log.w(TAG, "Received invalid result data from " + intent.getAction()
                + ": data=" + bundleToString(data));
        callback.onError(null, MediaControlIntent.ERROR_UNKNOWN, data);
    }

    void handleError(Intent intent, ActionCallback callback,
            String error, Bundle data) {
        final int code;
        if (data != null) {
            code = data.getInt(MediaControlIntent.EXTRA_ERROR_CODE,
                    MediaControlIntent.ERROR_UNKNOWN);
        } else {
            code = MediaControlIntent.ERROR_UNKNOWN;
        }
        if (DEBUG) {
            Log.w(TAG, "Received error from " + intent.getAction()
                    + ": error=" + error
                    + ", code=" + code
                    + ", data=" + bundleToString(data));
        }
        callback.onError(error, code, data);
    }

    private void detectFeatures() {
        mRouteSupportsRemotePlayback = routeSupportsAction(MediaControlIntent.ACTION_PLAY)
                && routeSupportsAction(MediaControlIntent.ACTION_SEEK)
                && routeSupportsAction(MediaControlIntent.ACTION_GET_STATUS)
                && routeSupportsAction(MediaControlIntent.ACTION_PAUSE)
                && routeSupportsAction(MediaControlIntent.ACTION_RESUME)
                && routeSupportsAction(MediaControlIntent.ACTION_STOP);
        mRouteSupportsQueuing = mRouteSupportsRemotePlayback
                && routeSupportsAction(MediaControlIntent.ACTION_ENQUEUE)
                && routeSupportsAction(MediaControlIntent.ACTION_REMOVE);
        mRouteSupportsSessionManagement = mRouteSupportsRemotePlayback
                && routeSupportsAction(MediaControlIntent.ACTION_START_SESSION)
                && routeSupportsAction(MediaControlIntent.ACTION_GET_SESSION_STATUS)
                && routeSupportsAction(MediaControlIntent.ACTION_END_SESSION);
        mRouteSupportsMessaging = doesRouteSupportMessaging();
    }

    private boolean routeSupportsAction(String action) {
        return mRoute.supportsControlAction(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK, action);
    }

    private boolean doesRouteSupportMessaging() {
        for (IntentFilter filter : mRoute.getControlFilters()) {
            if (filter.hasAction(MediaControlIntent.ACTION_SEND_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    private void throwIfRemotePlaybackNotSupported() {
        if (!mRouteSupportsRemotePlayback) {
            throw new UnsupportedOperationException("The route does not support remote playback.");
        }
    }

    private void throwIfQueuingNotSupported() {
        if (!mRouteSupportsQueuing) {
            throw new UnsupportedOperationException("The route does not support queuing.");
        }
    }

    private void throwIfSessionManagementNotSupported() {
        if (!mRouteSupportsSessionManagement) {
            throw new UnsupportedOperationException("The route does not support "
                    + "session management.");
        }
    }

    private void throwIfMessageNotSupported() {
        if (!mRouteSupportsMessaging) {
            throw new UnsupportedOperationException("The route does not support message.");
        }
    }

    private void throwIfNoCurrentSession() {
        if (mSessionId == null) {
            throw new IllegalStateException("There is no current session.");
        }
    }

    static String inferMissingResult(String request, String result) {
        if (result == null) {
            // Result is missing.
            return request;
        }
        if (request == null || request.equals(result)) {
            // Request didn't specify a value or result matches request.
            return result;
        }
        // Result conflicts with request.
        return null;
    }

    private static void logRequest(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Sending request: " + intent);
        }
    }

    static String bundleToString(Bundle bundle) {
        if (bundle != null) {
            bundle.size(); // force bundle to be unparcelled
            return bundle.toString();
        }
        return "null";
    }

    private final class ActionReceiver extends BroadcastReceiver {
        public static final String ACTION_ITEM_STATUS_CHANGED =
                "androidx.mediarouter.media.actions.ACTION_ITEM_STATUS_CHANGED";
        public static final String ACTION_SESSION_STATUS_CHANGED =
                "androidx.mediarouter.media.actions.ACTION_SESSION_STATUS_CHANGED";
        public static final String ACTION_MESSAGE_RECEIVED =
                "androidx.mediarouter.media.actions.ACTION_MESSAGE_RECEIVED";

        ActionReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            if (sessionId == null || !sessionId.equals(mSessionId)) {
                Log.w(TAG, "Discarding spurious status callback "
                        + "with missing or invalid session id: sessionId=" + sessionId);
                return;
            }

            MediaSessionStatus sessionStatus = MediaSessionStatus.fromBundle(
                    intent.getBundleExtra(MediaControlIntent.EXTRA_SESSION_STATUS));
            String action = intent.getAction();
            if (action.equals(ACTION_ITEM_STATUS_CHANGED)) {
                String itemId = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
                if (itemId == null) {
                    Log.w(TAG, "Discarding spurious status callback with missing item id.");
                    return;
                }

                MediaItemStatus itemStatus = MediaItemStatus.fromBundle(
                        intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_STATUS));
                if (itemStatus == null) {
                    Log.w(TAG, "Discarding spurious status callback with missing item status.");
                    return;
                }

                if (DEBUG) {
                    Log.d(TAG, "Received item status callback: sessionId=" + sessionId
                            + ", sessionStatus=" + sessionStatus
                            + ", itemId=" + itemId
                            + ", itemStatus=" + itemStatus);
                }

                if (mStatusCallback != null) {
                    mStatusCallback.onItemStatusChanged(intent.getExtras(),
                            sessionId, sessionStatus, itemId, itemStatus);
                }
            } else if (action.equals(ACTION_SESSION_STATUS_CHANGED)) {
                if (sessionStatus == null) {
                    Log.w(TAG, "Discarding spurious media status callback with "
                            +"missing session status.");
                    return;
                }

                if (DEBUG) {
                    Log.d(TAG, "Received session status callback: sessionId=" + sessionId
                            + ", sessionStatus=" + sessionStatus);
                }

                if (mStatusCallback != null) {
                    mStatusCallback.onSessionStatusChanged(intent.getExtras(),
                            sessionId, sessionStatus);
                }
            } else if (action.equals(ACTION_MESSAGE_RECEIVED)) {
                if (DEBUG) {
                    Log.d(TAG, "Received message callback: sessionId=" + sessionId);
                }

                if (mOnMessageReceivedListener != null) {
                    mOnMessageReceivedListener.onMessageReceived(sessionId,
                            intent.getBundleExtra(MediaControlIntent.EXTRA_MESSAGE));
                }
            }
        }
    }

    /**
     * A callback that will receive media status updates.
     */
    public static abstract class StatusCallback {
        /**
         * Called when the status of a media item changes.
         *
         * @param data The result data bundle.
         * @param sessionId The session id.
         * @param sessionStatus The session status, or null if unknown.
         * @param itemId The item id.
         * @param itemStatus The item status.
         */
        public void onItemStatusChanged(Bundle data,
                String sessionId, MediaSessionStatus sessionStatus,
                String itemId, MediaItemStatus itemStatus) {
        }

        /**
         * Called when the status of a media session changes.
         *
         * @param data The result data bundle.
         * @param sessionId The session id.
         * @param sessionStatus The session status, or null if unknown.
         */
        public void onSessionStatusChanged(Bundle data,
                String sessionId, MediaSessionStatus sessionStatus) {
        }

        /**
         * Called when the session of the remote playback client changes.
         *
         * @param sessionId The new session id.
         */
        public void onSessionChanged(String sessionId) {
        }
    }

    /**
     * Base callback type for remote playback requests.
     */
    public static abstract class ActionCallback {
        /**
         * Called when a media control request fails.
         *
         * @param error A localized error message which may be shown to the user, or null
         * if the cause of the error is unclear.
         * @param code The error code, or {@link MediaControlIntent#ERROR_UNKNOWN} if unknown.
         * @param data The error data bundle, or null if none.
         */
        public void onError(String error, int code, Bundle data) {
        }
    }

    /**
     * Callback for remote playback requests that operate on items.
     */
    public static abstract class ItemActionCallback extends ActionCallback {
        /**
         * Called when the request succeeds.
         *
         * @param data The result data bundle.
         * @param sessionId The session id.
         * @param sessionStatus The session status, or null if unknown.
         * @param itemId The item id.
         * @param itemStatus The item status.
         */
        public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
                String itemId, MediaItemStatus itemStatus) {
        }
    }

    /**
     * Callback for remote playback requests that operate on sessions.
     */
    public static abstract class SessionActionCallback extends ActionCallback {
        /**
         * Called when the request succeeds.
         *
         * @param data The result data bundle.
         * @param sessionId The session id.
         * @param sessionStatus The session status, or null if unknown.
         */
        public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
        }
    }

    /**
     * A callback that will receive messages from media sessions.
     */
    public interface OnMessageReceivedListener {
        /**
         * Called when a message received.
         *
         * @param sessionId The session id.
         * @param message A bundle message denoting {@link MediaControlIntent#EXTRA_MESSAGE}.
         */
        void onMessageReceived(String sessionId, Bundle message);
    }
}
