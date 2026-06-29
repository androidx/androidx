/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalLookaheadAnimationVisualDebugApi
import androidx.compose.animation.LookaheadAnimationVisualDebugging
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(widthDp = 300, heightDp = 300)
@Composable
@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
fun PreviewWithDebuggingEnabled() {
    LookaheadAnimationVisualDebugging(isEnabled = true, isShowKeyLabelEnabled = true) {
        PreviewWithSharedElement()
    }
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
fun PreviewWithSharedElement() {
    var target by remember { mutableStateOf(false) }
    SharedTransitionLayout {
        Column {
            Button(onClick = { target = !target }) { Text("Toggle State") }
            AnimatedContent(targetState = target) {
                if (it) {
                    Box(
                        Modifier.sharedBounds(
                                rememberSharedContentState("box"),
                                this@AnimatedContent,
                            )
                            .size(100.dp)
                            .background(Color.Yellow)
                    )
                } else {
                    Box(
                        Modifier.sharedBounds(
                                rememberSharedContentState("box"),
                                this@AnimatedContent,
                            )
                            .size(130.dp)
                            .background(Color.Green)
                    )
                }
            }
        }
    }
}
