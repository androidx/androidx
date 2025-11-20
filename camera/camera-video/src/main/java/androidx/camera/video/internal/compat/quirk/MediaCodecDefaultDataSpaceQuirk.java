/*
 * Copyright 2025 The Android Open Source Project
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

import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_SRGB;

import androidx.camera.core.impl.Quirk;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;

import org.jspecify.annotations.NonNull;

/**
 * <p>QuirkSummary
 *     Bug Id: b/382186115
 *     Description: MediaCodec requires explicitly setting of data space (color standard, color
 *                  transfer, color range), and should not be allowed to infer the data space. In
 *                  the case where the camera is directly connected to MediaCodec, the camera passes
 *                  the data space information to MediaCodec via buffers, so MediaCodec does not
 *                  need to infer the data space. For the case involving OpenGL, OpenGL does not
 *                  pass through the original data space, so if the data space is not explicitly set
 *                  to MediaCodec, MediaCodec will guess the data space through heuristic ways. As a
 *                  result, for the case record from camera to MediaCodec involving OpenGL, the data
 *                  space is undefined and the value might depend on OEM implementation, for example
 *                  it may be fixed to a certain value or vary depending on the resolution.
 *     Device(s): all devices
 */
public class MediaCodecDefaultDataSpaceQuirk implements Quirk {


    static boolean load() {
        return true;
    }

    /**
     * Returns the suggested {@link VideoEncoderDataSpace} for the current device.
     */
    @NonNull
    public VideoEncoderDataSpace getSuggestedDataSpace() {
        // Uses sRGB as suggested default dataspace for recording to MediaCodec involving OpenGL.
        // According to Camera2 doc, "Image processing code can safely treat the output RGB as being
        // in the sRGB colorspace", assumes sRGB to be the default color space when none is set with
        // SessionConfiguration#setColorSpace or an explicit data space isn't set with an
        // ImageReader constructor.
        // (https://developer.android.com/reference/android/hardware/camera2/package-summary)
        return ENCODER_DATA_SPACE_SRGB;
    }
}
