/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope

/**
 * A group of [Picker]s to build components where multiple pickers are required to be combined
 * together. At most one [Picker] can be selected at a time. When touch exploration services are
 * enabled, the focus moves to the picker which is clicked.
 *
 * It is recommended to ensure that a [Picker] in non read only mode should have user scroll enabled
 * when touch exploration services are running.
 *
 * Example of a sample picker group with an hour and minute picker (24 hour format):
 *
 * @sample androidx.wear.compose.material3.samples.PickerGroupSample
 *
 * Example of an auto centering picker group where the total width exceeds screen's width:
 *
 * @sample androidx.wear.compose.material3.samples.AutoCenteringPickerGroup
 * @param selectedPickerState The [PickerState] of the [Picker] that is selected. Null value means
 *   that no [Picker] is selected.
 * @param modifier [Modifier] to be applied to the [PickerGroup].
 * @param autoCenter Indicates whether the selected [Picker] should be centered on the screen. It is
 *   recommended to set this as true when all the pickers cannot be fit into the screen. Or provide
 *   a mechanism to navigate to pickers which are not visible on screen. If false, the whole row
 *   containing pickers would be centered.
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param content The content of the [PickerGroup] as a container of [Picker]s.
 */
@Composable
@Suppress("ComposableLambdaParameterPosition")
public fun PickerGroup(
    modifier: Modifier = Modifier,
    selectedPickerState: PickerState? = null,
    autoCenter: Boolean = true,
    propagateMinConstraints: Boolean = false,
    content: @Composable PickerGroupScope.() -> Unit,
) {
    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    val scope = remember { PickerGroupScope() }

    AutoCenteringRow(
        modifier =
            modifier.then(
                // When touch exploration services are enabled, send the scroll events on the parent
                // composable to selected picker
                if (touchExplorationServicesEnabled && selectedPickerState != null) {
                    Modifier.scrollable(
                        state = selectedPickerState,
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                    )
                } else {
                    Modifier
                }
            ),
        propagateMinConstraints = propagateMinConstraints,
        autoCenter = autoCenter,
    ) {
        with(scope) {
            autoCenteringEnabled = autoCenter
            content()
        }
    }
}

public class PickerGroupScope {

    /**
     * A [Picker] in a [PickerGroup]
     *
     * @param pickerState The state of the picker.
     * @param selected If the [Picker] is selected.
     * @param onSelected Action triggered when the [Picker] is selected by clicking.
     * @param modifier [Modifier] to be applied to the [Picker].
     * @param contentDescription A block which computes text used by accessibility services to
     *   describe what the selected option represents. This text should be localized, such as by
     *   using [androidx.compose.ui.res.stringResource] or similar.
     * @param focusRequester Optional [FocusRequester] for the [Picker]. If not provided, a local
     *   instance of [FocusRequester] will be created to handle the focus between different pickers.
     *   If it is provided, the caller is responsible for handling the focus.
     * @param verticalSpacing The amount of vertical spacing in [Dp] between items. Can be negative,
     *   which can be useful for Text if it has plenty of whitespace.
     * @param readOnlyLabel A slot for providing a label, displayed above the selected option when
     *   the [Picker] is read-only. The label is overlaid with the currently selected option within
     *   a Box, so it is recommended that the label is given [Alignment.TopCenter].
     * @param option A block which describes the content. The integer parameter to the composable
     *   denotes the index of the option and boolean denotes whether the picker is selected or not.
     */
    @Composable
    public fun PickerGroupItem(
        pickerState: PickerState,
        selected: Boolean,
        onSelected: () -> Unit,
        modifier: Modifier = Modifier,
        contentDescription: (() -> String)? = null,
        focusRequester: FocusRequester? = null,
        readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
        verticalSpacing: Dp = 0.dp,
        option: @Composable PickerScope.(optionIndex: Int, pickerSelected: Boolean) -> Unit,
    ) {
        val touchExplorationServicesEnabled by
            LocalTouchExplorationStateProvider.current.touchExplorationState()

        val latestOnSelected by rememberUpdatedState(onSelected)
        Picker(
            state = pickerState,
            contentDescription = contentDescription,
            readOnly = !selected,
            modifier =
                modifier
                    .then(
                        // If auto center is enabled, apply auto centering modifier on
                        // selected picker to center it.
                        if (selected && autoCenteringEnabled) Modifier.autoCenteringTarget()
                        else Modifier
                    )
                    .then(
                        Modifier.pointerInput(touchExplorationServicesEnabled, selected) {
                            // better to restart this PointerInputScope when the keys change
                            // than trigger the entire modifier chain
                            if (touchExplorationServicesEnabled || selected) {
                                return@pointerInput
                            }
                            coroutineScope {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    latestOnSelected()
                                }
                            }
                        }
                    )
                    .hierarchicalFocusGroup(active = selected)
                    .then(
                        // If the user provided a focus requester, we add it here, otherwise,
                        // we take care of focus using the HFC.
                        focusRequester?.let { Modifier.focusRequester(it) }
                            ?: Modifier.requestFocusOnHierarchyActive()
                    ),
            // Do not need focusable as it's already set in ScalingLazyColumn
            readOnlyLabel = readOnlyLabel,
            onSelected = latestOnSelected,
            verticalSpacing = verticalSpacing,
            userScrollEnabled = !touchExplorationServicesEnabled || selected,
            option = { optionIndex -> option(optionIndex, selected) },
        )
    }

    internal var autoCenteringEnabled by mutableStateOf(false)
}

