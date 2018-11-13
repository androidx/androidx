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

package androidx.media2;

import static androidx.media2.MediaBrowser.BrowserResult.RESULT_CODE_SUCCESS;
import static androidx.media2.TestUtils.assertMediaItemWithId;
import static androidx.media2.TestUtils.createLibraryParams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.media2.MediaBrowser.BrowserCallback;
import androidx.media2.MediaBrowser.BrowserResult;
import androidx.media2.MediaController.ControllerCallback;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MockMediaBrowserServiceCompat.Proxy;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser} with {@link MediaBrowserServiceCompat}.
 */
@SmallTest
public class MediaBrowserLegacyTest extends MediaSessionTestBase {
    private static final String TAG = "MediaBrowserLegacyTest";

    @Override
    TestControllerInterface onCreateController(final @NonNull SessionToken token,
            final @Nullable ControllerCallback callback) throws InterruptedException {
        final AtomicReference<TestControllerInterface> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                controller.set(new TestMediaBrowser(
                        mContext, token, new MockBrowserCallback(callback)));
            }
        });
        return controller.get();
    }

    private MediaBrowser createBrowser(BrowserCallback callback) {
        return createBrowser(true, callback);
    }

    private MediaBrowser createBrowser(boolean waitForConnect, BrowserCallback callback) {
        SessionToken token = new SessionToken(mContext,
                new ComponentName(mContext, MockMediaBrowserServiceCompat.class));
        try {
            return (MediaBrowser) createController(token, waitForConnect, callback);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fail("failed to create MediaBrowser for connecting MediaBrowserServiceCompat");
        return null;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testConnect() throws InterruptedException {
        prepareLooper();

        MediaBrowser browser = createBrowser(new MediaBrowser.BrowserCallback() { });
        // If connection failed, exception will be thrown inside of #createBrowser().
    }

    @Test
    public void testConnect_rejected() throws InterruptedException {
        prepareLooper();

        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                    Bundle rootHints) {
                return null;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        MediaBrowser browser = createBrowser(false, new MediaBrowser.BrowserCallback() {
            @Override
            public void onConnected(MediaController controller,
                    SessionCommandGroup allowedCommands) {
                fail("shouldn't allow connection");
                super.onConnected(controller, allowedCommands);
            }

            @Override
            public void onDisconnected(MediaController controller) {
                super.onDisconnected(controller);
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetLibraryRoot() throws Exception {
        prepareLooper();
        final String testMediaId = "testGetLibraryRoot";
        final Bundle testExtra = new Bundle();
        testExtra.putString(testMediaId, testMediaId);
        final LibraryParams testParams = new LibraryParams.Builder()
                .setExtras(testExtra).setOffline(true).setRecent(true).setSuggested(true).build();

        final BrowserRoot browserRootWithoutParam = new BrowserRoot(testMediaId, null);
        final Bundle testReturnedExtra = new Bundle(testExtra);
        testReturnedExtra.putBoolean(BrowserRoot.EXTRA_OFFLINE, true);
        final BrowserRoot browserRootWithParam = new BrowserRoot(testMediaId, testReturnedExtra);

        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                    Bundle rootHints) {
                assertEquals(mContext.getPackageName(), clientPackageName);
                if (rootHints.keySet().contains(testMediaId)) {
                    // This should happen because getLibraryRoot() is called with testExtras.
                    return browserRootWithParam;
                }
                // This shouldn't be happen for getLibraryRoot(testParams)
                return browserRootWithoutParam;
            }
        });

        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getLibraryRoot(testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertMediaItemWithId(testMediaId, result.getMediaItem());

        assertEquals(testReturnedExtra.getBoolean(BrowserRoot.EXTRA_RECENT),
                result.getLibraryParams().isRecent());
        assertEquals(testReturnedExtra.getBoolean(BrowserRoot.EXTRA_OFFLINE),
                result.getLibraryParams().isOffline());
        assertEquals(testReturnedExtra.getBoolean(BrowserRoot.EXTRA_SUGGESTED),
                result.getLibraryParams().isSuggested());

        // Note that TestUtils#equals() cannot be used for this because
        // MediaBrowserServiceCompat adds extra_client_version to the rootHints.
        assertTrue(TestUtils.contains(result.getLibraryParams().getExtras(), testExtra));
    }

    @Test
    public void testGetItem() throws Exception {
        prepareLooper();
        final String testMediaId = "test_media_item";
        final MediaBrowserCompat.MediaItem testItem = createMediaItem(testMediaId);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadItem(String itemId, Result<MediaBrowserCompat.MediaItem> result) {
                assertEquals(testMediaId, itemId);
                result.sendResult(testItem);
            }
        });

        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getItem(testMediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertItemEquals(testItem, result.getMediaItem());
    }

    @Test
    public void testGetItem_nullResult() throws Exception {
        prepareLooper();
        final String testMediaId = "test_media_item";
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadItem(String itemId, Result<MediaBrowserCompat.MediaItem> result) {
                assertEquals(testMediaId, itemId);
                result.sendResult(null);
            }
        });
        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getItem(testMediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
    }

    @Test
    public void testGetChildren_onLoadChildrenWithoutOptions() throws Exception {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        final List<MediaBrowserCompat.MediaItem> testFullMediaItemList = createMediaItems(
                (testPage + 1) * testPageSize);
        final List<MediaBrowserCompat.MediaItem> testPaginatedMediaItemList =
                testFullMediaItemList.subList(testPage * testPageSize,
                        Math.min((testPage + 1) * testPageSize, testFullMediaItemList.size()));
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result) {
                result.sendResult(testFullMediaItemList);
            }
        });
        MediaBrowser browser = createBrowser(null);
        ListenableFuture<BrowserResult> future = browser
                .getChildren(testParentId, testPage, testPageSize, null);
        BrowserResult result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertItemsEquals(testPaginatedMediaItemList, result.getMediaItems());
    }

    @Test
    public void testGetChildren_withoutOption() throws Exception {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        final List<MediaBrowserCompat.MediaItem> testMediaItemList = createMediaItems(testPageSize);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result) {
                fail("This isn't expected to be called");
            }

            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result, Bundle options) {
                assertEquals(testParentId, parentId);
                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                assertEquals(2, options.keySet().size());
                result.sendResult(testMediaItemList);
            }
        });

        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getChildren(testParentId, testPage, testPageSize, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertItemsEquals(testMediaItemList, result.getMediaItems());
        assertNull(result.getLibraryParams());
    }

    @Test
    public void testGetChildren_withOption() throws Exception {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        final LibraryParams testParams = createLibraryParams();
        final List<MediaBrowserCompat.MediaItem> testMediaItemList =
                createMediaItems(testPageSize / 2);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result, Bundle options) {
                assertEquals(testParentId, parentId);
                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                assertTrue(TestUtils.contains(options, testParams.getExtras()));
                result.sendResult(testMediaItemList);
            }
        });
        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getChildren(testParentId, testPage, testPageSize, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertItemsEquals(testMediaItemList, result.getMediaItems());
        assertNull(result.getLibraryParams());
    }

    @Test
    public void testGetChildren_nullResult() throws Exception {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result, Bundle options) {
                assertEquals(testParentId, parentId);
                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                result.sendResult(null);
            }
        });

        MediaBrowser browser = createBrowser(null);
        BrowserResult result = browser.getChildren(testParentId, testPage, testPageSize, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertNull(result.getLibraryParams());
    }

    @Test
    public void testSearch() throws Exception {
        prepareLooper();
        final String testQuery = "search_query";
        final int testPage = 2;
        final int testPageSize = 4;
        final LibraryParams testParams = createLibraryParams();
        final List<MediaBrowserCompat.MediaItem> testFullSearchResult = createMediaItems(
                (testPage + 1) * testPageSize + 3);
        final List<MediaBrowserCompat.MediaItem> testPaginatedSearchResult =
                testFullSearchResult.subList(testPage * testPageSize,
                        Math.min((testPage + 1) * testPageSize, testFullSearchResult.size()));

        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onSearch(String query, Bundle extras,
                    Result<List<MediaBrowserCompat.MediaItem>> result) {
                assertEquals(testQuery, query);
                assertTrue(TestUtils.contains(extras, testParams.getExtras()));
                if (extras != null && extras.getInt(MediaBrowserCompat.EXTRA_PAGE, -1) >= 0) {
                    assertEquals(testPage, extras.getInt(MediaBrowserCompat.EXTRA_PAGE));
                    assertEquals(testPageSize, extras.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                    result.sendResult(testPaginatedSearchResult);
                } else {
                    result.sendResult(testFullSearchResult);
                }
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        MediaBrowser browser = createBrowser(new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser browser, String query, int itemCount,
                    LibraryParams params) {
                assertEquals(testQuery, query);
                assertEquals(testFullSearchResult.size(), itemCount);
                assertNull(params);
                latch.countDown();
            }
        });

        BrowserResult result = browser.search(testQuery, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        result = browser.getSearchResult(testQuery, testPage, testPageSize, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertItemsEquals(testPaginatedSearchResult, result.getMediaItems());
    }

    @Test
    public void testSearch_nullResult() throws Exception {
        prepareLooper();
        final String testQuery = "search_query";
        final int testPage = 2;
        final int testPageSize = 4;

        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onSearch(String query, Bundle extras,
                    Result<List<MediaBrowserCompat.MediaItem>> result) {
                result.sendResult(null);
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        MediaBrowser browser = createBrowser(new BrowserCallback() {
            @Override
            public void onSearchResultChanged(MediaBrowser browser, String query, int itemCount,
                    LibraryParams params) {
                assertEquals(testQuery, query);
                assertEquals(0, itemCount);
                latch.countDown();
            }
        });

        BrowserResult result = browser.search(testQuery, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        result = browser.getSearchResult(testQuery, testPage, testPageSize, null)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotEquals(RESULT_CODE_SUCCESS, result.getResultCode());
    }

    /**
     * Tests following APIs
     * <ul>
     *    <li>{@link MediaBrowser#subscribe(String, LibraryParams)}</li>
     *    <li>{@link MediaBrowser#unsubscribe(String)}</li>
     *    <li>{@link MediaBrowser.BrowserCallback#onChildrenChanged}</li>
     * </ul>
     * @throws InterruptedException
     */
    @Test
    public void testSubscribeAndUnsubscribe() throws Exception {
        prepareLooper();
        final String testParentId = "testSubscribe";
        final LibraryParams testParams = createLibraryParams();
        final List<MediaBrowserCompat.MediaItem> testFullMediaItemList = createMediaItems(4);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(2);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId,
                    Result<List<MediaBrowserCompat.MediaItem>> result, Bundle option) {
                // Called by subscribe and notifyChildrenChanged()
                assertEquals(testParentId, parentId);
                assertTrue(TestUtils.equals(testParams.getExtras(), option));
                result.sendResult(testFullMediaItemList);

                // Shouldn't call notifyChildrenChanged() again here because it will call
                // onLoadChildren() again for getting list of children.
                if (subscribeLatch.getCount() > 0) {
                    subscribeLatch.countDown();
                }
            }
        });
        MediaBrowser browser = createBrowser(new MediaBrowser.BrowserCallback() {
            @Override
            public void onChildrenChanged(MediaBrowser browser, String parentId,
                    int itemCount, LibraryParams params) {
                // Triggered by both subscribe and notifyChildrenChanged().
                // Shouldn't be called after the unsubscribe().
                assertNotEquals(0, latch.getCount());
                assertEquals(testParentId, parentId);
                assertEquals(testFullMediaItemList.size(), itemCount);
                assertNull(params);
                latch.countDown();
            }
        });
        BrowserResult result = browser.subscribe(testParentId, testParams)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        MockMediaBrowserServiceCompat.getInstance().notifyChildrenChanged(testParentId);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        result = browser.unsubscribe(testParentId).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // Unsubscribe takes some time. Wait for some time.
        Thread.sleep(TIMEOUT_MS);
        MockMediaBrowserServiceCompat.getInstance().notifyChildrenChanged(testParentId);
        // This shouldn't trigger browser's onChildrenChanged().
        // Wait for some time. Exception will be thrown in the callback if error happens.
        Thread.sleep(TIMEOUT_MS);
    }

    private static MediaBrowserCompat.MediaItem createMediaItem(String mediaId) {
        final MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId).setTitle("title: " + mediaId).build();
        return new MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    private static List<MediaBrowserCompat.MediaItem> createMediaItems(int size) {
        final List<MediaBrowserCompat.MediaItem> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[2].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(createMediaItem(caller + "_child_" + i));
        }
        return list;
    }

    private static void assertItemEquals(MediaBrowserCompat.MediaItem item, MediaItem item2) {
        assertEquals(item.getMediaId(), item2.getMediaId());
        assertEquals(item.getMediaId(), item2.getMetadata().getString(
                MediaMetadata.METADATA_KEY_MEDIA_ID));
        assertEquals(item.getDescription().getTitle(),
                item2.getMetadata().getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
    }

    private static void assertItemsEquals(List<MediaBrowserCompat.MediaItem> itemList,
            List<MediaItem> item2List) {
        if (itemList == null && item2List == null) {
            return;
        }
        assertFalse(itemList == null || item2List == null);
        assertEquals(itemList.size(), item2List.size());
        for (int i = 0; i < itemList.size(); i++) {
            assertItemEquals(itemList.get(i), item2List.get(i));
        }
    }

    public class TestMediaBrowser extends MediaBrowser implements TestControllerInterface {
        private final BrowserCallback mCallback;

        public TestMediaBrowser(@NonNull Context context, @NonNull SessionToken token,
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
