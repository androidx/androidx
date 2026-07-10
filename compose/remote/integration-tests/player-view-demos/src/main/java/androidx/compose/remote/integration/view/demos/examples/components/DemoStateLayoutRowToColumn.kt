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
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter

/**
 * Demo for StateLayout showing 3 boxes in a RowLayout (fillMaxWidth) in State 0 and transitioning
 * to a ColumnLayout (fillMaxHeight) in State 1.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoStateLayoutRowToColumn(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 300, "DemoStateLayoutRowToColumn") {
            root {
                val state = (ContinuousSec() % 2f)

                stateLayout(Modifier.fillMaxSize(), Utils.idFromNan(state.toFloat())) {
                    // State 0: 3 boxes in a Row (fillMaxWidth)
                    row(
                        Modifier.fillMaxWidth().fillMaxHeight().background(Color.LTGRAY),
                        horizontal = RowLayout.SPACE_EVENLY,
                        vertical = RowLayout.CENTER,
                    ) {
                        box(Modifier.size(60).animationSpec(100).background(Color.RED))
                        box(Modifier.size(60).animationSpec(101).background(Color.GREEN))
                        box(Modifier.size(60).animationSpec(102).background(Color.BLUE))
                    }

                    // State 1: 3 boxes in a Column (fillMaxHeight)
                    column(
                        Modifier.fillMaxWidth().fillMaxHeight().background(Color.LTGRAY),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.SPACE_EVENLY,
                    ) {
                        box(Modifier.size(60).animationSpec(100).background(Color.RED))
                        box(Modifier.size(60).animationSpec(101).background(Color.GREEN))
                        box(Modifier.size(60).animationSpec(102).background(Color.BLUE))
                    }
                }
            }
        }
        .writer
}
