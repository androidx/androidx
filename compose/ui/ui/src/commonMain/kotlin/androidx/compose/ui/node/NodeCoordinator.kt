/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadLayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.toSize

/**
 * Measurable and Placeable type that has a position.
 */
internal abstract class NodeCoordinator(
    override val layoutNode: LayoutNode,
) :
    LookaheadCapablePlaceable(),
    Measurable,
    LayoutCoordinates,
    OwnerScope {

    internal var forcePlaceWithLookaheadOffset: Boolean = false
    internal var forceMeasureWithLookaheadConstraints: Boolean = false
    abstract val tail: Modifier.Node

    internal var wrapped: NodeCoordinator? = null
    internal var wrappedBy: NodeCoordinator? = null

    override val layoutDirection: LayoutDirection
        get() = layoutNode.layoutDirection

    override val density: Float
        get() = layoutNode.density.density

    override val fontScale: Float
        get() = layoutNode.density.fontScale

    override val parent: LookaheadCapablePlaceable?
        get() = wrappedBy

    override val coordinates: LayoutCoordinates
        get() = this

    override val isPositionedByParentWithDirectManipulation: Boolean
        get() = isDirectManipulationPlacement

    private var released = false

    private fun headNode(includeTail: Boolean): Modifier.Node? {
        return if (layoutNode.outerCoordinator === this) {
            layoutNode.nodes.head
        } else if (includeTail) {
            wrappedBy?.tail?.child
        } else {
            wrappedBy?.tail
        }
    }

    inline fun visitNodes(mask: Int, includeTail: Boolean, block: (Modifier.Node) -> Unit) {
        val stopNode = if (includeTail) tail else (tail.parent ?: return)
        var node: Modifier.Node? = headNode(includeTail)
        while (node != null) {
            if (node.aggregateChildKindSet and mask == 0) return
            if (node.kindSet and mask != 0) block(node)
            if (node === stopNode) break
            node = node.child
        }
    }

    inline fun <reified T> visitNodes(type: NodeKind<T>, block: (T) -> Unit) {
        visitNodes(type.mask, type.includeSelfInTraversal) {
            it.dispatchForKind(type, block)
        }
    }

    private fun hasNode(type: NodeKind<*>): Boolean {
        return headNode(type.includeSelfInTraversal)?.has(type) == true
    }

    fun head(type: NodeKind<*>): Modifier.Node? {
        visitNodes(type.mask, type.includeSelfInTraversal) { return it }
        return null
    }

    // Size exposed to LayoutCoordinates.
    final override val size: IntSize get() = measuredSize

    private var isClipping: Boolean = false

    protected var layerBlock: (GraphicsLayerScope.() -> Unit)? = null
        private set
    private var layerDensity: Density = layoutNode.density
    private var layerLayoutDirection: LayoutDirection = layoutNode.layoutDirection

    private var lastLayerAlpha: Float = 0.8f
    fun isTransparent(): Boolean {
        if (layer != null && lastLayerAlpha <= 0f) return true
        return this.wrappedBy?.isTransparent() ?: return false
    }

    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = layoutNode.layoutDelegate.alignmentLinesOwner

    override val child: LookaheadCapablePlaceable?
        get() = wrapped

    override fun replace() {
        val explicitLayer = explicitLayer
        if (explicitLayer != null) {
            placeAt(position, zIndex, explicitLayer)
        } else {
            placeAt(position, zIndex, layerBlock)
        }
    }

    override val hasMeasureResult: Boolean
        get() = _measureResult != null

    override val isAttached: Boolean
        get() = tail.isAttached

    private var _measureResult: MeasureResult? = null
    override var measureResult: MeasureResult
        get() = _measureResult ?: error(UnmeasuredError)
        internal set(value) {
            val old = _measureResult
            if (value !== old) {
                _measureResult = value
                if (old == null || value.width != old.width || value.height != old.height) {
                    onMeasureResultChanged(value.width, value.height)
                }
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if ((!oldAlignmentLines.isNullOrEmpty() || value.alignmentLines.isNotEmpty()) &&
                    value.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    @Suppress("PrimitiveInCollection")
                    val oldLines = oldAlignmentLines
                        ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(value.alignmentLines)
                }
            }
        }

    abstract var lookaheadDelegate: LookaheadDelegate?
        protected set

    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null

    abstract fun ensureLookaheadDelegateCreated()

    override val providedAlignmentLines: Set<AlignmentLine>
        get() {
            var set: MutableSet<AlignmentLine>? = null
            var coordinator: NodeCoordinator? = this
            while (coordinator != null) {
                val alignmentLines = coordinator._measureResult?.alignmentLines
                if (alignmentLines?.isNotEmpty() == true) {
                    if (set == null) {
                        set = mutableSetOf()
                    }
                    set.addAll(alignmentLines.keys)
                }
                coordinator = coordinator.wrapped
            }
            return set ?: emptySet()
        }

    /**
     * Called when the width or height of [measureResult] change. The object instance pointed to
     * by [measureResult] may or may not have changed.
     */
    protected open fun onMeasureResultChanged(width: Int, height: Int) {
        val layer = layer
        if (layer != null) {
            layer.resize(IntSize(width, height))
        } else {
            wrappedBy?.invalidateLayer()
        }
        measuredSize = IntSize(width, height)
        if (layerBlock != null) {
            updateLayerParameters(invokeOnLayoutChange = false)
        }
        visitNodes(Nodes.Draw) {
            it.onMeasureResultChanged()
        }
        layoutNode.owner?.onLayoutChange(layoutNode)
    }

    override var position: IntOffset = IntOffset.Zero
        protected set

    var zIndex: Float = 0f
        protected set

    override val parentData: Any?
        get() {
            // NOTE: If you make changes to this getter, please check the generated bytecode to
            // ensure no extra allocation is made. See the note below.
            if (layoutNode.nodes.has(Nodes.ParentData)) {
                val thisNode = tail
                // NOTE: Keep this mutable variable scoped inside the if statement. When moved
                // to the outer scope of get(), this causes the compiler to generate a
                // Ref$ObjectRef instance on every call of this getter.
                var data: Any? = null
                layoutNode.nodes.tailToHead { node ->
                    if (node.isKind(Nodes.ParentData)) {
                        node.dispatchForKind(Nodes.ParentData) {
                            data = with(it) { layoutNode.density.modifyParentData(data) }
                        }
                    }
                    if (node === thisNode) return@tailToHead
                }
                return data
            }
            return null
        }

    internal fun onCoordinatesUsed() {
        layoutNode.layoutDelegate.onCoordinatesUsed()
    }

    final override val parentLayoutCoordinates: LayoutCoordinates?
        get() {
            checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
            onCoordinatesUsed()
            return layoutNode.outerCoordinator.wrappedBy
        }

    final override val parentCoordinates: LayoutCoordinates?
        get() {
            checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
            onCoordinatesUsed()
            return wrappedBy
        }

    private var _rectCache: MutableRect? = null
    protected val rectCache: MutableRect
        get() = _rectCache ?: MutableRect(0f, 0f, 0f, 0f).also {
            _rectCache = it
        }

    private val snapshotObserver get() = layoutNode.requireOwner().snapshotObserver

    /**
     * The current layer's positional attributes.
     */
    private var layerPositionalProperties: LayerPositionalProperties? = null

    internal val lastMeasurementConstraints: Constraints get() = measurementConstraints

    protected inline fun performingMeasure(
        constraints: Constraints,
        crossinline block: () -> Placeable
    ): Placeable {
        measurementConstraints = constraints
        return block()
    }

    fun onMeasured() {
        if (hasNode(Nodes.LayoutAware)) {
            Snapshot.withoutReadObservation {
                visitNodes(Nodes.LayoutAware) {
                    it.onRemeasured(measuredSize)
                }
            }
        }
    }

    /**
     * Places the modified child.
     */
    /*@CallSuper*/
    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        if (forcePlaceWithLookaheadOffset) {
            placeSelf(lookaheadDelegate!!.position, zIndex, layerBlock, null)
        } else {
            placeSelf(position, zIndex, layerBlock, null)
        }
    }

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layer: GraphicsLayer
    ) {
        if (forcePlaceWithLookaheadOffset) {
            placeSelf(lookaheadDelegate!!.position, zIndex, null, layer)
        } else {
            placeSelf(position, zIndex, null, layer)
        }
    }

    private fun placeSelf(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        explicitLayer: GraphicsLayer?
    ) {
        if (explicitLayer != null) {
            requirePrecondition(layerBlock == null) {
                "both ways to create layers shouldn't be used together"
            }
            if (this.explicitLayer !== explicitLayer) {
                // reset previous layer object first if the explicitLayer changed
                this.explicitLayer = null
                updateLayerBlock(null)
                this.explicitLayer = explicitLayer
            }
            if (layer == null) {
                layer = layoutNode.requireOwner().createLayer(
                    drawBlock,
                    invalidateParentLayer,
                    explicitLayer
                ).apply {
                    resize(measuredSize)
                    move(position)
                }
                layoutNode.innerLayerCoordinatorIsDirty = true
                invalidateParentLayer()
            }
        } else {
            releaseExplicitLayer()
            updateLayerBlock(layerBlock)
        }
        if (this.position != position) {
            this.position = position
            layoutNode.layoutDelegate.measurePassDelegate
                .notifyChildrenUsingCoordinatesWhilePlacing()
            val layer = layer
            if (layer != null) {
                layer.move(position)
            } else {
                wrappedBy?.invalidateLayer()
            }
            invalidateAlignmentLinesFromPositionChange()
            layoutNode.owner?.onLayoutChange(layoutNode)
        }
        this.zIndex = zIndex
        if (!isPlacingForAlignment) {
            captureRulers(measureResult)
        }
    }

    fun releaseExplicitLayer() {
        if (explicitLayer != null) {
            explicitLayer = null
            updateLayerBlock(null)
        }
    }

    fun placeSelfApparentToRealOffset(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        layer: GraphicsLayer?
    ) {
        placeSelf(position + apparentToRealOffset, zIndex, layerBlock, layer)
    }

    /**
     * Draws the content of the LayoutNode
     */
    fun draw(canvas: Canvas, graphicsLayer: GraphicsLayer?) {
        val layer = layer
        if (layer != null) {
            layer.drawLayer(canvas, graphicsLayer)
        } else {
            val x = position.x.toFloat()
            val y = position.y.toFloat()
            canvas.translate(x, y)
            drawContainedDrawModifiers(canvas, graphicsLayer)
            canvas.translate(-x, -y)
        }
    }

    private fun drawContainedDrawModifiers(canvas: Canvas, graphicsLayer: GraphicsLayer?) {
        val head = head(Nodes.Draw)
        if (head == null) {
            performDraw(canvas, graphicsLayer)
        } else {
            val drawScope = layoutNode.mDrawScope
            drawScope.draw(canvas, size.toSize(), this, head, graphicsLayer)
        }
    }

    open fun performDraw(canvas: Canvas, graphicsLayer: GraphicsLayer?) {
        wrapped?.draw(canvas, graphicsLayer)
    }

    fun onPlaced() {
        visitNodes(Nodes.LayoutAware) {
            it.onPlaced(this)
        }
    }

    // implementation of draw block passed to the OwnedLayer
    @Suppress("LiftReturnOrAssignment")
    private val drawBlock: (Canvas) -> Unit = { canvas ->
        if (layoutNode.isPlaced) {
            snapshotObserver.observeReads(this, onCommitAffectingLayer) {
                drawContainedDrawModifiers(canvas, explicitLayer)
            }
            lastLayerDrawingWasSkipped = false
        } else {
            // The invalidation is requested even for nodes which are not placed. As we are not
            // going to display them we skip the drawing. It is safe to just draw nothing as the
            // layer will be invalidated again when the node will be finally placed.
            lastLayerDrawingWasSkipped = true
        }
    }

    fun updateLayerBlock(
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        forceUpdateLayerParameters: Boolean = false
    ) {
        requirePrecondition(layerBlock == null || explicitLayer == null) {
            "layerBlock can't be provided when explicitLayer is provided"
        }
        val layoutNode = layoutNode
        val updateParameters = forceUpdateLayerParameters || this.layerBlock !== layerBlock ||
            layerDensity != layoutNode.density || layerLayoutDirection != layoutNode.layoutDirection
        this.layerDensity = layoutNode.density
        this.layerLayoutDirection = layoutNode.layoutDirection

        if (layoutNode.isAttached && layerBlock != null) {
            this.layerBlock = layerBlock
            if (layer == null) {
                layer = layoutNode.requireOwner().createLayer(
                    drawBlock,
                    invalidateParentLayer
                ).apply {
                    resize(measuredSize)
                    move(position)
                }
                updateLayerParameters()
                layoutNode.innerLayerCoordinatorIsDirty = true
                invalidateParentLayer()
            } else if (updateParameters) {
                updateLayerParameters()
            }
        } else {
            this.layerBlock = null
            layer?.let {
                it.destroy()
                layoutNode.innerLayerCoordinatorIsDirty = true
                invalidateParentLayer()
                if (isAttached) {
                    layoutNode.owner?.onLayoutChange(layoutNode)
                }
            }
            layer = null
            lastLayerDrawingWasSkipped = false
        }
    }

    private fun updateLayerParameters(invokeOnLayoutChange: Boolean = true) {
        if (explicitLayer != null) {
            // the parameters of the explicit layers are configured differently.
            return
        }
        val layer = layer
        if (layer != null) {
            val layerBlock = checkPreconditionNotNull(layerBlock) {
                "updateLayerParameters requires a non-null layerBlock"
            }
            graphicsLayerScope.reset()
            graphicsLayerScope.graphicsDensity = layoutNode.density
            graphicsLayerScope.size = size.toSize()
            snapshotObserver.observeReads(this, onCommitAffectingLayerParams) {
                layerBlock.invoke(graphicsLayerScope)
            }
            val layerPositionalProperties = layerPositionalProperties
                ?: LayerPositionalProperties().also { layerPositionalProperties = it }
            layerPositionalProperties.copyFrom(graphicsLayerScope)
            layer.updateLayerProperties(
                graphicsLayerScope,
                layoutNode.layoutDirection,
                layoutNode.density,
            )
            isClipping = graphicsLayerScope.clip
            lastLayerAlpha = graphicsLayerScope.alpha
            if (invokeOnLayoutChange) {
                layoutNode.owner?.onLayoutChange(layoutNode)
            }
        } else {
            checkPrecondition(layerBlock == null) { "null layer with a non-null layerBlock" }
        }
    }

    private val invalidateParentLayer: () -> Unit = {
        wrappedBy?.invalidateLayer()
    }

    /**
     * True when the last drawing of this layer didn't draw the real content as the LayoutNode
     * containing this layer was not placed by the parent.
     */
    internal var lastLayerDrawingWasSkipped = false
        private set

    var layer: OwnedLayer? = null
        private set

    private var explicitLayer: GraphicsLayer? = null

    override val isValidOwnerScope: Boolean
        get() = layer != null && !released && layoutNode.isAttached

    val minimumTouchTargetSize: Size
        get() = with(layerDensity) { layoutNode.viewConfiguration.minimumTouchTargetSize.toSize() }

    /**
     * Executes a hit test for this [NodeCoordinator].
     *
     * @param hitTestSource The hit test specifics for pointer input or semantics
     * @param pointerPosition The tested pointer position, which is relative to
     * the [NodeCoordinator].
     * @param hitTestResult The parent [HitTestResult] that any hit should be added to.
     * @param isTouchEvent `true` if this is from a touch source. Touch sources allow for
     * minimum touch target. Semantics hit tests always treat hits as needing minimum touch target.
     * @param isInLayer `true` if the touch event is in the layer of this and all parents or `false`
     * if it is outside the layer, but within the minimum touch target of the edge of the layer.
     * This can only be `false` when [isTouchEvent] is `true` or else a layer miss means the event
     * will be clipped out.
     */
    fun hitTest(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        val head = head(hitTestSource.entityType())
        if (!withinLayerBounds(pointerPosition)) {
            // This missed the clip, but if this layout is too small and this is within the
            // minimum touch target, we still consider it a hit.
            if (isTouchEvent) {
                val distanceFromEdge =
                    distanceInMinimumTouchTarget(pointerPosition, minimumTouchTargetSize)
                if (distanceFromEdge.isFinite() &&
                    hitTestResult.isHitInMinimumTouchTargetBetter(distanceFromEdge, false)
                ) {
                    head.hitNear(
                        hitTestSource,
                        pointerPosition,
                        hitTestResult,
                        isTouchEvent,
                        false,
                        distanceFromEdge
                    )
                } // else it is a complete miss.
            }
        } else if (head == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else if (isPointerInBounds(pointerPosition)) {
            // A real hit
            head.hit(
                hitTestSource,
                pointerPosition,
                hitTestResult,
                isTouchEvent,
                isInLayer
            )
        } else {
            val distanceFromEdge = if (!isTouchEvent) Float.POSITIVE_INFINITY else {
                distanceInMinimumTouchTarget(pointerPosition, minimumTouchTargetSize)
            }

            if (distanceFromEdge.isFinite() &&
                hitTestResult.isHitInMinimumTouchTargetBetter(distanceFromEdge, isInLayer)
            ) {
                // Hit closer than existing handlers, so just record it
                head.hitNear(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            } else {
                head.speculativeHit(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        }
    }

    /**
     * The [NodeCoordinator] had a hit in bounds and can record any children in the
     * [hitTestResult].
     */
    private fun Modifier.Node?.hit(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            hitTestResult.hit(this, isInLayer) {
                nextUntil(hitTestSource.entityType(), Nodes.Layout)
                    .hit(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }
        }
    }

    /**
     * The [NodeCoordinator] had a hit [distanceFromEdge] from the bounds and it is within
     * the minimum touch target distance, so it should be recorded as such in the [hitTestResult].
     */
    private fun Modifier.Node?.hitNear(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean,
        distanceFromEdge: Float
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            // Hit closer than existing handlers, so just record it
            hitTestResult.hitInMinimumTouchTarget(
                this,
                distanceFromEdge,
                isInLayer
            ) {
                nextUntil(hitTestSource.entityType(), Nodes.Layout).hitNear(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        }
    }

    /**
     * The [NodeCoordinator] had a miss, but it hasn't been clipped out. The child must be
     * checked to see if it hit.
     */
    private fun Modifier.Node?.speculativeHit(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean,
        distanceFromEdge: Float
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else if (hitTestSource.interceptOutOfBoundsChildEvents(this)) {
            // We only want to replace the existing touch target if there are better
            // hits in the children
            hitTestResult.speculativeHit(
                this,
                distanceFromEdge,
                isInLayer
            ) {
                nextUntil(hitTestSource.entityType(), Nodes.Layout).speculativeHit(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        } else {
            nextUntil(hitTestSource.entityType(), Nodes.Layout).speculativeHit(
                hitTestSource,
                pointerPosition,
                hitTestResult,
                isTouchEvent,
                isInLayer,
                distanceFromEdge
            )
        }
    }

    /**
     * Do a [hitTest] on the children of this [NodeCoordinator].
     */
    open fun hitTestChild(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        // Also, keep looking to see if we also might hit any children.
        // This avoids checking layer bounds twice as when we call super.hitTest()
        val wrapped = wrapped
        if (wrapped != null) {
            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTest(
                hitTestSource,
                positionInWrapped,
                hitTestResult,
                isTouchEvent,
                isInLayer
            )
        }
    }

    /**
     * Returns the bounds of this [NodeCoordinator], including the minimum touch target.
     */
    fun touchBoundsInRoot(): Rect {
        if (!isAttached) {
            return Rect.Zero
        }

        val root = findRootCoordinates()

        val bounds = rectCache
        val padding = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        bounds.left = -padding.width
        bounds.top = -padding.height
        bounds.right = measuredWidth + padding.width
        bounds.bottom = measuredHeight + padding.height

        var coordinator: NodeCoordinator = this
        while (coordinator !== root) {
            coordinator.rectInParent(
                bounds,
                clipBounds = false,
                clipToMinimumTouchTargetSize = true
            )
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            coordinator = coordinator.wrappedBy!!
        }
        return bounds.toRect()
    }

    override fun screenToLocal(relativeToScreen: Offset): Offset {
        checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
        val owner = layoutNode.requireOwner()
        val positionInRoot = owner.screenToLocal(relativeToScreen)
        val root = findRootCoordinates()
        return localPositionOf(root, positionInRoot)
    }

    override fun localToScreen(relativeToLocal: Offset): Offset {
        checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
        val positionInRoot = localToRoot(relativeToLocal)
        val owner = layoutNode.requireOwner()
        return owner.localToScreen(positionInRoot)
    }

    override fun windowToLocal(relativeToWindow: Offset): Offset {
        checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
        val root = findRootCoordinates()
        val positionInRoot = layoutNode.requireOwner()
            .calculateLocalPosition(relativeToWindow) - root.positionInRoot()
        return localPositionOf(root, positionInRoot)
    }

    override fun localToWindow(relativeToLocal: Offset): Offset {
        val positionInRoot = localToRoot(relativeToLocal)
        val owner = layoutNode.requireOwner()
        return owner.calculatePositionInWindow(positionInRoot)
    }

    private fun LayoutCoordinates.toCoordinator() =
        (this as? LookaheadLayoutCoordinates)?.coordinator ?: this as NodeCoordinator

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset = localPositionOf(sourceCoordinates, relativeToSource, false)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset,
        excludeDirectManipulationOffset: Boolean
    ): Offset {
        if (sourceCoordinates is LookaheadLayoutCoordinates) {
            sourceCoordinates.coordinator.onCoordinatesUsed()
            return -sourceCoordinates.localPositionOf(
                sourceCoordinates = this,
                relativeToSource = -relativeToSource,
                excludeDirectManipulationOffset = excludeDirectManipulationOffset
            )
        }

        val nodeCoordinator = sourceCoordinates.toCoordinator()
        nodeCoordinator.onCoordinatesUsed()
        val commonAncestor = findCommonAncestor(nodeCoordinator)

        var position = relativeToSource
        var coordinator = nodeCoordinator
        while (coordinator !== commonAncestor) {
            position = coordinator.toParentPosition(position, excludeDirectManipulationOffset)
            coordinator = coordinator.wrappedBy!!
        }

        return ancestorToLocal(commonAncestor, position, excludeDirectManipulationOffset)
    }

    override fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        val coordinator = sourceCoordinates.toCoordinator()
        coordinator.onCoordinatesUsed()
        val commonAncestor = findCommonAncestor(coordinator)

        matrix.reset()
        // Transform from the source to the common ancestor
        coordinator.transformToAncestor(commonAncestor, matrix)
        // Transform from the common ancestor to this
        transformFromAncestor(commonAncestor, matrix)
    }

    override fun transformToScreen(matrix: Matrix) {
        val owner = layoutNode.requireOwner()
        val rootCoordinator = findRootCoordinates().toCoordinator()
        transformToAncestor(rootCoordinator, matrix)
        owner.localToScreen(matrix)
    }

    private fun transformToAncestor(ancestor: NodeCoordinator, matrix: Matrix) {
        var wrapper = this
        while (wrapper != ancestor) {
            wrapper.layer?.transform(matrix)
            val position = wrapper.position
            if (position != IntOffset.Zero) {
                tmpMatrix.reset()
                tmpMatrix.translate(position.x.toFloat(), position.y.toFloat())
                matrix.timesAssign(tmpMatrix)
            }
            wrapper = wrapper.wrappedBy!!
        }
    }

    private fun transformFromAncestor(ancestor: NodeCoordinator, matrix: Matrix) {
        if (ancestor != this) {
            wrappedBy!!.transformFromAncestor(ancestor, matrix)
            if (position != IntOffset.Zero) {
                tmpMatrix.reset()
                tmpMatrix.translate(-position.x.toFloat(), -position.y.toFloat())
                matrix.timesAssign(tmpMatrix)
            }
            layer?.inverseTransform(matrix)
        }
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect {
        checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
        checkPrecondition(sourceCoordinates.isAttached) {
            "LayoutCoordinates $sourceCoordinates is not attached!"
        }
        val srcCoordinator = sourceCoordinates.toCoordinator()
        srcCoordinator.onCoordinatesUsed()
        val commonAncestor = findCommonAncestor(srcCoordinator)

        val bounds = rectCache
        bounds.left = 0f
        bounds.top = 0f
        bounds.right = sourceCoordinates.size.width.toFloat()
        bounds.bottom = sourceCoordinates.size.height.toFloat()

        var coordinator = srcCoordinator
        while (coordinator !== commonAncestor) {
            coordinator.rectInParent(bounds, clipBounds)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            coordinator = coordinator.wrappedBy!!
        }

        ancestorToLocal(commonAncestor, bounds, clipBounds)
        return bounds.toRect()
    }

    private fun ancestorToLocal(
        ancestor: NodeCoordinator,
        offset: Offset,
        excludeDirectManipulationOffset: Boolean,
    ): Offset {
        if (ancestor === this) {
            return offset
        }
        val wrappedBy = wrappedBy
        if (wrappedBy == null || ancestor == wrappedBy) {
            return fromParentPosition(offset, excludeDirectManipulationOffset)
        }
        return fromParentPosition(
            position = wrappedBy.ancestorToLocal(ancestor, offset, excludeDirectManipulationOffset),
            excludeDirectManipulationOffset = excludeDirectManipulationOffset
        )
    }

    private fun ancestorToLocal(
        ancestor: NodeCoordinator,
        rect: MutableRect,
        clipBounds: Boolean
    ) {
        if (ancestor === this) {
            return
        }
        wrappedBy?.ancestorToLocal(ancestor, rect, clipBounds)
        return fromParentRect(rect, clipBounds)
    }

    override fun localToRoot(relativeToLocal: Offset): Offset {
        checkPrecondition(isAttached) { ExpectAttachedLayoutCoordinates }
        onCoordinatesUsed()
        var coordinator: NodeCoordinator? = this
        var position = relativeToLocal
        while (coordinator != null) {
            position = coordinator.toParentPosition(position)
            coordinator = coordinator.wrappedBy
        }
        return position
    }

    protected inline fun withPositionTranslation(canvas: Canvas, block: (Canvas) -> Unit) {
        val x = position.x.toFloat()
        val y = position.y.toFloat()
        canvas.translate(x, y)
        block(canvas)
        canvas.translate(-x, -y)
    }

    /**
     * Converts [position] in the local coordinate system to a [Offset] in the
     * [parentLayoutCoordinates] coordinate system.
     */
    open fun toParentPosition(
        position: Offset,
        excludeDirectManipulationOffset: Boolean = false
    ): Offset {
        val layer = layer
        val targetPosition = layer?.mapOffset(position, inverse = false) ?: position
        return if (excludeDirectManipulationOffset && isDirectManipulationPlacement) {
            targetPosition
        } else {
            targetPosition + this.position
        }
    }

    /**
     * Converts [position] in the [parentLayoutCoordinates] coordinate system to a [Offset] in the
     * local coordinate system.
     */
    open fun fromParentPosition(
        position: Offset,
        excludeDirectManipulationOffset: Boolean = false
    ): Offset {
        val relativeToPosition =
            if (excludeDirectManipulationOffset && isDirectManipulationPlacement) {
                position
            } else {
                position - this.position
            }
        val layer = layer
        return layer?.mapOffset(relativeToPosition, inverse = true)
            ?: relativeToPosition
    }

    protected fun drawBorder(canvas: Canvas, paint: Paint) {
        val rect = Rect(
            left = 0.5f,
            top = 0.5f,
            right = measuredSize.width.toFloat() - 0.5f,
            bottom = measuredSize.height.toFloat() - 0.5f
        )
        canvas.drawRect(rect, paint)
    }

    /**
     * This will be called when the [LayoutNode] associated with this [NodeCoordinator] is
     * attached to the [Owner].
     */
    fun onLayoutNodeAttach() {
        // this call will update the parameters of the layer (alpha, scale, etc)
        updateLayerBlock(layerBlock, forceUpdateLayerParameters = true)
        // this call will invalidate the content of the layer
        layer?.invalidate()
    }

    /**
     * This will be called when the [LayoutNode] associated with this [NodeCoordinator] is
     * released or when the [NodeCoordinator] is released (will not be used anymore).
     */
    fun onRelease() {
        releaseExplicitLayer()
        released = true
        // It is important to call invalidateParentLayer() here, even though updateLayerBlock() may
        // call it. The reason is because we end up calling this from the bottom up, which means
        // that if we have two layout modifiers getting removed, where the parent one has a layer
        // and the bottom one doesn't, the parent layer gets invalidated but then removed, leaving
        // no layers invalidated. By always calling this, we ensure that after all nodes are
        // removed at least one layer is invalidated.
        invalidateParentLayer()
        if (layer != null) {
            updateLayerBlock(null)
        }
    }

    /**
     * Modifies bounds to be in the parent NodeCoordinator's coordinates, including clipping,
     * if [clipBounds] is true. If [clipToMinimumTouchTargetSize] is true and the layer clips,
     * then the clip bounds are extended to allow minimum touch target extended area.
     */
    internal fun rectInParent(
        bounds: MutableRect,
        clipBounds: Boolean,
        clipToMinimumTouchTargetSize: Boolean = false
    ) {
        val layer = layer
        if (layer != null) {
            if (isClipping) {
                if (clipToMinimumTouchTargetSize) {
                    val minTouch = minimumTouchTargetSize
                    val horz = minTouch.width / 2f
                    val vert = minTouch.height / 2f
                    bounds.intersect(
                        -horz, -vert, size.width.toFloat() + horz, size.height.toFloat() + vert
                    )
                } else if (clipBounds) {
                    bounds.intersect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                }
                if (bounds.isEmpty) {
                    return
                }
            }
            layer.mapBounds(bounds, inverse = false)
        }

        val x = position.x
        bounds.left += x
        bounds.right += x

        val y = position.y
        bounds.top += y
        bounds.bottom += y
    }

    /**
     * Modifies bounds in the parent's coordinates to be in this NodeCoordinator's
     * coordinates, including clipping, if [clipBounds] is true.
     */
    private fun fromParentRect(bounds: MutableRect, clipBounds: Boolean) {
        val x = position.x
        bounds.left -= x
        bounds.right -= x

        val y = position.y
        bounds.top -= y
        bounds.bottom -= y

        val layer = layer
        if (layer != null) {
            layer.mapBounds(bounds, inverse = true)
            if (isClipping && clipBounds) {
                bounds.intersect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                if (bounds.isEmpty) {
                    return
                }
            }
        }
    }

    protected fun withinLayerBounds(pointerPosition: Offset): Boolean {
        if (!pointerPosition.isFinite) {
            return false
        }
        val layer = layer
        return layer == null || !isClipping || layer.isInLayer(pointerPosition)
    }

    /**
     * Whether a pointer that is relative to the [NodeCoordinator] is in the bounds of this
     * NodeCoordinator.
     */
    protected fun isPointerInBounds(pointerPosition: Offset): Boolean {
        val x = pointerPosition.x
        val y = pointerPosition.y
        return x >= 0f && y >= 0f && x < measuredWidth && y < measuredHeight
    }

    /**
     * Invalidates the layer that this coordinator will draw into.
     */
    open fun invalidateLayer() {
        val layer = layer
        if (layer != null) {
            layer.invalidate()
        } else {
            wrappedBy?.invalidateLayer()
        }
    }

    /**
     * Called when [LayoutNode.modifier] has changed and all the NodeCoordinators have been
     * configured.
     */
    open fun onLayoutModifierNodeChanged() {
        layer?.invalidate()
    }

    internal fun findCommonAncestor(other: NodeCoordinator): NodeCoordinator {
        var ancestor1 = other.layoutNode
        var ancestor2 = layoutNode
        if (ancestor1 === ancestor2) {
            val otherNode = other.tail
            // They are on the same node, but we don't know which is the deeper of the two
            tail.visitLocalAncestors(Nodes.Layout.mask) {
                if (it === otherNode) return other
            }
            return this
        }

        while (ancestor1.depth > ancestor2.depth) {
            ancestor1 = ancestor1.parent!!
        }

        while (ancestor2.depth > ancestor1.depth) {
            ancestor2 = ancestor2.parent!!
        }

        while (ancestor1 !== ancestor2) {
            val parent1 = ancestor1.parent
            val parent2 = ancestor2.parent
            if (parent1 == null || parent2 == null) {
                throw IllegalArgumentException("layouts are not part of the same hierarchy")
            }
            ancestor1 = parent1
            ancestor2 = parent2
        }

        return when {
            ancestor2 === layoutNode -> this
            ancestor1 === other.layoutNode -> other
            else -> ancestor1.innerCoordinator
        }
    }

    fun shouldSharePointerInputWithSiblings(): Boolean {
        val start = headNode(Nodes.PointerInput.includeSelfInTraversal) ?: return false

        if (start.isAttached) {
            start.visitLocalDescendants(Nodes.PointerInput) {
                if (it.sharePointerInputWithSiblings()) return true
            }
        }

        return false
    }

    private fun offsetFromEdge(pointerPosition: Offset): Offset {
        val x = pointerPosition.x
        val horizontal = maxOf(0f, if (x < 0) -x else x - measuredWidth)
        val y = pointerPosition.y
        val vertical = maxOf(0f, if (y < 0) -y else y - measuredHeight)

        return Offset(horizontal, vertical)
    }

    /**
     * Returns the additional amount on the horizontal and vertical dimensions that
     * this extends beyond [width] and [height] on all sides. This takes into account
     * [minimumTouchTargetSize] and [measuredSize] vs. [width] and [height].
     */
    protected fun calculateMinimumTouchTargetPadding(minimumTouchTargetSize: Size): Size {
        val widthDiff = minimumTouchTargetSize.width - measuredWidth.toFloat()
        val heightDiff = minimumTouchTargetSize.height - measuredHeight.toFloat()
        return Size(maxOf(0f, widthDiff / 2f), maxOf(0f, heightDiff / 2f))
    }

    /**
     * The distance within the [minimumTouchTargetSize] of [pointerPosition] to the layout
     * size. If [pointerPosition] isn't within [minimumTouchTargetSize], then
     * [Float.POSITIVE_INFINITY] is returned.
     */
    protected fun distanceInMinimumTouchTarget(
        pointerPosition: Offset,
        minimumTouchTargetSize: Size
    ): Float {
        if (measuredWidth >= minimumTouchTargetSize.width &&
            measuredHeight >= minimumTouchTargetSize.height
        ) {
            // this layout is big enough that it doesn't qualify for minimum touch targets
            return Float.POSITIVE_INFINITY
        }

        val (width, height) = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        val offsetFromEdge = offsetFromEdge(pointerPosition)

        return if ((width > 0f || height > 0f) &&
            offsetFromEdge.x <= width && offsetFromEdge.y <= height
        ) {
            offsetFromEdge.getDistanceSquared()
        } else {
            Float.POSITIVE_INFINITY // miss
        }
    }

    /**
     * [LayoutNode.hitTest] and [LayoutNode.hitTestSemantics] are very similar, but the data
     * used in their implementations are different. This extracts the differences between the
     * two methods into a single interface.
     */
    internal interface HitTestSource {
        /**
         * Returns the [NodeKind] for the hit test target.
         */
        fun entityType(): NodeKind<*>

        /**
         * Pointer input hit tests can intercept child hits when enabled. This returns `true`
         * if the modifier has requested intercepting.
         */
        fun interceptOutOfBoundsChildEvents(node: Modifier.Node): Boolean

        /**
         * Returns false if the parent layout node has a state that suppresses
         * hit testing of its children.
         */
        fun shouldHitTestChildren(parentLayoutNode: LayoutNode): Boolean

        /**
         * Calls a hit test on [layoutNode].
         */
        fun childHitTest(
            layoutNode: LayoutNode,
            pointerPosition: Offset,
            hitTestResult: HitTestResult,
            isTouchEvent: Boolean,
            isInLayer: Boolean
        )
    }

    internal companion object {
        const val ExpectAttachedLayoutCoordinates = "LayoutCoordinate operations are only valid " +
            "when isAttached is true"
        const val UnmeasuredError = "Asking for measurement result of unmeasured layout modifier"
        private val onCommitAffectingLayerParams: (NodeCoordinator) -> Unit = { coordinator ->
            if (coordinator.isValidOwnerScope) {
                // coordinator.layerPositionalProperties should always be non-null here, but
                // we'll just be careful with a null check.
                // todo how this will be communicated to us in the new impl?
                val layerPositionalProperties = coordinator.layerPositionalProperties
                if (layerPositionalProperties == null) {
                    coordinator.updateLayerParameters()
                } else {
                    tmpLayerPositionalProperties.copyFrom(layerPositionalProperties)
                    coordinator.updateLayerParameters()
                    if (!tmpLayerPositionalProperties.hasSameValuesAs(layerPositionalProperties)) {
                        val layoutNode = coordinator.layoutNode
                        val layoutDelegate = layoutNode.layoutDelegate
                        if (layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
                            if (layoutDelegate.coordinatesAccessedDuringModifierPlacement ||
                                layoutDelegate.coordinatesAccessedDuringPlacement) {
                                layoutNode.requestRelayout()
                            }
                            layoutDelegate.measurePassDelegate
                                .notifyChildrenUsingCoordinatesWhilePlacing()
                        }
                        layoutNode.owner?.requestOnPositionedCallback(layoutNode)
                    }
                }
            }
        }
        private val onCommitAffectingLayer: (NodeCoordinator) -> Unit = { coordinator ->
            coordinator.layer?.invalidate()
        }
        private val graphicsLayerScope = ReusableGraphicsLayerScope()
        private val tmpLayerPositionalProperties = LayerPositionalProperties()

        // Used for matrix calculations. It should not be used for anything that could lead to
        // reentrancy.
        private val tmpMatrix = Matrix()

        /**
         * Hit testing specifics for pointer input.
         */
        val PointerInputSource =
            object : HitTestSource {
                override fun entityType() = Nodes.PointerInput

                override fun interceptOutOfBoundsChildEvents(node: Modifier.Node): Boolean {
                    node.dispatchForKind(Nodes.PointerInput) {
                        if (it.interceptOutOfBoundsChildEvents()) return true
                    }
                    return false
                }

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) = true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult,
                    isTouchEvent: Boolean,
                    isInLayer: Boolean
                ) = layoutNode.hitTest(pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }

        /**
         * Hit testing specifics for semantics.
         */
        val SemanticsSource =
            object : HitTestSource {
                override fun entityType() = Nodes.Semantics

                override fun interceptOutOfBoundsChildEvents(node: Modifier.Node) = false

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) =
                    parentLayoutNode.collapsedSemantics?.isClearingSemantics != true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult,
                    isTouchEvent: Boolean,
                    isInLayer: Boolean
                ) = layoutNode.hitTestSemantics(
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer
                )
            }
    }
}

/**
 * These are the components of a layer that changes the position and may lead
 * to an OnGloballyPositionedCallback.
 */
private class LayerPositionalProperties {
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var cameraDistance: Float = DefaultCameraDistance
    private var transformOrigin: TransformOrigin = TransformOrigin.Center

    fun copyFrom(other: LayerPositionalProperties) {
        scaleX = other.scaleX
        scaleY = other.scaleY
        translationX = other.translationX
        translationY = other.translationY
        rotationX = other.rotationX
        rotationY = other.rotationY
        rotationZ = other.rotationZ
        cameraDistance = other.cameraDistance
        transformOrigin = other.transformOrigin
    }

    fun copyFrom(scope: GraphicsLayerScope) {
        scaleX = scope.scaleX
        scaleY = scope.scaleY
        translationX = scope.translationX
        translationY = scope.translationY
        rotationX = scope.rotationX
        rotationY = scope.rotationY
        rotationZ = scope.rotationZ
        cameraDistance = scope.cameraDistance
        transformOrigin = scope.transformOrigin
    }

    fun hasSameValuesAs(other: LayerPositionalProperties): Boolean {
        return scaleX == other.scaleX &&
            scaleY == other.scaleY &&
            translationX == other.translationX &&
            translationY == other.translationY &&
            rotationX == other.rotationX &&
            rotationY == other.rotationY &&
            rotationZ == other.rotationZ &&
            cameraDistance == other.cameraDistance &&
            transformOrigin == other.transformOrigin
    }
}

private fun DelegatableNode.nextUntil(
    type: NodeKind<*>,
    stopType: NodeKind<*>
): Modifier.Node? {
    val child = node.child ?: return null
    if (child.aggregateChildKindSet and type.mask == 0) return null
    var next: Modifier.Node? = child
    while (next != null) {
        val kindSet = next.kindSet
        if (kindSet and stopType.mask != 0) return null
        if (kindSet and type.mask != 0) {
            return next
        }
        next = next.child
    }
    return null
}
