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
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.RenderNode
import android.os.Build
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RequiresApi
import androidx.compose.trace
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.autofill.performAutofill
import androidx.ui.autofill.populateViewStructure
import androidx.ui.autofill.registerCallback
import androidx.ui.autofill.unregisterCallback
import androidx.ui.core.NodeStagesModelObserver.Stage
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.toPointerInputEvent
import androidx.ui.core.text.AndroidFontResourceLoader
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Shape
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Path
import androidx.ui.input.TextInputServiceAndroid
import androidx.ui.input.TextInputService
import androidx.ui.text.font.Font
import java.util.TreeSet
import kotlin.math.roundToInt

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidComposeView constructor(context: Context) :
    ViewGroup(context), Owner, SemanticsTreeProvider, DensityScope {
    override var density: Density = Density(context)
        private set

    val root = LayoutNode().also {
        it.measureBlocks = RootMeasureBlocks
    }

    // LayoutNodes that need measure and layout, the value is true when measure is needed
    private val relayoutNodes = TreeSet<LayoutNode>(DepthComparator)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    val autofillTree = AutofillTree()

    // RepaintBoundaryNodes that have had their boundary changed. When using Views,
    // the size/position of a View should change during layout, so this list
    // is kept separate from dirtyRepaintBoundaryNodes.
    private val repaintBoundaryChanges = TreeSet<RepaintBoundaryNode>(DepthComparator)

    // RepaintBoundaryNodes that are dirty and should be redrawn. This is only
    // used when RenderNodes are active in Q+. When Views are used, the View
    // system tracks the dirty RenderNodes.
    internal val dirtyRepaintBoundaryNodes = TreeSet<RepaintBoundaryNode>(DepthComparator)

    var ref: Ref<AndroidComposeView>? = null
        set(value) {
            field = value
            if (value != null) {
                value.value = this
            }
        }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    var constraints = Constraints.tightConstraints(width = IntPx.Zero, height = IntPx.Zero)
    // TODO(mount): reinstate when coroutines are supported by IR compiler
    // private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    // Used for updating the ConfigurationAmbient when configuration changes - consume the
    // configuration ambient instead of changing this observer if you are writing a component that
    // adapts to configuration changes.
    var configurationChangeObserver: () -> Unit = {}

    private val _autofill = if (autofillSupported()) AndroidAutofill(this, autofillTree) else null

    // Used as an ambient for performing autofill.
    val autofill: Autofill? get() = _autofill

    override var measureIteration: Long = 1L
        private set

    private val elevationHandler =
        if (Build.VERSION.SDK_INT >= 29) {
            ElevationHandler29()
        } else {
            ElevationHandlerCompat(this) { canvas ->
                super.dispatchDraw(canvas)
            }
        }

    /**
     * Flag to indicate that we're measuring and we shouldn't be requesting
     * measure/layout.
     */
    private var duringMeasureLayout = false

    /**
     * This is true when the apk is build with android:debuggable=true.
     * We can enable some additional expensive assertions while in debug mode.
     */
    private val debugMode = 0 != context.applicationInfo.flags and
            ApplicationInfo.FLAG_DEBUGGABLE

    private val modelObserver = NodeStagesModelObserver(debugMode) { stage, affectedNode ->
        when (stage) {
            Stage.Draw -> (affectedNode as DrawNode).invalidate()
            Stage.Measure -> requestMeasure(affectedNode as LayoutNode, false)
            Stage.Layout -> requestRelayout(affectedNode as LayoutNode)
        }
    }

    internal fun isObservingModels() = modelObserver.isObserving

    override fun enableModelReadObserving(enabled: Boolean) {
        modelObserver.modelReadEnabled = enabled
    }

    init {
        setWillNotDraw(false)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusable = View.FOCUSABLE
        }
        isFocusableInTouchMode = true
        clipChildren = false
        root.isPlaced = true
    }

    override fun onInvalidate(drawNode: DrawNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
        // ownerScope.launch {
        invalidateRepaintBoundary(drawNode)
        // }
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
        // ownerScope.launch {
        layoutNode.visitChildren(::collectChildrenRepaintBoundaries)
        invalidateRepaintBoundary(layoutNode)
        // }
    }

    /**
     * Make sure the containing RepaintBoundary repaints.
     */
    internal fun invalidateRepaintBoundary(node: ComponentNode) {
        node.requireOwner()
        val repaintBoundary = node.repaintBoundary
        val repaintBoundaryContainer = repaintBoundary?.container
        if (repaintBoundaryContainer != null) {
            repaintBoundaryContainer.dirty = true
        } else {
            invalidate()
        }
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
        // ownerScope.launch {
        invalidateRepaintBoundary(layoutNode)
        // }
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
        requestMeasure(layoutNode, isNodeChange = true)
    }

    private fun requestMeasure(layoutNode: LayoutNode, isNodeChange: Boolean) {
        trace("AndroidOwner:onRequestMeasure") {
            layoutNode.requireOwner()
            if (layoutNode.needsRemeasure) {
                // requestMeasure has already been called for this node
                return
            }
            layoutNode.needsRemeasure = true

            // find root of layout request:
            var layout = layoutNode
            while (layout.parentLayoutNode != null && layout.parentLayoutNode != root &&
                layout.affectsParentSize && !layout.isMeasuring
            ) {
                layout = layout.parentLayoutNode!!
                if (layout.needsRemeasure) {
                    // don't need to do anything else since the parent already needs measuring
                    return
                }
                layout.needsRemeasure = true
            }

            val parent = layout.parentLayoutNode

            // If we're doing this during the measure/layout
            // then we shouldn't affect the relayoutNodes. This can happen in rare
            // cases when using subcomposition, where the measure lambda is set during
            // measurement.
            if (duringMeasureLayout) {
                check(isNodeChange) { "onRequest called during measure/layout" }
            } else if (parent == null) {
                requestRelayout(layout)
            } else {
                requestRelayout(parent)
            }
        }
    }

    private fun requestRelayout(layoutNode: LayoutNode) {
        layoutNode.requireOwner()
        if (layoutNode == root || constraints.isZero) {
            requestLayout()
            layoutNode.needsRemeasure = true
        } else if (relayoutNodes.isEmpty()) {
            // Invalidate and catch measureAndLayout() in the dispatchDraw()
            invalidateRepaintBoundary(layoutNode)
        }

        if (!layoutNode.alignmentLinesRequired) {
            layoutNode.needsRelayout = true
            relayoutNodes += layoutNode
            // Mark parents alignment lines as dirty, for cases when we needed alignment lines
            // at some point, but currently they are not queried anymore. If they are actively
            // queried, they will be made dirty below in this method.
            var layout: LayoutNode? = layoutNode
            while (layout != null && !layout.dirtyAlignmentLines) {
                layout.dirtyAlignmentLines = true
                layout = layout.parentLayoutNode
            }
            return
        }

        var layout = layoutNode
        while (layout != layoutNode.alignmentLinesQueryOwner && !layout.needsRelayout) {
            layout.needsRelayout = true
            layout.dirtyAlignmentLines = true
            if (layout.parentLayoutNode == null) break
            layout = layout.parentLayoutNode!!
        }
        layout.needsRelayout = true
        layout.dirtyAlignmentLines = true
        relayoutNodes += layout
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()

        if (node is RepaintBoundaryNode) {
            val ownerData = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || isInEditMode()) {
                RepaintBoundaryView(this, elevationHandler, node)
            } else {
                RepaintBoundaryRenderNode(this, elevationHandler, node)
            }
            node.ownerData = ownerData
            ownerData.attach(node.parent?.repaintBoundary?.container)
            repaintBoundaryChanges += node
            node.parent?.let { invalidateRepaintBoundary(it) }
        }
    }

    override fun onDetach(node: ComponentNode) {
        when (node) {
            is RepaintBoundaryNode -> {
                node.container.detach()
                node.ownerData = null
                repaintBoundaryChanges -= node
                dirtyRepaintBoundaryNodes -= node
            }
            is LayoutNode -> {
                relayoutNodes -= node
            }
        }
        modelObserver.onNodeDetached(node)
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    internal fun measureAndLayout() {
        if (relayoutNodes.isEmpty() && repaintBoundaryChanges.isEmpty()) {
            return
        }
        trace("AndroidOwner:measureAndLayout") {
            try {
                duringMeasureLayout = true
                measureIteration++
                val topNode = relayoutNodes.firstOrNull()
                modelObserver.observeReads {
                    relayoutNodes.forEach { layoutNode ->
                        if (layoutNode.needsRemeasure) {
                            val parent = layoutNode.parentLayoutNode
                            if (parent != null) {
                                // This should call measure and layout on the child
                                val parentLayout = parent
                                parent.needsRelayout = true
                                parentLayout.placeChildren()
                            } else {
                                layoutNode.measure(layoutNode.constraints)
                                layoutNode.placeChildren()
                            }
                        } else if (layoutNode.needsRelayout) {
                            layoutNode.placeChildren()
                        }
                    }
                }
                topNode?.dispatchOnPositionedCallbacks()
                repaintBoundaryChanges.forEach { node ->
                    val parentSize = node.parentLayoutNode!!.contentSize
                    node.container.setSize(parentSize.width.value, parentSize.height.value)
                }
                relayoutNodes.clear()
                repaintBoundaryChanges.clear()
            } finally {
                duringMeasureLayout = false
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

            measureIteration++
            modelObserver.observeReads {
                root.measure(constraints)
            }
            setMeasuredDimension(root.width.value, root.height.value)
        }
    }

    override fun onEndLayout(layoutNode: LayoutNode) {
        modelObserver.afterStage(Stage.Layout, layoutNode)
    }

    override fun onEndMeasure(layoutNode: LayoutNode) {
        modelObserver.afterStage(Stage.Measure, layoutNode)
    }

    override fun onStartLayout(layoutNode: LayoutNode) {
        modelObserver.beforeStage(Stage.Layout, layoutNode)
    }

    override fun onStartMeasure(layoutNode: LayoutNode) {
        modelObserver.beforeStage(Stage.Measure, layoutNode)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        relayoutNodes.add(root)
        measureAndLayout()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    fun callDraw(
        canvas: Canvas,
        node: ComponentNode,
        parentSize: PxSize
    ) {
        trace("AndroidOwner:callDraw") {
            when (node) {
                is DrawNode -> {
                    modelObserver.stage(Stage.Draw, node) {
                        val onPaintWithChildren = node.onPaintWithChildren
                        if (onPaintWithChildren != null) {
                            val ownerData = node.ownerData
                            val receiver: DrawReceiverImpl
                            if (ownerData == null) {
                                receiver =
                                    DrawReceiverImpl(node, canvas, parentSize, density)
                                node.ownerData = receiver
                            } else {
                                receiver = ownerData as DrawReceiverImpl
                                receiver.childDrawn = false
                                receiver.canvas = canvas
                                receiver.parentSize = parentSize
                                receiver.density = density
                            }
                            onPaintWithChildren(receiver, canvas, parentSize)
                            if (!receiver.childDrawn) {
                                receiver.drawChildren()
                            }
                        } else {
                            val onPaint = node.onPaint!!
                            this.onPaint(canvas, parentSize)
                        }
                        node.needsPaint = false
                    }
                }
                is RepaintBoundaryNode -> {
                    val container = node.container
                    if (node.elevation > 0.dp) {
                        container.parentElevationHandler.callDrawWithEnabledZ(canvas, container)
                    } else {
                        container.callDraw(canvas)
                    }
                }
                is LayoutNode -> {
                    if (node.isPlaced) {
                        val contentPos = node.contentPosition
                        val doTranslate = contentPos.x != 0.ipx || contentPos.y != 0.ipx
                        if (doTranslate) {
                            canvas.save()
                            canvas.translate(
                                contentPos.x.value.toFloat(),
                                contentPos.y.value.toFloat()
                            )
                        }
                        val contentSize = node.contentSize.toPxSize()
                        node.visitChildren { child ->
                            callDraw(canvas, child, contentSize)
                        }
                        if (doTranslate) {
                            canvas.restore()
                        }
                    }
                }
                else -> node.visitChildren {
                    callDraw(canvas, it, parentSize)
                }
            }
        }
    }

    override fun drawChild(
        canvas: android.graphics.Canvas,
        child: View,
        drawingTime: Long
    ): Boolean {
        if (elevationHandler.handleDrawChild(canvas, child)) {
            return false
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    internal fun drawChild(canvas: Canvas, view: View, drawingTime: Long) {
        super.drawChild(canvas.nativeCanvas, view, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        measureAndLayout()
        watchDraw(canvas, root)
    }

    /**
     * This call converts the framework Canvas to an androidx [Canvas] and paints node's
     * children.
     */
    internal fun callChildDraw(canvas: android.graphics.Canvas, node: ComponentNode) {
        val layoutNode = node as? LayoutNode ?: node.parentLayoutNode!!
        val parentSize = layoutNode.contentSize.toPxSize()
        val uiCanvas = Canvas(canvas)
        node.visitChildren { child ->
            callDraw(uiCanvas, child, parentSize)
        }
    }

    /**
     * Called to draw the root or a repaint boundary node and observe all model reads during
     * the draw calls. Note that this takes a framework Canvas, so it is called only from
     * [AndroidComposeView] or [RepaintBoundaryView].
     */
    internal fun watchDraw(canvas: android.graphics.Canvas, node: ComponentNode) {
        trace("AndroidOwner:draw") {
            modelObserver.observeReads {
                callChildDraw(canvas, node)
                if (dirtyRepaintBoundaryNodes.isNotEmpty()) {
                    dirtyRepaintBoundaryNodes.forEach { node ->
                        node.container.updateDisplayList()
                    }
                    dirtyRepaintBoundaryNodes.clear()
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        modelObserver.enableModelUpdatesObserving(true)
        ifDebug { if (autofillSupported()) _autofill?.registerCallback() }
        root.attach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        modelObserver.enableModelUpdatesObserving(false)
        ifDebug { if (autofillSupported()) _autofill?.unregisterCallback() }
        root.detach()
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure?, flags: Int) {
        if (autofillSupported() && structure != null) _autofill?.populateViewStructure(structure)
    }

    override fun autofill(values: SparseArray<AutofillValue>) {
        if (autofillSupported()) _autofill?.performAutofill(values)
    }

    // TODO(shepshapard): Test this method.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        trace("AndroidOwner:onTouch") {
            if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                pointerInputEventProcessor.processCancel()
            } else {
                pointerInputEventProcessor.process(event.toPointerInputEvent(), calculatePosition())
            }
        }
        // TODO(shepshapard): Only return if some aspect of the change was consumed.
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
        dispatchTouchEvent(event)
    }

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    val textInputService = TextInputService(textInputServiceAndroid)

    val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    override fun onCheckIsTextEditor(): Boolean = textInputServiceAndroid.isEditorFocused()

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        textInputServiceAndroid.createInputConnection(outAttrs)

    override fun calculatePosition(): IntPxPosition {
        val positionArray = intArrayOf(0, 0)
        getLocationOnScreen(positionArray)
        return IntPxPosition(positionArray[0].ipx, positionArray[1].ipx)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        configurationChangeObserver()
    }

    private inner class DrawReceiverImpl(
        private val drawNode: DrawNode,
        var canvas: Canvas,
        var parentSize: PxSize,
        override var density: Density
    ) : DensityScope, DrawReceiver {
        internal var childDrawn = false

        override fun drawChildren() {
            if (childDrawn) {
                throw IllegalStateException("Cannot call drawChildren() twice within Draw element")
            }
            childDrawn = true
            drawNode.visitChildren { child ->
                callDraw(canvas, child, parentSize)
            }
        }
    }

    private fun autofillSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    internal companion object {
        private val RootMeasureBlocks = object : LayoutNode.MeasureBlocks {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureScope.LayoutResult {
                return when {
                    measurables.isEmpty() -> measureScope.layout(IntPx.Zero, IntPx.Zero) {}
                    measurables.size == 1 -> {
                        val placeable = measurables[0].measure(constraints)
                        measureScope.layout(placeable.width, placeable.height) {
                            placeable.place(IntPx.Zero, IntPx.Zero)
                        }
                    }
                    else -> {
                        val placeables = measurables.map { it.measure(constraints) }
                        var maxWidth = IntPx.Zero
                        var maxHeight = IntPx.Zero
                        placeables.forEach { placeable ->
                            maxWidth = max(placeable.width, maxWidth)
                            maxHeight = max(placeable.height, maxHeight)
                        }
                        measureScope.layout(maxWidth, maxHeight) {
                            placeables.forEach { placeable ->
                                placeable.place(IntPx.Zero, IntPx.Zero)
                            }
                        }
                    }
                }
            }

            override fun minIntrinsicWidth(
                densityScope: DensityScope,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx
            ) = error("Undefined intrinsics block and it is required")

            override fun minIntrinsicHeight(
                densityScope: DensityScope,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicWidth(
                densityScope: DensityScope,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicHeight(
                densityScope: DensityScope,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx
            ) = error("Undefined intrinsics block and it is required")
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
     * For RenderNodes, this updates the RenderNode in place. After this, [dirty] must
     * be `false`.
     */
    fun updateDisplayList()

    /**
     * `true` indicates that the RepaintBoundary must be redrawn and `false` indicates
     * that no change has occured since the previous [callDraw] call.
     */
    var dirty: Boolean

    /**
     * The ElevationHandler of a parent for this RepaintBoundary. this can be or
     * an owner view ElevationHandler or the parent RepaintBoundary's one.
     */
    val parentElevationHandler: ElevationHandler
}

/**
 * View implementation of RepaintBoundary.
 */
private class RepaintBoundaryView(
    val ownerView: AndroidComposeView,
    val ownerElevationHandler: ElevationHandler,
    val repaintBoundaryNode: RepaintBoundaryNode
) : ViewGroup(ownerView.context), RepaintBoundary {
    init {
        setTag(repaintBoundaryNode.name)
        // In the future, we want to have clipChildren = true so that we get better performance.
        // We can do that once the size of draw functions are well understood.
        clipChildren = false
        setWillNotDraw(false) // we WILL draw
        id = View.generateViewId()
    }

    private val density = Density(context)
    private val outlineResolver = OutlineResolver(density)
    private val outlineProviderImpl = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: android.graphics.Outline) {
            outlineResolver.applyTo(outline)
        }
    }
    private var clipPath: android.graphics.Path? = null
    private var hasSize = false
    override var dirty: Boolean = true
        set(value) {
            if (value && !field) {
                invalidate()
            }
            field = value
        }
    private val elevationHandler =
        if (Build.VERSION.SDK_INT >= 29) {
            ElevationHandler29()
        } else {
            ElevationHandlerCompat(this) { canvas ->
                super.dispatchDraw(canvas)
            }
        }

    override val parentElevationHandler: ElevationHandler
        get() {
            val parentView = parent
            return if (parentView is RepaintBoundaryView) {
                parentView.elevationHandler
            } else {
                ownerElevationHandler
            }
        }

    override fun setSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            measure(widthSpec, heightSpec)
            layout(0, 0, width, height)
            onParamsChange()
        } else if (!hasSize) {
            // we need to update params after attaching even if size has not been changed
            onParamsChange()
        }
        hasSize = true
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
        (parent as? ViewGroup)?.removeView(this)
    }

    fun drawChild(canvas: Canvas, child: View, drawingTime: Long) {
        super.drawChild(canvas.nativeCanvas, child, drawingTime)
    }

    override fun drawChild(
        canvas: android.graphics.Canvas,
        child: View,
        drawingTime: Long
    ): Boolean {
        if (elevationHandler.handleDrawChild(canvas, child)) {
            return false
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        ownerView.measureAndLayout()
        check(hasSize) { "setSize() should be called before drawing the RepaintBoundary" }
        val clipPath = clipPath
        if (clipPath != null) {
            canvas.save()
            canvas.clipPath(clipPath)
        }
        if (!ownerView.isObservingModels()) {
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
        hasSize = false
    }

    override fun onParamsChange() {
        outlineResolver.update(repaintBoundaryNode, PxSize(width.px, height.px))
        clipToOutline = outlineResolver.clipToOutline
        this.outlineProvider = if (outlineResolver.hasOutline) outlineProviderImpl else null
        if (outlineResolver.manualClipPath !== clipPath) {
            clipPath = outlineResolver.manualClipPath
            dirty = true
        }
        alpha = repaintBoundaryNode.opacity
        elevation = withDensity(density) { repaintBoundaryNode.elevation.toPx().value }
    }

    override fun updateDisplayList() {
        // Do nothing. This is really only for RenderNodes
    }
}

/**
 * RenderNode implementation of RepaintBoundary.
 */
@TargetApi(29)
private class RepaintBoundaryRenderNode(
    val ownerView: AndroidComposeView,
    override val parentElevationHandler: ElevationHandler,
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
    val renderNode = RenderNode(repaintBoundaryNode.name).apply {
        setHasOverlappingRendering(true)
    }
    private val outline = android.graphics.Outline()
    private val density = Density(ownerView.context)
    private val outlineResolver = OutlineResolver(density)
    private var clipPath: android.graphics.Path? = null
    private var hasSize = false

    override fun setSize(width: Int, height: Int) {
        if (width != renderNode.width || height != renderNode.height) {
            renderNode.setPosition(0, 0, width, height)
            onParamsChange()
        } else if (!hasSize) {
            // we need to update params after attaching even if size has not been changed
            onParamsChange()
        }
        hasSize = true
        dirty = true
    }

    override fun attach(parent: RepaintBoundary?) {
        hasSize = false
    }

    override fun detach() {
        repaintBoundaryNode.parent?.let { ownerView.invalidateRepaintBoundary(it) }
    }

    override fun callDraw(canvas: Canvas) {
        check(hasSize) { "setSize() should be called before drawing the RepaintBoundary" }
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

    override fun updateDisplayList() {
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
        renderNode.clipToOutline = outlineResolver.clipToOutline && outlineResolver.hasOutline
        if (outlineResolver.hasOutline) {
            renderNode.setOutline(outline.apply { outlineResolver.applyTo(this) })
        } else {
            renderNode.setOutline(null)
        }
        if (outlineResolver.manualClipPath !== clipPath) {
            clipPath = outlineResolver.manualClipPath
            dirty = true
        }
        renderNode.alpha = repaintBoundaryNode.opacity
        renderNode.elevation = withDensity(density) { repaintBoundaryNode.elevation.toPx().value }
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
    private var clipToShape: Boolean = false
    private var hasElevation: Boolean = false
    private var outlinePath: android.graphics.Path? = null
    val hasOutline: Boolean get() = !cachedOutline.isEmpty
    val clipToOutline: Boolean get() = clipToShape && manualClipPath == null
    val manualClipPath: android.graphics.Path? get() = if (clipToShape) outlinePath else null

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
        hasElevation = node.elevation > 0.dp
        clipToShape = (shape != null && node.clipToShape)
        if (cacheIsDirty) {
            outlinePath = null
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
        if (hasElevation) {
            if (path.isConvex) {
                cachedOutline.setConvexPath(path)
            } else {
                throw IllegalStateException("Only convex paths can be used for drawing the shadow!")
            }
        } else {
            cachedOutline.setEmpty()
        }
        outlinePath = path
    }
}

private interface ElevationHandler {
    fun callDrawWithEnabledZ(canvas: Canvas, boundary: RepaintBoundary)
    fun handleDrawChild(canvas: android.graphics.Canvas, child: View): Boolean
}

@RequiresApi(29)
private class ElevationHandler29 : ElevationHandler {

    override fun callDrawWithEnabledZ(canvas: Canvas, boundary: RepaintBoundary) {
        val nativeCanvas = canvas.nativeCanvas
        nativeCanvas.enableZ()
        boundary.callDraw(canvas)
        nativeCanvas.disableZ()
    }

    override fun handleDrawChild(canvas: android.graphics.Canvas, child: View) = false
}

/**
 * Compatible version of Canvas.enableZ()/disableZ().
 *
 * On API 29 we can just do:
 * canvas.enableZ()
 * node.container.callDraw(canvas)
 * canvas.disableZ()
 *
 * But on older versions there is no such methods, but ViewGroup will call
 * them internally inside dispatchDraw before calling the drawChild method.
 * We can use this mechanism for our need just to have Canvas with Z enabled.
 *
 * So if we add a fake view, then call dispatchDraw manually, remember that
 * if wasn't called by system(fakeDrawPass) and then when we handle next
 * drawChild callback we would have a Canvas with z enabled. As we check for
 * the fakeDrawPass we wouldn't draw other children Views ViewGroup can have.
 * Then instead of drawing our fake view we draw our stored RepaintBoundary.
 */
private class ElevationHandlerCompat(
    private val viewGroup: ViewGroup,
    private val superDispatchDraw: (android.graphics.Canvas) -> Unit
) : ElevationHandler {
    private val fakeChild: View = View(viewGroup.context).apply {
        setWillNotDraw(true)
        viewGroup.addView(this)
    }
    private var fakeDrawPass: Boolean = false
    private var boundary: RepaintBoundary? = null

    override fun callDrawWithEnabledZ(canvas: Canvas, boundary: RepaintBoundary) {
        this.boundary = boundary
        fakeDrawPass = true
        superDispatchDraw(canvas.nativeCanvas)
        fakeDrawPass = false
    }

    override fun handleDrawChild(canvas: android.graphics.Canvas, child: View): Boolean {
        return if (fakeDrawPass) {
            if (child === fakeChild) {
                boundary?.callDraw(Canvas(canvas))
                boundary = null
            }
            true
        } else {
            false
        }
    }
}
