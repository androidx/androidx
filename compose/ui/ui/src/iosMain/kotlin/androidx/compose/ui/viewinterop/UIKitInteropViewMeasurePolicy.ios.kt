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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.asCGSize
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.useContents
import platform.UIKit.UILayoutPriorityFittingSizeLevel
import platform.UIKit.UIView

internal class UIKitInteropViewMeasurePolicy<T: UIView>(
    val interopView: T
): MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val measuredSize by lazy {
            val targetSize = DpSize(
                constraints.maxWidth.toDp(),
                constraints.maxHeight.toDp()
            ).asCGSize()

            interopView.systemLayoutSizeFittingSize(
                targetSize,
                withHorizontalFittingPriority = UILayoutPriorityFittingSizeLevel,
                verticalFittingPriority = UILayoutPriorityFittingSizeLevel
            )
        }

        val width = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            measuredSize.useContents { width.dp.roundToPx() }
                .coerceIn(constraints.minWidth, constraints.maxWidth)
        }

        val height = if (constraints.hasFixedHeight) {
            constraints.minHeight
        } else {
            measuredSize.useContents { height.dp.roundToPx() }
                .coerceIn(constraints.minHeight, constraints.maxHeight)
        }

        return layout(width, height) {
            // No-op, no children are expected
        }
    }
}