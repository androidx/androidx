/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.capture.shaders

import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.frontend.state.RemoteMatrix3x3
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.SolidColor

/*
 * This is used to provide a way to intercept linear gradient brushes in Remote, so that
 * we can serialize them.
 */
@Immutable
abstract class RemoteBrush {

    /**
     * Return the intrinsic size of the [Brush]. If the there is no intrinsic size (i.e. filling
     * bounds with an arbitrary color) return [Size.Unspecified]. If there is no intrinsic size in a
     * single dimension, return [Size] with [Float.NaN] in the desired dimension.
     */
    open val intrinsicSize: Size = Size.Unspecified

    abstract fun toComposeUi(): Brush

    abstract fun createShader(size: Size): Shader

    open val hasShader: Boolean
        get() = true

    companion object {
        fun fromComposeUi(brush: Brush): RemoteBrush {
            return when (brush) {
                is SolidColor -> RemoteBrush.solidColor(Color.Red)
                else -> {
                    println("RemoteBrush.fromComposeUi not implemented for $brush")
                    RemoteBrush.solidColor(Color.Transparent)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
interface RemoteShader {
    abstract fun apply(paintBundle: PaintBundle)

    /**
     * The [RemoteMatrix3x3] if any to apply to the shader. Note not all profiles will support
     * shader rotation.
     */
    abstract val remoteMatrix3x3: RemoteMatrix3x3?
}
