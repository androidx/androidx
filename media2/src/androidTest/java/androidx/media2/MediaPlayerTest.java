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

import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_SKIPPED;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_SUCCESS;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.media.AudioAttributesCompat;
import androidx.media2.SessionPlayer.PlayerResult;
import androidx.media2.TestUtils.Monitor;
import androidx.media2.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
public class MediaPlayerTest extends MediaPlayerTestBase {
    private static final String LOG_TAG = "MediaPlayerTest";

    private static final int SLEEP_TIME = 1000;
    private static final int WAIT_TIME_MS = 300;
    private static final float FLOAT_TOLERANCE = .0001f;
    private static final int INVALID_SHUFFLE_MODE = -1000;
    private static final int INVALID_REPEAT_MODE = -1000;

    private Object mPlayerCbArg1;
    private Object mPlayerCbArg2;

    private final Vector<Integer> mSubtitleTrackIndex = new Vector<>();
    private int mSelectedSubtitleIndex;
    private final Monitor mOnSubtitleDataCalled = new Monitor();
    private final Monitor mOnInfoCalled = new Monitor();
    private final Monitor mOnMediaTimeDiscontinuityCalled = new Monitor();
    private final Monitor mOnErrorCalled = new Monitor();

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @MediumTest
    public void testPlayAudioOnce() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);

        final TestUtils.Monitor playing = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor, new SessionPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(SessionPlayer player, int playerState) {
                playing.signal();
            }
        });

        mPlayer.prepare();
        mPlayer.play();

        assertTrue(playing.waitForSignal(SLEEP_TIME));
    }

    @Test
    @LargeTest
    public void testPlayAudio() throws Exception {
        final int resid = R.raw.testmp3_2;
        final int mp3Duration = 34909;
        final int tolerance = 70;
        final int seekDuration = 100;

        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        mPlayer.setMediaItem(new FileMediaItem.Builder(
                afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength())
                .build());
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);

        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());

        assertFalse(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);
        future = mPlayer.play();
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        assertEquals(mp3Duration, mPlayer.getDuration(), tolerance);
        long pos = mPlayer.getCurrentPosition();
        assertTrue(pos >= 0);
        assertTrue(pos < mp3Duration - seekDuration);

        future = mPlayer.seekTo(pos + seekDuration, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());
        assertEquals(pos + seekDuration, mPlayer.getCurrentPosition(), tolerance);

        future = mPlayer.pause();
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());
        assertFalse(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);
        future = mPlayer.play();
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // waiting to complete
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
    }

    @Test
    @LargeTest
    public void testPlayVideo() throws Exception {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        final int width = 352;
        final int height = 288;
        final float volume = 0.5f;

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        final TestUtils.Monitor onVideoSizeChangedCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onVideoRenderingStartCalled = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, MediaItem dsd, VideoSize size) {
                if (size.getWidth() == 0 && size.getHeight() == 0) {
                    // A size of 0x0 can be sent initially one time when using NuPlayer.
                    assertFalse(onVideoSizeChangedCalled.isSignalled());
                    return;
                }
                onVideoSizeChangedCalled.signal();
                assertEquals(width, size.getWidth());
                assertEquals(height, size.getHeight());
            }

            @Override
            public void onError(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                fail("Media player had error " + what + " playing video");
            }

            @Override
            public void onInfo(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    onVideoRenderingStartCalled.signal();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        mPlayer.prepare();
        mPlayer.play();

        onVideoSizeChangedCalled.waitForSignal();
        onVideoRenderingStartCalled.waitForSignal();

        mPlayer.setPlayerVolume(volume);

        // waiting to complete
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }

        // validate a few MediaMetrics.
        PersistableBundle metrics = mPlayer.getMetrics();
        if (metrics == null) {
            fail("MediaPlayer.getMetrics() returned null metrics");
        } else if (metrics.isEmpty()) {
            fail("MediaPlayer.getMetrics() returned empty metrics");
        } else {

            int size = metrics.size();
            Set<String> keys = metrics.keySet();

            if (keys == null) {
                fail("MediaMetricsSet returned no keys");
            } else if (keys.size() != size) {
                fail("MediaMetricsSet.keys().size() mismatch MediaMetricsSet.size()");
            }

            // we played something; so one of these should be non-null
            String vmime = metrics.getString(MediaPlayer2.MetricsConstants.MIME_TYPE_VIDEO, null);
            String amime = metrics.getString(MediaPlayer2.MetricsConstants.MIME_TYPE_AUDIO, null);
            if (vmime == null && amime == null) {
                fail("getMetrics() returned neither video nor audio mime value");
            }

            long duration = metrics.getLong(MediaPlayer2.MetricsConstants.DURATION, -2);
            if (duration == -2) {
                fail("getMetrics() didn't return a duration");
            }
            long playing = metrics.getLong(MediaPlayer2.MetricsConstants.PLAYING, -2);
            if (playing == -2) {
                fail("getMetrics() didn't return a playing time");
            }
            if (!keys.contains(MediaPlayer2.MetricsConstants.PLAYING)) {
                fail("MediaMetricsSet.keys() missing: " + MediaPlayer2.MetricsConstants.PLAYING);
            }
        }
    }

    @Test
    @SmallTest
    public void testGetDuration() throws Exception {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        final int expectedDuration = 11047;
        final int tolerance = 70;

        mPlayer.setSurface(mActivity.getSurfaceHolder2().getSurface());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getDuration());

        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());

        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());
        assertEquals(expectedDuration, mPlayer.getDuration(), tolerance);
    }

    @Test
    @SmallTest
    public void testGetCurrentPosition() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getCurrentPosition());
    }

    @Test
    @SmallTest
    public void testGetBufferedPosition() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getBufferedPosition());
    }

    @Test
    @SmallTest
    public void testGetPlaybackSpeed() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        try {
            mPlayer.getPlaybackSpeed();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @LargeTest
    public void testPlaybackRate() throws Exception {
        final int toleranceMs = 1000;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            fail();
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        float[] rates = { 0.25f, 0.5f, 1.0f, 2.0f };
        for (float playbackRate : rates) {
            mPlayer.seekTo(0, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            Thread.sleep(1000);
            int playTime = 4000;  // The testing clip is about 10 second long.
            int privState = mPlayer.getPlayerState();

            future = mPlayer.setPlaybackParams(
                    new PlaybackParams.Builder().setSpeed(playbackRate).build());
            assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());
            assertTrue("setPlaybackParams() should not change player state. "
                            + mPlayer.getPlayerState(), mPlayer.getPlayerState() == privState);

            future = mPlayer.play();
            assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());
            Thread.sleep(playTime);

            PlaybackParams pbp = mPlayer.getPlaybackParams();
            assertEquals(playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
            assertTrue("The player should still be playing",
                    mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

            long playedMediaDurationMs = mPlayer.getCurrentPosition();
            int diff = Math.abs((int) (playedMediaDurationMs / playbackRate) - playTime);
            if (diff > toleranceMs) {
                fail("Media player had error in playback rate " + playbackRate
                        + ", play time is " + playTime + " vs expected " + playedMediaDurationMs);
            }
            future = mPlayer.pause();
            assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

            pbp = mPlayer.getPlaybackParams();
            assertEquals("pause() should not change the playback rate property.",
                    playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
        }
        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void testSeekModes() throws Exception {
        // This clip has 2 I frames at 66687us and 4299687us.
        if (!loadResource(
                R.raw.bbb_s1_320x240_mp4_h264_mp2_800kbps_30fps_aac_lc_5ch_240kbps_44100hz)) {
            fail();
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        mPlayer.play();

        final long seekPosMs = 3000;
        final long timeToleranceMs = 100;
        final long syncTime1Ms = 67;
        final long syncTime2Ms = 4300;

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to previous sync or next sync.
        long cp = runSeekMode(MediaPlayer2.SEEK_CLOSEST, seekPosMs);
        assertTrue("MediaPlayer2 did not seek to closest position",
                cp > seekPosMs && cp < syncTime2Ms);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or next sync.
        cp = runSeekMode(MediaPlayer2.SEEK_PREVIOUS_SYNC, seekPosMs);
        assertTrue("MediaPlayer2 did not seek to preivous sync position",
                cp < seekPosMs - timeToleranceMs);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or previous sync.
        cp = runSeekMode(MediaPlayer2.SEEK_NEXT_SYNC, seekPosMs);
        assertTrue("MediaPlayer2 did not seek to next sync position",
                cp > syncTime2Ms - timeToleranceMs);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or previous sync.
        cp = runSeekMode(MediaPlayer2.SEEK_CLOSEST_SYNC, seekPosMs);
        assertTrue("MediaPlayer2 did not seek to closest sync position",
                cp > syncTime2Ms - timeToleranceMs);

        mPlayer.reset();
    }

    private long runSeekMode(int seekMode, long seekPosMs) throws Exception {
        final int sleepIntervalMs = 100;
        int timeRemainedMs = 10000;  // total time for testing
        final int timeToleranceMs = 100;

        ListenableFuture<PlayerResult> future = mPlayer.seekTo(seekPosMs, seekMode);
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        long cp = -seekPosMs;
        while (timeRemainedMs > 0) {
            cp = mPlayer.getCurrentPosition();
            // Wait till MediaPlayer2 starts rendering since MediaPlayer2 caches
            // seek position as current position.
            if (cp < seekPosMs - timeToleranceMs || cp > seekPosMs + timeToleranceMs) {
                break;
            }
            timeRemainedMs -= sleepIntervalMs;
            Thread.sleep(sleepIntervalMs);
        }
        assertTrue("MediaPlayer2 did not finish seeking in time for mode " + seekMode,
                timeRemainedMs > 0);
        return cp;
    }

    @Test
    @LargeTest
    public void testGetTimestamp() throws Exception {
        final int toleranceUs = 100000;
        final float playbackRate = 1.0f;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            fail();
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        ListenableFuture<PlayerResult> future = mPlayer.prepare();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        mPlayer.play();
        mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(playbackRate).build());
        Thread.sleep(SLEEP_TIME);  // let player get into stable state.
        long nt1 = System.nanoTime();
        MediaTimestamp ts1 = mPlayer.getTimestamp();
        long nt2 = System.nanoTime();
        assertTrue("Media player should return a valid time stamp", ts1 != null);
        assertEquals("MediaPlayer2 had error in clockRate " + ts1.getMediaClockRate(),
                playbackRate, ts1.getMediaClockRate(), 0.001f);
        assertTrue("The nanoTime of Media timestamp should be taken when getTimestamp is called.",
                nt1 <= ts1.getAnchorSystemNanoTime() && ts1.getAnchorSystemNanoTime() <= nt2);

        future = mPlayer.pause();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        ts1 = mPlayer.getTimestamp();
        assertTrue("Media player should return a valid time stamp", ts1 != null);
        assertTrue("Media player should have play rate of 0.0f when paused",
                ts1.getMediaClockRate() == 0.0f);

        mPlayer.seekTo(0, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mPlayer.play();
        Thread.sleep(SLEEP_TIME);  // let player get into stable state.
        int playTime = 4000;  // The testing clip is about 10 second long.
        ts1 = mPlayer.getTimestamp();
        assertTrue("Media player should return a valid time stamp", ts1 != null);
        Thread.sleep(playTime);
        MediaTimestamp ts2 = mPlayer.getTimestamp();
        assertTrue("Media player should return a valid time stamp", ts2 != null);
        assertTrue("The clockRate should not be changed.",
                ts1.getMediaClockRate() == ts2.getMediaClockRate());
        assertEquals("MediaPlayer2 had error in timestamp.",
                ts1.getAnchorMediaTimeUs() + (long) (playTime * ts1.getMediaClockRate() * 1000),
                ts2.getAnchorMediaTimeUs(), toleranceUs);

        mPlayer.reset();
    }

    private void readSubtitleTracks() throws Exception {
        mSubtitleTrackIndex.clear();
        List<MediaPlayer.TrackInfo> trackInfos = mPlayer.getTrackInfo();
        if (trackInfos == null || trackInfos.size() == 0) {
            return;
        }

        Vector<Integer> subtitleTrackIndex = new Vector<>();
        for (int i = 0; i < trackInfos.size(); ++i) {
            assertTrue(trackInfos.get(i) != null);
            if (trackInfos.get(i).getTrackType()
                    == MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                subtitleTrackIndex.add(i);
            }
        }

        mSubtitleTrackIndex.addAll(subtitleTrackIndex);
    }

    private void selectSubtitleTrack(int index) throws Exception {
        int trackIndex = mSubtitleTrackIndex.get(index);
        ListenableFuture<PlayerResult> future = mPlayer.selectTrack(trackIndex);
        assertEquals(RESULT_CODE_SUCCESS, future.get().getResultCode());
        mSelectedSubtitleIndex = index;
    }

    private int deselectSubtitleTrack(int index) throws Exception {
        int trackIndex = mSubtitleTrackIndex.get(index);
        ListenableFuture<PlayerResult> future = mPlayer.deselectTrack(trackIndex);
        if (mSelectedSubtitleIndex == index) {
            mSelectedSubtitleIndex = -1;
        }
        return future.get().getResultCode();
    }

    @Test
    @LargeTest
    public void testDeselectTrackForSubtitleTracks() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        mInstrumentation.waitForIdleSync();

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onInfo(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }

            @Override
            public void onSubtitleData(MediaPlayer mp, MediaItem dsd, SubtitleData data) {
                if (data != null && data.getData() != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.prepare();
        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // Closed caption tracks are in-band.
        // So, those tracks will be found after processing a number of frames.
        mOnInfoCalled.waitForSignal(1500);

        mOnInfoCalled.reset();
        mOnInfoCalled.waitForSignal(1500);

        readSubtitleTracks();

        // Run twice to check if repeated selection-deselection on the same track works well.
        for (int i = 0; i < 2; i++) {
            // Waits until at least one subtitle is fired. Timeout is 2.5 seconds.
            selectSubtitleTrack(i);
            mOnSubtitleDataCalled.reset();
            assertTrue(mOnSubtitleDataCalled.waitForSignal(2500));

            // Try deselecting track.
            assertEquals(RESULT_CODE_SUCCESS, deselectSubtitleTrack(i));
            mOnSubtitleDataCalled.reset();
            assertFalse(mOnSubtitleDataCalled.waitForSignal(1500));
        }

        // Deselecting unselected track: expected error status
        assertNotEquals(RESULT_CODE_SUCCESS, deselectSubtitleTrack(0));
        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void testChangeSubtitleTrack() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onInfo(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }

            @Override
            public void onSubtitleData(
                    MediaPlayer mp, MediaItem dsd, SubtitleData data) {
                if (data != null && data.getData() != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.prepare();
        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // Closed caption tracks are in-band.
        // So, those tracks will be found after processing a number of frames.
        mOnInfoCalled.waitForSignal(1500);

        mOnInfoCalled.reset();
        mOnInfoCalled.waitForSignal(1500);

        readSubtitleTracks();

        // Waits until at least two captions are fired. Timeout is 2.5 sec.
        selectSubtitleTrack(0);
        assertTrue(mOnSubtitleDataCalled.waitForCountedSignals(2, 2500) >= 2);

        mOnSubtitleDataCalled.reset();
        selectSubtitleTrack(1);
        assertTrue(mOnSubtitleDataCalled.waitForCountedSignals(2, 2500) >= 2);

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void testGetTrackInfoForVideoWithSubtitleTracks() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onInfo(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.prepare();
        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // The media metadata will be changed while playing since closed caption tracks are in-band
        // and those tracks will be found after processing a number of frames. These tracks will be
        // found within one second.
        mOnInfoCalled.waitForSignal(1500);

        mOnInfoCalled.reset();
        mOnInfoCalled.waitForSignal(1500);

        readSubtitleTracks();
        assertEquals(2, mSubtitleTrackIndex.size());

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void testMediaTimeDiscontinuity() throws Exception {
        if (!loadResource(
                R.raw.bbb_s1_320x240_mp4_h264_mp2_800kbps_30fps_aac_lc_5ch_240kbps_44100hz)) {
            return; // skip
        }

        final BlockingDeque<MediaTimestamp> timestamps = new LinkedBlockingDeque<>();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onMediaTimeDiscontinuity(
                    MediaPlayer mp, MediaItem dsd, MediaTimestamp timestamp) {
                timestamps.add(timestamp);
                mOnMediaTimeDiscontinuityCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.prepare();

        // Timestamp needs to be reported when playback starts.
        mOnMediaTimeDiscontinuityCalled.reset();
        mPlayer.play();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 1.0f) > 0.01f);

        // Timestamp needs to be reported when seeking is done.
        mOnMediaTimeDiscontinuityCalled.reset();
        assertEquals(RESULT_CODE_SUCCESS, mPlayer.seekTo(3000).get().getResultCode());
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 1.0f) > 0.01f);

        // Timestamp needs to be updated when playback rate changes.
        mOnMediaTimeDiscontinuityCalled.reset();
        mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(0.5f).build());
        mOnMediaTimeDiscontinuityCalled.waitForSignal();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 0.5f) > 0.01f);

        // Timestamp needs to be updated when player is paused.
        mOnMediaTimeDiscontinuityCalled.reset();
        mPlayer.pause();
        mOnMediaTimeDiscontinuityCalled.waitForSignal();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 0.0f) > 0.01f);

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void testPlaybackFromDataSourceCallback() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final int duration = 10000;

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        // Test returning -1 from getSize() to indicate unknown size.
        dataSource.returnFromGetSize(-1);
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());
        mPlayer.prepare();
        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // Test pause and restart.
        mPlayer.pause();
        Thread.sleep(SLEEP_TIME);
        assertFalse(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // Test reset.
        mPlayer.reset();
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        mPlayer.prepare();
        mPlayer.play().get();
        assertTrue(mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING);

        // Test seek. Note: the seek position is cached and returned as the
        // current position so there's no point in comparing them.
        mPlayer.seekTo(duration - SLEEP_TIME, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
    }

    @Test
    @LargeTest
    public void testNullMedia2DataSourceIsRejected() throws Exception {
        try {
            mPlayer.setMediaItem(null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    @Test
    @LargeTest
    public void testMedia2DataSourceIsClosedOnReset() throws Exception {
        TestDataSourceCallback dataSource = new TestDataSourceCallback(new byte[0]);
        assertEquals(RESULT_CODE_SUCCESS,
                mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build()).get()
                        .getResultCode());
        mPlayer.reset();
        assertTrue(dataSource.isClosed());
    }

    @Test
    @LargeTest
    public void testPlaybackFailsIfMedia2DataSourceThrows() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onError(
                    MediaPlayer mp, MediaItem dsd, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        mPlayer.prepare().get();

        mOnErrorCalled.reset();
        dataSource.throwFromReadAt();
        mPlayer.play();
        assertTrue(mOnErrorCalled.waitForSignal());
    }

    @Test
    @LargeTest
    public void testPlaybackFailsIfMedia2DataSourceReturnsAnError() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onError(
                    MediaPlayer mp, MediaItem dsd, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        mPlayer.prepare().get();

        dataSource.returnFromReadAt(-2);
        mPlayer.play();
        assertTrue(mOnErrorCalled.waitForSignal());
    }

    @Test
    @LargeTest
    public void testPreservePlaybackProperties() throws Exception {
        /* TODO: enable this test once MediaPlayer has playlist implementation.
        final int resid1 = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final long start1 = 6000;
        final long end1 = 7000;
        AssetFileDescriptor afd1 = mResources.openRawResourceFd(resid1);
        MediaItem dsd1 = new FileMediaItem.Builder(
                afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength())
                .setStartPosition(start1)
                .setEndPosition(end1)
                .build();

        final int resid2 = R.raw.testvideo;
        final long start2 = 3000;
        final long end2 = 4000;
        AssetFileDescriptor afd2 = mResources.openRawResourceFd(resid2);
        MediaItem dsd2 = new FileMediaItem.Builder(
                afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength())
                .setStartPosition(start2)
                .setEndPosition(end2)
                .build();

        mPlayer.setMediaItem();
        mPlayer.setNextMediaItem(dsd2);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem dsd, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem dsd, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    assertTrue(status == MediaPlayer2.CALL_STATUS_NO_ERROR);
                    mOnPlayCalled.signal();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mOnCompletionCalled.reset();
        mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(2.0f).build());
        mPlayer.play();

        mOnPlayCalled.waitForSignal();
        mOnCompletionCalled.waitForSignal();

        assertEquals(dsd2, mPlayer.getCurrentMediaItem());
        assertEquals(2.0f, mPlayer.getPlaybackParams().getSpeed(), 0.001f);

        afd1.close();
        afd2.close();
        */
    }

    @Test
    @MediumTest
    public void testDefaultPlaybackParams() throws Exception {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }
        mPlayer.prepare().get();

        PlaybackParams playbackParams = mPlayer.getPlaybackParams();
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT,
                (int) playbackParams.getAudioFallbackMode());
        assertEquals(1.0f, playbackParams.getPitch(), 0.001f);
        assertEquals(1.0f, playbackParams.getSpeed(), 0.001f);

        mPlayer.reset();
    }

    @Test
    @SmallTest
    public void testSkipUnnecessarySeek() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final TestDataSourceCallback source =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        final Monitor readAllowed = new Monitor();
        DataSourceCallback dataSource = new DataSourceCallback() {
            @Override
            public int readAt(long position, byte[] buffer, int offset, int size)
                    throws IOException {
                if (!readAllowed.isSignalled()) {
                    try {
                        readAllowed.waitForSignal();
                    } catch (InterruptedException e) {
                        fail();
                    }
                }
                return source.readAt(position, buffer, offset, size);
            }

            @Override
            public long getSize() throws IOException {
                return source.getSize();
            }

            @Override
            public void close() throws IOException {
                source.close();
            }
        };

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onError(MediaPlayer mp, MediaItem dsd, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        mOnErrorCalled.reset();

        // prepare() will be pending until readAllowed is signaled.
        mPlayer.prepare();

        ListenableFuture<PlayerResult> seekFuture1 = mPlayer.seekTo(3000);
        ListenableFuture<PlayerResult> seekFuture2 = mPlayer.seekTo(2000);
        ListenableFuture<PlayerResult> seekFuture3 = mPlayer.seekTo(1000);

        readAllowed.signal();

        assertEquals(RESULT_CODE_SKIPPED, seekFuture1.get().getResultCode());
        assertEquals(RESULT_CODE_SKIPPED, seekFuture2.get().getResultCode());
        assertEquals(RESULT_CODE_SUCCESS, seekFuture3.get().getResultCode());
        assertFalse(mOnErrorCalled.isSignalled());
    }

    @Test
    @LargeTest
    public void testPlayerStates() throws Throwable {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        ListenableFuture<PlayerResult> future;
        assertEquals(MediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        future = mPlayer.prepare();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        future = mPlayer.play();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        future = mPlayer.pause();
        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        mPlayer.reset();
        assertEquals(MediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
    }

    @Test
    @LargeTest
    public void testPlayerCallback() throws Throwable {
        final int mp4Duration = 8484;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip;
        }

        ListenableFuture<PlayerResult> future;
        final TestUtils.Monitor onSeekCompleteCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onPlayerStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger playerState = new AtomicInteger();
        final TestUtils.Monitor onBufferingStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger bufferingState = new AtomicInteger();
        final TestUtils.Monitor onPlaybackSpeedChanged = new TestUtils.Monitor();
        final AtomicReference<Float> playbackSpeed = new AtomicReference<>();

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(SessionPlayer player, int state) {
                playerState.set(state);
                onPlayerStateChangedCalled.signal();
            }

            @Override
            public void onBufferingStateChanged(SessionPlayer player, MediaItem item,
                    int buffState) {
                bufferingState.set(buffState);
                onBufferingStateChangedCalled.signal();
            }

            @Override
            public void onPlaybackSpeedChanged(SessionPlayer player, float speed) {
                playbackSpeed.set(speed);
                onPlaybackSpeedChanged.signal();
            }

            @Override
            public void onSeekCompleted(SessionPlayer player, long position) {
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
        } while (bufferingState.get() != MediaPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED);

        assertEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, future.get().getResultCode());

        do {
            assertTrue(onPlayerStateChangedCalled.waitForSignal(1000));
        } while (playerState.get() != MediaPlayer.PLAYER_STATE_PAUSED);
        do {
            assertTrue(onBufferingStateChangedCalled.waitForSignal(1000));
        } while (bufferingState.get() != MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);

        onSeekCompleteCalled.reset();
        mPlayer.seekTo(mp4Duration >> 1);
        onSeekCompleteCalled.waitForSignal();

        onPlaybackSpeedChanged.reset();
        mPlayer.setPlaybackSpeed(0.5f);
        do {
            assertTrue(onPlaybackSpeedChanged.waitForSignal(1000));
        } while (Math.abs(playbackSpeed.get() - 0.5f) > FLOAT_TOLERANCE);

        mPlayer.reset();
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        mPlayer.unregisterPlayerCallback(callback);
    }

    @Test
    @MediumTest
    public void testClose() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);
        mPlayer.prepare();
        mPlayer.play();
        mPlayer.close();

        // Tests whether the notification from the player after the close() doesn't crash.
        Thread.sleep(SLEEP_TIME);
    }

    @Test
    @LargeTest
    public void testReset() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);

        mPlayer.reset();

        assertNull(mPlayer.getAudioAttributes());
        assertNull(mPlayer.getCurrentMediaItem());
    }

    @Test
    @LargeTest
    public void testCancelPendingCommands() throws Exception {
        final Monitor readRequested = new Monitor();
        final Monitor readAllowed = new Monitor();
        DataSourceCallback dataSource = new DataSourceCallback() {
            TestDataSourceCallback mTestSource = TestDataSourceCallback.fromAssetFd(
                    mResources.openRawResourceFd(R.raw.testmp3));
            @Override
            public int readAt(long position, byte[] buffer, int offset, int size)
                    throws IOException {
                try {
                    readRequested.signal();
                    readAllowed.waitForSignal();
                } catch (InterruptedException e) {
                    fail();
                }
                return mTestSource.readAt(position, buffer, offset, size);
            }

            @Override
            public long getSize() throws IOException {
                return mTestSource.getSize();
            }

            @Override
            public void close() throws IOException {
                mTestSource.close();
            }
        };
        MediaPlayer.PlayerCallback ecb = new MediaPlayer.PlayerCallback() {
            @Override
            public void onError(MediaPlayer mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, ecb);

        mOnErrorCalled.reset();

        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        // prepare() will be pending until readAllowed is signaled.
        mPlayer.prepare();

        ListenableFuture<PlayerResult> seekFuture = mPlayer.seekTo(1000);
        ListenableFuture<PlayerResult> volumeFuture = mPlayer.setPlayerVolume(0.7f);

        readRequested.waitForSignal();

        // Cancel the pending commands while preparation is on hold.
        seekFuture.cancel(false);
        volumeFuture.cancel(false);

        // Make the on-going prepare operation resumed and check the results.
        readAllowed.signal();
        mPlayer.setSurface(null).get();

        assertEquals(0 /* default value */, mPlayer.getCurrentPosition());
        assertEquals(1.0f /* default value */, mPlayer.getPlayerVolume(), 0.001f);

        assertEquals(0, mOnErrorCalled.getNumSignal());
    }

    @SmallTest
    public void testSetAndGetShuflleMode() throws Exception {
        final TestUtils.Monitor onShuffleModeChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onShuffleModeChanged(SessionPlayer player, int shuffleMode) {
                mPlayerCbArg1 = player;
                mPlayerCbArg2 = new Integer(shuffleMode);
                onShuffleModeChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        int shuffleMode = mPlayer.getShuffleMode();
        ListenableFuture<PlayerResult> future;
        PlayerResult result;
        if (shuffleMode != SessionPlayer.SHUFFLE_MODE_NONE) {
            onShuffleModeChangedMonitor.reset();
            future = mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_NONE);
            assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
            result = future.get();
            assertEquals(mPlayer, mPlayerCbArg1);
            assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
            assertEquals(SessionPlayer.SHUFFLE_MODE_NONE, ((Integer) mPlayerCbArg2).intValue());
            assertEquals(SessionPlayer.SHUFFLE_MODE_NONE, mPlayer.getShuffleMode());
        }

        onShuffleModeChangedMonitor.reset();
        future = mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_ALL);
        assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(SessionPlayer.SHUFFLE_MODE_ALL, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.SHUFFLE_MODE_ALL, mPlayer.getShuffleMode());

        onShuffleModeChangedMonitor.reset();
        future = mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_GROUP);
        assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, mPlayer.getShuffleMode());

        // INVALID_SHUFFLE_MODE will not change the shuffle mode.
        onShuffleModeChangedMonitor.reset();
        future = mPlayer.setShuffleMode(INVALID_SHUFFLE_MODE);
        assertFalse(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_BAD_VALUE, result.getResultCode());
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, mPlayer.getShuffleMode());
    }

    @Test
    @SmallTest
    public void testSetAndGetRepeatMode() throws Exception {
        final TestUtils.Monitor onRepeatModeChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onRepeatModeChanged(SessionPlayer player, int repeatMode) {
                mPlayerCbArg1 = player;
                mPlayerCbArg2 = new Integer(repeatMode);
                onRepeatModeChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        int repeatMode = mPlayer.getRepeatMode();
        ListenableFuture<PlayerResult> future;
        PlayerResult result;
        if (repeatMode != SessionPlayer.REPEAT_MODE_NONE) {
            onRepeatModeChangedMonitor.reset();
            future = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE);
            assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
            result = future.get();
            assertEquals(mPlayer, mPlayerCbArg1);
            assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
            assertEquals(SessionPlayer.REPEAT_MODE_NONE, ((Integer) mPlayerCbArg2).intValue());
            assertEquals(SessionPlayer.REPEAT_MODE_NONE, mPlayer.getRepeatMode());
        }

        onRepeatModeChangedMonitor.reset();
        future = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL);
        assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(SessionPlayer.REPEAT_MODE_ALL, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.REPEAT_MODE_ALL, mPlayer.getRepeatMode());

        onRepeatModeChangedMonitor.reset();
        future = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_GROUP);
        assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, mPlayer.getRepeatMode());

        // INVALID_REPEAT_MODE will not change the repeat mode.
        onRepeatModeChangedMonitor.reset();
        future = mPlayer.setRepeatMode(INVALID_REPEAT_MODE);
        assertFalse(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        result = future.get();
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(RESULT_CODE_BAD_VALUE, result.getResultCode());
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, mPlayer.getRepeatMode());
    }

    @Test
    @SmallTest
    public void testSetPlaylist() throws Exception {
        List<MediaItem> playlist = createPlaylist(10);
        try {
            mPlayer.setPlaylist(null, null);
            fail();
        } catch (Exception e) {
            // pass-through
        }
        try {
            List<MediaItem> list = new ArrayList<>();
            list.add(null);
            mPlayer.setPlaylist(list, null);
            fail();
        } catch (Exception e) {
            // pass-through
        }
        ListenableFuture<PlayerResult> future = mPlayer.setPlaylist(playlist, null);
        PlayerResult result = future.get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testSkipToPlaylistItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // Test skipToPlaylistItem
        for (int i = listSize - 1; i >= 0; --i) {
            result = mPlayer.skipToPlaylistItem(i).get();
            assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
            assertEquals(playlist.get(i), mPlayer.getCurrentMediaItem());
        }
    }

    @Test
    @SmallTest
    public void testSkipToNextItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // Test skipToNextPlaylistItem
        // curPlayPos = 0
        for (int curPlayPos = 0; curPlayPos < listSize - 1; ++curPlayPos) {
            result = mPlayer.skipToNextPlaylistItem().get();
            assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
            assertEquals(playlist.get(curPlayPos + 1), mPlayer.getCurrentMediaItem());
        }
        result = mPlayer.skipToNextPlaylistItem().get();
        assertEquals(RESULT_CODE_INVALID_STATE, result.getResultCode());
        assertEquals(playlist.get(listSize - 1), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testSkipToPreviousItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        result = mPlayer.skipToPlaylistItem(listSize - 1).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        // Test skipToPrevious
        // curPlayPos = listSize - 1
        for (int curPlayPos = listSize - 1; curPlayPos > 0; --curPlayPos) {
            result = mPlayer.skipToPreviousPlaylistItem().get();
            assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
            assertEquals(playlist.get(curPlayPos - 1), mPlayer.getCurrentMediaItem());
        }
        result = mPlayer.skipToPreviousPlaylistItem().get();
        assertEquals(RESULT_CODE_INVALID_STATE, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testSkipToNextPreviousItemsWithRepeatMode() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        result = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());

        result = mPlayer.skipToPreviousPlaylistItem().get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(listSize - 1), mPlayer.getCurrentMediaItem());

        result = mPlayer.skipToNextPlaylistItem().get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testPlaylistAfterSkipToNextItem() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        result = mPlayer.skipToNextPlaylistItem().get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());

        // Will not go to the next if the next is end of the playlist
        result = mPlayer.skipToNextPlaylistItem().get();
        assertEquals(RESULT_CODE_INVALID_STATE, result.getResultCode());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());

        result = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        result = mPlayer.skipToNextPlaylistItem().get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testPlaylistAfterSkipToPreviousItem() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);
        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        // Will not go to the previous if the current is the first one
        result = mPlayer.skipToPreviousPlaylistItem().get();
        assertEquals(RESULT_CODE_INVALID_STATE, result.getResultCode());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        result = mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        result = mPlayer.skipToPreviousPlaylistItem().get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());
    }

    @Test
    @SmallTest
    public void testCurrentMediaItemChangedCalledAfterSetMediaItem() throws Exception {
        MediaItem item = createMediaItem(100);

        final TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onCurrentMediaItemChanged(SessionPlayer player, MediaItem item) {
                onCurrentMediaItemChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        PlayerResult result = mPlayer.setMediaItem(item).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertTrue(onCurrentMediaItemChangedMonitor.waitForSignal(WAIT_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCurrentMediaItemChangedCalledAfterSetPlayList() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);

        final TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onCurrentMediaItemChanged(SessionPlayer player, MediaItem item) {
                onCurrentMediaItemChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        PlayerResult result = mPlayer.setPlaylist(playlist, null).get();
        assertEquals(RESULT_CODE_SUCCESS, result.getResultCode());
        assertTrue(onCurrentMediaItemChangedMonitor.waitForSignal(WAIT_TIME_MS));
    }

    private MediaItem createMediaItem(int key) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(R.raw.testvideo);
        return new FileMediaItem.Builder(
                afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build();
    }

    private List<MediaItem> createPlaylist(int size) throws Exception {
        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            items.add(createMediaItem(i));
        }
        return items;
    }
}
