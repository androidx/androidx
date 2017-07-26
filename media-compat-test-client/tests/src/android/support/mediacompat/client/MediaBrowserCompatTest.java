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

package android.support.mediacompat.client;

import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_KEY;
import static android.support.mediacompat.testlib.MediaBrowserConstants.EXTRAS_VALUE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.MEDIA_ID_ROOT;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.testutils.PollingCheck;
import android.support.v4.media.MediaBrowserCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link android.support.v4.media.MediaBrowserCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaBrowserCompatTest {
    static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            "android.support.mediacompat.service.test",
            "android.support.mediacompat.service.StubMediaBrowserServiceCompat");

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;

    private MediaBrowserCompat mMediaBrowser;
    private StubConnectionCallback mConnectionCallback;
    private Bundle mRootHints;

    @Before
    public void setUp() {
        mConnectionCallback = new StubConnectionCallback();
        mRootHints = new Bundle();

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
    public void testConnectAndDisconnect() throws Exception {
        assertFalse(mMediaBrowser.isConnected());
        connectToMediaBrowserService();

        assertEquals(1, mConnectionCallback.mConnectedCount);
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

    private void connectToMediaBrowserService() throws Exception {
        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
            if (!mMediaBrowser.isConnected()) {
                fail("Browser failed to connect!");
            }
        }
    }

    private class StubConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        Object mWaitLock = new Object();
        volatile int mConnectedCount;

        @Override
        public void onConnected() {
            synchronized (mWaitLock) {
                mConnectedCount++;
                mWaitLock.notify();
            }
        }
    }
}
