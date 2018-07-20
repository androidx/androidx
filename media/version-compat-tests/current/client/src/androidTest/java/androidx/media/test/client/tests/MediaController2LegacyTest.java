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

package androidx.media.test.client.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.test.client.MediaTestUtils;
import androidx.media.test.client.RemoteMediaSessionCompat;
import androidx.media.test.lib.MockActivity;
import androidx.media2.MediaController2;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaUtils2;
import androidx.media2.SessionToken2;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Tests {@link MediaController2} interacting with {@link MediaSessionCompat}.
 */
@SmallTest
public class MediaController2LegacyTest extends MediaSession2TestBase {

    AudioManager mAudioManager;
    RemoteMediaSessionCompat mSession;

    SessionToken2 mToken2;
    MediaController2 mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSession = new RemoteMediaSessionCompat(DEFAULT_TEST_NAME, mContext);
        createMediaSessionToken2();
    }

    private void createMediaSessionToken2() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        SessionToken2.createSessionToken2(mContext, mSession.getSessionToken(),
                sHandlerExecutor, new SessionToken2.OnSessionToken2CreatedListener() {
                    @Override
                    public void onSessionToken2Created(
                            MediaSessionCompat.Token token, SessionToken2 token2) {
                        assertTrue(token2.isLegacySession());
                        mToken2 = token2;
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.cleanUp();
        if (mController != null) {
            mController.close();
        }
    }

    @Test
    public void testGettersAfterConnected() throws Exception {
        prepareLooper();
        final long position = 150000;
        final long bufferedPosition = 900000;
        final long timeDiff = 102;
        final float speed = 0.5f;
        final MediaMetadataCompat metadata = MediaUtils2.convertToMediaMetadataCompat(
                MediaTestUtils.createMetadata());

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, position, speed)
                .setBufferedPosition(bufferedPosition)
                .build());
        mSession.setMetadata(metadata);

        mController = createController(mToken2);
        mController.setTimeDiff(timeDiff);

        assertEquals(MediaPlayerConnector.PLAYER_STATE_PLAYING, mController.getPlayerState());
        assertEquals(MediaPlayerConnector.BUFFERING_STATE_BUFFERING_COMPLETE,
                mController.getBufferingState());
        assertEquals(bufferedPosition, mController.getBufferedPosition());
        assertEquals(speed, mController.getPlaybackSpeed(), 0.0f);
        assertEquals((double) position + (speed * timeDiff),
                (double) mController.getCurrentPosition(), 100.0 /* 100 ms */);
        assertEquals(metadata.getDescription().getMediaId(),
                mController.getCurrentMediaItem().getMediaId());
    }

    @Test
    public void testGetPackageName() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
        assertEquals(SERVICE_PACKAGE_NAME, mController.getSessionToken().getPackageName());
    }

    @Test
    public void testGetSessionActivity() throws Exception {
        prepareLooper();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        PendingIntent pi = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);
        mSession.setSessionActivity(pi);

        mController = createController(mToken2);
        PendingIntent sessionActivityOut = mController.getSessionActivity();
        assertEquals(mContext.getPackageName(), sessionActivityOut.getCreatorPackage());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController2, List, MediaMetadata2)}.
     */
    @Test
    public void testGetPlaylist() throws Exception {
        prepareLooper();
        final List<MediaItem2> testList = MediaTestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testList);
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                assertEquals(testList.size(), playlist.size());
                for (int i = 0; i < playlist.size(); i++) {
                    assertEquals(testList.get(i).getMediaId(), playlist.get(i).getMediaId());
                }
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        mController = createController(mToken2, true, callback);

        mSession.setQueue(testQueue);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(listFromCallback.get(), mController.getPlaylist());
    }

    @Test
    public void testGetPlaylistMetadata() throws Exception {
        prepareLooper();
        final AtomicReference<MediaMetadata2> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final CharSequence queueTitle = "test queue title";
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(MediaController2 controller,
                    MediaMetadata2 metadata) {
                assertEquals(queueTitle.toString(),
                        metadata.getString(MediaMetadata2.METADATA_KEY_TITLE));
                metadataFromCallback.set(metadata);
                latch.countDown();
            }
        };
        mController = createController(mToken2, true, callback);

        mSession.setQueueTitle(queueTitle);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(metadataFromCallback.get(), mController.getPlaylistMetadata());
    }

    @Test
    @Ignore("b/110738672")
    public void testControllerCallback_onSeekCompleted() throws Exception {
        prepareLooper();
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onSeekCompleted(MediaController2 controller, long position) {
                assertEquals(testSeekPosition, position);
                latch.countDown();
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        mController = createController(mToken2, true, callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testSeekPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @Ignore("b/110738672")
    public void testControllerCallbackBufferingCompleted() throws Exception {
        prepareLooper();
        final List<MediaItem2> testPlaylist = MediaTestUtils.createPlaylist(1);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testPlaylist);
        final MediaMetadataCompat metadata = MediaUtils2.convertToMediaMetadataCompat(
                testQueue.get(0).getDescription());

        final int testBufferingState = MediaPlayerConnector.BUFFERING_STATE_BUFFERING_COMPLETE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                assertEquals(metadata.getDescription().getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        mSession.setMetadata(metadata);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(0)
                .build());
        mController = createController(mToken2, true, callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(testBufferingPosition)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallbackBufferingStarved() throws Exception {
        prepareLooper();
        final List<MediaItem2> testPlaylist = MediaTestUtils.createPlaylist(1);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testPlaylist);
        final MediaMetadataCompat metadata = MediaUtils2.convertToMediaMetadataCompat(
                testQueue.get(0).getDescription());

        final int testBufferingState = MediaPlayerConnector.BUFFERING_STATE_BUFFERING_AND_STARVED;
        final long testBufferingPosition = 0;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                assertEquals(metadata.getDescription().getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        mSession.setMetadata(metadata);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 100 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(500)
                .build());
        mController = createController(mToken2, true, callback);
        mController.setTimeDiff(0L);

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(testBufferingPosition)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @Ignore("b/110738672")
    public void testControllerCallback_onPlayerStateChanged() throws Exception {
        prepareLooper();
        final int testPlayerState = MediaPlayerConnector.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0 /* position */,
                        1f /* playbackSpeed */)
                .build());
        mController = createController(mToken2, true, callback);
        mController.setTimeDiff(Long.valueOf(0));
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_onConnected() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
    }

    @Test
    public void testControllerCallback_releaseSession() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
        mSession.release();
        waitForDisconnect(mController, true);
    }

    @Test
    public void testControllerCallback_close() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
        mController.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void testClose_twice() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
        mController.close();
        mController.close();
    }

    @Test
    public void testIsConnected() throws Exception {
        prepareLooper();
        mController = createController(mToken2);
        assertTrue(mController.isConnected());

        mSession.release();
        waitForDisconnect(mController, true);
        assertFalse(mController.isConnected());
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mToken2, false, null);

        // Should not crash.
        controller.close();
    }
}
