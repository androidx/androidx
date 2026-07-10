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

package androidx.compose.remote.integration.view.demos.examples.components

import android.graphics.Color
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter

/**
 * Atomic demo for StateLayout showing a row with fillMaxWidth containing a square red box
 * positioned on the left (RowLayout.START) in State 0 and at the end of the row (RowLayout.END) in
 * State 1.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoStateLayoutRow(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 200, "DemoStateLayoutRow") {
            root {
                // Toggles state index (0 vs 1) every 2 seconds
                val state = (ContinuousSec() % 2f)

                stateLayout(Modifier.fillMaxSize(), Utils.idFromNan(state.toFloat())) {
                    // State 0: Row on left
                    row(
                        Modifier.fillMaxWidth().height(100f).background(Color.LTGRAY),
                        horizontal = RowLayout.START,
                    ) {
                        box(Modifier.size(50).animationSpec(100).background(Color.RED))
                    }

                    // State 1: Row at end
                    row(
                        Modifier.fillMaxWidth().height(100f).background(Color.LTGRAY),
                        horizontal = RowLayout.END,
                    ) {
                        box(Modifier.size(50).animationSpec(100).background(Color.RED))
                    }
                }
            }
        }
        .writer
}
