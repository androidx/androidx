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

package androidx.compose.remote.integration.view.demos.customCompose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.custom.ComposeCustomSupport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@SuppressLint("RestrictedApiAndroidX")
public object SupportSlider {
    public const val PROP_PROGRESS: Short = 5
    public const val PROP_MAX_PROGRESS: Short = 6
    public const val PROP_INDETERMINATE: Short = 7
    public const val PROP_PROGRESS_COLOR: Short = 8
    public const val RET_PROGRESS: Short = 9

    @Composable
    @SuppressLint("RestrictedApiAndroidX")
    public fun Content(state: ComposeCustomSupport.ComponentState, remoteContext: RemoteContext?) {
        var opProgress =
            state.floatProps[PROP_PROGRESS.toInt()]
                ?: (state.intProps[PROP_PROGRESS.toInt()]?.toFloat() ?: 0f)
        val maxProgress =
            state.floatProps[PROP_MAX_PROGRESS.toInt()]
                ?: (state.intProps[PROP_MAX_PROGRESS.toInt()]?.toFloat() ?: 100f)
        val sliderColor =
            state.intProps[PROP_PROGRESS_COLOR.toInt()]?.let { Color(it) } ?: Color.Unspecified
        val retId = state.intProps[RET_PROGRESS.toInt()] ?: -1

        if (retId != -1 && remoteContext != null) {
            val loaded = remoteContext.getFloat(retId)
            if (!loaded.isNaN()) {
                opProgress = loaded
            }
        }
        if (opProgress.isNaN()) {
            opProgress = 0f
        }

        var currentProgress by remember(opProgress) { mutableFloatStateOf(opProgress) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val valueRange = if (maxProgress > 0f) 0f..maxProgress else 0f..100f
            val safeProgress = currentProgress.coerceIn(valueRange)
            Slider(
                value = safeProgress,
                onValueChange = { newVal ->
                    currentProgress = newVal
                    if (retId != -1) {
                        println("Return ..... RET_PROGRESS + $retId + $newVal")
                        remoteContext?.loadFloat(retId, newVal)
                    }
                },
                valueRange = valueRange,
                colors =
                    if (sliderColor != Color.Unspecified) {
                        SliderDefaults.colors(
                            thumbColor = sliderColor,
                            activeTrackColor = sliderColor,
                        )
                    } else {
                        SliderDefaults.colors()
                    },
            )
        }
    }
}
