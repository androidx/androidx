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
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.InteractionState
import androidx.ui.foundation.Text
import androidx.ui.foundation.indication
import androidx.ui.layout.padding
import androidx.ui.unit.dp

@Composable
@Sampled
fun IndicationSample() {
    val interactionState = remember { InteractionState() }
    Clickable(
        onClick = { /** do something */ },
        // clickable will update interaction state
        interactionState = interactionState,
        // Modifier.indication will consume interaction state and show visual indication
        modifier = Modifier.indication(interactionState)
    ) {
        Text("Clickable text", Modifier.padding(16.dp))
    }
}