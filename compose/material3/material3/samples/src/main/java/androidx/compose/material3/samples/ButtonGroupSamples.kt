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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ButtonGroupSample() {
    ButtonGroup {
        val options = listOf("A", "B", "C", "D")
        val checked = remember { mutableStateListOf(false, false, false, false) }
        val modifiers =
            listOf(
                Modifier.weight(1.5f),
                Modifier.weight(1f),
                Modifier.width(90.dp),
                Modifier.weight(1f)
            )
        options.fastForEachIndexed { index, label ->
            ToggleButton(
                checked = checked[index],
                onCheckedChange = { checked[index] = it },
                modifier = modifiers[index]
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
fun SingleSelectConnectedButtonGroupSample() {
    val startButtonShapes =
        ButtonShapes(
            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
            pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
            checkedShape = ToggleButtonDefaults.checkedShape
        )
    val middleButtonShapes =
        ToggleButtonDefaults.shapes(
            ShapeDefaults.Small,
            ToggleButtonDefaults.pressedShape,
            ToggleButtonDefaults.checkedShape
        )
    val endButtonShapes =
        ButtonShapes(
            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
            pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
            checkedShape = ToggleButtonDefaults.checkedShape
        )
    val options = listOf("Work", "Restaurant", "Coffee")
    val unCheckedIcons =
        listOf(Icons.Outlined.Work, Icons.Outlined.Restaurant, Icons.Outlined.Coffee)
    val checkedIcons = listOf(Icons.Filled.Work, Icons.Filled.Restaurant, Icons.Filled.Coffee)
    val shapes = listOf(startButtonShapes, middleButtonShapes, endButtonShapes)
    var selectedIndex by remember { mutableIntStateOf(0) }

    ButtonGroup(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.connectedSpaceBetween),
        animateFraction = 0f
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { selectedIndex = index },
                shapes = shapes[index]
            ) {
                Icon(
                    if (selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                    contentDescription = "Localized description"
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Sampled
@Composable
fun MultiSelectConnectedButtonGroupSample() {
    val startButtonShapes =
        ButtonShapes(
            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
            pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
            checkedShape = ToggleButtonDefaults.checkedShape
        )
    val middleButtonShapes =
        ToggleButtonDefaults.shapes(
            ShapeDefaults.Small,
            ToggleButtonDefaults.pressedShape,
            ToggleButtonDefaults.checkedShape
        )
    val endButtonShapes =
        ButtonShapes(
            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
            pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
            checkedShape = ToggleButtonDefaults.checkedShape
        )
    val options = listOf("Work", "Restaurant", "Coffee")
    val unCheckedIcons =
        listOf(Icons.Outlined.Work, Icons.Outlined.Restaurant, Icons.Outlined.Coffee)
    val checkedIcons = listOf(Icons.Filled.Work, Icons.Filled.Restaurant, Icons.Filled.Coffee)
    val shapes = listOf(startButtonShapes, middleButtonShapes, endButtonShapes)
    val checked = remember { mutableStateListOf(false, false, false) }

    ButtonGroup(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.connectedSpaceBetween),
        animateFraction = 0f
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = checked[index],
                onCheckedChange = { checked[index] = it },
                shapes = shapes[index]
            ) {
                Icon(
                    if (checked[index]) checkedIcons[index] else unCheckedIcons[index],
                    contentDescription = "Localized description"
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(label)
            }
        }
    }
}
