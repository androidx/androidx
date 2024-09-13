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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope

/**
 * A group of [Picker]s to build components where multiple pickers are required to be combined
 * together. At most one [Picker] can be selected at a time. When touch exploration services are
 * enabled, the focus moves to the picker which is clicked. To handle clicks in a different manner,
 * use the [onPickerSelected] lambda to control the focus of talkback and actual focus.
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
 * @param selectedPickerIndex The index of the [Picker] that is selected. The value is ignored when
 *   negative, which means that no [Picker] is selected.
 * @param onPickerSelected Action triggered when one of the [Picker]s is selected inside the group.
 *   Typically, [selectedPickerIndex] and [onPickerSelected] are used in tandem to apply a visual
 *   highlight to the currently selected [Picker] as a guide to the user.
 * @param modifier [Modifier] to be applied to the [PickerGroup].
 * @param autoCenter Indicates whether the selected [Picker] should be centered on the screen. It is
 *   recommended to set this as true when all the pickers cannot be fit into the screen. Or provide
 *   a mechanism to navigate to pickers which are not visible on screen. If false, the whole row
 *   containing pickers would be centered.
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param separator A composable block which describes the separator between different [Picker]s.
 *   The integer parameter to the composable depicts the index where it will be kept. For example, 0
 *   would represent the separator between the first and second picker.
 * @param content The content of the [PickerGroup] as a container of [Picker]s.
 */
