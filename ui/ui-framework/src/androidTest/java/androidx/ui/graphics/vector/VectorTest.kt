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

import android.graphics.Bitmap
import androidx.compose.Composable
import androidx.compose.composer
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Px
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.setContent
import androidx.ui.core.test.AtLeastSize
import androidx.ui.core.test.runOnUiThreadIR
import androidx.ui.core.test.waitAndScreenShot
import androidx.ui.core.toPx
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.toArgb
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class VectorTest {

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @Test
    fun testVectorTint() {
        rule.runOnUiThreadIR {
            activity.setContent {
                VectorTint()
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
        }
    }

    @Composable
    private fun VectorTint() {
        val size = 200.ipx
        val sizePx = size.toPx()
        AtLeastSize(size = size) {
            DrawVector(
                defaultWidth = sizePx,
                defaultHeight = sizePx,
                tintColor = Color.Cyan) { _, _ ->
                Path(
                    pathData = PathData {
                        lineTo(sizePx.value, 0.0f)
                        lineTo(sizePx.value, sizePx.value)
                        lineTo(0.0f, sizePx.value)
                        close()
                    },
                    fill = SolidColor(Color.Black)
                )

                drawLatch.countDown()
            }
        }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }
}