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

package android.support.mediacompat.client;

import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_KEY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_VALUE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN_DELAYED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_INVALID;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ROOT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEND_DELAYED_ITEM_LOADED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEND_DELAYED_NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SET_SESSION_TOKEN;
import static android.support.mediacompat.testlib.VersionConstants.KEY_SERVICE_VERSION;
import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;
import static android.support.mediacompat.testlib.util.IntentUtil.callMediaBrowserServiceMethod;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getArguments;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.Build;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test connection between framework {@link MediaBrowser} and
 * {@link androidx.media.MediaBrowserServiceCompat}.
 *
 * TODO: Lower the minSdkVersion of this test to LOLLIPOP.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
public class MediaBrowserTest {

    private static final String TAG = "MediaBrowserTest";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final long WAIT_TIME_FOR_NO_RESPONSE_MS = 300L;

    /**
     * To check {@link MediaBrowser#unsubscribe} works properly,
     * we notify to the browser after the unsubscription that the media items have changed.
     * Then {@link MediaBrowser.SubscriptionCallback#onChildrenLoaded} should not be called.
     *
     * The measured time from calling {@link MediaBrowserService#notifyChildrenChanged}
     * to {@link MediaBrowser.SubscriptionCallback#onChildrenLoaded} being called is about
     * 50ms.
     * So we make the thread sleep for 100ms to properly check that the callback is not called.
     */
    private static final long SLEEP_MS = 100L;
    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME,
            "android.support.mediacompat.service.StubMediaBrowserServiceCompat");
    private static final ComponentName TEST_BROWSER_SERVICE_DELAYED_MEDIA_SESSION =
            new ComponentName(
                    SERVICE_PACKAGE_NAME,
                    "android.support.mediacompat.service"
                            + ".StubMediaBrowserServiceCompatWithDelayedMediaSession");
    private static final ComponentName TEST_INVALID_BROWSER_SERVICE = new ComponentName(
            "invalid.package", "invalid.ServiceClassName");

    private String mServiceVersion;
    private MediaBrowser mMediaBrowser;
    private StubConnectionCallback mConnectionCallback;
    private StubSubscriptionCallback mSubscriptionCallback;
    private StubItemCallback mItemCallback;
    private Bundle mRootHints;

    @Before
    public void setUp() {
        // The version of the service app is provided through the instrumentation arguments.
        mServiceVersion = getArguments().getString(KEY_SERVICE_VERSION, "");
        Log.d(TAG, "Service app version: " + mServiceVersion);

        mConnectionCallback = new StubConnectionCallback();
        mSubscriptionCallback = new StubSubscriptionCallback();
        mItemCallback = new StubItemCallback();

        mRootHints = new Bundle();
        mRootHints.putBoolean(MediaBrowserService.BrowserRoot.EXTRA_RECENT, true);
        mRootHints.putBoolean(MediaBrowserService.BrowserRoot.EXTRA_OFFLINE, true);
        mRootHints.putBoolean(MediaBrowserService.BrowserRoot.EXTRA_SUGGESTED, true);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowser(getInstrumentation().getTargetContext(),
                        TEST_BROWSER_SERVICE, mConnectionCallback, mRootHints);
            }
        });
    }

    @After
    public void tearDown() {
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
        }
    }

    @Test
    @SmallTest
    public void testBrowserRoot() {
        final String id = "test-id";
        final String key = "test-key";
        final String val = "test-val";
        final Bundle extras = new Bundle();
        extras.putString(key, val);

        MediaBrowserService.BrowserRoot browserRoot =
                new MediaBrowserService.BrowserRoot(id, extras);
        assertEquals(id, browserRoot.getRootId());
        assertEquals(val, browserRoot.getExtras().getString(key));
    }

    @Test
    @SmallTest
    public void testMediaBrowser() throws Exception {
        assertFalse(mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertTrue(mMediaBrowser.isConnected());

        assertEquals(TEST_BROWSER_SERVICE, mMediaBrowser.getServiceComponent());
        assertEquals(MEDIA_ID_ROOT, mMediaBrowser.getRoot());
        assertEquals(EXTRAS_VALUE, mMediaBrowser.getExtras().getString(EXTRAS_KEY));

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
    public void testGetServiceComponentBeforeConnection() {
        try {
            ComponentName serviceComponent = mMediaBrowser.getServiceComponent();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    @SmallTest
    public void testConnectionFailed() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowser(getInstrumentation().getTargetContext(),
                        TEST_INVALID_BROWSER_SERVICE, mConnectionCallback, mRootHints);
            }
        });

        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
        }
        assertEquals(1, mConnectionCallback.mConnectionFailedCount);
        assertEquals(0, mConnectionCallback.mConnectedCount);
        assertEquals(0, mConnectionCallback.mConnectionSuspendedCount);
    }

    @Test
    @SmallTest
    public void testConnectTwice() throws Exception {
        connectMediaBrowserService();
        try {
            mMediaBrowser.connect();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    @MediumTest
    public void testReconnection() throws Exception {
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

        // Test subscribe.
        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_ROOT, mSubscriptionCallback);
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(1, mSubscriptionCallback.mChildrenLoadedCount);
        assertEquals(MEDIA_ID_ROOT, mSubscriptionCallback.mLastParentId);

        synchronized (mItemCallback.mWaitLock) {
            // Test getItem.
            mItemCallback.reset();
            mMediaBrowser.getItem(MEDIA_ID_CHILDREN[0], mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(MEDIA_ID_CHILDREN[0], mItemCallback.mLastMediaItem.getMediaId());
        }

        // Reconnect after connection was established.
        mMediaBrowser.disconnect();
        connectMediaBrowserService();

        synchronized (mItemCallback.mWaitLock) {
            // Test getItem.
            mItemCallback.reset();
            mMediaBrowser.getItem(MEDIA_ID_CHILDREN[0], mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(MEDIA_ID_CHILDREN[0], mItemCallback.mLastMediaItem.getMediaId());
        }
    }

    @Test
    @MediumTest
    public void testConnectionCallbackNotCalledAfterDisconnect() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser.connect();
                mMediaBrowser.disconnect();
                mConnectionCallback.reset();
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
    @MediumTest
    public void testMultipleConnections() throws Exception {
        final Context context = getInstrumentation().getTargetContext();
        final StubConnectionCallback callback1 = new StubConnectionCallback();
        final StubConnectionCallback callback2 = new StubConnectionCallback();
        final StubConnectionCallback callback3 = new StubConnectionCallback();
        final List<MediaBrowser> browserList = new ArrayList<>();

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                MediaBrowser browser1 = new MediaBrowser(context, TEST_BROWSER_SERVICE,
                        callback1, new Bundle());
                MediaBrowser browser2 = new MediaBrowser(context, TEST_BROWSER_SERVICE,
                        callback2, new Bundle());
                MediaBrowser browser3 = new MediaBrowser(context, TEST_BROWSER_SERVICE,
                        callback3, new Bundle());

                browserList.add(browser1);
                browserList.add(browser2);
                browserList.add(browser3);

                browser1.connect();
                browser2.connect();
                browser3.connect();
            }
        });

        try {
            new PollingCheck(TIME_OUT_MS) {
                @Override
                protected boolean check() {
                    return callback1.mConnectedCount == 1
                            && callback2.mConnectedCount == 1
                            && callback3.mConnectedCount == 1;
                }
            }.run();
        } finally {
            for (int i = 0; i < browserList.size(); i++) {
                MediaBrowser browser = browserList.get(i);
                if (browser.isConnected()) {
                    browser.disconnect();
                }
            }
        }
    }

    @Test
    @MediumTest
    public void testSubscribe() throws Exception {
        connectMediaBrowserService();

        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_ROOT, mSubscriptionCallback);
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(1, mSubscriptionCallback.mChildrenLoadedCount);
        assertEquals(MEDIA_ID_ROOT, mSubscriptionCallback.mLastParentId);
        assertEquals(MEDIA_ID_CHILDREN.length, mSubscriptionCallback.mLastChildMediaItems.size());
        for (int i = 0; i < MEDIA_ID_CHILDREN.length; ++i) {
            assertEquals(MEDIA_ID_CHILDREN[i],
                    mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
        }

        // Test MediaBrowserService.notifyChildrenChanged()
        mSubscriptionCallback.reset(1);
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                getApplicationContext());
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(1, mSubscriptionCallback.mChildrenLoadedCount);

        // Test unsubscribe.
        mSubscriptionCallback.reset(1);
        mMediaBrowser.unsubscribe(MEDIA_ID_ROOT);

        // After unsubscribing, make StubMediaBrowserService notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                getApplicationContext());
        mSubscriptionCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS);

        // onChildrenLoaded should not be called.
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 26)
    public void testSubscribeWithOptions() throws Exception {
        connectMediaBrowserService();
        final int pageSize = 3;
        final int lastPage = (MEDIA_ID_CHILDREN.length - 1) / pageSize;
        Bundle options = new Bundle();
        options.putInt(MediaBrowser.EXTRA_PAGE_SIZE, pageSize);

        for (int page = 0; page <= lastPage; ++page) {
            mSubscriptionCallback.reset(1);
            options.putInt(MediaBrowser.EXTRA_PAGE, page);
            mMediaBrowser.subscribe(MEDIA_ID_ROOT, options, mSubscriptionCallback);
            assertTrue(mSubscriptionCallback.await(TIME_OUT_MS));
            assertEquals(1, mSubscriptionCallback.mChildrenLoadedWithOptionCount);
            assertEquals(MEDIA_ID_ROOT, mSubscriptionCallback.mLastParentId);
            if (page != lastPage) {
                assertEquals(pageSize, mSubscriptionCallback.mLastChildMediaItems.size());
            } else {
                assertEquals((MEDIA_ID_CHILDREN.length - 1) % pageSize + 1,
                        mSubscriptionCallback.mLastChildMediaItems.size());
            }
            // Check whether all the items in the current page are loaded.
            for (int i = 0; i < mSubscriptionCallback.mLastChildMediaItems.size(); ++i) {
                assertEquals(MEDIA_ID_CHILDREN[page * pageSize + i],
                        mSubscriptionCallback.mLastChildMediaItems.get(i).getMediaId());
            }

            // Test MediaBrowserService.notifyChildrenChanged()
            mSubscriptionCallback.reset(page + 1);
            callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                    getApplicationContext());
            assertTrue(mSubscriptionCallback.await(TIME_OUT_MS * (page + 1)));
            assertEquals(page + 1, mSubscriptionCallback.mChildrenLoadedWithOptionCount);
        }

        // Test unsubscribe with callback argument.
        mSubscriptionCallback.reset(1);
        mMediaBrowser.unsubscribe(MEDIA_ID_ROOT, mSubscriptionCallback);

        // After unsubscribing, make StubMediaBrowserService notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                getApplicationContext());
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException occurred.");
        }
        // onChildrenLoaded should not be called.
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @MediumTest
    public void testSubscribeDelayedItems() throws Exception {
        connectMediaBrowserService();

        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_CHILDREN_DELAYED, mSubscriptionCallback);
        assertFalse(mSubscriptionCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS));
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);

        callMediaBrowserServiceMethod(
                SEND_DELAYED_NOTIFY_CHILDREN_CHANGED, MEDIA_ID_CHILDREN_DELAYED,
                getApplicationContext());
        assertTrue(mSubscriptionCallback.await(TIME_OUT_MS));
        assertEquals(1, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @SmallTest
    public void testSubscribeInvalidItem() throws Exception {
        connectMediaBrowserService();

        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_INVALID, mSubscriptionCallback);
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(MEDIA_ID_INVALID, mSubscriptionCallback.mLastErrorId);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testSubscribeInvalidItemWithOptions() throws Exception {
        connectMediaBrowserService();

        final int pageSize = 5;
        final int page = 2;
        Bundle options = new Bundle();
        options.putInt(MediaBrowser.EXTRA_PAGE_SIZE, pageSize);
        options.putInt(MediaBrowser.EXTRA_PAGE, page);

        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_INVALID, options, mSubscriptionCallback);
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(MEDIA_ID_INVALID, mSubscriptionCallback.mLastErrorId);
        assertNotNull(mSubscriptionCallback.mLastOptions);
        assertEquals(page,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowser.EXTRA_PAGE));
        assertEquals(pageSize,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowser.EXTRA_PAGE_SIZE));
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 24)
    public void testUnsubscribeForMultipleSubscriptions() throws Exception {
        connectMediaBrowserService();
        final List<StubSubscriptionCallback> subscriptionCallbacks = new ArrayList<>();
        final int pageSize = 1;

        // Subscribe four pages, one item per page.
        for (int page = 0; page < 4; page++) {
            final StubSubscriptionCallback callback = new StubSubscriptionCallback();
            subscriptionCallbacks.add(callback);

            Bundle options = new Bundle();
            options.putInt(MediaBrowser.EXTRA_PAGE, page);
            options.putInt(MediaBrowser.EXTRA_PAGE_SIZE, pageSize);
            callback.reset(1);
            mMediaBrowser.subscribe(MEDIA_ID_ROOT, options, callback);
            callback.await(TIME_OUT_MS);

            // Each onChildrenLoaded() must be called.
            assertEquals(1, callback.mChildrenLoadedWithOptionCount);
        }

        // Reset callbacks and unsubscribe.
        for (StubSubscriptionCallback callback : subscriptionCallbacks) {
            callback.reset(1);
        }
        mMediaBrowser.unsubscribe(MEDIA_ID_ROOT);

        // After unsubscribing, make StubMediaBrowserService notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                getApplicationContext());
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
    @LargeTest
    @SdkSuppress(minSdkVersion = 26)
    public void testUnsubscribeWithSubscriptionCallbackForMultipleSubscriptions() throws Exception {
        connectMediaBrowserService();
        final List<StubSubscriptionCallback> subscriptionCallbacks = new ArrayList<>();
        final int pageSize = 1;

        // Subscribe four pages, one item per page.
        for (int page = 0; page < 4; page++) {
            final StubSubscriptionCallback callback = new StubSubscriptionCallback();
            subscriptionCallbacks.add(callback);

            Bundle options = new Bundle();
            options.putInt(MediaBrowser.EXTRA_PAGE, page);
            options.putInt(MediaBrowser.EXTRA_PAGE_SIZE, pageSize);
            callback.reset(1);
            mMediaBrowser.subscribe(MEDIA_ID_ROOT, options, callback);
            callback.await(TIME_OUT_MS);

            // Each onChildrenLoaded() must be called.
            assertEquals(1, callback.mChildrenLoadedWithOptionCount);
        }

        // Unsubscribe existing subscriptions one-by-one.
        final int[] orderOfRemovingCallbacks = {2, 0, 3, 1};
        for (int i = 0; i < orderOfRemovingCallbacks.length; i++) {
            // Reset callbacks
            for (StubSubscriptionCallback callback : subscriptionCallbacks) {
                callback.reset(1);
            }

            // Remove one subscription
            mMediaBrowser.unsubscribe(MEDIA_ID_ROOT,
                    subscriptionCallbacks.get(orderOfRemovingCallbacks[i]));

            // Make StubMediaBrowserService notify that the children are changed.
            callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT,
                    getApplicationContext());

            // Remaining subscriptionCallbacks should be called.
            int remaining = orderOfRemovingCallbacks.length - i - 1;
            for (int j = i + 1; j < orderOfRemovingCallbacks.length; j++) {
                StubSubscriptionCallback callback = subscriptionCallbacks
                        .get(orderOfRemovingCallbacks[j]);
                assertTrue(callback.await(TIME_OUT_MS * remaining));
                assertEquals(1, callback.mChildrenLoadedWithOptionCount);
            }

            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                fail("Unexpected InterruptedException occurred.");
            }

            // Removed subscriptionCallbacks should NOT be called.
            for (int j = 0; j <= i; j++) {
                StubSubscriptionCallback callback = subscriptionCallbacks
                        .get(orderOfRemovingCallbacks[j]);
                assertEquals(0, callback.mChildrenLoadedWithOptionCount);
            }
        }
    }

    @Test
    @SmallTest
    public void testGetItem() throws Exception {
        connectMediaBrowserService();

        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(MEDIA_ID_CHILDREN[0], mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertNotNull(mItemCallback.mLastMediaItem);
            assertEquals(MEDIA_ID_CHILDREN[0], mItemCallback.mLastMediaItem.getMediaId());
        }
    }

    @Test
    @MediumTest
    public void testGetItemDelayed() throws Exception {
        connectMediaBrowserService();

        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(MEDIA_ID_CHILDREN_DELAYED, mItemCallback);
            mItemCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertNull(mItemCallback.mLastMediaItem);

            mItemCallback.reset();
            callMediaBrowserServiceMethod(SEND_DELAYED_ITEM_LOADED, new Bundle(),
                    getApplicationContext());
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertNotNull(mItemCallback.mLastMediaItem);
            assertEquals(MEDIA_ID_CHILDREN_DELAYED, mItemCallback.mLastMediaItem.getMediaId());
        }
    }

    @Test
    @SmallTest
    public void testGetItemWhenOnLoadItemIsNotImplemented() throws Exception {
        connectMediaBrowserService();
        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED, mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            // Limitation: Framework media browser gets onItemLoaded() call with null media item,
            // instead of onError().
            // assertEquals(MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED, mItemCallback.mLastErrorId);
        }
    }

    @Test
    @SmallTest
    public void testGetItemWhenMediaIdIsInvalid() throws Exception {
        mItemCallback.mLastMediaItem = new MediaItem(new MediaDescription.Builder()
                .setMediaId("dummy_id").build(), MediaItem.FLAG_BROWSABLE);

        connectMediaBrowserService();
        synchronized (mItemCallback.mWaitLock) {
            mMediaBrowser.getItem(MEDIA_ID_INVALID, mItemCallback);
            mItemCallback.mWaitLock.wait(TIME_OUT_MS);
            assertNull(mItemCallback.mLastMediaItem);
            assertNull(mItemCallback.mLastErrorId);
        }
    }

    @Test
    @MediumTest
    public void testDelayedSetSessionToken() throws Exception {
        // This test has no meaning in API 21. The framework MediaBrowserService just connects to
        // the media browser without waiting setMediaSession() to be called.
        if (Build.VERSION.SDK_INT == 21) {
            return;
        }
        final ConnectionCallbackForDelayedMediaSession callback =
                new ConnectionCallbackForDelayedMediaSession();

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowser(
                        getInstrumentation().getTargetContext(),
                        TEST_BROWSER_SERVICE_DELAYED_MEDIA_SESSION,
                        callback,
                        null);
            }
        });

        synchronized (callback.mWaitLock) {
            mMediaBrowser.connect();
            callback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertEquals(0, callback.mConnectedCount);

            callMediaBrowserServiceMethod(SET_SESSION_TOKEN, new Bundle(), getApplicationContext());
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(1, callback.mConnectedCount);
        }
    }

    private void connectMediaBrowserService() throws Exception {
        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
            if (!mMediaBrowser.isConnected()) {
                fail("Browser failed to connect!");
            }
        }
    }

    private class StubConnectionCallback extends MediaBrowser.ConnectionCallback {
        final Object mWaitLock = new Object();
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

    private class StubSubscriptionCallback extends MediaBrowser.SubscriptionCallback {
        private volatile CountDownLatch mLatch;
        private volatile int mChildrenLoadedCount;
        private volatile int mChildrenLoadedWithOptionCount;
        private volatile String mLastErrorId;
        private volatile String mLastParentId;
        private volatile Bundle mLastOptions;
        private volatile List<MediaItem> mLastChildMediaItems;

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
            mChildrenLoadedCount = 0;
            mChildrenLoadedWithOptionCount = 0;
            mLastErrorId = null;
            mLastParentId = null;
            mLastOptions = null;
            mLastChildMediaItems = null;
        }

        public boolean await(long timeoutMs) {
            try {
                return mLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupt while awaiting", e);
                return false;
            }
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            mChildrenLoadedCount++;
            mLastParentId = parentId;
            mLastChildMediaItems = children;
            mLatch.countDown();
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children,
                @NonNull Bundle options) {
            mChildrenLoadedWithOptionCount++;
            mLastParentId = parentId;
            mLastOptions = options;
            mLastChildMediaItems = children;
            mLatch.countDown();
        }

        @Override
        public void onError(@NonNull String id) {
            mLastErrorId = id;
            mLatch.countDown();
        }

        @Override
        public void onError(@NonNull String id, @NonNull Bundle options) {
            mLastErrorId = id;
            mLastOptions = options;
            mLatch.countDown();
        }
    }

    private class StubItemCallback extends MediaBrowser.ItemCallback {
        final Object mWaitLock = new Object();
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
        public void onError(@NonNull String id) {
            synchronized (mWaitLock) {
                mLastErrorId = id;
                mWaitLock.notify();
            }
        }
    }

    private class ConnectionCallbackForDelayedMediaSession extends MediaBrowser.ConnectionCallback {
        final Object mWaitLock = new Object();
        private int mConnectedCount = 0;

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
                mWaitLock.notify();
            }
        }

        @Override
        public void onConnectionSuspended() {
            synchronized (mWaitLock) {
                mWaitLock.notify();
            }
        }
    }
}
