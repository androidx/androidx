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

package androidx.media.test.service;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.KEY_SEEK_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_SPEED;
import static androidx.media.test.lib.MediaController2Constants.PAUSE;
import static androidx.media.test.lib.MediaController2Constants.PLAY;
import static androidx.media.test.lib.MediaController2Constants.PREPARE;
import static androidx.media.test.lib.MediaController2Constants.RESET;
import static androidx.media.test.lib.MediaController2Constants.SEEK_TO;
import static androidx.media.test.lib.MediaController2Constants.SET_PLAYBACK_SPEED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.os.Bundle;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.BaseMediaPlayer;
import androidx.media.MediaSession2;
import androidx.media.SessionCommandGroup2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link BaseMediaPlayer} are triggered by the
 * session/controller.
 */
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class BaseMediaPlayerTest extends MediaSession2TestBase {

    private MediaSession2 mSession;
    private MockPlayer mPlayer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
        TestServiceRegistry.getInstance().setHandler(sHandler);

        // Create a default MediaController2 in client app.
        mTestHelper.createMediaController2(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Test
    public void testPlayBySession() throws Exception {
        prepareLooper();
        mSession.play();
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPlayByController() {
        mTestHelper.callMediaController2Method(PLAY, null);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPauseBySession() throws Exception {
        prepareLooper();
        mSession.pause();
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPauseByController() {
        mTestHelper.callMediaController2Method(PAUSE, null);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testResetBySession() throws Exception {
        prepareLooper();
        mSession.reset();
        assertTrue(mPlayer.mResetCalled);
    }

    @Test
    public void testResetByController() {
        mTestHelper.callMediaController2Method(RESET, null);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mResetCalled);
    }

    @Test
    public void testPrepareBySession() throws Exception {
        prepareLooper();
        mSession.prepare();
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void testPrepareByController() {
        mTestHelper.callMediaController2Method(PREPARE, null);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void testSeekToBySession() throws Exception {
        prepareLooper();
        final long pos = 1004L;
        mSession.seekTo(pos);
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(pos, mPlayer.mSeekPosition);
    }

    @Test
    public void testSeekToByController() {
        final long seekPosition = 12125L;
        Bundle args = new Bundle();
        args.putLong(KEY_SEEK_POSITION, seekPosition);
        mTestHelper.callMediaController2Method(SEEK_TO, args);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void testSetPlaybackSpeedBySession() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mSession.setPlaybackSpeed(speed);
        assertTrue(mPlayer.mSetPlaybackSpeedCalled);
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void testSetPlaybackSpeedByController() throws Exception {
        final float speed = 1.5f;
        Bundle args = new Bundle();
        args.putFloat(KEY_SPEED, speed);
        mTestHelper.callMediaController2Method(SET_PLAYBACK_SPEED, args);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }
}
