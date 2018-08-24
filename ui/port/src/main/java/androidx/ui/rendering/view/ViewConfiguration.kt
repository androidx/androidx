package androidx.ui.rendering.view

import androidx.ui.engine.geometry.Size
import androidx.ui.vectormath64.Matrix4

// / The layout constraints for the root render object.
// / By default, the view has zero [size] and a [devicePixelRatio] of 1.0.
// @immutable
data class ViewConfiguration(
    val size: Size = Size.zero, // / The size of the output surface.
    val devicePixelRatio: Double = 1.0 // / The pixel density of the output surface.
) {

    // / Creates a transformation matrix that applies the [devicePixelRatio].
    fun toMatrix(): Matrix4 {
        return Matrix4.diagonal3Values(devicePixelRatio.toFloat(), devicePixelRatio.toFloat(), 1f)
    }

    override fun toString(): String {
        return "$size at ${devicePixelRatio}x"
    }
}