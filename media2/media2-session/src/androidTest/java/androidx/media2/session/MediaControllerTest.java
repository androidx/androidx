/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static android.media.MediaFormat.MIMETYPE_TEXT_CEA_608;

import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSession.SessionCallback;
import androidx.media2.session.TestServiceRegistry.SessionServiceCallback;
import androidx.media2.session.TestUtils.SyncHandler;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.testutils.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController}.
 */
// TODO(jaewan): Implement host-side test so controller and session can run in different processes.
// TODO(jaewan): Fix flaky failure -- see MediaControllerImpl.getController()
// TODO(jaeawn): Revisit create/close session in the sHandler. It's no longer necessary.
@RunWith(AndroidJUnit4.class)
@LargeTest
@FlakyTest
public class MediaControllerTest extends MediaSessionTestBase {
    private static final String TAG = "MediaControllerTest";
    private static final long VOLUME_CHANGE_TIMEOUT_MS = 5000L;

    PendingIntent mIntent;
    MediaSession mSession;
    MediaController mController;
    MockPlayer mPlayer;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);

        mPlayer = new MockPlayer(1);
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller, @NonNull String mediaId) {
                        return TestUtils.createMediaItem(mediaId);
                    }
                })
                .setSessionActivity(mIntent)
                .setId(TAG).build();
        mController = createController(mSession.getToken());
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        TestServiceRegistry.getInstance().setHandler(sHandler);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }

        if (mController != null) {
            mController.close();
            mController = null;
        }
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Test
    public void play() throws Exception {
        SessionResult result = mController.play().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void play_autoPrepare() throws Exception {
        final MockPlayer player = new MockPlayer(2);
        player.mLastPlayerState = SessionPlayer.PLAYER_STATE_IDLE;
        mSession.updatePlayer(player);
        SessionResult result = mController.play().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(player.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(player.mPlayCalled);
        assertTrue(player.mPrepareCalled);
    }

    @Test
    public void pause() throws Exception {
        SessionResult result = mController.pause().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void prepare() throws Exception {
        SessionResult result = mController.prepare().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void seekTo() throws Exception {
        final long seekPosition = 12125L;
        SessionResult result = mController.seekTo(seekPosition)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void gettersAfterConnected() throws InterruptedException {
        final int state = SessionPlayer.PLAYER_STATE_PLAYING;
        final int bufferingState = SessionPlayer.BUFFERING_STATE_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem currentMediaItem = TestUtils.createMediaItemWithMetadata();
        final int shuffleMode = SessionPlayer.SHUFFLE_MODE_ALL;
        final int repeatMode = SessionPlayer.REPEAT_MODE_ONE;

        mPlayer.mLastPlayerState = state;
        mPlayer.mLastBufferingState = bufferingState;
        mPlayer.mCurrentPosition = position;
        mPlayer.mBufferedPosition = bufferedPosition;
        mPlayer.mPlaybackSpeed = speed;
        mPlayer.mCurrentMediaItem = currentMediaItem;
        mPlayer.mShuffleMode = shuffleMode;
        mPlayer.mRepeatMode = repeatMode;

        MediaController controller = createController(mSession.getToken());
        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        assertEquals(currentMediaItem, controller.getCurrentMediaItem());
        assertEquals(shuffleMode, controller.getShuffleMode());
        assertEquals(repeatMode, controller.getRepeatMode());
    }

    @Test
    public void updatePlayer() throws InterruptedException {
        final int testState = SessionPlayer.PLAYER_STATE_PLAYING;
        final List<MediaItem> testPlaylist = TestUtils.createMediaItems(3);
        final AudioAttributesCompat testAudioAttributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_RING).build();
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_ALL;
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_ONE;
        final CountDownLatch latch = new CountDownLatch(5);
        mController = createController(mSession.getToken(), true, null, new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
                assertEquals(mController, controller);
                assertEquals(testState, state);
                latch.countDown();
            }

            @Override
            public void onPlaylistChanged(@NonNull MediaController controller, List<MediaItem> list,
                    MediaMetadata metadata) {
                assertEquals(mController, controller);
                assertEquals(testPlaylist, list);
                assertNull(metadata);
                latch.countDown();
            }

            @Override
            public void onPlaybackInfoChanged(@NonNull MediaController controller,
                    @NonNull PlaybackInfo info) {
                assertEquals(mController, controller);
                assertEquals(testAudioAttributes, info.getAudioAttributes());
                latch.countDown();
            }

            @Override
            public void onShuffleModeChanged(
                    @NonNull MediaController controller,
                    int shuffleMode) {
                assertEquals(mController, controller);
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }

            @Override
            public void onRepeatModeChanged(
                    @NonNull MediaController controller,
                    int repeatMode) {
                assertEquals(mController, controller);
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        });

        MockPlayer player = new MockPlayer(0);
        player.mLastPlayerState = testState;
        player.mAudioAttributes = testAudioAttributes;
        player.mPlaylist = testPlaylist;
        player.mShuffleMode = testShuffleMode;
        player.mRepeatMode = testRepeatMode;

        mSession.updatePlayer(player);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void getSessionActivity() {
        PendingIntent sessionActivity = mController.getSessionActivity();
        assertNotNull(sessionActivity);
        if (Build.VERSION.SDK_INT >= 17) {
            // PendingIntent#getCreatorPackage() is added in API 17.
            assertEquals(mContext.getPackageName(), sessionActivity.getCreatorPackage());
            assertEquals(Process.myUid(), sessionActivity.getCreatorUid());
        }
    }

    @Test
    public void setPlaylist() throws Exception {
        final List<String> list = TestUtils.createMediaIds(2);
        SessionResult result = mController.setPlaylist(list, null /* Metadata */)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetPlaylistCalled);
        assertNull(mPlayer.mMetadata);

        assertNotNull(mPlayer.mPlaylist);
        assertEquals(list.size(), mPlayer.mPlaylist.size());
        for (int i = 0; i < list.size(); i++) {
            // MediaController.setPlaylist does not ensure the equality of the items.
            assertEquals(list.get(i), mPlayer.mPlaylist.get(i).getMediaId());
        }
    }

    @Test
    public void setMediaItem() throws Exception {
        String mediaId = "testSetMediaItem";
        SessionResult result = mController.setMediaItem(mediaId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertNull(mPlayer.mMetadata);
        assertEquals(mediaId, mPlayer.mItem.getMediaId());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController, List, MediaMetadata)}.
     */
    @Test
    public void getPlaylist() throws InterruptedException {
        final List<MediaItem> testList = TestUtils.createMediaItems(2);
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    List<MediaItem> playlist, MediaMetadata metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.mPlaylist = testList;
        mPlayer.mMetadata = testMetadata;
        mPlayer.notifyPlaylistChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Ensures object equality
        assertEquals(listFromCallback.get(), controller.getPlaylist());
        TestUtils.assertMetadataEquals(testMetadata, controller.getPlaylistMetadata());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController, List, MediaMetadata)}.
     */
    @Test
    @LargeTest
    public void getPlaylist_withLongPlaylist() throws InterruptedException {
        final List<MediaItem> testList = TestUtils.createMediaItems(5000);
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    List<MediaItem> playlist, MediaMetadata metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.mPlaylist = testList;
        mPlayer.mMetadata = testMetadata;
        mPlayer.notifyPlaylistChanged();
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        // Ensures object equality
        assertEquals(listFromCallback.get(), controller.getPlaylist());
        TestUtils.assertMetadataEquals(testMetadata, controller.getPlaylistMetadata());
    }

    @Test
    public void updatePlaylistMetadata() throws Exception {
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        SessionResult result = mController.updatePlaylistMetadata(testMetadata)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertNotNull(mPlayer.mMetadata);
        assertEquals(testMetadata.getMediaId(), mPlayer.mMetadata.getMediaId());
    }

    @Test
    public void getPlaylistMetadata() throws InterruptedException {
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<MediaMetadata> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(@NonNull MediaController controller,
                    MediaMetadata metadata) {
                assertNotNull(testMetadata);
                assertEquals(testMetadata.getMediaId(), metadata.getMediaId());
                metadataFromCallback.set(metadata);
                latch.countDown();
            }
        };
        mPlayer.mMetadata = testMetadata;
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testGetPlaylistMetadata")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .build()) {
            MediaController controller = createController(session.getToken(), true, null, callback);
            mPlayer.notifyPlaylistMetadataChanged();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(metadataFromCallback.get().getMediaId(),
                    controller.getPlaylistMetadata().getMediaId());
        }
    }

    @Test
    public void setPlaybackSpeed() throws Exception {
        final float speed = 1.5f;
        SessionResult result = mController.setPlaybackSpeed(speed)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    /**
     * This also tests {@link MediaController.ControllerCallback#onPlaybackSpeedChanged(
     * MediaController, float)}.
     *
     * @throws InterruptedException
     */
    @Test
    public void getPlaybackSpeed() throws InterruptedException {
        final float speed = 1.5f;
        mPlayer.mPlaybackSpeed = speed;

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController controller =
                createController(mSession.getToken(), true, null, new ControllerCallback() {
                    @Override
                    public void onPlaybackSpeedChanged(@NonNull MediaController controller,
                            float speedOut) {
                        assertEquals(speed, speedOut, 0.0f);
                        latch.countDown();
                    }
                });

        mPlayer.notifyPlaybackSpeedChanged(speed);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
    }

    /**
     * Test whether {@link SessionPlayer#setPlaylist(List, MediaMetadata)} is notified
     * through the
     * {@link ControllerCallback#onPlaylistMetadataChanged(MediaController, MediaMetadata)}
     * if the controller doesn't have {@link SessionCommand#COMMAND_CODE_PLAYER_GET_PLAYLIST} but
     * {@link SessionCommand#COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA}.
     */
    @Test
    public void controllerCallback_onPlaylistMetadataChanged() throws InterruptedException {
        final MediaItem item = TestUtils.createMediaItemWithMetadata();
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(@NonNull MediaController controller,
                    MediaMetadata metadata) {
                assertNotNull(metadata);
                assertEquals(item.getMediaId(), metadata.getMediaId());
                latch.countDown();
            }
        };
        final SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    SessionCommandGroup commands = new SessionCommandGroup.Builder()
                            .addCommand(new SessionCommand(
                                    SessionCommand.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA))
                            .build();
                    return commands;
                }
                return super.onConnect(session, controller);
            }
        };
        mPlayer.mMetadata = item.getMetadata();
        mPlayer.mPlaylist = list;
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testControllerCallback_onPlaylistMetadataChanged")
                .setSessionCallback(sHandlerExecutor, sessionCallback)
                .build()) {
            MediaController controller = createController(session.getToken(), true, null, callback);
            mPlayer.notifyPlaylistChanged();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void controllerCallback_onSeekCompleted() throws InterruptedException {
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onSeekCompleted(@NonNull MediaController controller, long position) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, null,
                callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.mLastPlayerState = SessionPlayer.PLAYER_STATE_PAUSED;
        mPlayer.notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController#getBufferedPosition()}, and
     * {@link MediaController#getBufferingState()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void controllerCallback_onBufferingStateChanged() throws InterruptedException {
        final List<MediaItem> testPlaylist = TestUtils.createMediaItems(3);
        final MediaItem testItem = testPlaylist.get(0);
        final int testBufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(@NonNull MediaController controller,
                    @NonNull MediaItem item, int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testItem, item);
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, null,
                callback);
        mPlayer.mPlaylist = testPlaylist;
        mPlayer.mBufferedPosition = testBufferingPosition;
        mPlayer.notifyBufferingStateChanged(testItem, testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController#getPlayerState()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void controllerCallback_onPlayerStateChanged() throws InterruptedException {
        final int testPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, null,
                callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.notifyPlayerStateChanged(testPlayerState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController#getCurrentMediaItem()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void controllerCallback_onCurrentMediaItemChanged() throws InterruptedException {
        final int listSize = 5;
        final List<MediaItem> list = TestUtils.createMediaItems(listSize);
        mPlayer.mPlaylist = list;

        final int index = 3;
        final MediaItem currentItem = list.get(index);
        final MediaItem unknownItem = TestUtils.createMediaItemWithMetadata();
        final CountDownLatch latch = new CountDownLatch(3);
        final MediaController controller =
                createController(mSession.getToken(), true, null, new ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                            MediaItem item) {
                        switch ((int) latch.getCount()) {
                            case 3:
                                assertEquals(-1, controller.getCurrentMediaItemIndex());
                                assertEquals(unknownItem, item);
                                break;
                            case 2:
                                assertEquals(index, controller.getCurrentMediaItemIndex());
                                assertEquals(currentItem, item);
                                break;
                            case 1:
                                assertEquals(-1, controller.getCurrentMediaItemIndex());
                                assertNull(item);
                        }
                        latch.countDown();
                    }
                });

        // Player notifies with the unknown item. It's still OK.
        mPlayer.notifyCurrentMediaItemChanged(unknownItem);
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Known DSD should be notified through the onCurrentMediaItemChanged.
        mPlayer.mIndex = index;
        mPlayer.mCurrentMediaItem = mPlayer.mItem = mPlayer.mPlaylist.get(index);
        mPlayer.notifyCurrentMediaItemChanged(currentItem);
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Null DSD becomes null MediaItem.
        mPlayer.mCurrentMediaItem = mPlayer.mItem = null;
        mPlayer.notifyCurrentMediaItemChanged(null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void addPlaylistItem() throws Exception {
        final int testIndex = 12;
        final String testId = "testAddPlaylistItem";
        SessionResult result = mController.addPlaylistItem(testIndex, testId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertEquals(testId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void removePlaylistItem() throws Exception {
        mPlayer.mPlaylist = TestUtils.createMediaItems(2);

        // Recreate controller for sending removePlaylistItem.
        // It's easier to ensure that MediaController.getPlaylist() returns the playlist from the
        // player.
        MediaController controller = createController(mSession.getToken());
        int targetIndex = 0;
        SessionResult result = controller.removePlaylistItem(targetIndex)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void replacePlaylistItem() throws Exception {
        final int testIndex = 12;
        final String testId = "testAddPlaylistItem";
        SessionResult result = mController.replacePlaylistItem(testIndex, testId)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        // MediaController.replacePlaylistItem does not ensure the equality of the items.
        assertEquals(testId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void skipToPreviousItem() throws Exception {
        SessionResult result = mController.skipToPreviousPlaylistItem()
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void skipToNextItem() throws Exception {
        SessionResult result = mController.skipToNextPlaylistItem()
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void skipToPlaylistItem() throws Exception {
        List<MediaItem> playlist = TestUtils.createMediaItems(2);
        int targetIndex = 1;
        mPlayer.mPlaylist = playlist;
        MediaController controller = createController(mSession.getToken());
        SessionResult result = controller.skipToPlaylistItem(targetIndex)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController, int)}.
     */
    @Test
    public void getShuffleMode() throws InterruptedException {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mPlayer.mShuffleMode = testShuffleMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(@NonNull MediaController controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    @Test
    public void setShuffleMode() throws Exception {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        SessionResult result = mController.setShuffleMode(testShuffleMode)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    /**
     * This also tests {@link ControllerCallback#onRepeatModeChanged(MediaController, int)}.
     */
    @Test
    public void getRepeatMode() throws InterruptedException {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mPlayer.mRepeatMode = testRepeatMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(@NonNull MediaController controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void setRepeatMode() throws Exception {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        SessionResult result = mController.setRepeatMode(testRepeatMode)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void updatedIndicesInRepeatMode() throws InterruptedException {
        final int noneRepeatMode = SessionPlayer.REPEAT_MODE_NONE;
        final int groupRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        final int currentIndex = -1;
        final int targetIndex = 2;
        final CountDownLatch latch = new CountDownLatch(2);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(@NonNull MediaController controller, int repeatMode) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertEquals(noneRepeatMode, repeatMode);
                        break;
                    case 1:
                        assertEquals(groupRepeatMode, repeatMode);
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);

        mPlayer.mPrevMediaItemIndex = currentIndex;
        mPlayer.mRepeatMode = noneRepeatMode;
        // Need to call this in order to update previous media item index.
        mPlayer.notifyRepeatModeChanged();
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(currentIndex, controller.getPreviousMediaItemIndex());

        mPlayer.mPrevMediaItemIndex = targetIndex;
        mPlayer.mRepeatMode = groupRepeatMode;
        mPlayer.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(targetIndex, controller.getPreviousMediaItemIndex());
    }

    @Test
    public void updatedIndicesInShuffleMode() throws InterruptedException {
        final int noneShuffleMode = SessionPlayer.SHUFFLE_MODE_NONE;
        final int groupShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        final int currentIndex = -1;
        final int targetIndex = 2;
        final CountDownLatch latch = new CountDownLatch(2);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(@NonNull MediaController controller, int shuffleMode) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertEquals(noneShuffleMode, shuffleMode);
                        break;
                    case 1:
                        assertEquals(groupShuffleMode, shuffleMode);

                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);

        mPlayer.mPrevMediaItemIndex = currentIndex;
        mPlayer.mShuffleMode = noneShuffleMode;
        mPlayer.notifyShuffleModeChanged();
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(currentIndex, controller.getPreviousMediaItemIndex());

        mPlayer.mPrevMediaItemIndex = targetIndex;
        mPlayer.mShuffleMode = groupShuffleMode;
        mPlayer.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(targetIndex, controller.getPreviousMediaItemIndex());
    }

    @Test
    public void setVolumeTo() throws Exception {
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer =
                new MockRemotePlayer(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);
        final MediaController controller = createController(mSession.getToken(), true, null, null);

        final int targetVolume = 50;
        SessionResult result = controller.setVolumeTo(targetVolume, 0 /* flags */)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mSetVolumeToCalled);
        assertEquals(targetVolume, (int) remotePlayer.mCurrentVolume);
    }

    @Test
    public void adjustVolume() throws Exception {
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer =
                new MockRemotePlayer(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);
        final MediaController controller = createController(mSession.getToken(), true, null, null);

        final int direction = AudioManager.ADJUST_RAISE;
        SessionResult result = controller.adjustVolume(direction, 0 /* flags */)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mAdjustVolumeCalled);
        assertEquals(direction, remotePlayer.mDirection);
    }

    @Test
    @Suppress // b/183700008
    public void setVolumeWithLocalVolume() throws Exception {
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

        // Set stream of the session.
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.mAudioAttributes = attrs;
        mSession.updatePlayer(mPlayer);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        SessionResult result = mController.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
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
    @Suppress // b/183700008
    public void adjustVolumeWithLocalVolume() throws Exception {
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

        // Set stream of the session.
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.mAudioAttributes = attrs;
        mSession.updatePlayer(mPlayer);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        SessionResult result = mController.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
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
    public void getPackageName() {
        assertEquals(mContext.getPackageName(),
                mController.getConnectedToken().getPackageName());
    }

    @Test
    public void sendCustomCommand() throws Exception {
        // TODO(jaewan): Need to revisit with the permission.
        final String command = "test_custom_command";
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "test_args");
        final SessionCommand testCommand = new SessionCommand(command, null);

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup commands =
                        new SessionCommandGroup.Builder(super.onConnect(session, controller))
                        .addCommand(testCommand)
                        .build();
                return commands;
            }

            @Override
            @NonNull
            public SessionResult onCustomCommand(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller,
                    @NonNull SessionCommand customCommand, Bundle args) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(command, customCommand.getCustomAction());
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new SessionResult(RESULT_SUCCESS, null);
            }
        };
        mSession.close();
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController controller = createController(mSession.getToken());
        SessionResult result = controller.sendCustomCommand(testCommand, testArgs)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onConnected() throws InterruptedException {
        // createController() uses controller callback to wait until the controller becomes
        // available.
        MediaController controller = createController(mSession.getToken());
        assertNotNull(controller);
    }

    @Test
    public void controllerCallback_sessionRejects() throws InterruptedException {
        final MediaSession.SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                return null;
            }
        };
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mSession = new MediaSession.Builder(mContext, mPlayer)
                        .setSessionCallback(sHandlerExecutor, sessionCallback).build();
            }
        });
        MediaController controller =
                createController(mSession.getToken(), false, null, null);
        assertNotNull(controller);
        waitForConnect(controller, false);
        waitForDisconnect(controller, true);
    }

    @Test
    public void controllerCallback_releaseSession() throws InterruptedException {
        mSession.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void controllerCallback_close() throws InterruptedException {
        mController.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void fastForward() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onFastForward(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testFastForward").build()) {
            MediaController controller = createController(session.getToken());
            SessionResult result = controller.fastForward().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(RESULT_SUCCESS, result.getResultCode());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void rewind() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onRewind(@NonNull MediaSession session, @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testRewind").build()) {
            MediaController controller = createController(session.getToken());
            SessionResult result = controller.rewind().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(RESULT_SUCCESS, result.getResultCode());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void setMediaUri() throws Exception {
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetMediaUri").build()) {
            MediaController controller = createController(session.getToken());
            SessionResult result = controller.setMediaUri(request, bundle)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(RESULT_SUCCESS, result.getResultCode());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void setRating() throws Exception {
        final float ratingValue = 3.5f;
        final Rating rating = new StarRating(5, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetRating(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaIdOut,
                    @NonNull Rating ratingOut) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(rating, ratingOut);
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };

        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetRating").build()) {
            MediaController controller = createController(session.getToken());
            SessionResult result = controller.setRating(mediaId, rating)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(RESULT_SUCCESS, result.getResultCode());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void isConnected() throws InterruptedException {
        assertTrue(mController.isConnected());
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
            }
        });
        waitForDisconnect(mController, true);
        assertFalse(mController.isConnected());
    }

    /**
     * Test potential deadlock for calls between controller and session.
     */
    @Test
    public void deadlock() throws InterruptedException {
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
                    mSession = new MediaSession.Builder(mContext, mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final MediaController controller = createController(mSession.getToken());
            testHandler.post(new Runnable() {
                @SuppressWarnings("FutureReturnValueIgnored")
                @Override
                public void run() {
                    final int state = SessionPlayer.PLAYER_STATE_ERROR;
                    for (int i = 0; i < 100; i++) {
                        // triggers call from session to controller.
                        player.notifyPlayerStateChanged(state);
                        // triggers call from controller to session.
                        controller.play();

                        // Repeat above
                        player.notifyPlayerStateChanged(state);
                        controller.pause();
                        player.notifyPlayerStateChanged(state);
                        controller.seekTo(0);
                        player.notifyPlayerStateChanged(state);
                        controller.skipToNextPlaylistItem();
                        player.notifyPlayerStateChanged(state);
                        controller.skipToPreviousPlaylistItem();
                    }
                    // This may hang if deadlock happens.
                    latch.countDown();
                }
            });
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
    public void getServiceToken() {
        SessionToken token = TestUtils.getServiceToken(mContext, MockMediaSessionService.ID);
        assertNotNull(token);
        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(SessionToken.TYPE_SESSION_SERVICE, token.getType());
    }

    @Test
    public void connectToService_sessionService() throws Exception {
        connectToService(MockMediaSessionService.ID);
    }

    @Test
    public void connectToService_libraryService() throws Exception {
        connectToService(MockMediaLibraryService.ID);
    }

    private void connectToService(String id) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    if (mSession != null) {
                        mSession.close();
                    }
                    mSession = session;
                    mPlayer = (MockPlayer) session.getPlayer();
                    assertEquals(mContext.getPackageName(), controller.getPackageName());
                    assertFalse(controller.isTrusted());
                    latch.countDown();
                }
                return super.onConnect(session, controller);
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);

        final SessionCommand testCommand = new SessionCommand("testConnectToService", null);
        final CountDownLatch controllerLatch = new CountDownLatch(1);
        mController = createController(TestUtils.getServiceToken(mContext, id), true,
                null, new ControllerCallback() {
                    @Override
                    @NonNull
                    public SessionResult onCustomCommand(@NonNull MediaController controller,
                            @NonNull SessionCommand command, Bundle args) {
                        if (testCommand.equals(command)) {
                            controllerLatch.countDown();
                        }
                        return new SessionResult(RESULT_SUCCESS);
                    }
                }
        );
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Test command from controller to session service.
        SessionResult result = mController.play().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);

        // Test command from session service to controller.
        mSession.broadcastCustomCommand(testCommand, null);
        assertTrue(controllerLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @LargeTest
    @Test
    public void controllerAfterSessionIsGone_session() throws InterruptedException {
        testControllerAfterSessionIsClosed(TAG);
    }

    @LargeTest
    @Test
    public void controllerAfterSessionIsClosed_sessionService() throws Exception {
        connectToService(MockMediaSessionService.ID);
        testControllerAfterSessionIsClosed(MockMediaSessionService.ID);
    }

    @Test
    public void close_beforeConnected() throws InterruptedException {
        MediaController controller =
                createController(mSession.getToken(), false, null, null);
        controller.close();
    }

    @Test
    public void close_twice() {
        mController.close();
        mController.close();
    }

    @LargeTest
    @Test
    public void close_session() throws InterruptedException {
        mController.close();
        // close is done immediately for session.
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(TAG);
    }

    @LargeTest
    @Test
    public void close_sessionService() throws InterruptedException {
        testCloseFromService(MockMediaSessionService.ID);
    }

    @LargeTest
    @Test
    public void close_libraryService() throws InterruptedException {
        testCloseFromService(MockMediaLibraryService.ID);
    }

    @Test
    public void getCurrentPosition() throws InterruptedException {
        final int pausedState = SessionPlayer.PLAYER_STATE_PAUSED;
        final int playingState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long timeDiff = 5000L;
        final long position = 0L;
        final CountDownLatch latch = new CountDownLatch(2);

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertEquals(state, pausedState);
                        assertEquals(position, controller.getCurrentPosition());
                        mPlayer.notifyPlayerStateChanged(playingState);
                        break;
                    case 1:
                        assertEquals(state, playingState);
                        assertEquals(position + timeDiff, controller.getCurrentPosition());
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        controller.setTimeDiff(timeDiff);
        mPlayer.notifyPlayerStateChanged(pausedState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setMetadataForCurrentMediaItem() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        final long duration = 1000L;
        final MediaItem item = TestUtils.createMediaItemWithMetadata();
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                    @Nullable MediaItem item) {
                MediaMetadata metadata = item.getMetadata();
                if (metadata != null) {
                    switch ((int) latch.getCount()) {
                        case 2:
                            assertEquals(-1, controller.getCurrentMediaItemIndex());
                            assertFalse(metadata.containsKey(
                                    MediaMetadata.METADATA_KEY_DURATION));
                            break;
                        case 1:
                            assertEquals(-1, controller.getCurrentMediaItemIndex());
                            assertTrue(metadata.containsKey(
                                    MediaMetadata.METADATA_KEY_DURATION));
                            assertEquals(duration,
                                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    }
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.mCurrentMediaItem = mPlayer.mItem = item;
        mPlayer.notifyCurrentMediaItemChanged(item);
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        item.setMetadata(TestUtils.createMetadata(item.getMediaId(), duration));
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setMetadataForMediaItemInPlaylist() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        final long duration = 1000L;
        final int currentItemIdx = 0;
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        final MediaMetadata oldMetadata = list.get(1).getMetadata();
        final MediaMetadata newMetadata = TestUtils.createMetadata(oldMetadata.getMediaId(),
                duration);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    @Nullable List<MediaItem> list, @Nullable MediaMetadata metadata) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertEquals(currentItemIdx, controller.getCurrentMediaItemIndex());
                        assertFalse(oldMetadata.containsKey(MediaMetadata.METADATA_KEY_DURATION));
                        break;
                    case 1:
                        assertEquals(currentItemIdx, controller.getCurrentMediaItemIndex());
                        assertNotNull(list);
                        assertTrue(list.get(1).getMetadata().containsKey(
                                MediaMetadata.METADATA_KEY_DURATION));
                        assertEquals(duration, list.get(1).getMetadata().getLong(
                                MediaMetadata.METADATA_KEY_DURATION));
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.mPlaylist = list;
        mPlayer.mIndex = currentItemIdx;
        mPlayer.mCurrentMediaItem = mPlayer.mItem = mPlayer.mPlaylist.get(currentItemIdx);
        mPlayer.notifyPlaylistChanged();
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        list.get(1).setMetadata(newMetadata);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void getVideoSize() throws InterruptedException {
        final MediaItem item = TestUtils.createMediaItemWithMetadata();
        final VideoSize testVideoSize = new VideoSize(100, 42);
        final CountDownLatch latch = new CountDownLatch(2);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onVideoSizeChanged(@NonNull MediaController controller,
                    @NonNull MediaItem item, @NonNull VideoSize videoSize) {
                assertNotNull(item);
                assertEquals(testVideoSize, videoSize);
                latch.countDown();
            }

            @Override
            public void onVideoSizeChanged(@NonNull MediaController controller,
                    @NonNull VideoSize videoSize) {
                assertEquals(testVideoSize, videoSize);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.notifyCurrentMediaItemChanged(item);
        mPlayer.notifyVideoSizeChanged(testVideoSize);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testVideoSize, controller.getVideoSize());
    }

    @Test
    public void getTracks() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<TrackInfo> testTracks = TestUtils.createTrackInfoList();

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onTracksChanged(@NonNull MediaController controller,
                    @NonNull List<TrackInfo> tracks) {
                assertEquals(testTracks.size(), tracks.size());
                for (int i = 0; i < testTracks.size(); i++) {
                    assertEquals(testTracks.get(i), tracks.get(i));
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.notifyTracksChanged(testTracks);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        List<TrackInfo> tracks = controller.getTracks();
        assertEquals(testTracks.size(), tracks.size());
        for (int i = 0; i < testTracks.size(); i++) {
            assertEquals(testTracks.get(i), tracks.get(i));
        }
    }

    @Test
    public void selectDeselectTrackAndGetSelectedTrack() throws InterruptedException {
        final CountDownLatch trackSelectedLatch = new CountDownLatch(1);
        final CountDownLatch trackDeselectedLatch = new CountDownLatch(1);
        final List<TrackInfo> testTracks = TestUtils.createTrackInfoList();
        final TrackInfo testTrack = testTracks.get(2);
        int testTrackType = testTrack.getTrackType();

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onTrackSelected(@NonNull MediaController controller,
                    @NonNull TrackInfo trackInfo) {
                assertEquals(testTrack, trackInfo);
                trackSelectedLatch.countDown();
            }

            @Override
            public void onTrackDeselected(@NonNull MediaController controller,
                    @NonNull TrackInfo trackInfo) {
                assertEquals(testTrack, trackInfo);
                trackDeselectedLatch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);

        assertNull(controller.getSelectedTrack(testTrackType));

        mPlayer.notifyTrackSelected(testTrack);
        assertTrue(trackSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testTrack, controller.getSelectedTrack(testTrackType));

        mPlayer.notifyTrackDeselected(testTrack);
        assertTrue(trackDeselectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(controller.getSelectedTrack(testTrackType));
    }

    @Test
    public void onSubtitleData() throws InterruptedException {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_LANGUAGE, "und");
        format.setString(MediaFormat.KEY_MIME, MIMETYPE_TEXT_CEA_608);
        final MediaItem testItem = TestUtils.createMediaItem("onSubtitleData");
        final TrackInfo testTrack = new TrackInfo(1, TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE, format);
        final SubtitleData testData = new SubtitleData(123, 456,
                new byte[] { 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 });

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onSubtitleData(@NonNull MediaController controller, @NonNull MediaItem item,
                    @NonNull TrackInfo track, @NonNull SubtitleData data) {
                assertSame(testItem, item);
                assertEquals(testTrack, track);
                assertEquals(testData, data);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, null, callback);
        mPlayer.notifySubtitleData(testItem, testTrack, testData);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void testCloseFromService(String id) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        TestServiceRegistry.getInstance().setSessionServiceCallback(new SessionServiceCallback() {
            @Override
            public void onCreated() {
                // Do nothing.
            }

            @Override
            public void onDestroyed() {
                latch.countDown();
            }
        });
        mController = createController(TestUtils.getServiceToken(mContext, id));
        mController.close();
        // Wait until close triggers onDestroy() of the session service.
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(TestServiceRegistry.getInstance().getServiceInstance());
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(id);
    }

    private void testControllerAfterSessionIsClosed(final String id) throws InterruptedException {
        // This cause session service to be died.
        mSession.close();
        waitForDisconnect(mController, true);
        testNoInteraction();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .setId(id).build();
        testNoInteraction();
    }

    // Test that mSession and mController doesn't interact.
    // Note that this method can be called after the mSession is died, so mSession may not have
    // valid player.
    private void testNoInteraction() throws InterruptedException {
        // TODO: check that calls from the controller to session shouldn't be delivered.

        // Calls from the session to controller shouldn't be delivered.
        final CountDownLatch latch = new CountDownLatch(1);
        setRunnableForOnCustomCommand(mController, new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        SessionCommand customCommand = new SessionCommand("testNoInteraction", null);
        mSession.broadcastCustomCommand(customCommand, null);
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        setRunnableForOnCustomCommand(mController, null);
    }

    // TODO(jaewan): Add  test for service connect rejection, when we differentiate session
    //               active/inactive and connection accept/refuse

    class TestSessionCallback extends SessionCallback {
        CountDownLatch mLatch;

        void resetLatchCount(int count) {
            mLatch = new CountDownLatch(count);
        }
    }
}
