/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.PickerGroup
import androidx.wear.compose.material.PickerGroupItem
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerGroupState
import androidx.wear.compose.material.rememberPickerState

@Sampled
@Composable
fun PickerGroup24Hours() {
    val pickerGroupState = rememberPickerGroupState()
    val pickerStateHour = rememberPickerState(initialNumberOfOptions = 24)
    val pickerStateMinute = rememberPickerState(initialNumberOfOptions = 60)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(30.dp))
        Text(text = if (pickerGroupState.selectedIndex == 0) "Hours" else "Minutes")
        Spacer(modifier = Modifier.size(10.dp))
        PickerGroup(
            PickerGroupItem(
                pickerState = pickerStateHour,
                option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            PickerGroupItem(
                pickerState = pickerStateMinute,
                option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            pickerGroupState = pickerGroupState,
            autoCenter = false
        )
    }
}

@Sampled
@Composable
fun AutoCenteringPickerGroup() {
    val pickerGroupState = rememberPickerGroupState()
    val pickerStateHour = rememberPickerState(initialNumberOfOptions = 24)
    val pickerStateMinute = rememberPickerState(initialNumberOfOptions = 60)
    val pickerStateSeconds = rememberPickerState(initialNumberOfOptions = 60)
    val pickerStateMilliSeconds = rememberPickerState(initialNumberOfOptions = 1000)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val headingText = mapOf(
            0 to "Hours", 1 to "Minutes", 2 to "Seconds", 3 to "Milli"
        )
        Spacer(modifier = Modifier.size(30.dp))
        Text(text = headingText[pickerGroupState.selectedIndex]!!)
        Spacer(modifier = Modifier.size(10.dp))
        PickerGroup(
            PickerGroupItem(
                pickerState = pickerStateHour,
                option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            PickerGroupItem(
                pickerState = pickerStateMinute,
                option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            PickerGroupItem(
                pickerState = pickerStateSeconds,
                option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            PickerGroupItem(
                pickerState = pickerStateMilliSeconds,
                option = { optionIndex, _ -> Text(text = "%03d".format(optionIndex)) },
                modifier = Modifier.size(80.dp, 100.dp)
            ),
            pickerGroupState = pickerGroupState,
            autoCenter = true
        )
    }
}
