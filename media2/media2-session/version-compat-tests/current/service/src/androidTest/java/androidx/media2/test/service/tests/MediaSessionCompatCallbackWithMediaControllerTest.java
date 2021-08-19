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

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

import static androidx.media2.session.MediaConstants.MEDIA_URI_AUTHORITY;
import static androidx.media2.session.MediaConstants.MEDIA_URI_PATH_SET_MEDIA_URI;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_ID;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_QUERY;
import static androidx.media2.session.MediaConstants.MEDIA_URI_QUERY_URI;
import static androidx.media2.session.MediaConstants.MEDIA_URI_SCHEME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaUtils;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionToken;
import androidx.media2.session.StarRating;
import androidx.media2.test.common.MockActivity;
import androidx.media2.test.common.PollingCheck;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.RemoteMediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaController}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSessionCompatCallbackWithMediaControllerTest extends MediaSessionTestBase {
    private static final String TAG = "MediaControllerTest";

    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final long VOLUME_CHANGE_TIMEOUT_MS = 5000L;

    PendingIntent mIntent;
    MediaSessionCompat mSession;
    MediaSessionCallback mSessionCallback;
    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Intent sessionActivity = new Intent(mContext, MockActivity.class);
        // Create this test specific MediaSession to use our own Handler.
        mIntent = PendingIntent.getActivity(mContext, 0, sessionActivity,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        mSessionCallback = new MediaSessionCallback();
        mSession = new MediaSessionCompat(mContext, TAG + "Compat");
        mSession.setCallback(mSessionCallback, sHandler);
        mSession.setSessionActivity(mIntent);
        mSession.setActive(true);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
    }

    private RemoteMediaController createControllerAndWaitConnection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<SessionToken> sessionToken2 = new AtomicReference<>();
        SessionToken.createSessionToken(mContext, mSession.getSessionToken(),
                (compatToken, sessionToken) -> {
                    assertTrue(sessionToken.isLegacySession());
                    sessionToken2.set(sessionToken);
                    latch.countDown();
                });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        return createRemoteController(sessionToken2.get(), true, null);
    }

    @Test
    public void play() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.play();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(1, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void pause() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.pause();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPauseCalled);
    }

    @Test
    public void prepare() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.prepare();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void seekTo() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        final long seekPosition = 12125L;
        controller.seekTo(seekPosition);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSeekToCalled);
        assertEquals(seekPosition, mSessionCallback.mSeekPosition);
    }

    @Test
    public void setPlaybackSpeed() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        final float testSpeed = 2.0f;
        controller.setPlaybackSpeed(testSpeed);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetPlaybackSpeedCalled);
        assertEquals(testSpeed, mSessionCallback.mSpeed, 0.0f);
    }

    @Test
    public void addPlaylistItem() throws Exception {
        final List<MediaItem> testList = MediaTestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);
        final String testMediaId = "testAddPlaylistItem";

        mSession.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        RemoteMediaController controller = createControllerAndWaitConnection();

        final int testIndex = 1;
        controller.addPlaylistItem(testIndex, testMediaId);
        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnAddQueueItemAtCalled);

        assertEquals(testIndex, mSessionCallback.mQueueIndex);
        assertNotNull(mSessionCallback.mQueueDescriptionForAdd);
        assertEquals(testMediaId, mSessionCallback.mQueueDescriptionForAdd.getMediaId());
    }

    @Test
    public void removePlaylistItem() throws Exception {
        final List<MediaItem> testList = MediaTestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);

        mSession.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        RemoteMediaController controller = createControllerAndWaitConnection();

        final MediaItem itemToRemove = testList.get(1);
        controller.removePlaylistItem(1);
        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnRemoveQueueItemCalled);

        assertNotNull(mSessionCallback.mQueueDescriptionForRemove);
        assertEquals(itemToRemove.getMediaId(),
                mSessionCallback.mQueueDescriptionForRemove.getMediaId());
    }

    @Test
    public void replacePlaylistItem() throws Exception {
        final int testReplaceIndex = 1;
        // replace = remove + add
        final List<MediaItem> testList = MediaTestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);
        final String testMediaId = "testReplacePlaylistItem";

        mSession.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        RemoteMediaController controller = createControllerAndWaitConnection();

        mSessionCallback.reset(2);
        controller.replacePlaylistItem(testReplaceIndex, testMediaId);
        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnRemoveQueueItemCalled);
        assertTrue(mSessionCallback.mOnAddQueueItemAtCalled);

        assertNotNull(mSessionCallback.mQueueDescriptionForRemove);
        assertEquals(testList.get(testReplaceIndex).getMediaId(),
                mSessionCallback.mQueueDescriptionForRemove.getMediaId());

        assertNotNull(mSessionCallback.mQueueDescriptionForAdd);
        assertEquals(testMediaId, mSessionCallback.mQueueDescriptionForAdd.getMediaId());
    }

    @Test
    public void skipToPreviousItem() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.skipToPreviousItem();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToPreviousCalled);
    }

    @Test
    public void skipToNextItem() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.skipToNextItem();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToNextCalled);
    }

    @Test
    public void skipToPlaylistItem() throws Exception {
        final int testSkipToIndex = 1;
        final List<MediaItem> testList = MediaTestUtils.createPlaylist(2);
        final List<QueueItem> testQueue = MediaUtils.convertToQueueItemList(testList);

        mSession.setQueue(testQueue);
        mSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        RemoteMediaController controller = createControllerAndWaitConnection();

        mSessionCallback.reset(1);
        controller.skipToPlaylistItem(testSkipToIndex);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSkipToQueueItemCalled);
        assertEquals(testQueue.get(testSkipToIndex).getQueueId(), mSessionCallback.mQueueItemId);
    }

    @Test
    public void setShuffleMode() throws Exception {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;

        mSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setShuffleMode(testShuffleMode);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetShuffleModeCalled);
        assertEquals(testShuffleMode, mSessionCallback.mShuffleMode);
    }

    @Test
    public void setRepeatMode() throws Exception {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_ALL;

        mSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setRepeatMode(testRepeatMode);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetRepeatModeCalled);
        assertEquals(testRepeatMode, mSessionCallback.mRepeatMode);
    }

    @Test
    public void setVolumeTo() throws Exception {
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        TestVolumeProvider volumeProvider =
                new TestVolumeProvider(volumeControlType, maxVolume, currentVolume);
        mSession.setPlaybackToRemote(volumeProvider);
        RemoteMediaController controller = createControllerAndWaitConnection();

        final int targetVolume = 50;
        controller.setVolumeTo(targetVolume, 0 /* flags */);
        assertTrue(volumeProvider.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(volumeProvider.mSetVolumeToCalled);
        assertEquals(targetVolume, volumeProvider.mVolume);
    }

    @Test
    public void adjustVolume() throws Exception {
        final int maxVolume = 100;
        final int currentVolume = 23;
        final int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
        TestVolumeProvider volumeProvider =
                new TestVolumeProvider(volumeControlType, maxVolume, currentVolume);
        mSession.setPlaybackToRemote(volumeProvider);
        RemoteMediaController controller = createControllerAndWaitConnection();

        final int direction = AudioManager.ADJUST_RAISE;
        controller.adjustVolume(direction, 0 /* flags */);
        assertTrue(volumeProvider.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(volumeProvider.mAdjustVolumeCalled);
        assertEquals(direction, volumeProvider.mDirection);
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
        mSession.setPlaybackToLocal(stream);
        RemoteMediaController controller = createControllerAndWaitConnection();
        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        controller.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI);
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
        mSession.setPlaybackToLocal(stream);
        RemoteMediaController controller = createControllerAndWaitConnection();

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;
        Log.d(TAG, "originalVolume=" + originalVolume + ", targetVolume=" + targetVolume);

        controller.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
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
    public void sendCustomCommand() throws Exception {
        final String command = "test_custom_command";
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "test_args");
        final SessionCommand testCommand = new SessionCommand(command, null);
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.sendCustomCommand(testCommand, testArgs);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnCommandCalled);
        assertEquals(command, mSessionCallback.mCommand);
        assertTrue(TestUtils.equals(testArgs, mSessionCallback.mExtras));
    }

    @Test
    public void fastForward() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.fastForward();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnFastForwardCalled);
    }

    @Test
    public void rewind() throws Exception {
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.rewind();
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertEquals(true, mSessionCallback.mOnRewindCalled);
    }

    @Test
    public void setRating() throws Exception {
        final float ratingValue = 3.5f;
        final Rating rating2 = new StarRating(5, ratingValue);
        final String mediaId = "media_id";
        final MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId).build();
        mSession.setMetadata(metadata);
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setRating(mediaId, rating2);
        assertTrue(mSessionCallback.await(TIME_OUT_MS));
        assertTrue(mSessionCallback.mOnSetRatingCalled);
        assertEquals(rating2, MediaUtils.convertToRating(mSessionCallback.mRating));
    }

    @Test
    public void setMediaUri_ignored() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(Uri.parse("androidx://test?test=xx"), /* extras= */ null);

        assertFalse(mSessionCallback.await(TIMEOUT_MS));
    }

    @Test
    public void setMediaUri_followedByPrepare_callsPrepareFromMediaId() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        String testMediaId = "anyMediaId";
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(MEDIA_URI_QUERY_ID, testMediaId).build(),
                testExtras);
        controller.prepare();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(testMediaId, mSessionCallback.mMediaId);
        assertTrue(TestUtils.equals(testExtras, mSessionCallback.mExtras));
        assertNull(mSessionCallback.mQuery);
        assertNull(mSessionCallback.mUri);
        assertFalse(mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void setMediaUri_followedByPrepare_callsPrepareFromSearch() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        String testSearchQuery = "anyQuery";
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(MEDIA_URI_QUERY_QUERY, testSearchQuery).build(),
                testExtras);
        controller.prepare();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromSearchCalled);
        assertEquals(testSearchQuery, mSessionCallback.mQuery);
        assertTrue(TestUtils.equals(testExtras, mSessionCallback.mExtras));
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mUri);
        assertFalse(mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void setMediaUri_followedByPrepare_callsPrepareFromUri() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        Uri testMediaUri = Uri.parse("androidx://jetpack/test?query=android%20media");
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(
                                MEDIA_URI_QUERY_URI, testMediaUri.toString()).build(),
                testExtras);
        controller.prepare();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromUriCalled);
        assertEquals(testMediaUri, mSessionCallback.mUri);
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mQuery);
        assertFalse(mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void setMediaUri_withoutFormattingFollowedByPrepare_callsPrepareFromUri()
            throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        Uri testMediaUri = Uri.parse("androidx://jetpack/test?query=android%20media");
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(testMediaUri, testExtras);
        controller.prepare();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromUriCalled);
        assertEquals(testMediaUri, mSessionCallback.mUri);
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mQuery);
        assertFalse(mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void setMediaUri_followedByPlay_callsPlayFromMediaId() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        String testMediaId = "anyMediaId";
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(MEDIA_URI_QUERY_ID, testMediaId).build(),
                testExtras);
        controller.play();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPlayFromMediaIdCalled);
        assertEquals(testMediaId, mSessionCallback.mMediaId);
        assertTrue(TestUtils.equals(testExtras, mSessionCallback.mExtras));
        assertNull(mSessionCallback.mQuery);
        assertNull(mSessionCallback.mUri);
        assertEquals(0, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void setMediaUri_followedByPlay_callsPlayFromSearch() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        String testSearchQuery = "anyQuery";
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(MEDIA_URI_QUERY_QUERY, testSearchQuery).build(),
                testExtras);
        controller.play();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPlayFromSearchCalled);
        assertEquals(testSearchQuery, mSessionCallback.mQuery);
        assertTrue(TestUtils.equals(testExtras, mSessionCallback.mExtras));
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mUri);
        assertEquals(0, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void setMediaUri_followedByPlay_callsPlayFromUri() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        Uri testMediaUri = Uri.parse("androidx://jetpack/test?query=android%20media");
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(
                new Uri.Builder()
                        .scheme(MEDIA_URI_SCHEME)
                        .authority(MEDIA_URI_AUTHORITY)
                        .path(MEDIA_URI_PATH_SET_MEDIA_URI)
                        .appendQueryParameter(
                                MEDIA_URI_QUERY_URI, testMediaUri.toString()).build(),
                testExtras);
        controller.play();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPlayFromUriCalled);
        assertEquals(testMediaUri, mSessionCallback.mUri);
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mQuery);
        assertEquals(0, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void setMediaUri_withoutFormattingFollowedByPlay_callsPlayFromUri()
            throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        Uri testMediaUri = Uri.parse("androidx://jetpack/test?query=android%20media");
        Bundle testExtras = new Bundle();
        testExtras.putString("testKey", "testValue");

        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(1);

        controller.setMediaUri(testMediaUri, testExtras);
        controller.play();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPlayFromUriCalled);
        assertEquals(testMediaUri, mSessionCallback.mUri);
        assertNull(mSessionCallback.mMediaId);
        assertNull(mSessionCallback.mQuery);
        assertEquals(0, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void setMediaUri_followedByPrepareTwice_callsPrepareFromUriAndPrepare()
            throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(2);

        controller.setMediaUri(Uri.parse("androidx://test"), null);

        controller.prepare();
        controller.prepare();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromUriCalled);
        assertTrue(mSessionCallback.mOnPrepareCalled);
    }

    @Test
    public void setMediaUri_followedByPlayTwice_callsPlayFromUriAndPlay() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(2);

        controller.setMediaUri(Uri.parse("androidx://test"), /* extras= */ null);

        controller.play();
        controller.play();

        assertTrue(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPlayFromUriCalled);
        assertEquals(1, mSessionCallback.mOnPlayCalledCount);
    }

    @Test
    public void setMediaUri_multipleCalls_skipped() throws Exception {
        if (!MediaTestUtils.isClientToT()) {
            return;
        }
        RemoteMediaController controller = createControllerAndWaitConnection();
        mSessionCallback.reset(2);

        Uri testUri1 = Uri.parse("androidx://test1");
        Uri testUri2 = Uri.parse("androidx://test2");
        controller.setMediaUri(testUri1, /* extras= */ null);
        controller.setMediaUri(testUri2, /* extras= */ null);
        controller.prepare();

        assertFalse(mSessionCallback.await(TIMEOUT_MS));
        assertTrue(mSessionCallback.mOnPrepareFromUriCalled);
        assertEquals(testUri2, mSessionCallback.mUri);
    }

    private void setPlaybackState(int state) {
        final long allActions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_FAST_FORWARD | PlaybackStateCompat.ACTION_REWIND;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder().setActions(allActions)
                .setState(state, 0L, 0.0f).build();
        mSession.setPlaybackState(playbackState);
    }

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

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private CountDownLatch mLatch = new CountDownLatch(1);
        private long mSeekPosition;
        private float mSpeed;
        private long mQueueItemId;
        private RatingCompat mRating;
        private String mMediaId;
        private String mQuery;
        private Uri mUri;
        private String mAction;
        private String mCommand;
        private Bundle mExtras;
        private ResultReceiver mCommandCallback;
        private boolean mCaptioningEnabled;
        private int mRepeatMode;
        private int mShuffleMode;
        private int mQueueIndex;
        private MediaDescriptionCompat mQueueDescriptionForAdd;
        private MediaDescriptionCompat mQueueDescriptionForRemove;

        private int mOnPlayCalledCount;
        private boolean mOnPauseCalled;
        private boolean mOnStopCalled;
        private boolean mOnFastForwardCalled;
        private boolean mOnRewindCalled;
        private boolean mOnSkipToPreviousCalled;
        private boolean mOnSkipToNextCalled;
        private boolean mOnSeekToCalled;
        private boolean mOnSetPlaybackSpeedCalled;
        private boolean mOnSkipToQueueItemCalled;
        private boolean mOnSetRatingCalled;
        private boolean mOnPlayFromMediaIdCalled;
        private boolean mOnPlayFromSearchCalled;
        private boolean mOnPlayFromUriCalled;
        private boolean mOnCustomActionCalled;
        private boolean mOnCommandCalled;
        private boolean mOnPrepareCalled;
        private boolean mOnPrepareFromMediaIdCalled;
        private boolean mOnPrepareFromSearchCalled;
        private boolean mOnPrepareFromUriCalled;
        private boolean mOnSetCaptioningEnabledCalled;
        private boolean mOnSetRepeatModeCalled;
        private boolean mOnSetShuffleModeCalled;
        private boolean mOnAddQueueItemCalled;
        private boolean mOnAddQueueItemAtCalled;
        private boolean mOnRemoveQueueItemCalled;

        public void reset(int count) {
            mLatch = new CountDownLatch(count);
            mSeekPosition = -1;
            mSpeed = -1.0f;
            mQueueItemId = -1;
            mRating = null;
            mMediaId = null;
            mQuery = null;
            mUri = null;
            mAction = null;
            mExtras = null;
            mCommand = null;
            mCommandCallback = null;
            mCaptioningEnabled = false;
            mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE;
            mQueueIndex = -1;
            mQueueDescriptionForAdd = null;
            mQueueDescriptionForRemove = null;

            mOnPlayCalledCount = 0;
            mOnPauseCalled = false;
            mOnStopCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSkipToPreviousCalled = false;
            mOnSkipToNextCalled = false;
            mOnSkipToQueueItemCalled = false;
            mOnSeekToCalled = false;
            mOnSetPlaybackSpeedCalled = false;
            mOnSetRatingCalled = false;
            mOnPlayFromMediaIdCalled = false;
            mOnPlayFromSearchCalled = false;
            mOnPlayFromUriCalled = false;
            mOnCustomActionCalled = false;
            mOnCommandCalled = false;
            mOnPrepareCalled = false;
            mOnPrepareFromMediaIdCalled = false;
            mOnPrepareFromSearchCalled = false;
            mOnPrepareFromUriCalled = false;
            mOnSetCaptioningEnabledCalled = false;
            mOnSetRepeatModeCalled = false;
            mOnSetShuffleModeCalled = false;
            mOnAddQueueItemCalled = false;
            mOnAddQueueItemAtCalled = false;
            mOnRemoveQueueItemCalled = false;
        }

        public boolean await(long timeoutMs) {
            try {
                return mLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public void onPlay() {
            mOnPlayCalledCount++;
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mLatch.countDown();
        }

        @Override
        public void onPause() {
            mOnPauseCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            mLatch.countDown();
        }

        @Override
        public void onStop() {
            mOnStopCalled = true;
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            mLatch.countDown();
        }

        @Override
        public void onFastForward() {
            mOnFastForwardCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onRewind() {
            mOnRewindCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToPrevious() {
            mOnSkipToPreviousCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSkipToNext() {
            mOnSkipToNextCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onSeekTo(long pos) {
            mOnSeekToCalled = true;
            mSeekPosition = pos;
            mLatch.countDown();
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            mOnSetPlaybackSpeedCalled = true;
            mSpeed = speed;
            mLatch.countDown();
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            mOnSetRatingCalled = true;
            mRating = rating;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mOnPlayFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            mOnPlayFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            mOnPlayFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            mOnCustomActionCalled = true;
            mAction = action;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mOnSkipToQueueItemCalled = true;
            mQueueItemId = id;
            mLatch.countDown();
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            mOnCommandCalled = true;
            mCommand = command;
            mExtras = extras;
            mCommandCallback = cb;
            mLatch.countDown();
        }

        @Override
        public void onPrepare() {
            mOnPrepareCalled = true;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            mOnPrepareFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            mOnPrepareFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            mOnPrepareFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mLatch.countDown();
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            mOnSetRepeatModeCalled = true;
            mRepeatMode = repeatMode;
            mSession.setRepeatMode(repeatMode);
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            mOnAddQueueItemCalled = true;
            mQueueDescriptionForAdd = description;
            mLatch.countDown();
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            mOnAddQueueItemAtCalled = true;
            mQueueIndex = index;
            mQueueDescriptionForAdd = description;
            mLatch.countDown();
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            mOnRemoveQueueItemCalled = true;
            mQueueDescriptionForRemove = description;
            mLatch.countDown();
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            mOnSetCaptioningEnabledCalled = true;
            mCaptioningEnabled = enabled;
            mLatch.countDown();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            mOnSetShuffleModeCalled = true;
            mShuffleMode = shuffleMode;
            mSession.setShuffleMode(shuffleMode);
            mLatch.countDown();
        }
    }
}
