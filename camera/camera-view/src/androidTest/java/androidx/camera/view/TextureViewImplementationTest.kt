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
package androidx.camera.view

import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.SystemClock
import android.util.Size
import android.view.Choreographer
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.view.PreviewViewImplementation.OnSurfaceNotInUseListener
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextureViewImplementationTest {
    private var parent: FrameLayout? = null
    private var implementation: TextureViewImplementation? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var _surfaceRequest: SurfaceRequest? = null
    private val surfaceRequest: SurfaceRequest
        get() {
            if (_surfaceRequest == null) {
                _surfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
            }
            return _surfaceRequest!!
        }

    @Before
    fun setUp() {
        val mContext = InstrumentationRegistry.getInstrumentation().targetContext
        surfaceTexture = SurfaceTexture(0)
        parent = FrameLayout(mContext)
        implementation = TextureViewImplementation(parent!!, PreviewTransformation())
    }

    @After
    fun tearDown() {
        if (_surfaceRequest != null) {
            _surfaceRequest!!.willNotProvideSurface()
            // Ensure all successful requests have their returned future finish.
            _surfaceRequest!!.deferrableSurface.close()
            _surfaceRequest = null
        }
        surfaceTexture?.release()
    }

    @LargeTest
    @Test(expected = TimeoutException::class)
    @Throws(Exception::class)
    fun doNotProvideSurface_ifSurfaceTextureNotAvailableYet() {
        val request = surfaceRequest
        implementation!!.onSurfaceRequested(request, null)
        request.deferrableSurface.surface[2, TimeUnit.SECONDS]
    }

    @Test
    @Throws(Exception::class)
    fun provideSurface_ifSurfaceTextureAvailable() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceListenableFuture = surfaceRequest.deferrableSurface.surface
        implementation!!
            .mTextureView
            .surfaceTextureListener!!
            .onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        val surface = surfaceListenableFuture.get()
        Truth.assertThat(surface).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceListenableFuture = surfaceRequest.deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNotNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isFalse()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun onSurfaceNotInUseListener_IsCalledWhenCameraNotUsingSurface() {
        val surfaceRequest = surfaceRequest
        val latchForSurfaceNotInUse = CountDownLatch(1)
        val onSurfaceNotInUseListener = OnSurfaceNotInUseListener {
            latchForSurfaceNotInUse.countDown()
        }
        implementation!!.onSurfaceRequested(surfaceRequest, onSurfaceNotInUseListener)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        deferrableSurface.close()
        Truth.assertThat(latchForSurfaceNotInUse.await(500, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    @MediumTest
    @Throws(Exception::class)
    fun onSurfaceNotInUseListener_IsCalledWhenSurfaceRequestIsCancelled() {
        val surfaceRequest = surfaceRequest
        val latchForSurfaceNotInUse = CountDownLatch(1)
        val onSurfaceNotInUseListener = OnSurfaceNotInUseListener {
            latchForSurfaceNotInUse.countDown()
        }
        implementation!!.onSurfaceRequested(surfaceRequest, onSurfaceNotInUseListener)
        val deferrableSurface = surfaceRequest.deferrableSurface
        deferrableSurface.surface

        // onSurfaceTextureAvailable is not called, so the surface will not be provided.
        // closing the surface will only trigger the SurfaceRequest RequestCancellationListener.
        deferrableSurface.close()
        Truth.assertThat(latchForSurfaceNotInUse.await(500, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        // Cancel the request from the camera side
        surfaceRequest.deferrableSurface.surface.cancel(true)

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceRequest).isNull()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurface_whenSurfaceTextureDestroyed_andCameraSurfaceRequestIsCancelled() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        // Cancel the request from the client side
        surfaceRequest.willNotProvideSurface()
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        // Wait enough time for surfaceReleaseFuture's listener to be called.
        Thread.sleep(1000)
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
        Truth.assertThat(implementation!!.mSurfaceTexture).isNull()
    }

    @Test
    fun doNotCreateTextureView_beforeSensorOutputSizeKnown() {
        Truth.assertThat(parent!!.childCount).isEqualTo(0)
    }

    @Test
    @Throws(Exception::class)
    fun resetSurfaceTextureOnDetachAndAttachWindow() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        implementation!!.onDetachedFromWindow()
        implementation!!.onAttachedToWindow()
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
        Truth.assertThat(implementation!!.mTextureView.surfaceTexture).isEqualTo(surfaceTexture)
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseDetachedSurfaceTexture_whenDeferrableSurfaceClose() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
    }

    @Test
    fun keepOnlyLatestTextureView_whenGetSurfaceProviderCalledMultipleTimes() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView1 = parent!!.getChildAt(0) as TextureView
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView2 = parent!!.getChildAt(0) as TextureView
        Truth.assertThat(textureView1).isNotSameInstanceAs(textureView2)
        Truth.assertThat(parent!!.childCount).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun waitForNextFrame_futureCompletesWhenFrameArrives() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        val futureNextFrame = implementation!!.waitForNextFrame()
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceTextureListener.onSurfaceTextureUpdated(surfaceTexture!!)
        futureNextFrame[300, TimeUnit.MILLISECONDS]
    }

    @LargeTest
    @Test
    fun withoutCameraTransform_rotatesRenderedContentOnScreenWithoutNewFrame() {
        // Apply locally so the headless @SmallTest methods in this class stay fast and rule-free.
        RequireForegroundRule()
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        val instrumentation = InstrumentationRegistry.getInstrumentation()
                        val context = instrumentation.targetContext
                        val containerWidth = 240
                        val containerHeight = 320
                        lateinit var previewContainer: FrameLayout
                        lateinit var activityWindow: Window
                        val previewTransform =
                            PreviewTransformation().apply {
                                scaleType = PreviewView.ScaleType.FIT_CENTER
                            }
                        val layoutReadyLatch = CountDownLatch(1)

                        CoreAppTestUtil.checkKeyguard(context)
                        ActivityScenario.launch(FakeActivity::class.java).use { scenario ->
                            val request = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
                            try {
                                request.updateTransformationInfo(
                                    SurfaceRequest.TransformationInfo.of(
                                        Rect(0, 0, ANY_WIDTH, ANY_HEIGHT),
                                        /* rotationDegrees= */ 0,
                                        Surface.ROTATION_0,
                                        /* hasCameraTransform= */ false,
                                        Matrix(),
                                        /* mirroring= */ false,
                                    )
                                )
                                scenario.onActivity { activity ->
                                    activityWindow = activity.window
                                    val root =
                                        FrameLayout(context).apply {
                                            setBackgroundColor(Color.BLACK)
                                        }
                                    previewContainer =
                                        FrameLayout(context).apply {
                                            setBackgroundColor(Color.BLACK)
                                            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _
                                                ->
                                                if (
                                                    view.width == containerWidth &&
                                                        view.height == containerHeight
                                                ) {
                                                    layoutReadyLatch.countDown()
                                                }
                                            }
                                        }
                                    root.addView(
                                        previewContainer,
                                        FrameLayout.LayoutParams(
                                            containerWidth,
                                            containerHeight,
                                            Gravity.CENTER,
                                        ),
                                    )
                                    activity.setContentView(root)
                                }
                                assertThat(layoutReadyLatch.await(5, TimeUnit.SECONDS)).isTrue()

                                val textureImpl =
                                    TextureViewImplementation(previewContainer, previewTransform)
                                request.setTransformationInfoListener(
                                    ContextCompat.getMainExecutor(context)
                                ) { info ->
                                    previewTransform.setTransformationInfo(
                                        info,
                                        request.resolution,
                                        /* useFrontCameraRotation= */ false,
                                    )
                                    textureImpl.redrawPreview()
                                }
                                instrumentation.runOnMainSync {
                                    textureImpl.onSurfaceRequested(request, null)
                                }
                                val firstFrameFuture = textureImpl.waitForNextFrame()
                                val surface =
                                    request.deferrableSurface.surface.get(5, TimeUnit.SECONDS)
                                drawQuadrants(surface)
                                firstFrameFuture.get(5, TimeUnit.SECONDS)
                                instrumentation.waitForIdleSync()

                                // Draw only once above, then rotate without posting any new frame
                                // so the test verifies TextureView#invalidate() redraws the
                                // HardwareLayer on rotation changes.
                                val baseQuadrants =
                                    listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
                                for ((step, rotationDegrees) in
                                    listOf(0, 90, 180, 270).withIndex()) {
                                    request.updateTransformationInfo(
                                        SurfaceRequest.TransformationInfo.of(
                                            Rect(0, 0, ANY_WIDTH, ANY_HEIGHT),
                                            rotationDegrees,
                                            Surface.ROTATION_0,
                                            /* hasCameraTransform= */ false,
                                            Matrix(),
                                            /* mirroring= */ false,
                                        )
                                    )
                                    instrumentation.waitForIdleSync()
                                    val expectedQuadrants =
                                        List(baseQuadrants.size) { index ->
                                            baseQuadrants[(index - step).mod(baseQuadrants.size)]
                                        }
                                    val fittedRect =
                                        if (rotationDegrees % 180 == 0) {
                                            Rect(0, 70, 240, 250)
                                        } else {
                                            Rect(0, 0, 240, 320)
                                        }
                                    assertRenderedQuadrantsEventually(
                                        previewContainer,
                                        activityWindow,
                                        rotationDegrees,
                                        fittedRect,
                                        expectedQuadrants,
                                    )
                                }
                            } finally {
                                request.willNotProvideSurface()
                                request.deferrableSurface.close()
                            }
                        }
                    }
                },
                Description.createTestDescription(
                    TextureViewImplementationTest::class.java,
                    "withoutCameraTransform_rotatesRenderedContentOnScreenWithoutNewFrame",
                ),
            )
            .evaluate()
    }

    private fun drawQuadrants(surface: Surface) {
        val canvas = surface.lockCanvas(null)
        try {
            assertThat(canvas.width).isEqualTo(ANY_WIDTH)
            assertThat(canvas.height).isEqualTo(ANY_HEIGHT)
            val paint = Paint().apply { style = Paint.Style.FILL }
            val halfWidth = ANY_WIDTH / 2f
            val halfHeight = ANY_HEIGHT / 2f
            paint.color = Color.RED
            canvas.drawRect(0f, 0f, halfWidth, halfHeight, paint)
            paint.color = Color.GREEN
            canvas.drawRect(halfWidth, 0f, ANY_WIDTH.toFloat(), halfHeight, paint)
            paint.color = Color.BLUE
            canvas.drawRect(halfWidth, halfHeight, ANY_WIDTH.toFloat(), ANY_HEIGHT.toFloat(), paint)
            paint.color = Color.YELLOW
            canvas.drawRect(0f, halfHeight, halfWidth, ANY_HEIGHT.toFloat(), paint)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun assertRenderedQuadrantsEventually(
        view: View,
        window: Window,
        rotationDegrees: Int,
        fittedRect: Rect,
        expectedQuadrants: List<Int>,
    ) {
        val insetX = fittedRect.width() / 6
        val insetY = fittedRect.height() / 6
        val samplePoints =
            listOf(
                fittedRect.left + insetX to fittedRect.top + insetY,
                fittedRect.right - insetX to fittedRect.top + insetY,
                fittedRect.right - insetX to fittedRect.bottom - insetY,
                fittedRect.left + insetX to fittedRect.bottom - insetY,
            )
        var lastSampledColors: List<Int> = emptyList()
        val deadlineMillis = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadlineMillis) {
            awaitNextFrame()
            val screenshot = captureViewOnScreen(view, window) ?: continue
            try {
                val sampledColors = samplePoints.map { (x, y) -> screenshot.getPixel(x, y) }
                if (
                    sampledColors.zip(expectedQuadrants).all { (actual, expected) ->
                        matchesColor(actual, expected)
                    }
                ) {
                    return
                }
                lastSampledColors = sampledColors
            } finally {
                screenshot.recycle()
            }
        }
        throw AssertionError(
            "Rendered quadrants at rotation=$rotationDegrees did not match expected " +
                "${expectedQuadrants.map(::formatColor)}, last sampled " +
                "${lastSampledColors.map(::formatColor)} at $samplePoints"
        )
    }

    private fun awaitNextFrame() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val frameLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback { frameLatch.countDown() }
        }
        assertThat(frameLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    private fun captureViewOnScreen(view: View, window: Window): Bitmap? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val location = IntArray(2)
        var width = 0
        var height = 0
        var useWindowScreenshot = false
        instrumentation.runOnMainSync {
            width = view.width
            height = view.height
            useWindowScreenshot =
                Build.VERSION.SDK_INT >= 34 && view.display?.displayId != Display.DEFAULT_DISPLAY
            if (useWindowScreenshot) {
                view.getLocationInWindow(location)
            } else {
                view.getLocationOnScreen(location)
            }
        }
        if (width <= 0 || height <= 0) {
            return null
        }
        val rawScreenshot =
            if (Build.VERSION.SDK_INT >= 34 && useWindowScreenshot) {
                Api34Impl.takeScreenshot(instrumentation.uiAutomation, window)
            } else {
                instrumentation.uiAutomation.takeScreenshot()
            } ?: return null
        var cropped: Bitmap? = null
        try {
            val softwareScreenshot =
                if (Build.VERSION.SDK_INT >= 26 && rawScreenshot.config == Bitmap.Config.HARDWARE) {
                    rawScreenshot.copy(Bitmap.Config.ARGB_8888, /* isMutable= */ false)
                        ?: return null
                } else {
                    rawScreenshot
                }
            try {
                val left = location[0]
                val top = location[1]
                if (
                    left < 0 ||
                        top < 0 ||
                        left + width > softwareScreenshot.width ||
                        top + height > softwareScreenshot.height
                ) {
                    return null
                }
                cropped = Bitmap.createBitmap(softwareScreenshot, left, top, width, height)
                return cropped
            } finally {
                if (softwareScreenshot !== rawScreenshot && softwareScreenshot !== cropped) {
                    softwareScreenshot.recycle()
                }
            }
        } finally {
            if (rawScreenshot !== cropped) {
                rawScreenshot.recycle()
            }
        }
    }

    private fun matchesColor(actual: Int, expected: Int): Boolean {
        val r = Color.red(actual)
        val g = Color.green(actual)
        val b = Color.blue(actual)
        return when (expected) {
            Color.RED -> r >= 180 && g <= 80 && b <= 80
            Color.GREEN -> r <= 80 && g >= 180 && b <= 80
            Color.BLUE -> r <= 80 && g <= 80 && b >= 180
            Color.YELLOW -> r >= 180 && g >= 180 && b <= 80
            else -> false
        }
    }

    private fun formatColor(color: Int): String =
        "#${color.toUInt().toString(16).uppercase().padStart(8, '0')}"

    @RequiresApi(34)
    private object Api34Impl {
        fun takeScreenshot(uiAutomation: UiAutomation, window: Window): Bitmap? =
            uiAutomation.takeScreenshot(window)
    }

    companion object {
        private const val ANY_WIDTH = 1600
        private const val ANY_HEIGHT = 1200
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }
    }
}
