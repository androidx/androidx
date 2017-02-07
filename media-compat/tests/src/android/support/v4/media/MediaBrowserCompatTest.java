/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.media;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Test {@link android.support.v4.media.MediaBrowserCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaBrowserCompatTest {

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;

    /**
     * To check {@link MediaBrowserCompat#unsubscribe} works properly,
     * we notify to the browser after the unsubscription that the media items have changed.
     * Then {@link MediaBrowserCompat.SubscriptionCallback#onChildrenLoaded} should not be called.
     *
     * The measured time from calling {@link StubMediaBrowserServiceCompat#notifyChildrenChanged}
     * to {@link MediaBrowserCompat.SubscriptionCallback#onChildrenLoaded} being called is about
     * 50ms.
     * So we make the thread sleep for 100ms to properly check that the callback is not called.
     */
    private static final long SLEEP_MS = 100L;
    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            "android.support.mediacompat.test",
            "android.support.v4.media.StubMediaBrowserServiceCompat");
    private static final ComponentName TEST_INVALID_BROWSER_SERVICE = new ComponentName(
            "invalid.package", "invalid.ServiceClassName");
    private final StubConnectionCallback mConnectionCallback = new StubConnectionCallback();
    private final StubSubscriptionCallback mSubscriptionCallback = new StubSubscriptionCallback();
    private final StubItemCallback mItemCallback = new StubItemCallback();

    private MediaBrowserCompat mMediaBrowser;

    @Test
    @SmallTest
    public void testMediaBrowser() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        assertEquals(false, mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertEquals(true, mMediaBrowser.isConnected());

        assertEquals(TEST_BROWSER_SERVICE, mMediaBrowser.getServiceComponent());
        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, mMediaBrowser.getRoot());
        assertEquals(StubMediaBrowserServiceCompat.EXTRAS_VALUE,
                mMediaBrowser.getExtras().getString(StubMediaBrowserServiceCompat.EXTRAS_KEY));
        assertEquals(StubMediaBrowserServiceCompat.sSession.getSessionToken(),
                mMediaBrowser.getSessionToken());

        mMediaBrowser.disconnect();
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return !mMediaBrowser.isConnected();
            }
        }.run();
    }

    @Test
    @SmallTest
    public void testConnectTwice() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        try {
            mMediaBrowser.connect();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    @SmallTest
    public void testConnectionFailed() {
        resetCallbacks();
        createMediaBrowser(TEST_INVALID_BROWSER_SERVICE);

        mMediaBrowser.connect();
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mConnectionCallback.mConnectionFailedCount > 0
                        && mConnectionCallback.mConnectedCount == 0
                        && mConnectionCallback.mConnectionSuspendedCount == 0;
            }
        }.run();
    }

    @Test
    @SmallTest
    public void testGetServiceComponentBeforeConnection() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        try {
            ComponentName serviceComponent = mMediaBrowser.getServiceComponent();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    @SmallTest
    public void testSubscribe() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, mSubscriptionCallback);
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mSubscriptionCallback.mChildrenLoadedCount > 0;
            }
        }.run();

        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                mSubscriptionCallback.mLastParentId);
        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length,
                mSubscriptionCallback.mLastChildMediaItems.size());
        for (int i = 0; i < StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length; ++i) {
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[i],
                    mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
        }

        // Test unsubscribe.
        resetCallbacks();
        mMediaBrowser.unsubscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        StubMediaBrowserServiceCompat.sInstance.notifyChildrenChanged(
                StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException occurred.");
        }
        // onChildrenLoaded should not be called.
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @SmallTest
    public void testSubscribeWithOptions() {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        final int pageSize = 3;
        final int lastPage =
                (StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length - 1) / pageSize;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        for (int page = 0; page <= lastPage; ++page) {
            resetCallbacks();
            options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options,
                    mSubscriptionCallback);
            new PollingCheck(TIME_OUT_MS) {
                @Override
                protected boolean check() {
                    return mSubscriptionCallback.mChildrenLoadedWithOptionCount > 0;
                }
            }.run();
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                    mSubscriptionCallback.mLastParentId);
            if (page != lastPage) {
                assertEquals(pageSize, mSubscriptionCallback.mLastChildMediaItems.size());
            } else {
                assertEquals(
                        (StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length - 1) % pageSize + 1,
                        mSubscriptionCallback.mLastChildMediaItems.size());
            }
            // Check whether all the items in the current page are loaded.
            for (int i = 0; i < mSubscriptionCallback.mLastChildMediaItems.size(); ++i) {
                assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[page * pageSize + i],
                        mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
            }
        }

        // Test unsubscribe with callback argument.
        resetCallbacks();
        mMediaBrowser.unsubscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                mSubscriptionCallback);

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        StubMediaBrowserServiceCompat.sInstance.notifyChildrenChanged(
                StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException occurred.");
        }
        // onChildrenLoaded should not be called.
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @SmallTest
    public void testSubscribeInvalidItem() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                mSubscriptionCallback);
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mSubscriptionCallback.mLastErrorId != null;
            }
        }.run();

        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                mSubscriptionCallback.mLastErrorId);
    }

    @Test
    @SmallTest
    public void testSubscribeInvalidItemWithOptions() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();

        final int pageSize = 5;
        final int page = 2;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID, options,
                mSubscriptionCallback);
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mSubscriptionCallback.mLastErrorId != null;
            }
        }.run();

        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                mSubscriptionCallback.mLastErrorId);
        assertEquals(page,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE));
        assertEquals(pageSize,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
    }

    @Test
    @SmallTest
    public void testUnsubscribeForMultipleSubscriptions() {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        final List<StubSubscriptionCallback> subscriptionCallbacks = new ArrayList<>();
        final int pageSize = 1;

        // Subscribe four pages, one item per page.
        for (int page = 0; page < 4; page++) {
            final StubSubscriptionCallback callback = new StubSubscriptionCallback();
            subscriptionCallbacks.add(callback);

            Bundle options = new Bundle();
            options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
            options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options, callback);

            // Each onChildrenLoaded() must be called.
            new PollingCheck(TIME_OUT_MS) {
                @Override
                protected boolean check() {
                    return callback.mChildrenLoadedWithOptionCount == 1;
                }
            }.run();
        }

        // Reset callbacks and unsubscribe.
        for (StubSubscriptionCallback callback : subscriptionCallbacks) {
            callback.reset();
        }
        mMediaBrowser.unsubscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        StubMediaBrowserServiceCompat.sInstance.notifyChildrenChanged(
                StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException occurred.");
        }

        // onChildrenLoaded should not be called.
        for (StubSubscriptionCallback callback : subscriptionCallbacks) {
            assertEquals(0, callback.mChildrenLoadedWithOptionCount);
        }
    }

    @Test
    @SmallTest
    public void testUnsubscribeWithSubscriptionCallbackForMultipleSubscriptions() {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        final List<StubSubscriptionCallback> subscriptionCallbacks = new ArrayList<>();
        final int pageSize = 1;

        // Subscribe four pages, one item per page.
        for (int page = 0; page < 4; page++) {
            final StubSubscriptionCallback callback = new StubSubscriptionCallback();
            subscriptionCallbacks.add(callback);

            Bundle options = new Bundle();
            options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
            options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options, callback);

            // Each onChildrenLoaded() must be called.
            new PollingCheck(TIME_OUT_MS) {
                @Override
                protected boolean check() {
                    return callback.mChildrenLoadedWithOptionCount == 1;
                }
            }.run();
        }

        // Unsubscribe existing subscriptions one-by-one.
        final int[] orderOfRemovingCallbacks = {2, 0, 3, 1};
        for (int i = 0; i < orderOfRemovingCallbacks.length; i++) {
            // Reset callbacks
            for (StubSubscriptionCallback callback : subscriptionCallbacks) {
                callback.reset();
            }

            // Remove one subscription
            mMediaBrowser.unsubscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                    subscriptionCallbacks.get(orderOfRemovingCallbacks[i]));

            // Make StubMediaBrowserServiceCompat notify that the children are changed.
            StubMediaBrowserServiceCompat.sInstance.notifyChildrenChanged(
                    StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                fail("Unexpected InterruptedException occurred.");
            }

            // Only the remaining subscriptionCallbacks should be called.
            for (int j = 0; j < 4; j++) {
                int childrenLoadedWithOptionsCount = subscriptionCallbacks
                        .get(orderOfRemovingCallbacks[j]).mChildrenLoadedWithOptionCount;
                if (j <= i) {
                    assertEquals(0, childrenLoadedWithOptionsCount);
                } else {
                    assertEquals(1, childrenLoadedWithOptionsCount);
                }
            }
        }
    }

    @Test
    @SmallTest
    public void testGetItem() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        mMediaBrowser.getItem(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0], mItemCallback);
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mItemCallback.mLastMediaItem != null;
            }
        }.run();

        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0],
                mItemCallback.mLastMediaItem.getMediaId());
    }

    // TODO(hdmoon): Uncomment after fixing failing tests. (Fails on Android O)
    // @Test
    // @SmallTest
    public void testGetItemFailure() {
        resetCallbacks();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        mMediaBrowser.getItem(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID, mItemCallback);
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mItemCallback.mLastErrorId != null;
            }
        }.run();

        assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID, mItemCallback.mLastErrorId);
    }

    private void createMediaBrowser(final ComponentName component) {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowserCompat(getInstrumentation().getTargetContext(),
                        component, mConnectionCallback, null);
            }
        });
    }

    private void connectMediaBrowserService() {
        mMediaBrowser.connect();
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mConnectionCallback.mConnectedCount > 0;
            }
        }.run();
    }

    private void resetCallbacks() {
        mConnectionCallback.reset();
        mSubscriptionCallback.reset();
        mItemCallback.reset();
    }

    private static class StubConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        volatile int mConnectedCount;
        volatile int mConnectionFailedCount;
        volatile int mConnectionSuspendedCount;

        public void reset() {
            mConnectedCount = 0;
            mConnectionFailedCount = 0;
            mConnectionSuspendedCount = 0;
        }

        @Override
        public void onConnected() {
            mConnectedCount++;
        }

        @Override
        public void onConnectionFailed() {
            mConnectionFailedCount++;
        }

        @Override
        public void onConnectionSuspended() {
            mConnectionSuspendedCount++;
        }
    }

    private static class StubSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        private volatile int mChildrenLoadedCount;
        private volatile int mChildrenLoadedWithOptionCount;
        private volatile String mLastErrorId;
        private volatile String mLastParentId;
        private volatile Bundle mLastOptions;
        private volatile List<MediaItem> mLastChildMediaItems;

        public void reset() {
            mChildrenLoadedCount = 0;
            mChildrenLoadedWithOptionCount = 0;
            mLastErrorId = null;
            mLastParentId = null;
            mLastOptions = null;
            mLastChildMediaItems = null;
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
            mChildrenLoadedCount++;
            mLastParentId = parentId;
            mLastChildMediaItems = children;
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options) {
            mChildrenLoadedWithOptionCount++;
            mLastParentId = parentId;
            mLastOptions = options;
            mLastChildMediaItems = children;
        }

        @Override
        public void onError(String id) {
            mLastErrorId = id;
        }

        @Override
        public void onError(String id, Bundle options) {
            mLastErrorId = id;
            mLastOptions = options;
        }
    }

    private static class StubItemCallback extends MediaBrowserCompat.ItemCallback {
        private volatile MediaItem mLastMediaItem;
        private volatile String mLastErrorId;

        public void reset() {
            mLastMediaItem = null;
            mLastErrorId = null;
        }

        @Override
        public void onItemLoaded(MediaItem item) {
            mLastMediaItem = item;
        }

        @Override
        public void onError(String id) {
            mLastErrorId = id;
        }
    }
}
