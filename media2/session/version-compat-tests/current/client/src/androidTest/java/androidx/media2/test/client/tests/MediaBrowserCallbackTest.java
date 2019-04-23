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

package androidx.media2.test.client.tests;

import static androidx.media2.session.LibraryResult.RESULT_ERROR_BAD_VALUE;
import static androidx.media2.session.LibraryResult.RESULT_SUCCESS;
import static androidx.media2.test.common.CommonConstants.MOCK_MEDIA2_LIBRARY_SERVICE;
import static androidx.media2.test.common.MediaBrowserConstants.CUSTOM_ACTION_ASSERT_PARAMS;
import static androidx.media2.test.common.MediaBrowserConstants.LONG_LIST_COUNT;
import static androidx.media2.test.common.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED_EXTRAS;
import static androidx.media2.test.common.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED_ITEM_COUNT;
import static androidx.media2.test.common.MediaBrowserConstants.ROOT_EXTRAS;
import static androidx.media2.test.common.MediaBrowserConstants.ROOT_ID;
import static androidx.media2.test.common.MediaBrowserConstants.SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;
import static androidx.media2.test.common.MediaBrowserConstants.SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
import static androidx.media2.test.common.MediaBrowserConstants.SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;
import static androidx.media2.test.common.MediaBrowserConstants.SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import static org.junit.Assert.assertNotEquals;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.session.LibraryResult;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaBrowser.BrowserCallback;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionResult;
import androidx.media2.session.SessionToken;
import androidx.media2.test.client.MediaTestUtils;
import androidx.media2.test.common.MediaBrowserConstants;
import androidx.media2.test.common.TestUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser.BrowserCallback}.
 * <p>
 * This test inherits {@link MediaControllerCallbackTest} to ensure that inherited APIs from
 * {@link MediaController} works cleanly.
 */
