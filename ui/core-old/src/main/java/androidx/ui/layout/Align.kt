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

package androidx.ui.layout

import androidx.ui.core.Constraints
import androidx.ui.core.Dimension
import androidx.ui.core.MeasureBox
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import com.google.r4a.Children
import com.google.r4a.Composable

/**
 * A layout that takes a child and centers it within itself.
 * The layout will be as large as possible for finite incoming
 * constraints, or wrap content otherwise.
 *
 * Example usage:
 * <Center>
 *    <SizedRectangle color=Color(0xFF0000FF.toInt()) width = 40.dp height = 40.dp />
 * </Center>
 */
@Composable
fun Center(@Children children: () -> Unit) {
    <MeasureBox> constraints, measureOperations ->
        val measurable = measureOperations.collect(children).firstOrNull()
        if (measurable == null) {
            measureOperations.layout(0.dp, 0.dp) {}
        } else {
            // The child cannot be larger than our max constraints, but we ignore min constraints.
            val childConstraints = Constraints(
                0.dp, constraints.maxWidth, 0.dp, constraints.maxHeight
            )
            val placeable = measureOperations.measure(measurable, childConstraints)

            // The layout is as large as possible for bounded constraints,
            // or wrap content otherwise.
            val layoutWidth = if (constraints.maxWidth != Float.POSITIVE_INFINITY.dp) {
                constraints.maxWidth
            } else {
                placeable.width
            }
            val layoutHeight = if (constraints.maxHeight != Float.POSITIVE_INFINITY.dp) {
                constraints.maxHeight
            } else {
                placeable.height
            }

            measureOperations.layout(layoutWidth, layoutHeight) {
                placeable.place(
                    (layoutWidth - placeable.width) / 2,
                    (layoutHeight - placeable.height) / 2
                )
            }
        }
    </MeasureBox>
}
