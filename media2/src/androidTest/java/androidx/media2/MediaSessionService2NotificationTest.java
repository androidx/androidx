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

import android.os.Build;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Manual test of {@link MediaSessionService2} for showing/removing notification when the
 * playback is started/ended.
 * <p>
 * This test is a manual test, which means the one who runs this test should keep looking at the
 * device and check whether the notification is shown/removed.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
public class MediaSessionService2NotificationTest extends MediaSession2TestBase {
    private static final long NOTIFICATION_SHOW_TIME_MS = 5000;

    MediaSession2 mSession;
    MockPlayer mPlayer;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        TestServiceRegistry.getInstance().setHandler(sHandler);
    }

    @After
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testNotification() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    mSession = session;
                    mPlayer = (MockPlayer) session.getPlayerConnector();
                    assertEquals(mContext.getPackageName(), controller.getPackageName());
                    assertFalse(controller.isTrusted());
                    latch.countDown();
                }
                return super.onConnect(session, controller);
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);

        // Create a controller to start the service.
        MediaController2 controller =
                createController(TestUtils.getServiceToken(mContext, MockMediaSessionService2.ID));

        // Notification should be shown for NOTIFICATION_SHOW_TIME_MS (ms).
        // The notification will not be removed by swiping horizontally, since the service is
        // running as foreground.
        showToast("Notification will be shown");
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PLAYING);
        Thread.sleep(NOTIFICATION_SHOW_TIME_MS);

        // Notification will still be shown. However, one can swipe the notification horizontally
        // to remove the notification, since the service is no longer a foreground service.
        showToast("Notification can be removable shortly");
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_ERROR);
        Thread.sleep(NOTIFICATION_SHOW_TIME_MS);

        // Notification will be removed since the test framework stops the test process.
        showToast("Notification will be removed");
    }

    private void showToast(final String msg) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
