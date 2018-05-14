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

package androidx.media;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.media.AudioManager.AUDIOFOCUS_NONE;
import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
import static android.media.AudioManager.STREAM_MUSIC;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_SPEECH;
import static androidx.media.AudioAttributesCompat.USAGE_ALARM;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
import static androidx.media.AudioAttributesCompat.USAGE_GAME;
import static androidx.media.AudioAttributesCompat.USAGE_MEDIA;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_PAUSED;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_PLAYING;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Process;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.GuardedBy;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.MediaSession2.SessionCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2} for audio focus and noisy intent handling.
 * <p>
 * This may be flaky test because another app including system component may take audio focus.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@MediumTest
@FlakyTest
public class MediaSession2_AudioFocusTest extends MediaSession2TestBase {
    private AudioManager mAudioManager;
    private AudioFocusListener mAudioFocusListener;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusListener = null;
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        abandonAudioFocus();
    }

    private AudioAttributesCompat createAudioAttributes(int contentType, int usage) {
        return new AudioAttributesCompat.Builder()
                .setContentType(contentType).setUsage(usage).build();
    }

    private void sendNoisyIntent(MediaSession2 session) {
        // We cannot use Context.sendBroadcast() because it throws SecurityException for such
        // framework related intent.
        Intent intent = new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        session.getAudioFocusHandler().sendIntent(intent);
    }

    private void testPausedAfterAction(final AudioAttributesCompat attr,
            final SessionRunnable action) throws InterruptedException {
        BaseMediaPlayer player = new MockPlayer(true);
        player.setAudioAttributes(attr);

        final CountDownLatch latchForPlaying = new CountDownLatch(1);
        final CountDownLatch latchForPaused = new CountDownLatch(1);
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(player)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public void onPlayerStateChanged(MediaSession2 session,
                            BaseMediaPlayer player, int state) {
                        switch (state) {
                            case PLAYER_STATE_PLAYING:
                                latchForPlaying.countDown();
                                break;
                            case PLAYER_STATE_PAUSED:
                                latchForPaused.countDown();
                                break;
                        }
                    }
                }).build()) {
            // Play here for registering noisy intent.
            session.play();
            // Playback becomes PLAYING needs to be propagated to the session and its focus handler.
            // Wait for a while for that.
            assertTrue(latchForPlaying.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            // Ensures that it hasn't paused yet.
            assertTrue(latchForPaused.getCount() > 0);

            // Do something that would pause playback.
            action.run(session);

            // Wait until pause actually taking effect.
            assertTrue(latchForPaused.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    private void testDuckedAfterAction(final AudioAttributesCompat attr,
            final SessionRunnable action) throws InterruptedException {
        final CountDownLatch latchForDucked = new CountDownLatch(1);
        BaseMediaPlayer player = new MockPlayer(true) {
            @Override
            public void setPlayerVolume(float volume) {
                super.setPlayerVolume(volume);
                if (volume < getMaxPlayerVolume()) {
                    latchForDucked.countDown();
                }
            }
        };
        player.setAudioAttributes(attr);

        final CountDownLatch latchForPlaying = new CountDownLatch(1);
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(player)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public void onPlayerStateChanged(MediaSession2 session,
                            BaseMediaPlayer player, int state) {
                        if (state == PLAYER_STATE_PLAYING) {
                            latchForPlaying.countDown();
                        }
                    }
                }).build()) {
            session.play();
            // Playback becomes PLAYING needs to be propagated to the session and its focus handler.
            // Wait for a while for that.
            assertTrue(latchForPlaying.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(latchForDucked.getCount() > 0);

            // Do something that would pause playback.
            action.run(session);

            // Wait until pause actually taking effect.
            assertTrue(latchForDucked.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testNoisyIntent_pausePlaybackForMedia() throws InterruptedException {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new SessionRunnable() {
                    @Override
                    public void run(MediaSession2 session) {
                        // Noisy intent would pause for USAGE_MEDIA.
                        sendNoisyIntent(session);
                    }
                });
    }

    @Test
    public void testNoisyIntent_lowerVolumeForGame() throws InterruptedException {
        prepareLooper();

        testDuckedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_GAME),
                new SessionRunnable() {
                    @Override
                    public void run(MediaSession2 session) {
                        // Noisy intent would duck for USAGE_GAME.
                        sendNoisyIntent(session);
                    }
                });
    }

    private void requestAudioFocus(final int gainType) throws InterruptedException {
        if (mAudioFocusListener == null) {
            mAudioFocusListener = new AudioFocusListener();
        }
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                assertEquals(AUDIOFOCUS_REQUEST_GRANTED,
                        mAudioFocusListener.requestAudioFocus(gainType));
            }
        });
    }

    private void waitForAudioFocus(int targetFocusGain) throws InterruptedException {
        assertNotNull(mAudioFocusListener);
        mAudioFocusListener.waitFor(targetFocusGain);
    }

    private void abandonAudioFocus() {
        if (mAudioFocusListener != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
            mAudioFocusListener = null;
        }
    }

    private MediaSession2 createSession(AudioAttributesCompat attr) {
        BaseMediaPlayer player = new MockPlayer(true);
        player.setAudioAttributes(attr);
        return new MediaSession2.Builder(mContext)
                .setPlayer(player)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build();
    }

    /**
     * Tests whether the session requests audio focus, so previsouly focused one loss focus.
     */
    @Test
    public void testAudioFocus_requestFocusWhenPlay() throws InterruptedException {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        AudioAttributesCompat attrs = createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA);
        try (MediaSession2 session = createSession(attrs)) {
            // Play should request audio focus with AUDIOFOCUS_GAIN for USAGE_MEDIA
            session.play();

            // Previously focused one should loss audio focus
            waitForAudioFocus(AUDIOFOCUS_LOSS);
        }
    }

    @Test
    public void testAudioFocus_requestFocusTransient() throws InterruptedException {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        AudioAttributesCompat attrs = createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_ALARM);
        try (MediaSession2 session = createSession(attrs)) {
            // Play should request audio focus with AUDIOFOCUS_GAIN_TRANSIENT for USAGE_ALARM
            session.play();

            waitForAudioFocus(AUDIOFOCUS_LOSS_TRANSIENT);
        }
    }

    @Test
    public void testAudioFocus_requestFocusTransientMayDuck() throws InterruptedException {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        AudioAttributesCompat attrs = createAudioAttributes(
                CONTENT_TYPE_SPEECH, USAGE_ASSISTANCE_NAVIGATION_GUIDANCE);
        try (MediaSession2 session = createSession(attrs)) {
            // Play should request audio focus with AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK for
            // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE.
            session.play();

            waitForAudioFocus(AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
        }
    }

    @Test
    public void testAudioFocus_abandonFocusWhenIdle() throws InterruptedException {
        prepareLooper();

        requestAudioFocus(AUDIOFOCUS_GAIN);

        AudioAttributesCompat attrs = createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_ALARM);
        try (MediaSession2 session = createSession(attrs)) {
            session.play();
            waitForAudioFocus(AUDIOFOCUS_LOSS_TRANSIENT);

            // When session is reset (previously stopped), it should abandon the audio focus.
            session.reset();
            waitForAudioFocus(AUDIOFOCUS_GAIN);
        }
    }

    @Test
    public void testAudioFocus_pauseForFocusLoss() throws InterruptedException {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new SessionRunnable() {
                    @Override
                    public void run(MediaSession2 session) throws InterruptedException {
                        // Somebody else has request audio focus.
                        // Session should lose audio focus and pause playback.
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                });
    }

    @Test
    public void testAudioFocus_pauseForDuckableFocusLoss() throws InterruptedException {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_SPEECH, USAGE_MEDIA),
                new SessionRunnable() {
                    @Override
                    public void run(MediaSession2 session) throws InterruptedException {
                        // Although ducking is possible, CONTENT_TYPE_SPEECH should prefer pause.
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    }
                });
    }

    @Test
    public void testAudioFocus_duckForFocusLoss() throws InterruptedException {
        if (VERSION.SDK_INT >= 26) {
            // On API 26, framework automatically ducks so we cannot test it.
            return;
        }

        prepareLooper();

        testDuckedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new SessionRunnable() {
                    @Override
                    public void run(MediaSession2 session) throws InterruptedException {
                        // This will trigger duck (lower volume).
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    }
                });
    }

    @FunctionalInterface
    private interface SessionRunnable {
        void run(MediaSession2 session) throws InterruptedException;
    }

    private class AudioFocusListener implements OnAudioFocusChangeListener {
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        public int mAudioGain = AUDIOFOCUS_NONE;
        @GuardedBy("mLock")
        private int mTargetAudioGain;
        @GuardedBy("mLock")
        private CountDownLatch mLatch;

        public int requestAudioFocus(int gainType) {
            synchronized (mLock) {
                int gainResult = mAudioManager.requestAudioFocus(
                        mAudioFocusListener, STREAM_MUSIC, gainType);
                mAudioGain = gainResult == AUDIOFOCUS_REQUEST_GRANTED
                        ? AUDIOFOCUS_GAIN : AUDIOFOCUS_LOSS;
                return gainResult;
            }
        }

        @Override
        public void onAudioFocusChange(int focusGain) {
            synchronized (mLock) {
                mAudioGain = focusGain;
                if (mTargetAudioGain == focusGain && mLatch != null) {
                    mLatch.countDown();
                    mLatch = null;
                }
            }
        }

        public void waitFor(int targetFocusGain) throws InterruptedException {
            final CountDownLatch latch;
            synchronized (mLock) {
                if (mAudioGain == targetFocusGain) {
                    // it's already the same as target. Skipping.
                    return;
                }
                mTargetAudioGain = targetFocusGain;
                mLatch = new CountDownLatch(1);
                latch = mLatch;
            }
            assertTrue(
                    "Audio focus didn't change as expected. Expected focusGain=" + targetFocusGain,
                    latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }
}
