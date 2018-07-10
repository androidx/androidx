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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.os.Build;
import android.os.Process;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether {@link MediaSessionService2} works with {@link MediaBrowserCompat}.
 */
@SmallTest
public class MediaSessionService2LegacyTest extends MediaSession2TestBase {
    MediaBrowserCompat mBrowserCompat;
    TestConnectionCallback mConnectionCallback;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        mConnectionCallback = new TestConnectionCallback();
        final ComponentName component = getServiceComponent();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Make browser's internal handler to be initialized with test thread.
                mBrowserCompat = new MediaBrowserCompat(
                        mContext, component, mConnectionCallback, null);
            }
        });
        TestServiceRegistry.getInstance().setHandler(sHandler);
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
        return new ComponentName(mContext, MockMediaSessionService2.class);
    }

    void connectAndWait() throws InterruptedException {
        mBrowserCompat.connect();
        assertTrue(mConnectionCallback.mConnectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConnect() throws InterruptedException {
        prepareLooper();
        TestServiceRegistry.getInstance().setSessionCallback(new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                if (controller != null && controller.getUid() == Process.myUid()) {
                    return super.onConnect(session, controller);
                }
                return null;
            }
        });
        connectAndWait();
        assertNotEquals(0, mConnectionCallback.mFailedLatch.getCount());
    }

    @Test
    public void testConnect_rejected() throws InterruptedException {
        prepareLooper();
        TestServiceRegistry.getInstance().setSessionCallback(new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                return null;
            }
        });
        mBrowserCompat.connect();
        assertTrue(mConnectionCallback.mFailedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotEquals(0, mConnectionCallback.mConnectedLatch.getCount());
    }

    /**
     * Note that this test fails on pre-P devices because session legacy stub doesn't allow any
     * command to media controller compat.
     *
     * @throws Exception
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    public void testGetSessionToken() throws Exception {
        prepareLooper();
        connectAndWait();
        MediaControllerCompat controller = new MediaControllerCompat(mContext,
                mBrowserCompat.getSessionToken());
        controller.getTransportControls().play();
        MockPlayer player = (MockPlayer) TestServiceRegistry.getInstance()
                .getServiceInstance().getSession().getPlayer();
        assertTrue(player.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(player.mPlayCalled);
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
