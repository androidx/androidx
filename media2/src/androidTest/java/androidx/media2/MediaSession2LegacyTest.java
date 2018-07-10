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

import static androidx.media2.MediaItem2.FLAG_PLAYABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionCallback;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession2LegacyTest extends MediaSession2TestBase {
    private static final String TAG = "MediaSession2LegacyTest";

    private static final long WAIT_TIME_MS = 1000L;

    private MediaSession2 mSession;
    private MockPlayer mPlayer;
    private MockPlaylistAgent mMockAgent;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(0);
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
                }).build();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.close();
    }

    @Test
    public void testRepeatModeChange() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public int getRepeatMode() {
                return testRepeatMode;
            }
        };

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mSession.updatePlayer(mPlayer, agent);
        agent.notifyRepeatModeChanged();
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnRepeatModeChangedCalled);
        assertEquals(testRepeatMode, controller.getRepeatMode());
    }

    @Test
    public void testShuffleModeChange() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        final MediaPlaylistAgent agent = new MockPlaylistAgent() {
            @Override
            public int getShuffleMode() {
                return testShuffleMode;
            }
        };

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mSession.updatePlayer(mPlayer, agent);
        agent.notifyShuffleModeChanged();
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnShuffleModeChangedCalled);
        assertEquals(testShuffleMode, controller.getShuffleMode());
    }

    @Test
    public void testClose() throws InterruptedException {
        prepareLooper();
        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mSession.close();
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnSessionDestroyedCalled);
    }

    @Test
    public void testUpdatePlayer() throws InterruptedException {
        prepareLooper();
        final int testState = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        final int testBufferingPosition = 1500;
        final float testSpeed = 1.5f;
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(3);
        final String testPlaylistTitle = "testPlaylistTitle";
        final MediaMetadata2 testPlaylistMetadata = new MediaMetadata2.Builder()
                .putText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, testPlaylistTitle).build();

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        // TODO: Make each callback method use their own CountDownLatch.
        if (Build.VERSION.SDK_INT < 21) {
            controllerCallback.reset(7);
        } else {
            // On API 21+, MediaControllerCompat.Callback.onAudioInfoChanged() is called
            // only when the playback type is changed. Since this test method does not change
            // the playback type (local -> local), onAudioInfoChanged will not be called.
            controllerCallback.reset(6);
        }
        controller.registerCallback(controllerCallback, sHandler);

        MockPlayer player = new MockPlayer(0);
        player.mLastPlayerState = testState;
        player.mBufferedPosition = testBufferingPosition;
        player.mPlaybackSpeed = testSpeed;

        MockPlaylistAgent agent = new MockPlaylistAgent();
        agent.mPlaylist = testPlaylist;
        agent.mCurrentMediaItem = testPlaylist.get(0);
        agent.mMetadata = testPlaylistMetadata;
        mSession.updatePlayer(player, agent);

        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(testState,
                MediaUtils2.convertToPlayerState(controllerCallback.mPlaybackState.getState()));
        assertEquals(testBufferingPosition,
                controllerCallback.mPlaybackState.getBufferedPosition());
        assertEquals(testSpeed, controllerCallback.mPlaybackState.getPlaybackSpeed(), 0.0f);

        assertTrue(controllerCallback.mOnMetadataChangedCalled);
        assertTrue(controllerCallback.mOnQueueChangedCalled);
        assertTrue(controllerCallback.mOnQueueTitleChangedCalled);
        List<QueueItem> queue = controller.getQueue();
        assertNotNull(queue);
        assertEquals(testPlaylist.size(), queue.size());
        for (int i = 0; i < testPlaylist.size(); i++) {
            assertEquals(testPlaylist.get(i).getMediaId(),
                    queue.get(i).getDescription().getMediaId());
        }
        assertEquals(testPlaylistTitle, controllerCallback.mTitle);
    }

    @Test
    public void testUpdatePlayer_playbackTypeChangedToRemote() throws InterruptedException {
        prepareLooper();
        final int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        final float maxVolume = 25;
        final float currentVolume = 10;

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {
                    @Override
                    public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                                info.getPlaybackType());
                        assertEquals(controlType, info.getVolumeControl());
                        assertEquals(maxVolume, info.getMaxVolume(), 0.0f);
                        latch.countDown();
                    }
                };
        controller.registerCallback(controllerCallback, sHandler);

        MockRemotePlayer remotePlayer = new MockRemotePlayer(controlType, maxVolume, currentVolume);
        mSession.updatePlayer(remotePlayer, null);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                info.getPlaybackType());
        assertEquals(controlType, info.getVolumeControl());
        assertEquals(maxVolume, info.getMaxVolume(), 0.0f);
        assertEquals(currentVolume, info.getCurrentVolume(), 0.0f);
    }

    @Test
    public void testUpdatePlayer_playbackTypeChangedToLocal() throws InterruptedException {
        prepareLooper();
        mSession.updatePlayer(
                new MockRemotePlayer(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 10, 1), null);

        final int legacyStream = AudioManager.STREAM_RING;
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(legacyStream).build();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {
                    @Override
                    public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                        if (MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                                == info.getPlaybackType()
                                && legacyStream == info.getAudioStream()) {
                            latch.countDown();
                        }
                    }
                };
        controller.registerCallback(controllerCallback, sHandler);

        MockPlayer player = new MockPlayer(0);
        player.setAudioAttributes(attrs);
        mSession.updatePlayer(player, null);

        // In API 21 and 22, onAudioInfoChanged is not called when playback is changed to local.
        if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
            Thread.sleep(WAIT_TIME_MS);
        } else {
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                info.getPlaybackType());
        assertEquals(legacyStream, info.getAudioStream());
    }

    @Test
    public void testUpdatePlayer_playbackTypeNotChanged_local() throws InterruptedException {
        final int legacyStream = AudioManager.STREAM_RING;
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(legacyStream).build();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {
                    @Override
                    public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                                info.getPlaybackType());
                        assertEquals(legacyStream, info.getAudioStream());
                        latch.countDown();
                    }
                };
        controller.registerCallback(controllerCallback, sHandler);

        MockPlayer player = new MockPlayer(0);
        player.setAudioAttributes(attrs);
        mSession.updatePlayer(player, null);

        // In API 21+, onAudioInfoChanged() is not called when playbackType is not changed.
        if (Build.VERSION.SDK_INT >= 21) {
            Thread.sleep(WAIT_TIME_MS);
        } else {
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                info.getPlaybackType());
        assertEquals(legacyStream, info.getAudioStream());
    }

    @Test
    public void testUpdatePlayer_playbackTypeNotChanged_remote() throws InterruptedException {
        mSession.updatePlayer(
                new MockRemotePlayer(VolumeProviderCompat.VOLUME_CONTROL_FIXED, 10, 1), null);

        final int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        final float maxVolume = 25;
        final float currentVolume = 10;

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {
                    @Override
                    public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
                        if (MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE
                                == info.getPlaybackType()
                                && controlType == info.getVolumeControl()
                                && maxVolume == info.getMaxVolume()
                                && currentVolume == info.getCurrentVolume()) {
                            latch.countDown();
                        }
                    }
                };
        controller.registerCallback(controllerCallback, sHandler);

        MockRemotePlayer remotePlayer = new MockRemotePlayer(controlType, maxVolume, currentVolume);
        mSession.updatePlayer(remotePlayer, null);

        // In API 21+, onAudioInfoChanged() is not called when playbackType is not changed.
        if (Build.VERSION.SDK_INT >= 21) {
            Thread.sleep(WAIT_TIME_MS);
        } else {
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }

        MediaControllerCompat.PlaybackInfo info = controller.getPlaybackInfo();
        assertEquals(MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                info.getPlaybackType());
        assertEquals(controlType, info.getVolumeControl());
        assertEquals(maxVolume, info.getMaxVolume(), 0.0f);
        assertEquals(currentVolume, info.getCurrentVolume(), 0.0f);
    }

    @Test
    public void testPlayerStateChange() throws Exception {
        prepareLooper();
        final int targetState = BaseMediaPlayer.PLAYER_STATE_PLAYING;

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mPlayer.notifyPlaybackState(targetState);
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnSessionReadyCalled);
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(targetState,
                MediaUtils2.convertToPlayerState(controllerCallback.mPlaybackState.getState()));
    }

    @Test
    public void testPlaybackSpeedChange() throws Exception {
        prepareLooper();
        final float speed = 1.5f;

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mPlayer.setPlaybackSpeed(speed);
        mPlayer.notifyPlaybackSpeedChanged(speed);
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(speed, controllerCallback.mPlaybackState.getPlaybackSpeed(), 0.0f);
    }

    @Test
    public void testBufferingStateChange() throws Exception {
        prepareLooper();
        final List<MediaItem2> testPlaylist = TestUtils.createPlaylist(3);
        final MediaItem2 testItem = testPlaylist.get(0);
        final int testBufferingState = BaseMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        final long testBufferingPosition = 500;
        mSession.setPlaylist(testPlaylist, null);

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mPlayer.mBufferedPosition = testBufferingPosition;
        mPlayer.notifyBufferingStateChanged(testItem.getDataSourceDesc(), testBufferingState);
        assertTrue(controllerCallback.await(WAIT_TIME_MS));
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(testBufferingPosition,
                controllerCallback.mPlaybackState.getBufferedPosition(), 0.0f);
    }

    @Test
    public void testSeekComplete() throws InterruptedException {
        prepareLooper();
        final long testSeekPosition = 1300;

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mPlayer.mCurrentPosition = testSeekPosition;
        mPlayer.mLastPlayerState = BaseMediaPlayer.PLAYER_STATE_PAUSED;
        mPlayer.notifySeekCompleted(testSeekPosition);
        assertTrue(controllerCallback.await(TIMEOUT_MS));
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(testSeekPosition, controllerCallback.mPlaybackState.getPosition());
    }

    @Test
    public void testNotifyError() throws InterruptedException {
        prepareLooper();
        final int errorCode = MediaSession2.ERROR_CODE_NOT_AVAILABLE_IN_REGION;
        final Bundle extras = new Bundle();
        extras.putString("args", "testNotifyError");

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mSession.notifyError(errorCode, extras);
        assertTrue(controllerCallback.await(TIMEOUT_MS));
        assertTrue(controllerCallback.mOnPlaybackStateChangedCalled);
        assertEquals(errorCode, controllerCallback.mPlaybackState.getErrorCode());
        assertTrue(TestUtils.equals(extras, controllerCallback.mPlaybackState.getExtras()));
    }

    @Test
    public void testCurrentMediaItemChange() throws InterruptedException {
        prepareLooper();

        String displayTitle = "displayTitle";
        MediaMetadata2 metadata = new MediaMetadata2.Builder()
                .putText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, displayTitle).build();
        MediaItem2 currentMediaItem = new MediaItem2.Builder(FLAG_PLAYABLE)
                .setMetadata(metadata).setDataSourceDesc(TestUtils.createDSD()).build();

        List<MediaItem2> playlist = TestUtils.createPlaylist(5);
        playlist.set(3, currentMediaItem);
        mMockAgent.mPlaylist = playlist;

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mMockAgent.mCurrentMediaItem = currentMediaItem;
        mPlayer.notifyCurrentDataSourceChanged(currentMediaItem.getDataSourceDesc());

        assertTrue(controllerCallback.await(TIMEOUT_MS));
        assertTrue(controllerCallback.mOnMetadataChangedCalled);
        assertEquals(displayTitle, controllerCallback.mMediaMetadata
                .getString(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE));
    }

    @Test
    public void testPlaylistAndPlaylistMetadataChange() throws InterruptedException {
        prepareLooper();
        final List<MediaItem2> playlist = TestUtils.createPlaylist(5);
        final String playlistTitle = "playlistTitle";
        MediaMetadata2 playlistMetadata = new MediaMetadata2.Builder()
                .putText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, playlistTitle).build();

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(2);
        controller.registerCallback(controllerCallback, sHandler);

        mMockAgent.mPlaylist = playlist;
        mMockAgent.mMetadata = playlistMetadata;
        mMockAgent.notifyPlaylistChanged();

        assertTrue(controllerCallback.await(TIMEOUT_MS));
        assertTrue(controllerCallback.mOnQueueChangedCalled);
        assertTrue(controllerCallback.mOnQueueTitleChangedCalled);

        List<QueueItem> queue = controller.getQueue();
        assertNotNull(queue);
        assertEquals(playlist.size(), queue.size());
        for (int i = 0; i < playlist.size(); i++) {
            assertEquals(playlist.get(i).getMediaId(), queue.get(i).getDescription().getMediaId());
        }
        assertEquals(playlistTitle, controllerCallback.mTitle);
    }

    @Test
    public void testPlaylistMetadataChange() throws InterruptedException {
        prepareLooper();
        final String playlistTitle = "playlistTitle";
        MediaMetadata2 playlistMetadata = new MediaMetadata2.Builder()
                .putText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, playlistTitle).build();

        final MediaControllerCompat controller = mSession.getSessionCompat().getController();
        final MediaControllerCallback controllerCallback = new MediaControllerCallback();
        controllerCallback.reset(1);
        controller.registerCallback(controllerCallback, sHandler);

        mMockAgent.mMetadata = playlistMetadata;
        mMockAgent.notifyPlaylistMetadataChanged();

        assertTrue(controllerCallback.await(TIMEOUT_MS));
        assertTrue(controllerCallback.mOnQueueTitleChangedCalled);
        assertEquals(playlistTitle, controllerCallback.mTitle);
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        private CountDownLatch mLatch;

        private boolean mOnPlaybackStateChangedCalled;
        private boolean mOnMetadataChangedCalled;
        private boolean mOnQueueChangedCalled;
        private boolean mOnQueueTitleChangedCalled;
        private boolean mOnExtraChangedCalled;
        private boolean mOnAudioInfoChangedCalled;
        private boolean mOnSessionDestroyedCalled;
        private boolean mOnSessionEventCalled;
        private boolean mOnCaptioningEnabledChangedCalled;
        private boolean mOnRepeatModeChangedCalled;
        private boolean mOnShuffleModeChangedCalled;
        private boolean mOnSessionReadyCalled;

        private PlaybackStateCompat mPlaybackState;
        private MediaMetadataCompat mMediaMetadata;
        private List<MediaSessionCompat.QueueItem> mQueue;
        private CharSequence mTitle;
        private String mEvent;
        private Bundle mExtras;
        private MediaControllerCompat.PlaybackInfo mPlaybackInfo;
        private boolean mCaptioningEnabled;
        private int mRepeatMode;
        private int mShuffleMode;

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
            mOnPlaybackStateChangedCalled = false;
            mOnMetadataChangedCalled = false;
            mOnQueueChangedCalled = false;
            mOnQueueTitleChangedCalled = false;
            mOnExtraChangedCalled = false;
            mOnAudioInfoChangedCalled = false;
            mOnSessionDestroyedCalled = false;
            mOnSessionEventCalled = false;
            mOnRepeatModeChangedCalled = false;
            mOnShuffleModeChangedCalled = false;

            mPlaybackState = null;
            mMediaMetadata = null;
            mQueue = null;
            mTitle = null;
            mExtras = null;
            mPlaybackInfo = null;
            mCaptioningEnabled = false;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;
        }

        public boolean await(long timeoutMs) {
            try {
                return mLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mOnPlaybackStateChangedCalled = true;
            mPlaybackState = state;
            mLatch.countDown();
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mOnMetadataChangedCalled = true;
            mMediaMetadata = metadata;
            mLatch.countDown();
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            mOnQueueChangedCalled = true;
            mQueue = queue;
            mLatch.countDown();
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            mOnQueueTitleChangedCalled = true;
            mTitle = title;
            mLatch.countDown();
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            mOnExtraChangedCalled = true;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
            mOnAudioInfoChangedCalled = true;
            mPlaybackInfo = info;
            mLatch.countDown();
        }

        @Override
        public void onSessionDestroyed() {
            mOnSessionDestroyedCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            mOnSessionEventCalled = true;
            mEvent = event;
            mExtras = (Bundle) extras.clone();
            mLatch.countDown();
        }

        @Override
        public void onCaptioningEnabledChanged(boolean enabled) {
            mOnCaptioningEnabledChangedCalled = true;
            mCaptioningEnabled = enabled;
            mLatch.countDown();
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            mOnRepeatModeChangedCalled = true;
            mRepeatMode = repeatMode;
            mLatch.countDown();
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            mOnShuffleModeChangedCalled = true;
            mShuffleMode = shuffleMode;
            mLatch.countDown();
        }

        @Override
        public void onSessionReady() {
            mOnSessionReadyCalled = true;
        }
    }
}
