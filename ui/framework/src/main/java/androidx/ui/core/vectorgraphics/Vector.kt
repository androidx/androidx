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

package androidx.ui.core.vectorgraphics

import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Canvas
import androidx.ui.painting.Image
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Emittable
import androidx.compose.composer
import androidx.ui.painting.withSave
import androidx.ui.painting.Path as PaintingPath

const val DefaultGroupName = ""
const val DefaultRotate = 0.0f
const val DefaultPivotX = 0.0f
const val DefaultPivotY = 0.0f
const val DefaultScaleX = 1.0f
const val DefaultScaleY = 1.0f
const val DefaultTranslateX = 0.0f
const val DefaultTranslateY = 0.0f

val EmptyPath = emptyArray<PathNode>()

/**
 * paint used to draw the cached vector graphic to the provided canvas
 * TODO (njawad) Can we update the Crane Canvas API to make this paint optional?
 */
internal val EmptyPaint = Paint()

class PathDelegate(val delegate: PathBuilder.() -> Unit)

// TODO figure out how to use UNIONTYPE with a Lambda receiver. Cannot cast to KClass which is what
// UnionType is expecting
// TODO uncomment usage of UnionType when Compose can be accessed across modules
typealias PathData = /*@UnionType(String::class, PathDelegate::class, Array<PathNode>::class)*/ Any?

// TODO (njawad) change to color int
typealias BrushType = /*@UnionType(Int::class, Brush::class)*/ Any

const val DefaultPathName = ""
const val DefaultAlpha = 1.0f
const val DefaultStrokeLineWidth = 0.0f
const val DefaultStrokeLineMiter = 4.0f

val DefaultStrokeLineCap = StrokeCap.butt
val DefaultStrokeLineJoin = StrokeJoin.miter

fun addPathNodes(pathStr: String?): Array<PathNode> =
    if (pathStr == null) {
        EmptyPath
    } else {
        PathParser().parsePathString(pathStr).toNodes()
    }

@Composable
fun vector(
    name: String = "",
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = viewportWidth,
    defaultHeight: Float = viewportHeight,
    @Children children: @Composable() () -> Unit
) {
    <Vector name defaultWidth defaultHeight viewportWidth viewportHeight>
        children()
    </Vector>
}

@Composable
fun group(
    name: String = DefaultGroupName,
    rotate: Float = DefaultRotate,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translateX: Float = DefaultTranslateX,
    translateY: Float = DefaultTranslateY,
    clipPathData: PathData = EmptyPath,
    @Children childNodes: @Composable() () -> Unit
) {

    val clipPathNodes = createPath(clipPathData)
    <Group
        name
        rotate
        pivotX
        pivotY
        scaleX
        scaleY
        translateX
        translateY
        clipPathNodes>
        childNodes()
    </Group>
}

@Composable
fun path(
    pathData: PathData,
    name: String = DefaultPathName,
    fill: BrushType = EmptyBrush,
    fillAlpha: Float = DefaultAlpha,
    stroke: BrushType = EmptyBrush,
    strokeAlpha: Float = DefaultAlpha,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter
) {
    val pathNodes = createPath(pathData)
    val fillBrush: Brush = obtainBrush(fill)
    val strokeBrush: Brush = obtainBrush(stroke)

    <Path
        name
        pathNodes
        fill=fillBrush
        fillAlpha
        stroke=strokeBrush
        strokeAlpha
        strokeLineWidth
        strokeLineCap
        strokeLineJoin
        strokeLineMiter />
}

private sealed class VNode {
    abstract fun draw(canvas: Canvas)
}

