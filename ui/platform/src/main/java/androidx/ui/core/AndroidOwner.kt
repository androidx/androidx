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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RestrictTo
import androidx.ui.core.input.TextInputServiceAndroid
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.toPointerInputEvent
import androidx.ui.input.TextInputService
import androidx.ui.painting.Canvas
import androidx.compose.frames.FrameCommitObserver
import androidx.compose.frames.FrameReadObserver
import androidx.compose.frames.currentFrame
import androidx.compose.frames.registerCommitObserver

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidCraneView constructor(context: Context)
    : ViewGroup(context), Owner, SemanticsTreeProvider {

    val root = LayoutNode()
    private val relayoutNodes = mutableSetOf<LayoutNode>()
    private val modelToNodes = mutableMapOf<Any, MutableSet<ComponentNode>>()
    private val nodeToModels = mutableMapOf<ComponentNode, MutableSet<Any>>()

    var ref: Ref<AndroidCraneView>? = null
        set(value) {
            field = value
            if (value != null) {
                value.value = this
            }
        }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    var constraints = Constraints.tightConstraints(width = IntPx.Zero, height = IntPx.Zero)
    // TODO(mount): reinstate when coroutines are supported by IR compiler
//    private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    // Used for tracking which nodes a frame read is applied to
    private var currentNode: ComponentNode? = null

    private val frameReadObserver: FrameReadObserver = { readValue ->
        val node = currentNode
        if (node != null) {
            val models = nodeToModels.getOrElse(node) {
                val set = mutableSetOf<Any>()
                nodeToModels[node] = set
                set
            }
            models += readValue
            val nodes = modelToNodes.getOrElse(readValue) {
                val set = mutableSetOf<ComponentNode>()
                modelToNodes[readValue] = set
                set
            }
            nodes += node
        }
    }

    private val commitObserver: FrameCommitObserver = { committed ->
        committed.forEach {
            modelToNodes[it]?.forEach { node ->
                when (node) {
                    is DrawNode -> node.invalidate()
                    is LayoutNode -> node.requestLayout()
                }
            }
        }
    }

    init {
        setWillNotDraw(false)
        // TODO(mount): How do I unregister?
        registerCommitObserver(commitObserver)

        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusable = View.FOCUSABLE
        }
        isFocusableInTouchMode = true
    }

    override fun onInvalidate(drawNode: DrawNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        invalidate() // TODO(mount): invalidate sub-area when possible
//        }
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
//        }
        // Do I need to invalidate after the size has changed or will it be done automatically?
//        invalidate()
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        invalidate()
//        }
    }

    override fun onRequestLayout(layoutNode: LayoutNode) {
        // find root of layout request:
        layoutNode.needsRemeasure = true

        var layout = layoutNode
        while (layout.parentLayoutNode != null && layout.affectsParentSize) {
            layout = layout.parentLayoutNode!!
            layout.needsRemeasure = true
        }

        relayoutNodes += layout

        if (layout == root) {
            requestLayout()
        } else {
            postOnAnimation {
                measureAndLayout()
            }
        }
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()

        requestLayout()
    }

    override fun onDetach(node: ComponentNode) {
        node.ownerData = null
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    private fun measureAndLayout() {
        val frame = currentFrame()
        frame.observeReads(frameReadObserver) {
            relayoutNodes.sortedBy { it.depth }.forEach { layoutNode ->
                if (layoutNode.needsRemeasure) {
                    val parent = layoutNode.parentLayoutNode
                    if (parent != null) {
                        // This should call measure and layout on the child
                        parent.layout?.callLayout()
                    } else {
                        layoutNode.layout?.callMeasure(layoutNode.constraints)
                        layoutNode.layout?.callLayout()
                    }
                }
            }
            relayoutNodes.clear()
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val targetWidth = convertMeasureSpec(widthMeasureSpec)
        val targetHeight = convertMeasureSpec(heightMeasureSpec)

        val constraints = Constraints(
            targetWidth.min, targetWidth.max,
            targetHeight.min, targetHeight.max
        )

        if (this.constraints != constraints) {
            this.constraints = constraints
        }
        // commit the current frame
        val frame = currentFrame()
        frame.observeReads(frameReadObserver) {
            callMeasure(constraints)
        }
        setMeasuredDimension(root.width.value, root.height.value)
    }

    override fun onEndLayout(layoutNode: LayoutNode) {
        currentNode = layoutNode.parentLayoutNode // TODO(mount): make this smarter
    }

    override fun onEndMeasure(layoutNode: LayoutNode) {
        currentNode = layoutNode.parentLayoutNode // TODO(mount): make this smarter
    }

    override fun onStartLayout(layoutNode: LayoutNode) {
        currentNode = layoutNode
    }

    override fun onStartMeasure(layoutNode: LayoutNode) {
        clearNodeModels(layoutNode)
        currentNode = layoutNode
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val frame = currentFrame()
        root.startLayout()
        frame.observeReads(frameReadObserver) {
            root.visitChildren { child ->
                child.layoutNode?.moveTo(0.ipx, 0.ipx)
                child.layoutNode?.layout?.callLayout()
            }
        }
        root.layoutNode.moveTo(0.ipx, 0.ipx)
        root.endLayout()
        measureAndLayout()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    private fun callDraw(
        canvas: Canvas,
        node: ComponentNode,
        parentSize: PxSize,
        densityReceiver: DensityReceiver
    ) {
        when (node) {
            is DrawNode -> {
                val onPaint = node.onPaint
                currentNode = node
                clearNodeModels(node)
                val receiver = DrawNodeScopeImpl(node.child, canvas, parentSize,
                    densityReceiver.density)
                receiver.onPaint(canvas, parentSize)
                if (!receiver.childDrawn) {
                    receiver.drawChildren()
                }
                node.needsPaint = false
                currentNode = null
            }
            is LayoutNode -> {
                if (node.visible) {
                    canvas.save()
                    canvas.translate(node.x.value.toFloat(), node.y.value.toFloat())
                    val size = PxSize(node.width, node.height)
                    node.visitChildren { child ->
                        callDraw(canvas, child, size, densityReceiver)
                    }
                    canvas.restore()
                }
            }
            else -> node.visitChildren { callDraw(canvas, it, parentSize, densityReceiver) }
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // Start looking for model changes:
        val frame = currentFrame()
        frame.observeReads(frameReadObserver) {
            val uiCanvas = Canvas(canvas)
            val densityReceiver = DensityReceiverImpl(density = Density(context))
            val parentSize = PxSize(root.width, root.height)
            callDraw(uiCanvas, root, parentSize, densityReceiver)
        }
        currentNode = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        root.attach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        root.detach()
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

    override fun getAllSemanticNodes(): List<SemanticsTreeNode> {
        return findAllSemanticNodesIn(root)
    }

    override fun sendEvent(event: MotionEvent) {
        onTouchEvent(event)
    }

    /**
     * @hide
     */
    val textInputService: TextInputService
        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() = textInputServiceAndroid

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    override fun onCheckIsTextEditor(): Boolean = textInputServiceAndroid.isEditorFocused()

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        textInputServiceAndroid.createInputConnection(outAttrs)

    private fun callMeasure(constraints: Constraints) {
        var maxWidth = 0.ipx
        var maxHeight = 0.ipx
        root.startMeasure()
        root.visitChildren { child ->
            val layoutNode = child.layoutNode
            if (layoutNode != null) {
                layoutNode.layout?.callMeasure(constraints)
                maxWidth = max(maxWidth, layoutNode.width)
                maxHeight = max(maxHeight, layoutNode.height)
            }
        }
        root.resize(maxWidth, maxHeight)
        root.endMeasure()
    }

    private fun clearNodeModels(node: ComponentNode) {
        val models = nodeToModels.remove(node)
        if (models != null) {
            models.forEach { model ->
                modelToNodes[model]!! -= node
            }
        }
    }

    private inner class DrawNodeScopeImpl(
        private val child: ComponentNode?,
        private val canvas: Canvas,
        private val parentSize: PxSize,
        override val density: Density
    ) : DensityReceiver, DrawNodeScope {
        var childDrawn = child == null

        override fun drawChildren() {
            if (childDrawn) {
                throw IllegalStateException("Cannot call drawChildren() twice within Draw element")
            }
            childDrawn = true
            if (child != null) {
                callDraw(canvas, child, parentSize, this)
            }
        }
    }
}

private class ConstraintRange(val min: IntPx, val max: IntPx)
