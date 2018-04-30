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

package androidx.media.test.client;

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
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaController2;
import androidx.media.MediaController2.ControllerCallback;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlayerInterface;
import androidx.media.MediaPlaylistAgent;
import androidx.media.MediaSession2;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.MediaSession2.SessionCallback;
import androidx.media.Rating2;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.VolumeProviderCompat;
import androidx.media.test.client.TestUtils.SyncHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
public class MediaController2Test_copied extends MediaSession2TestBase {
    private static final String TAG = "MediaController2Test_copied";

    PendingIntent mIntent;
    MediaSession2 mSession;
    MediaController2 mController;
    MockPlayer mPlayer;
    MockPlaylistAgent mMockAgent;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession2 to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);

        mPlayer = new MockPlayer(1);
        mMockAgent = new MockPlaylistAgent();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
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
                    public void onPlaylistMetadataChanged(MediaSession2 session,
                            MediaPlaylistAgent playlistAgent,
                            MediaMetadata2 metadata) {
                        super.onPlaylistMetadataChanged(session, playlistAgent, metadata);
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
        }
        TestServiceRegistry.getInstance().cleanUp();
    }

    /**
     * Test if the {@link MediaSession2TestBase.TestControllerCallback} wraps the callback proxy
     * without missing any method.
     */
    @Test
    public void testTestControllerCallback() {
        prepareLooper();
        Method[] methods = TestControllerCallback.class.getMethods();
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
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPause() {
        prepareLooper();
        mController.pause();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testReset() {
        prepareLooper();
        mController.reset();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mResetCalled);
    }

    @Test
    public void testPrepare() {
        prepareLooper();
        mController.prepare();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void testGettersAfterConnected() throws InterruptedException {
        prepareLooper();
        final int state = MediaPlayerInterface.PLAYER_STATE_PLAYING;
        final int bufferingState = MediaPlayerInterface.BUFFERING_STATE_BUFFERING_COMPLETE;
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
        mMockAgent.mCurrentMediaItem = currentMediaItem;

        MediaController2 controller = createController(mSession.getToken());
        // setTimeDiff is package-private, so cannot be called here.
//        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        assertEquals(currentMediaItem, controller.getCurrentMediaItem());
    }

    @Test
    public void testUpdatePlayer() throws InterruptedException {
        prepareLooper();
        final int testState = MediaPlayerInterface.PLAYER_STATE_PLAYING;
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(3);
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

        MockPlaylistAgent agent = new MockPlaylistAgent();
        agent.mPlaylist = testPlaylist;

        mSession.updatePlayer(player, agent, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetSessionActivity() {
        prepareLooper();
        PendingIntent sessionActivity = mController.getSessionActivity();
        assertEquals(mContext.getPackageName(), sessionActivity.getCreatorPackage());
        assertEquals(Process.myUid(), sessionActivity.getCreatorUid());
    }

    @Test
    public void testSetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> list = TestUtils.createPlaylist(2);
        mController.setPlaylist(list, null /* Metadata */);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetPlaylistCalled);
        assertNull(mMockAgent.mMetadata);

        assertNotNull(mMockAgent.mPlaylist);
        assertEquals(list.size(), mMockAgent.mPlaylist.size());
        for (int i = 0; i < list.size(); i++) {
            // MediaController2.setPlaylist does not ensure the equality of the items.
            assertEquals(list.get(i).getMediaId(), mMockAgent.mPlaylist.get(i).getMediaId());
        }
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController2, List, MediaMetadata2)}.
     */
    @Test
    public void testGetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testList = TestUtils.createPlaylist(2);
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
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public List<MediaItem2> getPlaylist() {
                return testList;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setId("testControllerCallback_onPlaylistChanged")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .setPlaylistAgent(agent)
                .build()) {
            MediaController2 controller = createController(
                    session.getToken(), true, callback);
            agent.notifyPlaylistChanged();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(listFromCallback.get(), controller.getPlaylist());
        }
    }

    @Test
    public void testUpdatePlaylistMetadata() throws InterruptedException {
        prepareLooper();
        final MediaMetadata2 testMetadata = TestUtils.createMetadata();
        mController.updatePlaylistMetadata(testMetadata);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mUpdatePlaylistMetadataCalled);
        assertNotNull(mMockAgent.mMetadata);
        assertEquals(testMetadata.getMediaId(), mMockAgent.mMetadata.getMediaId());
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
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public MediaMetadata2 getPlaylistMetadata() {
                return testMetadata;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setId("testGetPlaylistMetadata")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .setPlaylistAgent(agent)
                .build()) {
            MediaController2 controller = createController(session.getToken(), true, callback);
            agent.notifyPlaylistMetadataChanged();
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
     * Test whether {@link MediaSession2#setPlaylist(List, MediaMetadata2)} is notified
     * through the
     * {@link ControllerCallback#onPlaylistMetadataChanged(MediaController2, MediaMetadata2)}
     * if the controller doesn't have {@link SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST} but
     * {@link SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST_METADATA}.
     */
    @Test
    public void testControllerCallback_onPlaylistMetadataChanged() throws InterruptedException {
        prepareLooper();
        final MediaItem2 item = TestUtils.createMediaItemWithMetadata();
        final List<MediaItem2> list = TestUtils.createPlaylist(2);
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
                    SessionCommandGroup2 commands = new SessionCommandGroup2();
                    commands.addCommand(new SessionCommand2(
                              SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST_METADATA));
                    return commands;
                }
                return super.onConnect(session, controller);
            }
        };
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public MediaMetadata2 getPlaylistMetadata() {
                return item.getMetadata();
            }

            @Override
            public List<MediaItem2> getPlaylist() {
                return list;
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setId("testControllerCallback_onPlaylistMetadataChanged")
                .setSessionCallback(sHandlerExecutor, sessionCallback)
                .setPlaylistAgent(agent)
                .build()) {
            MediaController2 controller = createController(session.getToken(), true, callback);
            agent.notifyPlaylistMetadataChanged();
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
                // setTimeDiff is package-private, so cannot be called here.
//                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_onBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(3);
        final MediaItem2 testItem = testPlaylist.get(0);
        final int testBufferingState = MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                // setTimeDiff is package-private, so cannot be called here.
//                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testItem, item);
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mSession.setPlaylist(testPlaylist, null);
        mPlayer.mBufferedPosition = testBufferingPosition;
        mPlayer.notifyBufferingStateChanged(testItem.getDataSourceDesc(), testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_onPlayerStateChanged() throws InterruptedException {
        prepareLooper();
        final int testPlayerState = MediaPlayerInterface.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
                // setTimeDiff is package-private, so cannot be called here.
//                controller.setTimeDiff(Long.valueOf(0));
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        final MediaController2 controller = createController(mSession.getToken(), true, callback);
        mPlayer.mCurrentPosition = testPosition;
        mPlayer.notifyPlaybackState(testPlayerState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAddPlaylistItem() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = TestUtils.createMediaItemWithMetadata();
        mController.addPlaylistItem(testIndex, testMediaItem);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mAddPlaylistItemCalled);
        assertEquals(testIndex, mMockAgent.mIndex);
        // MediaController2.addPlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaItem.getMediaId(), mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testRemovePlaylistItem() throws InterruptedException {
        prepareLooper();
        mMockAgent.mPlaylist = TestUtils.createPlaylist(2);

        // Recreate controller for sending removePlaylistItem.
        // It's easier to ensure that MediaController2.getPlaylist() returns the playlist from the
        // agent.
        MediaController2 controller = createController(mSession.getToken());
        MediaItem2 targetItem = controller.getPlaylist().get(0);
        controller.removePlaylistItem(targetItem);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mRemovePlaylistItemCalled);
        assertEquals(targetItem, mMockAgent.mItem);
    }

    @Test
    public void testReplacePlaylistItem() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = TestUtils.createMediaItemWithMetadata();
        mController.replacePlaylistItem(testIndex, testMediaItem);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mReplacePlaylistItemCalled);
        // MediaController2.replacePlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaItem.getMediaId(), mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testSkipToPreviousItem() throws InterruptedException {
        prepareLooper();
        mController.skipToPreviousItem();
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToNextItem() throws InterruptedException {
        prepareLooper();
        mController.skipToNextItem();
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToPlaylistItem() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mSession.getToken());
        MediaItem2 targetItem = TestUtils.createMediaItemWithMetadata();
        controller.skipToPlaylistItem(targetItem);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSkipToPlaylistItemCalled);
        assertEquals(targetItem, mMockAgent.mItem);
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController2, int)}.
     */
    @Test
    public void testGetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public int getShuffleMode() {
                return testShuffleMode;
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        mSession.updatePlayer(mPlayer, agent, null);
        MediaController2 controller = createController(mSession.getToken(), true, callback);
        agent.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    @Test
    public void testSetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        mController.setShuffleMode(testShuffleMode);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mMockAgent.mShuffleMode);
    }

    /**
     * This also tests {@link ControllerCallback#onRepeatModeChanged(MediaController2, int)}.
     */
    @Test
    public void testGetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public int getRepeatMode() {
                return testRepeatMode;
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        mSession.updatePlayer(mPlayer, agent, null);
        MediaController2 controller = createController(mSession.getToken(), true, callback);
        agent.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testSetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        mController.setRepeatMode(testRepeatMode);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mMockAgent.mRepeatMode);
    }

    @Test
    public void testSetVolumeTo() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        TestVolumeProvider volumeProvider =
                new TestVolumeProvider(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(new MockPlayer(0), null, volumeProvider);
        final MediaController2 controller = createController(mSession.getToken(), true, null);

        final int targetVolume = 50;
        controller.setVolumeTo(targetVolume, 0 /* flags */);
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

        mSession.updatePlayer(new MockPlayer(0), null, volumeProvider);
        final MediaController2 controller = createController(mSession.getToken(), true, null);

        final int direction = AudioManager.ADJUST_RAISE;
        controller.adjustVolume(direction, 0 /* flags */);
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
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.setAudioAttributes(attrs);
        mSession.updatePlayer(mPlayer, null, null);

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
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream)
                .build();
        mPlayer.setAudioAttributes(attrs);
        mSession.updatePlayer(mPlayer, null, null);

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
    public void testGetPackageName() {
        prepareLooper();
        assertEquals(mContext.getPackageName(), mController.getSessionToken().getPackageName());
    }

    @Test
    public void testSendCustomCommand() throws InterruptedException {
        prepareLooper();
        // TODO(jaewan): Need to revisit with the permission.
        final SessionCommand2 testCommand =
                new SessionCommand2(SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE);
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "testSendCustomCommand");

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public void onCustomCommand(MediaSession2 session, ControllerInfo controller,
                    SessionCommand2 customCommand, Bundle args, ResultReceiver cb) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(testCommand, customCommand);
                assertTrue(TestUtils.equals(testArgs, args));
                assertNull(cb);
                latch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final MediaController2 controller = createController(mSession.getToken());
        controller.sendCustomCommand(testCommand, testArgs, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
                mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
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
            public void onFastForward(MediaSession2 session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onRewind(MediaSession2 session, ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPlayFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                super.onPlayFromSearch(session, controller, query, extras);
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPlayFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPlayFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPrepareFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPrepareFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
            public void onPrepareFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
        final int ratingType = Rating2.RATING_5_STARS;
        final float ratingValue = 3.5f;
        final Rating2 rating = Rating2.newStarRating(ratingType, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public void onSetRating(MediaSession2 session, ControllerInfo controller,
                    String mediaIdOut, Rating2 ratingOut) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(rating, ratingOut);
                latch.countDown();
            }
        };

        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
                    mSession = new MediaSession2.Builder(mContext)
                            .setPlayer(mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final MediaController2 controller = createController(mSession.getToken());
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int state = MediaPlayerInterface.PLAYER_STATE_ERROR;
                    for (int i = 0; i < 100; i++) {
                        // triggers call from session to controller.
                        player.notifyPlaybackState(state);
                        // triggers call from controller to session.
                        controller.play();

                        // Repeat above
                        player.notifyPlaybackState(state);
                        controller.pause();
                        player.notifyPlaybackState(state);
                        controller.reset();
                        player.notifyPlaybackState(state);
                        controller.skipToNextItem();
                        player.notifyPlaybackState(state);
                        controller.skipToPreviousItem();
                    }
                    // This may hang if deadlock happens.
                    latch.countDown();
                }
            });
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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

    // Temporaily commenting out, since we don't have the Mock services yet.
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
//    @Test
//    public void testConnectToService_sessionService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaSessionService2.ID);
//    }
//
//    @Test
//    public void testConnectToService_libraryService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaLibraryService2.ID);
//    }
//
//    public void testConnectToService(String id) throws InterruptedException {
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
//                    mPlayer = (MockPlayer) session.getPlayer();
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

    @Test
    public void testControllerAfterSessionIsGone_session() throws InterruptedException {
        prepareLooper();
        testControllerAfterSessionIsClosed(mSession.getToken().getId());
    }

    // Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testControllerAfterSessionIsClosed_sessionService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaSessionService2.ID);
