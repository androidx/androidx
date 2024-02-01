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

package androidx.compose.foundation

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.concurrent.futures.ResolvableFuture
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnPreDraw
import androidx.test.core.internal.os.HandlerExecutor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.graphics.HardwareRendererCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val FrameCount = 12

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@RunWith(AndroidJUnit4::class)
class AndroidExternalSurfaceTest {
    @get:Rule
    val rule = createComposeRule()

    val size = 48.dp

    @Test
    fun testOnSurface() {
        var surfaceRef: Surface? = null
        var surfaceWidth = 0
        var surfaceHeight = 0
        var expectedSize = 0

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                size.toPx().roundToInt()
            }

            AndroidExternalSurface(modifier = Modifier.size(size)) {
                onSurface { surface, width, height ->
                    surfaceRef = surface
                    surfaceWidth = width
                    surfaceHeight = height
                }
            }
        }

        rule.onRoot()
            .assertWidthIsEqualTo(size)
            .assertHeightIsEqualTo(size)
            .assertIsDisplayed()

        rule.runOnIdle {
            assertNotNull(surfaceRef)
            assertEquals(expectedSize, surfaceWidth)
            assertEquals(expectedSize, surfaceHeight)
        }
    }

    @Test
    fun testOnSurfaceChanged() {
        var surfaceWidth = 0
        var surfaceHeight = 0
        var expectedSize = 0

        var desiredSize by mutableStateOf(size)

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                desiredSize.toPx().roundToInt()
            }

            AndroidExternalSurface(modifier = Modifier.size(desiredSize)) {
                onSurface { surface, _, _ ->
                    surface.onChanged { newWidth, newHeight ->
                        surfaceWidth = newWidth
                        surfaceHeight = newHeight
                    }
                }
            }
        }

        rule.onRoot()
            .assertWidthIsEqualTo(desiredSize)
            .assertHeightIsEqualTo(desiredSize)

        // onChanged() hasn't been called yet
        rule.runOnIdle {
            assertEquals(0, surfaceWidth)
            assertEquals(0, surfaceHeight)
        }

        desiredSize = size * 2
        val prevSurfaceWidth = surfaceWidth
        val prevSurfaceHeight = surfaceHeight

        rule.onRoot()
            .assertWidthIsEqualTo(desiredSize)
            .assertHeightIsEqualTo(desiredSize)

        rule.runOnIdle {
            assertNotEquals(prevSurfaceWidth, surfaceWidth)
            assertNotEquals(prevSurfaceHeight, surfaceHeight)
            assertEquals(expectedSize, surfaceWidth)
            assertEquals(expectedSize, surfaceHeight)
        }
    }

    @Test
    fun testOnSurfaceDestroyed() {
        var surfaceRef: Surface? = null
        var visible by mutableStateOf(true)

        rule.setContent {
            if (visible) {
                AndroidExternalSurface(modifier = Modifier.size(size)) {
                    onSurface { surface, _, _ ->
                        surfaceRef = surface

                        surface.onDestroyed {
                            surfaceRef = null
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(surfaceRef)
        }

        visible = false

        rule.runOnIdle {
            assertNull(surfaceRef)
        }
    }

    @Test
    fun testOnSurfaceRecreated() {
        var surfaceCreatedCount = 0
        var surfaceDestroyedCount = 0

        var view: View? = null

        rule.setContent {
            view = LocalView.current
            AndroidExternalSurface(modifier = Modifier.size(size)) {
                onSurface { surface, _, _ ->
                    surfaceCreatedCount++
                    surface.onDestroyed {
                        surfaceDestroyedCount++
                    }
                }
            }
        }

        // NOTE: SurfaceView only triggers a Surface destroy/create cycle on visibility
        // change if its *own* visibility or the visibility of the window changes. Here
        // we change the visibility of the window by setting the visibility of the root
        // view (the host view in ViewRootImpl).
        rule.runOnIdle {
            assertEquals(1, surfaceCreatedCount)
            assertEquals(0, surfaceDestroyedCount)
            view?.rootView?.visibility = View.INVISIBLE
        }

        rule.runOnIdle {
            assertEquals(1, surfaceCreatedCount)
            assertEquals(1, surfaceDestroyedCount)
            view?.rootView?.visibility = View.VISIBLE
        }

        rule.runOnIdle {
            assertEquals(2, surfaceCreatedCount)
            assertEquals(1, surfaceDestroyedCount)
        }
    }

    @Test
    fun testRender() {
        var surfaceRef: Surface? = null
        var expectedSize = 0

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                size.toPx().roundToInt()
            }
            AndroidExternalSurface(modifier = Modifier.size(size)) {
                onSurface { surface, _, _ ->
                    surfaceRef = surface
                    surface.lockHardwareCanvas().apply {
                        drawColor(Color.Blue.toArgb())
                        surface.unlockCanvasAndPost(this)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(surfaceRef)
        }

        surfaceRef!!
            .captureToImage(expectedSize, expectedSize)
            .assertPixels { Color.Blue }
    }

    @Test
    fun testZOrderDefault() {
        val latch = CountDownLatch(FrameCount)

        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                AndroidExternalSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("GraphicSurface")
                ) {
                    onSurface { surface, _, _ ->
                        // Draw > 3 frames to make sure the screenshot copy will pick up
                        // a SurfaceFlinger composition that includes our Surface
                        repeat(FrameCount) {
                            withFrameNanos {
                                surface.lockHardwareCanvas().apply {
                                    drawColor(Color.Blue.toArgb())
                                    surface.unlockCanvasAndPost(this)
                                }
                                latch.countDown()
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.size(size)) {
                    drawRect(Color.Green)
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed waiting for render")
        }

        rule
            .onNodeWithTag("GraphicSurface")
            .screenshotToImage()!!
            .assertPixels { Color.Green }
    }

    @Test
    @Ignore(
        """Despite best efforts in screenshotToImage(), this test is too flaky currently.
            |Since this test only tests that the `zOrder` parameter is properly passed
            |to the underlying SurfaceView, we don't lose much by disabling it.
            |This test should be more robust on API level 34 using the window
            |screenshot API."""
    )
    fun testZOrderMediaOverlay() {
        val latch = CountDownLatch(FrameCount * 2) // for 2 Surfaces

        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                AndroidExternalSurface(
                    modifier = Modifier.size(size),
                    zOrder = AndroidExternalSurfaceZOrder.Behind
                ) {
                    onSurface { surface, _, _ ->
                        // Draw > 3 frames to make sure the screenshot copy will pick up
                        // a SurfaceFlinger composition that includes our Surface
                        repeat(FrameCount) {
                            withFrameNanos {
                                surface.lockHardwareCanvas().apply {
                                    drawColor(Color.Blue.toArgb())
                                    surface.unlockCanvasAndPost(this)
                                }
                                latch.countDown()
                            }
                        }
                    }
                }
                AndroidExternalSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("GraphicSurface"),
                    zOrder = AndroidExternalSurfaceZOrder.MediaOverlay
                ) {
                    onSurface { surface, _, _ ->
                        repeat(FrameCount) {
                            withFrameNanos {
                                surface.lockHardwareCanvas().apply {
                                    drawColor(Color.Red.toArgb())
                                    surface.unlockCanvasAndPost(this)
                                }
                                latch.countDown()
                            }
                        }
                    }
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed waiting for render")
        }

        rule
            .onNodeWithTag("GraphicSurface")
            .screenshotToImage()!!
            .assertPixels { Color.Red }
    }

    @Test
    @Ignore(
        """Despite best efforts in screenshotToImage(), this test is too flaky currently.
            |Since this test only tests that the `zOrder` parameter is properly passed
            |to the underlying SurfaceView, we don't lose much by disabling it.
            |This test should be more robust on API level 34 using the window
            |screenshot API."""
    )
    fun testZOrderOnTop() {
        val latch = CountDownLatch(FrameCount)

        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                AndroidExternalSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("GraphicSurface"),
                    zOrder = AndroidExternalSurfaceZOrder.OnTop
                ) {
                    onSurface { surface, _, _ ->
                        // Draw > 3 frames to make sure the screenshot copy will pick up
                        // a SurfaceFlinger composition that includes our Surface
                        repeat(FrameCount) {
                            withFrameNanos {
                                surface.lockHardwareCanvas().apply {
                                    drawColor(Color.Blue.toArgb())
                                    surface.unlockCanvasAndPost(this)
                                }
                                latch.countDown()
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.size(size)) {
                    drawRect(Color.Green)
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed waiting for render")
        }

        rule
            .onNodeWithTag("GraphicSurface")
            .screenshotToImage()!!
            .assertPixels { Color.Blue }
    }

    @Test
    @Ignore(
        """Despite best efforts in screenshotToImage(), this test is too flaky currently.
            |Since this test only tests that the `isOpaque` parameter is properly passed
            |to the underlying SurfaceView, we don't lose much by disabling it.
            |This test should be more robust on API level 34 using the window
            |screenshot API."""
    )
    fun testNotOpaque() {
        val latch = CountDownLatch(FrameCount)
        val translucentRed = Color(1.0f, 0.0f, 0.0f, 0.5f).toArgb()

        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                AndroidExternalSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("GraphicSurface"),
                    isOpaque = false,
                    zOrder = AndroidExternalSurfaceZOrder.OnTop
                ) {
                    onSurface { surface, _, _ ->
                        // Draw > 3 frames to make sure the screenshot copy will pick up
                        // a SurfaceFlinger composition that includes our Surface
                        repeat(FrameCount) {
                            withFrameNanos {
                                surface.lockHardwareCanvas().apply {
                                    // Since we are drawing a translucent color we need to
                                    // clear first
                                    drawColor(0x00000000, PorterDuff.Mode.CLEAR)
                                    drawColor(translucentRed)
                                    surface.unlockCanvasAndPost(this)
                                }
                                latch.countDown()
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.size(size)) {
                    drawRect(Color.White)
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed waiting for render")
        }

        val expectedColor = Color(ColorUtils.compositeColors(translucentRed, Color.White.toArgb()))

        rule
            .onNodeWithTag("GraphicSurface")
            .screenshotToImage()!!
            .assertPixels { expectedColor }
    }

    @Test
    @Ignore(
        """Despite best efforts in screenshotToImage(), this test is too flaky currently.
            |Since this test only tests that the `isSecure` parameter is properly passed
            |to the underlying SurfaceView, we don't lose much by disabling it.
            |This test should be more robust on API level 34 using the window
            |screenshot API."""
    )
    fun testSecure() {
        val latch = CountDownLatch(FrameCount)

        rule.setContent {
            AndroidExternalSurface(
                modifier = Modifier
                    .size(size)
                    .testTag("GraphicSurface"),
                isSecure = true
            ) {
                onSurface { surface, _, _ ->
                    // Draw > 3 frames to make sure the screenshot copy will pick up
                    // a SurfaceFlinger composition that includes our Surface
                    repeat(FrameCount) {
                        withFrameNanos {
                            surface.lockHardwareCanvas().apply {
                                drawColor(Color.Blue.toArgb())
                                surface.unlockCanvasAndPost(this)
                            }
                            latch.countDown()
                        }
                    }
                }
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw AssertionError("Failed waiting for render")
        }

        val screen = rule
            .onNodeWithTag("GraphicSurface")
            .screenshotToImage(true)

        // Before API 33 taking a screenshot with a secure surface returns null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            assertNull(screen)
        } else {
            screen!!.assertPixels { Color.Black }
        }
    }
}

/**
 * Returns an ImageBitmap containing a screenshot of the device. On API < 33,
 * a secure surface present on screen can cause this function to return null.
 */
private fun SemanticsNodeInteraction.screenshotToImage(
    hasSecureSurfaces: Boolean = false
): ImageBitmap? {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()

    val uiAutomation = instrumentation.uiAutomation

    val node = fetchSemanticsNode()
    val view = (node.root as ViewRootForTest).view

    return withDrawingEnabled {
        val bitmapFuture: ResolvableFuture<Bitmap> = ResolvableFuture.create()

        var cleanup = { }

        val mainExecutor = HandlerExecutor(Handler(Looper.getMainLooper()))
        mainExecutor.execute {
            cleanup = view.waitForWindowManager {
                Choreographer.getInstance().postFrameCallback {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)

                    val bounds = node.boundsInRoot.translate(
                        location[0].toFloat(),
                        location[1].toFloat()
                    ).deflate(1.0f) // inset the rectangle to avoid rounding errors

                    // do multiple retries of uiAutomation.takeScreenshot because it is
                    // known to return null on API 31+ b/257274080
                    var bitmap: Bitmap? = null
                    var i = 0
                    while (i < 3 && bitmap == null) {
                        bitmap = uiAutomation.takeScreenshot()
                        i++
                    }

                    if (bitmap != null) {
                        bitmap = Bitmap.createBitmap(
                            bitmap,
                            bounds.left.toInt(),
                            bounds.top.toInt(),
                            bounds.width.toInt(),
                            bounds.height.toInt()
                        )
                        bitmapFuture.set(bitmap)
                    } else {
                        if (hasSecureSurfaces) {
                            // may be null on older API levels when a secure surface is showing
                            bitmapFuture.set(null)
                        }
                        // if we don't show secure surfaces, let the future timeout on get()
                    }
                }
            }
        }

        val bitmap = try {
            bitmapFuture.get(5, TimeUnit.SECONDS)?.asImageBitmap()
        } catch (e: ExecutionException) {
            null
        }

        cleanup()

        bitmap
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun Surface.captureToImage(width: Int, height: Int): ImageBitmap {
    val bitmap = createBitmap(width, height)

    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result ->
        copyResult = result
        latch.countDown()
    }

    PixelCopy.request(
        this,
        Rect(0, 0, width, height),
        bitmap,
        onCopyFinished,
        Handler(Looper.getMainLooper())
    )

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }

    if (copyResult != PixelCopy.SUCCESS) {
        throw AssertionError("PixelCopy failed!")
    }

    return bitmap.asImageBitmap()
}

private fun <R> withDrawingEnabled(block: () -> R): R {
    val wasDrawingEnabled = HardwareRendererCompat.isDrawingEnabled()
    try {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(true)
        }
        return block.invoke()
    } finally {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(false)
        }
    }
}

/**
 * Waits for the WindowManager to be "ready" and then runs [onWindowManagerReady].
 * The wait is implemented by creating a new window and waiting for its content to
 * draw once. Doing so should in theory guarantee that SurfaceViews from the parent
 * window are also ready and visible, and thus reduce flakes.
 *
 * This method returns a lambda that must be executed to perform cleanup before
 * the test finishes and the activity is torn down.
 */
@UiThread
private fun View.waitForWindowManager(onWindowManagerReady: () -> Unit): () -> Unit {
    val subWindow = TextView(context).apply {
        text = "WM Ready"
        setBackgroundColor(Color.White.toArgb())
    }

    val windowManager = context.getSystemService<WindowManager>()!!
    windowManager.addView(
        subWindow,
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        ).apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.RIGHT or Gravity.BOTTOM
        }
    )

    subWindow.doOnPreDraw {
        onWindowManagerReady()
    }

    return { windowManager.removeView(subWindow) }
}
