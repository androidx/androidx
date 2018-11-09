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

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static androidx.media.test.lib.MediaBrowser2Constants.CUSTOM_ACTION;
import static androidx.media.test.lib.MediaBrowser2Constants.CUSTOM_ACTION_ASSERT_PARAMS;
import static androidx.media.test.lib.MediaBrowser2Constants.CUSTOM_ACTION_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.GET_CHILDREN_RESULT;
import static androidx.media.test.lib.MediaBrowser2Constants.LONG_LIST_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.MEDIA_ID_GET_INVALID_ITEM;
import static androidx.media.test.lib.MediaBrowser2Constants.MEDIA_ID_GET_ITEM;
import static androidx.media.test.lib.MediaBrowser2Constants.MEDIA_ID_GET_NULL_ITEM;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_ITEM_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.PARENT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants.PARENT_ID_ERROR;
import static androidx.media.test.lib.MediaBrowser2Constants.PARENT_ID_LONG_LIST;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY_EMPTY_RESULT;
import static androidx.media.test.lib.MediaBrowser2Constants.SEARCH_QUERY_LONG_LIST;
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
import static androidx.media.test.service.MediaTestUtils.assertLibraryParamsEquals;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaMetadata2.BROWSABLE_TYPE_MIXED;
import static androidx.media2.MediaMetadata2.BROWSABLE_TYPE_NONE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata2.METADATA_KEY_PLAYABLE;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.media.test.lib.TestUtils;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.MediaItem2;
import androidx.media2.MediaLibraryService2;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MockMediaLibraryService2 extends MediaLibraryService2 {
    /**
     * ID of the session that this service will create.
     */
    public static final String ID = "TestLibrary";
    public static final MediaItem2 ROOT_ITEM = new MediaItem2.Builder()
            .setMetadata(new MediaMetadata2.Builder()
                    .putString(METADATA_KEY_MEDIA_ID, ROOT_ID)
                    .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED)
                    .putLong(METADATA_KEY_PLAYABLE, 0)
                    .build()).build();
    public static final LibraryParams ROOT_PARAMS = new LibraryParams.Builder()
            .setExtras(ROOT_EXTRAS).build();
    private static final LibraryParams NOTIFY_CHILDREN_CHANGED_PARAMS = new LibraryParams.Builder()
            .setExtras(NOTIFY_CHILDREN_CHANGED_EXTRAS).build();

    private static final String TAG = "MockMediaLibrarySvc2";

    @GuardedBy("MockMediaLibraryService2.class")
    private static boolean sAssertLibraryParams;
    @GuardedBy("MockMediaLibraryService2.class")
    private static LibraryParams sExpectedParams;

    MediaLibrarySession mSession;
    SyncHandler mHandler;
    HandlerThread mHandlerThread;

    @Override
    public void onCreate() {
        TestServiceRegistry.getInstance().setServiceInstance(this);
        super.onCreate();
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new SyncHandler(mHandlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (MockMediaLibraryService2.class) {
            sAssertLibraryParams = false;
            sExpectedParams = null;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            mHandler.getLooper().quitSafely();
        } else {
            mHandler.getLooper().quit();
        }
        mHandler = null;
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Override
    public MediaLibrarySession onGetSession() {
        TestServiceRegistry registry = TestServiceRegistry.getInstance();
        TestServiceRegistry.OnGetSessionHandler onGetSessionHandler =
                registry.getOnGetSessionHandler();
        if (onGetSessionHandler != null) {
            return (MediaLibrarySession) onGetSessionHandler.onGetSession();
        }

        final MockPlayer player = new MockPlayer(1);
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                mHandler.post(runnable);
            }
        };

        MediaLibrarySessionCallback callback = registry.getSessionCallback();
        mSession = new MediaLibrarySession.Builder(MockMediaLibraryService2.this, player, executor,
                callback != null ? callback : new TestLibrarySessionCallback())
                .setId(ID)
                .build();
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

    public static void setAssertLibraryParams(LibraryParams expectedParams) {
        synchronized (MockMediaLibraryService2.class) {
            sAssertLibraryParams = true;
            sExpectedParams = expectedParams;
        }
    }

    private class TestLibrarySessionCallback extends MediaLibrarySessionCallback {

        @Override
        public SessionCommandGroup2 onConnect(MediaSession2 session,
                ControllerInfo controller) {
            if (!CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                return null;
            }
            SessionCommandGroup2 group = super.onConnect(session, controller);
            group.addCommand(new SessionCommand2(CUSTOM_ACTION, null));
            group.addCommand(new SessionCommand2(CUSTOM_ACTION_ASSERT_PARAMS, null));
            return group;
        }

        @Override
        public LibraryResult onGetLibraryRoot(MediaLibrarySession session,
                ControllerInfo controller, LibraryParams params) {
            assertLibraryParams(params);
            return new LibraryResult(RESULT_CODE_SUCCESS, ROOT_ITEM, ROOT_PARAMS);
        }

        @Override
        public LibraryResult onGetItem(MediaLibrarySession session, ControllerInfo controller,
                String mediaId) {
            switch (mediaId) {
                case MEDIA_ID_GET_ITEM:
                    return new LibraryResult(RESULT_CODE_SUCCESS, createMediaItem(mediaId), null);
                case MEDIA_ID_GET_NULL_ITEM:
                    return new LibraryResult(RESULT_CODE_SUCCESS);
                case MEDIA_ID_GET_INVALID_ITEM:
                    // No browsable
                    MediaMetadata2 metadata =  new MediaMetadata2.Builder()
                            .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId)
                            .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
                            .build();
                    return new LibraryResult(RESULT_CODE_SUCCESS,
                            new MediaItem2.Builder().setMetadata(metadata).build(), null);
            }
            return new LibraryResult(RESULT_CODE_BAD_VALUE);
        }

        @Override
        public LibraryResult onGetChildren(MediaLibrarySession session,
                ControllerInfo controller, String parentId, int page, int pageSize,
                LibraryParams params) {
            assertLibraryParams(params);
            if (PARENT_ID.equals(parentId)) {
                return new LibraryResult(RESULT_CODE_SUCCESS,
                        getPaginatedResult(GET_CHILDREN_RESULT, page, pageSize), null);
            } else if (PARENT_ID_LONG_LIST.equals(parentId)) {
                List<MediaItem2> list = new ArrayList<>(LONG_LIST_COUNT);
                MediaItem2.Builder builder = new MediaItem2.Builder();
                for (int i = 0; i < LONG_LIST_COUNT; i++) {
                    list.add(builder
                            .setMetadata(new MediaMetadata2.Builder()
                                    .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID,
                                            TestUtils.getMediaIdInDummyList(i))
                                    .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE,
                                            MediaMetadata2.BROWSABLE_TYPE_NONE)
                                    .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
                                    .build())
                            .build());
                }
                return new LibraryResult(RESULT_CODE_SUCCESS, list, null);
            } else if (PARENT_ID_ERROR.equals(parentId)) {
                return new LibraryResult(RESULT_CODE_BAD_VALUE);
            }
            // Includes the case of PARENT_ID_NO_CHILDREN.
            return new LibraryResult(RESULT_CODE_SUCCESS, new ArrayList<MediaItem2>(), null);
        }

        @Override
        public int onSearch(MediaLibrarySession session,
                final ControllerInfo controllerInfo, final String query,
                final LibraryParams params) {
            assertLibraryParams(params);
            if (SEARCH_QUERY.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, SEARCH_RESULT_COUNT,
                        params);
            } else if (SEARCH_QUERY_LONG_LIST.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, LONG_LIST_COUNT, params);
            } else if (SEARCH_QUERY_TAKES_TIME.equals(query)) {
                // Searching takes some time. Notify after 5 seconds.
                Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                    @Override
                    public void run() {
                        mSession.notifySearchResultChanged(
                                controllerInfo, query, SEARCH_RESULT_COUNT, params);
                    }
                }, SEARCH_TIME_IN_MS, TimeUnit.MILLISECONDS);
            } else {
                // SEARCH_QUERY_EMPTY_RESULT and SEARCH_QUERY_ERROR will be handled here.
                mSession.notifySearchResultChanged(controllerInfo, query, 0, params);
            }
            return RESULT_CODE_SUCCESS;
        }

        @Override
        public LibraryResult onGetSearchResult(MediaLibrarySession session,
                ControllerInfo controllerInfo, String query, int page, int pageSize,
                LibraryParams params) {
            assertLibraryParams(params);
            if (SEARCH_QUERY.equals(query)) {
                return new LibraryResult(RESULT_CODE_SUCCESS,
                        getPaginatedResult(SEARCH_RESULT, page, pageSize), null);
            } else if (SEARCH_QUERY_LONG_LIST.equals(query)) {
                List<MediaItem2> list = new ArrayList<>(LONG_LIST_COUNT);
                MediaItem2.Builder builder = new MediaItem2.Builder();
                for (int i = 0; i < LONG_LIST_COUNT; i++) {
                    list.add(createMediaItem(TestUtils.getMediaIdInDummyList(i)));
                }
                return new LibraryResult(RESULT_CODE_SUCCESS, list, null);
            } else if (SEARCH_QUERY_EMPTY_RESULT.equals(query)) {
                return new LibraryResult(RESULT_CODE_SUCCESS, new ArrayList<MediaItem2>(), null);
            } else {
                // SEARCH_QUERY_ERROR will be handled here.
                return new LibraryResult(RESULT_CODE_BAD_VALUE);
            }
        }

        @Override
        public int onSubscribe(MediaLibrarySession session,
                ControllerInfo controller, String parentId, LibraryParams params) {
            assertLibraryParams(params);
            final String unsubscribedId = "unsubscribedId";
            switch (parentId) {
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL:
                    mSession.notifyChildrenChanged(
                            parentId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_PARAMS);
                    return RESULT_CODE_SUCCESS;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE:
                    mSession.notifyChildrenChanged(
                            MediaTestUtils.getTestControllerInfo(mSession),
                            parentId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_PARAMS);
                    return RESULT_CODE_SUCCESS;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID:
                    mSession.notifyChildrenChanged(
                            unsubscribedId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_PARAMS);
                    return RESULT_CODE_SUCCESS;
                case SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID:
                    mSession.notifyChildrenChanged(
                            MediaTestUtils.getTestControllerInfo(mSession),
                            unsubscribedId,
                            NOTIFY_CHILDREN_CHANGED_ITEM_COUNT,
                            NOTIFY_CHILDREN_CHANGED_PARAMS);
                    return RESULT_CODE_SUCCESS;
            }
            return RESULT_CODE_BAD_VALUE;
        }

        @Override
        public MediaSession2.SessionResult onCustomCommand(MediaSession2 session,
                ControllerInfo controller, SessionCommand2 customCommand, Bundle args) {
            switch (customCommand.getCustomCommand()) {
                case CUSTOM_ACTION:
                    return new MediaSession2.SessionResult(
                            RESULT_CODE_SUCCESS, CUSTOM_ACTION_EXTRAS);
                case CUSTOM_ACTION_ASSERT_PARAMS:
                    LibraryParams params = ParcelUtils.getVersionedParcelable(args,
                            CUSTOM_ACTION_ASSERT_PARAMS);
                    setAssertLibraryParams(params);
                    return new MediaSession2.SessionResult(RESULT_CODE_SUCCESS, null);
            }
            return new MediaSession2.SessionResult(RESULT_CODE_BAD_VALUE, null);
        }

        private void assertLibraryParams(LibraryParams params) {
            synchronized (MockMediaLibraryService2.class) {
                if (sAssertLibraryParams) {
                    assertLibraryParamsEquals(sExpectedParams, params);
                }
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
        int fromIndex = page * pageSize;
        int toIndex = Math.min((page + 1) * pageSize, totalItemCount);

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
        MediaMetadata2 metadata =  new MediaMetadata2.Builder()
                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId)
                .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
                .build();
        return new MediaItem2.Builder()
                .setMetadata(metadata)
                .build();
    }
}
