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
import static android.support.mediacompat.testlib.MediaControllerConstants.ADD_QUEUE_ITEM_WITH_CUSTOM_PARCELABLE;
import static android.support.mediacompat.testlib.MediaControllerConstants.ADD_QUEUE_ITEM_WITH_INDEX;
import static android.support.mediacompat.testlib.MediaControllerConstants.ADJUST_VOLUME;
import static android.support.mediacompat.testlib.MediaControllerConstants.DISPATCH_MEDIA_BUTTON;
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
import static android.support.mediacompat.testlib.MediaControllerConstants.SEND_CUSTOM_ACTION_PARCELABLE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_CAPTIONING_ENABLED;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_PLAYBACK_SPEED;
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
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;

import static androidx.media.MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getArguments;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

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
import android.os.Process;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.mediacompat.testlib.util.IntentUtil;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media.VolumeProviderCompat;
import androidx.media.test.lib.CustomParcelable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

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
    private static final long VOLUME_CHANGE_TIMEOUT_MS = 5000L;
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
    private final Bundle mSessionInfo = new Bundle();
    private AudioManager mAudioManager;

    @Before
    public void setUp() throws Exception {
        // The version of the client app is provided through the instrumentation arguments.
        mClientVersion = getArguments().getString(KEY_CLIENT_VERSION, "");
        Log.d(TAG, "Client app version: " + mClientVersion);

        mSessionInfo.putString(TEST_KEY, TEST_VALUE);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mAudioManager = (AudioManager) getApplicationContext().getSystemService(
                        Context.AUDIO_SERVICE);
                mSession = new MediaSessionCompat(getApplicationContext(), TEST_SESSION_TAG,
                        null, null, mSessionInfo);
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
    @SuppressWarnings("deprecation")
    @Test
    @SmallTest
    public void testCreateSession() throws Exception {
        assertNotNull(mSession.getSessionToken());
        assertFalse("New session should not be active", mSession.isActive());

        // Verify by getting the controller and checking all its fields
        MediaControllerCompat controller = mSession.getController();
        assertNotNull(controller);

        final String errorMsg = "New session has unexpected configuration.";
        assertBundleEquals(mSessionInfo, controller.getSessionInfo());
        assertEquals(errorMsg, FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS,
                controller.getFlags());
        assertNull(errorMsg, controller.getExtras());
        assertNull(errorMsg, controller.getMetadata());
        assertEquals(errorMsg, getApplicationContext().getPackageName(),
                controller.getPackageName());
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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testFromSession() throws Exception {
        mCallback.reset(1);
        mCallback.setExpectedCallerPackageName(getExpectedPackageNameForSelf());
        mSession.setCallback(mCallback, new Handler(Looper.getMainLooper()));
        MediaSessionCompat session = MediaSessionCompat.fromMediaSession(
                getApplicationContext(), mSession.getMediaSession());
        assertEquals(session.getSessionToken(), mSession.getSessionToken());

        session.getController().getTransportControls().play();
        mCallback.await(TIME_OUT_MS);
        assertEquals(1, mCallback.mOnPlayCalledCount);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testCallers() throws Exception {
        mCallback.reset(1, getExpectedPackageNameForSelf());
        mSession.setCallback(mCallback, new Handler(Looper.getMainLooper()));
        MediaSessionCompat session = MediaSessionCompat.fromMediaSession(
                getApplicationContext(), mSession.getMediaSession());
        assertEquals(session.getSessionToken(), mSession.getSessionToken());

        MediaControllerCompat controller1 = session.getController();
        MediaControllerCompat controller2 =
                new MediaControllerCompat(getApplicationContext(), session.getSessionToken());

        controller1.getTransportControls().stop();
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnStopCalled);
        RemoteUserInfo remoteUserInfo1 = mCallback.mRemoteUserInfoForStop;
        assertEquals(getExpectedPackageNameForSelf(), remoteUserInfo1.getPackageName());
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(Process.myUid(), remoteUserInfo1.getUid());
            assertEquals(Process.myPid(), remoteUserInfo1.getPid());
        }

        mCallback.reset(1, getExpectedPackageNameForSelf());
        controller2.getTransportControls().stop();
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnStopCalled);
        RemoteUserInfo remoteUserInfo2 = mCallback.mRemoteUserInfoForStop;
        assertEquals(getExpectedPackageNameForSelf(), remoteUserInfo2.getPackageName());
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(Process.myUid(), remoteUserInfo2.getUid());
            assertEquals(Process.myPid(), remoteUserInfo2.getPid());
        }
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
                    getApplicationContext(),
                    (MediaSession.Token) mSession.getSessionToken().getToken());
            PlaybackState state = controller.getPlaybackState();
            assertEquals(state.getLastPositionUpdateTime(), stateOut.getLastPositionUpdateTime(),
                    updateTimeTolerance);
            assertEquals(state.getPosition(), stateOut.getPosition(), positionTolerance);
        }
    }

    /**
     * Tests {@link MediaSessionCompat#setCallback} with {@code null}.
     * No callback messages should be posted once {@code setCallback(null)} is done.
     */
    @Test
    @MediumTest
    public void testSetCallbackWithNull() throws Exception {
        mSession.setActive(true);
        mCallback.reset(1);
        mSession.setCallback(null);
        callTransportControlsMethod(PLAY, null, getApplicationContext(),
                mSession.getSessionToken());
        assertFalse(mCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS));
        assertEquals("Callback shouldn't be called.", 0, mCallback.mOnPlayCalledCount);
    }

    /**
     * Tests whether {@link MediaSessionCompat#setCallback} with {@code null} stops receiving
     * callback methods.
     */
    @Test
    @MediumTest
    public void testSetCallbackWithNullShouldRemoveCallbackMessages() throws Exception {
        mSession.setActive(true);
        mCallback.reset(1);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                callTransportControlsMethod(PLAY, null, getApplicationContext(),
                        mSession.getSessionToken());
                mSession.setCallback(null);
            }
        });
        assertFalse(mCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS));
        assertEquals("Callback shouldn't be called.", 0, mCallback.mOnPlayCalledCount);
    }

    /**
     * Tests whether {@link MediaSessionCompat#setCallback} with different callback prevents
     * old callback from receiving callback methods.
     */
    @Test
    @MediumTest
    public void testSetCallbacWithDifferentCallback() throws Exception {
        mSession.setActive(true);
        mCallback.reset(1);
        final MediaSessionCallback newCallback = new MediaSessionCallback();
        newCallback.reset(1);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
                mSession.setCallback(newCallback);
            }
        });
        assertFalse(mCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS));
        assertEquals("Callback shouldn't be called.", 0, mCallback.mOnPlayCalledCount);

        assertFalse(newCallback.await(WAIT_TIME_FOR_NO_RESPONSE_MS));
        assertEquals("Callback shouldn't be called.", 0, newCallback.mOnPlayCalledCount);
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
        ResultReceiver receiver = new ResultReceiver(null /* handler */);
        arguments.putParcelable("resultReceiver", receiver);
        callMediaControllerMethod(
                SEND_COMMAND, arguments, getApplicationContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCommandCalled);
        assertNotNull(mCallback.mResultReceiver);
        assertEquals(TEST_COMMAND, mCallback.mCommand);
        assertBundleEquals(extras, mCallback.mExtras);
    }

    @Test
    @SmallTest
    public void testSendCommandWithNullResultReceiver() throws Exception {
        mCallback.reset(1);
        Bundle arguments = new Bundle();
        arguments.putString("command", TEST_COMMAND);
        // No result receiver.
        callMediaControllerMethod(
                SEND_COMMAND, arguments, getApplicationContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCommandCalled);
        assertNull(mCallback.mResultReceiver);
        assertEquals(TEST_COMMAND, mCallback.mCommand);
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
                ADD_QUEUE_ITEM, itemDescription1, getApplicationContext(),
                mSession.getSessionToken());

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
                ADD_QUEUE_ITEM_WITH_INDEX, arguments, getApplicationContext(),
                mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnAddQueueItemAtCalled);
        assertEquals(0, mCallback.mQueueIndex);
        assertEquals(TEST_MEDIA_ID_2, mCallback.mQueueDescription.getMediaId());
        assertEquals(TEST_MEDIA_TITLE_2, mCallback.mQueueDescription.getTitle());

        mCallback.reset(1);
        callMediaControllerMethod(
                REMOVE_QUEUE_ITEM, itemDescription1, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnRemoveQueueItemCalled);
        assertEquals(TEST_MEDIA_ID_1, mCallback.mQueueDescription.getMediaId());
        assertEquals(TEST_MEDIA_TITLE_1, mCallback.mQueueDescription.getTitle());
    }

    @Test
    @SmallTest
    public void testTransportControlsAndMediaSessionCallback() throws Exception {
        mCallback.reset(1);
        callTransportControlsMethod(PLAY, null, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertEquals(1, mCallback.mOnPlayCalledCount);

        mCallback.reset(1);
        callTransportControlsMethod(PAUSE, null, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPauseCalled);

        mCallback.reset(1);
        callTransportControlsMethod(STOP, null, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnStopCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                FAST_FORWARD, null, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnFastForwardCalled);

        mCallback.reset(1);
        callTransportControlsMethod(REWIND, null, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnRewindCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                SKIP_TO_PREVIOUS, null, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToPreviousCalled);

        mCallback.reset(1);
        callTransportControlsMethod(
                SKIP_TO_NEXT, null, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToNextCalled);

        mCallback.reset(1);
        final long seekPosition = 1000;
        callTransportControlsMethod(
                SEEK_TO, seekPosition, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSeekToCalled);
        assertEquals(seekPosition, mCallback.mSeekPosition);

        mCallback.reset(1);
        final RatingCompat rating =
                RatingCompat.newStarRating(RatingCompat.RATING_5_STARS, 3f);
        callTransportControlsMethod(
                SET_RATING, rating, getApplicationContext(), mSession.getSessionToken());
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
                PLAY_FROM_MEDIA_ID, arguments, getApplicationContext(), mSession.getSessionToken());
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
                PLAY_FROM_SEARCH, arguments, getApplicationContext(), mSession.getSessionToken());
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
                PLAY_FROM_URI, arguments, getApplicationContext(), mSession.getSessionToken());
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
                SEND_CUSTOM_ACTION, arguments, getApplicationContext(), mSession.getSessionToken());
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
                getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCustomActionCalled);
        assertEquals(action, mCallback.mAction);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        final long queueItemId = 1000;
        callTransportControlsMethod(
                SKIP_TO_QUEUE_ITEM, queueItemId, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSkipToQueueItemCalled);
        assertEquals(queueItemId, mCallback.mQueueItemId);

        mCallback.reset(1);
        callTransportControlsMethod(
                PREPARE, null, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareCalled);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putString("mediaId", TEST_MEDIA_ID_2);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_MEDIA_ID, arguments, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(TEST_MEDIA_ID_2, mCallback.mMediaId);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putString("query", query);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_SEARCH, arguments, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        arguments = new Bundle();
        arguments.putParcelable("uri", uri);
        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                PREPARE_FROM_URI, arguments, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnPrepareFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertBundleEquals(extras, mCallback.mExtras);

        mCallback.reset(1);
        callTransportControlsMethod(
                SET_CAPTIONING_ENABLED, ENABLED, getApplicationContext(),
                mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetCaptioningEnabledCalled);
        assertEquals(ENABLED, mCallback.mCaptioningEnabled);

        mCallback.reset(1);
        final int repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
        callTransportControlsMethod(
                SET_REPEAT_MODE, repeatMode, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetRepeatModeCalled);
        assertEquals(repeatMode, mCallback.mRepeatMode);

        mCallback.reset(1);
        final int shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_ALL;
        callTransportControlsMethod(
                SET_SHUFFLE_MODE, shuffleMode, getApplicationContext(), mSession.getSessionToken());
        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnSetShuffleModeCalled);
        assertEquals(shuffleMode, mCallback.mShuffleMode);
    }

    /**
     * Tests {@link MediaSessionCompat.Callback#onSetPlaybackSpeed(float)}.
     */
    @Test
    @SmallTest
    public void testCallback_onSetPlaybackSpeed() {
        mCallback.reset(1);
        final float testSpeed = 2.0f;
        callTransportControlsMethod(
                SET_PLAYBACK_SPEED, testSpeed, getApplicationContext(), mSession.getSessionToken());
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnSetPlaybackSpeedCalled);
        assertEquals(testSpeed, mCallback.mSpeed, 0.0f);
    }

    /**
     * Tests {@link MediaSessionCompat.Callback#onMediaButtonEvent}.
     */
    @Test
    @MediumTest
    @FlakyTest(bugId = 111811728)
    public void testCallbackOnMediaButtonEvent() throws Exception {
        mSession.setActive(true);

        final long waitTimeForNoResponse = 100L;

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(new ComponentName(getApplicationContext(),
                        getApplicationContext().getClass()));
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent,
                0);
        mSession.setMediaButtonReceiver(pi);

        // Set state to STATE_PLAYING to get higher priority.
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnPauseCalled);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_NEXT);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnSkipToNextCalled);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnSkipToPreviousCalled);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnStopCalled);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnFastForwardCalled);

        mCallback.reset(1);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_REWIND);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnRewindCalled);

        // Test PLAY_PAUSE button twice.
        // First, send PLAY_PAUSE button event while in STATE_PAUSED.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        // Next, send PLAY_PAUSE button event while in STATE_PLAYING.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertTrue(mCallback.mOnPauseCalled);

        // Double tap of PLAY_PAUSE is the next track.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertFalse(mCallback.await(waitTimeForNoResponse));
        assertTrue(mCallback.mOnSkipToNextCalled);
        assertEquals(0, mCallback.mOnPlayCalledCount);
        assertFalse(mCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button long-press.
        // It should be the same as the single short-press.
        mCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertEquals(1, mCallback.mOnPlayCalledCount);

        // Double tap of PLAY_PAUSE should be handled once.
        // Initial down event from the second press within double tap time-out will make
        // onSkipToNext() to be called, so further down events shouldn't be handled again.
        mCallback.reset(2);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertFalse(mCallback.await(waitTimeForNoResponse));
        assertTrue(mCallback.mOnSkipToNextCalled);
        assertEquals(0, mCallback.mOnPlayCalledCount);
        assertFalse(mCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button long-press followed by the short-press.
        // Initial long-press of the PLAY_PAUSE is considered as the single short-press already,
        // so it shouldn't be used as the first tap of the double tap.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
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
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_STOP);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(mCallback.await(TIME_OUT_MS));
        assertFalse(mCallback.mOnSkipToNextCalled);
        assertTrue(mCallback.mOnStopCalled);
        assertEquals(2, mCallback.mOnPlayCalledCount);

        // Test if media keys are handled in order.
        mCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyEventFromController(KeyEvent.KEYCODE_MEDIA_STOP);
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
    public void testSetVolumeWithLocalVolume() throws Exception {
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume =
                Build.VERSION.SDK_INT >= 28 ? mAudioManager.getStreamMinVolume(stream) : 0;
        Log.d(TAG, "maxVolume=" + maxVolume + ", minVolume=" + minVolume);
        if (maxVolume <= minVolume) {
            return;
        }

        mSession.setPlaybackToLocal(stream);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        callMediaControllerMethod(SET_VOLUME_TO, targetVolume, getApplicationContext(),
                mSession.getSessionToken());
        new PollingCheck(VOLUME_CHANGE_TIMEOUT_MS) {
            @Override
            protected boolean check() {
                return targetVolume == mAudioManager.getStreamVolume(stream);
            }
        }.run();

        // Set back to original volume.
        mAudioManager.setStreamVolume(stream, originalVolume, 0 /* flags */);
    }

    @Test
    @SmallTest
    public void testAdjustVolumeWithLocalVolume() throws Exception {
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume =
                Build.VERSION.SDK_INT >= 28 ? mAudioManager.getStreamMinVolume(stream) : 0;
        Log.d(TAG, "maxVolume=" + maxVolume + ", minVolume=" + minVolume);
        if (maxVolume <= minVolume) {
            return;
        }

        mSession.setPlaybackToLocal(stream);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        callMediaControllerMethod(ADJUST_VOLUME, direction, getApplicationContext(),
                mSession.getSessionToken());
        new PollingCheck(VOLUME_CHANGE_TIMEOUT_MS) {
            @Override
            protected boolean check() {
                return targetVolume == mAudioManager.getStreamVolume(stream);
            }
        }.run();

        // Set back to original volume.
        mAudioManager.setStreamVolume(stream, originalVolume, 0 /* flags */);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1) // To prevent System UI Exception.
    public void testRemoteVolumeControl() throws Exception {
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
                    7 /* Target volume */, getApplicationContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());

            // test adjustVolume
            callMediaControllerMethod(ADJUST_VOLUME,
                    AudioManager.ADJUST_LOWER, getApplicationContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(6, vp.getCurrentVolume());

            callMediaControllerMethod(ADJUST_VOLUME,
                    AudioManager.ADJUST_RAISE, getApplicationContext(), mSession.getSessionToken());
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(7, vp.getCurrentVolume());
        }
    }

    @Test
    @SmallTest
    public void testReceivingMediaParcelables() throws Exception {
        mCallback.reset(1);
        final String action = "test-action";

        Bundle arguments = new Bundle();
        arguments.putString("action", action);

        final MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId("testMediaId")
                .build();
        final MediaSessionCompat.QueueItem queueItem =
                new MediaSessionCompat.QueueItem(desc, 1 /* flags */);
        final MediaBrowserCompat.MediaItem mediaItem =
                new MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
        final PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setBufferedPosition(1000)
                .build();

        Bundle extras = new Bundle();
        extras.putParcelable("description", desc);
        extras.putParcelable("queueItem", queueItem);
        extras.putParcelable("mediaItem", mediaItem);
        extras.putParcelable("state", state);

        arguments.putBundle("extras", extras);
        callTransportControlsMethod(
                SEND_CUSTOM_ACTION, arguments, getApplicationContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnCustomActionCalled);
        assertEquals(action, mCallback.mAction);

        // Viewing the contents of bundle from remote process should not throw any exceptions.
        // Also check whether the bundle has expected contents.
        Bundle extrasOut = mCallback.mExtras;
        assertNotNull(extrasOut);
        assertNotNull(extrasOut.getClassLoader());

        MediaDescriptionCompat descOut = extrasOut.getParcelable("description");
        assertEquals(desc.getMediaId(), descOut.getMediaId());

        MediaSessionCompat.QueueItem queueItemOut = extrasOut.getParcelable("queueItem");
        assertEquals(queueItem.getQueueId(), queueItemOut.getQueueId());

        MediaBrowserCompat.MediaItem mediaItemOut = extrasOut.getParcelable("mediaItem");
        assertEquals(mediaItem.getFlags(), mediaItemOut.getFlags());

        PlaybackStateCompat stateOut = extrasOut.getParcelable("state");
        assertEquals(state.getBufferedPosition(), stateOut.getBufferedPosition());
    }

    @Test
    @SmallTest
    public void testMediaDescriptionContainsUserParcelable() {
        mCallback.reset(1);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        final int testValue = 3;
        // The client app will call addQueueItem() with MediaDescriptionCompat which includes
        // CustomParcelable created with given testValue.
        callMediaControllerMethod(ADD_QUEUE_ITEM_WITH_CUSTOM_PARCELABLE,
                testValue, getApplicationContext(), mSession.getSessionToken());

        mCallback.await(TIME_OUT_MS);
        assertTrue(mCallback.mOnAddQueueItemCalled);
        CustomParcelable customParcelableOut =
                mCallback.mQueueDescription.getExtras().getParcelable("customParcelable");
        assertEquals(testValue, customParcelableOut.mValue);
    }

    /**
     * Tests b/139093164.
     */
    @Test
    @SmallTest
    public void testCallbacksAfterReleased() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSession.setActive(true);

                // Dispatch media key events from the controller while blocking the looper for the
                // session callback. For blocking purpose, create a local media controller here
                // rather than using RemoteMediaController.
                MediaControllerCompat controller =
                        new MediaControllerCompat(getApplicationContext(),
                                mSession.getSessionToken());
                long currentTimeMs = System.currentTimeMillis();
                KeyEvent down = new KeyEvent(
                        currentTimeMs, currentTimeMs, KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                KeyEvent up = new KeyEvent(
                        currentTimeMs, System.currentTimeMillis(), KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                controller.dispatchMediaButtonEvent(down);
                controller.dispatchMediaButtonEvent(up);

                // Keeps the old reference to prevent GC.
                MediaSessionCompat oldSession = mSession;

                // Recreate media session with the same callback reference.
                mSession.release();
                mSession = new MediaSessionCompat(getApplicationContext(), TEST_SESSION_TAG,
                        null, null, mSessionInfo);
                mSession.setCallback(mCallback, mHandler);

                // Do something with old session, just not to be optimized away.
                oldSession.setActive(false);
            }
        });
        // Post asserts to the main thread to ensure that mCallback has received all pended
        // callbacks.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                assertEquals(0, mCallback.mOnPlayCalledCount);
                assertFalse(mCallback.mOnPauseCalled);
            }
        });
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

    private void sendMediaKeyEventFromController(int keyCode) {
        sendMediaKeyEventFromController(keyCode, false);
    }

    private void sendMediaKeyEventFromController(int keyCode, boolean isLongPress) {
        long currentTimeMs = System.currentTimeMillis();
        KeyEvent down = new KeyEvent(
                currentTimeMs, currentTimeMs, KeyEvent.ACTION_DOWN, keyCode, 0);
        callMediaControllerMethod(
                DISPATCH_MEDIA_BUTTON, down, getApplicationContext(), mSession.getSessionToken());
        if (isLongPress) {
            KeyEvent longPress = new KeyEvent(
                    currentTimeMs, System.currentTimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 1);
            callMediaControllerMethod(
                    DISPATCH_MEDIA_BUTTON, longPress, getApplicationContext(),
                    mSession.getSessionToken());
        }
        KeyEvent up = new KeyEvent(
                currentTimeMs, System.currentTimeMillis(), KeyEvent.ACTION_UP, keyCode, 0);
        callMediaControllerMethod(
                DISPATCH_MEDIA_BUTTON, up, getApplicationContext(), mSession.getSessionToken());
    }

    private String getExpectedPackageNameForSelf() {
        if (Build.VERSION.SDK_INT >= 24 || Build.VERSION.SDK_INT < 21) {
            return IntentUtil.SERVICE_PACKAGE_NAME;
        } else {
            return LEGACY_CONTROLLER;
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private CountDownLatch mLatch;
        private RemoteUserInfo mRemoteUserInfoForStop;
        private String mExpectedCallerPackageName;
        private long mSeekPosition;
        private long mQueueItemId;
        private RatingCompat mRating;
        private String mMediaId;
        private String mQuery;
        private Uri mUri;
        private String mAction;
        private String mCommand;
        private Bundle mExtras;
        private ResultReceiver mResultReceiver;
        private boolean mCaptioningEnabled;
        private int mRepeatMode;
        private int mShuffleMode;
        private int mQueueIndex;
        private MediaDescriptionCompat mQueueDescription;
        private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();
        private float mSpeed;

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
        private boolean mOnSetPlaybackSpeedCalled;

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
            if (Build.VERSION.SDK_INT >= 24 || Build.VERSION.SDK_INT < 21) {
                setExpectedCallerPackageName(IntentUtil.CLIENT_PACKAGE_NAME);
            } else {
                setExpectedCallerPackageName(LEGACY_CONTROLLER);
            }
            mSeekPosition = -1;
            mQueueItemId = -1;
            mRating = null;
            mMediaId = null;
            mQuery = null;
            mUri = null;
            mAction = null;
            mExtras = null;
            mCommand = null;
            mResultReceiver = null;
            mCaptioningEnabled = false;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;
            mQueueIndex = -1;
            mQueueDescription = null;
            mSpeed = -1.0f;

            mRemoteUserInfoForStop = null;
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
            mOnSetPlaybackSpeedCalled = false;
        }

        public void reset(int count, String expectedCallerPackageName) {
            reset(count);
            setExpectedCallerPackageName(expectedCallerPackageName);
        }

        public void setExpectedCallerPackageName(String packageName) {
            mExpectedCallerPackageName = packageName;
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
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPlayCalledCount++;
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mLatch.countDown();
        }

        @Override
        public void onPause() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPauseCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            mLatch.countDown();
        }

        @Override
        public void onStop() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnStopCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            mRemoteUserInfoForStop = mSession.getCurrentControllerInfo();
            mLatch.countDown();
        }

        @Override
        public void onFastForward() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnFastForwardCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onRewind() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnRewindCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToPrevious() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSkipToPreviousCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToNext() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSkipToNextCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSeekTo(long pos) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSeekToCalled = true;
            mSeekPosition = pos;
            mLatch.countDown();
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSetRatingCalled = true;
            mRating = rating;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPlayFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPlayFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPlayFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnCustomActionCalled = true;
            mAction = action;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSkipToQueueItemCalled = true;
            mQueueItemId = id;
            mLatch.countDown();
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnCommandCalled = true;
            mCommand = command;
            mExtras = extras;
            mResultReceiver = cb;
            mLatch.countDown();
        }

        @Override
        public void onPrepare() {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPrepareCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPrepareFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPrepareFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnPrepareFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSetRepeatModeCalled = true;
            mRepeatMode = repeatMode;
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnAddQueueItemCalled = true;
            mQueueDescription = description;
            mQueue.add(new MediaSessionCompat.QueueItem(description, mQueue.size()));
            mSession.setQueue(mQueue);
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnAddQueueItemAtCalled = true;
            mQueueIndex = index;
            mQueueDescription = description;
            mQueue.add(index, new MediaSessionCompat.QueueItem(description, mQueue.size()));
            mSession.setQueue(mQueue);
            mLatch.countDown();
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
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
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSetCaptioningEnabledCalled = true;
            mCaptioningEnabled = enabled;
            mLatch.countDown();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSetShuffleModeCalled = true;
            mShuffleMode = shuffleMode;
            mLatch.countDown();
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            if (!isCallerTestClient()) {
                // Ignore
                return;
            }
            mOnSetPlaybackSpeedCalled = true;
            mSpeed = speed;
            mLatch.countDown();
        }

        private boolean isCallerTestClient() {
            RemoteUserInfo info = mSession.getCurrentControllerInfo();
            assertNotNull(info);

            // Don't stop test for an unexpected package name here, because any controller may
            // connect to test session and send command while testing.
            return mExpectedCallerPackageName.equals(info.getPackageName());
        }
    }
}
