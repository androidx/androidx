/*
 * Copyright (C) 2023 The Android Open Source Project
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
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
public fun RemoteBrush.Companion.solidColor(color: RemoteColor): RemoteBrush =
    RemoteSolidColor(color)

@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSolidColor(public val color: RemoteColor) : RemoteBrush() {

    override fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader {
        throw UnsupportedOperationException(
            "SolidColor not supported for Shader, use Color directly"
        )
    }

    override fun RemoteStateScope.applyTo(paint: RemotePaint, size: RemoteSize) {
        paint.color = color
        paint.shader = null
    }

    override val hasShader: Boolean
        get() = false
}
