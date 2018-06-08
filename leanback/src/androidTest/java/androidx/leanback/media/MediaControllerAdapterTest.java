/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.media;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.leanback.widget.PlaybackControlsRow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link MediaControllerAdapter}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaControllerAdapterTest {

    // SESSION_TAG
    private static final String SESSION_TAG = "test-session";
    private final Object mWaitLock = new Object();
    private MediaSessionCompat mSession;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaSessionCallback mCallback = new MediaSessionCallback();
    private MediaControllerCompat mControllerCompat;
    private MediaControllerAdapter mMediaControllerAdapter;
    private PlayerAdapterCallback mPlayerAdapterCallback;

    // Instrumented context for testing purpose
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession = new MediaSessionCompat(getContext(), SESSION_TAG);
                mSession.setCallback(mCallback, mHandler);
                mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);
                mControllerCompat = new MediaControllerCompat(mContext, mSession);
                mMediaControllerAdapter = new MediaControllerAdapter(mControllerCompat);
                mPlayerAdapterCallback = new PlayerAdapterCallback();
                mMediaControllerAdapter.setCallback(mPlayerAdapterCallback);
            }
        });
    }

    /**
     * Check if STATE_STOPPED is associated with onPlayComplete() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStateStopped() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_STOPPED));
            assertTrue(mPlayerAdapterCallback.mOnPlayCompletedCalled);
        }
    }

    /**
     * Check if STATE_PAUSED is associated with onPlaybackStateChanged() and
     * onCurrentPositionChanged() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStatePaused() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_PAUSED));
            assertTrue(mPlayerAdapterCallback.mOnPlayStateChangedCalled);
            assertTrue(mPlayerAdapterCallback.mOnCurrentPositionChangedCalled);
        }
    }

    /**
     * Check if STATE_PLAYING is associated with onPlaybackStateChanged() and
     * onCurrentPositionChanged() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStatePlaying() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_PLAYING));
            assertTrue(mPlayerAdapterCallback.mOnPlayStateChangedCalled);
            assertTrue(mPlayerAdapterCallback.mOnCurrentPositionChangedCalled);
        }
    }

    /**
     * Check if STATE_BUFFERING is associated with onBufferingStateChanged() and
     * onBufferedPositionChanged() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStateBuffering() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_BUFFERING));
            assertTrue(mPlayerAdapterCallback.mOnBufferingStateChangedCalled);
            assertTrue(mPlayerAdapterCallback.mOnBufferedPositionChangedCalled);
        }
    }

    /**
     * Check if STATE_ERROR is associated with onError() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStateError() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_ERROR));
            assertTrue(mPlayerAdapterCallback.mOnErrorCalled);
        }
    }

    /**
     * Check if STATE_FAST_FORWARDING is associated with onPlaybackStateChanged() and
     * onCurrentPositionChanged() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStateFastForwarding() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_FAST_FORWARDING));
            assertTrue(mPlayerAdapterCallback.mOnPlayStateChangedCalled);
            assertTrue(mPlayerAdapterCallback.mOnCurrentPositionChangedCalled);
        }
    }

    /**
     * Check if STATE_REWIND is associated with onPlaybackStateChanged() and
     * onCurrentPositionChanged() callback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testStateRewinding() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onPlaybackStateChanged(
                    createPlaybackStateForTesting(PlaybackStateCompat.STATE_REWINDING));
            assertTrue(mPlayerAdapterCallback.mOnPlayStateChangedCalled);
            assertTrue(mPlayerAdapterCallback.mOnCurrentPositionChangedCalled);
        }
    }

    /**
     * Check onMetadataChanged() function in PlayerAdapterCallback.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testOnMetadataChanged() {
        synchronized (mWaitLock) {
            mPlayerAdapterCallback.reset();
            mMediaControllerAdapter.mMediaControllerCallback.onMetadataChanged(
                    new MediaMetadataCompat.Builder().build());
            assertTrue(mPlayerAdapterCallback.mOnMetadataChangedCalled);
        }
    }

    /**
     * Check adapter's play operation.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testPlay() throws InterruptedException {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.play();
            mWaitLock.wait();
            assertTrue(mCallback.mOnPlayCalled);
        }
    }

    /**
     * Check adapter's pause operation.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testPause() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.pause();
            mWaitLock.wait();
            assertTrue(mCallback.mOnPauseCalled);
        }
    }

    /**
     * Check adapter's seekTo operation.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testSeekTo() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            long seekPosition = 445L;
            mMediaControllerAdapter.seekTo(seekPosition);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSeekToCalled);
            assertEquals(seekPosition, mCallback.mSeekPosition);
        }
    }

    /**
     * Check adapter's next operation.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testNext() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.next();
            mWaitLock.wait();
            assertTrue(mCallback.mOnSkipToNextCalled);
        }
    }

    /**
     * Check adapter's previous operation.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testPrevious() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.previous();
            mWaitLock.wait();
            assertTrue(mCallback.mOnSkipToPreviousCalled);
        }
    }

    /**
     * Check adapter's setRepeatAction operation.
     * In this test case, the repeat mode is set to REPEAT_MODE_NONE.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testRepeatModeRepeatModeNone() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSetRepeatModeCalled);
            assertEquals(mCallback.mRepeatMode, PlaybackStateCompat.REPEAT_MODE_NONE);
        }
    }

    /**
     * Check adapter's setRepeatAction operation.
     * In this test case, the repeat mode is set to REPEAT_MODE_ONE.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testRepeatModeRepeatModeOne() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_ONE);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSetRepeatModeCalled);
            assertEquals(mCallback.mRepeatMode, PlaybackStateCompat.REPEAT_MODE_ONE);
        }
    }

    /**
     * Check adapter's setRepeatAction operation.
     * In this test case, the repeat mode is set to REPEAT_MODE_ALL.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testRepeatModeRepeatModeAll() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_ALL);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSetRepeatModeCalled);
            assertEquals(mCallback.mRepeatMode, PlaybackStateCompat.REPEAT_MODE_ALL);
        }
    }

    /**
     * Check adapter's setShuffleAction operation.
     * In this test case, the shuffle mode is set to SHUFFLE_MODE_NONE.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testShuffleModeShuffleModeNone() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.setShuffleAction(PlaybackControlsRow.ShuffleAction.INDEX_OFF);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSetShuffleModeCalled);
            assertEquals(mCallback.mShuffleMode, PlaybackStateCompat.SHUFFLE_MODE_NONE);
        }
    }

    /**
     * Check adapter's setShuffleAction operation.
     * In this test case, the shuffle mode is set to SHUFFLE_MODE_ALL.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testShuffleModeShuffleModeAll() throws InterruptedException {
        synchronized (mWaitLock) {
            mCallback.reset();
            mMediaControllerAdapter.setShuffleAction(PlaybackControlsRow.ShuffleAction.INDEX_ON);
            mWaitLock.wait();
            assertTrue(mCallback.mOnSetShuffleModeCalled);
            assertEquals(mCallback.mShuffleMode, PlaybackStateCompat.SHUFFLE_MODE_ALL);
        }
    }

    /**
     * Check adapter's isPlaying operation when the playState is null.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testIsPlayingWithNullPlaybackState() {
        boolean defualtIsPlayingStatus = false;
        assertEquals(mMediaControllerAdapter.isPlaying(), defualtIsPlayingStatus);
    }

    /**
     * Check adapter's isPlaying operation when the playback state is not null and the
     * media is playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testIsPlayingWithPlayingState() {
        long positionForTest = 0L;
        boolean playingStatus = true;
        createPlaybackStatePlaying(positionForTest);
        assertEquals(mMediaControllerAdapter.isPlaying(), playingStatus);
    }

    /**
     * Check adapter's isPlaying operation when the playback state is not null and the
     * media is not playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testIsPlayingWithNotPlayingState() {
        long positionForTest = 0L;
        boolean notPlayingStatus = false;
        createPlaybackStateNotPlaying(positionForTest);
        assertEquals(mMediaControllerAdapter.isPlaying(), notPlayingStatus);
    }

    /**
     * Check adapter's getCurrentPosition operation when the playbackState is null.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetCurrentPositionWithNullPlaybackState() {
        long defaultPosition = 0L;
        assertEquals(mMediaControllerAdapter.getCurrentPosition(), defaultPosition);
    }

    /**
     * Check adapter's getCurrentPosition operation when the playback state is not null and the
     * media is playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetCurrentPositionWithPlayingState() {

        long positionForTest = 445L;
        createPlaybackStatePlaying(positionForTest);
        assertTrue(mMediaControllerAdapter.getCurrentPosition() >= positionForTest);
    }

    /**
     * Check adapter's getBufferedPosition method when the playback state is not null and the
     * media is not playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetCurrentPositionWithNotPlayingState() {

        long positionForTest = 445L;
        createPlaybackStateNotPlaying(positionForTest);
        assertTrue(mMediaControllerAdapter.getCurrentPosition() == positionForTest);
    }

    /**
     * Check adapter's getBufferedPosition method when the playback state is null.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetBufferedPositionWithNullPlaybackState() {
        // TODO: considering chaning default buffered position to -1
        long defaultBufferedPosition = 0L;
        assertEquals(mMediaControllerAdapter.getBufferedPosition(), defaultBufferedPosition);
    }

    /**
     * Check adapter's getBufferedPosition method when the playback state is not null and the
     * media is playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetBufferedPositionWithPlayingState() {
        long positionForTest = 445L;
        createPlaybackStatePlayingWithBufferedPosition(positionForTest, positionForTest);
        assertEquals(mMediaControllerAdapter.getBufferedPosition(), positionForTest);
    }

    /**
     * Check adapter's getBufferedPosition method when the playback state is not null and the
     * media is not playing.
     *
     * @throws InterruptedException wait() operation may cause InterruptedException.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetBufferedPositionWithNotPlayingState() {
        long positionForTest = 445L;
        createPlaybackStateNotPlayingWithBufferedPosition(positionForTest, positionForTest);
        assertEquals(mMediaControllerAdapter.getBufferedPosition(), positionForTest);
    }

    /**
     * check adapter's getMediaTitle() operation when the media meta data is not null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaTitleWithValidMetaData() {
        int widthForTest = 1;
        int heightForTest = 1;
        Bitmap bitmapForTesting = Bitmap.createBitmap(widthForTest, heightForTest,
                Bitmap.Config.ARGB_8888);
        Long durationForTeting = 0L;
        String mediaTitle = "media title";
        String albumName = "album name";
        String artistName = "artist name";

        createMediaMetaData(bitmapForTesting, durationForTeting, mediaTitle, albumName, artistName);
        assertEquals(mMediaControllerAdapter.getMediaTitle(), mediaTitle);
    }

    /**
     * check adapter's getMediaTitle() operation when the media meta data is null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaTitleWithNullMetaData() {
        String defaultMediaTitle = "";
        assertEquals(mMediaControllerAdapter.getMediaTitle(), defaultMediaTitle);
    }

    /**
     * check adapter's getMediaSubtitle() operation when the media meta data is not null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaSubTitleWithValidMetaData() {
        int widthForTest = 1;
        int heightForTest = 1;
        Bitmap bitmapForTesting = Bitmap.createBitmap(widthForTest, heightForTest,
                Bitmap.Config.ARGB_8888);
        Long durationForTeting = 0L;
        String mediaTitle = "media title";
        String albumName = "album name";
        String artistName = "artist name";

        createMediaMetaData(bitmapForTesting, durationForTeting, mediaTitle, albumName, artistName);
        assertEquals(mMediaControllerAdapter.getMediaSubtitle(), albumName);
    }

    /**
     * check adapter's getMediaSubtitle() operation when the media meta data is null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaSubTitleWithNullMetaData() {
        String defaultMediaSubTitle = "";
        assertEquals(mMediaControllerAdapter.getMediaSubtitle(), defaultMediaSubTitle);
    }

    /**
     * check adapter's getMediaArt operation when the media meta data is not null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaArtWithValidMetaData() {
        int widthForTest = 1;
        int heightForTest = 1;
        Bitmap bitmapForTesting = Bitmap.createBitmap(widthForTest, heightForTest,
                Bitmap.Config.ARGB_8888);
        Long durationForTeting = 0L;
        String mediaTitle = "media title";
        String albumName = "album name";
        String artistName = "artist name";

        createMediaMetaData(bitmapForTesting, durationForTeting, mediaTitle, albumName, artistName);
        Drawable testDrawable = new BitmapDrawable(mContext.getResources(), bitmapForTesting);
        // compare two drawable objects through serveral selected fields.
        assertEquals(mMediaControllerAdapter.getMediaArt(mContext).getBounds(),
                testDrawable.getBounds());
        assertEquals(mMediaControllerAdapter.getMediaArt(mContext).getAlpha(),
                testDrawable.getAlpha());
        assertEquals(mMediaControllerAdapter.getMediaArt(mContext).getColorFilter(),
                testDrawable.getColorFilter());
    }

    /**
     * check adapter's getMediaArt operation when the media meta data is null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetMediaArtWithNullMetaData() {
        Bitmap defaultMediaArt = null;
        assertEquals(mMediaControllerAdapter.getMediaArt(mContext), defaultMediaArt);
    }

    /**
     * check adapter's getDuration operation when the media meta data is not null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetDurationWithValidMetaData() {
        int widthForTest = 1;
        int heightForTest = 1;
        Bitmap bitmapForTesting = Bitmap.createBitmap(widthForTest, heightForTest,
                Bitmap.Config.ARGB_8888);
        Long durationForTeting = 45L;
        String mediaTitle = "media title";
        String albumName = "album name";
        String artistName = "artist name";

        createMediaMetaData(bitmapForTesting, durationForTeting, mediaTitle, albumName, artistName);
        assertEquals((Long) mMediaControllerAdapter.getDuration(), durationForTeting);
    }

    /**
     * check adapter's getDuration operation when the media meta data is null.
     */
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetDurationWithNullMetaData() {
        Long defaultDuration = 0L;
        assertEquals((Long) mMediaControllerAdapter.getDuration(), defaultDuration);
    }

    @After
    public void tearDown() throws Exception {
        mSession.release();
    }

    /**
     * Helper function to create playback state in the situation where media is playing.
     *
     * @param position current media item's playing position.
     */
    private void createPlaybackStatePlaying(long position) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        int playState = PlaybackStateCompat.STATE_PLAYING;
        long currentPosition = position;
        playbackStateBuilder.setState(playState, currentPosition, (float) 1.0).setActions(
                getPlaybackStateActions()
        );
        mSession.setPlaybackState(playbackStateBuilder.build());
    }

    /**
     * Helper function to create playback state in the situation where media is paused.
     *
     * @param position current media item's playing position.
     */
    private void createPlaybackStateNotPlaying(long position) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        int playState = PlaybackStateCompat.STATE_PAUSED;
        long currentPosition = position;
        playbackStateBuilder.setState(playState, currentPosition, (float) 1.0).setActions(
                getPlaybackStateActions()
        );
        mSession.setPlaybackState(playbackStateBuilder.build());
    }

    /**
     * Helper function to create playback state in the situation that media is playing.
     * Also the buffered position will be assigned in this function.
     *
     * @param position         current media item's playing position.
     * @param bufferedPosition current media item's buffered position.
     */
    private void createPlaybackStatePlayingWithBufferedPosition(long position,
            long bufferedPosition) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        int playState = PlaybackStateCompat.STATE_PLAYING;
        long currentPosition = position;
        playbackStateBuilder.setState(playState, currentPosition, (float) 1.0).setActions(
                getPlaybackStateActions()
        );
        playbackStateBuilder.setBufferedPosition(bufferedPosition);
        mSession.setPlaybackState(playbackStateBuilder.build());
    }

    /**
     * Helper function to create playback state in the situation that media is paused.
     * Also the buffered position will be assigned in this function.
     *
     * @param position         current media item's playing position.
     * @param bufferedPosition current media item's buffered position.
     */
    private void createPlaybackStateNotPlayingWithBufferedPosition(long position,
            long bufferedPosition) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        int playState = PlaybackStateCompat.STATE_PLAYING;
        long currentPosition = position;
        playbackStateBuilder.setState(playState, currentPosition, (float) 1.0).setActions(
                getPlaybackStateActions()
        );
        playbackStateBuilder.setBufferedPosition(bufferedPosition);
        mSession.setPlaybackState(playbackStateBuilder.build());
    }

    /**
     * Helper function to compute the supported playback action.
     *
     * @return supported playback actions.
     */
    private long getPlaybackStateActions() {
        long res = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE;
        return res;
    }

    /**
     * Helper function to create fake media meta data.
     *
     * @param bitmapForTesting   Bitmap
     * @param duratoinForTesting Duration
     * @param mediaTitle         Title.
     * @param albumName          Album name.
     * @param artistName         Artist name.
     */
    private void createMediaMetaData(Bitmap bitmapForTesting, Long duratoinForTesting,
            String mediaTitle, String albumName, String artistName) {
        MediaMetadataCompat.Builder metaDataBuilder = new MediaMetadataCompat.Builder();
        metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duratoinForTesting);
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                mediaTitle);
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                albumName);
        metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                albumName);
        metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                bitmapForTesting);
        metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                duratoinForTesting);
        mSession.setMetadata(metaDataBuilder.build());
    }

    // helper function to create dummy playback state for testing.
    PlaybackStateCompat createPlaybackStateForTesting(int playbackStateCompat) {
        long currentPosition = 0L;
        float playbackSpeed = 0.0f;
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setState(playbackStateCompat, currentPosition, playbackSpeed);
        return builder.build();
    }

    /**
     * Simulated MediaSessionCallback class for verification.
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private long mSeekPosition;
        private int mRepeatMode;
        private int mShuffleMode;

        private boolean mOnPlayCalled;
        private boolean mOnPauseCalled;
        private boolean mOnStopCalled;
        private boolean mOnFastForwardCalled;
        private boolean mOnRewindCalled;
        private boolean mOnSkipToPreviousCalled;
        private boolean mOnSkipToNextCalled;
        private boolean mOnSeekToCalled;
        private boolean mOnSetRepeatModeCalled;
        private boolean mOnSetShuffleModeCalled;

        public void reset() {
            mSeekPosition = -1;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;

            mOnPlayCalled = false;
            mOnPauseCalled = false;
            mOnStopCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSkipToPreviousCalled = false;
            mOnSkipToNextCalled = false;
            mOnSeekToCalled = false;
            mOnSetRepeatModeCalled = false;
            mOnSetShuffleModeCalled = false;
        }

        @Override
        public void onPlay() {
            synchronized (mWaitLock) {
                mOnPlayCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPause() {
            synchronized (mWaitLock) {
                mOnPauseCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onStop() {
            synchronized (mWaitLock) {
                mOnStopCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onFastForward() {
            synchronized (mWaitLock) {
                mOnFastForwardCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRewind() {
            synchronized (mWaitLock) {
                mOnRewindCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToPrevious() {
            synchronized (mWaitLock) {
                mOnSkipToPreviousCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToNext() {
            synchronized (mWaitLock) {
                mOnSkipToNextCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            synchronized (mWaitLock) {
                mOnSeekToCalled = true;
                mSeekPosition = pos;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            synchronized (mWaitLock) {
                mOnSetShuffleModeCalled = true;
                mShuffleMode = shuffleMode;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            synchronized (mWaitLock) {
                mOnSetRepeatModeCalled = true;
                mRepeatMode = repeatMode;
                mWaitLock.notify();
            }
        }
    }

    private class PlayerAdapterCallback extends PlayerAdapter.Callback {
        private boolean mOnPlayStateChangedCalled;
        private boolean mOnPreparedStateChangedCalled;
        private boolean mOnPlayCompletedCalled;
        private boolean mOnCurrentPositionChangedCalled;
        private boolean mOnBufferedPositionChangedCalled;
        private boolean mOnDurationChnagedCalled;
        private boolean mOnVideoSizeChangedCalled;
        private boolean mOnErrorCalled;
        private boolean mOnBufferingStateChangedCalled;
        private boolean mOnMetadataChangedCalled;

        public void reset() {
            mOnPlayStateChangedCalled = false;
            mOnPreparedStateChangedCalled = false;
            mOnPlayCompletedCalled = false;
            mOnCurrentPositionChangedCalled = false;
            mOnBufferedPositionChangedCalled = false;
            mOnDurationChnagedCalled = false;
            mOnVideoSizeChangedCalled = false;
            mOnErrorCalled = false;
            mOnBufferingStateChangedCalled = false;
            mOnMetadataChangedCalled = false;
        }

        @Override
        public void onPlayStateChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnPlayStateChangedCalled = true;
            }
        }

        @Override
        public void onPreparedStateChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnPreparedStateChangedCalled = true;
            }
        }

        @Override
        public void onPlayCompleted(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnPlayCompletedCalled = true;
            }
        }

        @Override
        public void onCurrentPositionChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnCurrentPositionChangedCalled = true;
            }
        }

        @Override
        public void onBufferedPositionChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnBufferedPositionChangedCalled = true;
            }
        }

        @Override
        public void onDurationChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnDurationChnagedCalled = true;
            }
        }

        @Override
        public void onVideoSizeChanged(PlayerAdapter adapter, int width, int height) {
            synchronized (mWaitLock) {
                mOnVideoSizeChangedCalled = true;
            }
        }

        @Override
        public void onError(PlayerAdapter adapter, int errorCode, String errorMessage) {
            synchronized (mWaitLock) {
                mOnErrorCalled = true;
            }
        }

        @Override
        public void onBufferingStateChanged(PlayerAdapter adapter, boolean start) {
            synchronized (mWaitLock) {
                mOnBufferingStateChangedCalled = true;
            }
        }

        @Override
        public void onMetadataChanged(PlayerAdapter adapter) {
            synchronized (mWaitLock) {
                mOnMetadataChangedCalled = true;
            }
        }
    }
}
