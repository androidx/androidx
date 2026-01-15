/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.animation.demos.sharedelement

import androidx.compose.animation.ExperimentalLookaheadAnimationVisualDebugApi
import androidx.compose.animation.LookaheadAnimationVisualDebugging
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Suppress("DisallowLookaheadAnimationVisualDebug")
@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
@Composable
internal fun LookaheadAnimationVisualDebuggingToggle(content: @Composable () -> Unit) {
    var visualDebuggingEnabled by remember { mutableStateOf(true) }

    Column {
        Button(
            onClick = { visualDebuggingEnabled = !visualDebuggingEnabled },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(Color(0xfffbbc04)),
        ) {
            Text(if (visualDebuggingEnabled) "Disable Visual Debug" else "Enable Visual Debug")
        }

        if (visualDebuggingEnabled) {
            LookaheadAnimationVisualDebugging(content = content)
        } else {
            content()
        }
    }
}
