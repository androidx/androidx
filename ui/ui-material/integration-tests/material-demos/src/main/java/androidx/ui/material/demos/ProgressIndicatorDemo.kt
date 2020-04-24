/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.demos

import android.os.Handler
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onActive
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxWidth
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.LinearProgressIndicator

@Composable
fun ProgressIndicatorDemo() {
    val state = remember { ProgressState() }

    onActive { state.start() }
    onDispose { state.stop() }

    Column {
        val modifier = Modifier.weight(1f, true)
            .gravity(Alignment.CenterHorizontally)
            .fillMaxWidth()
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            // Determinate indicators
            LinearProgressIndicator(state.progress, Modifier.gravity(Alignment.CenterVertically))
            CircularProgressIndicator(state.progress, Modifier.gravity(Alignment.CenterVertically))
        }
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            // Fancy colours!
            LinearProgressIndicator(
                progress = (state.progress),
                color = state.color,
                modifier = Modifier.gravity(Alignment.CenterVertically)
            )
            CircularProgressIndicator(
                progress = (state.progress),
                color = state.color,
                modifier = Modifier.gravity(Alignment.CenterVertically)
            )
        }
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            // Indeterminate indicators
            LinearProgressIndicator(Modifier.gravity(Alignment.CenterVertically))
            CircularProgressIndicator(Modifier.gravity(Alignment.CenterVertically))
        }
    }
}

@Stable
private class ProgressState {
    var progress by mutableStateOf(0f)
    var color by mutableStateOf(Color.Red)

    fun start() {
        handler.postDelayed(updateProgress, 400)
    }

    fun stop() {
        handler.removeCallbacks(updateProgress)
    }

    private val handler = Handler()
    private val updateProgress: Runnable = object : Runnable {
        override fun run() {
            if (progress == 1f) {
                color = when (color) {
                    Color.Red -> Color.Green
                    Color.Green -> Color.Blue
                    else -> Color.Red
                }
                progress = 0f
            } else {
                progress += 0.25f
            }
            handler.postDelayed(this, 400)
        }
    }
}
