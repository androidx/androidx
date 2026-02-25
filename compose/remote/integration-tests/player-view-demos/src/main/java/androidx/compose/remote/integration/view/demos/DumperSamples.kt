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

package androidx.compose.remote.integration.view.demos

import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.integration.view.demos.examples.*
import androidx.compose.runtime.Composable

@Suppress("RestrictedApiAndroidX")
sealed class DumperSample(val name: String) {
    class ComposableSample(name: String, val content: @Composable @RemoteComposable () -> Unit) :
        DumperSample(name)

    class Context(name: String, val getContext: () -> RemoteComposeContext) : DumperSample(name)
}

@Suppress("RestrictedApiAndroidX")
val AllSamples =
    listOf(
        DumperSample.ComposableSample("Clock", @Composable @RemoteComposable { RcSimpleClock1() }),
        DumperSample.Context("Cube") { cube3d() },
        DumperSample.Context("Timer") { RemoteComposeContext(basicTimer()) },
        DumperSample.Context("PressureGauge") { RemoteComposeContext(demoPressureGauge()) },
        DumperSample.Context("Countdown") { countDown() },
        DumperSample.Context("ParticleShader") { shaderFireworks() },
    )
