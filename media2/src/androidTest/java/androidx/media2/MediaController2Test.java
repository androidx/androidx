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

import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SUCCESS;

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
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionCallback;
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
 * Tests {@link MediaController2}.
 */
// TODO(jaewan): Implement host-side test so controller and session can run in different processes.
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
// TODO(jaeawn): Revisit create/close session in the sHandler. It's no longer necessary.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaController2Test extends MediaSession2TestBase {
    private static final String TAG = "MediaController2Test";

    PendingIntent mIntent;
    MediaSession2 mSession;
    MediaController2 mController;
    MockPlayer mPlayer;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession2 to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);

        mPlayer = new MockPlayer(1);
        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem2 onCreateMediaItem(MediaSession2 session,
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
        player.mLastPlayerState = SessionPlayer2.PLAYER_STATE_IDLE;
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
        final int state = SessionPlayer2.PLAYER_STATE_PLAYING;
        final int bufferingState = SessionPlayer2.BUFFERING_STATE_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem2 currentMediaItem = TestUtils.createMediaItemWithMetadata();

        mPlayer.mLastPlayerState = state;
        mPlayer.mLastBufferingState = bufferingState;
        mPlayer.mCurrentPosition = position;
        mPlayer.mBufferedPosition = bufferedPosition;
        mPlayer.mPlaybackSpeed = speed;
        mPlayer.mCurrentMediaItem = currentMediaItem;

        MediaController2 controller = createController(mSession.getToken());
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
        final int testState = SessionPlayer2.PLAYER_STATE_PLAYING;
        final List<MediaItem2> testPlaylist = TestUtils.createMediaItems(3);
        final AudioAttributesCompat testAudioAttributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_RING).build();
        final CountDownLatch latch = new CountDownLatch(3);
        mController = createController(mSession.getToken(), true, new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
                assertEquals(mController, controller);
                assertEquals(testState, state);
                latch.countDown();
            }

            @Override
            public void onPlaylistChanged(MediaController2 controller, List<MediaItem2> list,
                    MediaMetadata2 metadata) {
                assertEquals(mController, controller);
                assertEquals(testPlaylist, list);
                assertNull(metadata);
                latch.countDown();
            }

            @Override
            public void onPlaybackInfoChanged(MediaController2 controller, PlaybackInfo info) {
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
            // MediaController2.setPlaylist does not ensure the equality of the items.
            assertEquals(list.get(i), mPlayer.mPlaylist.get(i).getMediaId());
        }
    }

    @Test
    public void testSetMediaItem() throws InterruptedException {
        prepareLooper();
        final MediaItem2 item = TestUtils.createMediaItemWithMetadata();
        mController.setMediaItem(item.getMetadata()
                .getString(MediaMetadata2.METADATA_KEY_MEDIA_ID));
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertNull(mPlayer.mMetadata);
        assertEquals(item.getMediaId(), mPlayer.mItem.getMediaId());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController2, List, MediaMetadata2)}.
     */
    @Test
    public void testGetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testList = TestUtils.createMediaItems(2);
        final MediaMetadata2 testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
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
     * MediaController2, List, MediaMetadata2)}.
     */
    @Test
    @LargeTest
    public void testGetPlaylist_withLongPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testList = TestUtils.createMediaItems(5000);
        final MediaMetadata2 testMetadata = TestUtils.createMetadata();
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> playlist, MediaMetadata2 metadata) {
                assertNotNull(playlist);
                TestUtils.assertMediaItemListEquals(testList, playlist);
                TestUtils.assertMetadataEquals(testMetadata, metadata);
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
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
        final MediaMetadata2 testMetadata = TestUtils.createMetadata();
        mController.updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertNotNull(mPlayer.mMetadata);
        assertEquals(testMetadata.getMediaId(), mPlayer.mMetadata.getMediaId());
    }

    @Test
    public void testGetPlaylistMetadata() throws InterruptedException {
        prepareLooper();
        final MediaMetadata2 testMetadata = TestUtils.createMetadata();
        final AtomicReference<MediaMetadata2> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(MediaController2 controller,
                    MediaMetadata2 metadata) {
                assertNotNull(testMetadata);
                assertEquals(testMetadata.getMediaId(), metadata.getMediaId());
                metadataFromCallback.set(metadata);
                latch.countDown();
            }
        };
        mPlayer.mMetadata = testMetadata;
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setId("testGetPlaylistMetadata")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .build()) {
            MediaController2 controller = createController(session.getToken(), true, callback);
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
     * This also tests {@link MediaController2.ControllerCallback#onPlaybackSpeedChanged(
     * MediaController2, float)}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetPlaybackSpeed() throws InterruptedException {
        prepareLooper();
        final float speed = 1.5f;
        mPlayer.setPlaybackSpeed(speed);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2 controller =
                createController(mSession.getToken(), true, new ControllerCallback() {
                    @Override
                    public void onPlaybackSpeedChanged(MediaController2 controller,
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
     * Test whether {@link SessionPlayer2#setPlaylist(List, MediaMetadata2)} is notified
     * through the
     * {@link ControllerCallback#onPlaylistMetadataChanged(MediaController2, MediaMetadata2)}
     * if the controller doesn't have {@link SessionCommand2#COMMAND_CODE_PLAYER_GET_PLAYLIST} but
     * {@link SessionCommand2#COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA}.
     */
    @Test
    public void testControllerCallback_onPlaylistMetadataChanged() throws InterruptedException {
        prepareLooper();
        final MediaItem2 item = TestUtils.createMediaItemWithMetadata();
        final List<MediaItem2> list = TestUtils.createMediaItems(2);
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(MediaController2 controller,
                    MediaMetadata2 metadata) {
                assertNotNull(metadata);
                assertEquals(item.getMediaId(), metadata.getMediaId());
                latch.countDown();
            }
        };
        final SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    SessionCommandGroup2 commands = new SessionCommandGroup2.Builder()
                            .addCommand(new SessionCommand2(
                                    SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA))
                            .build();
                    return commands;
                }
                return super.onConnect(session, controller);
            }
        };
        mPlayer.mMetadata = item.getMetadata();
        mPlayer.mPlaylist = list;
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setId("testControllerCallback_onPlaylistMetadataChanged")
                .setSessionCallback(sHandlerExecutor, sessionCallback)
                .build()) {
            MediaController2 controller = createController(session.getToken(), true, callback);
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
            public void onSeekCompleted(MediaController2 controller, long position) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.mLastPlayerState = SessionPlayer2.PLAYER_STATE_PAUSED;
        mPlayer.notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController2#getBufferedPosition()}, and
     * {@link MediaController2#getBufferingState()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testControllerCallback_onBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testPlaylist = TestUtils.createMediaItems(3);
        final MediaItem2 testItem = testPlaylist.get(0);
        final int testBufferingState = SessionPlayer2.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testItem, item);
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mSession.getPlayer().setPlaylist(testPlaylist, null);
        mPlayer.mBufferedPosition = testBufferingPosition;
        mPlayer.notifyBufferingStateChanged(testItem, testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController2#getPlayerState()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testControllerCallback_onPlayerStateChanged() throws InterruptedException {
        prepareLooper();
        final int testPlayerState = SessionPlayer2.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.notifyPlayerStateChanged(testPlayerState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController2#getCurrentMediaItem()}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testControllerCallback_onCurrentMediaItemChanged() throws InterruptedException {
        prepareLooper();
        final int listSize = 5;
        final List<MediaItem2> list = TestUtils.createMediaItems(listSize);
        mPlayer.setPlaylist(list, null);

        final MediaItem2 currentItem = list.get(3);
        final MediaItem2 unknownItem = TestUtils.createMediaItemWithMetadata();
        final CountDownLatch latch = new CountDownLatch(3);
        final MediaController2 controller =
                createController(mSession.getToken(), true, new ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(MediaController2 controller,
                            MediaItem2 item) {
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
        // Null DSD becomes null MediaItem2.
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
        // It's easier to ensure that MediaController2.getPlaylist() returns the playlist from the
        // player.
        MediaController2 controller = createController(mSession.getToken());
        MediaItem2 targetItem = controller.getPlaylist().get(0);
        controller.removePlaylistItem(0);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(targetItem, mPlayer.mItem);
    }

    @Test
    public void testReplacePlaylistItem() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final String testId = "testAddPlaylistItem";
        mController.replacePlaylistItem(testIndex, testId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        // MediaController2.replacePlaylistItem does not ensure the equality of the items.
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
        List<MediaItem2> playlist = TestUtils.createMediaItems(2);
        mPlayer.mPlaylist = playlist;

        MediaController2 controller = createController(mSession.getToken());
        controller.skipToPlaylistItem(1);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(playlist.get(1), mPlayer.mItem);
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController2, int)}.
     */
    @Test
    public void testGetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer2.SHUFFLE_MODE_GROUP;
        mPlayer.mShuffleMode = testShuffleMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    @Test
    public void testSetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer2.SHUFFLE_MODE_GROUP;
        mController.setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    /**
     * This also tests {@link ControllerCallback#onRepeatModeChanged(MediaController2, int)}.
     */
    @Test
    public void testGetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer2.REPEAT_MODE_GROUP;
        mPlayer.mRepeatMode = testRepeatMode;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testSetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer2.REPEAT_MODE_GROUP;
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
        final MediaController2 controller = createController(mSession.getToken(), true, null);

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
        final MediaController2 controller = createController(mSession.getToken(), true, null);

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
        final SessionCommand2 testCommand = new SessionCommand2(command, null);

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup2 commands =
                        new SessionCommandGroup2.Builder(super.onConnect(session, controller))
                        .addCommand(testCommand)
                        .build();
                return commands;
            }

            @Override
            public MediaSession2.SessionResult onCustomCommand(MediaSession2 session,
                    ControllerInfo controller, SessionCommand2 customCommand, Bundle args) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(command, customCommand.getCustomCommand());
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new MediaSession2.SessionResult(RESULT_CODE_SUCCESS, null);
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController2 controller = createController(mSession.getToken());
        controller.sendCustomCommand(testCommand, testArgs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_onConnected() throws InterruptedException {
        prepareLooper();
        // createController() uses controller callback to wait until the controller becomes
        // available.
        MediaController2 controller = createController(mSession.getToken());
        assertNotNull(controller);
    }

    @Test
    public void testControllerCallback_sessionRejects() throws InterruptedException {
        prepareLooper();
        final MediaSession2.SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                return null;
            }
        };
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mSession = new MediaSession2.Builder(mContext, mPlayer)
                        .setSessionCallback(sHandlerExecutor, sessionCallback).build();
            }
        });
        MediaController2 controller =
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
            public int onFastForward(MediaSession2 session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testFastForward").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onRewind(MediaSession2 session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testRewind").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPlayFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                super.onPlayFromSearch(session, controller, query, extras);
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromSearch").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPlayFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromUri").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPlayFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromMediaId").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPrepareFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromSearch").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPrepareFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromUri").build()) {
            MediaController2 controller = createController(session.getToken());
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
            public int onPrepareFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromMediaId").build()) {
            MediaController2 controller = createController(session.getToken());
            controller.prepareFromMediaId(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testSetRating() throws InterruptedException {
        prepareLooper();
        final float ratingValue = 3.5f;
        final Rating2 rating = new StarRating2(5, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetRating(MediaSession2 session, ControllerInfo controller,
                    String mediaIdOut, Rating2 ratingOut) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(rating, ratingOut);
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };

        try (MediaSession2 session = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetRating").build()) {
            MediaController2 controller = createController(session.getToken());
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
                    mSession = new MediaSession2.Builder(mContext, mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final MediaController2 controller = createController(mSession.getToken());
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int state = SessionPlayer2.PLAYER_STATE_ERROR;
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
        SessionToken2 token = TestUtils.getServiceToken(mContext, MockMediaSessionService2.ID);
        assertNotNull(token);
        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(SessionToken2.TYPE_SESSION_SERVICE, token.getType());
    }

    @Test
    public void testConnectToService_sessionService() throws InterruptedException {
        prepareLooper();
        testConnectToService(MockMediaSessionService2.ID);
    }

    @Test
    public void testConnectToService_libraryService() throws InterruptedException {
        prepareLooper();
        testConnectToService(MockMediaLibraryService2.ID);
    }

    public void testConnectToService(String id) throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
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

        final SessionCommand2 testCommand = new SessionCommand2("testConnectToService", null);
        final CountDownLatch controllerLatch = new CountDownLatch(1);
        mController = createController(TestUtils.getServiceToken(mContext, id), true,
                new ControllerCallback() {
                    @Override
                    public MediaController2.ControllerResult onCustomCommand(
                            MediaController2 controller, SessionCommand2 command, Bundle args) {
                        if (testCommand.equals(command)) {
                            controllerLatch.countDown();
                        }
                        return new MediaController2.ControllerResult(RESULT_CODE_SUCCESS);
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
        testConnectToService(MockMediaSessionService2.ID);
        testControllerAfterSessionIsClosed(MockMediaSessionService2.ID);
    }

    @Test
    public void testSubscribeRouteInfo() throws InterruptedException {
        prepareLooper();
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public int onSubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                mLatch.countDown();
                return RESULT_CODE_SUCCESS;
            }

            @Override
            public int onUnsubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                mLatch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController2 controller = createController(mSession.getToken());

        callback.resetLatchCount(1);
        controller.subscribeRoutesInfo();
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        callback.resetLatchCount(1);
        controller.unsubscribeRoutesInfo();
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSelectRouteInfo() throws InterruptedException {
        prepareLooper();
        final Bundle testRoute = new Bundle();
        testRoute.putString("id", "testRoute");
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public int onSelectRoute(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller, @NonNull Bundle route) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertTrue(TestUtils.equals(route, testRoute));
                mLatch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController2 controller = createController(mSession.getToken());

        callback.resetLatchCount(1);
        controller.selectRoute(testRoute);
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller =
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
        testCloseFromService(MockMediaSessionService2.ID);
    }

    @LargeTest
    @Test
    public void testClose_libraryService() throws InterruptedException {
        prepareLooper();
        testCloseFromService(MockMediaLibraryService2.ID);
    }

    @Test
    public void testGetCurrentPosition() throws InterruptedException {
        prepareLooper();
        final int pausedState = SessionPlayer2.PLAYER_STATE_PAUSED;
        final int playingState = SessionPlayer2.PLAYER_STATE_PLAYING;
        final long timeDiff = 5000L;
        final long position = 0L;
        final CountDownLatch latch = new CountDownLatch(2);

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
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
        MediaController2 controller = createController(mSession.getToken(), true, callback);
        controller.setTimeDiff(timeDiff);
        mPlayer.notifyPlayerStateChanged(pausedState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetMetadataForCurrentMediaItem() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        final long duration = 1000L;
        final MediaItem2 item = TestUtils.createMediaItemWithMetadata();
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController2 controller,
                    @Nullable MediaItem2 item) {
                MediaMetadata2 metadata = item.getMetadata();
                if (metadata != null) {
                    switch ((int) latch.getCount()) {
                        case 2:
                            assertFalse(metadata.containsKey(
                                    MediaMetadata2.METADATA_KEY_DURATION));
                            break;
                        case 1:
                            assertTrue(metadata.containsKey(
                                    MediaMetadata2.METADATA_KEY_DURATION));
                            assertEquals(duration,
                                    metadata.getLong(MediaMetadata2.METADATA_KEY_DURATION));
                    }
                }
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
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
        final List<MediaItem2> list = TestUtils.createMediaItems(2);
        final MediaMetadata2 oldMetadata = list.get(1).getMetadata();
        final MediaMetadata2 newMetadata = TestUtils.createMetadata(oldMetadata.getMediaId(),
                duration);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController2 controller,
                    @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
                switch ((int) latch.getCount()) {
                    case 2:
                        assertFalse(oldMetadata.containsKey(MediaMetadata2.METADATA_KEY_DURATION));
                        break;
                    case 1:
                        assertTrue(list.get(1).getMetadata().containsKey(
                                MediaMetadata2.METADATA_KEY_DURATION));
                        assertEquals(duration, list.get(1).getMetadata().getLong(
                                MediaMetadata2.METADATA_KEY_DURATION));
                }
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mSession.getToken(), true, callback);
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
        mSession = new MediaSession2.Builder(mContext, mPlayer)
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
        SessionCommand2 customCommand = new SessionCommand2("testNoInteraction", null);
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
