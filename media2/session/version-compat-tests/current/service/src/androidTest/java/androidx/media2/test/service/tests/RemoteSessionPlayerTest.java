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

package androidx.media2.test.service.tests;

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.os.Build;

import androidx.media2.session.MediaSession;
import androidx.media2.session.RemoteSessionPlayer;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.MockRemotePlayer;
import androidx.media2.test.service.RemoteMediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link RemoteSessionPlayer} are triggered by the
 * controller.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteSessionPlayerTest extends MediaSessionTestBase {

    MediaSession mSession;
    RemoteMediaController mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create this test specific MediaSession to use our own Handler.
        mSession = new MediaSession.Builder(mContext, new MockPlayer(1))
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(MediaSession session,
                            MediaSession.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                })
                .setId("RemoteSessionPlayerTest")
                .build();
        // Create a default MediaController in client app.
        mController = createRemoteController(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
    }

    @Test
    public void testSetVolumeToByController() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = RemoteSessionPlayer.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer = new MockRemotePlayer(
                volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);

        final int targetVolume = 50;
        mController.setVolumeTo(targetVolume, 0 /* flags */);

        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mSetVolumeToCalled);
        assertEquals(targetVolume, remotePlayer.mCurrentVolume, 0.001f);
    }

    @Test
    public void testAdjustVolumeByController() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = RemoteSessionPlayer.VOLUME_CONTROL_ABSOLUTE;

        MockRemotePlayer remotePlayer = new MockRemotePlayer(
                volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);

        final int direction = AudioManager.ADJUST_RAISE;
        mController.adjustVolume(direction, 0 /* flags */);

        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mAdjustVolumeCalled);
        assertEquals(direction, remotePlayer.mDirection);
    }
}
