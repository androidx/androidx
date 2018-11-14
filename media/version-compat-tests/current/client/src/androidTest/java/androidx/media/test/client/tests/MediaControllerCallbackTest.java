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

import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;

import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_NULL_ITEM;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_UNKONWN_ITEM;
import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_LIBRARY_SERVICE;
import static androidx.media.test.lib.MediaSessionConstants
        .TEST_CONTROLLER_CALLBACK_SESSION_REJECTS;
import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;

import androidx.media.AudioAttributesCompat;
import androidx.media.test.client.MediaTestUtils;
import androidx.media.test.client.RemoteMediaSession;
import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaController;
import androidx.media2.MediaController.PlaybackInfo;
import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaSession;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.SessionCommand;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionPlayer;
import androidx.media2.SessionToken;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController.ControllerCallback}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaControllerCallbackTest extends MediaSessionTestBase {

    // Since ControllerInfo cannot be passed, we just pass null and the service app chooses the
    // right controller by using the package name.
    static final ControllerInfo TEST_CONTROLLER_INFO = null;
    RemoteMediaSession mRemoteSession2;
    MediaController mController;

    final List<RemoteMediaSession> mRemoteSessionList = new ArrayList<>();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mRemoteSession2 = createRemoteMediaSession(DEFAULT_TEST_NAME);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        for (int i = 0; i < mRemoteSessionList.size(); i++) {
            RemoteMediaSession session = mRemoteSessionList.get(i);
            if (session != null) {
                session.cleanUp();
            }
        }
    }

    @Test
    public void testConnection_sessionAccepts() throws InterruptedException {
        prepareLooper();
        // createController() uses controller callback to wait until the controller becomes
        // available.
        MediaController controller = createController(mRemoteSession2.getToken());
        assertNotNull(controller);
    }

    @Test
    public void testConnection_sessionRejects() throws InterruptedException {
        prepareLooper();
        RemoteMediaSession session =
                createRemoteMediaSession(TEST_CONTROLLER_CALLBACK_SESSION_REJECTS);

        MediaController controller = createController(session.getToken(),
                false /* waitForConnect */, null);
        assertNotNull(controller);
        waitForConnect(controller, false /* expected */);
        waitForDisconnect(controller, true /* expected */);

        session.cleanUp();
    }

    @Test
    public void testConnection_toLibraryService() throws InterruptedException {
        prepareLooper();
        SessionToken token = new SessionToken(mContext, MOCK_MEDIA_LIBRARY_SERVICE);
        MediaController controller = createController(token);
        assertNotNull(controller);
    }

    @Test
    public void testConnection_sessionClosed() throws InterruptedException {
        prepareLooper();
        MediaController controller = createController(mRemoteSession2.getToken());

        mRemoteSession2.close();
        waitForDisconnect(controller, true);
    }

    @Test
    public void testConnection_controllerClosed() throws InterruptedException {
        prepareLooper();
        MediaController controller = createController(mRemoteSession2.getToken());

        controller.close();
        waitForDisconnect(controller, true);
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterSessionClose_session() throws InterruptedException {
        prepareLooper();
        SessionToken token = mRemoteSession2.getToken();
        mController = createController(token);
        testControllerAfterSessionIsClosed(DEFAULT_TEST_NAME);
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterControllerClose_session() throws InterruptedException {
        prepareLooper();
        final SessionToken token = mRemoteSession2.getToken();
        mController = createController(token);

        mController.close();
        // close is done immediately for session.
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(DEFAULT_TEST_NAME);
    }

    @Test
    @LargeTest
    public void testConnection_withLongPlaylist() throws InterruptedException {
        prepareLooper();
        final int playlistSize = 5000;
        mRemoteSession2.getMockPlayer().createAndSetDummyPlaylist(playlistSize);

        final CountDownLatch latch = new CountDownLatch(1);
        MediaController controller = new MediaController(mContext, mRemoteSession2.getToken(),
                sHandlerExecutor, new MediaController.ControllerCallback() {
                    @Override
                    public void onConnected(MediaController controller,
                            SessionCommandGroup allowedCommands) {
                        super.onConnected(controller, allowedCommands);
                        latch.countDown();
                    }
                });
        assertNotNull(controller);
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // After connection, getPlaylist() should return the playlist which is set to the player.
        List<MediaItem> playlist = controller.getPlaylist();
        assertNotNull(playlist);
        assertEquals(playlistSize, playlist.size());
        for (int i = 0; i < playlist.size(); i++) {
            assertEquals(TestUtils.getMediaIdInDummyList(i), playlist.get(i).getMediaId());
        }
    }

    @Test
    public void testControllerCallback_sessionUpdatePlayer() throws InterruptedException {
        prepareLooper();
        final int testState = SessionPlayer.PLAYER_STATE_PLAYING;
        final List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(3);
        final AudioAttributesCompat testAudioAttributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_RING).build();
        final CountDownLatch latch = new CountDownLatch(3);
        mController = createController(mRemoteSession2.getToken(),
                true /* waitForConnect */, new MediaController.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(MediaController controller, int state) {
                        assertEquals(mController, controller);
                        assertEquals(testState, state);
                        latch.countDown();
                    }

                    @Override
                    public void onPlaylistChanged(MediaController controller,
                            List<MediaItem> list, MediaMetadata metadata) {
                        assertEquals(mController, controller);
                        MediaTestUtils.assertNotMediaItemSubclass(list);
                        MediaTestUtils.assertMediaIdEquals(testPlaylist, list);
                        assertNull(metadata);
                        latch.countDown();
                    }

                    @Override
                    public void onPlaybackInfoChanged(MediaController controller,
                            MediaController.PlaybackInfo info) {
                        assertEquals(mController, controller);
                        assertEquals(testAudioAttributes, info.getAudioAttributes());
                        latch.countDown();
                    }
                });

        Bundle config = RemoteMediaSession.createMockPlayerConnectorConfig(
                testState, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, testAudioAttributes, testPlaylist, null /* currentItem */,
                null /* metadata */);

        mRemoteSession2.updatePlayer(config);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCurrentMediaItemChanged() throws Exception {
        prepareLooper();
        final int listSize = 5;
        final List<MediaItem> list = MediaTestUtils.createFileMediaItems(listSize);
        mRemoteSession2.getMockPlayer().setPlaylistWithDummyItem(list);

        final int currentItemIndex = 3;
        final MediaItem currentItem = list.get(currentItemIndex);
        final CountDownLatch latchForControllerCallback = new CountDownLatch(3);
        MediaController controller = createController(
                mRemoteSession2.getToken(), true, new MediaController.ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(MediaController controller,
                            MediaItem item) {
                        switch ((int) latchForControllerCallback.getCount()) {
                            case 3:
                                // No check needed..
                                break;
                            case 2:
                                MediaTestUtils.assertNotMediaItemSubclass(item);
                                assertEquals(currentItem.getMediaId(), item.getMediaId());
                                break;
                            case 1:
                                assertNull(item);
                        }
                        latchForControllerCallback.countDown();
                    }
                });
        // Player notifies with the unknown item. Still OK.
        mRemoteSession2.getMockPlayer().notifyCurrentMediaItemChanged(INDEX_FOR_UNKONWN_ITEM);

        // Known ITEM should be notified through the onCurrentMediaItemChanged.
        mRemoteSession2.getMockPlayer().notifyCurrentMediaItemChanged(currentItemIndex);

        // Null ITEM becomes null MediaItem.
        mRemoteSession2.getMockPlayer().notifyCurrentMediaItemChanged(INDEX_FOR_NULL_ITEM);
        assertTrue(latchForControllerCallback.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController#getPlaybackSpeed()}.
     */
    @Test
    public void testOnPlaybackSpeedChanged() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mRemoteSession2.getMockPlayer().setPlaybackSpeed(speed);

        final CountDownLatch latchForControllerCallback = new CountDownLatch(1);
        MediaController controller = createController(
                mRemoteSession2.getToken(), true, new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaybackSpeedChanged(MediaController controller,
                            float speedOut) {
                        assertEquals(speed, speedOut, 0.0f);
                        latchForControllerCallback.countDown();
                    }
                });
        mRemoteSession2.getMockPlayer().notifyPlaybackSpeedChanged(speed);
        assertTrue(latchForControllerCallback.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
    }

    /**
     * This also tests {@link MediaController#getPlaybackInfo()}.
     */
    @Test
    public void testOnPlaybackInfoChanged() throws Exception {
        prepareLooper();

        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VOLUME_CONTROL_ABSOLUTE;

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public void onPlaybackInfoChanged(MediaController controller, PlaybackInfo info) {
                assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
                assertEquals(attrs, info.getAudioAttributes());
                assertEquals(volumeControlType, info.getPlaybackType());
                assertEquals(maxVolume, info.getMaxVolume());
                assertEquals(currentVolume, info.getCurrentVolume());
                latch.countDown();
            }
        };
        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);

        Bundle playerConfig = RemoteMediaSession.createMockPlayerConnectorConfig(
                volumeControlType, maxVolume, currentVolume, attrs);
        mRemoteSession2.updatePlayer(playerConfig);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
        assertEquals(attrs, info.getAudioAttributes());
        assertEquals(volumeControlType, info.getControlType());
        assertEquals(maxVolume, info.getMaxVolume());
        assertEquals(currentVolume, info.getCurrentVolume());
    }

    @Test
    public void testOnPlaybackInfoChanged_byAudioAttributesChange() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .build();
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaybackInfoChanged(MediaController controller,
                            PlaybackInfo info) {
                        assertNotNull(info.getAudioAttributes());
                        assertEquals(attrs, info.getAudioAttributes());
                        latch.countDown();
                    }
                };
        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().notifyAudioAttributesChanged(attrs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController#getPlaylist()}.
     */
    @Test
    public void testOnPlaylistChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem> testList = MediaTestUtils.createFileMediaItems(2);
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(MediaController controller,
                            List<MediaItem> playlist, MediaMetadata metadata) {
                        assertNotNull(playlist);
                        MediaTestUtils.assertNotMediaItemSubclass(playlist);
                        MediaTestUtils.assertMediaIdEquals(testList, playlist);
                        listFromCallback.set(playlist);
                        latch.countDown();
                    }
                };
        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);

        mRemoteSession2.getMockPlayer().setPlaylist(testList);
        mRemoteSession2.getMockPlayer().notifyPlaylistChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(listFromCallback.get(), controller.getPlaylist());
    }

    @Test
    @LargeTest
    public void testOnPlaylistChanged_longList() throws InterruptedException {
        prepareLooper();
        final int listSize = 5000;
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(MediaController controller,
                            List<MediaItem> playlist, MediaMetadata metadata) {
                        assertNotNull(playlist);
                        assertEquals(listSize, playlist.size());
                        for (int i = 0; i < playlist.size(); i++) {
                            assertEquals(TestUtils.getMediaIdInDummyList(i),
                                    playlist.get(i).getMediaId());
                        }
                        listFromCallback.set(playlist);
                        latch.countDown();
                    }
                };
        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().createAndSetDummyPlaylist(listSize);
        mRemoteSession2.getMockPlayer().notifyPlaylistChanged();

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(listFromCallback.get(), controller.getPlaylist());
    }

    /**
     * This also tests {@link MediaController#getPlaylistMetadata()}.
     */
    @Test
    public void testOnPlaylistMetadataChanged() throws InterruptedException {
        prepareLooper();
        final MediaMetadata testMetadata = MediaTestUtils.createMetadata();
        final AtomicReference<MediaMetadata> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaylistMetadataChanged(MediaController controller,
                            MediaMetadata metadata) {
                        assertNotNull(metadata);
                        assertEquals(testMetadata.getMediaId(), metadata.getMediaId());
                        metadataFromCallback.set(metadata);
                        latch.countDown();
                    }
                };
        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setPlaylistMetadata(testMetadata);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        player.notifyPlaylistMetadataChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(metadataFromCallback.get().getMediaId(),
                controller.getPlaylistMetadata().getMediaId());
    }

    @Test
    @LargeTest
    public void testOnPlaylistMetadataChanged_withManyLargeImages() throws InterruptedException {
        prepareLooper();
        final int imageCount = 20;
        final int originalWidth = 1024;
        final int originalHeight = 1024;

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaylistMetadataChanged(MediaController controller,
                            MediaMetadata metadata) {
                        assertNotNull(metadata);
                        Set<String> keySet = metadata.keySet();
                        assertEquals(imageCount, keySet.size());
                        for (String key : keySet) {
                            Bitmap value = metadata.getBitmap(key);
                            assertTrue("Bitmap should have been scaled down.",
                                    originalWidth > value.getWidth()
                                            && originalHeight > value.getHeight());
                        }
                        latch.countDown();
                    }
                };
        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setPlaylistMetadataWithLargeBitmaps(imageCount, originalWidth, originalHeight);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        player.notifyPlaylistMetadataChanged();
        if (Build.VERSION.SDK_INT <= 19) {
            // Due to the GC, time takes longer than expected.
            // It seems to be due to the Dalvik GC mechanism.
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } else {
            assertTrue(latch.await(3, TimeUnit.SECONDS));
        }
    }

    /**
     * This also tests {@link MediaController#getShuffleMode()}.
     */
    @Test
    public void testOnShuffleModeChanged() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onShuffleModeChanged(MediaController controller, int shuffleMode) {
                        assertEquals(testShuffleMode, shuffleMode);
                        latch.countDown();
                    }
                };
        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setShuffleMode(testShuffleMode);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        player.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    /**
     * This also tests {@link MediaController#getRepeatMode()}.
     */
    @Test
    public void testOnRepeatModeChanged() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onRepeatModeChanged(MediaController controller, int repeatMode) {
                        assertEquals(testRepeatMode, repeatMode);
                        latch.countDown();
                    }
                };

        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setRepeatMode(testRepeatMode);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        player.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testOnPlaybackCompleted() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
                    @Override
                    public void onPlaybackCompleted(MediaController controller) {
                        latch.countDown();
                    }
                };

        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        player.notifyPlaybackCompleted();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnSeekCompleted() throws InterruptedException {
        prepareLooper();
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public void onSeekCompleted(MediaController controller, long position) {
                controller.setTimeDiff(0L);
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlayer().setPlayerState(SessionPlayer.PLAYER_STATE_PAUSED);
        mRemoteSession2.getMockPlayer().setCurrentPosition(testPosition);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);

        final List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(3);
        final int targetItemIndex = 0;
        final int testBufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;

        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController controller, MediaItem item,
                    int state) {
                controller.setTimeDiff(0L);
                MediaTestUtils.assertNotMediaItemSubclass(item);
                assertEquals(testPlaylist.get(targetItemIndex).getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlayer().setPlaylistWithDummyItem(testPlaylist);

        RemoteMediaSession.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setBufferedPosition(testBufferingPosition);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        // Since we cannot pass the DataSourceDesc directly, send the item index so that the player
        // can select which item's state change should be notified.
        player.notifyBufferingStateChanged(targetItemIndex, testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnPlayerStateChanged() throws InterruptedException {
        prepareLooper();
        final int testPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController controller, int state) {
                controller.setTimeDiff(0L);
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlayer().setCurrentPosition(testPosition);

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().notifyPlayerStateChanged(testPlayerState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnAllowedCommandsChanged() throws InterruptedException {
        prepareLooper();
        final SessionCommandGroup commands = new SessionCommandGroup();
        commands.addCommand(SessionCommand.COMMAND_CODE_PLAYER_PLAY);
        commands.addCommand(SessionCommand.COMMAND_CODE_PLAYER_PAUSE);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public void onAllowedCommandsChanged(MediaController controller,
                    SessionCommandGroup commandsOut) {
                assertNotNull(commandsOut);
                Set<SessionCommand> expected = commands.getCommands();
                Set<SessionCommand> actual = commandsOut.getCommands();

                assertNotNull(actual);
                assertEquals(expected.size(), actual.size());
                for (SessionCommand command : expected) {
                    assertTrue(actual.contains(command));
                }
                latch.countDown();
            }
        };

        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.setAllowedCommands(TEST_CONTROLLER_INFO, commands);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCustomCommand() throws InterruptedException {
        prepareLooper();
        final SessionCommand testCommand = new SessionCommand(
                SessionCommand.COMMAND_CODE_PLAYER_PREPARE);
        final Bundle testArgs = TestUtils.createTestBundle();

        final CountDownLatch latch = new CountDownLatch(2);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public MediaController.ControllerResult onCustomCommand(MediaController controller,
                    SessionCommand command, Bundle args) {
                assertEquals(testCommand, command);
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new MediaController.ControllerResult(RESULT_CODE_SUCCESS, null);
            }
        };
        MediaController controller = createController(mRemoteSession2.getToken(), true, callback);

        // TODO(jaewan): Test with multiple controllers
        mRemoteSession2.broadcastCustomCommand(testCommand, testArgs);

        // TODO(jaewan): Test receivers as well.
        mRemoteSession2.sendCustomCommand(TEST_CONTROLLER_INFO, testCommand, testArgs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCustomLayoutChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaSession.CommandButton> buttons = new ArrayList<>();

        MediaSession.CommandButton button = new MediaSession.CommandButton.Builder()
                .setCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PLAY))
                .setDisplayName("button")
                .build();
        buttons.add(button);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController.ControllerCallback callback =
                new MediaController.ControllerCallback() {
            @Override
            public int onSetCustomLayout(MediaController controller,
                    List<MediaSession.CommandButton> layout) {
                assertEquals(layout.size(), buttons.size());
                for (int i = 0; i < layout.size(); i++) {
                    assertEquals(layout.get(i).getCommand(), buttons.get(i).getCommand());
                    assertEquals(layout.get(i).getDisplayName(), buttons.get(i).getDisplayName());
                }
                latch.countDown();
                return RESULT_CODE_SUCCESS;
            }
        };
        final MediaController controller =
                createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.setCustomLayout(TEST_CONTROLLER_INFO, buttons);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void testControllerAfterSessionIsClosed(String id) throws InterruptedException {
        // This cause session service to be died.
        mRemoteSession2.close();
        waitForDisconnect(mController, true);
        testNoInteraction();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        mRemoteSession2 = createRemoteMediaSession(id);
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

        mRemoteSession2.broadcastCustomCommand(customCommand, null);

        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        setRunnableForOnCustomCommand(mController, null);
    }

    RemoteMediaSession createRemoteMediaSession(String id) {
        RemoteMediaSession session = new RemoteMediaSession(id, mContext);
        mRemoteSessionList.add(session);
        return session;
    }
}
