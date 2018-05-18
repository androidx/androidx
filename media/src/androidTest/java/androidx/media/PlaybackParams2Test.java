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

package androidx.media;

import static junit.framework.Assert.assertEquals;

import android.media.PlaybackParams;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link PlaybackParams2}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PlaybackParams2Test extends MediaTestBase {

    @Test
    public void testConstructor() {
        PlaybackParams2 params = new PlaybackParams2.Builder()
                .setAudioFallbackMode(PlaybackParams2.AUDIO_FALLBACK_MODE_MUTE)
                .setPitch(1.0f)
                .setSpeed(1.0f)
                .build();
        assertEquals(PlaybackParams2.AUDIO_FALLBACK_MODE_MUTE, (int) params.getAudioFallbackMode());
        assertEquals(1.0f, params.getPitch());
        assertEquals(1.0f, params.getSpeed());
    }

    @Test
    public void testConstructorNullValues() {
        PlaybackParams2 params = new PlaybackParams2.Builder().build();
        assertEquals(null, params.getAudioFallbackMode());
        assertEquals(null, params.getPitch());
        assertEquals(null, params.getSpeed());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testConstants() {
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT,
                PlaybackParams2.AUDIO_FALLBACK_MODE_DEFAULT);
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_MUTE,
                PlaybackParams2.AUDIO_FALLBACK_MODE_MUTE);
        assertEquals(PlaybackParams.AUDIO_FALLBACK_MODE_FAIL,
                PlaybackParams2.AUDIO_FALLBACK_MODE_FAIL);
    }
}
