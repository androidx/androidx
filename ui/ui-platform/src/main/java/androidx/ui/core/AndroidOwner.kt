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
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RestrictTo
import androidx.compose.ObserverMap
import androidx.ui.core.input.TextInputServiceAndroid
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.toPointerInputEvent
import androidx.ui.engine.geometry.Outline
import androidx.ui.input.TextInputService
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.compose.frames.FrameCommitObserver
import androidx.compose.frames.FrameReadObserver
import androidx.compose.frames.currentFrame
import androidx.compose.frames.registerCommitObserver
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Shape
import kotlin.math.roundToInt
import java.util.TreeSet
import androidx.compose.trace

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidCraneView constructor(context: Context)
    : ViewGroup(context), Owner, SemanticsTreeProvider {

    val root = LayoutNode()
    // LayoutNodes that need measure and layout, the value is true when measure is needed
    private val relayoutNodes = TreeSet<LayoutNode>(DepthComparator)

    // Map from model to DrawNodes that should be redrawn or LayoutNodes that need measuring
    private val modelToNodes = ObserverMap<Any, ComponentNode>()

    // Map from LayoutNode or DrawNode to the models that they use
    private val nodeToModels = ObserverMap<ComponentNode, Any>()

    // Map from model to LayoutNodes that *only* need layout and not measure
    private val relayoutOnly = ObserverMap<Any, LayoutNode>()

    // RepaintBoundaryNodes that have had their boundary changed. When using Views,
    // the size/position of a View should change during layout, so this list
    // is kept separate from dirtyRepaintBoundaryNodes.
    private val repaintBoundaryChanges = TreeSet<RepaintBoundaryNode>(DepthComparator)

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
        clipChildren = false
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
        layoutNode.visitChildren(::collectChildrenRepaintBoundaries)
        invalidateRepaintBoundary(layoutNode)
//        }
    }

    /**
     * Make sure the containing RepaintBoundary repaints.
     */
    private fun invalidateRepaintBoundary(node: ComponentNode) {
        val repaintBoundary = node.repaintBoundary
        val repaintBoundaryContainer = repaintBoundary?.container
        if (repaintBoundaryContainer != null) {
            repaintBoundaryContainer.dirty = true
            // as we marked this RepaintBoundary as dirty all the parent RepaintBoundaries
            // are also dirty.
            val parent = repaintBoundary.parent
            if (parent != null) {
                invalidateRepaintBoundary(parent)
            }
        } else {
            invalidate()
        }
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
//        ownerScope.launch {
        invalidateRepaintBoundary(layoutNode)
//        }
    }

    override fun onRepaintBoundaryParamsChange(repaintBoundaryNode: RepaintBoundaryNode) {
        repaintBoundaryNode.container.onParamsChange()
    }

    /**
     * Adds all repaint boundaries with the same parent LayoutNode into [repaintBoundaryChanges].
     * When the size or the position of the LayoutNode has been changed all the children
     * [RepaintBoundaryNode] should be repositioned.
     */
    private fun collectChildrenRepaintBoundaries(node: ComponentNode) {
        if (node is RepaintBoundaryNode) {
            repaintBoundaryChanges += node
        }
        if (node !is LayoutNode) {
            node.visitChildren(::collectChildrenRepaintBoundaries)
        }
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        trace("AndroidOwner:onRequestMeasure") {
            // find root of layout request:
            if (layoutNode.needsRemeasure) {
                // don't need to do anything because it already needs to be remeasured
                return
            }
            layoutNode.needsRemeasure = true

            var layout = layoutNode
            while (layout.parentLayoutNode != null && layout.parentLayoutNode != root &&
                layout.affectsParentSize
            ) {
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
        trace("AndroidOwner:measureAndLayout") {
            measureIteration++
            val frame = currentFrame()
            frame.observeReads(frameReadObserver) {
                relayoutNodes.forEach { layoutNode ->
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
                repaintBoundaryChanges.forEach { node ->
                    val parent = node.parentLayoutNode!!
                    node.container.setSize(parent.width.value, parent.height.value)
                }
                relayoutNodes.clear()
                repaintBoundaryChanges.clear()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        trace("AndroidOwner:onMeasure") {
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
        trace("AndroidOwner:onLayout") {
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
        trace("AndroidOwner:callDraw") {
            when (node) {
                is DrawNode -> {
                    val onPaint = node.onPaint
                    val previousNode = currentNode
                    currentNode = node
                    clearNodeModels(node)
                    val receiver = DrawNodeScopeImpl(
                        node, canvas, parentSize,
                        densityReceiver.density
                    )
                    receiver.onPaint(canvas, parentSize)
                    if (!receiver.childDrawn) {
                        receiver.drawChildren()
                    }
                    node.needsPaint = false
                    currentNode = previousNode
                }
                is RepaintBoundaryNode -> {
                    node.container.callDraw(canvas)
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
        trace("AndroidOwner:draw") {
            currentNode = node
            // Start looking for model changes:
            val frame = currentFrame()
            frame.observeReads(frameReadObserver) {
                callChildDraw(canvas, node)
            }
            currentNode = null
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
        trace("AndroidOwner:onTouch") {
            pointerInputEventProcessor.process(event.toPointerInputEvent())
            // TODO(shepshapard): Only return if a child was hit.
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
     * Changes the size of the RepaintBoundary.
     */
    fun setSize(width: Int, height: Int)

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
     * This is not causing re-recording of the RepaintBoundary, but updates params
     * like outline, clipping, elevation or alpha.
     */
    fun onParamsChange()

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
    private val outlineResolver = OutlineResolver(Density(context))
    private val outlineProviderImpl = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: android.graphics.Outline) {
            outlineResolver.applyTo(outline)
        }
    }
    private var clipPath: android.graphics.Path? = null
    override var dirty: Boolean = true
        set(value) {
            if (value && !field) {
                invalidate()
            }
            field = value
        }

    override fun setSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            measure(widthSpec, heightSpec)
            layout(0, 0, width, height)
            onParamsChange()
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
        val clipPath = clipPath
        if (clipPath != null) {
            canvas.save()
            canvas.clipPath(clipPath)
        }
        if (ownerView.currentNode == null) {
            // Only this repaint boundary was invalidated and nothing higher in the view hierarchy.
            // We must observe changes
            ownerView.watchDraw(canvas, repaintBoundaryNode)
        } else {
            // We don't have to observe changes
            ownerView.callChildDraw(canvas, repaintBoundaryNode)
        }
        if (clipPath != null) {
            canvas.restore()
        }
        dirty = false
    }

    override fun attach(parent: RepaintBoundary?) {
        if (parent != null) {
            (parent as RepaintBoundaryView).addView(this)
        } else {
            ownerView.addView(this)
        }
    }

    override fun onParamsChange() {
        outlineResolver.update(repaintBoundaryNode, PxSize(width.px, height.px))
        clipToOutline = outlineResolver.clipToOutline
        this.outlineProvider = if (outlineResolver.hasShape) outlineProviderImpl else null
        if (outlineResolver.manualClipPath !== clipPath) {
            clipPath = outlineResolver.manualClipPath
            dirty = true
        }
        alpha = repaintBoundaryNode.opacity
    }
}

/**
 * RenderNode implementation of RepaintBoundary.
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
            }
            field = value
        }
    val renderNode = RenderNode(repaintBoundaryNode.name)
    private val outline = android.graphics.Outline()
    private val outlineResolver = OutlineResolver(Density(ownerView.context))
    private var clipPath: android.graphics.Path? = null

    override fun setSize(width: Int, height: Int) {
        if (width != renderNode.width || height != renderNode.height) {
            renderNode.setPosition(0, 0, width, height)
            onParamsChange()
        }
    }

    override fun attach(parent: RepaintBoundary?) {
        // nothing needs to be done
    }

    override fun detach() {
        // nothing needs to be done
    }

    override fun callDraw(canvas: Canvas) {
        if (renderNode.alpha > 0f) {
            val androidCanvas = canvas.nativeCanvas
            if (androidCanvas.isHardwareAccelerated) {
                updateDisplayList()
                androidCanvas.drawRenderNode(renderNode)
            } else {
                ownerView.callChildDraw(androidCanvas, repaintBoundaryNode)
                dirty = false
            }
        }
    }

    private fun updateDisplayList() {
        if (dirty || !renderNode.hasDisplayList()) {
            val canvas = renderNode.beginRecording()
            clipPath?.let { canvas.clipPath(it) }
            ownerView.callChildDraw(canvas, repaintBoundaryNode)
            renderNode.endRecording()
            dirty = false
        }
    }

    override fun onParamsChange() {
        val size = PxSize(renderNode.width.px, renderNode.height.px)
        outlineResolver.update(repaintBoundaryNode, size)
        renderNode.clipToOutline = outlineResolver.clipToOutline
        if (outlineResolver.hasShape) {
            renderNode.setOutline(outline.apply { outlineResolver.applyTo(this) })
        } else {
            renderNode.setOutline(null)
        }
        if (outlineResolver.manualClipPath !== clipPath) {
            clipPath = outlineResolver.manualClipPath
            dirty = true
        }
        renderNode.alpha = repaintBoundaryNode.opacity
        ownerView.invalidate()
    }
}

private val RepaintBoundaryNode.container: RepaintBoundary get() = ownerData as RepaintBoundary

private val DepthComparator: Comparator<ComponentNode> = object : Comparator<ComponentNode> {
    override fun compare(l1: ComponentNode, l2: ComponentNode): Int {
        val depth1 = l1.depth
        val depth2 = l2.depth
        val depthDiff = depth1 - depth2
        if (depthDiff != 0) {
            return depthDiff
        }
        return System.identityHashCode(l1) - System.identityHashCode(l2)
    }
}

/**
 * Resolves the Android [Outline] from the [Shape] of [RepaintBoundaryNode].
 */
private class OutlineResolver(private val density: Density) {
    private val cachedOutline = android.graphics.Outline().apply { alpha = 1f }
    private var size: PxSize = PxSize.Zero
    private var shape: Shape? = null
    var manualClipPath: android.graphics.Path? = null
        private set
    var clipToOutline: Boolean = false
        private set
    val hasShape: Boolean get() = shape != null

    fun update(node: RepaintBoundaryNode, size: PxSize) {
        var cacheIsDirty = false
        if (node.shape != shape) {
            this.shape = node.shape
            cacheIsDirty = true
        }
        if (this.size != size) {
            this.size = size
            cacheIsDirty = true
        }
        clipToOutline = (shape != null && node.clipToShape)
        if (cacheIsDirty) {
            manualClipPath = null
            shape?.let { updateCache(it) }
        }
    }

    fun applyTo(outline: android.graphics.Outline) {
        if (shape == null) {
            throw IllegalStateException("Cache is dirty!")
        }
        outline.set(cachedOutline)
    }

    private fun updateCache(shape: Shape) {
        if (size.width == 0.px && size.height == 0.px) {
            cachedOutline.setEmpty()
            return
        }
        val outline = shape.createOutline(size, density)
        when (outline) {
            is Outline.Rectangle -> updateCacheWithRect(outline.rect)
            is Outline.Rounded -> updateCacheWithRRect(outline.rrect)
            is Outline.Generic -> updateCacheWithPath(outline.path)
        }
    }

    private /*inline*/ fun updateCacheWithRect(rect: Rect) {
        cachedOutline.setRect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )
    }

    private /*inline*/ fun updateCacheWithRRect(rrect: RRect) {
        val radius = rrect.topLeftRadiusX
        if (radius == rrect.topLeftRadiusY &&
            radius == rrect.topRightRadiusX &&
            radius == rrect.topRightRadiusY &&
            radius == rrect.bottomRightRadiusX &&
            radius == rrect.bottomRightRadiusY &&
            radius == rrect.bottomLeftRadiusX &&
            radius == rrect.bottomLeftRadiusY
        ) {
            cachedOutline.setRoundRect(
                rrect.left.roundToInt(),
                rrect.top.roundToInt(),
                rrect.right.roundToInt(),
                rrect.bottom.roundToInt(),
                radius
            )
        } else {
            updateCacheWithPath(Path().apply { addRRect(rrect) })
        }
    }

    private fun updateCacheWithPath(composePath: Path) {
        val path = composePath.toFrameworkPath()
        cachedOutline.setConvexPath(path)
        if (clipToOutline) {
            manualClipPath = path
        }
    }
}
