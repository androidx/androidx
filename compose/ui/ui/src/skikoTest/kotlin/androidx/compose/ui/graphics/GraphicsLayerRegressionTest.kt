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

package androidx.compose.ui.graphics

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runInternalSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class)
class GraphicsLayerRegressionTest {

    // Bug: https://youtrack.jetbrains.com/issue/CMP-6660
    @Test
    fun layerDrawingWithRecording() = runLayerTest {
        var drawCount = 0
        setContent {
            val graphicsLayer = rememberGraphicsLayer()
            Box(Modifier.size(100.dp).drawWithContent {
                drawCount++
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            })
        }

        // shouldn't hang
        awaitIdle()

        assertEquals(1, drawCount)
    }

    // Bug: https://youtrack.jetbrains.com/issue/CMP-6695
    @Test
    fun invalidateWhenScrollChanged() = runLayerTest {
        val scrollState = ScrollState(0)

        setContent {
            val backgroundLayer = rememberGraphicsLayer()

            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        backgroundLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        this@drawWithContent.drawContent()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    repeat(100) {
                        Box(Modifier
                            .size(100.dp)
                            .background(if (it % 2 == 0) Color.Red else Color.Black))
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .testTag("bar")
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RectangleShape)
                    .drawWithContent {
                        drawLayer(backgroundLayer)
                    }
            )
        }

        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        scrollState.scrollTo(100)
        awaitIdle()
        assertEquals(Color.Black, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        scrollState.scrollTo(200)
        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])
    }

    // Bug: https://youtrack.jetbrains.com/issue/CMP-6695
    @Test
    fun invalidateNestedDrawWhenScrollChanged() = runLayerTest {
        val scrollState = ScrollState(0)

        setContent {
            val graphicsContext = LocalGraphicsContext.current
            val backgroundLayer = rememberGraphicsLayer()

            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        backgroundLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        this@drawWithContent.drawContent()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    repeat(100) {
                        Box(Modifier
                            .size(100.dp)
                            .background(if (it % 2 == 0) Color.Red else Color.Black))
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .testTag("bar")
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RectangleShape)
                    .drawWithContent {
                        val layer = graphicsContext.createGraphicsLayer()
                        layer.record { drawLayer(backgroundLayer) }
                        drawLayer(layer)
                        graphicsContext.releaseGraphicsLayer(layer)
                    }
            )
        }

        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        scrollState.scrollTo(100)
        awaitIdle()
        assertEquals(Color.Black, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        scrollState.scrollTo(200)
        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])
    }

    @Test
    fun invalidateWhenContentChanged() = runLayerTest {
        var color by mutableStateOf(Color.Red)

        setContent {
            val backgroundLayer = rememberGraphicsLayer()

            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        backgroundLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        this@drawWithContent.drawContent()
                    }
            ) {
                Box(Modifier.width(100.dp).height(1000.dp).drawWithContent {
                    drawRect(color, Offset.Zero, Size(100f, 1000f))
                })
            }

            Spacer(
                modifier = Modifier
                    .testTag("bar")
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RectangleShape)
                    .drawWithContent {
                        drawLayer(backgroundLayer)
                    }
            )
        }

        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        color = Color.Black
        awaitIdle()
        assertEquals(Color.Black, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])

        color = Color.Red
        awaitIdle()
        assertEquals(Color.Red, onNodeWithTag("bar").captureToImage().toPixelMap()[0, 0])
    }

    @OptIn(InternalTestApi::class)
    private fun runLayerTest(body: suspend SkikoComposeUiTest.() -> Unit) {
        runInternalSkikoComposeUiTest {
            runOnUiThread {
                runTest(timeout = 10.seconds) {
                    body()
                }
            }
        }
    }
}
