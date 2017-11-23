/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v4.media.session;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.VolumeProviderCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link MediaControllerCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaControllerCompatTest {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final String SESSION_TAG = "test-session";
    private static final long TEST_POSITION = 1000000L;
    private static final float TEST_PLAYBACK_SPEED = 3.0f;

    private final Object mWaitLock = new Object();
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;

    @Before
    public void setUp() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession = new MediaSessionCompat(getContext(), SESSION_TAG);
                mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);
                mController = mSession.getController();
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mSession.release();
    }

    @Test
    @SmallTest
    public void testGetPackageName() {
        assertEquals(getContext().getPackageName(), mController.getPackageName());
    }

    @Test
    @SmallTest
    public void testGetSessionToken() throws Exception {
        assertEquals(mSession.getSessionToken(), mController.getSessionToken());
    }

    @Test
    @SmallTest
    public void testIsSessionReady() throws Exception {
        // mController already has the extra binder since it was created with the session token
        // which holds the extra binder.
        assertTrue(mController.isSessionReady());
    }

    // TODO: Uncomment after fixing this test. This test causes an Exception on System UI.
    // @Test
    // @SmallTest
    public void testVolumeControl() throws Exception {
        VolumeProviderCompat vp =
                new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 11, 5) {
            @Override
            public void onSetVolumeTo(int volume) {
                synchronized (mWaitLock) {
                    setCurrentVolume(volume);
                    mWaitLock.notify();
                }
            }

            @Override
            public void onAdjustVolume(int direction) {
                synchronized (mWaitLock) {
                    switch (direction) {
                        case AudioManager.ADJUST_LOWER:
                            setCurrentVolume(getCurrentVolume() - 1);
                            break;
                        case AudioManager.ADJUST_RAISE:
                            setCurrentVolume(getCurrentVolume() + 1);
                            break;
                    }
                    mWaitLock.notify();
                }
            }
        };
        mSession.setPlaybackToRemote(vp);

        synchronized (mWaitLock) {
            // test setVolumeTo
            mController.setVolumeTo(7, 0);
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());

            // test adjustVolume
            mController.adjustVolume(AudioManager.ADJUST_LOWER, 0);
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(6, vp.getCurrentVolume());

            mController.adjustVolume(AudioManager.ADJUST_RAISE, 0);
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());
        }
    }

    @Test
    @SmallTest
    public void testPlaybackInfo() {
        final int playbackType = MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL;
        final int volumeControl = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        final int maxVolume = 10;
        final int currentVolume = 3;

        int audioStream = 77;
        MediaControllerCompat.PlaybackInfo info = new MediaControllerCompat.PlaybackInfo(
                playbackType, audioStream, volumeControl, maxVolume, currentVolume);

        assertEquals(playbackType, info.getPlaybackType());
        assertEquals(audioStream, info.getAudioStream());
        assertEquals(volumeControl, info.getVolumeControl());
        assertEquals(maxVolume, info.getMaxVolume());
        assertEquals(currentVolume, info.getCurrentVolume());
    }

    @Test
    @SmallTest
    public void testGetPlaybackStateWithPositionUpdate() throws InterruptedException {
        final long stateSetTime = SystemClock.elapsedRealtime();
        PlaybackStateCompat stateIn = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, TEST_POSITION, TEST_PLAYBACK_SPEED,
                        stateSetTime)
                .build();
        mSession.setPlaybackState(stateIn);

        final long waitDuration = 100L;
        Thread.sleep(waitDuration);

        final long expectedUpdateTime = waitDuration + stateSetTime;
        final long expectedPosition = (long) (TEST_PLAYBACK_SPEED * waitDuration) + TEST_POSITION;

        final double updateTimeTolerance = 50L;
        final double positionTolerance = updateTimeTolerance * TEST_PLAYBACK_SPEED;

        PlaybackStateCompat stateOut = mSession.getController().getPlaybackState();
        assertEquals(expectedUpdateTime, stateOut.getLastPositionUpdateTime(), updateTimeTolerance);
        assertEquals(expectedPosition, stateOut.getPosition(), positionTolerance);

        // Compare the result with MediaController.getPlaybackState().
        if (Build.VERSION.SDK_INT >= 21) {
            MediaController controller = new MediaController(
                    getContext(), (MediaSession.Token) mSession.getSessionToken().getToken());
            PlaybackState state = controller.getPlaybackState();
            assertEquals(state.getLastPositionUpdateTime(), stateOut.getLastPositionUpdateTime(),
                    updateTimeTolerance);
            assertEquals(state.getPosition(), stateOut.getPosition(), positionTolerance);
        }
    }
}
