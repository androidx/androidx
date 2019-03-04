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

package androidx.ui.material.demos

import androidx.ui.core.Constraints
import androidx.ui.core.Dp
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.times
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BorderSide
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.ripple.BoundedRipple
import androidx.ui.material.surface.Card
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer

// TODO("Andrey: While composables work way better when used from the same module")
// TODO("Andrey: Let's keep demos in 'ui-material' as well")

@Composable
internal fun FillAll(padding: Dp, @Children children: () -> Unit) {
    <MeasureBox> constraints ->
        val paddingPx = padding.toIntPx()
        val measurables = collect(children)
        val itemConstraints = Constraints.tightConstraints(constraints.maxWidth - paddingPx * 2,
            constraints.maxHeight - paddingPx * 2)
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEach {
                it.measure(itemConstraints).place(paddingPx, paddingPx)
            }
        }
    </MeasureBox>
}

@Composable
class RippleDemo : Component() {

    override fun compose() {
        <FillAll padding=50.dp>
            val shape = RoundedRectangleBorder(
                side = BorderSide(Color(0x80000000.toInt())),
                borderRadius = BorderRadius.circular(100f)
            )
            <Card shape>
                <BoundedRipple>
                    // here we will have a clickable element
                </BoundedRipple>
            </Card>
        </FillAll>
    }
}
