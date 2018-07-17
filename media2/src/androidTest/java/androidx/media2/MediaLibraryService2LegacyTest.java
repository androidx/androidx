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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.CustomActionCallback;
import android.support.v4.media.MediaBrowserCompat.ItemCallback;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SearchCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.MediaLibraryService2.LibraryRoot;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.test.filters.SmallTest;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaLibraryService2} with {@link MediaBrowserCompat}.
 */
@SmallTest
public class MediaLibraryService2LegacyTest extends MediaSessionService2LegacyTest {
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
        return new ComponentName(mContext, MockMediaLibraryService2.class);
    }

    @Test
    public void testGetRoot() throws InterruptedException {
        prepareLooper();
        final String testRootId = "testGetRoot";
        final Bundle testRootHint = new Bundle();
        testRootHint.putString(testRootId, testRootId);
        final Bundle testExtra = new Bundle();
        testExtra.putBoolean(testRootId, true);
        final LibraryRoot testLibraryRoot = new LibraryRoot("testLibraryRoot", testExtra);

        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mBrowserCompat = new MediaBrowserCompat(mContext, getServiceComponent(),
                        mConnectionCallback, testRootHint);
            }
        });
        TestServiceRegistry.getInstance().setSessionCallback(new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                if (Process.myUid() != controller.getUid()) {
                    return null;
                }
                return super.onConnect(session, controller);
            }

            @Override
            public LibraryRoot onGetLibraryRoot(MediaLibrarySession session,
                    ControllerInfo controller, Bundle rootHints) {
                assertTrue(TestUtils.equals(rootHints, testRootHint));
                return testLibraryRoot;
            }
        });

        connectAndWait();
        assertEquals(testLibraryRoot.getRootId(), mBrowserCompat.getRoot());

        // Note: Cannot use equals() here because browser compat's extra contains server version,
        // extra binder, and extra messenger.
        assertTrue(TestUtils.contains(mBrowserCompat.getExtras(), testLibraryRoot.getExtras()));
    }

    @Test
    public void testGetItem() throws InterruptedException {
        prepareLooper();
        final String mediaId = MockMediaLibraryService2.MEDIA_ID_GET_ITEM;

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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren() throws InterruptedException {
        prepareLooper();
        final String testParentId = MockMediaLibraryService2.PARENT_ID;

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertEquals(testParentId, parentId);
                assertNotNull(children);
                assertEquals(MockMediaLibraryService2.GET_CHILDREN_RESULT.size(), children.size());

                // Compare the given results with originals.
                for (int i = 0; i < children.size(); i++) {
                    assertEquals(MockMediaLibraryService2.GET_CHILDREN_RESULT.get(i).getMediaId(),
                            children.get(i).getMediaId());
                }
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle option) {
                fail();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren_withPagination() throws InterruptedException {
        prepareLooper();
        final String testParentId = MockMediaLibraryService2.PARENT_ID;
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
                int toIndex = Math.min((page + 1) * pageSize,
                        MockMediaLibraryService2.CHILDREN_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(MockMediaLibraryService2.GET_CHILDREN_RESULT.get(originalIndex)
                                    .getMediaId(),
                            children.get(relativeIndex).getMediaId());
                }
                latch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                fail();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren_emptyResult() throws InterruptedException {
        prepareLooper();
        final String testParentId = MockMediaLibraryService2.PARENT_ID_NO_CHILDREN;

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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetChildren_nullResult() throws InterruptedException {
        prepareLooper();
        final String testParentId = MockMediaLibraryService2.PARENT_ID_ERROR;

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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearch() throws InterruptedException {
        prepareLooper();
        final String testQuery = MockMediaLibraryService2.SEARCH_QUERY;
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
                int expectedSize = Math.max(Math.min(pageSize,
                        MockMediaLibraryService2.SEARCH_RESULT_COUNT - pageSize * (page - 1)), 0);
                assertEquals(expectedSize, items.size());

                int fromIndex = page * pageSize;
                int toIndex = Math.min((page + 1) * pageSize,
                        MockMediaLibraryService2.SEARCH_RESULT_COUNT);

                // Compare the given results with originals.
                for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
                    int relativeIndex = originalIndex - fromIndex;
                    assertEquals(
                            MockMediaLibraryService2.SEARCH_RESULT.get(originalIndex).getMediaId(),
                            items.get(relativeIndex).getMediaId());
                }
                latch.countDown();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSearch_emptyResult() throws InterruptedException {
        prepareLooper();
        final String testQuery = MockMediaLibraryService2.SEARCH_QUERY_EMPTY_RESULT;
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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSubscribe() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testSubscribeId";
        final List<MediaItem2> testList = TestUtils.createPlaylist(3);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onSubscribe(@NonNull MediaLibraryService2.MediaLibrarySession session,
                    @NonNull MediaSession2.ControllerInfo info, @NonNull String parentId,
                    @Nullable Bundle extras) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                }
            }

            @Override
            public List<MediaItem2> onGetChildren(MediaLibrarySession session,
                    ControllerInfo controller,
                    String parentId, int page, int pageSize, Bundle extras) {
                assertEquals(testParentId, parentId);
                assertEquals(0, page);
                assertEquals(Integer.MAX_VALUE, pageSize);
                return testList;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        mBrowserCompat.subscribe(testParentId, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertMediaItemListEquals(testList, children);
                latch.countDown();
            }

            @Override
            public void onError(String parentId) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSubscribe_withExtras() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testSubscribe_withExtras";
        final Bundle testExtras = new Bundle();
        testExtras.putString(testParentId, testParentId);
        final List<MediaItem2> testList = TestUtils.createPlaylist(3);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onSubscribe(@NonNull MediaLibraryService2.MediaLibrarySession session,
                    @NonNull MediaSession2.ControllerInfo info, @NonNull String parentId,
                    @Nullable Bundle extras) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    assertTrue(TestUtils.equals(testExtras, extras));
                }
            }

            @Override
            public List<MediaItem2> onGetChildren(MediaLibrarySession session,
                    ControllerInfo controller,
                    String parentId, int page, int pageSize, Bundle extras) {
                assertEquals(testParentId, parentId);
                assertEquals(0, page);
                assertEquals(Integer.MAX_VALUE, pageSize);
                return testList;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children,
                    Bundle options) {
                assertMediaItemListEquals(testList, children);
                assertTrue(TestUtils.equals(testExtras, options));
                latch.countDown();
                super.onChildrenLoaded(parentId, children, options);
            }

            @Override
            public void onError(String parentId) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSubscribe_withPagination() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testSubscribe_pagination_ID";
        final List<MediaItem2> testList = TestUtils.createPlaylist(3);
        final int testPage = 2;
        final int testPageSize = 3;
        final Bundle testExtras = new Bundle();
        testExtras.putString(testParentId, testParentId);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE, testPage);
        testExtras.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, testPageSize);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onSubscribe(@NonNull MediaLibraryService2.MediaLibrarySession session,
                    @NonNull MediaSession2.ControllerInfo info, @NonNull String parentId,
                    @Nullable Bundle extras) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    assertTrue(TestUtils.equals(testExtras, extras));
                }
            }

            @Override
            public List<MediaItem2> onGetChildren(MediaLibrarySession session,
                    ControllerInfo controller,
                    String parentId, int page, int pageSize, Bundle extras) {
                assertEquals(testParentId, parentId);
                assertEquals(testPage, page);
                assertEquals(testPageSize, pageSize);
                return testList;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                fail();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children,
                    Bundle options) {
                assertEquals(testParentId, parentId);
                assertMediaItemListEquals(testList, children);
                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                latch.countDown();
            }

            @Override
            public void onError(String parentId) {
                fail();
            }

            @Override
            public void onError(String parentId, Bundle options) {
                fail();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSubscribeAndUnsubscribe() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testUnsubscribe";
        final Bundle testExtras = new Bundle();
        testExtras.putString(testParentId, testParentId);

        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onSubscribe(@NonNull MediaLibraryService2.MediaLibrarySession session,
                    @NonNull MediaSession2.ControllerInfo info, @NonNull String parentId,
                    @Nullable Bundle extras) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    assertTrue(TestUtils.equals(testExtras, extras));
                    subscribeLatch.countDown();
                }
            }

            @Override
            public void onUnsubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo info, @NonNull String parentId) {
                if (Process.myUid() == info.getUid()) {
                    assertEquals(testParentId, parentId);
                    unsubscribeLatch.countDown();
                }
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        mBrowserCompat.subscribe(testParentId, testExtras, new SubscriptionCallback() {});
        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Subscription is needed for MediaBrowserCompat to send unsubscribe request.
        mBrowserCompat.unsubscribe(testParentId);
        assertTrue(unsubscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNotifyChildrenChanged() throws InterruptedException {
        prepareLooper();
        final String testSubscribedParentId = "testNotifyChildrenChanged";
        final String testUnsubscribedParentId = "testNotifyChildrenChanged22";
        final Bundle testExtras = new Bundle();
        testExtras.putString(testSubscribedParentId, testSubscribedParentId);
        final List<MediaItem2> testList = TestUtils.createPlaylist(3);

        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onSubscribe(@NonNull MediaLibraryService2.MediaLibrarySession session,
                    @NonNull MediaSession2.ControllerInfo info, @NonNull String parentId,
                    @Nullable Bundle extras) {
                if (Process.myUid() == info.getUid()) {
                    subscribeLatch.countDown();
                }
            }

            @Override
            public List<MediaItem2> onGetChildren(MediaLibrarySession session,
                    ControllerInfo controller, String parentId, int page, int pageSize,
                    Bundle extras) {
                assertEquals(testSubscribedParentId, parentId);
                return testList;
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        final CountDownLatch onChildrenLoadedLatch = new CountDownLatch(2);
        mBrowserCompat.subscribe(testSubscribedParentId, testExtras, new SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children) {
                assertEquals(testSubscribedParentId, parentId);
                onChildrenLoadedLatch.countDown();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<MediaItem> children,
                    Bundle options) {
                super.onChildrenLoaded(parentId, children, options);
            }
        });
        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        MediaLibrarySession librarySession = (MediaLibrarySession)
                TestServiceRegistry.getInstance().getServiceInstance().getSession();
        librarySession.notifyChildrenChanged(testSubscribedParentId, testList.size(), null);
        librarySession.notifyChildrenChanged(testUnsubscribedParentId, testList.size(), null);
        assertFalse(onChildrenLoadedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCustomAction() throws InterruptedException {
        prepareLooper();
        final String testAction = "testCustomAction";
        final Bundle testArgs = new Bundle();
        testArgs.putString(testAction, testAction);
        final Bundle testResult = new Bundle();
        testResult.putBoolean(testAction, true);

        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    SessionCommandGroup2 allowedCommands = new SessionCommandGroup2.Builder()
                            .addAllPredefinedCommands(
                                    SessionCommand2.COMMAND_VERSION_1)
                            .addCommand(new SessionCommand2(testAction, null))
                            .build();
                    return allowedCommands;
                }
                return null;
            }

            @Override
            public void onCustomCommand(MediaSession2 session, ControllerInfo controller,
                    SessionCommand2 customCommand, Bundle args, ResultReceiver cb) {
                assertEquals(SessionCommand2.COMMAND_CODE_CUSTOM, customCommand.getCommandCode());
                assertEquals(testAction, customCommand.getCustomCommand());
                assertTrue(TestUtils.equals(testArgs, args));
                cb.send(0, testResult);
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

        connectAndWait();
        final CountDownLatch latch = new CountDownLatch(1);
        mBrowserCompat.sendCustomAction(testAction, testArgs, new CustomActionCallback() {
            @Override
            public void onResult(String action, Bundle extras, Bundle resultData) {
                assertEquals(testAction, action);
                assertTrue(TestUtils.equals(testArgs, extras));
                assertTrue(TestUtils.equals(testResult, resultData));
                latch.countDown();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCustomAction_rejected() throws InterruptedException {
        prepareLooper();
        final String testAction = "testCustomAction";

        final MediaLibrarySessionCallback callback = new MediaLibrarySessionCallback() {
            @Override
            public void onCustomCommand(MediaSession2 session, ControllerInfo controller,
                    SessionCommand2 customCommand, Bundle args, ResultReceiver cb) {
                fail("LibrarySession shouldn't receive custom command");
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(callback);

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

    private static void assertMediaItemListEquals(List<MediaItem2> a, List<MediaItem> b) {
        if (a == null && b == null) {
            return;
        }
        assertFalse(a == null || b == null);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals("item at " + i + " are differ. " + a.get(i) + " vs " + b.get(i),
                    a.get(i).getMediaId(), b.get(i).getMediaId());
        }
    }
}
