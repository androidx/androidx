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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitSelectableButton
import androidx.wear.compose.material3.Text

@Composable
fun SplitSelectableButtonDemo() {
    var selectedRadioIndex by remember { mutableIntStateOf(0) }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text("Split Selectable Button") }
        }
        item {
            DemoSplitSelectableButton(
                enabled = true,
                (selectedRadioIndex == 0)
            ) { selectedRadioIndex = 0 }
        }
        item {
            DemoSplitSelectableButton(
                enabled = true,
                (selectedRadioIndex == 1)
            ) { selectedRadioIndex = 1 }
        }
        item {
            ListHeader { Text("Disabled Radio Button") }
        }
        item {
            DemoSplitSelectableButton(enabled = false, selected = true)
        }
        item {
            DemoSplitSelectableButton(enabled = false, selected = false)
        }
        item {
            ListHeader { Text("Multi-line") }
        }
        item {
            DemoSplitSelectableButton(
                enabled = true,
                selected = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoSplitSelectableButton(
                enabled = true,
                selected = true,
                primary = "Primary Label with 3 lines of very long content max"
            )
        }
        item {
            DemoSplitSelectableButton(
                enabled = true,
                selected = true,
                primary = "Primary Label with 3 lines of very long content max",
                secondary = "Secondary label with 2 lines"
            )
        }
    }
}

@Composable
private fun DemoSplitSelectableButton(
    enabled: Boolean,
    selected: Boolean,
    primary: String = "Primary label",
    secondary: String? = null,
    onSelected: () -> Unit = {},
) {
    val context = LocalContext.current
    SplitSelectableButton(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                primary,
                Modifier.fillMaxWidth(),
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = secondary?.let {
            {
                Text(
                    secondary,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        selected = selected,
        onSelectionClick = onSelected,
        onContainerClick = {
            val toastText = primary + " " + if (selected) "Checked" else "Not Checked"
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
        },
        enabled = enabled,
        selectionControl = {
            RadioButton(modifier = Modifier.semantics {
                contentDescription = primary
            })
        }
    )
}
