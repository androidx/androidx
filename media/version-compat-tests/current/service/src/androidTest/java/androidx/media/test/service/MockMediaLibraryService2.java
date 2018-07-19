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

package androidx.media.test.service;

import static androidx.media.test.lib.MediaBrowser2Constants.EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.GET_CHILDREN_RESULT;
import static androidx.media.test.lib.MediaBrowser2Constants.MEDIA_ID_GET_ITEM;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_ITEM_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.PARENT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants.PARENT_ID_ERROR;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY_EMPTY_RESULT;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY_TAKES_TIME;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_RESULT;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_RESULT_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_TIME_IN_MS;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;

import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.MediaItem2;
import androidx.media2.MediaLibraryService2;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MockMediaLibraryService2 extends MediaLibraryService2 {
    // Keep in sync with the AndroidManifest.xml
    public static final String ID = "TestLibrary";

    private static final String TAG = "MockMediaLibrarySvc2";

    MediaLibrarySession mSession;
    SyncHandler mHandler;
    HandlerThread mHandlerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new SyncHandler(mHandlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= 18) {
            mHandler.getLooper().quitSafely();
        } else {
            mHandler.getLooper().quit();
        }
        mHandler = null;
    }

    @Override
    public MediaLibrarySession onCreateSession(String sessionId) {
        final MockPlayerConnector player = new MockPlayerConnector(1);
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                mHandler.post(runnable);
            }
        };

        mSession = new MediaLibrarySession.Builder(MockMediaLibraryService2.this, executor,
                new TestLibrarySessionCallback()).setPlayer(player).setId(sessionId).build();
        return mSession;
    }

    /**
     * This changes the visibility of {@link Service#attachBaseContext(Context)} to public.
     * This is a workaround for creating {@link MediaLibrarySession} without starting a service.
     */
    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private class TestLibrarySessionCallback extends MediaLibrarySessionCallback {
        @Override
        public LibraryRoot onGetLibraryRoot(MediaLibrarySession session, ControllerInfo controller,
                Bundle rootHints) {
            return new LibraryRoot(ROOT_ID, EXTRAS);
        }

        @Override
        public MediaItem2 onGetItem(MediaLibrarySession session, ControllerInfo controller,
                String mediaId) {
            if (MEDIA_ID_GET_ITEM.equals(mediaId)) {
                return createMediaItem(mediaId);
            } else {
                return null;
            }
        }

        @Override
        public List<MediaItem2> onGetChildren(MediaLibrarySession session,
                ControllerInfo controller, String parentId, int page, int pageSize, Bundle extras) {
            if (PARENT_ID.equals(parentId)) {
                return getPaginatedResult(GET_CHILDREN_RESULT, page, pageSize);
            } else if (PARENT_ID_ERROR.equals(parentId)) {
                return null;
            }
            // Includes the case of PARENT_ID_NO_CHILDREN.
            return new ArrayList<>();
        }

        @Override
        public void onSearch(MediaLibrarySession session, final ControllerInfo controllerInfo,
                final String query, final Bundle extras) {
            if (SEARCH_QUERY.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, SEARCH_RESULT_COUNT,
                        extras);
            } else if (SEARCH_QUERY_TAKES_TIME.equals(query)) {
                // Searching takes some time. Notify after 5 seconds.
                Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                    @Override
                    public void run() {
                        mSession.notifySearchResultChanged(
                                controllerInfo, query, SEARCH_RESULT_COUNT, extras);
                    }
                }, SEARCH_TIME_IN_MS, TimeUnit.MILLISECONDS);
            } else if (SEARCH_QUERY_EMPTY_RESULT.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, 0, extras);
            } else {
                // TODO: For the error case, how should we notify the browser?
            }
        }

        @Override
        public List<MediaItem2> onGetSearchResult(MediaLibrarySession session,
                ControllerInfo controllerInfo, String query, int page, int pageSize,
                Bundle extras) {
            if (SEARCH_QUERY.equals(query)) {
                return getPaginatedResult(SEARCH_RESULT, page, pageSize);
            } else {
                return null;
            }
        }

        @Override
        public void onSubscribe(MediaLibrarySession session,
                ControllerInfo controller, String parentId, Bundle extras) {
            final String unsubscribedId = "unsubscribedId";
            switch (parentId) {
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL:
                    mSession.notifyChildrenChanged(
                            parentId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_EXTRAS);
                    break;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE:
                    mSession.notifyChildrenChanged(
                            MediaTestUtils.getTestControllerInfo(mSession),
                            parentId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_EXTRAS);
                    break;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID:
                    mSession.notifyChildrenChanged(
                            unsubscribedId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_EXTRAS);
                    break;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID:
                    mSession.notifyChildrenChanged(
                            MediaTestUtils.getTestControllerInfo(mSession),
                            unsubscribedId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_EXTRAS);
                    break;
            }
        }
    }

    private List<MediaItem2> getPaginatedResult(List<String> items, int page, int pageSize) {
        if (items == null) {
            return null;
        } else if (items.size() == 0) {
            return new ArrayList<>();
        }

        final int totalItemCount = items.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(page * pageSize, totalItemCount);

        List<String> paginatedMediaIdList = new ArrayList<>();
        try {
            // The case of (fromIndex >= totalItemCount) will throw exception below.
            paginatedMediaIdList = items.subList(fromIndex, toIndex);
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            Log.d(TAG, "Result is empty for given pagination arguments: totalItemCount="
                    + totalItemCount + ", page=" + page + ", pageSize=" + pageSize, ex);
        }

        // Create a list of MediaItem2 from the list of media IDs.
        List<MediaItem2> result = new ArrayList<>();
        for (int i = 0; i < paginatedMediaIdList.size(); i++) {
            result.add(createMediaItem(paginatedMediaIdList.get(i)));
        }
        return result;
    }

    private MediaItem2 createMediaItem(String mediaId) {
        return new MediaItem2.Builder(0 /* Flags */)
                .setMediaId(mediaId)
                .setMetadata(new MediaMetadata2.Builder()
                        .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId)
                        .build())
                .build();
    }
}
