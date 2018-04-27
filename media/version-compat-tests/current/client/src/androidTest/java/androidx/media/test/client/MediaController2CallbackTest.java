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

import static androidx.media.test.lib.CommonConstants.KEY_AUDIO_ATTRIBUTES;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERED_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERING_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_CURRENT_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_CUSTOM_COMMAND;
import static androidx.media.test.lib.CommonConstants.KEY_ITEM_INDEX;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYER_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST_METADATA;
import static androidx.media.test.lib.CommonConstants.KEY_REPEAT_MODE;
import static androidx.media.test.lib.CommonConstants.KEY_SEEK_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_SHUFFLE_MODE;
import static androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods
        .NOTIFY_BUFFERED_STATE_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods
        .NOTIFY_PLAYER_STATE_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods
        .NOTIFY_SEEK_COMPLETED;
import static androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods
        .SET_BUFFERED_POSITION_MANUALLY;
import static androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods
        .SET_CURRENT_POSITION_MANUALLY;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .NOTIFY_PLAYLIST_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .NOTIFY_PLAYLIST_METADATA_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .NOTIFY_REPEAT_MODE_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .NOTIFY_SHUFFLE_MODE_CHANGED;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .SET_PLAYLIST_METADATA_MANUALLY;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .SET_REPEAT_MODE_MANUALLY;
