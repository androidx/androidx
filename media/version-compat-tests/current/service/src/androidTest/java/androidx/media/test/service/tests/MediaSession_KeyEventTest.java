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

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER;
import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_SUCCESS;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.support.mediacompat.service.R;
import android.view.KeyEvent;

import androidx.media.test.service.MockPlayer;
import androidx.media2.MediaSession;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionPlayer;
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
 * Tests {@link MediaSession} whether it handles key events correctly.
 * In order to get the media key events, the player state is set to 'Playing' before every test
 * method.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession_KeyEventTest extends MediaSessionTestBase {
    private static String sExpectedControllerPackageName;

    private AudioManager mAudioManager;
    private MediaSession mSession;
    private MockPlayer mPlayer;
    private TestSessionCallback mSessionCallback;

    static {
        if (Build.VERSION.SDK_INT >= 28) {
            sExpectedControllerPackageName = SERVICE_PACKAGE_NAME;
        } else if (Build.VERSION.SDK_INT >= 24) {
            // KeyEvent from system service has the package name "android".
            sExpectedControllerPackageName = "android";
        } else {
            // In API 21+, MediaSessionCompat#getCurrentControllerInfo always returns dummy info.
            sExpectedControllerPackageName = LEGACY_CONTROLLER;
        }
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPlayer = new MockPlayer(1);
        mPlayer.notifyPlayerStateChanged(SessionPlayer.PLAYER_STATE_PLAYING);

        mSessionCallback = new TestSessionCallback();
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, mSessionCallback)
                .build();

        // Make this test to get priority for handling media key event
        // SDK < 26: Playback state should become *playing*
        mPlayer.notifyPlayerStateChanged(SessionPlayer.PLAYER_STATE_PLAYING);

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
                        player.release();
                    }
                });
                player.start();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
    public void testPlayKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPauseKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testNextKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void testPreviousKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void testStopKeyEvent() throws Exception {
        prepareLooper();
        mPlayer = new MockPlayer(2);
        mSession.updatePlayer(mPlayer);
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
        assertTrue(mPlayer.mSeekToCalled);
    }

    @Test
    public void testFastForwardKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, false);
        assertTrue(mSessionCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mFastForwardCalled);
    }

    @Test
    public void testRewindKeyEvent() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND, false);
        assertTrue(mSessionCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mRewindCalled);
    }

    @Test
    public void testPlayPauseKeyEvent_play() throws Exception {
        prepareLooper();
        mPlayer.notifyPlayerStateChanged(SessionPlayer.PLAYER_STATE_PAUSED);
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPlayPauseKeyEvent_pause() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPlayPauseKeyEvent_doubleTapIsTranslatedToSkipToNext() throws Exception {
        prepareLooper();
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
        assertFalse(mPlayer.mPlayCalled);
        assertFalse(mPlayer.mPauseCalled);
    }

    private static class TestSessionCallback extends MediaSession.SessionCallback {
        final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        boolean mFastForwardCalled;
        boolean mRewindCalled;

        @Override
        public SessionCommandGroup onConnect(MediaSession session, ControllerInfo controller) {
            if (sExpectedControllerPackageName.equals(controller.getPackageName())) {
                return super.onConnect(session, controller);
            }
            return null;
        }

        @Override
        public int onFastForward(MediaSession session, ControllerInfo controller) {
            mFastForwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_CODE_SUCCESS;
        }

        @Override
        public int onRewind(MediaSession session, ControllerInfo controller) {
            mRewindCalled = true;
            mCountDownLatch.countDown();
            return RESULT_CODE_SUCCESS;
        }
    }
}
