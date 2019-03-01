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
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.ui.painting.Canvas
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.vectorgraphics.Brush
import androidx.ui.vectorgraphics.BrushType
import androidx.ui.vectorgraphics.DEFAULT_ALPHA
import androidx.ui.vectorgraphics.DEFAULT_GROUP_NAME
import androidx.ui.vectorgraphics.DEFAULT_PATH_NAME
import androidx.ui.vectorgraphics.DEFAULT_PIVOT_X
import androidx.ui.vectorgraphics.DEFAULT_PIVOT_Y
import androidx.ui.vectorgraphics.DEFAULT_ROTATE
import androidx.ui.vectorgraphics.DEFAULT_SCALE_X
import androidx.ui.vectorgraphics.DEFAULT_SCALE_Y
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_CAP
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_JOIN
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_MITER
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_WIDTH
import androidx.ui.vectorgraphics.DEFAULT_TRANSLATE_X
import androidx.ui.vectorgraphics.DEFAULT_TRANSLATE_Y
import androidx.ui.vectorgraphics.EMPTY_PATH
import androidx.ui.vectorgraphics.PathBuilder
import androidx.ui.vectorgraphics.PathData
import androidx.ui.vectorgraphics.PathDelegate
import androidx.ui.vectorgraphics.PathNode
import androidx.ui.vectorgraphics.PathParser
import androidx.ui.vectorgraphics.VGroup
import androidx.ui.vectorgraphics.VNode
import androidx.ui.vectorgraphics.VPath
import androidx.ui.vectorgraphics.VectorGraphic
import androidx.ui.vectorgraphics.obtainBrush
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Emittable
import com.google.r4a.composer

fun parsePathNodes(pathStr: String?): Array<PathNode> =
    if (pathStr == null) {
        EMPTY_PATH
    } else {
        PathParser().parsePathString(pathStr).getNodes()
    }

@Composable
fun vector(
    name: String = "",
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = viewportWidth,
    defaultHeight: Float = viewportHeight,
    @Children children: () -> Unit
) {
    <Vector name defaultWidth defaultHeight viewportWidth viewportHeight>
        <children />
    </Vector>
}

@Composable
fun group(
    name: String = DEFAULT_GROUP_NAME,
    rotate: Float = DEFAULT_ROTATE,
    pivotX: Float = DEFAULT_PIVOT_X,
    pivotY: Float = DEFAULT_PIVOT_Y,
    scaleX: Float = DEFAULT_SCALE_X,
    scaleY: Float = DEFAULT_SCALE_Y,
    translateX: Float = DEFAULT_TRANSLATE_X,
    translateY: Float = DEFAULT_TRANSLATE_Y,
    clipPathData: PathData = EMPTY_PATH,
    @Children childNodes: () -> Unit
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
        <childNodes />
    </Group>
}

@Composable
fun path(
    pathData: PathData,
    name: String = DEFAULT_PATH_NAME,
    fill: BrushType? = null,
    fillAlpha: Float = DEFAULT_ALPHA,
    stroke: BrushType? = null,
    strokeAlpha: Float = DEFAULT_ALPHA,
    strokeLineWidth: Float = DEFAULT_STROKE_LINE_WIDTH,
    strokeLineCap: StrokeCap = DEFAULT_STROKE_LINE_CAP,
    strokeLineJoin: StrokeJoin = DEFAULT_STROKE_LINE_JOIN,
    strokeLineMiter: Float = DEFAULT_STROKE_LINE_MITER
) {

    val pathNodes = createPath(pathData)
    val fillBrush: Brush? = (obtainBrush(fill) as? Brush)
    val strokeBrush: Brush? = (obtainBrush(stroke) as? Brush)

    <Path
        name
        pathNodes
        fillBrush
        fillAlpha
        strokeBrush
        strokeAlpha
        strokeLineWidth
        strokeLineCap
        strokeLineJoin
        strokeLineMiter />
}

private sealed class VectorGraphicComponent {
    abstract val node: VNode
    abstract fun draw(canvas: Canvas)
}

