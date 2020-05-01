/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.draggable
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.dp

@Sampled
@Composable
fun InteractionStateSample() {
    val interactionState = remember { InteractionState() }

    val draggable = Modifier.draggable(
        dragDirection = DragDirection.Horizontal,
        interactionState = interactionState
    ) { /* update some business state here */ 0f }

    // Use InteractionState to determine how this component should appear during transient UI states
    val (text, color) = when {
        Interaction.Dragged in interactionState -> "Dragged" to Color.Red
        Interaction.Pressed in interactionState -> "Pressed" to Color.Blue
        // Default / baseline state
        else -> "Drag me horizontally, or press me!" to Color.Black
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
            .preferredSize(width = 240.dp, height = 80.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionState = interactionState) { /* do nothing */ }
                .plus(draggable),
            border = Border(3.dp, color)
        ) {
            Text(
                text, style = currentTextStyle().copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxSize().wrapContentSize()
            )
        }
    }
}