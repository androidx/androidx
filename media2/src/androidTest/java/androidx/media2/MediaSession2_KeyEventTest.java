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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Process;
import android.view.KeyEvent;

import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.test.R;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2} whether it handles key events correctly.
 */
// Note: Test fails on pre-P for now because ControllerInfo isn't available for key event, and fails
//       to pass permission check.
@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession2_KeyEventTest extends MediaSession2TestBase {
    private AudioManager mAudioManager;
    private MediaSession2 mSession;
    private MockPlayer mPlayer;
    private MockPlaylistAgent mMockAgent;
    private TestSessionCallback mSessionCallback;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPlayer = new MockPlayer(1);
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PLAYING);
        mMockAgent = new MockPlaylistAgent();

        mSessionCallback = new TestSessionCallback();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
                .setSessionCallback(sHandlerExecutor, mSessionCallback)
                .build();

        // Make this test to get priority for handling media key event
        // SDK < 26: Playback state should become *playing*
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PLAYING);

        // SDK >= 26: Play a media item in the same process of the session.
        // Target raw resource should be short enough to finish within the time limit of @SmallTest.
        final CountDownLatch latch = new CountDownLatch(1);
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Pick the shortest media.
                final MediaPlayer player = MediaPlayer.create(mContext, R.raw.camera_click);
                player.setOnCompletionListener(new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        latch.countDown();
                    }
                });
                player.start();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.close();
    }

    private void dispatchMediaKeyEvent(int keyCode, boolean doubleTap) {
        mAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        mAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        if (doubleTap) {
            mAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            mAudioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }

    @Test
    public void testPlay() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPause() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testNext() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, false);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToNextItemCalled);
    }

    @Test
    public void testPrevious() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToPreviousItemCalled);
    }

    @Test
    public void testStop() throws Exception {
        prepareLooper();
        mPlayer = new MockPlayer(2);
        mSession.updatePlayerConnector(mPlayer, mSession.getPlaylistAgent());
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
        assertTrue(mPlayer.mSeekToCalled);
    }

    @Test
    public void testFastForward() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, false);
        assertTrue(mSessionCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mFastForwardCalled);
    }

    @Test
    public void testRewind() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, false);
        assertTrue(mSessionCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mRewindCalled);
    }

    @Test
    public void testPlayPause_play() throws Exception {
        prepareLooper();
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PAUSED);
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPlayPause_pause() throws Exception {
        prepareLooper();
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PLAYING);
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPlayPause_doubleTap() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToNextItemCalled);
        assertFalse(mPlayer.mPlayCalled);
        assertFalse(mPlayer.mPauseCalled);
    }

    private static class TestSessionCallback extends MediaSession2.SessionCallback {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        boolean mFastForwardCalled;
        boolean mRewindCalled;

        @Override
        public SessionCommandGroup2 onConnect(MediaSession2 session, ControllerInfo controller) {
            if (Process.myUid() == controller.getUid()) {
                return super.onConnect(session, controller);
            }
            return null;
        }

        @Override
        public void onFastForward(MediaSession2 session, ControllerInfo controller) {
            mFastForwardCalled = true;
            mCountDownLatch.countDown();
        }

        @Override
        public void onRewind(MediaSession2 session, ControllerInfo controller) {
            mRewindCalled = true;
            mCountDownLatch.countDown();
        }
    }
}
