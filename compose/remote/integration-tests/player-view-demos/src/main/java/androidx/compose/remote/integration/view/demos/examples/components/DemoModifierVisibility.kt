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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter

/**
 * Atomic demo for the visibility modifier. Demonstrates dynamic show/hide based on a remote
 * variable.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierVisibility(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 400, "DemoModifierVisibility") {
            root {
                // Blinking visibility (looping every 2 seconds)
                val isVisible = (ContinuousSec() % 2f)
                // TODO  val visibilityId = integerExpression(0, Component.Visibility.GONE.toLong(),
                // Component.Visibility.VISIBLE.toLong()).toInt()

                box(
                    Modifier.size(200)
                        .background(Color.RED)
                        .visibility(Utils.idFromNan(isVisible.toFloat()))
                )
            }
        }
        .writer
}
