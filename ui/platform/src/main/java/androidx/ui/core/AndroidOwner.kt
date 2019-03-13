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
import com.google.r4a.frames.FrameCommitObserver
import com.google.r4a.frames.FrameReadObserver
import com.google.r4a.frames.commit
import com.google.r4a.frames.open
import com.google.r4a.frames.registerCommitObserver
import com.google.r4a.frames.restore
import com.google.r4a.frames.suspend

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
    private val modelToDrawNodes = mutableMapOf<Any, MutableSet<DrawNode>>()
    private val drawNodeToModels = mutableMapOf<DrawNode, MutableSet<Any>>()

    var onMeasureRecompose: () -> Unit = {}
    var ref: Array<AndroidCraneView?>? = null
        set(value) {
            field = value
            if (value != null) {
                value[0] = this
            }
        }

    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    var constraints = Constraints.tightConstraints(width = IntPx.Zero, height = IntPx.Zero)
    // TODO(mount): reinstate when coroutines are supported by IR compiler
//    private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    private val densityReceiver = DensityReceiverImpl(Density(context))

    // Used for tracking which nodes a frame read is applied to
    private var currentDrawNode: DrawNode? = null

    private val frameReadObserver: FrameReadObserver = { read ->
        val drawNode = currentDrawNode
        if (drawNode != null) {
            val models = drawNodeToModels.getOrElse(drawNode) {
                val set = mutableSetOf<Any>()
                drawNodeToModels[drawNode] = set
                set
            }
            models += read
            val nodes = modelToDrawNodes.getOrElse(read) {
                val set = mutableSetOf<DrawNode>()
                modelToDrawNodes[read] = set
                set
            }
            nodes += drawNode
        }
    }

    private val commitObserver: FrameCommitObserver = { committed ->
        committed.forEach {
            modelToDrawNodes[it]?.forEach { drawNode ->
                drawNode.invalidate()
            }
        }
    }

    init {
        setWillNotDraw(false)
        // TODO(mount): How do I unregister?
        registerCommitObserver(commitObserver)
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
        val constraintsChagned = constraints != this.constraints
        if (constraintsChagned) {
            this.constraints = constraints
        }
        if (constraintsChagned || adjustedLayouts.isNotEmpty()) {
            onMeasureRecompose()
        }

        setMeasuredDimension(root.width.value, root.height.value)

        adjustedLayouts.forEach { layout ->
            val layoutWidth = layout.width.value
            val layoutHeight = layout.height.value
            val width = View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(layoutHeight, View.MeasureSpec.EXACTLY)
            layout.androidData?.view?.measure(width, height)
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        // Nothing to draw internally
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // Start looking for model changes:
        val current = suspend()
        val frame = open(readObserver = frameReadObserver)
        try {
            super.dispatchDraw(canvas)
        } finally {
            commit(frame)
            restore(current)
        }
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
            val left = layout.x.value
            val top = layout.y.value
            val right = left + layout.width.value
            val bottom = top + layout.height.value
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
        val size = IntPx(MeasureSpec.getSize(measureSpec))
        return when (mode) {
            MeasureSpec.EXACTLY -> ConstraintRange(size, size)
            MeasureSpec.UNSPECIFIED -> ConstraintRange(IntPx.Zero, IntPx.Infinity)
            MeasureSpec.AT_MOST -> ConstraintRange(IntPx.Zero, size)
            else -> throw IllegalStateException()
        }
    }

    internal fun callDrawOnChildren(node: ComponentNode, canvas: Canvas) {
        node.visitChildren { child ->
            if (child is DrawNode) {
                val oldNode = currentDrawNode
                currentDrawNode = child
                // remove previously observed values
                val oldModels = drawNodeToModels[child]
                if (oldModels != null) {
                    oldModels.forEach { modelToDrawNodes[it]?.remove(child) }
                    oldModels.clear()
                }
                val ownerData = child.androidData
                if (ownerData != null) {
                    val size = PxSize(ownerData.view.width.ipx, ownerData.view.height.ipx)
                    child.onPaint(densityReceiver, canvas, size)
                }
                child.needsPaint = false
                currentDrawNode = oldNode
            } else if (child is LayoutNode) {
                val view = (child.ownerData as AndroidData).view
                val fwCanvas = canvas.toFrameworkCanvas()
                drawChild(fwCanvas, view, drawingTime)
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

private class ConstraintRange(val min: IntPx, val max: IntPx)

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

    private val densityReceiver = DensityReceiverImpl(Density(context))

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
        val owner = node.owner as AndroidCraneView
        owner.callDrawOnChildren(node, wrappedCanvas)
    }

    /**
     * Override dispatchDraw so that we can change the order of drawing to intersperse
     * [DrawNode]s and [LayoutNode]s.
     */
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
    }
}
