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

package androidx.media.test.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media.test.service.MediaTestUtils;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaController2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.SessionCallback;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlayer2;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession2Test extends MediaSession2TestBase {

    private static final String TAG = "MediaSession2Test";

    private MediaSession2 mSession;
    private MockPlayer mPlayer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);

        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setId(TAG)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
    }

    @Test
    public void testBuilder() {
        prepareLooper();
        MediaSession2.Builder builder;
        try {
            builder = new MediaSession2.Builder(mContext, null);
            fail("null player shouldn't be allowed");
        } catch (IllegalArgumentException e) {
            // expected. pass-through
        }
        try {
            builder = new MediaSession2.Builder(mContext, mPlayer);
            builder.setId(null);
            fail("null id shouldn't be allowed");
        } catch (IllegalArgumentException e) {
            // expected. pass-through
        }
    }

    @Test
    public void testGetDuration() throws Exception {
        prepareLooper();
        final long testDuration = 9999;
        mPlayer.mLastPlayerState = SessionPlayer2.PLAYER_STATE_PAUSED;
        mPlayer.mDuration = testDuration;
        assertEquals(testDuration, mSession.getPlayer().getDuration());
    }

    @Test
    public void testGetPlaybackSpeed() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mPlayer.mLastPlayerState = SessionPlayer2.PLAYER_STATE_PAUSED;
        mPlayer.setPlaybackSpeed(speed);
        assertEquals(speed, mSession.getPlayer().getPlaybackSpeed(), 0.0f);
    }

    @Test
    public void testGetPlayerState() {
        prepareLooper();
        final int state = SessionPlayer2.PLAYER_STATE_PLAYING;
        mPlayer.mLastPlayerState = state;
        assertEquals(state, mSession.getPlayer().getPlayerState());
    }

    @Test
    public void testGetBufferingState() {
        prepareLooper();
        final int bufferingState = SessionPlayer2.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        mPlayer.mLastBufferingState = bufferingState;
        assertEquals(bufferingState, mSession.getPlayer().getBufferingState());
    }

    @Test
    public void testGetPosition() {
        prepareLooper();
        final long position = 150000;
        mPlayer.mCurrentPosition = position;
        mPlayer.mLastPlayerState = SessionPlayer2.PLAYER_STATE_PAUSED;
        assertEquals(position, mSession.getPlayer().getCurrentPosition());
    }

    @Test
    public void testGetBufferedPosition() {
        prepareLooper();
        final long bufferedPosition = 900000;
        mPlayer.mBufferedPosition = bufferedPosition;
        mPlayer.mLastPlayerState = SessionPlayer2.PLAYER_STATE_PAUSED;
        assertEquals(bufferedPosition, mSession.getPlayer().getBufferedPosition());
    }

    @Test
    public void testGetCurrentMediaItem() {
        prepareLooper();
        MediaItem2 item = MediaTestUtils.createMediaItemWithMetadata();
        mPlayer.mCurrentMediaItem = item;
        assertEquals(item, mSession.getPlayer().getCurrentMediaItem());
    }

    @Test
    public void testGetPlaylist() {
        prepareLooper();
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(2);
        mPlayer.mPlaylist = list;
        assertEquals(list, mSession.getPlayer().getPlaylist());
    }

    @Test
    public void testGetPlaylistMetadata() {
        prepareLooper();
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        mPlayer.mMetadata = testMetadata;
        assertEquals(testMetadata, mSession.getPlayer().getPlaylistMetadata());
    }

    @Test
    public void testGetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer2.SHUFFLE_MODE_GROUP;
        mPlayer.setShuffleMode(testShuffleMode);
        assertEquals(testShuffleMode, mSession.getPlayer().getShuffleMode());
    }

    @Test
    public void testGetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer2.REPEAT_MODE_GROUP;
        mPlayer.setRepeatMode(testRepeatMode);
        assertEquals(testRepeatMode, mSession.getPlayer().getRepeatMode());
    }

    @Test
    public void testUpdatePlayer() throws Exception {
        prepareLooper();
        MockPlayer player = new MockPlayer(0);

        // Test if setPlayer doesn't crash with various situations.
        mSession.updatePlayer(mPlayer);
        assertEquals(mPlayer, mSession.getPlayer());

        mSession.updatePlayer(player);
        assertEquals(player, mSession.getPlayer());
    }

    /**
     * Test potential deadlock for calls between controller and session.
     */
    @Test
    @LargeTest
    public void testDeadlock() throws InterruptedException {
        prepareLooper();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mSession = null;
            }
        });

        // Two more threads are needed not to block test thread nor test wide thread (sHandler).
        final HandlerThread sessionThread = new HandlerThread("testDeadlock_session");
        final HandlerThread testThread = new HandlerThread("testDeadlock_test");
        sessionThread.start();
        testThread.start();
        final SyncHandler sessionHandler = new SyncHandler(sessionThread.getLooper());
        final Handler testHandler = new Handler(testThread.getLooper());
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            final MockPlayer player = new MockPlayer(0);
            sessionHandler.postAndSync(new Runnable() {
                @Override
                public void run() {
                    mSession = new MediaSession2.Builder(mContext, mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final RemoteMediaController2 controller = createRemoteController2(mSession.getToken());
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int state = SessionPlayer2.PLAYER_STATE_ERROR;
                    for (int i = 0; i < 100; i++) {
                        Log.d(TAG, "testDeadlock for-loop started: index=" + i);
                        long startTime = SystemClock.elapsedRealtime();

                        // triggers call from session to controller.
                        player.notifyPlayerStateChanged(state);
                        long endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "1) Time spent on API call(ms): " + (endTime - startTime));

                        // triggers call from controller to session.
                        startTime = endTime;
                        controller.play();
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "2) Time spent on API call(ms): " + (endTime - startTime));

                        // Repeat above
                        startTime = endTime;
                        player.notifyPlayerStateChanged(state);
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "3) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        controller.pause();
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "4) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        player.notifyPlayerStateChanged(state);
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "5) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        controller.seekTo(0);
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "6) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        player.notifyPlayerStateChanged(state);
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "7) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        controller.skipToNextItem();
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "8) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        player.notifyPlayerStateChanged(state);
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "9) Time spent on API call(ms): " + (endTime - startTime));

                        startTime = endTime;
                        controller.skipToPreviousItem();
                        endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "10) Time spent on API call(ms): " + (endTime - startTime));
                    }
                    // This may hang if deadlock happens.
                    latch.countDown();
                }
            });
            assertTrue(latch.await(3, TimeUnit.SECONDS));
        } finally {
            if (mSession != null) {
                sessionHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        // Clean up here because sessionHandler will be removed afterwards.
                        mSession.close();
                        mSession = null;
                    }
                });
            }

            if (Build.VERSION.SDK_INT >= 18) {
                sessionThread.quitSafely();
                testThread.quitSafely();
            } else {
                sessionThread.quit();
                testThread.quit();
            }
        }
    }

    @Test
    public void testCreatingTwoSessionWithSameId() {
        prepareLooper();
        final String sessionId = "testSessionId";
        MediaSession2 session = new MediaSession2.Builder(mContext, new MockPlayer(0))
                .setId(sessionId)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {})
                .build();

        MediaSession2.Builder builderWithSameId =
                new MediaSession2.Builder(mContext, new MockPlayer(0));
        try {
            builderWithSameId.setId(sessionId)
                    .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {})
                    .build();
            fail("Creating a new session with the same ID in a process should not be allowed");
        } catch (IllegalArgumentException e) {
            // expected. pass-through
        }

        session.close();
        // Creating a new session with ID of the closed session is okay.
        MediaSession2 sessionWithSameId = builderWithSameId.build();
        sessionWithSameId.close();
    }
}
