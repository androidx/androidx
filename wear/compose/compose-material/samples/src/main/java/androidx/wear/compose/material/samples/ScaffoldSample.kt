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

import android.annotation.SuppressLint
import androidx.annotation.Sampled
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberScalingLazyListState

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalAnimationApi::class, ExperimentalWearMaterialApi::class)
@Sampled
@Composable
fun SimpleScaffoldWithScrollIndicator() {

    val listState = rememberScalingLazyListState()
    val vignetteState = mutableStateOf(VignettePosition.TopAndBottom)
    val showVignette = mutableStateOf(true)

    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier
            )
        },
        vignette = {
            if (showVignette.value) {
                Vignette(vignettePosition = vignetteState.value)
            }
        },
        timeText = {
            TimeText()
        }
    ) {
        ScalingLazyColumn(
            contentPadding = PaddingValues(top = 40.dp),
            state = listState
        ) {
            item {
                Chip(
                    onClick = { showVignette.value = false },
                    label = { Text("No Vignette") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    onClick = {
                        showVignette.value = true
                        vignetteState.value = VignettePosition.Top
                    },
                    label = { Text("Top Vignette only") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    onClick = {
                        showVignette.value = true
                        vignetteState.value = VignettePosition.Bottom
                    },
                    label = { Text("Bottom Vignette only") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    onClick = {
                        showVignette.value = true
                        vignetteState.value = VignettePosition.TopAndBottom
                    },
                    label = { Text("Top and Bottom Vignette") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            items(20) {
                Chip(
                    onClick = { },
                    label = { Text("List item $it") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