@Composable
@Suppress("ComposableLambdaParameterPosition")
fun PickerGroup(
    selectedPickerIndex: Int,
    onPickerSelected: (selectedIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    autoCenter: Boolean = true,
    separator: (@Composable (Int) -> Unit)? = null,
    propagateMinConstraints: Boolean = false,
    content: PickerGroupScope.() -> Unit
) {
    val actualContent = PickerGroupScope().apply(block = content)
    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    AutoCenteringRow(
        modifier =
            modifier.then(
                // When touch exploration services are enabled, send the scroll events on the parent
                // composable to selected picker
                if (
                    touchExplorationServicesEnabled &&
                        selectedPickerIndex >= 0 &&
                        selectedPickerIndex in actualContent.items.indices
                ) {
                    Modifier.scrollable(
                        state = actualContent.items[selectedPickerIndex].pickerState,
                        orientation = Orientation.Vertical,
                        reverseDirection = true
                    )
                } else {
                    Modifier
                }
            ),
        propagateMinConstraints = propagateMinConstraints
    ) {
        // When no Picker is selected, provide an empty composable as a placeholder
        // and tell the HierarchicalFocusCoordinator to clear the focus.
        HierarchicalFocusCoordinator(
            requiresFocus = { !actualContent.items.indices.contains(selectedPickerIndex) }
        ) {}
        actualContent.items.forEachIndexed { index, pickerData ->
            val pickerSelected = index == selectedPickerIndex
            HierarchicalFocusCoordinator(requiresFocus = { pickerSelected }) {
                val focusRequester = pickerData.focusRequester ?: rememberActiveFocusRequester()
                Picker(
                    state = pickerData.pickerState,
                    contentDescription = pickerData.contentDescription,
                    readOnly = !pickerSelected,
                    modifier =
                        pickerData.modifier
                            .then(
                                // If auto center is enabled, apply auto centering modifier on
                                // selected picker to center it.
                                if (pickerSelected && autoCenter) Modifier.autoCenteringTarget()
                                else Modifier
                            )
                            // Do not need focusable as it's already set in ScalingLazyColumn
                            .focusRequester(focusRequester),
                    readOnlyLabel = pickerData.readOnlyLabel,
                    onSelected = pickerData.onSelected,
                    spacing = pickerData.spacing,
                    userScrollEnabled = !touchExplorationServicesEnabled || pickerSelected,
                    option = { optionIndex ->
                        with(pickerData) {
                            Box(
                                if (touchExplorationServicesEnabled || pickerSelected) {
                                    Modifier
                                } else
                                    Modifier.pointerInput(Unit) {
                                        coroutineScope {
                                            // Keep looking for touch events on the picker if it is
                                            // not selected
                                            while (true) {
                                                awaitEachGesture {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    onPickerSelected(index)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                option(optionIndex, pickerSelected)
                            }
                        }
                    }
                )
            }
            if (index < actualContent.items.size - 1) {
                separator?.invoke(index)
            }
        }
    }
}

class PickerGroupScope {
    internal val items = mutableListOf<PickerGroupItem>()

    /**
     * Adds a [Picker] item to the [PickerGroup]
     *
     * @param pickerState The state of the picker.
     * @param modifier [Modifier] to be applied to the [Picker].
     * @param contentDescription Text used by accessibility services to describe what the selected
     *   option represents. This text should be localized, such as by using
     *   [androidx.compose.ui.res.stringResource] or similar. Typically, the content description is
     *   inferred via derivedStateOf to avoid unnecessary recompositions, like this: val description
     *   by remember { derivedStateOf { /* expression using state.selectedOption */ } }.
     * @param focusRequester Optional [FocusRequester] for the [Picker]. If not provided, a local
     *   instance of [FocusRequester] will be created to handle the focus between different pickers.
     * @param onSelected Action triggered when the [Picker] is selected by clicking.
     * @param spacing The amount of spacing in [Dp] between items. Can be negative, which can be
     *   useful for Text if it has plenty of whitespace.
     * @param readOnlyLabel A slot for providing a label, displayed above the selected option when
     *   the [Picker] is read-only. The label is overlaid with the currently selected option within
     *   a Box, so it is recommended that the label is given [Alignment.TopCenter].
     * @param option A block which describes the content. The integer parameter to the composable
     *   denotes the index of the option and boolean denotes whether the picker is selected or not.
     */
    fun pickerGroupItem(
        pickerState: PickerState,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
        focusRequester: FocusRequester? = null,
        onSelected: () -> Unit = {},
        readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
        spacing: Dp = 0.dp,
        option: @Composable PickerScope.(optionIndex: Int, pickerSelected: Boolean) -> Unit
    ) =
        items.add(
            PickerGroupItem(
                pickerState,
                modifier,
                contentDescription,
                focusRequester,
                onSelected,
                readOnlyLabel,
                spacing,
                option
            )
        )
}

internal data class PickerGroupItem(
    val pickerState: PickerState,
    val modifier: Modifier = Modifier,
    val contentDescription: String? = null,
    val focusRequester: FocusRequester? = null,
    val onSelected: () -> Unit = {},
    val readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
    val spacing: Dp = 0.dp,
    val option: @Composable PickerScope.(optionIndex: Int, pickerSelected: Boolean) -> Unit
)

/*
 * A row that horizontally aligns the center of the first child that has
 * Modifier.autoCenteringTarget() with the center of this row.
 * If no child has that modifier, the whole row is horizontally centered.
 * Vertically, each child is centered.
 */
@Composable
private fun AutoCenteringRow(
    modifier: Modifier = Modifier,
    propagateMinConstraints: Boolean,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, parentConstraints ->
        // Reset the min width and height of the constraints used to measure child composables
        // if min constraints are not supposed to propagated.
        val constraints =
            if (propagateMinConstraints) {
                parentConstraints
            } else {
                parentConstraints.copy(minWidth = 0, minHeight = 0)
            }
        val placeables = measurables.fastMap { it.measure(constraints) }
        val centeringOffset = computeCenteringOffset(placeables)
        val rowWidth =
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val rowHeight = calculateHeight(constraints, placeables)
        layout(width = rowWidth, height = rowHeight) {
            var x = rowWidth / 2f - centeringOffset
            placeables.fastForEach {
                it.placeRelative(x.roundToInt(), ((rowHeight - it.height) / 2f).roundToInt())
                x += it.width
            }
        }
    }
}

/**
 * Calculates the center for the list of [Placeable]. Returns the offset which can be applied on
 * parent composable to center the contents. If [autoCenteringTarget] is applied to any [Placeable],
 * the offset returned will allow to center that particular composable.
 */
private fun computeCenteringOffset(placeables: List<Placeable>): Int {
    var sumWidth = 0
    placeables.fastForEach { p ->
        if (p.isAutoCenteringTarget()) {
            // The target centering offset is at the middle of this child.
            return sumWidth + p.width / 2
        }
        sumWidth += p.width
    }

    // No target, center the whole row.
    return sumWidth / 2
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
