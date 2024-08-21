/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import android.util.SparseIntArray
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

/** Generates a [StreamConfigurationMap] with the given [supportedSizes]. */
fun generateFakeStreamConfigurationMap(supportedSizes: Array<Size>): StreamConfigurationMap {
    val map =
        StreamConfigurationMapBuilder.newBuilder()
            .apply { supportedSizes.forEach(::addOutputSize) }
            .build()

    // Workaround for NullPointerException in Robolectric when calling getOutputFormats(). The
    // issue seems to be due to following formats not being handled in Robolectric.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ReflectionHelpers.setField(
            StreamConfigurationMap::class.java,
            map,
            "mDepthOutputFormats",
            SparseIntArray()
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ReflectionHelpers.setField(
            StreamConfigurationMap::class.java,
            map,
            "mDynamicDepthOutputFormats",
            SparseIntArray()
        )

        ReflectionHelpers.setField(
            StreamConfigurationMap::class.java,
            map,
            "mHeicOutputFormats",
            SparseIntArray()
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ReflectionHelpers.setField(
            StreamConfigurationMap::class.java,
            map,
            "mJpegROutputFormats",
            SparseIntArray()
        )
    }

    return map
}
