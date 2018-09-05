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

import static android.content.Context.KEYGUARD_SERVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Build;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.concurrent.listenablefuture.ListenableFuture;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
public class XMediaPlayerTest {
    private static final String LOG_TAG = "XMediaPlayerTest";

    private static final float FLOAT_TOLERANCE = .0001f;

    private Context mContext;
    private Resources mResources;
    private ExecutorService mExecutor;
    protected XMediaPlayer mPlayer;

    private MediaStubActivity mActivity;
    private Instrumentation mInstrumentation;
    @Rule
    public ActivityTestRule<MediaStubActivity> mActivityRule =
            new ActivityTestRule<>(MediaStubActivity.class);
    private KeyguardManager mKeyguardManager;
    private List<AssetFileDescriptor> mFdsToClose = new ArrayList<>();

    @Before
    @CallSuper
    public void setUp() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mKeyguardManager = (KeyguardManager)
                mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
        mActivity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Keep screen on while testing.
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mActivity.setTurnScreenOn(true);
                mActivity.setShowWhenLocked(true);
                mKeyguardManager.requestDismissKeyguard(mActivity, null);
            }
        });
        mInstrumentation.waitForIdleSync();

        try {
            mActivityRule.runOnUiThread(new Runnable() {
                public void run() {
                    mPlayer = new XMediaPlayer(mActivity);
                }
            });
        } catch (Throwable e) {
            fail();
        }
        mContext = mActivityRule.getActivity();
        mResources = mContext.getResources();
        mExecutor = Executors.newFixedThreadPool(1);
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        if (mPlayer != null) {
            mPlayer.close();
            mPlayer = null;
        }
        mExecutor.shutdown();
        mActivity = null;
        for (AssetFileDescriptor afd :  mFdsToClose) {
            afd.close();
        }
    }

    @Test
    @LargeTest
    public void testPlayerStates() throws Throwable {
        if (!loadResource(androidx.media2.test.R.raw.testvideo)) {
            return; // skip;
        }
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        ListenableFuture<CommandResult2> future;
        assertEquals(XMediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(XMediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        future = mPlayer.prepare();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(XMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(XMediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        future = mPlayer.play();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(XMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(XMediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        future = mPlayer.pause();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(XMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(XMediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        mPlayer.reset();
        assertEquals(XMediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(XMediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
    }

    @Test
    @LargeTest
    public void testPlayerCallback() throws Throwable {
        final int mp4Duration = 8484;
        if (!loadResource(androidx.media2.test.R.raw
                .video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip;
        }

        ListenableFuture<CommandResult2> future;
        final TestUtils.Monitor onSeekCompleteCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onPlayerStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger playerState = new AtomicInteger();
        final TestUtils.Monitor onBufferingStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger bufferingState = new AtomicInteger();
        final TestUtils.Monitor onPlaybackSpeedChanged = new TestUtils.Monitor();
        final AtomicReference<Float> playbackSpeed = new AtomicReference<>();

        XMediaPlayer.PlayerCallback callback = new XMediaPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(SessionPlayer2 player, int state) {
                playerState.set(state);
                onPlayerStateChangedCalled.signal();
            }

            @Override
            public void onBufferingStateChanged(SessionPlayer2 player, DataSourceDesc2 desc,
                    int buffState) {
                bufferingState.set(buffState);
                onBufferingStateChangedCalled.signal();
            }

            @Override
            public void onPlaybackSpeedChanged(SessionPlayer2 player, float speed) {
                playbackSpeed.set(speed);
                onPlaybackSpeedChanged.signal();
            }

            @Override
            public void onSeekCompleted(SessionPlayer2 player, long position) {
                onSeekCompleteCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        onPlayerStateChangedCalled.reset();
        onBufferingStateChangedCalled.reset();
        future = mPlayer.prepare();
        do {
            assertTrue(onBufferingStateChangedCalled.waitForSignal(1000));
        } while (bufferingState.get() != XMediaPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED);

        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        do {
            assertTrue(onPlayerStateChangedCalled.waitForSignal(1000));
        } while (playerState.get() != XMediaPlayer.PLAYER_STATE_PAUSED);
        do {
            assertTrue(onBufferingStateChangedCalled.waitForSignal(1000));
        } while (bufferingState.get() != XMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);

        onSeekCompleteCalled.reset();
        mPlayer.seekTo(mp4Duration >> 1);
        onSeekCompleteCalled.waitForSignal();

        onPlaybackSpeedChanged.reset();
        mPlayer.setPlaybackSpeed(0.5f);
        do {
            assertTrue(onPlaybackSpeedChanged.waitForSignal(1000));
        } while (Math.abs(playbackSpeed.get() - 0.5f) > FLOAT_TOLERANCE);

        mPlayer.reset();
        assertEquals(XMediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        mPlayer.unregisterPlayerCallback(callback);
    }

    private boolean loadResource(int resid) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        try {
            mPlayer.setMediaItem(new FileDataSourceDesc2.Builder(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build());
        } finally {
            // Close descriptor later when test finishes since setMediaItem is async operation.
            mFdsToClose.add(afd);
        }
        return true;
    }
}
