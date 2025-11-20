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

import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.testutils.captureToImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TestActivity
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

const val TEST_WIDTH = 600
const val TEST_HEIGHT = 400

fun shadowTest(block: DrawScope.() -> Unit, verify: (PixelMap) -> Unit) {
    var scenario: ActivityScenario<TestActivity>? = null
    try {
        var target: View? = null
        scenario =
            ActivityScenario.launch(TestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    val content =
                        FrameLayout(it).apply {
                            clipChildren = false
                            clipToPadding = false
                            setForeground(ShadowDrawableHost(block))
                        }
                    it.setContentView(
                        content,
                        FrameLayout.LayoutParams(TEST_WIDTH, TEST_HEIGHT).apply {
                            gravity = Gravity.CENTER
                        },
                    )
                    target = content
                }
        val resumed = CountDownLatch(1)
        scenario.moveToState(Lifecycle.State.RESUMED).onActivity { activity ->
            activity.runOnUiThread { resumed.countDown() }
        }
        assertTrue(resumed.await(3000, TimeUnit.MILLISECONDS))

        val pixelMap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                target!!.captureToImage().toPixelMap()
            } else {
                val bitmap = ImageBitmap(TEST_WIDTH, TEST_HEIGHT)
                val canvas = ComposeCanvas(bitmap)
                target!!.draw(canvas.nativeCanvas)
                bitmap.toPixelMap()
            }
        verify(pixelMap)
    } finally {
        val detachLatch = CountDownLatch(1)
        scenario?.onActivity { detachLatch.countDown() }
        assertTrue(detachLatch.await(3000, TimeUnit.MILLISECONDS))
        scenario?.moveToState(Lifecycle.State.DESTROYED)
    }
}

fun verifyShadow(
    pixelmap: PixelMap,
    leftToCenter: (Color, Color) -> Unit = { _, _ -> },
    topToCenter: (Color, Color) -> Unit = { _, _ -> },
    rightToCenter: (Color, Color) -> Unit = { _, _ -> },
    bottomToCenter: (Color, Color) -> Unit = { _, _ -> },
) {
    var prevLeft = pixelmap[0, pixelmap.height / 2]
    for (i in 0 until pixelmap.width / 4) {
        val current = pixelmap[i, pixelmap.height / 2]
        leftToCenter(prevLeft, current)
        prevLeft = current
    }

    var prevTop = pixelmap[pixelmap.width / 2, 0]
    for (i in 0 until pixelmap.height / 4) {
        val current = pixelmap[pixelmap.width / 2, i]
        topToCenter(prevTop, current)
        prevTop = current
    }

    var prevRight = pixelmap[pixelmap.width - 1, pixelmap.height / 2]
    for (i in pixelmap.width - 1 downTo pixelmap.width / 2 + pixelmap.width / 4) {
        val current = pixelmap[i, pixelmap.height / 2]
        rightToCenter(prevRight, current)
        prevRight = current
    }

    var prevBottom = pixelmap[pixelmap.width / 2, pixelmap.height - 1]
    for (i in pixelmap.height - 1 downTo (pixelmap.height / 2 + pixelmap.width / 4)) {
        val current = pixelmap[pixelmap.width / 2, i]
        bottomToCenter(prevBottom, current)
        prevBottom = current
    }
}

private class ShadowDrawableHost(val block: DrawScope.() -> Unit) : Drawable() {

    val canvasDrawScope = CanvasDrawScope()

    override fun draw(canvas: AndroidCanvas) {
        val bounds = getBounds()
        canvasDrawScope.draw(
            Density(1f, 1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = ComposeCanvas(canvas),
            size = Size(bounds.width().toFloat(), bounds.height().toFloat()),
            block = block,
        )
    }

    override fun setAlpha(alpha: Int) {
        // NO-OP
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // NO-OP
    }

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun createTestImageShaderBrush(
    width: Int = TEST_WIDTH,
    height: Int = TEST_HEIGHT,
    block: DrawScope.() -> Unit = {
        val halfHeight = size.height / 2
        val size = Size(width.toFloat(), halfHeight)
        drawRect(Color.Red, size = size)
        drawRect(Color.Green, size = size, topLeft = Offset(0f, halfHeight))
    },
): ShaderBrush {
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope()
        .draw(
            Density(1f, 1f),
            LayoutDirection.Ltr,
            ComposeCanvas(bitmap),
            Size(width.toFloat(), height.toFloat()),
            block,
        )
    return ShaderBrush(ImageShader(bitmap))
}
