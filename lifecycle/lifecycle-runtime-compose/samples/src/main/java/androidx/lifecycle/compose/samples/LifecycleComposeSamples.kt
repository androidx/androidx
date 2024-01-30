/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.lifecycle.compose.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

@Sampled
@Composable
fun StateFlowCollectAsStateWithLifecycle() {
    class ExampleState {
        private val _uiState = MutableStateFlow("")
        val uiState: StateFlow<String> = _uiState.asStateFlow()
    }

    val state = remember { ExampleState() }

    val uiState by state.uiState.collectAsStateWithLifecycle()
    Text(text = uiState)
}

@Sampled
@Composable
fun FlowCollectAsStateWithLifecycle() {
    class ExampleState {
        val counter = flow {
            var count = 0
            while (true) {
                emit(count++)
                delay(1000)
            }
        }
    }

    val state = remember { ExampleState() }
    val count by state.counter.collectAsStateWithLifecycle(initialValue = 0)
    Text(text = "$count")
}

private interface TimeAnalytics {
    fun stopTimeTracking(): TimeAnalytics
}

private interface DataAnalytics {
    fun trackScreenView(screenName: String)
    fun startTimeTracking(): TimeAnalytics
    fun sendDisposalAnalytics(timeAnalytics: TimeAnalytics)
}

@Sampled
@Composable
fun lifecycleEventEffectSample() {
    @Composable
    fun Analytics(dataAnalytics: DataAnalytics) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            dataAnalytics.trackScreenView("screen1")
        }

        // ...
    }
}

@Sampled
@Composable
fun lifecycleStartEffectSample() {
    @Composable
    fun Analytics(dataAnalytics: DataAnalytics) {
        LifecycleStartEffect {
            val timeTracker = dataAnalytics.startTimeTracking()

            onStopOrDispose {
                timeTracker.stopTimeTracking()
            }
        }

        // ...
    }
}

@Sampled
@Composable
fun lifecycleResumeEffectSample() {
    @Composable
    fun Analytics(dataAnalytics: DataAnalytics) {
        LifecycleResumeEffect {
            val timeTracker = dataAnalytics.startTimeTracking()

            onPauseOrDispose {
                timeTracker.stopTimeTracking()
            }
        }

        // ...
    }
}
