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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun SimpleButtonSample() {
    Button(onClick = { /* Do something */ }, label = { Text("Button") })
}

@Sampled
@Composable
fun ButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Button(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Button") },
        secondaryLabel = { Text("with long click") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun ButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}

@Sampled
@Composable
fun SimpleFilledTonalButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun FilledTonalButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    FilledTonalButton(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Filled Tonal Button") },
        secondaryLabel = { Text("with long click") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun FilledTonalButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun SimpleFilledVariantButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    Button(
        onClick = { /* Do something */ },
        colors = ButtonDefaults.filledVariantButtonColors(),
        label = { Text("Filled Variant Button") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun FilledVariantButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    Button(
        onClick = { /* Do something */ },
        colors = ButtonDefaults.filledVariantButtonColors(),
        label = { Text("Filled Variant Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}

@Sampled
@Composable
fun SimpleOutlinedButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun OutlinedButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedButton(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Outlined Button") },
        secondaryLabel = { Text("with long click") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun OutlinedButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun SimpleChildButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    ChildButton(
        onClick = { /* Do something */ },
        label = { Text("Child Button") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun ChildButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    ChildButton(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Child Button") },
        secondaryLabel = { Text("with long click") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun ChildButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    ChildButton(
        onClick = { /* Do something */ },
        label = { Text("Child Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}

@Sampled
@Composable
fun CompactButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    CompactButton(
        onClick = { /* Do something */ },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
            )
        },
        modifier = modifier,
    ) {
        Text("Compact Button", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Sampled
@Composable
fun CompactButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    CompactButton(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Long clickable") },
        modifier = modifier,
    )
}

@Sampled
@Composable
fun FilledTonalCompactButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    CompactButton(
        onClick = { /* Do something */ },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
            )
        },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier,
    ) {
        Text("Filled Tonal Compact Button", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Sampled
@Composable
fun OutlinedCompactButtonSample(modifier: Modifier = Modifier.fillMaxWidth()) {
    CompactButton(
        onClick = { /* Do something */ },
        icon = {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Expand",
                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
            )
        },
        colors = ButtonDefaults.outlinedButtonColors(),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = modifier,
    ) {
        Text("Show More", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
