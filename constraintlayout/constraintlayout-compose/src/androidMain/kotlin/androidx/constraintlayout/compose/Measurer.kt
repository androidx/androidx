/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.util.Log
import androidx.collection.IntIntPair
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.constraintlayout.core.state.WidgetFrame
import androidx.constraintlayout.core.widgets.ConstraintWidget
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_PARENT
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
import androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD
import androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.core.widgets.Guideline
import androidx.constraintlayout.core.widgets.VirtualLayout
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.Measure.TRY_GIVEN_DIMENSIONS
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.Measure.USE_GIVEN_DIMENSIONS

private const val DEBUG = false

/**
 * Returns the Id set from either [LayoutIdParentData] or [ConstraintLayoutParentData]. Otherwise
 * returns "null".
 */
internal val Measurable.anyOrNullId: String
    get() = (this.layoutId ?: this.constraintLayoutId)?.toString() ?: "null"

/**
 * Measurer "bridge" for ConstraintLayout in Compose.
 *
 * Takes ConstraintSets and Measurables and passes it to [ConstraintWidgetContainer] for measurement
 * (Measurables are measured in the [measure] callback).
 */
@PublishedApi
internal open class Measurer2(
    density: Density // TODO: Change to a variable since density may change
) : BasicMeasure.Measurer, DesignInfoProvider {
    private var computedLayoutResult: String = ""
    protected var layoutInformationReceiver: LayoutInformationReceiver? = null
    protected val root = ConstraintWidgetContainer(0, 0).also { it.measurer = this }
    /**
     * Mapping between [Measurable]s and their corresponding [Placeable] result.
     *
     * Due to Lookahead measure pass, any object holding Measurables should not be instantiated
     * internally. Instead, this object is expected to be instantiated from the MeasurePolicy, and
     * passed to update this reference for each the Measure and Layout steps ([performMeasure] and
     * [performLayout]).
     *
     * This way, we have different containers tracking the Measurable states for Lookahead and
     * non-Lookahead pass.
     */
    protected var placeables = mutableMapOf<Measurable, Placeable>()

    /** Mapping of Id to last width and height measurements. */
    private val lastMeasures = mutableMapOf<String, Array<Int>>()

    /** Mapping of Id to interpolated frame. */
    protected val frameCache = mutableMapOf<String, WidgetFrame>()

    protected val state = State(density)

    private val widthConstraintsHolder = IntArray(2)
    private val heightConstraintsHolder = IntArray(2)

    var forcedScaleFactor = Float.NaN
    val layoutCurrentWidth: Int
        get() = root.width

    val layoutCurrentHeight: Int
        get() = root.height

    /**
     * Method called by Compose tooling. Returns a JSON string that represents the Constraints
     * defined for this ConstraintLayout Composable.
     */
    override fun getDesignInfo(startX: Int, startY: Int, args: String) =
        parseConstraintsToJson(root, state, startX, startY, args)

    /** Measure the given [constraintWidget] with the specs defined by [measure]. */
    override fun measure(constraintWidget: ConstraintWidget, measure: BasicMeasure.Measure) {
        val widgetId = constraintWidget.stringId

        if (DEBUG) {
            Log.d("CCL", "Measuring $widgetId with: " + constraintWidget.toDebugString() + "\n")
        }

        val measurableLastMeasures = lastMeasures[widgetId]
        obtainConstraints(
            measure.horizontalBehavior,
            measure.horizontalDimension,
            constraintWidget.mMatchConstraintDefaultWidth,
            measure.measureStrategy,
            (measurableLastMeasures?.get(1) ?: 0) == constraintWidget.height,
            constraintWidget.isResolvedHorizontally,
            state.rootIncomingConstraints.maxWidth,
            widthConstraintsHolder
        )
        obtainConstraints(
            measure.verticalBehavior,
            measure.verticalDimension,
            constraintWidget.mMatchConstraintDefaultHeight,
            measure.measureStrategy,
            (measurableLastMeasures?.get(0) ?: 0) == constraintWidget.width,
            constraintWidget.isResolvedVertically,
            state.rootIncomingConstraints.maxHeight,
            heightConstraintsHolder
        )

        var constraints =
            Constraints(
                widthConstraintsHolder[0],
                widthConstraintsHolder[1],
                heightConstraintsHolder[0],
                heightConstraintsHolder[1]
            )

        if (
            (measure.measureStrategy == TRY_GIVEN_DIMENSIONS ||
                measure.measureStrategy == USE_GIVEN_DIMENSIONS) ||
                !(measure.horizontalBehavior == MATCH_CONSTRAINT &&
                    constraintWidget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD &&
                    measure.verticalBehavior == MATCH_CONSTRAINT &&
                    constraintWidget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD)
        ) {
            if (DEBUG) {
                Log.d("CCL", "Measuring $widgetId with $constraints")
            }
            val result = measureWidget(constraintWidget, constraints)
            constraintWidget.isMeasureRequested = false
            if (DEBUG) {
                Log.d("CCL", "$widgetId is size ${result.first} ${result.second}")
            }

            val coercedWidth =
                result.first.coerceIn(
                    constraintWidget.mMatchConstraintMinWidth.takeIf { it > 0 },
                    constraintWidget.mMatchConstraintMaxWidth.takeIf { it > 0 }
                )
            val coercedHeight =
                result.second.coerceIn(
                    constraintWidget.mMatchConstraintMinHeight.takeIf { it > 0 },
                    constraintWidget.mMatchConstraintMaxHeight.takeIf { it > 0 }
                )

            var remeasure = false
            if (coercedWidth != result.first) {
                constraints =
                    Constraints(
                        minWidth = coercedWidth,
                        minHeight = constraints.minHeight,
                        maxWidth = coercedWidth,
                        maxHeight = constraints.maxHeight
                    )
                remeasure = true
            }
            if (coercedHeight != result.second) {
                constraints =
                    Constraints(
                        minWidth = constraints.minWidth,
                        minHeight = coercedHeight,
                        maxWidth = constraints.maxWidth,
                        maxHeight = coercedHeight
                    )
                remeasure = true
            }
            if (remeasure) {
                if (DEBUG) {
                    Log.d("CCL", "Remeasuring coerced $widgetId with $constraints")
                }
                measureWidget(constraintWidget, constraints)
                constraintWidget.isMeasureRequested = false
            }
        }

        val currentPlaceable = placeables[constraintWidget.companionWidget]
        measure.measuredWidth = currentPlaceable?.width ?: constraintWidget.width
        measure.measuredHeight = currentPlaceable?.height ?: constraintWidget.height
        val baseline =
            if (currentPlaceable != null && state.isBaselineNeeded(constraintWidget)) {
                currentPlaceable[FirstBaseline]
            } else {
                AlignmentLine.Unspecified
            }
        measure.measuredHasBaseline = baseline != AlignmentLine.Unspecified
        measure.measuredBaseline = baseline
        lastMeasures
            .getOrPut(widgetId) { arrayOf(0, 0, AlignmentLine.Unspecified) }
            .copyFrom(measure)

        measure.measuredNeedsSolverPass =
            measure.measuredWidth != measure.horizontalDimension ||
                measure.measuredHeight != measure.verticalDimension
    }

    fun addLayoutInformationReceiver(layoutReceiver: LayoutInformationReceiver?) {
        layoutInformationReceiver = layoutReceiver
        layoutInformationReceiver?.setLayoutInformation(computedLayoutResult)
    }

    open fun computeLayoutResult() {
        val json = StringBuilder()
        json.append("{ ")
        json.append("  root: {")
        json.append("interpolated: { left:  0,")
        json.append("  top:  0,")
        json.append("  right:   ${root.width} ,")
        json.append("  bottom:  ${root.height} ,")
        json.append(" } }")

        @Suppress("ListIterator")
        for (child in root.children) {
            val measurable = child.companionWidget
            if (measurable !is Measurable) {
                if (child is Guideline) {
                    json.append(" ${child.stringId}: {")
                    if (child.orientation == ConstraintWidget.HORIZONTAL) {
                        json.append(" type: 'hGuideline', ")
                    } else {
                        json.append(" type: 'vGuideline', ")
                    }
                    json.append(" interpolated: ")
                    json.append(
                        " { left: ${child.x}, top: ${child.y}, " +
                            "right: ${child.x + child.width}, " +
                            "bottom: ${child.y + child.height} }"
                    )
                    json.append("}, ")
                }
                continue
            }
            if (child.stringId == null) {
                val id = measurable.layoutId ?: measurable.constraintLayoutId
                child.stringId = id?.toString()
            }
            val frame = frameCache[measurable.anyOrNullId]?.widget?.frame
            if (frame == null) {
                continue
            }
            json.append(" ${child.stringId}: {")
            json.append(" interpolated : ")
            frame.serialize(json, true)
            json.append("}, ")
        }
        json.append(" }")
        computedLayoutResult = json.toString()
        layoutInformationReceiver?.setLayoutInformation(computedLayoutResult)
    }

    /**
     * Calculates the [Constraints] in one direction that should be used to measure a child, based
     * on the solver measure request. Returns `true` if the constraints correspond to a wrap content
     * measurement.
     */
    private fun obtainConstraints(
        dimensionBehaviour: ConstraintWidget.DimensionBehaviour,
        dimension: Int,
        matchConstraintDefaultDimension: Int,
        measureStrategy: Int,
        otherDimensionResolved: Boolean,
        currentDimensionResolved: Boolean,
        rootMaxConstraint: Int,
        outConstraints: IntArray
    ): Boolean =
        when (dimensionBehaviour) {
            FIXED -> {
                outConstraints[0] = dimension
                outConstraints[1] = dimension
                false
            }
            WRAP_CONTENT -> {
                outConstraints[0] = 0
                outConstraints[1] = rootMaxConstraint
                true
            }
            MATCH_CONSTRAINT -> {
                if (DEBUG) {
                    Log.d("CCL", "Measure strategy $measureStrategy")
                    Log.d("CCL", "DW $matchConstraintDefaultDimension")
                    Log.d("CCL", "ODR $otherDimensionResolved")
                    Log.d("CCL", "IRH $currentDimensionResolved")
                }
                val useDimension =
                    currentDimensionResolved ||
                        (measureStrategy == TRY_GIVEN_DIMENSIONS ||
                            measureStrategy == USE_GIVEN_DIMENSIONS) &&
                            (measureStrategy == USE_GIVEN_DIMENSIONS ||
                                matchConstraintDefaultDimension != MATCH_CONSTRAINT_WRAP ||
                                otherDimensionResolved)
                if (DEBUG) {
                    Log.d("CCL", "UD $useDimension")
                }
                outConstraints[0] = if (useDimension) dimension else 0
                outConstraints[1] = if (useDimension) dimension else rootMaxConstraint
                !useDimension
            }
            MATCH_PARENT -> {
                outConstraints[0] = rootMaxConstraint
                outConstraints[1] = rootMaxConstraint
                false
            }
            else -> {
                error("$dimensionBehaviour is not supported")
            }
        }

    private fun Array<Int>.copyFrom(measure: BasicMeasure.Measure) {
        this[0] = measure.measuredWidth
        this[1] = measure.measuredHeight
        this[2] = measure.measuredBaseline
    }

    fun performMeasure(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        constraintSet: ConstraintSet,
        measurables: List<Measurable>,
        placeableMap: MutableMap<Measurable, Placeable>, // Initialized by caller, filled by us
        optimizationLevel: Int,
    ): IntSize {
        this.placeables = placeableMap
        if (measurables.isEmpty()) {
            // TODO(b/335524398): Behavior with zero children is unexpected. It's also inconsistent
            //      with ViewGroup, so this is a workaround to handle those cases the way it seems
            //      right for this implementation.
            return IntSize(constraints.minWidth, constraints.minHeight)
        }

        // Define the size of the ConstraintLayout.
        state.width(
            if (constraints.hasFixedWidth) {
                SolverDimension.createFixed(constraints.maxWidth)
            } else {
                SolverDimension.createWrap().min(constraints.minWidth)
            }
        )
        state.height(
            if (constraints.hasFixedHeight) {
                SolverDimension.createFixed(constraints.maxHeight)
            } else {
                SolverDimension.createWrap().min(constraints.minHeight)
            }
        )
        state.mParent.width.apply(state, root, ConstraintWidget.HORIZONTAL)
        state.mParent.height.apply(state, root, ConstraintWidget.VERTICAL)
        // Build constraint set and apply it to the state.
        state.rootIncomingConstraints = constraints
        state.isRtl = layoutDirection == LayoutDirection.Rtl
        resetMeasureState()
        if (constraintSet.isDirty(measurables)) {
            state.reset()
            constraintSet.applyTo(state, measurables)
            buildMapping(state, measurables)
            state.apply(root)
        } else {
            buildMapping(state, measurables)
        }

        applyRootSize(constraints)
        root.updateHierarchy()

        if (DEBUG) {
            root.debugName = "ConstraintLayout"
            root.children.fastForEach { child ->
                child.debugName =
                    (child.companionWidget as? Measurable)?.layoutId?.toString() ?: "NOTAG"
            }
            Log.d("CCL", "ConstraintLayout is asked to measure with $constraints")
            Log.d("CCL", root.toDebugString())
            root.children.fastForEach { child -> Log.d("CCL", child.toDebugString()) }
        }

        // No need to set sizes and size modes as we passed them to the state above.
        root.optimizationLevel = optimizationLevel
        root.measure(root.optimizationLevel, 0, 0, 0, 0, 0, 0, 0, 0)

        if (DEBUG) {
            Log.d("CCL", "ConstraintLayout is at the end ${root.width} ${root.height}")
        }
        return IntSize(root.width, root.height)
    }

    internal fun resetMeasureState() {
        placeables.clear()
        lastMeasures.clear()
        frameCache.clear()
    }

    protected fun applyRootSize(constraints: Constraints) {
        root.width = constraints.maxWidth
        root.height = constraints.maxHeight
        forcedScaleFactor = Float.NaN
        if (
            layoutInformationReceiver != null &&
                layoutInformationReceiver?.getForcedWidth() != Int.MIN_VALUE
        ) {
            val forcedWidth = layoutInformationReceiver!!.getForcedWidth()
            if (forcedWidth > root.width) {
                val scale = root.width / forcedWidth.toFloat()
                forcedScaleFactor = scale
            } else {
                forcedScaleFactor = 1f
            }
            root.width = forcedWidth
        }
        if (
            layoutInformationReceiver != null &&
                layoutInformationReceiver?.getForcedHeight() != Int.MIN_VALUE
        ) {
            val forcedHeight = layoutInformationReceiver!!.getForcedHeight()
            var scaleFactor = 1f
            if (forcedScaleFactor.isNaN()) {
                forcedScaleFactor = 1f
            }
            if (forcedHeight > root.height) {
                scaleFactor = root.height / forcedHeight.toFloat()
            }
            if (scaleFactor < forcedScaleFactor) {
                forcedScaleFactor = scaleFactor
            }
            root.height = forcedHeight
        }
    }

    fun Placeable.PlacementScope.performLayout(
        measurables: List<Measurable>,
        placeableMap: MutableMap<Measurable, Placeable>
    ) {
        placeables = placeableMap
        if (frameCache.isEmpty()) {
            root.children.fastForEach { child ->
                val measurable = child.companionWidget
                if (measurable !is Measurable) return@fastForEach
                val frame = WidgetFrame(child.frame.update())
                frameCache[measurable.anyOrNullId] = frame
            }
        }
        measurables.fastForEach { measurable ->
            val frame = frameCache[measurable.anyOrNullId] ?: return@fastForEach
            val placeable = placeables[measurable] ?: return@fastForEach
            placeWithFrameTransform(placeable, frame)
        }
        if (layoutInformationReceiver?.getLayoutInformationMode() == LayoutInfoFlags.BOUNDS) {
            computeLayoutResult()
        }
    }

    override fun didMeasures() {}

    /**
     * Measure a [ConstraintWidget] with the given [constraints].
     *
     * Note that the [constraintWidget] could correspond to either a Composable or a Helper, which
     * need to be measured differently.
     *
     * Returns a [Pair] with the result of the measurement, the first and second values are the
     * measured width and height respectively.
     */
    private fun measureWidget(
        constraintWidget: ConstraintWidget,
        constraints: Constraints
    ): IntIntPair {
        val measurable = constraintWidget.companionWidget
        val widgetId = constraintWidget.stringId
        return when {
            constraintWidget is VirtualLayout -> {
                // TODO: This step should really be performed within ConstraintWidgetContainer,
                //  compose-ConstraintLayout should only have to measure Composables/Measurables
                val widthMode =
                    when {
                        constraints.hasFixedWidth -> BasicMeasure.EXACTLY
                        constraints.hasBoundedWidth -> BasicMeasure.AT_MOST
                        else -> BasicMeasure.UNSPECIFIED
                    }
                val heightMode =
                    when {
                        constraints.hasFixedHeight -> BasicMeasure.EXACTLY
                        constraints.hasBoundedHeight -> BasicMeasure.AT_MOST
                        else -> BasicMeasure.UNSPECIFIED
                    }
                constraintWidget.measure(
                    widthMode,
                    constraints.maxWidth,
                    heightMode,
                    constraints.maxHeight
                )
                IntIntPair(constraintWidget.measuredWidth, constraintWidget.measuredHeight)
            }
            measurable is Measurable -> {
                val result = measurable.measure(constraints).also { placeables[measurable] = it }
                IntIntPair(result.width, result.height)
            }
            else -> {
                Log.w("CCL", "Nothing to measure for widget: $widgetId")
                IntIntPair(0, 0)
            }
        }
    }
}
