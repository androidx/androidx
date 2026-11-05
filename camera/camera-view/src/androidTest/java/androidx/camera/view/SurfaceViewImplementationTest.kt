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

package androidx.camera.view

import android.app.UiAutomation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.util.Size
import android.view.Choreographer
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.ParameterizedTestConfigUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.view.PreviewViewImplementation.OnSurfaceNotInUseListener
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class SurfaceViewImplementationTest(name: String) {

    companion object {
        private const val ANY_WIDTH = 640
        private const val ANY_HEIGHT = 480
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            ParameterizedTestConfigUtil.generateInLabRequiredTestDefaultParameterizedTestConfigs()
    }

    @get:Rule val requireForegroundRule = RequireForegroundRule()

    private lateinit var mParent: FrameLayout
    private lateinit var mImplementation: SurfaceViewImplementation
    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var mSurfaceRequest: SurfaceRequest
    private lateinit var mContext: Context

    // Shows the view in activity so that SurfaceView can work normally
    private lateinit var mActivityScenario: ActivityScenario<FakeActivity>

    private val mSurfaceRequests = mutableListOf<SurfaceRequest>()

    @Before
    fun setUp() {
        mActivityScenario = ActivityScenario.launch(FakeActivity::class.java)
        mContext = ApplicationProvider.getApplicationContext()
        mParent = FrameLayout(mContext)
        setContentView(mParent)

        mSurfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
        mImplementation = SurfaceViewImplementation(mParent, PreviewTransformation())
    }

    @After
    fun tearDown() {
        if (::mSurfaceRequest.isInitialized) {
            mSurfaceRequest.willNotProvideSurface()
            mSurfaceRequest.deferrableSurface.close()
        }
        for (surfaceRequest in mSurfaceRequests) {
            surfaceRequest.willNotProvideSurface()
            surfaceRequest.deferrableSurface.close()
        }
        mSurfaceRequests.clear()
        if (::mActivityScenario.isInitialized) {
            mActivityScenario.close()
        }
    }

    @Test
    fun surfaceProvidedSuccessfully() {
        CoreAppTestUtil.checkKeyguard(mContext)

        mImplementation.testSurfaceRequest(mSurfaceRequest)
    }

    @Test
    fun reuseSurfaceView_whenResolutionNotChanged() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        mImplementation.testSurfaceRequest(mSurfaceRequest)
        val previousSurfaceView = mImplementation.mSurfaceView

        // Act.
        val sameResolutionSurfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
        mImplementation.testSurfaceRequest(sameResolutionSurfaceRequest)
        val newSurfaceView = mImplementation.mSurfaceView

        // Assert.
        assertThat(newSurfaceView).isEqualTo(previousSurfaceView)
    }

    @Test
    fun notReuseSurfaceView_whenResolutionChanged() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        mImplementation.testSurfaceRequest(mSurfaceRequest)
        val previousSurfaceView = mImplementation.mSurfaceView

        // Act.
        val differentSize: Size by lazy { Size(720, 480) }
        val differentResolutionSurfaceRequest = SurfaceRequest(differentSize, FakeCamera()) {}
        mImplementation.testSurfaceRequest(differentResolutionSurfaceRequest)
        val newSurfaceView = mImplementation.mSurfaceView

        // Assert.
        assertThat(newSurfaceView).isNotEqualTo(previousSurfaceView)
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceIsNotUsedAnyMore() {
        CoreAppTestUtil.checkKeyguard(mContext)

        val listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = { listenerLatch.countDown() }

        mImplementation.testSurfaceRequest(mSurfaceRequest, onSurfaceNotInUseListener)

        assertThat(listenerLatch.await(300, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceRequestIsCancelled() {
        val listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = { listenerLatch.countDown() }

        // Not attach the mParent to the window so that the Surface cannot be created.
        setContentView(View(mContext))

        mInstrumentation.runOnMainSync {
            mImplementation.onSurfaceRequested(mSurfaceRequest, onSurfaceNotInUseListener)
        }

        // Since the surface is not created,  close the deferrableSurface will trigger only
        // SurfaceRequest RequestCancellationListener
        mSurfaceRequest.deferrableSurface.close()

        assertThat(listenerLatch.await(300, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun withCameraTransform_providesSurfaceViewSurface() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        val surfaceRequest = createSurfaceRequest(hasCameraTransform = true)

        // Act.
        val surface = mImplementation.provideSurface(surfaceRequest)

        // Assert.
        assertThat(surface).isSameInstanceAs(mImplementation.mSurfaceView.holder.surface)
        surfaceRequest.deferrableSurface.close()
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun withoutCameraTransform_providesContentSurfaceUntilSurfaceViewIsDestroyed() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        val surfaceRequest = createSurfaceRequest(hasCameraTransform = false)

        // Act.
        val surface = mImplementation.provideSurface(surfaceRequest)

        // Assert: the frames are displayed through a separate layer that applies the rotation.
        assertThat(surface).isNotSameInstanceAs(mImplementation.mSurfaceView.holder.surface)
        assertThat(surface.isValid).isTrue()

        // Act: the camera no longer uses the Surface.
        surfaceRequest.deferrableSurface.close()
        mInstrumentation.waitForIdleSync()

        // Assert: the Surface stays valid while the SurfaceView is displayed, e.g. for the next
        // SurfaceRequest.
        assertThat(surface.isValid).isTrue()

        // Act: the SurfaceView is destroyed.
        setContentView(View(mContext))
        mInstrumentation.waitForIdleSync()

        // Assert: the Surface is released.
        assertThat(surface.isValid).isFalse()
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun notReuseSurfaceView_whenCameraTransformChanged() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        mImplementation.testSurfaceRequest(createSurfaceRequest(hasCameraTransform = true))
        val previousSurfaceView = mImplementation.mSurfaceView

        // Act.
        mImplementation.testSurfaceRequest(createSurfaceRequest(hasCameraTransform = false))

        // Assert.
        assertThat(mImplementation.mSurfaceView).isNotEqualTo(previousSurfaceView)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun withoutCameraTransform_rotatesRenderedContentOnScreen() {
        // Arrange: a 240x320 container displaying a 640x480 stream without the camera transform.
        CoreAppTestUtil.checkKeyguard(mContext)
        val containerWidth = 240
        val containerHeight = 320
        lateinit var previewContainer: FrameLayout
        lateinit var activityWindow: Window
        val previewTransform =
            PreviewTransformation().apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
        val layoutReadyLatch = CountDownLatch(1)
        mActivityScenario.onActivity { activity ->
            activityWindow = activity.window
            val root = FrameLayout(mContext).apply { setBackgroundColor(Color.BLACK) }
            previewContainer =
                FrameLayout(mContext).apply {
                    setBackgroundColor(Color.BLACK)
                    addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                        if (view.width == containerWidth && view.height == containerHeight) {
                            layoutReadyLatch.countDown()
                        }
                    }
                }
            root.addView(
                previewContainer,
                FrameLayout.LayoutParams(containerWidth, containerHeight, Gravity.CENTER),
            )
            activity.setContentView(root)
        }
        assertThat(layoutReadyLatch.await(5, TimeUnit.SECONDS)).isTrue()

        val implementation = SurfaceViewImplementation(previewContainer, previewTransform)
        val surfaceRequest = createSurfaceRequest(hasCameraTransform = false, rotationDegrees = 0)
        surfaceRequest.setTransformationInfoListener(ContextCompat.getMainExecutor(mContext)) { info
            ->
            previewTransform.setTransformationInfo(
                info,
                surfaceRequest.resolution,
                /* useFrontCameraRotation= */ false,
            )
            implementation.redrawPreview()
        }
        val surface = implementation.provideSurface(surfaceRequest)
        drawQuadrants(surface)

        // Act & Assert: verify SurfaceFlinger composites each clockwise rotation and fits bounds.
        val baseQuadrants = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        for ((step, rotationDegrees) in listOf(0, 90, 180, 270).withIndex()) {
            surfaceRequest.updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                    Rect(0, 0, ANY_WIDTH, ANY_HEIGHT),
                    rotationDegrees,
                    Surface.ROTATION_0,
                    /* hasCameraTransform= */ false,
                    Matrix(),
                    /* mirroring= */ false,
                )
            )
            mInstrumentation.waitForIdleSync()
            // Each 90-degree clockwise step shifts [TL, TR, BR, BL] right by one position.
            val expectedQuadrants =
                List(baseQuadrants.size) { index ->
                    baseQuadrants[(index - step).mod(baseQuadrants.size)]
                }
            val fittedRect =
                if (rotationDegrees % 180 == 0) {
                    // 640x480 (4:3) fitted inside 240x320 -> 240x180 centered vertically.
                    Rect(0, 70, 240, 250)
                } else {
                    // Rotated 480x640 (3:4) fitted inside 240x320 -> full 240x320 bounds.
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
        surfaceRequest.deferrableSurface.close()
    }

    @Test
    fun waitForNextFrame_futureCompletesImmediately() {
        val future = mImplementation.waitForNextFrame()
        future.get(20, TimeUnit.MILLISECONDS)
    }

    @Throws(Throwable::class)
    private fun setContentView(view: View) {
        mActivityScenario.onActivity { activity -> activity.setContentView(view) }
    }

    private fun createSurfaceRequest(
        hasCameraTransform: Boolean,
        rotationDegrees: Int = 90,
    ): SurfaceRequest =
        SurfaceRequest(ANY_SIZE, FakeCamera()) {}
            .apply {
                mSurfaceRequests.add(this)
                updateTransformationInfo(
                    SurfaceRequest.TransformationInfo.of(
                        Rect(0, 0, ANY_WIDTH, ANY_HEIGHT),
                        rotationDegrees,
                        Surface.ROTATION_0,
                        hasCameraTransform,
                        Matrix(),
                        /* mirroring= */ false,
                    )
                )
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

    @RequiresApi(29)
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
        val frameLatch = CountDownLatch(1)
        mInstrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback { frameLatch.countDown() }
        }
        assertThat(frameLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @RequiresApi(29)
    private fun captureViewOnScreen(view: View, window: Window): Bitmap? {
        val location = IntArray(2)
        var width = 0
        var height = 0
        var useWindowScreenshot = false
        mInstrumentation.runOnMainSync {
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
                Api34Impl.takeScreenshot(mInstrumentation.uiAutomation, window)
            } else {
                mInstrumentation.uiAutomation.takeScreenshot()
            } ?: return null
        var cropped: Bitmap? = null
        try {
            val softwareScreenshot =
                if (rawScreenshot.config == Bitmap.Config.HARDWARE) {
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

    private fun SurfaceViewImplementation.provideSurface(surfaceRequest: SurfaceRequest): Surface {
        mInstrumentation.runOnMainSync { onSurfaceRequested(surfaceRequest, null) }
        return surfaceRequest.deferrableSurface.surface.get(1000, TimeUnit.MILLISECONDS)
    }

    private fun SurfaceViewImplementation.testSurfaceRequest(
        surfaceRequest: SurfaceRequest,
        listener: OnSurfaceNotInUseListener? = null,
    ) {
        mInstrumentation.runOnMainSync { onSurfaceRequested(surfaceRequest, listener) }

        surfaceRequest.deferrableSurface.surface.get(1000, TimeUnit.MILLISECONDS)
        surfaceRequest.deferrableSurface.close()
    }
}
