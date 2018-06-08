/*
 * Copyright 2017 The Android Open Source Project
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

import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_FOR_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .CUSTOM_ACTION_SEND_PROGRESS_UPDATE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_RESULT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_KEY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_VALUE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_CHILDREN_DELAYED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_INVALID;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ROOT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY_FOR_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEARCH_QUERY_FOR_NO_RESULT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEND_DELAYED_ITEM_LOADED;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .SEND_DELAYED_NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SET_SESSION_TOKEN;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_KEY_1;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_KEY_2;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_KEY_3;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_KEY_4;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_VALUE_1;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_VALUE_2;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_VALUE_3;
import static android.support.mediacompat.testlib.MediaBrowserConstants.TEST_VALUE_4;
import static android.support.mediacompat.testlib.VersionConstants.KEY_SERVICE_VERSION;
import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;
import static android.support.mediacompat.testlib.util.IntentUtil.callMediaBrowserServiceMethod;
import static android.support.test.InstrumentationRegistry.getArguments;
import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link android.support.v4.media.MediaBrowserCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaBrowserCompatTest {

    private static final String TAG = "MediaBrowserCompatTest";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final long WAIT_TIME_FOR_NO_RESPONSE_MS = 300L;

    /**
     * To check {@link MediaBrowserCompat#unsubscribe} works properly,
     * we notify to the browser after the unsubscription that the media items have changed.
     * Then {@link MediaBrowserCompat.SubscriptionCallback#onChildrenLoaded} should not be called.
     *
     * The measured time from calling {@link MediaBrowserServiceCompat#notifyChildrenChanged}
     * to {@link MediaBrowserCompat.SubscriptionCallback#onChildrenLoaded} being called is about
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
    private MediaBrowserCompat mMediaBrowser;
    private StubConnectionCallback mConnectionCallback;
    private StubSubscriptionCallback mSubscriptionCallback;
    private StubItemCallback mItemCallback;
    private StubSearchCallback mSearchCallback;
    private CustomActionCallback mCustomActionCallback;
    private Bundle mRootHints;

    @Before
    public void setUp() {
        // The version of the service app is provided through the instrumentation arguments.
        mServiceVersion = getArguments().getString(KEY_SERVICE_VERSION, "");
        Log.d(TAG, "Service app version: " + mServiceVersion);

        mConnectionCallback = new StubConnectionCallback();
        mSubscriptionCallback = new StubSubscriptionCallback();
        mItemCallback = new StubItemCallback();
        mSearchCallback = new StubSearchCallback();
        mCustomActionCallback = new CustomActionCallback();

        mRootHints = new Bundle();
        mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT, true);
        mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_OFFLINE, true);
        mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_SUGGESTED, true);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowserCompat(getInstrumentation().getTargetContext(),
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

        MediaBrowserServiceCompat.BrowserRoot browserRoot =
                new MediaBrowserServiceCompat.BrowserRoot(id, extras);
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
                mMediaBrowser = new MediaBrowserCompat(getInstrumentation().getTargetContext(),
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

        // Test MediaBrowserServiceCompat.notifyChildrenChanged()
        mSubscriptionCallback.reset(1);
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(1, mSubscriptionCallback.mChildrenLoadedCount);

        // Test unsubscribe.
        mSubscriptionCallback.reset(1);
        mMediaBrowser.unsubscribe(MEDIA_ID_ROOT);

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
        mSubscriptionCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS);

        // onChildrenLoaded should not be called.
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);
    }

    @Test
    @MediumTest
    public void testSubscribeWithOptions() throws Exception {
        connectMediaBrowserService();
        final int pageSize = 3;
        final int lastPage = (MEDIA_ID_CHILDREN.length - 1) / pageSize;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);

        for (int page = 0; page <= lastPage; ++page) {
            mSubscriptionCallback.reset(1);
            options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
            mMediaBrowser.subscribe(MEDIA_ID_ROOT, options, mSubscriptionCallback);
            mSubscriptionCallback.await(TIME_OUT_MS);
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

            // Test MediaBrowserServiceCompat.notifyChildrenChanged()
            mSubscriptionCallback.reset(page + 1);
            callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
            mSubscriptionCallback.await(TIME_OUT_MS);
            assertEquals(page + 1, mSubscriptionCallback.mChildrenLoadedWithOptionCount);
        }

        // Test unsubscribe with callback argument.
        mSubscriptionCallback.reset(1);
        mMediaBrowser.unsubscribe(MEDIA_ID_ROOT, mSubscriptionCallback);

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
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
        mSubscriptionCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS);
        assertEquals(0, mSubscriptionCallback.mChildrenLoadedCount);

        callMediaBrowserServiceMethod(
                SEND_DELAYED_NOTIFY_CHILDREN_CHANGED, MEDIA_ID_CHILDREN_DELAYED, getContext());
        mSubscriptionCallback.await(TIME_OUT_MS);
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
    public void testSubscribeInvalidItemWithOptions() throws Exception {
        connectMediaBrowserService();

        final int pageSize = 5;
        final int page = 2;
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);

        mSubscriptionCallback.reset(1);
        mMediaBrowser.subscribe(MEDIA_ID_INVALID, options, mSubscriptionCallback);
        mSubscriptionCallback.await(TIME_OUT_MS);
        assertEquals(MEDIA_ID_INVALID, mSubscriptionCallback.mLastErrorId);
        assertNotNull(mSubscriptionCallback.mLastOptions);
        assertEquals(page,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE));
        assertEquals(pageSize,
                mSubscriptionCallback.mLastOptions.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
    }

    @Test
    @MediumTest
    public void testUnsubscribeForMultipleSubscriptions() throws Exception {
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

        // After unsubscribing, make StubMediaBrowserServiceCompat notify that the children are
        // changed.
        callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
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
    @MediumTest
    @FlakyTest(bugId = 74093976)
    public void testUnsubscribeWithSubscriptionCallbackForMultipleSubscriptions() throws Exception {
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

            // Make StubMediaBrowserServiceCompat notify that the children are changed.
            callMediaBrowserServiceMethod(NOTIFY_CHILDREN_CHANGED, MEDIA_ID_ROOT, getContext());
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
            callMediaBrowserServiceMethod(SEND_DELAYED_ITEM_LOADED, new Bundle(), getContext());
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
            assertEquals(MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED, mItemCallback.mLastErrorId);
        }
    }

    @Test
    @SmallTest
    public void testGetItemWhenMediaIdIsInvalid() throws Exception {
        mItemCallback.mLastMediaItem = new MediaItem(new MediaDescriptionCompat.Builder()
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
    @SmallTest
    public void testSearch() throws Exception {
        connectMediaBrowserService();

        final String key = "test-key";
        final String val = "test-val";

        synchronized (mSearchCallback.mWaitLock) {
            mSearchCallback.reset();
            mMediaBrowser.search(SEARCH_QUERY_FOR_NO_RESULT, null, mSearchCallback);
            mSearchCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertTrue(mSearchCallback.mSearchResults != null
                    && mSearchCallback.mSearchResults.size() == 0);
            assertEquals(null, mSearchCallback.mSearchExtras);

            mSearchCallback.reset();
            mMediaBrowser.search(SEARCH_QUERY_FOR_ERROR, null, mSearchCallback);
            mSearchCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertNull(mSearchCallback.mSearchResults);
            assertEquals(null, mSearchCallback.mSearchExtras);

            mSearchCallback.reset();
            Bundle extras = new Bundle();
            extras.putString(key, val);
            mMediaBrowser.search(SEARCH_QUERY, extras, mSearchCallback);
            mSearchCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertNotNull(mSearchCallback.mSearchResults);
            for (MediaItem item : mSearchCallback.mSearchResults) {
                assertNotNull(item.getMediaId());
                assertTrue(item.getMediaId().contains(SEARCH_QUERY));
            }
            assertNotNull(mSearchCallback.mSearchExtras);
            assertEquals(val, mSearchCallback.mSearchExtras.getString(key));
        }
    }

    @Test
    @SmallTest
    public void testSendCustomAction() throws Exception {
        connectMediaBrowserService();

        synchronized (mCustomActionCallback.mWaitLock) {
            Bundle customActionExtras = new Bundle();
            customActionExtras.putString(TEST_KEY_1, TEST_VALUE_1);
            mMediaBrowser.sendCustomAction(
                    CUSTOM_ACTION, customActionExtras, mCustomActionCallback);
            mCustomActionCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);

            mCustomActionCallback.reset();
            Bundle data1 = new Bundle();
            data1.putString(TEST_KEY_2, TEST_VALUE_2);
            callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_PROGRESS_UPDATE, data1, getContext());
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);

            assertTrue(mCustomActionCallback.mOnProgressUpdateCalled);
            assertEquals(CUSTOM_ACTION, mCustomActionCallback.mAction);
            assertNotNull(mCustomActionCallback.mExtras);
            assertEquals(TEST_VALUE_1, mCustomActionCallback.mExtras.getString(TEST_KEY_1));
            assertNotNull(mCustomActionCallback.mData);
            assertEquals(TEST_VALUE_2, mCustomActionCallback.mData.getString(TEST_KEY_2));

            mCustomActionCallback.reset();
            Bundle data2 = new Bundle();
            data2.putString(TEST_KEY_3, TEST_VALUE_3);
            callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_PROGRESS_UPDATE, data2, getContext());
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);

            assertTrue(mCustomActionCallback.mOnProgressUpdateCalled);
            assertEquals(CUSTOM_ACTION, mCustomActionCallback.mAction);
            assertNotNull(mCustomActionCallback.mExtras);
            assertEquals(TEST_VALUE_1, mCustomActionCallback.mExtras.getString(TEST_KEY_1));
            assertNotNull(mCustomActionCallback.mData);
            assertEquals(TEST_VALUE_3, mCustomActionCallback.mData.getString(TEST_KEY_3));

            Bundle resultData = new Bundle();
            resultData.putString(TEST_KEY_4, TEST_VALUE_4);
            mCustomActionCallback.reset();
            callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_RESULT, resultData, getContext());
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);

            assertTrue(mCustomActionCallback.mOnResultCalled);
            assertEquals(CUSTOM_ACTION, mCustomActionCallback.mAction);
            assertNotNull(mCustomActionCallback.mExtras);
            assertEquals(TEST_VALUE_1, mCustomActionCallback.mExtras.getString(TEST_KEY_1));
            assertNotNull(mCustomActionCallback.mData);
            assertEquals(TEST_VALUE_4, mCustomActionCallback.mData.getString(TEST_KEY_4));
        }
    }


    @Test
    @MediumTest
    public void testSendCustomActionWithDetachedError() throws Exception {
        connectMediaBrowserService();

        synchronized (mCustomActionCallback.mWaitLock) {
            Bundle customActionExtras = new Bundle();
            customActionExtras.putString(TEST_KEY_1, TEST_VALUE_1);
            mMediaBrowser.sendCustomAction(
                    CUSTOM_ACTION, customActionExtras, mCustomActionCallback);
            mCustomActionCallback.mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);

            mCustomActionCallback.reset();
            Bundle progressUpdateData = new Bundle();
            progressUpdateData.putString(TEST_KEY_2, TEST_VALUE_2);
            callMediaBrowserServiceMethod(
                    CUSTOM_ACTION_SEND_PROGRESS_UPDATE, progressUpdateData, getContext());
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCustomActionCallback.mOnProgressUpdateCalled);
            assertEquals(CUSTOM_ACTION, mCustomActionCallback.mAction);
            assertNotNull(mCustomActionCallback.mExtras);
            assertEquals(TEST_VALUE_1, mCustomActionCallback.mExtras.getString(TEST_KEY_1));
            assertNotNull(mCustomActionCallback.mData);
            assertEquals(TEST_VALUE_2, mCustomActionCallback.mData.getString(TEST_KEY_2));

            mCustomActionCallback.reset();
            Bundle errorData = new Bundle();
            errorData.putString(TEST_KEY_3, TEST_VALUE_3);
            callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_ERROR, errorData, getContext());
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCustomActionCallback.mOnErrorCalled);
            assertEquals(CUSTOM_ACTION, mCustomActionCallback.mAction);
            assertNotNull(mCustomActionCallback.mExtras);
            assertEquals(TEST_VALUE_1, mCustomActionCallback.mExtras.getString(TEST_KEY_1));
            assertNotNull(mCustomActionCallback.mData);
            assertEquals(TEST_VALUE_3, mCustomActionCallback.mData.getString(TEST_KEY_3));
        }
    }

    @Test
    @MediumTest
    public void testSendCustomActionWithNullCallback() throws Exception {
        connectMediaBrowserService();

        Bundle customActionExtras = new Bundle();
        customActionExtras.putString(TEST_KEY_1, TEST_VALUE_1);
        mMediaBrowser.sendCustomAction(CUSTOM_ACTION, customActionExtras, null);
        // Wait some time so that the service can get a result receiver for the custom action.
        Thread.sleep(WAIT_TIME_FOR_NO_RESPONSE_MS);

        // These calls should not make any exceptions.
        callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_PROGRESS_UPDATE, new Bundle(),
                getContext());
        callMediaBrowserServiceMethod(CUSTOM_ACTION_SEND_RESULT, new Bundle(), getContext());
        Thread.sleep(WAIT_TIME_FOR_NO_RESPONSE_MS);
    }

    @Test
    @SmallTest
    public void testSendCustomActionWithError() throws Exception {
        connectMediaBrowserService();

        synchronized (mCustomActionCallback.mWaitLock) {
            mMediaBrowser.sendCustomAction(CUSTOM_ACTION_FOR_ERROR, null, mCustomActionCallback);
            mCustomActionCallback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCustomActionCallback.mOnErrorCalled);
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
                mMediaBrowser = new MediaBrowserCompat(
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

            callMediaBrowserServiceMethod(SET_SESSION_TOKEN, new Bundle(), getContext());
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertEquals(1, callback.mConnectedCount);

            if (Build.VERSION.SDK_INT >= 21) {
                assertNotNull(mMediaBrowser.getSessionToken().getExtraBinder());
            }
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

    private class StubConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
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

    private class StubSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        private CountDownLatch mLatch;
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

    private class StubItemCallback extends MediaBrowserCompat.ItemCallback {
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

    private class StubSearchCallback extends MediaBrowserCompat.SearchCallback {
        final Object mWaitLock = new Object();
        boolean mOnSearchResult;
        Bundle mSearchExtras;
        List<MediaItem> mSearchResults;

        @Override
        public void onSearchResult(@NonNull String query, Bundle extras,
                @NonNull List<MediaItem> items) {
            synchronized (mWaitLock) {
                mOnSearchResult = true;
                mSearchResults = items;
                mSearchExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(@NonNull String query, Bundle extras) {
            synchronized (mWaitLock) {
                mOnSearchResult = true;
                mSearchResults = null;
                mSearchExtras = extras;
                mWaitLock.notify();
            }
        }

        public void reset() {
            mOnSearchResult = false;
            mSearchExtras = null;
            mSearchResults = null;
        }
    }

    private class CustomActionCallback extends MediaBrowserCompat.CustomActionCallback {
        final Object mWaitLock = new Object();
        String mAction;
        Bundle mExtras;
        Bundle mData;
        boolean mOnProgressUpdateCalled;
        boolean mOnResultCalled;
        boolean mOnErrorCalled;

        @Override
        public void onProgressUpdate(String action, Bundle extras, Bundle data) {
            synchronized (mWaitLock) {
                mOnProgressUpdateCalled = true;
                mAction = action;
                mExtras = extras;
                mData = data;
                mWaitLock.notify();
            }
        }

        @Override
        public void onResult(String action, Bundle extras, Bundle resultData) {
            synchronized (mWaitLock) {
                mOnResultCalled = true;
                mAction = action;
                mExtras = extras;
                mData = resultData;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(String action, Bundle extras, Bundle data) {
            synchronized (mWaitLock) {
                mOnErrorCalled = true;
                mAction = action;
                mExtras = extras;
                mData = data;
                mWaitLock.notify();
            }
        }

        public void reset() {
            mOnResultCalled = false;
            mOnProgressUpdateCalled = false;
            mOnErrorCalled = false;
            mAction = null;
            mExtras = null;
            mData = null;
        }
    }

    private class ConnectionCallbackForDelayedMediaSession extends
            MediaBrowserCompat.ConnectionCallback {
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
