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

package androidx.media.test.service.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.os.Build;

import androidx.media.test.service.MockPlayerConnector;
import androidx.media.test.service.MockPlaylistAgent;
import androidx.media.test.service.MockRemotePlayerConnector;
import androidx.media.test.service.RemoteMediaController2;
import androidx.media2.BaseRemoteMediaPlayerConnector;
import androidx.media2.MediaSession2;
import androidx.media2.SessionCommandGroup2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link BaseRemoteMediaPlayerConnector} are triggered by the
 * controller.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteMediaPlayerConnectorTest extends MediaSession2TestBase {

    MediaSession2 mSession;
    RemoteMediaController2 mController2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create this test specific MediaSession2 to use our own Handler.
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(new MockPlayerConnector(1))
                .setPlaylistAgent(new MockPlaylistAgent())
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build();
        // Create a default MediaController2 in client app.
        mController2 = createRemoteController2(mSession.getToken());
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
        final float maxVolume = 100;
        final float currentVolume = 23;
        final int volumeControlType = BaseRemoteMediaPlayerConnector.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayerConnector remotePlayer = new MockRemotePlayerConnector(
                volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayerConnector(remotePlayer, null);

        final int targetVolume = 50;
        mController2.setVolumeTo(targetVolume, 0 /* flags */);

        assertTrue(remotePlayer.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mSetVolumeToCalled);
        assertEquals(targetVolume, remotePlayer.mCurrentVolume, 0.001f);
    }

    @Test
    public void testAdjustVolumeByController() throws Exception {
        prepareLooper();
        final float maxVolume = 100.0f;
        final float currentVolume = 23.0f;
        final int volumeControlType = BaseRemoteMediaPlayerConnector.VOLUME_CONTROL_ABSOLUTE;

        MockRemotePlayerConnector remotePlayer = new MockRemotePlayerConnector(
                volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayerConnector(remotePlayer, null);

        final int direction = AudioManager.ADJUST_RAISE;
        mController2.adjustVolume(direction, 0 /* flags */);

        assertTrue(remotePlayer.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mAdjustVolumeCalled);
        assertEquals(direction, remotePlayer.mDirection);
    }
}
