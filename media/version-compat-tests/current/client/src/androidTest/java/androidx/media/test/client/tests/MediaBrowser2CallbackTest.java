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
import static androidx.media.test.lib.MediaBrowser2Constants.EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_EXTRAS;
import static androidx.media.test.lib.MediaBrowser2Constants.NOTIFY_CHILDREN_CHANGED_ITEM_COUNT;
import static androidx.media.test.lib.MediaBrowser2Constants.ROOT_ID;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;
import static androidx.media.test.lib.MediaBrowser2Constants
        .SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;

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
import android.os.ResultReceiver;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.lib.MediaBrowser2Constants;
import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaBrowser2;
import androidx.media2.MediaBrowser2.BrowserCallback;
import androidx.media2.MediaController2;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionToken2;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser2.BrowserCallback}.
 * <p>
 * This test inherits {@link MediaController2CallbackTest} to ensure that inherited APIs from
 * {@link MediaController2} works cleanly.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaBrowser2CallbackTest extends MediaController2CallbackTest {
    private static final String TAG = "MediaBrowser2CallbackTest";

    @Override
    TestControllerInterface onCreateController(final @NonNull SessionToken2 token,
            final @Nullable ControllerCallback callback) throws InterruptedException {
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
    public void testOnGetLibraryRootDone() throws InterruptedException {
        prepareLooper();
        final Bundle param = TestUtils.createTestBundle();

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetLibraryRootDone(MediaBrowser2 browser,
                    Bundle rootHints, String rootMediaId, Bundle rootExtra) {
                assertTrue(TestUtils.equals(param, rootHints));
                assertEquals(ROOT_ID, rootMediaId);
                // Note that TestUtils#equals() cannot be used for this because
                // MediaBrowserServiceCompat adds extra_client_version to the rootHints.
                assertTrue(TestUtils.contains(rootExtra, EXTRAS));
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser =
                (MediaBrowser2) createController(token, true, callback);
        browser.getLibraryRoot(param);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnGetItemDone() throws InterruptedException {
        prepareLooper();
        final String mediaId = MediaBrowser2Constants.MEDIA_ID_GET_ITEM;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetItemDone(MediaBrowser2 browser, String mediaIdOut, MediaItem2 result) {
                assertEquals(mediaId, mediaIdOut);
                assertNotNull(result);
                assertEquals(mediaId, result.getMediaId());
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.getItem(mediaId);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnGetItemDone_nullResult() throws InterruptedException {
        prepareLooper();
        final String mediaId = "random_media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetItemDone(MediaBrowser2 browser, String mediaIdOut, MediaItem2 result) {
                assertEquals(mediaId, mediaIdOut);
                assertNull(result);
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.getItem(mediaId);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnGetChildrenDone() throws InterruptedException {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID;
        final int page = 4;
        final int pageSize = 10;
        final Bundle extras = new Bundle();
        extras.putString(TAG, TAG);

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetChildrenDone(MediaBrowser2 browser, String parentIdOut, int pageOut,
                    int pageSizeOut, List<MediaItem2> result, Bundle extrasOut) {
                assertEquals(parentId, parentIdOut);
                assertEquals(page, pageOut);
                assertEquals(pageSize, pageSizeOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                assertNotNull(result);

                int fromIndex = (page - 1) * pageSize;
                int toIndex = Math.min(page * pageSize, MediaBrowser2Constants.CHILDREN_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(
                            MediaBrowser2Constants.GET_CHILDREN_RESULT.get(originalIndex),
                            result.get(relativeIndex).getMediaId());
                }
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.getChildren(parentId, page, pageSize, extras);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnGetChildrenDone_emptyResult() throws InterruptedException {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID_NO_CHILDREN;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetChildrenDone(MediaBrowser2 browser, String parentIdOut,
                    int pageOut, int pageSizeOut, List<MediaItem2> result, Bundle extrasOut) {
                assertNotNull(result);
                assertEquals(0, result.size());
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.getChildren(parentId, 1, 1, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnGetChildrenDone_nullResult() throws InterruptedException {
        prepareLooper();
        final String parentId = MediaBrowser2Constants.PARENT_ID_ERROR;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onGetChildrenDone(MediaBrowser2 browser, String parentIdOut,
                    int pageOut, int pageSizeOut, List<MediaItem2> result, Bundle extrasOut) {
                assertNull(result);
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.getChildren(parentId, 1, 1, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearchCallbacks() throws InterruptedException {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY;
        final int page = 4;
        final int pageSize = 10;
        final Bundle extras = new Bundle();
        extras.putString(TAG, TAG);

        final CountDownLatch latchForSearch = new CountDownLatch(1);
        final CountDownLatch latchForGetSearchResult = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser2 browser,
                    String queryOut, int itemCount, Bundle extrasOut) {
                assertEquals(query, queryOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                assertEquals(MediaBrowser2Constants.SEARCH_RESULT_COUNT, itemCount);
                latchForSearch.countDown();
            }

            @Override
            public void onGetSearchResultDone(MediaBrowser2 browser, String queryOut,
                    int pageOut, int pageSizeOut, List<MediaItem2> result, Bundle extrasOut) {
                assertEquals(query, queryOut);
                assertEquals(page, pageOut);
                assertEquals(pageSize, pageSizeOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                assertNotNull(result);

                int fromIndex = (page - 1) * pageSize;
                int toIndex = Math.min(
                        page * pageSize, MediaBrowser2Constants.SEARCH_RESULT_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(
                            MediaBrowser2Constants.SEARCH_RESULT.get(originalIndex),
                            result.get(relativeIndex).getMediaId());
                }
                latchForGetSearchResult.countDown();
            }
        };

        // Request the search.
        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.search(query, extras);
        assertTrue(latchForSearch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        // Get the search result.
        browser.getSearchResult(query, page, pageSize, extras);
        assertTrue(latchForGetSearchResult.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testOnSearchResultChanged_searchTakesTime() throws InterruptedException {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY_TAKES_TIME;
        final Bundle extras = new Bundle();
        extras.putString(TAG, TAG);

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser2 browser, String queryOut, int itemCount, Bundle extrasOut) {
                assertEquals(query, queryOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                assertEquals(MediaBrowser2Constants.SEARCH_RESULT_COUNT, itemCount);
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.search(query, extras);
        assertTrue(latch.await(
                MediaBrowser2Constants.SEARCH_TIME_IN_MS + WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnSearchResultChanged_emptyResult() throws InterruptedException {
        prepareLooper();
        final String query = MediaBrowser2Constants.SEARCH_QUERY_EMPTY_RESULT;
        final Bundle extras = new Bundle();
        extras.putString(TAG, TAG);

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback callback = new BrowserCallback() {
            @Override
            public void onSearchResultChanged(
                    MediaBrowser2 browser, String queryOut, int itemCount, Bundle extrasOut) {
                assertEquals(query, queryOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                assertEquals(0, itemCount);
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaBrowser2 browser = (MediaBrowser2) createController(token, true, callback);
        browser.search(query, extras);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_calledWhenSubscribed() throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String expectedParentId = SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, Bundle extras) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                assertTrue(TestUtils.equals(NOTIFY_CHILDREN_CHANGED_EXTRAS, extras));
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        final MediaBrowser2 browser = (MediaBrowser2) createController(
                token, true, controllerCallbackProxy);
        browser.subscribe(expectedParentId, null);

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
        // notifyChildrenChanged() in its callback onSubscribe().
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_calledWhenSubscribed2() throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String expectedParentId = SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE;

        final CountDownLatch latch = new CountDownLatch(1);
        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, Bundle extras) {
                assertEquals(expectedParentId, parentId);
                assertEquals(NOTIFY_CHILDREN_CHANGED_ITEM_COUNT, itemCount);
                assertTrue(TestUtils.equals(NOTIFY_CHILDREN_CHANGED_EXTRAS, extras));
                latch.countDown();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        final MediaBrowser2 browser = (MediaBrowser2) createController(
                token, true, controllerCallbackProxy);
        browser.subscribe(expectedParentId, null);

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
        // notifyChildrenChanged(ControllerInfo) in its callback onSubscribe().
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_notCalledWhenNotSubscribed() throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged().
        prepareLooper();
        final String subscribedMediaId =
                SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ALL_WITH_NON_SUBSCRIBED_ID;
        final CountDownLatch latch = new CountDownLatch(1);

        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, Bundle extras) {
                // Unexpected call.
                fail();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        final MediaBrowser2 browser = (MediaBrowser2) createController(
                token, true, controllerCallbackProxy);
        browser.subscribe(subscribedMediaId, null);

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
        // notifyChildrenChanged() in its callback onSubscribe(), but with a different media ID.
        // Therefore, onChildrenChanged() should not be called.
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnChildrenChanged_notCalledWhenNotSubscribed2() throws InterruptedException {
        // This test uses MediaLibrarySession.notifyChildrenChanged(ControllerInfo).
        prepareLooper();
        final String subscribedMediaId =
                SUBSCRIBE_ID_NOTIFY_CHILDREN_CHANGED_TO_ONE_WITH_NON_SUBSCRIBED_ID;
        final CountDownLatch latch = new CountDownLatch(1);

        final BrowserCallback controllerCallbackProxy = new BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser2 browser, String parentId,
                    int itemCount, Bundle extras) {
                // Unexpected call.
                fail();
            }
        };

        final SessionToken2 token = new SessionToken2(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        final MediaBrowser2 browser = (MediaBrowser2) createController(
                token, true, controllerCallbackProxy);
        browser.subscribe(subscribedMediaId, null);

        // The MediaLibrarySession in MockMediaLibraryService2 is supposed to call
        // notifyChildrenChanged(ControllerInfo) in its callback onSubscribe(),
        // but with a different media ID.
        // Therefore, onChildrenChanged() should not be called.
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
                assertTrue(connectLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void waitForDisconnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(disconnectLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
        public void onCustomCommand(MediaController2 controller, SessionCommand2 command,
                Bundle args, ResultReceiver receiver) {
            mCallbackProxy.onCustomCommand(controller, command, args, receiver);
            synchronized (this) {
                if (mOnCustomCommandRunnable != null) {
                    mOnCustomCommandRunnable.run();
                }
            }
        }

        @Override
        public void onCustomLayoutChanged(MediaController2 controller, List<CommandButton> layout) {
            mCallbackProxy.onCustomLayoutChanged(controller, layout);
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
        public void onError(MediaController2 controller, int errorCode, Bundle extras) {
            mCallbackProxy.onError(controller, errorCode, extras);
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
        public void onRoutesInfoChanged(MediaController2 controller, List<Bundle> routes) {
            mCallbackProxy.onRoutesInfoChanged(controller, routes);
        }

        @Override
        public void onGetLibraryRootDone(MediaBrowser2 browser, Bundle rootHints,
                String rootMediaId, Bundle rootExtra) {
            super.onGetLibraryRootDone(browser, rootHints, rootMediaId, rootExtra);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onGetLibraryRootDone(browser, rootHints, rootMediaId, rootExtra);
            }
        }

        @Override
        public void onGetItemDone(MediaBrowser2 browser, String mediaId, MediaItem2 result) {
            super.onGetItemDone(browser, mediaId, result);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy).onGetItemDone(browser, mediaId, result);
            }
        }

        @Override
        public void onGetChildrenDone(MediaBrowser2 browser, String parentId, int page,
                int pageSize, List<MediaItem2> result, Bundle extras) {
            super.onGetChildrenDone(browser, parentId, page, pageSize, result, extras);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onGetChildrenDone(browser, parentId, page, pageSize, result, extras);
            }
        }

        @Override
        public void onSearchResultChanged(MediaBrowser2 browser, String query, int itemCount,
                Bundle extras) {
            super.onSearchResultChanged(browser, query, itemCount, extras);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onSearchResultChanged(browser, query, itemCount, extras);
            }
        }

        @Override
        public void onGetSearchResultDone(MediaBrowser2 browser, String query, int page,
                int pageSize, List<MediaItem2> result, Bundle extras) {
            super.onGetSearchResultDone(browser, query, page, pageSize, result, extras);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onGetSearchResultDone(browser, query, page, pageSize, result, extras);
            }
        }

        @Override
        public void onChildrenChanged(MediaBrowser2 browser, String parentId, int itemCount,
                Bundle extras) {
            super.onChildrenChanged(browser, parentId, itemCount, extras);
            if (mCallbackProxy instanceof BrowserCallback) {
                ((BrowserCallback) mCallbackProxy)
                        .onChildrenChanged(browser, parentId, itemCount, extras);
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
