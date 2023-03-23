/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.drawSquares
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlUtils
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CanvasFrontBufferedRendererTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testFrontBufferedLayerRender() {
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                canvas.drawColor(Color.RED)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                canvas.drawColor(Color.BLUE)
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED ==
                    bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiBufferedLayerRender() {
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                Log.v("MultiBufferTest", "")
                canvas.drawColor(Color.RED)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                canvas.drawColor(Color.BLUE)
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
                renderer?.commit()
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (Math.abs(
                    Color.red(Color.BLUE) - Color.red(
                        bitmap.getPixel(
                            coords[0] + width / 2,
                            coords[1] + height / 2
                        )
                    )
                ) < 2) &&
                    (Math.abs(
                        Color.green(Color.BLUE) - Color.green(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2) &&
                    (Math.abs(
                        Color.blue(Color.BLUE) - Color.blue(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2)
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderMultiBufferLayer() {
        val squareSize = 100f
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Int> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Int
            ) {
                // NO-OP we do not render to the front buffered layer in this test case
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Int>
            ) {
                drawSquares(
                    canvas,
                    bufferWidth,
                    bufferHeight,
                    Color.RED,
                    Color.BLACK,
                    Color.YELLOW,
                    Color.BLUE
                )
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Int>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                val colors = listOf(Color.RED, Color.BLACK, Color.YELLOW, Color.BLUE)
                renderer?.renderMultiBufferedLayer(colors)
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            with(surfaceView!!) {
                getLocationOnScreen(coords)
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                val topLeft = bitmap.getPixel(
                    coords[0] + (squareSize / 4).toInt(),
                    coords[1] + (squareSize / 4).toInt()
                )
                val topRight = bitmap.getPixel(
                    coords[0] + (squareSize * 3f / 4f).roundToInt(),
                    coords[1] + (squareSize / 4).toInt()
                )
                val bottomLeft = bitmap.getPixel(
                    coords[0] + (squareSize / 4f).toInt(),
                    coords[1] + (squareSize * 3f / 4f).roundToInt()
                )
                val bottomRight = bitmap.getPixel(
                    coords[0] + (squareSize * 3f / 4f).roundToInt(),
                    coords[1] + (squareSize * 3f / 4f).roundToInt()
                )
                Color.RED == topLeft &&
                    Color.BLACK == topRight &&
                    Color.YELLOW == bottomLeft &&
                    Color.BLUE == bottomRight
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testCancelFrontBufferLayerRender() {
        val squareSize = 100f
        val renderLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Int> {

            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Int
            ) {
                canvas.drawColor(param)
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Int>
            ) {
                for (p in params) {
                    canvas.drawColor(p)
                }
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Int>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                with(renderer!!) {
                    renderFrontBufferedLayer(Color.BLUE)
                    commit()
                    renderFrontBufferedLayer(Color.RED)
                    cancel()
                }
            }
            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            with(surfaceView!!) {
                getLocationOnScreen(coords)
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                val pixel = bitmap.getPixel(
                    coords[0] + (squareSize / 2).toInt(),
                    coords[1] + (squareSize / 2).toInt()
                )
                // After cancel is invoked the front buffered layer should not be visible
                Color.BLUE == pixel
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @Ignore("b/266749527")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiBufferedContentsNotPersisted() {
        val screenWidth = FrontBufferedRendererTestActivity.WIDTH
        val renderLatch = CountDownLatch(1)
        val firstDrawLatch = CountDownLatch(1)
        val callbacks = object : CanvasFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                param: Any
            ) {
                canvas.drawColor(Color.RED)
                firstDrawLatch.countDown()
            }

            override fun onDrawMultiBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
                params: Collection<Any>
            ) {
                for (param in params) {
                    canvas.drawColor(Color.RED)
                }
            }

            override fun onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: CanvasFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = CanvasFrontBufferedRenderer(surfaceView!!, callbacks)
                }
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(0f)
                renderer?.commit()
                renderer?.renderFrontBufferedLayer(screenWidth / 2f)
                renderer?.commit()
            }

            Assert.assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (bitmap.getPixel(
                    coords[0] + width / 4, coords[1] + height / 2
                ) == Color.BLACK) &&
                    (bitmap.getPixel(
                        coords[0] + 3 * width / 4 - 1,
                        coords[1] + height / 2
                    ) == Color.RED)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun CanvasFrontBufferedRenderer<*>?.blockingRelease(timeoutMillis: Long = 3000) {
        if (this != null) {
            val destroyLatch = CountDownLatch(1)
            release(false) {
                destroyLatch.countDown()
            }
            Assert.assertTrue(destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS))
        } else {
            Assert.fail("CanvasFrontBufferedRenderer is not initialized")
        }
    }
}