private class Vector(
    val name: String = "",
    val viewportWidth: Float,
    val viewportHeight: Float,
    val defaultWidth: Float = viewportWidth,
    val defaultHeight: Float = viewportHeight
) : VNode(), Emittable {

    private val root = Group(this@Vector.name).apply {
        pivotX = 0.0f
        pivotY = 0.0f
        scaleX = defaultWidth / viewportWidth
        scaleY = defaultHeight / viewportHeight
    }

    /**
     * Cached Image of the Vector Graphic to be re-used across draw calls
     * if the Vector graphic is not dirty
     */
    // TODO (njawad) add invalidation logic to re-draw into the offscreen Image
    private var cachedImage: Image? = null

    val size: Int
        get() = root.size

    override fun draw(canvas: Canvas) {
        var targetImage = cachedImage
        if (targetImage == null) {
            targetImage = Image(
                kotlin.math.ceil(defaultWidth).toInt(),
                kotlin.math.ceil(defaultHeight).toInt()
            )
            cachedImage = targetImage
            root.draw(Canvas(targetImage))
        }
        canvas.drawImage(targetImage, Offset.zero, EmptyPaint)
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

    override fun emitInsertAt(index: Int, instance: Emittable) {
        root.emitInsertAt(index, instance)
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        root.emitMove(from, to, count)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        root.emitRemoveAt(index, count)
    }
}

private class Path(val name: String) : VNode(), Emittable {

    var fill: Brush = EmptyBrush
        set(value) {
            field = value
            updateFillPaint {
                field.applyBrush(this)
            }
        }

    var fillAlpha: Float = DefaultAlpha
        set(value) {
            field = value
            updateFillPaint {
                alpha = field
            }
        }

    var pathNodes: Array<PathNode> = emptyArray()
        set(value) {
            field = value
            isPathDirty = true
        }

    var strokeAlpha: Float = DefaultAlpha
        set(value) {
            field = value
            updateStrokePaint {
                alpha = field
            }
        }

    var strokeLineWidth: Float = DefaultStrokeLineWidth
        set(value) {
            field = value
            updateStrokePaint {
                strokeWidth = field
            }
        }

    var stroke: Brush = EmptyBrush
        set(value) {
            field = value
            updateStrokePaint {
                field.applyBrush(this)
            }
        }

    var strokeLineCap: StrokeCap = DefaultStrokeLineCap
        set(value) {
            field = value
            updateStrokePaint {
                strokeCap = field
            }
        }

    var strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin
        set(value) {
            field = value
            updateStrokePaint {
                strokeJoin = field
            }
        }

    var strokeLineMiter: Float = DefaultStrokeLineMiter
        set(value) {
            field = value
            updateStrokePaint {
                strokeMiterLimit = field
            }
        }

    private var isPathDirty = true

    private val path = PaintingPath()

    private var fillPaint: Paint? = null
    private var strokePaint: Paint? = null

    private val parser = PathParser()

    private fun updateStrokePaint(strokePaintUpdater: Paint.() -> Unit) {
        if (strokePaint == null) {
            strokePaint = createStrokePaint()
        } else {
            strokePaint?.strokePaintUpdater()
        }
    }

    private fun createStrokePaint(): Paint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.stroke
        alpha = strokeAlpha
        strokeWidth = strokeLineWidth
        strokeCap = strokeLineCap
        strokeJoin = strokeLineJoin
        strokeMiterLimit = strokeLineMiter
        stroke.applyBrush(this)
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
        alpha = fillAlpha
        style = PaintingStyle.fill
        fill.applyBrush(this)
    }

    private fun updatePath() {
        parser.clear()
        path.reset()
        parser.addPathNodes(pathNodes).toPath(path)
    }

    override fun draw(canvas: Canvas) {
        if (isPathDirty) {
            updatePath()
            isPathDirty = false
        }

        val fillBrush = fill
        if (fillBrush !== EmptyBrush) {
            var targetFillPaint = fillPaint
            if (targetFillPaint == null) {
                targetFillPaint = createFillPaint()
                fillPaint = targetFillPaint
            }
            canvas.drawPath(path, targetFillPaint)
        }

        val strokeBrush = stroke
        if (strokeBrush !== EmptyBrush) {
            var targetStrokePaint = strokePaint
            if (targetStrokePaint == null) {
                targetStrokePaint = createStrokePaint()
                strokePaint = targetStrokePaint
            }
            canvas.drawPath(path, targetStrokePaint)
        }
    }

    override fun emitInsertAt(index: Int, instance: Emittable) {
        throw IllegalArgumentException("Unable to insert Emittable into Path")
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        throw IllegalArgumentException("Unable to move Emittable within Path")
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        throw IllegalArgumentException("Unable to remove Emittable from Path")
    }

    override fun toString(): String {
        return path.toString()
    }
}

