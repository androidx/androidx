/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Size.Companion.Unspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import kotlin.math.ceil

const val DefaultGroupName = ""
const val DefaultRotation = 0.0f
const val DefaultPivotX = 0.0f
const val DefaultPivotY = 0.0f
const val DefaultScaleX = 1.0f
const val DefaultScaleY = 1.0f
const val DefaultTranslationX = 0.0f
const val DefaultTranslationY = 0.0f

val EmptyPath = emptyList<PathNode>()

const val DefaultPathName = ""
const val DefaultStrokeLineWidth = 0.0f
const val DefaultStrokeLineMiter = 4.0f
const val DefaultTrimPathStart = 0.0f
const val DefaultTrimPathEnd = 1.0f
const val DefaultTrimPathOffset = 0.0f

val DefaultStrokeLineCap = StrokeCap.Butt
val DefaultStrokeLineJoin = StrokeJoin.Miter
val DefaultTintBlendMode = BlendMode.SrcIn
val DefaultTintColor = Color.Transparent
val DefaultFillType = PathFillType.NonZero

inline fun PathData(block: PathBuilder.() -> Unit) =
    with(PathBuilder()) {
        block()
        nodes
    }

fun addPathNodes(pathStr: String?) =
    if (pathStr == null) {
        EmptyPath
    } else {
        PathParser().parsePathString(pathStr).toNodes()
    }

sealed class VNode {
    /**
     * Callback invoked whenever the node in the vector tree is modified in a way that would change
     * the output of the Vector
     */
    internal open var invalidateListener: ((VNode) -> Unit)? = null

    fun invalidate() {
        invalidateListener?.invoke(this)
    }

    abstract fun DrawScope.draw()
}

internal class VectorComponent(val root: GroupComponent) : VNode() {

    init {
        root.invalidateListener = { doInvalidate() }
    }

    var name: String = DefaultGroupName

    private fun doInvalidate() {
        isDirty = true
        invalidateCallback.invoke()
    }

    private var isDirty = true

    private val cacheDrawScope = DrawCache()

    internal val cacheBitmapConfig: ImageBitmapConfig
        get() = cacheDrawScope.mCachedImage?.config ?: ImageBitmapConfig.Argb8888

    internal var invalidateCallback = {}

    internal var intrinsicColorFilter: ColorFilter? by mutableStateOf(null)

    // Conditional filter used if the vector is all one color. In this case we allocate a
    // alpha8 channel bitmap and tint the result to the desired color
    private var tintFilter: ColorFilter? = null

    internal var viewportSize by mutableStateOf(Size.Zero)

    private var previousDrawSize = Unspecified

    private var rootScaleX = 1f
    private var rootScaleY = 1f

    /** Cached lambda used to avoid allocating the lambda on each draw invocation */
    private val drawVectorBlock: DrawScope.() -> Unit = {
        with(root) { scale(rootScaleX, rootScaleY, pivot = Offset.Zero) { draw() } }
    }

    fun DrawScope.draw(alpha: Float, colorFilter: ColorFilter?) {
        // If the content of the vector has changed, or we are drawing a different size
        // update the cached image to ensure we are scaling the vector appropriately
        val isOneColor = root.isTintable && root.tintColor.isSpecified
        val targetImageConfig =
            if (
                isOneColor &&
                    intrinsicColorFilter.tintableWithAlphaMask() &&
                    colorFilter.tintableWithAlphaMask()
            ) {
                ImageBitmapConfig.Alpha8
            } else {
                ImageBitmapConfig.Argb8888
            }

        if (isDirty || previousDrawSize != size || targetImageConfig != cacheBitmapConfig) {
            tintFilter =
                if (targetImageConfig == ImageBitmapConfig.Alpha8) {
                    ColorFilter.tint(root.tintColor.toOpaque())
                } else {
                    null
                }
            rootScaleX = size.width / viewportSize.width
            rootScaleY = size.height / viewportSize.height
            cacheDrawScope.drawCachedImage(
                targetImageConfig,
                IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
                this@draw,
                layoutDirection,
                drawVectorBlock,
            )
            isDirty = false
            previousDrawSize = size
        }
        val targetFilter =
            if (colorFilter != null) {
                colorFilter
            } else if (intrinsicColorFilter != null) {
                intrinsicColorFilter
            } else {
                tintFilter
            }
        cacheDrawScope.drawInto(this, alpha, targetFilter)
    }

