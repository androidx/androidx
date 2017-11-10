/*
 * Copyright 2017 The Android Open Source Project
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
package android.support.mediacompat.service;

import static android.support.mediacompat.testlib.MediaControllerConstants.ADD_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants
        .ADD_QUEUE_ITEM_WITH_INDEX;
import static android.support.mediacompat.testlib.MediaControllerConstants.FAST_FORWARD;
import static android.support.mediacompat.testlib.MediaControllerConstants.PAUSE;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_MEDIA_ID;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_SEARCH;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_URI;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_MEDIA_ID;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_SEARCH;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_URI;
import static android.support.mediacompat.testlib.MediaControllerConstants.REMOVE_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants.REWIND;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEEK_TO;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEND_COMMAND;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEND_CUSTOM_ACTION;
import static android.support.mediacompat.testlib.MediaControllerConstants
        .SEND_CUSTOM_ACTION_PARCELABLE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_CAPTIONING_ENABLED;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_RATING;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_REPEAT_MODE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_SHUFFLE_MODE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_NEXT;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_PREVIOUS;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants.STOP;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_COMMAND;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_KEY;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_SESSION_TAG;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_VALUE;
import static android.support.mediacompat.testlib.VersionConstants.KEY_CLIENT_VERSION;
import static android.support.mediacompat.testlib.util.IntentUtil.callMediaControllerMethod;
import static android.support.mediacompat.testlib.util.IntentUtil.callTransportControlsMethod;
import static android.support.mediacompat.testlib.util.TestUtil.assertBundleEquals;
import static android.support.test.InstrumentationRegistry.getArguments;
import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Test {@link MediaSessionCompat.Callback}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaSessionCompatCallbackTest {

    private static final String TAG = "MediaSessionCompatCallbackTest";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final float DELTA = 1e-4f;
    private static final boolean ENABLED = true;

    private final Object mWaitLock = new Object();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String mClientVersion;
    private MediaSessionCompat mSession;
    private MediaSessionCallback mCallback = new MediaSessionCallback();

    @Before
    public void setUp() throws Exception {
        // The version of the client app is provided through the instrumentation arguments.
        mClientVersion = getArguments().getString(KEY_CLIENT_VERSION, "");
        Log.d(TAG, "Client app version: " + mClientVersion);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession = new MediaSessionCompat(getTargetContext(), TEST_SESSION_TAG);
                mSession.setCallback(mCallback, mHandler);
                mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mSession.release();
    }

    @Test
    @SmallTest
    public void testSendCommand() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();

            Bundle arguments = new Bundle();
            arguments.putString("command", TEST_COMMAND);
            Bundle extras = new Bundle();
            extras.putString(TEST_KEY, TEST_VALUE);
            arguments.putBundle("extras", extras);
            callMediaControllerMethod(
                    SEND_COMMAND, arguments, getContext(), mSession.getSessionToken());

            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCommandCalled);
            assertNotNull(mCallback.mCommandCallback);
            assertEquals(TEST_COMMAND, mCallback.mCommand);
            assertBundleEquals(extras, mCallback.mExtras);
        }
    }

    @Test
    @SmallTest
    public void testAddRemoveQueueItems() throws Exception {
        final String mediaId1 = "media_id_1";
        final String mediaTitle1 = "media_title_1";
        MediaDescriptionCompat itemDescription1 = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId1).setTitle(mediaTitle1).build();

        final String mediaId2 = "media_id_2";
        final String mediaTitle2 = "media_title_2";
        MediaDescriptionCompat itemDescription2 = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId2).setTitle(mediaTitle2).build();

        synchronized (mWaitLock) {
            mCallback.reset();
            callMediaControllerMethod(
                    ADD_QUEUE_ITEM, itemDescription1, getContext(), mSession.getSessionToken());

            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnAddQueueItemCalled);
            assertEquals(-1, mCallback.mQueueIndex);
            assertEquals(mediaId1, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle1, mCallback.mQueueDescription.getTitle());

            mCallback.reset();
            Bundle arguments = new Bundle();
            arguments.putParcelable("description", itemDescription2);
            arguments.putInt("index", 0);
            callMediaControllerMethod(
                    ADD_QUEUE_ITEM_WITH_INDEX, arguments, getContext(), mSession.getSessionToken());

            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnAddQueueItemAtCalled);
            assertEquals(0, mCallback.mQueueIndex);
            assertEquals(mediaId2, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle2, mCallback.mQueueDescription.getTitle());

            mCallback.reset();
            callMediaControllerMethod(
                    REMOVE_QUEUE_ITEM, itemDescription1, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnRemoveQueueItemCalled);
            assertEquals(mediaId1, mCallback.mQueueDescription.getMediaId());
            assertEquals(mediaTitle1, mCallback.mQueueDescription.getTitle());
        }
    }

    @Test
    @SmallTest
    public void testTransportControlsAndMediaSessionCallback() throws Exception {
        synchronized (mWaitLock) {
            mCallback.reset();
            callTransportControlsMethod(PLAY, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayCalled);

            mCallback.reset();
            callTransportControlsMethod(PAUSE, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPauseCalled);

            mCallback.reset();
            callTransportControlsMethod(STOP, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnStopCalled);

            mCallback.reset();
            callTransportControlsMethod(
                    FAST_FORWARD, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnFastForwardCalled);

            mCallback.reset();
            callTransportControlsMethod(REWIND, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnRewindCalled);

            mCallback.reset();
            callTransportControlsMethod(
                    SKIP_TO_PREVIOUS, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToPreviousCalled);

            mCallback.reset();
            callTransportControlsMethod(
                    SKIP_TO_NEXT, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToNextCalled);

            mCallback.reset();
            final long seekPosition = 1000;
            callTransportControlsMethod(
                    SEEK_TO, seekPosition, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSeekToCalled);
            assertEquals(seekPosition, mCallback.mSeekPosition);

            mCallback.reset();
            final RatingCompat rating =
                    RatingCompat.newStarRating(RatingCompat.RATING_5_STARS, 3f);
            callTransportControlsMethod(
                    SET_RATING, rating, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetRatingCalled);
            assertEquals(rating.getRatingStyle(), mCallback.mRating.getRatingStyle());
            assertEquals(rating.getStarRating(), mCallback.mRating.getStarRating(), DELTA);

            mCallback.reset();
            final String mediaId = "test-media-id";
            final Bundle extras = new Bundle();
            extras.putString(TEST_KEY, TEST_VALUE);
            Bundle arguments = new Bundle();
            arguments.putString("mediaId", mediaId);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PLAY_FROM_MEDIA_ID, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromMediaIdCalled);
            assertEquals(mediaId, mCallback.mMediaId);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            final String query = "test-query";
            arguments = new Bundle();
            arguments.putString("query", query);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PLAY_FROM_SEARCH, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromSearchCalled);
            assertEquals(query, mCallback.mQuery);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            final Uri uri = Uri.parse("content://test/popcorn.mod");
            arguments = new Bundle();
            arguments.putParcelable("uri", uri);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PLAY_FROM_URI, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPlayFromUriCalled);
            assertEquals(uri, mCallback.mUri);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            final String action = "test-action";
            arguments = new Bundle();
            arguments.putString("action", action);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    SEND_CUSTOM_ACTION, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCustomActionCalled);
            assertEquals(action, mCallback.mAction);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            mCallback.mOnCustomActionCalled = false;
            final PlaybackStateCompat.CustomAction customAction =
                    new PlaybackStateCompat.CustomAction.Builder(action, action, -1)
                            .setExtras(extras)
                            .build();
            arguments = new Bundle();
            arguments.putParcelable("action", customAction);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    SEND_CUSTOM_ACTION_PARCELABLE,
                    arguments,
                    getContext(),
                    mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnCustomActionCalled);
            assertEquals(action, mCallback.mAction);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            final long queueItemId = 1000;
            callTransportControlsMethod(
                    SKIP_TO_QUEUE_ITEM, queueItemId, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSkipToQueueItemCalled);
            assertEquals(queueItemId, mCallback.mQueueItemId);

            mCallback.reset();
            callTransportControlsMethod(
                    PREPARE, null, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareCalled);

            mCallback.reset();
            arguments = new Bundle();
            arguments.putString("mediaId", mediaId);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PREPARE_FROM_MEDIA_ID, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
            assertEquals(mediaId, mCallback.mMediaId);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            arguments = new Bundle();
            arguments.putString("query", query);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PREPARE_FROM_SEARCH, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromSearchCalled);
            assertEquals(query, mCallback.mQuery);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            arguments = new Bundle();
            arguments.putParcelable("uri", uri);
            arguments.putBundle("extras", extras);
            callTransportControlsMethod(
                    PREPARE_FROM_URI, arguments, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnPrepareFromUriCalled);
            assertEquals(uri, mCallback.mUri);
            assertBundleEquals(extras, mCallback.mExtras);

            mCallback.reset();
            callTransportControlsMethod(
                    SET_CAPTIONING_ENABLED, ENABLED, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetCaptioningEnabledCalled);
            assertEquals(ENABLED, mCallback.mCaptioningEnabled);

            mCallback.reset();
            final int repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
            callTransportControlsMethod(
                    SET_REPEAT_MODE, repeatMode, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetRepeatModeCalled);
            assertEquals(repeatMode, mCallback.mRepeatMode);

            mCallback.reset();
            final int shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_ALL;
            callTransportControlsMethod(
                    SET_SHUFFLE_MODE, shuffleMode, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSetShuffleModeCalled);
            assertEquals(shuffleMode, mCallback.mShuffleMode);
        }
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
        private int mShuffleMode;
        private int mQueueIndex;
        private MediaDescriptionCompat mQueueDescription;
        private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();

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
        private boolean mOnSetShuffleModeCalled;
        private boolean mOnAddQueueItemCalled;
        private boolean mOnAddQueueItemAtCalled;
        private boolean mOnRemoveQueueItemCalled;

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
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;
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
            mOnSetShuffleModeCalled = false;
            mOnAddQueueItemCalled = false;
            mOnAddQueueItemAtCalled = false;
            mOnRemoveQueueItemCalled = false;
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
                mQueue.add(new MediaSessionCompat.QueueItem(description, mQueue.size()));
                mSession.setQueue(mQueue);
                mWaitLock.notify();
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            synchronized (mWaitLock) {
                mOnAddQueueItemAtCalled = true;
                mQueueIndex = index;
                mQueueDescription = description;
                mQueue.add(index, new MediaSessionCompat.QueueItem(description, mQueue.size()));
                mSession.setQueue(mQueue);
                mWaitLock.notify();
            }
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            synchronized (mWaitLock) {
                mOnRemoveQueueItemCalled = true;
                String mediaId = description.getMediaId();
                for (int i = mQueue.size() - 1; i >= 0; --i) {
                    if (mediaId.equals(mQueue.get(i).getDescription().getMediaId())) {
                        mQueueDescription = mQueue.remove(i).getDescription();
                        mSession.setQueue(mQueue);
                        break;
                    }
                }
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
        public void onSetShuffleMode(int shuffleMode) {
            synchronized (mWaitLock) {
                mOnSetShuffleModeCalled = true;
                mShuffleMode = shuffleMode;
                mWaitLock.notify();
            }
        }
    }
}
