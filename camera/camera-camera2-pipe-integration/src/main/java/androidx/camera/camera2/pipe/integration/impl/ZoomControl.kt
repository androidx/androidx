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

package androidx.camera.camera2.pipe.integration.impl

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class ZoomControl @Inject constructor(private val zoomCompat: ZoomCompat) : UseCaseCameraControl {
    private var _zoomRatio = 1.0f
    var zoomRatio: Float
        get() = _zoomRatio
        set(value) {
            // TODO: Make this a suspend function?
            _zoomRatio = value
            update()
        }

    // NOTE: minZoom may be lower than 1.0
    // NOTE: Default zoom ratio is 1.0
    // NOTE: Linear zoom is
    val minZoom: Float = zoomCompat.minZoom
    val maxZoom: Float = zoomCompat.maxZoom

    /** Linear zoom is between 0.0f and 1.0f */
    fun toLinearZoom(zoomRatio: Float): Float {
        val range = zoomCompat.maxZoom - zoomCompat.minZoom
        if (range > 0) {
            return (zoomRatio - zoomCompat.minZoom) / range
        }
        return 0.0f
    }

    /** Zoom ratio is commonly used as the "1x, 2x, 5x" zoom ratio. Zoom ratio may be less than 1 */
    fun toZoomRatio(linearZoom: Float): Float {
        val range = zoomCompat.maxZoom - zoomCompat.minZoom
        if (range > 0) {
            return linearZoom * range + zoomCompat.minZoom
        }
        return 1.0f
    }

    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            update()
        }

    override fun reset() {
        // TODO: 1.0 may not be a reasonable value to reset the zoom state too.
        zoomRatio = 1.0f
        update()
    }

    private fun update() {
        _useCaseCamera?.let {
            zoomCompat.apply(_zoomRatio, it)
        }
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(zoomControl: ZoomControl): UseCaseCameraControl
    }
}
