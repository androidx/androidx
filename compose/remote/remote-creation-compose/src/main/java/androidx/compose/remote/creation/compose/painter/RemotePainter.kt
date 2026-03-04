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

package androidx.compose.remote.creation.compose.painter

import android.graphics.BlendMode
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha

/**
 * A class that holds drawing operations for a remote component. This is analogous to the
 * [androidx.compose.ui.graphics.painter.Painter] class for remote compose operations. Subclasses of
 * [RemotePainter] are responsible for implementing the [onDraw] method, which defines the drawing
 * operations.
 */
public abstract class RemotePainter @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor() {

    private var paint: RemotePaint? = null

    /** Defines the drawing operations within [RemoteDrawScope]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public abstract fun RemoteDrawScope.onDraw()

    /**
     * The intrinsic size of the painter. This is the size of the painter before any scaling or
     * transformations are applied.
     */
    public abstract val intrinsicSize: RemoteSize?

    /**
     * Lazily create a [RemotePaint] object or return the existing instance if it is already
     * allocated
     */
    private fun obtainPaint(): RemotePaint {
        return paint ?: RemotePaint().also { paint = it }
    }

    private fun configureBlendMode(blendMode: BlendMode?) {
        if (blendMode == null) {
            return
        }
        obtainPaint().blendMode = blendMode
    }

    /** Update the alpha component of RemoteColor. */
    private fun configureAlpha(alpha: RemoteFloat) {
        val color = obtainPaint().remoteColor ?: RemoteColor(Color.Black)
        obtainPaint().remoteColor = color.copy(alpha = alpha)
    }

    /** Returns the size of the component that this painter is drawing on. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun RemoteDrawScope.componentSize(): RemoteSize {
        return remoteSize
    }

    /**
     * The main entry point for drawing. This method is called by the remote compose framework to
     * draw the painter.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun RemoteDrawScope.draw(
        blendMode: android.graphics.BlendMode? = null,
        alpha: RemoteFloat = DefaultAlpha.rf,
    ) {
        configureBlendMode(blendMode)
        configureAlpha(alpha)
        usePaint(obtainPaint()) { onDraw() }
    }
}
