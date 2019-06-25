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

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import androidx.ui.core.CraneWrapper
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.MainAxisAlignment.Companion.SpaceEvenly
import androidx.ui.layout.Row
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.graphics.Color
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.onActive
import androidx.compose.onDispose
import androidx.compose.setContent
import androidx.compose.unaryPlus

class ProgressIndicatorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    ProgressIndicatorDemo()
                }
            }
        }
    }
}

@Model
private class ProgressState {
    var progress = 0f
    var cycle = 1

    fun generateColor(): Color {
        return when (cycle) {
            1 -> Color.Red
            2 -> Color.Green
            3 -> Color.Blue
            // unused
            else -> Color.Black
        }
    }

    fun start() {
        handler.postDelayed(updateProgress, 400)
    }

    fun stop() {
        handler.removeCallbacks(updateProgress)
    }

    var handler = Handler()
    var updateProgress: Runnable = object : Runnable {
        override fun run() {
            if (progress == 1f) {
                cycle++
                if (cycle > 3) {
                    cycle = 1
                }
                progress = 0f
            } else {
                progress += 0.25f
            }
            handler.postDelayed(this, 400)
        }
    }
}

@Composable
private fun ProgressIndicatorDemo(state: ProgressState = ProgressState()) {

    +onActive { state.start() }
    +onDispose { state.stop() }

    FlexColumn {
        expanded(flex = 1f) {
            Row(mainAxisAlignment = SpaceEvenly) {
                // Determinate indicators
                LinearProgressIndicator(progress = state.progress)
                CircularProgressIndicator(progress = state.progress)
            }
            Row(mainAxisAlignment = SpaceEvenly) {
                // Fancy colours!
                LinearProgressIndicator(progress = (state.progress), color = state.generateColor())
                CircularProgressIndicator(
                    progress = (state.progress),
                    color = state.generateColor()
                )
            }
            Row(mainAxisAlignment = SpaceEvenly) {
                // Indeterminate indicators
                LinearProgressIndicator()
                CircularProgressIndicator()
            }
        }
    }
}