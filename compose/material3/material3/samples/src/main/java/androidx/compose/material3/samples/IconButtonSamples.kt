/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun IconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun IconButtonWithAnimatedShapeSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = { /* doSomething() */ }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun ExtraSmallNarrowSquareIconButtonsSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        // Small narrow round icon button
        FilledIconButton(
            onClick = { /* doSomething() */ },
            modifier =
                Modifier.minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.extraSmallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Narrow
                        )
                    ),
            shape = IconButtonDefaults.extraSmallSquareShape,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = description,
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun MediumRoundWideIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = { /* doSomething() */ },
            modifier =
                Modifier.size(
                    IconButtonDefaults.mediumContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Wide
                    )
                ),
            shape = IconButtonDefaults.mediumRoundShape,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = description,
                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun LargeRoundUniformOutlinedIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconButton(
            onClick = { /* doSomething() */ },
            modifier = Modifier.size(IconButtonDefaults.largeContainerSize()),
            shape = IconButtonDefaults.largeRoundShape,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = description,
                modifier = Modifier.size(IconButtonDefaults.largeIconSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun TintedIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(
                rememberVectorPainter(image = Icons.Filled.Lock),
                contentDescription = description,
                tint = Color.Red,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun IconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun IconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = { checked = it },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledIconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledIconButtonWithAnimatedShapeSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledIconButton(onClick = { /* doSomething() */ }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledIconToggleButton(
            checked = checked,
            onCheckedChange = { checked = it },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledTonalIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledTonalIconButtonWithAnimatedShapeSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconButton(
            onClick = { /* doSomething() */ },
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledTonalIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FilledTonalIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = { checked = it },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun OutlinedIconButtonSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Preview
@Sampled
@Composable
fun OutlinedIconButtonWithAnimatedShapeSample() {
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconButton(
            onClick = { /* doSomething() */ },
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = description)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun OutlinedIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun OutlinedIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    val description = "Localized description"
    // Icon button should have a tooltip associated with it for a11y.
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconToggleButton(
            checked = checked,
            onCheckedChange = { checked = it },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            if (checked) {
                Icon(Icons.Filled.Lock, contentDescription = description)
            } else {
                Icon(Icons.Outlined.Lock, contentDescription = description)
            }
        }
    }
}
