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

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED;
import static androidx.media2.test.common.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media2.test.common.CommonConstants.SERVICE_PACKAGE_NAME;
import static androidx.media2.test.common.MediaSessionConstants.TEST_GET_SESSION_ACTIVITY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaController.ControllerCallbackRunnable;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.SessionToken;
import androidx.media2.test.client.MediaTestUtils;
import androidx.media2.test.client.RemoteMediaSession;
import androidx.media2.test.common.PollingCheck;
import androidx.media2.test.common.TestUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaController}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControllerTest extends MediaSessionTestBase {

    static final String TAG = "MediaControllerTest";
    private static final long VOLUME_CHANGE_TIMEOUT_MS = 5000L;

    final List<RemoteMediaSession> mRemoteSessionList = new ArrayList<>();

    AudioManager mAudioManager;
    RemoteMediaSession mRemoteSession;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRemoteSession = createRemoteMediaSession(DEFAULT_TEST_NAME, null);
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
    public void testBuilder() {
        prepareLooper();
        MediaController.Builder builder;

        try {
            builder = new MediaController.Builder(null);
            fail("null context shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }

        try {
            builder = new MediaController.Builder(mContext);
            builder.setSessionToken(null);
            fail("null token shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }

        try {
            builder = new MediaController.Builder(mContext);
            builder.setSessionCompatToken(null);
            fail("null compat token shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }

        try {
            builder = new MediaController.Builder(mContext);
            builder.setControllerCallback(null, null);
            fail("null executor or null callback shouldn't be allowed");
        } catch (NullPointerException e) {
            // expected. pass-through
        }

        MediaController controller = new MediaController.Builder(mContext)
                .setSessionToken(mRemoteSession.getToken())
                .setControllerCallback(sHandlerExecutor, new ControllerCallback() {})
                .build();
        controller.close();
    }

    @Test
    public void testGetSessionActivity() throws InterruptedException {
        prepareLooper();
        RemoteMediaSession session = createRemoteMediaSession(TEST_GET_SESSION_ACTIVITY, null);

        MediaController controller = createController(session.getToken());
        PendingIntent sessionActivity = controller.getSessionActivity();
        assertNotNull(sessionActivity);
        if (Build.VERSION.SDK_INT >= 17) {
            // PendingIntent#getCreatorPackage() is added in API 17.
            assertEquals(SERVICE_PACKAGE_NAME, sessionActivity.getCreatorPackage());

            // TODO: Add getPid/getUid in MediaControllerProviderService and compare them.
            // assertEquals(mRemoteSession.getUid(), sessionActivity.getCreatorUid());
        }
        session.cleanUp();
    }

    @Test
    public void testSetVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        MediaController controller = createController(mRemoteSession.getToken());

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

        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream).build();
        Bundle playerConfig = RemoteMediaSession.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession.updatePlayer(playerConfig);

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
    public void testAdjustVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        MediaController controller = createController(mRemoteSession.getToken());

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

        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream).build();
        Bundle playerConfig = RemoteMediaSession.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession.updatePlayer(playerConfig);

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
    public void testGetPackageName() throws Exception {
        prepareLooper();
        MediaController controller = createController(mRemoteSession.getToken());
        assertEquals(SERVICE_PACKAGE_NAME, controller.getConnectedToken().getPackageName());
    }

    @Test
    public void testGetTokenExtras() throws Exception {
        prepareLooper();
        Bundle testTokenExtras = TestUtils.createTestBundle();
        RemoteMediaSession session = createRemoteMediaSession("testGetExtras", testTokenExtras);

        MediaController controller = createController(session.getToken());
        SessionToken connectedToken = controller.getConnectedToken();
        assertNotNull(connectedToken);
        assertTrue(TestUtils.equals(testTokenExtras, connectedToken.getExtras()));
    }

    @Test
    public void testIsConnected() throws InterruptedException {
        prepareLooper();
        MediaController controller = createController(mRemoteSession.getToken());
        assertTrue(controller.isConnected());

        mRemoteSession.close();
        waitForDisconnect(controller, true);
        assertFalse(controller.isConnected());
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController controller = createController(mRemoteSession.getToken(),
                false /* waitForConnect */, null, null /* callback */);
        controller.close();
    }

    @Test
    public void testClose_twice() throws InterruptedException {
        prepareLooper();
        MediaController controller = createController(mRemoteSession.getToken());
        controller.close();
        controller.close();
    }

    @Test
    public void testGettersAfterConnected() throws InterruptedException {
        prepareLooper();
        final int state = SessionPlayer.PLAYER_STATE_PLAYING;
        final int bufferingState = SessionPlayer.BUFFERING_STATE_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem currentMediaItem = MediaTestUtils.createFileMediaItemWithMetadata();

        Bundle config = RemoteMediaSession.createMockPlayerConnectorConfig(
                state, bufferingState, position, bufferedPosition, speed, null /* audioAttrs */,
                null /* playlist */, currentMediaItem, null /* metadata */);
        mRemoteSession.updatePlayer(config);

        MediaController controller = createController(mRemoteSession.getToken());
        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        MediaTestUtils.assertNotMediaItemSubclass(controller.getCurrentMediaItem());
        MediaTestUtils.assertMediaIdEquals(currentMediaItem, controller.getCurrentMediaItem());
    }

    @Test
    public void testGetPlaybackInfo() throws Exception {
        prepareLooper();
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();

        Bundle playerConfig = RemoteMediaSession.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession.updatePlayer(playerConfig);

        final MediaController controller = createController(mRemoteSession.getToken());
        PlaybackInfo info = controller.getPlaybackInfo();
        assertNotNull(info);
        assertEquals(PlaybackInfo.PLAYBACK_TYPE_LOCAL, info.getPlaybackType());
        assertEquals(attrs, info.getAudioAttributes());

        int localVolumeControlType = VOLUME_CONTROL_ABSOLUTE;
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            localVolumeControlType = VOLUME_CONTROL_FIXED;
        }
        assertEquals(localVolumeControlType, info.getControlType());
        assertEquals(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                info.getMaxVolume());
        assertEquals(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                info.getCurrentVolume());
    }

    @Test
    public void testGetVideoSize() throws InterruptedException {
        prepareLooper();

        VideoSize testSize = new VideoSize(100, 42);
        Bundle playerConfig = RemoteMediaSession.createMockPlayerConnectorConfigForVideoSize(
                testSize);
        mRemoteSession.updatePlayer(playerConfig);
        MediaController controller = createController(mRemoteSession.getToken());
        assertEquals(testSize, controller.getVideoSize());
    }

    @Test
    public void testGetTrackInfo() throws Exception {
        prepareLooper();

        final List<SessionPlayer.TrackInfo> testTracks = MediaTestUtils.createTrackInfoList();
        Bundle playerConfig =
                RemoteMediaSession.createMockPlayerConnectorConfigForTrackInfo(testTracks);
        mRemoteSession.updatePlayer(playerConfig);

        MediaController controller = createController(mRemoteSession.getToken());
        List<SessionPlayer.TrackInfo> testTracksFromController = controller.getTrackInfo();
        assertEquals(testTracks.size(), testTracksFromController.size());
        for (int i = 0; i < testTracks.size(); i++) {
            assertEquals(testTracks.get(i), testTracksFromController.get(i));
        }
    }

    @Test
    public void testSelectDeselectTrackAndGetSelectedTrack() throws Exception {
        prepareLooper();
        final CountDownLatch selectTrackLatch = new CountDownLatch(1);
        final CountDownLatch deselectTrackLatch = new CountDownLatch(1);

        final List<TrackInfo> testTracks = MediaTestUtils.createTrackInfoList();
        final TrackInfo testTrack = testTracks.get(2);
        int testTrackType = testTrack.getTrackType();
        Bundle playerConfig =
                RemoteMediaSession.createMockPlayerConnectorConfigForTrackInfo(testTracks);
        mRemoteSession.updatePlayer(playerConfig);
        MediaController controller = createController(mRemoteSession.getToken(), true, null,
                new MediaController.ControllerCallback() {
                    @Override
                    public void onTrackSelected(MediaController controller,
                            SessionPlayer.TrackInfo trackInfo) {
                        assertEquals(testTrack, trackInfo);
                        selectTrackLatch.countDown();
                    }

                    @Override
                    public void onTrackDeselected(MediaController controller,
                            SessionPlayer.TrackInfo trackInfo) {
                        assertEquals(testTrack, trackInfo);
                        deselectTrackLatch.countDown();
                    }
                });
        assertNull(controller.getSelectedTrack(testTrackType));

        controller.selectTrack(testTrack);
        assertTrue(selectTrackLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(testTrack, controller.getSelectedTrack(testTrackType));

        controller.deselectTrack(testTrack);
        assertTrue(deselectTrackLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(controller.getSelectedTrack(testTrackType));
    }

    /**
     * It tests {@link MediaController#registerExtraCallback(Executor, ControllerCallback)} and
     * {@link MediaController#unregisterExtraCallback(ControllerCallback)}.
     */
    @Test
    public void testRegisterExtraCallback() throws InterruptedException {
        prepareLooper();

        MediaController controller = createController(mRemoteSession.getToken(),
                false /* waitForConnect */, null, null);
        ControllerCallback testCallback1 = new ControllerCallback() {};
        ControllerCallback testCallback2 = new ControllerCallback() {};

        List<Pair<ControllerCallback, Executor>> callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(0, callbacks.size());

        controller.registerExtraCallback(sHandlerExecutor, testCallback1);
        callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(1, callbacks.size());
        assertNotNull(callbacks.get(0));
        assertSame(testCallback1, callbacks.get(0).first);

        controller.registerExtraCallback(sHandlerExecutor, testCallback1);
        callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(1, callbacks.size());

        controller.unregisterExtraCallback(testCallback2);
        callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(1, callbacks.size());

        controller.registerExtraCallback(sHandlerExecutor, testCallback2);
        callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(2, callbacks.size());
        assertNotNull(callbacks.get(0));
        assertSame(testCallback1, callbacks.get(0).first);
        assertNotNull(callbacks.get(1));
        assertSame(testCallback2, callbacks.get(1).first);

        controller.unregisterExtraCallback(testCallback1);
        callbacks = controller.getExtraCallbacks();
        assertNotNull(callbacks);
        assertEquals(1, callbacks.size());
        assertNotNull(callbacks.get(0));
        assertSame(testCallback2, callbacks.get(0).first);
    }

    @Test
    public void testNotifyControllerCallback() throws InterruptedException {
        prepareLooper();

        final CountDownLatch primaryLatch = new CountDownLatch(1);
        ControllerCallback primaryCallback = new ControllerCallback() {
            @Override
            public void onPlaybackCompleted(@NonNull MediaController controller) {
                primaryLatch.countDown();
            }
        };
        final CountDownLatch extraLatch1 = new CountDownLatch(1);
        ControllerCallback extraCallback1 = new ControllerCallback() {
            @Override
            public void onPlaybackCompleted(@NonNull MediaController controller) {
                extraLatch1.countDown();
            }
        };
        final CountDownLatch extraLatch2 = new CountDownLatch(1);
        ControllerCallback extraCallback2 = new ControllerCallback() {
            @Override
            public void onPlaybackCompleted(@NonNull MediaController controller) {
                extraLatch2.countDown();
            }
        };
        final MediaController controller = createController(mRemoteSession.getToken(),
                false /* waitForConnect */, null, primaryCallback);
        controller.registerExtraCallback(sHandlerExecutor, extraCallback1);
        controller.registerExtraCallback(sHandlerExecutor, extraCallback2);
        controller.notifyControllerCallback(new ControllerCallbackRunnable() {
            @Override
            public void run(@NonNull ControllerCallback callback) {
                callback.onPlaybackCompleted(controller);
            }
        });
        assertTrue(primaryLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(extraLatch1.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(extraLatch2.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    RemoteMediaSession createRemoteMediaSession(String id, Bundle tokenExtras) {
        RemoteMediaSession session = new RemoteMediaSession(id, mContext, tokenExtras);
        mRemoteSessionList.add(session);
        return session;
    }
}
