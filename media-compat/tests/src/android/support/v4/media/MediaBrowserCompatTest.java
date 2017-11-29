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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.testutils.PollingCheck;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import org.junit.Before;
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
    private static final ComponentName TEST_REMOTE_BROWSER_SERVICE = new ComponentName(
            "android.support.mediacompat.test",
            "android.support.v4.media.StubRemoteMediaBrowserServiceCompat");
    private static final ComponentName TEST_INVALID_BROWSER_SERVICE = new ComponentName(
            "invalid.package", "invalid.ServiceClassName");

    private MediaBrowserCompat mMediaBrowser;
    private StubConnectionCallback mConnectionCallback;
    private StubSubscriptionCallback mSubscriptionCallback;
    private StubItemCallback mItemCallback;

    @Before
    public void setUp() {
        mConnectionCallback = new StubConnectionCallback();
        mSubscriptionCallback = new StubSubscriptionCallback();
        mItemCallback = new StubItemCallback();
    }

    @Test
    @SmallTest
    public void testMediaBrowser() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        assertFalse(mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertTrue(mMediaBrowser.isConnected());

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
    public void testMediaBrowserWithRemoteService() throws Exception {
        createMediaBrowser(TEST_REMOTE_BROWSER_SERVICE);
        assertFalse(mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertTrue(mMediaBrowser.isConnected());

        assertEquals(TEST_REMOTE_BROWSER_SERVICE, mMediaBrowser.getServiceComponent());
        assertEquals(StubRemoteMediaBrowserServiceCompat.MEDIA_ID_ROOT, mMediaBrowser.getRoot());
        assertEquals(StubRemoteMediaBrowserServiceCompat.EXTRAS_VALUE,
                mMediaBrowser.getExtras().getString(
                        StubRemoteMediaBrowserServiceCompat.EXTRAS_KEY));

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
    public void testSessionReadyWithRemoteService() throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            // This test is for API 21+
            return;
        }

        createMediaBrowser(TEST_REMOTE_BROWSER_SERVICE);
        assertFalse(mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertTrue(mMediaBrowser.isConnected());

        // Create a session token by removing the extra binder of the token from MediaBrowserCompat.
        final MediaSessionCompat.Token tokenWithoutExtraBinder = MediaSessionCompat.Token.fromToken(
                mMediaBrowser.getSessionToken().getToken());

        final MediaControllerCallback callback = new MediaControllerCallback();
        synchronized (callback.mWaitLock) {
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    try {
                        MediaControllerCompat controller = new MediaControllerCompat(
                                getInstrumentation().getTargetContext(), tokenWithoutExtraBinder);
                        controller.registerCallback(callback, new Handler());
                        assertFalse(controller.isSessionReady());
                    } catch (Exception e) {
                        fail();
                    }
                }
            });
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSessionReadyCalled);
        }

        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testSubscriptionWithCustomOptionsWithRemoteService() throws Exception {
        final String mediaId = "1000";
        createMediaBrowser(TEST_REMOTE_BROWSER_SERVICE);
        assertFalse(mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertTrue(mMediaBrowser.isConnected());

        MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "title")
                .putRating(MediaMetadataCompat.METADATA_KEY_RATING,
                        RatingCompat.newPercentageRating(0.5f))
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING,
                        RatingCompat.newPercentageRating(0.8f))
                .build();
        Bundle options = new Bundle();
        options.putParcelable(
                StubRemoteMediaBrowserServiceCompat.MEDIA_METADATA, mediaMetadataCompat);

        // Remote MediaBrowserService will create a media item with the given MediaMetadataCompat
        mMediaBrowser.subscribe(mMediaBrowser.getRoot(), options, mSubscriptionCallback);
        synchronized (mSubscriptionCallback.mWaitLock) {
            mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(1, mSubscriptionCallback.mChildrenLoadedWithOptionCount);
            assertEquals(1, mSubscriptionCallback.mLastChildMediaItems.size());
            assertEquals(mediaId, mSubscriptionCallback.mLastChildMediaItems.get(0).getMediaId());
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testConnectTwice() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        try {
            mMediaBrowser.connect();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testConnectionFailed() throws Exception {
        createMediaBrowser(TEST_INVALID_BROWSER_SERVICE);

        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
        }
        assertTrue(mConnectionCallback.mConnectionFailedCount > 0);
        assertEquals(0, mConnectionCallback.mConnectedCount);
        assertEquals(0, mConnectionCallback.mConnectionSuspendedCount);
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testReconnection() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser.connect();
                // Reconnect before the first connection was established.
                mMediaBrowser.disconnect();
                mMediaBrowser.connect();
            }
        });

        synchronized (mConnectionCallback.mWaitLock) {
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(1, mConnectionCallback.mConnectedCount);
        }

        synchronized (mSubscriptionCallback.mWaitLock) {
            // Test subscribe.
            resetCallbacks();
            mMediaBrowser.subscribe(
                    StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, mSubscriptionCallback);
            mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mChildrenLoadedCount > 0);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                    mSubscriptionCallback.mLastParentId);
        }

        synchronized (mItemCallback.mWaitLock) {
            // Test getItem.
            resetCallbacks();
            mMediaBrowser.getItem(
                    StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0], mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0],
                    mItemCallback.mLastMediaItem.getMediaId());
        }

        // Reconnect after connection was established.
        mMediaBrowser.disconnect();
        resetCallbacks();
        connectMediaBrowserService();

        synchronized (mItemCallback.mWaitLock) {
            // Test getItem.
            resetCallbacks();
            mMediaBrowser.getItem(
                    StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0], mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0],
                    mItemCallback.mLastMediaItem.getMediaId());
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testConnectionCallbackNotCalledAfterDisconnect() {
        createMediaBrowser(TEST_BROWSER_SERVICE);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser.connect();
                mMediaBrowser.disconnect();
                resetCallbacks();
            }
        });

        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException occurred.");
        }
        assertEquals(0, mConnectionCallback.mConnectedCount);
        assertEquals(0, mConnectionCallback.mConnectionFailedCount);
        assertEquals(0, mConnectionCallback.mConnectionSuspendedCount);
    }

    @Test
    @SmallTest
    public void testGetServiceComponentBeforeConnection() {
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
    public void testSubscribe() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();

        synchronized (mSubscriptionCallback.mWaitLock) {
            mMediaBrowser.subscribe(
                    StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, mSubscriptionCallback);
            mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mChildrenLoadedCount > 0);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                    mSubscriptionCallback.mLastParentId);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length,
                    mSubscriptionCallback.mLastChildMediaItems.size());
            for (int i = 0; i < StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length; ++i) {
                assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[i],
                        mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
            }
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
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testSubscribeWithOptions() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        final int pageSize = 3;
        final int lastPage =
                (StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length - 1) / pageSize;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);

        synchronized (mSubscriptionCallback.mWaitLock) {
            for (int page = 0; page <= lastPage; ++page) {
                resetCallbacks();
                options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
                mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options,
                        mSubscriptionCallback);
                mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
                assertTrue(mSubscriptionCallback.mChildrenLoadedWithOptionCount > 0);
                assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT,
                        mSubscriptionCallback.mLastParentId);
                if (page != lastPage) {
                    assertEquals(pageSize, mSubscriptionCallback.mLastChildMediaItems.size());
                } else {
                    assertEquals(
                            (StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN.length - 1) % pageSize
                                    + 1,
                            mSubscriptionCallback.mLastChildMediaItems.size());
                }
                // Check whether all the items in the current page are loaded.
                for (int i = 0; i < mSubscriptionCallback.mLastChildMediaItems.size(); ++i) {
                    assertEquals(
                            StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[page * pageSize + i],
                            mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
                }
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
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testSubscribeInvalidItem() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();

        synchronized (mSubscriptionCallback.mWaitLock) {
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                    mSubscriptionCallback);
            mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                    mSubscriptionCallback.mLastErrorId);
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testSubscribeInvalidItemWithOptions() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();

        final int pageSize = 5;
        final int page = 2;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);

        synchronized (mSubscriptionCallback.mWaitLock) {
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID, options,
                    mSubscriptionCallback);
            mSubscriptionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID,
                    mSubscriptionCallback.mLastErrorId);
            assertNotNull(mSubscriptionCallback.mLastOptions);
            assertEquals(page,
                    mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE));
            assertEquals(pageSize,
                    mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testUnsubscribeForMultipleSubscriptions() throws Exception {
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
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options,
                    callback);
            synchronized (callback.mWaitLock) {
                callback.mWaitLock.wait(TIME_OUT_MS);
            }
            // Each onChildrenLoaded() must be called.
            assertEquals(1, callback.mChildrenLoadedWithOptionCount);
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
        mMediaBrowser.disconnect();
    }

    @Test
    @MediumTest
    public void testUnsubscribeWithSubscriptionCallbackForMultipleSubscriptions() throws Exception {
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
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options,
                    callback);
            synchronized (callback.mWaitLock) {
                callback.mWaitLock.wait(TIME_OUT_MS);
            }
            // Each onChildrenLoaded() must be called.
            assertEquals(1, callback.mChildrenLoadedWithOptionCount);
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
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testGetItem() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();

        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0],
                    mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertNotNull(mItemCallback.mLastMediaItem);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN[0],
                    mItemCallback.mLastMediaItem.getMediaId());
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @LargeTest
    public void testGetItemWhenOnLoadItemIsNotImplemented() throws Exception {
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(
                    StubMediaBrowserServiceCompat.MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED,
                    mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(StubMediaBrowserServiceCompat.MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED,
                    mItemCallback.mLastErrorId);
        }
        mMediaBrowser.disconnect();
    }

    @Test
    @SmallTest
    public void testGetItemWhenMediaIdIsInvalid() throws Exception {
        mItemCallback.mLastMediaItem = new MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId("dummy_id").build(), MediaItem.FLAG_BROWSABLE);

        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(StubMediaBrowserServiceCompat.MEDIA_ID_INVALID, mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertNull(mItemCallback.mLastMediaItem);
            assertNull(mItemCallback.mLastErrorId);
        }
        mMediaBrowser.disconnect();
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

    private void connectMediaBrowserService() throws Exception {
        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
        }
    }

    private void resetCallbacks() {
        mConnectionCallback.reset();
        mSubscriptionCallback.reset();
        mItemCallback.reset();
    }

    private class StubConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        Object mWaitLock = new Object();
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
            synchronized (mWaitLock) {
                mConnectedCount++;
                mWaitLock.notify();
            }
        }

        @Override
        public void onConnectionFailed() {
            synchronized (mWaitLock) {
                mConnectionFailedCount++;
                mWaitLock.notify();
            }
        }

        @Override
        public void onConnectionSuspended() {
            synchronized (mWaitLock) {
                mConnectionSuspendedCount++;
                mWaitLock.notify();
            }
        }
    }

    private class StubSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        Object mWaitLock = new Object();
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
            synchronized (mWaitLock) {
                mChildrenLoadedCount++;
                mLastParentId = parentId;
                mLastChildMediaItems = children;
                mWaitLock.notify();
            }
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options) {
            synchronized (mWaitLock) {
                mChildrenLoadedWithOptionCount++;
                mLastParentId = parentId;
                mLastOptions = options;
                mLastChildMediaItems = children;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(String id) {
            synchronized (mWaitLock) {
                mLastErrorId = id;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(String id, Bundle options) {
            synchronized (mWaitLock) {
                mLastErrorId = id;
                mLastOptions = options;
                mWaitLock.notify();
            }
        }
    }

    private class StubItemCallback extends MediaBrowserCompat.ItemCallback {
        Object mWaitLock = new Object();
        private volatile MediaItem mLastMediaItem;
        private volatile String mLastErrorId;

        public void reset() {
            mLastMediaItem = null;
            mLastErrorId = null;
        }

        @Override
        public void onItemLoaded(MediaItem item) {
            synchronized (mWaitLock) {
                mLastMediaItem = item;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(String id) {
            synchronized (mWaitLock) {
                mLastErrorId = id;
                mWaitLock.notify();
            }
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        Object mWaitLock = new Object();
        private volatile boolean mOnSessionReadyCalled;

        @Override
        public void onSessionReady() {
            synchronized (mWaitLock) {
                mOnSessionReadyCalled = true;
                mWaitLock.notify();
            }
        }
    }
}
