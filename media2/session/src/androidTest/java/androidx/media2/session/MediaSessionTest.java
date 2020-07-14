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

import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;

import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED;
import static androidx.media2.session.SessionResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaSessionManager;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSession.SessionCallback;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaSessionTest extends MediaSessionTestBase {
    private static final String TAG = "MediaSessionTest";

    private MediaSession mSession;
    private MockPlayer mPlayer;
    private ControllerInfo mTestControllerInfo;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);

        if (mSession != null && !mSession.isClosed()) {
            mSession.close();
        }
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setId(TAG)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (TextUtils.equals(mContext.getPackageName(),
                                controller.getPackageName())) {
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
            mSession = null;
        }
    }

    @Test
    public void builder() {
        MediaSession.Builder builder;
        try {
            builder = new MediaSession.Builder(mContext, null);
            fail("null player shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }
        try {
            builder = new MediaSession.Builder(mContext, mPlayer);
            builder.setId(null);
            fail("null id shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }
    }

    @Test
    public void builder_emptyStringAsId() throws Exception {
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("").build()) {
            // Using empty string as Id shouldn't crash.
        }
    }

    @Test
    public void close_noException() {
        MediaSession session = new MediaSession.Builder(mContext, mPlayer).build();
        session.close();
    }

    @Test
    public void updatePlayer() throws Exception {
        MockPlayer anotherPlayer = new MockPlayer(0);

        // Test if setPlayer doesn't crash with various situations.
        mSession.updatePlayer(mPlayer);
        assertEquals(mPlayer, mSession.getPlayer());

        mSession.updatePlayer(anotherPlayer);
        assertEquals(anotherPlayer, mSession.getPlayer());
    }

    @Test
    public void updatePlayer_playbackInfo() throws Exception {
        MockPlayer player = new MockPlayer(0);
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();
        player.mAudioAttributes = attrs;

        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VOLUME_CONTROL_ABSOLUTE;

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaybackInfoChanged(@NonNull MediaController controller,
                    @NonNull PlaybackInfo info) {
                Assert.assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
                assertEquals(attrs, info.getAudioAttributes());
                assertEquals(volumeControlType, info.getPlaybackType());
                assertEquals(maxVolume, info.getMaxVolume());
                assertEquals(currentVolume, info.getCurrentVolume());
                latch.countDown();
            }
        };

        mSession.updatePlayer(player);

        final MediaController controller = createController(mSession.getToken(), true, null,
                callback);
        PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(PlaybackInfo.PLAYBACK_TYPE_LOCAL, info.getPlaybackType());
        assertEquals(attrs, info.getAudioAttributes());
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        int localVolumeControlType = VOLUME_CONTROL_ABSOLUTE;
        if (Build.VERSION.SDK_INT >= 21 && manager.isVolumeFixed()) {
            localVolumeControlType = VOLUME_CONTROL_FIXED;
        }
        assertEquals(localVolumeControlType, info.getControlType());
        assertEquals(manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), info.getMaxVolume());
        assertEquals(manager.getStreamVolume(AudioManager.STREAM_MUSIC), info.getCurrentVolume());

        MockRemotePlayer remotePlayer = new MockRemotePlayer(
                volumeControlType, maxVolume, currentVolume) {
            @Override
            public AudioAttributesCompat getAudioAttributes() {
                return attrs;
            }
        };
        mSession.updatePlayer(remotePlayer);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(PlaybackInfo.PLAYBACK_TYPE_REMOTE, info.getPlaybackType());
        assertEquals(attrs, info.getAudioAttributes());
        assertEquals(volumeControlType, info.getControlType());
        assertEquals(maxVolume, info.getMaxVolume());
        assertEquals(currentVolume, info.getCurrentVolume());
    }

    @Test
    public void play() {
        assertNotNull(mSession.getPlayer().play());
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void pause() {
        assertNotNull(mSession.getPlayer().pause());
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void prepare() {
        assertNotNull(mSession.getPlayer().prepare());
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void seekTo() {
        final long pos = 1004L;
        assertNotNull(mSession.getPlayer().seekTo(pos));
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(pos, mPlayer.mSeekPosition);
    }

    @Test
    public void getDuration() {
        final long testDuration = 9999;
        mPlayer.mDuration = testDuration;
        mPlayer.mLastPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        assertEquals(testDuration, mSession.getPlayer().getDuration());
    }

    @Test
    public void setPlaybackSpeed() {
        final float speed = 1.5f;
        assertNotNull(mSession.getPlayer().setPlaybackSpeed(speed));
        assertTrue(mPlayer.mSetPlaybackSpeedCalled);
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void getPlaybackSpeed() {
        final float speed = 1.5f;
        mPlayer.mPlaybackSpeed = speed;
        mPlayer.mLastPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        assertEquals(speed, mSession.getPlayer().getPlaybackSpeed(), 0.0f);
    }

    @Test
    public void getCurrentMediaItem() {
        MediaItem item = TestUtils.createMediaItemWithMetadata();
        mPlayer.mCurrentMediaItem = item;
        assertEquals(item, mSession.getPlayer().getCurrentMediaItem());
    }

    @Test
    public void skipToPreviousItem() {
        assertNotNull(mSession.getPlayer().skipToPreviousPlaylistItem());
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void skipToNextItem() {
        assertNotNull(mSession.getPlayer().skipToNextPlaylistItem());
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void skipToPlaylistItem() {
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        int targetIndex = 0;
        assertNotNull(mSession.getPlayer().setPlaylist(list, null));
        assertNotNull(mSession.getPlayer().skipToPlaylistItem(targetIndex));
        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertSame(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void getPlayerState() {
        final int state = SessionPlayer.PLAYER_STATE_PLAYING;
        mPlayer.mLastPlayerState = state;
        assertEquals(state, mSession.getPlayer().getPlayerState());
    }

    @Test
    public void getBufferingState() {
        final int bufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        mPlayer.mLastBufferingState = bufferingState;
        assertEquals(bufferingState, mSession.getPlayer().getBufferingState());
    }

    @Test
    public void getPosition() {
        final long position = 150000;
        mPlayer.mCurrentPosition = position;
        mPlayer.mLastPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        assertEquals(position, mSession.getPlayer().getCurrentPosition());
    }

    @Test
    public void getBufferedPosition() {
        final long bufferedPosition = 900000;
        mPlayer.mBufferedPosition = bufferedPosition;
        mPlayer.mLastPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        assertEquals(bufferedPosition, mSession.getPlayer().getBufferedPosition());
    }

    @Test
    public void setPlaylist() {
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        assertNotNull(mSession.getPlayer().setPlaylist(list, null));
        assertTrue(mPlayer.mSetPlaylistCalled);
        assertSame(list, mPlayer.mPlaylist);
        assertNull(mPlayer.mMetadata);
    }

    @Test
    public void getPlaylist() {
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        mPlayer.mPlaylist = list;
        assertEquals(list, mSession.getPlayer().getPlaylist());
    }

    @Test
    public void updatePlaylistMetadata() {
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        assertNotNull(mSession.getPlayer().updatePlaylistMetadata(testMetadata));
        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertSame(testMetadata, mPlayer.mMetadata);
    }

    @Test
    public void getPlaylistMetadata() {
        final MediaMetadata testMetadata = TestUtils.createMetadata();
        mPlayer.mMetadata = testMetadata;
        assertEquals(testMetadata, mSession.getPlayer().getPlaylistMetadata());
    }

    @Test
    public void addPlaylistItem() {
        final int testIndex = 12;
        final MediaItem testMediaItem = TestUtils.createMediaItemWithMetadata();
        assertNotNull(mSession.getPlayer().addPlaylistItem(testIndex, testMediaItem));
        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void removePlaylistItem() {
        final List<MediaItem> list = TestUtils.createMediaItems(2);
        int targetIndex = 0;
        assertNotNull(mSession.getPlayer().setPlaylist(list, null));
        assertNotNull(mSession.getPlayer().removePlaylistItem(targetIndex));
        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertSame(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void replacePlaylistItem() {
        final int testIndex = 12;
        final MediaItem testMediaItem = TestUtils.createMediaItemWithMetadata();
        assertNotNull(mSession.getPlayer().replacePlaylistItem(testIndex, testMediaItem));
        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void setShuffleMode() {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        assertNotNull(mSession.getPlayer().setShuffleMode(testShuffleMode));
        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void setRepeatMode() {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        assertNotNull(mSession.getPlayer().setRepeatMode(testRepeatMode));
        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void onCommandCallback() throws Exception {
        final MockOnCommandCallback callback = new MockOnCommandCallback();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mPlayer = new MockPlayer(1);
                mSession = new MediaSession.Builder(mContext, mPlayer)
                        .setSessionCallback(sHandlerExecutor, callback).build();
            }
        });
        MediaController controller = createController(mSession.getToken());
        SessionResult pauseResult = controller.pause().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_ERROR_INVALID_STATE, pauseResult.getResultCode());
        assertFalse(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(1, callback.commands.size());
        assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PAUSE,
                callback.commands.get(0).getCommandCode());

        SessionResult playResult = controller.play().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, playResult.getResultCode());
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(2, callback.commands.size());
        assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PLAY,
                (long) callback.commands.get(1).getCommandCode());
    }

    @Test
    public void onConnectCallback() throws InterruptedException {
        final MockOnConnectCallback sessionCallback = new MockOnConnectCallback();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mSession = new MediaSession.Builder(mContext, mPlayer)
                        .setSessionCallback(sHandlerExecutor, sessionCallback).build();
            }
        });
        MediaController controller = createController(mSession.getToken(), false, null, null);
        assertNotNull(controller);
        waitForConnect(controller, false);
        waitForDisconnect(controller, true);
    }

    @Test
    public void onDisconnectCallback() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testOnDisconnectCallback")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public void onDisconnected(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        assertEquals(Process.myUid(), controller.getUid());
                        latch.countDown();
                    }
                }).build()) {
            MediaController controller = createController(session.getToken());
            controller.close();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void setCustomLayout() throws Exception {
        final List<CommandButton> customLayout = new ArrayList<>();
        customLayout.add(new CommandButton.Builder()
                .setCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PLAY))
                .setDisplayName("button")
                .build());
        final CountDownLatch latch = new CountDownLatch(1);

        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("testSetCustomLayout")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (mContext.getPackageName().equals(controller.getPackageName())) {
                            mTestControllerInfo = controller;
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                })
                .build()) {

            final ControllerCallback callback = new ControllerCallback() {
                @Override
                public int onSetCustomLayout(
                        @NonNull MediaController controller, @NonNull List<CommandButton> layout) {
                    assertEquals(customLayout.size(), layout.size());
                    for (int i = 0; i < layout.size(); i++) {
                        assertEquals(customLayout.get(i).getCommand(), layout.get(i).getCommand());
                        assertEquals(customLayout.get(i).getDisplayName(),
                                layout.get(i).getDisplayName());
                    }
                    latch.countDown();
                    return RESULT_SUCCESS;
                }
            };
            MediaController controller = createController(session.getToken(), true, null, callback);
            SessionResult result = session.setCustomLayout(mTestControllerInfo, customLayout)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(RESULT_SUCCESS, result.getResultCode());
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * This also tests {@link MediaController#getAllowedCommands()}.
      */
    @Test
    public void setAllowedCommands() throws InterruptedException {
        final SessionCommandGroup commands = new SessionCommandGroup.Builder()
                .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PLAY))
                .addCommand(new SessionCommand(SessionCommand.COMMAND_CODE_PLAYER_PAUSE))
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onAllowedCommandsChanged(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup commandsOut) {
                assertEquals(commands, commandsOut);
                latch.countDown();
            }
        };

        final MediaController controller = createController(mSession.getToken(), true, null,
                callback);
        ControllerInfo controllerInfo = getTestControllerInfo();
        assertNotNull(controllerInfo);

        mSession.setAllowedCommands(controllerInfo, commands);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(commands, controller.getAllowedCommands());
    }

    @Test
    public void sendCustomCommand() throws Exception {
        final SessionCommand testCommand = new SessionCommand("test_command_code", null);
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "testSendCustomAction");

        final CountDownLatch latch = new CountDownLatch(2);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            @NonNull
            public SessionResult onCustomCommand(@NonNull MediaController controller,
                    @NonNull SessionCommand command, Bundle args) {
                assertEquals(testCommand, command);
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new SessionResult(RESULT_SUCCESS);
            }
        };
        final MediaController controller =
                createController(mSession.getToken(), true, null, callback);
        // TODO(jaewan): Test with multiple controllers
        mSession.broadcastCustomCommand(testCommand, testArgs);

        ControllerInfo controllerInfo = getTestControllerInfo();
        assertNotNull(controllerInfo);
        // TODO(jaewan): Test receivers as well.
        SessionResult result = mSession.sendCustomCommand(controllerInfo, testCommand, testArgs)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Test expected failure of sendCustomCommand() when it's called in
     * SessionCallback#onConnect().
     */
    @Test
    public void sendCustomCommand_onConnect() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCommand testCommand = new SessionCommand("test", null);
        final SessionCallback testSessionCallback = new SessionCallback() {
            @Override
            @Nullable
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertNotNull(session.sendCustomCommand(controller, testCommand, null));
                return super.onConnect(session, controller);
            }
        };
        final ControllerCallback testControllerCallback = new ControllerCallback() {
            @Override
            @NonNull
            public SessionResult onCustomCommand(@NonNull MediaController controller,
                    @NonNull SessionCommand command, @Nullable Bundle args) {
                if (TextUtils.equals(testCommand.getCustomAction(), command.getCustomAction())) {
                    latch.countDown();
                }
                return super.onCustomCommand(controller, command, args);
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, testSessionCallback).build()) {
            MediaController controller = createController(session.getToken(), true,
                    null, testControllerCallback);
            assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Test expected success of sendCustomCommand() when it's called in
     * SessionCallback#onPostConnect().
     */
    @Test
    public void sendCustomCommand_onPostConnect() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCommand testCommand = new SessionCommand("test", null);
        final SessionCallback testSessionCallback = new SessionCallback() {
            @Override
            @Nullable
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup gr = super.onConnect(session, controller);
                return gr;
            }

            @Override
            public void onPostConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertNotNull(session.sendCustomCommand(controller, testCommand, null));
            }
        };
        final ControllerCallback testControllerCallback = new ControllerCallback() {
            @Override
            @NonNull
            public SessionResult onCustomCommand(@NonNull MediaController controller,
                    @NonNull SessionCommand command, @Nullable Bundle args) {
                if (TextUtils.equals(testCommand.getCustomAction(), command.getCustomAction())) {
                    latch.countDown();
                }
                return super.onCustomCommand(controller, command, args);
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, testSessionCallback).build()) {
            MediaController controller = createController(session.getToken(), true,
                    null, testControllerCallback);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Test {@link MediaSession#getSessionCompatToken()}.
     */
    @Test
    public void getSessionCompatToken_returnsCompatibleWithMediaControllerCompat()
            throws Exception {
        String expectedControllerCompatPackageName =
                (21 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 24)
                        ? MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER
                        : mContext.getPackageName();
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("getSessionCompatToken_returnsCompatibleWithMediaControllerCompat")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (TextUtils.equals(expectedControllerCompatPackageName,
                                controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build()) {
            MediaSessionCompat.Token token = session.getSessionCompatToken();
            MediaControllerCompat compat = new MediaControllerCompat(mContext, token);
            long testSeekPosition = 1234;
            compat.getTransportControls().seekTo(testSeekPosition);
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mSeekToCalled);
            assertEquals(testSeekPosition, mPlayer.mSeekPosition);
        }
    }

    private ControllerInfo getTestControllerInfo() {
        List<ControllerInfo> controllers = mSession.getConnectedControllers();
        assertNotNull(controllers);
        for (int i = 0; i < controllers.size(); i++) {
            if (Process.myUid() == controllers.get(i).getUid()) {
                return controllers.get(i);
            }
        }
        fail("Failed to get test controller info");
        return null;
    }

    public class MockOnConnectCallback extends SessionCallback {
        @Override
        public SessionCommandGroup onConnect(@NonNull MediaSession session,
                @NonNull ControllerInfo controllerInfo) {
            if (Process.myUid() != controllerInfo.getUid()) {
                return null;
            }
            assertEquals(mContext.getPackageName(), controllerInfo.getPackageName());
            assertEquals(Process.myUid(), controllerInfo.getUid());
            assertFalse(controllerInfo.isTrusted());
            // Reject all
            return null;
        }
    }

    public class MockOnCommandCallback extends SessionCallback {
        public final ArrayList<SessionCommand> commands = new ArrayList<>();

        @Override
        public int onCommandRequest(@NonNull MediaSession session,
                @NonNull ControllerInfo controllerInfo, @NonNull SessionCommand command) {
            assertEquals(mContext.getPackageName(), controllerInfo.getPackageName());
            assertEquals(Process.myUid(), controllerInfo.getUid());
            assertFalse(controllerInfo.isTrusted());
            commands.add(command);
            if (command.getCommandCode() == SessionCommand.COMMAND_CODE_PLAYER_PAUSE) {
                return RESULT_ERROR_INVALID_STATE;
            }
            return RESULT_SUCCESS;
        }
    }
}
