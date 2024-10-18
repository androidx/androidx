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

package androidx.wear.compose.material3.demos

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.Text

@Composable
fun SplitRadioButtonDemo() {
    var selectedRadioIndex by remember { mutableIntStateOf(0) }
    var selectedMultiLineRadioIndex by remember { mutableIntStateOf(0) }

    ScalingLazyDemo {
        item { ListHeader { Text("Split Radio Button") } }
        item {
            DemoSplitRadioButton(enabled = true, (selectedRadioIndex == 0)) {
                selectedRadioIndex = 0
            }
        }
        item {
            DemoSplitRadioButton(enabled = true, (selectedRadioIndex == 1)) {
                selectedRadioIndex = 1
            }
        }
        item { ListHeader { Text("Disabled Radio Button") } }
        item { DemoSplitRadioButton(enabled = false, selected = true) }
        item { DemoSplitRadioButton(enabled = false, selected = false) }
        item { ListHeader { Text("Multi-line") } }
        item {
            DemoSplitRadioButton(
                enabled = true,
                selected = selectedMultiLineRadioIndex == 0,
                onSelected = { selectedMultiLineRadioIndex = 0 },
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoSplitRadioButton(
                enabled = true,
                selected = selectedMultiLineRadioIndex == 1,
                onSelected = { selectedMultiLineRadioIndex = 1 },
                primary = "Primary label with at most three lines of content"
            )
        }
        item {
            DemoSplitRadioButton(
                enabled = true,
                selected = selectedMultiLineRadioIndex == 2,
                onSelected = { selectedMultiLineRadioIndex = 2 },
                primary = "Primary label with at most three lines of content",
                secondary = "Secondary label with at most two lines of text"
            )
        }
        item {
            DemoSplitRadioButton(
                enabled = true,
                selected = selectedMultiLineRadioIndex == 3,
                onSelected = { selectedMultiLineRadioIndex = 3 },
                primary = "Override the maximum number of primary label content to be four",
                primaryMaxLines = 4,
            )
        }
        item { ListHeader { Text("Disabled Multi-line") } }
        for (selected in booleanArrayOf(true, false)) {
            item {
                DemoSplitRadioButton(
                    enabled = false,
                    selected = selected,
                    primary = "Primary label",
                    secondary = "Secondary label"
                )
            }
        }
    }
}

@Composable
private fun DemoSplitRadioButton(
    enabled: Boolean,
    selected: Boolean,
    primary: String = "Primary label",
    primaryMaxLines: Int? = null,
    secondary: String? = null,
    onSelected: () -> Unit = {},
) {
    val context = LocalContext.current
    SplitRadioButton(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                primary,
                Modifier.fillMaxWidth(),
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
        selected = selected,
        onSelectionClick = onSelected,
        selectionContentDescription = primary,
        onContainerClick = {
            val toastText = primary + " " + if (selected) "Checked" else "Not Checked"
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
        },
        containerClickLabel = "click",
        enabled = enabled,
    )
}
