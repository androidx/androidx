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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Sampled
@Composable
fun IconButtonSample() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun XSmallNarrowSquareIconButtonsSample() {
    // Small narrow round icon button
    FilledIconButton(
        onClick = { /* doSomething() */ },
        modifier =
            Modifier.minimumInteractiveComponentSize()
                .size(
                    IconButtonDefaults.xSmallContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Narrow
                    )
                ),
        shape = IconButtonDefaults.xSmallSquareShape
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Localized description",
            modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MediumRoundWideIconButtonSample() {
    IconButton(
        onClick = { /* doSomething() */ },
        modifier =
            Modifier.size(
                IconButtonDefaults.mediumContainerSize(
                    IconButtonDefaults.IconButtonWidthOption.Wide
                )
            ),
        shape = IconButtonDefaults.mediumRoundShape
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Localized description",
            modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun LargeRoundUniformOutlinedIconButtonSample() {
    OutlinedIconButton(
        onClick = { /* doSomething() */ },
        modifier = Modifier.size(IconButtonDefaults.largeContainerSize()),
        shape = IconButtonDefaults.largeRoundShape
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Localized description",
            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
        )
    }
}

@Preview
@Sampled
@Composable
fun TintedIconButtonSample() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(
            rememberVectorPainter(image = Icons.Outlined.Lock),
            contentDescription = "Localized description",
            tint = Color.Red
        )
    }
}

@Preview
@Sampled
@Composable
fun IconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@Preview
@Sampled
@Composable
fun IconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@Preview
@Sampled
@Composable
fun FilledIconButtonSample() {
    FilledIconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
    }
}

@Preview
@Sampled
@Composable
fun FilledIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    FilledIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun FilledIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shapes =
            IconButtonShapes(
                shape = IconButtonDefaults.smallRoundShape,
                pressedShape = IconButtonDefaults.smallPressedShape,
                checkedShape = IconButtonDefaults.smallSquareShape,
            )
    ) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@Preview
@Sampled
@Composable
fun FilledTonalIconButtonSample() {
    FilledTonalIconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
    }
}

@Preview
@Sampled
@Composable
fun FilledTonalIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    FilledTonalIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun FilledTonalIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shapes =
            IconButtonShapes(
                shape = IconButtonDefaults.smallRoundShape,
                pressedShape = IconButtonDefaults.smallPressedShape,
                checkedShape = IconButtonDefaults.smallSquareShape,
            )
    ) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@Preview
@Sampled
@Composable
fun OutlinedIconButtonSample() {
    OutlinedIconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
    }
}

@Preview
@Sampled
@Composable
fun OutlinedIconToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    OutlinedIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun OutlinedIconToggleButtonWithAnimatedShapeSample() {
    var checked by remember { mutableStateOf(false) }
    OutlinedIconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        shapes =
            IconButtonShapes(
                shape = IconButtonDefaults.smallRoundShape,
                pressedShape = IconButtonDefaults.smallPressedShape,
                checkedShape = IconButtonDefaults.smallSquareShape,
            )
    ) {
        if (checked) {
            Icon(Icons.Filled.Lock, contentDescription = "Localized description")
        } else {
            Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
        }
    }
}
