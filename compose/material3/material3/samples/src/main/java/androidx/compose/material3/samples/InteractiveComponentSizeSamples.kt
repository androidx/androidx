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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun MinimumInteractiveComponentSizeSample() {
    @Composable
    fun Widget(color: Color, modifier: Modifier = Modifier) {
        // Default size is 24.dp, which is smaller than the recommended touch target
        Box(modifier.size(24.dp).background(color))
    }

    Column(Modifier.border(1.dp, Color.Black)) {
        // Not interactable, no need for touch target enforcement
        Widget(Color.Red)

        Widget(
            color = Color.Green,
            modifier =
                Modifier.clickable { /* do something */ }
                    // Component is now interactable, so it should enforce a sufficient touch target
                    .minimumInteractiveComponentSize()
        )

        Widget(
            color = Color.Blue,
            modifier =
                Modifier.clickable { /* do something */ }
                    // Component is now interactable, so it should enforce a sufficient touch target
                    .minimumInteractiveComponentSize()
                    // Any size modifiers should come after `minimumInteractiveComponentSize`
                    // so as not to interfere with layout expansion
                    .size(36.dp)
        )
    }
}

@Preview
@Sampled
@Composable
fun MinimumInteractiveComponentSizeCheckboxRowSample() {
    var checked by remember { mutableStateOf(false) }

    // The entire row accepts interactions to toggle the checkbox,
    // so we apply `minimumInteractiveComponentSize`
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.toggleable(
                    value = checked,
                    onValueChange = { checked = it },
                    role = Role.Checkbox,
                )
                .minimumInteractiveComponentSize()
    ) {
        // Cannot rely on Checkbox for touch target expansion because it only enforces
        // `minimumInteractiveComponentSize` if onCheckedChange is non-null
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text("Label for checkbox")
    }
}
