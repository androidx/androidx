/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.LookaheadCapablePlaceable
import androidx.compose.ui.node.MotionReferencePlacementDelegate
import androidx.compose.ui.node.Owner
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its parent layout. Most
 * [Placeable]s are the result of a [Measurable.measure] call.
 *
 * A `Placeable` should never be stored between measure calls.
 */
abstract class Placeable : Measured {
    /**
     * The width, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured width of the layout (aka the `width` value passed into
     * [MeasureScope.layout]), but can be different if the layout does not respect its incoming
     * constraints: in these cases the width will be coerced inside the min and max width
     * constraints - to access the actual width that the layout measured itself to, use
     * [measuredWidth].
     */
    var width: Int = 0
        private set

    /**
     * The height, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured height of the layout (aka the `height` value passed into
     * [MeasureScope.layout]), but can be different if the layout does not respect its incoming
     * constraints: in these cases the height will be coerced inside the min and max height
     * constraints - to access the actual height that the layout measured itself to, use
     * [measuredHeight].
     */
    var height: Int = 0
        private set

    /** The measured width of the layout. This might not respect the measurement constraints. */
    override val measuredWidth: Int
        get() = measuredSize.width

    /** The measured height of the layout. This might not respect the measurement constraints. */
    override val measuredHeight: Int
        get() = measuredSize.height

    /** The measured size of this Placeable. This might not respect [measurementConstraints]. */
    protected var measuredSize: IntSize = IntSize(0, 0)
        set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    private fun onMeasuredSizeChanged() {
        width =
            measuredSize.width.coerceIn(
                measurementConstraints.minWidth,
                measurementConstraints.maxWidth
            )
        height =
            measuredSize.height.coerceIn(
                measurementConstraints.minHeight,
                measurementConstraints.maxHeight
            )
        apparentToRealOffset =
            IntOffset((width - measuredSize.width) / 2, (height - measuredSize.height) / 2)
    }

