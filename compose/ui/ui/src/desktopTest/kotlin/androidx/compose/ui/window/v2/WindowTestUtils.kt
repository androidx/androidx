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

package androidx.compose.ui.window.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import java.awt.Dimension
import java.awt.Window


abstract class EmptyMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(1, 1) {}
    }
}


@Composable
fun BoxWithIntrinsicSize(
    minWidth: (IntrinsicMeasureScope.(Int) -> Int)? = null,
    maxWidth: (IntrinsicMeasureScope.(Int) -> Int)? = null,
    minHeight: (IntrinsicMeasureScope.(Int) -> Int)? = null,
    maxHeight: (IntrinsicMeasureScope.(Int) -> Int)? = null,
) {
    Box {
        Layout(
            measurePolicy = object : EmptyMeasurePolicy() {
                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    return minWidth?.invoke(this, height) ?: 0
                }

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    return maxWidth?.invoke(this, height) ?: 0
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    return minHeight?.invoke(this, width) ?: 0
                }

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    return maxHeight?.invoke(this, width) ?: 0
                }
            },
            content = {}
        )
    }
}

val Window.contentSize
    get() = Dimension(
        size.width - insets.left - insets.right,
        size.height - insets.top - insets.bottom,
    )
