/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.player;

import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_BAD_VALUE;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_INFO_SKIPPED;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_SUCCESS;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.CallbackMediaItem;
import androidx.media2.common.DataSourceCallback;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.PlayerResult;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.player.TestUtils.Monitor;
import androidx.media2.player.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MediaPlayerTest extends MediaPlayerTestBase {
    private static final String LOG_TAG = "MediaPlayerTest";

    private static final int SLEEP_TIME = 1000;
    private static final long WAIT_TIME_MS = 300;
    private static final long LARGE_TEST_WAIT_TIME_MS = 10000;
    private static final float FLOAT_TOLERANCE = .0001f;
    private static final int INVALID_SHUFFLE_MODE = -1000;
    private static final int INVALID_REPEAT_MODE = -1000;
    private static final String TEST_PLAYLIST_GENRE = "GENRE_TEST";

    private Object mPlayerCbArg1;
    private Object mPlayerCbArg2;

    private final List<TrackInfo> mVideoTrackInfos = new ArrayList<>();
    private final List<TrackInfo> mAudioTrackInfos = new ArrayList<>();
    private final List<TrackInfo> mSubtitleTrackInfos = new ArrayList<>();
    private TrackInfo mSelectedTrack = null;
    private final Monitor mOnSubtitleDataCalled = new Monitor();
    private final Monitor mTracksFullyFound = new Monitor();
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
    @Ignore("Test disabled due to flakiness, see b/138474897")
    public void playAudioOnce() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        Future<PlayerResult> setAttrFuture = mPlayer.setAudioAttributes(attributes);

        final TestUtils.Monitor playing = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor, new SessionPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull SessionPlayer player, int playerState) {
                playing.signal();
            }
        });

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();

        assertTrue(playing.waitForSignal(SLEEP_TIME));
        assertFutureSuccess(setAttrFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
    }

    @Test
    @LargeTest
    public void playAudio() throws Exception {
        final int resid = R.raw.testmp3_2;
        final int mp3Duration = 34909;
        final int tolerance = 100;
        final int seekDuration = 100;

        Future<PlayerResult> setItemFuture;
        try (AssetFileDescriptor afd = mResources.openRawResourceFd(resid)) {
            setItemFuture = mPlayer.setMediaItem(new FileMediaItem.Builder(
                    ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                    .setFileDescriptorOffset(afd.getStartOffset())
                    .setFileDescriptorLength(afd.getLength())
                    .build());
        }
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        Future<PlayerResult> setAttrFuture = mPlayer.setAudioAttributes(attributes);
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(setAttrFuture);
        assertFutureSuccess(prepareFuture);

        assertNotEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());
        assertFutureSuccess(mPlayer.play());
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        assertEquals(mp3Duration, mPlayer.getDuration(), tolerance);
        long pos = mPlayer.getCurrentPosition();
        assertTrue(pos >= 0);
        assertTrue(pos < mp3Duration - seekDuration);

        assertFutureSuccess(mPlayer.seekTo(pos + seekDuration, MediaPlayer.SEEK_PREVIOUS_SYNC));
        assertEquals(pos + seekDuration, mPlayer.getCurrentPosition(), tolerance);

        assertFutureSuccess(mPlayer.pause());
        assertNotEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());
        assertFutureSuccess(mPlayer.play());
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // waiting to complete
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
    }

    @Test
    @LargeTest
    public void playVideo() throws Exception {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        final int width = 352;
        final int height = 288;
        final float volume = 0.5f;

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());

        final TestUtils.Monitor onVideoSizeChangedCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onVideoRenderingStartCalled = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, MediaItem dsd, VideoSize size) {
                assertVideoSizeEquals(size);
            }

            @Override
            public void onVideoSizeChanged(@NonNull SessionPlayer player,
                    @NonNull androidx.media2.common.VideoSize size) {
                assertVideoSizeEquals(new VideoSize(size.getWidth(), size.getHeight()));
            }

            private void assertVideoSizeEquals(VideoSize size) {
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

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();

        onVideoSizeChangedCalled.waitForCountedSignals(2);
        onVideoRenderingStartCalled.waitForSignal();

        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertFutureSuccess(mPlayer.setPlayerVolume(volume));

        // waiting to complete
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }

        // Validate media metrics from API 21 where PersistableBundle was added.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
                String vmime = metrics.getString(MediaPlayer.MetricsConstants.MIME_TYPE_VIDEO,
                        null);
                String amime = metrics.getString(MediaPlayer.MetricsConstants.MIME_TYPE_AUDIO,
                        null);
                if (vmime == null && amime == null) {
                    fail("getMetrics() returned neither video nor audio mime value");
                }

                long duration = metrics.getLong(MediaPlayer.MetricsConstants.DURATION, -2);
                if (duration == -2) {
                    fail("getMetrics() didn't return a duration");
                }
                long playing = metrics.getLong(MediaPlayer.MetricsConstants.PLAYING, -2);
                if (playing == -2) {
                    fail("getMetrics() didn't return a playing time");
                }
                if (!keys.contains(MediaPlayer.MetricsConstants.PLAYING)) {
                    fail("MediaMetricsSet.keys() missing: "
                            + MediaPlayer.MetricsConstants.PLAYING);
                }
            }
        }
        MediaItem item = mPlayer.getCurrentMediaItem();
        mPlayer.reset();
        assertTrue(((FileMediaItem) item).isClosed());
    }

    @Test
    @LargeTest
    public void playVideoWithUri() throws Exception {
        if (!loadResourceWithUri(R.raw.testvideo)) {
            fail();
        }
        final int width = 352;
        final int height = 288;

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());

        final TestUtils.Monitor onVideoSizeChangedCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onVideoRenderingStartCalled = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, MediaItem dsd, VideoSize size) {
                assertVideoSizeEquals(size);
            }

            @Override
            public void onVideoSizeChanged(@NonNull SessionPlayer player,
                    @NonNull androidx.media2.common.VideoSize size) {
                assertVideoSizeEquals(new VideoSize(size.getWidth(), size.getHeight()));
            }

            private void assertVideoSizeEquals(VideoSize size) {
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

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();

        onVideoSizeChangedCalled.waitForCountedSignals(2);
        onVideoRenderingStartCalled.waitForSignal();

        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
    }

    @Test
    @MediumTest
    public void getDuration() throws Exception {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        final int expectedDuration = 11047;
        final int tolerance = 70;

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder2().getSurface());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getDuration());

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);

        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());
        assertEquals(expectedDuration, mPlayer.getDuration(), tolerance);
    }

    @Test
    @MediumTest
    public void getCurrentPosition() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getCurrentPosition());
    }

    @Test
    @MediumTest
    public void getBufferedPosition() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        assertEquals(MediaPlayer.UNKNOWN_TIME, mPlayer.getBufferedPosition());
    }

    @Test
    @MediumTest
    public void getPlaybackSpeed() throws Exception {
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
        try {
            mPlayer.getPlaybackSpeed();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @LargeTest
    @Ignore("May be flaky if emulator runs slowly")
    public void playbackRate() throws Exception {
        final int toleranceMs = 1000;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            fail();
        }

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);

        float[] rates = {0.25f, 0.5f, 1.0f, 2.0f};
        for (float playbackRate : rates) {
            Future<PlayerResult> seekFuture = mPlayer.seekTo(0, MediaPlayer.SEEK_PREVIOUS_SYNC);
            Thread.sleep(1000);
            int playTime = 4000;  // The testing clip is about 10 second long.
            int privState = mPlayer.getPlayerState();

            Future<PlayerResult> setParamsFuture = mPlayer.setPlaybackParams(
                    new PlaybackParams.Builder().setSpeed(playbackRate).build());
            assertFutureSuccess(seekFuture);
            assertFutureSuccess(setParamsFuture);
            assertEquals("setPlaybackParams() should not change player state. "
                    + mPlayer.getPlayerState(), privState, mPlayer.getPlayerState());

            Future<PlayerResult> playFuture = mPlayer.play();
            Thread.sleep(playTime);

            PlaybackParams pbp = mPlayer.getPlaybackParams();
            assertEquals(playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
            assertEquals("The player should still be playing",
                    MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

            long playedMediaDurationMs = mPlayer.getCurrentPosition();
            long expectedPosition = (long) (playTime * playbackRate);
            int diff = (int) Math.abs(playedMediaDurationMs - expectedPosition);
            if (diff > toleranceMs) {
                fail("Media player had error in playback rate " + playbackRate
                        + ". expected position after playing " + playTime
                        + " was " + expectedPosition + ", but actually " + playedMediaDurationMs);
            }
            assertFutureSuccess(playFuture);
            assertFutureSuccess(mPlayer.pause());

            pbp = mPlayer.getPlaybackParams();
            assertEquals("pause() should not change the playback rate property.",
                    playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
        }
        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void seekModes() throws Exception {
        // This clip has 2 I frames at 66687us and 4299687us.
        if (!loadResource(
                R.raw.bbb_s1_320x240_mp4_h264_mp2_800kbps_30fps_aac_lc_5ch_240kbps_44100hz)) {
            fail();
        }

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);

        final long seekPosMs = 3000;
        final long timeToleranceMs = 100;
        final long syncTime1Ms = 67;
        final long syncTime2Ms = 4300;

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to previous sync or next sync.
        long cp = runSeekMode(MediaPlayer.SEEK_CLOSEST, seekPosMs);
        assertTrue("MediaPlayer did not seek to closest position",
                cp > seekPosMs && cp < syncTime2Ms);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or next sync.
        cp = runSeekMode(MediaPlayer.SEEK_PREVIOUS_SYNC, seekPosMs);
        assertTrue("MediaPlayer did not seek to preivous sync position",
                cp < seekPosMs - timeToleranceMs);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or previous sync.
        cp = runSeekMode(MediaPlayer.SEEK_NEXT_SYNC, seekPosMs);
        assertTrue("MediaPlayer did not seek to next sync position",
                cp > syncTime2Ms - timeToleranceMs);

        // TODO: tighten checking range. For now, ensure mediaplayer doesn't
        // seek to closest position or previous sync.
        cp = runSeekMode(MediaPlayer.SEEK_CLOSEST_SYNC, seekPosMs);
        assertTrue("MediaPlayer did not seek to closest sync position",
                cp > syncTime2Ms - timeToleranceMs);

        mPlayer.reset();
    }

    private long runSeekMode(int seekMode, long seekPosMs) throws Exception {
        final int sleepIntervalMs = 100;
        int timeRemainedMs = 10000;  // total time for testing
        final int timeToleranceMs = 100;

        assertFutureSuccess(mPlayer.seekTo(seekPosMs, seekMode));

        long cp = -seekPosMs;
        while (timeRemainedMs > 0) {
            cp = mPlayer.getCurrentPosition();
            // Wait till MediaPlayer starts rendering since MediaPlayer caches
            // seek position as current position.
            if (cp < seekPosMs - timeToleranceMs || cp > seekPosMs + timeToleranceMs) {
                break;
            }
            timeRemainedMs -= sleepIntervalMs;
            Thread.sleep(sleepIntervalMs);
        }
        assertTrue("MediaPlayer did not finish seeking in time for mode " + seekMode,
                timeRemainedMs > 0);
        return cp;
    }

    @FlakyTest(bugId = 189489889)
    @Test
    @LargeTest
    public void getTimestamp() throws Exception {
        final int toleranceUs = 100000;
        final float playbackRate = 1.0f;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            fail();
        }

        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        Future<PlayerResult> setParamsFuture = mPlayer.setPlaybackParams(
                new PlaybackParams.Builder().setSpeed(playbackRate).build());
        Thread.sleep(SLEEP_TIME);  // let player get into stable state.
        long nt1 = System.nanoTime();
        MediaTimestamp ts1 = mPlayer.getTimestamp();
        long nt2 = System.nanoTime();
        assertNotNull("Media player should return a valid time stamp", ts1);
        assertEquals("MediaPlayer had error in clockRate " + ts1.getMediaClockRate(),
                playbackRate, ts1.getMediaClockRate(), FLOAT_TOLERANCE);
        assertTrue("The nanoTime of Media timestamp should be taken when getTimestamp is called.",
                nt1 <= ts1.getAnchorSystemNanoTime() && ts1.getAnchorSystemNanoTime() <= nt2);
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertFutureSuccess(setParamsFuture);

        assertFutureSuccess(mPlayer.pause());

        ts1 = mPlayer.getTimestamp();
        assertNotNull("Media player should return a valid time stamp", ts1);
        assertEquals("Media player should have play rate of 0.0f when paused",
                0.0f, ts1.getMediaClockRate(), FLOAT_TOLERANCE);

        Future<PlayerResult> seekFuture = mPlayer.seekTo(0, MediaPlayer.SEEK_PREVIOUS_SYNC);
        playFuture = mPlayer.play();
        Thread.sleep(SLEEP_TIME);  // let player get into stable state.
        int playTime = 4000;  // The testing clip is about 10 second long.
        ts1 = mPlayer.getTimestamp();
        assertNotNull("Media player should return a valid time stamp", ts1);
        Thread.sleep(playTime);
        MediaTimestamp ts2 = mPlayer.getTimestamp();
        assertNotNull("Media player should return a valid time stamp", ts2);
        assertEquals("The clockRate should not be changed.",
                ts1.getMediaClockRate(), ts2.getMediaClockRate(), FLOAT_TOLERANCE);
        assertEquals("MediaPlayer had error in timestamp.",
                ts1.getAnchorMediaTimeUs() + (long) (playTime * ts1.getMediaClockRate() * 1000),
                ts2.getAnchorMediaTimeUs(), toleranceUs);
        assertFutureSuccess(seekFuture);
        assertFutureSuccess(playFuture);

        mPlayer.reset();
    }

    private void readTracks() {
        mVideoTrackInfos.clear();
        mAudioTrackInfos.clear();
        mSubtitleTrackInfos.clear();
        List<TrackInfo> trackInfos = mPlayer.getTracks();
        if (trackInfos == null || trackInfos.size() == 0) {
            return;
        }

        for (TrackInfo track : trackInfos) {
            assertNotNull(track);
            switch (track.getTrackType()) {
                case TrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                    mVideoTrackInfos.add(track);
                    break;
                case TrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                    mAudioTrackInfos.add(track);
                    break;
                case TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                    mSubtitleTrackInfos.add(track);
                    break;
            }
        }
    }

    private int selectSubtitleTrack(int index) throws Exception {
        assertTrue(index < mSubtitleTrackInfos.size());
        final TrackInfo track = mSubtitleTrackInfos.get(index);
        Future<PlayerResult> future = mPlayer.selectTrack(track);
        int result = future.get().getResultCode();
        if (result == RESULT_SUCCESS) {
            mSelectedTrack = track;
        }
        return result;
    }

    private int deselectSubtitleTrack(int index) throws Exception {
        assertTrue(index < mSubtitleTrackInfos.size());
        final TrackInfo track = mSubtitleTrackInfos.get(index);
        Future<PlayerResult> future = mPlayer.deselectTrack(track);
        int result = future.get().getResultCode();
        if (result == RESULT_SUCCESS) {
            mSelectedTrack = null;
        }
        return result;
    }

    @Test
    @LargeTest
    public void deselectTrackForSubtitleTracks() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        mInstrumentation.waitForIdleSync();

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onSubtitleData(@NonNull SessionPlayer player, @NonNull MediaItem item,
                    @NonNull TrackInfo track, @NonNull SubtitleData data) {
                if (track != null && data != null && data.getData() != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }

            @Override
            public void onTracksChanged(@NonNull SessionPlayer player,
                    @NonNull List<TrackInfo> tracks) {
                assertNotNull(tracks);
                if (tracks.size() < 3) {
                    // This callback can be called before tracks are available after setMediaItem.
                    return;
                }
                mTracksFullyFound.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // Closed caption tracks are in-band.
        // So, those tracks will be found after processing a number of frames.
        assertTrue(mTracksFullyFound.waitForSignal(3000));

        readTracks();

        // Run twice to check if repeated selection-deselection on the same track works well.
        for (int i = 0; i < 2; i++) {
            // Waits until at least one subtitle is fired. Timeout is 2.5 seconds.
            assertEquals(RESULT_SUCCESS, selectSubtitleTrack(i));
            mOnSubtitleDataCalled.reset();
            assertTrue(mOnSubtitleDataCalled.waitForSignal(2500));

            // Try deselecting track.
            assertEquals(RESULT_SUCCESS, deselectSubtitleTrack(i));
            mOnSubtitleDataCalled.reset();
            assertFalse(mOnSubtitleDataCalled.waitForSignal(1500));
        }
        // Deselecting unselected track: expected error status
        assertNotEquals(RESULT_SUCCESS, deselectSubtitleTrack(0));
        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void changeSubtitleTrack() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onSubtitleData(@NonNull SessionPlayer player, @NonNull MediaItem item,
                    @NonNull TrackInfo track, @NonNull SubtitleData data) {
                if (track != null && data != null && data.getData() != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }

            @Override
            public void onTracksChanged(@NonNull SessionPlayer player,
                    @NonNull List<TrackInfo> tracks) {
                assertNotNull(tracks);
                if (tracks.size() < 3) {
                    // This callback can be called before tracks are available after setMediaItem.
                    return;
                }
                mTracksFullyFound.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // Closed caption tracks are in-band.
        // So, those tracks will be found after processing a number of frames.
        assertTrue(mTracksFullyFound.waitForSignal(3000));

        readTracks();
        assertEquals(mSelectedTrack, mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE));

        // Waits until at least two captions are fired. Timeout is 2.5 sec.
        assertEquals(RESULT_SUCCESS, selectSubtitleTrack(0));
        assertTrue(mOnSubtitleDataCalled.waitForCountedSignals(2, 2500) >= 2);
        assertEquals(mSelectedTrack, mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE));

        mOnSubtitleDataCalled.reset();
        assertEquals(RESULT_SUCCESS, selectSubtitleTrack(1));
        assertTrue(mOnSubtitleDataCalled.waitForCountedSignals(2, 2500) >= 2);
        assertEquals(mSelectedTrack, mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE));

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void getTracksForVideoWithSubtitleTracks() throws Throwable {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onTracksChanged(@NonNull SessionPlayer player,
                    @NonNull List<TrackInfo> tracks) {
                assertNotNull(tracks);
                if (tracks.size() < 3) {
                    // This callback can be called before tracks are available after setMediaItem.
                    return;
                }
                mTracksFullyFound.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // The media metadata will be changed while playing since closed caption tracks are in-band
        // and those tracks will be found after processing a number of frames. These tracks will be
        // found within one second.
        assertTrue(mTracksFullyFound.waitForSignal(3000));

        readTracks();
        assertEquals(2, mSubtitleTrackInfos.size());

        // Test isSelectable
        assertFalse(mVideoTrackInfos.get(0).isSelectable());
        assertTrue(mSubtitleTrackInfos.get(0).isSelectable());
        assertTrue(mSubtitleTrackInfos.get(1).isSelectable());

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void getTracksForVideoWithoutSubtitleTracks() throws Throwable {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onTracksChanged(@NonNull SessionPlayer player,
                    @NonNull List<TrackInfo> tracks) {
                assertNotNull(tracks);
                if (tracks.size() < 2) {
                    // This callback can be called before tracks are available after setMediaItem.
                    return;
                }
                mTracksFullyFound.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        Future<PlayerResult> prepareFuture = mPlayer.prepare();

        mTracksFullyFound.waitForSignal(1500);
        assertFutureSuccess(prepareFuture);

        readTracks();

        // R.raw.testvideo contains the following tracks:
        //  MEDIA_TRACK_TYPE_VIDEO: 1
        //  MEDIA_TRACK_TYPE_AUDIO: 1
        assertEquals(1, mVideoTrackInfos.size());
        assertEquals(1, mAudioTrackInfos.size());
        assertEquals(0, mSubtitleTrackInfos.size());

        // Test isSelectable
        assertFalse(mVideoTrackInfos.get(0).isSelectable());
        assertTrue(mAudioTrackInfos.get(0).isSelectable());

        // Test getSelectedTrack
        assertEquals(mVideoTrackInfos.get(0),
                mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        assertEquals(mAudioTrackInfos.get(0),
                mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        assertNull(mPlayer.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE));
        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void mediaTimeDiscontinuity() throws Exception {
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
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();

        // Timestamp needs to be reported when playback starts.
        mOnMediaTimeDiscontinuityCalled.reset();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 1.0f) > 0.01f);

        // Timestamp needs to be reported when seeking is done.
        mOnMediaTimeDiscontinuityCalled.reset();
        assertFutureSuccess(mPlayer.seekTo(3000));
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 1.0f) > 0.01f);

        // Timestamp needs to be updated when playback rate changes.
        mOnMediaTimeDiscontinuityCalled.reset();
        assertFutureSuccess(mPlayer.setPlaybackParams(
                new PlaybackParams.Builder().setSpeed(0.5f).build()));
        mOnMediaTimeDiscontinuityCalled.waitForSignal();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 0.5f) > 0.01f);

        // Timestamp needs to be updated when player is paused.
        mOnMediaTimeDiscontinuityCalled.reset();
        assertFutureSuccess(mPlayer.pause());
        mOnMediaTimeDiscontinuityCalled.waitForSignal();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 0.0f) > 0.01f);

        mPlayer.reset();
    }

    @Test
    @LargeTest
    public void playbackFromDataSourceCallback() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final int duration = 10000;

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        // Test returning -1 from getSize() to indicate unknown size.
        dataSource.returnFromGetSize(-1);
        Future<PlayerResult> setItemFuture = mPlayer.setMediaItem(
                new CallbackMediaItem.Builder(dataSource).build());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // Test pause and restart.
        assertFutureSuccess(mPlayer.pause());
        assertNotEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        assertFutureSuccess(mPlayer.play());
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // Test reset.
        mPlayer.reset();
        setItemFuture = mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());
        prepareFuture = mPlayer.prepare();
        playFuture = mPlayer.play();
        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(playFuture);
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        // Test seek. Note: the seek position is cached and returned as the
        // current position so there's no point in comparing them.
        assertFutureSuccess(mPlayer.seekTo(duration - SLEEP_TIME, MediaPlayer.SEEK_PREVIOUS_SYNC));
        while (mPlayer.getPlayerState() == MediaPlayer.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
    }

    @Test
    @LargeTest
    public void nullMedia2DataSourceIsRejected() throws Exception {
        try {
            assertNotNull(mPlayer.setMediaItem(null));
            fail();
        } catch (NullPointerException e) {
            // Expected exception
        }
    }

    @Test
    @LargeTest
    public void media2DataSourceIsClosedOnReset() throws Exception {
        TestDataSourceCallback dataSource = new TestDataSourceCallback(new byte[0]);
        assertFutureSuccess(mPlayer.setMediaItem(
                new CallbackMediaItem.Builder(dataSource).build()));
        mPlayer.reset();
        assertTrue(dataSource.isClosed());
    }

    @Test
    @LargeTest
    public void playbackFailsIfMedia2DataSourceThrows() throws Exception {
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
        // Ensure that we throw after reading enough data for preparation to complete.
        dataSource.throwFromReadAtPosition(500_000);
        Future<PlayerResult> setItemFuture = mPlayer.setMediaItem(
                new CallbackMediaItem.Builder(dataSource).build());
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(prepareFuture);

        mOnErrorCalled.reset();
        assertFutureSuccess(mPlayer.play());
        assertTrue(mOnErrorCalled.waitForSignal());
    }

    @Test
    @LargeTest
    public void preservePlaybackProperties() throws Exception {
        final int resid1 = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final long start1 = 6000;
        final long end1 = 7000;
        MediaItem dsd1;
        try (AssetFileDescriptor afd1 = mResources.openRawResourceFd(resid1)) {
            dsd1 = new FileMediaItem.Builder(
                    ParcelFileDescriptor.dup(afd1.getFileDescriptor()))
                    .setFileDescriptorOffset(afd1.getStartOffset())
                    .setFileDescriptorLength(afd1.getLength())
                    .setStartPosition(start1)
                    .setEndPosition(end1)
                    .build();
        }

        final int resid2 = R.raw.testvideo;
        final long start2 = 3000;
        final long end2 = 4000;
        MediaItem dsd2;
        try (AssetFileDescriptor afd2 = mResources.openRawResourceFd(resid2)) {
            dsd2 = new FileMediaItem.Builder(
                    ParcelFileDescriptor.dup(afd2.getFileDescriptor()))
                    .setFileDescriptorOffset(afd2.getStartOffset())
                    .setFileDescriptorLength(afd2.getLength())
                    .setStartPosition(start2)
                    .setEndPosition(end2)
                    .build();
        }

        List<MediaItem> items = new ArrayList<>();
        items.add(dsd1);
        items.add(dsd2);
        Future<PlayerResult> setListFuture = mPlayer.setPlaylist(items, null);
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());

        final Monitor onCompletionCalled = new Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onPlaybackCompleted(@NonNull SessionPlayer player) {
                onCompletionCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setListFuture);
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);

        Future<PlayerResult> setParamsFuture = mPlayer.setPlaybackParams(
                new PlaybackParams.Builder().setSpeed(2.0f).build());
        Future<PlayerResult> playFuture = mPlayer.play();
        assertFutureSuccess(setParamsFuture);
        assertFutureSuccess(playFuture);

        onCompletionCalled.waitForSignal();
        assertEquals(dsd2, mPlayer.getCurrentMediaItem());
        assertEquals(2.0f, mPlayer.getPlaybackParams().getSpeed(), 0.001f);
    }

    @Test
    @MediumTest
    public void defaultPlaybackParams() throws Exception {
        if (!loadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }
        assertFutureSuccess(mPlayer.prepare());

        PlaybackParams playbackParams = mPlayer.getPlaybackParams();
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT,
                (int) playbackParams.getAudioFallbackMode());
        assertEquals(1.0f, playbackParams.getPitch(), 0.001f);
        assertEquals(1.0f, playbackParams.getSpeed(), 0.001f);

        mPlayer.reset();
    }

    @Test
    @MediumTest
    public void skipUnnecessarySeek() throws Exception {
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
        Future<PlayerResult> setItemFuture = mPlayer.setMediaItem(
                new CallbackMediaItem.Builder(dataSource).build());

        mOnErrorCalled.reset();

        // prepare() will be pending until readAllowed is signaled.
        Future<PlayerResult> prepareFuture = mPlayer.prepare();

        Future<PlayerResult> seekFuture1 = mPlayer.seekTo(3000);
        Future<PlayerResult> seekFuture2 = mPlayer.seekTo(2000);
        Future<PlayerResult> seekFuture3 = mPlayer.seekTo(1000);

        readAllowed.signal();

        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureStateEquals(seekFuture1, RESULT_INFO_SKIPPED);
        assertFutureStateEquals(seekFuture2, RESULT_INFO_SKIPPED);
        assertFutureSuccess(seekFuture3);
        assertFalse(mOnErrorCalled.isSignalled());
    }

    @Test
    @LargeTest
    public void playerStates() throws Throwable {
        if (!loadResource(R.raw.testvideo)) {
            fail();
        }
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());

        assertEquals(MediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        assertFutureSuccess(mPlayer.play());

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PLAYING, mPlayer.getPlayerState());

        assertFutureSuccess(mPlayer.pause());

        assertEquals(MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
                mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        mPlayer.reset();
        assertEquals(MediaPlayer.BUFFERING_STATE_UNKNOWN, mPlayer.getBufferingState());
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());
    }

    @Test
    @LargeTest
    public void playerCallback() throws Throwable {
        final int mp4Duration = 8484;
        if (!loadResource(R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip;
        }

        final TestUtils.Monitor onSeekCompleteCalled = new TestUtils.Monitor();
        final TestUtils.Monitor onPlayerStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger playerState = new AtomicInteger();
        final TestUtils.Monitor onBufferingStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger bufferingState = new AtomicInteger();
        final TestUtils.Monitor onPlaybackSpeedChanged = new TestUtils.Monitor();
        final AtomicReference<Float> playbackSpeed = new AtomicReference<>();

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull SessionPlayer player, int state) {
                playerState.set(state);
                onPlayerStateChangedCalled.signal();
            }

            @Override
            public void onBufferingStateChanged(@NonNull SessionPlayer player, MediaItem item,
                    int buffState) {
                bufferingState.set(buffState);
                onBufferingStateChangedCalled.signal();
            }

            @Override
            public void onPlaybackSpeedChanged(@NonNull SessionPlayer player, float speed) {
                playbackSpeed.set(speed);
                onPlaybackSpeedChanged.signal();
            }

            @Override
            public void onSeekCompleted(@NonNull SessionPlayer player, long position) {
                onSeekCompleteCalled.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);
        Future<PlayerResult> setSurfaceFuture = mPlayer.setSurface(
                mActivity.getSurfaceHolder().getSurface());

        onPlayerStateChangedCalled.reset();
        onBufferingStateChangedCalled.reset();
        Future<PlayerResult> prepareFuture = mPlayer.prepare();
        do {
            assertTrue(onBufferingStateChangedCalled.waitForSignal(1000));
        } while (bufferingState.get() != MediaPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED);

        assertFutureSuccess(setSurfaceFuture);
        assertFutureSuccess(prepareFuture);

        do {
            assertTrue(onPlayerStateChangedCalled.waitForSignal(1000));
        } while (playerState.get() != MediaPlayer.PLAYER_STATE_PAUSED);
        do {
            assertTrue(onBufferingStateChangedCalled.waitForSignal(1000));
        } while (bufferingState.get() != MediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);

        onSeekCompleteCalled.reset();
        assertFutureSuccess(mPlayer.seekTo(mp4Duration >> 1));
        onSeekCompleteCalled.waitForSignal();

        onPlaybackSpeedChanged.reset();
        assertFutureSuccess(mPlayer.setPlaybackSpeed(0.5f));
        do {
            assertTrue(onPlaybackSpeedChanged.waitForSignal(1000));
        } while (Math.abs(playbackSpeed.get() - 0.5f) > FLOAT_TOLERANCE);

        mPlayer.reset();
        assertEquals(MediaPlayer.PLAYER_STATE_IDLE, mPlayer.getPlayerState());

        mPlayer.unregisterPlayerCallback(callback);
    }

    @Test
    @LargeTest
    public void setPlaybackSpeedWithIllegalArguments() throws Throwable {
        // Zero is not allowed.
        assertFutureStateEquals(mPlayer.setPlaybackSpeed(0.0f), RESULT_ERROR_BAD_VALUE);

        // Negative values are not allowed.
        assertFutureStateEquals(mPlayer.setPlaybackSpeed(-1.0f), RESULT_ERROR_BAD_VALUE);
    }

    @Test
    @LargeTest
    public void close() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        assertNotNull(mPlayer.setAudioAttributes(attributes));
        assertNotNull(mPlayer.prepare());
        assertNotNull(mPlayer.play());
        mPlayer.close();

        // Set the player to null so we don't try to close it again in tearDown().
        mPlayer = null;

        // Tests whether the notification from the player after the close() doesn't crash.
        Thread.sleep(SLEEP_TIME);
    }

    @Test
    @LargeTest
    public void reset() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        assertNotNull(mPlayer.setAudioAttributes(attributes));

        mPlayer.reset();

        assertNull(mPlayer.getAudioAttributes());
        assertNull(mPlayer.getCurrentMediaItem());
    }

    @Test
    @LargeTest
    public void cancelPendingCommands() throws Exception {
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

        Future<PlayerResult> setItemFuture = mPlayer.setMediaItem(
                new CallbackMediaItem.Builder(dataSource).build());

        // prepare() will be pending until readAllowed is signaled.
        Future<PlayerResult> prepareFuture = mPlayer.prepare();

        Future<PlayerResult> seekFuture = mPlayer.seekTo(1000);
        Future<PlayerResult> volumeFuture = mPlayer.setPlayerVolume(0.7f);

        readRequested.waitForSignal();

        // Cancel the pending commands while preparation is on hold.
        seekFuture.cancel(false);
        volumeFuture.cancel(false);

        // Make the on-going prepare operation resumed and check the results.
        readAllowed.signal();
        assertFutureSuccess(setItemFuture);
        assertFutureSuccess(prepareFuture);
        assertFutureSuccess(mPlayer.setSurface(null));

        assertEquals(0 /* default value */, mPlayer.getCurrentPosition());
        assertEquals(1.0f /* default value */, mPlayer.getPlayerVolume(), 0.001f);

        assertEquals(0, mOnErrorCalled.getNumSignal());
    }

    @Test
    @MediumTest
    public void setAndGetShuffleMode() throws Exception {
        final TestUtils.Monitor onShuffleModeChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onShuffleModeChanged(@NonNull SessionPlayer player, int shuffleMode) {
                mPlayerCbArg1 = player;
                mPlayerCbArg2 = new Integer(shuffleMode);
                onShuffleModeChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        int shuffleMode = mPlayer.getShuffleMode();
        if (shuffleMode != SessionPlayer.SHUFFLE_MODE_NONE) {
            onShuffleModeChangedMonitor.reset();
            assertFutureSuccess(mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_NONE));
            assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
            assertEquals(mPlayer, mPlayerCbArg1);
            assertEquals(SessionPlayer.SHUFFLE_MODE_NONE, ((Integer) mPlayerCbArg2).intValue());
            assertEquals(SessionPlayer.SHUFFLE_MODE_NONE, mPlayer.getShuffleMode());
        }

        onShuffleModeChangedMonitor.reset();
        assertFutureSuccess(mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_ALL));
        assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.SHUFFLE_MODE_ALL, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.SHUFFLE_MODE_ALL, mPlayer.getShuffleMode());

        onShuffleModeChangedMonitor.reset();
        assertFutureSuccess(mPlayer.setShuffleMode(SessionPlayer.SHUFFLE_MODE_GROUP));
        assertTrue(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, mPlayer.getShuffleMode());

        // INVALID_SHUFFLE_MODE will not change the shuffle mode.
        onShuffleModeChangedMonitor.reset();
        assertFutureStateEquals(mPlayer.setShuffleMode(INVALID_SHUFFLE_MODE),
                RESULT_ERROR_BAD_VALUE);
        assertFalse(onShuffleModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.SHUFFLE_MODE_GROUP, mPlayer.getShuffleMode());
    }

    @Test
    @MediumTest
    public void setAndGetRepeatMode() throws Exception {
        final TestUtils.Monitor onRepeatModeChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onRepeatModeChanged(@NonNull SessionPlayer player, int repeatMode) {
                mPlayerCbArg1 = player;
                mPlayerCbArg2 = new Integer(repeatMode);
                onRepeatModeChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        int repeatMode = mPlayer.getRepeatMode();
        if (repeatMode != SessionPlayer.REPEAT_MODE_NONE) {
            onRepeatModeChangedMonitor.reset();
            assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE));
            assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
            assertEquals(mPlayer, mPlayerCbArg1);
            assertEquals(SessionPlayer.REPEAT_MODE_NONE, ((Integer) mPlayerCbArg2).intValue());
            assertEquals(SessionPlayer.REPEAT_MODE_NONE, mPlayer.getRepeatMode());
        }

        onRepeatModeChangedMonitor.reset();
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL));
        assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.REPEAT_MODE_ALL, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.REPEAT_MODE_ALL, mPlayer.getRepeatMode());

        onRepeatModeChangedMonitor.reset();
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_GROUP));
        assertTrue(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, ((Integer) mPlayerCbArg2).intValue());
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, mPlayer.getRepeatMode());

        // INVALID_REPEAT_MODE will not change the repeat mode.
        onRepeatModeChangedMonitor.reset();
        assertFutureStateEquals(mPlayer.setRepeatMode(INVALID_REPEAT_MODE), RESULT_ERROR_BAD_VALUE);
        assertFalse(onRepeatModeChangedMonitor.waitForSignal(WAIT_TIME_MS));
        assertEquals(mPlayer, mPlayerCbArg1);
        assertEquals(SessionPlayer.REPEAT_MODE_GROUP, mPlayer.getRepeatMode());
    }

    @Test
    @MediumTest
    public void setPlaylist() throws Exception {
        List<MediaItem> playlist = createPlaylist(10);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        try {
            assertNotNull(mPlayer.setPlaylist(null, null));
            fail();
        } catch (Exception e) {
            // pass-through
        }
        try {
            List<MediaItem> list = new ArrayList<>();
            list.add(null);
            assertNotNull(mPlayer.setPlaylist(list, null));
            fail();
        } catch (Exception e) {
            // pass-through
        }
        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void setFileMediaItem() throws Exception {
        MediaItem closedItem = createMediaItem();
        ((FileMediaItem) closedItem).close();
        try {
            assertNotNull(mPlayer.setMediaItem(closedItem));
            fail();
        } catch (Exception e) {
            // Expected.
        }

        final List<MediaItem> closedPlaylist = createPlaylist(1);
        ((FileMediaItem) closedPlaylist.get(0)).close();
        try {
            assertNotNull(mPlayer.setPlaylist(closedPlaylist, null));
            fail();
        } catch (Exception e) {
            // Expected.
        }

        List<MediaItem> playlist = createPlaylist(2);
        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));

        try {
            assertNotNull(mPlayer.addPlaylistItem(0, closedItem));
            fail();
        } catch (Exception e) {
            // Expected.
        }

        try {
            assertNotNull(mPlayer.replacePlaylistItem(0, closedItem));
            fail();
        } catch (Exception e) {
            // Expected.
        }

        List<MediaItem> reversedList = new ArrayList<>(
                Arrays.asList(playlist.get(1), playlist.get(0)));
        assertFutureSuccess(mPlayer.setPlaylist(reversedList, null));
    }

    @Test
    @MediumTest
    public void playlistModification() throws Exception {
        final TestUtils.Monitor playlistChangeMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor, new SessionPlayer.PlayerCallback() {
            public void onPlaylistChanged(@NonNull SessionPlayer player,
                    @Nullable List<MediaItem> list, @Nullable MediaMetadata metadata) {
                playlistChangeMonitor.signal();
            }
        });

        List<MediaItem> playlist = createPlaylist(3);
        MediaItem item0 = playlist.get(0);
        MediaItem item1 = playlist.get(1);
        MediaItem item2 = playlist.get(2);
        MediaItem item3 = createMediaItem();
        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertTrue(playlistChangeMonitor.waitForSignal(WAIT_TIME_MS));
        playlistChangeMonitor.reset();
        // mPlayer's playlist will be [0 (current) 1 2]
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(item0, mPlayer.getCurrentMediaItem());
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(1, mPlayer.getNextMediaItemIndex());

        assertFutureSuccess(mPlayer.addPlaylistItem(0, item3));
        assertTrue(playlistChangeMonitor.waitForSignal(WAIT_TIME_MS));
        playlistChangeMonitor.reset();
        // mPlayer's playlist will be [3 0 (current) 1 2]
        assertEquals(playlist.size() + 1, mPlayer.getPlaylist().size());
        assertEquals(item0, mPlayer.getCurrentMediaItem());
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(2, mPlayer.getNextMediaItemIndex());

        assertFutureSuccess(mPlayer.removePlaylistItem(1));
        assertTrue(playlistChangeMonitor.waitForSignal(WAIT_TIME_MS));
        playlistChangeMonitor.reset();
        // mPlayer's playlist will be [3 1 (current) 2]
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(item1, mPlayer.getCurrentMediaItem());
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(2, mPlayer.getNextMediaItemIndex());

        assertFutureSuccess(mPlayer.movePlaylistItem(1, 0));
        assertTrue(playlistChangeMonitor.waitForSignal(WAIT_TIME_MS));
        playlistChangeMonitor.reset();
        // mPlayer's playlist will be [1 (current), 3, 2]
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(item1, mPlayer.getCurrentMediaItem());
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(1, mPlayer.getNextMediaItemIndex());

        assertFutureSuccess(mPlayer.skipToNextPlaylistItem());
        // mPlayer's playlist will be [1, 3 (current), 2]
        assertEquals(playlist.size(), mPlayer.getPlaylist().size());
        assertEquals(item3, mPlayer.getCurrentMediaItem());
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(2, mPlayer.getNextMediaItemIndex());
    }

    @Test
    @MediumTest
    public void setPlaylistAndRemoveAll_playerShouldMoveIdleState() throws Exception {
        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createMediaItem(R.raw.number1));

        final TestUtils.Monitor onPlayerStateChangedCalled = new TestUtils.Monitor();
        final AtomicInteger playerState = new AtomicInteger();

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull SessionPlayer player, int state) {
                playerState.set(state);
                onPlayerStateChangedCalled.signal();
            }
        };

        mPlayer.registerPlayerCallback(mExecutor, callback);

        assertNotNull(mPlayer.setPlaylist(playlist, null));
        assertNotNull(mPlayer.prepare());
        do {
            assertTrue(onPlayerStateChangedCalled.waitForSignal(1000));
        } while (playerState.get() != MediaPlayer.PLAYER_STATE_PAUSED);

        mPlayer.removePlaylistItem(0);
        do {
            assertTrue(onPlayerStateChangedCalled.waitForSignal(1000));
        } while (playerState.get() != MediaPlayer.PLAYER_STATE_IDLE);
    }

    @Test
    @MediumTest
    public void skipToPlaylistItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE));

        // Test skipToPlaylistItem
        for (int i = listSize - 1; i >= 0; --i) {
            assertFutureSuccess(mPlayer.skipToPlaylistItem(i));
            assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
            assertEquals(i, mPlayer.getCurrentMediaItemIndex());
            assertEquals(playlist.get(i), mPlayer.getCurrentMediaItem());
        }
    }

    @Test
    @MediumTest
    public void skipToNextItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE));

        // Test skipToNextPlaylistItem
        // curPlayPos = 0
        for (int curPlayPos = 0; curPlayPos < listSize - 1; ++curPlayPos) {
            assertFutureSuccess(mPlayer.skipToNextPlaylistItem());
            assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
            assertEquals(curPlayPos + 1, mPlayer.getCurrentMediaItemIndex());
            assertEquals(playlist.get(curPlayPos + 1), mPlayer.getCurrentMediaItem());
        }
        assertFutureStateEquals(mPlayer.skipToNextPlaylistItem(), RESULT_ERROR_INVALID_STATE);
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, false);
        assertEquals(listSize - 1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(listSize - 1), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void skipToPreviousItems() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_NONE));

        assertFutureSuccess(mPlayer.skipToPlaylistItem(listSize - 1));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);

        // Test skipToPrevious
        // curPlayPos = listSize - 1
        for (int curPlayPos = listSize - 1; curPlayPos > 0; --curPlayPos) {
            assertFutureSuccess(mPlayer.skipToPreviousPlaylistItem());
            assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
            assertEquals(curPlayPos - 1, mPlayer.getCurrentMediaItemIndex());
            assertEquals(playlist.get(curPlayPos - 1), mPlayer.getCurrentMediaItem());
        }
        assertFutureStateEquals(mPlayer.skipToPreviousPlaylistItem(), RESULT_ERROR_INVALID_STATE);
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, false);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void skipToNextPreviousItemsWithRepeatMode() throws Exception {
        int listSize = 5;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL));

        assertEquals(listSize - 1, mPlayer.getPreviousMediaItemIndex());
        assertFutureSuccess(mPlayer.skipToPreviousPlaylistItem());
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(listSize - 1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(listSize - 1), mPlayer.getCurrentMediaItem());

        assertEquals(0, mPlayer.getNextMediaItemIndex());
        assertFutureSuccess(mPlayer.skipToNextPlaylistItem());
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void playlistAfterSkipToNextItem() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));
        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        assertFutureSuccess(mPlayer.skipToNextPlaylistItem());
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());

        // Will not go to the next if the next is end of the playlist
        assertFutureStateEquals(mPlayer.skipToNextPlaylistItem(), RESULT_ERROR_INVALID_STATE);
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, false);
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());

        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL));
        assertFutureSuccess(mPlayer.skipToNextPlaylistItem());
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void playlistAfterSkipToPreviousItem() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);
        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));
        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        // Will not go to the previous if the current is the first one
        assertFutureStateEquals(mPlayer.skipToPreviousPlaylistItem(), RESULT_ERROR_INVALID_STATE);
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, false);
        assertEquals(0, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        assertFutureSuccess(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL));
        assertFutureSuccess(mPlayer.skipToPreviousPlaylistItem());
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
        assertEquals(1, mPlayer.getCurrentMediaItemIndex());
        assertEquals(playlist.get(1), mPlayer.getCurrentMediaItem());
    }

    @Test
    @MediumTest
    public void currentMediaItemChangedCalledAfterSetMediaItem() throws Exception {
        final int currentIdx = -1;
        MediaItem item1 = createMediaItem();
        MediaItem item2 = createMediaItem();

        final TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                    @NonNull MediaItem item) {
                assertEquals(currentIdx, mPlayer.getCurrentMediaItemIndex());
                onCurrentMediaItemChangedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        assertFutureSuccess(mPlayer.setMediaItem(item1));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);

        // Test if multiple calls to setMediaItem calls onCurrentMediaItemChanged.
        assertFutureSuccess(mPlayer.setMediaItem(item2));
        assertWaitForSignalAndReset(onCurrentMediaItemChangedMonitor, true);
    }

    @Test
    @MediumTest
    public void currentMediaItemChangedCalledAfterSetPlaylist() throws Exception {
        int listSize = 2;
        List<MediaItem> playlist = createPlaylist(listSize);

        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor,
                new PlayerCallbackForPlaylist(playlist, onCurrentMediaItemChangedMonitor));

        assertFutureSuccess(mPlayer.setPlaylist(playlist, null));
        assertTrue(onCurrentMediaItemChangedMonitor.waitForSignal(WAIT_TIME_MS));
    }

    @Test
    @LargeTest
    public void currentMediaItemChangedAndPlaybackCompletedWhilePlayingPlaylist()
            throws Exception {
        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createMediaItem(R.raw.number1));
        playlist.add(createMediaItem(R.raw.number2));
        playlist.add(createMediaItem(R.raw.number3));

        TestUtils.Monitor onPlaybackCompletedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor, new SessionPlayer.PlayerCallback() {
            int mCurrentMediaItemChangedCount = 0;

            @Override
            public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                    @NonNull MediaItem item) {
                assertEquals(player.getCurrentMediaItem(), item);

                int currentIdx = player.getCurrentMediaItemIndex();
                int expectedCurrentIdx = mCurrentMediaItemChangedCount++;
                assertEquals(expectedCurrentIdx, currentIdx);
                assertEquals(playlist.get(expectedCurrentIdx), item);
            }

            @Override
            public void onPlaybackCompleted(@NonNull SessionPlayer player) {
                onPlaybackCompletedMonitor.signal();
            }
        });

        assertNotNull(mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface()));
        assertNotNull(mPlayer.setPlaylist(playlist, null));
        assertNotNull(mPlayer.prepare());
        assertNotNull(mPlayer.play());

        assertTrue(onPlaybackCompletedMonitor.waitForSignal(LARGE_TEST_WAIT_TIME_MS));
    }

    @Ignore("Test disabled due to flakiness, see b/189898969")
    @Test
    @LargeTest
    public void repeatAll() throws Exception {
        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createMediaItem(R.raw.number1));
        playlist.add(createMediaItem(R.raw.number2));
        playlist.add(createMediaItem(R.raw.number3));
        int listSize = playlist.size();
        int waitForCurrentMediaChangeCount = listSize * 2;

        TestUtils.Monitor onCurrentMediaItemChangedMonitor = new TestUtils.Monitor();
        TestUtils.Monitor onPlaybackCompletedMonitor = new TestUtils.Monitor();
        PlayerCallbackForPlaylist callback = new PlayerCallbackForPlaylist(
                playlist, onCurrentMediaItemChangedMonitor) {
            int mCurrentMediaItemChangedCount = 0;

            @Override
            public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                    @NonNull MediaItem item) {
                super.onCurrentMediaItemChanged(player, item);

                int expectedCurrentIdx = (mCurrentMediaItemChangedCount++) % listSize;
                assertEquals(expectedCurrentIdx, player.getCurrentMediaItemIndex());
            }

            @Override
            public void onPlaybackCompleted(@NonNull SessionPlayer player) {
                onPlaybackCompletedMonitor.signal();
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        assertNotNull(mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface()));
        assertNotNull(mPlayer.setPlaylist(playlist, null));
        assertNotNull(mPlayer.prepare());
        assertNotNull(mPlayer.setRepeatMode(SessionPlayer.REPEAT_MODE_ALL));
        assertNotNull(mPlayer.play());

        assertEquals(waitForCurrentMediaChangeCount,
                onCurrentMediaItemChangedMonitor.waitForCountedSignals(
                        waitForCurrentMediaChangeCount, LARGE_TEST_WAIT_TIME_MS));
        assertEquals(0, onPlaybackCompletedMonitor.getNumSignal());
    }

    @Ignore("Test disabled due to flakiness, see b/144876689")
    @Test
    @LargeTest
    public void onVideoSizeChanged() throws Exception {
        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createMediaItem(R.raw.testvideo));
        playlist.add(createMediaItem(R.raw.testmp3));
        playlist.add(createMediaItem(R.raw.testvideo_with_2_subtitle_tracks));

        VideoSize sizeFor1stItem = new VideoSize(352, 288);
        VideoSize sizeFor2ndItem = new VideoSize(0, 0);
        VideoSize sizeFor3rdItem = new VideoSize(160, 90);

        CountDownLatch latchFor1stItem = new CountDownLatch(1);
        CountDownLatch latchFor2ndItem = new CountDownLatch(1);
        CountDownLatch latchFor3rdItem = new CountDownLatch(1);

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onVideoSizeChanged(@NonNull SessionPlayer player,
                    @NonNull androidx.media2.common.VideoSize size) {
                if (latchFor1stItem.getCount() > 0) {
                    assertEquals(sizeFor1stItem, size);
                    latchFor1stItem.countDown();
                } else if (latchFor2ndItem.getCount() > 0) {
                    assertEquals(sizeFor2ndItem, size);
                    latchFor2ndItem.countDown();
                } else if (latchFor3rdItem.getCount() > 0) {
                    assertEquals(sizeFor3rdItem, size);
                    latchFor3rdItem.countDown();
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        assertNotNull(mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface()));
        assertNotNull(mPlayer.setPlaylist(playlist, null));
        assertNotNull(mPlayer.prepare());
        assertNotNull(mPlayer.play());

        assertTrue(latchFor1stItem.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mPlayer.skipToNextPlaylistItem());
        assertTrue(latchFor2ndItem.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mPlayer.skipToNextPlaylistItem());
        assertTrue(latchFor3rdItem.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Ignore("Test disabled due to flakiness, see b/144972397")
    @Test
    @LargeTest
    public void seekToEndOfMediaItem() throws Exception {
        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createMediaItem(R.raw.testmp3));
        playlist.add(createMediaItem(R.raw.testmp3));
        playlist.add(createMediaItem(R.raw.testmp3));

        List<CountDownLatch> latches = new ArrayList<>();
        for (int i = 0; i < playlist.size(); i++) {
            latches.add(new CountDownLatch(1));
        }

        MediaPlayer.PlayerCallback callback = new MediaPlayer.PlayerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                    @NonNull MediaItem item) {
                for (int i = 0; i < playlist.size(); i++) {
                    if (playlist.get(i) == item) {
                        latches.get(i).countDown();
                        break;
                    }
                }
            }
        };
        mPlayer.registerPlayerCallback(mExecutor, callback);

        assertNotNull(mPlayer.setPlaylist(playlist, null));
        assertNotNull(mPlayer.prepare());
        assertFutureSuccess(mPlayer.play());

        for (int i = 0; i < playlist.size(); i++) {
            assertTrue("onCurrentMediaItemChanged is not called for item i=" + i,
                    latches.get(i).await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            if (i + 1 < playlist.size()) {
                long duration = mPlayer.getDuration();
                assertNotEquals(SessionPlayer.UNKNOWN_TIME, duration);
                assertFutureSuccess(mPlayer.seekTo(duration));
            }
        }
    }

    @Test
    @SmallTest
    public void onPlaylistChangedCalledAfterSetMediaItem() throws Exception {
        List<MediaItem> playlist = createPlaylist(3);
        MediaMetadata playlistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_GENRE, TEST_PLAYLIST_GENRE)
                .build();
        int listSize = playlist.size();

        TestUtils.Monitor onPlaylistChangedMonitor = new TestUtils.Monitor();
        mPlayer.registerPlayerCallback(mExecutor, new MediaPlayer.PlayerCallback() {
            public void onPlaylistChanged(SessionPlayer player, List<MediaItem> list,
                    MediaMetadata metadata) {
                switch (onPlaylistChangedMonitor.getNumSignal()) {
                    case 0:
                        assertEquals(listSize, list.size());
                        for (int i = 0; i < listSize; i++) {
                            assertEquals(playlist.get(i), list.get(i));
                        }
                        assertEquals(TEST_PLAYLIST_GENRE,
                                metadata.getText(MediaMetadata.METADATA_KEY_GENRE));
                        break;
                    case 1:
                        assertNull(list);
                        assertNull(metadata);
                        break;
                }
                onPlaylistChangedMonitor.signal();
            }
        });

        assertFutureSuccess(mPlayer.setPlaylist(playlist, playlistMetadata));
        assertEquals(1, onPlaylistChangedMonitor.waitForCountedSignals(1, WAIT_TIME_MS));
        List<MediaItem> playerPlaylist = mPlayer.getPlaylist();
        assertEquals(listSize, playerPlaylist.size());
        for (int i = 0; i < listSize; i++) {
            assertEquals(playlist.get(i), playerPlaylist.get(i));
        }
        assertEquals(TEST_PLAYLIST_GENRE,
                mPlayer.getPlaylistMetadata().getText(MediaMetadata.METADATA_KEY_GENRE));
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());

        assertFutureSuccess(mPlayer.setMediaItem(playlist.get(0)));
        assertEquals(2, onPlaylistChangedMonitor.waitForCountedSignals(2, WAIT_TIME_MS));
        assertNull(mPlayer.getPlaylist());
        assertNull(mPlayer.getPlaylistMetadata());
        assertEquals(playlist.get(0), mPlayer.getCurrentMediaItem());
    }

    private MediaItem createMediaItem() throws Exception {
        return createMediaItem(R.raw.testvideo);
    }

    private MediaItem createMediaItem(int resId) throws Exception {
        try (AssetFileDescriptor afd = mResources.openRawResourceFd(resId)) {
            return new FileMediaItem.Builder(
                    ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                    .setFileDescriptorOffset(afd.getStartOffset())
                    .setFileDescriptorLength(afd.getLength())
                    .build();
        }
    }

    private List<MediaItem> createPlaylist(int size) throws Exception {
        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            items.add(createMediaItem());
        }
        return items;
    }

    private class PlayerCallbackForPlaylist extends MediaPlayer.PlayerCallback {
        private List<MediaItem> mPlaylist;
        private TestUtils.Monitor mOnCurrentMediaItemChangedMonitor;

        PlayerCallbackForPlaylist(List<MediaItem> playlist, TestUtils.Monitor monitor) {
            mPlaylist = playlist;
            mOnCurrentMediaItemChangedMonitor = monitor;
        }

        @Override
        public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                @NonNull MediaItem item) {
            int currentIdx = mPlaylist.indexOf(item);
            assertEquals(currentIdx, mPlayer.getCurrentMediaItemIndex());
            mOnCurrentMediaItemChangedMonitor.signal();
        }
    }

    private void assertWaitForSignalAndReset(TestUtils.Monitor monitor, boolean assertTrue)
            throws Exception {
        if (assertTrue) {
            assertTrue(monitor.waitForSignal(WAIT_TIME_MS));
        } else {
            assertFalse(monitor.waitForSignal(WAIT_TIME_MS));
        }
        monitor.reset();
    }
}
