/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.LayoutModifierImpl
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DrawModifierTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheHitWithStateChange() {
        // Verify that a state change outside of the cache block does not
        // require the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val size = 200
        rule.setContent {
            var rectColor by remember { mutableStateOf(Color.Blue) }
            AtLeastSize(
                size = size,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        val drawSize = this.size
                        val path = Path().apply {
                            lineTo(drawSize.width / 2f, 0f)
                            lineTo(drawSize.width / 2f, drawSize.height)
                            lineTo(0f, drawSize.height)
                            close()
                        }
                        cacheBuildCount++
                        onDrawBehind {
                            drawRect(rectColor)
                            drawPath(path, Color.Red)
                        }
                    }
                    .clickable {
                        if (rectColor == Color.Blue) {
                            rectColor = Color.Green
                        } else {
                            rectColor = Color.Blue
                        }
                    }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 - 2))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 - 2))

                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, height - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was re-used and only built once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 - 1))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 - 2))

                assertEquals(Color.Green.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Green.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Green.toArgb(), getPixel(width / 2 + 1, height - 2))
                assertEquals(Color.Green.toArgb(), getPixel(width - 2, height - 2))
            }
        }
    }

    @Test
    fun invalidationForDrawWithCache() {
        var size by mutableStateOf(10f)
        var drawLatch = CountDownLatch(1)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier
                    .graphicsLayer { }
                    .size(50.dp)
                    .drawWithCache {
                        val rectSize = Size(size, size)
                        onDrawBehind {
                            drawRect(Color.Blue, Offset.Zero, rectSize)
                            drawLatch.countDown()
                        }
                    }
                    .graphicsLayer { }
                )
            }
        }
        assertThat(drawLatch.await(2, TimeUnit.SECONDS)).isTrue()

        drawLatch = CountDownLatch(1)
        size = 15f
        assertThat(drawLatch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheInvalidatedAfterStateChange() {
        // Verify that a state change within the cache block does
        // require the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val size = 200

        rule.setContent {
            var pathFillBounds by remember { mutableStateOf(false) }
            AtLeastSize(
                size = size,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        val pathSize = if (pathFillBounds) this.size else this.size / 2f
                        val path = Path().apply {
                            lineTo(pathSize.width, 0f)
                            lineTo(pathSize.width, pathSize.height)
                            lineTo(0f, pathSize.height)
                            close()
                        }
                        cacheBuildCount++
                        onDrawBehind {
                            drawRect(Color.Red)
                            drawPath(path, Color.Blue)
                        }
                    }
                    .clickable {
                        pathFillBounds = !pathFillBounds
                    }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 - 2, height / 2 - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(1, height / 2 - 1))

                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, height / 2 - 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, height / 2 - 2))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 + 1))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 + 1))

                assertEquals(Color.Red.toArgb(), getPixel(1, height - 2))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            assertEquals(2, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(size - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(size - 2, size - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(1, size - 2))
            }
        }
    }

    @Test
    fun combinedModifiers_drawingSizesAreUsingTheSizeDefinedByLayoutModifier() {
        var drawingSize: Size = Size.Unspecified
        var drawingCacheSize: Size = Size.Unspecified
        val modifier = object : LayoutModifier, DrawCacheModifier {
            override fun onBuildCache(params: BuildDrawCacheParams) {
                drawingCacheSize = params.size
            }

            override fun ContentDrawScope.draw() {
                drawingSize = size
            }

            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                val placeable = measurable.measure(Constraints.fixed(10, 10))
                return layout(20, 20) {
                    placeable.place(0, 0)
                }
            }
        }
        rule.setContent {
            Box(modifier)
        }

        rule.runOnIdle {
            val expectedSize = Size(10f, 10f)
            assertThat(drawingSize).isEqualTo(expectedSize)
            assertThat(drawingCacheSize).isEqualTo(expectedSize)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheInvalidatedAfterSizeChange() {
        // Verify that a size change does cause the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val startSize = 200
        val endSize = 400
        rule.setContent {
            var size by remember { mutableStateOf(startSize) }
            AtLeastSize(
                size = size,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        val drawSize = this.size
                        val path = Path().apply {
                            lineTo(drawSize.width, 0f)
                            lineTo(drawSize.height, drawSize.height)
                            lineTo(0f, drawSize.height)
                            close()
                        }
                        cacheBuildCount++
                        onDrawBehind {
                            drawPath(path, Color.Red)
                        }
                    }
                    .clickable {
                        if (size == startSize) {
                            size = endSize
                        } else {
                            size = startSize
                        }
                    }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(startSize, this.width)
                assertEquals(startSize, this.height)
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was re-used and only built once
            assertEquals(2, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(endSize, this.width)
                assertEquals(endSize, this.height)
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
        }
    }

    @Test
    fun testCacheInvalidatedAfterLayoutDirectionChange() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var realLayoutDirection: LayoutDirection? = null
        var drawLatch = CountDownLatch(1)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.drawWithCache {
                        realLayoutDirection = layoutDirection
                        drawLatch.countDown()
                        onDrawBehind {}
                    }
                ) { }
            }
        }

        assertThat(drawLatch.await(2, TimeUnit.SECONDS)).isTrue()
        rule.runOnIdle {
            assertEquals(LayoutDirection.Ltr, realLayoutDirection)
            drawLatch = CountDownLatch(1)
            layoutDirection = LayoutDirection.Rtl
        }

        assertThat(drawLatch.await(2, TimeUnit.SECONDS)).isTrue()
        rule.runOnIdle {
            assertEquals(LayoutDirection.Rtl, realLayoutDirection)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheInvalidatedWithHelperModifier() {
        // If Modifier.drawWithCache is used as part of the implementation for another modifier
        // defined in a helper function, make sure that an change in state parameter ends up calling
        // ModifiedDrawNode.onModifierChanged and updates the internal cache for
        // Modifier.drawWithCache
        val testTag = "testTag"
        val startSize = 200
        rule.setContent {
            val color = remember { mutableStateOf(Color.Red) }
            AtLeastSize(
                size = startSize,
                modifier = Modifier
                    .testTag(testTag)
                    .drawPathHelperModifier(color.value)
                    .clickable {
                        if (color.value == Color.Red) {
                            color.value = Color.Blue
                        } else {
                            color.value = Color.Red
                        }
                    }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was re-used and only built once
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 2, height - 2))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testGraphicsLayerCacheInvalidatedAfterStateChange() {
        // Verify that a state change within the cache block does
        // require the cache block to be invalidated if a graphicsLayer is also
        // configured on the composable and the state parameter is configured elsewhere
        val boxTag = "boxTag"
        val clickTag = "clickTag"

        var cacheBuildCount = 0

        rule.setContent {
            val flag = remember { mutableStateOf(false) }
            Column {
                AtLeastSize(
                    size = 50,
                    modifier = Modifier
                        .testTag(boxTag)
                        .graphicsLayer()
                        .drawWithCache {
                            // State read of flag
                            val color = if (flag.value) Color.Red else Color.Blue
                            cacheBuildCount++

                            onDrawBehind {
                                drawRect(color)
                            }
                        }
                )

                Box(
                    Modifier
                        .testTag(clickTag)
                        .size(20.dp)
                        .clickable {
                            flag.value = !flag.value
                        }
                )
            }
        }

        rule.onNodeWithTag(boxTag).apply {
            // Verify that the cache lambda was invoked once
            assertEquals(1, cacheBuildCount)
            captureToImage().assertPixels { Color.Blue }
        }

        rule.onNodeWithTag(clickTag).performClick()

        rule.waitForIdle()

        rule.onNodeWithTag(boxTag).apply {
            // Verify the cache lambda was invoked again and the
            // rect is drawn with the updated color
            assertEquals(2, cacheBuildCount)
            captureToImage().assertPixels { Color.Red }
        }
    }

    // Helper Modifier that uses Modifier.drawWithCache internally. If the color
    // parameter
    private fun Modifier.drawPathHelperModifier(color: Color) =
        this.then(
            Modifier.drawWithCache {
                val drawSize = this.size
                val path = Path().apply {
                    lineTo(drawSize.width, 0f)
                    lineTo(drawSize.height, drawSize.height)
                    lineTo(0f, drawSize.height)
                    close()
                }
                onDrawBehind {
                    drawPath(path, color)
                }
            }
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheContentDrawnImplicitly() {
        // Verify that drawContent is invoked even if it is not explicitly called within
        // the implementation of the callback provided in the onDraw method
        // in Modifier.drawWithCache
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(Color.Red, size = Size(size.width / 2, size.height))
                        }
                    }
                    .background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(0, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Blue.toArgb(), getPixel(0, height - 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheOverContent() {
        // Verify that drawContent is not invoked implicitly if it is explicitly called within
        // the implementation of the callback provided in the onDraw method
        // in Modifier.drawWithCache. That is the red rectangle is drawn above the contents
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(Color.Red, size = Size(size.width / 2, size.height))
                        }
                    }
                    .background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, height - 1))

                assertEquals(Color.Red.toArgb(), getPixel(0, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 1, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 1, height - 1))
                assertEquals(Color.Red.toArgb(), getPixel(0, height - 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheBlendsContent() {
        // Verify that the drawing commands of drawContent are blended against the green
        // rectangle with the specified BlendMode
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier
                    .testTag(testTag)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(Color.Green, blendMode = BlendMode.Plus)
                        }
                    }
                    .background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Cyan.toArgb(), getPixel(0, 0))
                assertEquals(Color.Cyan.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Cyan.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Cyan.toArgb(), getPixel(0, height - 1))
            }
        }
    }

    @Test
    fun testInspectorValueForDrawBehind() {
        val onDraw: DrawScope.() -> Unit = {}
        rule.setContent {
            val modifier = Modifier.drawBehind(onDraw) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("drawBehind")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("onDraw")
        }
    }

    @Test
    fun testInspectorValueForDrawWithCache() {
        val onBuildDrawCache: CacheDrawScope.() -> DrawResult = { DrawResult {} }
        rule.setContent {
            val modifier = Modifier.drawWithCache(onBuildDrawCache) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("drawWithCache")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("onBuildDrawCache")
        }
    }

    @Test
    fun testInspectorValueForDrawWithContent() {
        val onDraw: DrawScope.() -> Unit = {}
        rule.setContent {
            val modifier = Modifier.drawWithContent(onDraw) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("drawWithContent")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("onDraw")
        }
    }

    @Test
    fun recompositionWithTheSameDrawBehindLambdaIsNotTriggeringRedraw() {
        val recompositionCounter = mutableStateOf(0)
        var redrawCounter = 0
        val drawBlock: DrawScope.() -> Unit = {
            redrawCounter++
        }
        rule.setContent {
            recompositionCounter.value
            Layout({}, modifier = Modifier.drawBehind(drawBlock)) { _, _ ->
                layout(100, 100) {}
            }
        }

        rule.runOnIdle {
            assertThat(redrawCounter).isEqualTo(1)
            recompositionCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(redrawCounter).isEqualTo(1)
        }
    }

    @Test
    fun recompositionWithTheSameDrawWithContentLambdaIsNotTriggeringRedraw() {
        val recompositionCounter = mutableStateOf(0)
        var redrawCounter = 0
        val drawBlock: ContentDrawScope.() -> Unit = {
            redrawCounter++
        }
        rule.setContent {
            recompositionCounter.value
            Layout({}, modifier = Modifier.drawWithContent(drawBlock)) { _, _ ->
                layout(100, 100) {}
            }
        }

        rule.runOnIdle {
            assertThat(redrawCounter).isEqualTo(1)
            recompositionCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(redrawCounter).isEqualTo(1)
        }
    }

    @Test
    fun recompositionWithTheSameDrawWithCacheLambdaIsNotTriggeringRedraw() {
        val recompositionCounter = mutableStateOf(0)
        var cacheRebuildCounter = 0
        var redrawCounter = 0
        val drawBlock: CacheDrawScope.() -> DrawResult = {
            cacheRebuildCounter++
            onDrawBehind {
                redrawCounter++
            }
        }
        rule.setContent {
            recompositionCounter.value
            Layout({}, modifier = Modifier.drawWithCache(drawBlock)) { _, _ ->
                layout(100, 100) {}
            }
        }

        rule.runOnIdle {
            assertThat(cacheRebuildCounter).isEqualTo(1)
            assertThat(redrawCounter).isEqualTo(1)
            recompositionCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(cacheRebuildCounter).isEqualTo(1)
            assertThat(redrawCounter).isEqualTo(1)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDelegatedDrawNodesDraw() {
        val testTag = "testTag"
        val size = 200

        val node = object : DelegatingNode() {
            val draw = delegate(DrawBackgroundModifier {
                drawRect(Color.Red)
            })
        }

        rule.setContent {
            AtLeastSize(
                size = size,
                modifier = Modifier
                    .testTag(testTag)
                    .elementFor(node)
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testMultipleDelegatedDrawNodes() {
        val testTag = "testTag"

        val node = object : DelegatingNode() {
            val a = delegate(DrawBackgroundModifier {
                drawRect(
                    Color.Red,
                    size = Size(10f, 10f)
                )
            })

            val b = delegate(DrawBackgroundModifier {
                drawRect(
                    Color.Blue,
                    topLeft = Offset(10f, 0f),
                    size = Size(10f, 10f))
            })
        }

        rule.setContent {
            AtLeastSize(
                size = 200,
                modifier = Modifier
                    .testTag(testTag)
                    .elementFor(node)
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(11, 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDelegatedLayoutModifierNode() {
        val testTag = "testTag"

        val node = object : DelegatingNode() {
            val a = delegate(LayoutModifierImpl { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(10.dp.roundToPx(), 10.dp.roundToPx()) {
                    p.place(0, 0)
                }
            })
        }

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag(testTag)
                    .elementFor(node)
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
    }

    @Test
    fun testInvalidationInsideOnSizeChanged() {
        var someState by mutableStateOf(1)
        var drawCount = 0
        val drawLatch = CountDownLatch(1)

        rule.setContent {
            Box(
                Modifier
                    .drawBehind {
                        @Suppress("UNUSED_EXPRESSION")
                        someState
                        drawCount++
                        drawLatch.countDown()
                    }
                    .onSizeChanged {
                        // assert that draw hasn't happened yet
                        assertEquals(0, drawCount)
                        someState++
                    }
                    .size(10.dp)
            )
        }
        assertThat(drawLatch.await(2, TimeUnit.SECONDS)).isTrue()
        rule.runOnIdle {
            // assert that state invalidation inside of onSizeChanged
            // doesn't schedule additional draw
            assertEquals(1, drawCount)
        }
    }

    // captureToImage() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun SemanticsNodeInteraction.captureToBitmap() = captureToImage().asAndroidBitmap()
}
