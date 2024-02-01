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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.OutlinedChip
import androidx.wear.compose.material.OutlinedCompactChip
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun ChipWithIconAndLabel() {
    Chip(
        onClick = { /* Do something */ },
        enabled = true,
        // Primary label can have up to 3 lines of text
        label = {
            Text(
                text = "Main label can span up to 3 lines",
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.IconSize)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    )
}

@Sampled
@Composable
fun OutlinedChipWithIconAndLabel() {
    OutlinedChip(
        onClick = { /* Do something */ },
        enabled = true,
        // Primary label can have up to 3 lines of text
        label = {
            Text(
                text = "Main label can span up to 3 lines",
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.IconSize)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    )
}

@Sampled
@Composable
fun ChipWithIconAndLabels() {
    Chip(
        onClick = { /* Do something */ },
        enabled = true,
        // Primary label has maximum 3 lines, Secondary label has maximum 2 lines.
        label = { Text(text = "Main label", maxLines = 3, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(text = "secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.IconSize)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    )
}

@Sampled
@Composable
fun CompactChipWithIconAndLabel() {
    CompactChip(
        onClick = { /* Do something */ },
        enabled = true,
        // CompactChip label should be no more than 1 line of text
        label = {
            Text("Single line label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.SmallIconSize),
            )
        },
    )
}

@Sampled
@Composable
fun CompactChipWithLabel() {
    CompactChip(
        onClick = { /* Do something */ },
        enabled = true,
        // CompactChip label should be no more than 1 line of text
        label = {
            Text(
                text = "Single line label",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
    )
}

@Sampled
@Composable
fun CompactChipWithIcon() {
    CompactChip(
        onClick = { /* Do something */ },
        enabled = true,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
    )
}

@Sampled
@Composable
fun OutlinedCompactChipWithIconAndLabel() {
    OutlinedCompactChip(
        onClick = { /* Do something */ },
        enabled = true,
        // CompactChip label should be no more than 1 line of text
        label = {
            Text("Single line label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(ChipDefaults.SmallIconSize),
            )
        },
    )
}
