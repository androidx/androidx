/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.service;

import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_FOR_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_KEY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_VALUE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN_DELAYED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_INCLUDE_METADATA;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_INVALID;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ROOT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_METADATA;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY_FOR_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY_FOR_NO_RESULT;
import static android.support.mediacompat.testlib.MediaSessionConstants.ROOT_HINT_EXTRA_KEY_CALLER_PKG;
import static android.support.mediacompat.testlib.MediaSessionConstants.ROOT_HINT_EXTRA_KEY_CALLER_UID;
import static android.support.mediacompat.testlib.MediaSessionConstants.SESSION_EVENT_NOTIFY_CALLBACK_METHOD_NAME_PREFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.util.IntentUtil;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of {@link MediaBrowserServiceCompat}.
 */
public class StubMediaBrowserServiceCompat extends MediaBrowserServiceCompat {
    private static final String TAG = "StubMBSC";

    public static StubMediaBrowserServiceCompat sInstance;

    public static MediaSessionCompat sSession;
    public static MediaSessionCompatCallback sSessionCallback;
    private Bundle mExtras;
    private Result<List<MediaItem>> mPendingLoadChildrenResult;
    private Result<MediaItem> mPendingLoadItemResult;
    private Bundle mPendingRootHints;
    private RemoteUserInfo mClientAppRemoteUserInfo;

    public Bundle mCustomActionExtras;
    public Result<Bundle> mCustomActionResult;

