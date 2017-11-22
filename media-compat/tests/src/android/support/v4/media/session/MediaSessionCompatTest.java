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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.view.KeyEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaSessionCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaSessionCompatTest {
    // The maximum time to wait for an operation, that is expected to happen.
    private static final long TIME_OUT_MS = 3000L;
    // The maximum time to wait for an operation, that is expected not to happen.
    private static final long WAIT_TIME_MS = 30L;
    private static final int MAX_AUDIO_INFO_CHANGED_CALLBACK_COUNT = 10;
    private static final String TEST_SESSION_TAG = "test-session-tag";
    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-val";
    private static final Bundle TEST_BUNDLE = createTestBundle();
    private static final String TEST_SESSION_EVENT = "test-session-event";
    private static final int TEST_CURRENT_VOLUME = 10;
    private static final int TEST_MAX_VOLUME = 11;
    private static final long TEST_QUEUE_ID = 12L;
    private static final long TEST_ACTION = 55L;
    private static final int TEST_ERROR_CODE =
            PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED;
    private static final String TEST_ERROR_MSG = "test-error-msg";

    private static Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(TEST_KEY, TEST_VALUE);
        return bundle;
    }

    private AudioManager mAudioManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Object mWaitLock = new Object();
    private MediaControllerCallback mCallback = new MediaControllerCallback();
    private MediaSessionCompat mSession;

    @Before
    public void setUp() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                mSession = new MediaSessionCompat(getContext(), TEST_SESSION_TAG);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        // It is OK to call release() twice.
        mSession.release();
        mSession = null;
    }

    /**
     * Tests that a session can be created and that all the fields are
     * initialized correctly.
     */
    @Test
    @SmallTest
    public void testCreateSession() throws Exception {
        assertNotNull(mSession.getSessionToken());
        assertFalse("New session should not be active", mSession.isActive());

        // Verify by getting the controller and checking all its fields
        MediaControllerCompat controller = mSession.getController();
        assertNotNull(controller);
        verifyNewSession(controller, TEST_SESSION_TAG);
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
        MediaSessionCallback callback = new MediaSessionCallback();
        callback.reset(1);
        mSession.setCallback(callback, new Handler(Looper.getMainLooper()));
        MediaSessionCompat session = MediaSessionCompat.fromMediaSession(
                getContext(), mSession.getMediaSession());
        assertEquals(session.getSessionToken(), mSession.getSessionToken());
        synchronized (mWaitLock) {
            session.getController().getTransportControls().play();
            mWaitLock.wait(TIME_OUT_MS);
            assertEquals(1, callback.mOnPlayCalledCount);
        }
    }

    /**
     * Tests MediaSessionCompat.Token created in the constructor of MediaSessionCompat.
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
     * Tests {@link MediaSessionCompat#setActive} and {@link MediaSessionCompat#release}.
     */
    @Test
    @SmallTest
    public void testSetActiveAndRelease() throws Exception {
        MediaControllerCompat controller = mSession.getController();
        controller.registerCallback(mCallback, mHandler);
        synchronized (mWaitLock) {
            mSession.setActive(true);
            assertTrue(mSession.isActive());

            mCallback.resetLocked();
            mSession.release();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mCallback.mOnSessionDestroyedCalled);
        }
    }

    /**
     * Tests {@link MediaSessionCompat#setCallback} with {@code null}. No callback will be called
     * once {@code setCallback(null)} is done.
     */
    @Test
    @SmallTest
    public void testSetCallbackWithNull() throws Exception {
        MediaSessionCallback sessionCallback = new MediaSessionCallback();
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);
        mSession.setCallback(sessionCallback, mHandler);

        MediaControllerCompat controller = mSession.getController();
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        sessionCallback.reset(1);
        mSession.setCallback(null, mHandler);

        controller.getTransportControls().pause();
        assertFalse(sessionCallback.await(WAIT_TIME_MS));
        assertFalse("Callback shouldn't be called.", sessionCallback.mOnPauseCalled);
    }

    /**
     * Tests {@link MediaSessionCompat.Callback#onMediaButtonEvent}.
     */
    @Test
    @SmallTest
    public void testCallbackOnMediaButtonEvent() throws Exception {
        MediaSessionCallback sessionCallback = new MediaSessionCallback();
        mSession.setCallback(sessionCallback, new Handler(Looper.getMainLooper()));
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mSession.setActive(true);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(
                new ComponentName(getContext(), getContext().getClass()));
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, mediaButtonIntent, 0);
        mSession.setMediaButtonReceiver(pi);

        // Set state to STATE_PLAYING to get higher priority.
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertEquals(1, sessionCallback.mOnPlayCalledCount);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PAUSE);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnPauseCalled);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_NEXT);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnSkipToNextCalled);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnSkipToPreviousCalled);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnStopCalled);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnFastForwardCalled);

        sessionCallback.reset(1);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_REWIND);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnRewindCalled);

        // Test PLAY_PAUSE button twice.
        // First, send PLAY_PAUSE button event while in STATE_PAUSED.
        sessionCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertEquals(1, sessionCallback.mOnPlayCalledCount);

        // Next, send PLAY_PAUSE button event while in STATE_PLAYING.
        sessionCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertTrue(sessionCallback.mOnPauseCalled);

        // Double tap of PLAY_PAUSE is the next track.
        sessionCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertFalse(sessionCallback.await(WAIT_TIME_MS));
        assertTrue(sessionCallback.mOnSkipToNextCalled);
        assertEquals(0, sessionCallback.mOnPlayCalledCount);
        assertFalse(sessionCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button long-press.
        // It should be the same as the single short-press.
        sessionCallback.reset(1);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertEquals(1, sessionCallback.mOnPlayCalledCount);

        // Double tap of PLAY_PAUSE should be handled once.
        // Initial down event from the second press within double tap time-out will make
        // onSkipToNext() to be called, so further down events shouldn't be handled again.
        sessionCallback.reset(2);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        assertFalse(sessionCallback.await(WAIT_TIME_MS));
        assertTrue(sessionCallback.mOnSkipToNextCalled);
        assertEquals(0, sessionCallback.mOnPlayCalledCount);
        assertFalse(sessionCallback.mOnPauseCalled);

        // Test PLAY_PAUSE button short-press followed by the long-press.
        // Initial long-press of the PLAY_PAUSE is considered as the single short-press already,
        // so it shouldn't be used as the first tap of the double tap.
        sessionCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        // onMediaButtonEvent() calls either onPlay() or onPause() depending on the playback state,
        // so onPlay() should be called twice while onPause() isn't called.
        assertEquals(1, sessionCallback.mOnPlayCalledCount);
        assertTrue(sessionCallback.mOnPauseCalled);
        assertFalse(sessionCallback.mOnSkipToNextCalled);

        // If another media key is pressed while the double tap of PLAY_PAUSE,
        // PLAY_PAUSE should be handles as normal.
        sessionCallback.reset(3);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertFalse(sessionCallback.mOnSkipToNextCalled);
        assertTrue(sessionCallback.mOnStopCalled);
        assertEquals(2, sessionCallback.mOnPlayCalledCount);

        // Test if media keys are handled in order.
        sessionCallback.reset(2);
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        sendMediaKeyInputToController(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(sessionCallback.await(TIME_OUT_MS));
        assertEquals(1, sessionCallback.mOnPlayCalledCount);
        assertTrue(sessionCallback.mOnStopCalled);
        synchronized (mWaitLock) {
            assertEquals(PlaybackStateCompat.STATE_STOPPED,
                    mSession.getController().getPlaybackState().getState());
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

    /**
     * Tests {@link MediaSessionCompat.QueueItem}.
     */
    @Test
    @SmallTest
    public void testQueueItem() {
        MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId("media-id")
                        .setTitle("title")
                        .build(),
                TEST_QUEUE_ID);
        assertEquals(TEST_QUEUE_ID, item.getQueueId());
        assertEquals("media-id", item.getDescription().getMediaId());
        assertEquals("title", item.getDescription().getTitle());
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
     * Verifies that a new session hasn't had any configuration bits set yet.
     *
     * @param controller The controller for the session
     */
    private void verifyNewSession(MediaControllerCompat controller, String tag) {
        assertEquals("New session has unexpected configuration", 0L, controller.getFlags());
        assertNull("New session has unexpected configuration", controller.getExtras());
        assertNull("New session has unexpected configuration", controller.getMetadata());
        assertEquals("New session has unexpected configuration",
                getContext().getPackageName(), controller.getPackageName());
        assertNull("New session has unexpected configuration", controller.getPlaybackState());
        assertNull("New session has unexpected configuration", controller.getQueue());
        assertNull("New session has unexpected configuration", controller.getQueueTitle());
        assertEquals("New session has unexpected configuration", RatingCompat.RATING_NONE,
                controller.getRatingType());
        assertNull("New session has unexpected configuration", controller.getSessionActivity());

        assertNotNull(controller.getSessionToken());
        assertNotNull(controller.getTransportControls());

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                info.getPlaybackType());
        assertEquals(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                info.getCurrentVolume());
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

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        private volatile boolean mOnPlaybackStateChangedCalled;
        private volatile boolean mOnMetadataChangedCalled;
        private volatile boolean mOnQueueChangedCalled;
        private volatile boolean mOnQueueTitleChangedCalled;
        private volatile boolean mOnExtraChangedCalled;
        private volatile boolean mOnAudioInfoChangedCalled;
        private volatile boolean mOnSessionDestroyedCalled;
        private volatile boolean mOnSessionEventCalled;
        private volatile boolean mOnCaptioningEnabledChangedCalled;
        private volatile boolean mOnRepeatModeChangedCalled;
        private volatile boolean mOnShuffleModeChangedCalled;

        private volatile PlaybackStateCompat mPlaybackState;
        private volatile MediaMetadataCompat mMediaMetadata;
        private volatile List<MediaSessionCompat.QueueItem> mQueue;
        private volatile CharSequence mTitle;
        private volatile String mEvent;
        private volatile Bundle mExtras;
        private volatile MediaControllerCompat.PlaybackInfo mPlaybackInfo;
        private volatile boolean mCaptioningEnabled;
        private volatile int mRepeatMode;
        private volatile int mShuffleMode;

        public void resetLocked() {
            mOnPlaybackStateChangedCalled = false;
            mOnMetadataChangedCalled = false;
            mOnQueueChangedCalled = false;
            mOnQueueTitleChangedCalled = false;
            mOnExtraChangedCalled = false;
            mOnAudioInfoChangedCalled = false;
            mOnSessionDestroyedCalled = false;
            mOnSessionEventCalled = false;
            mOnRepeatModeChangedCalled = false;
            mOnShuffleModeChangedCalled = false;

            mPlaybackState = null;
            mMediaMetadata = null;
            mQueue = null;
            mTitle = null;
            mExtras = null;
            mPlaybackInfo = null;
            mCaptioningEnabled = false;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (mWaitLock) {
                mOnPlaybackStateChangedCalled = true;
                mPlaybackState = state;
                mWaitLock.notify();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (mWaitLock) {
                mOnMetadataChangedCalled = true;
                mMediaMetadata = metadata;
                mWaitLock.notify();
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            synchronized (mWaitLock) {
                mOnQueueChangedCalled = true;
                mQueue = queue;
                mWaitLock.notify();
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            synchronized (mWaitLock) {
                mOnQueueTitleChangedCalled = true;
                mTitle = title;
                mWaitLock.notify();
            }
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            synchronized (mWaitLock) {
                mOnExtraChangedCalled = true;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
            synchronized (mWaitLock) {
                mOnAudioInfoChangedCalled = true;
                mPlaybackInfo = info;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSessionDestroyed() {
            synchronized (mWaitLock) {
                mOnSessionDestroyedCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            synchronized (mWaitLock) {
                mOnSessionEventCalled = true;
                mEvent = event;
                mExtras = (Bundle) extras.clone();
                mWaitLock.notify();
            }
        }

        @Override
        public void onCaptioningEnabledChanged(boolean enabled) {
            synchronized (mWaitLock) {
                mOnCaptioningEnabledChangedCalled = true;
                mCaptioningEnabled = enabled;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            synchronized (mWaitLock) {
                mOnRepeatModeChangedCalled = true;
                mRepeatMode = repeatMode;
                mWaitLock.notify();
            }
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            synchronized (mWaitLock) {
                mOnShuffleModeChangedCalled = true;
                mShuffleMode = shuffleMode;
                mWaitLock.notify();
            }
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private CountDownLatch mLatch;
        private int mOnPlayCalledCount;
        private boolean mOnPauseCalled;
        private boolean mOnStopCalled;
        private boolean mOnFastForwardCalled;
        private boolean mOnRewindCalled;
        private boolean mOnSkipToPreviousCalled;
        private boolean mOnSkipToNextCalled;

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
            mOnPlayCalledCount = 0;
            mOnPauseCalled = false;
            mOnStopCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSkipToPreviousCalled = false;
            mOnSkipToNextCalled = false;
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
    }
}
