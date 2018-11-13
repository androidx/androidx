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

package androidx.media.test.client.tests;

import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_LIBRARY_SERVICE;
import static androidx.media.test.lib.MediaBrowser2Constants.CUSTOM_ACTION_ASSERT_PARAMS;
import static androidx.media.test.lib.MediaBrowser2Constants.LONG_LIST_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_ITEM_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;
import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaBrowser2.BrowserResult.RESULT_CODE_SUCCESS;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.client.MediaTestUtils;
import androidx.media.test.lib.MediaBrowser2Constants;
import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaBrowser2;
import androidx.media2.MediaBrowser2.BrowserCallback;
import androidx.media2.MediaBrowser2.BrowserResult;
import androidx.media2.MediaController2;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaController2.ControllerResult;
import androidx.media2.MediaItem2;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionToken2;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser2.BrowserCallback}.
 * <p>
 * This test inherits {@link MediaController2CallbackTest} to ensure that inherited APIs from
 * {@link MediaController2} works cleanly.
 */
// TODO: (internal cleanup) Move tests that aren't related with callbacks.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaBrowser2CallbackTest extends MediaController2CallbackTest {
    private static final String TAG = "MediaBrowser2CallbackTest";

    @Override
    TestControllerInterface onCreateController(final @NonNull SessionToken2 token,
            final @Nullable ControllerCallback callback) throws InterruptedException {
        assertNotNull("Test bug", token);
        final AtomicReference<TestControllerInterface> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                controller.set(new TestMediaBrowser(
                        mContext, token, new TestBrowserCallback(callback)));
            }
        });
        return controller.get();
    }

    final MediaBrowser2 createBrowser() throws InterruptedException {
        return createBrowser(null);
    }

    final MediaBrowser2 createBrowser(@Nullable BrowserCallback callback)
            throws InterruptedException {
        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        return (MediaBrowser2) createController(token, true, callback);
    }

    /**
     * Test if the {@link TestBrowserCallback} wraps the callback proxy without missing any method.
     */
    @Test
    public void testTestBrowserCallback() {
        prepareLooper();
        Method[] methods = TestBrowserCallback.class.getMethods();
        assertNotNull(methods);
        for (int i = 0; i < methods.length; i++) {
            // For any methods in the controller callback, TestControllerCallback should have
            // overriden the method and call matching API in the callback proxy.
            assertNotEquals("TestBrowserCallback should override " + methods[i]
                            + " and call callback proxy",
                    BrowserCallback.class, methods[i].getDeclaringClass());
            assertNotEquals("TestBrowserCallback should override " + methods[i]
                            + " and call callback proxy",
                    ControllerCallback.class, methods[i].getDeclaringClass());
        }
    }

    @Test
    public void testGetLibraryRoot() throws Exception {
        prepareLooper();
        final LibraryParams params = new LibraryParams.Builder()
                .setOffline(true).setRecent(true).setExtras(new Bundle()).build();

        MediaBrowser2 browser = createBrowser();
        setExpectedLibraryParam(browser, params);
        BrowserResult result = browser.getLibraryRoot(params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        MediaMetadata2 metadata = result.getMediaItem().getMetadata();
        assertEquals(ROOT_ID, metadata.getString(MediaMetadata2.METADATA_KEY_MEDIA_ID));
        assertTrue(TestUtils.equals(ROOT_EXTRAS, result.getLibraryParams().getExtras()));
    }

    @Test
    public void testGetItem() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowser2Constants.MEDIA_ID_GET_ITEM;

        BrowserResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        MediaTestUtils.assertMediaItemHasId(result.getMediaItem(), mediaId);
    }

    @Test
    public void testGetItem_unknownId() throws Exception {
        prepareLooper();
        final String mediaId = "random_media_id";

        BrowserResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_BAD_VALUE, result.getResultCode());
        assertNull(result.getMediaItem());
    }

    @Test
    public void testGetItem_nullResult() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowser2Constants.MEDIA_ID_GET_NULL_ITEM;

        // Exception will be thrown in the service side, and the process will be crashed.
        // In that case one of following will happen
        //   Case 1) Process is crashed. Pending ListenableFuture will get error
        //   Case 2) Due to the frequent crashes with other tests, process may not crash immediately
        //           because the Android shows dialog 'xxx keeps stopping' and defer sending
        //           SIG_KILL until the user's explicit action.
        try {
            BrowserResult result = createBrowser().getItem(mediaId)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // Case 1.
            assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        } catch (TimeoutException e) {
            // Case 2.
        }
    }

    @Test
    public void testGetItem_invalidResult() throws Exception {
        prepareLooper();
        final String mediaId = MediaBrowser2Constants.MEDIA_ID_GET_INVALID_ITEM;

        // Exception will be thrown in the service side, and the process will be crashed.
        // In that case one of following will happen
        //   Case 1) Process is crashed. Pending ListenableFuture will get error
        //   Case 2) Due to the frequent crashes with other tests, process may not crash immediately
        //           because the Android shows dialog 'xxx keeps stopping' and defer sending
        //           SIG_KILL until the user's explicit action.
        try {
            BrowserResult result = createBrowser().getItem(mediaId)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // Case 1.
            assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        } catch (TimeoutException e) {
            // Case 2.
        }
    }

    @Test
    public void testGetChildren() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams params = MediaTestUtils.createLibraryParams();

        MediaBrowser2 browser = createBrowser();
        setExpectedLibraryParam(browser, params);

        BrowserResult result = browser.getChildren(parentId, page, pageSize, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertNull(result.getLibraryParams());

        MediaTestUtils.assertPaginatedListHasIds(
                result.getMediaItems(), MediaBrowser2Constants.GET_CHILDREN_RESULT,
                page, pageSize);
    }

    @Test
    @LargeTest
    public void testGetChildren_withLongList() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID_LONG_LIST;
        final int page = 0;
        final int pageSize = Integer.MAX_VALUE;
        final LibraryParams params = MediaTestUtils.createLibraryParams();

        MediaBrowser2 browser = createBrowser();
        setExpectedLibraryParam(browser, params);

        BrowserResult result = browser.getChildren(parentId, page, pageSize, params)
                .get(10, TimeUnit.SECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertNull(result.getLibraryParams());

        List<MediaItem2> list = result.getMediaItems();
        assertEquals(LONG_LIST_COUNT, list.size());
        for (int i = 0; i < result.getMediaItems().size(); i++) {
            assertEquals(TestUtils.getMediaIdInDummyList(i), list.get(i).getMediaId());
        }
    }

    @Test
    public void testGetChildren_emptyResult() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID_NO_CHILDREN;

        MediaBrowser2 browser = createBrowser();
        BrowserResult result = browser.getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(0, result.getMediaItems().size());
    }

    @Test
    public void testGetChildren_nullResult() throws Exception {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID_ERROR;

        MediaBrowser2 browser = createBrowser();
        BrowserResult result = browser.getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItems());
    }

    @Test
    public void testSearchCallbacks() throws Exception {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latchForSearch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser2 browser,
                    String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowser2Constants.SEARCH_RESULT_COUNT, itemCount);
                latchForSearch.countDown();
            }
        };

        // Request the search.
        MediaBrowser2 browser = createBrowser(callback);
        setExpectedLibraryParam(browser, testParams);
        BrowserResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // Get the search result.
        result = browser.getSearchResult(query, page, pageSize, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        MediaTestUtils.assertPaginatedListHasIds(result.getMediaItems(),
                MediaBrowser2Constants.SEARCH_RESULT, page, pageSize);
    }

    @Test
    @LargeTest
    public void testSearchCallbacks_withLongList() throws Exception {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY_LONG_LIST;
        final int page = 0;
        final int pageSize = Integer.MAX_VALUE;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser2 browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowser2Constants.LONG_LIST_COUNT, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser2 browser = createBrowser(callback);
        setExpectedLibraryParam(browser, testParams);
        BrowserResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        result = browser.getSearchResult(query, page, pageSize, testParams)
                .get(10, TimeUnit.SECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        List<MediaItem2> list = result.getMediaItems();
        for (int i = 0; i < list.size(); i++) {
            assertEquals(TestUtils.getMediaIdInDummyList(i), list.get(i).getMediaId());
        }
    }

    @Test
    @LargeTest
    public void testOnSearchResultChanged_searchTakesTime() throws Exception {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY_TAKES_TIME;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser2 browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(MediaBrowser2Constants.SEARCH_RESULT_COUNT, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser2 browser = createBrowser(callback);
        setExpectedLibraryParam(browser, testParams);
        BrowserResult result = browser.search(query, testParams)
                .get(MediaBrowser2Constants.SEARCH_TIME_IN_MS + TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
    }

    @Test
    public void testOnSearchResultChanged_emptyResult() throws Exception {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY_EMPTY_RESULT;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser2 browser, String queryOut, int itemCount, LibraryParams params) {
                assertEquals(query, queryOut);
                MediaTestUtils.assertLibraryParamsEquals(testParams, params);
                assertEquals(0, itemCount);
                latch.countDown();
            }
        };

        MediaBrowser2 browser = createBrowser(callback);
        setExpectedLibraryParam(browser, testParams);
        BrowserResult result = browser.search(query, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
    }

    @Test
    public void testOnChildrenChanged_calledWhenSubscribed() throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String expectedParentId = SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                MediaTestUtils.assertLibraryParamsEquals(params, NOTIFY_CHILDREN_CHANGED_EXTRAS);
                latch.countDown();
            }
        };

        BrowserResult result = createBrowser(controllerCallbackProxy)
                .subscribe(expectedParentId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
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
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                MediaTestUtils.assertLibraryParamsEquals(params, NOTIFY_CHILDREN_CHANGED_EXTRAS);
                latch.countDown();
            }
        };

        BrowserResult result = createBrowser(controllerCallbackProxy)
                .subscribe(expectedParentId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
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
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        BrowserResult result = createBrowser(controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
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
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        BrowserResult result = createBrowser(controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
        // notifyChildrenChanged(ControllerInfo) in its callback onSubscribe(),
        // but with a different media ID.
        // Therefore, onChildrenChanged() should not be called.
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void setExpectedLibraryParam(MediaBrowser2 browser, LibraryParams params)
            throws Exception {
        SessionCommand2 command = new SessionCommand2(CUSTOM_ACTION_ASSERT_PARAMS, null);
        Bundle args = new Bundle();
        ParcelUtils.putVersionedParcelable(args, CUSTOM_ACTION_ASSERT_PARAMS, params);
        ControllerResult result = browser.sendCustomCommand(command, args)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(ControllerResult.RESULT_CODE_SUCCESS, result.getResultCode());
    }

    public static class TestBrowserCallback extends BrowserCallback
            implements TestControllerCallbackInterface {
        private final ControllerCallback mCallbackProxy;
        public final CountDownLatch connectLatch = new CountDownLatch(1);
        public final CountDownLatch disconnectLatch = new CountDownLatch(1);
        @GuardedBy("this")
        private Runnable mOnCustomCommandRunnable;

        TestBrowserCallback(ControllerCallback callbackProxy) {
            if (callbackProxy == null) {
                callbackProxy = new BrowserCallback() {};
            }
            mCallbackProxy = callbackProxy;
        }

        @CallSuper
        @Override
        public void onConnected(MediaController2 controller, SessionCommandGroup2 commands) {
            connectLatch.countDown();
        }

        @CallSuper
        @Override
        public void onDisconnected(MediaController2 controller) {
            disconnectLatch.countDown();
        }

        @Override
        public void waitForConnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void waitForDisconnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(disconnectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(disconnectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void onPlaybackInfoChanged(MediaController2 controller,
                MediaController2.PlaybackInfo info) {
            mCallbackProxy.onPlaybackInfoChanged(controller, info);
        }

        @Override
        public MediaController2.ControllerResult onCustomCommand(MediaController2 controller,
                SessionCommand2 command, Bundle args) {
            synchronized (this) {
                if (mOnCustomCommandRunnable != null) {
                    mOnCustomCommandRunnable.run();
                }
            }
            return mCallbackProxy.onCustomCommand(controller, command, args);
        }

        @Override
        public int onSetCustomLayout(MediaController2 controller, List<CommandButton> layout) {
            return mCallbackProxy.onSetCustomLayout(controller, layout);
        }

        @Override
        public void onAllowedCommandsChanged(MediaController2 controller,
                SessionCommandGroup2 commands) {
            mCallbackProxy.onAllowedCommandsChanged(controller, commands);
        }

        @Override
        public void onPlayerStateChanged(MediaController2 controller, int state) {
            mCallbackProxy.onPlayerStateChanged(controller, state);
        }

        @Override
        public void onSeekCompleted(MediaController2 controller, long position) {
            mCallbackProxy.onSeekCompleted(controller, position);
        }

        @Override
        public void onPlaybackSpeedChanged(MediaController2 controller, float speed) {
            mCallbackProxy.onPlaybackSpeedChanged(controller, speed);
        }

        @Override
        public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                int state) {
            mCallbackProxy.onBufferingStateChanged(controller, item, state);
        }

        @Override
        public void onCurrentMediaItemChanged(MediaController2 controller, MediaItem2 item) {
            mCallbackProxy.onCurrentMediaItemChanged(controller, item);
        }

        @Override
        public void onPlaylistChanged(MediaController2 controller,
                List<MediaItem2> list, MediaMetadata2 metadata) {
            mCallbackProxy.onPlaylistChanged(controller, list, metadata);
        }

        @Override
        public void onPlaylistMetadataChanged(MediaController2 controller,
                MediaMetadata2 metadata) {
            mCallbackProxy.onPlaylistMetadataChanged(controller, metadata);
        }

        @Override
        public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
            mCallbackProxy.onShuffleModeChanged(controller, shuffleMode);
        }

        @Override
        public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
            mCallbackProxy.onRepeatModeChanged(controller, repeatMode);
        }

        @Override
        public void onPlaybackCompleted(MediaController2 controller) {
            mCallbackProxy.onPlaybackCompleted(controller);
        }

        @Override
        public void onSearchResultChanged(MediaBrowser2 browser, String query, int itemCount,
                LibraryParams params) {
            super.onSearchResultChanged(browser, query, itemCount, params);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onSearchResultChanged(browser, query, itemCount, params);
            }
        }

        @Override
        public void onChildrenChanged(MediaBrowser2 browser, String parentId, int itemCount,
                LibraryParams params) {
            super.onChildrenChanged(browser, parentId, itemCount, params);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onChildrenChanged(browser, parentId, itemCount, params);
            }
        }

        @Override
        public void setRunnableForOnCustomCommand(Runnable runnable) {
            synchronized (this) {
                mOnCustomCommandRunnable = runnable;
            }
        }
    }

    public class TestMediaBrowser extends MediaBrowser2 implements TestControllerInterface {
        private final BrowserCallback mCallback;

        public TestMediaBrowser(@NonNull Context context, @NonNull SessionToken2 token,
                @NonNull BrowserCallback callback) {
            super(context, token, sHandlerExecutor, callback);
            mCallback = callback;
        }

        @Override
        public BrowserCallback getCallback() {
            return mCallback;
        }
    }
}
