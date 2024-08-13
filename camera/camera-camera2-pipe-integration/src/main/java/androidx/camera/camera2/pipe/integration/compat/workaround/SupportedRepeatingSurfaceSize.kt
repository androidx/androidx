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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.RepeatingStreamConstraintForVideoRecordingQuirk
import androidx.camera.core.impl.utils.CompareSizesByArea

private val MINI_PREVIEW_SIZE_HUAWEI_MATE_9 = Size(320, 240)
private val SIZE_COMPARATOR: Comparator<Size> = CompareSizesByArea()

public fun Array<Size>.getSupportedRepeatingSurfaceSizes(): Array<Size> {
    DeviceQuirks[RepeatingStreamConstraintForVideoRecordingQuirk::class.java] ?: return this

    val supportedSizes = mutableListOf<Size>()
    for (size in this) {
        if (SIZE_COMPARATOR.compare(size, MINI_PREVIEW_SIZE_HUAWEI_MATE_9) >= 0) {
            supportedSizes.add(size)
        }
    }
    return supportedSizes.toTypedArray()
}
