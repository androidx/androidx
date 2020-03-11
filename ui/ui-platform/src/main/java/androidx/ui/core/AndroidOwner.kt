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
import android.content.res.Configuration
import android.graphics.RenderNode
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RestrictTo
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.autofill.performAutofill
import androidx.ui.autofill.populateViewStructure
import androidx.ui.autofill.registerCallback
import androidx.ui.autofill.unregisterCallback
import androidx.ui.core.hapticfeedback.AndroidHapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.pointerinput.MotionEventAdapter
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.SemanticsOwner
import androidx.ui.core.text.AndroidFontResourceLoader
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.graphics.Shape
import androidx.ui.input.TextInputService
import androidx.ui.input.TextInputServiceAndroid
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.px
import androidx.ui.util.trace

import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Method
import kotlin.math.roundToInt

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidComposeView constructor(context: Context) :
    ViewGroup(context), AndroidOwner, SemanticsTreeProvider {

    override var density = Density(context)
        private set

    val root = LayoutNode().also {
        it.measureBlocks = RootMeasureBlocks
        it.layoutDirection = context.getLayoutDirection()
    }

    // LayoutNodes that need measure and layout
    private val relayoutNodes = DepthSortedSet<LayoutNode>(enableExtraAssertions)

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    val autofillTree = AutofillTree()

    // RepaintBoundaryNodes that have had their boundary changed. When using Views,
    // the size/position of a View should change during layout, so this list
    // is kept separate from dirtyRepaintBoundaryNodes.
    private val repaintBoundaryChanges =
        DepthSortedSet<RepaintBoundaryNode>(enableExtraAssertions)

    // RepaintBoundaryNodes that are dirty and should be redrawn. This is only
    // used when RenderNodes are active in Q+. When Views are used, the View
    // system tracks the dirty RenderNodes.
    internal val dirtyRepaintBoundaryNodes =
        DepthSortedSet<RepaintBoundaryNode>(enableExtraAssertions)

    // OwnedLayers that are dirty and should be redrawn.
    internal val dirtyLayers = mutableListOf<OwnedLayer>()

    var ref: Ref<AndroidComposeView>? = null
        set(value) {
            field = value
            if (value != null) {
                value.value = this
            }
        }

    private val motionEventAdapter = MotionEventAdapter()
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    var constraints = Constraints.fixed(width = IntPx.Zero, height = IntPx.Zero)
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
        get() {
            require(duringMeasureLayout) {
                "measureIteration should be only used during the measure/layout pass"
            }
            return field
        }
        private set

    /**
     * Flag to indicate that we're currently measuring.
     */
    private var duringMeasureLayout = false
    /**
     * Stores the list of [LayoutNode]s scheduled to be remeasured in the next measure/layout pass.
     * We were unable to mark them as needsRemeasure=true previously as this request happened
     * during the previous measure/layout pass and they were already measured as part of it.
     * See [onRequestMeasure] for more details.
     */
    private val postponedMeasureRequests = mutableListOf<LayoutNode>()

    private val modelObserver = ModelObserver { command ->
        if (handler.looper === Looper.myLooper()) {
            command()
        } else {
            handler.post(command)
        }
    }

    private val onCommitAffectingMeasure: (LayoutNode) -> Unit = { layoutNode ->
        onRequestMeasure(layoutNode)
    }

    private val onCommitAffectingLayout: (LayoutNode) -> Unit = { layoutNode ->
        requestRelayout(layoutNode)
    }

    private val onCommitAffectingRepaintBoundary: (RepaintBoundaryNode) -> Unit =
        { repaintBoundary ->
            val repaintBoundaryContainer = repaintBoundary.container
            repaintBoundaryContainer.dirty = true
        }

    internal val onCommitAffectingLayer: (OwnedLayer) -> Unit = { layer ->
        layer.invalidate()
    }

    private val onCommitAffectingRootDraw: (Unit) -> Unit = { _ ->
        invalidate()
    }

    private val onCommitAffectingLayerParams: (OwnedLayer) -> Unit = { layer ->
        handler.postAtFrontOfQueue {
            updateLayerProperties(layer)
        }
    }

    private val onPositionedDispatcher = OnPositionedDispatcher()

    override var showLayoutBounds = false
        /** @hide */
        @TestOnly
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        set

    private val consistencyChecker: LayoutTreeConsistencyChecker? =
        if (enableExtraAssertions) {
            LayoutTreeConsistencyChecker(
                root,
                { duringMeasureLayout },
                relayoutNodes,
                postponedMeasureRequests
            )
        } else {
            null
        }

    override fun pauseModelReadObserveration(block: () -> Unit) =
        modelObserver.pauseObservingReads(block)

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
        invalidate(drawNode)
    }

    override fun onInvalidate(layoutNode: LayoutNode) {
        invalidate(layoutNode)
    }

    private fun invalidate(node: ComponentNode) {
        val repaintBoundary = node.repaintBoundary

        // This is going to be slow temporarily until we remove DrawNode
        val layerWrapper = findContainingLayer(node)
        if (layerWrapper != null &&
            layerWrapper.layoutNode.repaintBoundary === repaintBoundary) {
            layerWrapper.layer.invalidate()
            return
        }
        invalidateRepaintBoundary(node)
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
        val repaintBoundary = node.repaintBoundary
        if (repaintBoundary != null && repaintBoundary.isAttached()) {
            repaintBoundary.container.dirty = true
        } else {
            invalidate()
        }
    }

    private fun findContainingLayer(node: ComponentNode): LayerWrapper? {
        val layoutNode = if (node is LayoutNode) node else node.parentLayoutNode ?: return null
        var wrapper: LayoutNodeWrapper? = layoutNode.innerLayoutNodeWrapper
        while (wrapper != null && wrapper !is LayerWrapper) {
            wrapper = wrapper.wrappedBy
        }
        return wrapper as LayerWrapper?
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
            repaintBoundaryChanges.add(node)
        }
        if (node !is LayoutNode) {
            node.visitChildren(::collectChildrenRepaintBoundaries)
        }
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        trace("AndroidOwner:onRequestMeasure") {
            layoutNode.requireOwner()
            if (enableExtraAssertions) {
                Log.d("AndroidOwner", "onRequestMeasure on $layoutNode")
            }
            if (layoutNode.isMeasuring) {
                // we're already measuring it, let's swallow. example when it happens: we compose
                // DataNode inside WithConstraints, this calls onRequestMeasure on DataNode's
                // parent, but this parent is WithConstraints which is currently measuring.
                return
            }
            if (layoutNode.needsRemeasure) {
                // requestMeasure has already been called for this node
                return
            }

            // find root of layout request:
            var layout = layoutNode
            while (layout.affectsParentSize && layout.parentLayoutNode != null) {
                val parent = layout.parentLayoutNode!!
                if (parent.isMeasuring || parent.isLayingOut) {
                    if (layout.measureIteration == measureIteration) {
                        // the node we want to remeasure is the child of the parent which is
                        // currently being measured and this parent did already measure us as a
                        // child. so we have to postpone the measure request till the end of
                        // the measuring pass to remeasure our parent again after it.
                        // this can happen if the already measured child was requested to be
                        // remeasured for example if the used @Model has been modified and the
                        // frame has been committed during the measuring pass.
                        postponedMeasureRequests.add(layout)
                    } else {
                        // otherwise we finished. this child wasn't measured yet, will be
                        // measured soon.
                    }
                    consistencyChecker?.assertConsistent()
                    return
                } else {
                    layout.markRemeasureRequested()
                    if (parent.needsRemeasure) {
                        // don't need to do anything else since the parent is already scheduled
                        // for a remeasuring
                        consistencyChecker?.assertConsistent()
                        return
                    }
                    layout = parent
                }
            }
            layout.markRemeasureRequested()

            requestRelayout(layout.parentLayoutNode ?: layout)
        }
    }

    private fun requestRelayout(layoutNode: LayoutNode) {
        if (layoutNode.needsRelayout || (layoutNode.needsRemeasure && layoutNode !== root) ||
                layoutNode.isLayingOut) {
            // don't need to do anything else since the parent is already scheduled
            // for a relayout (measure pass includes relayout), or is laying out right now
            consistencyChecker?.assertConsistent()
            return
        }
        layoutNode.requireOwner()
        var nodeToRelayout = layoutNode

        // mark alignments as dirty first
        if (!layoutNode.alignmentLinesRequired) {
            // Mark parents alignment lines as dirty, for cases when we needed alignment lines
            // at some point, but currently they are not queried anymore. If they are actively
            // queried, they will be made dirty below in this method.
            var layout: LayoutNode? = layoutNode
            while (layout != null && !layout.dirtyAlignmentLines) {
                layout.dirtyAlignmentLines = true
                layout = layout.parentLayoutNode
            }
        } else {
            var layout = layoutNode
            while (layout != layoutNode.alignmentLinesQueryOwner &&
                // and relayout or remeasure(includes relayout) is not scheduled already
                !(layout.needsRelayout || layout.needsRemeasure)) {
                layout.markRelayoutRequested()
                layout.dirtyAlignmentLines = true
                if (layout.parentLayoutNode == null) break
                layout = layout.parentLayoutNode!!
            }
            layout.dirtyAlignmentLines = true
            nodeToRelayout = layout
        }

        nodeToRelayout.markRelayoutRequested()
        relayoutNodes.add(nodeToRelayout)
        if (!duringMeasureLayout) {
            if (nodeToRelayout == root || constraints.isZero) {
                requestLayout()
            } else {
                // Invalidate and catch measureAndLayout() in the dispatchDraw()
                invalidateRepaintBoundary(nodeToRelayout)
            }
        }
        consistencyChecker?.assertConsistent()
    }

    private fun LayoutNode.markRemeasureRequested() {
        needsRemeasure = true
        if (needsRelayout) {
            // cancel needsRelayout as remeasure includes relayout
            needsRelayout = false
        }
    }

    private fun LayoutNode.markRelayoutRequested() {
        // remeasure includes relayout so we are ok
        if (!needsRemeasure) {
            needsRelayout = true
        }
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()

        if (node is RepaintBoundaryNode) {
            val ownerData = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || isInEditMode()) {
                RepaintBoundaryView(this, node)
            } else {
                RepaintBoundaryRenderNode(this, node)
            }
            node.ownerData = ownerData
            ownerData.attach(node.parent?.repaintBoundary?.container)
            repaintBoundaryChanges.add(node)
            node.parent?.let { invalidateRepaintBoundary(it) }
        }
    }

    override fun onDetach(node: ComponentNode) {
        when (node) {
            is RepaintBoundaryNode -> {
                node.container.detach()
                node.ownerData = null
                repaintBoundaryChanges.remove(node)
                dirtyRepaintBoundaryNodes.remove(node)
            }
            is LayoutNode -> {
                relayoutNodes.remove(node)
            }
        }
        modelObserver.clear(node)
    }

    private val androidViewsHandler by lazy(LazyThreadSafetyMode.NONE) {
        AndroidViewsHandler(context).also { addView(it) }
    }

    override fun addAndroidView(view: View, layoutNode: LayoutNode) {
        androidViewsHandler.addView(view)
        androidViewsHandler.layoutNode[view] = layoutNode
    }

    override fun removeAndroidView(view: View) {
        androidViewsHandler.removeView(view)
        androidViewsHandler.layoutNode.remove(view)
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    override fun measureAndLayout() {
        trace("AndroidOwner:measureAndLayout") {
            if (relayoutNodes.isNotEmpty()) {
                duringMeasureLayout = true
                relayoutNodes.popEach { layoutNode ->
                    measureIteration++
                    if (layoutNode === root) {
                        // it is the root node - the only top node from relayoutNodes
                        // which needs to be remeasured.
                        layoutNode.measure(constraints)
                    }
                    require(!layoutNode.needsRemeasure) {
                        "$layoutNode shouldn't require remeasure. relayoutNodes " +
                                "consists of the top nodes of the affected subtrees"
                    }
                    if (layoutNode.needsRelayout) {
                        layoutNode.layout()
                        onPositionedDispatcher.onNodePositioned(layoutNode)
                        consistencyChecker?.assertConsistent()
                    }
                    // execute postponed `onRequestMeasure`
                    if (postponedMeasureRequests.isNotEmpty()) {
                        postponedMeasureRequests.forEach {
                            if (it.isAttached()) {
                                onRequestMeasure(it)
                            }
                        }
                        postponedMeasureRequests.clear()
                    }
                }
                duringMeasureLayout = false
                onPositionedDispatcher.dispatch()
                consistencyChecker?.assertConsistent()
            }
            repaintBoundaryChanges.popEach { node ->
                val parentNode = node.parentLayoutNode!!
                node.container.setSize(
                    parentNode.innerLayoutNodeWrapper.width.value,
                    parentNode.innerLayoutNodeWrapper.height.value
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        trace("AndroidOwner:onMeasure") {
            val targetWidth = convertMeasureSpec(widthMeasureSpec)
            val targetHeight = convertMeasureSpec(heightMeasureSpec)

            this.constraints = Constraints(
                targetWidth.min, targetWidth.max,
                targetHeight.min, targetHeight.max
            )

            relayoutNodes.add(root)
            onPositionedDispatcher.disableDispatching {
                // we want to postpone onPositioned callbacks until onLayout as LayoutCoordinates
                // are currently wrong if you try to get the global(activity) coordinates -
                // View is not yet laid out.
                measureAndLayout()
            }
            setMeasuredDimension(root.width.value, root.height.value)
        }
    }

    override fun observeDrawModelReads(node: RepaintBoundaryNode, block: () -> Unit) {
        modelObserver.observeReads(node, onCommitAffectingRepaintBoundary, block)
    }

    override fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit) {
        modelObserver.observeReads(node, onCommitAffectingLayout, block)
    }

    override fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit) {
        modelObserver.observeReads(node, onCommitAffectingMeasure, block)
    }

    fun observeLayerModelReads(layer: OwnedLayer, block: () -> Unit) {
        modelObserver.observeReads(layer, onCommitAffectingLayer, block)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        onPositionedDispatcher.dispatch()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    override fun callDraw(
        canvas: Canvas,
        node: ComponentNode,
        parentSize: PxSize
    ) {
        trace("AndroidOwner:callDraw") {
            when (node) {
                is DrawNode -> {
                    val onPaintWithChildren = node.onPaintWithChildren
                    if (onPaintWithChildren != null) {
                        val ownerData = node.ownerData
                        val receiver: DrawReceiverImpl
                        if (ownerData == null) {
                            receiver = DrawReceiverImpl(node, canvas, parentSize, density)
                            node.ownerData = receiver
                        } else {
                            receiver = ownerData as DrawReceiverImpl
                            receiver.childDrawn = false
                            receiver.canvas = canvas
                            receiver.parentSize = parentSize
                            receiver.currentDensity = density
                        }
                        onPaintWithChildren(receiver, canvas, parentSize)
                        if (!receiver.childDrawn) {
                            receiver.drawChildren()
                        }
                    } else {
                        val onPaint = node.onPaint!!
                        density.onPaint(canvas, parentSize)
                    }
                    node.needsPaint = false
                }
                is RepaintBoundaryNode -> {
                    val boundary = node.container
                    boundary.callDraw(canvas)
                }
                is LayoutNode -> {
                    if (node.isPlaced) {
                        require(!node.needsRemeasure) { "$node is not measured, draw requested" }
                        require(!node.needsRelayout) { "$node is not laid out, draw requested" }
                        node.draw(canvas, density)
                    }
                }
                else -> node.visitChildren {
                    callDraw(canvas, it, parentSize)
                }
            }
        }
    }

    override fun createLayer(
        drawLayerModifier: DrawLayerModifier,
        drawBlock: (Canvas, Density) -> Unit
    ): OwnedLayer {
        val layer = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || isInEditMode()) {
            ViewLayer(this, drawLayerModifier, drawBlock)
        } else {
            RenderNodeLayer(this, drawLayerModifier, drawBlock)
        }

        updateLayerProperties(layer)

        return layer
    }

    private fun updateLayerProperties(layer: OwnedLayer) {
        modelObserver.observeReads(layer, onCommitAffectingLayerParams) {
            layer.updateLayerProperties()
        }
    }

    internal fun drawChild(canvas: Canvas, view: View, drawingTime: Long) {
        super.drawChild(canvas.nativeCanvas, view, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        measureAndLayout()
        val uiCanvas = Canvas(canvas)
        val parentSize = PxSize(root.width, root.height)
        uiCanvas.enableZ()
        modelObserver.observeReads(Unit, onCommitAffectingRootDraw) {
            root.visitChildren { callDraw(uiCanvas, it, parentSize) }
        }
        uiCanvas.disableZ()
        dirtyRepaintBoundaryNodes.popEach { node ->
            node.container.updateDisplayList()
        }
        if (dirtyLayers.isNotEmpty()) {
            for (i in 0 until dirtyLayers.size) {
                val layer = dirtyLayers[i]
                layer.updateDisplayList()
            }
            dirtyLayers.clear()
        }
    }

    /**
     * This call converts the framework Canvas to an androidx [Canvas] and paints node's
     * children.
     */
    internal fun callChildDraw(
        canvas: android.graphics.Canvas,
        repaintBoundaryNode: RepaintBoundaryNode
    ) {
        val layoutNode = repaintBoundaryNode.parentLayoutNode!!
        val parentSize = PxSize(
            layoutNode.innerLayoutNodeWrapper.width,
            layoutNode.innerLayoutNodeWrapper.height
        )
        val uiCanvas = Canvas(canvas)
        uiCanvas.enableZ()
        observeDrawModelReads(repaintBoundaryNode) {
            repaintBoundaryNode.visitChildren { child ->
                callDraw(uiCanvas, child, parentSize)
            }
        }
        uiCanvas.disableZ()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showLayoutBounds = getIsShowingLayoutBounds()
        modelObserver.enableModelUpdatesObserving(true)
        ifDebug { if (autofillSupported()) _autofill?.registerCallback() }
        root.attach(this)
        semanticsOwner.invalidateSemanticsRoot()
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
            val pointerInputEvent = motionEventAdapter.processMotionEvent(event)
            if (pointerInputEvent != null) {
                pointerInputEventProcessor.process(pointerInputEvent, calculatePosition())
            } else {
                pointerInputEventProcessor.processCancel()
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

    override fun getAllSemanticNodes(): List<SemanticsNode> {
        return findAllSemanticNodesIn(semanticsOwner.rootSemanticsNode)
    }

    override fun sendEvent(event: MotionEvent) {
        dispatchTouchEvent(event)
    }

    private fun Context.getLayoutDirection() =
        when (applicationContext.resources.configuration.layoutDirection) {
            android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
            android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
            // API doc says Configuration#getLayoutDirection only returns LTR or RTL.
            // Fallback to LTR for unexpected return value.
            else -> LayoutDirection.Ltr
        }

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    val textInputService = TextInputService(textInputServiceAndroid)

    val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    val hapticFeedBack: HapticFeedback =
        AndroidHapticFeedback(this)

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
        var currentDensity: Density
    ) : DrawReceiver {
        internal var childDrawn = false

        override val density: Float get() = currentDensity.density
        override val fontScale: Float get() = currentDensity.fontScale

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

    companion object {
        private var systemPropertiesClass: Class<*>? = null
        private var getBooleanMethod: Method? = null

        // TODO(mount): replace with ViewCompat.isShowingLayoutBounds() when it becomes available.
        @SuppressLint("PrivateApi")
        private fun getIsShowingLayoutBounds(): Boolean {
            try {
                if (systemPropertiesClass == null) {
                    systemPropertiesClass = Class.forName("android.os.SystemProperties")
                    getBooleanMethod = systemPropertiesClass?.getDeclaredMethod(
                        "getBoolean",
                        String::class.java,
                        Boolean::class.java
                    )
                }

                return getBooleanMethod?.invoke(null, "debug.layout", false) as? Boolean ?: false
            } catch (e: Exception) {
                return false
            }
        }

        /**
         * Enables additional (and expensive to do in production) assertions. Useful to be set
         * to true during the tests covering our core logic.
         */
        var enableExtraAssertions: Boolean = false

        private val RootMeasureBlocks = object : LayoutNode.MeasureBlocks {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints,
                layoutDirection: LayoutDirection
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
                density: Density,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun minIntrinsicHeight(
                density: Density,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicWidth(
                density: Density,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicHeight(
                density: Density,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")
        }
    }
}

/**
 * Interface to be implemented by [Owner]s able to handle Android [View]s as part of
 * their hierarchy.
 */
interface AndroidOwner : Owner {
    /**
     * Called to inform the owner that a new Android [View] was [attached][Owner.onAttach]
     * to the hierarchy.
     */
    fun addAndroidView(view: View, layoutNode: LayoutNode)

    /**
     * Called to inform the owner that an Android [View] was [detached][Owner.onDetach]
     * from the hierarchy.
     */
    fun removeAndroidView(view: View)
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
}

/**
 * View implementation of RepaintBoundary.
 */
private class RepaintBoundaryView(
    val ownerView: AndroidComposeView,
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
    private val outlineResolver = OutlineResolverWithNode(density)
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

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        ownerView.measureAndLayout()
        check(hasSize) { "setSize() should be called before drawing the RepaintBoundary" }
        val clipPath = clipPath
        if (clipPath != null) {
            canvas.save()
            canvas.clipPath(clipPath)
        }
        // as we call measure and layout in this callback sometimes it is possible
        // the node we wanted to draw became detached already and it would be incorrect
        // to draw the attached node so we just skip the drawing - this RepaintBoundaryView
        // will be removed from the hierarchy anyway.
        if (repaintBoundaryNode.isAttached()) {
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
        elevation = with(density) { repaintBoundaryNode.elevation.toPx().value }
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
    val repaintBoundaryNode: RepaintBoundaryNode
) : RepaintBoundary {
    override var dirty = true
        set(value) {
            if (value && !field) {
                ownerView.invalidate()
                ownerView.dirtyRepaintBoundaryNodes.add(repaintBoundaryNode)
            }
            field = value
        }
    val renderNode = RenderNode(repaintBoundaryNode.name).apply {
        setHasOverlappingRendering(true)
    }
    private val outline = android.graphics.Outline()
    private val density = Density(ownerView.context)
    private val outlineResolver = OutlineResolverWithNode(density)
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
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            updateDisplayList()
            androidCanvas.drawRenderNode(renderNode)
        } else {
            ownerView.callChildDraw(androidCanvas, repaintBoundaryNode)
            dirty = false
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
        renderNode.elevation = with(density) { repaintBoundaryNode.elevation.toPx().value }
        ownerView.invalidate()
    }
}

private val RepaintBoundaryNode.container: RepaintBoundary get() = ownerData as RepaintBoundary

/**
 * Resolves the Android [Outline] from the [Shape] of [RepaintBoundaryNode].
 */
private class OutlineResolverWithNode(private val density: Density) {
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
        if (hasElevation && path.isConvex) {
            cachedOutline.setConvexPath(path)
        } else {
            cachedOutline.setEmpty()
        }
        outlinePath = path
    }
}