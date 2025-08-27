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

@file:OptIn(ExperimentalMaterial3Api::class)

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun FilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = { /* Do Nothing */ }) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("My Button")
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun SplitButtonWithUnCheckableTrailingButtonSample() {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = { /* Do Nothing */ }) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("My Button")
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(onClick = { /* Do Nothing */ }) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                        contentDescription = description,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun SplitButtonWithDropdownMenuSample() {
    var checked by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().wrapContentSize()) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = { /* Do Nothing */ }) {
                    Icon(
                        Icons.Filled.Edit,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                        contentDescription = "Localized description",
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("My Button")
                }
            },
            trailingButton = {
                val description = "Toggle Button"
                // Icon-only trailing button should have a tooltip for a11y.
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                    tooltip = { PlainTooltip { Text(description) } },
                    state = rememberTooltipState(),
                ) {
                    SplitButtonDefaults.TrailingButton(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier =
                            Modifier.semantics {
                                stateDescription = if (checked) "Expanded" else "Collapsed"
                                contentDescription = description
                            },
                    ) {
                        val rotation: Float by
                            animateFloatAsState(
                                targetValue = if (checked) 180f else 0f,
                                label = "Trailing Icon Rotation",
                            )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            modifier =
                                Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                    this.rotationZ = rotation
                                },
                            contentDescription = "Localized description",
                        )
                    }
                }
            },
        )

        DropdownMenu(expanded = checked, onDismissRequest = { checked = false }) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun TonalSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.TonalLeadingButton(onClick = { /* Do Nothing */ }) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("My Button")
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun ElevatedSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.ElevatedLeadingButton(onClick = { /* Do Nothing */ }) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("My Button")
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.ElevatedTrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun OutlinedSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.OutlinedLeadingButton(onClick = { /* Do Nothing */ }) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("My Button")
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.OutlinedTrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun SplitButtonWithTextSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = { /* Do Nothing */ }) { Text("My Button") }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun SplitButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            val description = "Button"
            // Icon-only leading button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.LeadingButton(onClick = { /* Do Nothing */ }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = description,
                        Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    )
                }
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun XSmallFilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val size = SplitButtonDefaults.ExtraSmallContainerHeight

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
                modifier = Modifier.heightIn(size),
                shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(size)),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text("My Button", style = ButtonDefaults.textStyleFor(size))
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                                .graphicsLayer { this.rotationZ = rotation },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun MediumFilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val size = SplitButtonDefaults.MediumContainerHeight

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
                modifier = Modifier.heightIn(size),
                shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(size)),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text("My Button", style = ButtonDefaults.textStyleFor(size))
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                                .graphicsLayer { this.rotationZ = rotation },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun LargeFilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val size = SplitButtonDefaults.LargeContainerHeight

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
                modifier = Modifier.heightIn(size),
                shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(size)),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text("My Button", style = ButtonDefaults.textStyleFor(size))
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                                .graphicsLayer { this.rotationZ = rotation },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun ExtraLargeFilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val size = SplitButtonDefaults.ExtraLargeContainerHeight

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
                modifier = Modifier.heightIn(size),
                shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(size)),
                    contentDescription = "Localized description",
                )
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                Text("Button", style = ButtonDefaults.textStyleFor(size))
            }
        },
        trailingButton = {
            val description = "Toggle Button"
            // Icon-only trailing button should have a tooltip for a11y.
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                SplitButtonDefaults.TrailingButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier =
                        Modifier.heightIn(size).semantics {
                            stateDescription = if (checked) "Expanded" else "Collapsed"
                            contentDescription = description
                        },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                ) {
                    val rotation: Float by
                        animateFloatAsState(
                            targetValue = if (checked) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        modifier =
                            Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                                .graphicsLayer { this.rotationZ = rotation },
                        contentDescription = "Localized description",
                    )
                }
            }
        },
    )
}
