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

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
import static androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED;
import static androidx.media.test.lib.CommonConstants.KEY_AUDIO_ATTRIBUTES;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERED_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_BUFFERING_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_CURRENT_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ITEM;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYER_STATE;
import static androidx.media.test.lib.CommonConstants.KEY_SPEED;
import static androidx.media.test.lib.CommonConstants.KEY_STREAM;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.CLOSE;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods
        .CUSTOM_METHOD_SET_MULTIPLE_VALUES;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods.UPDATE_PLAYER;
import static androidx.media.test.lib.MediaSession2Constants.Session2Methods
        .UPDATE_PLAYER_FOR_SETTING_STREAM_TYPE;
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
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.AudioAttributesCompat;
import androidx.media.BaseMediaPlayer;
import androidx.media.MediaController2;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaItem2;
import androidx.media.SessionToken2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * Tests {@link MediaController2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaController2Test extends MediaSession2TestBase {

    AudioManager mAudioManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
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
        MediaController2 controller = createController(
                mTestHelper.createMediaSession2(TEST_GET_SESSION_ACTIVITY));
        PendingIntent sessionActivity = controller.getSessionActivity();
        assertEquals(SERVICE_PACKAGE_NAME, sessionActivity.getCreatorPackage());
        // TODO: Add getPid/getUid in TestHelperService and compare them.
        // assertEquals(mTestHelper.getUid(), sessionActivity.getCreatorUid());
    }

    @Test
    public void testSetVolumeWithLocalVolume() throws Exception {
        prepareLooper();
        if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
            // This test is not eligible for this device.
            return;
        }

        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2());

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }

        Bundle args = new Bundle();
        args.putInt(KEY_STREAM, stream);
        mTestHelper.callMediaSession2Method(UPDATE_PLAYER_FOR_SETTING_STREAM_TYPE, args);

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

        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2());

        // Here, we intentionally choose STREAM_ALARM in order not to consider
        // 'Do Not Disturb' or 'Volume limit'.
        final int stream = AudioManager.STREAM_ALARM;
        final int maxVolume = mAudioManager.getStreamMaxVolume(stream);
        final int minVolume = 0;
        if (maxVolume <= minVolume) {
            return;
        }

        Bundle args = new Bundle();
        args.putInt(KEY_STREAM, stream);
        mTestHelper.callMediaSession2Method(UPDATE_PLAYER_FOR_SETTING_STREAM_TYPE, args);

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
        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2());
        assertEquals(SERVICE_PACKAGE_NAME, controller.getSessionToken().getPackageName());
    }

    @Test
    public void testIsConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2());
        assertTrue(controller.isConnected());

        mTestHelper.callMediaSession2Method(CLOSE, null);
        waitForDisconnect(controller, true);
        assertFalse(controller.isConnected());
    }

    @Test
    public void testClose_beforeConnected() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2(),
                false /* waitForConnect */, null /* callback */);
        controller.close();
    }

    @Test
    public void testClose_twice() throws InterruptedException {
        prepareLooper();
        MediaController2 controller = createController(mTestHelper.createDefaultMediaSession2());
        controller.close();
        controller.close();
    }

    @Test
    public void testGettersAfterConnected() throws InterruptedException {
        prepareLooper();
        final int state = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        final int bufferingState = BaseMediaPlayer.BUFFERING_STATE_BUFFERING_COMPLETE;
        final long position = 150000;
        final long bufferedPosition = 900000;
        final float speed = 0.5f;
        final long timeDiff = 102;
        final MediaItem2 currentMediaItem = MediaTestUtils.createMediaItemWithMetadata();

        SessionToken2 token = mTestHelper.createDefaultMediaSession2();

        Bundle args = new Bundle();
        args.putInt(KEY_PLAYER_STATE, state);
        args.putInt(KEY_BUFFERING_STATE, bufferingState);
        args.putLong(KEY_CURRENT_POSITION, position);
        args.putLong(KEY_BUFFERED_POSITION, bufferedPosition);
        args.putFloat(KEY_SPEED, speed);
        args.putBundle(KEY_MEDIA_ITEM, currentMediaItem.toBundle());
        mTestHelper.callMediaSession2Method(CUSTOM_METHOD_SET_MULTIPLE_VALUES, args);

        MediaController2 controller = createController(token);
        controller.setTimeDiff(timeDiff);
        assertEquals(state, controller.getPlayerState());
        assertEquals(bufferedPosition, controller.getBufferedPosition());
        assertEquals(speed, controller.getPlaybackSpeed(), 0.0f);
        assertEquals(position + (long) (speed * timeDiff), controller.getCurrentPosition());
        assertEquals(currentMediaItem, controller.getCurrentMediaItem());
    }

    @Test
    public void testGetPlaybackInfo() throws Exception {
        prepareLooper();
        SessionToken2 token = mTestHelper.createDefaultMediaSession2();

        final AudioAttributesCompat attrs = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .build();

        Bundle args = new Bundle();
        args.putBundle(KEY_AUDIO_ATTRIBUTES, attrs.toBundle());
        mTestHelper.callMediaSession2Method(UPDATE_PLAYER, args);

        final MediaController2 controller = createController(token);
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
}
