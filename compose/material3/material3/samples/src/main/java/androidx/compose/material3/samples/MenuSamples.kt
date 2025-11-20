/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MenuSample() {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
        // Icon button should have a tooltip associated with it for a11y.
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text("Localized description") } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { /* Handle edit! */ },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { /* Handle settings! */ },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { /* Handle send feedback! */ },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                trailingIcon = { Text("F11", textAlign = TextAlign.Center) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun GroupedMenuSample() {
    val groupInteractionSource = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    var homeChecked by remember { mutableStateOf(false) }
    val groupLabels = listOf("Modification", "Navigation")
    val groupItemLabels = listOf(listOf("Edit", "Settings"), listOf("Home", "More Options"))
    val groupItemLeadingIcons =
        listOf(
            listOf(Icons.Outlined.Edit, Icons.Outlined.Settings),
            listOf(null, Icons.Outlined.Info),
        )
    val groupItemCheckedLeadingIcons =
        listOf(
            listOf(Icons.Filled.Edit, Icons.Filled.Settings),
            listOf(Icons.Filled.Check, Icons.Filled.Info),
        )
    val groupItemTrailingIcons: List<List<ImageVector?>> =
        listOf(
            listOf(null, null),
            listOf(
                if (homeChecked) {
                    Icons.Filled.Home
                } else {
                    Icons.Outlined.Home
                },
                Icons.Filled.MoreVert,
            ),
        )
    val groupItemSupportingText: List<List<String?>> =
        listOf(listOf("Edit mode", null), listOf(null, "Opens menu"))
    val checked = remember {
        listOf(mutableStateListOf(false, false), mutableStateListOf(false, false))
    }

    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
        // Icon button should have a tooltip associated with it for a11y.
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text("Localized description") } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
            }
        }
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            val groupCount = groupLabels.size
            groupLabels.fastForEachIndexed { groupIndex, label ->
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(groupIndex, groupCount),
                    interactionSource = groupInteractionSource,
                ) {
                    MenuDefaults.Label { Text(label) }
                    HorizontalDivider(
                        modifier =
                            Modifier.padding(horizontal = MenuDefaults.HorizontalDividerPadding)
                    )
                    val groupItemCount = groupItemLabels[groupIndex].size
                    groupItemLabels[groupIndex].fastForEachIndexed { itemIndex, itemLabel ->
                        DropdownMenuItem(
                            text = {
                                val itemSupportingText =
                                    groupItemSupportingText[groupIndex][itemIndex]
                                if (itemSupportingText != null) {
                                    MenuDefaults.LabelWithSupportingText(
                                        supportingText = { Text(itemSupportingText) }
                                    ) {
                                        Text(itemLabel)
                                    }
                                } else {
                                    Text(itemLabel)
                                }
                            },
                            shapes = MenuDefaults.itemShape(itemIndex, groupItemCount),
                            leadingIcon =
                                groupItemLeadingIcons[groupIndex][itemIndex]?.let { iconData ->
                                    { Icon(iconData, contentDescription = null) }
                                },
                            checkedLeadingIcon = {
                                Icon(
                                    groupItemCheckedLeadingIcons[groupIndex][itemIndex],
                                    contentDescription = null,
                                )
                            },
                            trailingIcon =
                                groupItemTrailingIcons[groupIndex][itemIndex]?.let { iconData ->
                                    { Icon(iconData, contentDescription = null) }
                                },
                            checked = checked[groupIndex][itemIndex],
                            onCheckedChange = { checked[groupIndex][itemIndex] = it },
                        )
                    }
                }

                if (groupIndex != groupCount - 1) {
                    Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                }
            }
            if (checked.last().last()) {
                DropdownMenuButtonGroup()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun MenuWithScrollStateSample() {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
        // Icon button should have a tooltip associated with it for a11y.
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text("Localized description") } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = scrollState,
        ) {
            repeat(30) {
                DropdownMenuItem(
                    text = { Text("Item ${it + 1}") },
                    onClick = { /* TODO */ },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                )
            }
        }
        LaunchedEffect(expanded) {
            if (expanded) {
                // Scroll to show the bottom menu items.
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DropdownMenuButtonGroup() {
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        customItem(
            buttonGroupContent = {
                FilledIconButton(
                    onClick = {},
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MenuDefaults.groupStandardContainerColor
                        ),
                ) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "Localized description")
                }
            },
            menuContent = {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(Icons.Filled.ThumbUp, contentDescription = "Localized description")
                    },
                    text = { Text("Thumbs up") },
                    onClick = {},
                )
            },
        )

        customItem(
            buttonGroupContent = {
                FilledIconButton(
                    onClick = {},
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MenuDefaults.groupStandardContainerColor
                        ),
                ) {
                    Icon(Icons.Filled.ThumbDown, contentDescription = "Localized description")
                }
            },
            menuContent = {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(Icons.Filled.ThumbDown, contentDescription = "Localized description")
                    },
                    text = { Text("Thumbs down") },
                    onClick = {},
                )
            },
        )

        customItem(
            buttonGroupContent = {
                FilledIconButton(
                    onClick = {},
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MenuDefaults.groupStandardContainerColor
                        ),
                ) {
                    Icon(Icons.Filled.TagFaces, contentDescription = "Localized description")
                }
            },
            menuContent = {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(Icons.Filled.TagFaces, contentDescription = "Localized description")
                    },
                    text = { Text("Emotes") },
                    onClick = {},
                )
            },
        )
    }
}
