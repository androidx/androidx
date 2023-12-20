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

package androidx.hardware

import android.hardware.HardwareBuffer.BLOB
import android.hardware.HardwareBuffer.DS_24UI8
import android.hardware.HardwareBuffer.DS_FP32UI8
import android.hardware.HardwareBuffer.D_16
import android.hardware.HardwareBuffer.D_24
import android.hardware.HardwareBuffer.D_FP32
import android.hardware.HardwareBuffer.RGBA_1010102
import android.hardware.HardwareBuffer.RGBA_8888
import android.hardware.HardwareBuffer.RGBA_FP16
import android.hardware.HardwareBuffer.RGBX_8888
import android.hardware.HardwareBuffer.RGB_565
import android.hardware.HardwareBuffer.RGB_888
import android.hardware.HardwareBuffer.S_UI8
import android.hardware.HardwareBuffer.YCBCR_420_888
import android.hardware.HardwareBuffer.YCBCR_P010
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    RGBA_8888,
    RGBA_FP16,
    RGBA_1010102,
    RGBX_8888,
    RGB_888,
    RGB_565,
    BLOB,
    YCBCR_420_888,
    D_16,
    D_24,
    DS_24UI8,
    D_FP32,
    DS_FP32UI8,
    S_UI8,
    YCBCR_P010
)
annotation class HardwareBufferFormat
