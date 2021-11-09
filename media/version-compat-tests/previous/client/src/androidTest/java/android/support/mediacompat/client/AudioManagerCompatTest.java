/*
 * Copyright 2020 The Android Open Source Project
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

package android.support.mediacompat.client;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import androidx.media.AudioManagerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link AudioManagerCompat}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AudioManagerCompatTest {

    private AudioManager mAudioManager;
    private int mStreamType;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mStreamType = AudioManager.STREAM_MUSIC;
    }

    @Test
    public void getStreamMaxVolume_returnsStreamMaxVolume() {
        assertEquals(
                mAudioManager.getStreamMaxVolume(mStreamType),
                AudioManagerCompat.getStreamMaxVolume(mAudioManager, mStreamType));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    public void getStreamMinVolume_fromP_returnsStreamMinVolume() {
        assertEquals(
                mAudioManager.getStreamMinVolume(mStreamType),
                AudioManagerCompat.getStreamMinVolume(mAudioManager, mStreamType));
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    @Test
    public void getStreamMinVolume_underP_returnsZero() {
        assertEquals(0, AudioManagerCompat.getStreamMinVolume(mAudioManager, mStreamType));
    }
}
