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

package androidx.r4a

import android.os.Handler
import android.os.Looper
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dimension
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.tightConstraints
import androidx.ui.core.times
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.Recompose


@Composable
fun GrayRect() {
    <MeasureBox> constraints, operations ->
        operations.collect {
            val paint = Paint()
            paint.color = Color(android.graphics.Color.GRAY)
            <Draw> canvas, parentSize ->
                canvas.drawRect(Rect(0f, 0f, parentSize.width, parentSize.height), paint)
            </Draw>
        }
        operations.layout(constraints.maxWidth, constraints.maxHeight) {
        }
    </MeasureBox>
}

@Composable
fun ListWithOffset(
    itemsCount: Int,
    offset: Dimension,
    @Children item: () -> Unit
) {
    <MeasureBox> constraints, measureOperations ->
        val measurables = measureOperations.collect {
            repeat(itemsCount) {
                <item />
            }
        }
        val itemHeight = (constraints.maxHeight - offset * (itemsCount - 1)) / itemsCount
        val itemConstraint = tightConstraints(constraints.maxWidth, itemHeight)
        measureOperations.layout(constraints.maxWidth, itemHeight) {
            var top = 0.dp
            measurables.map { it.measure(itemConstraint) }.forEach {
                it.place(0.dp, top)
                top += itemHeight + offset
            }
        }
    </MeasureBox>
}

class RecomposeTest : Component() {

    var needsScheduling = true
    var offset = 10.dp
    var itemsCount = 1
    var addsItem = true

    override fun compose() {
        <CraneWrapper>
            <Recompose> recompose ->
                if (needsScheduling) {
                    needsScheduling = false
                    val handler = Handler(Looper.getMainLooper())
                    val r = object : Runnable {
                        override fun run() {
                            if (addsItem) {
                                itemsCount++
                            } else {
                                offset += 10.dp
                            }
                            addsItem = !addsItem
                            needsScheduling = true
                            recompose()
                        }
                    }
                    handler.postDelayed(r, 500)
                }
                <ListWithOffset itemsCount offset>
                    <GrayRect />
                </ListWithOffset>
            </Recompose>
        </CraneWrapper>
    }
}