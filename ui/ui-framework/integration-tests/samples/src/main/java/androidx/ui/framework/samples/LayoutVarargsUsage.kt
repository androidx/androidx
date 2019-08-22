/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.framework.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.ipx

@Sampled
@Composable
fun LayoutVarargsUsage(header: @Composable() () -> Unit, footer: @Composable() () -> Unit) {
    Layout(header, footer) { children, constraints ->
        val headerMeasurables = children[header]
        val footerMeasurables = children[footer]

        val headerPlaceables = headerMeasurables.map { child ->
            // You should use appropriate constraints.
            // This is shortened for the sake of a short example.
            child.measure(Constraints.tightConstraints(100.ipx, 100.ipx))
        }
        val footerPlaceables = footerMeasurables.map { child ->
            child.measure(constraints)
        }
        // Size should be derived from headerMeasurables and footerMeasurables measured
        // sizes, but this is shortened for the purposes of the example.
        layout(100.ipx, 100.ipx) {
            headerPlaceables.forEach { it.place(0.ipx, 0.ipx) }
            footerPlaceables.forEach { it.place(0.ipx, 0.ipx) }
        }
    }
}
