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

package androidx.media2.test.client.tests;

import static androidx.media2.common.BaseResult.RESULT_INFO_SKIPPED;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;
import static androidx.media2.test.common.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media2.test.common.CommonConstants.SERVICE_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.CustomAction;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.media2.session.MediaUtils;
import androidx.media2.session.PercentageRating;
import androidx.media2.session.RemoteSessionPlayer;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;
import androidx.media2.test.client.MediaTestUtils;
import androidx.media2.test.client.RemoteMediaSessionCompat;
import androidx.media2.test.common.MockActivity;
import androidx.media2.test.common.PollingCheck;
import androidx.media2.test.common.TestUtils;
import androidx.test.filters.MediumTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController} interacting with {@link MediaSessionCompat}.
 *
 * TODO: Pull out callback tests to a separate file (i.e. MediaControllerLegacyCallbackTest).
 */
@MediumTest
public class MediaControllerLegacyTest extends MediaSessionTestBase {
    private static final String TAG = "MediaControllerLegacyTest";
    private static final long EXPECTED_TIMEOUT_MS = 100;

    AudioManager mAudioManager;
    RemoteMediaSessionCompat mSession;
    MediaController mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSession = new RemoteMediaSessionCompat(DEFAULT_TEST_NAME, mContext);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.cleanUp();
        if (mController != null) {
            mController.close();
        }
    }

    @Test
    public void gettersAfterConnected() throws Exception {
        final long position = 150000;
        final long bufferedPosition = 900000;
        final long timeDiff = 102;
        final float speed = 0.5f;
        final int shuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        final int repeatMode = SessionPlayer.REPEAT_MODE_ALL;
        final MediaMetadataCompat metadata = MediaUtils.convertToMediaMetadataCompat(
                MediaTestUtils.createMetadata());

        mSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, position, speed)
                        .setBufferedPosition(bufferedPosition).build());
        mSession.setMetadata(metadata);
        mSession.setShuffleMode(shuffleMode);
        mSession.setRepeatMode(repeatMode);
        mSession.setRatingType(RatingCompat.RATING_PERCENTAGE);

        mController = createController(mSession.getSessionToken());
        mController.setTimeDiff(timeDiff);

        assertEquals(SessionPlayer.PLAYER_STATE_PLAYING, mController.getPlayerState());
        assertEquals(SessionPlayer.BUFFERING_STATE_COMPLETE,
                mController.getBufferingState());
        assertEquals(bufferedPosition, mController.getBufferedPosition());
        assertEquals(speed, mController.getPlaybackSpeed(), 0.0f);
        assertEquals((double) position + (speed * timeDiff),
                (double) mController.getCurrentPosition(), 100.0 /* 100 ms */);
        assertEquals(metadata.getDescription().getMediaId(),
                mController.getCurrentMediaItem().getMediaId());
        Rating rating = mController.getCurrentMediaItem().getMetadata()
                .getRating(MediaMetadata.METADATA_KEY_USER_RATING);
        assertTrue(rating instanceof PercentageRating);
        assertFalse(rating.isRated());
        assertEquals(shuffleMode, mController.getShuffleMode());
        assertEquals(repeatMode, mController.getRepeatMode());
    }

    @Test
    public void getPackageName() throws Exception {
        mController = createController(mSession.getSessionToken());
        assertEquals(SERVICE_PACKAGE_NAME, mController.getConnectedToken().getPackageName());
    }

    @Test
    public void getSessionActivity() throws Exception {
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        PendingIntent pi = PendingIntent.getActivity(mContext, 0, sessionActivity,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        mSession.setSessionActivity(pi);

        mController = createController(mSession.getSessionToken());
        PendingIntent sessionActivityOut = mController.getSessionActivity();
        assertNotNull(sessionActivityOut);
        if (Build.VERSION.SDK_INT >= 17) {
            // PendingIntent#getCreatorPackage() is added in API 17.
            assertEquals(mContext.getPackageName(), sessionActivityOut.getCreatorPackage());
        }
    }

    /**
     * This also tests {@link ControllerCallback#onRepeatModeChanged(MediaController, int)}.
     */
    @Test
    public void getRepeatMode() throws Exception {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onRepeatModeChanged(@NonNull MediaController controller, int repeatMode) {
                assertEquals(testRepeatMode, repeatMode);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);

        mSession.setRepeatMode(testRepeatMode);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, mController.getRepeatMode());
    }

    /**
     * This also tests {@link ControllerCallback#onShuffleModeChanged(MediaController, int)}.
     */
    @Test
    public void getShuffleMode() throws Exception {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onShuffleModeChanged(@NonNull MediaController controller, int shuffleMode) {
                assertEquals(testShuffleMode, shuffleMode);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);

        mSession.setShuffleMode(testShuffleMode);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, mController.getShuffleMode());
    }

    /**
     * This also tests {@link ControllerCallback#onPlaylistChanged(
     * MediaController, List, MediaMetadata)}.
     */
    @Test
    public void getPlaylist() throws Exception {
        final List<MediaItem> testList = MediaTestUtils.createFileMediaItems(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);
        final AtomicReference<List<MediaItem>> listFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    List<MediaItem> playlist, MediaMetadata metadata) {
                assertNotNull(playlist);
                assertEquals(testList.size(), playlist.size());
                for (int i = 0; i < playlist.size(); i++) {
                    assertEquals(testList.get(i).getMediaId(), playlist.get(i).getMediaId());
                }
                listFromCallback.set(playlist);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);

        mSession.setQueue(testQueue);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(listFromCallback.get(), mController.getPlaylist());
    }

    @Test
    public void getPlaylistMetadata() throws Exception {
        final AtomicReference<MediaMetadata> metadataFromCallback = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final CharSequence queueTitle = "test queue title";
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaylistMetadataChanged(@NonNull MediaController controller,
                    MediaMetadata metadata) {
                assertEquals(queueTitle.toString(),
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                metadataFromCallback.set(metadata);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);

        mSession.setQueueTitle(queueTitle);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(metadataFromCallback.get(), mController.getPlaylistMetadata());
    }

    @Test
    public void getCurrentMediaItemAfterConnected() throws Exception {
        mController = createController(mSession.getSessionToken(), true, null);
        assertNull(mController.getCurrentMediaItem());
    }

    @Test
    public void getCurrentMediaItemAfterConnected_metadata() throws Exception {
        final String testMediaId = "testGetCurrentMediaItemWhenConnected_metadata";
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, testMediaId)
                .build();
        mSession.setMetadata(metadata);

        mController = createController(mSession.getSessionToken(), true, null);
        assertEquals(testMediaId, mController.getCurrentMediaItem().getMediaId());
    }

    @Test
    public void setMediaUri_resultSetAfterPrepare() throws Exception {
        mController = createController(mSession.getSessionToken(), true, null);

        Uri testUri = Uri.parse("androidx://test");
        ListenableFuture<SessionResult> future =
                mController.setMediaUri(testUri, /* extras= */ null);

        SessionResult result;
        try {
            result = future.get(EXPECTED_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TimeoutException is expected");
        } catch (TimeoutException e) {
            // expected.
        }

        mController.prepare();

        result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
    }

    @Test
    public void setMediaUri_resultSetAfterPlay() throws Exception {
        mController = createController(mSession.getSessionToken(), true, null);

        Uri testUri = Uri.parse("androidx://test");
        ListenableFuture<SessionResult> future =
                mController.setMediaUri(testUri, /* extras= */ null);

        SessionResult result;
        try {
            result = future.get(EXPECTED_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TimeoutException is expected");
        } catch (TimeoutException e) {
            // expected.
        }

        mController.play();

        result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_SUCCESS, result.getResultCode());
    }

    @Test
    public void setMediaUris_multipleCalls_previousCallReturnsResultInfoSkipped() throws Exception {
        mController = createController(mSession.getSessionToken(), true, null);

        Uri testUri1 = Uri.parse("androidx://test1");
        Uri testUri2 = Uri.parse("androidx://test2");
        ListenableFuture<SessionResult> future1 =
                mController.setMediaUri(testUri1, /* extras= */ null);
        ListenableFuture<SessionResult> future2 =
                mController.setMediaUri(testUri2, /* extras= */ null);

        mController.prepare();

        SessionResult result1 = future1.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        SessionResult result2 = future2.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(RESULT_INFO_SKIPPED, result1.getResultCode());
        assertEquals(RESULT_SUCCESS, result2.getResultCode());
    }

    @Test
    public void controllerCallback_onCurrentMediaItemChanged_byMetadataChange()
            throws Exception {
        final String testMediaId = "testControllerCallback_onCurrentMediaItemChanged_bySetMetadata";
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                    MediaItem item) {
                MediaTestUtils.assertMediaItemHasId(item, testMediaId);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, testMediaId)
                .build();
        mSession.setMetadata(metadata);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onCurrentMediaItemChanged_byActiveQueueItemChange()
            throws Exception {
        final List<MediaItem> testList = MediaTestUtils.createFileMediaItems(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);
        mSession.setQueue(testQueue);

        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();

        // Set the current active queue item to index 'oldItemIndex'.
        final int oldItemIndex = 0;
        builder.setActiveQueueItemId(testQueue.get(oldItemIndex).getQueueId());
        mSession.setPlaybackState(builder.build());

        final int newItemIndex = 1;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                    MediaItem item) {
                MediaTestUtils.assertMediaIdEquals(testList.get(newItemIndex), item);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);

        // The new playbackState will tell the controller that the active queue item is changed to
        // 'newItemIndex'.
        builder.setActiveQueueItemId(testQueue.get(newItemIndex).getQueueId());
        mSession.setPlaybackState(builder.build());

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onSeekCompleted() throws Exception {
        final long testSeekPosition = 400;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onSeekCompleted(@NonNull MediaController controller, long position) {
                assertEquals(testSeekPosition, position);
                latch.countDown();
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        mController = createController(mSession.getSessionToken(), true, callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testSeekPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onBufferingCompleted() throws Exception {
        final List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(1);
        final MediaMetadataCompat metadata = MediaUtils.convertToMediaMetadataCompat(
                testPlaylist.get(0).getMetadata());

        final int testBufferingState = SessionPlayer.BUFFERING_STATE_COMPLETE;
        final long testBufferingPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(@NonNull MediaController controller,
                    @NonNull MediaItem item, int state) {
                assertEquals(metadata.getDescription().getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        mSession.setMetadata(metadata);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(0)
                .build());
        mController = createController(mSession.getSessionToken(), true, callback);
        mController.setTimeDiff(Long.valueOf(0));

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(testBufferingPosition)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onBufferingStarved() throws Exception {
        final List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(1);
        final MediaMetadataCompat metadata = MediaUtils.convertToMediaMetadataCompat(
                testPlaylist.get(0).getMetadata());

        final int testBufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED;
        final long testBufferingPosition = 0;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onBufferingStateChanged(@NonNull MediaController controller,
                    @NonNull MediaItem item, int state) {
                assertEquals(metadata.getDescription().getMediaId(), item.getMediaId());
                assertEquals(testBufferingState, state);
                assertEquals(testBufferingState, controller.getBufferingState());
                assertEquals(testBufferingPosition, controller.getBufferedPosition());
                latch.countDown();
            }
        };
        mSession.setMetadata(metadata);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 100 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(500)
                .build());
        mController = createController(mSession.getSessionToken(), true, callback);
        mController.setTimeDiff(0L);

        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0 /* position */,
                        1f /* playbackSpeed */)
                .setBufferedPosition(testBufferingPosition)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onPlayerStateChanged() throws Exception {
        final int testPlayerState = SessionPlayer.PLAYER_STATE_PLAYING;
        final long testPosition = 500;
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
                assertEquals(testPlayerState, state);
                assertEquals(testPlayerState, controller.getPlayerState());
                assertEquals(testPosition, controller.getCurrentPosition());
                latch.countDown();
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0 /* position */,
                        1f /* playbackSpeed */)
                .build());
        mController = createController(mSession.getSessionToken(), true, callback);
        mController.setTimeDiff(Long.valueOf(0));
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, testPosition /* position */,
                        1f /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onPlaybackSpeedChanged() throws Exception {
        final float testSpeed = 3.0f;

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaybackSpeedChanged(@NonNull MediaController controller, float speed) {
                assertEquals(testSpeed, speed, 0.0f);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0 /* position */,
                        testSpeed /* playbackSpeed */)
                .build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onPlaybackInfoChanged_byPlaybackTypeChangeToRemote()
            throws Exception {
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        final int maxVolume = 100;
        final int currentVolume = 45;

        final AtomicReference<MediaController.PlaybackInfo> infoOut = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaybackInfoChanged(@NonNull MediaController controller,
                    @NonNull MediaController.PlaybackInfo info) {
                // Here, we are intentionally avoid using assertEquals(), since this callback
                // can be called many times which of them have inaccurate values.
                Log.d(TAG, "Given playbackType=" + info.getPlaybackType()
                        + " controlType=" + info.getControlType()
                        + " maxVolume=" + info.getMaxVolume()
                        + " currentVolume=" + info.getCurrentVolume()
                        + " audioAttrs=" + info.getAudioAttributes());
                if (MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE == info.getPlaybackType()
                        && volumeControlType == info.getControlType()
                        && maxVolume == info.getMaxVolume()
                        && currentVolume == info.getCurrentVolume()) {
                    infoOut.set(info);
                    latch.countDown();
                }
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.setPlaybackToRemote(volumeControlType, maxVolume, currentVolume);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(infoOut.get(), mController.getPlaybackInfo());
    }

    @Test
    public void controllerCallback_onPlaybackInfoChanged_byPlaybackTypeChangeToLocal()
            throws Exception {
        if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
            // In API 21 and 22, onAudioInfoChanged is not called.
            return;
        }
        mSession.setPlaybackToRemote(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 100, 45);

        final int testLocalStreamType = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(testLocalStreamType);
        final int currentVolume = mAudioManager.getStreamVolume(testLocalStreamType);

        final AtomicReference<MediaController.PlaybackInfo> infoOut = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onPlaybackInfoChanged(@NonNull MediaController controller,
                    @NonNull MediaController.PlaybackInfo info) {
                assertEquals(MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                        info.getPlaybackType());
                assertEquals(RemoteSessionPlayer.VOLUME_CONTROL_ABSOLUTE, info.getControlType());
                assertEquals(maxVolume, info.getMaxVolume());
                assertEquals(currentVolume, info.getCurrentVolume());
                infoOut.set(info);
                latch.countDown();
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.setPlaybackToLocal(testLocalStreamType);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(infoOut.get(), mController.getPlaybackInfo());
    }

    @Test
    public void controllerCallback_onCustomCommand() throws Exception {
        final String event = "testControllerCallback_onCustomCommand";
        final Bundle extras = TestUtils.createTestBundle();

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @NonNull
            @Override
            public SessionResult onCustomCommand(@NonNull MediaController controller,
                    @NonNull SessionCommand command, Bundle args) {
                assertEquals(event, command.getCustomAction());
                assertTrue(TestUtils.equals(extras, args));
                latch.countDown();
                return null;
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.sendSessionEvent(event, extras);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onSetCustomLayout() throws Exception {
        final CustomAction testCustomAction1 =
                new CustomAction.Builder("testCustomAction1", "testName1", 1).build();
        final CustomAction testCustomAction2 =
                new CustomAction.Builder("testCustomAction2", "testName2", 2).build();
        final CountDownLatch latch = new CountDownLatch(2);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public int onSetCustomLayout(@NonNull MediaController controller,
                    @NonNull List<CommandButton> layout) {
                assertEquals(1, layout.size());
                CommandButton button = layout.get(0);

                switch ((int) latch.getCount()) {
                    case 2:
                        assertEquals(testCustomAction1.getAction(),
                                button.getCommand().getCustomAction());
                        assertEquals(testCustomAction1.getName(), button.getDisplayName());
                        assertEquals(testCustomAction1.getIcon(), button.getIconResId());
                        break;
                    case 1:
                        assertEquals(testCustomAction2.getAction(),
                                button.getCommand().getCustomAction());
                        assertEquals(testCustomAction2.getName(), button.getDisplayName());
                        assertEquals(testCustomAction2.getIcon(), button.getIconResId());
                        break;
                }
                latch.countDown();
                return RESULT_SUCCESS;
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .addCustomAction(testCustomAction1).build());
        // onSetCustomLayout will be called when its connected
        mController = createController(mSession.getSessionToken(), true, callback);
        // onSetCustomLayout will be called again when the custom action in the playback state is
        // changed.
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .addCustomAction(testCustomAction2).build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onAllowedCommandChanged() throws Exception {
        final CustomAction testCustomAction1 =
                new CustomAction.Builder("testCustomAction1", "testName1", 1).build();
        final CustomAction testCustomAction2 =
                new CustomAction.Builder("testCustomAction2", "testName2", 2).build();
        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            public void onAllowedCommandsChanged(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup commands) {
                assertFalse(commands.hasCommand(new SessionCommand(
                        testCustomAction1.getAction(), testCustomAction1.getExtras())));
                assertTrue(commands.hasCommand(new SessionCommand(
                        testCustomAction2.getAction(), testCustomAction2.getExtras())));
                latch.countDown();
            }
        };
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .addCustomAction(testCustomAction1).build());
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .addCustomAction(testCustomAction2).build());
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void controllerCallback_onConnected() throws Exception {
        mController = createController(mSession.getSessionToken());
    }

    @Test
    public void controllerCallback_onDisconnected() throws Exception {
        mController = createController(mSession.getSessionToken());
        mSession.release();
        waitForDisconnect(mController, true);
    }

    @Test
    public void controllerCallback_close() throws Exception {
        mController = createController(mSession.getSessionToken());
        mController.close();
        waitForDisconnect(mController, true);
    }

    @Test
    public void close_twice() throws Exception {
        mController = createController(mSession.getSessionToken());
        mController.close();
        mController.close();
    }

    @Test
    public void isConnected() throws Exception {
        mController = createController(mSession.getSessionToken());
        assertTrue(mController.isConnected());

        mSession.release();
        waitForDisconnect(mController, true);
        assertFalse(mController.isConnected());
    }

    @Test
    public void close_beforeConnected() throws InterruptedException {
        MediaController controller = createController(mSession.getSessionToken(), false, null);

        // Should not crash.
        controller.close();
    }

    @Test
    public void controllerCallback_onCustomCommand_bySetCaptioningEnabled() throws Exception {
        final String sessionCommandOnCaptioningEnabledChanged =
                "android.media.session.command.ON_CAPTIONING_ENALBED_CHANGED";
        final String argumentCaptioningEnabled = "androidx.media2.argument.CAPTIONING_ENABLED";

        final CountDownLatch latch = new CountDownLatch(1);
        final ControllerCallback callback = new ControllerCallback() {
            @Override
            @NonNull
            public SessionResult onCustomCommand(@NonNull MediaController controller,
                    @NonNull SessionCommand command, Bundle args) {
                assertEquals(sessionCommandOnCaptioningEnabledChanged, command.getCustomAction());
                assertEquals(true, args.getBoolean(argumentCaptioningEnabled, false));
                latch.countDown();
                return new SessionResult(RESULT_SUCCESS, null);
            }
        };
        mController = createController(mSession.getSessionToken(), true, callback);
        mSession.setCaptioningEnabled(true);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void constructorWithoutCallback() throws InterruptedException {
        MediaController controller = new MediaController.Builder(mContext)
                .setSessionCompatToken(mSession.getSessionToken())
                .build();
        PollingCheck.waitFor(TIMEOUT_MS, () -> controller.isConnected());
    }
}
