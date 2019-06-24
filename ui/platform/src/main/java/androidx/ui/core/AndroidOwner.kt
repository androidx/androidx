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
import android.graphics.RenderNode
import android.os.Build
import android.os.Looper
import android.os.Trace
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RestrictTo
import androidx.compose.ObserverMap
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
    // LayoutNodes that need measure and layout, the value is true when measure is needed
    private val relayoutNodes = mutableSetOf<LayoutNode>()

    // Map from model to DrawNodes that should be redrawn or LayoutNodes that need measuring
    private val modelToNodes = ObserverMap<Any, ComponentNode>()

    // Map from LayoutNode or DrawNode to the models that they use
    private val nodeToModels = ObserverMap<ComponentNode, Any>()

    // Map from model to LayoutNodes that *only* need layout and not measure
    private val relayoutOnly = ObserverMap<Any, LayoutNode>()

    // RepaintBoundaryNodes that have had their boundary changed. When using Views,
    // the size/position of a View should change during layout, so this list
    // is kept separate from dirtyRepaintBoundaryNodes.
    private val repaintBoundaryChanges = mutableSetOf<RepaintBoundaryNode>()

    // RepaintBoundaryNodes that are dirty and should be redrawn. This is only
    // used when RenderNodes are active in Q+. When Views are used, the View
    // system tracks the dirty RenderNodes.
    internal val dirtyRepaintBoundaryNodes = mutableListOf<RepaintBoundaryNode>()

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
    internal var currentNode: ComponentNode? = null

    override var measureIteration: Long = 1L
        private set

    private val frameReadObserver: FrameReadObserver = { readValue ->
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw IllegalStateException("Frame reads are expected only on the main thread")
        }
        val node = currentNode
        if (node != null) {
            nodeToModels.add(node, readValue)
            if (node is LayoutNode) {
                if (node.isInMeasure) {
                    relayoutOnly.remove(readValue, node)
                    modelToNodes.add(readValue, node)
                } else if (!modelToNodes.contains(readValue, node)) {
                    relayoutOnly.add(readValue, node)
                }
            } else {
                modelToNodes.add(readValue, node)
            }
        }
    }

    private val commitObserver: FrameCommitObserver = { committed ->
        if (Looper.getMainLooper() == Looper.myLooper()) {
            onModelsCommitted(committed)
        } else {
            val list = ArrayList(committed)
            post(object : Runnable {
                override fun run() {
                    onModelsCommitted(list)
                }
            })
        }
    }

    var commitUnsubscribe: (() -> Unit)? = null

    init {
        setWillNotDraw(false)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusable = View.FOCUSABLE
        }
        isFocusableInTouchMode = true
    }

    private fun onModelsCommitted(models: Iterable<Any>) {
        modelToNodes[models].forEach { node ->
            when (node) {
                is DrawNode -> node.invalidate()
                is LayoutNode -> node.requestRemeasure()
            }
        }
        relayoutOnly[models].forEach { node ->
            requestRelayout(node)
        }
    }

    override fun onInvalidate(drawNode: DrawNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        invalidateRepaintBoundary(drawNode)
//        }
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        markRepaintBoundaryBoundsChanged(layoutNode)
        invalidateRepaintBoundary(layoutNode)
//        }
    }

    /**
     * Make sure the containing RepaintBoundary repaints.
     */
    private fun invalidateRepaintBoundary(node: ComponentNode) {
        val repaintBoundary = node.repaintBoundary?.container
        if (repaintBoundary != null) {
            repaintBoundary.dirty = true
        } else {
            invalidate()
        }
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        markRepaintBoundaryBoundsChanged(layoutNode)
//        }
    }

    /**
     * If layoutNode is a child of a repaint boundary, this sets up the repaint boundary
     * to be resized/repositioned after layout completes.
     */
    private fun markRepaintBoundaryBoundsChanged(layoutNode: LayoutNode) {
        val repaintBoundaryNode = layoutNode.repaintBoundary
        // If the repaint boundary has this layoutNode as a child, the repaint boundary
        // position and size may have changed, so set it up for resetting its bounds after the
        // layout pass completes.
        if (repaintBoundaryNode == null) {
            invalidate() // The main view needs to be redrawn
        } else if (layoutNode.parentLayoutNode == repaintBoundaryNode.parentLayoutNode) {
            var boundary: RepaintBoundaryNode? = repaintBoundaryNode
            val parentLayoutNode = repaintBoundaryNode.parentLayoutNode
            while (boundary != null && boundary.parentLayoutNode == parentLayoutNode) {
                repaintBoundaryChanges += boundary
                boundary = boundary.parent?.repaintBoundary
            }
        }
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        // find root of layout request:
        if (layoutNode.needsRemeasure) {
            // don't need to do anything because it already needs to be remeasured
            return
        }
        layoutNode.needsRemeasure = true

        var layout = layoutNode
        while (layout.parentLayoutNode != null && layout.parentLayoutNode != root &&
            layout.affectsParentSize) {
            layout = layout.parentLayoutNode!!
            if (layout.needsRemeasure) {
                // don't need to do anything else since the parent already needs measuring
                return
            }
            layout.needsRemeasure = true
        }

        val parent = layout.parentLayoutNode
        if (parent == null) {
            requestRelayout(layout)
        } else {
            requestRelayout(parent)
        }
    }

    private fun requestRelayout(layoutNode: LayoutNode) {
        if (layoutNode == root || constraints.isZero) {
            requestLayout()
        } else if (relayoutNodes.isEmpty()) {
            Choreographer.getInstance().postFrameCallback {
                measureAndLayout()
            }
        }
        layoutNode.needsRelayout = true
        relayoutNodes += layoutNode
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()

        if (node is RepaintBoundaryNode) {
            val ownerData = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                RepaintBoundaryView(this, node)
            } else {
                RepaintBoundaryRenderNode(this, node)
            }
            node.ownerData = ownerData
            ownerData.attach(node.parent?.repaintBoundary?.container)
        }
    }

    override fun onDetach(node: ComponentNode) {
        if (node is RepaintBoundaryNode) {
            node.container.detach()
            node.ownerData = null
        }
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    private fun measureAndLayout() {
        Trace.beginSection("Compose:measureAndLayout")
        try {
            measureIteration++
            val frame = currentFrame()
            frame.observeReads(frameReadObserver) {
                relayoutNodes.sortedBy { it.depth }.forEach { layoutNode ->
                    if (layoutNode.needsRemeasure) {
                        val parent = layoutNode.parentLayoutNode
                        if (parent != null && parent.layout != null) {
                            // This should call measure and layout on the child
                            val parentLayout = parent.layout!!
                            parent.needsRelayout = true
                            parentLayout.callLayout()
                        } else {
                            layoutNode.layout?.callMeasure(layoutNode.constraints)
                            layoutNode.layout?.callLayout()
                        }
                    } else if (layoutNode.needsRelayout) {
                        layoutNode.layout?.callLayout()
                    }
                }
                repaintBoundaryChanges.sortedBy { it.depth }.forEach { node ->
                    var bounds = node.calculateChildrenBoundingBox()
                    node.layoutX = bounds.left
                    node.layoutY = bounds.top
                    calculateRepaintBoundaryNodePosition(node)

                    val left = node.containerX.value
                    val top = node.containerY.value
                    val right = left + bounds.width.value
                    val bottom = top + bounds.height.value
                    val container = node.container
                    container.setBounds(left, top, right, bottom)
                }
                relayoutNodes.clear()
                repaintBoundaryChanges.clear()
            }
        } finally {
            Trace.endSection()
        }
    }

    /**
     * Calculates and sets the [RepaintBoundaryNode.containerX] and
     * [RepaintBoundaryNode.containerY].
     */
    private fun calculateRepaintBoundaryNodePosition(node: RepaintBoundaryNode) {
        var left = node.layoutX
        var top = node.layoutY
        val repaintBoundary = node.parent?.repaintBoundary
        val containingLayoutNode = repaintBoundary?.parentLayoutNode
        var layoutNode = node.parentLayoutNode

        while (layoutNode != null && layoutNode != containingLayoutNode) {
            left += layoutNode.x
            top += layoutNode.y
            layoutNode = layoutNode.parentLayoutNode
        }
        if (containingLayoutNode != null) {
            left -= repaintBoundary.layoutX
            top -= repaintBoundary.layoutY
        }
        node.containerX = left
        node.containerY = top
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("Compose:onMeasure")
        try {
            val targetWidth = convertMeasureSpec(widthMeasureSpec)
            val targetHeight = convertMeasureSpec(heightMeasureSpec)

            val constraints = Constraints(
                targetWidth.min, targetWidth.max,
                targetHeight.min, targetHeight.max
            )

            this.constraints = constraints

            // commit the current frame
            val frame = currentFrame()
            measureIteration++
            frame.observeReads(frameReadObserver) {
                callMeasure(constraints)
            }
            setMeasuredDimension(root.width.value, root.height.value)
        } finally {
            Trace.endSection()
        }
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
        Trace.beginSection("Compose:onLayout")
        try {
            val frame = currentFrame()
            root.startLayout()
            frame.observeReads(frameReadObserver) {
                root.visitLayoutChildren { child ->
                    child.moveTo(0.ipx, 0.ipx)
                    child.layout?.callLayout()
                }
            }
            root.moveTo(0.ipx, 0.ipx)
            root.endLayout()
        } finally {
            Trace.endSection()
        }
        measureAndLayout()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    fun callDraw(
        canvas: Canvas,
        node: ComponentNode,
        parentSize: PxSize,
        densityReceiver: DensityReceiver
    ) {
        when (node) {
            is DrawNode -> {
                val onPaint = node.onPaint
                val previousNode = currentNode
                currentNode = node
                clearNodeModels(node)
                val receiver = DrawNodeScopeImpl(node, canvas, parentSize,
                    densityReceiver.density)
                receiver.onPaint(canvas, parentSize)
                if (!receiver.childDrawn) {
                    receiver.drawChildren()
                }
                node.needsPaint = false
                currentNode = previousNode
            }
            is RepaintBoundaryNode -> {
                val x = (node.containerX.value - node.layoutX.value).toFloat()
                val y = (node.containerY.value - node.layoutY.value).toFloat()
                val doTranslate = x != 0f || y != 0f
                if (doTranslate) {
                    canvas.save()
                    canvas.translate(-x, -y)
                }
                node.container.callDraw(canvas)
                if (doTranslate) {
                    canvas.restore()
                }
            }
            is LayoutNode -> {
                if (node.visible) {
                    val doTranslate = node.x != 0.ipx || node.y != 0.ipx
                    if (doTranslate) {
                        canvas.save()
                        canvas.translate(node.x.value.toFloat(), node.y.value.toFloat())
                    }
                    val size = PxSize(node.width, node.height)
                    node.visitChildren { child ->
                        callDraw(canvas, child, size, densityReceiver)
                    }
                    if (doTranslate) {
                        canvas.restore()
                    }
                }
            }
            else -> node.visitChildren {
                callDraw(canvas, it, parentSize, densityReceiver)
            }
        }
    }

    internal fun drawChild(canvas: Canvas, view: View, drawingTime: Long) {
        super.drawChild(canvas.nativeCanvas, view, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        watchDraw(canvas, root)
    }

    /**
     * This call converts the framework Canvas to an androidx [Canvas] and paints node's
     * children.
     */
    internal fun callChildDraw(canvas: android.graphics.Canvas, node: ComponentNode) {
        val layoutNode = node as? LayoutNode ?: node.parentLayoutNode!!
        val parentSize = PxSize(layoutNode.width, layoutNode.height)
        val uiCanvas = Canvas(canvas)
        val densityReceiver = DensityReceiverImpl(density = Density(context))
        node.visitChildren { child ->
            callDraw(uiCanvas, child, parentSize, densityReceiver)
        }
    }

    /**
     * Called to draw the root or a repaint boundary node and observe all model reads during
     * the draw calls. Note that this takes a framework Canvas, so it is called only from
     * [AndroidCraneView] or [RepaintBoundaryView].
     */
    internal fun watchDraw(canvas: android.graphics.Canvas, node: ComponentNode) {
        Trace.beginSection("Compose:draw")
        try {
            currentNode = node
            // Start looking for model changes:
            val frame = currentFrame()
            frame.observeReads(frameReadObserver) {
                callChildDraw(canvas, node)
                dirtyRepaintBoundaryNodes.forEach { node ->
                    node.container.updateDisplayList()
                }
                dirtyRepaintBoundaryNodes.clear()
            }
            currentNode = null
        } finally {
            Trace.endSection()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        commitUnsubscribe = registerCommitObserver(commitObserver)
        root.attach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        commitUnsubscribe?.invoke()
        root.detach()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Trace.beginSection("Compose:touch")
        try {
            pointerInputEventProcessor.process(event.toPointerInputEvent())
            // TODO(shepshapard): Only return if a child was hit.
        } finally {
            Trace.endSection()
        }
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

    override fun calculatePosition(): PxPosition {
        val positionArray = intArrayOf(0, 0)
        getLocationInWindow(positionArray)
        return PxPosition(positionArray[0].ipx, positionArray[1].ipx)
    }

    private fun callMeasure(constraints: Constraints) {
        var maxWidth = 0.ipx
        var maxHeight = 0.ipx
        root.startMeasure()
        root.visitLayoutChildren { layoutNode ->
            layoutNode.layout?.callMeasure(constraints)
            maxWidth = max(maxWidth, layoutNode.width)
            maxHeight = max(maxHeight, layoutNode.height)
        }
        root.resize(maxWidth, maxHeight)
        root.endMeasure()
    }

    private fun clearNodeModels(node: ComponentNode) {
        nodeToModels.remove(node).forEach { model ->
            modelToNodes.remove(model, node)
        }
        if (node is LayoutNode) {
            relayoutNodes.remove(node)
        }
    }

    private inner class DrawNodeScopeImpl(
        private val drawNode: DrawNode,
        private val canvas: Canvas,
        private val parentSize: PxSize,
        override val density: Density
    ) : DensityReceiver, DrawNodeScope {
        internal var childDrawn = false

        override fun drawChildren() {
            if (childDrawn) {
                throw IllegalStateException("Cannot call drawChildren() twice within Draw element")
            }
            childDrawn = true
            drawNode.visitChildren { child ->
                callDraw(canvas, child, parentSize, this)
            }
        }
    }
}

private class ConstraintRange(val min: IntPx, val max: IntPx)

/**
 * A common interface to be used with either Views or RenderNode implementations of
 * RepaintBoundaries.
 */
private interface RepaintBoundary {
    /**
     * Changes the size and position of the RepaintBoundary.
     */
    fun setBounds(left: Int, top: Int, right: Int, bottom: Int)

    /**
     * Called when attaching the RepaintBoundary to the emitted hierarchy.
     */
    fun attach(parent: RepaintBoundary?)

    /**
     * Called when detaching the RepaintBoundary from the emitted hierarchy.
     */
    fun detach()

    /**
     * Draws the contents of the RepaintBoundary onto the given Canvas. After this,
     * [dirty] must be `false`.
     */
    fun callDraw(canvas: Canvas)

    /**
     * For RenderNodes, this updates the RenderNode in place. After this, [dirty] must
     * be `false`.
     */
    fun updateDisplayList()

    /**
     * `true` indicates that the RepaintBoundary must be redrawn and `false` indicates
     * that no change has occured since the previous [callDraw] or [updateDisplayList] call.
     */
    var dirty: Boolean
}

/**
 * View implementation of RepaintBoundary.
 */
private class RepaintBoundaryView(
    val ownerView: AndroidCraneView,
    val repaintBoundaryNode: RepaintBoundaryNode
) : ViewGroup(ownerView.context), RepaintBoundary {
    init {
        setTag(repaintBoundaryNode.name)
        // In the future, we want to have clipChildren = true so that we get better performance.
        // We can do that once the size of draw functions are well understood.
        clipChildren = false
        setWillNotDraw(false) // we WILL draw
    }
    override var dirty: Boolean = true
        set(value) {
            if (value && !field) {
                invalidate()
            }
            field = value
        }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        if (width != this.width || height != this.height) {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            measure(widthSpec, heightSpec)
            layout(left, top, right, bottom)
        } else {
            val offsetHorizontal = left - this.left
            if (offsetHorizontal != 0) {
                offsetLeftAndRight(offsetHorizontal)
            }
            val offsetVertical = top - this.top
            if (offsetVertical != 0) {
                offsetTopAndBottom(offsetVertical)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun callDraw(canvas: Canvas) {
        val parentView = parent
        val drawingTime = drawingTime
        if (parentView is RepaintBoundaryView) {
            parentView.drawChild(canvas, this, drawingTime)
        } else {
            ownerView.drawChild(canvas, this, drawingTime)
        }
        dirty = false
    }

    override fun detach() {
        val parent = parent as ViewGroup
        parent.removeView(this)
    }

    fun drawChild(canvas: Canvas, child: View, drawingTime: Long) {
        drawChild(canvas.nativeCanvas, child, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        // We have to pretend that we're at the position of the enclosing LayoutNode
        canvas.save()
        canvas.translate(-repaintBoundaryNode.layoutX.value.toFloat(),
            -repaintBoundaryNode.layoutY.value.toFloat())
        if (ownerView.currentNode == null) {
            // Only this repaint boundary was invalidated and nothing higher in the view hierarchy.
            // We must observe changes
            ownerView.watchDraw(canvas, repaintBoundaryNode)
        } else {
            // We don't have to observe changes
            ownerView.callChildDraw(canvas, repaintBoundaryNode)
        }
        canvas.restore()
        dirty = false
    }

    override fun attach(parent: RepaintBoundary?) {
        if (parent != null) {
            (parent as RepaintBoundaryView).addView(this)
        } else {
            ownerView.addView(this)
        }
    }

    override fun updateDisplayList() {
        // Don't need to do anything here. This is handled by View
        throw IllegalStateException("updateDisplayList should not be called on RepaintBoundaryView")
    }
}

/**
 * RenderNode implemenation of RepaintBoundary.
 */
@TargetApi(29)
private class RepaintBoundaryRenderNode(
    val ownerView: AndroidCraneView,
    val repaintBoundaryNode: RepaintBoundaryNode
) : RepaintBoundary {
    override var dirty = true
        set(value) {
            if (value && !field) {
                ownerView.invalidate()
                ownerView.dirtyRepaintBoundaryNodes += repaintBoundaryNode
            }
            field = value
        }
    val renderNode = RenderNode(repaintBoundaryNode.name)

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        if (width != renderNode.width || height != renderNode.height) {
            renderNode.setPosition(left, top, right, bottom)
        } else {
            var needsChange = renderNode.offsetLeftAndRight(left - renderNode.left)
            needsChange = renderNode.offsetTopAndBottom(top - renderNode.top) || needsChange
            if (needsChange) {
                // Trigger a frame without damaging any RenderNodes
                ownerView.onDescendantInvalidated(ownerView, ownerView)
            }
        }
    }

    override fun attach(parent: RepaintBoundary?) {
        // nothing needs to be done
    }

    override fun detach() {
        // nothing needs to be done
    }

    override fun callDraw(canvas: Canvas) {
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            updateDisplayList()
            androidCanvas.drawRenderNode(renderNode)
        } else {
            canvas.save()
            canvas.translate(renderNode.left.toFloat(), renderNode.top.toFloat())
            ownerView.callChildDraw(androidCanvas, repaintBoundaryNode)
            canvas.restore()
            dirty = false
        }
    }

    override fun updateDisplayList() {
        if (dirty || !renderNode.hasDisplayList()) {
            val canvas = renderNode.beginRecording()
            canvas.translate(-repaintBoundaryNode.layoutX.value.toFloat(),
                -repaintBoundaryNode.layoutY.value.toFloat())
            ownerView.callChildDraw(canvas, repaintBoundaryNode)
            renderNode.endRecording()
            dirty = false
        }
    }
}

private val RepaintBoundaryNode.container: RepaintBoundary get() = ownerData as RepaintBoundary
