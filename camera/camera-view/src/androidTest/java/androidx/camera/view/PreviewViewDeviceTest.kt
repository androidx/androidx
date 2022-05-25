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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPoint
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.CoreAppTestUtil.ForegroundOccupiedError
import androidx.camera.testing.SurfaceFormatUtil
import androidx.camera.testing.fakes.FakeActivity
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.camera.view.internal.compat.quirk.DeviceQuirks
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk
import androidx.camera.view.test.R
import androidx.core.content.ContextCompat
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

/**
 * Instrumented tests for [PreviewView].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewViewDeviceTest {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<FakeActivity>? = null
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var cameraProvider: ProcessCameraProvider? = null
    private val surfaceRequestList: MutableList<SurfaceRequest> = ArrayList()
    private var mMeteringPointFactory: MeteringPointFactory? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Before
    @Throws(
        ForegroundOccupiedError::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        activityScenario = ActivityScenario.launch(FakeActivity::class.java)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
    }

    @After
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun tearDown() {
        for (surfaceRequest in surfaceRequestList) {
            surfaceRequest.willNotProvideSurface()
            // Ensure all successful requests have their returned future finish.
            surfaceRequest.deferrableSurface.close()
        }
        if (cameraProvider != null) {
            cameraProvider!!.shutdown()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun previewViewSetScaleType_controllerRebinds() {
        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val fitTypeSemaphore = Semaphore(0)
        val fakeController: CameraController = object : CameraController(context) {
            public override fun attachPreviewSurface(
                surfaceProvider: Preview.SurfaceProvider,
                viewPort: ViewPort,
                display: Display
            ) {
                if (viewPort.scaleType == ViewPort.FIT) {
                    fitTypeSemaphore.release()
                }
            }

            public override fun startCamera(): Camera? {
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
    @Throws(InterruptedException::class)
    fun receiveSurfaceRequest_transformIsValid() {
        // Arrange: set up PreviewView.
        val previewView = AtomicReference<PreviewView>()
        val countDownLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView.set(PreviewView(context))
            setContentView(previewView.get())
            // Feed the PreviewView with a fake SurfaceRequest
            val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
            previewView.get().surfaceProvider.onSurfaceRequested(
                createSurfaceRequest(cameraInfo)
            )
            notifyLatchWhenLayoutReady(previewView.get(), countDownLatch)
        }
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT)

        // Assert: OutputTransform is not null.
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            Truth.assertThat(previewView.get().outputTransform).isNotNull()
        }
    }

    @Test
    @Throws(InterruptedException::class)
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
        }
    }

    @Test
    @Throws(InterruptedException::class, UiObjectNotFoundException::class)
    fun previewViewPinched_pinchToZoomInvokedOnController() {
        // TODO(b/169058735): investigate and enable on Cuttlefish.
        Assume.assumeFalse(
            "Skip Cuttlefish until further investigation.",
            Build.MODEL.contains("Cuttlefish")
        )

        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val semaphore = Semaphore(0)
        val fakeController: CameraController = object : CameraController(context) {
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
    @Throws(InterruptedException::class, UiObjectNotFoundException::class)
    fun previewViewClicked_tapToFocusInvokedOnController() {
        // Arrange.
        val countDownLatch = CountDownLatch(1)
        val semaphore = Semaphore(0)
        val fakeController: CameraController = object : CameraController(context) {
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
        instrumentation.runOnMainSync {
            val previewView = PreviewView(context)
            previewView.controller = fakeController
            notifyLatchWhenLayoutReady(previewView, countDownLatch)
            setContentView(previewView)
        }
        // Wait for layout ready
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()

        // Act: click on PreviewView
        uiDevice.findObject(UiSelector().index(0)).click()

        // Assert: tap-to-focus is invoked.
        Truth.assertThat(semaphore.tryAcquire(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class, UiObjectNotFoundException::class)
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
    @UiThreadTest
    fun clearCameraController_controllerIsNull() {
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
    @UiThreadTest
    @Throws(InterruptedException::class)
    fun setNewCameraController_oldControllerIsCleared() {
        // Arrange.
        val previewView = PreviewView(context)
        setContentView(previewView)
        val previewClearSemaphore = Semaphore(0)
        val oldController: CameraController = object : CameraController(context) {
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
            previewClearSemaphore.tryAcquire(
                TIMEOUT_SECONDS.toLong(),
                TimeUnit.SECONDS
            )
        ).isTrue()
    }

    @Test
    @UiThreadTest
    fun usesTextureView_whenLegacyDevice() {
        val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)
        val previewView = PreviewView(context)
        setContentView(previewView)
        previewView.implementationMode = ImplementationMode.PERFORMANCE
        val surfaceProvider = previewView.surfaceProvider
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        Truth.assertThat(previewView.mImplementation).isInstanceOf(
            TextureViewImplementation::class.java
        )
    }

    @Test
    @UiThreadTest
    fun usesTextureView_whenAPILevelNotNewerThanN() {
        Assume.assumeTrue(Build.VERSION.SDK_INT <= 24)
        val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val previewView = PreviewView(context)
        setContentView(previewView)
        previewView.implementationMode = ImplementationMode.PERFORMANCE
        val surfaceProvider = previewView.surfaceProvider
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        Truth.assertThat(previewView.mImplementation).isInstanceOf(
            TextureViewImplementation::class.java
        )
    }

    @Test
    @UiThreadTest
    fun usesSurfaceView_whenNonLegacyDevice_andAPILevelNewerThanN() {
        Assume.assumeTrue(Build.VERSION.SDK_INT > 24)
        Assume.assumeTrue(
            DeviceQuirks.get(
                SurfaceViewStretchedQuirk::class.java
            ) == null
        )
        val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val previewView = PreviewView(context)
        setContentView(previewView)
        previewView.implementationMode = ImplementationMode.PERFORMANCE
        val surfaceProvider = previewView.surfaceProvider
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        Truth.assertThat(previewView.mImplementation).isInstanceOf(
            SurfaceViewImplementation::class.java
        )
    }

    @Test
    @UiThreadTest
    fun usesTextureView_whenNonLegacyDevice_andImplModeIsTextureView() {
        val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val previewView = PreviewView(context)
        setContentView(previewView)
        previewView.implementationMode = ImplementationMode.COMPATIBLE
        val surfaceProvider = previewView.surfaceProvider
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo))
        Truth.assertThat(previewView.mImplementation).isInstanceOf(
            TextureViewImplementation::class.java
        )
    }

    @Test
    @Throws(Throwable::class)
    fun correctSurfacePixelFormat_whenRGBA8888IsRequired() {
        val cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val surfaceRequest = createRgb8888SurfaceRequest(cameraInfo)
        val future = surfaceRequest.deferrableSurface.surface
        activityScenario!!.onActivity {
            val previewView = PreviewView(context)
            setContentView(previewView)
            previewView.implementationMode = ImplementationMode.PERFORMANCE
            val surfaceProvider = previewView.surfaceProvider
            surfaceProvider.onSurfaceRequested(surfaceRequest)
        }
        val surface = arrayOfNulls<Surface>(1)
        val countDownLatch = CountDownLatch(1)
        Futures.addCallback(future, object : FutureCallback<Surface?> {
            override fun onSuccess(result: Surface?) {
                surface[0] = result
                countDownLatch.countDown()
            }

            override fun onFailure(t: Throwable) {}
        }, CameraXExecutors.directExecutor())
        Truth.assertThat(countDownLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)).isTrue()
        Truth.assertThat(SurfaceFormatUtil.getSurfaceFormat(surface[0]))
            .isEqualTo(PixelFormat.RGBA_8888)
    }

    @Test
    @Throws(Exception::class)
    fun canCreateValidMeteringPoint() {
        val cameraInfo = createCameraInfo(
            90,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK
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
    @Throws(Exception::class)
    fun meteringPointFactoryAutoAdjusted_whenViewSizeChange() {
        val cameraInfo = createCameraInfo(
            90,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK
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

    @Throws(InterruptedException::class)
    private fun changeViewSize(previewView: PreviewView?, newWidth: Int, newHeight: Int) {
        val latchToWaitForLayoutChange = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView!!.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
            })
            previewView.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight)
        }

        // Wait until the new layout is changed.
        Truth.assertThat(
            latchToWaitForLayoutChange.await(
                TIMEOUT_SECONDS.toLong(),
                TimeUnit.SECONDS
            )
        ).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun meteringPointFactoryAutoAdjusted_whenScaleTypeChanged() {
        val cameraInfo = createCameraInfo(
            90,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK
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
        instrumentation.runOnMainSync {
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        val point1 = mMeteringPointFactory!!.createPoint(100f, 100f)
        instrumentation.runOnMainSync {
            previewView.scaleType = PreviewView.ScaleType.FIT_START
        }
        val point2 = mMeteringPointFactory!!.createPoint(100f, 100f)
        assertPointIsValid(point1)
        assertPointIsValid(point2)
        // These points should be different
        assertPointsAreDifferent(point1, point2)
    }

    @Test
    @Throws(Exception::class)
    fun meteringPointFactoryAutoAdjusted_whenTransformationInfoChanged() {
        val cameraInfo1 = createCameraInfo(
            90,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK
        )
        val cameraInfo2 = createCameraInfo(
            270,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_FRONT
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
        previewView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
        })
    }

    @Test
    @UiThreadTest
    fun meteringPointInvalid_whenPreviewViewWidthOrHeightIs0() {
        val cameraInfo = createCameraInfo(
            90,
            CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK
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

    private fun assertPointIsInvalid(point: MeteringPoint) {
        Truth.assertThat(point.x < 0f || point.x > 1.0f).isTrue()
        Truth.assertThat(point.y < 0f || point.y > 1.0f).isTrue()
    }

    @Test
    @UiThreadTest
    fun meteringPointInvalid_beforeCreatingSurfaceProvider() {
        val previewView = PreviewView(context)
        // make PreviewView.getWidth() getHeight not 0.
        setContentView(previewView)
        val factory = previewView.meteringPointFactory

        // verifying the factory only creates invalid points.
        val point = factory.createPoint(100f, 100f)
        assertPointIsInvalid(point)
    }

    @Test
    @UiThreadTest
    fun getsImplementationMode() {
        val previewView = PreviewView(context)
        previewView.implementationMode = ImplementationMode.PERFORMANCE
        Truth.assertThat(previewView.implementationMode).isEqualTo(ImplementationMode.PERFORMANCE)
    }

    @Test
    @UiThreadTest
    fun getsScaleTypeProgrammatically() {
        val previewView = PreviewView(context)
        previewView.scaleType = PreviewView.ScaleType.FIT_END
        Truth.assertThat(previewView.scaleType).isEqualTo(PreviewView.ScaleType.FIT_END)
    }

    @Test
    @UiThreadTest
    fun getsScaleTypeFromXMLLayout() {
        val previewView = LayoutInflater.from(context).inflate(
            R.layout.preview_view_scale_type_fit_end, null
        ) as PreviewView
        Truth.assertThat(previewView.scaleType).isEqualTo(PreviewView.ScaleType.FIT_END)
    }

    @Test
    @UiThreadTest
    fun defaultImplementationMode_isPerformance() {
        val previewView = PreviewView(context)
        Truth.assertThat(previewView.implementationMode).isEqualTo(ImplementationMode.PERFORMANCE)
    }

    @Test
    @UiThreadTest
    fun getsImplementationModeFromXmlLayout() {
        val previewView = LayoutInflater.from(context).inflate(
            R.layout.preview_view_implementation_mode_compatible, null
        ) as PreviewView
        Truth.assertThat(previewView.implementationMode).isEqualTo(ImplementationMode.COMPATIBLE)
    }

    @Test
    @UiThreadTest
    fun redrawsPreview_whenScaleTypeChanges() {
        val previewView = PreviewView(context)
        val implementation: PreviewViewImplementation = Mockito.mock(
            TestPreviewViewImplementation::class.java
        )
        previewView.mImplementation = implementation
        previewView.scaleType = PreviewView.ScaleType.FILL_START
        Mockito.verify(implementation, Mockito.times(1)).redrawPreview()
    }

    @Test
    fun redrawsPreview_whenLayoutResized() {
        val previewView = AtomicReference<PreviewView>()
        val container = AtomicReference<FrameLayout>()
        val implementation: PreviewViewImplementation = Mockito.mock(
            TestPreviewViewImplementation::class.java
        )
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
        val implementation: PreviewViewImplementation = Mockito.mock(
            TestPreviewViewImplementation::class.java
        )
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
        val implementation: PreviewViewImplementation = Mockito.mock(
            TestPreviewViewImplementation::class.java
        )
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
    @UiThreadTest
    fun setsDefaultBackground_whenBackgroundNotExplicitlySet() {
        val previewView = PreviewView(context)
        Truth.assertThat(previewView.background).isInstanceOf(
            ColorDrawable::class.java
        )
        val actualBackground = previewView.background as ColorDrawable
        val expectedBackground = ContextCompat.getColor(
            context,
            PreviewView.DEFAULT_BACKGROUND_COLOR
        )
        Truth.assertThat(actualBackground.color).isEqualTo(expectedBackground)
    }

    @Test
    @UiThreadTest
    fun overridesDefaultBackground_whenBackgroundExplicitlySet_programmatically() {
        val previewView = PreviewView(context)
        val backgroundColor = ContextCompat.getColor(context, android.R.color.white)
        previewView.setBackgroundColor(backgroundColor)
        Truth.assertThat(previewView.background).isInstanceOf(
            ColorDrawable::class.java
        )
        val actualBackground = previewView.background as ColorDrawable
        Truth.assertThat(actualBackground.color).isEqualTo(backgroundColor)
    }

    @Test
    @UiThreadTest
    fun overridesDefaultBackground_whenBackgroundExplicitlySet_xml() {
        val previewView = LayoutInflater.from(context).inflate(
            R.layout.preview_view_background_white, null
        ) as PreviewView
        Truth.assertThat(previewView.background).isInstanceOf(
            ColorDrawable::class.java
        )
        val actualBackground = previewView.background as ColorDrawable
        val expectedBackground = ContextCompat.getColor(context, android.R.color.white)
        Truth.assertThat(actualBackground.color).isEqualTo(expectedBackground)
    }

    @Test
    @UiThreadTest
    fun doNotRemovePreview_whenCreatingNewSurfaceProvider() {
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
            if (previewView.getChildAt(i) is TextureView ||
                previewView.getChildAt(i) is SurfaceView
            ) {
                wasPreviewRemoved = false
                break
            }
        }
        Truth.assertThat(wasPreviewRemoved).isFalse()
    }

    private fun setContentView(view: View?) {
        activityScenario!!.onActivity { activity: FakeActivity -> activity.setContentView(view) }
    }

    private fun createRgb8888SurfaceRequest(cameraInfo: CameraInfoInternal): SurfaceRequest {
        return createSurfaceRequest(cameraInfo, true)
    }

    private fun createSurfaceRequest(
        cameraInfo: CameraInfoInternal,
        isRGBA8888Required: Boolean = false
    ): SurfaceRequest {
        val fakeCamera = FakeCamera( /*cameraControl=*/null, cameraInfo)
        val surfaceRequest = SurfaceRequest(
            DEFAULT_SURFACE_SIZE, fakeCamera,
            isRGBA8888Required
        )
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
        val cameraInfoInternal = FakeCameraInfoInternal(
            rotationDegrees,
            lensFacing
        )
        cameraInfoInternal.implementationType = implementationType
        return cameraInfoInternal
    }

    private fun updateCropRectAndWaitForIdle(cropRect: Rect) {
        for (surfaceRequest in surfaceRequestList) {
            surfaceRequest.updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(cropRect, 0, Surface.ROTATION_0)
            )
        }
        instrumentation.waitForIdleSync()
    }

    /**
     * An empty implementation of [PreviewViewImplementation] used for testing. It allows
     * mocking [PreviewViewImplementation] since the latter is package private.
     */
    internal open class TestPreviewViewImplementation constructor(
        parent: FrameLayout,
        previewTransform: PreviewTransformation
    ) : PreviewViewImplementation(parent, previewTransform) {
        public override fun initializePreview() {}
        public override fun getPreview(): View? {
            return null
        }

        public override fun onSurfaceRequested(
            surfaceRequest: SurfaceRequest,
            onSurfaceNotInUseListener: OnSurfaceNotInUseListener?
        ) {
        }

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
    }
}