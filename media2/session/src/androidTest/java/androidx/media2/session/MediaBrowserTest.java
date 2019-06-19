/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.media2.session.LibraryResult.RESULT_ERROR_PERMISSION_DENIED;
import static androidx.media2.session.LibraryResult.RESULT_SUCCESS;
import static androidx.media2.session.TestUtils.assertLibraryParamsEquals;
import static androidx.media2.session.TestUtils.assertMediaItemEquals;
import static androidx.media2.session.TestUtils.assertMediaItemWithId;
import static androidx.media2.session.TestUtils.createLibraryParams;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.session.MediaBrowser.BrowserCallback;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaLibraryService.MediaLibrarySession;
import androidx.media2.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser}.
 * <p>
 * This test inherits {@link MediaControllerTest} to ensure that inherited APIs from
 * {@link MediaController} works cleanly.
 */
// TODO(jaewan): Implement host-side test so browser and service can run in different processes.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaBrowserTest extends MediaControllerTest {
    private static final String TAG = "MediaBrowserTest";

    @Override
    MediaController onCreateController(@NonNull final SessionToken token,
            @Nullable final Bundle connectionHints, @Nullable final TestBrowserCallback callback)
            throws InterruptedException {
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
        return createBrowser(null);
    }

    final MediaBrowser createBrowser(@Nullable BrowserCallback callback)
            throws InterruptedException {
        return (MediaBrowser) createController(MockMediaLibraryService.getToken(mContext),
                true, null, callback);
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
            // For any methods in the controller callback, TestBrowserCallback should have
            // overridden the method and call matching API in the callback proxy.
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
        final LibraryParams params = createLibraryParams();

        MockMediaLibraryService.setAssertLibraryParams(params);
        LibraryResult result = createBrowser().getLibraryRoot(params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertMediaItemEquals(MockMediaLibraryService.ROOT_ITEM, result.getMediaItem());
        assertLibraryParamsEquals(MockMediaLibraryService.ROOT_PARAMS, result.getLibraryParams());
    }

    @Test
    public void testGetItem() throws Exception {
        prepareLooper();
        final String mediaId = MockMediaLibraryService.MEDIA_ID_GET_ITEM;

        LibraryResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertMediaItemWithId(mediaId, result.getMediaItem());
    }

    @Test
    public void testGetItemNullResult() throws Exception {
        prepareLooper();
        final String mediaId = "random_media_id";

        LibraryResult result = createBrowser().getItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItem());
    }

    @Test
    public void testGetChildren() throws Exception {
        prepareLooper();
        final String parentId = MockMediaLibraryService.PARENT_ID;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams params = createLibraryParams();

        MockMediaLibraryService.setAssertLibraryParams(params);
        LibraryResult result = createBrowser().getChildren(parentId, page, pageSize, params)
                        .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        TestUtils.assertPaginatedListEquals(MockMediaLibraryService.GET_CHILDREN_RESULT,
                page, pageSize, result.getMediaItems());
    }

    @Test
    public void testGetChildrenEmptyResult() throws Exception {
        prepareLooper();
        final String parentId = MockMediaLibraryService.PARENT_ID_NO_CHILDREN;

        LibraryResult result = createBrowser().getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertEquals(0, result.getMediaItems().size());
    }

    @Test
    public void testGetChildrenNullResult() throws Exception {
        prepareLooper();
        final String parentId = MockMediaLibraryService.PARENT_ID_ERROR;

        LibraryResult result = createBrowser().getChildren(parentId, 1, 1, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItems());
    }

    @Test
    public void testSearch() throws Exception {
        prepareLooper();
        final String query = MockMediaLibraryService.SEARCH_QUERY;
        final int page = 4;
        final int pageSize = 10;
        final LibraryParams params = createLibraryParams();

        final CountDownLatch latchForSearch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser browser,
                    String queryOut, int itemCount, LibraryParams paramsOut) {
                assertEquals(query, queryOut);
                assertLibraryParamsEquals(params, paramsOut);
                assertEquals(MockMediaLibraryService.SEARCH_RESULT_COUNT, itemCount);
                latchForSearch.countDown();
            }
        };

        // Request the search.
        MockMediaLibraryService.setAssertLibraryParams(params);
        MediaBrowser browser = createBrowser(callback);
        LibraryResult result = browser.search(query, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(latchForSearch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Get the search result.
        result = browser.getSearchResult(query, page, pageSize, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        TestUtils.assertPaginatedListEquals(MockMediaLibraryService.SEARCH_RESULT,
                page, pageSize, result.getMediaItems());
    }

    @Test
    @LargeTest
    public void testSearchTakesTime() throws Exception {
        prepareLooper();
        final String query = MockMediaLibraryService.SEARCH_QUERY_TAKES_TIME;
        final LibraryParams params = createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser browser, String queryOut, int itemCount,
                    LibraryParams paramsOut) {
                assertEquals(query, queryOut);
                assertLibraryParamsEquals(params, paramsOut);
                assertEquals(MockMediaLibraryService.SEARCH_RESULT_COUNT, itemCount);
                latch.countDown();
            }
        };

        MockMediaLibraryService.setAssertLibraryParams(params);
        LibraryResult result = createBrowser(callback).search(query, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(latch.await(
                MockMediaLibraryService.SEARCH_TIME_IN_MS + TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearchEmptyResult() throws Exception {
        prepareLooper();
        final String query = MockMediaLibraryService.SEARCH_QUERY_EMPTY_RESULT;
        final LibraryParams params = createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser browser, String queryOut, int itemCount,
                    LibraryParams paramsOut) {
                assertEquals(query, queryOut);
                assertLibraryParamsEquals(params, paramsOut);
                assertEquals(0, itemCount);
                latch.countDown();
            }
        };

        MockMediaLibraryService.setAssertLibraryParams(params);
        LibraryResult result = createBrowser(callback).search(query, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSubscribe() throws Exception {
        prepareLooper();
        final String testParentId = "testSubscribeId";
        final LibraryParams params = createLibraryParams();

        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public int onSubscribe(@NonNull MediaLibraryService.MediaLibrarySession session,
                    @NonNull MediaSession.ControllerInfo info, @NonNull String parentId,
                    @Nullable LibraryParams paramsOut) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    assertLibraryParamsEquals(params, paramsOut);
                    return RESULT_SUCCESS;
                }
                return RESULT_ERROR_PERMISSION_DENIED;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);
        MockMediaLibraryService.setAssertLibraryParams(params);
        LibraryResult result = createBrowser().subscribe(testParentId, params)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItems());
    }

    @Test
    public void testUnsubscribe() throws Exception {
        prepareLooper();
        final String testParentId = "testUnsubscribeId";
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public int onUnsubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo info, @NonNull String parentId) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    return RESULT_SUCCESS;
                }
                return RESULT_ERROR_PERMISSION_DENIED;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);
        LibraryResult result = createBrowser().unsubscribe(testParentId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertNull(result.getMediaItems());
    }

    @Test
    public void testBrowserCallback_onChildrenChangedIsNotCalledWhenNotSubscribed()
            throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String subscribedMediaId = "subscribedMediaId";
        final String anotherMediaId = "anotherMediaId";
        final int testChildrenCount = 101;

        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                if (Process.myUid() == controller.getUid()) {
                    // Shouldn't trigger onChildrenChanged() for the browser,
                    // because the browser didn't subscribe this media id.
                    session.notifyChildrenChanged(anotherMediaId, testChildrenCount, null);
                }
                return RESULT_SUCCESS;
            }

            @NonNull
            @Override
            public LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, LibraryParams params) {
                // This wouldn't be called at all.
                return new LibraryResult(RESULT_SUCCESS,
                        TestUtils.createMediaItems(testChildrenCount), null);
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);
        LibraryResult result = createBrowser(controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Subscribe itself is success because onSubscribe() returned SUCCESS.
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // notifyChildrenChanged() in onSubscribe() should fail onChildrenChanged() should not be
        // called, because the ID hasn't been subscribed.
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBrowserCallback_onChildrenChangedIsCalledWhenSubscribed()
            throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String expectedParentId = "expectedParentId";
        final int testChildrenCount = 101;
        final LibraryParams testParams = createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                if (Process.myUid() == controller.getUid()) {
                    // Should trigger onChildrenChanged() for the browser.
                    session.notifyChildrenChanged(expectedParentId, testChildrenCount, params);
                }
                return RESULT_SUCCESS;
            }

            @NonNull
            @Override
            public LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, LibraryParams params) {
                return new LibraryResult(RESULT_SUCCESS,
                        TestUtils.createMediaItems(testChildrenCount), null);
            }
        };
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(testChildrenCount, itemCount);
                assertLibraryParamsEquals(testParams, params);
                latch.countDown();
            }
        };

        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);
        MockMediaLibraryService.setAssertLibraryParams(testParams);
        createBrowser(controllerCallbackProxy).subscribe(expectedParentId, testParams);

        // onChildrenChanged() should be called.
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBrowserCallback_onChildrenChangedIsNotCalledWhenNotSubscribed2()
            throws Exception {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String subscribedMediaId = "subscribedMediaId";
        final String anotherMediaId = "anotherMediaId";
        final int testChildrenCount = 101;

        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                if (Process.myUid() == controller.getUid()) {
                    // Shouldn't trigger onChildrenChanged() for the browser,
                    // because the browser didn't subscribe this media id.
                    session.notifyChildrenChanged(
                            controller, anotherMediaId, testChildrenCount, null);
                }
                return RESULT_SUCCESS;
            }

            @NonNull
            @Override
            public LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, LibraryParams params) {
                return new LibraryResult(RESULT_SUCCESS,
                        TestUtils.createMediaItems(testChildrenCount), null);
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
                    int itemCount, LibraryParams params) {
                // Unexpected call.
                fail();
                latch.countDown();
            }
        };

        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);

        LibraryResult result = createBrowser(controllerCallbackProxy)
                .subscribe(subscribedMediaId, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // onSubscribe() always returns SUCCESS, so success is expected.
        assertEquals(RESULT_SUCCESS, result.getResultCode());

        // But onChildrenChanged() wouldn't be called because notifyChildrenChanged() fails.
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBrowserCallback_onChildrenChangedIsCalledWhenSubscribed2()
            throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String expectedParentId = "expectedParentId";
        final int testChildrenCount = 101;
        final LibraryParams testParams = createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                if (Process.myUid() == controller.getUid()) {
                    // Should trigger onChildrenChanged() for the browser.
                    session.notifyChildrenChanged(
                            controller, expectedParentId, testChildrenCount, testParams);
                }
                return RESULT_SUCCESS;
            }

            @NonNull
            @Override
            public LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, LibraryParams params) {
                return new LibraryResult(RESULT_SUCCESS,
                        TestUtils.createMediaItems(testChildrenCount), null);
            }
        };
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
                    int itemCount, LibraryParams params) {
                assertEquals(expectedParentId, parentId);
                assertEquals(testChildrenCount, itemCount);
                assertLibraryParamsEquals(testParams, params);
                latch.countDown();
            }
        };

        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);
        createBrowser(controllerCallbackProxy).subscribe(expectedParentId, null);

        // onChildrenChanged() should be called.
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