/*
 * A row that horizontally aligns the center of the first child that has
 * Modifier.autoCenteringTarget() with the center of this row.
 * The change of centered child is animated.
 * If no child has that modifier, the whole row is horizontally centered.
 * Vertically, each child is centered.
 */
@Composable
private fun AutoCenteringRow(
    modifier: Modifier = Modifier,
    propagateMinConstraints: Boolean,
    autoCenter: Boolean,
    content: @Composable () -> Unit,
) {
    // Use a sentinel value to detect the initial state, allowing us to differentiate
    // between the very first composition and subsequent states where no item is selected.
    var targetCenteringOffset by remember { mutableFloatStateOf(CenteringOffsetNotInitialized) }

    // If the sentinel value is still set, we are in the initial state. The offset for
    // the animation should be 0f until the first layout pass calculates the actual default offset.
    val offsetForAnimation =
        if (targetCenteringOffset == CenteringOffsetNotInitialized) 0f else targetCenteringOffset

    val animatedCenteringOffset by
        animateFloatAsState(
            targetValue = offsetForAnimation,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        )

    Layout(modifier = modifier, content = content) { measurables, parentConstraints ->
        // Reset the min width and height of the constraints used to measure child composables
        // if min constraints are not supposed to propagated.
        val constraints =
            if (propagateMinConstraints) {
                parentConstraints
            } else {
                parentConstraints.copyMaxDimensions()
            }

        val placeables = measurables.fastMap { it.measure(constraints) }
        // Try to find an explicitly selected picker to center.
        val newTargetOffset = findTargetCenteringOffset(placeables)

        if (newTargetOffset != null) {
            // A specific picker is selected, so we update the target offset.
            targetCenteringOffset = newTargetOffset.toFloat()
        } else {
            // No specific picker is selected.
            // If this is the first composition (sentinel value is present),
            // calculate and set the default offset (which centers the first item).
            // Otherwise, we do nothing, preserving the last known centered position.
            if (targetCenteringOffset == CenteringOffsetNotInitialized) {
                targetCenteringOffset =
                    computeDefaultCenteringOffset(placeables, autoCenter).toFloat()
            }
        }

        val rowWidth =
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val rowHeight = calculateHeight(constraints, placeables)

        layout(width = rowWidth, height = rowHeight) {
            var x = rowWidth / 2f - animatedCenteringOffset
            placeables.fastForEach {
                it.placeRelative(x.roundToInt(), ((rowHeight - it.height) / 2f).roundToInt())
                x += it.width
            }
        }
    }
}

/**
 * Calculates the offset required to center a specific target Placeable, if one is found. A target
 * is identified by the `Modifier.autoCenteringTarget()`.
 *
 * @return The offset in pixels to center the target, or null if no target is found.
 */
private fun findTargetCenteringOffset(placeables: List<Placeable>): Int? {
    var currentWidth = 0
    placeables.fastForEach { p ->
        if (p.isAutoCenteringTarget()) {
            // The target centering offset is at the middle of this child.
            return currentWidth + p.width / 2
        }
        currentWidth += p.width
    }
    return null // No specific target found.
}

/**
 * Calculates the default centering offset for the group when no specific item is targeted. If
 * auto-centering is enabled, it centers the first item. Otherwise, it centers the entire group.
 */
private fun computeDefaultCenteringOffset(placeables: List<Placeable>, autoCenter: Boolean): Int {
    return if (autoCenter && placeables.isNotEmpty()) {
        // Default to centering the first item.
        placeables.first().width / 2
    } else {
        // Fallback to centering the whole group.
        placeables.sumOf { it.width } / 2
    }
}

/**
 * Calculates the height of the [AutoCenteringRow] from the given [Placeable]s and [Constraints]. It
 * is calculated based on the max height of all the [Placeable]s and the height passed from the
 * [Constraints].
 */
private fun calculateHeight(constraints: Constraints, placeables: List<Placeable>): Int {
    val maxChildrenHeight = placeables.fastMaxOfOrNull { it.height }!!
    return maxChildrenHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
}

internal fun Modifier.autoCenteringTarget() =
    this.then(
        object : ParentDataModifier {
            override fun Density.modifyParentData(parentData: Any?) = AutoCenteringRowParentData()
        }
    )

internal class AutoCenteringRowParentData

internal fun Placeable.isAutoCenteringTarget() = (parentData as? AutoCenteringRowParentData) != null

private const val CenteringOffsetNotInitialized = Float.MIN_VALUE
