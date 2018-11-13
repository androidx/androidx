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
import static androidx.media.test.lib.MediaBrowserConstants.CHILDREN_COUNT;
import static androidx.media.test.lib.MediaBrowserConstants.CUSTOM_ACTION;
import static androidx.media.test.lib.MediaBrowserConstants.CUSTOM_ACTION_EXTRAS;
import static androidx.media.test.lib.MediaBrowserConstants.GET_CHILDREN_RESULT;
import static androidx.media.test.lib.MediaBrowserConstants.LONG_LIST_COUNT;
import static androidx.media.test.lib.MediaBrowserConstants.MEDIA_ID_GET_ITEM;
import static androidx.media.test.lib.MediaBrowserConstants.PARENT_ID;
import static androidx.media.test.lib.MediaBrowserConstants.PARENT_ID_ERROR;
import static androidx.media.test.lib.MediaBrowserConstants.PARENT_ID_LONG_LIST;
import static androidx.media.test.lib.MediaBrowserConstants.PARENT_ID_NO_CHILDREN;
import static androidx.media.test.lib.MediaBrowserConstants.ROOT_EXTRAS;
import static androidx.media.test.lib.MediaBrowserConstants.ROOT_ID;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_QUERY;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_QUERY_EMPTY_RESULT;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_QUERY_ERROR;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_QUERY_LONG_LIST;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_RESULT;
import static androidx.media.test.lib.MediaBrowserConstants.SEARCH_RESULT_COUNT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.CustomActionCallback;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SearchCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;

import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaLibraryService;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether {@link MediaBrowserCompat} works well with {@link MediaLibraryService}.
 */
