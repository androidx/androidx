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

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.Text

@Composable
fun SplitCheckboxButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Checkbox") } }
        item { DemoSplitCheckboxButton(enabled = true, initiallyChecked = true) }
        item { DemoSplitCheckboxButton(enabled = true, initiallyChecked = false) }
        item { ListHeader { Text("Disabled Checkbox") } }
        item { DemoSplitCheckboxButton(enabled = false, initiallyChecked = true) }
        item { DemoSplitCheckboxButton(enabled = false, initiallyChecked = false) }
        item { ListHeader { Text("Multi-line") } }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with at most three lines of content "
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with at most three lines of content",
                secondary = "Secondary label with at most two lines of text"
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Override the maximum number of primary label content to be four",
                primaryMaxLines = 4,
            )
        }
    }
}

@Composable
private fun DemoSplitCheckboxButton(
    enabled: Boolean,
    initiallyChecked: Boolean,
    primary: String = "Primary label",
    primaryMaxLines: Int? = null,
    secondary: String? = null
) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    val context = LocalContext.current
    SplitCheckboxButton(
        checked = checked,
        onCheckedChange = { checked = it },
        toggleContentDescription = primary,
        containerClickLabel = "click",
        onContainerClick = {
            val toastText = if (checked) "Checked" else "Not Checked"
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = primaryMaxLines ?: LocalTextConfiguration.current.maxLines
            )
        },
        secondaryLabel =
            secondary?.let {
                {
                    Text(
                        secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
    )
}