    /**
     * Place a [Placeable] at [position] in its parent's coordinate system.
     *
     * @param position position in the parent's coordinate system.
     * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
     *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children have
     *   the same [zIndex] the order in which the items were placed is used.
     * @param layerBlock when non-null this [Placeable] should be placed with an introduced graphic
     *   layer. You can configure any layer property available on [GraphicsLayerScope] via this
     *   block. Also if the [Placeable] will be placed with a new [position] next time only the
     *   graphic layer will be moved without requiring to redrawn the [Placeable] content.
     */
    protected abstract fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    )

    /**
     * Place a [Placeable] at [position] in its parent's coordinate system.
     *
     * @param position position in the parent's coordinate system.
     * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
     *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children have
     *   the same [zIndex] the order in which the items were placed is used.
     * @param layer [GraphicsLayer] to place this placeable with. If the [Placeable] will be placed
     *   with a new [position] next time only the graphic layer will be moved without requiring to
     *   redrawn the [Placeable] content.
     */
    protected open fun placeAt(position: IntOffset, zIndex: Float, layer: GraphicsLayer) {
        placeAt(position, zIndex, null)
    }

    /** The constraints used for the measurement made to obtain this [Placeable]. */
    protected var measurementConstraints: Constraints = DefaultConstraints
        set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    /**
     * The offset to be added to an apparent position assigned to this [Placeable] to make it real.
     * The real layout will be centered on the space assigned by the parent, which computed the
     * child's position only seeing its apparent size.
     */
    protected var apparentToRealOffset: IntOffset = IntOffset.Zero
        private set

    /**
     * Receiver scope that permits explicit placement of a [Placeable].
     *
     * While a [Placeable] may be placed at any time, this explicit receiver scope is used to
     * discourage placement outside of [MeasureScope.layout] positioning blocks. This permits
     * Compose UI to perform additional layout optimizations allowing repositioning a [Placeable]
     * without remeasuring its original [Measurable] if factors contributing to its potential
     * measurement have not changed. The scope also allows automatic mirroring of children positions
     * in RTL layout direction contexts using the [placeRelative] methods available in the scope. If
     * the automatic mirroring is not desired, [place] should be used instead.
     */
    // TODO(b/150276678): using the PlacementScope to place outside the layout pass is not working.
    @PlacementScopeMarker
    abstract class PlacementScope {
        /**
         * Keeps the parent layout node's width to make the automatic mirroring of the position in
         * RTL environment. If the value is zero, than the [Placeable] will be be placed to the
         * original position (position will not be mirrored).
         */
        protected abstract val parentWidth: Int

        /**
         * Keeps the layout direction of the parent of the placeable that is being places using
         * current [PlacementScope]. Used to support automatic position mirroring for convenient RTL
         * support in custom layouts.
         */
        protected abstract val parentLayoutDirection: LayoutDirection

        /**
         * The [LayoutCoordinates] of this layout, if known or `null` if the layout hasn't been
         * placed yet. [coordinates] will be `null` when determining alignment lines, preventing
         * alignment lines from depending on absolute coordinates.
         *
         * When [coordinates] is `null`, there will always be a follow-up placement call in which
         * [coordinates] is not-`null`.
         *
         * If you read a position from the coordinates during the placement block the block will be
         * automatically re-executed when the parent layout changes a position. If you don't read it
         * the placement block execution can be skipped as an optimization.
         *
         * @sample androidx.compose.ui.samples.PlacementScopeCoordinatesSample
         */
        open val coordinates: LayoutCoordinates?
            get() = null

        /**
         * Returns the value for this [Ruler] or [defaultValue] if it wasn't
         * [provided][RulerScope.provides]. [Ruler] values are unavailable while calculating
         * [AlignmentLine]s.
         *
         * @sample androidx.compose.ui.samples.RulerConsumerUsage
         */
        open fun Ruler.current(defaultValue: Float): Float = defaultValue

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system. If the layout
         * direction is right-to-left, the given [position] will be horizontally mirrored so that
         * the position of the [Placeable] implicitly reacts to RTL layout direction contexts. If
         * this method is used outside the [MeasureScope.layout] positioning block, the automatic
         * position mirroring will not happen and the [Placeable] will be placed at the given
         * [position], similar to the [place] method.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.placeRelative(position: IntOffset, zIndex: Float = 0f) =
            placeAutoMirrored(position, zIndex, null)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system. If the layout
         * direction is right-to-left, the given position will be horizontally mirrored so that the
         * position of the [Placeable] implicitly reacts to RTL layout direction contexts. If this
         * method is used outside the [MeasureScope.layout] positioning block, the automatic
         * position mirroring will not happen and the [Placeable] will be placed at the given
         * position, similar to the [place] method.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.placeRelative(x: Int, y: Int, zIndex: Float = 0f) =
            placeAutoMirrored(IntOffset(x, y), zIndex, null)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system. Unlike
         * [placeRelative], the given position will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.place(x: Int, y: Int, zIndex: Float = 0f) =
            placeApparentToRealOffset(IntOffset(x, y), zIndex, null)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system. Unlike
         * [placeRelative], the given [position] will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.place(position: IntOffset, zIndex: Float = 0f) =
            placeApparentToRealOffset(position, zIndex, null)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer. If the layout direction is right-to-left, the given [position] will be
         * horizontally mirrored so that the position of the [Placeable] implicitly reacts to RTL
         * layout direction contexts. If this method is used outside the [MeasureScope.layout]
         * positioning block, the automatic position mirroring will not happen and the [Placeable]
         * will be placed at the given [position], similar to the [place] method.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope]
         *   via this block. If the [Placeable] will be placed with a new [position] next time only
         *   the graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            position: IntOffset,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeAutoMirrored(position, zIndex, layerBlock)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer. If the layout direction is right-to-left, the given position will be
         * horizontally mirrored so that the position of the [Placeable] implicitly reacts to RTL
         * layout direction contexts. If this method is used outside the [MeasureScope.layout]
         * positioning block, the automatic position mirroring will not happen and the [Placeable]
         * will be placed at the given position, similar to the [place] method.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope]
         *   via this block. If the [Placeable] will be placed with a new [x] or [y] next time only
         *   the graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            x: Int,
            y: Int,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeAutoMirrored(IntOffset(x, y), zIndex, layerBlock)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer. Unlike [placeRelative], the given position will not implicitly react in
         * RTL layout direction contexts.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope]
         *   via this block. If the [Placeable] will be placed with a new [x] or [y] next time only
         *   the graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            x: Int,
            y: Int,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeApparentToRealOffset(IntOffset(x, y), zIndex, layerBlock)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer. Unlike [placeRelative], the given [position] will not implicitly react in
         * RTL layout direction contexts.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope]
         *   via this block. If the [Placeable] will be placed with a new [position] next time only
         *   the graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            position: IntOffset,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeApparentToRealOffset(position, zIndex, layerBlock)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer. Unlike [placeRelative], the given position will not implicitly react in
         * RTL layout direction contexts.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layer [GraphicsLayer] to place this placeable with. If the [Placeable] will be
         *   placed with a new [x] or [y] next time only the graphic layer will be moved without
         *   requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            x: Int,
            y: Int,
            layer: GraphicsLayer,
            zIndex: Float = 0f,
        ) = placeApparentToRealOffset(IntOffset(x, y), zIndex, layer)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer. Unlike [placeRelative], the given [position] will not implicitly react in
         * RTL layout direction contexts.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layer [GraphicsLayer] to place this placeable with. If the [Placeable] will be
         *   placed with a new [position] next time only the graphic layer will be moved without
         *   requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            position: IntOffset,
            layer: GraphicsLayer,
            zIndex: Float = 0f,
        ) = placeApparentToRealOffset(position, zIndex, layer)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer. If the layout direction is right-to-left, the given position will be
         * horizontally mirrored so that the position of the [Placeable] implicitly reacts to RTL
         * layout direction contexts. If this method is used outside the [MeasureScope.layout]
         * positioning block, the automatic position mirroring will not happen and the [Placeable]
         * will be placed at the given position, similar to the [place] method.
         *
         * @param x x coordinate in the parent's coordinate system.
         * @param y y coordinate in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layer [GraphicsLayer] to place this placeable with. If the [Placeable] will be
         *   placed with a new [x] or [y] next time only the graphic layer will be moved without
         *   requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            x: Int,
            y: Int,
            layer: GraphicsLayer,
            zIndex: Float = 0f
        ) = placeAutoMirrored(IntOffset(x, y), zIndex, layer)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer. If the layout direction is right-to-left, the given [position] will be
         * horizontally mirrored so that the position of the [Placeable] implicitly reacts to RTL
         * layout direction contexts. If this method is used outside the [MeasureScope.layout]
         * positioning block, the automatic position mirroring will not happen and the [Placeable]
         * will be placed at the given [position], similar to the [place] method.
         *
         * @param position position in the parent's coordinate system.
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         *   [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         *   have the same [zIndex] the order in which the items were placed is used.
         * @param layer [GraphicsLayer] to place this placeable with. If the [Placeable] will be
         *   placed with a new [position] next time only the graphic layer will be moved without
         *   requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            position: IntOffset,
            layer: GraphicsLayer,
            zIndex: Float = 0f
        ) = placeAutoMirrored(position, zIndex, layer)

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeAutoMirrored(
            position: IntOffset,
            zIndex: Float,
            noinline layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            if (parentLayoutDirection == LayoutDirection.Ltr || parentWidth == 0) {
                placeApparentToRealOffset(position, zIndex, layerBlock)
            } else {
                placeApparentToRealOffset(
                    IntOffset((parentWidth - width - position.x), position.y),
                    zIndex,
                    layerBlock
                )
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeAutoMirrored(
            position: IntOffset,
            zIndex: Float,
            layer: GraphicsLayer
        ) {
            if (parentLayoutDirection == LayoutDirection.Ltr || parentWidth == 0) {
                placeApparentToRealOffset(position, zIndex, layer)
            } else {
                placeApparentToRealOffset(
                    IntOffset((parentWidth - width - position.x), position.y),
                    zIndex,
                    layer
                )
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeApparentToRealOffset(
            position: IntOffset,
            zIndex: Float,
            noinline layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {
            handleMotionFrameOfReferencePlacement()
            placeAt(position + apparentToRealOffset, zIndex, layerBlock)
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeApparentToRealOffset(
            position: IntOffset,
            zIndex: Float,
            layer: GraphicsLayer
        ) {
            handleMotionFrameOfReferencePlacement()
            placeAt(position + apparentToRealOffset, zIndex, layer)
        }

        /**
         * Internal indicator to know when to tag [Placeable] as placed on the same frame of
         * reference.
         */
        private var motionFrameOfReferencePlacement: Boolean = false

        /**
         * Placement done under [block], will have their [Placeable] placed on the same frame of
         * reference as the current layout.
         *
         * In [LayoutCoordinates], this means that the offset introduced under [block] may be
         * excluded when calculating positions by passing `includeMotionFrameOfReference = false` in
         * [LayoutCoordinates.localPositionOf].
         *
         * Excluding the position set by certain layouts can be helpful to trigger lookahead based
         * animation when intended. The typical case are layouts that change frequently due to a
         * provided value, like [scroll][androidx.compose.foundation.verticalScroll].
         */
        fun withMotionFrameOfReferencePlacement(block: PlacementScope.() -> Unit) {
            motionFrameOfReferencePlacement = true
            block()
            motionFrameOfReferencePlacement = false
        }

        /**
         * Updates the [MotionReferencePlacementDelegate.isPlacedUnderMotionFrameOfReference] flag
         * when called a [Placeable] is placed under [withMotionFrameOfReferencePlacement].
         *
         * Note that the Main/Lookahead pass delegate are expected to propagate the flag to the
         * proper [LookaheadCapablePlaceable].
         */
        private fun Placeable.handleMotionFrameOfReferencePlacement() {
            if (this is MotionReferencePlacementDelegate) {
                this.isPlacedUnderMotionFrameOfReference =
                    this@PlacementScope.motionFrameOfReferencePlacement
            }
        }
    }
}

/** Block on [GraphicsLayerScope] which applies the default layer parameters. */
private val DefaultLayerBlock: GraphicsLayerScope.() -> Unit = {}

private val DefaultConstraints = Constraints()

internal fun PlacementScope(
    lookaheadCapablePlaceable: LookaheadCapablePlaceable
): Placeable.PlacementScope = LookaheadCapablePlacementScope(lookaheadCapablePlaceable)

internal fun PlacementScope(owner: Owner): Placeable.PlacementScope = OuterPlacementScope(owner)

/** PlacementScope used by almost all parts of Compose. */
private class LookaheadCapablePlacementScope(private val within: LookaheadCapablePlaceable) :
    Placeable.PlacementScope() {
    override val parentWidth: Int
        get() = within.measuredWidth

    override val parentLayoutDirection: LayoutDirection
        get() = within.layoutDirection

    override val coordinates: LayoutCoordinates?
        get() {
            val coords = if (within.isPlacingForAlignment) null else within.coordinates
            // if coordinates are not null we will only set this flag when the inner
            // coordinate values are read. see NodeCoordinator.onCoordinatesUsed()
            if (coords == null) {
                within.layoutNode.layoutDelegate.onCoordinatesUsed()
            }
            return coords
        }

    override fun Ruler.current(defaultValue: Float): Float =
        within.findRulerValue(this, defaultValue)
}

/** The PlacementScope that is used at the root of the compose layout hierarchy. */
private class OuterPlacementScope(val owner: Owner) : Placeable.PlacementScope() {
    override val parentWidth: Int
        get() = owner.root.width

    override val parentLayoutDirection: LayoutDirection
        get() = owner.layoutDirection

    override val coordinates: LayoutCoordinates
        get() = owner.root.outerCoordinator
}
