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
package androidx.ui.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.toPointerInputEvent
import androidx.ui.painting.Canvas

/**
 * [ComponentNode.ownerData] under [AndroidCraneView] control.
 */
private class AndroidData(val view: ViewGroup) {
    val drawNodes = mutableSetOf<DrawNode>()
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidCraneView constructor(context: Context)
    : ViewGroup(context), Owner, SemanticsTreeProvider {

    private val adjustedLayouts = mutableListOf<LayoutNode>()
    val root = LayoutNode()

    var constraintsChanged: () -> Unit = {}
    var ref: Array<AndroidCraneView?>? = null
        set(value) {
            field = value
            if (value != null) {
                value[0] = this
            }
        }

    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    var constraints = Constraints.tightConstraints(width = 0.px, height = 0.px)
    // TODO(mount): reinstate when coroutines are supported by IR compiler
//    private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    init {
        setWillNotDraw(false)
    }

    /**
     * Convenience property to avoid all that casting.
     */
    private var ComponentNode.androidData: AndroidData?
        get() = ownerData as AndroidData?
        set(value) {
            ownerData = value
        }

    override fun onInvalidate(drawNode: DrawNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        drawNode.androidData?.view?.invalidate()
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
                androidData?.drawNodes?.add(node)
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
            val androidData = node.androidData!!
            androidData.drawNodes.remove(node)
            if (true) {
            } // Don't know why this is needed for the R4A compiler
        } else if (node is LayoutNode) {
            val view = node.androidData!!.view
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
            root.width,
            root.height
        )

        adjustedLayouts.forEach { layout ->
            val layoutWidth = layout.width
            val layoutHeight = layout.height
            val width = View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(layoutHeight, View.MeasureSpec.EXACTLY)
            layout.androidData?.view?.measure(width, height)
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
            val left = layout.x
            val top = layout.y
            val right = left + layout.width
            val bottom = top + layout.height
            layout.androidData?.view?.layout(left, top, right, bottom)
        }
        adjustedLayouts.clear()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        pointerInputEventProcessor.process(event.toPointerInputEvent())
        // TODO(shepshapard): Only return if a child was hit.
        return true
    }

    private fun convertMeasureSpec(measureSpec: Int): ConstraintRange {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec).px
        return when (mode) {
            MeasureSpec.EXACTLY -> ConstraintRange(size, size)
            MeasureSpec.UNSPECIFIED -> ConstraintRange(0.px, Px.Infinity)
            MeasureSpec.AT_MOST -> ConstraintRange(0.px, size)
            else -> throw IllegalStateException()
        }
    }

    private fun callDrawOnChildren(node: ComponentNode, canvas: Canvas) {
        node.visitChildren { child ->
            if (child is DrawNode) {
                // TODO(mount): get rid of PixelSize and use PxSize instead
                child.onPaint(canvas, PxSize(root.width.px, root.height.px))
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

    override fun getAllSemanticNodes(): List<SemanticsTreeNode> {
        return findAllSemanticNodesIn(root)
    }

    override fun sendEvent(event: MotionEvent) {
        onTouchEvent(event)
    }
}

private class ConstraintRange(val min: Px, val max: Px)

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
                child.onPaint(
                    canvas,
                    PxSize(this.node.width.px, this.node.height.px)
                )
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