import static androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods
        .SET_SHUFFLE_MODE_MANUALLY;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.CLOSE;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.SEND_CUSTOM_COMMAND;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.SET_PLAYLIST;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.UPDATE_PLAYER;
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
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.AudioAttributesCompat;
import androidx.media.BaseMediaPlayer;
import androidx.media.MediaController2;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlaylistAgent;
import androidx.media.MediaSession2;
import androidx.media.SessionCommand2;
import androidx.media.SessionToken2;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController2.ControllerCallback}.
 */
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaController2CallbackTest extends MediaSession2TestBase {

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
        MediaController2 controller = createController(mTestHelper.createDefaultSession2());
        assertNotNull(controller);
    }

    @Test
    public void testConnection_sessionRejects() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(
                mTestHelper.createSession2(TEST_CONTROLLER_CALLBACK_SESSION_REJECTS),
                false /* waitForConnect */, null);
        assertNotNull(controller);
        waitForConnect(controller, false /* expected */);
        waitForDisconnect(controller, true /* expected */);
    }

    @Test
    public void testConnection_sessionClosed() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mTestHelper.createDefaultSession2());

        mTestHelper.callMediaSession2Method(CLOSE, null);
        waitForDisconnect(controller, true);
    }

    @Test
    public void testConnection_controllerClosed() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mTestHelper.createDefaultSession2());

        controller.close();
        waitForDisconnect(controller, true);
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterSessionClose_session() throws InterruptedException {
        prepareLooper();
        SessionToken2 token = mTestHelper.createDefaultSession2();
        mController = createController(token);
        testControllerAfterSessionIsClosed(token.getId());
    }

    @Test
    @LargeTest
    public void testNoInteractionAfterControllerClose_session() throws InterruptedException {
        prepareLooper();
        final SessionToken2 token = mTestHelper.createDefaultSession2();
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
        mController = createController(mTestHelper.createDefaultSession2(),
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
                KEY_PLAYLIST, MediaTestUtils.toParcelableArrayList(testPlaylist));
        args.putBundle(KEY_AUDIO_ATTRIBUTES, testAudioAttributes.toBundle());

        mTestHelper.callMediaSession2Method(UPDATE_PLAYER, args);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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

        // Create a session.
        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle setPlaylistArgs = new Bundle();
        setPlaylistArgs.putParcelableArrayList(
                KEY_PLAYLIST, MediaTestUtils.toParcelableArrayList(testList));
        mTestHelper.callMediaPlaylistAgentMethod(
                PlaylistAgentMethods.SET_PLAYLIST_MANUALLY, setPlaylistArgs);

        MediaController2 controller = createController(token, true, callback);
        mTestHelper.callMediaPlaylistAgentMethod(NOTIFY_PLAYLIST_CHANGED, null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(listFromCallback.get(), controller.getPlaylist());
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

        // Create a session.
        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle setMetadataArgs = new Bundle();
        setMetadataArgs.putBundle(KEY_PLAYLIST_METADATA, testMetadata.toBundle());
        mTestHelper.callMediaPlaylistAgentMethod(SET_PLAYLIST_METADATA_MANUALLY, setMetadataArgs);

        MediaController2 controller = createController(token, true, callback);
        mTestHelper.callMediaPlaylistAgentMethod(NOTIFY_PLAYLIST_METADATA_CHANGED, null);
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

        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle args = new Bundle();
        args.putInt(KEY_SHUFFLE_MODE, testShuffleMode);
        mTestHelper.callMediaPlaylistAgentMethod(SET_SHUFFLE_MODE_MANUALLY, args);

        MediaController2 controller = createController(token, true, callback);
        mTestHelper.callMediaPlaylistAgentMethod(NOTIFY_SHUFFLE_MODE_CHANGED, null);
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

        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle args = new Bundle();
        args.putInt(KEY_REPEAT_MODE, testRepeatMode);
        mTestHelper.callMediaPlaylistAgentMethod(SET_REPEAT_MODE_MANUALLY, args);

        MediaController2 controller = createController(token, true, callback);
        mTestHelper.callMediaPlaylistAgentMethod(NOTIFY_REPEAT_MODE_CHANGED, null);
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

        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle setCurrentPositionArgs = new Bundle();
        setCurrentPositionArgs.putLong(KEY_CURRENT_POSITION, testPosition);
        mTestHelper.callMediaPlayerInterfaceMethod(
                SET_CURRENT_POSITION_MANUALLY, setCurrentPositionArgs);

        final MediaController2 controller = createController(token, true, callback);

        Bundle notifySeekCompletedArgs = new Bundle();
        notifySeekCompletedArgs.putLong(KEY_SEEK_POSITION, testSeekPosition);
        mTestHelper.callMediaPlayerInterfaceMethod(NOTIFY_SEEK_COMPLETED, notifySeekCompletedArgs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnBufferingStateChanged() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> testPlaylist = MediaTestUtils.createPlaylist(3);
        final int targetItemIndex = 0;
        final MediaItem2 testItem = testPlaylist.get(targetItemIndex);
        final int testBufferingState = BaseMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2.ControllerCallback callback =
                new MediaController2.ControllerCallback() {
            @Override
            public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                    int state) {
                controller.setTimeDiff(0L);
                assertEquals(testItem.getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };

        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle setPlaylistArgs = new Bundle();
        setPlaylistArgs.putParcelableArrayList(KEY_PLAYLIST,
                MediaTestUtils.toParcelableArrayList(testPlaylist));
        mTestHelper.callMediaSession2Method(SET_PLAYLIST, setPlaylistArgs);

        Bundle setBufferedPositionArgs = new Bundle();
        setBufferedPositionArgs.putLong(KEY_BUFFERED_POSITION, testBufferingPosition);
        mTestHelper.callMediaPlayerInterfaceMethod(SET_BUFFERED_POSITION_MANUALLY,
                setBufferedPositionArgs);

        final MediaController2 controller = createController(token, true, callback);
        Bundle notifyBufferingStateChangedArgs = new Bundle();
        notifyBufferingStateChangedArgs.putInt(KEY_BUFFERING_STATE, testBufferingState);
        // Since we cannot pass the DataSourceDesc directly, send the item index so that the player
        // can select which item's state change should be notified.
        notifyBufferingStateChangedArgs.putInt(KEY_ITEM_INDEX, targetItemIndex);
        mTestHelper.callMediaPlayerInterfaceMethod(NOTIFY_BUFFERED_STATE_CHANGED,
                notifyBufferingStateChangedArgs);
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

        SessionToken2 token = mTestHelper.createDefaultSession2();

        Bundle setCurrentPositionArgs = new Bundle();
        setCurrentPositionArgs.putLong(KEY_CURRENT_POSITION, testPosition);
        mTestHelper.callMediaPlayerInterfaceMethod(
                SET_CURRENT_POSITION_MANUALLY, setCurrentPositionArgs);

        final MediaController2 controller = createController(token, true, callback);

        Bundle notifyPlayerStateArgs = new Bundle();
        notifyPlayerStateArgs.putInt(KEY_PLAYER_STATE, testPlayerState);
        mTestHelper.callMediaPlayerInterfaceMethod(NOTIFY_PLAYER_STATE_CHANGED,
                notifyPlayerStateArgs);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void testControllerAfterSessionIsClosed(String id) throws InterruptedException {
        // This cause session service to be died.
        mTestHelper.callMediaSession2Method(CLOSE, null);
        waitForDisconnect(mController, true);
        testNoInteraction();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        SessionToken2 token = mTestHelper.createDefaultSession2();
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

        Bundle args = new Bundle();
        args.putBundle(KEY_CUSTOM_COMMAND, customCommand.toBundle());
        mTestHelper.callMediaSession2Method(SEND_CUSTOM_COMMAND, args);

        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        setRunnableForOnCustomCommand(mController, null);
    }
}
