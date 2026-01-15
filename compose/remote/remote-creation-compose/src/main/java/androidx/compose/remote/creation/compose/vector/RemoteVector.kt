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

package androidx.compose.remote.creation.compose.vector

import android.graphics.Paint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.toRemotePath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.DefaultGroupName
import androidx.compose.ui.graphics.vector.DefaultPathName
import androidx.compose.ui.graphics.vector.DefaultPivotX
import androidx.compose.ui.graphics.vector.DefaultPivotY
import androidx.compose.ui.graphics.vector.DefaultRotation
import androidx.compose.ui.graphics.vector.DefaultScaleX
import androidx.compose.ui.graphics.vector.DefaultScaleY
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.graphics.vector.DefaultStrokeLineJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.graphics.vector.DefaultStrokeLineWidth
import androidx.compose.ui.graphics.vector.DefaultTranslationX
import androidx.compose.ui.graphics.vector.DefaultTranslationY
import androidx.compose.ui.graphics.vector.DefaultTrimPathEnd
import androidx.compose.ui.graphics.vector.DefaultTrimPathOffset
import androidx.compose.ui.graphics.vector.DefaultTrimPathStart
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.util.fastForEach

/** DSL for building a vector with [RemotePathBuilder]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun RemotePathData(block: RemotePathBuilder.() -> Unit): List<PathNode> =
    with(RemotePathBuilder()) {
        block()
        nodes
    }

internal sealed class RemoteVNode {
    abstract fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?)
}

internal class RemoteVectorComponent(val root: RemoteGroupComponent) : RemoteVNode() {

    var name: String = DefaultGroupName

    internal var intrinsicColorFilter: RemoteColorFilter? = null
    internal var viewportSize = RemoteSize(24f.rf, 24f.rf)

    private var rootScaleX = 1f.rf
    private var rootScaleY = 1f.rf

    private fun RemoteDrawScope.drawVector(tintFilter: RemoteColorFilter?) {
        with(root) { scale(rootScaleX, rootScaleY) { draw(tintFilter) } }
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        rootScaleX = remoteWidth / viewportSize.width
        rootScaleY = remoteHeight / viewportSize.height
        val targetFilter = colorFilter ?: intrinsicColorFilter
        drawVector(targetFilter)
    }

    override fun toString(): String {
        return buildString {
            append("Params: ")
            append("\tname: ").append(name).append("\n")
            append("\tviewportWidth: ").append(viewportSize.width).append("\n")
            append("\tviewportHeight: ").append(viewportSize.height).append("\n")
        }
    }
}

internal class RemotePathComponent : RemoteVNode() {
    var name = DefaultPathName
    var fill: Brush? = null
    var fillAlpha = 1.0f
    var pathData: List<PathNode> = EmptyPath
    var strokeAlpha = 1.0f
    var strokeLineWidth = DefaultStrokeLineWidth
    var stroke: Brush? = null
    var strokeLineCap = DefaultStrokeLineCap
    var strokeLineJoin = DefaultStrokeLineJoin
    var strokeLineMiter = DefaultStrokeLineMiter
    var trimPathStart = DefaultTrimPathStart
    var trimPathEnd = DefaultTrimPathEnd
    var trimPathOffset = DefaultTrimPathOffset

    private val path = RemotePath()
    private var renderPath = path

    private fun updatePath() {
        // The call below resets the path
        pathData.toRemotePath(path)
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        updatePath()

        val paint = RemotePaint().apply { remoteColorFilter = colorFilter }
        fill?.let {
            paint.style = Paint.Style.FILL
            drawPath(renderPath, paint)
        }
        stroke?.let {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeLineWidth
            paint.strokeMiter = strokeLineMiter
            paint.strokeCap = strokeLineCap.toAndroidCap()
            paint.strokeJoin = strokeLineJoin.toAndroidJoin()
            drawPath(renderPath, paint)
        }
    }

    override fun toString() = path.toString()
}

internal class RemoteGroupComponent : RemoteVNode() {
    private var groupMatrix: Matrix? = null

    private val children = mutableListOf<RemoteVNode>()

    /**
     * Flag to determine if the contents of this group can be rendered with a single color This is
     * true if all the paths and groups within this group can be rendered with the same color
     */
    var isTintable: Boolean = true
        private set

    /**
     * Tint color to render all the contents of this group. This is configured only if all the
     * contents within the group are the same color
     */
    var tintColor: Color = Color.Unspecified
        private set

    /**
     * Helper method to inspect whether the provided brush matches the current color of paths within
     * the group in order to help determine if only an alpha channel bitmap can be allocated and
     * tinted in order to save on memory overhead.
     */
    private fun markTintForBrush(brush: Brush?) {
        if (!isTintable) {
            return
        }
        if (brush != null) {
            if (brush is SolidColor) {
                markTintForColor(brush.value)
            } else {
                // If the brush is not a solid color then we require a explicit ARGB channels in the
                // cached bitmap
                markNotTintable()
            }
        }
    }

    /**
     * Helper method to inspect whether the provided color matches the current color of paths within
     * the group in order to help determine if only an alpha channel bitmap can be allocated and
     * tinted in order to save on memory overhead.
     */
    private fun markTintForColor(color: Color) {
        if (!isTintable) {
            return
        }

        if (color.isSpecified) {
            if (tintColor.isUnspecified) {
                // Initial color has not been specified, initialize the target color to the
                // one provided
                tintColor = color
            } else if (!tintColor.rgbEqual(color)) {
                // The given color does not match the rgb channels if our previous color
                // Therefore we require explicit ARGB channels in the cached bitmap
                markNotTintable()
            }
        }
    }

    private fun markTintForVNode(node: RemoteVNode) {
        if (node is RemotePathComponent) {
            markTintForBrush(node.fill)
            markTintForBrush(node.stroke)
        } else if (node is RemoteGroupComponent) {
            if (node.isTintable && isTintable) {
                markTintForColor(node.tintColor)
            } else {
                markNotTintable()
            }
        }
    }

    private fun markNotTintable() {
        isTintable = false
        tintColor = Color.Unspecified
    }

    // If the name changes we should re-draw as individual nodes could
    // be modified based off of this name parameter.
    var name: String = DefaultGroupName

    var rotation: Float = DefaultRotation

    var pivotX: Float = DefaultPivotX

    var pivotY: Float = DefaultPivotY

    var scaleX: Float = DefaultScaleX

    var scaleY: Float = DefaultScaleY

    var translationX: Float = DefaultTranslationX

    var translationY: Float = DefaultTranslationY

    val numChildren: Int
        get() = children.size

    fun insertAt(index: Int, instance: RemoteVNode) {
        if (index < numChildren) {
            children[index] = instance
        } else {
            children.add(instance)
        }

        markTintForVNode(instance)
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        withTransform({ groupMatrix?.let { transform(it) } }) {
            children.fastForEach { node -> with(node) { this@draw.draw(colorFilter) } }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder().append("VGroup: ").append(name)
        children.fastForEach { node -> sb.append("\t").append(node.toString()).append("\n") }
        return sb.toString()
    }
}

/**
 * helper method to verify if the rgb channels are equal excluding comparison of the alpha channel
 */
internal fun Color.rgbEqual(other: Color) =
    this.red == other.red && this.green == other.green && this.blue == other.blue

internal fun StrokeJoin.toAndroidJoin(): Paint.Join {
    return when (this) {
        StrokeJoin.Miter -> Paint.Join.MITER
        StrokeJoin.Round -> Paint.Join.ROUND
        StrokeJoin.Bevel -> Paint.Join.BEVEL
        else -> Paint.Join.MITER
    }
}

internal fun StrokeCap.toAndroidCap(): Paint.Cap {
    return when (this) {
        StrokeCap.Butt -> Paint.Cap.BUTT
        StrokeCap.Round -> Paint.Cap.ROUND
        StrokeCap.Square -> Paint.Cap.SQUARE
        else -> Paint.Cap.BUTT
    }
}
