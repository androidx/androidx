package androidx.ui.vectorgraphics

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Canvas
import androidx.ui.painting.Image
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.skia.SkMatrix

const val DEFAULT_GROUP_NAME = ""
const val DEFAULT_ROTATE = 0.0f
const val DEFAULT_PIVOT_X = 0.0f
const val DEFAULT_PIVOT_Y = 0.0f
const val DEFAULT_SCALE_X = 1.0f
const val DEFAULT_SCALE_Y = 1.0f
const val DEFAULT_TRANSLATE_X = 0.0f
const val DEFAULT_TRANSLATE_Y = 0.0f

val EMPTY_PATH = emptyArray<PathNode>()

/**
 * paint used to draw the cached vector graphic to the provided canvas
 * TODO (njawad) Can we update the Crane Canvas API to make this paint optional?
 */
private val EMPTY_PAINT = Paint()

// TODO (njawad) merge VNode into R4A equivalent once IR metadata issues are resolved
sealed class VNode {
    abstract fun draw(canvas: Canvas)
}

// TODO (njawad) merge VectorGraphic into R4A equivalent once IR metadata issues are resolved
class VectorGraphic(
    val name: String,
    val width: Float,
    val height: Float,
    val viewportWidth: Float,
    val viewportHeight: Float
) : VNode() {

    private val root = VGroup(this@VectorGraphic.name).apply {
        pivotX = 0.0f
        pivotY = 0.0f
        scaleX = width / viewportWidth
        scaleY = height / viewportHeight
    }

    /**
     * Cached Image of the Vector Graphic to be re-used across draw calls
     * if the Vector graphic is not dirty
     */
    // TODO (njawad) add invalidation logic to re-draw into the offscreen bitmap
    private var cachedImage: Image? = null

    val size: Int
        get() = root.size

    fun insert(index: Int, node: VNode) {
        root.insertAt(index, node)
    }

    fun move(from: Int, to: Int, count: Int) {
        root.move(from, to, count)
    }

    fun removeAt(index: Int, count: Int) {
        root.removeAt(index, count)
    }

    override fun draw(canvas: Canvas) {
        var targetImage = cachedImage
        if (targetImage == null) {
            val bitmap = Bitmap.createBitmap(kotlin.math.ceil(width).toInt(),
                kotlin.math.ceil(height).toInt(), Bitmap.Config.ARGB_8888)
            targetImage = Image(bitmap)
            cachedImage = targetImage
            root.draw(Canvas(android.graphics.Canvas(bitmap)))
        }
        canvas.drawImage(targetImage, Offset.zero, EMPTY_PAINT)
    }

    override fun toString(): String {
        return buildString {
            append("Params: ")
            append("\tname: ").append(name).append("\n")
            append("\twidth: ").append(width).append("\n")
            append("\theight: ").append(height).append("\n")
            append("\tviewportWidth: ").append(viewportWidth).append("\n")
            append("\tviewportHeight: ").append(viewportHeight).append("\n")
        }
    }
}

// TODO (njawad) merge VGroup into R4A equivalent once IR metadata issues are resolved
class VGroup(val name: String = DEFAULT_GROUP_NAME) : VNode() {

    private var groupMatrix: SkMatrix? = null

    private val children = mutableListOf<VNode>()

    var clipPathNodes: Array<PathNode> = EMPTY_PATH
        set(value) {
            field = value
            isClipPathDirty = true
        }

    private val willClipPath: Boolean
        get() = clipPathNodes.isNotEmpty()

    private var isClipPathDirty = true

