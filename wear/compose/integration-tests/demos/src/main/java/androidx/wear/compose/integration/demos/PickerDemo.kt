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

package androidx.wear.compose.integration.demos

import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerDefaults
import androidx.wear.compose.material.PickerScope
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.ScalingParams
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimePickerWithHoursMinutesSeconds() {
    var selectedColumn by remember { mutableStateOf(0) }
    val textStyle = MaterialTheme.typography.display2
    val optionColor = MaterialTheme.colors.secondary
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (selectedColumn) {
                    0 -> "Hour"
                    1 -> "Minute"
                    else -> "Second"
                },
                color = optionColor,
                style = MaterialTheme.typography.button,
                maxLines = 1,
            )
            val weightsToCenterVertically = 0.5f
            Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val selectablePickerModifier = Modifier.size(54.dp, 120.dp)
                val separation = (-10).dp
                Spacer(Modifier.width(8.dp))
                SelectablePicker(
                    selected = selectedColumn == 0,
                    state = rememberPickerState(numberOfOptions = 24, initiallySelectedOption = 6),
                    modifier = selectablePickerModifier,
                    separation = separation,
                ) { hour: Int, selected: Boolean ->
                    TimePiece(
                        selected = selected,
                        onSelected = { selectedColumn = 0 },
                        text = "%02d".format(hour),
                        style = textStyle,
                    )
                }
                Separator(4.dp)
                SelectablePicker(
                    selected = selectedColumn == 1,
                    state = rememberPickerState(numberOfOptions = 60),
                    modifier = selectablePickerModifier,
                    separation = separation,
                ) { minute: Int, selected: Boolean ->
                    TimePiece(
                        selected = selected,
                        onSelected = { selectedColumn = 1 },
                        text = "%02d".format(minute),
                        style = textStyle,
                    )
                }
                Separator(4.dp)
                SelectablePicker(
                    selected = selectedColumn == 2,
                    state = rememberPickerState(numberOfOptions = 60),
                    modifier = selectablePickerModifier,
                    separation = separation,
                ) { second: Int, selected: Boolean ->
                    TimePiece(
                        selected = selected,
                        onSelected = { selectedColumn = 2 },
                        text = "%02d".format(second),
                        style = textStyle,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
            Button(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_24px),
                    contentDescription = "check",
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimePickerWith12HourClock() {
    var morning by remember { mutableStateOf(true) }
    var selectedColumn by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        CompactChip(
            onClick = { morning = !morning },
            modifier = Modifier.size(width = 50.dp, height = 24.dp),
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = if (morning) "AM" else "PM",
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.button,
                    )
                }
            },
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.secondary),
            contentPadding = PaddingValues(vertical = 0.dp),
        )
        Spacer(Modifier.fillMaxWidth().weight(0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.width(8.dp))
            SelectablePicker(
                selected = selectedColumn == 0,
                state = rememberPickerState(numberOfOptions = 12, initiallySelectedOption = 6),
                modifier = Modifier.size(64.dp, 120.dp),
                separation = (-10).dp,
                label = { LabelText("Hour") }
            ) { hour: Int, selected: Boolean ->
                TimePiece(
                    selected = selected,
                    onSelected = { selectedColumn = 0 },
                    text = "%2d".format(hour + 1),
                )
            }
            Separator(8.dp)
            SelectablePicker(
                selected = selectedColumn == 1,
                state = rememberPickerState(numberOfOptions = 60),
                modifier = Modifier.size(64.dp, 120.dp),
                separation = (-10).dp,
                label = { LabelText("Minute") }
            ) { minute: Int, selected: Boolean ->
                TimePiece(
                    selected = selected,
                    onSelected = { selectedColumn = 1 },
                    text = "%02d".format(minute),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.fillMaxWidth().weight(0.5f))
        Button(onClick = {}) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24px),
                contentDescription = "check",
                modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * [SelectablePicker] builds on the functionality of [Picker], displaying a [Picker] when selected
 * and providing a slot for content (typically [Text]) otherwise.
 *
 * @param selected Determines whether the [SelectablePicker] is selected
 * (in which case it shows a Picker).
 * @param state The state of the component.
 * @param modifier Modifier to be applied to the Picker. Typically provides size for the underlying
 * [Picker].
 * @param label A slot for providing a label, displayed above the [SelectablePicker]
 * when unselected.
 * @param scalingParams the parameters to configure the scaling and transparency effects for the
 * component. See [ScalingParams]
 * @param separation the amount of separation in [Dp] between items. Can be negative, which can be
 * useful for Text if it has plenty of whitespace.
 * @param option a block which describes the content. Inside this block you can reference
 * [PickerScope.selectedOption] and other properties in [PickerScope]. The Int parameter determines
 * the option index and the Boolean parameter determines whether the Pickable is selected (typically
 * this is used to change the appearance of the content, e.g. by setting a highlight color).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SelectablePicker(
    selected: Boolean,
    state: PickerState,
    modifier: Modifier = Modifier,
    label: @Composable (BoxScope.() -> Unit)? = null,
    scalingParams: ScalingParams = PickerDefaults.scalingParams(),
    separation: Dp = 0.dp,
    option: @Composable BoxScope.(Int, Boolean) -> Unit
) {
    Box(modifier = modifier) {
        if (selected) {
            Picker(
                state = state,
                modifier = Modifier.fillMaxSize(),
                separation = separation,
                scalingParams = scalingParams,
            ) {
                option(it, true)
            }
        } else {
            if (label != null) {
                label()
            }
            option(
                state.selectedOption,
                false
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoxScope.TimePiece(
    selected: Boolean,
    onSelected: () -> Unit,
    text: String,
    style: TextStyle = MaterialTheme.typography.display1,
) {
    val modifier = Modifier.align(Alignment.Center).wrapContentSize()
    Text(
        text = text,
        style = style,
        color =
            if (selected) MaterialTheme.colors.secondary
            else MaterialTheme.colors.onBackground,
        modifier =
            if (selected) modifier
            else modifier.pointerInteropFilter {
                if (it.action == MotionEvent.ACTION_DOWN) onSelected()
                true
            },
    )
}

@Composable
private fun BoxScope.LabelText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.button,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier.align(Alignment.TopCenter).offset(y = 12.dp)
    )
}

@Composable
private fun Separator(width: Dp) {
    Spacer(Modifier.width(width))
    Text(
        text = ":",
        style = MaterialTheme.typography.display2,
        color = MaterialTheme.colors.onBackground
    )
    Spacer(Modifier.width(width))
}
