/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
fun SurfaceTransformationDemo() {
    val transformationSpec = rememberTransformationSpec()
    TransformingLazyColumn(
        modifier = Modifier.background(Color.Black),
        contentPadding = PaddingValues(vertical = 50.dp, horizontal = 10.dp),
    ) {
        item {
            ListHeader(
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Buttons")
            }
        }
        item {
            Button(
                onClick = {},
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Button")
            }
        }
        item {
            FilledTonalButton(
                onClick = {},
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Filled Tonal Button")
            }
        }
        item {
            OutlinedButton(
                onClick = {},
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Filled Tonal Button")
            }
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = {
                    Text(
                        "Child Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                label = {
                    Text(
                        "Compact Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }
        item {
            var checked by remember { mutableStateOf(true) }
            CheckboxButton(
                label = { Text("Checkbox Button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = {
                    Text("With secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                checked = checked,
                onCheckedChange = { checked = it },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite icon") },
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            var checked by remember { mutableStateOf(true) }
            SplitCheckboxButton(
                label = {
                    Text("Split Checkbox Button", maxLines = 3, overflow = TextOverflow.Ellipsis)
                },
                checked = checked,
                onCheckedChange = { checked = it },
                toggleContentDescription = "Split Checkbox Button Sample",
                onContainerClick = {
                    /* Do something */
                },
                containerClickLabel = "click",
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            var selectedButton by remember { mutableIntStateOf(0) }
            // RadioButton uses the Radio selection control by default.
            RadioButton(
                label = { Text("Radio button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = {
                    Text("With secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                selected = selectedButton == 0,
                onSelect = { selectedButton = 0 },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite icon") },
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            var selectedButton by remember { mutableIntStateOf(0) }
            // SplitRadioButton uses the Radio selection control by default.
            SplitRadioButton(
                label = { Text("First Button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
                selected = selectedButton == 0,
                onSelectionClick = { selectedButton = 0 },
                selectionContentDescription = "First",
                onContainerClick = {
                    /* Do something */
                },
                containerClickLabel = "click",
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            var checked by remember { mutableStateOf(true) }
            SwitchButton(
                label = { Text("Switch Button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = {
                    Text("With secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                checked = checked,
                onCheckedChange = { checked = it },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite icon") },
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            var checked by remember { mutableStateOf(true) }
            SplitSwitchButton(
                label = {
                    Text("Split Switch Button", maxLines = 3, overflow = TextOverflow.Ellipsis)
                },
                checked = checked,
                onCheckedChange = { checked = it },
                toggleContentDescription = "Split Switch Button Sample",
                onContainerClick = {
                    /* Do something */
                },
                enabled = true,
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }

        item {
            ListHeader(
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Cards")
            }
        }

        item {
            Card(
                onClick = { /* Do something */ },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Card")
            }
        }

        item {
            AppCard(
                onClick = { /* Do something */ },
                appName = { Text("App name") },
                appImage = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.star_big_off),
                        contentDescription = "Star icon",
                        modifier =
                            Modifier.size(CardDefaults.AppImageSize)
                                .wrapContentSize(align = Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                title = { Text("Card title") },
                time = { Text("Now") },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            ) {
                Text("Card content")
            }
        }

        item {
            TitleCard(
                onClick = { /* Do something */ },
                time = { Text("Now") },
                title = { Text("Title card") },
                subtitle = { Text("Subtitle") },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(this, transformationSpec),
            )
        }
    }
}
