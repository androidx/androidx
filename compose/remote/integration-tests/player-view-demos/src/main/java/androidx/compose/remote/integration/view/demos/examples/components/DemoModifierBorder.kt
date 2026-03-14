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

/**
 * Atomic demo for the border modifier. Demonstrates static strokes with optional rounded corners.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierBorder(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 400, "DemoModifierBorder") {
            root {
                column(Modifier.padding(20).spacedBy(20f)) {
                    // Rectangular border // TODO Shape?
                    box(Modifier.size(100).border(4f, 0.1f, Color.RED, 2))
                    // Rounded border
                    box(Modifier.size(100).border(4f, 0.1f, Color.BLUE, 2))
                }
            }
        }
        .writer
}
