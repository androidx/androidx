package androidx.ui.rendering.view

import androidx.ui.engine.geometry.Size
import androidx.ui.vectormath64.Matrix4

/**
 * The layout constraints for the root render object.
 * By default, the view has zero [size] and a [devicePixelRatio] of 1.0.
 */
// @immutable
data class ViewConfiguration(
    val size: Size = Size.zero, // / The size of the output surface.
    val devicePixelRatio: Float = 1.0f // / The pixel density of the output surface.
) {

    /** Creates a transformation matrix that applies the [devicePixelRatio]. */
    fun toMatrix(): Matrix4 {
        // TODO("Migration|Andrey: Flutter uses DPs for drawing on canvas, but we use pixels now")
//        return Matrix4.diagonal3Values(devicePixelRatio, devicePixelRatio, 1.0f)
        return Matrix4.diagonal3Values(1.0f, 1.0f, 1.0f)
    }

    override fun toString(): String {
        return "$size at ${devicePixelRatio}x"
    }
}
