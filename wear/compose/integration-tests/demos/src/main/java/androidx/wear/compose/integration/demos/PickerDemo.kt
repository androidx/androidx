/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState

@Composable
fun PickerTimeDemo() {
    // The option initially selected on the Picker can be passed to rememberPickerState()
    val hourState = rememberPickerState(numberOfOptions = 24, initiallySelectedOption = 6)
    val minuteState = rememberPickerState(numberOfOptions = 60)
    LaunchedEffect(true) {
        // This is possible, but not desirable, since the observed state.selectedOption may take a
        // few frames to update to this value.
        minuteState.scrollToOption(15)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Picker(
                state = hourState,
                modifier = Modifier
                    .size(50.dp, 80.dp)
                    .background(color = Color.Black),
                separation = (-10).dp
            ) {
                Text(
                    modifier = Modifier.wrapContentSize(),
                    text = it.toString(),
                    style = MaterialTheme.typography.display3
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Picker(
                state = minuteState,
                modifier = Modifier
                    .size(50.dp, 80.dp)
                    .background(color = Color.Black),
                separation = (-10).dp
            ) {
                Text(
                    modifier = Modifier.wrapContentSize(),
                    text = "%02d".format(it),
                    style = MaterialTheme.typography.display3
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            Text(
                "${hourState.selectedOption} : " +
                    "%02d".format(minuteState.selectedOption),
                style = MaterialTheme.typography.title3.copy(color = Color.White)
            )
        }
    }
}