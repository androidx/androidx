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
class InnerShadowTest {

    @get:Rule val rule = createComposeRule()

    private val InnerShadowItemTag = "innerShadowItemTag"

    private val wrapperModifier = Modifier.testTag(InnerShadowItemTag)

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
        var radiusDp by mutableStateOf(2.dp)
        rule.setContent {
            with(LocalDensity.current) {
                Box(modifier = wrapperModifier.size(12.toDp()).background(Color.White)) {
                    Box(
                        Modifier.align(Alignment.Center).size(8.toDp()).innerShadow(
                            RectangleShape
                        ) {
                            radius = radiusDp.toPx()
                        }
                    )
                }
            }
        }

        takeScreenShot().apply { hasShadow() }
        rule.runOnUiThread { radiusDp = 0.dp }
        takeScreenShot().apply { hasNoShadow() }
        rule.runOnUiThread { radiusDp = 2.dp }
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
            AtLeastSize(size = 12, modifier = wrapperModifier.background(Color.White)) {
                val shadow =
                    if (model.value) {
                        Modifier.innerShadow(RectangleShape, Shadow(2.dp))
                    } else {
                        Modifier
                    }
                AtLeastSize(size = 8, modifier = shadow) {}
            }
        }

        rule.runOnUiThread { model.value = true }

        takeScreenShot().apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun innerShadowPropertiesResetWhenScopeChanges() {
        var capturedRadius: Float? = null
        val redInnerShadow: InnerShadowScope.() -> Unit = {
            color = Color.Red
            radius = 2f
            capturedRadius = radius
        }
        val bigRadiusInnerShadow: InnerShadowScope.() -> Unit = {
            // This scope does not set the color, so it should use the default (Black).
            radius = 4f
            capturedRadius = radius
        }
        var shadowBlock by mutableStateOf(redInnerShadow)

        rule.setContent {
            with(LocalDensity.current) {
                Box(modifier = wrapperModifier.size(12.toDp()).background(Color.White)) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(8.toDp())
                            .innerShadow(RectangleShape, block = shadowBlock)
                    )
                }
            }
        }

        takeScreenShot().apply {
            hasShadow()
            val shadowColor = color(width / 2, 3)
            assertTrue("Inner shadow color should be red", shadowColor.red > shadowColor.green)
            assertTrue("Inner shadow color should be red", shadowColor.red > shadowColor.blue)
        }

        assertEquals("Inner shadow radius should be 2f", 2f, capturedRadius)
        rule.runOnUiThread { shadowBlock = bigRadiusInnerShadow }

        takeScreenShot().apply {
            val shadowColor = color(width / 2, 3)
            hasShadow()
            // Assert that the color is now black (or a dark gray), not red.
            val red = shadowColor.red
            val green = shadowColor.green
            val blue = shadowColor.blue
            assertEquals("Inner shadow color should reset to default (black)", red, green)
            assertEquals("Inner shadow color should reset to default (black)", green, blue)
        }
        assertEquals("Inner shadow radius should be 4f", 4f, capturedRadius)
    }

    @Test
    fun testInspectorValue() {
        rule.runOnUiThread {
            val modifier =
                Modifier.innerShadow(RectangleShape, Shadow(8.dp)).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("innerShadow")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable())
                .containsExactly(
                    ValueElement("shape", RectangleShape),
                    ValueElement("innerShadow", Shadow(8.dp)),
                )
        }
    }

    @Composable
    private fun ShadowContainer(
        modifier: Modifier = Modifier,
        radius: State<Dp> = mutableStateOf(2.dp),
    ) {
        AtLeastSize(size = 12, modifier = modifier.background(Color.White)) {
            AtLeastSize(
                size = 8,
                modifier = Modifier.innerShadow(RectangleShape, Shadow(radius.value)),
            ) {}
        }
    }

    private fun Bitmap.hasShadow() {
        // Shadow drawn inside the 8 x 8 box centered in the 12 x 12 container
        assertNotEquals(Color.White, color(width / 2, 3))
        // No shadow drawn outside the 8 x 8 box
        assertEquals(Color.White, color(width / 2, 0))
    }

    private fun Bitmap.hasNoShadow() {
        // No shadow drawn inside the 8 x 8 box centered in the 12 x 12 container
        assertEquals(Color.White, color(width / 2, 3))
        // No shadow drawn outside the 8 x 8 box
        assertEquals(Color.White, color(width / 2, 0))
    }

    private fun Modifier.background(color: Color): Modifier = drawBehind { drawRect(color) }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(): Bitmap =
        rule.onNodeWithTag(InnerShadowItemTag).captureToImage().asAndroidBitmap()
}
