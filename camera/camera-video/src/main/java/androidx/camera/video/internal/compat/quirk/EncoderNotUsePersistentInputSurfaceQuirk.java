/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * Quirk denotes that the encoder should create new input surface for every encoding instead of
 * using {@link MediaCodec#createPersistentInputSurface()}.
 *
 * <p>{@link MediaCodec#createPersistentInputSurface()} is introduced on API 23, which creates a
 * reusable surface for multiple encodings and is the suggested approach. So for devices with API
 * 21 and 22, a new surface has to be created for every encoding instead.
 *
 * <p>As describe in b/202798966, there is a device that has API 23+, but the recorded video is
 * abnormal if using {@link MediaCodec#createPersistentInputSurface()}. Creating a new surface
 * for new recording resolve the issue, hence this quirk is also applied to the problematic devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class EncoderNotUsePersistentInputSurfaceQuirk implements Quirk {

    private static final List<String> DEVICE_MODELS = Arrays.asList(
            "SM-N9208"
    );

    static boolean load() {
        return Build.VERSION.SDK_INT <= 22 || DEVICE_MODELS.contains(
                android.os.Build.MODEL.toUpperCase());
    }
}
