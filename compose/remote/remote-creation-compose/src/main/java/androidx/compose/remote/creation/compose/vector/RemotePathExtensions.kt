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

package androidx.compose.remote.creation.compose.vector

import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope

internal class RemotePathExtensions(private val creationState: RemoteStateScope) {

    fun RemotePath.moveTo(x: RemoteFloat, y: RemoteFloat) = withScope {
        moveTo(x.floatId, y.floatId)
    }

    private inline fun withScope(function: RemoteStateScope.() -> Unit) {
        with(creationState, function)
    }

    fun RemotePath.rMoveTo(dx: RemoteFloat, dy: RemoteFloat) = withScope {
        rMoveTo(dx.floatId, dy.floatId)
    }

    fun RemotePath.quadTo(x1: RemoteFloat, y1: RemoteFloat, x2: RemoteFloat, y2: RemoteFloat) =
        withScope {
            quadTo(x1.floatId, y1.floatId, x2.floatId, y2.floatId)
        }

    fun RemotePath.rQuadTo(dx1: RemoteFloat, dy1: RemoteFloat, dx2: RemoteFloat, dy2: RemoteFloat) =
        withScope {
            rQuadTo(dx1.floatId, dy1.floatId, dx2.floatId, dy2.floatId)
        }

    fun RemotePath.conicTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
        weight: RemoteFloat,
    ) = withScope { conicTo(x1.floatId, y1.floatId, x2.floatId, y2.floatId, weight.floatId) }

    fun RemotePath.rConicTo(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
        weight: RemoteFloat,
    ) = withScope { rConicTo(dx1.floatId, dy1.floatId, dx2.floatId, dy2.floatId, weight.floatId) }

    fun RemotePath.lineTo(x: RemoteFloat, y: RemoteFloat) = withScope {
        lineTo(x.floatId, y.floatId)
    }

    fun RemotePath.rLineTo(dx: RemoteFloat, dy: RemoteFloat) = withScope {
        rLineTo(dx.floatId, dy.floatId)
    }

    fun RemotePath.cubicTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
        x3: RemoteFloat,
        y3: RemoteFloat,
    ) = withScope {
        cubicTo(x1.floatId, y1.floatId, x2.floatId, y2.floatId, x3.floatId, y3.floatId)
    }

    fun RemotePath.rCubicTo(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
        dx3: RemoteFloat,
        dy3: RemoteFloat,
    ) = withScope {
        rCubicTo(dx1.floatId, dy1.floatId, dx2.floatId, dy2.floatId, dx3.floatId, dy3.floatId)
    }
}
