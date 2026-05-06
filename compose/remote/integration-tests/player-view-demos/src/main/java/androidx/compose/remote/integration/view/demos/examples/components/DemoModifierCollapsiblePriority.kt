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
import androidx.compose.remote.creation.modifiers.CollapsiblePriorityModifier

/**
 * Atomic demo for the collapsiblePriority modifier. Demonstrates how items are hidden in a specific
 * order.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierCollapsiblePriority(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(400, 400, "DemoModifierCollapsiblePriority") {
            root { // TODO horizontal and vertical doc
                collapsibleRow(Modifier.fillMaxWidth().height(100).background(Color.LTGRAY), 1, 1) {
                    // Priority 1: Hides last // TODO move CollapsiblePriorityModifier.HORIZONTAL to
                    // a constant Rc ?
                    box(
                        Modifier.size(100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriorityModifier.HORIZONTAL, 1f)
                    )
                    // Priority 2: Hides first
                    box(
                        Modifier.size(100)
                            .background(Color.BLUE)
                            .collapsiblePriority(CollapsiblePriorityModifier.HORIZONTAL, 2f)
                    )
                }
            }
        }
        .writer
}