//        testControllerAfterSessionIsClosed(MockMediaSessionService2.ID);
//    }

    @Test
    public void testSubscribeRouteInfo() throws InterruptedException {
        prepareLooper();
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public void onSubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                mLatch.countDown();
            }

            @Override
            public void onUnsubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                mLatch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
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
            public void onSelectRoute(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller, @NonNull Bundle route) {
                assertEquals(mContext.getPackageName(), controller.getPackageName());
                assertTrue(TestUtils.equals(route, testRoute));
                mLatch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
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

    @Test
    public void testClose_session() throws InterruptedException {
        prepareLooper();
        final String id = mSession.getToken().getId();
        mController.close();
        // close is done immediately for session.
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(id);
    }

    // Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testClose_sessionService() throws InterruptedException {
//        prepareLooper();
//        testCloseFromService(MockMediaSessionService2.ID);
//    }
//
//    @Test
//    public void testClose_libraryService() throws InterruptedException {
//        prepareLooper();
//        testCloseFromService(MockMediaLibraryService2.ID);
//    }
//
//    private void testCloseFromService(String id) throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        TestServiceRegistry.getInstance().setSessionServiceCallback(new SessionServiceCallback() {
//            @Override
//            public void onCreated() {
//                // Do nothing.
//            }
//
//            @Override
//            public void onDestroyed() {
//                latch.countDown();
//            }
//        });
//        mController = createController(TestUtils.getServiceToken(mContext, id));
//        mController.close();
//        // Wait until close triggers onDestroy() of the session service.
//        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
//        assertNull(TestServiceRegistry.getInstance().getServiceInstance());
//        testNoInteraction();
//
//        // Test whether the controller is notified about later close of the session or
//        // re-creation.
//        testControllerAfterSessionIsClosed(id);
//    }

    private void testControllerAfterSessionIsClosed(final String id) throws InterruptedException {
        // This cause session service to be died.
        mSession.close();
        waitForDisconnect(mController, true);
        testNoInteraction();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
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
        mSession.sendCustomCommand(customCommand, null);
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        setRunnableForOnCustomCommand(mController, null);
    }

    // TODO(jaewan): Add  test for service connect rejection, when we differentiate session
    //               active/inactive and connection accept/refuse

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

    class TestSessionCallback extends SessionCallback {
        CountDownLatch mLatch;

        void resetLatchCount(int count) {
            mLatch = new CountDownLatch(count);
        }
    }
}
