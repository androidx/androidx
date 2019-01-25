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

import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dimension
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.tightConstraints
import androidx.ui.core.times
import androidx.ui.engine.geometry.BorderRadius
import androidx.ui.material.InkRippleFactory
import androidx.ui.material.InkWell
import androidx.ui.material.Material
import androidx.ui.material.MaterialType
import androidx.ui.material.borders.BorderSide
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable


// TODO("Migration|Andrey: While composables work way better when used from the same module")
// TODO("Migration|Andrey: Let's keep demos in 'ui-material' as well")

@Composable
internal fun FillAll(padding: Dimension, @Children children: () -> Unit) {
    <MeasureBox> constraints, measureOperations ->
        val measurables = measureOperations.collect(children)
        val itemConstraints = tightConstraints(constraints.maxWidth - padding * 2,
            constraints.maxHeight - padding * 2)
        measureOperations.layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEach {
                measureOperations.measure(it, itemConstraints).place(padding, padding)
            }
        }
    </MeasureBox>
}

@Composable
class InksDemo : Component() {

    override fun compose() {
        <CraneWrapper>
            <FillAll padding=50.dp>
                val shape = RoundedRectangleBorder(
                    side = BorderSide(Color(0x80000000.toInt())),
                    borderRadius = BorderRadius.circular(100f)
                )
                <Material
                    type=MaterialType.CARD
                    color=Color(0x28CCCCCC)
                    shape>
                    <InkWell
                        splashFactory=InkRippleFactory
                        splashColor=Color(0x50CCCCCC)
                        highlightColor=Color(0x3C888888)
                        onTap={}>
                    </InkWell>
                </Material>
            </FillAll>
        </CraneWrapper>
    }
}


