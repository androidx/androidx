/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents the image format.
 *
 * This is a superset of the image formats defined in [android.graphics.ImageFormat],
 * [android.graphics.PixelFormat], and [android.hardware.HardwareBuffer].
 *
 * @see [android.graphics.ImageFormat]
 * @see [android.graphics.PixelFormat]
 * @see [android.hardware.HardwareBuffer]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            android.graphics.ImageFormat.UNKNOWN,
            android.graphics.ImageFormat.RGB_565,
            android.graphics.ImageFormat.YV12,
            android.graphics.ImageFormat.Y8,
            android.graphics.ImageFormat.NV16,
            android.graphics.ImageFormat.NV21,
            android.graphics.ImageFormat.YUY2,
            android.graphics.ImageFormat.JPEG,
            android.graphics.ImageFormat.DEPTH_JPEG,
            android.graphics.ImageFormat.YUV_420_888,
            android.graphics.ImageFormat.YUV_422_888,
            android.graphics.ImageFormat.YUV_444_888,
            android.graphics.ImageFormat.FLEX_RGB_888,
            android.graphics.ImageFormat.FLEX_RGBA_8888,
            android.graphics.ImageFormat.RAW_SENSOR,
            android.graphics.ImageFormat.RAW_PRIVATE,
            android.graphics.ImageFormat.RAW10,
            android.graphics.ImageFormat.RAW12,
            android.graphics.ImageFormat.DEPTH16,
            android.graphics.ImageFormat.DEPTH_POINT_CLOUD,
            android.graphics.ImageFormat.PRIVATE,
            android.graphics.ImageFormat.HEIC,
            android.graphics.ImageFormat.YCBCR_P010,
            android.graphics.PixelFormat.UNKNOWN,
            android.graphics.PixelFormat.TRANSLUCENT,
            android.graphics.PixelFormat.TRANSPARENT,
            android.graphics.PixelFormat.OPAQUE,
            android.graphics.PixelFormat.RGBA_8888,
            android.graphics.PixelFormat.RGBX_8888,
            android.graphics.PixelFormat.RGB_888,
            android.graphics.PixelFormat.RGB_565,
            android.graphics.PixelFormat.RGBA_F16,
            android.graphics.PixelFormat.RGBA_1010102,
            android.hardware.HardwareBuffer.RGBA_8888,
            android.hardware.HardwareBuffer.RGBA_FP16,
            android.hardware.HardwareBuffer.RGBA_1010102,
            android.hardware.HardwareBuffer.RGBX_8888,
            android.hardware.HardwareBuffer.RGB_888,
            android.hardware.HardwareBuffer.RGB_565,
            android.hardware.HardwareBuffer.BLOB,
            android.hardware.HardwareBuffer.YCBCR_420_888,
            android.hardware.HardwareBuffer.D_16,
            android.hardware.HardwareBuffer.D_24,
            android.hardware.HardwareBuffer.DS_24UI8,
            android.hardware.HardwareBuffer.D_FP32,
            android.hardware.HardwareBuffer.DS_FP32UI8,
            android.hardware.HardwareBuffer.S_UI8,
            android.hardware.HardwareBuffer.YCBCR_P010,
        ]
)
public annotation class ImageFormat
