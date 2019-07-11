/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.media.ImageReader;

import com.google.auto.value.AutoValue;

/** Recommends formats for a combination of {@link ImageReader} instances. */
final class ImageReaderFormatRecommender {

    private ImageReaderFormatRecommender() {
    }

    /** Chooses a combination which is compatible for the current device. */
    static FormatCombo chooseCombo() {
        if (ImageReaderProxys.inSharedReaderWhitelist(DeviceProperties.create())) {
            return FormatCombo.create(ImageFormat.YUV_420_888, ImageFormat.YUV_420_888);
        } else {
            return FormatCombo.create(ImageFormat.JPEG, ImageFormat.YUV_420_888);
        }
    }

    /** Container for a combination of {@link ImageReader} formats. */
    @AutoValue
    abstract static class FormatCombo {
        static FormatCombo create(int imageCaptureFormat, int imageAnalysisFormat) {
            return new AutoValue_ImageReaderFormatRecommender_FormatCombo(
                    imageCaptureFormat, imageAnalysisFormat);
        }

        // Returns the format for image capture.
        abstract int imageCaptureFormat();

        // Returns the format for image analysis.
        abstract int imageAnalysisFormat();
    }
}
