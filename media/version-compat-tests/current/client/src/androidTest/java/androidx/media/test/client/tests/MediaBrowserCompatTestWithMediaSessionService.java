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

import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_SESSION_SERVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.media2.MediaSessionService;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether {@link MediaBrowserCompat} works well with {@link MediaSessionService}.
 */
@SmallTest
public class MediaBrowserCompatTestWithMediaSessionService extends MediaSessionTestBase {
    MediaBrowserCompat mBrowserCompat;
    TestConnectionCallback mConnectionCallback;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mConnectionCallback = new TestConnectionCallback();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Make browser's internal handler to be initialized with test thread.
                mBrowserCompat = new MediaBrowserCompat(
                        mContext, getServiceComponent(), mConnectionCallback, null);
            }
        });
    }

    @After
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mBrowserCompat != null) {
            mBrowserCompat.disconnect();
            mBrowserCompat = null;
        }
    }

    ComponentName getServiceComponent() {
        return MOCK_MEDIA_SESSION_SERVICE;
    }

    void connectAndWait() throws InterruptedException {
        mBrowserCompat.connect();
        assertTrue(mConnectionCallback.mConnectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConnect() throws InterruptedException {
        prepareLooper();
        connectAndWait();
        assertNotEquals(0, mConnectionCallback.mFailedLatch.getCount());
    }

    @Ignore
    @Test
    public void testConnect_rejected() throws InterruptedException {
        prepareLooper();
        // TODO: Connect the browser to the session service whose onConnect() returns null.
        assertTrue(mConnectionCallback.mFailedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotEquals(0, mConnectionCallback.mConnectedLatch.getCount());
    }

    @Test
    public void testGetSessionToken() throws Exception {
        prepareLooper();
        connectAndWait();
        MediaControllerCompat controller = new MediaControllerCompat(mContext,
                mBrowserCompat.getSessionToken());
        assertEquals(mBrowserCompat.getServiceComponent().getPackageName(),
                controller.getPackageName());
    }

    class TestConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        public final CountDownLatch mConnectedLatch = new CountDownLatch(1);
        public final CountDownLatch mSuspendedLatch = new CountDownLatch(1);
        public final CountDownLatch mFailedLatch = new CountDownLatch(1);

        TestConnectionCallback() {
            super();
        }

        @Override
        public void onConnected() {
            super.onConnected();
            mConnectedLatch.countDown();
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            mSuspendedLatch.countDown();
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            mFailedLatch.countDown();
        }
    }
}
