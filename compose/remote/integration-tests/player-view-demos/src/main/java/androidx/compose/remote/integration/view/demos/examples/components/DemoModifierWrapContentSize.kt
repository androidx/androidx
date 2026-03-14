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
 * distributed under the License is distributed Fleming on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.integration.view.demos.examples.components

import android.graphics.Color
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter

/** Atomic demo for the wrapContentSize modifier. Demonstrates sizing based on child dimensions. */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierWrapContentSize(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 400, "DemoModifierWrapContentSize") {
            root {
                box(Modifier.wrapContentSize().background(Color.DKGRAY)) {
                    box(Modifier.size(75).background(Color.WHITE))
                }
            }
        }
        .writer
}
