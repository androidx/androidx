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

package androidx.compose.ui.draw

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DropShadowTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawn() {
        rule.runOnUiThreadIR { activity.setContent { ShadowContainer() } }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(14).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowRedrawnWhenValueChanges() {
        var radiusPixels by mutableStateOf(14)
        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(modifier = Modifier.size(14.toDp()).background(Color.White)) {
                        Box(
                            Modifier.align(Alignment.Center)
                                .size(10.toDp())
                                .dropShadow(RectangleShape) { radius = radiusPixels.toFloat() }
                                .background(Color.Red)
                                .drawBehind { drawLatch.countDown() }
                        )
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(14).apply { hasShadow() }
        drawLatch = CountDownLatch(1)
        radiusPixels = 0
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(14).apply { hasNoShadow() }
        drawLatch = CountDownLatch(1)
        radiusPixels = 14
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(14).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawnInsideRenderNode() {
        rule.runOnUiThreadIR {
            activity.setContent { ShadowContainer(modifier = Modifier.graphicsLayer()) }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(14).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromNoShadowToShadowWithNestedRepaintBoundaries() {
        val radius = mutableStateOf(0.dp)

        rule.runOnUiThreadIR {
            activity.setContent {
                ShadowContainer(modifier = Modifier.graphicsLayer(clip = true), radius)
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThreadIR { radius.value = 2.dp }

        takeScreenShot(14).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitShadowLater() {
        val model = mutableStateOf(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 14, modifier = Modifier.background(Color.White)) {
                    val shadow =
                        if (model.value) {
                            Modifier.dropShadow(RectangleShape, Shadow(2.dp))
                        } else {
                            Modifier
                        }
                    AtLeastSize(size = 10, modifier = shadow) {}
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.value = true }

        takeScreenShot(14).apply { hasShadow() }
    }

    @Test
    fun testInspectorValue() {
        rule.runOnUiThreadIR {
            val modifier =
                Modifier.dropShadow(RectangleShape, Shadow(8.dp)).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("dropShadow")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable())
                .containsExactly(
                    ValueElement("shape", RectangleShape),
                    ValueElement("dropShadow", Shadow(8.dp)),
                )
        }
    }

    @Composable
    private fun ShadowContainer(
        modifier: Modifier = Modifier,
        radius: State<Dp> = mutableStateOf(2.dp),
    ) {
        AtLeastSize(size = 14, modifier = modifier.background(Color.White)) {
            AtLeastSize(
                size = 10,
                modifier = Modifier.dropShadow(RectangleShape, Shadow(radius.value)),
            ) {}
        }
    }

    private fun Bitmap.hasShadow() {
        assertNotEquals(Color.White, color(width / 2, height - 2))
    }

    private fun Bitmap.hasNoShadow() {
        assertEquals(Color.White, color(width / 2, height - 2))
    }

    private fun Modifier.background(color: Color): Modifier = drawBehind {
        drawRect(color)
        drawLatch.countDown()
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }
}
