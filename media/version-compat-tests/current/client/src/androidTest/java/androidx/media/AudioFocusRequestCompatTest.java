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

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test {@link AudioFocusRequestCompat}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AudioFocusRequestCompatTest {
    private AudioManager.OnAudioFocusChangeListener mStubListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int i) {
                    // Stub!
                }
            };

    @Test
    public void testGetters() {
        AudioFocusRequestCompat request =
                new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(mStubListener)
                        .build();
        assertThat(request.getFocusGain(), equalTo(AudioManager.AUDIOFOCUS_GAIN));
        assertThat(request.willPauseWhenDucked(), equalTo(true));
    }

    @Test
    public void testEqualsAndHashCode() {
        AudioFocusRequestCompat request =
                new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(mStubListener)
                        .build();
        AudioFocusRequestCompat requestCopy = new AudioFocusRequestCompat.Builder(request).build();
        assertThat(request, equalTo(requestCopy));
        assertThat(request.hashCode(), equalTo(requestCopy.hashCode()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testFrameworkEquality() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        AudioFocusRequestCompat requestCompat =
                new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(mStubListener, mainHandler)
                        .build();
        AudioFocusRequest compatAfr = requestCompat.getAudioFocusRequest();
        AudioFocusRequest afr =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(mStubListener, mainHandler)
                        .build();
        assertThat(afr.getFocusGain(), equalTo(compatAfr.getFocusGain()));
        assertThat(afr.getAudioAttributes(), equalTo(compatAfr.getAudioAttributes()));
        assertThat(afr.acceptsDelayedFocusGain(), equalTo(compatAfr.acceptsDelayedFocusGain()));
        assertThat(afr.willPauseWhenDucked(), equalTo(compatAfr.willPauseWhenDucked()));
    }
}
