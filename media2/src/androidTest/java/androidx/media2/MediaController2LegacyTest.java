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

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.testutils.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController2}.
 */
// TODO(jaewan): Implement host-side test so controller and session can run in different processes.
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
// TODO(jaeawn): Revisit create/close session in the sHandler. It's no longer necessary.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaController2LegacyTest extends MediaSession2TestBase {
    private static final String TAG = "MediaController2Test";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;

    PendingIntent mIntent;
    MediaSessionCompat mSession;
    MediaSessionCallback mSessionCallback;
    AudioManager mAudioManager;
    MediaController2 mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession2 to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);

        mSessionCallback = new MediaSessionCallback();
        mSession = new MediaSessionCompat(mContext, TAG + "Compat");
        mSession.setCallback(mSessionCallback, sHandler);
        mSession.setSessionActivity(mIntent);
        mSession.setActive(true);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        TestServiceRegistry.getInstance().setHandler(sHandler);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }

        if (mController != null) {
            mController.close();
            mController = null;
        }
        TestServiceRegistry.getInstance().cleanUp();
    }

    private void createControllerAndWaitConnection() throws Exception {
        createControllerAndWaitConnection(new ControllerCallback() {});
    }

    private void createControllerAndWaitConnection(final ControllerCallback callback)
            throws Exception {
        final MockControllerCallback testControllerCallback = new MockControllerCallback(callback);
        SessionToken2.createSessionToken2(mContext, mSession.getSessionToken(),
                sHandlerExecutor, new SessionToken2.OnSessionToken2CreatedListener() {
                    @Override
                    public void onSessionToken2Created(
                            MediaSessionCompat.Token token, SessionToken2 token2) {
                        assertTrue(token2.isLegacySession());
                        mController = new MediaController2(mContext, token2, sHandlerExecutor,
                                testControllerCallback);
                    }
                });

        if (mController == null) {
            testControllerCallback.waitForConnect(true);
        }
    }

    @Test
    public void testPlay() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.play();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(1, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void testPause() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.pause();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPauseCalled);
    }

    @Test
    public void testReset() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.reset();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnStopCalled);
    }

    @Test
    public void testPrepare() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.prepare();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void testSeekTo() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        final long seekPosition = 12125L;
        mController.seekTo(seekPosition);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSeekToCalled);
    }

    @Test
    public void testGettersAfterConnected() throws Exception {
        prepareLooper();

        final long position = 150000;
        final long bufferedPosition = 900000;
        final long timeDiff = 102;
        final float speed = 0.5f;
        final MediaMetadataCompat metadata = MediaUtils2.convertToMediaMetadataCompat(
                TestUtils.createMetadata());

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, position, speed)
                .setBufferedPosition(bufferedPosition)
                .build());
        mSession.setMetadata(metadata);
        createControllerAndWaitConnection();
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
    public void testGetSessionActivity() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        PendingIntent sessionActivity = mController.getSessionActivity();
        assertEquals(mContext.getPackageName(), sessionActivity.getCreatorPackage());
        assertEquals(Process.myUid(), sessionActivity.getCreatorUid());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController2, List, MediaMetadata2)}.
     */
    @Test
    public void testGetPlaylist() throws Exception {
        prepareLooper();
        final List<MediaItem2> testList = TestUtils.createPlaylist(2);
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
        createControllerAndWaitConnection(callback);

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
        createControllerAndWaitConnection(callback);
        mSession.setQueueTitle(queueTitle);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(metadataFromCallback.get(), mController.getPlaylistMetadata());
    }

    //@Test see: b/110738672
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
        createControllerAndWaitConnection(callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testSeekPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    //@Test see: b/110738672
    public void testControllerCallbackBufferingCompleted() throws Exception {
        prepareLooper();
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(1);
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
        createControllerAndWaitConnection(callback);
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
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(1);
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
        createControllerAndWaitConnection(callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(testBufferingPosition)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    //@Test see: b/110738672
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
        createControllerAndWaitConnection(callback);
        mController.setTimeDiff(Long.valueOf(0));
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAddPlaylistItem() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final List<MediaItem2> testList = TestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testList);
        final MediaItem2 testMediaItem2ToAdd = TestUtils.createMediaItemWithMetadata();
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        mSessionCallback.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        createControllerAndWaitConnection(callback);

        mController.addPlaylistItem(1, testMediaItem2ToAdd);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mOnAddQueueItemAtCalled);
        assertEquals(testList.get(0).getMediaId(), listFromCallback.get().get(0).getMediaId());
        assertEquals(testMediaItem2ToAdd.getMediaId(), listFromCallback.get().get(1).getMediaId());
        assertEquals(testList.get(1).getMediaId(), listFromCallback.get().get(2).getMediaId());
    }

    @Test
    public void testRemovePlaylistItem() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final List<MediaItem2> testList = TestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testList);
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        mSessionCallback.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        createControllerAndWaitConnection(callback);

        mController.removePlaylistItem(testList.get(0));
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mOnRemoveQueueItemCalled);
        assertEquals(testList.get(1).getMediaId(), listFromCallback.get().get(0).getMediaId());
    }

    @Test
    public void testReplacePlaylistItem() throws Exception {
        prepareLooper();
        // replace = remove + add
        final CountDownLatch latch = new CountDownLatch(2);
        final List<MediaItem2> testList = TestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils2.convertToQueueItemList(testList);
        final MediaItem2 testMediaItem2ToReplace = TestUtils.createMediaItemWithMetadata();
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        mSessionCallback.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        createControllerAndWaitConnection(callback);

        mController.replacePlaylistItem(1, testMediaItem2ToReplace);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCallback.mOnRemoveQueueItemCalled);
        assertTrue(mSessionCallback.mOnAddQueueItemAtCalled);
        assertEquals(testList.get(0).getMediaId(), listFromCallback.get().get(0).getMediaId());
        assertEquals(testMediaItem2ToReplace.getMediaId(),
                listFromCallback.get().get(1).getMediaId());
    }

    @Test
    public void testSkipToPreviousItem() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.skipToPreviousItem();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToPreviousCalled);
    }

    @Test
    public void testSkipToNextItem() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.skipToNextItem();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToNextCalled);
    }

    //@Test see: b/110738672
    public void testSkipToPlaylistItem() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        final long queueItemId = 1;
        final QueueItem queueItem = new QueueItem(
                MediaUtils2.convertToMediaMetadataCompat(TestUtils.createMetadata())
                        .getDescription(),
                queueItemId);
        final MediaItem2 mediaItem2 = MediaUtils2.convertToMediaItem2(queueItem);

        mController.skipToPlaylistItem(mediaItem2);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToQueueItemCalled);
        assertEquals(queueItemId, mSessionCallback.mQueueItemId);
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController2, int)}.
     */
    @Test
    public void testSetAndGetShuffleMode() throws Exception {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        mSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
        createControllerAndWaitConnection(callback);
        mSessionCallback.reset(1);

        mController.setShuffleMode(testShuffleMode);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetShuffleModeCalled);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, mController.getShuffleMode());
    }

    @Test
    public void testSetAndGetRepeatMode() throws Exception {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_ALL;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        mSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
        createControllerAndWaitConnection(callback);
        mSessionCallback.reset(1);

        mController.setRepeatMode(testRepeatMode);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetRepeatModeCalled);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, mController.getRepeatMode());
    }

    @Test
    public void testSetVolumeTo() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        TestVolumeProvider volumeProvider =
                new TestVolumeProvider(volumeControlType, maxVolume, currentVolume);
        mSession.setPlaybackToRemote(volumeProvider);
        createControllerAndWaitConnection();

        final int targetVolume = 50;
        mController.setVolumeTo(targetVolume, 0 /* flags */);
        assertTrue(volumeProvider.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(volumeProvider.mSetVolumeToCalled);
        assertEquals(targetVolume, volumeProvider.mVolume);
    }

    @Test
    public void testAdjustVolume() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        TestVolumeProvider volumeProvider =
                new TestVolumeProvider(volumeControlType, maxVolume, currentVolume);
        mSession.setPlaybackToRemote(volumeProvider);
        createControllerAndWaitConnection();

        final int direction = AudioManager.ADJUST_RAISE;
        mController.adjustVolume(direction, 0 /* flags */);
        assertTrue(volumeProvider.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(volumeProvider.mAdjustVolumeCalled);
        assertEquals(direction, volumeProvider.mDirection);
    }

    @Test
    public void testSetVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }
        // Set stream of the session.
        mSession.setPlaybackToLocal(stream);
        createControllerAndWaitConnection();
        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;

        mController.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(WAIT_TIME_MS) {
            @Override
            protected boolean check() {
                return targetVolume == mAudioManager.getStreamVolume(stream);
            }
        }.run();

        // Set back to original volume.
        mAudioManager.setStreamVolume(stream, originalVolume, 0 /* flags */);
    }

    @Test
    public void testAdjustVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }
        // Set stream of the session.
        mSession.setPlaybackToLocal(stream);
        createControllerAndWaitConnection();

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;

        mController.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(WAIT_TIME_MS) {
            @Override
            protected boolean check() {
                return targetVolume == mAudioManager.getStreamVolume(stream);
            }
        }.run();

        // Set back to original volume.
        mAudioManager.setStreamVolume(stream, originalVolume, 0 /* flags */);
    }

    @Test
    public void testGetPackageName() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        assertEquals(mContext.getPackageName(), mController.getSessionToken().getPackageName());
    }

    @Test
    public void testSendCustomCommand() throws Exception {
        prepareLooper();
        final String command = "test_custom_command";
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "test_args");
        final SessionCommand2 testCommand = new SessionCommand2(command, null);
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.sendCustomCommand(testCommand, testArgs, null);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnCommandCalled);
        assertEquals(command, mSessionCallback.mCommand);
        assertTrue(TestUtils.equals(testArgs, mSessionCallback.mExtras));
    }

    @Test
    public void testControllerCallback_onConnected() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onConnected(MediaController2 controller,
                    SessionCommandGroup2 allowedCommands) {
                latch.countDown();
            }
        };
        createControllerAndWaitConnection(callback);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_releaseSession() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onDisconnected(MediaController2 controller) {
                latch.countDown();
            }
        };
        createControllerAndWaitConnection(callback);

        mSession.release();
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_close() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onDisconnected(MediaController2 controller) {
                latch.countDown();
            }
        };
        createControllerAndWaitConnection(callback);

        mController.close();
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFastForward() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.fastForward();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnFastForwardCalled);
    }

    @Test
    public void testRewind() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.rewind();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnRewindCalled);
    }

    @Test
    public void testPlayFromSearch() throws Exception {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.playFromSearch(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPlayFromSearchCalled);
        assertEquals(request, mSessionCallback.mQuery);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    @Test
    public void testPlayFromUri() throws Exception {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.playFromUri(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPlayFromUriCalled);
        assertEquals(request, mSessionCallback.mUri);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    @Test
    public void testPlayFromMediaId() throws Exception {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.playFromMediaId(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPlayFromMediaIdCalled);
        assertEquals(request, mSessionCallback.mMediaId);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    //@Test see: b/110738672
    public void testPrepareFromSearch() throws Exception {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.prepareFromSearch(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPrepareFromSearchCalled);
        assertEquals(request, mSessionCallback.mQuery);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    @Test
    public void testPrepareFromUri() throws Exception {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.prepareFromUri(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPrepareFromUriCalled);
        assertEquals(request, mSessionCallback.mUri);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    @Test
    public void testPrepareFromMediaId() throws Exception {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.prepareFromMediaId(request, bundle);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(request, mSessionCallback.mMediaId);
        assertTrue(TestUtils.equals(bundle, mSessionCallback.mExtras));
    }

    @Test
    public void testSetRating() throws Exception {
        prepareLooper();
        final int ratingType = Rating2.RATING_5_STARS;
        final float ratingValue = 3.5f;
        final Rating2 rating2 = Rating2.newStarRating(ratingType, ratingValue);
        final String mediaId = "media_id";
        final MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_MEDIA_ID, mediaId).build();
        mSession.setMetadata(metadata);
        createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        mController.setRating(mediaId, rating2);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetRatingCalled);
        assertEquals(rating2, MediaUtils2.convertToRating2(mSessionCallback.mRating));
    }

    @Test
    public void testIsConnected() throws Exception {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onDisconnected(MediaController2 controller) {
                latch.countDown();
            }
        };
        createControllerAndWaitConnection(callback);

        assertTrue(mController.isConnected());
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.release();
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mController.isConnected());
    }

//    @Test
//    public void testGetServiceToken() {
//        prepareLooper();
//        SessionToken2 token = TestUtils.getServiceToken(mContext, MockMediaSessionService2.ID);
//        assertNotNull(token);
//        assertEquals(mContext.getPackageName(), token.getPackageName());
//        assertEquals(MockMediaSessionService2.ID, token.getId());
//        assertEquals(SessionToken2.TYPE_SESSION_SERVICE, token.getType());
//    }
//
//    @Ignore
//    @Test
//    public void testConnectToService_sessionService() throws Exception {
//        prepareLooper();
//        testConnectToService(MockMediaSessionService2.ID);
//    }
//
//    @Ignore
//    @Test
//    public void testConnectToService_libraryService() throws Exception {
//        prepareLooper();
//        testConnectToService(MockMediaLibraryService2.ID);
//    }
//
//    public void testConnectToService(String id) throws Exception {
//        prepareLooper();
//        final CountDownLatch latch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
//            @Override
//            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
//                    @NonNull ControllerInfo controller) {
//                if (Process.myUid() == controller.getUid()) {
//                    if (mSession != null) {
//                        mSession.close();
//                    }
//                    mSession = session;
//                    mPlayer = (MockPlayer) session.getPlayerConnector();
//                    assertEquals(mContext.getPackageName(), controller.getPackageName());
//                    assertFalse(controller.isTrusted());
//                    latch.countDown();
//                }
//                return super.onConnect(session, controller);
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);
//
//        final SessionCommand2 testCommand = new SessionCommand2("testConnectToService", null);
//        final CountDownLatch controllerLatch = new CountDownLatch(1);
//        mController = createController(TestUtils.getServiceToken(mContext, id), true,
//                new ControllerCallback() {
//                    @Override
//                    public void onCustomCommand(MediaController2 controller,
//                            SessionCommand2 command, Bundle args, ResultReceiver receiver) {
//                        if (testCommand.equals(command)) {
//                            controllerLatch.countDown();
//                        }
//                    }
//                }
//        );
//        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//
//        // Test command from controller to session service.
//        mController.play();
//        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        assertTrue(mPlayer.mPlayCalled);
//
//        // Test command from session service to controller.
//        mSession.sendCustomCommand(testCommand, null);
//        assertTrue(controllerLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testClose_beforeConnected() throws InterruptedException {
//        prepareLooper();
//        MediaController2 controller =
//                createController(mSession.getToken(), false, null);
//        controller.close();
//    }

    @Test
    public void testClose_twice() throws Exception {
        prepareLooper();
        createControllerAndWaitConnection();
        mController.close();
        mController.close();
    }

