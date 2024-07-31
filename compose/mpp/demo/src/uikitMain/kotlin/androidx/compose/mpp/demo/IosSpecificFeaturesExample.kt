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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

val HapticFeedbackExample = Screen.Example("Haptic feedback") {
    val feedback = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {
            feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }) {
            Text("TextHandleMove")
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }) {
            Text("LongPress")
        }
    }
}

val IosSpecificFeatures = Screen.Selection(
    "iOS-specific features",
    NativeModalWithNaviationExample,
    HapticFeedbackExample,
    LazyColumnWithInteropViewsExample,
    AccessibilityLiveRegionExample,
    InteropViewAndSemanticsConfigMerge,
    StatusBarStateExample,
    UIKitInteropExample
)