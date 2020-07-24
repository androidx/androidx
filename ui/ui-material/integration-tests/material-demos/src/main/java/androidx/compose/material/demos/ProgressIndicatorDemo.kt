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

package androidx.compose.material.demos

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator

@Composable
fun ProgressIndicatorDemo() {
    val state = remember { ProgressState() }

    onActive { state.start() }
    onDispose { state.stop() }

    ScrollableColumn {
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

    val handler = Handler(Looper.getMainLooper())
    val updateProgress: Runnable = object : Runnable {
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