@SmallTest
public class MediaBrowserCompatTestWithMediaLibraryService extends
        MediaBrowserCompatTestWithMediaSessionService {
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Override
    ComponentName getServiceComponent() {
        return MOCK_MEDIA_LIBRARY_SERVICE;
    }

    @Test
    public void testGetRoot() throws InterruptedException {
        prepareLooper();
        // The MockMediaLibraryService gives MediaBrowserConstants.ROOT_ID as root ID, and
        // MediaBrowserConstants.ROOT_EXTRAS as extras.
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mBrowserCompat = new MediaBrowserCompat(mContext, getServiceComponent(),
                        mConnectionCallback, null /* rootHint */);
            }
        });
        connectAndWait();
        assertEquals(ROOT_ID, mBrowserCompat.getRoot());

        // Note: Cannot use equals() here because browser compat's extra contains server version,
        // extra binder, and extra messenger.
        assertTrue(TestUtils.contains(mBrowserCompat.getExtras(), ROOT_EXTRAS));
    }

    @Test
    public void testGetItem() throws InterruptedException {
        prepareLooper();
        final String mediaId = MEDIA_ID_GET_ITEM;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.getItem(mediaId, new ItemCallback() {
            @Override
            public void onItemLoaded(MediaItem item) {
                assertEquals(mediaId, item.getMediaId());
                assertNotNull(item);
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetItem_nullResult() throws InterruptedException {
        prepareLooper();
        final String mediaId = "random_media_id";

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.getItem(mediaId, new ItemCallback() {
            @Override
            public void onItemLoaded(MediaItem item) {
                assertNull(item);
                latch.countDown();
            }

            @Override
            public void onError(String itemId) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren() throws InterruptedException {
        prepareLooper();
        final String testParentId = PARENT_ID;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertEquals(testParentId, parentId);
                assertNotNull(children);
                assertEquals(GET_CHILDREN_RESULT.size(), children.size());

                // Compare the given results with originals.
                for (int i = 0; i < children.size(); i++) {
                    assertEquals(GET_CHILDREN_RESULT.get(i), children.get(i).getMediaId());
                }
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle option) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testGetChildren_withLongList() throws InterruptedException {
        prepareLooper();
        final String testParentId = PARENT_ID_LONG_LIST;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertEquals(testParentId, parentId);
                assertNotNull(children);
                assertTrue(children.size() < LONG_LIST_COUNT);

                // Compare the given results with originals.
                for (int i = 0; i < children.size(); i++) {
                    assertEquals(TestUtils.getMediaIdInDummyList(i), children.get(i).getMediaId());
                }
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle option) {
                fail();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void testGetChildren_withPagination() throws InterruptedException {
        prepareLooper();
        final String testParentId = PARENT_ID;
        final int page = 4;
        final int pageSize = 10;
        final Bundle extras = new Bundle();
        extras.putString(testParentId, testParentId);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        final Bundle option = new Bundle();
        option.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        option.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        mBrowserCompat.subscribe(testParentId, option, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children,
                    Bundle options) {
                assertEquals(testParentId, parentId);
                assertEquals(page, option.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(pageSize, option.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                assertNotNull(children);

                int fromIndex = page * pageSize;
                int toIndex = Math.min((page + 1) * pageSize, CHILDREN_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(GET_CHILDREN_RESULT.get(originalIndex),
                            children.get(relativeIndex).getMediaId());
                }
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren_emptyResult() throws InterruptedException {
        prepareLooper();
        final String testParentId = PARENT_ID_NO_CHILDREN;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertNotNull(children);
                assertEquals(0, children.size());
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren_nullResult() throws InterruptedException {
        prepareLooper();
        final String testParentId = PARENT_ID_ERROR;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onError(String parentId) {
                assertEquals(testParentId, parentId);
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children,
                    Bundle options) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearch() throws InterruptedException {
        prepareLooper();
        final String testQuery = SEARCH_QUERY;
        final int page = 4;
        final int pageSize = 10;
        final Bundle testExtras = new Bundle();
        testExtras.putString(testQuery, testQuery);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.search(testQuery, testExtras, new SearchCallback() {
            @Override
            public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));
                int expectedSize = Math.max(
                        Math.min(pageSize, SEARCH_RESULT_COUNT - pageSize * page),
                        0);
                assertEquals(expectedSize, items.size());

                int fromIndex = page * pageSize;
                int toIndex = Math.min((page + 1) * pageSize, SEARCH_RESULT_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(
                            SEARCH_RESULT.get(originalIndex),
                            items.get(relativeIndex).getMediaId());
                }
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testSearch_withLongList() throws InterruptedException {
        prepareLooper();
        final String testQuery = SEARCH_QUERY_LONG_LIST;
        final int page = 0;
        final int pageSize = Integer.MAX_VALUE;
        final Bundle testExtras = new Bundle();
        testExtras.putString(testQuery, testQuery);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.search(testQuery, testExtras, new SearchCallback() {
            @Override
            public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));

                assertNotNull(items);
                assertTrue(items.size() < LONG_LIST_COUNT);
                for (int i = 0; i < items.size(); i++) {
                    assertEquals(TestUtils.getMediaIdInDummyList(i), items.get(i).getMediaId());
                }
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void testSearch_emptyResult() throws InterruptedException {
        prepareLooper();
        final String testQuery = SEARCH_QUERY_EMPTY_RESULT;
        final Bundle testExtras = new Bundle();
        testExtras.putString(testQuery, testQuery);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.search(testQuery, testExtras, new SearchCallback() {
            @Override
            public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));
                assertNotNull(items);
                assertEquals(0, items.size());
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearch_error() throws InterruptedException {
        prepareLooper();
        final String testQuery = SEARCH_QUERY_ERROR;
        final Bundle testExtras = new Bundle();
        testExtras.putString(testQuery, testQuery);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.search(testQuery, testExtras, new SearchCallback() {
            @Override
            public void onError(String query, Bundle extras) {
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }

            @Override
            public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("TODO: Move this test to MediaLibrarySessionLegacyCallbackTest.")
    @Test
    public void testSubscribe() throws InterruptedException {
//        prepareLooper();
//        final String testParentId = "testSubscribeId";
//        final List<MediaItem> testList = TestUtils.createMediaItems(3);
//
//        final CountDownLatch latch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
//            @Override
//            public void onSubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId,
//                    @Nullable Bundle extras) {
//                if (Process.myUid() == info.getUid()) {
//                    assertEquals(testParentId, parentId);
//                }
//            }
//
//            @Override
//            public List<MediaItem> onGetChildren(MediaLibrarySession session,
//                    ControllerInfo controller,
//                    String parentId, int page, int pageSize, Bundle extras) {
//                assertEquals(testParentId, parentId);
//                assertEquals(0, page);
//                assertEquals(Integer.MAX_VALUE, pageSize);
//                return testList;
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(callback);
//
//        connectAndWait();
//        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
//                assertMediaItemListEquals(testList, children);
//                latch.countDown();
//            }
//
//            @Override
//            public void onError(String parentId) {
//                fail();
//            }
//        });
//        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("TODO: Move this test to MediaLibrarySessionLegacyCallbackTest.")
    @Test
    public void testSubscribe_withExtras() throws InterruptedException {
//        prepareLooper();
//        final String testParentId = "testSubscribe_withExtras";
//        final Bundle testExtras = new Bundle();
//        testExtras.putString(testParentId, testParentId);
//        final List<MediaItem> testList = TestUtils.createMediaItems(3);
//
//        final CountDownLatch latch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
//            @Override
//            public void onSubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId,
//                    @Nullable Bundle extras) {
//                if (Process.myUid() == info.getUid()) {
//                    assertEquals(testParentId, parentId);
//                    assertTrue(TestUtils.equals(testExtras, extras));
//                }
//            }
//
//            @Override
//            public List<MediaItem> onGetChildren(MediaLibrarySession session,
//                    ControllerInfo controller,
//                    String parentId, int page, int pageSize, Bundle extras) {
//                assertEquals(testParentId, parentId);
//                assertEquals(0, page);
//                assertEquals(Integer.MAX_VALUE, pageSize);
//                return testList;
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(callback);
//
//        connectAndWait();
//        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children,
//                    Bundle options) {
//                assertMediaItemListEquals(testList, children);
//                assertTrue(TestUtils.equals(testExtras, options));
//                latch.countDown();
//                super.onChildrenLoaded(parentId, children, options);
//            }
//
//            @Override
//            public void onError(String parentId) {
//                fail();
//            }
//        });
//        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("TODO: Move this test to MediaLibrarySessionLegacyCallbackTest.")
    @Test
    public void testSubscribe_withPagination() throws InterruptedException {
//        prepareLooper();
//        final String testParentId = "testSubscribe_pagination_ID";
//        final List<MediaItem> testList = TestUtils.createMediaItems(3);
//        final int testPage = 2;
//        final int testPageSize = 3;
//        final Bundle testExtras = new Bundle();
//        testExtras.putString(testParentId, testParentId);
//        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE, testPage);
//        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, testPageSize);
//
//        final CountDownLatch latch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
//            @Override
//            public void onSubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId,
//                    @Nullable Bundle extras) {
//                if (Process.myUid() == info.getUid()) {
//                    assertEquals(testParentId, parentId);
//                    assertTrue(TestUtils.equals(testExtras, extras));
//                }
//            }
//
//            @Override
//            public List<MediaItem> onGetChildren(MediaLibrarySession session,
//                    ControllerInfo controller,
//                    String parentId, int page, int pageSize, Bundle extras) {
//                assertEquals(testParentId, parentId);
//                assertEquals(testPage, page);
//                assertEquals(testPageSize, pageSize);
//                return testList;
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(callback);
//
//        connectAndWait();
//        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
//                fail();
//            }
//
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children,
//                    Bundle options) {
//                assertEquals(testParentId, parentId);
//                assertMediaItemListEquals(testList, children);
//                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
//                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
//                latch.countDown();
//            }
//
//            @Override
//            public void onError(String parentId) {
//                fail();
//            }
//
//            @Override
//            public void onError(String parentId, Bundle options) {
//                fail();
//            }
//        });
//        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("TODO: Move this test to MediaLibrarySessionLegacyCallbackTest.")
    @Test
    public void testSubscribeAndUnsubscribe() throws InterruptedException {
//        prepareLooper();
//        final String testParentId = "testUnsubscribe";
//        final Bundle testExtras = new Bundle();
//        testExtras.putString(testParentId, testParentId);
//
//        final CountDownLatch subscribeLatch = new CountDownLatch(1);
//        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
//            @Override
//            public void onSubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId,
//                    @Nullable Bundle extras) {
//                if (Process.myUid() == info.getUid()) {
//                    assertEquals(testParentId, parentId);
//                    assertTrue(TestUtils.equals(testExtras, extras));
//                    subscribeLatch.countDown();
//                }
//            }
//
//            @Override
//            public void onUnsubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId) {
//                if (Process.myUid() == info.getUid()) {
//                    assertEquals(testParentId, parentId);
//                    unsubscribeLatch.countDown();
//                }
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(callback);
//
//        connectAndWait();
//        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {});
//        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        // Subscription is needed for MediaBrowserCompat to send unsubscribe request.
//        mBrowserCompat.unsubscribe(testParentId);
//        assertTrue(unsubscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("TODO: Split this test to here and MediaLibrarySessionLegacyCallbackTest.")
    @Test
    public void testNotifyChildrenChanged() throws InterruptedException {
//        prepareLooper();
//        final String testSubscribedParentId = "testNotifyChildrenChanged";
//        final String testUnsubscribedParentId = "testNotifyChildrenChanged22";
//        final Bundle testExtras = new Bundle();
//        testExtras.putString(testSubscribedParentId, testSubscribedParentId);
//        final List<MediaItem> testList = TestUtils.createMediaItems(3);
//
//        final CountDownLatch subscribeLatch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
//            @Override
//            public void onSubscribe(@NonNull MediaLibrarySession session,
//                    @NonNull ControllerInfo info, @NonNull String parentId,
//                    @Nullable Bundle extras) {
//                if (Process.myUid() == info.getUid()) {
//                    subscribeLatch.countDown();
//                }
//            }
//
//            @Override
//            public List<MediaItem> onGetChildren(MediaLibrarySession session,
//                    ControllerInfo controller, String parentId, int page, int pageSize,
//                    Bundle extras) {
//                assertEquals(testSubscribedParentId, parentId);
//                return testList;
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(callback);
//
//        connectAndWait();
//        final CountDownLatch onChildrenLoadedLatch = new CountDownLatch(2);
//        mBrowserCompat.subscribe(testSubscribedParentId, testExtras, new SubscriptionCallback() {
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
//                assertEquals(testSubscribedParentId, parentId);
//                onChildrenLoadedLatch.countDown();
//            }
//
//            @Override
//            public void onChildrenLoaded(String parentId, List<MediaItem> children,
//                    Bundle options) {
//                super.onChildrenLoaded(parentId, children, options);
//            }
//        });
//        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        MediaLibrarySession librarySession = (MediaLibrarySession)
//                TestServiceRegistry.getInstance().getServiceInstance().getSession();
//        librarySession.notifyChildrenChanged(testSubscribedParentId, testList.size(), null);
//        librarySession.notifyChildrenChanged(testUnsubscribedParentId, testList.size(), null);
//        assertFalse(onChildrenLoadedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO: Add test for onCustomCommand() in MediaLibrarySessionLegacyCallbackTest.
    @Test
    public void testCustomAction() throws InterruptedException {
        prepareLooper();
        final Bundle testArgs = new Bundle();
        testArgs.putString("args_key", "args_value");

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.sendCustomAction(CUSTOM_ACTION, testArgs, new CustomActionCallback() {
            @Override
            public void onResult(String action, Bundle extras, Bundle resultData) {
                assertEquals(CUSTOM_ACTION, action);
                assertTrue(TestUtils.equals(testArgs, extras));
                assertTrue(TestUtils.equals(CUSTOM_ACTION_EXTRAS, resultData));
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO: Add test for onCustomCommand() in MediaLibrarySessionLegacyCallbackTest.
    @Test
    public void testCustomAction_rejected() throws InterruptedException {
        prepareLooper();
        // This action will not be allowed by the library session.
        final String testAction = "random_custom_action";

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.sendCustomAction(testAction, null, new CustomActionCallback() {
            @Override
            public void onResult(String action, Bundle extras, Bundle resultData) {
                latch.countDown();
            }
        });
        assertFalse("BrowserCompat shouldn't receive custom command",
                latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