    override fun DrawScope.draw() {
        draw(1.0f, null)
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

internal class PathComponent : VNode() {
    var name = DefaultPathName
        set(value) {
            field = value
            invalidate()
        }

    var fill: Brush? = null
        set(value) {
            field = value
            invalidate()
        }

    var fillAlpha = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var pathData = EmptyPath
        set(value) {
            field = value
            isPathDirty = true
            invalidate()
        }

    var pathFillType = DefaultFillType
        set(value) {
            field = value
            renderPath.fillType = value
            invalidate()
        }

    var strokeAlpha = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var strokeLineWidth = DefaultStrokeLineWidth
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var stroke: Brush? = null
        set(value) {
            field = value
            invalidate()
        }

    var strokeLineCap = DefaultStrokeLineCap
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var strokeLineJoin = DefaultStrokeLineJoin
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var strokeLineMiter = DefaultStrokeLineMiter
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var trimPathStart = DefaultTrimPathStart
        set(value) {
            field = value
            isTrimPathDirty = true
            invalidate()
        }

    var trimPathEnd = DefaultTrimPathEnd
        set(value) {
            field = value
            isTrimPathDirty = true
            invalidate()
        }

    var trimPathOffset = DefaultTrimPathOffset
        set(value) {
            field = value
            isTrimPathDirty = true
            invalidate()
        }

    private var isPathDirty = true
    private var isStrokeDirty = true
    private var isTrimPathDirty = false

    private var strokeStyle: Stroke? = null

    private val path = Path()
    private var renderPath = path

    private var _tmpPath: Path? = null
    private val tmpPath: Path
        get() {
            val localTmp = _tmpPath
            if (localTmp != null) return localTmp

            return Path().also { _tmpPath = it }
        }

    private val pathMeasure: PathMeasure by lazy(LazyThreadSafetyMode.NONE) { PathMeasure() }

    private fun updatePath() {
        // The call below resets the path
        pathData.toPath(path)
        updateRenderPath()
    }

    private fun updateRenderPath() {
        if (trimPathStart == DefaultTrimPathStart && trimPathEnd == DefaultTrimPathEnd) {
            renderPath = path
        } else {
            if (renderPath == path) {
                renderPath = Path()
            } else {
                // Rewind unsets the fill type so reset it here
                val fillType = renderPath.fillType
                renderPath.rewind()
                renderPath.fillType = fillType
            }

            pathMeasure.setPath(path, false)
            val length = pathMeasure.length
            val start = ((trimPathStart + trimPathOffset) % 1f) * length
            val end = ((trimPathEnd + trimPathOffset) % 1f) * length
            if (start > end) {
                val dstPath = tmpPath
                dstPath.reset()
                pathMeasure.getSegment(start, length, dstPath, true)
                renderPath.addPath(dstPath)
                dstPath.reset()
                pathMeasure.getSegment(0f, end, dstPath, true)
                renderPath.addPath(dstPath)
            } else {
                pathMeasure.getSegment(start, end, renderPath, true)
            }
        }
    }

    override fun DrawScope.draw() {
        if (isPathDirty) {
            updatePath()
        } else if (isTrimPathDirty) {
            updateRenderPath()
        }
        isPathDirty = false
        isTrimPathDirty = false

        fill?.let { drawPath(renderPath, brush = it, alpha = fillAlpha) }
        stroke?.let {
            var targetStroke = strokeStyle
            if (isStrokeDirty || targetStroke == null) {
                targetStroke =
                    Stroke(strokeLineWidth, strokeLineMiter, strokeLineCap, strokeLineJoin)
                strokeStyle = targetStroke
                isStrokeDirty = false
            }
            drawPath(renderPath, brush = it, alpha = strokeAlpha, style = targetStroke)
        }
    }

    override fun toString() = path.toString()
}

internal class GroupComponent : VNode() {
    private var groupMatrix: Matrix? = null

    private val children = mutableListOf<VNode>()

    /**
     * Flag to determine if the contents of this group can be rendered with a single color This is
     * true if all the paths and groups within this group can be rendered with the same color
     */
    var isTintable = true
        private set

    /**
     * Tint color to render all the contents of this group. This is configured only if all the
     * contents within the group are the same color
     */
    var tintColor = Color.Unspecified
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

    private fun markTintForVNode(node: VNode) {
        if (node is PathComponent) {
            markTintForBrush(node.fill)
            markTintForBrush(node.stroke)
        } else if (node is GroupComponent) {
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

    var clipPathData = EmptyPath
        set(value) {
            field = value
            isClipPathDirty = true
            invalidate()
        }

    private val willClipPath: Boolean
        get() = clipPathData.isNotEmpty()

    private var isClipPathDirty = true

    private var clipPath: Path? = null

    override var invalidateListener: ((VNode) -> Unit)? = null

    private val wrappedListener: (VNode) -> Unit = { node ->
        markTintForVNode(node)
        invalidateListener?.invoke(node)
    }

    private fun updateClipPath() {
        if (willClipPath) {
            var targetClip = clipPath
            if (targetClip == null) {
                targetClip = Path()
                clipPath = targetClip
            }

            // toPath() will reset the path we send
            clipPathData.toPath(targetClip)
        }
    }

    // If the name changes we should re-draw as individual nodes could
    // be modified based off of this name parameter.
    var name = DefaultGroupName
        set(value) {
            field = value
            invalidate()
        }

    var rotation = DefaultRotation
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var pivotX = DefaultPivotX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var pivotY = DefaultPivotY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var scaleX = DefaultScaleX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var scaleY = DefaultScaleY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var translationX = DefaultTranslationX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var translationY = DefaultTranslationY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    val numChildren: Int
        get() = children.size

    private var isMatrixDirty = true

    private fun updateMatrix() {
        val matrix: Matrix
        val target = groupMatrix
        if (target == null) {
            matrix = Matrix()
            groupMatrix = matrix
        } else {
            matrix = target
            matrix.reset()
        }
        // M = T(translationX + pivotX, translationY + pivotY) *
        //     R(rotation) * S(scaleX, scaleY) *
        //     T(-pivotX, -pivotY)
        matrix.translate(translationX + pivotX, translationY + pivotY)
        matrix.rotateZ(degrees = rotation)
        matrix.scale(scaleX, scaleY, 1f)
        matrix.translate(-pivotX, -pivotY)
    }

    fun insertAt(index: Int, instance: VNode) {
        if (index < numChildren) {
            children[index] = instance
        } else {
            children.add(instance)
        }

        markTintForVNode(instance)

        instance.invalidateListener = wrappedListener
        invalidate()
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from > to) {
            var current = to
            repeat(count) {
                val node = children[from]
                children.removeAt(from)
                children.add(current, node)
                current++
            }
        } else {
            repeat(count) {
                val node = children[from]
                children.removeAt(from)
                children.add(to - 1, node)
            }
        }
        invalidate()
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            if (index < children.size) {
                children[index].invalidateListener = null
                children.removeAt(index)
            }
        }
        invalidate()
    }

    override fun DrawScope.draw() {
        if (isMatrixDirty) {
            updateMatrix()
            isMatrixDirty = false
        }

        if (isClipPathDirty) {
            updateClipPath()
            isClipPathDirty = false
        }

        withTransform({
            groupMatrix?.let { transform(it) }
            val targetClip = clipPath
            if (willClipPath && targetClip != null) {
                clipPath(targetClip)
            }
        }) {
            children.fastForEach { node -> with(node) { this@draw.draw() } }
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

/** helper method to get the opaque version of color */
internal fun Color.toOpaque(): Color = if (this.alpha != 1.0F) this.copy(alpha = 1.0F) else this

/**
 * Helper method to determine if a particular ColorFilter will generate the same output if the
 * bitmap has an Alpha8 or ARGB8888 configuration
 */
internal fun ColorFilter?.tintableWithAlphaMask() =
    if (this is BlendModeColorFilter) {
        this.blendMode == BlendMode.SrcIn || this.blendMode == BlendMode.SrcOver
    } else {
        this == null
    }
