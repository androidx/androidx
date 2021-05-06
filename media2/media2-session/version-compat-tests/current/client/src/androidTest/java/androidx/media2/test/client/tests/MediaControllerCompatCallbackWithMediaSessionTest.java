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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.HeartRating;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaUtils;
import androidx.media2.session.RemoteSessionPlayer;
import androidx.media2.session.ThumbRating;
import androidx.media2.test.client.MediaTestUtils;
import androidx.media2.test.client.RemoteMediaSession;
import androidx.media2.test.common.PollingCheck;
import androidx.media2.test.common.TestUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link MediaControllerCompat.Callback} with {@link MediaSession}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControllerCompatCallbackWithMediaSessionTest extends MediaSessionTestBase {
    private static final String TAG = "MCCCallbackTestWithMS2";

    private static final long TIMEOUT_MS = 1000L;
    private static final float EPSILON = 1e-6f;

    private RemoteMediaSession mSession;
    private MediaControllerCompat mControllerCompat;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mSession = new RemoteMediaSession(TAG, mContext, null);
        mControllerCompat = new MediaControllerCompat(mContext, mSession.getCompatToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.close();
    }

    @Test
    public void gettersAfterConnected() throws Exception {
        int testState = SessionPlayer.PLAYER_STATE_PLAYING;
        int testBufferingPosition = 1500;
        float testSpeed = 1.5f;
        List<MediaItem> testPlaylist = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            testPlaylist.add(new MediaItem.Builder()
                    .setMetadata(new MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "id=" + i)
                            .putRating(MediaMetadata.METADATA_KEY_USER_RATING, new HeartRating())
                            .build())
                    .build());
        }
        int testItemIndex = 0;
        String testPlaylistTitle = "testPlaylistTitle";
        MediaMetadata testPlaylistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, testPlaylistTitle).build();
        int testShuffleMode = SessionPlayer.SHUFFLE_MODE_ALL;
        int testRepeatMode = SessionPlayer.SHUFFLE_MODE_GROUP;

        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setPlayerState(testState)
                .setBufferedPosition(testBufferingPosition)
                .setPlaybackSpeed(testSpeed)
                .setPlaylist(testPlaylist)
                .setPlaylistMetadata(testPlaylistMetadata)
                .setCurrentMediaItem(testPlaylist.get(testItemIndex))
                .setShuffleMode(testShuffleMode)
                .setRepeatMode(testRepeatMode)
                .build();
        mSession.updatePlayer(playerConfig);

        MediaControllerCompat controller =
                new MediaControllerCompat(mContext, mSession.getCompatToken());
        CountDownLatch latch = new CountDownLatch(1);
        controller.registerCallback(
                new MediaControllerCompat.Callback() {
                    @Override
                    public void onSessionReady() {
                        latch.countDown();
                    }
                }, sHandler);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(testState, MediaUtils.convertToPlayerState(controller.getPlaybackState()));
        assertEquals(testBufferingPosition, controller.getPlaybackState().getBufferedPosition());
        assertEquals(testSpeed, controller.getPlaybackState().getPlaybackSpeed(), EPSILON);

        assertEquals(testPlaylist.get(testItemIndex).getMediaId(),
                controller.getMetadata().getString(MediaMetadata.METADATA_KEY_MEDIA_ID));

        List<QueueItem> queue = controller.getQueue();
        assertNotNull(queue);
        assertEquals(testPlaylist.size(), queue.size());
        for (int i = 0; i < testPlaylist.size(); i++) {
            assertEquals(testPlaylist.get(i).getMediaId(),
                    queue.get(i).getDescription().getMediaId());
        }
        assertEquals(testPlaylistTitle, controller.getQueueTitle().toString());
        // TODO(b/182255673): Remove this when the relevant fix is released.
        if (MediaTestUtils.isServiceToT()) {
            assertEquals(RatingCompat.RATING_HEART, controller.getRatingType());
            assertEquals(testShuffleMode, controller.getShuffleMode());
            assertEquals(testRepeatMode, controller.getRepeatMode());
        }
    }

    @Test
    public void repeatModeChange() throws Exception {
        int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger repeatModeRef = new AtomicInteger();
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onRepeatModeChanged(int repeatMode) {
                repeatModeRef.set(repeatMode);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setRepeatMode(testRepeatMode);
        mSession.getMockPlayer().notifyRepeatModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testRepeatMode, repeatModeRef.get());
        assertEquals(testRepeatMode, mControllerCompat.getRepeatMode());
    }

    @Test
    public void shuffleModeChange() throws Exception {
        int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger shuffleModeRef = new AtomicInteger();
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onShuffleModeChanged(int shuffleMode) {
                shuffleModeRef.set(shuffleMode);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setShuffleMode(testShuffleMode);
        mSession.getMockPlayer().notifyShuffleModeChanged();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testShuffleMode, shuffleModeRef.get());
        assertEquals(testShuffleMode, mControllerCompat.getShuffleMode());
    }

    @Test
    public void close() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onSessionDestroyed() {
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.close();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void updatePlayer() throws Exception {
        int testState = SessionPlayer.PLAYER_STATE_PLAYING;
        int testBufferingPosition = 1500;
        float testSpeed = 1.5f;
        List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(3);
        int testItemIndex = 0;
        String testPlaylistTitle = "testPlaylistTitle";
        MediaMetadata testPlaylistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, testPlaylistTitle).build();
        int testShuffleMode = SessionPlayer.SHUFFLE_MODE_ALL;
        int testRepeatMode = SessionPlayer.SHUFFLE_MODE_GROUP;

        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        AtomicReference<MediaMetadataCompat> metadataRef = new AtomicReference<>();
        AtomicReference<CharSequence> queueTitleRef = new AtomicReference<>();
        AtomicInteger shuffleModeRef = new AtomicInteger();
        AtomicInteger repeatModeRef = new AtomicInteger();
        CountDownLatch latchForPlaybackState = new CountDownLatch(1);
        CountDownLatch latchForMetadata = new CountDownLatch(1);
        CountDownLatch latchForQueue = new CountDownLatch(2);
        CountDownLatch latchForShuffleMode = new CountDownLatch(1);
        CountDownLatch latchForRepeatMode = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latchForPlaybackState.countDown();
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                metadataRef.set(metadata);
                latchForMetadata.countDown();
            }

            @Override
            public void onQueueChanged(List<QueueItem> queue) {
                latchForQueue.countDown();
            }

            @Override
            public void onQueueTitleChanged(CharSequence title) {
                queueTitleRef.set(title);
                latchForQueue.countDown();
            }

            @Override
            public void onShuffleModeChanged(int shuffleMode) {
                shuffleModeRef.set(shuffleMode);
                latchForShuffleMode.countDown();
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                repeatModeRef.set(repeatMode);
                latchForRepeatMode.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setPlayerState(testState)
                .setBufferedPosition(testBufferingPosition)
                .setPlaybackSpeed(testSpeed)
                .setPlaylist(testPlaylist)
                .setPlaylistMetadata(testPlaylistMetadata)
                .setCurrentMediaItem(testPlaylist.get(testItemIndex))
                .setShuffleMode(testShuffleMode)
                .setRepeatMode(testRepeatMode)
                .build();
        mSession.updatePlayer(playerConfig);

        assertTrue(latchForPlaybackState.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testState, MediaUtils.convertToPlayerState(playbackStateRef.get()));
        assertEquals(testBufferingPosition, playbackStateRef.get().getBufferedPosition());
        assertEquals(testSpeed, playbackStateRef.get().getPlaybackSpeed(), EPSILON);

        assertTrue(latchForMetadata.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testPlaylist.get(testItemIndex).getMediaId(),
                metadataRef.get().getString(MediaMetadata.METADATA_KEY_MEDIA_ID));

        assertTrue(latchForQueue.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        List<QueueItem> queue = mControllerCompat.getQueue();
        assertNotNull(queue);
        assertEquals(testPlaylist.size(), queue.size());
        for (int i = 0; i < testPlaylist.size(); i++) {
            assertEquals(testPlaylist.get(i).getMediaId(),
                    queue.get(i).getDescription().getMediaId());
        }
        assertEquals(testPlaylistTitle, queueTitleRef.get().toString());

        // TODO(b/182255673): Remove this when the relevant fix is released.
        if (MediaTestUtils.isServiceToT()) {
            assertTrue(latchForShuffleMode.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(testShuffleMode, shuffleModeRef.get());
            assertTrue(latchForRepeatMode.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(testRepeatMode, repeatModeRef.get());
        }
    }

    @Test
    public void updatePlayer_playbackTypeChangedToRemote() throws Exception {
        int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        int maxVolume = 25;
        int currentVolume = 10;

        CountDownLatch playbackInfoNotified = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                if (info.getPlaybackType()
                        == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE
                        && info.getVolumeControl() == controlType
                        && info.getMaxVolume() == maxVolume
                        && info.getCurrentVolume() == currentVolume) {
                    playbackInfoNotified.countDown();
                }
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setVolumeControlType(controlType)
                .setMaxVolume(maxVolume)
                .setCurrentVolume(currentVolume)
                .build();
        mSession.updatePlayer(playerConfig);

        assertTrue(playbackInfoNotified.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                info.getPlaybackType());
        assertEquals(controlType, info.getVolumeControl());
        assertEquals(maxVolume, info.getMaxVolume());
        assertEquals(currentVolume, info.getCurrentVolume());
    }

    @Test
    public void updatePlayer_playbackTypeChangedToLocal() throws Exception {
        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setVolumeControlType(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE)
                .setMaxVolume(10)
                .setCurrentVolume(1)
                .build();
        mSession.updatePlayer(playerConfig);

        int legacyStream = AudioManager.STREAM_RING;
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(legacyStream).build();

        CountDownLatch playbackInfoNotified = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                if (info.getPlaybackType() == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                        && info.getAudioAttributes().getLegacyStreamType() == legacyStream) {
                    playbackInfoNotified.countDown();
                }
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        Bundle playerConfigToUpdate = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setAudioAttributes(attrs)
                .build();
        mSession.updatePlayer(playerConfigToUpdate);

        // In API 21 and 22, onAudioInfoChanged is not called when playback is changed to local.
        if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
            PollingCheck.waitFor(TIMEOUT_MS, () -> {
                MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
                return info.getPlaybackType()
                        == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                        && info.getAudioAttributes().getLegacyStreamType() == legacyStream;
            });
        } else {
            assertTrue(playbackInfoNotified.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
            assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                    info.getPlaybackType());
            assertEquals(legacyStream, info.getAudioAttributes().getLegacyStreamType());
        }
    }

    @Test
    public void updatePlayer_playbackTypeNotChanged_local() throws Exception {
        int legacyStream = AudioManager.STREAM_RING;
        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(legacyStream).build();

        CountDownLatch playbackInfoNotified = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                if (info.getPlaybackType() == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                        && info.getAudioAttributes().getLegacyStreamType() == legacyStream) {
                    playbackInfoNotified.countDown();
                }
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setAudioAttributes(attrs)
                .build();
        mSession.updatePlayer(playerConfig);

        // In API 21+, onAudioInfoChanged() is not called when playbackType is not changed.
        if (Build.VERSION.SDK_INT >= 21) {
            PollingCheck.waitFor(TIMEOUT_MS, () -> {
                MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
                return info.getPlaybackType()
                        == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                        && info.getAudioAttributes().getLegacyStreamType() == legacyStream;
            });
        } else {
            assertTrue(playbackInfoNotified.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
            assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                    info.getPlaybackType());
            assertEquals(legacyStream, info.getAudioAttributes().getLegacyStreamType());
        }
    }

    @Test
    public void updatePlayer_playbackTypeNotChanged_remote() throws Exception {
        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setVolumeControlType(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE)
                .setMaxVolume(10)
                .setCurrentVolume(1)
                .build();
        mSession.updatePlayer(playerConfig);

        int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        int maxVolume = 25;
        int currentVolume = 1;

        CountDownLatch playbackInfoNotified = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                if (info.getPlaybackType()
                        == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE
                        && info.getVolumeControl() == controlType
                        && info.getMaxVolume() == maxVolume
                        && info.getCurrentVolume() == currentVolume) {
                    playbackInfoNotified.countDown();
                }
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        Bundle playerConfigToUpdate = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setVolumeControlType(controlType)
                .setMaxVolume(maxVolume)
                .setCurrentVolume(currentVolume)
                .build();
        mSession.updatePlayer(playerConfigToUpdate);

        // In API 21+, onAudioInfoChanged() is not called when playbackType is not changed.
        if (Build.VERSION.SDK_INT >= 21) {
            PollingCheck.waitFor(TIMEOUT_MS, () -> {
                MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
                return info.getPlaybackType()
                        == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE
                        && info.getVolumeControl() == controlType
                        && info.getMaxVolume() == maxVolume
                        && info.getCurrentVolume() == currentVolume;
            });
        } else {
            assertTrue(playbackInfoNotified.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            MediaControllerCompat.PlaybackInfo info = mControllerCompat.getPlaybackInfo();
            assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                    info.getPlaybackType());
            assertEquals(controlType, info.getVolumeControl());
            assertEquals(maxVolume, info.getMaxVolume());
            assertEquals(currentVolume, info.getCurrentVolume());
        }
    }

    @Test
    public void playerStateChange() throws Exception {
        int targetState = SessionPlayer.PLAYER_STATE_PLAYING;

        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onSessionReady() {
                latch.countDown();
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().notifyPlayerStateChanged(targetState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(targetState, MediaUtils.convertToPlayerState(playbackStateRef.get()));
        assertEquals(targetState,
                MediaUtils.convertToPlayerState(mControllerCompat.getPlaybackState()));
    }

    @Test
    public void playbackSpeedChange() throws Exception {
        float speed = 1.5f;

        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setPlaybackSpeed(speed);
        mSession.getMockPlayer().notifyPlaybackSpeedChanged(speed);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, playbackStateRef.get().getPlaybackSpeed(), EPSILON);
        assertEquals(speed, mControllerCompat.getPlaybackState().getPlaybackSpeed(), EPSILON);
    }

    @Test
    public void bufferingStateChange() throws Exception {
        List<MediaItem> testPlaylist = MediaTestUtils.createFileMediaItems(3);
        int testItemIndex = 0;
        int testBufferingState = SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        long testBufferingPosition = 500;
        mSession.getMockPlayer().setPlaylistWithFakeItem(testPlaylist);

        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setBufferedPosition(testBufferingPosition);
        mSession.getMockPlayer().notifyBufferingStateChanged(testItemIndex, testBufferingState);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testBufferingPosition, playbackStateRef.get().getBufferedPosition());
        assertEquals(testBufferingPosition,
                mControllerCompat.getPlaybackState().getBufferedPosition());
    }

    @Test
    public void seekComplete() throws Exception {
        long testSeekPosition = 1300;

        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setCurrentPosition(testSeekPosition);
        mSession.getMockPlayer().setPlayerState(SessionPlayer.PLAYER_STATE_PAUSED);
        mSession.getMockPlayer().notifySeekCompleted(testSeekPosition);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testSeekPosition, playbackStateRef.get().getPosition());
        assertEquals(testSeekPosition, mControllerCompat.getPlaybackState().getPosition());
    }

    @FlakyTest(bugId = 187338985)
    @Test
    public void currentMediaItemChange() throws Exception {
        int testItemIndex = 3;
        long testPosition = 1234;
        String displayTitle = "displayTitle";
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, displayTitle)
                .putRating(MediaMetadata.METADATA_KEY_USER_RATING, new ThumbRating())
                .build();
        MediaItem currentMediaItem = new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
                .setMetadata(metadata)
                .build();

        List<MediaItem> playlist = MediaTestUtils.createFileMediaItems(5);
        playlist.set(testItemIndex, currentMediaItem);
        mSession.getMockPlayer().setPlaylistWithFakeItem(playlist);

        AtomicReference<MediaMetadataCompat> metadataRef = new AtomicReference<>();
        AtomicReference<PlaybackStateCompat> playbackStateRef = new AtomicReference<>();
        CountDownLatch latchForMetadata = new CountDownLatch(1);
        CountDownLatch latchForPlaybackState = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                metadataRef.set(metadata);
                latchForMetadata.countDown();
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                playbackStateRef.set(state);
                latchForPlaybackState.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setCurrentMediaItem(testItemIndex);
        mSession.getMockPlayer().setCurrentPosition(testPosition);
        mSession.getMockPlayer().notifyCurrentMediaItemChanged(testItemIndex);

        assertTrue(latchForMetadata.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(displayTitle,
                metadataRef.get().getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));
        assertEquals(displayTitle,
                mControllerCompat.getMetadata().getString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));
        if (MediaTestUtils.isServiceToT()) {
            // TODO(b/156594425): Move these assertions out of this condition once the
            //  previous session is updated to have the fix of b/159147455.
            assertTrue(latchForPlaybackState.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(testPosition, playbackStateRef.get().getPosition());
            assertEquals(testPosition, mControllerCompat.getPlaybackState().getPosition());
            assertEquals(MediaUtils.convertToQueueItemId(testItemIndex),
                    playbackStateRef.get().getActiveQueueItemId());
            assertEquals(MediaUtils.convertToQueueItemId(testItemIndex),
                    mControllerCompat.getPlaybackState().getActiveQueueItemId());
            assertEquals(RatingCompat.RATING_THUMB_UP_DOWN, mControllerCompat.getRatingType());
        }
    }

    @Test
    public void playlistAndPlaylistMetadataChange() throws Exception {
        List<MediaItem> playlist = MediaTestUtils.createFileMediaItems(5);
        String playlistTitle = "playlistTitle";
        MediaMetadata playlistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, playlistTitle).build();

        AtomicReference<CharSequence> queueTitleRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onQueueChanged(List<QueueItem> queue) {
                latch.countDown();
            }

            @Override
            public void onQueueTitleChanged(CharSequence title) {
                queueTitleRef.set(title);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setPlaylist(playlist);
        mSession.getMockPlayer().setPlaylistMetadata(playlistMetadata);
        mSession.getMockPlayer().notifyPlaylistChanged();

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        List<QueueItem> queue = mControllerCompat.getQueue();
        assertNotNull(queue);
        assertEquals(playlist.size(), queue.size());
        for (int i = 0; i < playlist.size(); i++) {
            assertEquals(playlist.get(i).getMediaId(), queue.get(i).getDescription().getMediaId());
        }
        assertEquals(playlistTitle, queueTitleRef.get().toString());
    }

    @Test
    public void playlistAndPlaylistMetadataChange_longList() throws Exception {
        String playlistTitle = "playlistTitle";
        MediaMetadata playlistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, playlistTitle).build();

        AtomicReference<CharSequence> queueTitleRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onQueueChanged(List<QueueItem> queue) {
                latch.countDown();
            }

            @Override
            public void onQueueTitleChanged(CharSequence title) {
                queueTitleRef.set(title);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        int listSize = 5000;
        mSession.getMockPlayer().createAndSetFakePlaylist(listSize);
        mSession.getMockPlayer().setPlaylistMetadata(playlistMetadata);
        mSession.getMockPlayer().notifyPlaylistChanged();

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        List<QueueItem> queue = mControllerCompat.getQueue();
        assertNotNull(queue);

        if (Build.VERSION.SDK_INT >= 21) {
            assertEquals(listSize, queue.size());
        } else {
            // Below API 21, only the initial part of the playlist is sent to the
            // MediaControllerCompat when the list is too long.
            assertTrue(queue.size() < listSize);
        }
        for (int i = 0; i < queue.size(); i++) {
            assertEquals(TestUtils.getMediaIdInFakeList(i),
                    queue.get(i).getDescription().getMediaId());
        }
        assertEquals(playlistTitle, queueTitleRef.get().toString());
    }

    @Test
    public void playlistMetadataChange() throws Exception {
        String playlistTitle = "playlistTitle";
        MediaMetadata playlistMetadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, playlistTitle).build();

        AtomicReference<CharSequence> queueTitleRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onQueueTitleChanged(CharSequence title) {
                queueTitleRef.set(title);
                latch.countDown();
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().setPlaylistMetadata(playlistMetadata);
        mSession.getMockPlayer().notifyPlaylistMetadataChanged();

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(playlistTitle, queueTitleRef.get().toString());
    }

    @Test
    public void onAudioInfoChanged_isCalled_byVolumeChange() throws Exception {
        if (!MediaTestUtils.isServiceToT()) {
            // TODO(b/156594425): Remove this condition once the previous session becomes to notify
            //  volume changes of RemoteSessionPlayer (b/155059866).
            return;
        }

        Bundle playerConfig = new RemoteMediaSession.MockPlayerConfigBuilder()
                .setVolumeControlType(RemoteSessionPlayer.VOLUME_CONTROL_ABSOLUTE)
                .setMaxVolume(10)
                .setCurrentVolume(1)
                .build();
        mSession.updatePlayer(playerConfig);

        int targetVolume = 3;
        CountDownLatch targetVolumeNotified = new CountDownLatch(1);
        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                if (info.getCurrentVolume() == targetVolume) {
                    targetVolumeNotified.countDown();
                }
            }
        };
        mControllerCompat.registerCallback(callback, sHandler);

        mSession.getMockPlayer().notifyVolumeChanged(targetVolume);

        assertTrue(targetVolumeNotified.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(targetVolume, mControllerCompat.getPlaybackInfo().getCurrentVolume());
    }
}
