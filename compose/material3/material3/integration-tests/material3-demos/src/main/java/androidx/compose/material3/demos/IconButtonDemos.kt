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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Narrow
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Wide
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButtonMeasurementsDemo() {
    val rowScrollState = rememberScrollState()
    Row(modifier = Modifier.horizontalScroll(rowScrollState)) {
        val columnScrollState = rememberScrollState()
        val padding = 16.dp
        Column(
            modifier = Modifier.padding(horizontal = padding).verticalScroll(columnScrollState),
        ) {
            Spacer(modifier = Modifier.height(padding + 48.dp))
            Text("XSmall", modifier = Modifier.height(48.dp + padding))
            Text("Small", modifier = Modifier.height(48.dp + padding))
            Text("Medium", modifier = Modifier.height(56.dp + padding))
            Text("Large", modifier = Modifier.height(96.dp + padding))
            Text("XLarge", modifier = Modifier.height(136.dp + padding))
        }

        // Default
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(136.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text("Default", modifier = Modifier.height(48.dp))
            // XSmall uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xSmallContainerSize()),
                shape = IconButtonDefaults.xSmallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
                )
            }

            // Small uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize)
                )
            }

            // Medium uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                )
            }

            // Large uniform round icon button
            FilledIconButton(
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

            // XLarge uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xLargeContainerSize()),
                shape = IconButtonDefaults.xLargeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xLargeIconSize)
                )
            }
        }

        // Narrow
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(104.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text("Narrow", modifier = Modifier.height(48.dp))

            // XSmall narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xSmallContainerSize(Narrow)),
                shape = IconButtonDefaults.xSmallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
                )
            }

            // Small narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize(Narrow)),
                shape = IconButtonDefaults.smallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize)
                )
            }

            // Medium narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(Narrow)),
                shape = IconButtonDefaults.mediumRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                )
            }

            // Large narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize(Narrow)),
                shape = IconButtonDefaults.largeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                )
            }

            // XLarge narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xLargeContainerSize(Narrow)),
                shape = IconButtonDefaults.xLargeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xLargeIconSize)
                )
            }
        }

        // Wide
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(184.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding)
        ) {
            Text("Wide", modifier = Modifier.height(48.dp))

            // XSmall wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xSmallContainerSize(Wide)),
                shape = IconButtonDefaults.xSmallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
                )
            }
            // Small wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize(Wide)),
                shape = IconButtonDefaults.smallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize)
                )
            }

            // medium wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(Wide)),
                shape = IconButtonDefaults.mediumRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                )
            }

            // Large wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize(Wide)),
                shape = IconButtonDefaults.largeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                )
            }

            // XLarge wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.xLargeContainerSize(Wide)),
                shape = IconButtonDefaults.xLargeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xLargeIconSize)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButtonCornerRadiusDemo() {
    Column {
        val rowScrollState = rememberScrollState()
        val padding = 16.dp
        // uniform round row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // xsmall round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.xSmallContainerSize()),
                shape = IconButtonDefaults.xSmallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
                )
            }

            // Small round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize)
                )
            }

            // Medium round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                )
            }

            // Large uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.largeContainerSize()),
                shape = IconButtonDefaults.largeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                )
            }

            // XLarge uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.xLargeContainerSize()),
                shape = IconButtonDefaults.xLargeRoundShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xLargeIconSize)
                )
            }
        }

        // uniform square row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // xsmall square icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.xSmallContainerSize()),
                shape = IconButtonDefaults.xSmallSquareShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xSmallIconSize)
                )
            }

            // Small round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallSquareShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize)
                )
            }

            // Medium round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumSquareShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                )
            }

            // Large uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.largeContainerSize()),
                shape = IconButtonDefaults.largeSquareShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                )
            }

            // XLarge uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.xLargeContainerSize()),
                shape = IconButtonDefaults.xLargeSquareShape
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.xLargeIconSize)
                )
            }
        }
    }
}

@Composable
fun IconToggleButtonsDemo() {
    Column {
        val rowScrollState = rememberScrollState()
        val padding = 16.dp
        // unselected round row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(72.dp))
            Text("Filled")
            Text("Tonal")
            Text("Outline")
            Text("Standard")
        }
        // unselected round row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Unselected")

            FilledIconToggleButton(checked = false, onCheckedChange = { /* change the state */ }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            FilledTonalIconToggleButton(
                checked = false,
                onCheckedChange = { /* change the state */ }
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            OutlinedIconToggleButton(
                checked = false,
                onCheckedChange = { /* change the state */ }
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            IconToggleButton(checked = false, onCheckedChange = { /* change the state */ }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }
        }

        // unselected round row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Selected")
            FilledIconToggleButton(checked = true, onCheckedChange = { /* change the state */ }) {
                Icon(Icons.Filled.Edit, contentDescription = "Localized description")
            }

            FilledTonalIconToggleButton(
                checked = true,
                onCheckedChange = { /* change the state */ }
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Localized description")
            }

            OutlinedIconToggleButton(checked = true, onCheckedChange = { /* change the state */ }) {
                Icon(Icons.Filled.Edit, contentDescription = "Localized description")
            }

            IconToggleButton(checked = true, onCheckedChange = { /* change the state */ }) {
                Icon(Icons.Filled.Edit, contentDescription = "Localized description")
            }
        }
    }
}
