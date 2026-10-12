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

package androidx.camera.effects.playservices.lowlight

import android.graphics.Matrix
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.util.GraphicDeviceInfo
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/** A fake [OpenGlRenderer] that records its calls without using EGL. */
internal class FakeGlRenderer(
    private val texName: Int = 1,
    private val failInit: Boolean = false,
) : OpenGlRenderer() {
    var isReleased = false
        private set

    val registeredSurfaces = mutableSetOf<Surface>()

    /** The number of [registerOutputSurface] calls. */
    var registerCallCount = 0
        private set

    /** The output surface of each [render] call, in order. */
    val renderedSurfaces = mutableListOf<Surface>()

    override fun init(dynamicRange: DynamicRange): GraphicDeviceInfo {
        if (failInit) {
            throw IllegalStateException("Failed to initialize EGL")
        }
        return GraphicDeviceInfo.builder().build()
    }

    override fun getTextureName(): Int = texName

    override fun release() {
        isReleased = true
    }

    override fun registerOutputSurface(surface: Surface) {
        registerCallCount++
        registeredSurfaces.add(surface)
    }

    override fun unregisterOutputSurface(surface: Surface) {
        registeredSurfaces.remove(surface)
    }

    override fun render(timestampNs: Long, textureTransform: FloatArray, surface: Surface) {
        renderedSurfaces.add(surface)
    }
}

/** A fake [SurfaceOutput] that returns the given [surface]. */
internal class FakeSurfaceOutput(
    private val surface: Surface,
    private val format: Int = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
) : SurfaceOutput {
    var isClosed = false
        private set

    /** The listener passed to [getSurface], used to send a close request. */
    var listener: Consumer<SurfaceOutput.Event>? = null
        private set

    override fun getSurface(executor: Executor, listener: Consumer<SurfaceOutput.Event>): Surface {
        this.listener = listener
        return surface
    }

    override fun getTargets(): Int = CameraEffect.PREVIEW

    override fun getFormat(): Int = format

    override fun getSize(): Size = Size(1920, 1080)

    override fun getSensorToBufferTransform(): Matrix = Matrix()

    override fun close() {
        isClosed = true
    }

    override fun updateTransformMatrix(output: FloatArray, input: FloatArray) {}
}
