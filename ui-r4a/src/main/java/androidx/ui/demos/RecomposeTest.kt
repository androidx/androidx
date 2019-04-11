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

package androidx.ui.demos

import android.os.Handler
import android.os.Looper
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.toRect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer

@Composable
fun GrayRect() {
    <Layout layoutBlock = { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }>
        val paint = Paint()
        paint.color = Color(android.graphics.Color.GRAY)
        <Draw> canvas, parentSize ->
            canvas.drawRect(parentSize.toRect(), paint)
        </Draw>
    </Layout>
}

@Composable
fun ListWithOffset(
    itemsCount: Int,
    offset: Dp,
    @Children item: () -> Unit
) {
    <Layout layoutBlock = { measurables, constraints ->
        val offsetPx = offset.toIntPx()
        val itemHeight = (constraints.maxHeight - offsetPx * (itemsCount - 1)) / itemsCount
        val itemConstraint = Constraints.tightConstraints(constraints.maxWidth, itemHeight)
        layout(constraints.maxWidth, constraints.maxHeight) {
            var top = 0.ipx
            measurables.map { it.measure(itemConstraint) }.forEach {
                it.place(0.ipx, top)
                top += itemHeight + offsetPx
            }
        }
    }>
        repeat(itemsCount) {
            <item />
        }
    </Layout>
}

class RecomposeTest : Component() {

    var needsScheduling = true
    var offset = 10.dp
    var itemsCount = 1
    var step = 0

    override fun compose() {
        <CraneWrapper>
            <Recompose> recompose ->
                if (needsScheduling) {
                    needsScheduling = false
                    val handler = Handler(Looper.getMainLooper())
                    val r = object : Runnable {
                        override fun run() {
                            if (step.rem(3) == 0) {
                                itemsCount++
                            } else if (step.rem(3) == 1) {
                                offset += 10.dp
                            } else {
                                itemsCount--
                            }
                            step++
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