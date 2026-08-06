/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.shaders

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * A remote representation of a [Brush] that can be serialized and reconstructed on a remote
 * surface.
 *
 * This class provides a mechanism to intercept standard Compose [Brush] instances, such as linear
 * gradients or solid colors, and convert them into a format suitable for remote rendering.
 */
@Immutable
public abstract class RemoteBrush internal constructor() {

    /**
     * Return the intrinsic size of the [RemoteBrush]. If the there is no intrinsic size (i.e.
     * filling bounds with an arbitrary color) return [Size.Unspecified]. If there is no intrinsic
     * size in a single dimension, return [Size] with [Float.NaN] in the desired dimension.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val intrinsicSize: Size = Size.Unspecified

    /**
     * Applies this [RemoteBrush] to a paint.
     *
     * @param paint The paint to apply to.
     * @param size The size of the area being drawn, used for shader calculation.
     */
    public abstract fun RemoteStateScope.applyTo(paint: RemotePaint, size: RemoteSize)

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromComposeUi(brush: Brush): RemoteBrush {
            return when (brush) {
                is SolidColor -> RemoteBrush.solidColor(brush.value.rc)
                else -> {
                    println("RemoteBrush.fromComposeUi not implemented for $brush")
                    RemoteBrush.solidColor(Color.Transparent.rc)
                }
            }
        }

        internal fun resolve(value: RemoteFloat, infinityValue: RemoteFloat): RemoteFloat {
            return if (value.constantValueOrNull == Float.POSITIVE_INFINITY) {
                infinityValue
            } else {
                value
            }
        }
    }
}

/**
 * Brush that applies a shader to the drawn area.
 *
 * Base class for shader-based brushes such as gradients and image textures.
 */
public abstract class RemoteShaderBrush internal constructor() : RemoteBrush() {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader

    override fun RemoteStateScope.applyTo(paint: RemotePaint, size: RemoteSize) {
        val shader = createShader(size)
        paint.shader = shader
        paint.color = Color.Black.rc
    }

    /**
     * Applies this [RemoteShaderBrush] to a [paint] with a given [size] and [matrix3x3]
     * transformation.
     *
     * @sample androidx.compose.remote.creation.compose.samples.RemoteCanvasShaderMatrixSample
     * @param paint The paint to apply the shader to.
     * @param size The size of the area being drawn.
     * @param matrix3x3 The 3x3 matrix to apply to the shader.
     */
    public open fun RemoteStateScope.applyTo(
        paint: RemotePaint,
        size: RemoteSize,
        matrix3x3: RemoteMatrix3x3,
    ) {
        val shader = createShader(size)
        shader.remoteMatrix3x3 = matrix3x3
        paint.shader = shader
        paint.color = Color.Black.rc
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("DEPRECATION")
public abstract class RemoteShader : android.graphics.Shader() {
    public abstract fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle)

    /**
     * The [RemoteMatrix3x3] to apply to the shader. Note not all profiles will support shader
     * rotation.
     */
    public open var remoteMatrix3x3: RemoteMatrix3x3 = RemoteMatrix3x3.createIdentity()
}
