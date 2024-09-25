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

import androidx.collection.MutableObjectFloatMap
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableObjectIntMapOf
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.throwIllegalStateExceptionForNullCheck
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadLayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.layout.RulerScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * This is the base class for NodeCoordinator and LookaheadDelegate. The common functionalities
 * between the two are extracted here.
 */
internal abstract class LookaheadCapablePlaceable :
    Placeable(), MeasureScopeWithLayoutNode, MotionReferencePlacementDelegate {
    abstract val position: IntOffset
    abstract val child: LookaheadCapablePlaceable?
    abstract val parent: LookaheadCapablePlaceable?
    abstract val hasMeasureResult: Boolean
    abstract override val layoutNode: LayoutNode
    abstract val coordinates: LayoutCoordinates
    private var _rulerScope: RulerScope? = null

    /**
     * Indicates whether the [Placeable] was placed under a motion frame of reference.
     *
     * This means, that its offset may be excluded from calculation with
     * `includeMotionFrameOfReference = false` in [LookaheadLayoutCoordinates.localPositionOf].
     */
    override var isPlacedUnderMotionFrameOfReference: Boolean = false

    val rulerScope: RulerScope
        get() {
            return _rulerScope
                ?: object : RulerScope {
                    override val coordinates: LayoutCoordinates
                        get() {
                            this@LookaheadCapablePlaceable.layoutNode.layoutDelegate
                                .onCoordinatesUsed()
                            return this@LookaheadCapablePlaceable.coordinates
                        }

                    override fun Ruler.provides(value: Float) {
                        this@LookaheadCapablePlaceable.provideRulerValue(this, value)
                    }

                    override fun VerticalRuler.providesRelative(value: Float) {
                        this@LookaheadCapablePlaceable.provideRelativeRulerValue(this, value)
                    }

                    override val density: Float
                        get() = this@LookaheadCapablePlaceable.density

                    override val fontScale: Float
                        get() = this@LookaheadCapablePlaceable.fontScale
                }
        }

    final override fun get(alignmentLine: AlignmentLine): Int {
        if (!hasMeasureResult) return AlignmentLine.Unspecified
        val measuredPosition = calculateAlignmentLine(alignmentLine)
        if (measuredPosition == AlignmentLine.Unspecified) return AlignmentLine.Unspecified
        return measuredPosition +
            if (alignmentLine is VerticalAlignmentLine) {
                apparentToRealOffset.x
            } else {
                apparentToRealOffset.y
            }
    }

    abstract fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int

    /**
     * True when the coordinator is running its own placing block to obtain the position in parent,
     * but is not interested in the position of children.
     */
    internal var isShallowPlacing: Boolean = false
    internal abstract val measureResult: MeasureResult

    internal abstract fun replace()

    abstract val alignmentLinesOwner: AlignmentLinesOwner

    /**
     * Used to indicate that this placement pass is for the purposes of calculating an alignment
     * line. If it is, then [LayoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement] will be
     * changed when [Placeable.PlacementScope.coordinates] is accessed to indicate that the
     * placement is not finalized and must be run again.
     */
    internal var isPlacingForAlignment = false

    /** [PlacementScope] used to place children. */
    val placementScope = PlacementScope(this)

    protected fun NodeCoordinator.invalidateAlignmentLinesFromPositionChange() {
        if (wrapped?.layoutNode != layoutNode) {
            alignmentLinesOwner.alignmentLines.onAlignmentsChanged()
        } else {
            alignmentLinesOwner.parentAlignmentLinesOwner?.alignmentLines?.onAlignmentsChanged()
        }
    }

    override val isLookingAhead: Boolean
        get() = false

    private var rulerValues: MutableObjectFloatMap<Ruler>? = null

    // For comparing before and after running the ruler lambda
    private var rulerValuesCache: MutableObjectFloatMap<Ruler>? = null
    private var rulerReaders:
        MutableScatterMap<Ruler, MutableScatterSet<WeakReference<LayoutNode>>>? =
        null

    fun findRulerValue(ruler: Ruler, defaultValue: Float): Float {
        if (isPlacingForAlignment) {
            return defaultValue
        }
        var p: LookaheadCapablePlaceable = this
        while (true) {
            val rulerValue = p.rulerValues?.getOrDefault(ruler, Float.NaN) ?: Float.NaN
            if (!rulerValue.isNaN()) {
                p.addRulerReader(layoutNode, ruler)
                return ruler.calculateCoordinate(rulerValue, p.coordinates, coordinates)
            }
            val parent = p.parent
            if (parent == null) {
                p.addRulerReader(layoutNode, ruler)
                return defaultValue
            }
            p = parent
        }
    }

    private fun addRulerReader(layoutNode: LayoutNode, ruler: Ruler) {
        rulerReaders?.forEachValue { set -> set.removeIf { it.get()?.isAttached != true } }
        rulerReaders?.removeIf { _, value -> value.isEmpty() }
        val readerMap =
            rulerReaders
                ?: MutableScatterMap<Ruler, MutableScatterSet<WeakReference<LayoutNode>>>().also {
                    rulerReaders = it
                }
        val readers = readerMap.getOrPut(ruler) { MutableScatterSet() }
        readers += WeakReference(layoutNode)
    }

    private fun findAncestorRulerDefiner(ruler: Ruler): LookaheadCapablePlaceable {
        var p: LookaheadCapablePlaceable = this
        while (true) {
            if (p.rulerValues?.contains(ruler) == true) {
                return p
            }
            val parent = p.parent ?: return p
            p = parent
        }
    }

    private fun LayoutNode.isLayoutNodeAncestor(ancestor: LayoutNode): Boolean {
        if (this === ancestor) {
            return true
        }
        return parent?.isLayoutNodeAncestor(ancestor) ?: false
    }

    private fun invalidateChildrenOfDefiningRuler(ruler: Ruler) {
        val definer = findAncestorRulerDefiner(ruler)
        val readers = definer.rulerReaders?.remove(ruler)
        if (readers != null) {
            notifyRulerValueChange(readers)
        }
    }

    @Suppress("PrimitiveInCollection")
    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<out AlignmentLine, Int>,
        rulers: (RulerScope.() -> Unit)?,
        placementBlock: PlacementScope.() -> Unit
    ): MeasureResult {
        checkMeasuredSize(width, height)
        return object : MeasureResult {
            override val width: Int
                get() = width

            override val height: Int
                get() = height

            override val alignmentLines: Map<out AlignmentLine, Int>
                get() = alignmentLines

            override val rulers: (RulerScope.() -> Unit)?
                get() = rulers

            override fun placeChildren() {
                placementScope.placementBlock()
            }
        }
    }

    internal fun captureRulers(result: MeasureResult?) {
        if (result != null) {
            captureRulers(PlaceableResult(result, this))
        } else {
            rulerReaders?.forEachValue { notifyRulerValueChange(it) }
            rulerReaders?.clear()
            rulerValues?.clear()
        }
    }

    private fun captureRulers(placeableResult: PlaceableResult) {
        if (isPlacingForAlignment) {
            return
        }
        val rulerLambda = placeableResult.result.rulers
        val rulerReaders = rulerReaders
        if (rulerLambda == null) {
            // Notify anything that read a value it must have a relayout
            if (rulerReaders != null) {
                rulerReaders.forEachValue { notifyRulerValueChange(it) }
                rulerReaders.clear()
            }
        } else {
            val oldValues =
                rulerValuesCache ?: MutableObjectFloatMap<Ruler>().also { rulerValuesCache = it }
            val newValues = rulerValues ?: MutableObjectFloatMap<Ruler>().also { rulerValues = it }
            oldValues.putAll(newValues)
            newValues.clear()
            // capture the new values
            layoutNode.owner?.snapshotObserver?.observeReads(
                placeableResult,
                onCommitAffectingRuler
            ) {
                placeableResult.result.rulers?.invoke(rulerScope)
            }
            // compare the old values to the new ones
            if (rulerReaders != null) {
                // Notify any LayoutNode that got a value that the value has changed
                oldValues.forEach { ruler, oldValue ->
                    val newValue = newValues.getOrDefault(ruler, Float.NaN)
                    if (newValue != oldValue) {
                        // Either the value has changed or it stopped being provided.
                        // Notify all watchers of that value that it has changed.
                        val readers = rulerReaders.remove(ruler)
                        if (readers != null) {
                            notifyRulerValueChange(readers)
                        }
                    }
                }
            }
            // Notify everything that might want to read new values
            newValues.forEachKey { ruler ->
                if (ruler !in oldValues) {
                    parent?.invalidateChildrenOfDefiningRuler(ruler)
                }
            }
            oldValues.clear()
        }
    }

    private fun notifyRulerValueChange(layoutNodes: MutableScatterSet<WeakReference<LayoutNode>>) {
        layoutNodes.forEach { layoutNodeRef ->
            layoutNodeRef.get()?.let { layoutNode ->
                if (isLookingAhead) {
                    layoutNode.requestLookaheadRelayout(false)
                } else {
                    layoutNode.requestRelayout(false)
                }
            }
        }
    }

    fun provideRulerValue(ruler: Ruler, value: Float) {
        val rulerValues = rulerValues ?: MutableObjectFloatMap<Ruler>().also { rulerValues = it }
        rulerValues[ruler] = value
    }

    fun provideRelativeRulerValue(ruler: Ruler, value: Float) {
        val rulerValues = rulerValues ?: MutableObjectFloatMap<Ruler>().also { rulerValues = it }
        rulerValues[ruler] =
            if (layoutDirection == LayoutDirection.Ltr) {
                value
            } else {
                width - value
            }
    }

    companion object {
        private val onCommitAffectingRuler: (PlaceableResult) -> Unit = { result ->
            if (result.isValidOwnerScope) {
                result.placeable.captureRulers(result)
            }
        }
    }
}