// TODO: (internal cleanup) Move tests that aren't related with callbacks.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaBrowserCallbackTest extends MediaControllerCallbackTest {
    private static final String TAG = "MediaBrowserCallbackTest";

    @Override
    MediaController onCreateController(final @NonNull SessionToken token,
            final @Nullable Bundle connectionHints, final @Nullable TestBrowserCallback callback)
            throws InterruptedException {
        assertNotNull("Test bug", token);
        final AtomicReference<MediaController> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                MediaBrowser.Builder builder = new MediaBrowser.Builder(mContext)
                        .setSessionToken(token)
                        .setControllerCallback(sHandlerExecutor, callback);
                if (connectionHints != null) {
                    builder.setConnectionHints(connectionHints);
                }
                controller.set(builder.build());
            }
        });
        return controller.get();
    }

    final MediaBrowser createBrowser() throws InterruptedException {
        return createBrowser(null, null);
    }

    final MediaBrowser createBrowser(@Nullable Bundle connectionHints,
            @Nullable BrowserCallback callback) throws InterruptedException {
        final SessionToken token = new SessionToken(mContext, MOCK_MEDIA2_LIBRARY_SERVICE);
        return (MediaBrowser) createController(token, true, connectionHints, callback);
    }

    @Test
    public void testGetLibraryRoot() throws Exception {
        prepareLooper();
        final LibraryParams params = new LibraryParams.Builder()
                .setOffline(true).setRecent(true).setExtras(new Bundle()).build();

        MediaBrowser browser = createBrowser();
        setExpectedLibraryParam(browser, params);
        LibraryResult result = browser.getLibraryRoot(params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        MediaMetadata metadata = result.getMediaItem().getMetadata();
        assertEquals(ROOT_ID, metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
        assertTrue(TestUtils.equals(ROOT_EXTRAS, result.getLibraryParams().getExtras()));
    }

    @Test
    public void testGetItem() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowserConstants.MEDIA_ID_GET_ITEM;

        LibraryResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        MediaTestUtils.assertMediaItemHasId(result.getMediaItem(), mediaId);
    }

    @Test
    public void testGetItem_unknownId() throws Exception {
        prepareLooper();
        final String mediaId = "random_media_id";

        LibraryResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_ERROR_BAD_VALUE, result.getResultCode());
        assertNull(result.getMediaItem());
    }

    @Test
    public void testGetItem_nullResult() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowserConstants.MEDIA_ID_GET_NULL_ITEM;

        // Exception will be thrown in the service side, and the process will be crashed.
        // In that case one of following will happen
        //   Case 1) Process is crashed. Pending ListenableFuture will get error
        //   Case 2) Due to the frequent crashes with other tests, process may not crash immediately
        //           because the Android shows dialog 'xxx keeps stopping' and defer sending
        //           SIG_KILL until the user's explicit action.
        try {
            LibraryResult result = createBrowser().getItem(mediaId)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // Case 1.
            assertNotEquals(RESULT_SUCCESS, result.getResultCode());
        } catch (TimeoutException e) {
            // Case 2.
        }
    }

    @Test
    public void testGetItem_invalidResult() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowserConstants.MEDIA_ID_GET_INVALID_ITEM;

        // Exception will be thrown in the service side, and the process will be crashed.
        // In that case one of following will happen
        //   Case 1) Process is crashed. Pending ListenableFuture will get error
        //   Case 2) Due to the frequent crashes with other tests, process may not crash immediately
        //           because the Android shows dialog 'xxx keeps stopping' and defer sending
        //           SIG_KILL until the user's explicit action.
        try {
            LibraryResult result = createBrowser().getItem(mediaId)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // Case 1.
            assertNotEquals(RESULT_SUCCESS, result.getResultCode());
        } catch (TimeoutException e) {
            // Case 2.
        }
    }

    @Test
    public void testGetChildren() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowserConstants.PARENT_ID;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams params = MediaTestUtils.createLibraryParams();

        MediaBrowser browser = createBrowser();
        setExpectedLibraryParam(browser, params);

        LibraryResult result = browser.getChildren(parentId, page, pageSize, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getLibraryParams());

        MediaTestUtils.assertPaginatedListHasIds(
                result.getMediaItems(), MediaBrowserConstants.GET_CHILDREN_RESULT,
                page, pageSize);
    }

    @Test
    @LargeTest
    public void testGetChildren_withLongList() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowserConstants.PARENT_ID_LONG_LIST;
        final int page = 0;
        final int pageSize = Integer.MAX_VALUE;
        final LibraryParams params = MediaTestUtils.createLibraryParams();

        MediaBrowser browser = createBrowser();
        setExpectedLibraryParam(browser, params);

        LibraryResult result = browser.getChildren(parentId, page, pageSize, params)
                .get(10, TimeUnit.SECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getLibraryParams());

        List<MediaItem> list = result.getMediaItems();
        assertEquals(LONG_LIST_COUNT, list.size());
        for (int i = 0; i < result.getMediaItems().size(); i++) {
            assertEquals(TestUtils.getMediaIdInDummyList(i), list.get(i).getMediaId());
        }
    }

    @Test
    public void testGetChildren_emptyResult() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowserConstants.PARENT_ID_NO_CHILDREN;

        MediaBrowser browser = createBrowser();
        LibraryResult result = browser.getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertEquals(0, result.getMediaItems().size());
    }

    @Test
    public void testGetChildren_nullResult() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowserConstants.PARENT_ID_ERROR;

        MediaBrowser browser = createBrowser();
        LibraryResult result = browser.getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItems());
    }

    @Test
    public void testSearchCallbacks() throws Exception {
        prepareLooper();
        final String query = MediaBrowserConstants.SEARCH_QUERY;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latchForSearch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser browser,
                    String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowserConstants.SEARCH_RESULT_COUNT, itemCount);
                latchForSearch.countDown();
            }
        };

        // Request the search.
        MediaBrowser browser = createBrowser(null, callback);
        setExpectedLibraryParam(browser, testParams);
        LibraryResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // Get the search result.
        result = browser.getSearchResult(query, page, pageSize, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        MediaTestUtils.assertPaginatedListHasIds(result.getMediaItems(),
                MediaBrowserConstants.SEARCH_RESULT, page, pageSize);
    }

    @Test
    @LargeTest
    public void testSearchCallbacks_withLongList() throws Exception {
        prepareLooper();
        final String query = MediaBrowserConstants.SEARCH_QUERY_LONG_LIST;
        final int page = 0;
        final int pageSize = Integer.MAX_VALUE;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowserConstants.LONG_LIST_COUNT, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser browser = createBrowser(null, callback);
        setExpectedLibraryParam(browser, testParams);
        LibraryResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        result = browser.getSearchResult(query, page, pageSize, testParams)
                .get(10, TimeUnit.SECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        List<MediaItem> list = result.getMediaItems();
        for (int i = 0; i < list.size(); i++) {
            assertEquals(TestUtils.getMediaIdInDummyList(i), list.get(i).getMediaId());
        }
    }

    @Test
    @LargeTest
    public void testOnSearchResultChanged_searchTakesTime() throws Exception {
        prepareLooper();
        final String query = MediaBrowserConstants.SEARCH_QUERY_TAKES_TIME;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowserConstants.SEARCH_RESULT_COUNT, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser browser = createBrowser(null, callback);
        setExpectedLibraryParam(browser, testParams);
        LibraryResult result = browser.search(query, testParams)
                .get(MediaBrowserConstants.SEARCH_TIME_IN_MS + TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
    }

    @Test
    public void testOnSearchResultChanged_emptyResult() throws Exception {
        prepareLooper();
        final String query = MediaBrowserConstants.SEARCH_QUERY_EMPTY_RESULT;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(0, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser browser = createBrowser(null, callback);
        setExpectedLibraryParam(browser, testParams);
        LibraryResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
    }

    @Test
    public void testOnChildrenChanged_calledWhenSubscribed() throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String expectedParentId = SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser browser, String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                MediaTestUtils.assertLibraryParamsEquals(params, NOTIFY_CHILDREN_CHANGED_EXTRAS);
                latch.countDown();
            }
        };

        LibraryResult result = createBrowser(null, controllerCallbackProxy)
                .subscribe(expectedParentId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService is supposed to call
        // notifyChildrenChanged() in its callback onSubscribe().
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_calledWhenSubscribed2() throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String expectedParentId = SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser browser, String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                MediaTestUtils.assertLibraryParamsEquals(params, NOTIFY_CHILDREN_CHANGED_EXTRAS);
                latch.countDown();
            }
        };

        LibraryResult result = createBrowser(null, controllerCallbackProxy)
                .subscribe(expectedParentId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService is supposed to call
        // notifyChildrenChanged(ControllerInfo) in its callback onSubscribe().
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @FlakyTest(bugId = 118671770)
    public void testOnChildrenChanged_notCalledWhenNotSubscribed() throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String subscribedMediaId =
                SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
        final CountDownLatch latch = new CountDownLatch(1);

        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser browser, String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        LibraryResult result = createBrowser(null, controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService is supposed to call
        // notifyChildrenChanged() in its callback onSubscribe(), but with a different media ID.
        // Therefore, onChildrenChanged() should not be called.
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_notCalledWhenNotSubscribed2() throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String subscribedMediaId =
                SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;
        final CountDownLatch latch = new CountDownLatch(1);

        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser browser, String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        LibraryResult result = createBrowser(null, controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService is supposed to call
        // notifyChildrenChanged(ControllerInfo) in its callback onSubscribe(),
        // but with a different media ID.
        // Therefore, onChildrenChanged() should not be called.
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void setExpectedLibraryParam(MediaBrowser browser, LibraryParams params)
            throws Exception {
        SessionCommand command = new SessionCommand(CUSTOM_ACTION_ASSERT_PARAMS, null);
        Bundle args = new Bundle();
        ParcelUtils.putVersionedParcelable(args, CUSTOM_ACTION_ASSERT_PARAMS, params);
        SessionResult result = browser.sendCustomCommand(command, args)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(SessionResult.RESULT_SUCCESS, result.getResultCode());
    }
}