    private var clipPath: Path? = null
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
                targetClip = Path()
                clipPath = targetClip
            } else {
                targetClip.reset()
            }

            targetParser.parsePathNodes(clipPathNodes).toPath(targetClip)
        }
    }

    var rotate: Float = DEFAULT_ROTATE
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var pivotX: Float = DEFAULT_PIVOT_X
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var pivotY: Float = DEFAULT_PIVOT_Y
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var scaleX: Float = DEFAULT_SCALE_X
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var scaleY: Float = DEFAULT_SCALE_Y
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var translateX: Float = DEFAULT_TRANSLATE_X
        set(value) {
            field = value
            isMatrixDirty = true
        }

    var translateY: Float = DEFAULT_TRANSLATE_Y
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
            groupMatrix = SkMatrix(matrix)
        } else {
            matrix = target.frameworkMatrix
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

    fun insertAt(index: Int, instance: VNode) {
        if (index < size) {
            children[index] = instance
        } else {
            children.add(instance)
        }
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
    }

    fun removeAt(index: Int, count: Int) {
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

        with(canvas) {
            save()

            val targetClip = clipPath
            if (willClipPath && targetClip != null) {
                canvas.clipPath(targetClip)
            }

            val matrix = groupMatrix
            if (matrix != null) {
                toFrameworkCanvas().concat(matrix.frameworkMatrix)
            }

            for (node in children) {
                node.draw(this)
            }
            restore()
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

class PathDelegate(val delegate: PathBuilder.() -> Unit)

// TODO figure out how to use UNIONTYPE with a Lambda receiver. Cannot cast to KClass which is what
// UnionType is expecting
// TODO uncomment usage of UnionType when R4A can be accessed across modules
typealias PathData = /*@UnionType(String::class, PathDelegate::class)*/ Any

// TODO (njawad) change to color int
typealias BrushType = /*@UnionType(Int::class, Brush::class)*/ Any

const val DEFAULT_PATH_NAME = ""
const val DEFAULT_ALPHA = 1.0f
const val DEFAULT_STROKE_LINE_WIDTH = 0.0f
const val DEFAULT_STROKE_LINE_MITER = 4.0f

val DEFAULT_STROKE_LINE_CAP = StrokeCap.butt
val DEFAULT_STROKE_LINE_JOIN = StrokeJoin.miter

// TODO (njawad) merge VPath into R4A equivalent once IR metadata issues are resolved
class VPath(val name: String = DEFAULT_PATH_NAME) : VNode() {

    var fill: Brush? = null
        set(value) {
            field = value
            updateFillPaint {
                field?.applyBrush(this)
            }
        }

    var fillAlpha: Float = DEFAULT_ALPHA
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

    var strokeAlpha: Float = DEFAULT_ALPHA
        set(value) {
            field = value
            updateStrokePaint {
                alpha = field
            }
        }

    var strokeLineWidth: Float = DEFAULT_STROKE_LINE_WIDTH
        set(value) {
            field = value
            updateStrokePaint {
                strokeWidth = field
            }
        }

    var stroke: Brush? = null
        set(value) {
            field = value
            updateStrokePaint {
                field?.applyBrush(this)
            }
        }

    var strokeLineCap: StrokeCap = DEFAULT_STROKE_LINE_CAP
        set(value) {
            field = value
            updateStrokePaint {
                strokeCap = field
            }
        }

    var strokeLineJoin: StrokeJoin = DEFAULT_STROKE_LINE_JOIN
        set(value) {
            field = value
            updateStrokePaint {
                strokeJoin = field
            }
        }

    var strokeLineMiter: Float = DEFAULT_STROKE_LINE_MITER
        set(value) {
            field = value
            updateStrokePaint {
                strokeMiterLimit = field
            }
        }

    private var isPathDirty = true

    private val path = Path()

    private var fillPaint: Paint? = null
    private var strokePaint: Paint? = null

    private val parser = PathParser()

    private fun updateStrokePaint(strokePaintUpdater: Paint.() -> Unit) {
        val targetStroke = stroke
        if (targetStroke != null) {
            if (strokePaint == null) {
                strokePaint = createStrokePaint()
            } else {
                strokePaint?.strokePaintUpdater()
            }
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
        stroke?.applyBrush(this)
    }

    private fun updateFillPaint(fillPaintUpdater: Paint.() -> Unit) {
        val targetFill = fill
        if (targetFill != null) {
            if (fillPaint == null) {
                fillPaint = createFillPaint()
            } else {
                fillPaint?.fillPaintUpdater()
            }
        }
    }

    private fun createFillPaint(): Paint = Paint().apply {
        isAntiAlias = true
        alpha = fillAlpha
        style = PaintingStyle.fill
        fill?.applyBrush(this)
    }

    private fun updatePath() {
        parser.clear()
        path.reset()
        parser.parsePathNodes(pathNodes).toPath(path)
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
        return buildString {
            append("VPath: ").append(name).append(pathNodes)
        }
    }
}