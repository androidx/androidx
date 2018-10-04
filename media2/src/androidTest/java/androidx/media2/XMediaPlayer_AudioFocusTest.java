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
import static androidx.media.AudioAttributesCompat.USAGE_UNKNOWN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.media.AudioAttributesCompat;
import androidx.media2.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link XMediaPlayer} for audio focus and noisy intent handling.
 * <p>
 * This may be flaky test because another app including system component may take audio focus.
 */
// TODO: Lower the minSdk version. Currently instantiating ExoPlayerMediaPlayer2Impl fails in API26
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@MediumTest
public class XMediaPlayer_AudioFocusTest extends XMediaPlayerTestBase {
    private static final int WAIT_TIME_MS = 2000;

    static TestUtils.SyncHandler sHandler;
    static Executor sHandlerExecutor;

    private AudioManager mAudioManager;
    private AudioFocusListener mAudioFocusListener;

    @BeforeClass
    public static void setUpThread() {
        synchronized (XMediaPlayer_AudioFocusTest.class) {
            if (sHandler != null) {
                return;
            }
            prepareLooper();
            HandlerThread handlerThread = new HandlerThread("XMediaPlayer_AudioFocusTest");
            handlerThread.start();
            sHandler = new TestUtils.SyncHandler(handlerThread.getLooper());
            sHandlerExecutor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    TestUtils.SyncHandler handler;
                    synchronized (MediaSession2TestBase.class) {
                        handler = sHandler;
                    }
                    if (handler != null) {
                        handler.post(runnable);
                    }
                }
            };
            if (Looper.getMainLooper() == null) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepareMainLooper();
                    }
                });
            }
        }
    }

    @AfterClass
    public static void cleanUpThread() {
        synchronized (XMediaPlayer_AudioFocusTest.class) {
            if (sHandler == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 18) {
                sHandler.getLooper().quitSafely();
            } else {
                sHandler.getLooper().quit();
            }
            sHandler = null;
            sHandlerExecutor = null;
        }
    }

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusListener = null;
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        abandonAudioFocus();
    }

    private AudioAttributesCompat createAudioAttributes(int contentType, int usage) {
        return new AudioAttributesCompat.Builder()
                .setContentType(contentType).setUsage(usage).build();
    }

    private void sendNoisyIntent(XMediaPlayer player) {
        // We cannot use Context.sendBroadcast() because it throws SecurityException for such
        // framework related intent.
        Intent intent = new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        player.getAudioFocusHandler().sendIntent(intent);
    }

    private void initPlayer(AudioAttributesCompat attr) throws Exception {
        loadResource(R.raw.loudsoftogg);
        mPlayer.setAudioAttributes(attr);
        assertEquals(SessionPlayer2.PlayerResult.RESULT_CODE_SUCCESS,
                mPlayer.prepare().get(WAIT_TIME_MS, TimeUnit.MILLISECONDS).getResultCode());
    }

    private void testPausedAfterAction(final AudioAttributesCompat attr,
            final PlayerRunnable action) throws Exception {
        final CountDownLatch latchForPlaying = new CountDownLatch(1);
        final CountDownLatch latchForPaused = new CountDownLatch(1);
        initPlayer(attr);

        mPlayer.registerPlayerCallback(sHandlerExecutor, new SessionPlayer2.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(SessionPlayer2 mPlayer, int playerState) {
                    switch (playerState) {
                        case SessionPlayer2.PLAYER_STATE_PLAYING:
                            latchForPlaying.countDown();
                            break;
                        case SessionPlayer2.PLAYER_STATE_PAUSED:
                            latchForPaused.countDown();
                            break;
                    }
                }
            });

        // Play here for registering noisy intent.
        mPlayer.play();
        // Playback becomes PLAYING needs to be propagated to the session and its focus handler.
        // Wait for a while for that.
        assertTrue(latchForPlaying.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        // Ensures that it hasn't paused yet.
        assertTrue(latchForPaused.getCount() > 0);

        // Do something that would pause playback.
        action.run(mPlayer);

        // Wait until pause actually taking effect.
        assertTrue(latchForPaused.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    private void testDuckedAfterAction(final AudioAttributesCompat attr,
            final PlayerRunnable action) throws Exception {
        final CountDownLatch latchForDucked = new CountDownLatch(1);
        final CountDownLatch latchForPlaying = new CountDownLatch(1);

        try {
            mInstrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    mPlayer = new XMediaPlayer(mActivity) {
                        @Override
                        public ListenableFuture<PlayerResult> setPlayerVolume(float volume) {
                            if (volume < getMaxPlayerVolume()) {
                                latchForDucked.countDown();
                            }
                            return super.setPlayerVolume(volume);
                        }
                    };
                }
            });
        } catch (Throwable e) {
            fail();
        }

        initPlayer(attr);
        mPlayer.registerPlayerCallback(sHandlerExecutor, new SessionPlayer2.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(SessionPlayer2 player, int playerState) {
                if (playerState == SessionPlayer2.PLAYER_STATE_PLAYING) {
                    latchForPlaying.countDown();
                }
            }
        });
        mPlayer.play();
        // Playback becomes PLAYING needs to be propagated to the session and its focus handler.
        // Wait for a while for that.
        assertTrue(latchForPlaying.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(latchForDucked.getCount() > 0);

        // Do something that would pause playback.
        action.run(mPlayer);

        // Wait until pause actually taking effect.
        assertTrue(latchForDucked.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNoisyIntent_pausePlaybackForMedia() throws Exception {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new PlayerRunnable() {
                    @Override
                    public void run(XMediaPlayer player) {
                        // Noisy intent would pause for USAGE_MEDIA.
                        sendNoisyIntent(player);
                    }
                });
    }

    @Test
    public void testNoisyIntent_lowerVolumeForGame() throws Exception {
        prepareLooper();

        testDuckedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_GAME),
                new PlayerRunnable() {
                    @Override
                    public void run(XMediaPlayer player) {
                        // Noisy intent would duck for USAGE_GAME.
                        sendNoisyIntent(player);
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

    private void assertNoAudioFocusChanges(int expectedFocusGain) throws InterruptedException {
        assertNotNull(mAudioFocusListener);
        mAudioFocusListener.assertNoAudioFocusChanges(expectedFocusGain);
    }

    private void abandonAudioFocus() {
        if (mAudioFocusListener != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
            mAudioFocusListener = null;
        }
    }

    /**
     * Tests whether the session requests audio focus, so previously focused one loss focus.
     */
    @Test
    public void testAudioFocus_requestFocusWhenPlay() throws Exception {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        initPlayer(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA));

        // Play should request audio focus with AUDIOFOCUS_GAIN for USAGE_MEDIA
        mPlayer.play();

        // Previously focused one should loss audio focus
        waitForAudioFocus(AUDIOFOCUS_LOSS);
    }

    @Test
    public void testAudioFocus_requestFocusWhenUnknown() throws Exception {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        initPlayer(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_UNKNOWN));

        // Play should request audio focus with AUDIOFOCUS_GAIN for USAGE_MEDIA
        mPlayer.play();

        // Previously focused one should loss audio focus
        waitForAudioFocus(AUDIOFOCUS_LOSS);
    }

    @Test
    public void testAudioFocus_requestFocusTransient() throws Exception {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        initPlayer(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_ALARM));

        // Play should request audio focus with AUDIOFOCUS_GAIN_TRANSIENT for USAGE_ALARM
        mPlayer.play();

        waitForAudioFocus(AUDIOFOCUS_LOSS_TRANSIENT);
    }

    @Test
    public void testAudioFocus_requestFocusTransientMayDuck() throws Exception {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);

        initPlayer(createAudioAttributes(
                CONTENT_TYPE_SPEECH, USAGE_ASSISTANCE_NAVIGATION_GUIDANCE));

        // Play should request audio focus with AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK for
        // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE.
        mPlayer.play();

        waitForAudioFocus(AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    }

    @Test
    public void testAudioFocus_setVolumeZeroWhenAudioAttributesIsMissing()
            throws Exception {
        prepareLooper();

        // Request an audio focus in advance.
        requestAudioFocus(AUDIOFOCUS_GAIN);
        initPlayer(null);
        mPlayer.play();
        assertNoAudioFocusChanges(AUDIOFOCUS_GAIN);
        assertEquals(0, mPlayer.getPlayerVolume(), 0.1f);
    }

    @Test
    public void testAudioFocus_pauseForFocusLoss() throws Exception {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new PlayerRunnable() {
                    @Override
                    public void run(XMediaPlayer player) throws InterruptedException {
                        // Somebody else has request audio focus.
                        // Session should lose audio focus and pause playback.
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                });
    }

    @Test
    public void testAudioFocus_pauseForDuckableFocusLoss() throws Exception {
        prepareLooper();

        testPausedAfterAction(createAudioAttributes(CONTENT_TYPE_SPEECH, USAGE_MEDIA),
                new PlayerRunnable() {
                    @Override
                    public void run(XMediaPlayer player) throws InterruptedException {
                        // Although ducking is possible, CONTENT_TYPE_SPEECH should prefer pause.
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    }
                });
    }

    @Test
    public void testAudioFocus_duckForFocusLoss() throws Exception {
        if (VERSION.SDK_INT >= 26) {
            // On API 26, framework automatically ducks so we cannot test it.
            return;
        }

        prepareLooper();

        testDuckedAfterAction(createAudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA),
                new PlayerRunnable() {
                    @Override
                    public void run(XMediaPlayer player) throws InterruptedException {
                        // This will trigger duck (lower volume).
                        requestAudioFocus(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    }
                });
    }

    @FunctionalInterface
    private interface PlayerRunnable {
        void run(XMediaPlayer player) throws InterruptedException;
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
                    latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }

        public void assertNoAudioFocusChanges(int expectedFocusGain) throws InterruptedException {
            final CountDownLatch latch;
            synchronized (mLock) {
                assertEquals(expectedFocusGain, mAudioGain);
                mTargetAudioGain = AUDIOFOCUS_NONE;
                mLatch = new CountDownLatch(1);
                latch = mLatch;
            }
            assertFalse("Audio focus unexpectidly changed",
                    latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }
}
