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

import android.hardware.HardwareBuffer.USAGE_COMPOSER_OVERLAY
import android.hardware.HardwareBuffer.USAGE_CPU_READ_OFTEN
import android.hardware.HardwareBuffer.USAGE_CPU_READ_RARELY
import android.hardware.HardwareBuffer.USAGE_CPU_WRITE_OFTEN
import android.hardware.HardwareBuffer.USAGE_CPU_WRITE_RARELY
import android.hardware.HardwareBuffer.USAGE_FRONT_BUFFER
import android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
import android.hardware.HardwareBuffer.USAGE_GPU_CUBE_MAP
import android.hardware.HardwareBuffer.USAGE_GPU_DATA_BUFFER
import android.hardware.HardwareBuffer.USAGE_GPU_MIPMAP_COMPLETE
import android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
import android.hardware.HardwareBuffer.USAGE_PROTECTED_CONTENT
import android.hardware.HardwareBuffer.USAGE_SENSOR_DIRECT_DATA
import android.hardware.HardwareBuffer.USAGE_VIDEO_ENCODE
import androidx.annotation.LongDef
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@LongDef(
    USAGE_CPU_READ_RARELY,
    USAGE_CPU_READ_OFTEN,
    USAGE_CPU_WRITE_RARELY,
    USAGE_CPU_WRITE_OFTEN,
    USAGE_GPU_SAMPLED_IMAGE,
    USAGE_GPU_COLOR_OUTPUT,
    USAGE_COMPOSER_OVERLAY,
    USAGE_PROTECTED_CONTENT,
    USAGE_VIDEO_ENCODE,
    USAGE_GPU_DATA_BUFFER,
    USAGE_SENSOR_DIRECT_DATA,
    USAGE_GPU_CUBE_MAP,
    USAGE_GPU_MIPMAP_COMPLETE,
    USAGE_FRONT_BUFFER
)
annotation class HardwareBufferUsage
