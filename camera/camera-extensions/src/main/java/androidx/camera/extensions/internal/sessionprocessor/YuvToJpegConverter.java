/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import android.graphics.ImageFormat;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageProcessingUtil;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.core.util.Preconditions;

/**
 * A image converter for YUV_420_888 to JPEG. The converted JPEG images were written to the given
 * output surface after {@link #writeYuvImage(ImageProxy)} is invoked.
 */
class YuvToJpegConverter {
    private static final String TAG = "YuvToJpegConverter";
    private final Surface mOutputJpegSurface;
    @IntRange(from = 1, to = 100)
    private volatile int mJpegQuality;
    @ImageOutputConfig.RotationDegreesValue
    private volatile int mRotationDegrees = 0;

    YuvToJpegConverter(int jpegQuality, @NonNull Surface outputJpegSurface) {
        mJpegQuality = jpegQuality;
        mOutputJpegSurface = outputJpegSurface;
    }

    public void setRotationDegrees(@ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        mRotationDegrees = rotationDegrees;
    }

    void setJpegQuality(int jpgQuality) {
        mJpegQuality = jpgQuality;
    }

    static class ConversionFailedException extends Exception {
        ConversionFailedException(String message) {
            super(message);
        }
        ConversionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Writes an YUV_420_888 image and converts it into a JPEG image.
     */
    void writeYuvImage(@NonNull ImageProxy imageProxy) throws ConversionFailedException {
        Preconditions.checkState(imageProxy.getFormat() == ImageFormat.YUV_420_888,
                "Input image is not expected YUV_420_888 image format");
        try {
            // TODO(b/258667618): remove extra copy by writing the jpeg data directly into the
            //  Surface's ByteBuffer.
            boolean success = ImageProcessingUtil.convertYuvToJpegBytesIntoSurface(
                    imageProxy,
                    mJpegQuality,
                    mRotationDegrees,
                    mOutputJpegSurface);
            if (!success) {
                throw new ConversionFailedException("Failed to process YUV -> JPEG");
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to process YUV -> JPEG", e);
            throw new ConversionFailedException("Failed to process YUV -> JPEG", e);
        } finally {
            imageProxy.close();
        }
    }
}
