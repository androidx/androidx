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

package androidx.media2.test.service.tests;

import static androidx.media.MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER;
import static androidx.media2.session.SessionResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;
import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSession.SessionCallback;
import androidx.media2.session.MediaUtils;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;
import androidx.media2.test.common.MockActivity;
import androidx.media2.test.common.PollingCheck;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.common.TestUtils.SyncHandler;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.MockRemotePlayer;
import androidx.media2.test.service.RemoteMediaControllerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link SessionCallback} working with {@link MediaControllerCompat}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaSessionCallbackWithMediaControllerCompatTest extends MediaSessionTestBase {
    private static final String TAG = "MediaSessionCallbackTestWithMediaControllerCompat";
    private static final long VOLUME_CHANGE_TIMEOUT_MS = 5000L;

    private static final String EXPECTED_CONTROLLER_PACKAGE_NAME =
            (Build.VERSION.SDK_INT < 21 || Build.VERSION.SDK_INT >= 24)
                    ? CLIENT_PACKAGE_NAME : LEGACY_CONTROLLER;

    PendingIntent mIntent;
    MediaSession mSession;
    RemoteMediaControllerCompat mController;
    MockPlayer mPlayer;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

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
                        if (EXPECTED_CONTROLLER_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller, @NonNull String mediaId) {
                        return MediaTestUtils.createMediaItem(mediaId);
                    }
                })
                .setSessionActivity(mIntent)
                .build();
        mController = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
        if (mController != null) {
            mController.cleanUp();
            mController = null;
        }
    }

    @Test
    public void disconnectedAfterTimeout() throws Exception {
        CountDownLatch disconnectedLatch = new CountDownLatch(1);
        RemoteMediaControllerCompat controller = null;
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("disconnectedAfterTimeout")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    private ControllerInfo mConnectedController;

                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (EXPECTED_CONTROLLER_PACKAGE_NAME.equals(controller.getPackageName())) {
                            mConnectedController = controller;
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public void onDisconnected(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (TestUtils.equals(mConnectedController, controller)) {
                            disconnectedLatch.countDown();
                        }
                    }
                })
                .build()) {
            // Make onDisconnected() to be called immediately after the connection.
            session.setLegacyControllerConnectionTimeoutMs(0);
            controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompatToken(), /* waitForConnection= */ true);
            // Invoke any command for session to recognize the controller compat.
            controller.getTransportControls().seekTo(111);
            assertTrue(disconnectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } finally {
            if (controller != null) {
                controller.cleanUp();
            }
        }
    }

    @Test
    public void connectedCallbackAfterDisconnectedByTimeout() throws Exception {
        CountDownLatch connectedLatch = new CountDownLatch(2);
        CountDownLatch disconnectedLatch = new CountDownLatch(1);
        RemoteMediaControllerCompat controller = null;
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setId("connectedCallbackAfterDisconnectedByTimeout")
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    private ControllerInfo mConnectedController;

                    @Override
                    public SessionCommandGroup onConnect(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (EXPECTED_CONTROLLER_PACKAGE_NAME.equals(controller.getPackageName())) {
                            mConnectedController = controller;
                            connectedLatch.countDown();
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public void onDisconnected(@NonNull MediaSession session,
                            @NonNull ControllerInfo controller) {
                        if (TestUtils.equals(mConnectedController, controller)) {
                            disconnectedLatch.countDown();
                        }
                    }
                })
                .build()) {
            // Make onDisconnected() to be called immediately after the connection.
            session.setLegacyControllerConnectionTimeoutMs(0);
            controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompatToken(), /* waitForConnection= */ true);
            // Invoke any command for session to recognize the controller compat.
            controller.getTransportControls().seekTo(111);
            assertTrue(disconnectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // Test whenter onConnect() is called again after the onDisconnected().
            controller.getTransportControls().seekTo(111);

            assertTrue(connectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } finally {
            if (controller != null) {
                controller.cleanUp();
            }
        }
    }

    @Test
    public void play() {
        mController.getTransportControls().play();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void pause() {
        mController.getTransportControls().pause();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void stop() {
        // MediaControllerCompat#stop() will call MediaSession#pause() and MediaSession#seekTo(0).
        // Therefore, the latch's initial count is 2.
        MockPlayer player = new MockPlayer(2);
        player.mCurrentPosition = 1530;
        mSession.updatePlayer(player);

        mController.getTransportControls().stop();
        try {
            assertTrue(player.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(player.mPauseCalled);
        assertTrue(player.mSeekToCalled);
        assertEquals(0, player.mSeekPosition);
    }

    @Test
    public void prepare() {
        mController.getTransportControls().prepare();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void seekTo() {
        final long seekPosition = 12125L;
        mController.getTransportControls().seekTo(seekPosition);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void setPlaybackSpeed() {
        final float testSpeed = 2.0f;
        mController.getTransportControls().setPlaybackSpeed(testSpeed);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSetPlaybackSpeedCalled);
        assertEquals(testSpeed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void addQueueItem() throws InterruptedException {
        final int playlistSize = 10;

        List<MediaItem> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mPlayer.mPlaylist = playlist;
        mPlayer.notifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(TIMEOUT_MS);

        // Prepare an item to add.
        final String mediaId = "media_id";
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .build();
        mController.addQueueItem(desc);

        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(mediaId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void addQueueItemWithIndex() throws InterruptedException {
        final int playlistSize = 10;

        List<MediaItem> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mPlayer.mPlaylist = playlist;
        mPlayer.notifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(TIMEOUT_MS);

        // Prepare an item to add.
        final int testIndex = 0;
        final String mediaId = "media_id";
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .build();
        mController.addQueueItem(desc, testIndex);

        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertEquals(mediaId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void removeQueueItem() throws InterruptedException {
        final int playlistSize = 10;

        List<MediaItem> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mPlayer.mPlaylist = playlist;
        mPlayer.notifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(TIMEOUT_MS);

        // Select an item to remove.
        final int targetIndex = 3;
        final MediaItem targetItem = playlist.get(targetIndex);
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(targetItem.getMediaId())
                .build();
        mController.removeQueueItem(desc);

        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void skipToPrevious() throws InterruptedException {
        mController.getTransportControls().skipToPrevious();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void skipToNext() throws InterruptedException {
        mController.getTransportControls().skipToNext();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void skipToQueueItem() throws InterruptedException {
        final int playlistSize = 10;

        List<MediaItem> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mPlayer.mPlaylist = playlist;
        mPlayer.notifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(TIMEOUT_MS);

        // Get Queue from local MediaControllerCompat.
        List<QueueItem> queue = mSession.getSessionCompat().getController().getQueue();
        final int targetIndex = 3;
        mController.getTransportControls().skipToQueueItem(queue.get(targetIndex).getQueueId());

        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void setShuffleMode() throws InterruptedException {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mController.getTransportControls().setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void setRepeatMode() throws InterruptedException {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mController.getTransportControls().setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void setVolumeTo() throws Exception {
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayer remotePlayer =
                new MockRemotePlayer(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayer(remotePlayer);

        final int targetVolume = 50;
        mController.setVolumeTo(targetVolume, 0 /* flags */);
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

        final int direction = AudioManager.ADJUST_RAISE;
        mController.adjustVolume(direction, 0 /* flags */);
        assertTrue(remotePlayer.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mAdjustVolumeCalled);
        assertEquals(direction, remotePlayer.mDirection);
    }

    @Test
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
        MockPlayer player = new MockPlayer(0);
        player.setAudioAttributes(attrs);

        // Replace with another player rather than setting the audio attribute of the existing
        // player for making changes to take effect immediately.
        mSession.updatePlayer(player);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        mController.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI);
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
        MockPlayer player = new MockPlayer(0);
        player.setAudioAttributes(attrs);
        // Replace with another player rather than setting the audio attribute of the existing
        // player for making changes to take effect immediately.
        mSession.updatePlayer(player);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        mController.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
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
    public void sendCommand() throws InterruptedException {
        // TODO(jaewan): Need to revisit with the permission.
        final String testCommand = "test_command";
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "test_args");

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup commands = super.onConnect(session, controller);
                SessionCommandGroup.Builder builder = new SessionCommandGroup.Builder(commands);
                builder.addCommand(new SessionCommand(testCommand, null));
                return builder.build();
            }

            @NonNull
            @Override
            public SessionResult onCustomCommand(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller,
                    @NonNull SessionCommand sessionCommand, Bundle args) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testCommand, sessionCommand.getCustomAction());
                assertTrue(TestUtils.equals(testArgs, args));
                latch.countDown();
                return new SessionResult(RESULT_SUCCESS, null);
            }
        };
        mSession.close();
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);
        controller.sendCommand(testCommand, testArgs, null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_sessionRejects() throws Exception {
        final SessionCallback sessionCallback = new SessionCallback() {
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

        // Session will not accept the controller's commands.
        RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);
        controller.getTransportControls().play();
        assertFalse(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void fastForward() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onFastForward(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testFastForward").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().fastForward();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void rewind() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onRewind(@NonNull MediaSession session, @NonNull ControllerInfo controller) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testRewind").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().rewind();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void prepareFromMediaUri() throws InterruptedException {
        final Uri mediaId = Uri.parse("foo://bar");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals(mediaId, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromMediaUri").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromUri(mediaId, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPrepareCalled);
        }
    }

    @Test
    public void playFromMediaUri() throws InterruptedException {
        final Uri request = Uri.parse("foo://bar");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromMediaUri").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromUri(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPlayCalled);
        }
    }

    @Test
    public void prepareFromMediaId() throws InterruptedException {
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals("androidx://media2-session/prepareFromMediaId?id=" + request,
                        uri.toString());
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromMediaId").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromMediaId(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPrepareCalled);
        }
    }

    @Test
    public void playFromMediaId() throws InterruptedException {
        final String mediaId = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals("androidx://media2-session/playFromMediaId?id=" + mediaId,
                        uri.toString());
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromMediaId").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromMediaId(mediaId, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPlayCalled);
        }
    }

    @Test
    public void prepareFromSearch() throws InterruptedException {
        final String query = "test_query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals("androidx://media2-session/prepareFromSearch?query=" + query,
                        uri.toString());
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromSearch").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromSearch(query, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPrepareCalled);
        }
    }

    @Test
    public void playFromSearch() throws InterruptedException {
        final String query = "test_query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetMediaUri(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals("androidx://media2-session/playFromSearch?query=" + query,
                        uri.toString());
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromSearch").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromSearch(query, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPlayCalled);
        }
    }

    @Test
    public void setRating() throws InterruptedException {
        final int ratingType = RatingCompat.RATING_5_STARS;
        final float ratingValue = 3.5f;
        final RatingCompat rating = RatingCompat.newStarRating(ratingType, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onSetRating(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaIdOut,
                    @NonNull Rating ratingOut) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(MediaUtils.convertToRating(rating), ratingOut);
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };

        mPlayer.mCurrentMediaItem = MediaTestUtils.createMediaItem(mediaId);
        try (MediaSession session = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetRating").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().setRating(rating);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void onCommandCallback() throws InterruptedException {
        final ArrayList<SessionCommand> commands = new ArrayList<>();
        final CountDownLatch latchForPause = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public int onCommandRequest(@NonNull MediaSession session,
                    @NonNull ControllerInfo controllerInfo, @NonNull SessionCommand command) {
                assertEquals(EXPECTED_CONTROLLER_PACKAGE_NAME, controllerInfo.getPackageName());
                assertFalse(controllerInfo.isTrusted());
                commands.add(command);
                if (command.getCommandCode() == SessionCommand.COMMAND_CODE_PLAYER_PAUSE) {
                    latchForPause.countDown();
                    return RESULT_ERROR_INVALID_STATE;
                }
                return RESULT_SUCCESS;
            }
        };

        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mPlayer = new MockPlayer(1);
                mSession = new MediaSession.Builder(mContext, mPlayer)
                        .setId("testOnCommandCallback")
                        .setSessionCallback(sHandlerExecutor, callback).build();
            }
        });
        RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);

        controller.getTransportControls().pause();
        assertTrue(latchForPause.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(1, commands.size());
        assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PAUSE,
                (long) commands.get(0).getCommandCode());

        controller.getTransportControls().play();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(2, commands.size());
        assertEquals(SessionCommand.COMMAND_CODE_PLAYER_PLAY,
                (long) commands.get(1).getCommandCode());
    }

    /**
     * Test potential deadlock for calls between controller and session.
     */
    @Test
    @LargeTest
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
                    mSession = new MediaSession.Builder(mContext, player)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                            .setId("testDeadlock").build();
                }
            });
            final RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, mSession.getSessionCompat().getSessionToken(), true);
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int state = SessionPlayer.PLAYER_STATE_ERROR;
                    for (int i = 0; i < 100; i++) {
                        // triggers call from session to controller.
                        player.notifyPlayerStateChanged(state);
                        // triggers call from controller to session.
                        controller.getTransportControls().play();

                        // Repeat above
                        player.notifyPlayerStateChanged(state);
                        controller.getTransportControls().pause();
                        player.notifyPlayerStateChanged(state);
                        controller.getTransportControls().stop();
                        player.notifyPlayerStateChanged(state);
                        controller.getTransportControls().skipToNext();
                        player.notifyPlayerStateChanged(state);
                        controller.getTransportControls().skipToPrevious();
                    }
                    // This may hang if deadlock happens.
                    latch.countDown();
                }
            });
            assertTrue(latch.await(3, TimeUnit.SECONDS));
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
    @LargeTest
    public void controllerAfterSessionIsGone() throws InterruptedException {
        mSession.close();
        testSessionCallbackIsNotCalled();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .setId(TAG)
                .build();
        testSessionCallbackIsNotCalled();
    }

    void testSessionCallbackIsNotCalled() throws InterruptedException {
        mController.getTransportControls().play();
        assertFalse(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
