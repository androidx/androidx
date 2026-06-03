/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.SoundEffectOnInteraction
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun InteractionSoundDemos() {
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Debug Instructions:", modifier = Modifier.padding(bottom = 8.dp))
        Text("1. Pointer Click: Tap a button normally.")
        Text("2. Key Click: Focus a button (DPAD/Tab) and press DPAD Center or Enter.")
        Text("3. A11y Click: Use TalkBack to perform a double-tap action.")
        Text("4. Focus Navigation: Use DPAD or Tab to navigate between buttons.")
        Text("5. Double Click Delay: Tap 'Combined Click' once and wait.")

        Text("\n--- Interactive Demo ---")

        Text("\nStandard Clickable (Has Sound):")
        Button(onClick = {}) { Text("Pointer/Key Click") }

        Text("\nDisabled Sound Feedback (Interaction subtree):")
        SoundEffectOnInteraction(enabled = false) {
            Button(onClick = {}) { Text("Silent Button") }

            Text("\nAccessibility Click (No Sound):")
            Box(
                Modifier.size(100.dp, 50.dp)
                    .clickable(onClickLabel = "Click me silently") {}
                    .semantics { onClick(label = "Accessibility Click Me (talkback)") { true } }
            ) {
                Text("A11y Button")
            }
        }

        Text("\nFocus Navigation (Has Navigation Sound):")
        Column {
            Button(onClick = {}) { Text("Button 1") }
            Button(onClick = {}) { Text("Button 2") }
        }

        Text("\nCombined Clickable (Delayed Sound?):")
        Text(
            "Instruction: Tap once and wait. Observe if the sound plays immediately or is delayed " +
                "until the double-click timeout expires."
        )
        Text(
            "Combined Click",
            modifier =
                Modifier.padding(top = 8.dp)
                    .background(androidx.compose.ui.graphics.Color.LightGray)
                    .combinedClickable(onClick = {}, onDoubleClick = {})
                    .padding(16.dp),
        )
    }
}
