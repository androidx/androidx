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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader

    /**
     * Applies this [RemoteBrush] to a paint.
     *
     * Depending on whether the brush is a shader or a solid color, this method updates
     * [RemotePaint.shader] and [RemotePaint.color] accordingly.
     *
     * @param paint The paint to apply to.
     * @param size The size of the area being drawn, used for shader calculation.
     */
    public open fun RemoteStateScope.applyTo(paint: RemotePaint, size: RemoteSize) {
        if (hasShader) {
            paint.shader = createShader(size)
            paint.color = Color.Black.rc
        } else {
            TODO("Unimplemented RemoteBrush.applyTo for ${this@RemoteBrush}")
        }
    }

    /**
     * Applies this [RemoteBrush] to a paint.
     *
     * Depending on whether the brush is a shader or a solid color, this method updates
     * [RemotePaint.shader] and [RemotePaint.color] accordingly.
     *
     * @param paint The paint to apply to.
     * @param size The size of the area being drawn, used for shader calculation.
     * @param matrix3x3 An optional matrix to apply to the shader.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun RemoteStateScope.applyTo(
        paint: RemotePaint,
        size: RemoteSize,
        matrix3x3: RemoteMatrix3x3? = null,
    ) {
        if (hasShader) {
            paint.shader = createShader(size).apply { this.remoteMatrix3x3 = matrix3x3 }
            paint.color = Color.Black.rc
        } else {
            applyTo(paint, size)
        }
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val hasShader: Boolean
        get() = true

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("DEPRECATION")
public abstract class RemoteShader : android.graphics.Shader() {
    public abstract fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle)

    /**
     * The [RemoteMatrix3x3] if any to apply to the shader. Note not all profiles will support
     * shader rotation.
     */
    public abstract var remoteMatrix3x3: RemoteMatrix3x3?
}
