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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media2.TestUtils.Monitor;
import androidx.media2.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

@RunWith(AndroidJUnit4.class)
public class MediaPlayer2Test extends MediaPlayer2TestBase {

    private static final String LOG_TAG = "MediaPlayer2Test";

    private static final int  RECORDED_VIDEO_WIDTH  = 176;
    private static final int  RECORDED_VIDEO_HEIGHT = 144;
    private static final long RECORDED_DURATION_MS  = 3000;
    private static final float FLOAT_TOLERANCE = .0001f;
    private static final long PLAYBACK_COMPLETE_TOLERANCE_MS = 100;

    private String mRecordedFilePath;
    private final Vector<Integer> mSubtitleTrackIndex = new Vector<>();
    private final Monitor mOnSubtitleDataCalled = new Monitor();
    private int mSelectedSubtitleIndex;

    private File mOutFile;
    private Camera mCamera;

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
        mRecordedFilePath = new File(Environment.getExternalStorageDirectory(),
                "mediaplayer_record.out").getAbsolutePath();
        mOutFile = new File(mRecordedFilePath);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (mOutFile != null && mOutFile.exists()) {
            mOutFile.delete();
        }
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayNullSourcePath() throws Exception {
        final Monitor onSetDataSourceCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE) {
                    assertTrue(status != MediaPlayer2.CALL_STATUS_NO_ERROR);
                    onSetDataSourceCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        onSetDataSourceCalled.reset();
        mPlayer.setMediaItem((MediaItem) null);
        onSetDataSourceCalled.waitForSignal();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayAudioFromDataURI() throws Exception {
        final int mp3Duration = 34909;
        final int tolerance = 70;
        final int seekDuration = 100;

        // This is "R.raw.testmp3_2", base64-encoded.
        final int resid = R.raw.testmp3_3;

        InputStream is = mContext.getResources().openRawResource(resid);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        StringBuilder builder = new StringBuilder();
        builder.append("data:;base64,");
        builder.append(reader.readLine());
        Uri uri = Uri.parse(builder.toString());

        MediaPlayer2 mp = createMediaPlayer2(mContext, uri);

        final Monitor onPrepareCalled = new Monitor();
        final Monitor onPlayCalled = new Monitor();
        final Monitor onSeekToCalled = new Monitor();
        final Monitor onLoopCurrentCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    onPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    onPlayCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT) {
                    onLoopCurrentCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    onSeekToCalled.signal();
                }
            }
        };
        mp.setEventCallback(mExecutor, ecb);

        try {
            AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            mp.setAudioAttributes(attributes);

            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            /* FIXME: what's API for checking loop state?
            assertFalse(mp.isLooping());
            */
            onLoopCurrentCalled.reset();
            mp.loopCurrent(true);
            onLoopCurrentCalled.waitForSignal();
            /* FIXME: what's API for checking loop state?
            assertTrue(mp.isLooping());
            */

            assertEquals(mp3Duration, mp.getDuration(), tolerance);
            long pos = mp.getCurrentPosition();
            assertTrue(pos >= 0);
            assertTrue(pos < mp3Duration - seekDuration);

            onSeekToCalled.reset();
            mp.seekTo(pos + seekDuration, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            onSeekToCalled.waitForSignal();
            assertEquals(pos + seekDuration, mp.getCurrentPosition(), tolerance);

            // test pause and restart
            mp.pause();
            Thread.sleep(SLEEP_TIME);
            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            // test stop and restart
            mp.reset();
            mp.setEventCallback(mExecutor, ecb);
            mp.setMediaItem(new UriMediaItem.Builder(mContext, uri).build());
            onPrepareCalled.reset();
            mp.prepare();
            onPrepareCalled.waitForSignal();

            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            // waiting to complete
            while (mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
                Thread.sleep(SLEEP_TIME);
            }
        } finally {
            mp.close();
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayAudio() throws Exception {
        final int resid = R.raw.testmp3_2;
        final int mp3Duration = 34909;
        final int tolerance = 70;
        final int seekDuration = 100;

        MediaPlayer2 mp = createMediaPlayer2(mContext, resid);
        AssetFileDescriptor afd = null;

        final Monitor onPrepareCalled = new Monitor();
        final Monitor onPlayCalled = new Monitor();
        final Monitor onSeekToCalled = new Monitor();
        final Monitor onLoopCurrentCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    onPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    onPlayCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT) {
                    onLoopCurrentCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    onSeekToCalled.signal();
                }
            }
        };
        mp.setEventCallback(mExecutor, ecb);

        try {
            AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            mp.setAudioAttributes(attributes);

            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            //assertFalse(mp.isLooping());
            onLoopCurrentCalled.reset();
            mp.loopCurrent(true);
            onLoopCurrentCalled.waitForSignal();
            //assertTrue(mp.isLooping());

            assertEquals(mp3Duration, mp.getDuration(), tolerance);
            long pos = mp.getCurrentPosition();
            assertTrue(pos >= 0);
            assertTrue(pos < mp3Duration - seekDuration);

            onSeekToCalled.reset();
            mp.seekTo(pos + seekDuration, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            onSeekToCalled.waitForSignal();
            assertEquals(pos + seekDuration, mp.getCurrentPosition(), tolerance);

            // test pause and restart
            mp.pause();
            Thread.sleep(SLEEP_TIME);
            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            // test stop and restart
            mp.reset();
            afd = mResources.openRawResourceFd(resid);
            mp.setMediaItem(new FileMediaItem.Builder(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength())
                    .build());

            mp.setEventCallback(mExecutor, ecb);
            onPrepareCalled.reset();
            mp.prepare();
            onPrepareCalled.waitForSignal();
            afd.close();

            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            // waiting to complete
            while (mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
                Thread.sleep(SLEEP_TIME);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            mp.close();
            afd.close();
        }
    }

    /*
    public void testConcurentPlayAudio() throws Exception {
        final int resid = R.raw.test1m1s; // MP3 longer than 1m are usualy offloaded
        final int tolerance = 70;

        List<MediaPlayer2> mps = Stream.generate(() -> createMediaPlayer2(mContext, resid))
                                      .limit(5).collect(Collectors.toList());

        try {
            for (MediaPlayer2 mp : mps) {
                Monitor onPlayCalled = new Monitor();
                Monitor onLoopCurrentCalled = new Monitor();
                MediaPlayer2.EventCallback ecb =
                    new MediaPlayer2.EventCallback() {
                        @Override
                        public void onCallCompleted(MediaPlayer2 mp, MediaItem item,
                                int what, int status) {
                            if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                                onPlayCalled.signal();
                            } else if (what == MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT) {
                                onLoopCurrentCalled.signal();
                            }
                        }
                    };
                mp.setEventCallback(mExecutor, ecb);

                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setInternalLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build();
                mp.setAudioAttributes(attributes);

                assertFalse(mp.isPlaying());
                onPlayCalled.reset();
                mp.play();
                onPlayCalled.waitForSignal();
                assertTrue(mp.isPlaying());

                assertFalse(mp.isLooping());
                onLoopCurrentCalled.reset();
                mp.loopCurrent(true);
                onLoopCurrentCalled.waitForSignal();
                assertTrue(mp.isLooping());

                long pos = mp.getCurrentPosition();
                assertTrue(pos >= 0);

                Thread.sleep(SLEEP_TIME); // Delay each track to be able to ear them
            }
            // Check that all mp3 are playing concurrently here
            for (MediaPlayer2 mp : mps) {
                long pos = mp.getCurrentPosition();
                Thread.sleep(SLEEP_TIME);
                assertEquals(pos + SLEEP_TIME, mp.getCurrentPosition(), tolerance);
            }
        } finally {
            mps.forEach(MediaPlayer2::close);
        }
    }
    */

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayAudioLooping() throws Exception {
        final int resid = R.raw.testmp3;

        MediaPlayer2 mp = createMediaPlayer2(mContext, resid);
        try {
            AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            mp.setAudioAttributes(attributes);
            mp.loopCurrent(true);
            final Monitor onCompletionCalled = new Monitor();
            final Monitor onDataSourceRepeatCalled = new Monitor();
            final Monitor onPlayCalled = new Monitor();
            MediaPlayer2.EventCallback ecb =
                    new MediaPlayer2.EventCallback() {
                        @Override
                        public void onInfo(MediaPlayer2 mp, MediaItem item,
                                int what, int extra) {
                            if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                                Log.i("@@@", "got oncompletion");
                                onCompletionCalled.signal();
                            } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_REPEAT) {
                                onDataSourceRepeatCalled.signal();
                            }
                        }

                        @Override
                        public void onCallCompleted(MediaPlayer2 mp, MediaItem item,
                                int what, int status) {
                            if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                                onPlayCalled.signal();
                            }
                        }
                    };
            mp.setEventCallback(mExecutor, ecb);

            assertFalse(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            onPlayCalled.reset();
            mp.play();
            onPlayCalled.waitForSignal();
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            onDataSourceRepeatCalled.waitForCountedSignals(3); // allow for several loops
            assertTrue(mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            onCompletionCalled.reset();
            mp.loopCurrent(false);

            // wait for playback to finish
            while (mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
                Thread.sleep(SLEEP_TIME);
            }
            assertEquals("wrong number of completion signals", 1,
                    onCompletionCalled.getNumSignal());
        } finally {
            mp.close();
        }
    }

    // The pre-Pie implementation of MediaPlayer2 does not support MIDI playback.
    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testPlayMidi() throws Exception {
        final int resid = R.raw.midi8sec;
        final int midiDuration = 8000;
        final int tolerance = 70;
        final int seekDuration = 1000;

        MediaPlayer2 mp = createMediaPlayer2(mContext, resid);
        AssetFileDescriptor afd = null;

        final Monitor onPrepareCalled = new Monitor();
        final Monitor onSeekToCalled = new Monitor();
        final Monitor onLoopCurrentCalled = new Monitor();
        MediaPlayer2.EventCallback ecb =
                new MediaPlayer2.EventCallback() {
                    @Override
                    public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                        if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                            onPrepareCalled.signal();
                        }
                    }

                    @Override
                    public void onCallCompleted(MediaPlayer2 mp, MediaItem item,
                            int what, int status) {
                        if (what == MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT) {
                            onLoopCurrentCalled.signal();
                        } else if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                            onSeekToCalled.signal();
                        }
                    }
                };
        mp.setEventCallback(mExecutor, ecb);

        try {
            AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            mp.setAudioAttributes(attributes);

            mp.play();

            /* FIXME: what's API for checking loop state?
            assertFalse(mp.isLooping());
            */
            onLoopCurrentCalled.reset();
            mp.loopCurrent(true);
            onLoopCurrentCalled.waitForSignal();
            /* FIXME: what's API for checking loop state?
            assertTrue(mp.isLooping());
            */

            assertEquals(midiDuration, mp.getDuration(), tolerance);
            long pos = mp.getCurrentPosition();
            assertTrue(pos >= 0);
            assertTrue(pos < midiDuration - seekDuration);

            onSeekToCalled.reset();
            mp.seekTo(pos + seekDuration, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            onSeekToCalled.waitForSignal();
            assertEquals(pos + seekDuration, mp.getCurrentPosition(), tolerance);

            // test stop and restart
            mp.reset();
            afd = mResources.openRawResourceFd(resid);
            mp.setMediaItem(new FileMediaItem.Builder(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build());

            mp.setEventCallback(mExecutor, ecb);
            onPrepareCalled.reset();
            mp.prepare();
            onPrepareCalled.waitForSignal();

            mp.play();

            Thread.sleep(SLEEP_TIME);
        } finally {
            mp.close();
            afd.close();
        }
    }

    static class OutputListener {
        int mSession;
        AudioEffect mVc;
        Visualizer mVis;
        byte [] mVisData;
        boolean mSoundDetected;
        OutputListener(int session) {
            mSession = session;
            /* FIXME: find out a public API for replacing AudioEffect contructor.
            // creating a volume controller on output mix ensures that ro.audio.silent mutes
            // audio after the effects and not before
            mVc = new AudioEffect(
                    AudioEffect.EFFECT_TYPE_NULL,
                    UUID.fromString("119341a0-8469-11df-81f9-0002a5d5c51b"),
                    0,
                    session);
            mVc.setEnabled(true);
            */
            mVis = new Visualizer(session);
            int size = 256;
            int[] range = Visualizer.getCaptureSizeRange();
            if (size < range[0]) {
                size = range[0];
            }
            if (size > range[1]) {
                size = range[1];
            }
            assertTrue(mVis.setCaptureSize(size) == Visualizer.SUCCESS);

            mVis.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer,
                        byte[] waveform, int samplingRate) {
                    if (!mSoundDetected) {
                        for (int i = 0; i < waveform.length; i++) {
                            // 8 bit unsigned PCM, zero level is at 128, which is -128 when
                            // seen as a signed byte
                            if (waveform[i] != -128) {
                                mSoundDetected = true;
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                }
            }, 10000 /* milliHertz */, true /* PCM */, false /* FFT */);
            assertTrue(mVis.setEnabled(true) == Visualizer.SUCCESS);
        }

        void reset() {
            mSoundDetected = false;
        }

        boolean heardSound() {
            return mSoundDetected;
        }

        void release() {
            mVis.release();
            /* FIXME: find out a public API for replacing AudioEffect contructor.
            mVc.release();
            */
        }
    }

    public void testPlayAudioTwice() throws Exception {

        final int resid = R.raw.camera_click;

        MediaPlayer2 mp = createMediaPlayer2(mContext, resid);
        try {
            AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            mp.setAudioAttributes(attributes);

            OutputListener listener = new OutputListener(mp.getAudioSessionId());

            Thread.sleep(SLEEP_TIME);
            assertFalse("noise heard before test started", listener.heardSound());

            mp.play();
            Thread.sleep(SLEEP_TIME);
            assertFalse("player was still playing after " + SLEEP_TIME + " ms",
                    mp.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
            assertTrue("nothing heard while test ran", listener.heardSound());
            listener.reset();
            mp.seekTo(0, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            mp.play();
            Thread.sleep(SLEEP_TIME);
            assertTrue("nothing heard when sound was replayed", listener.heardSound());
            listener.release();
        } finally {
            mp.close();
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayVideo() throws Exception {
        playVideoTest(R.raw.testvideo, 352, 288);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetDuration() throws Exception {
        if (!checkLoadResource(R.raw.testvideo)) {
            return;
        }
        final int expectedDuration = 11047;
        final int tolerance = 70;

        final Monitor prepareCompleted = new Monitor();
        MediaPlayer2.EventCallback callback = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PREPARE) {
                    prepareCompleted.signal();
                }
                super.onCallCompleted(mp, item, what, status);
            }
        };
        mPlayer.setSurface(mActivity.getSurfaceHolder2().getSurface());
        mPlayer.setEventCallback(mExecutor, callback);
        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
        try {
            assertTrue(mPlayer.getDuration() <= 0);
        } catch (IllegalStateException e) {
            // may throw exception
        }

        mPlayer.prepare();
        assertTrue(prepareCompleted.waitForSignal());
        assertEquals(MediaPlayer2.PLAYER_STATE_PREPARED, mPlayer.getState());
        assertEquals(expectedDuration, mPlayer.getDuration(), tolerance);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetCurrentPosition() throws Exception {
        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
        try {
            assertTrue(mPlayer.getCurrentPosition() <= 0);
        } catch (IllegalStateException e) {
            // OK to thrown an exception while in the IDLE
        }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetBufferedPosition() throws Exception {
        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
        try {
            assertTrue(mPlayer.getBufferedPosition() <= 0);
        } catch (IllegalStateException e) {
            // OK to thrown an exception while in the IDLE
        }
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetPlayerParams() throws Exception {
        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
        assertNotNull(mPlayer.getPlaybackParams());
    }

    /**
     * Test for resetting a surface during video playback
     * After resetting, the video should continue playing
     * from the time setDisplay() was called
     */
    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testVideoSurfaceResetting() throws Exception {
        final int tolerance = 150;
        final int audioLatencyTolerance = 1000;  /* covers audio path latency variability */
        final int seekPos = 1840;  // This is the I-frame position

        final CountDownLatch seekDone = new CountDownLatch(1);

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    seekDone.countDown();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        if (!checkLoadResource(
                R.raw.video_480x360_mp4_h264_500kbps_25fps_aac_stereo_128kbps_44100hz)) {
            return; // skip;
        }
        playLoadedVideo(480, 360, -1);

        Thread.sleep(SLEEP_TIME);

        long posBefore = mPlayer.getCurrentPosition();
        mPlayer.setSurface(mActivity.getSurfaceHolder2().getSurface());
        long posAfter = mPlayer.getCurrentPosition();

        /* temporarily disable timestamp checking because MediaPlayer2 now seeks to I-frame
         * position, instead of requested position. setDisplay invovles a seek operation
         * internally.
         */
        // TODO: uncomment out line below when MediaPlayer2 can seek to requested position.
        // assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        Thread.sleep(SLEEP_TIME);

        mPlayer.seekTo(seekPos, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        seekDone.await();
        posAfter = mPlayer.getCurrentPosition();
        assertEquals(seekPos, posAfter, tolerance + audioLatencyTolerance);

        Thread.sleep(SLEEP_TIME / 2);
        posBefore = mPlayer.getCurrentPosition();
        mPlayer.setSurface(null);
        posAfter = mPlayer.getCurrentPosition();
        // TODO: uncomment out line below when MediaPlayer2 can seek to requested position.
        // assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        Thread.sleep(SLEEP_TIME);

        posBefore = mPlayer.getCurrentPosition();
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        posAfter = mPlayer.getCurrentPosition();

        // TODO: uncomment out line below when MediaPlayer2 can seek to requested position.
        // assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        Thread.sleep(SLEEP_TIME);
    }

    @Test
    @LargeTest
    @Ignore("Fails to connect to camera service")
    public void testRecordedVideoPlayback0() throws Exception {
        testRecordedVideoPlaybackWithAngle(0);
    }

    @Test
    @LargeTest
    @Ignore("Fails to connect to camera service")
    public void testRecordedVideoPlayback90() throws Exception {
        testRecordedVideoPlaybackWithAngle(90);
    }

    @Test
    @LargeTest
    @Ignore("Fails to connect to camera service")
    public void testRecordedVideoPlayback180() throws Exception {
        testRecordedVideoPlaybackWithAngle(180);
    }

    @Test
    @LargeTest
    @Ignore("Fails to connect to camera service")
    public void testRecordedVideoPlayback270() throws Exception {
        testRecordedVideoPlaybackWithAngle(270);
    }

    private boolean hasCamera() {
        return mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void testRecordedVideoPlaybackWithAngle(int angle) throws Exception {
        int width = RECORDED_VIDEO_WIDTH;
        int height = RECORDED_VIDEO_HEIGHT;
        final String file = mRecordedFilePath;
        final long durationMs = RECORDED_DURATION_MS;

        if (!hasCamera()) {
            return;
        }

        boolean isSupported = false;
        mCamera = Camera.open(0);
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> videoSizes = parameters.getSupportedVideoSizes();
        // getSupportedVideoSizes returns null when separate video/preview size
        // is not supported.
        if (videoSizes == null) {
            videoSizes = parameters.getSupportedPreviewSizes();
        }
        for (Camera.Size size : videoSizes) {
            if (size.width == width && size.height == height) {
                isSupported = true;
                break;
            }
        }
        mCamera.release();
        mCamera = null;
        if (!isSupported) {
            width = videoSizes.get(0).width;
            height = videoSizes.get(0).height;
        }
        checkOrientation(angle);
        recordVideo(width, height, angle, file, durationMs);
        checkDisplayedVideoSize(width, height, angle, file);
        checkVideoRotationAngle(angle, file);
    }

    private void checkOrientation(int angle) throws Exception {
        assertTrue(angle >= 0);
        assertTrue(angle < 360);
        assertTrue((angle % 90) == 0);
    }

    private void recordVideo(
            int w, int h, int angle, String file, long durationMs) throws Exception {

        MediaRecorder recorder = new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(file);
        recorder.setOrientationHint(angle);
        recorder.setVideoSize(w, h);
        recorder.setPreviewDisplay(mActivity.getSurfaceHolder2().getSurface());
        recorder.prepare();
        recorder.start();
        Thread.sleep(durationMs);
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private void checkDisplayedVideoSize(
            int w, int h, int angle, String file) throws Exception {

        int displayWidth  = w;
        int displayHeight = h;
        if ((angle % 180) != 0) {
            displayWidth  = h;
            displayHeight = w;
        }
        playVideoTest(file, displayWidth, displayHeight);
    }

    private void checkVideoRotationAngle(int angle, String file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file);
        String rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        retriever.release();
        retriever = null;
        assertNotNull(rotation);
        assertEquals(Integer.parseInt(rotation), angle);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSkipToNext() throws Exception {
        testSetNextDataSources(true, true);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSetNextDataSourcesWithVideos() throws Exception {
        testSetNextDataSources(true, false);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSetNextDataSourcesWithAudios() throws Exception {
        testSetNextDataSources(false, false);
    }

    private void testSetNextDataSources(boolean video, boolean skip) throws Exception {
        int res1 = video ? R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz
                : R.raw.loudsoftmp3;
        int res2 = video ? R.raw.testvideo : R.raw.testmp3;
        if (!checkLoadResource(res1)) {
            return; // skip
        }
        final MediaItem item1 = createDataSourceDesc(res1);
        final MediaItem item2 = createDataSourceDesc(res2);
        ArrayList<MediaItem> nextDSDs = new ArrayList<MediaItem>(2);
        nextDSDs.add(item2);
        nextDSDs.add(item1);

        mPlayer.setNextMediaItems(nextDSDs);

        final Monitor onCompletion1Called = new Monitor();
        final Monitor onCompletion2Called = new Monitor();
        final Monitor onPlaylistEndCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    Log.i(LOG_TAG, "testSetNextDataSources: prepared item MediaId="
                            + item.getMediaId());
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    if (item == item1) {
                        onCompletion1Called.signal();
                    } else if (item == item2) {
                        onCompletion2Called.signal();
                    } else {
                        mOnCompletionCalled.signal();
                    }
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_LIST_END) {
                    onPlaylistEndCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnCompletionCalled.reset();
        onCompletion1Called.reset();
        onCompletion2Called.reset();
        onPlaylistEndCalled.reset();

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mPlayer.prepare();

        mPlayer.play();

        if (skip) {
            mPlayer.skipToNext();
            mPlayer.skipToNext();
        } else {
            mOnCompletionCalled.waitForSignal();
            onCompletion2Called.waitForSignal();
        }
        onCompletion1Called.waitForSignal();
        if (skip) {
            assertFalse("first item completed", mOnCompletionCalled.isSignalled());
            assertFalse("second item completed", onCompletion2Called.isSignalled());
        }
        onPlaylistEndCalled.waitForSignal();

        mPlayer.reset();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSetNextDataSource() throws Exception {
        final MediaItem item1 = createDataSourceDesc(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz);
        final MediaItem item2 = createDataSourceDesc(
                R.raw.testvideo);

        final Monitor onPlaybackCompletedCalled = new Monitor();
        final List<MediaItem> playedDSDs = new ArrayList<>();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE
                        || what == MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCES) {
                    if (status != MediaPlayer2.CALL_STATUS_NO_ERROR) {
                        fail("Unexpected status code: " + status);
                    }
                }
            }

            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    playedDSDs.add(item);
                    onPlaybackCompletedCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        onPlaybackCompletedCalled.reset();
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.setMediaItem(item1);
        mPlayer.setNextMediaItem(item1);
        mPlayer.setNextMediaItems(Arrays.asList(item2, item1));
        mPlayer.prepare();
        mPlayer.play();
        onPlaybackCompletedCalled.waitForCountedSignals(3);
        assertEquals(3, playedDSDs.size());
        assertEquals(item1, playedDSDs.get(0));
        assertEquals(item2, playedDSDs.get(1));
        assertEquals(item1, playedDSDs.get(2));

        mPlayer.reset();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSetNextDataSourceBeforeSetDataSource() throws Exception {
        final MediaItem item1 = createDataSourceDesc(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz);
        final MediaItem item2 = createDataSourceDesc(
                R.raw.testvideo);

        final Monitor onCallCompletedCalled = new Monitor();
        final List<MediaItem> playedDSDs = new ArrayList<>();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE
                        || what == MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCES) {
                    if (status == MediaPlayer2.CALL_STATUS_INVALID_OPERATION) {
                        // expected
                        onCallCompletedCalled.signal();
                    } else {
                        fail("Unexpected status code: " + status);
                    }
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        onCallCompletedCalled.reset();
        mPlayer.setNextMediaItem(item1);
        assertTrue(onCallCompletedCalled.waitForSignal());

        onCallCompletedCalled.reset();
        mPlayer.setNextMediaItems(Arrays.asList(item2, item1));
        assertTrue(onCallCompletedCalled.waitForSignal());

        mPlayer.reset();
    }

    // setPlaybackParams() with non-zero speed should NOT start playback.
    // TODO: enable this test when MediaPlayer2.setPlaybackParams() is fixed
    /*
    public void testSetPlaybackParamsPositiveSpeed() throws Exception {
        if (!checkLoadResource(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip
        }

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    mOnSeekCompleteCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnCompletionCalled.reset();
        mPlayer.setDisplay(mActivity.getSurfaceHolder());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnSeekCompleteCalled.reset();
        mPlayer.seekTo(0, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mOnSeekCompleteCalled.waitForSignal();

        final float playbackRate = 1.0f;

        int playTime = 2000;  // The testing clip is about 10 second long.
        mPlayer.setPlaybackParams(new PlaybackParams().setSpeed(playbackRate));
        assertTrue("MediaPlayer2 should be playing", mPlayer.isPlaying());
        Thread.sleep(playTime);
        assertTrue("MediaPlayer2 should still be playing",
                mPlayer.getCurrentPosition() > 0);

        long duration = mPlayer.getDuration();
        mOnSeekCompleteCalled.reset();
        mPlayer.seekTo(duration - 1000, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mOnSeekCompleteCalled.waitForSignal();

        mOnCompletionCalled.waitForSignal();
        assertFalse("MediaPlayer2 should not be playing", mPlayer.isPlaying());
        long eosPosition = mPlayer.getCurrentPosition();

        mPlayer.setPlaybackParams(new PlaybackParams().setSpeed(playbackRate));
        assertTrue("MediaPlayer2 should be playing after EOS", mPlayer.isPlaying());
        Thread.sleep(playTime);
        long position = mPlayer.getCurrentPosition();
        assertTrue("MediaPlayer2 should still be playing after EOS",
                position > 0 && position < eosPosition);

        mPlayer.reset();
    }
    */

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlaybackRate() throws Exception {
        final int toleranceMs = 1000;
        if (!checkLoadResource(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip
        }

        final Monitor labelReached = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCommandLabelReached(MediaPlayer2 mp, @NonNull Object label) {
                labelReached.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        float[] rates = { 0.25f, 0.5f, 1.0f, 2.0f };
        for (float playbackRate : rates) {
            mPlayer.seekTo(0, MediaPlayer2.SEEK_PREVIOUS_SYNC);
            Thread.sleep(1000);
            int playTime = 4000;  // The testing clip is about 10 second long.
            int privState = mPlayer.getState();
            mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(playbackRate).build());
            labelReached.reset();
            mPlayer.notifyWhenCommandLabelReached(new Object());
            labelReached.waitForSignal();
            assertTrue("setPlaybackParams() should not change player state. " + mPlayer.getState(),
                    mPlayer.getState() == privState);

            mPlayer.play();
            Thread.sleep(playTime);

            labelReached.reset();
            mPlayer.notifyWhenCommandLabelReached(new Object());
            labelReached.waitForSignal();

            PlaybackParams pbp = mPlayer.getPlaybackParams();
            assertEquals(playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
            assertTrue("MediaPlayer2 should still be playing",
                    mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

            long playedMediaDurationMs = mPlayer.getCurrentPosition();
            int diff = Math.abs((int) (playedMediaDurationMs / playbackRate) - playTime);
            if (diff > toleranceMs) {
                fail("Media player had error in playback rate " + playbackRate
                        + ", play time is " + playTime + " vs expected " + playedMediaDurationMs);
            }
            mPlayer.pause();

            labelReached.reset();
            mPlayer.notifyWhenCommandLabelReached(new Object());
            labelReached.waitForSignal();

            pbp = mPlayer.getPlaybackParams();
            assertEquals("pause() should not change the playback rate property.",
                    playbackRate, pbp.getSpeed(), FLOAT_TOLERANCE);
        }
        mPlayer.reset();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testSeekModes() throws Exception {
        // This clip has 2 I frames at 66687us and 4299687us.
        if (!checkLoadResource(
                R.raw.bbb_s1_320x240_mp4_h264_mp2_800kbps_30fps_aac_lc_5ch_240kbps_44100hz)) {
            return; // skip
        }

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    mOnSeekCompleteCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnSeekCompleteCalled.reset();
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

        mPlayer.seekTo(seekPosMs, seekMode);
        mOnSeekCompleteCalled.waitForSignal();
        mOnSeekCompleteCalled.reset();
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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetTimestamp() throws Exception {
        final int toleranceUs = 100000;
        final float playbackRate = 1.0f;
        if (!checkLoadResource(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz)) {
            return; // skip
        }

        final Monitor onPauseCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PAUSE) {
                    onPauseCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

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

        onPauseCalled.reset();
        mPlayer.pause();
        onPauseCalled.waitForSignal();
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

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MKV_H265_1280x720_500kbps_25fps_AAC_Stereo_128kbps_44100Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_1280x720_mkv_h265_500kbps_25fps_aac_stereo_128kbps_44100hz, 1280, 720);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_500kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_500kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_500kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_500kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1000kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1000kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1350kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1350kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1350kbps_30fps_AAC_Stereo_128kbps_44110Hz_frag()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz_fragmented,
                480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_MP4_H264_480x360_1350kbps_30fps_AAC_Stereo_192kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz, 480, 360);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    @Test
    @LargeTest
    @SdkSuppress(
            minSdkVersion = Build.VERSION_CODES.KITKAT, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_22050hz, 176, 144);
    }

    private void readSubtitleTracks() throws Exception {
        mSubtitleTrackIndex.clear();
        List<MediaPlayer2.TrackInfo> trackInfos = mPlayer.getTrackInfo();
        if (trackInfos == null || trackInfos.size() == 0) {
            return;
        }

        Vector<Integer> subtitleTrackIndex = new Vector<>();
        for (int i = 0; i < trackInfos.size(); ++i) {
            assertNotNull(trackInfos.get(i));
            if (trackInfos.get(i).getTrackType()
                    == MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                subtitleTrackIndex.add(i);
            }
        }

        mSubtitleTrackIndex.addAll(subtitleTrackIndex);
    }

    private void selectSubtitleTrack(int index) throws Exception {
        int trackIndex = mSubtitleTrackIndex.get(index);
        mPlayer.selectTrack(trackIndex);
        mSelectedSubtitleIndex = index;
    }

    private void deselectSubtitleTrack(int index) throws Exception {
        int trackIndex = mSubtitleTrackIndex.get(index);
        mOnDeselectTrackCalled.reset();
        mPlayer.deselectTrack(trackIndex);
        mOnDeselectTrackCalled.waitForSignal();
        if (mSelectedSubtitleIndex == index) {
            mSelectedSubtitleIndex = -1;
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testDeselectTrackForSubtitleTracks() throws Throwable {
        if (!checkLoadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            return; // skip;
        }

        mInstrumentation.waitForIdleSync();

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    mOnSeekCompleteCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK) {
                    mCallStatus = status;
                    mOnDeselectTrackCalled.signal();
                }
            }

            @Override
            public void onSubtitleData(
                    MediaPlayer2 mp, MediaItem item, SubtitleData data) {
                if (data != null && data.getData() != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());

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
            deselectSubtitleTrack(i);
            mOnSubtitleDataCalled.reset();
            assertFalse(mOnSubtitleDataCalled.waitForSignal(1500));
        }

        // Deselecting unselected track: expected error status
        mCallStatus = MediaPlayer2.CALL_STATUS_NO_ERROR;
        deselectSubtitleTrack(0);
        assertNotEquals(MediaPlayer2.CALL_STATUS_NO_ERROR, mCallStatus);

        mPlayer.reset();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testChangeSubtitleTrack() throws Throwable {
        if (!checkLoadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            return; // skip;
        }

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }

            @Override
            public void onSubtitleData(
                    MediaPlayer2 mp, MediaItem item, SubtitleData data) {
                if (data != null) {
                    mOnSubtitleDataCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());

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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testGetTrackInfoForVideoWithSubtitleTracks() throws Throwable {
        if (!checkLoadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            return; // skip;
        }

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_METADATA_UPDATE) {
                    mOnInfoCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());

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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testMediaTimeDiscontinuity() throws Exception {
        if (!checkLoadResource(
                R.raw.bbb_s1_320x240_mp4_h264_mp2_800kbps_30fps_aac_lc_5ch_240kbps_44100hz)) {
            return; // skip
        }

        final BlockingDeque<MediaTimestamp> timestamps = new LinkedBlockingDeque<>();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    mOnSeekCompleteCalled.signal();
                }
            }
            @Override
            public void onMediaTimeDiscontinuity(
                    MediaPlayer2 mp, MediaItem item, MediaTimestamp timestamp) {
                timestamps.add(timestamp);
                mOnMediaTimeDiscontinuityCalled.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());
        mPlayer.prepare();

        // Timestamp needs to be reported when playback starts.
        mOnMediaTimeDiscontinuityCalled.reset();
        mPlayer.play();
        do {
            assertTrue(mOnMediaTimeDiscontinuityCalled.waitForSignal(1000));
        } while (Math.abs(timestamps.getLast().getMediaClockRate() - 1.0f) > 0.01f);

        // Timestamp needs to be reported when seeking is done.
        mOnSeekCompleteCalled.reset();
        mOnMediaTimeDiscontinuityCalled.reset();
        mPlayer.seekTo(3000, MediaPlayer2.SEEK_NEXT_SYNC);
        mOnSeekCompleteCalled.waitForSignal();
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

    /*
     *  This test assumes the resources being tested are between 8 and 14 seconds long
     *  The ones being used here are 10 seconds long.
     */
    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testResumeAtEnd() throws Throwable {
        int testsRun = testResumeAtEnd(R.raw.loudsoftmp3)
                + testResumeAtEnd(R.raw.loudsoftwav)
                + testResumeAtEnd(R.raw.loudsoftogg)
                + testResumeAtEnd(R.raw.loudsoftitunes)
                + testResumeAtEnd(R.raw.loudsoftfaac)
                + testResumeAtEnd(R.raw.loudsoftaac);
    }

    // returns 1 if test was run, 0 otherwise
    private int testResumeAtEnd(int res) throws Throwable {
        if (!loadResource(res)) {
            Log.i(LOG_TAG, "testResumeAtEnd: No decoder found for "
                    + mContext.getResources().getResourceEntryName(res) + " --- skipping.");
            return 0; // skip
        }
        mOnCompletionCalled.reset();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                    mPlayer.play();
                }
            }
        };
        mPlayer.setEventCallback(mExecutor, ecb);

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        // skip the first part of the file so we reach EOF sooner
        mPlayer.seekTo(5000, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mPlayer.play();
        // sleep long enough that we restart playback at least once, but no more
        Thread.sleep(10000);
        assertEquals("MediaPlayer2 should still be playing",
                 MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());
        mPlayer.reset();
        assertEquals("wrong number of repetitions", 1, mOnCompletionCalled.getNumSignal());
        return 1;
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPositionAtEnd() throws Throwable {
        testPositionAtEnd(R.raw.test1m1shighstereo);
        testPositionAtEnd(R.raw.loudsoftmp3);
        testPositionAtEnd(R.raw.loudsoftwav);
        testPositionAtEnd(R.raw.loudsoftogg);
        testPositionAtEnd(R.raw.loudsoftitunes);
        testPositionAtEnd(R.raw.loudsoftfaac);
        testPositionAtEnd(R.raw.loudsoftaac);
    }

    private int testPositionAtEnd(int res) throws Throwable {
        if (!loadResource(res)) {
            Log.i(LOG_TAG, "testPositionAtEnd: No decoder found for "
                    + mContext.getResources().getResourceEntryName(res) + " --- skipping.");
            return 0; // skip
        }
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);

        mOnCompletionCalled.reset();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }
        };
        mPlayer.setEventCallback(mExecutor, ecb);

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        long duration = mPlayer.getDuration();
        assertTrue("resource too short", duration > 6000);
        mPlayer.seekTo(duration - 5000, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        while (mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
            Log.i("@@@@", "position: " + mPlayer.getCurrentPosition());
            Thread.sleep(100);
        }
        Log.i("@@@@", "final position: " + mPlayer.getCurrentPosition());
        long pos = mPlayer.getCurrentPosition();
        assertTrue("current pos (" + pos + "us) does not match the duration (" + duration + "us).",
                Math.abs(pos - duration) < 1000);
        mPlayer.reset();
        return 1;
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testMediaPlayer2Callback() throws Throwable {
        final int mp4Duration = 8484;

        if (!checkLoadResource(R.raw.testvideo)) {
            return; // skip;
        }

        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        mOnCompletionCalled.reset();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onVideoSizeChanged(MediaPlayer2 mp, MediaItem item,
                    int width, int height) {
                mOnVideoSizeChangedCalled.signal();
            }

            @Override
            public void onError(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }

            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnInfoCalled.signal();

                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    mOnSeekCompleteCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        assertFalse(mOnPrepareCalled.isSignalled());
        assertFalse(mOnVideoSizeChangedCalled.isSignalled());
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();
        mOnVideoSizeChangedCalled.waitForSignal();

        mOnSeekCompleteCalled.reset();
        mPlayer.seekTo(mp4Duration >> 1, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        mOnSeekCompleteCalled.waitForSignal();

        assertFalse(mOnCompletionCalled.isSignalled());
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        while (mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
        assertFalse(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);
        mOnCompletionCalled.waitForSignal();
        assertFalse(mOnErrorCalled.isSignalled());
        mPlayer.reset();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlayerStates() throws Throwable {
        final int mp4Duration = 8484;

        if (!checkLoadResource(R.raw.testvideo)) {
            return; // skip;
        }
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        final Monitor prepareCompleted = new Monitor();
        final Monitor playCompleted = new Monitor();
        final Monitor pauseCompleted = new Monitor();

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PREPARE) {
                    prepareCompleted.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    playCompleted.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PAUSE) {
                    pauseCompleted.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
        prepareCompleted.reset();
        mPlayer.prepare();
        prepareCompleted.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PREPARED, mPlayer.getState());

        playCompleted.reset();
        mPlayer.play();
        playCompleted.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());

        pauseCompleted.reset();
        mPlayer.pause();
        pauseCompleted.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PAUSED, mPlayer.getState());

        mPlayer.reset();
        assertEquals(MediaPlayer2.PLAYER_STATE_IDLE, mPlayer.getState());
    }

    @Test
    @LargeTest
    @Ignore("MediaRecorder.setAudioSource fails")
    public void testRecordAndPlay() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        /* FIXME: check the codec exists.
        if (!MediaUtils.checkDecoder(MediaFormat.MIMETYPE_AUDIO_AMR_NB)
                || !MediaUtils.checkEncoder(MediaFormat.MIMETYPE_AUDIO_AMR_NB)) {
            return; // skip
        }
        */
        File outputFile = new File(Environment.getExternalStorageDirectory(),
                "record_and_play.3gp");
        String outputFileLocation = outputFile.getAbsolutePath();
        try {
            recordMedia(outputFileLocation);

            Uri uri = Uri.parse(outputFileLocation);
            MediaPlayer2 mp = MediaPlayer2.create(mActivity);
            try {
                mp.setMediaItem(new UriMediaItem.Builder(mContext, uri).build());
                mp.prepare();
                Thread.sleep(SLEEP_TIME);
                playAndStop(mp);
            } finally {
                mp.close();
            }

            try {
                mp = createMediaPlayer2(mContext, uri);
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.close();
                }
            }

            try {
                mp = createMediaPlayer2(mContext, uri, mActivity.getSurfaceHolder());
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.close();
                }
            }
        } finally {
            outputFile.delete();
        }
    }

    private void playAndStop(MediaPlayer2 mp) throws Exception {
        mp.play();
        Thread.sleep(SLEEP_TIME);
        mp.reset();
    }

    private void recordMedia(String outputFile) throws Exception {
        MediaRecorder mr = new MediaRecorder();
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mr.setOutputFile(outputFile);

            mr.prepare();
            mr.start();
            Thread.sleep(SLEEP_TIME);
            mr.stop();
        } finally {
            mr.release();
        }
    }

    private boolean hasMicrophone() {
        return mActivity.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }

    // Smoke test playback from a DataSourceCallback.
    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlaybackFromAMedia2DataSource() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final int duration = 10000;

        /* FIXME: check the codec exists.
        if (!MediaUtils.hasCodecsForResource(mContext, resid)) {
            return;
        }
        */

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        // Test returning -1 from getSize() to indicate unknown size.
        dataSource.returnFromGetSize(-1);
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());
        playLoadedVideo(null, null, -1);
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        // Test pause and restart.
        mPlayer.pause();
        Thread.sleep(SLEEP_TIME);
        assertFalse(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }
        };
        mPlayer.setEventCallback(mExecutor, ecb);

        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        // Test reset.
        mPlayer.reset();
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        mPlayer.setEventCallback(mExecutor, ecb);

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertTrue(mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING);

        // Test seek. Note: the seek position is cached and returned as the
        // current position so there's no point in comparing them.
        mPlayer.seekTo(duration - SLEEP_TIME, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        while (mPlayer.getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
            Thread.sleep(SLEEP_TIME);
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testNullMedia2DataSourceIsRejected() throws Exception {
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE) {
                    mCallStatus = status;
                    mOnPlayCalled.signal();
                }
            }
        };
        mPlayer.setEventCallback(mExecutor, ecb);

        mCallStatus = MediaPlayer2.CALL_STATUS_NO_ERROR;
        mPlayer.setMediaItem((MediaItem) null);
        mOnPlayCalled.waitForSignal();
        assertTrue(mCallStatus != MediaPlayer2.CALL_STATUS_NO_ERROR);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testMedia2DataSourceIsClosedOnReset() throws Exception {
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE) {
                    mCallStatus = status;
                    mOnPlayCalled.signal();
                }
            }
        };
        mPlayer.setEventCallback(mExecutor, ecb);

        TestDataSourceCallback dataSource = new TestDataSourceCallback(new byte[0]);
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());
        mOnPlayCalled.waitForSignal();
        mPlayer.reset();
        assertTrue(dataSource.isClosed());
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlaybackFailsIfMedia2DataSourceThrows() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        /* FIXME: check the codec exists.
        if (!MediaUtils.hasCodecsForResource(mContext, resid)) {
            return;
        }
        */

        setOnErrorListener();
        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        dataSource.throwFromReadAt();
        mPlayer.play();
        assertTrue(mOnErrorCalled.waitForSignal());
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPlaybackFailsIfMedia2DataSourceReturnsAnError() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        /* FIXME: check the codec exists.
        if (!MediaUtils.hasCodecsForResource(mContext, resid)) {
            return;
        }
        */

        TestDataSourceCallback dataSource =
                TestDataSourceCallback.fromAssetFd(mResources.openRawResourceFd(resid));
        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        setOnErrorListener();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        dataSource.returnFromReadAt(-2);
        mPlayer.play();
        assertTrue(mOnErrorCalled.waitForSignal());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testClearPendingCommands() throws Exception {
        final Monitor readRequested = new Monitor();
        final Monitor readAllowed = new Monitor();
        DataSourceCallback dataSource = new DataSourceCallback() {
            @Override
            public int readAt(long position, byte[] buffer, int offset, int size)
                    throws IOException {
                try {
                    readRequested.signal();
                    readAllowed.waitForSignal();
                } catch (InterruptedException e) {
                    fail();
                }
                return -1;
            }

            @Override
            public long getSize() throws IOException {
                return -1;  // Unknown size
            }

            @Override
            public void close() throws IOException {}
        };
        final ArrayDeque<Integer> commandsCompleted = new ArrayDeque<>();
        setOnErrorListener();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                commandsCompleted.add(what);
            }

            @Override
            public void onError(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mOnErrorCalled.reset();

        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        // prepare() will be pending until readAllowed is signaled.
        mPlayer.prepare();

        mPlayer.play();
        mPlayer.pause();
        mPlayer.play();
        mPlayer.pause();
        mPlayer.play();
        mPlayer.seekTo(1000);

        // Clear the pending commands once the prepare operation starts.
        readRequested.waitForSignal();
        mPlayer.clearPendingCommands();

        // Make the on-going prepare operation fail and check the results.
        readAllowed.signal();
        mOnErrorCalled.waitForSignal();
        assertEquals(0, mOnPrepareCalled.getNumSignal());
        assertEquals(2, commandsCompleted.size());
        assertEquals(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE,
                (int) commandsCompleted.peekFirst());
        assertEquals(MediaPlayer2.CALL_COMPLETED_PREPARE,
                (int) commandsCompleted.getLast());
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testDataSourceStartEnd() throws Exception {
        final int resid1 = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final long start1 = 6000;
        final long end1 = 8000;
        AssetFileDescriptor afd1 = mResources.openRawResourceFd(resid1);
        MediaItem item1 = new FileMediaItem.Builder(
                afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength())
                .setStartPosition(start1)
                .setEndPosition(end1)
                .build();

        final int resid2 = R.raw.testvideo;
        final long start2 = 3000;
        final long end2 = 5000;
        final int expectedDuration2 = 11047;
        AssetFileDescriptor afd2 = mResources.openRawResourceFd(resid2);
        MediaItem item2 = new FileMediaItem.Builder(
                afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength())
                .setStartPosition(start2)
                .setEndPosition(end2)
                .build();

        mPlayer.setMediaItem(item1);
        mPlayer.setNextMediaItem(item2);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        final Monitor seekDone = new Monitor();
        final int[] seekResults = new int[1];
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    assertTrue(status == MediaPlayer2.CALL_STATUS_NO_ERROR);
                    mOnPlayCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                    seekResults[0] = status;
                    seekDone.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mOnCompletionCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertTrue(mPlayer.getCurrentPosition() >= start1);

        mOnCompletionCalled.waitForSignal();
        assertTrue(mPlayer.getCurrentPosition() >= start2);
        mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(0.5f).build());

        mOnCompletionCalled.reset();
        mOnCompletionCalled.waitForSignal();
        assertTrue(Math.abs(mPlayer.getCurrentPosition() - end2) < PLAYBACK_COMPLETE_TOLERANCE_MS);

        seekDone.reset();
        mPlayer.seekTo(start2 - 1000);
        seekDone.waitForSignal();
        assertEquals(MediaPlayer2.CALL_STATUS_BAD_VALUE, seekResults[0]);

        mPlayer.seekTo(end2 + 1000);
        seekDone.waitForSignal();
        assertEquals(MediaPlayer2.CALL_STATUS_BAD_VALUE, seekResults[0]);

        assertEquals(expectedDuration2, mPlayer.getDuration());

        afd1.close();
        afd2.close();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testDataSourceStartEndWithLooping() throws Exception {
        final int resid = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final long start = 6000;
        final long end = 8000;
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        MediaItem item = new FileMediaItem.Builder(
                afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength())
                .setStartPosition(start)
                .setEndPosition(end)
                .build();

        mPlayer.setMediaItem(item);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        final Monitor onDataSourceRepeatCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_REPEAT) {
                    onDataSourceRepeatCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    assertTrue(status == MediaPlayer2.CALL_STATUS_NO_ERROR);
                    mOnPlayCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mPlayer.loopCurrent(true);
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        onDataSourceRepeatCalled.reset();
        mPlayer.play();
        mOnPlayCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());
        assertTrue(mPlayer.getCurrentPosition() >= start);

        onDataSourceRepeatCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());
        assertTrue(mPlayer.getCurrentPosition() >= start);
        onDataSourceRepeatCalled.waitForCountedSignals(2);
        assertEquals(MediaPlayer2.PLAYER_STATE_PLAYING, mPlayer.getState());
        assertTrue(mPlayer.getCurrentPosition() >= start);

        mOnCompletionCalled.reset();
        mPlayer.loopCurrent(false);
        mOnCompletionCalled.waitForSignal();
        assertEquals(MediaPlayer2.PLAYER_STATE_PAUSED, mPlayer.getState());
        long pos = mPlayer.getCurrentPosition();
        assertTrue("current pos (" + pos + "us) does not match requested pos (" + end + "us).",
                Math.abs(pos - end) < PLAYBACK_COMPLETE_TOLERANCE_MS);

        afd.close();
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testPreservePlaybackProperties() throws Exception {
        final int resid1 = R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz;
        final long start1 = 6000;
        final long end1 = 7000;
        AssetFileDescriptor afd1 = mResources.openRawResourceFd(resid1);
        MediaItem item1 = new FileMediaItem.Builder(
                afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength())
                .setStartPosition(start1)
                .setEndPosition(end1)
                .build();

        final int resid2 = R.raw.testvideo;
        final long start2 = 3000;
        final long end2 = 4000;
        AssetFileDescriptor afd2 = mResources.openRawResourceFd(resid2);
        MediaItem item2 = new FileMediaItem.Builder(
                afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength())
                .setStartPosition(start2)
                .setEndPosition(end2)
                .build();

        mPlayer.setMediaItem(item1);
        mPlayer.setNextMediaItem(item2);
        mPlayer.setSurface(mActivity.getSurfaceHolder().getSurface());

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    mOnCompletionCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    assertTrue(status == MediaPlayer2.CALL_STATUS_NO_ERROR);
                    mOnPlayCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        mOnPlayCalled.reset();
        mOnCompletionCalled.reset();
        mPlayer.setPlaybackParams(new PlaybackParams.Builder().setSpeed(2.0f).build());
        mPlayer.play();

        mOnPlayCalled.waitForSignal();
        mOnCompletionCalled.waitForSignal();

        assertEquals(item2, mPlayer.getCurrentMediaItem());
        assertEquals(2.0f, mPlayer.getPlaybackParams().getSpeed(), 0.001f);

        afd1.close();
        afd2.close();
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testDefaultPlaybackParams() throws Exception {
        if (!checkLoadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            return; // skip;
        }

        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }
        mOnPrepareCalled.reset();
        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal();

        PlaybackParams playbackParams = mPlayer.getPlaybackParams();
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT,
                (int) playbackParams.getAudioFallbackMode());
        assertEquals(1.0f, playbackParams.getPitch(), 0.001f);
        assertEquals(1.0f, playbackParams.getSpeed(), 0.001f);

        mPlayer.reset();
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
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
        final Monitor labelReached = new Monitor();
        final ArrayList<Pair<Integer, Integer>> commandsCompleted = new ArrayList<>();
        setOnErrorListener();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                commandsCompleted.add(new Pair<>(what, status));
            }

            @Override
            public void onError(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }

            @Override
            public void onCommandLabelReached(MediaPlayer2 mp, @NonNull Object label) {
                labelReached.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mOnErrorCalled.reset();

        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        // prepare() will be pending until readAllowed is signaled.
        mPlayer.prepare();

        mPlayer.seekTo(3000);
        mPlayer.seekTo(2000);
        mPlayer.seekTo(1000);
        mPlayer.notifyWhenCommandLabelReached(new Object());

        readAllowed.signal();
        labelReached.waitForSignal();

        assertFalse(mOnErrorCalled.isSignalled());
        assertTrue(mOnPrepareCalled.isSignalled());
        assertEquals(5, commandsCompleted.size());
        assertEquals(
                new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE,
                        MediaPlayer2.CALL_STATUS_NO_ERROR),
                commandsCompleted.get(0));
        assertEquals(
                new Pair<>(MediaPlayer2.CALL_COMPLETED_PREPARE,
                        MediaPlayer2.CALL_STATUS_NO_ERROR),
                commandsCompleted.get(1));
        assertEquals(
                new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO,
                        MediaPlayer2.CALL_STATUS_SKIPPED),
                commandsCompleted.get(2));
        assertEquals(
                new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO,
                        MediaPlayer2.CALL_STATUS_SKIPPED),
                commandsCompleted.get(3));
        assertEquals(
                new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO,
                        MediaPlayer2.CALL_STATUS_NO_ERROR),
                commandsCompleted.get(4));
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
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
        final ArrayList<Integer> commandsCompleted = new ArrayList<>();
        setOnErrorListener();
        final Monitor labelReached = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_PREPARED) {
                    mOnPrepareCalled.signal();
                }
            }

            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                commandsCompleted.add(what);
            }

            @Override
            public void onError(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }

            @Override
            public void onCommandLabelReached(MediaPlayer2 mp, Object label) {
                labelReached.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        mOnPrepareCalled.reset();
        mOnErrorCalled.reset();

        mPlayer.setMediaItem(new CallbackMediaItem.Builder(dataSource).build());

        // prepare() will be pending until readAllowed is signaled.
        mPlayer.prepare();

        Object playToken = mPlayer.play();
        Object seekToken = mPlayer.seekTo(1000);
        mPlayer.pause();

        readRequested.waitForSignal();

        // Cancel the pending commands while preparation is on hold.
        mPlayer.cancel(playToken);
        mPlayer.cancel(seekToken);

        // Make the on-going prepare operation fail and check the results.
        readAllowed.signal();
        mPlayer.notifyWhenCommandLabelReached(new Object());
        labelReached.waitForSignal();

        assertEquals(3, commandsCompleted.size());
        assertEquals(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE, (int) commandsCompleted.get(0));
        assertEquals(MediaPlayer2.CALL_COMPLETED_PREPARE, (int) commandsCompleted.get(1));
        assertEquals(MediaPlayer2.CALL_COMPLETED_PAUSE, (int) commandsCompleted.get(2));
        assertEquals(0, mOnErrorCalled.getNumSignal());
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testClose() throws Exception {
        assertTrue(loadResource(R.raw.testmp3_2));
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build();
        mPlayer.setAudioAttributes(attributes);
        mPlayer.prepare();
        mPlayer.play();
        mPlayer.close();
        mExecutor.shutdown();

        // Tests whether the notification from the player after the close() doesn't crash.
        Thread.sleep(SLEEP_TIME);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
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
}
