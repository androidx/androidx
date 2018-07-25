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

package androidx.media.test.service.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static androidx.media.MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media.test.lib.MockActivity;
import androidx.media.test.lib.TestUtils;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media.test.service.MediaTestUtils;
import androidx.media.test.service.MockPlayerConnector;
import androidx.media.test.service.MockPlaylistAgent;
import androidx.media.test.service.MockRemotePlayerConnector;
import androidx.media.test.service.RemoteMediaControllerCompat;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaPlaylistAgent;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionCallback;
import androidx.media2.MediaUtils2;
import androidx.media2.Rating2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.test.filters.FlakyTest;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link SessionCallback} working with {@link MediaControllerCompat}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaSession2LegacyCallbackTest extends MediaSession2TestBase {
    private static final String TAG = "MediaSession2LegacyCallbackTest";

    PendingIntent mIntent;
    MediaSession2 mSession;
    RemoteMediaControllerCompat mController;
    MockPlayerConnector mPlayer;
    MockPlaylistAgent mMockAgent;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession2 to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity, 0);

        mPlayer = new MockPlayerConnector(1);
        mMockAgent = new MockPlaylistAgent();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
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
    }

    @Test
    public void testPlay() {
        prepareLooper();
        mController.getTransportControls().play();
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
        mController.getTransportControls().pause();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testStop() {
        prepareLooper();

        // MediaControllerCompat#stop() will call MediaSession2#pause() and MediaSession2#seekTo(0).
        // Therefore, the latch's initial count is 2.
        MockPlayerConnector player = new MockPlayerConnector(2);
        player.mCurrentPosition = 1530;
        mSession.updatePlayerConnector(player, null);

        mController.getTransportControls().stop();
        try {
            assertTrue(player.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(player.mPauseCalled);
        assertTrue(player.mSeekToCalled);
        assertEquals(0, player.mSeekPosition);
    }

    @Test
    public void testPrepare() {
        prepareLooper();
        mController.getTransportControls().prepare();
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
        mController.getTransportControls().seekTo(seekPosition);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void testAddQueueItem() throws InterruptedException {
        prepareLooper();
        final int playlistSize = 10;

        List<MediaItem2> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mMockAgent.mPlaylist = playlist;
        mMockAgent.callNotifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(WAIT_TIME_MS);

        // Prepare an item to add.
        final String mediaId = "media_id";
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .build();
        mController.addQueueItem(desc);

        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mAddPlaylistItemCalled);
        assertEquals(mediaId, mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testAddQueueItemWithIndex() throws InterruptedException {
        prepareLooper();
        final int playlistSize = 10;

        List<MediaItem2> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mMockAgent.mPlaylist = playlist;
        mMockAgent.callNotifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(WAIT_TIME_MS);

        // Prepare an item to add.
        final int testIndex = 0;
        final String mediaId = "media_id";
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .build();
        mController.addQueueItem(desc, testIndex);

        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mAddPlaylistItemCalled);
        assertEquals(testIndex, mMockAgent.mIndex);
        assertEquals(mediaId, mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testRemoveQueueItem() throws InterruptedException {
        prepareLooper();
        final int playlistSize = 10;

        List<MediaItem2> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mMockAgent.mPlaylist = playlist;
        mMockAgent.callNotifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(WAIT_TIME_MS);

        // Select an item to remove.
        final int targetIndex = 3;
        final MediaItem2 targetItem = playlist.get(targetIndex);
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(targetItem.getMediaId())
                .build();
        mController.removeQueueItem(desc);

        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mRemovePlaylistItemCalled);
        assertEquals(targetItem.getMediaId(), mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testSkipToPrevious() throws InterruptedException {
        prepareLooper();
        mController.getTransportControls().skipToPrevious();
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToNext() throws InterruptedException {
        prepareLooper();
        mController.getTransportControls().skipToNext();
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToQueueItem() throws InterruptedException {
        prepareLooper();
        final int playlistSize = 10;

        List<MediaItem2> playlist = MediaTestUtils.createPlaylist(playlistSize);
        mMockAgent.mPlaylist = playlist;
        mMockAgent.callNotifyPlaylistChanged();
        // Wait some time for setting the playlist.
        Thread.sleep(WAIT_TIME_MS);

        // Get Queue from local MediaControllerCompat.
        List<QueueItem> queue = mSession.getSessionCompat().getController().getQueue();
        final int targetIndex = 3;
        mController.getTransportControls().skipToQueueItem(queue.get(targetIndex).getQueueId());

        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToPlaylistItemCalled);
        assertEquals(playlist.get(targetIndex), mMockAgent.mItem);
    }

    @Test
    public void testSetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        mController.getTransportControls().setShuffleMode(testShuffleMode);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mMockAgent.mShuffleMode);
    }

    @Test
    public void testSetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        mController.getTransportControls().setRepeatMode(testRepeatMode);
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
        MockRemotePlayerConnector remotePlayer =
                new MockRemotePlayerConnector(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayerConnector(remotePlayer, null);

        final int targetVolume = 50;
        mController.setVolumeTo(targetVolume, 0 /* flags */);
        assertTrue(remotePlayer.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(remotePlayer.mSetVolumeToCalled);
        assertEquals(targetVolume, (int) remotePlayer.mCurrentVolume);
    }

    @Test
    public void testAdjustVolume() throws Exception {
        prepareLooper();
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        MockRemotePlayerConnector remotePlayer =
                new MockRemotePlayerConnector(volumeControlType, maxVolume, currentVolume);

        mSession.updatePlayerConnector(remotePlayer, null);

        final int direction = AudioManager.ADJUST_RAISE;
        mController.adjustVolume(direction, 0 /* flags */);
        assertTrue(remotePlayer.mLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
        mSession.updatePlayerConnector(mPlayer, null);

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
        mSession.updatePlayerConnector(mPlayer, null);

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
    public void testSendCommand() throws InterruptedException {
        prepareLooper();
        // TODO(jaewan): Need to revisit with the permission.
        final String testCommand = "test_command";
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "test_args");

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup2 commands = super.onConnect(session, controller);
                commands.addCommand(new SessionCommand2(testCommand, null));
                return commands;
            }

            @Override
            public void onCustomCommand(MediaSession2 session, ControllerInfo controller,
                    SessionCommand2 customCommand, Bundle args, ResultReceiver cb) {
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(testCommand, customCommand.getCustomCommand());
                assertTrue(TestUtils.equals(testArgs, args));
                assertNull(cb);
                latch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        final RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);
        controller.sendCommand(testCommand, testArgs, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testControllerCallback_sessionRejects() throws Exception {
        prepareLooper();
        final SessionCallback sessionCallback = new SessionCallback() {
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

        // Session will not accept the controller's commands.
        RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);
        controller.getTransportControls().play();
        assertFalse(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFastForward() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public void onFastForward(MediaSession2 session, ControllerInfo controller) {
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testFastForward").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().fastForward();
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testRewind").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().rewind();
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromSearch").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromSearch(request, bundle);
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromUri").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromUri(request, bundle);
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPlayFromMediaId").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().playFromMediaId(request, bundle);
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromSearch").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromSearch(request, bundle);
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromUri").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromUri(request, bundle);
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
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testPrepareFromMediaId").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().prepareFromMediaId(request, bundle);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testSetRating() throws InterruptedException {
        prepareLooper();
        final int ratingType = Rating2.RATING_5_STARS;
        final float ratingValue = 3.5f;
        final RatingCompat rating = RatingCompat.newStarRating(ratingType, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public void onSetRating(MediaSession2 session, ControllerInfo controller,
                    String mediaIdOut, Rating2 ratingOut) {
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                } else {
                    assertEquals(LEGACY_CONTROLLER, controller.getPackageName());
                }
                assertEquals(mediaId, mediaIdOut);
                assertEquals(MediaUtils2.convertToRating2(rating), ratingOut);
                latch.countDown();
            }
        };

        mMockAgent.mCurrentMediaItem = MediaTestUtils.createMediaItem(
                mediaId, MediaTestUtils.createDSD());
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testSetRating").build()) {
            RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, session.getSessionCompat().getSessionToken(), true);
            controller.getTransportControls().setRating(rating);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnCommandCallback() throws InterruptedException {
        prepareLooper();
        final ArrayList<SessionCommand2> commands = new ArrayList<>();
        final CountDownLatch latchForPause = new CountDownLatch(1);
        final SessionCallback callback = new SessionCallback() {
            @Override
            public boolean onCommandRequest(MediaSession2 session, ControllerInfo controllerInfo,
                    SessionCommand2 command) {
                assertEquals(CLIENT_PACKAGE_NAME, controllerInfo.getPackageName());
                assertFalse(controllerInfo.isTrusted());
                commands.add(command);
                if (command.getCommandCode() == SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE) {
                    latchForPause.countDown();
                    return false;
                }
                return true;
            }
        };

        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mPlayer = new MockPlayerConnector(1);
                mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                        .setSessionCallback(sHandlerExecutor, callback).build();
            }
        });
        RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                mContext, mSession.getSessionCompat().getSessionToken(), true);

        controller.getTransportControls().pause();
        assertTrue(latchForPause.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(1, commands.size());
        assertEquals(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE,
                (long) commands.get(0).getCommandCode());

        controller.getTransportControls().play();
        assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mPlayCalled);
        assertFalse(mPlayer.mPauseCalled);
        assertEquals(2, commands.size());
        assertEquals(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY,
                (long) commands.get(1).getCommandCode());
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
            final MockPlayerConnector player = new MockPlayerConnector(0);
            sessionHandler.postAndSync(new Runnable() {
                @Override
                public void run() {
                    mSession = new MediaSession2.Builder(mContext)
                            .setPlayer(mPlayer)
                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {
                            })
                            .setId("testDeadlock").build();
                }
            });
            final RemoteMediaControllerCompat controller = new RemoteMediaControllerCompat(
                    mContext, mSession.getSessionCompat().getSessionToken(), true);
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int state = MediaPlayerConnector.PLAYER_STATE_ERROR;
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

    @Test
    @LargeTest
    public void testControllerAfterSessionIsGone() throws InterruptedException {
        prepareLooper();
        final String sessionId = mSession.getToken().getId();

        mSession.close();
        testSessionCallbackIsNotCalled();

        // Ensure that the controller cannot use newly create session with the same ID.
        // Recreated session has different session stub, so previously created controller
        // shouldn't be available.
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
                .setId(sessionId)
                .build();
        testSessionCallbackIsNotCalled();
    }

    void testSessionCallbackIsNotCalled() throws InterruptedException {
        mController.getTransportControls().play();
        assertFalse(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }
}