    private String mExpectedCallerPackageName;
    private int mExpectedCallerUid;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        sSession = new MediaSessionCompat(this, "StubMediaBrowserServiceCompat");
        sSessionCallback = new MediaSessionCompatCallback(this, sSession);
        sSession.setCallback(sSessionCallback);
        sSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);
        setSessionToken(sSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sSessionCallback != null) {
            sSessionCallback = null;
        }
        if (sSession != null) {
            sSession.release();
            sSession = null;
        }
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        if (!TextUtils.equals(clientPackageName, IntentUtil.CLIENT_PACKAGE_NAME)) {
            return null;
        }
        mExtras = new Bundle();
        mExtras.putString(EXTRAS_KEY, EXTRAS_VALUE);
        mClientAppRemoteUserInfo = getCurrentBrowserInfo();
        if (rootHints.containsKey(ROOT_HINT_EXTRA_KEY_CALLER_PKG)) {
            mExpectedCallerPackageName = (21 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 24)
                    ? RemoteUserInfo.LEGACY_CONTROLLER
                    : rootHints.getString(ROOT_HINT_EXTRA_KEY_CALLER_PKG);
        }
        if (rootHints.containsKey(ROOT_HINT_EXTRA_KEY_CALLER_UID)) {
            mExpectedCallerUid = (21 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 28)
                    ? RemoteUserInfo.UNKNOWN_UID
                    : rootHints.getInt(ROOT_HINT_EXTRA_KEY_CALLER_UID);
        }
        return new BrowserRoot(MEDIA_ID_ROOT, mExtras);
    }

    @Override
    public void onLoadChildren(final String parentId, final Result<List<MediaItem>> result) {
        // Calling getBrowserRootHints()/getCurrentBrowserInfo() should not fail.
        getBrowserRootHints();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(mClientAppRemoteUserInfo, info);
        }
        List<MediaItem> mediaItems = new ArrayList<>();
        if (MEDIA_ID_ROOT.equals(parentId)) {
            Bundle rootHints = getBrowserRootHints();
            for (String id : MEDIA_ID_CHILDREN) {
                mediaItems.add(createMediaItem(id));
            }
            result.sendResult(mediaItems);
        } else if (MEDIA_ID_CHILDREN_DELAYED.equals(parentId)) {
            assertNull(mPendingLoadChildrenResult);
            mPendingLoadChildrenResult = result;
            mPendingRootHints = getBrowserRootHints();
            result.detach();
        } else if (MEDIA_ID_INVALID.equals(parentId)) {
            result.sendResult(null);
        }
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result,
            @NonNull Bundle options) {
        // Calling getBrowserRootHints()/getCurrentBrowserInfo() should not fail.
        getBrowserRootHints();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(mClientAppRemoteUserInfo, info);
        }
        if (MEDIA_ID_INCLUDE_METADATA.equals(parentId)) {
            // Test unparcelling the Bundle.
            MediaMetadataCompat metadata = options.getParcelable(MEDIA_METADATA);
            if (metadata == null) {
                super.onLoadChildren(parentId, result, options);
            } else {
                List<MediaItem> mediaItems = new ArrayList<>();
                mediaItems.add(new MediaItem(metadata.getDescription(), MediaItem.FLAG_PLAYABLE));
                result.sendResult(mediaItems);
            }
        } else {
            super.onLoadChildren(parentId, result, options);
        }
    }

    @Override
    public void onLoadItem(String itemId, Result<MediaItem> result) {
        // Calling getBrowserRootHints()/getCurrentBrowserInfo() should not fail.
        getBrowserRootHints();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(mClientAppRemoteUserInfo, info);
        }
        if (MEDIA_ID_CHILDREN_DELAYED.equals(itemId)) {
            mPendingLoadItemResult = result;
            mPendingRootHints = getBrowserRootHints();
            result.detach();
            return;
        }

        if (MEDIA_ID_INVALID.equals(itemId)) {
            result.sendResult(null);
            return;
        }

        for (String id : MEDIA_ID_CHILDREN) {
            if (id.equals(itemId)) {
                result.sendResult(createMediaItem(id));
                return;
            }
        }

        // Test the case where onLoadItem is not implemented.
        super.onLoadItem(itemId, result);
    }

    @Override
    public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
        // Calling getBrowserRootHints()/getCurrentBrowserInfo() should not fail.
        getBrowserRootHints();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(mClientAppRemoteUserInfo, info);
        }
        if (SEARCH_QUERY_FOR_NO_RESULT.equals(query)) {
            result.sendResult(Collections.<MediaItem>emptyList());
        } else if (SEARCH_QUERY_FOR_ERROR.equals(query)) {
            result.sendResult(null);
        } else if (SEARCH_QUERY.equals(query)) {
            List<MediaItem> items = new ArrayList<>();
            for (String id : MEDIA_ID_CHILDREN) {
                if (id.contains(query)) {
                    items.add(createMediaItem(id));
                }
            }
            result.sendResult(items);
        }
    }

    @Override
    public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
        // Calling getBrowserRootHints()/getCurrentBrowserInfo() should not fail.
        getBrowserRootHints();
        RemoteUserInfo info = getCurrentBrowserInfo();
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(mClientAppRemoteUserInfo, info);
        }
        mCustomActionResult = result;
        mCustomActionExtras = extras;
        if (CUSTOM_ACTION_FOR_ERROR.equals(action)) {
            result.sendError(null);
        } else if (CUSTOM_ACTION.equals(action)) {
            result.detach();
        }
    }

    public void sendDelayedNotifyChildrenChanged() {
        if (mPendingLoadChildrenResult != null) {
            mPendingLoadChildrenResult.sendResult(Collections.<MediaItem>emptyList());
            mPendingRootHints = null;
            mPendingLoadChildrenResult = null;
        }
    }

    public void sendDelayedItemLoaded() {
        if (mPendingLoadItemResult != null) {
            mPendingLoadItemResult.sendResult(new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(MEDIA_ID_CHILDREN_DELAYED).setExtras(mPendingRootHints).build(),
                    MediaItem.FLAG_BROWSABLE));
            mPendingRootHints = null;
            mPendingLoadItemResult = null;
        }
    }

    private MediaItem createMediaItem(String id) {
        return new MediaItem(new MediaDescriptionCompat.Builder().setMediaId(id).build(),
                MediaItem.FLAG_BROWSABLE);
    }

    public class MediaSessionCompatCallback extends MediaSessionCompat.Callback {
        Context mContext;
        MediaSessionCompat mSession;

        public MediaSessionCompatCallback(Context context, MediaSessionCompat session) {
            mContext = context;
            mSession = session;
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            notifyCurrentControllerInfo("onCommand");
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            notifyCurrentControllerInfo("onMediaButtonEvent");
            return true;
        }

        @Override
        public void onPrepare() {
            notifyCurrentControllerInfo("onPrepare");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            notifyCurrentControllerInfo("onPrepareFromMediaId");
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            notifyCurrentControllerInfo("onPrepareFromSearch");
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            notifyCurrentControllerInfo("onPrepareFromUri");
        }

        @Override
        public void onPlay() {
            notifyCurrentControllerInfo("onPlay");
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            notifyCurrentControllerInfo("onPlayFromMediaId");
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            notifyCurrentControllerInfo("onPlayFromSearch");
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            notifyCurrentControllerInfo("onPlayFromUri");
        }

        @Override
        public void onSkipToQueueItem(long id) {
            notifyCurrentControllerInfo("onSkipToQueueItem");
        }

        @Override
        public void onPause() {
            notifyCurrentControllerInfo("onPause");
        }

        @Override
        public void onSkipToNext() {
            notifyCurrentControllerInfo("onSkipToNext");
        }

        @Override
        public void onSkipToPrevious() {
            notifyCurrentControllerInfo("onSkipToPrevious");
        }

        @Override
        public void onFastForward() {
            notifyCurrentControllerInfo("onFastForward");
        }

        @Override
        public void onRewind() {
            notifyCurrentControllerInfo("onRewind");
        }

        @Override
        public void onStop() {
            notifyCurrentControllerInfo("onStop");
        }

        @Override
        public void onSeekTo(long pos) {
            notifyCurrentControllerInfo("onSeekTo");
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            notifyCurrentControllerInfo("onSetRating");
        }

        @Override
        public void onSetRating(RatingCompat rating, Bundle extras) {
            notifyCurrentControllerInfo("onSetRating");
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            notifyCurrentControllerInfo("onSetPlaybackSpeed");
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            notifyCurrentControllerInfo("onSetCaptioningEnabled");
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            notifyCurrentControllerInfo("onSetRepeatMode");
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            notifyCurrentControllerInfo("onSetShuffleMode");
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            notifyCurrentControllerInfo("onCustomAction");
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            notifyCurrentControllerInfo("onAddQueueItem");
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            notifyCurrentControllerInfo("onAddQueueItem");
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            notifyCurrentControllerInfo("onRemoveQueueItem");
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onRemoveQueueItemAt(int index) {
            notifyCurrentControllerInfo("onRemoveQueueItemAt");
        }

        private void notifyCurrentControllerInfo(String callbackMethodName) {
            RemoteUserInfo remoteUserInfo = mSession.getCurrentControllerInfo();
            int callerUid = remoteUserInfo.getUid();
            String callerPkg = remoteUserInfo.getPackageName();

            if (callerUid != mExpectedCallerUid
                    || !TextUtils.equals(callerPkg, mExpectedCallerPackageName)) {
                Log.w(TAG,
                        "Ignore calls to the session from unexpected source. Expected uid="
                                + mExpectedCallerUid + ", pkg=" + mExpectedCallerPackageName
                                + ", but was uid=" + callerUid + ", pkg=" + callerPkg);
                return;
            }

            // Send callback method name via encoded event string.
            // Extra bundle of sendSessionEvent() cannot be used, because it would be sent to the
            // fwk MediaController in API 21-22.
            String event = SESSION_EVENT_NOTIFY_CALLBACK_METHOD_NAME_PREFIX + callbackMethodName;
            mSession.sendSessionEvent(event, /* extras= */ null);
        }
    }
}
