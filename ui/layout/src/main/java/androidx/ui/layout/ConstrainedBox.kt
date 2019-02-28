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
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.enforce
import androidx.ui.core.toRoundedPixels
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Widget that enforces additional [Constraints] to its only child.
 *
 * Example usage:
 *     <ConstrainedBox additionalConstraints=tightConstraints(50.dp, 50.dp)>
 *         <SizedRectangle color=Color(0xFFFF0000) />
 *     </ConstrainedBox>
 * Assuming that the ConstrainedBox is measured with unbounded constraints, here
 * the additional constraints can be used to make the rectangle have a specific size.
 */
@Composable
fun ConstrainedBox(
    additionalConstraints: Constraints,
    @Children children: @Composable() () -> Unit
) {
    <MeasureBox> constraints ->
        val measurable = collect(children).firstOrNull()
        val childConstraints = additionalConstraints.enforce(constraints)
        val placeable = if (measurable != null) {
            measurable.measure(childConstraints)
        } else {
            null
        }

        val layoutWidth = placeable?.width ?: constraints.minWidth.toRoundedPixels()
        val layoutHeight = placeable?.height ?: constraints.minHeight.toRoundedPixels()
        layout(layoutWidth, layoutHeight) {
            placeable?.place(0, 0)
        }
    </MeasureBox>
}
