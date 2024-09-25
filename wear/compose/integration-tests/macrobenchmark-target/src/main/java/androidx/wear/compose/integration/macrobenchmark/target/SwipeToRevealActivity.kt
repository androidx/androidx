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

package androidx.wear.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SwipeToRevealChip
import androidx.wear.compose.material.SwipeToRevealDefaults
import androidx.wear.compose.material.SwipeToRevealPrimaryAction
import androidx.wear.compose.material.SwipeToRevealSecondaryAction
import androidx.wear.compose.material.Text

@OptIn(ExperimentalWearMaterialApi::class)
class SwipeToRevealActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val revealState = rememberRevealState()
                SwipeToRevealChip(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            contentDescription = CONTENT_DESCRIPTION
                        },
                    revealState = revealState,
                    primaryAction = {
                        SwipeToRevealPrimaryAction(
                            revealState = revealState,
                            icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
                            label = { Text("Delete") },
                            onClick = {}
                        )
                    },
                    secondaryAction = {
                        SwipeToRevealSecondaryAction(revealState = revealState, onClick = {}) {
                            Icon(SwipeToRevealDefaults.MoreOptions, "More Options")
                        }
                    },
                    onFullSwipe = {}
                ) {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {},
                        colors = ChipDefaults.primaryChipColors(),
                        border = ChipDefaults.outlinedChipBorder(),
                        label = { Text("SwipeToReveal Chip", maxLines = 3) }
                    )
                }
            }
        }
    }
}
