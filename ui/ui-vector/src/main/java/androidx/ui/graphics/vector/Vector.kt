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

package androidx.ui.graphics.vector

import android.graphics.Matrix
import androidx.ui.geometry.Offset
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.Path
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.withSave
import androidx.ui.util.fastForEach
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

/**
 * paint used to draw the cached vector graphic to the provided canvas
 */
// TODO (njawad) Can we update the Compose Canvas API to make this paint optional?
internal val EmptyPaint = Paint()

inline fun PathData(block: PathBuilder.() -> Unit): List<PathNode> =
    with(PathBuilder()) {
        block()
        getNodes()
    }

const val DefaultPathName = ""
const val DefaultAlpha = 1.0f
const val DefaultStrokeLineWidth = 0.0f
const val DefaultStrokeLineMiter = 4.0f

val DefaultStrokeLineCap = StrokeCap.butt
val DefaultStrokeLineJoin = StrokeJoin.miter
val DefaultTintBlendMode = BlendMode.srcIn
val DefaultTintColor = Color.Transparent

fun addPathNodes(pathStr: String?): List<PathNode> =
    if (pathStr == null) {
        EmptyPath
    } else {
        PathParser().parsePathString(pathStr).toNodes()
    }

sealed class VNode {

    /**
     * Callback invoked whenever the node in the vector tree is modified in a way that would
     * change the output of the Vector
     */
    // TODO (b/144849567) don't make this publicly accessible after the vector graphics modules
    //  are merged
    open var invalidateListener: (() -> Unit)? = null

    fun invalidate() {
        invalidateListener?.invoke()
    }

    abstract fun draw(canvas: Canvas)
}

class VectorComponent(
    var viewportWidth: Float,
    var viewportHeight: Float,
    var defaultWidth: Float,
    var defaultHeight: Float,
    val name: String = ""
) : VNode() {

    val root = GroupComponent(this@VectorComponent.name).apply {
        pivotX = 0.0f
        pivotY = 0.0f
        scaleX = defaultWidth / viewportWidth
        scaleY = defaultHeight / viewportHeight
        invalidateListener = {
            isDirty = true
        }
    }

    private var isDirty: Boolean = true

    private var vectorPaint: Paint? = null

    /**
     * Cached Image of the Vector Graphic to be re-used across draw calls
     * if the Vector graphic is not dirty
     */
    // TODO (njawad) add invalidation logic to re-draw into the offscreen Image
    private var cachedImage: ImageAsset? = null

    val size: Int
        get() = root.size

    fun draw(canvas: Canvas, alpha: Float, colorFilter: ColorFilter?) {
        var targetImage = cachedImage
        if (targetImage == null) {
            targetImage = ImageAsset(
                ceil(defaultWidth).toInt(),
                ceil(defaultHeight).toInt()
            )
            cachedImage = targetImage
        }
        if (isDirty) {
            root.draw(Canvas(targetImage))
            isDirty = false
        }
        canvas.drawImage(targetImage, Offset.zero, obtainVectorPaint(alpha, colorFilter))
    }

    override fun draw(canvas: Canvas) {
        draw(canvas, DefaultAlpha, null)
    }

    override fun toString(): String {
        return buildString {
            append("Params: ")
            append("\tname: ").append(name).append("\n")
            append("\twidth: ").append(defaultWidth).append("\n")
            append("\theight: ").append(defaultHeight).append("\n")
            append("\tviewportWidth: ").append(viewportWidth).append("\n")
            append("\tviewportHeight: ").append(viewportHeight).append("\n")
        }
    }

    private fun obtainVectorPaint(alpha: Float, colorFilter: ColorFilter?): Paint {
        return if (colorFilter == null && alpha == DefaultAlpha) {
            EmptyPaint
        } else {
            val targetPaint = vectorPaint ?: Paint().also { vectorPaint = it }
            val currentColorFilter = targetPaint.colorFilter
            if (currentColorFilter != colorFilter) {
                targetPaint.colorFilter = colorFilter
            }
            if (targetPaint.alpha != alpha) {
                targetPaint.alpha = alpha
            }
            targetPaint
        }
    }
}

data class PathComponent(val name: String) : VNode() {

    var fill: Brush? = null
        set(value) {
            if (field != value) {
                field = value
                updateFillPaint {
                    field?.applyTo(this, fillAlpha)
                }
                invalidate()
            }
        }

    var fillAlpha: Float = DefaultAlpha
        set(value) {
            if (field != value) {
                field = value
                updateFillPaint {
                    alpha = field
                }
                invalidate()
            }
        }

    var pathData: List<PathNode> = EmptyPath
        set(value) {
            if (field != value) {
                field = value
                isPathDirty = true
                invalidate()
            }
        }

