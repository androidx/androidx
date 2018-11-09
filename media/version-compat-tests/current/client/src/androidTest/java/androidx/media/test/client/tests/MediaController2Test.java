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

package androidx.media.test.client.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED;
import static androidx.media.test.client.MediaTestUtils.assertEqualMediaIds;
import static androidx.media.test.client.MediaTestUtils.assertNotMediaItemSubclass;
import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media.test.lib.MediaSession2Constants.TEST_GET_SESSION_ACTIVITY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.mediacompat.testlib.util.PollingCheck;

import androidx.media.AudioAttributesCompat;
import androidx.media.test.client.MediaTestUtils;
import androidx.media.test.client.RemoteMediaSession2;
import androidx.media2.MediaController2;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaItem2;
import androidx.media2.SessionPlayer2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link MediaController2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaController2Test extends MediaSession2TestBase {

    final List<RemoteMediaSession2> mRemoteSessionList = new ArrayList<>();

    AudioManager mAudioManager;
    RemoteMediaSession2 mRemoteSession2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRemoteSession2 = createRemoteMediaSession2(DEFAULT_TEST_NAME);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        for (int i = 0; i < mRemoteSessionList.size(); i++) {
            RemoteMediaSession2 session = mRemoteSessionList.get(i);
            if (session != null) {
                session.cleanUp();
            }
        }
    }

    /**
     * Test if the {@link MediaSession2TestBase.TestControllerCallback} wraps the callback proxy
     * without missing any method.
     */
    @Test
    public void testTestControllerCallback() {
        prepareLooper();
        Method[] methods = TestControllerCallback.class.getMethods();
        assertNotNull(methods);
        for (int i = 0; i < methods.length; i++) {
            // For any methods in the controller callback, TestControllerCallback should have
            // overridden the method and call matching API in the callback proxy.
            assertNotEquals("TestControllerCallback should override " + methods[i]
                            + " and call callback proxy",
                    MediaController2.ControllerCallback.class, methods[i].getDeclaringClass());
        }
    }

    @Test
    public void testGetSessionActivity() throws InterruptedException {
        prepareLooper();
        RemoteMediaSession2 session2 = createRemoteMediaSession2(TEST_GET_SESSION_ACTIVITY);

        MediaController2 controller = createController(session2.getToken());
        PendingIntent sessionActivity = controller.getSessionActivity();
        assertNotNull(sessionActivity);
        if (Build.VERSION.SDK_INT >= 17) {
            // PendingIntent#getCreatorPackage() is added in API 17.
            assertEquals(SERVICE_PACKAGE_NAME, sessionActivity.getCreatorPackage());

            // TODO: Add getPid/getUid in MediaController2ProviderService and compare them.
            // assertEquals(mRemoteSession2.getUid(), sessionActivity.getCreatorUid());
        }
        session2.cleanUp();
    }

    @Test
    public void testSetVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        MediaController2 controller = createController(mRemoteSession2.getToken());

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }

        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream).build();
        Bundle playerConfig = RemoteMediaSession2.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession2.updatePlayer(playerConfig);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int targetVolume = originalVolume == minVolume
                ? originalVolume + 1 : originalVolume - 1;

        controller.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(TIMEOUT_MS) {
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

        MediaController2 controller = createController(mRemoteSession2.getToken());

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }

        AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setLegacyStreamType(stream).build();
        Bundle playerConfig = RemoteMediaSession2.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession2.updatePlayer(playerConfig);

        final int originalVolume = mAudioManager.getStreamVolume(stream);
        final int direction = originalVolume == minVolume
                ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        final int targetVolume = originalVolume + direction;

        controller.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
        new PollingCheck(TIMEOUT_MS) {
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
        MediaController2 controller = createController(mRemoteSession2.getToken());
        assertEquals(SERVICE_PACKAGE_NAME, controller.getConnectedSessionToken().getPackageName());
    }

    @Test
    public void testIsConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mRemoteSession2.getToken());
        assertTrue(controller.isConnected());

        mRemoteSession2.close();
        waitForDisconnect(controller, true);
        assertFalse(controller.isConnected());
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mRemoteSession2.getToken(),
                false /* waitForConnect */, null /* callback */);
        controller.close();
    }

    @Test
    public void testClose_twice() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mRemoteSession2.getToken());
        controller.close();
        controller.close();
    }

    @Test
    public void testGettersAfterConnected() throws InterruptedException {
        prepareLooper();
        final int state = SessionPlayer2.PLAYER_STATE_PLAYING;
        final int bufferingState = SessionPlayer2.BUFFERING_STATE_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem2 currentMediaItem = MediaTestUtils.createFileMediaItemWithMetadata();

        Bundle config = RemoteMediaSession2.createMockPlayerConnectorConfig(
                state, bufferingState, position, bufferedPosition, speed, null /* audioAttrs */,
                null /* playlist */, currentMediaItem, null /* metadata */);
        mRemoteSession2.updatePlayer(config);

        MediaController2 controller = createController(mRemoteSession2.getToken());
        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        assertNotMediaItemSubclass(controller.getCurrentMediaItem());
        assertEqualMediaIds(currentMediaItem, controller.getCurrentMediaItem());
    }

    @Test
    public void testGetPlaybackInfo() throws Exception {
        prepareLooper();
        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();

        Bundle playerConfig = RemoteMediaSession2.createMockPlayerConnectorConfig(
                0 /* state */, 0 /* buffState */, 0 /* position */, 0 /* buffPosition */,
                0f /* speed */, attrs);
        mRemoteSession2.updatePlayer(playerConfig);

        final MediaController2 controller = createController(mRemoteSession2.getToken());
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

    RemoteMediaSession2 createRemoteMediaSession2(String id) {
        RemoteMediaSession2 session = new RemoteMediaSession2(id, mContext);
        mRemoteSessionList.add(session);
        return session;
    }
}
