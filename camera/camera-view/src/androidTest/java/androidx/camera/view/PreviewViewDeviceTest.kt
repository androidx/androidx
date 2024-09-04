/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.GuardedBy
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.MeteringPoint
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.camera.view.internal.compat.quirk.DeviceQuirks
import androidx.camera.view.internal.compat.quirk.SurfaceViewNotCroppedByParentQuirk
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk
import androidx.camera.view.test.R
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

/** Instrumented tests for [PreviewView]. */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewViewDeviceTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<FakeActivity>? = null
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var cameraProvider: ProcessCameraProvider? = null
    private val surfaceRequestList: MutableList<SurfaceRequest> = ArrayList()
    private var mMeteringPointFactory: MeteringPointFactory? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Before
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        activityScenario = ActivityScenario.launch(FakeActivity::class.java)
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
    }

    @After
    fun tearDown() {
        for (surfaceRequest in surfaceRequestList) {
            surfaceRequest.willNotProvideSurface()
            // Ensure all successful requests have their returned future finish.
            surfaceRequest.deferrableSurface.close()
        }
        if (cameraProvider != null) {
            cameraProvider!!.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun previewViewSetScaleType_controllerRebinds() {
        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val fitTypeSemaphore = Semaphore(0)
        val fakeController: CameraController =
            object : CameraController(context) {
                override fun attachPreviewSurface(
                    surfaceProvider: Preview.SurfaceProvider,
                    viewPort: ViewPort
                ) {
                    if (viewPort.scaleType == ViewPort.FIT) {
                        fitTypeSemaphore.release()
                    }
                }

                override fun startCamera(): Camera? {
                    return null
                }
            }
        val previewViewAtomicReference = AtomicReference<PreviewView>()
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewViewAtomicReference.set(previewView)
            previewView.implementationMode = ImplementationMode.COMPATIBLE
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()

        // Act: set controller then change the scale type.
        instrumentation.runOnMainSync {
            previewViewAtomicReference.get().controller = fakeController
            previewViewAtomicReference.get().scaleType = PreviewView.ScaleType.FIT_CENTER
        }

        // Assert: cameraController receives a fit type ViewPort.
        Truth.assertThat(fitTypeSemaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
            .isTrue()
    }

    @Test
    fun receiveSurfaceRequest_transformIsValid() {
        // Arrange: set up PreviewView.
        val previewView = AtomicReference<PreviewView>()
        val countDownLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView.set(PreviewView(context))
            setContentView(previewView.get())
            // Feed the PreviewView with a fake SurfaceRequest
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            previewView.get().surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            notifyLatchWhenLayoutReady(previewView.get(), countDownLatch)
        }
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)

        // Assert: OutputTransform is not null.
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            Truth.assertThat(previewView.get().outputTransform).isNotNull()
            Truth.assertThat(previewView.get().sensorToViewTransform).isNotNull()
        }
    }

    @Test
    fun noSurfaceRequest_transformIsInvalid() {
        // Arrange: set up PreviewView.
        val previewView = AtomicReference<PreviewView>()
        val countDownLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView.set(PreviewView(context))
            setContentView(previewView.get())
            notifyLatchWhenLayoutReady(previewView.get(), countDownLatch)
        }

        // Assert: OutputTransform is null.
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            Truth.assertThat(previewView.get().outputTransform).isNull()
            Truth.assertThat(previewView.get().sensorToViewTransform).isNull()
        }
    }

    @Test
    fun previewViewPinched_pinchToZoomInvokedOnController() {
        // TODO(b/169058735): investigate and enable on Cuttlefish.
        Assume.assumeFalse(
            "Skip Cuttlefish until further investigation.",
            Build.MODEL.contains("Cuttlefish")
        )

        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val semaphore = Semaphore(0)
        val fakeController: CameraController =
            object : CameraController(context) {
                public override fun onPinchToZoom(pinchToZoomScale: Float) {
                    semaphore.release()
                }

                public override fun startCamera(): Camera? {
                    return null
                }
            }
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewView.controller = fakeController
            previewView.implementationMode = ImplementationMode.COMPATIBLE
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()

        // Act: pinch-in 80% in 100 steps.
        uiDevice.findObject(UiSelector().index(0)).pinchIn(80, 100)

        // Assert: pinch-to-zoom is called.
        Truth.assertThat(semaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun previewViewClicked_tapToFocusInvokedOnController() {
        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val semaphore = Semaphore(0)
        val fakeController: CameraController =
            object : CameraController(context) {
                public override fun onTapToFocus(
                    meteringPointFactory: MeteringPointFactory,
                    x: Float,
                    y: Float
                ) {
                    semaphore.release()
                }

                public override fun startCamera(): Camera? {
                    return null
                }
            }

        var clickEventHelper: ClickEventHelper? = null

        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            // Specifies the content description and uses it to find the view to click
            previewView.contentDescription = previewView.hashCode().toString()
            clickEventHelper = ClickEventHelper(previewView)
            previewView.setOnTouchListener(clickEventHelper)
            previewView.controller = fakeController
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()

        // Act: click on PreviewView
        clickEventHelper!!.performSingleClick(uiDevice, 3)

        // Assert: tap-to-focus is invoked.
        Truth.assertThat(semaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    /**
     * A helper to perform single click
     *
     * Some devices might have incorrect [MotionEvent#getEventTime()] and
     * [MotionEvent#getDownTime()] info when issuing click event via UiDevice in the first time. The
     * system time only occupies around 100ms but the event time difference is around 1000ms and
     * then the click event is incorrectly recognized as long-click. The issue is caused by RPC
     * operation handling timing issue. The issue causes [PreviewView] to ignore the click operation
     * and then onTapToFocus event can't be received. The issue might be recovered in the new
     * UiDevice click actions.
     *
     * This helper will help to perform the click operation and monitor the motion events to retry
     * if a long click result is detected.
     */
    private class ClickEventHelper constructor(private val targetView: View) :
        View.OnTouchListener {
        private val lock = Any()

        @GuardedBy("lock") private var isPerformingClick = false
        private var uiDevice: UiDevice? = null
        private var limitedRetryCount = 0
        private var retriedCounter = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (view != targetView) {
                return false
            }

            if (event.action == MotionEvent.ACTION_UP) {
                val longPressTimeout = ViewConfiguration.getLongPressTimeout()

                // Retries the click action if the UP event forms a incorrect long click operation
                // and retry counter is still under the limited retry count.
                if (
                    event.eventTime - event.downTime > longPressTimeout &&
                        retriedCounter < limitedRetryCount
                ) {
                    retriedCounter++
                    performSingleClickInternal()
                    return false
                }

                // Resets member variables if incorrect long click operation is not detected or
                // retry is not allowed any more.
                uiDevice = null
                limitedRetryCount = 0
                retriedCounter = 0
                synchronized(lock) { isPerformingClick = false }
            }

            return false
        }

        /**
         * Perform single click action with UiDevice and will retry with the specified count when
         * incorrect long click operation is detected.
         *
         * New single click request will be ignored if the previous request is still performing.
         */
        fun performSingleClick(uiDevice: UiDevice, retryCount: Int = 0) {
            synchronized(lock) {
                if (isPerformingClick) {
                    return
                } else {
                    isPerformingClick = true
                }
            }

            limitedRetryCount = retryCount
            retriedCounter = 0
            this.uiDevice = uiDevice
            performSingleClickInternal()
        }

        private fun performSingleClickInternal() =
            uiDevice!!
                .findObject(UiSelector().descriptionContains(targetView.hashCode().toString()))
                .click()
    }

    @Test
    fun previewView_onClickListenerWorks() {
        // Arrange.
        val semaphore = Semaphore(0)
        val countDownLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewView.setOnClickListener { semaphore.release() }
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()

        // Act: click on PreviewView.
        uiDevice.findObject(UiSelector().index(0)).click()

        // Assert: view is clicked.
        Truth.assertThat(semaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun clearCameraController_controllerIsNull() =
        instrumentation.runOnMainSync {
            // Arrange.
            val previewView = PreviewView(context)
            setContentView(previewView)
            val cameraController: CameraController = LifecycleCameraController(context)
            previewView.controller = cameraController
            Truth.assertThat(previewView.controller).isEqualTo(cameraController)

            // Act and Assert.
            previewView.controller = null

            // Assert
            Truth.assertThat(previewView.controller).isNull()
        }

    @Test
    fun setNewCameraController_oldControllerIsCleared() {
        instrumentation.runOnMainSync {
            // Arrange.
            val previewView = PreviewView(context)
            setContentView(previewView)
            val previewClearSemaphore = Semaphore(0)
            val oldController: CameraController =
                object : CameraController(context) {
                    public override fun startCamera(): Camera? {
                        return null
                    }

                    public override fun clearPreviewSurface() {
                        previewClearSemaphore.release()
                    }
                }
            previewView.controller = oldController
            Truth.assertThat(previewView.controller).isEqualTo(oldController)
            val newController: CameraController = LifecycleCameraController(context)

            // Act and Assert.
            previewView.controller = newController

            // Assert
            Truth.assertThat(previewView.controller).isEqualTo(newController)
            Truth.assertThat(
                    previewClearSemaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                )
                .isTrue()
        }
    }

    @Test
    fun usesTextureView_whenLegacyDevice() {
        instrumentation.runOnMainSync {
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)
            val previewView = PreviewView(context)
            setContentView(previewView)
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            Truth.assertThat(previewView.mImplementation)
                .isInstanceOf(TextureViewImplementation::class.java)
        }
    }

    @Test
    fun usesTextureView_whenAPILevelNotNewerThanN() {
        instrumentation.runOnMainSync {
            Assume.assumeTrue(Build.VERSION.SDK_INT <= 24)
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            val previewView = PreviewView(context)
            setContentView(previewView)
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            Truth.assertThat(previewView.mImplementation)
                .isInstanceOf(TextureViewImplementation::class.java)
        }
    }

    @Test
    fun usesSurfaceView_whenNonLegacyDevice_andAPILevelNewerThanN() {
        instrumentation.runOnMainSync {
            Assume.assumeTrue(Build.VERSION.SDK_INT > 24)
            Assume.assumeFalse(hasSurfaceViewQuirk())
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            val previewView = PreviewView(context)
            setContentView(previewView)
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            Truth.assertThat(previewView.mImplementation)
                .isInstanceOf(SurfaceViewImplementation::class.java)
        }
    }

    @Test
    fun usesTextureView_whenNonLegacyDevice_andImplModeIsTextureView() {
        instrumentation.runOnMainSync {
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            val previewView = PreviewView(context)
            setContentView(previewView)
            previewView.implementationMode = ImplementationMode.COMPATIBLE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            Truth.assertThat(previewView.mImplementation)
                .isInstanceOf(TextureViewImplementation::class.java)
        }
    }

    @Test
    fun reuseImpl_whenImplModeIsSurfaceView_andSurfaceRequestCompatibleWithSurfaceView() {
        instrumentation.runOnMainSync {
            // Arrange.
            Assume.assumeTrue(Build.VERSION.SDK_INT > 24)
            Assume.assumeFalse(hasSurfaceViewQuirk())
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            val previewView = PreviewView(context)
            setContentView(previewView)

            // Act.
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            val previousImplementation = previewView.mImplementation
            assertThat(previousImplementation).isInstanceOf(SurfaceViewImplementation::class.java)
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))

            // Assert.
            val newImplementation = previewView.mImplementation
            assertThat(newImplementation).isEqualTo(previousImplementation)
        }
    }

    @Test
    fun notReuseImpl_whenImplIsTextureView() {
        instrumentation.runOnMainSync {
            // Arrange.
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            val previewView = PreviewView(context)
            setContentView(previewView)

            // Act.
            previewView.implementationMode = ImplementationMode.COMPATIBLE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            val previousImplementation = previewView.mImplementation
            assertThat(previousImplementation).isInstanceOf(TextureViewImplementation::class.java)
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))

            // Assert.
            val newImplementation = previewView.mImplementation
            assertThat(newImplementation).isNotEqualTo(previousImplementation)
        }
    }

    @Test
    fun canCreateValidMeteringPoint() {
        val cameraInfo =
            createCameraInfo(
                90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                CameraSelector.LENS_FACING_BACK
            )
        val countDownLatch = CountDownLatch(1)
        lateinit var previewView: PreviewView
        instrumentation.runOnMainSync {
            previewView = PreviewView(context)
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)
        instrumentation.runOnMainSync {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(100f, 100f)
            assertPointIsValid(point)
        }
    }

    private fun assertPointIsValid(point: MeteringPoint) {
        Truth.assertThat(point.x in 0f..1.0f).isTrue()
        Truth.assertThat(point.y in 0f..1.0f).isTrue()
    }

    @Test
    fun meteringPointFactoryAutoAdjusted_whenViewSizeChange() {
        val cameraInfo =
            createCameraInfo(
                90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                CameraSelector.LENS_FACING_BACK
            )
        lateinit var previewView: PreviewView
        instrumentation.runOnMainSync {
            previewView = PreviewView(context)
            mMeteringPointFactory = previewView.meteringPointFactory
            setContentView(previewView)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        }
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)
        changeViewSize(previewView, 1000, 1000)
        val point1 = mMeteringPointFactory!!.createPoint(100f, 100f)
        changeViewSize(previewView, 500, 400)
        val point2 = mMeteringPointFactory!!.createPoint(100f, 100f)
        assertPointIsValid(point1)
        assertPointIsValid(point2)
        // These points should be different because the layout is changed.
        assertPointsAreDifferent(point1, point2)
    }

    private fun changeViewSize(previewView: PreviewView?, newWidth: Int, newHeight: Int) {
        val latchToWaitForLayoutChange = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView!!.addOnLayoutChangeListener(
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        if (previewView.width == newWidth && previewView.height == newHeight) {
                            latchToWaitForLayoutChange.countDown()
                            previewView.removeOnLayoutChangeListener(this)
                        }
                    }
                }
            )
            previewView.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight)
        }

        // Wait until the new layout is changed.
        Truth.assertThat(
                latchToWaitForLayoutChange.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            )
            .isTrue()
    }

    @Test
    fun meteringPointFactoryAutoAdjusted_whenScaleTypeChanged() {
        val cameraInfo =
            createCameraInfo(
                90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                CameraSelector.LENS_FACING_BACK
            )
        lateinit var previewView: PreviewView
        instrumentation.runOnMainSync {
            previewView = PreviewView(context)
            mMeteringPointFactory = previewView.meteringPointFactory
            setContentView(previewView)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        }
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)

        // Surface resolution is 640x480 , set a different size for PreviewView.
        changeViewSize(previewView, 800, 700)
        instrumentation.runOnMainSync { previewView.scaleType = PreviewView.ScaleType.FILL_CENTER }
        val point1 = mMeteringPointFactory!!.createPoint(100f, 100f)
        instrumentation.runOnMainSync { previewView.scaleType = PreviewView.ScaleType.FIT_START }
        val point2 = mMeteringPointFactory!!.createPoint(100f, 100f)
        assertPointIsValid(point1)
        assertPointIsValid(point2)
        // These points should be different
        assertPointsAreDifferent(point1, point2)
    }

    @Test
    fun meteringPointFactoryAutoAdjusted_whenTransformationInfoChanged() {
        val cameraInfo1 =
            createCameraInfo(
                90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                CameraSelector.LENS_FACING_BACK
            )
        val cameraInfo2 =
            createCameraInfo(
                270,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                CameraSelector.LENS_FACING_FRONT
            )

        lateinit var previewView: PreviewView
        instrumentation.runOnMainSync {
            previewView = PreviewView(context)
            mMeteringPointFactory = previewView.meteringPointFactory
            setContentView(previewView)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo1))
        }
        changeViewSize(previewView, 1000, 1000)
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)

        // get a MeteringPoint from a non-center point.
        val point1 = mMeteringPointFactory!!.createPoint(100f, 120f)
        instrumentation.runOnMainSync {
            setContentView(previewView)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo2))
        }
        updateCropRectAndWaitForIdle(SMALLER_CROP_RECT)
        val point2 = mMeteringPointFactory!!.createPoint(100f, 120f)
        assertPointIsValid(point1)
        assertPointIsValid(point2)
        // These points should be different
        assertPointsAreDifferent(point1, point2)
    }

    private fun assertPointsAreDifferent(point1: MeteringPoint, point2: MeteringPoint) {
        Truth.assertThat(point1.x != point2.x || point1.y != point2.y).isTrue()
    }

    private fun notifyLatchWhenLayoutReady(
        previewView: PreviewView,
        countDownLatch: CountDownLatch
    ) {
        previewView.addOnLayoutChangeListener(
            object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    if (v.width > 0 && v.height > 0) {
                        countDownLatch.countDown()
                        previewView.removeOnLayoutChangeListener(this)
                    }
                }
            }
        )
    }

    @Test
    fun meteringPointInvalid_whenPreviewViewWidthOrHeightIs0() {
        instrumentation.runOnMainSync {
            val cameraInfo =
                createCameraInfo(
                    90,
                    CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                    CameraSelector.LENS_FACING_BACK
                )
            val previewView = PreviewView(context)
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
            val factory = previewView.meteringPointFactory

            // Width and height is 0,  but surface is requested,
            // verifying the factory only creates invalid points.
            val point = factory.createPoint(100f, 100f)
            assertPointIsInvalid(point)
        }
    }

    private fun assertPointIsInvalid(point: MeteringPoint) {
        Truth.assertThat(point.x < 0f || point.x > 1.0f).isTrue()
        Truth.assertThat(point.y < 0f || point.y > 1.0f).isTrue()
    }

    @Test
    fun meteringPointInvalid_beforeCreatingSurfaceProvider() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            // make PreviewView.getWidth() getHeight not 0.
            setContentView(previewView)
            val factory = previewView.meteringPointFactory

            // verifying the factory only creates invalid points.
            val point = factory.createPoint(100f, 100f)
            assertPointIsInvalid(point)
        }
    }

    @Test
    fun getsImplementationMode() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            Truth.assertThat(previewView.implementationMode)
                .isEqualTo(ImplementationMode.PERFORMANCE)
        }
    }

    @Test
    fun getsScaleTypeProgrammatically() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewView.scaleType = PreviewView.ScaleType.FIT_END
            Truth.assertThat(previewView.scaleType).isEqualTo(PreviewView.ScaleType.FIT_END)
        }
    }

    @Test
    fun getsScaleTypeFromXMLLayout() {
        instrumentation.runOnMainSync {
            val previewView =
                LayoutInflater.from(context).inflate(R.layout.preview_view_scale_type_fit_end, null)
                    as PreviewView
            Truth.assertThat(previewView.scaleType).isEqualTo(PreviewView.ScaleType.FIT_END)
        }
    }

    @Test
    fun defaultImplementationMode_isPerformance() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            Truth.assertThat(previewView.implementationMode)
                .isEqualTo(ImplementationMode.PERFORMANCE)
        }
    }

    @Test
    fun getsImplementationModeFromXmlLayout() {
        instrumentation.runOnMainSync {
            val previewView =
                LayoutInflater.from(context)
                    .inflate(R.layout.preview_view_implementation_mode_compatible, null)
                    as PreviewView
            Truth.assertThat(previewView.implementationMode)
                .isEqualTo(ImplementationMode.COMPATIBLE)
        }
    }

    @Test
    fun redrawsPreview_whenScaleTypeChanges() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            val implementation: PreviewViewImplementation =
                Mockito.mock(TestPreviewViewImplementation::class.java)
            previewView.mImplementation = implementation
            previewView.scaleType = PreviewView.ScaleType.FILL_START
            Mockito.verify(implementation, Mockito.times(1)).redrawPreview()
        }
    }

    @Test
    fun redrawsPreview_whenLayoutResized() {
        val previewView = AtomicReference<PreviewView>()
        val container = AtomicReference<FrameLayout>()
        val implementation: PreviewViewImplementation =
            Mockito.mock(TestPreviewViewImplementation::class.java)
        activityScenario!!.onActivity {
            previewView.set(PreviewView(context))
            previewView.get().mImplementation = implementation
            container.set(FrameLayout(context))
            container.get().addView(previewView.get())
            setContentView(container.get())

            // Resize container in order to trigger PreviewView's onLayoutChanged listener.
            val params = container.get().layoutParams as FrameLayout.LayoutParams
            params.width = params.width / 2
            container.get().requestLayout()
        }
        Mockito.verify(implementation, Mockito.timeout(1000).times(1)).redrawPreview()
    }

    @Test
    fun doesNotRedrawPreview_whenDetachedFromWindow() {
        val previewView = AtomicReference<PreviewView>()
        val container = AtomicReference<FrameLayout>()
        val implementation: PreviewViewImplementation =
            Mockito.mock(TestPreviewViewImplementation::class.java)
        activityScenario!!.onActivity {
            previewView.set(PreviewView(context))
            previewView.get().mImplementation = implementation
            container.set(FrameLayout(context))
            container.get().addView(previewView.get())
            setContentView(container.get())
            container.get().removeView(previewView.get())

            // Resize container
            val params = container.get().layoutParams as FrameLayout.LayoutParams
            params.width = params.width / 2
            container.get().requestLayout()
        }
        Mockito.verify(implementation, Mockito.never()).redrawPreview()
    }

    @Test
    fun redrawsPreview_whenReattachedToWindow() {
        val previewView = AtomicReference<PreviewView>()
        val container = AtomicReference<FrameLayout>()
        val implementation: PreviewViewImplementation =
            Mockito.mock(TestPreviewViewImplementation::class.java)
        activityScenario!!.onActivity {
            previewView.set(PreviewView(context))
            previewView.get().mImplementation = implementation
            container.set(FrameLayout(context))
            container.get().addView(previewView.get())
            setContentView(container.get())
            container.get().removeView(previewView.get())
            container.get().addView(previewView.get())
        }
        Mockito.verify(implementation, Mockito.timeout(1000).times(1)).redrawPreview()
    }

    @Test
    fun setsDefaultBackground_whenBackgroundNotExplicitlySet() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            Truth.assertThat(previewView.background).isInstanceOf(ColorDrawable::class.java)
            val actualBackground = previewView.background as ColorDrawable
            val expectedBackground =
                ContextCompat.getColor(context, PreviewView.DEFAULT_BACKGROUND_COLOR)
            Truth.assertThat(actualBackground.color).isEqualTo(expectedBackground)
        }
    }

    @Test
    fun overridesDefaultBackground_whenBackgroundExplicitlySet_programmatically() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            val backgroundColor = ContextCompat.getColor(context, android.R.color.white)
            previewView.setBackgroundColor(backgroundColor)
            Truth.assertThat(previewView.background).isInstanceOf(ColorDrawable::class.java)
            val actualBackground = previewView.background as ColorDrawable
            Truth.assertThat(actualBackground.color).isEqualTo(backgroundColor)
        }
    }

    @Test
    fun overridesDefaultBackground_whenBackgroundExplicitlySet_xml() {
        instrumentation.runOnMainSync {
            val previewView =
                LayoutInflater.from(context).inflate(R.layout.preview_view_background_white, null)
                    as PreviewView
            Truth.assertThat(previewView.background).isInstanceOf(ColorDrawable::class.java)
            val actualBackground = previewView.background as ColorDrawable
            val expectedBackground = ContextCompat.getColor(context, android.R.color.white)
            Truth.assertThat(actualBackground.color).isEqualTo(expectedBackground)
        }
    }

    @Test
    fun doNotRemovePreview_whenCreatingNewSurfaceProvider() {
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            setContentView(previewView)

            // Start a preview stream
            val surfaceProvider = previewView.surfaceProvider
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))

            // Create a new surfaceProvider
            previewView.surfaceProvider

            // Assert PreviewView doesn't remove the current preview TextureView/SurfaceView
            var wasPreviewRemoved = true
            for (i in 0 until previewView.childCount) {
                if (
                    previewView.getChildAt(i) is TextureView ||
                        previewView.getChildAt(i) is SurfaceView
                ) {
                    wasPreviewRemoved = false
                    break
                }
            }
            Truth.assertThat(wasPreviewRemoved).isFalse()
        }
    }

    @Test
    fun canSetFrameUpdateListener() {
        lateinit var previewView: PreviewView
        activityScenario!!.onActivity { activity ->
            previewView = PreviewView(context)
            previewView.implementationMode = ImplementationMode.COMPATIBLE
            activity.setContentView(previewView)
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider!!.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }

        var executedOnExecutor = false
        val executor = Executor {
            it.run()
            executedOnExecutor = true
        }

        val frameUpdateCountDownLatch = CountDownLatch(5)
        previewView.setFrameUpdateListener(executor) { frameUpdateCountDownLatch.countDown() }

        assertThat(frameUpdateCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executedOnExecutor).isTrue()
    }

    private fun setContentView(view: View?) {
        activityScenario!!.onActivity { activity: FakeActivity -> activity.setContentView(view) }
    }

    private fun createSurfaceRequest(
        cameraInfo: CameraInfoInternal,
    ): SurfaceRequest {
        val fakeCamera = FakeCamera(/* cameraControl= */ null, cameraInfo)
        val surfaceRequest = SurfaceRequest(DEFAULT_SURFACE_SIZE, fakeCamera) {}
        surfaceRequestList.add(surfaceRequest)
        return surfaceRequest
    }

    private fun createCameraInfo(implementationType: String): CameraInfoInternal {
        val cameraInfoInternal = FakeCameraInfoInternal()
        cameraInfoInternal.implementationType = implementationType
        return cameraInfoInternal
    }

    private fun createCameraInfo(
        rotationDegrees: Int,
        implementationType: String,
        @CameraSelector.LensFacing lensFacing: Int
    ): CameraInfoInternal {
        val cameraInfoInternal = FakeCameraInfoInternal(rotationDegrees, lensFacing)
        cameraInfoInternal.implementationType = implementationType
        return cameraInfoInternal
    }

    private fun updateCropRectAndWaitForIdle(cropRect: Rect) {
        for (surfaceRequest in surfaceRequestList) {
            surfaceRequest.updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                    cropRect,
                    0,
                    Surface.ROTATION_0,
                    /*hasCameraTransform=*/ true,
                    /*sensorToBufferTransform=*/ Matrix(),
                    /*mirroring=*/ false
                )
            )
        }
        instrumentation.waitForIdleSync()
    }

    private fun hasSurfaceViewQuirk(): Boolean {
        return DeviceQuirks.get(SurfaceViewStretchedQuirk::class.java) != null ||
            DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk::class.java) != null
    }

    /**
     * An empty implementation of [PreviewViewImplementation] used for testing. It allows mocking
     * [PreviewViewImplementation] since the latter is package private.
     */
    internal open class TestPreviewViewImplementation
    constructor(parent: FrameLayout, previewTransform: PreviewTransformation) :
        PreviewViewImplementation(parent, previewTransform) {
        override fun initializePreview() {}

        override fun getPreview(): View? {
            return null
        }

        override fun onSurfaceRequested(
            surfaceRequest: SurfaceRequest,
            onSurfaceNotInUseListener: OnSurfaceNotInUseListener?
        ) {}

        public override fun redrawPreview() {}

        public override fun onAttachedToWindow() {}

        public override fun onDetachedFromWindow() {}

        public override fun waitForNextFrame(): ListenableFuture<Void> {
            return Futures.immediateFuture(null)
        }

        public override fun getPreviewBitmap(): Bitmap? {
            return null
        }
    }

    companion object {
        private val DEFAULT_SURFACE_SIZE: Size by lazy { Size(640, 480) }
        private val DEFAULT_CROP_RECT = Rect(0, 0, 640, 480)
        private val SMALLER_CROP_RECT = Rect(20, 20, 600, 400)
        private const val TIMEOUT_SECONDS = 10

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }
}