private class Vector(
    name: String = "",
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = viewportWidth,
    defaultHeight: Float = viewportHeight
) : VectorGraphicComponent(), Emittable {

    private val vectorGraphic = VectorGraphic(
        name,
        defaultWidth,
        defaultHeight,
        viewportWidth,
        viewportHeight
    )

    val name: String = vectorGraphic.name

    val defaultWidth: Float = vectorGraphic.width
    val defaultHeight: Float = vectorGraphic.height

    val viewportWidth: Float = vectorGraphic.viewportWidth
    val viewportHeight: Float = vectorGraphic.viewportHeight

    override val node: VNode = vectorGraphic

    override fun emitInsertAt(index: Int, instance: Emittable) {
        if (instance is VectorGraphicComponent) {
            vectorGraphic.insert(index, instance.node)
        }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        vectorGraphic.move(from, to, count)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        vectorGraphic.removeAt(index, count)
    }

    override fun draw(canvas: Canvas) {
        vectorGraphic.draw(canvas)
    }

    override fun toString(): String {
        return vectorGraphic.toString()
    }
}

private class Path(name: String) : VectorGraphicComponent(), Emittable {

    private var path = VPath(name)

    val name: String = path.name

    var pathNodes: Array<PathNode>
        get() = path.pathNodes
        set(value) {
            path.pathNodes = value
        }

    var fillBrush: Brush?
        get() = path.fill
        set(value) {
            path.fill = value
        }

    var fillAlpha: Float
        get() = path.fillAlpha
        set(value) {
            path.fillAlpha = value
        }

    var strokeAlpha: Float
        get() = path.strokeAlpha
        set(value) {
            path.strokeAlpha = value
        }

    var strokeLineWidth: Float
        get() = path.strokeLineWidth
        set(value) {
            path.strokeLineWidth = value
        }

    var strokeBrush: Brush?
        get() = path.stroke
        set(value) {
            path.stroke = value
        }

    var strokeLineCap: StrokeCap
        get() = path.strokeLineCap
        set(value) {
            path.strokeLineCap = value
        }

    var strokeLineJoin: StrokeJoin
        get() = path.strokeLineJoin
        set(value) {
            path.strokeLineJoin = value
        }

    var strokeLineMiter: Float
        get() = path.strokeLineMiter
        set(value) {
            path.strokeLineMiter = value
        }

    override val node: VNode = path

    override fun draw(canvas: Canvas) {
        path.draw(canvas)
    }

    override fun emitInsertAt(index: Int, instance: Emittable) {
        throw IllegalArgumentException("Unable to insert Emittable into R4APath")
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        throw IllegalArgumentException("Unable to move Emittable within R4APath")
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        throw IllegalArgumentException("Unable to remove Emittable from R4APath")
    }

    override fun toString(): String {
        return path.toString()
    }
}

private class Group(name: String = DEFAULT_GROUP_NAME) : VectorGraphicComponent(), Emittable {

    private val group = VGroup(name)

    var clipPathNodes: Array<PathNode>
        get() = group.clipPathNodes
        set(value) {
            group.clipPathNodes = value
        }

    val name: String
        get() = group.name

    var rotate: Float
        get() = group.rotate
        set(value) {
            group.rotate = value
        }

    var pivotX: Float
        get() = group.pivotX
        set(value) {
            group.pivotX = value
        }

    var pivotY: Float
        get() = group.pivotY
        set(value) {
            group.pivotY = value
        }

    var scaleX: Float
        get() = group.scaleX
        set(value) {
            group.scaleX = value
        }

    var scaleY: Float
        get() = group.scaleY
        set(value) {
            group.scaleY = value
        }

    var translateX: Float
        get() = group.translateX
        set(value) {
            group.translateX = value
        }

    var translateY: Float
        get() = group.translateY
        set(value) {
            group.translateY = value
        }

    override val node: VNode = group

    override fun emitInsertAt(index: Int, instance: Emittable) {
        if (instance is VectorGraphicComponent) {
            group.insertAt(index, instance.node)
        } else {
            throw IllegalArgumentException("Emittable instance must be VNode")
        }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        group.move(from, to, count)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        group.removeAt(index, count)
    }

    override fun draw(canvas: Canvas) {
        group.draw(canvas)
    }

    override fun toString(): String {
        return group.toString()
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

    override fun draw(canvas: android.graphics.Canvas?) {
        vector.draw(androidx.ui.painting.Canvas(canvas!!))
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
