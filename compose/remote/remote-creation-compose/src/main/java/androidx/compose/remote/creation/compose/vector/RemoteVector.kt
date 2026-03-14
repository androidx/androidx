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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.DefaultGroupName
import androidx.compose.remote.creation.compose.capture.DefaultPathName
import androidx.compose.remote.creation.compose.capture.DefaultPivotX
import androidx.compose.remote.creation.compose.capture.DefaultPivotY
import androidx.compose.remote.creation.compose.capture.DefaultRotation
import androidx.compose.remote.creation.compose.capture.DefaultScaleX
import androidx.compose.remote.creation.compose.capture.DefaultScaleY
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineCap
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineJoin
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineMiter
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineWidth
import androidx.compose.remote.creation.compose.capture.DefaultTranslationX
import androidx.compose.remote.creation.compose.capture.DefaultTranslationY
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathEnd
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathOffset
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathStart
import androidx.compose.remote.creation.compose.capture.toRemotePath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/** DSL for building a vector with [RemotePathBuilder]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemotePathData(block: RemotePathBuilder.() -> Unit): List<RemotePathNode> =
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
        rootScaleX = width / viewportSize.width
        rootScaleY = height / viewportSize.height
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
    var fillAlpha = 1.0f.rf
    var pathData: List<RemotePathNode> = androidx.compose.remote.creation.compose.capture.EmptyPath
    var strokeAlpha = 1.0f.rf
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

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        // The call below resets the path
        pathData.toRemotePath(path, this.remoteCanvas.creationState)

        val paint = RemotePaint { this.colorFilter = colorFilter }
        fill?.let {
            paint.style = PaintingStyle.Fill
            drawPath(renderPath, paint)
        }
        stroke?.let {
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = strokeLineWidth
            paint.strokeCap = strokeLineCap
            paint.strokeJoin = strokeLineJoin
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

    var rotation: RemoteFloat = DefaultRotation

    var pivotX: RemoteFloat = DefaultPivotX

    var pivotY: RemoteFloat = DefaultPivotY

    var scaleX: RemoteFloat = DefaultScaleX

    var scaleY: RemoteFloat = DefaultScaleY

    var translationX: RemoteFloat = DefaultTranslationX

    var translationY: RemoteFloat = DefaultTranslationY

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

internal fun List<PathNode>.toRemotePathNodes(): List<RemotePathNode> {
    return this.fastMap { it.toRemotePathNode() }
}

private fun PathNode.toRemotePathNode(): RemotePathNode {
    return when (this) {
        is PathNode.MoveTo -> RemotePathNode.MoveTo(this.x.rf, this.y.rf)
        is PathNode.LineTo -> RemotePathNode.LineTo(this.x.rf, this.y.rf)
        is PathNode.CurveTo ->
            RemotePathNode.CurveTo(
                this.x1.rf,
                this.y1.rf,
                this.x2.rf,
                this.y2.rf,
                this.x3.rf,
                this.y3.rf,
            )
        is PathNode.RelativeMoveTo -> RemotePathNode.RelativeMoveTo(this.dx.rf, this.dy.rf)
        is PathNode.QuadTo -> RemotePathNode.QuadTo(this.x1.rf, this.y1.rf, this.x2.rf, this.y2.rf)
        is PathNode.ArcTo ->
            RemotePathNode.ArcTo(
                this.horizontalEllipseRadius.rf,
                this.verticalEllipseRadius.rf,
                this.theta.rf,
                this.isMoreThanHalf.rb,
                this.isPositiveArc.rb,
                this.arcStartX.rf,
                this.arcStartY.rf,
            )
        is PathNode.HorizontalTo -> RemotePathNode.HorizontalTo(this.x.rf)
        is PathNode.VerticalTo -> RemotePathNode.VerticalTo(this.y.rf)
        is PathNode.Close -> RemotePathNode.Close
        is PathNode.RelativeHorizontalTo -> RemotePathNode.RelativeHorizontalTo(this.dx.rf)
        is PathNode.RelativeVerticalTo -> RemotePathNode.RelativeVerticalTo(this.dy.rf)
        is PathNode.ReflectiveQuadTo -> RemotePathNode.ReflectiveQuadTo(this.x.rf, this.y.rf)
        is PathNode.ReflectiveCurveTo ->
            RemotePathNode.ReflectiveCurveTo(this.x1.rf, this.y1.rf, this.x2.rf, this.y2.rf)
        is PathNode.RelativeArcTo ->
            RemotePathNode.RelativeArcTo(
                this.horizontalEllipseRadius.rf,
                this.verticalEllipseRadius.rf,
                this.theta.rf,
                this.isMoreThanHalf.rb,
                this.isPositiveArc.rb,
                this.arcStartDx.rf,
                this.arcStartDy.rf,
            )
        is PathNode.RelativeCurveTo ->
            RemotePathNode.RelativeCurveTo(
                this.dx1.rf,
                this.dy1.rf,
                this.dx2.rf,
                this.dy2.rf,
                this.dx3.rf,
                this.dy3.rf,
            )
        is PathNode.RelativeLineTo -> RemotePathNode.RelativeLineTo(this.dx.rf, this.dy.rf)
        is PathNode.RelativeQuadTo ->
            RemotePathNode.RelativeQuadTo(this.dx1.rf, this.dy1.rf, this.dx2.rf, this.dy2.rf)
        is PathNode.RelativeReflectiveCurveTo ->
            RemotePathNode.RelativeReflectiveCurveTo(
                this.dx1.rf,
                this.dy1.rf,
                this.dx2.rf,
                this.dy2.rf,
            )
        is PathNode.RelativeReflectiveQuadTo ->
            RemotePathNode.RelativeReflectiveQuadTo(this.dx.rf, this.dy.rf)
    }
}
