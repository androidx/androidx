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

package androidx.wear.compose.material3.demos

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text

@Composable
fun HapticsDemos() {
    val haptics = HapticFeedbackProvider(LocalView.current)

    // Show a Button to trigger each haptic constant when clicked:
    // https://developer.android.com/reference/android/view/HapticFeedbackConstants
    val hapticConstants =
        listOf(
            Pair(HapticFeedbackConstants.CLOCK_TICK, "Clock Tick"),
            Pair(HapticFeedbackConstants.CONFIRM, "Confirm"),
            Pair(HapticFeedbackConstants.CONTEXT_CLICK, "Context Click"),
            Pair(HapticFeedbackConstants.DRAG_START, "Drag Start"),
            // NB HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING has been deprecated, so omit it
            Pair(HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING, "Flag Ignore"),
            Pair(HapticFeedbackConstants.GESTURE_END, "Gesture End"),
            Pair(HapticFeedbackConstants.GESTURE_START, "Gesture Start"),
            Pair(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, "Gesture Threshold Activate"),
            Pair(
                HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE,
                "Gesture Threshold Deactivate"
            ),
            Pair(HapticFeedbackConstants.KEYBOARD_PRESS, "Keyboard Press"),
            Pair(HapticFeedbackConstants.KEYBOARD_RELEASE, "Keyboard Release"),
            Pair(HapticFeedbackConstants.KEYBOARD_TAP, "Keyboard Tap"),
            Pair(HapticFeedbackConstants.LONG_PRESS, "Long Press"),
            Pair(HapticFeedbackConstants.NO_HAPTICS, "No haptics"),
            Pair(HapticFeedbackConstants.REJECT, "Reject"),
            Pair(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, "Segment Frequent Tick"),
            Pair(HapticFeedbackConstants.SEGMENT_TICK, "Segment Tick"),
            Pair(HapticFeedbackConstants.TEXT_HANDLE_MOVE, "Text Handle Move"),
            Pair(HapticFeedbackConstants.TOGGLE_OFF, "Toggle Off"),
            Pair(HapticFeedbackConstants.TOGGLE_ON, "Toggle On"),
            Pair(HapticFeedbackConstants.VIRTUAL_KEY, "Virtual Key"),
            Pair(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE, "Virtual Key Release"),
        )

    ScalingLazyDemo {
        item { ListHeader { Text("Haptic Constants") } }
        items(hapticConstants.size) { index ->
            val (constant, name) = hapticConstants[index]
            HapticsDemo(haptics, constant, name)
        }
    }
}

@Composable
private fun HapticsDemo(
    hapticFeedbackProvider: HapticFeedbackProvider,
    feedbackConstant: Int,
    demoName: String
) {
    Button(
        onClick = { hapticFeedbackProvider.performHapticFeedback(feedbackConstant) },
        label = {
            Text(demoName, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private class HapticFeedbackProvider(private val view: View) {
    fun performHapticFeedback(feedbackConstant: Int) {
        view.performHapticFeedback(feedbackConstant)
    }
}
