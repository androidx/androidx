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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotEquals
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DropShadowTest {

    @get:Rule val rule = createComposeRule()

    private val DropShadowItemTag = "dropShadowItemTag"

    private val wrapperModifier = Modifier.testTag(DropShadowItemTag)

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawn() {
        rule.setContent { ShadowContainer(wrapperModifier) }

        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowRedrawnWhenValueChanges() {
        var radiusPixels by mutableStateOf(14)
        rule.setContent {
            with(LocalDensity.current) {
                Box(modifier = wrapperModifier.size(14.toDp()).background(Color.White)) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(10.toDp())
                            .dropShadow(RectangleShape) { radius = radiusPixels.toFloat() }
                            .background(Color.Red)
                    )
                }
            }
        }

        takeScreenShot().apply { hasShadow() }
        rule.runOnUiThread { radiusPixels = 0 }
        takeScreenShot().apply { hasNoShadow() }
        rule.runOnUiThread { radiusPixels = 14 }
        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawnInsideRenderNode() {
        rule.setContent { ShadowContainer(modifier = wrapperModifier.graphicsLayer()) }

        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromNoShadowToShadowWithNestedRepaintBoundaries() {
        val radius = mutableStateOf(0.dp)

        rule.setContent {
            ShadowContainer(modifier = wrapperModifier.graphicsLayer(clip = true), radius)
        }

        rule.runOnUiThread { radius.value = 2.dp }

        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitShadowLater() {
        val model = mutableStateOf(false)

        rule.setContent {
            AtLeastSize(size = 14, modifier = wrapperModifier.background(Color.White)) {
                val shadow =
                    if (model.value) {
                        Modifier.dropShadow(RectangleShape, Shadow(2.dp))
                    } else {
                        Modifier
                    }
                AtLeastSize(size = 10, modifier = shadow) {}
            }
        }

        rule.runOnUiThread { model.value = true }

        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowPropertiesResetWhenScopeChanges() {
        var capturedRadius: Float? = null
        val redShadow: ShadowScope.() -> Unit = {
            color = Color.Red
            radius = 14f
            capturedRadius = radius
        }
        val bigRadiusShadow: ShadowScope.() -> Unit = {
            // This scope does not set the color, so it should fall back to the default (Black)
            // after being reset, not persist the previous Red color.
            radius = 28f
            capturedRadius = radius
        }
        var shadowBlock by mutableStateOf(redShadow)

        rule.setContent {
            with(LocalDensity.current) {
                Box(modifier = wrapperModifier.size(28.toDp()).background(Color.White)) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(10.toDp())
                            .dropShadow(RectangleShape, block = shadowBlock)
                            .background(Color.White)
                    )
                }
            }
        }

        takeScreenShot().run {
            val shadowColor = color(width / 2, height - 2)
            hasShadow()
            assertTrue("Shadow color should be red", shadowColor.red > shadowColor.green)
            assertTrue("Shadow color should be red", shadowColor.red > shadowColor.blue)
        }

        assertEquals("Drop shadow radius should be 14f", 14f, capturedRadius)

        rule.runOnUiThread { shadowBlock = bigRadiusShadow }

        takeScreenShot().apply {
            val shadowColor = color(width / 2, height - 2)
            hasShadow()
            // Assert that the color is now black (or a dark gray), not red.
            val red = shadowColor.red
            val green = shadowColor.green
            val blue = shadowColor.blue
            assertEquals("Shadow color should reset to default (black)", red, green)
            assertEquals("Shadow color should reset to default (black)", green, blue)
        }
        assertEquals("Drop shadow radius should be 28f", 28f, capturedRadius)
    }

    @Test
    fun testInspectorValue() {
        rule.runOnUiThread {
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

    private fun Modifier.background(color: Color): Modifier = drawBehind { drawRect(color) }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(): Bitmap =
        rule.onNodeWithTag(DropShadowItemTag).captureToImage().asAndroidBitmap()
}
