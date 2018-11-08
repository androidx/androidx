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

import static junit.framework.Assert.assertEquals;

import android.os.Build;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link PlaybackParams}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PlaybackParamsTest extends MediaTestBase {

    @Test
    public void testConstructor() {
        PlaybackParams params = new PlaybackParams.Builder()
                .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_MUTE)
                .setPitch(1.0f)
                .setSpeed(1.0f)
                .build();
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_MUTE, (int) params.getAudioFallbackMode());
        assertEquals(1.0f, params.getPitch());
        assertEquals(1.0f, params.getSpeed());
    }

    @Test
    public void testConstructorNullValues() {
        PlaybackParams params = new PlaybackParams.Builder().build();
        assertEquals(null, params.getAudioFallbackMode());
        assertEquals(null, params.getPitch());
        assertEquals(null, params.getSpeed());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testConstants() {
        assertEquals(android.media.PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT,
                PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT);
        assertEquals(android.media.PlaybackParams.AUDIO_FALLBACK_MODE_MUTE,
                PlaybackParams.AUDIO_FALLBACK_MODE_MUTE);
        assertEquals(android.media.PlaybackParams.AUDIO_FALLBACK_MODE_FAIL,
                PlaybackParams.AUDIO_FALLBACK_MODE_FAIL);
    }
}
