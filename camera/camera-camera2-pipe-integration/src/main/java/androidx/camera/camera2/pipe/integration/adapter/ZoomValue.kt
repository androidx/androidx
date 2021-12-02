/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.annotation.RequiresApi
import androidx.camera.core.ZoomState

/**
 * Immutable adaptor to the ZoomState interface.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
data class ZoomValue(
    private val zoomRatio: Float,
    private val minZoomRatio: Float,
    private val maxZoomRatio: Float
) : ZoomState {
    override fun getZoomRatio(): Float = zoomRatio
    override fun getMaxZoomRatio(): Float = maxZoomRatio
    override fun getMinZoomRatio(): Float = minZoomRatio
    override fun getLinearZoom(): Float {
        val range = maxZoomRatio - minZoomRatio
        if (range > 0) {
            return (zoomRatio - minZoomRatio) / range
        }
        return 1.0f
    }
}