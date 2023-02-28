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

package androidx.camera.camera2.pipe.testing

import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper
import kotlin.reflect.KClass

/** Fake [OutputConfigurationWrapper] for use in tests. */
class FakeOutputConfigurationWrapper(
    outputSurface: Surface? = null,
    override val physicalCameraId: CameraId? = null,
    override val surfaceSharing: Boolean = false,
    override val maxSharedSurfaceCount: Int = 1,
    override val surfaceGroupId: Int = -1
) : OutputConfigurationWrapper {
    private val _surfaces = mutableListOf<Surface>()

    init {
        if (outputSurface != null) {
            _surfaces.add(outputSurface)
        }
    }

    override val surface: Surface?
        get() = _surfaces.firstOrNull()

    override val surfaces: List<Surface>
        get() = _surfaces.toList()

    override fun addSurface(surface: Surface) {
        _surfaces.add(surface)
    }

    override fun removeSurface(surface: Surface) {
        _surfaces.remove(surface)
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null
}
