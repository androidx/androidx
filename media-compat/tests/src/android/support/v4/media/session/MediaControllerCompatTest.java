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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
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
    private static final String EXTRAS_KEY = "test-key";
    private static final String EXTRAS_VALUE = "test-val";
    private static final float DELTA = 1e-4f;
    private static final boolean ENABLED = true;
    private static final boolean DISABLED = false;

    private final Object mWaitLock = new Object();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaSessionCompat mSession;
    private MediaSessionCallback mCallback = new MediaSessionCallback();
    private MediaControllerCompat mController;

    @Before
    public void setUp() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession = new MediaSessionCompat(getContext(), SESSION_TAG);
                mSession.setCallback(mCallback, mHandler);
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
    public void testGetRatingType() {
        assertEquals("Default rating type of a session must be RatingCompat.RATING_NONE",
                RatingCompat.RATING_NONE, mController.getRatingType());

        mSession.setRatingType(RatingCompat.RATING_5_STARS);
        assertEquals(RatingCompat.RATING_5_STARS, mController.getRatingType());
    }

    @Test
    @SmallTest
    public void testGetSessionToken() throws Exception {
        assertEquals(mSession.getSessionToken(), mController.getSessionToken());
    }

    @Test
    @SmallTest
    public void testSendCommand() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            final String command = "test-command";
            final Bundle extras = new Bundle();
            extras.putString(EXTRAS_KEY, EXTRAS_VALUE);
            mController.sendCommand(command, extras, new ResultReceiver(null));
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCommandCalled);
            assertNotNull(mCallback.mCommandCallback);
            assertEquals(command, mCallback.mCommand);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));
        }
    }

    @Test
    @SmallTest
    public void testAddRemoveQueueItems() throws Exception {
        final String mediaId = "media_id";
        final String mediaTitle = "media_title";
        MediaDescriptionCompat itemDescription = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId).setTitle(mediaTitle).build();

        synchronized (mWaitLock) {
            mCallback.reset();
            mController.addQueueItem(itemDescription);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnAddQueueItemCalled);
            assertEquals(-1, mCallback.mQueueIndex);
            assertEquals(mediaId, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle, mCallback.mQueueDescription.getTitle());

            mCallback.reset();
            mController.addQueueItem(itemDescription, 0);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnAddQueueItemAtCalled);
            assertEquals(0, mCallback.mQueueIndex);
            assertEquals(mediaId, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle, mCallback.mQueueDescription.getTitle());

            mCallback.reset();
            mController.removeQueueItemAt(0);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnRemoveQueueItemAtCalled);
            assertEquals(0, mCallback.mQueueIndex);

            mCallback.reset();
            mController.removeQueueItem(itemDescription);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnRemoveQueueItemCalled);
            assertEquals(mediaId, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle, mCallback.mQueueDescription.getTitle());

            // Try to modify the queue when the session does not support queue management.
            mSession.setFlags(0);
            try {
                mController.addQueueItem(itemDescription);
                fail();
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
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
    public void testTransportControlsAndMediaSessionCallback() throws Exception {
        MediaControllerCompat.TransportControls controls = mController.getTransportControls();
        synchronized (mWaitLock) {
            mCallback.reset();
            controls.play();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayCalled);

            mCallback.reset();
            controls.pause();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPauseCalled);

            mCallback.reset();
            controls.stop();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnStopCalled);

            mCallback.reset();
            controls.fastForward();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnFastForwardCalled);

            mCallback.reset();
            controls.rewind();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnRewindCalled);

            mCallback.reset();
            controls.skipToPrevious();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToPreviousCalled);

            mCallback.reset();
            controls.skipToNext();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToNextCalled);

            mCallback.reset();
            final long seekPosition = 1000;
            controls.seekTo(seekPosition);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSeekToCalled);
            assertEquals(seekPosition, mCallback.mSeekPosition);

            mCallback.reset();
            final RatingCompat rating =
                    RatingCompat.newStarRating(RatingCompat.RATING_5_STARS, 3f);
            controls.setRating(rating);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetRatingCalled);
            assertEquals(rating.getRatingStyle(), mCallback.mRating.getRatingStyle());
            assertEquals(rating.getStarRating(), mCallback.mRating.getStarRating(), DELTA);

            mCallback.reset();
            final String mediaId = "test-media-id";
            final Bundle extras = new Bundle();
            extras.putString(EXTRAS_KEY, EXTRAS_VALUE);
            controls.playFromMediaId(mediaId, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromMediaIdCalled);
            assertEquals(mediaId, mCallback.mMediaId);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            final String query = "test-query";
            controls.playFromSearch(query, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromSearchCalled);
            assertEquals(query, mCallback.mQuery);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            final Uri uri = Uri.parse("content://test/popcorn.mod");
            controls.playFromUri(uri, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromUriCalled);
            assertEquals(uri, mCallback.mUri);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            final String action = "test-action";
            controls.sendCustomAction(action, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCustomActionCalled);
            assertEquals(action, mCallback.mAction);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            mCallback.mOnCustomActionCalled = false;
            final PlaybackStateCompat.CustomAction customAction =
                    new PlaybackStateCompat.CustomAction.Builder(action, action, -1)
                            .setExtras(extras)
                            .build();
            controls.sendCustomAction(customAction, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCustomActionCalled);
            assertEquals(action, mCallback.mAction);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            final long queueItemId = 1000;
            controls.skipToQueueItem(queueItemId);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToQueueItemCalled);
            assertEquals(queueItemId, mCallback.mQueueItemId);

            mCallback.reset();
            controls.prepare();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareCalled);

            mCallback.reset();
            controls.prepareFromMediaId(mediaId, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
            assertEquals(mediaId, mCallback.mMediaId);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            controls.prepareFromSearch(query, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromSearchCalled);
            assertEquals(query, mCallback.mQuery);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            controls.prepareFromUri(uri, extras);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromUriCalled);
            assertEquals(uri, mCallback.mUri);
            assertEquals(EXTRAS_VALUE, mCallback.mExtras.getString(EXTRAS_KEY));

            mCallback.reset();
            controls.setCaptioningEnabled(ENABLED);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetCaptioningEnabledCalled);
            assertEquals(ENABLED, mCallback.mCaptioningEnabled);

            mCallback.reset();
            final int repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
            controls.setRepeatMode(repeatMode);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetRepeatModeCalled);
            assertEquals(repeatMode, mCallback.mRepeatMode);

            mCallback.reset();
            controls.setShuffleModeEnabled(ENABLED);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetShuffleModeEnabledCalled);
            assertEquals(ENABLED, mCallback.mShuffleModeEnabled);
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

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private long mSeekPosition;
        private long mQueueItemId;
        private RatingCompat mRating;
        private String mMediaId;
        private String mQuery;
        private Uri mUri;
        private String mAction;
        private String mCommand;
        private Bundle mExtras;
        private ResultReceiver mCommandCallback;
        private boolean mCaptioningEnabled;
        private int mRepeatMode;
        private boolean mShuffleModeEnabled;
        private int mQueueIndex;
        private MediaDescriptionCompat mQueueDescription;

        private boolean mOnPlayCalled;
        private boolean mOnPauseCalled;
        private boolean mOnStopCalled;
        private boolean mOnFastForwardCalled;
        private boolean mOnRewindCalled;
        private boolean mOnSkipToPreviousCalled;
        private boolean mOnSkipToNextCalled;
        private boolean mOnSeekToCalled;
        private boolean mOnSkipToQueueItemCalled;
        private boolean mOnSetRatingCalled;
        private boolean mOnPlayFromMediaIdCalled;
        private boolean mOnPlayFromSearchCalled;
        private boolean mOnPlayFromUriCalled;
        private boolean mOnCustomActionCalled;
        private boolean mOnCommandCalled;
        private boolean mOnPrepareCalled;
        private boolean mOnPrepareFromMediaIdCalled;
        private boolean mOnPrepareFromSearchCalled;
        private boolean mOnPrepareFromUriCalled;
        private boolean mOnSetCaptioningEnabledCalled;
        private boolean mOnSetRepeatModeCalled;
        private boolean mOnSetShuffleModeEnabledCalled;
        private boolean mOnAddQueueItemCalled;
        private boolean mOnAddQueueItemAtCalled;
        private boolean mOnRemoveQueueItemCalled;
        private boolean mOnRemoveQueueItemAtCalled;

        public void reset() {
            mSeekPosition = -1;
            mQueueItemId = -1;
            mRating = null;
            mMediaId = null;
            mQuery = null;
            mUri = null;
            mAction = null;
            mExtras = null;
            mCommand = null;
            mCommandCallback = null;
            mCaptioningEnabled = false;
            mShuffleModeEnabled = false;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mQueueIndex = -1;
            mQueueDescription = null;

            mOnPlayCalled = false;
            mOnPauseCalled = false;
            mOnStopCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSkipToPreviousCalled = false;
            mOnSkipToNextCalled = false;
            mOnSkipToQueueItemCalled = false;
            mOnSeekToCalled = false;
            mOnSetRatingCalled = false;
            mOnPlayFromMediaIdCalled = false;
            mOnPlayFromSearchCalled = false;
            mOnPlayFromUriCalled = false;
            mOnCustomActionCalled = false;
            mOnCommandCalled = false;
            mOnPrepareCalled = false;
            mOnPrepareFromMediaIdCalled = false;
            mOnPrepareFromSearchCalled = false;
            mOnPrepareFromUriCalled = false;
            mOnSetCaptioningEnabledCalled = false;
            mOnSetRepeatModeCalled = false;
            mOnSetShuffleModeEnabledCalled = false;
            mOnAddQueueItemCalled = false;
            mOnAddQueueItemAtCalled = false;
            mOnRemoveQueueItemCalled = false;
            mOnRemoveQueueItemAtCalled = false;
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
        public void onSetRating(RatingCompat rating) {
            synchronized (mWaitLock) {
                mOnSetRatingCalled = true;
                mRating = rating;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPlayFromMediaIdCalled = true;
                mMediaId = mediaId;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPlayFromSearchCalled = true;
                mQuery = query;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPlayFromUriCalled = true;
                mUri = uri;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            synchronized (mWaitLock) {
                mOnCustomActionCalled = true;
                mAction = action;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            synchronized (mWaitLock) {
                mOnSkipToQueueItemCalled = true;
                mQueueItemId = id;
                mWaitLock.notify();
            }
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            synchronized (mWaitLock) {
                mOnCommandCalled = true;
                mCommand = command;
                mExtras = extras;
                mCommandCallback = cb;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPrepare() {
            synchronized (mWaitLock) {
                mOnPrepareCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPrepareFromMediaIdCalled = true;
                mMediaId = mediaId;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPrepareFromSearchCalled = true;
                mQuery = query;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPrepareFromUriCalled = true;
                mUri = uri;
                mExtras = extras;
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

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            synchronized (mWaitLock) {
                mOnAddQueueItemCalled = true;
                mQueueDescription = description;
                mWaitLock.notify();
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            synchronized (mWaitLock) {
                mOnAddQueueItemAtCalled = true;
                mQueueIndex = index;
                mQueueDescription = description;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            synchronized (mWaitLock) {
                mOnRemoveQueueItemCalled = true;
                mQueueDescription = description;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            synchronized (mWaitLock) {
                mOnSetCaptioningEnabledCalled = true;
                mCaptioningEnabled = enabled;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSetShuffleModeEnabled(boolean enabled) {
            synchronized (mWaitLock) {
                mOnSetShuffleModeEnabledCalled = true;
                mShuffleModeEnabled = enabled;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRemoveQueueItemAt(int index) {
            synchronized (mWaitLock) {
                mOnRemoveQueueItemAtCalled = true;
                mQueueIndex = index;
                mWaitLock.notify();
            }
        }
    }
}
