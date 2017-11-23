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
import static android.support.mediacompat.testlib.MediaControllerConstants.ADJUST_VOLUME;
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
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_VOLUME_TO;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_NEXT;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_PREVIOUS;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants.STOP;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_COMMAND;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_KEY;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_ID_1;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_ID_2;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_TITLE_1;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_TITLE_2;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_QUEUE_ID_1;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaSessionCompat.Callback}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaSessionCompatCallbackTest {

    private static final String TAG = "MediaSessionCompatCallbackTest";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final long WAIT_TIME_FOR_NO_RESPONSE_MS = 300L;

    private static final long TEST_POSITION = 1000000L;
    private static final float TEST_PLAYBACK_SPEED = 3.0f;
    private static final float DELTA = 1e-4f;
    private static final boolean ENABLED = true;

    private final Object mWaitLock = new Object();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String mClientVersion;
    private MediaSessionCompat mSession;
    private MediaSessionCallback mCallback = new MediaSessionCallback();
    private AudioManager mAudioManager;

    @Before
    public void setUp() throws Exception {
        // The version of the client app is provided through the instrumentation arguments.
        mClientVersion = getArguments().getString(KEY_CLIENT_VERSION, "");
        Log.d(TAG, "Client app version: " + mClientVersion);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                mSession = new MediaSessionCompat(getTargetContext(), TEST_SESSION_TAG);
                mSession.setCallback(mCallback, mHandler);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mSession.release();
    }

    /**
     * Tests that a session can be created and that all the fields are initialized correctly.
     */
    @Test
    @SmallTest
    public void testCreateSession() throws Exception {
        assertNotNull(mSession.getSessionToken());
        assertFalse("New session should not be active", mSession.isActive());

        // Verify by getting the controller and checking all its fields
        MediaControllerCompat controller = mSession.getController();
        assertNotNull(controller);

        final String errorMsg = "New session has unexpected configuration.";
        assertEquals(errorMsg, 0L, controller.getFlags());
        assertNull(errorMsg, controller.getExtras());
        assertNull(errorMsg, controller.getMetadata());
        assertEquals(errorMsg, getContext().getPackageName(), controller.getPackageName());
        assertNull(errorMsg, controller.getPlaybackState());
        assertNull(errorMsg, controller.getQueue());
        assertNull(errorMsg, controller.getQueueTitle());
        assertEquals(errorMsg, RatingCompat.RATING_NONE, controller.getRatingType());
        assertNull(errorMsg, controller.getSessionActivity());

        assertNotNull(controller.getSessionToken());
        assertNotNull(controller.getTransportControls());

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(errorMsg, MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                info.getPlaybackType());
        assertEquals(errorMsg, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                info.getCurrentVolume());
    }

    @Test
    @SmallTest
    public void testGetSessionToken() throws Exception {
        assertEquals(mSession.getSessionToken(), mSession.getController().getSessionToken());
    }

    /**
     * Tests that a session can be created from the framework session object and the callback
     * set on the framework session object before fromSession() is called works properly.
     */
    @Test
    @SmallTest
    public void testFromSession() throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            // MediaSession was introduced from API level 21.
            return;
        }
        mCallback.reset(1);
        mSession.setCallback(mCallback, new Handler(Looper.getMainLooper()));
        MediaSessionCompat session = MediaSessionCompat.fromMediaSession(
                getContext(), mSession.getMediaSession());
        assertEquals(session.getSessionToken(), mSession.getSessionToken());

        session.getController().getTransportControls().play();
        mCallback.await(TIME_OUT_MS);
        assertEquals(1, mCallback.mOnPlayCalledCount);
    }

    /**
     * Tests {@link MediaSessionCompat.Token} created in the constructor of MediaSessionCompat.
     */
    @Test
    @SmallTest
    public void testSessionToken() throws Exception {
        MediaSessionCompat.Token sessionToken = mSession.getSessionToken();

        assertNotNull(sessionToken);
        assertEquals(0, sessionToken.describeContents());

        // Test writeToParcel
        Parcel p = Parcel.obtain();
        sessionToken.writeToParcel(p, 0);
        p.setDataPosition(0);
        MediaSessionCompat.Token token = MediaSessionCompat.Token.CREATOR.createFromParcel(p);
        assertEquals(token, sessionToken);
        p.recycle();
    }

    /**
     * Tests {@link MediaSessionCompat.QueueItem}.
     */
    @Test
    @SmallTest
    public void testQueueItem() {
        MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(TEST_MEDIA_ID_1)
                        .setTitle(TEST_MEDIA_TITLE_1)
                        .build(),
                TEST_QUEUE_ID_1);
        assertEquals(TEST_QUEUE_ID_1, item.getQueueId());
        assertEquals(TEST_MEDIA_ID_1, item.getDescription().getMediaId());
        assertEquals(TEST_MEDIA_TITLE_1, item.getDescription().getTitle());
        assertEquals(0, item.describeContents());

        Parcel p = Parcel.obtain();
        item.writeToParcel(p, 0);
        p.setDataPosition(0);
        MediaSessionCompat.QueueItem other =
                MediaSessionCompat.QueueItem.CREATOR.createFromParcel(p);
        assertEquals(item.toString(), other.toString());
        p.recycle();
    }

    /**
     * Tests {@link MediaSessionCompat#setActive}.
     */
    @Test
    @SmallTest
    public void testSetActive() throws Exception {
        mSession.setActive(true);
        assertTrue(mSession.isActive());
    }

    @Test
    @SmallTest
    public void testGetPlaybackStateWithPositionUpdate() throws InterruptedException {
        final long stateSetTime = SystemClock.elapsedRealtime();
        PlaybackStateCompat stateIn = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, TEST_POSITION, TEST_PLAYBACK_SPEED,
                        stateSetTime)
                .build();
        mSession.setPlaybackState(stateIn);

        final long waitDuration = 100L;
        Thread.sleep(waitDuration);

        final long expectedUpdateTime = waitDuration + stateSetTime;
        final long expectedPosition = (long) (TEST_PLAYBACK_SPEED * waitDuration) + TEST_POSITION;

        final double updateTimeTolerance = 50L;
        final double positionTolerance = updateTimeTolerance * TEST_PLAYBACK_SPEED;

        PlaybackStateCompat stateOut = mSession.getController().getPlaybackState();
        assertEquals(expectedUpdateTime, stateOut.getLastPositionUpdateTime(), updateTimeTolerance);
        assertEquals(expectedPosition, stateOut.getPosition(), positionTolerance);

        // Compare the result with MediaController.getPlaybackState().
        if (Build.VERSION.SDK_INT >= 21) {
            MediaController controller = new MediaController(
                    getContext(), (MediaSession.Token) mSession.getSessionToken().getToken());
            PlaybackState state = controller.getPlaybackState();
            assertEquals(state.getLastPositionUpdateTime(), stateOut.getLastPositionUpdateTime(),
                    updateTimeTolerance);
            assertEquals(state.getPosition(), stateOut.getPosition(), positionTolerance);
        }
    }

    /**
     * Tests {@link MediaSessionCompat#setCallback} with {@code null}.
     * No callback should be called once {@code setCallback(null)} is done.
     */
    @Test
    @SmallTest
    public void testSetCallbackWithNull() throws Exception {
        mSession.setActive(true);
        mCallback.reset(1);
        callTransportControlsMethod(PLAY, null, getContext(), mSession.getSessionToken());
        mSession.setCallback(null, mHandler);
        mCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS);
        assertEquals("Callback shouldn't be called.", 0, mCallback.mOnPlayCalledCount);
    }

    @Test
    @SmallTest
    public void testSendCommand() throws Exception {
        mCallback.reset(1);

        Bundle arguments = new Bundle();
        arguments.putString("command", TEST_COMMAND);
        Bundle extras = new Bundle();
        extras.putString(TEST_KEY, TEST_VALUE);
        arguments.putBundle("extras", extras);
        callMediaControllerMethod(
                SEND_COMMAND, arguments, getContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCommandCalled);
        assertNotNull(mCallback.mCommandCallback);
        assertEquals(TEST_COMMAND, mCallback.mCommand);
        assertBundleEquals(extras, mCallback.mExtras);
    }

    @Test
    @SmallTest
    public void testAddRemoveQueueItems() throws Exception {
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        MediaDescriptionCompat itemDescription1 = new MediaDescriptionCompat.Builder()
                .setMediaId(TEST_MEDIA_ID_1).setTitle(TEST_MEDIA_TITLE_1).build();

        MediaDescriptionCompat itemDescription2 = new MediaDescriptionCompat.Builder()
                .setMediaId(TEST_MEDIA_ID_2).setTitle(TEST_MEDIA_TITLE_2).build();

        mCallback.reset(1);
        callMediaControllerMethod(
                ADD_QUEUE_ITEM, itemDescription1, getContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnAddQueueItemCalled);
        assertEquals(-1, mCallback.mQueueIndex);
        assertEquals(TEST_MEDIA_ID_1, mCallback.mQueueDescription.getMediaId());
        assertEquals(TEST_MEDIA_TITLE_1, mCallback.mQueueDescription.getTitle());

        mCallback.reset(1);
        Bundle arguments = new Bundle();
        arguments.putParcelable("description", itemDescription2);
        arguments.putInt("index", 0);
        callMediaControllerMethod(
                ADD_QUEUE_ITEM_WITH_INDEX, arguments, getContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnAddQueueItemAtCalled);
        assertEquals(0, mCallback.mQueueIndex);
        assertEquals(TEST_MEDIA_ID_2, mCallback.mQueueDescription.getMediaId());
        assertEquals(TEST_MEDIA_TITLE_2, mCallback.mQueueDescription.getTitle());

        mCallback.reset(1);
        callMediaControllerMethod(
                REMOVE_QUEUE_ITEM, itemDescription1, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnRemoveQueueItemCalled);
        assertEquals(TEST_MEDIA_ID_1, mCallback.mQueueDescription.getMediaId());
        assertEquals(TEST_MEDIA_TITLE_1, mCallback.mQueueDescription.getTitle());
    }

    @Test
    @SmallTest
    public void testTransportControlsAndMediaSessionCallback() throws Exception {
        mCallback.reset(1);
        callTransportControlsMethod(PLAY, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertEquals(1, mCallback.mOnPlayCalledCount);

        mCallback.reset(1);
        callTransportControlsMethod(PAUSE, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPauseCalled);

        mCallback.reset(1);
        callTransportControlsMethod(STOP, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnStopCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                FAST_FORWARD, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnFastForwardCalled);

        mCallback.reset(1);
        callTransportControlsMethod(REWIND, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnRewindCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                SKIP_TO_PREVIOUS, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToPreviousCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                SKIP_TO_NEXT, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToNextCalled);

        mCallback.reset(1);
        final long seekPosition = 1000;
        callTransportControlsMethod(
                SEEK_TO, seekPosition, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSeekToCalled);
        assertEquals(seekPosition, mCallback.mSeekPosition);

        mCallback.reset(1);
        final RatingCompat rating =
                RatingCompat.newStarRating(RatingCompat.RATING_5_STARS, 3f);
        callTransportControlsMethod(
                SET_RATING, rating, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetRatingCalled);
        assertEquals(rating.getRatingStyle(), mCallback.mRating.getRatingStyle());
        assertEquals(rating.getStarRating(), mCallback.mRating.getStarRating(), DELTA);

        mCallback.reset(1);
        final Bundle extras = new Bundle();
        extras.putString(TEST_KEY, TEST_VALUE);
        Bundle arguments = new Bundle();
        arguments.putString("mediaId", TEST_MEDIA_ID_1);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PLAY_FROM_MEDIA_ID, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPlayFromMediaIdCalled);
        assertEquals(TEST_MEDIA_ID_1, mCallback.mMediaId);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        final String query = "test-query";
        arguments = new Bundle();
        arguments.putString("query", query);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PLAY_FROM_SEARCH, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPlayFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        final Uri uri = Uri.parse("content://test/popcorn.mod");
        arguments = new Bundle();
        arguments.putParcelable("uri", uri);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PLAY_FROM_URI, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPlayFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        final String action = "test-action";
        arguments = new Bundle();
        arguments.putString("action", action);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                SEND_CUSTOM_ACTION, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCustomActionCalled);
        assertEquals(action, mCallback.mAction);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
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
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCustomActionCalled);
        assertEquals(action, mCallback.mAction);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        final long queueItemId = 1000;
        callTransportControlsMethod(
                SKIP_TO_QUEUE_ITEM, queueItemId, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToQueueItemCalled);
        assertEquals(queueItemId, mCallback.mQueueItemId);

        mCallback.reset(1);
        callTransportControlsMethod(
                PREPARE, null, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareCalled);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putString("mediaId", TEST_MEDIA_ID_2);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_MEDIA_ID, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(TEST_MEDIA_ID_2, mCallback.mMediaId);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putString("query", query);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_SEARCH, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putParcelable("uri", uri);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_URI, arguments, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        callTransportControlsMethod(
                SET_CAPTIONING_ENABLED, ENABLED, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetCaptioningEnabledCalled);
        assertEquals(ENABLED, mCallback.mCaptioningEnabled);

        mCallback.reset(1);
        final int repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
        callTransportControlsMethod(
                SET_REPEAT_MODE, repeatMode, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetRepeatModeCalled);
        assertEquals(repeatMode, mCallback.mRepeatMode);

        mCallback.reset(1);
        final int shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_ALL;
        callTransportControlsMethod(
                SET_SHUFFLE_MODE, shuffleMode, getContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetShuffleModeCalled);
        assertEquals(shuffleMode, mCallback.mShuffleMode);
    }

    /**
     * Tests {@link MediaSessionCompat.Callback#onMediaButtonEvent}.
     */
    @Test
    @MediumTest
    public void testCallbackOnMediaButtonEvent() throws Exception {
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mSession.setActive(true);

        final long waitTimeForNoResponse = 30L;

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(new ComponentName(getContext(), getContext().getClass()));
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, mediaButtonIntent, 0);
        mSession.setMediaButtonReceiver(pi);

        // Set state to STATE_PLAYING to get higher priority.
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnPauseCalled);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_NEXT);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnSkipToNextCalled);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnSkipToPreviousCalled);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnStopCalled);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnFastForwardCalled);

        mCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_REWIND);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnRewindCalled);

        // Test PLAY_PAUSE button twice.
        // First, send PLAY_PAUSE button event while in STATE_PAUSED.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        // Next, send PLAY_PAUSE button event while in STATE_PLAYING.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnPauseCalled);

        // Double tap of PLAY_PAUSE is the next track.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertFalse(mCallback.await(waitTimeForNoResponse));
        assertTrue(mCallback.mOnSkipToNextCalled);
        assertEquals(0, mCallback.mOnPlayCalledCount);
        assertFalse(mCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button long-press.
        // It should be the same as the single short-press.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        // Double tap of PLAY_PAUSE should be handled once.
        // Initial down event from the second press within double tap time-out will make
        // onSkipToNext() to be called, so further down events shouldn't be handled again.
        mCallback.reset(2);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertFalse(mCallback.await(waitTimeForNoResponse));
        assertTrue(mCallback.mOnSkipToNextCalled);
        assertEquals(0, mCallback.mOnPlayCalledCount);
        assertFalse(mCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button long-press followed by the short-press.
        // Initial long-press of the PLAY_PAUSE is considered as the single short-press already,
        // so it shouldn't be used as the first tap of the double tap.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        // onMediaButtonEvent() calls either onPlay() or onPause() depending on the playback state,
        // so onPlay() should be called once and onPause() also should be called once.
        assertEquals(1, mCallback.mOnPlayCalledCount);
        assertTrue(mCallback.mOnPauseCalled);
        assertFalse(mCallback.mOnSkipToNextCalled);

        // If another media key is pressed while the double tap of PLAY_PAUSE,
        // PLAY_PAUSE should be handled as normal.
        mCallback.reset(3);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertFalse(mCallback.mOnSkipToNextCalled);
        assertTrue(mCallback.mOnStopCalled);
        assertEquals(2, mCallback.mOnPlayCalledCount);

        // Test if media keys are handled in order.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);
        assertTrue(mCallback.mOnStopCalled);
        synchronized (mWaitLock) {
            assertEquals(PlaybackStateCompat.STATE_STOPPED,
                    mSession.getController().getPlaybackState().getState());
        }
    }

    @Test
    @SmallTest
    public void testVolumeControl() throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 27) {
            // This test causes an Exception on System UI in API < 27.
            return;
        }
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
            callMediaControllerMethod(SET_VOLUME_TO,
                    7 /* Target volume */, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());

            // test adjustVolume
            callMediaControllerMethod(ADJUST_VOLUME,
                    AudioManager.ADJUST_LOWER, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(6, vp.getCurrentVolume());

            callMediaControllerMethod(ADJUST_VOLUME,
                    AudioManager.ADJUST_RAISE, getContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());
        }
    }

    private void setPlaybackState(int state) {
        final long allActions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_FAST_FORWARD | PlaybackStateCompat.ACTION_REWIND;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder().setActions(allActions)
                .setState(state, 0L, 0.0f).build();
        synchronized (mWaitLock) {
            mSession.setPlaybackState(playbackState);
        }
    }

    private void sendMediaKeyInputToController(int keyCode) {
        sendMediaKeyInputToController(keyCode, false);
    }

    private void sendMediaKeyInputToController(int keyCode, boolean isLongPress) {
        MediaControllerCompat controller = mSession.getController();
        long currentTimeMs = System.currentTimeMillis();
        KeyEvent down = new KeyEvent(
                currentTimeMs, currentTimeMs, KeyEvent.ACTION_DOWN, keyCode, 0);
        controller.dispatchMediaButtonEvent(down);
        if (isLongPress) {
            KeyEvent longPress = new KeyEvent(
                    currentTimeMs, System.currentTimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 1);
            controller.dispatchMediaButtonEvent(longPress);
        }
        KeyEvent up = new KeyEvent(
                currentTimeMs, System.currentTimeMillis(), KeyEvent.ACTION_UP, keyCode, 0);
        controller.dispatchMediaButtonEvent(up);
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private CountDownLatch mLatch;
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

        private int mOnPlayCalledCount;
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

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
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

            mOnPlayCalledCount = 0;
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

        public boolean await(long timeoutMs) {
            try {
                return mLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public void onPlay() {
            mOnPlayCalledCount++;
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mLatch.countDown();
        }

        @Override
        public void onPause() {
            mOnPauseCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            mLatch.countDown();
        }

        @Override
        public void onStop() {
            mOnStopCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            mLatch.countDown();
        }

        @Override
        public void onFastForward() {
            mOnFastForwardCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onRewind() {
            mOnRewindCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToPrevious() {
            mOnSkipToPreviousCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToNext() {
            mOnSkipToNextCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSeekTo(long pos) {
            mOnSeekToCalled = true;
            mSeekPosition = pos;
            mLatch.countDown();
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            mOnSetRatingCalled = true;
            mRating = rating;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mOnPlayFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            mOnPlayFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            mOnPlayFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            mOnCustomActionCalled = true;
            mAction = action;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mOnSkipToQueueItemCalled = true;
            mQueueItemId = id;
            mLatch.countDown();
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            mOnCommandCalled = true;
            mCommand = command;
            mExtras = extras;
            mCommandCallback = cb;
            mLatch.countDown();
        }

        @Override
        public void onPrepare() {
            mOnPrepareCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            mOnPrepareFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            mOnPrepareFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            mOnPrepareFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            mOnSetRepeatModeCalled = true;
            mRepeatMode = repeatMode;
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            mOnAddQueueItemCalled = true;
            mQueueDescription = description;
            mQueue.add(new MediaSessionCompat.QueueItem(description, mQueue.size()));
            mSession.setQueue(mQueue);
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            mOnAddQueueItemAtCalled = true;
            mQueueIndex = index;
            mQueueDescription = description;
            mQueue.add(index, new MediaSessionCompat.QueueItem(description, mQueue.size()));
            mSession.setQueue(mQueue);
            mLatch.countDown();
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            mOnRemoveQueueItemCalled = true;
            String mediaId = description.getMediaId();
            for (int i = mQueue.size() - 1; i >= 0; --i) {
                if (mediaId.equals(mQueue.get(i).getDescription().getMediaId())) {
                    mQueueDescription = mQueue.remove(i).getDescription();
                    mSession.setQueue(mQueue);
                    break;
                }
            }
            mLatch.countDown();
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            mOnSetCaptioningEnabledCalled = true;
            mCaptioningEnabled = enabled;
            mLatch.countDown();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            mOnSetShuffleModeCalled = true;
            mShuffleMode = shuffleMode;
            mLatch.countDown();
        }
    }
}
