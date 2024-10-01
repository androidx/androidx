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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun FilledSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
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
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier =
                    Modifier.semantics {
                        stateDescription = if (checked) "Checked" else "Unchecked"
                        contentDescription = "Toggle Button"
                    },
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun SplitButtonWithUnCheckableTrailingButtonSample() {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
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
            SplitButtonDefaults.TrailingButton(
                onClick = { /* Do Nothing */ },
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                    contentDescription = "Localized description"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
@Preview
fun TonalSplitButtonSample() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.TonalLeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
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
            SplitButtonDefaults.TonalTrailingButton(
                checked = checked,
                onCheckedChange = { checked = it },
                //                modifier =
                //                    Modifier.semantics {
                //                        stateDescription = if (checked) "Checked" else "Unchecked"
                //                        contentDescription = "Toggle Button"
                //                    },
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
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
            SplitButtonDefaults.ElevatedLeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
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
            SplitButtonDefaults.ElevatedTrailingButton(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier =
                    Modifier.semantics {
                        stateDescription = if (checked) "Checked" else "Unchecked"
                        contentDescription = "Toggle Button"
                    },
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
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
            SplitButtonDefaults.OutlinedLeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
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
            SplitButtonDefaults.OutlinedTrailingButton(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier =
                    Modifier.semantics {
                        stateDescription = if (checked) "Checked" else "Unchecked"
                        contentDescription = "Toggle Button"
                    },
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
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
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
                Text("My Button")
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = { checked = it }
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
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
            SplitButtonDefaults.LeadingButton(
                onClick = { /* Do Nothing */ },
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Localized description",
                    Modifier.size(SplitButtonDefaults.LeadingIconSize)
                )
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = { checked = it }
            ) {
                val rotation: Float by
                    animateFloatAsState(
                        targetValue = if (checked) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier =
                        Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                            this.rotationZ = rotation
                        },
                    contentDescription = "Localized description"
                )
            }
        }
    )
}