    var strokeAlpha: Float = DefaultAlpha
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    alpha = field
                }
                invalidate()
            }
        }

    var strokeLineWidth: Float = DefaultStrokeLineWidth
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    strokeWidth = field
                }
                invalidate()
            }
        }

    var stroke: Brush? = null
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    field?.applyTo(this, strokeAlpha)
                }
                invalidate()
            }
        }

    var strokeLineCap: StrokeCap = DefaultStrokeLineCap
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    strokeCap = field
                }
                invalidate()
            }
        }

    var strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    strokeJoin = field
                }
                invalidate()
            }
        }

    var strokeLineMiter: Float = DefaultStrokeLineMiter
        set(value) {
            if (field != value) {
                field = value
                updateStrokePaint {
                    strokeMiterLimit = field
                }
                invalidate()
            }
        }

    private var isPathDirty = true

    private val path = Path()

    private var fillPaint: Paint? = null
    private var strokePaint: Paint? = null

    private val parser = PathParser()

    private inline fun updateStrokePaint(strokePaintUpdater: Paint.() -> Unit) {
        if (strokePaint == null) {
            strokePaint = createStrokePaint()
        } else {
            strokePaint?.strokePaintUpdater()
        }
    }

    private fun createStrokePaint(): Paint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.stroke
        strokeWidth = strokeLineWidth
        strokeCap = strokeLineCap
        strokeJoin = strokeLineJoin
        strokeMiterLimit = strokeLineMiter
        stroke?.applyTo(this, strokeAlpha)
    }

    private fun updateFillPaint(fillPaintUpdater: Paint.() -> Unit) {
        if (fillPaint == null) {
            fillPaint = createFillPaint()
        } else {
            fillPaint?.fillPaintUpdater()
        }
    }

    private fun createFillPaint(): Paint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.fill
        fill?.applyTo(this, fillAlpha)
    }

    private fun updatePath() {
        parser.clear()
        path.reset()
        parser.addPathNodes(pathData).toPath(path)
    }

    override fun draw(canvas: Canvas) {
        if (isPathDirty) {
            updatePath()
            isPathDirty = false
        }

        val fillBrush = fill
        if (fillBrush != null) {
            var targetFillPaint = fillPaint
            if (targetFillPaint == null) {
                targetFillPaint = createFillPaint()
                fillPaint = targetFillPaint
            }
            canvas.drawPath(path, targetFillPaint)
        }

        val strokeBrush = stroke
        if (strokeBrush != null) {
            var targetStrokePaint = strokePaint
            if (targetStrokePaint == null) {
                targetStrokePaint = createStrokePaint()
                strokePaint = targetStrokePaint
            }
            canvas.drawPath(path, targetStrokePaint)
        }
    }

    override fun toString(): String {
        return path.toString()
    }
}

data class GroupComponent(val name: String = DefaultGroupName) : VNode() {

    private var groupMatrix: Matrix? = null

    private val children = mutableListOf<VNode>()

    var clipPathData: List<PathNode> = EmptyPath
        set(value) {
            if (field != value) {
                field = value
                isClipPathDirty = true
                invalidate()
            }
        }

    private val willClipPath: Boolean
        get() = clipPathData.isNotEmpty()

    private var isClipPathDirty = true

    private var clipPath: Path? = null
    private var parser: PathParser? = null

    override var invalidateListener: (() -> Unit)? = null
        set(value) {
            field = value
            children.fastForEach { child ->
                child.invalidateListener = value
            }
        }

    private fun updateClipPath() {
        if (willClipPath) {
            var targetParser = parser
            if (targetParser == null) {
                targetParser = PathParser()
                parser = targetParser
            } else {
                targetParser.clear()
            }

            var targetClip = clipPath
            if (targetClip == null) {
                targetClip = Path()
                clipPath = targetClip
            } else {
                targetClip.reset()
            }

            targetParser.addPathNodes(clipPathData).toPath(targetClip)
        }
    }

    var rotation: Float = DefaultRotation
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var pivotX: Float = DefaultPivotX
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var pivotY: Float = DefaultPivotY
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var scaleX: Float = DefaultScaleX
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var scaleY: Float = DefaultScaleY
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var translationX: Float = DefaultTranslationX
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    var translationY: Float = DefaultTranslationY
        set(value) {
            if (field != value) {
                field = value
                isMatrixDirty = true
                invalidate()
            }
        }

    private var isMatrixDirty = true

    private fun updateMatrix() {
        val matrix: Matrix
        val target = groupMatrix
        if (target == null) {
            matrix = Matrix()
            groupMatrix = matrix
        } else {
            matrix = target
        }
        with(matrix) {
            reset()
            postTranslate(-pivotX, -pivotY)
            postScale(scaleX, scaleY)
            postRotate(rotation, 0f, 0f)
            postTranslate(translationX + pivotX,
                translationY + pivotY)
        }
    }

    fun insertAt(index: Int, instance: VNode) {
        if (index < size) {
            children[index] = instance
        } else {
            children.add(instance)
        }
        instance.invalidateListener = invalidateListener
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
            children[index].invalidateListener = null
            children.removeAt(index)
        }
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (isMatrixDirty) {
            updateMatrix()
            isMatrixDirty = false
        }

        if (isClipPathDirty) {
            updateClipPath()
            isClipPathDirty = false
        }

        canvas.withSave {
            val targetClip = clipPath
            if (willClipPath && targetClip != null) {
                canvas.clipPath(targetClip)
            }

            val matrix = groupMatrix
            if (matrix != null) {
                // TODO (njawad) add concat support to matrix
                canvas.nativeCanvas.concat(matrix)
            }

            children.fastForEach { node ->
                node.draw(canvas)
            }
        }
    }

    val size: Int
        get() = children.size

    override fun toString(): String {
        val sb = StringBuilder().append("VGroup: ").append(name)
        children.fastForEach { node ->
            sb.append("\t").append(node.toString()).append("\n")
        }
        return sb.toString()
    }
}
