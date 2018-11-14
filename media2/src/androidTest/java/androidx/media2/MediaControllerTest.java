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

import static androidx.media2.MediaSession.SessionResult.RESULT_CODE_SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaController.ControllerCallback;
import androidx.media2.MediaController.PlaybackInfo;
import androidx.media2.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.SessionCallback;
import androidx.media2.TestServiceRegistry.SessionServiceCallback;
import androidx.media2.TestUtils.SyncHandler;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.testutils.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaControllerTest extends MediaSessionTestBase {
    private static final String TAG = "MediaControllerTest";

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
                    public SessionCommandGroup onConnect(MediaSession session,
                            ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem onCreateMediaItem(MediaSession session,
                            ControllerInfo controller, String mediaId) {
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

    /**
     * Test if the {@link MockControllerCallback} wraps the callback proxy
     * without missing any method.
     */
    @Test
    public void testTestControllerCallback() {
        prepareLooper();
        Method[] methods = MockControllerCallback.class.getMethods();
        assertNotNull(methods);
        for (int i = 0; i < methods.length; i++) {
            // For any methods in the controller callback, TestControllerCallback should have
            // overriden the method and call matching API in the callback proxy.
            assertNotEquals("TestControllerCallback should override " + methods[i]
                            + " and call callback proxy",
                    ControllerCallback.class, methods[i].getDeclaringClass());
        }
    }

    @Test
    public void testPlay() {
        prepareLooper();
        mController.play();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPlay_autoPrepare() throws Exception {
        prepareLooper();

        final MockPlayer player = new MockPlayer(2);
        player.mLastPlayerState = SessionPlayer.PLAYER_STATE_IDLE;
        mSession.updatePlayer(player);
        mController.play();
        assertTrue(player.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(player.mPlayCalled);
        assertTrue(player.mPrepareCalled);
    }

    @Test
    public void testPause() {
        prepareLooper();
        mController.pause();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPrepare() {
        prepareLooper();
        mController.prepare();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void testSeekTo() {
        prepareLooper();
        final long seekPosition = 12125L;
        mController.seekTo(seekPosition);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }



    @Test
    public void testGettersAfterConnected() throws InterruptedException {
        prepareLooper();
        final int state = SessionPlayer.PLAYER_STATE_PLAYING;
        final int bufferingState = SessionPlayer.BUFFERING_STATE_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem currentMediaItem = TestUtils.createMediaItemWithMetadata();

        mPlayer.mLastPlayerState = state;
        mPlayer.mLastBufferingState = bufferingState;
        mPlayer.mCurrentPosition = position;
        mPlayer.mBufferedPosition = bufferedPosition;
        mPlayer.mPlaybackSpeed = speed;
        mPlayer.mCurrentMediaItem = currentMediaItem;

        MediaController controller = createController(mSession.getToken());
        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        assertEquals(currentMediaItem, controller.getCurrentMediaItem());
    }

    @Test
    public void testUpdatePlayer() throws InterruptedException {
        prepareLooper();
        final int testState = SessionPlayer.PLAYER_STATE_PLAYING;
        final List<MediaItem> testPlaylist = TestUtils.createMediaItems(3);
        final AudioAttributesCompat testAudioAttributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_RING).build();
        final CountDownLatch latch = new CountDownLatch(3);
        mController = createController(mSession.getToken(), true, new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController controller, int state) {
                assertEquals(mController, controller);
                assertEquals(testState, state);
                latch.countDown();
            }

            @Override
            public void onPlaylistChanged(MediaController controller, List<MediaItem> list,
                    MediaMetadata metadata) {
                assertEquals(mController, controller);
                assertEquals(testPlaylist, list);
                assertNull(metadata);
                latch.countDown();
            }

            @Override
            public void onPlaybackInfoChanged(MediaController controller, PlaybackInfo info) {
                assertEquals(mController, controller);
                assertEquals(testAudioAttributes, info.getAudioAttributes());
                latch.countDown();
            }
        });

        MockPlayer player = new MockPlayer(0);
        player.mLastPlayerState = testState;
        player.setAudioAttributes(testAudioAttributes);
        player.mPlaylist = testPlaylist;

        mSession.updatePlayer(player);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetSessionActivity() {
        prepareLooper();
        PendingIntent sessionActivity = mController.getSessionActivity();
        assertNotNull(sessionActivity);
        if (Build.VERSION.SDK_INT >= 17) {
            // PendingIntent#getCreatorPackage() is added in API 17.
            assertEquals(mContext.getPackageName(), sessionActivity.getCreatorPackage());
            assertEquals(Process.myUid(), sessionActivity.getCreatorUid());
        }
    }

    @Test
    public void testSetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<String> list = TestUtils.createMediaIds(2);
        mController.setPlaylist(list, null /* Metadata */);
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
    public void testSetMediaItem() throws InterruptedException {
        prepareLooper();
        final MediaItem item = TestUtils.createMediaItemWithMetadata();
        mController.setMediaItem(item.getMetadata()
                .getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertNull(mPlayer.mMetadata);
        assertEquals(item.getMediaId(), mPlayer.mItem.getMediaId());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController, List, MediaMetadata)}.
     */
    @Test
    public void testGetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem> testList = TestUtils.createMediaItems(2);
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController controller,
                    List<MediaItem> playlist, MediaMetadata metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
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
    public void testGetPlaylist_withLongPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem> testList = TestUtils.createMediaItems(5000);
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController controller,
                    List<MediaItem> playlist, MediaMetadata metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
        mPlayer.mPlaylist = testList;
        mPlayer.mMetadata = testMetadata;
        mPlayer.notifyPlaylistChanged();
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        // Ensures object equality
        assertEquals(listFromCallback.get(), controller.getPlaylist());
        TestUtils.assertMetadataEquals(testMetadata, controller.getPlaylistMetadata());
    }

    @Test
    public void testUpdatePlaylistMetadata() throws InterruptedException {
        prepareLooper();
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        mController.updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertNotNull(mPlayer.mMetadata);
        assertEquals(testMetadata.getMediaId(), mPlayer.mMetadata.getMediaId());
    }

    @Test
    public void testGetPlaylistMetadata() throws InterruptedException {
        prepareLooper();
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        final AtomicReference<MediaMetadata> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(MediaController controller,
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
            MediaController controller = createController(session.getToken(), true, callback);
            mPlayer.notifyPlaylistMetadataChanged();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(metadataFromCallback.get().getMediaId(),
                    controller.getPlaylistMetadata().getMediaId());
        }
    }

    @Test
    public void testSetPlaybackSpeed() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mController.setPlaybackSpeed(speed);
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
    public void testGetPlaybackSpeed() throws InterruptedException {
        prepareLooper();
        final float speed = 1.5f;
        mPlayer.setPlaybackSpeed(speed);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController controller =
                createController(mSession.getToken(), true, new ControllerCallback() {
                    @Override
                    public void onPlaybackSpeedChanged(MediaController controller,
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
    public void testControllerCallback_onPlaylistMetadataChanged() throws InterruptedException {
        prepareLooper();
        final MediaItem item = TestUtils.createMediaItemWithMetadata();
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(MediaController controller,
                    MediaMetadata metadata) {
                assertNotNull(metadata);
                assertEquals(item.getMediaId(), metadata.getMediaId());
                latch.countDown();
            }
        };
        final SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(MediaSession session,
                    ControllerInfo controller) {
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
            MediaController controller = createController(session.getToken(), true, callback);
            mPlayer.notifyPlaylistChanged();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testControllerCallback_onSeekCompleted() throws InterruptedException {
        prepareLooper();
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onSeekCompleted(MediaController controller, long position) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, callback);
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
    public void testControllerCallback_onBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem> testPlaylist = TestUtils.createMediaItems(3);
        final MediaItem testItem = testPlaylist.get(0);
        final int testBufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController controller, MediaItem item,
                    int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testItem, item);
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, callback);
        mSession.getPlayer().setPlaylist(testPlaylist, null);
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
    public void testControllerCallback_onPlayerStateChanged() throws InterruptedException {
        prepareLooper();
        final int testPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController controller, int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController controller = createController(mSession.getToken(), true, callback);
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
    public void testControllerCallback_onCurrentMediaItemChanged() throws InterruptedException {
        prepareLooper();
        final int listSize = 5;
        final List<MediaItem> list = TestUtils.createMediaItems(listSize);
        mPlayer.setPlaylist(list, null);

        final MediaItem currentItem = list.get(3);
        final MediaItem unknownItem = TestUtils.createMediaItemWithMetadata();
        final CountDownLatch latch = new CountDownLatch(3);
        final MediaController controller =
                createController(mSession.getToken(), true, new ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(MediaController controller,
                            MediaItem item) {
                        switch ((int) latch.getCount()) {
                            case 3:
                                assertEquals(unknownItem, item);
                                break;
                            case 2:
                                assertEquals(currentItem, item);
                                break;
                            case 1:
                                assertNull(item);
                        }
                        latch.countDown();
                    }
                });

        // Player notifies with the unknown item. It's still OK.
        mPlayer.notifyCurrentMediaItemChanged(unknownItem);
        // Known DSD should be notified through the onCurrentMediaItemChanged.
        mPlayer.notifyCurrentMediaItemChanged(currentItem);
        // Null DSD becomes null MediaItem.
        mPlayer.notifyCurrentMediaItemChanged(null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAddPlaylistItem() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final String testId = "testAddPlaylistItem";
        mController.addPlaylistItem(testIndex, testId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertEquals(testId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void testRemovePlaylistItem() throws InterruptedException {
        prepareLooper();
        mPlayer.mPlaylist = TestUtils.createMediaItems(2);

        // Recreate controller for sending removePlaylistItem.
        // It's easier to ensure that MediaController.getPlaylist() returns the playlist from the
        // player.
        MediaController controller = createController(mSession.getToken());
        int targetIndex = 0;
        controller.removePlaylistItem(targetIndex);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void testReplacePlaylistItem() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final String testId = "testAddPlaylistItem";
        mController.replacePlaylistItem(testIndex, testId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        // MediaController.replacePlaylistItem does not ensure the equality of the items.
        assertEquals(testId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void testSkipToPreviousItem() throws InterruptedException {
        prepareLooper();
        mController.skipToPreviousPlaylistItem();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToNextItem() throws InterruptedException {
        prepareLooper();
        mController.skipToNextPlaylistItem();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToPlaylistItem() throws InterruptedException {
        prepareLooper();
        List<MediaItem> playlist = TestUtils.createMediaItems(2);
        int targetIndex = 1;
        mPlayer.mPlaylist = playlist;
        MediaController controller = createController(mSession.getToken());
        controller.skipToPlaylistItem(targetIndex);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController, int)}.
     */
    @Test
    public void testGetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mPlayer.mShuffleMode = testShuffleMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(MediaController controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
        mPlayer.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    @Test
    public void testSetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mController.setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    /**
     * This also tests {@link ControllerCallback#onRepeatModeChanged(MediaController, int)}.
     */
    @Test
    public void testGetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mPlayer.mRepeatMode = testRepeatMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(MediaController controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
        mPlayer.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testSetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mController.setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void testSetVolumeTo() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer =
                new MockRemotePlayer(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);
        final MediaController controller = createController(mSession.getToken(), true, null);

        final int targetVolume = 50;
        controller.setVolumeTo(targetVolume, 0 /* flags */);
        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mSetVolumeToCalled);
        assertEquals(targetVolume, (int) remotePlayer.mCurrentVolume);
    }

    @Test
    public void testAdjustVolume() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer =
                new MockRemotePlayer(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);
        final MediaController controller = createController(mSession.getToken(), true, null);

        final int direction = AudioManager.ADJUST_RAISE;
        controller.adjustVolume(direction, 0 /* flags */);
        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mAdjustVolumeCalled);
        assertEquals(direction, remotePlayer.mDirection);
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
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.setAudioAttributes(attrs);
        mSession.updatePlayer(mPlayer);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;

        mController.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(TIMEOUT_MS) {
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
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.setAudioAttributes(attrs);
        mSession.updatePlayer(mPlayer);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;

        mController.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(TIMEOUT_MS) {
            @Override
            protected boolean check() {
                return targetVolume == mAudioManager.getStreamVolume(stream);
            }
        }.run();

        // Set back to original volume.
        mAudioManager.setStreamVolume(stream, originalVolume, 0 /* flags */);
    }

    @Test
    public void testGetPackageName() {
        prepareLooper();
        assertEquals(mContext.getPackageName(),
                mController.getConnectedSessionToken().getPackageName());
    }

    @Test
    public void testSendCustomCommand() throws InterruptedException {
        prepareLooper();
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
            public MediaSession.SessionResult onCustomCommand(MediaSession session,
                    ControllerInfo controller, SessionCommand customCommand, Bundle args) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(command, customCommand.getCustomCommand());
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new MediaSession.SessionResult(RESULT_CODE_SUCCESS, null);
            }
        };
        mSession.close();
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController controller = createController(mSession.getToken());
        controller.sendCustomCommand(testCommand, testArgs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_onConnected() throws InterruptedException {
        prepareLooper();
        // createController() uses controller callback to wait until the controller becomes
        // available.
        MediaController controller = createController(mSession.getToken());
        assertNotNull(controller);
    }

    @Test
    public void testControllerCallback_sessionRejects() throws InterruptedException {
        prepareLooper();
        final MediaSession.SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(MediaSession session,
                    ControllerInfo controller) {
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
                createController(mSession.getToken(), false, null);
        assertNotNull(controller);
        waitForConnect(controller, false);
        waitForDisconnect(controller, true);
    }

    @Test
    public void testControllerCallback_releaseSession() throws InterruptedException {
        prepareLooper();
        mSession.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void testControllerCallback_close() throws InterruptedException {
        prepareLooper();
        mController.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void testFastForward() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onFastForward(MediaSession session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testFastForward").build()) {
            MediaController controller = createController(session.getToken());
            controller.fastForward();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testRewind() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onRewind(MediaSession session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testRewind").build()) {
            MediaController controller = createController(session.getToken());
            controller.rewind();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPlayFromSearch() throws InterruptedException {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPlayFromSearch(MediaSession session, ControllerInfo controller,
                    String query, Bundle extras) {
                super.onPlayFromSearch(session, controller, query, extras);
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromSearch").build()) {
            MediaController controller = createController(session.getToken());
            controller.playFromSearch(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPlayFromUri() throws InterruptedException {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPlayFromUri(MediaSession session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromUri").build()) {
            MediaController controller = createController(session.getToken());
            controller.playFromUri(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPlayFromMediaId() throws InterruptedException {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPlayFromMediaId(MediaSession session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromMediaId").build()) {
            MediaController controller = createController(session.getToken());
            controller.playFromMediaId(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPrepareFromSearch() throws InterruptedException {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPrepareFromSearch(MediaSession session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromSearch").build()) {
            MediaController controller = createController(session.getToken());
            controller.prepareFromSearch(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPrepareFromUri() throws InterruptedException {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPrepareFromUri(MediaSession session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromUri").build()) {
            MediaController controller = createController(session.getToken());
            controller.prepareFromUri(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testPrepareFromMediaId() throws InterruptedException {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onPrepareFromMediaId(MediaSession session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromMediaId").build()) {
            MediaController controller = createController(session.getToken());
            controller.prepareFromMediaId(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testSetRating() throws InterruptedException {
        prepareLooper();
        final float ratingValue = 3.5f;
        final Rating rating = new StarRating(5, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetRating(MediaSession session, ControllerInfo controller,
                    String mediaIdOut, Rating ratingOut) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(rating, ratingOut);
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };

        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetRating").build()) {
            MediaController controller = createController(session.getToken());
            controller.setRating(mediaId, rating);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testIsConnected() throws InterruptedException {
        prepareLooper();
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
                    mSession = new MediaSession.Builder(mContext, mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final MediaController controller = createController(mSession.getToken());
            testHandler.post(new Runnable() {
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
    public void testGetServiceToken() {
        prepareLooper();
        SessionToken token = TestUtils.getServiceToken(mContext, MockMediaSessionService.ID);
        assertNotNull(token);
        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(SessionToken.TYPE_SESSION_SERVICE, token.getType());
    }

    @Test
    public void testConnectToService_sessionService() throws InterruptedException {
        prepareLooper();
        testConnectToService(MockMediaSessionService.ID);
    }

    @Test
    public void testConnectToService_libraryService() throws InterruptedException {
        prepareLooper();
        testConnectToService(MockMediaLibraryService.ID);
    }

    public void testConnectToService(String id) throws InterruptedException {
        prepareLooper();
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
                new ControllerCallback() {
                    @Override
                    public MediaController.ControllerResult onCustomCommand(
                            MediaController controller, SessionCommand command, Bundle args) {
                        if (testCommand.equals(command)) {
                            controllerLatch.countDown();
                        }
                        return new MediaController.ControllerResult(RESULT_CODE_SUCCESS);
                    }
                }
        );
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Test command from controller to session service.
        mController.play();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);

        // Test command from session service to controller.
        mSession.broadcastCustomCommand(testCommand, null);
        assertTrue(controllerLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @LargeTest
    @Test
    public void testControllerAfterSessionIsGone_session() throws InterruptedException {
        prepareLooper();
        testControllerAfterSessionIsClosed(TAG);
    }

    @LargeTest
    @Test
    public void testControllerAfterSessionIsClosed_sessionService() throws InterruptedException {
        prepareLooper();
        testConnectToService(MockMediaSessionService.ID);
        testControllerAfterSessionIsClosed(MockMediaSessionService.ID);
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController controller =
                createController(mSession.getToken(), false, null);
        controller.close();
    }

    @Test
    public void testClose_twice() {
        prepareLooper();
        mController.close();
        mController.close();
    }

    @LargeTest
    @Test
    public void testClose_session() throws InterruptedException {
        prepareLooper();
        mController.close();
        // close is done immediately for session.
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(TAG);
    }

    @LargeTest
    @Test
    public void testClose_sessionService() throws InterruptedException {
        prepareLooper();
        testCloseFromService(MockMediaSessionService.ID);
    }

    @LargeTest
    @Test
    public void testClose_libraryService() throws InterruptedException {
        prepareLooper();
        testCloseFromService(MockMediaLibraryService.ID);
    }

    @Test
    public void testGetCurrentPosition() throws InterruptedException {
        prepareLooper();
        final int pausedState = SessionPlayer.PLAYER_STATE_PAUSED;
        final int playingState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long timeDiff = 5000L;
        final long position = 0L;
        final CountDownLatch latch = new CountDownLatch(2);

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController controller, int state) {
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
        MediaController controller = createController(mSession.getToken(), true, callback);
        controller.setTimeDiff(timeDiff);
        mPlayer.notifyPlayerStateChanged(pausedState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetMetadataForCurrentMediaItem() throws InterruptedException {
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
                            assertFalse(metadata.containsKey(
                                    MediaMetadata.METADATA_KEY_DURATION));
                            break;
                        case 1:
                            assertTrue(metadata.containsKey(
                                    MediaMetadata.METADATA_KEY_DURATION));
                            assertEquals(duration,
                                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    }
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
        mPlayer.setMediaItem(item);
        mPlayer.notifyCurrentMediaItemChanged(item);
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        item.setMetadata(TestUtils.createMetadata(item.getMediaId(), duration));
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetMetadataForMediaItemInPlaylist() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        final long duration = 1000L;
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        final MediaMetadata oldMetadata = list.get(1).getMetadata();
        final MediaMetadata newMetadata = TestUtils.createMetadata(oldMetadata.getMediaId(),
                duration);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    @NonNull List<MediaItem> list, @Nullable MediaMetadata metadata) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertFalse(oldMetadata.containsKey(MediaMetadata.METADATA_KEY_DURATION));
                        break;
                    case 1:
                        assertTrue(list.get(1).getMetadata().containsKey(
                                MediaMetadata.METADATA_KEY_DURATION));
                        assertEquals(duration, list.get(1).getMetadata().getLong(
                                MediaMetadata.METADATA_KEY_DURATION));
                }
                latch.countDown();
            }
        };
        MediaController controller = createController(mSession.getToken(), true, callback);
        mPlayer.setPlaylist(list, null);
        mPlayer.notifyPlaylistChanged();
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        list.get(1).setMetadata(newMetadata);
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
