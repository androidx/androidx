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

package androidx.compose.remote.integration.view.demos.examples.components

import android.graphics.Color
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.ZIndexModifier

/** Atomic demo for the zIndex modifier. Demonstrates manual control over layer drawing order. */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierZIndex(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 400, "DemoModifierZIndex") {
            root {
                box(Modifier.fillMaxSize()) {
                    // Red is added first, but Blue has higher zIndex so it appears on top.
                    box(Modifier.size(200).background(Color.RED))
                    box(Modifier.size(150).margin(25).zIndex(1f).background(Color.BLUE))
                }
            }
        }
        .writer
}

// Helper for margin if not explicitly in DSL
@Suppress("RestrictedApiAndroidX") private fun RecordingModifier.margin(v: Int) = this.padding(v)

@Suppress("RestrictedApiAndroidX")
private fun RecordingModifier.zIndex(v: Float): RecordingModifier = then(ZIndexModifier(v))