private class Group(val name: String = DefaultGroupName) : VNode(), Emittable {

    private var groupMatrix: Matrix? = null

    private val children = mutableListOf<VNode>()

    var clipPathNodes: Array<PathNode> = EmptyPath
        set(value) {
            field = value
            isClipPathDirty = true
        }

    private val willClipPath: Boolean
        get() = clipPathNodes.isNotEmpty()

    private var isClipPathDirty = true

    private var clipPath: PaintingPath? = null
    private var parser: PathParser? = null

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
                targetClip = PaintingPath()
                clipPath = targetClip
            } else {
                targetClip.reset()
            }

            targetParser.addPathNodes(clipPathNodes).toPath(targetClip)
        }
    }

    var rotate: Float = DefaultRotate
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var pivotX: Float = DefaultPivotX
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var pivotY: Float = DefaultPivotY
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var scaleX: Float = DefaultScaleX
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var scaleY: Float = DefaultScaleY
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var translateX: Float = DefaultTranslateX
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var translateY: Float = DefaultTranslateY
        set(value) {
            field = value
            isMatrixDirty = true
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
            postRotate(rotate, 0f, 0f)
            postTranslate(translateX + pivotX,
                translateY + pivotY)
        }
    }

    override fun emitInsertAt(index: Int, instance: Emittable) {
        if (instance is VNode) {
            if (index < size) {
                children[index] = instance
            } else {
                children.add(instance)
            }
        }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
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
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        repeat(count) {
            children.removeAt(index)
        }
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

            for (node in children) {
                node.draw(canvas)
            }
        }
    }

    val size: Int
        get() = children.size

    override fun toString(): String {
        val sb = StringBuilder().append("VGroup: ").append(name)
        for (node in children) {
            sb.append("\t").append(node.toString()).append("\n")
        }
        return sb.toString()
    }
}

private fun createPath(pathData: PathData): Array<PathNode> {
    @Suppress("UNCHECKED_CAST")
    return when (pathData) {
        is Array<*> -> pathData as Array<PathNode>
        is PathDelegate -> {
            with(PathBuilder()) {
                pathData.delegate(this)
                getNodes()
            }
        }
        else -> throw IllegalArgumentException("Must be array of PathNodes or PathDelegate")
    }
}

// Temporary glue logic to wrap a Vector asset into an ImageView
fun adoptVectorGraphic(parent: Any?, child: Any?): View? {
    return if (parent is ViewGroup && child is Vector) {
        val imageView = ImageView(parent.context)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageDrawable(VectorGraphicDrawable(child))
        imageView
    } else {
        null
    }
}

private class VectorGraphicDrawable(private val vector: Vector) : Drawable() {

    override fun getIntrinsicWidth(): Int = Math.round(vector.defaultWidth)

    override fun getIntrinsicHeight(): Int = Math.round(vector.defaultHeight)

    override fun draw(canvas: android.graphics.Canvas) {
        vector.draw(androidx.ui.painting.Canvas(canvas))
    }

    override fun setAlpha(alpha: Int) {
        // TODO support modifying alpha for root of tree down to each node
        TODO("not implemented")
    }

    override fun getOpacity(): Int = PixelFormat.UNKNOWN

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // TODO support color tinting
        TODO("not implemented")
    }
}
