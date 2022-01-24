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

package androidx.wear.compose.integration.macrobenchmark.target

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.wear.compose.foundation.CurvedRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.CurvedText
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

private val ALERT_DIALOG = "alert-dialog"
private val CONFIRMATION_DIALOG = "confirmation-dialog"
private val STEPPER = "stepper"
private val SWIPE_DISMISS = "swipe-dismiss"

class BaselineActivity : ComponentActivity() {

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberSwipeDismissableNavController()
            val scrollState = rememberScrollState()

            MaterialTheme {
                Scaffold(
                    timeText = { TimeText() },
                    positionIndicator = { PositionIndicator(scrollState = scrollState) },
                    vignette = {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    },
                ) {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "start",
                        modifier = Modifier
                            .background(MaterialTheme.colors.background)
                            .semantics { contentDescription = SWIPE_DISMISS }
                    ) {
                        composable("start") {
                            Box {
                                CurvedTexts()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(state = scrollState)
                                        .padding(vertical = 16.dp)
                                        .semantics { contentDescription = CONTENT_DESCRIPTION },
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Dialogs(navController)
                                    Steppers(navController)
                                    Buttons()
                                    Cards()
                                    Chips()
                                    Sliders()
                                    Pickers()
                                }
                            }
                        }
                        composable(ALERT_DIALOG) {
                            Alert(
                                title = { Text("Alert") },
                                negativeButton = {},
                                positiveButton = {},
                            )
                        }
                        composable(CONFIRMATION_DIALOG) {
                            Confirmation(
                                onTimeout = { navController.popBackStack() },
                                content = { Text("Confirmation") },
                            )
                        }
                        composable(STEPPER) {
                            var value by remember { mutableStateOf(2f) }
                            Stepper(
                                value = value,
                                onValueChange = { value = it },
                                valueRange = 1f..4f,
                                steps = 7
                            ) { Text("Value: $value") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Buttons() {
    ListHeader { Text("Buttons") }
    Button(onClick = {}) { Text("Button") }
    CompactButton(onClick = {}) { Text("CompactButton") }
    ToggleButton(checked = true, onCheckedChange = {}) { Text("ToggleButton") }
}

@Composable
fun Cards() {
    ListHeader { Text("Cards") }
    Card(onClick = {}) { Text("Card") }
    AppCard(onClick = {}, appName = {}, time = {}, title = {}) { Text("AppCard") }
    TitleCard(onClick = {}, title = {}) { Text("TitleCard") }
}

@Composable
fun Chips() {
    ListHeader { Text("Chips") }
    Chip(onClick = {}, colors = ChipDefaults.primaryChipColors()) { Text("Chip") }
    CompactChip(onClick = {}, label = { Text("CompactChip") })
    ToggleChip(true, onCheckedChange = {}, label = { Text("ToggleChip") })
    SplitToggleChip(
        checked = true,
        onCheckedChange = {},
        label = { Text("SplitToggleChip") },
        onClick = {})
}

@Composable
fun CurvedTexts() {
    CurvedRow(anchor = 235f) {
        BasicCurvedText(
            "Basic",
            CurvedTextStyle(
                fontSize = 16.sp,
                color = Color.White,
                background = MaterialTheme.colors.background
            ),
            contentArcPadding = ArcPaddingValues(2.dp)
        )
    }
    CurvedRow(anchor = 310f) {
        CurvedText(text = "Curved")
    }
}

@Composable
fun Dialogs(navController: NavHostController) {
    ListHeader { Text("Dialogs") }
    CompactChip(
        onClick = { navController.navigate(ALERT_DIALOG) },
        colors = ChipDefaults.primaryChipColors(),
        label = { Text(ALERT_DIALOG) },
        modifier = Modifier.semantics { contentDescription = ALERT_DIALOG },

        )
    CompactChip(
        onClick = { navController.navigate(CONFIRMATION_DIALOG) },
        colors = ChipDefaults.primaryChipColors(),
        label = { Text(CONFIRMATION_DIALOG) },
        modifier = Modifier.semantics { contentDescription = CONFIRMATION_DIALOG },
    )
}

@Composable
fun Pickers() {
    ListHeader { Text("Pickers") }
    val items = listOf("One", "Two", "Three", "Four", "Five")
    Picker(
        state = rememberPickerState(items.size),
        option = { Text(items[it]) },
        modifier = Modifier.size(100.dp, 100.dp),
    )
}

@Composable
fun Sliders() {
    ListHeader { Text("Sliders") }
    var value by remember { mutableStateOf(4.5f) }
    InlineSlider(
        value = value,
        onValueChange = { value = it },
        valueRange = 3f..6f,
        steps = 5,
        segmented = false
    )
}

@Composable
fun Steppers(navController: NavHostController) {
    ListHeader { Text("Steppers") }
    CompactChip(
        onClick = { navController.navigate(STEPPER) },
        colors = ChipDefaults.primaryChipColors(),
        label = { Text(STEPPER) },
        modifier = Modifier.semantics { contentDescription = STEPPER },
        )
}
