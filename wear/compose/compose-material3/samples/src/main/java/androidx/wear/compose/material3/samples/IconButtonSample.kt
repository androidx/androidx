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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.OutlinedIconButton

@Composable
@Sampled
fun IconButtonSample() {
    IconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Sampled
fun FilledIconButtonSample() {
    FilledIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Sampled
fun FilledVariantIconButtonSample() {
    FilledIconButton(
        onClick = { /* Do something */ },
        colors = IconButtonDefaults.filledVariantIconButtonColors()
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Sampled
fun FilledTonalIconButtonSample() {
    FilledTonalIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Sampled
fun OutlinedIconButtonSample() {
    OutlinedIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Sampled
@Composable
fun IconButtonWithOnLongClickSample(onLongClick: () -> Unit) {
    IconButton(
        onClick = { /* Do something for onClick*/ },
        onLongClick = onLongClick,
        onLongClickLabel = "Long click"
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Sampled
fun IconButtonWithCornerAnimationSample() {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = { /* Do something */ },
        shape = IconButtonDefaults.animatedShape(interactionSource),
        interactionSource = interactionSource
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}
