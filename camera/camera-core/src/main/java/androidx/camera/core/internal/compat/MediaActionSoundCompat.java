/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal.compat;

import android.media.MediaActionSound;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;

/**
 * Helper for accessing features of {@link MediaActionSound} in a backwards compatible fashion.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class MediaActionSoundCompat {

    /**
     * Returns whether the shutter sound must be played in accordance to regional restrictions.
     *
     * <p>This method provides the general rule of playing shutter sounds. The exact
     * requirements of playing shutter sounds may vary among regions.
     *
     * <p>For image capture, the shutter sound is recommended to be played when receiving
     * {@link ImageCapture.OnImageCapturedCallback#onCaptureStarted()} or
     * {@link ImageCapture.OnImageSavedCallback#onCaptureStarted()}. For video capture, it's
     * recommended to play the start recording sound when receiving
     * {@code VideoRecordEvent.Start} and the stop recording sound when receiving
     * {@code VideoRecordEvent.Finalize}.
     *
     * <p>To play the system default sounds, it's recommended to use
     * {@link MediaActionSound#play(int)}. For image capture, play
     * {@link MediaActionSound#SHUTTER_CLICK}. For video capture, play
     * {@link MediaActionSound#START_VIDEO_RECORDING} and
     * {@link MediaActionSound#STOP_VIDEO_RECORDING}.
     *
     * @return {@code true} if shutter sound must be played, otherwise {@code false}.
     */
    public static boolean mustPlayShutterSound() {
        if (Build.VERSION.SDK_INT >= 33) {
            return MediaActionSoundCompatApi33Impl.mustPlayShutterSound();
        } else {
            return MediaActionSoundCompatBaseImpl.mustPlayShutterSound();
        }
    }

    // Class should not be instantiated.
    private MediaActionSoundCompat() {
    }
}
