/*
* Copyright (C) 2017 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.mediarouter.media.MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaRouter}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouterTest {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final String SESSION_TAG = "test-session";

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";

    private final Object mWaitLock = new Object();

    private Context mContext;
    private MediaRouter mRouter;
    private MediaSessionCompat mSession;
    private MediaSessionCallback mSessionCallback = new MediaSessionCallback();
    private MediaRouteProviderImpl mProvider;
    private CountDownLatch mActiveScanCountDownLatch;
    private CountDownLatch mPassiveScanCountDownLatch;

    @Before
    public void setUp() throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mContext = getApplicationContext();
                mRouter = MediaRouter.getInstance(mContext);
                mSession = new MediaSessionCompat(mContext, SESSION_TAG);
                mProvider = new MediaRouteProviderImpl(mContext);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mSession.release();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                MediaRouter.reset();
            }
        });
    }

    /**
     * This test checks whether the session callback work properly after setMediaSessionCompat() is
     * called.
     */
    @Test
    @SmallTest
    public void setMediaSessionCompat_receivesCallbacks() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession.setCallback(mSessionCallback);
                mRouter.setMediaSessionCompat(mSession);
            }
        });

        MediaControllerCompat controller = mSession.getController();
        MediaControllerCompat.TransportControls controls = controller.getTransportControls();
        synchronized (mWaitLock) {
            mSessionCallback.reset();
            controls.play();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSessionCallback.mOnPlayCalled);

            mSessionCallback.reset();
            controls.pause();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSessionCallback.mOnPauseCalled);
        }
    }

    @Test
    @SmallTest
    public void mediaRouterParamsBuilder() {
        final int dialogType = MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP;
        final boolean isOutputSwitcherEnabled = true;
        final boolean transferToLocalEnabled = true;
        final Bundle extras = new Bundle();
        extras.putString(TEST_KEY, TEST_VALUE);

        MediaRouterParams params = new MediaRouterParams.Builder()
                .setDialogType(dialogType)
                .setOutputSwitcherEnabled(isOutputSwitcherEnabled)
                .setTransferToLocalEnabled(transferToLocalEnabled)
                .setExtras(extras)
                .build();

        assertEquals(dialogType, params.getDialogType());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertEquals(isOutputSwitcherEnabled, params.isOutputSwitcherEnabled());
            assertEquals(transferToLocalEnabled, params.isTransferToLocalEnabled());
        } else {
            // Earlier than Android R, output switcher cannot be enabled.
            // Same for transfer to local.
            assertFalse(params.isOutputSwitcherEnabled());
            assertFalse(params.isTransferToLocalEnabled());
        }

        extras.remove(TEST_KEY);
        assertEquals(TEST_VALUE, params.getExtras().getString(TEST_KEY));

        // Tests copy constructor of builder
        MediaRouterParams copiedParams = new MediaRouterParams.Builder(params).build();
        assertEquals(params.getDialogType(), copiedParams.getDialogType());
        assertEquals(params.isOutputSwitcherEnabled(), copiedParams.isOutputSwitcherEnabled());
        assertEquals(params.isTransferToLocalEnabled(), copiedParams.isTransferToLocalEnabled());
        assertBundleEquals(params.getExtras(), copiedParams.getExtras());
    }

    @Test
    @SmallTest
    @UiThreadTest
    public void getRouterParams_afterSetRouterParams_returnsSetParams() {
        final int dialogType = MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP;
        final boolean isOutputSwitcherEnabled = true;
        final boolean transferToLocalEnabled = true;
        final Bundle paramExtras = new Bundle();
        paramExtras.putString(TEST_KEY, TEST_VALUE);

        MediaRouterParams expectedParams = new MediaRouterParams.Builder()
                .setDialogType(dialogType)
                .setOutputSwitcherEnabled(isOutputSwitcherEnabled)
                .setTransferToLocalEnabled(transferToLocalEnabled)
                .setExtras(paramExtras)
                .build();

        paramExtras.remove(TEST_KEY);
        mRouter.setRouterParams(expectedParams);

        MediaRouterParams actualParams = mRouter.getRouterParams();
        assertEquals(expectedParams, actualParams);
    }

    @Test
    @LargeTest
    public void testRegisterActiveScanCallback_suppressActiveScanAfter30Seconds() throws Exception {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO).build();
        MediaRouterCallbackImpl callback = new MediaRouterCallbackImpl();

        // Add the provider and callback.
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRouter.addProvider(mProvider);
                mRouter.addCallback(selector, callback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        });

        // Active scan should be true.
        assertTrue(mActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // After active scan duration, active scan should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(mPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Add the same callback again.
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRouter.addCallback(selector, callback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        });

        // Active scan should be true.
        assertTrue(mActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // After active scan duration, active scan should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(mPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testRegisterMultipleActiveScanCallbacks_suppressActiveScanAfter30Seconds()
            throws Exception {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO).build();
        MediaRouterCallbackImpl callback1 = new MediaRouterCallbackImpl();
        MediaRouterCallbackImpl callback2 = new MediaRouterCallbackImpl();

        // Add the provider and the first callback.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRouter.addProvider(mProvider);
                mRouter.addCallback(selector, callback1,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        });

        // Wait for 5 seconds, add the second callback.
        Thread.sleep(5000);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRouter.addCallback(selector, callback2,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        });

        resetActiveAndPassiveScanCountDownLatches();
        // Wait for active scan duration to nearly end, active scan flag should be true.
        assertFalse(mPassiveScanCountDownLatch.await(MAX_ACTIVE_SCAN_DURATION_MS - 1000,
                TimeUnit.MILLISECONDS));

        // Wait until active scan duration ends, active scan flag should be false.
        assertTrue(mPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @UiThreadTest
    public void testReset() {
        assertNotNull(mRouter);
        assertNotNull(MediaRouter.getGlobalRouter());

        MediaRouter.reset();
        assertNull(MediaRouter.getGlobalRouter());

        MediaRouter newInstance = MediaRouter.getInstance(mContext);
        assertNotNull(MediaRouter.getGlobalRouter());
        assertFalse(newInstance.getRoutes().isEmpty());
    }

    /**
     * Asserts that two Bundles are equal.
     */
    public static void assertBundleEquals(Bundle expected, Bundle observed) {
        if (expected == null || observed == null) {
            assertSame(expected, observed);
        }
        assertEquals(expected.size(), observed.size());
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), observed.get(key));
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private boolean mOnPlayCalled;
        private boolean mOnPauseCalled;

        public void reset() {
            mOnPlayCalled = false;
            mOnPauseCalled = false;
        }

        @Override
        public void onPlay() {
            synchronized (mWaitLock) {
                mOnPlayCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPause() {
            synchronized (mWaitLock) {
                mOnPauseCalled = true;
                mWaitLock.notify();
            }
        }
    }

    private class MediaRouteProviderImpl extends MediaRouteProvider {
        private boolean mIsActiveScan;

        MediaRouteProviderImpl(Context context) {
            super(context);
        }

        @Override
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest discoveryRequest) {
            boolean isActiveScan = discoveryRequest != null && discoveryRequest.isActiveScan();
            if (mIsActiveScan != isActiveScan) {
                mIsActiveScan = isActiveScan;
                if (mIsActiveScan) {
                    mActiveScanCountDownLatch.countDown();
                } else {
                    mPassiveScanCountDownLatch.countDown();
                }
            }
        }

    }

    private void resetActiveAndPassiveScanCountDownLatches() {
        mActiveScanCountDownLatch = new CountDownLatch(1);
        mPassiveScanCountDownLatch = new CountDownLatch(1);
    }

    private static class MediaRouterCallbackImpl extends MediaRouter.Callback {}
}