private data class PlaceableResult(
    val result: MeasureResult,
    val placeable: LookaheadCapablePlaceable
) : OwnerScope {
    override val isValidOwnerScope: Boolean
        get() = placeable.coordinates.isAttached
}

// This is about 16 million pixels. That should be big enough. We'll treat anything bigger as an
// error.
private const val MaxLayoutDimension = (1 shl 24) - 1
private const val MaxLayoutMask: Int = 0xFF00_0000.toInt()

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkMeasuredSize(width: Int, height: Int) {
    checkPrecondition(width and MaxLayoutMask == 0 && height and MaxLayoutMask == 0) {
        "Size($width x $height) is out of range. Each dimension must be between 0 and " +
            "$MaxLayoutDimension."
    }
}

internal abstract class LookaheadDelegate(
    val coordinator: NodeCoordinator,
) : Measurable, LookaheadCapablePlaceable() {
    override val child: LookaheadCapablePlaceable?
        get() = coordinator.wrapped?.lookaheadDelegate

    override val hasMeasureResult: Boolean
        get() = _measureResult != null

    override var position = IntOffset.Zero
    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null
    override val measureResult: MeasureResult
        get() =
            _measureResult
                ?: throwIllegalStateExceptionForNullCheck(
                    "LookaheadDelegate has not been measured yet when measureResult is requested."
                )

    override val isLookingAhead: Boolean
        get() = true

    override val layoutDirection: LayoutDirection
        get() = coordinator.layoutDirection

    override val density: Float
        get() = coordinator.density

    override val fontScale: Float
        get() = coordinator.fontScale

    override val parent: LookaheadCapablePlaceable?
        get() = coordinator.wrappedBy?.lookaheadDelegate

    override val layoutNode: LayoutNode
        get() = coordinator.layoutNode

    override val coordinates: LayoutCoordinates
        get() = lookaheadLayoutCoordinates

    internal val size: IntSize
        get() = IntSize(width, height)

    internal val constraints: Constraints
        get() = measurementConstraints

    val lookaheadLayoutCoordinates = LookaheadLayoutCoordinates(this)
    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = coordinator.layoutNode.layoutDelegate.lookaheadAlignmentLinesOwner!!

    private var _measureResult: MeasureResult? = null
        set(result) {
            result?.let { measuredSize = IntSize(it.width, it.height) }
                ?: run { measuredSize = IntSize.Zero }
            if (field != result && result != null) {
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if (
                    (!oldAlignmentLines.isNullOrEmpty() || result.alignmentLines.isNotEmpty()) &&
                        result.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    @Suppress("PrimitiveInCollection")
                    val oldLines =
                        oldAlignmentLines
                            ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(result.alignmentLines)
                }
            }
            field = result
        }

    protected val cachedAlignmentLinesMap = mutableObjectIntMapOf<AlignmentLine>()

    internal fun getCachedAlignmentLine(alignmentLine: AlignmentLine): Int =
        cachedAlignmentLinesMap.getOrDefault(alignmentLine, AlignmentLine.Unspecified)

    override fun replace() {
        placeAt(position, 0f, null)
    }

    final override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        placeSelf(position)
        if (isShallowPlacing) return
        placeChildren()
    }

    private fun placeSelf(position: IntOffset) {
        if (this.position != position) {
            this.position = position
            layoutNode.layoutDelegate.lookaheadPassDelegate
                ?.notifyChildrenUsingLookaheadCoordinatesWhilePlacing()
            coordinator.invalidateAlignmentLinesFromPositionChange()
        }
        if (!isPlacingForAlignment) {
            captureRulers(measureResult)
        }
    }

    internal fun placeSelfApparentToRealOffset(position: IntOffset) {
        placeSelf(position + apparentToRealOffset)
    }

    protected open fun placeChildren() {
        measureResult.placeChildren()
    }

    inline fun performingMeasure(constraints: Constraints, block: () -> MeasureResult): Placeable {
        measurementConstraints = constraints
        _measureResult = block()
        return this
    }

    override val parentData: Any?
        get() = coordinator.parentData

    override fun minIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicHeight(width)
    }

    internal fun positionIn(
        ancestor: LookaheadDelegate,
        excludingAgnosticOffset: Boolean,
    ): IntOffset {
        var aggregatedOffset = IntOffset.Zero
        var lookaheadDelegate = this
        while (lookaheadDelegate != ancestor) {
            if (
                !lookaheadDelegate.isPlacedUnderMotionFrameOfReference || !excludingAgnosticOffset
            ) {
                aggregatedOffset += lookaheadDelegate.position
            }
            lookaheadDelegate = lookaheadDelegate.coordinator.wrappedBy!!.lookaheadDelegate!!
        }
        return aggregatedOffset
    }
}
