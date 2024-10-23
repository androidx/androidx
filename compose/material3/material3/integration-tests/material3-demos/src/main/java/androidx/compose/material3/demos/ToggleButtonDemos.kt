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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButtonDemos() {
    val horizontalScrollState = rememberScrollState()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.heightIn(ToggleButtonDefaults.MinHeight),
                contentAlignment = Alignment.Center
            ) {
                Text("XSmall")
            }
            Box(
                Modifier.heightIn(ToggleButtonDefaults.MinHeight),
                contentAlignment = Alignment.Center
            ) {
                Text("Small")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
                contentAlignment = Alignment.Center
            ) {
                Text("Medium")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
                contentAlignment = Alignment.Center
            ) {
                Text("Large")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
                contentAlignment = Alignment.Center
            ) {
                Text("XLarge")
            }
        }
        Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            ToggleButtons()
            ElevatedToggleButtons()
            TonalToggleButtons()
            OutlinedToggleButtons()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text("Filled", modifier = Modifier.height(48.dp))

        ToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XSmallSquareShape,
                    ToggleButtonDefaults.XSmallPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XSmallContentPadding
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
            Text("Label")
        }

        ToggleButton(checked = checked[1], onCheckedChange = { checked[1] = it }) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Label")
        }

        ToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.MediumSquareShape,
                    ToggleButtonDefaults.MediumPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.MediumContentPadding
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.titleMedium)
        }

        ToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.LargeSquareShape,
                    ToggleButtonDefaults.LargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.LargeContentPadding
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.LargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
        }

        ToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XLargeSquareShape,
                    ToggleButtonDefaults.XLargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XLargeContentPadding
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElevatedToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text("Elevated", modifier = Modifier.height(48.dp))

        ElevatedToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XSmallSquareShape,
                    ToggleButtonDefaults.XSmallPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XSmallContentPadding
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
            Text("Label")
        }

        ElevatedToggleButton(checked = checked[1], onCheckedChange = { checked[1] = it }) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Label")
        }

        ElevatedToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.MediumSquareShape,
                    ToggleButtonDefaults.MediumPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.MediumContentPadding
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.titleMedium)
        }

        ElevatedToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.LargeSquareShape,
                    ToggleButtonDefaults.LargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.LargeContentPadding
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.LargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
        }

        ElevatedToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XLargeSquareShape,
                    ToggleButtonDefaults.XLargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XLargeContentPadding
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TonalToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text("Tonal", modifier = Modifier.height(48.dp))

        TonalToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XSmallSquareShape,
                    ToggleButtonDefaults.XSmallPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XSmallContentPadding
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
            Text("Label")
        }

        TonalToggleButton(checked = checked[1], onCheckedChange = { checked[1] = it }) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Label")
        }

        TonalToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.MediumSquareShape,
                    ToggleButtonDefaults.MediumPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.MediumContentPadding
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.titleMedium)
        }

        TonalToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.LargeSquareShape,
                    ToggleButtonDefaults.LargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.LargeContentPadding
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.LargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
        }

        TonalToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XLargeSquareShape,
                    ToggleButtonDefaults.XLargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XLargeContentPadding
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OutlinedToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text("Outlined", modifier = Modifier.height(48.dp))

        OutlinedToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XSmallSquareShape,
                    ToggleButtonDefaults.XSmallPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XSmallContentPadding
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
            Text("Label")
        }

        OutlinedToggleButton(checked = checked[1], onCheckedChange = { checked[1] = it }) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Label")
        }

        OutlinedToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.MediumSquareShape,
                    ToggleButtonDefaults.MediumPressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.MediumContentPadding
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.LargeSquareShape,
                    ToggleButtonDefaults.LargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.LargeContentPadding
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.LargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
        }

        OutlinedToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
            shapes =
                ToggleButtonDefaults.shapes(
                    ToggleButtonDefaults.XLargeSquareShape,
                    ToggleButtonDefaults.XLargePressedShape,
                    ToggleButtonDefaults.checkedShape
                ),
            contentPadding = ButtonDefaults.XLargeContentPadding
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
            Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
