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

package androidx.camera.viewfinder.compose

import android.util.Size
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

data class ViewfinderTestParams(
    val viewfinderSize: DpSize = TEST_VIEWFINDER_SIZE,
    val sourceRotation: Int = TEST_ROTATION,
    val sourceResolution: Size =
        when (sourceRotation) {
            0,
            180 -> TEST_RESOLUTION
            90,
            270 -> TEST_RESOLUTION.swapDimens()
            else -> throw IllegalArgumentException("Invalid source rotation: $sourceRotation")
        },
    val implementationMode: ImplementationMode = ImplementationMode.EXTERNAL,
    val transformationInfo: TransformationInfo =
        TransformationInfo(
            sourceRotation = sourceRotation,
            cropRectLeft = 0,
            cropRectTop = 0,
            cropRectRight = sourceResolution.width,
            cropRectBottom = sourceResolution.height,
            shouldMirror = false
        )
) {
    companion object {
        val TEST_VIEWFINDER_SIZE = DpSize(360.dp, 640.dp)
        const val TEST_ROTATION = 0
        val TEST_RESOLUTION = Size(540, 960)
        val Default = ViewfinderTestParams()
    }
}

private fun Size.swapDimens(): Size = Size(height, width)
