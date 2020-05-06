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

package androidx.ui.graphics.vector

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.drawBehind
import androidx.ui.core.paint
import androidx.ui.core.test.AtLeastSize
import androidx.ui.core.test.R
import androidx.ui.graphics.Color
import androidx.ui.res.loadVectorResource
import java.util.concurrent.CountDownLatch

class VectorInvalidationTestCase(var latch: CountDownLatch) {

    // Lazily initialize state as it needs to be constructed in the composition
    private var vectorState: MutableState<Int>? = null

    /**
     * Queries the size of the underlying vector image to draw
     * This assumes both width and height are the same
     */
    var vectorSize: Int = 0

    @Composable
    fun createTestVector() {
        val state = state { R.drawable.ic_triangle2 }
        vectorState = state

        val vectorAsset = loadVectorResource(state.value)
        with(DensityAmbient.current) {
            vectorAsset.resource.resource?.let {
                val width = it.defaultWidth
                vectorSize = width.toIntPx().value
                AtLeastSize(
                    size = width.toIntPx(),
                    modifier = WhiteBackground.paint(VectorPainter(it))) {
                    latch.countDown()
                }
            }
        }
    }

    val WhiteBackground = Modifier.drawBehind {
        drawRect(Color.White)
    }

    fun toggle() {
        val state = vectorState
        if (state != null) {
            state.value = if (state.value == R.drawable.ic_triangle) {
                R.drawable.ic_triangle2
            } else {
                R.drawable.ic_triangle
            }
        }
    }
}