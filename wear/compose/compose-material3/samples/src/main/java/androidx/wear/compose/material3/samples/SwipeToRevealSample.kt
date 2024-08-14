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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberRevealState

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
@Sampled
fun SwipeToRevealSample() {
    SwipeToReveal(
        // Use the double action anchor width when revealing two actions
        revealState =
            rememberRevealState(anchorWidth = SwipeToRevealDefaults.DoubleActionAnchorWidth),
        actions = {
            primaryAction(
                onClick = { /* This block is called when the primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                label = "Delete"
            )
            secondaryAction(
                onClick = { /* This block is called when the secondary action is executed. */ },
                icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "Options") },
                label = "Options"
            )
            undoPrimaryAction(
                onClick = { /* This block is called when the undo primary action is executed. */ },
                label = "Undo Delete"
            )
        }
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
            Text("This Button has two actions", modifier = Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
@Sampled
fun SwipeToRevealSingleActionCardSample() {
    SwipeToReveal(
        actionButtonHeight = SwipeToRevealDefaults.LargeActionButtonHeight,
        actions = {
            primaryAction(
                onClick = { /* This block is called when the primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                label = "Delete"
            )
        }
    ) {
        Card(modifier = Modifier.fillMaxWidth(), onClick = {}) {
            Text(
                "This Card has one action, and the revealed button is taller",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
@Sampled
fun SwipeToRevealNonAnchoredSample() {
    SwipeToReveal(
        revealState = rememberRevealState(useAnchoredActions = false),
        actions = {
            primaryAction(
                onClick = { /* This block is called when the primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                label = "Delete"
            )
            undoPrimaryAction(
                onClick = { /* This block is called when the undo primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Refresh, contentDescription = "Undo") },
                label = "Undo"
            )
        }
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
            Text("Swipe to execute the primary action.", modifier = Modifier.fillMaxSize())
        }
    }
}
