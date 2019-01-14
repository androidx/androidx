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
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.dp
import androidx.ui.material.InkRippleFactory
import androidx.ui.material.InkTmpGestureHost
import androidx.ui.material.InkWell
import androidx.ui.material.Material
import androidx.ui.material.MaterialType
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.Recompose


// TODO("Migration|Andrey: While composables work way better when used from the same module")
// TODO("Migration|Andrey: Let's keep demos in 'ui-material' as well")

@Composable
internal fun FillAll(@Children children: () -> Unit) {
    <MeasureBox> constraints, measureOperations ->
        val measurables = measureOperations.collect(children)
        measureOperations.layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEach { it.measure(constraints).place(0.dp, 0.dp) }
        }
    </MeasureBox>
}

@Composable
class InksDemo : Component() {

    override fun compose() {
        <InkTmpGestureHost>
            <CraneWrapper>
                <Recompose> recompose ->
                    <FillAll>
                        <Material
                            type=MaterialType.BUTTON
                            color=Color(0xFF000000.toInt())
                            shadowColor=Color(0xFF000000.toInt())>
                            <InkWell
                                splashFactory=InkRippleFactory
                                splashColor=Color(android.graphics.Color.LTGRAY).withAlpha(125)
                                highlightColor=Color(android.graphics.Color.GRAY).withAlpha(125)
                                onTap={}>
                            </InkWell>
                        </Material>
                    </FillAll>
                </Recompose>
            </CraneWrapper>
        </InkTmpGestureHost>
    }
}


