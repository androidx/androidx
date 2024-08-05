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

package androidx.compose.material3.demos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleableFloatingActionButton
import androidx.compose.material3.ToggleableFloatingActionButtonDefaults
import androidx.compose.material3.ToggleableFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingActionButtonMenuDemo() {
    Box(Modifier.fillMaxSize()) {
        val colorOptions =
            listOf(
                ColorOption(
                    label = "Primary Container",
                    initialContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    finalContainerColor = MaterialTheme.colorScheme.primary,
                    initialIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    finalIconColor = MaterialTheme.colorScheme.onPrimary,
                    itemContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                ColorOption(
                    label = "Secondary Container",
                    initialContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    finalContainerColor = MaterialTheme.colorScheme.secondary,
                    initialIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    finalIconColor = MaterialTheme.colorScheme.onSecondary,
                    itemContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                ColorOption(
                    label = "Tertiary Container",
                    initialContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    finalContainerColor = MaterialTheme.colorScheme.tertiary,
                    initialIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    finalIconColor = MaterialTheme.colorScheme.onTertiary,
                    itemContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                ColorOption(
                    label = "Primary",
                    initialContainerColor = MaterialTheme.colorScheme.primary,
                    finalContainerColor = MaterialTheme.colorScheme.primary,
                    initialIconColor = MaterialTheme.colorScheme.onPrimary,
                    finalIconColor = MaterialTheme.colorScheme.onPrimary,
                    itemContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                ColorOption(
                    label = "Secondary",
                    initialContainerColor = MaterialTheme.colorScheme.secondary,
                    finalContainerColor = MaterialTheme.colorScheme.secondary,
                    initialIconColor = MaterialTheme.colorScheme.onSecondary,
                    finalIconColor = MaterialTheme.colorScheme.onSecondary,
                    itemContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                ColorOption(
                    label = "Tertiary",
                    initialContainerColor = MaterialTheme.colorScheme.tertiary,
                    finalContainerColor = MaterialTheme.colorScheme.tertiary,
                    initialIconColor = MaterialTheme.colorScheme.onTertiary,
                    finalIconColor = MaterialTheme.colorScheme.onTertiary,
                    itemContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
            )
        val (selectedColor, onColorSelected) = remember { mutableStateOf(colorOptions[0]) }

        val sizeOptions =
            listOf(
                SizeOption(
                    label = "Default",
                    initialContainerSize = ToggleableFloatingActionButtonDefaults.containerSize(),
                    initialContainerCornerRadius =
                        ToggleableFloatingActionButtonDefaults.containerCornerRadius(),
                    initialIconSize = ToggleableFloatingActionButtonDefaults.iconSize(),
                ),
                SizeOption(
                    label = "Medium",
                    initialContainerSize =
                        ToggleableFloatingActionButtonDefaults.containerSizeMedium(),
                    initialContainerCornerRadius =
                        ToggleableFloatingActionButtonDefaults.containerCornerRadiusMedium(),
                    initialIconSize = ToggleableFloatingActionButtonDefaults.iconSizeMedium(),
                ),
                SizeOption(
                    label = "Large",
                    initialContainerSize =
                        ToggleableFloatingActionButtonDefaults.containerSizeLarge(),
                    initialContainerCornerRadius =
                        ToggleableFloatingActionButtonDefaults.containerCornerRadiusLarge(),
                    initialIconSize = ToggleableFloatingActionButtonDefaults.iconSizeLarge(),
                ),
            )
        val (selectedSize, onSizeSelected) = remember { mutableStateOf(sizeOptions[0]) }

        val alignmentOptions =
            listOf(
                AlignmentOption(
                    label = "End",
                    alignment = Alignment.BottomEnd,
                    fabCheckedAlignment = Alignment.TopEnd,
                    menuAlignment = Alignment.End,
                ),
                AlignmentOption(
                    label = "Center",
                    alignment = Alignment.BottomCenter,
                    fabCheckedAlignment = Alignment.TopCenter,
                    menuAlignment = Alignment.CenterHorizontally,
                ),
                AlignmentOption(
                    label = "Start",
                    alignment = Alignment.BottomStart,
                    fabCheckedAlignment = Alignment.TopStart,
                    menuAlignment = Alignment.Start,
                ),
            )
        val (selectedAlignment, onAlignmentSelected) =
            remember { mutableStateOf(alignmentOptions[0]) }

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(Modifier.padding(bottom = 8.dp).semantics { isTraversalGroup = true }) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Color Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Column(Modifier.selectableGroup()) {
                colorOptions.forEach { color ->
                    Row(
                        Modifier.fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (color.label == selectedColor.label),
                                onClick = { onColorSelected(color) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (color.label == selectedColor.label), onClick = null)
                        Text(
                            text = color.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Row(
                Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { isTraversalGroup = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Size Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Column(Modifier.selectableGroup()) {
                sizeOptions.forEach { size ->
                    Row(
                        Modifier.fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (size.label == selectedSize.label),
                                onClick = { onSizeSelected(size) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (size.label == selectedSize.label), onClick = null)
                        Text(
                            text = size.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Row(
                Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { isTraversalGroup = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.AlignHorizontalCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Alignment Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Column(Modifier.selectableGroup()) {
                alignmentOptions.forEach { alignment ->
                    Row(
                        Modifier.fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (alignment.label == selectedAlignment.label),
                                onClick = { onAlignmentSelected(alignment) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (alignment.label == selectedAlignment.label),
                            onClick = null
                        )
                        Text(
                            text = alignment.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        val items =
            listOf(
                Icons.AutoMirrored.Filled.Message to "Reply",
                Icons.Filled.People to "Reply all",
                Icons.Filled.Contacts to "Forward",
                Icons.Filled.Snooze to "Snooze",
                Icons.Filled.Archive to "Archive",
                Icons.AutoMirrored.Filled.Label to "Label",
            )

        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

        FloatingActionButtonMenu(
            modifier = Modifier.align(selectedAlignment.alignment),
            expanded = fabMenuExpanded,
            horizontalAlignment = selectedAlignment.menuAlignment,
            button = {
                ToggleableFloatingActionButton(
                    modifier =
                        Modifier.semantics {
                            traversalIndex = -1f
                            stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Toggle menu"
                        },
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    contentAlignment = selectedAlignment.fabCheckedAlignment,
                    containerColor =
                        ToggleableFloatingActionButtonDefaults.containerColor(
                            selectedColor.initialContainerColor,
                            selectedColor.finalContainerColor
                        ),
                    containerSize = selectedSize.initialContainerSize,
                    containerCornerRadius = selectedSize.initialContainerCornerRadius,
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier =
                            Modifier.animateIcon(
                                checkedProgress = { checkedProgress },
                                color =
                                    ToggleableFloatingActionButtonDefaults.iconColor(
                                        selectedColor.initialIconColor,
                                        selectedColor.finalIconColor
                                    ),
                                size = selectedSize.initialIconSize
                            )
                    )
                }
            }
        ) {
            items.forEachIndexed { i, item ->
                FloatingActionButtonMenuItem(
                    modifier =
                        Modifier.semantics {
                            isTraversalGroup = true
                            // Add a custom a11y action to allow closing the menu when focusing
                            // the last menu item, since the close button comes before the first
                            // menu item in the traversal order.
                            if (i == items.size - 1) {
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction(
                                            label = "Close menu",
                                            action = {
                                                fabMenuExpanded = false
                                                true
                                            }
                                        )
                                    )
                            }
                        },
                    onClick = { fabMenuExpanded = false },
                    icon = { Icon(item.first, contentDescription = null) },
                    text = { Text(text = item.second) },
                    containerColor = selectedColor.itemContainerColor,
                )
            }
        }
    }
}

private data class ColorOption(
    val label: String,
    val initialContainerColor: Color,
    val finalContainerColor: Color,
    val initialIconColor: Color,
    val finalIconColor: Color,
    val itemContainerColor: Color,
)

private data class SizeOption(
    val label: String,
    val initialContainerSize: (Float) -> Dp,
    val initialContainerCornerRadius: (Float) -> Dp,
    val initialIconSize: (Float) -> Dp,
)

private data class AlignmentOption(
    val label: String,
    val alignment: Alignment,
    val fabCheckedAlignment: Alignment,
    val menuAlignment: Alignment.Horizontal,
)
