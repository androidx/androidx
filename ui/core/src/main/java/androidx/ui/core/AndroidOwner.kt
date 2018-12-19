/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.ui.painting.Canvas
import kotlin.math.roundToInt

/**
 * [ComponentNode.ownerData] under [AndroidCraneView] control.
 */
private class AndroidData(val view: ViewGroup) {
    val drawNodes = mutableSetOf<DrawNode>()
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class AndroidCraneView constructor(context: Context) : ViewGroup(context), Owner {
    private val adjustedLayouts = mutableListOf<LayoutNode>()
    internal val root = LayoutNode()

    var constraintsChanged: () -> Unit = {}
    var ref: Array<AndroidCraneView?>? = null
        set(value) {
            field = value
            if (value != null) {
                value[0] = this
            }
        }

    var constraints = tightConstraints(width = 0.dp, height = 0.dp)
    // TODO(mount): reinstate when coroutines are supported by IR compiler
//    private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    init {
        setWillNotDraw(false)
    }

    /**
     * Convenience property to avoid all that casting.
     */
    private var ComponentNode.androidData: AndroidData
        get() = ownerData as AndroidData
        set(value) {
            ownerData = value
        }

    override fun onInvalidate(drawNode: DrawNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        drawNode.androidData.view.invalidate()
//        }
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        requestLayout()
//        }

        adjustedLayouts.add(layoutNode)
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        requestLayout()
//        }
        adjustedLayouts.add(layoutNode)
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()

        val parent = node.parent
        if (node !is LayoutNode) {
            val androidData = parent!!.androidData
            node.androidData = androidData
            if (node is DrawNode) {
                androidData.drawNodes.add(node)
            }
            return
        }

        // LayoutNode and DrawNode get their own Views
        val container = parent?.androidData?.view ?: this

        val nodeView = NodeView(container, node)
        val data = AndroidData(nodeView)
        node.androidData = data

        // node position or size may be changed while the node was detached
        adjustedLayouts.add(node)
    }

    override fun onDetach(node: ComponentNode) {
        adjustedLayouts.remove(node)
        if (node is DrawNode) {
            val androidData = node.androidData
            androidData.drawNodes.remove(node)
            if (true) {
            } // Don't know why this is needed for the R4A compiler
        } else if (node is LayoutNode) {
            val view = node.androidData.view
            (view.parent as ViewGroup).removeView(view)
        }
        node.ownerData = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val targetWidth = convertMeasureSpec(widthMeasureSpec)
        val targetHeight = convertMeasureSpec(heightMeasureSpec)

        val constraints = Constraints(
            targetWidth.min, targetWidth.max,
            targetHeight.min, targetHeight.max
        )
        if (constraints != this.constraints) {
            this.constraints = constraints
            constraintsChanged()
        }

        setMeasuredDimension(
            root.size.width.toPx(context).roundToInt(),
            root.size.height.toPx(context).roundToInt()
        )

        adjustedLayouts.forEach { layout ->
            val layoutWidth = layout.size.width.toPx(context).roundToInt()
            val layoutHeight = layout.size.height.toPx(context).roundToInt()
            val width = View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(layoutHeight, View.MeasureSpec.EXACTLY)
            layout.androidData.view.measure(width, height)
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        // Walk children to look for DrawNodes
        val wrappedCanvas = Canvas(canvas)
        callDrawOnChildren(root, wrappedCanvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        root.attach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        root.detach()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        adjustedLayouts.forEach { layout ->
            val left = layout.x.toPx(context).roundToInt()
            val top = layout.y.toPx(context).roundToInt()
            val right = left + layout.size.width.toPx(context).roundToInt()
            val bottom = top + layout.size.height.toPx(context).roundToInt()
            layout.androidData.view.layout(left, top, right, bottom)
        }
        adjustedLayouts.clear()
    }

    private fun convertMeasureSpec(measureSpec: Int): ConstraintRange {
        val pxToDp = 1f / 1.dp.toPx(context)
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec).dp * pxToDp
        return when (mode) {
            MeasureSpec.EXACTLY -> ConstraintRange(size, size)
            MeasureSpec.UNSPECIFIED -> ConstraintRange(0.dp, Float.POSITIVE_INFINITY.dp)
            MeasureSpec.AT_MOST -> ConstraintRange(0.dp, size)
            else -> throw IllegalStateException()
        }
    }

    private fun callDrawOnChildren(node: ComponentNode, canvas: Canvas) {
        node.visitChildren { child ->
            if (child is DrawNode) {
                child.onPaint(canvas, root.size.toPx(context))
                child.needsPaint = false
            } else if (child is LayoutNode) {
                val view = (child.ownerData as AndroidData).view
                val fwCanvas = canvas.toFrameworkCanvas()
                var saveCount = -1
                if (view.left != 0 || view.top != 0) {
                    saveCount = fwCanvas.save()
                    fwCanvas.translate(view.left.toFloat(), view.top.toFloat())
                }
                view.draw(fwCanvas)
                if (saveCount != -1) {
                    fwCanvas.restoreToCount(saveCount)
                }
            } else {
                callDrawOnChildren(child, canvas)
            }
        }
    }
}

private class ConstraintRange(val min: Dimension, val max: Dimension)

/**
 * Defines a View used to keep RenderNode information on LayoutNodes and DrawNodes.
 * This will be replaced with using RenderNodes instead.
 */
@SuppressLint("ViewConstructor")
private class NodeView(container: ViewGroup, val node: LayoutNode) :
    ViewGroup(container.context) {
    init {
        container.addView(this)
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        assert(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY)
        assert(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        // Walk children to look for DrawNodes
        val wrappedCanvas = Canvas(canvas)
        callDrawOnChildren(node, wrappedCanvas)
    }

    /**
     * Override dispatchDraw so that we can change the order of drawing to intersperse
     * [DrawNode]s and [LayoutNode]s.
     */
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
    }

    private fun callDrawOnChildren(node: ComponentNode, canvas: Canvas) {
        node.visitChildren { child ->
            if (child is DrawNode) {
                child.onPaint(canvas, this.node.size.toPx(context))
                child.needsPaint = false
            } else if (child is LayoutNode) {
                val view = (child.ownerData as AndroidData).view
                val fwCanvas = canvas.toFrameworkCanvas()
                drawChild(fwCanvas, view, drawingTime)
            } else {
                callDrawOnChildren(child, canvas)
            }
        }
    }
}
