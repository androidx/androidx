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
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.ui.graphics.Canvas
import androidx.ui.input.TextInputService
import androidx.ui.input.TextInputServiceAndroid
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.util.trace
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Method

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
        // This is going to be slow temporarily until we remove DrawNode
        val layerWrapper = findContainingLayer(node)
        if (layerWrapper != null) {
            layerWrapper.layer.invalidate()
            return
        }
        invalidate()
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        // TODO(mount): use ownerScope. This isn't supported by IR compiler yet
        // ownerScope.launch {
        onInvalidate(layoutNode)
        // }
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
        onInvalidate(layoutNode)
        // }
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
                    if (!layout.needsRemeasure) {
                        layout.needsRemeasure = true
                        // parent is currently measuring and we set needsRemeasure to true so if
                        // the parent didn't yet try to measure the node it will remeasure it.
                        // if the parent didn't plan to measure during this pass then needsRemeasure
                        // stay 'true' and we will manually call 'onRequestMeasure' for all
                        // the not-measured nodes in 'postponedMeasureRequests'.
                        postponedMeasureRequests.add(layout)
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
                onInvalidate(nodeToRelayout)
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
    }

    override fun onDetach(node: ComponentNode) {
        when (node) {
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
                            // if it was detached or already measured by the parent then skip it
                            if (it.isAttached() && it.needsRemeasure) {
                                it.needsRemeasure = false
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
        if (dirtyLayers.isNotEmpty()) {
            for (i in 0 until dirtyLayers.size) {
                val layer = dirtyLayers[i]
                layer.updateDisplayList()
            }
            dirtyLayers.clear()
        }
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
