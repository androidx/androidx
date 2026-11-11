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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberRevealState
import kotlinx.coroutines.launch

@Composable
fun SplitCheckboxButtonDemo() {
    val slcState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val swipeToRevealItems = remember { mutableStateListOf("Item 1", "Item 2", "Item 3") }

    ScalingLazyDemo(state = slcState) {
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
                secondary = "Monday",
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label with at most three lines of content ",
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label with at most three lines of content",
                secondary = "Secondary label with at most two lines of text",
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
        item { ListHeader { Text("Disabled Multi-line") } }
        for (initiallyChecked in booleanArrayOf(true, false)) {
            item {
                DemoSplitCheckboxButton(
                    enabled = false,
                    initiallyChecked = initiallyChecked,
                    primary = "Primary label",
                    secondary = "Secondary label",
                )
            }
        }
        item { ListHeader { Text("Custom Shapes") } }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Cut Corner Shape",
                secondary = "Secondary label",
                shape = CutCornerShape(14.dp),
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = false,
                primary = "Asymmetric Corners",
                secondary = "Secondary label",
                shape =
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 6.dp,
                        bottomEnd = 6.dp,
                        bottomStart = 24.dp,
                    ),
            )
        }
        item {
            DemoSplitCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Pill / Circle Corners",
                secondary = "Secondary label",
                shape = CircleShape,
            )
        }
        item { ListHeader { Text("Swipe to Reveal") } }
        items(items = swipeToRevealItems, key = { it }) { label ->
            val revealState = rememberRevealState()

            LaunchedEffect(slcState.isScrollInProgress) {
                if (
                    slcState.isScrollInProgress && revealState.currentValue != RevealValue.Covered
                ) {
                    coroutineScope.launch {
                        revealState.animateTo(targetValue = RevealValue.Covered)
                    }
                }
            }

            SwipeToReveal(
                revealState = revealState,
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { swipeToRevealItems.remove(label) },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { swipeToRevealItems.remove(label) },
            ) {
                DemoSplitCheckboxButton(
                    enabled = true,
                    initiallyChecked = true,
                    primary = label,
                    modifier =
                        Modifier.semantics {
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        swipeToRevealItems.remove(label)
                                        true
                                    }
                                )
                        },
                )
            }
        }
    }
}

@Composable
private fun DemoSplitCheckboxButton(
    enabled: Boolean,
    initiallyChecked: Boolean,
    modifier: Modifier = Modifier,
    primary: String = "Primary label",
    primaryMaxLines: Int? = null,
    secondary: String? = null,
    shape: Shape = CheckboxButtonDefaults.splitCheckboxButtonShape,
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
        shape = shape,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = primaryMaxLines ?: LocalTextConfiguration.current.maxLines,
            )
        },
        secondaryLabel = secondary?.let { { Text(secondary, modifier = Modifier.fillMaxWidth()) } },
    )
}
