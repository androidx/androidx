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

import static org.junit.Assert.assertNull;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of {@link android.support.v4.media.MediaBrowserServiceCompat}.
 */
public class StubMediaBrowserServiceCompat extends MediaBrowserServiceCompat {

    public static StubMediaBrowserServiceCompat sInstance;

    public static MediaSessionCompat sSession;
    private Bundle mExtras;
    private Result<List<MediaItem>> mPendingLoadChildrenResult;
    private Result<MediaItem> mPendingLoadItemResult;
    private Bundle mPendingRootHints;

    public Bundle mCustomActionExtras;
    public Result<Bundle> mCustomActionResult;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        sSession = new MediaSessionCompat(this, "StubMediaBrowserServiceCompat");
        setSessionToken(sSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sSession.release();
        sSession = null;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        mExtras = new Bundle();
        mExtras.putString(EXTRAS_KEY, EXTRAS_VALUE);
        return new BrowserRoot(MEDIA_ID_ROOT, mExtras);
    }

    @Override
    public void onLoadChildren(final String parentId, final Result<List<MediaItem>> result) {
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
}
