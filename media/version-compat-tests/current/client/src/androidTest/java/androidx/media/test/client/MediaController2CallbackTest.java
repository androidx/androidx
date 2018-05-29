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

import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;

import static androidx.media.MediaMetadata2.METADATA_KEY_DURATION;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_NULL_DSD;
import static androidx.media.test.lib.CommonConstants.INDEX_FOR_UNKONWN_DSD;
import static androidx.media.test.lib.CommonConstants.KEY_ARGUMENTS;
import static androidx.media.test.lib.CommonConstants.KEY_AUDIO_ATTRIBUTES;
import static androidx.media.test.lib.CommonConstants.KEY_CURRENT_VOLUME;
import static androidx.media.test.lib.CommonConstants.KEY_CUSTOM_COMMAND;
import static androidx.media.test.lib.CommonConstants.KEY_MAX_VOLUME;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYER_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST;
import static androidx.media.test.lib.CommonConstants.KEY_RESULT_RECEIVER;
import static androidx.media.test.lib.CommonConstants.KEY_VOLUME_CONTROL_TYPE;
import static androidx.media.test.lib.MediaSession2Constants.CustomCommands.UPDATE_PLAYER;
import static androidx.media.test.lib.MediaSession2Constants.CustomCommands
        .UPDATE_PLAYER_WITH_VOLUME_PROVIDER;
import static androidx.media.test.lib.MediaSession2Constants
        .TEST_CONTROLLER_CALLBACK_SESSION_REJECTS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.BaseMediaPlayer;
