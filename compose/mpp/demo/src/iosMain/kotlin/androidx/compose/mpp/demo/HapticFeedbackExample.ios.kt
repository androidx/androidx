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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private val haptics = listOf(
    "Confirm" to HapticFeedbackType.Confirm,
    "Reject" to HapticFeedbackType.Reject,

    "LongPress" to HapticFeedbackType.LongPress,
    "ContextClick" to HapticFeedbackType.ContextClick,

    "TextHandleMove" to HapticFeedbackType.TextHandleMove,
    "SegmentTick" to HapticFeedbackType.SegmentTick,
    "SegmentFrequentTick" to HapticFeedbackType.SegmentFrequentTick,

    "GestureEnd" to HapticFeedbackType.GestureEnd,
    "GestureThresholdActivate" to HapticFeedbackType.GestureThresholdActivate,
    "ToggleOff" to HapticFeedbackType.ToggleOff,
    "ToggleOn" to HapticFeedbackType.ToggleOn,
    "VirtualKey" to HapticFeedbackType.VirtualKey,
)

val HapticFeedbackExample = Screen.Example("Haptic feedback") {

    val feedback = LocalHapticFeedback.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp)
    ) {
        items(haptics) {
            Button(onClick = {
                feedback.performHapticFeedback(it.second)
            }) {
                Text(it.first)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
