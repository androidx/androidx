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

package androidx.compose.animation.benchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SeekableTransitionInterrupt : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SeekableTransitionInterruptTest() }
    }
}

@Composable
fun SeekableTransitionInterruptTest() {
    var isToggled by remember { mutableStateOf(false) }

    val seekableState = remember { SeekableTransitionState(isToggled) }
    val scope = rememberCoroutineScope()

    val transition = rememberTransition(seekableState, label = "DemoTransition")

    val size by transition.animateDp(label = "Size") { state -> if (state) 200.dp else 100.dp }
    val color by
        transition.animateColor(label = "Color") { state -> if (state) Color.Red else Color.Blue }

    Column(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(size).background(color))

        Button(
            modifier = Modifier.testTag("seekable_transition_interrupt_button"),
            onClick = {
                isToggled = !isToggled

                scope.launch {
                    seekableState.snapTo(!isToggled)

                    val animationJob = launch {
                        seekableState.animateTo(
                            targetState = isToggled,
                            animationSpec = tween(2000),
                        )
                    }

                    delay(500)

                    seekableState.seekTo(fraction = 0.3f, targetState = isToggled)

                    animationJob.cancel()
                }
            },
        ) {
            Text("interrupt animation w/ seekTo")
        }
    }
}
