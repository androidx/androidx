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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SwipeToRevealCard
import androidx.wear.compose.material.SwipeToRevealChip
import androidx.wear.compose.material.SwipeToRevealDefaults
import androidx.wear.compose.material.Text

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
@Sampled
fun SwipeToRevealChipSample() {
    SwipeToRevealChip(
        revealState = rememberRevealState(),
        modifier = Modifier.fillMaxWidth(),
        primaryAction = SwipeToRevealDefaults.primaryAction(
            icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
            label = { Text("Delete") },
            onClick = { /* Add the click handler here */ }
        ),
        secondaryAction = SwipeToRevealDefaults.secondaryAction(
            icon = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") },
            onClick = { /* Add the click handler here */ }
        ),
        undoPrimaryAction = SwipeToRevealDefaults.undoAction(
            label = { Text("Undo") },
            onClick = { /* Add the undo handler for primary action */ }
        ),
        undoSecondaryAction = SwipeToRevealDefaults.undoAction(
            label = { Text("Undo") },
            onClick = { /* Add the undo handler for secondary action */ }
        )
    ) {
        Chip(
            onClick = { /* Add the chip click handler here */ },
            colors = ChipDefaults.primaryChipColors(),
            border = ChipDefaults.outlinedChipBorder()
        ) {
            Text("SwipeToReveal Chip")
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
@Sampled
fun SwipeToRevealCardSample() {
    SwipeToRevealCard(
        revealState = rememberRevealState(),
        modifier = Modifier.fillMaxWidth(),
        primaryAction = SwipeToRevealDefaults.primaryAction(
            icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
            label = { Text("Delete") },
            onClick = { /* Add the click handler here */ }
        ),
        secondaryAction = SwipeToRevealDefaults.secondaryAction(
            icon = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") },
            onClick = { /* Add the click handler here */ }
        ),
        undoPrimaryAction = SwipeToRevealDefaults.undoAction(
            label = { Text("Undo") },
            onClick = { /* Add the undo handler for primary action */ }
        ),
        undoSecondaryAction = SwipeToRevealDefaults.undoAction(
            label = { Text("Undo") },
            onClick = { /* Add the undo handler for secondary action */ }
        )
    ) {
        AppCard(
            onClick = { /* Add the Card click handler */ },
            appName = { Text("AppName") },
            appImage = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                    contentDescription = "airplane",
                    modifier = Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
                )
            },
            title = { Text("AppCard") },
            time = { Text("now") }
        ) {
            Text("Basic card with SwipeToReveal actions")
        }
    }
}