//    @Ignore
//    @Test
//    public void testClose_sessionService() throws Exception {
//        prepareLooper();
//        testCloseFromService(MockMediaSessionService2.ID);
//    }
//
//    @Ignore
//    @Test
//    public void testClose_libraryService() throws Exception {
//        prepareLooper();
//        testCloseFromService(MockMediaLibraryService2.ID);
//    }

    private void setPlaybackState(int state) {
        final long allActions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_FAST_FORWARD | PlaybackStateCompat.ACTION_REWIND;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder().setActions(allActions)
                .setState(state, 0L, 0.0f).build();
        mSession.setPlaybackState(playbackState);
    }

    class TestVolumeProvider extends VolumeProviderCompat {
        final CountDownLatch mLatch = new CountDownLatch(1);
        boolean mSetVolumeToCalled;
        boolean mAdjustVolumeCalled;
        int mVolume;
        int mDirection;

        TestVolumeProvider(int controlType, int maxVolume, int currentVolume) {
            super(controlType, maxVolume, currentVolume);
        }

        @Override
        public void onSetVolumeTo(int volume) {
            mSetVolumeToCalled = true;
            mVolume = volume;
            mLatch.countDown();
        }

        @Override
        public void onAdjustVolume(int direction) {
            mAdjustVolumeCalled = true;
            mDirection = direction;
            mLatch.countDown();
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private CountDownLatch mLatch = new CountDownLatch(1);
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
            mSession.setRepeatMode(repeatMode);
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
            mSession.setShuffleMode(shuffleMode);
            mLatch.countDown();
        }

        void setQueue(List<QueueItem> queue) {
            mQueue = queue;
            mSession.setQueue(queue);
        }
    }
}
