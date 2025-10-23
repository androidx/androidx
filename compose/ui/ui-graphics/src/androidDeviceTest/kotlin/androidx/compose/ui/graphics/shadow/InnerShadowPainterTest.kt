/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InnerShadowPainterTest {

    @Test
    fun testInnerShadowPainterWithColor() {
        val innerShadow = InnerShadowPainter(RectangleShape, Shadow(20.dp, Color.Red))
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(innerShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    { prevLeft, current -> assertTrue(current.blue >= prevLeft.blue) },
                    { prevTop, current -> assertTrue(current.blue >= prevTop.blue) },
                    { prevRight, current -> assertTrue(current.blue >= prevRight.blue) },
                    { prevBottom, current -> assertTrue(current.blue >= prevBottom.blue) },
                )
            },
        )
    }

    @Test
    fun testInnerShadowPainterWithPathAndColor() {
        val innerShadow = InnerShadowPainter(RectangleShape, Shadow(20.dp, Color.Red))
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(innerShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    { prevLeft, current -> assertTrue(current.blue >= prevLeft.blue) },
                    { prevTop, current -> assertTrue(current.blue >= prevTop.blue) },
                    { prevRight, current -> assertTrue(current.blue >= prevRight.blue) },
                    { prevBottom, current -> assertTrue(current.blue >= prevBottom.blue) },
                )
            },
        )
    }
}