import androidx.media.MediaController2;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlaylistAgent;
import androidx.media.MediaSession2;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.SessionToken2;
import androidx.media.test.lib.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController2.ControllerCallback}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaController2CallbackTest extends MediaSession2TestBase {

    // Since ControllerInfo cannot be passed, we just pass null and the service app chooses the
    // right controller by using the package name.
    private static final ControllerInfo TEST_CONTROLLER_INFO = null;
    private MediaController2 mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testConnection_sessionAccepts() throws InterruptedException {
        prepareLooper();
        // createController() uses controller callback to wait until the controller becomes
        // available.
        MediaController2 controller = createController(mRemoteSession2.getToken());
        assertNotNull(controller);
    }

    @Test
    public void testConnection_sessionRejects() throws InterruptedException {
        prepareLooper();
        RemoteMediaSession2 session2 =
                new RemoteMediaSession2(TEST_CONTROLLER_CALLBACK_SESSION_REJECTS, mContext);

        MediaController2 controller = createController(session2.getToken(),
                false /* waitForConnect */, null);
        assertNotNull(controller);
        waitForConnect(controller, false /* expected */);
        waitForDisconnect(controller, true /* expected */);
    }

    @Test
    public void testConnection_sessionClosed() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mRemoteSession2.getToken());

        mRemoteSession2.close();
        waitForDisconnect(controller, true);
    }

    @Test
    public void testConnection_controllerClosed() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mRemoteSession2.getToken());

        controller.close();
        waitForDisconnect(controller, true);
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterSessionClose_session() throws InterruptedException {
        prepareLooper();
        SessionToken2 token = mRemoteSession2.getToken();
        mController = createController(token);
        testControllerAfterSessionIsClosed(token.getId());
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterControllerClose_session() throws InterruptedException {
        prepareLooper();
        final SessionToken2 token = mRemoteSession2.getToken();
        mController = createController(token);

        mController.close();
        // close is done immediately for session.
        testNoInteraction();

        // Test whether the controller is notified about later close of the session or
        // re-creation.
        testControllerAfterSessionIsClosed(token.getId());
    }

    @Test
    public void testControllerCallback_sessionUpdatePlayer() throws InterruptedException {
        prepareLooper();
        final int testState = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        final List<MediaItem2> testPlaylist = MediaTestUtils.createPlaylist(3);
        final AudioAttributesCompat testAudioAttributes = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(AudioManager.STREAM_RING).build();
        final CountDownLatch latch = new CountDownLatch(3);
        mController = createController(mRemoteSession2.getToken(),
                true /* waitForConnect */, new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(MediaController2 controller, int state) {
                        assertEquals(mController, controller);
                        assertEquals(testState, state);
                        latch.countDown();
                    }

                    @Override
                    public void onPlaylistChanged(MediaController2 controller,
                            List<MediaItem2> list, MediaMetadata2 metadata) {
                        assertEquals(mController, controller);
                        assertEquals(testPlaylist, list);
                        assertNull(metadata);
                        latch.countDown();
                    }

                    @Override
                    public void onPlaybackInfoChanged(MediaController2 controller,
                            MediaController2.PlaybackInfo info) {
                        assertEquals(mController, controller);
                        assertEquals(testAudioAttributes, info.getAudioAttributes());
                        latch.countDown();
                    }
                });

        Bundle args = new Bundle();
        args.putInt(KEY_PLAYER_STATE, testState);
        args.putParcelableArrayList(
                KEY_PLAYLIST, MediaTestUtils.playlistToParcelableArrayList(testPlaylist));
        args.putBundle(KEY_AUDIO_ATTRIBUTES, testAudioAttributes.toBundle());

        mRemoteSession2.runCustomTestCommands(UPDATE_PLAYER, args);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCurrentMediaItemChanged() throws Exception {
        prepareLooper();
        final int listSize = 5;
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(listSize);
        mRemoteSession2.getMockPlaylistAgent().setPlaylistNewDsd(list);

        final int currentItemIndex = 3;
        final MediaItem2 currentItem = list.get(currentItemIndex);
        final CountDownLatch latchForControllerCallback = new CountDownLatch(2);
        MediaController2 controller = createController(
                mRemoteSession2.getToken(), true, new MediaController2.ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(MediaController2 controller,
                            MediaItem2 item) {
                        switch ((int) latchForControllerCallback.getCount()) {
                            case 2:
                                assertEquals(currentItem.getMediaId(), item.getMediaId());
                                break;
                            case 1:
                                assertNull(item);
                        }
                        latchForControllerCallback.countDown();
                    }
                });
        // Player notifies with the unknown dsd. Should be ignored.
        mRemoteSession2.getMockPlayer().notifyCurrentDataSourceChanged(INDEX_FOR_UNKONWN_DSD);

        // Known DSD should be notified through the onCurrentMediaItemChanged.
        mRemoteSession2.getMockPlayer().notifyCurrentDataSourceChanged(currentItemIndex);

        // Null DSD becomes null MediaItem2.
        mRemoteSession2.getMockPlayer().notifyCurrentDataSourceChanged(INDEX_FOR_NULL_DSD);
        assertTrue(latchForControllerCallback.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController2#getPlaybackSpeed()}.
     */
    @Test
    public void testOnPlaybackSpeedChanged() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mRemoteSession2.getMockPlayer().setPlaybackSpeed(speed);

        final CountDownLatch latchForControllerCallback = new CountDownLatch(1);
        MediaController2 controller = createController(
                mRemoteSession2.getToken(), true, new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaybackSpeedChanged(MediaController2 controller,
                            float speedOut) {
                        assertEquals(speed, speedOut, 0.0f);
                        latchForControllerCallback.countDown();
                    }
                });
        mRemoteSession2.getMockPlayer().notifyPlaybackSpeedChanged(speed);
        assertTrue(latchForControllerCallback.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
    }

    /**
     * This also tests {@link MediaController2#getPlaybackInfo()}.
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
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onPlaybackInfoChanged(MediaController2 controller, PlaybackInfo info) {
                assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
                assertEquals(attrs, info.getAudioAttributes());
                assertEquals(volumeControlType, info.getPlaybackType());
                assertEquals(maxVolume, info.getMaxVolume());
                assertEquals(currentVolume, info.getCurrentVolume());
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);

        Bundle args = new Bundle();
        args.putInt(KEY_MAX_VOLUME, maxVolume);
        args.putInt(KEY_CURRENT_VOLUME, currentVolume);
        args.putInt(KEY_VOLUME_CONTROL_TYPE, volumeControlType);
        args.putParcelable(KEY_AUDIO_ATTRIBUTES, attrs.toBundle());
        mRemoteSession2.runCustomTestCommands(UPDATE_PLAYER_WITH_VOLUME_PROVIDER, args);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
        assertEquals(attrs, info.getAudioAttributes());
        assertEquals(volumeControlType, info.getControlType());
        assertEquals(maxVolume, info.getMaxVolume());
        assertEquals(currentVolume, info.getCurrentVolume());
    }

    /**
     * This also tests {@link MediaController2#getPlaylist()}.
     */
    @Test
    public void testOnPlaylistChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testList = MediaTestUtils.createPlaylist(2);
        final AtomicReference<List<MediaItem2>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(MediaController2 controller,
                            List<MediaItem2> playlist, MediaMetadata2 metadata) {
                        assertNotNull(playlist);
                        assertEquals(testList.size(), playlist.size());
                        for (int i = 0; i < playlist.size(); i++) {
                            assertEquals(
                                    testList.get(i).getMediaId(), playlist.get(i).getMediaId());
                        }
                        listFromCallback.set(playlist);
                        latch.countDown();
                    }
                };
        mRemoteSession2.getMockPlaylistAgent().setPlaylist(testList);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlaylistAgent().notifyPlaylistChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(listFromCallback.get(), controller.getPlaylist());
    }

    @Test
    public void testOnPlaylistChanged_byPlayerNotifyMediaPrepared() throws Exception {
        prepareLooper();

        final long testDuration = 9999;
        final List<MediaItem2> playlist = MediaTestUtils.createPlaylist(2);
        final int testItemIndex = 1;
        final CountDownLatch latch = new CountDownLatch(1);

        RemoteMediaSession2.RemoteMockPlaylistAgent agent = mRemoteSession2.getMockPlaylistAgent();
        agent.setPlaylistNewDsd(playlist);
        agent.setCurrentMediaItem(testItemIndex);

        RemoteMediaSession2.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setPlayerState(BaseMediaPlayer.PLAYER_STATE_PAUSED);
        player.setDuration(testDuration);

        MediaController2 controller = createController(
                mRemoteSession2.getToken(), true, new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(MediaController2 controller,
                            List<MediaItem2> list, MediaMetadata2 metadata) {
                        assertNotNull(list);
                        assertEquals(playlist.size(), list.size());

                        // The duration info should be passed through this callback.
                        MediaItem2 itemOut = list.get(testItemIndex);
                        assertEquals(playlist.get(testItemIndex).getMediaId(),
                                itemOut.getMediaId());
                        assertNotNull(itemOut.getMetadata());
                        assertEquals(testDuration,
                                itemOut.getMetadata().getLong(METADATA_KEY_DURATION));
                        latch.countDown();
                    }
                });

        player.notifyMediaPrepared(testItemIndex);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * This also tests {@link MediaController2#getPlaylistMetadata()}.
     */
    @Test
    public void testOnPlaylistMetadataChanged() throws InterruptedException {
        prepareLooper();
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        final AtomicReference<MediaMetadata2> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaylistMetadataChanged(MediaController2 controller,
                            MediaMetadata2 metadata) {
                        assertNotNull(testMetadata);
                        assertEquals(testMetadata.getMediaId(), metadata.getMediaId());
                        metadataFromCallback.set(metadata);
                        latch.countDown();
                    }
                };
        RemoteMediaSession2.RemoteMockPlaylistAgent agent = mRemoteSession2.getMockPlaylistAgent();
        agent.setPlaylistMetadata(testMetadata);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        agent.notifyPlaylistMetadataChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(metadataFromCallback.get().getMediaId(),
                controller.getPlaylistMetadata().getMediaId());
    }

    /**
     * Test whether {@link MediaSession2#setPlaylist(List, MediaMetadata2)} is notified
     * through the {@link MediaController2.ControllerCallback#onPlaylistMetadataChanged(
     * MediaController2, MediaMetadata2)}
     * if the controller doesn't have {@link SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST} but
     * {@link SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST_METADATA}.
     */
    @Ignore
    @Test
    public void testOnPlaylistMetadataChanged_sessionSetPlaylist() throws InterruptedException {
        // TODO: Implement
        // Create session with ID TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST.
    }

    /**
     * This also tests {@link MediaController2#getShuffleMode()}.
     */
    @Test
    public void testOnShuffleModeChanged() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
                    @Override
                    public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
                        assertEquals(testShuffleMode, shuffleMode);
                        latch.countDown();
                    }
                };
        RemoteMediaSession2.RemoteMockPlaylistAgent agent = mRemoteSession2.getMockPlaylistAgent();
        agent.setShuffleMode(testShuffleMode);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        agent.notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    /**
     * This also tests {@link MediaController2#getRepeatMode()}.
     */
    @Test
    public void testOnRepeatModeChanged() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
                    @Override
                    public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
                        assertEquals(testRepeatMode, repeatMode);
                        latch.countDown();
                    }
                };

        RemoteMediaSession2.RemoteMockPlaylistAgent agent = mRemoteSession2.getMockPlaylistAgent();
        agent.setRepeatMode(testRepeatMode);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        agent.notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testOnSeekCompleted() throws InterruptedException {
        prepareLooper();
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onSeekCompleted(MediaController2 controller, long position) {
                controller.setTimeDiff(0L);
                assertEquals(testSeekPosition, position);
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlayer().setPlayerState(BaseMediaPlayer.PLAYER_STATE_PAUSED);
        mRemoteSession2.getMockPlayer().setCurrentPosition(testPosition);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);

        final List<MediaItem2> testPlaylist = MediaTestUtils.createPlaylist(3);
        final int targetItemIndex = 0;
        final int testBufferingState = BaseMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;

        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                controller.setTimeDiff(0L);
                assertEquals(testPlaylist.get(targetItemIndex).getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlaylistAgent().setPlaylistNewDsd(testPlaylist);

        RemoteMediaSession2.RemoteMockPlayer player = mRemoteSession2.getMockPlayer();
        player.setBufferedPosition(testBufferingPosition);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        // Since we cannot pass the DataSourceDesc directly, send the item index so that the player
        // can select which item's state change should be notified.
        player.notifyBufferingStateChanged(targetItemIndex, testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnPlayerStateChanged() throws InterruptedException {
        prepareLooper();
        final int testPlayerState = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onPlayerStateChanged(MediaController2 controller, int state) {
                controller.setTimeDiff(0L);
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };

        mRemoteSession2.getMockPlayer().setCurrentPosition(testPosition);

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.getMockPlayer().notifyPlayerStateChanged(testPlayerState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnError() throws InterruptedException {
        prepareLooper();
        final int errorCode = MediaSession2.ERROR_CODE_NOT_AVAILABLE_IN_REGION;
        final Bundle extras = TestUtils.createTestBundle();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onError(MediaController2 controller, int errorCodeOut, Bundle extrasOut) {
                assertEquals(errorCode, errorCodeOut);
                assertTrue(TestUtils.equals(extras, extrasOut));
                latch.countDown();
            }
        };

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.notifyError(errorCode, extras);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnAllowedCommandsChanged() throws InterruptedException {
        prepareLooper();
        final SessionCommandGroup2 commands = new SessionCommandGroup2();
        commands.addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY);
        commands.addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE);
        commands.addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_RESET);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onAllowedCommandsChanged(MediaController2 controller,
                    SessionCommandGroup2 commandsOut) {
                assertNotNull(commandsOut);
                Set<SessionCommand2> expected = commands.getCommands();
                Set<SessionCommand2> actual = commandsOut.getCommands();

                assertNotNull(actual);
                assertEquals(expected.size(), actual.size());
                for (SessionCommand2 command : expected) {
                    assertTrue(actual.contains(command));
                }
                latch.countDown();
            }
        };

        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.setAllowedCommands(TEST_CONTROLLER_INFO, commands);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCustomCommand() throws InterruptedException {
        prepareLooper();
        final SessionCommand2 testCommand = new SessionCommand2(
                SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE);
        final Bundle testArgs = TestUtils.createTestBundle();

        final CountDownLatch latch = new CountDownLatch(2);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onCustomCommand(MediaController2 controller, SessionCommand2 command,
                    Bundle args, ResultReceiver receiver) {
                assertEquals(testCommand, command);
                assertTrue(TestUtils.equals(testArgs, args));
                assertNull(receiver);
                latch.countDown();
            }
        };
        MediaController2 controller = createController(mRemoteSession2.getToken(), true, callback);

        Bundle args = new Bundle();
        args.putBundle(KEY_CUSTOM_COMMAND, testCommand.toBundle());
        args.putBundle(KEY_ARGUMENTS, testArgs);
        // TODO(jaewan): Test with multiple controllers
        mRemoteSession2.sendCustomCommand(testCommand, testArgs);

        // TODO(jaewan): Test receivers as well.
        args.putParcelable(KEY_RESULT_RECEIVER, null);
        mRemoteSession2.sendCustomCommand(TEST_CONTROLLER_INFO, testCommand, testArgs, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnCustomLayoutChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaSession2.CommandButton> buttons = new ArrayList<>();

        MediaSession2.CommandButton button = new MediaSession2.CommandButton.Builder()
                .setCommand(new SessionCommand2(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY))
                .setDisplayName("button")
                .build();
        buttons.add(button);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onCustomLayoutChanged(MediaController2 controller2,
                    List<MediaSession2.CommandButton> layout) {
                assertEquals(layout.size(), buttons.size());
                for (int i = 0; i < layout.size(); i++) {
                    assertEquals(layout.get(i).getCommand(), buttons.get(i).getCommand());
                    assertEquals(layout.get(i).getDisplayName(), buttons.get(i).getDisplayName());
                }
                latch.countDown();
            }
        };
        final MediaController2 controller =
                createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.setCustomLayout(TEST_CONTROLLER_INFO, buttons);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnRoutesInfoChanged() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onRoutesInfoChanged(@NonNull MediaController2 controller,
                    @Nullable List<Bundle> routes) {
                assertNull(routes);
                latch.countDown();
            }
        };
        final MediaController2 controller =
                createController(mRemoteSession2.getToken(), true, callback);
        mRemoteSession2.notifyRoutesInfoChanged(TEST_CONTROLLER_INFO, null /* routeList */);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    private void testControllerAfterSessionIsClosed(String id) throws InterruptedException {
        // This cause session service to be died.
        mRemoteSession2.close();
        waitForDisconnect(mController, true);
        testNoInteraction();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        SessionToken2 token = mRemoteSession2.getToken();
        assertEquals(id, token.getId());
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

        mRemoteSession2.sendCustomCommand(customCommand, null);

        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        setRunnableForOnCustomCommand(mController, null);
    }
}
