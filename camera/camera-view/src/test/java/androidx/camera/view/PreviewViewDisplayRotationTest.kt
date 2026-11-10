/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.widget.FrameLayout
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CompositionSettings
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplay
import org.robolectric.shadows.ShadowDisplayManager

/** Robolectric unit tests for layout-synchronized display rotation in [PreviewView]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class PreviewViewDisplayRotationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val defaultDisplay: Display = ShadowDisplay.getDefaultDisplay()
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()

    private val processorsToCleanUp = mutableListOf<FakeSurfaceProcessorInternal>()
    private val unbindActions = mutableListOf<() -> Unit>()
    private var activityController: ActivityController<Activity>? = null
    private var presentationToDismiss: Presentation? = null
    private val addedDisplayIds = mutableListOf<Int>()
    private lateinit var savedProcessorSupplier: Any

    @Before
    fun setUp() {
        // DefaultSurfaceProcessor(DynamicRange) is package-private to
        // androidx.camera.core.processing,
        // so save and restore the static Factory.sSupplier via reflection to isolate test state.
        val supplierField =
            DefaultSurfaceProcessor.Factory::class.java.getDeclaredField("sSupplier")
        supplierField.isAccessible = true
        savedProcessorSupplier = checkNotNull(supplierField.get(null))
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
    }

    @After
    fun tearDown() {
        try {
            for (unbind in unbindActions.asReversed()) {
                unbind()
            }
            unbindActions.clear()
            for (processor in processorsToCleanUp) {
                processor.release()
                processor.cleanUp()
            }
            processorsToCleanUp.clear()
        } finally {
            val supplierField =
                DefaultSurfaceProcessor.Factory::class.java.getDeclaredField("sSupplier")
            supplierField.isAccessible = true
            supplierField.set(null, savedProcessorSupplier)
            presentationToDismiss?.dismiss()
            presentationToDismiss = null
            activityController?.pause()?.stop()?.destroy()
            activityController = null
            for (displayId in addedDisplayIds) {
                ShadowDisplayManager.removeDisplay(displayId)
            }
            addedDisplayIds.clear()
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            shadowOf(getMainLooper()).idle()
        }
    }

    @Test
    fun quarterTurnDisplayChange_heldBackUntilLayoutSizeChanges() {
        val (container, previewView) = createAttachedPreviewView(width = 100, height = 200)
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val surfaceView = previewView.provideSurfaceViewSurface()

        val portraitTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val portraitScaleX = surfaceView.scaleX

        // Early 90-degree DisplayListener callback before window/view relayout must hold back
        // PreviewView's rendered transform, while public getViewPort() reflects display rotation.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()

        assertThat(previewView.viewPort?.rotation).isEqualTo(Surface.ROTATION_90)
        assertThat(previewView.sensorToViewTransform).isEqualTo(portraitTransform)
        assertThat(surfaceView.scaleX).isEqualTo(portraitScaleX)

        // Even an explicit redraw while still at portrait 100x200 bounds keeps ROTATION_0.
        previewView.scaleType = PreviewView.ScaleType.FILL_START
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        assertThat(previewView.sensorToViewTransform).isEqualTo(portraitTransform)
        assertThat(surfaceView.scaleX).isEqualTo(portraitScaleX)

        // Once layout bounds swap to landscape 200x100, ROTATION_90 is accepted synchronously.
        resizePreviewView(container, previewView, width = 200, height = 100)

        assertThat(previewView.viewPort?.rotation).isEqualTo(Surface.ROTATION_90)
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(portraitTransform)
    }

    @Test
    fun quarterTurnDisplayChange_acceptedOnConfigurationChangedForSquareView() {
        val (_, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()

        val initialTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val initialCorrection = textureView.getTransform(null)

        // DisplayListener fires first; held back while awaiting layout/configuration update.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()
        assertThat(previewView.sensorToViewTransform).isEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isEqualTo(initialCorrection)

        // Configuration change schedules a layout pass; even with unchanged 200x200 bounds,
        // ROTATION_90 is accepted on that layout pass.
        val landscapeConfig =
            Configuration(context.resources.configuration).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            }
        previewView.dispatchConfigurationChanged(landscapeConfig)

        // The configuration change only schedules the layout pass, so ROTATION_90 is not applied
        // synchronously ahead of it.
        assertThat(previewView.isLayoutRequested).isTrue()
        assertThat(previewView.sensorToViewTransform).isEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isEqualTo(initialCorrection)

        shadowOf(getMainLooper()).idle()

        assertThat(previewView.width).isEqualTo(200)
        assertThat(previewView.height).isEqualTo(200)
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isNotEqualTo(initialCorrection)
    }

    @Test
    fun quarterTurnDisplayChange_configurationChangedBeforeResizeHoldsRotationUntilLayout() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 100,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()
        val portraitTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val portraitCorrection = textureView.getTransform(null)

        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()

        // onConfigurationChanged is dispatched while PreviewView still has its portrait 100x200
        // bounds. Applying ROTATION_90 now would stretch the preview until the relayout.
        val landscapeConfig =
            Configuration(context.resources.configuration).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            }
        previewView.dispatchConfigurationChanged(landscapeConfig)

        assertThat(previewView.isLayoutRequested).isTrue()
        assertThat(previewView.width).isEqualTo(100)
        assertThat(previewView.height).isEqualTo(200)
        assertThat(previewView.sensorToViewTransform).isEqualTo(portraitTransform)
        assertThat(textureView.getTransform(null)).isEqualTo(portraitCorrection)

        // The relayout to landscape 200x100 bounds applies ROTATION_90 together with them.
        resizePreviewView(container, previewView, width = 200, height = 100)

        assertThat(previewView.width).isEqualTo(200)
        assertThat(previewView.height).isEqualTo(100)
        val expectedTransform = createReferenceTransformation(Surface.ROTATION_90)
        assertMatrixClose(
            checkNotNull(previewView.sensorToViewTransform),
            checkNotNull(expectedTransform.getSensorToViewTransform(Size(200, 100), 0)),
        )
        assertThat(textureView.getTransform(null)).isNotEqualTo(portraitCorrection)
    }

    @Test
    fun halfTurnDisplayChange_acceptedImmediatelyAndFlipsBackWithoutLayoutChange() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        val (_, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 100,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)

        val textureView = previewView.provideTextureViewSurface()
        val transformAt90 = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val textureCorrectionAt90 = textureView.getTransform(null)
        assertThat(textureCorrectionAt90.isIdentity).isFalse()

        // 180-degree flip (90 -> 270) does not trigger configuration or size change, so
        // DisplayListener must update immediately.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270)
        shadowOf(getMainLooper()).idle()

        assertThat(previewView.sensorToViewTransform).isNotEqualTo(transformAt90)
        assertThat(textureView.getTransform(null)).isNotEqualTo(textureCorrectionAt90)

        // Flipping 180 degrees back (270 -> 90) also updates immediately.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()

        assertThat(previewView.sensorToViewTransform).isEqualTo(transformAt90)
        assertThat(textureView.getTransform(null)).isEqualTo(textureCorrectionAt90)
    }

    @Test
    fun getViewPortDuringHalfTurn_doesNotSwallowPreviewRedraw() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        lateinit var previewView: PreviewView
        var listenerViewPortRotation = -1

        // Register an app DisplayListener BEFORE PreviewView attaches so it runs first.
        val appListener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {
                    listenerViewPortRotation = previewView.viewPort?.rotation ?: -1
                }
            }
        displayManager.registerDisplayListener(appListener, Handler(getMainLooper()))
        unbindActions.add { displayManager.unregisterDisplayListener(appListener) }

        val pair =
            createAttachedPreviewView(
                width = 200,
                height = 100,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        previewView = pair.second
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()

        val transformAt90 = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val correctionAt90 = textureView.getTransform(null)

        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270)
        shadowOf(getMainLooper()).idle()

        assertThat(listenerViewPortRotation).isEqualTo(Surface.ROTATION_270)
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(transformAt90)
        assertThat(textureView.getTransform(null)).isNotEqualTo(correctionAt90)
    }

    @Test
    fun externalDisplay_quarterTurnOnDefaultDisplayAcceptedImmediately() {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val externalDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")
        addedDisplayIds.add(externalDisplayId)
        val externalDisplay = displayManager.getDisplay(externalDisplayId)
        shadowOf(externalDisplay).setRotation(Surface.ROTATION_0)
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)

        val (_, attachedPreviewView) =
            createAttachedPreviewView(
                width = 300,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
                displayOverride = externalDisplay,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(attachedPreviewView, camera)
        val textureView = attachedPreviewView.provideTextureViewSurface()
        assertThat(attachedPreviewView.display.displayId).isEqualTo(externalDisplayId)
        val initialTransform = Matrix(checkNotNull(attachedPreviewView.sensorToViewTransform))
        val initialCorrection = textureView.getTransform(null)

        // Rotating DEFAULT_DISPLAY by 90 degrees does not relayout the external display's window,
        // so PreviewView must accept the quarter-turn and redraw immediately.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()

        assertThat(attachedPreviewView.sensorToViewTransform).isNotEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isNotEqualTo(initialCorrection)
    }

    @Test
    fun attachedToAlreadyRotatedWindow_usesInitialDisplayRotation() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270)
        val (_, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 100,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()

        // At ROTATION_270 on a 90-degree back camera, TextureView correction is non-identity (90°).
        assertThat(textureView.getTransform(null).isIdentity).isFalse()
    }

    @Test
    fun rotatedWhileDetached_updatesOnReattachLayout() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()
        val initialTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val initialCorrection = textureView.getTransform(null)

        container.removeView(previewView)
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()

        container.addView(previewView, FrameLayout.LayoutParams(200, 200))
        shadowOf(getMainLooper()).idle()

        assertThat(previewView.sensorToViewTransform).isNotEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isNotEqualTo(initialCorrection)
    }

    @Test
    fun previewWithCameraEffect_syncsRotationAndTapToFocusWithoutTargetRotation() {
        for (lensFacing in
            listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)) {
            for (mirrorMode in
                listOf(
                    MirrorMode.MIRROR_MODE_ON,
                    MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
                    MirrorMode.MIRROR_MODE_OFF,
                )) {
                assertEffectStreamTracksDisplayRotation(
                    sensorRotation = if (lensFacing == CameraSelector.LENS_FACING_BACK) 90 else 270,
                    lensFacing = lensFacing,
                    mirrorMode = mirrorMode,
                )
            }
        }
    }

    private fun assertEffectStreamTracksDisplayRotation(
        sensorRotation: Int,
        lensFacing: Int,
        mirrorMode: Int,
    ) {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        val (container, previewView) = createAttachedPreviewView(width = 100, height = 200)
        val camera = createFakeCamera(sensorRotation, lensFacing)
        val processor = createFakeProcessor()
        val effect = FakeSurfaceEffect(PREVIEW, processor)
        val preview =
            Preview.Builder().setMirrorMode(mirrorMode).build().apply {
                this.effect = effect
                setSurfaceProvider(previewView.surfaceProvider)
            }
        bindUseCaseToCamera(preview, camera, Size(640, 480))

        // Also create a reference Preview + PreviewTransformation driven via setTargetRotation()
        // so we verify exact parity of rotation, sensorToViewTransform, and tap-to-focus points!
        val refCamera = createFakeCamera(sensorRotation, lensFacing)
        val refProcessor = createFakeProcessor()
        val refEffect = FakeSurfaceEffect(PREVIEW, refProcessor)
        var refInfo: SurfaceRequest.TransformationInfo? = null
        var refResolution: Size? = null
        val refPreview =
            Preview.Builder()
                .setMirrorMode(mirrorMode)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .apply {
                    this.effect = refEffect
                    setSurfaceProvider { request ->
                        refResolution = request.resolution
                        request.setTransformationInfoListener(mainThreadExecutor()) {
                            refInfo = it
                        }
                    }
                }
        bindUseCaseToCamera(refPreview, refCamera, Size(640, 480))

        val sequence =
            listOf(
                Surface.ROTATION_0 to Size(100, 200),
                Surface.ROTATION_90 to Size(200, 100),
                Surface.ROTATION_270 to Size(200, 100),
                Surface.ROTATION_180 to Size(100, 200),
            )
        for ((rotation, viewSize) in sequence) {
            rotateDisplayAndSyncView(container, previewView, rotation, viewSize)
            refPreview.targetRotation = rotation
            shadowOf(getMainLooper()).idle()

            val expectedTransform = PreviewTransformation()
            expectedTransform.setTransformationInfo(
                checkNotNull(refInfo),
                checkNotNull(refResolution),
                lensFacing != CameraSelector.LENS_FACING_BACK,
            )

            assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees)
                .isEqualTo(expectedTransform.surfaceViewContentRotationDegrees)
            assertMatrixClose(
                checkNotNull(previewView.sensorToViewTransform),
                checkNotNull(expectedTransform.getSensorToViewTransform(viewSize, 0)),
            )

            val refMeteringFactory =
                PreviewViewMeteringPointFactory(expectedTransform).apply {
                    setSensorRect(camera.cameraInfoInternal.sensorRect)
                    recalculate(viewSize, 0)
                }
            assertMeteringPointsMatch(previewView, refMeteringFactory, viewSize)
        }
    }

    @Test
    fun previewWithStreamSharing_backFrontAndPhysicalCamera_syncsRotationAndTapToFocus() {
        assertStreamSharingTracksDisplayRotation(
            sensorRotation = 90,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            physicalSensorRotation = null,
        )
        assertStreamSharingTracksDisplayRotation(
            sensorRotation = 270,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            physicalSensorRotation = null,
        )
        // Logical camera has 90° sensor, selected physical camera has 270° sensor: verifies
        // VirtualCameraInfo unwraps to the physical CameraInfo rather than logical implementation.
        assertStreamSharingTracksDisplayRotation(
            sensorRotation = 90,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            physicalSensorRotation = 270,
        )
    }

    private fun assertStreamSharingTracksDisplayRotation(
        sensorRotation: Int,
        lensFacing: Int,
        physicalSensorRotation: Int?,
    ) {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        val (container, previewView) = createAttachedPreviewView(width = 100, height = 200)
        val camera = createFakeCamera(sensorRotation, lensFacing, physicalSensorRotation)
        val sharingProcessor = createFakeProcessor()
        DefaultSurfaceProcessor.Factory.setSupplier { sharingProcessor }

        val preview =
            Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val companionChild =
            FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
        val streamSharing =
            StreamSharing(
                camera,
                /* secondaryCamera= */ null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, companionChild),
                emptyList(),
                useCaseConfigFactory,
            )
        bindUseCaseToCamera(streamSharing, camera, Size(640, 480))

        // Reference StreamSharing where Preview.setTargetRotation() IS called.
        val refCamera = createFakeCamera(sensorRotation, lensFacing, physicalSensorRotation)
        val refSharingProcessor = createFakeProcessor()
        DefaultSurfaceProcessor.Factory.setSupplier { refSharingProcessor }
        var refInfo: SurfaceRequest.TransformationInfo? = null
        var refResolution: Size? = null
        val refPreview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_0).build().apply {
                setSurfaceProvider { request ->
                    refResolution = request.resolution
                    request.setTransformationInfoListener(mainThreadExecutor()) { refInfo = it }
                }
            }
        val refCompanion =
            FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
        val refStreamSharing =
            StreamSharing(
                refCamera,
                /* secondaryCamera= */ null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(refPreview, refCompanion),
                emptyList(),
                useCaseConfigFactory,
            )
        bindUseCaseToCamera(refStreamSharing, refCamera, Size(640, 480))

        val sequence =
            listOf(
                Surface.ROTATION_0 to Size(100, 200),
                Surface.ROTATION_90 to Size(200, 100),
                Surface.ROTATION_270 to Size(200, 100),
                Surface.ROTATION_180 to Size(100, 200),
            )
        for ((rotation, viewSize) in sequence) {
            rotateDisplayAndSyncView(container, previewView, rotation, viewSize)
            refPreview.targetRotation = rotation
            shadowOf(getMainLooper()).idle()

            val expectedTransform = PreviewTransformation()
            expectedTransform.setTransformationInfo(
                checkNotNull(refInfo),
                checkNotNull(refResolution),
                lensFacing != CameraSelector.LENS_FACING_BACK,
            )

            assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees)
                .isEqualTo(expectedTransform.surfaceViewContentRotationDegrees)
            assertMatrixClose(
                checkNotNull(previewView.sensorToViewTransform),
                checkNotNull(expectedTransform.getSensorToViewTransform(viewSize, 0)),
            )

            val refMeteringFactory =
                PreviewViewMeteringPointFactory(expectedTransform).apply {
                    setSensorRect(camera.cameraInfoInternal.sensorRect)
                    recalculate(viewSize, 0)
                }
            assertMeteringPointsMatch(previewView, refMeteringFactory, viewSize)
        }
    }

    @Test
    fun compatibleMode_customTargetRotationPreservedAcrossDisplayChanges() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 100,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        val preview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_180).build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
        bindUseCaseToCamera(preview, camera, Size(640, 480))

        val textureView = previewView.provideTextureViewSurface()
        val customCorrection = Matrix(textureView.getTransform(null))
        assertThat(customCorrection.isIdentity).isFalse()
        assertThat(previewView.mUseDisplayRotation).isFalse()

        rotateDisplayAndSyncView(container, previewView, Surface.ROTATION_90, Size(200, 100))
        assertThat(previewView.mUseDisplayRotation).isFalse()
        assertThat(textureView.getTransform(null)).isEqualTo(customCorrection)

        rotateDisplayAndSyncView(container, previewView, Surface.ROTATION_270, Size(200, 100))
        assertThat(previewView.mUseDisplayRotation).isFalse()
        assertThat(textureView.getTransform(null)).isEqualTo(customCorrection)
    }

    @Test
    fun compatibleModeWithStreamSharing_customTargetRotationPreservedAcrossDisplayChanges() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 100,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        val sharingProcessor = createFakeProcessor()
        DefaultSurfaceProcessor.Factory.setSupplier { sharingProcessor }

        val preview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_180).build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
        val companionChild =
            FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
        val streamSharing =
            StreamSharing(
                camera,
                /* secondaryCamera= */ null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, companionChild),
                emptyList(),
                useCaseConfigFactory,
            )
        bindUseCaseToCamera(streamSharing, camera, Size(640, 480))

        val textureView = previewView.provideTextureViewSurface()
        assertThat(previewView.mUseDisplayRotation).isFalse()
        assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees).isEqualTo(0)

        rotateDisplayAndSyncView(container, previewView, Surface.ROTATION_90, Size(200, 100))
        assertThat(previewView.mUseDisplayRotation).isFalse()
        assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees).isEqualTo(0)
        assertThat(textureView.getTransform(null).isIdentity).isTrue()
    }

    @Test
    fun compatibleModeWithCameraEffect_customTargetRotationFollowsDisplayRotation() =
        assertCameraEffectWithCustomTargetRotationFollowsDisplayRotation(
            PreviewView.ImplementationMode.COMPATIBLE,
            TextureViewImplementation::class.java,
        )

    @Test
    fun performanceModeWithCameraEffect_customTargetRotationFollowsDisplayRotation() =
        assertCameraEffectWithCustomTargetRotationFollowsDisplayRotation(
            PreviewView.ImplementationMode.PERFORMANCE,
            SurfaceViewImplementation::class.java,
        )

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun performanceModeWithCameraEffect_customTargetRotationFollowsDisplayRotationOnApi28() =
        assertCameraEffectWithCustomTargetRotationFollowsDisplayRotation(
            PreviewView.ImplementationMode.PERFORMANCE,
            TextureViewImplementation::class.java,
        )

    /**
     * Asserts that the preview with a [androidx.camera.core.CameraEffect] follows the display
     * rotation, including when the app sets a target rotation at bind time and updates it on its
     * own schedule, since the target rotation is not carried over to the processed stream.
     */
    private fun assertCameraEffectWithCustomTargetRotationFollowsDisplayRotation(
        mode: PreviewView.ImplementationMode,
        expectedImplementation: Class<out PreviewViewImplementation>,
    ) {
        val (container, previewView) =
            createAttachedPreviewView(width = 100, height = 200, mode = mode)
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        val processor = createFakeProcessor()
        val effect = FakeSurfaceEffect(PREVIEW, processor)
        val preview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_180).build().apply {
                this.effect = effect
                setSurfaceProvider(previewView.surfaceProvider)
            }
        bindUseCaseToCamera(preview, camera, Size(640, 480))
        assertThat(previewView.mImplementation).isInstanceOf(expectedImplementation)
        assertThat(previewView.mUseDisplayRotation).isTrue()

        // ROTATION_180 is baked into the buffer, so a remaining 180 degrees rotates it upright
        // for the ROTATION_0 display.
        assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees).isEqualTo(180)
        val transformAt0 = Matrix(checkNotNull(previewView.sensorToViewTransform))

        // Rotating the display without updating Preview's target rotation is followed once the
        // layout changes.
        rotateDisplayAndSyncView(container, previewView, Surface.ROTATION_90, Size(200, 100))
        assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees).isEqualTo(90)
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(transformAt0)

        // Updating Preview's target rotation to the display rotation doesn't change how the
        // preview is oriented on the display.
        val transformAt90 = Matrix(checkNotNull(previewView.sensorToViewTransform))
        preview.targetRotation = Surface.ROTATION_90
        shadowOf(getMainLooper()).idle()
        assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees).isEqualTo(90)
        assertMatrixClose(checkNotNull(previewView.sensorToViewTransform), transformAt90)
    }

    @Test
    fun previewWithStreamSharingAndCameraEffect_syncsRotationAndTapToFocus() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        val (container, previewView) = createAttachedPreviewView(width = 100, height = 200)
        val camera = createFakeCamera(sensorRotation = 270, CameraSelector.LENS_FACING_FRONT)
        val sharingProcessor = createFakeProcessor()
        DefaultSurfaceProcessor.Factory.setSupplier { sharingProcessor }
        val childEffectProcessor = createFakeProcessor()
        val childEffect = FakeSurfaceEffect(PREVIEW, childEffectProcessor)

        val preview =
            Preview.Builder().build().apply {
                effect = childEffect
                setSurfaceProvider(previewView.surfaceProvider)
            }
        val companionChild =
            FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
        val streamSharing =
            StreamSharing(
                camera,
                /* secondaryCamera= */ null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, companionChild),
                listOf(childEffect),
                useCaseConfigFactory,
            )
        bindUseCaseToCamera(streamSharing, camera, Size(640, 480))

        val refCamera = createFakeCamera(sensorRotation = 270, CameraSelector.LENS_FACING_FRONT)
        val refSharingProcessor = createFakeProcessor()
        DefaultSurfaceProcessor.Factory.setSupplier { refSharingProcessor }
        val refChildEffectProcessor = createFakeProcessor()
        val refChildEffect = FakeSurfaceEffect(PREVIEW, refChildEffectProcessor)
        var refInfo: SurfaceRequest.TransformationInfo? = null
        var refResolution: Size? = null
        val refPreview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_0).build().apply {
                effect = refChildEffect
                setSurfaceProvider { request ->
                    refResolution = request.resolution
                    request.setTransformationInfoListener(mainThreadExecutor()) { refInfo = it }
                }
            }
        val refCompanion =
            FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
        val refStreamSharing =
            StreamSharing(
                refCamera,
                /* secondaryCamera= */ null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(refPreview, refCompanion),
                listOf(refChildEffect),
                useCaseConfigFactory,
            )
        bindUseCaseToCamera(refStreamSharing, refCamera, Size(640, 480))

        val sequence =
            listOf(
                Surface.ROTATION_0 to Size(100, 200),
                Surface.ROTATION_90 to Size(200, 100),
                Surface.ROTATION_270 to Size(200, 100),
                Surface.ROTATION_180 to Size(100, 200),
            )
        for ((rotation, viewSize) in sequence) {
            rotateDisplayAndSyncView(container, previewView, rotation, viewSize)
            refPreview.targetRotation = rotation
            shadowOf(getMainLooper()).idle()

            val expectedTransform = PreviewTransformation()
            expectedTransform.setTransformationInfo(
                checkNotNull(refInfo),
                checkNotNull(refResolution),
                /* useFrontCameraRotation= */ true,
            )
            assertThat(previewView.mPreviewTransform.surfaceViewContentRotationDegrees)
                .isEqualTo(expectedTransform.surfaceViewContentRotationDegrees)
            assertMatrixClose(
                checkNotNull(previewView.sensorToViewTransform),
                checkNotNull(expectedTransform.getSensorToViewTransform(viewSize, 0)),
            )
        }
    }

    @Test
    fun detachedPreLayoutAndBind_redrawsOnAttachAndReattachAtUnchangedBounds() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270)
        activityController?.pause()?.stop()?.destroy()
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        activityController = controller
        val activity = controller.get()
        val container = FrameLayout(activity)
        activity.setContentView(container)

        // Scenario A: Bind and pre-measure/layout PreviewView while detached on ROTATION_270
        // display.
        val previewView = PreviewView(activity)
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        previewView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(
                200,
                android.view.View.MeasureSpec.EXACTLY,
            ),
            android.view.View.MeasureSpec.makeMeasureSpec(
                100,
                android.view.View.MeasureSpec.EXACTLY,
            ),
        )
        previewView.layout(0, 0, 200, 100)
        shadowOf(getMainLooper()).idle()
        assertThat(previewView.sensorToViewTransform).isNull()

        // Attach at the exact same 200x100 bounds: must still redraw and initialize transform!
        container.addView(previewView, FrameLayout.LayoutParams(200, 100))
        resizePreviewView(container, previewView, width = 200, height = 100)
        val textureView = previewView.provideTextureViewSurface()
        assertThat(previewView.sensorToViewTransform).isNotNull()
        assertThat(textureView.getTransform(null).isIdentity).isFalse()
        val pt = previewView.meteringPointFactory.createPoint(100f, 50f)
        assertThat(pt.x).isIn(com.google.common.collect.Range.closed(0f, 1f))
        assertThat(pt.y).isIn(com.google.common.collect.Range.closed(0f, 1f))

        // Scenario B: Detach, rebind a fresh Preview while detached, then re-attach at the same
        // 200x100 bounds on ROTATION_270 display.
        container.removeView(previewView)
        val camera2 = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera2)
        val textureView2 = previewView.provideTextureViewSurface()
        // While detached, getDefaultDisplay() was null, so ROTATION_270 wasn't applied yet.
        assertThat(previewView.sensorToViewTransform).isNull()

        container.addView(previewView, FrameLayout.LayoutParams(200, 100))
        resizePreviewView(container, previewView, width = 200, height = 100)
        assertThat(previewView.sensorToViewTransform).isNotNull()
        assertThat(textureView2.getTransform(null).isIdentity).isFalse()
    }

    @Test
    fun redrawBeforeTransformationInfoDelivered_doesNotCrash() {
        val (container, previewView) = createAttachedPreviewView(width = 100, height = 200)
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        val surfaceRequest = SurfaceRequest(Size(640, 480), camera) {}
        surfaceRequest.updateTransformationInfo(
            SurfaceRequest.TransformationInfo.of(
                Rect(0, 0, 640, 480),
                /* rotationDegrees= */ 90,
                /* targetRotation= */ -1,
                /* hasCameraTransform= */ false,
                Matrix(),
                /* mirroring= */ false,
            )
        )
        unbindActions.add { surfaceRequest.deferrableSurface.close() }

        // Deliver SurfaceRequest on main thread without idling main looper yet, so the posted
        // TransformationInfoListener has not run when resizePreviewView calls redrawPreview().
        previewView.surfaceProvider.onSurfaceRequested(surfaceRequest)
        resizePreviewView(container, previewView, width = 200, height = 100, idleLooper = false)
        previewView.redrawPreview()

        shadowOf(getMainLooper()).idle()
        assertThat(previewView.sensorToViewTransform).isNotNull()
    }

    @Test
    fun performanceModeWithoutCameraEffect_customTargetRotationFollowsDisplayRotation() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 100,
                height = 200,
                mode = PreviewView.ImplementationMode.PERFORMANCE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        val preview =
            Preview.Builder().setTargetRotation(Surface.ROTATION_180).build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
        bindUseCaseToCamera(preview, camera, Size(640, 480))

        assertThat(previewView.mImplementation).isInstanceOf(SurfaceViewImplementation::class.java)
        assertThat(previewView.mUseDisplayRotation).isTrue()
        val initialTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))

        rotateDisplayAndSyncView(container, previewView, Surface.ROTATION_90, Size(200, 100))
        assertThat(previewView.mUseDisplayRotation).isTrue()
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(initialTransform)
    }

    @Test
    fun quarterTurnWithoutLayoutOrConfigChange_heldBackUntilConfigurationChanges() {
        val (_, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()
        val initialTransform = Matrix(checkNotNull(previewView.sensorToViewTransform))
        val initialCorrection = textureView.getTransform(null)

        // A quarter turn from the DisplayListener alone is held back, without any timeout, since
        // it would otherwise run ahead of the relayout that follows it.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        previewView.redrawPreview()
        assertThat(previewView.sensorToViewTransform).isEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isEqualTo(initialCorrection)

        // Accepted on the layout pass scheduled by the configuration change that comes with the
        // quarter turn, not synchronously with the configuration change.
        previewView.dispatchConfigurationChanged(Configuration(context.resources.configuration))
        assertThat(previewView.isLayoutRequested).isTrue()
        assertThat(previewView.sensorToViewTransform).isEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isEqualTo(initialCorrection)

        shadowOf(getMainLooper()).idle()
        assertThat(previewView.sensorToViewTransform).isNotEqualTo(initialTransform)
        assertThat(textureView.getTransform(null)).isNotEqualTo(initialCorrection)
    }

    @Test
    fun rotatedWhileDetached_redrawsOnReattachWithoutLayoutChange() {
        val (container, previewView) =
            createAttachedPreviewView(
                width = 200,
                height = 200,
                mode = PreviewView.ImplementationMode.COMPATIBLE,
            )
        val camera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        bindPreview(previewView, camera)
        val textureView = previewView.provideTextureViewSurface()
        val initialCorrection = textureView.getTransform(null)

        container.removeView(previewView)
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        shadowOf(getMainLooper()).idle()
        // Re-attach and check before the layout pass, which only runs when the looper idles.
        container.addView(previewView, FrameLayout.LayoutParams(200, 200))

        assertThat(textureView.getTransform(null)).isNotEqualTo(initialCorrection)
    }

    private fun createAttachedPreviewView(
        width: Int,
        height: Int,
        mode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE,
        displayOverride: Display? = null,
    ): Pair<FrameLayout, PreviewView> {
        presentationToDismiss?.dismiss()
        presentationToDismiss = null
        activityController?.pause()?.stop()?.destroy()
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        activityController = controller
        val activity = controller.get()
        val container = FrameLayout(activity)
        val previewView = PreviewView(activity)
        previewView.implementationMode = mode
        container.addView(previewView, FrameLayout.LayoutParams(width, height))
        if (displayOverride != null) {
            val presentation = Presentation(activity, displayOverride)
            presentationToDismiss = presentation
            presentation.setContentView(container)
            presentation.show()
        } else {
            activity.setContentView(container)
        }
        container.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(
                width,
                android.view.View.MeasureSpec.EXACTLY,
            ),
            android.view.View.MeasureSpec.makeMeasureSpec(
                height,
                android.view.View.MeasureSpec.EXACTLY,
            ),
        )
        container.layout(0, 0, width, height)
        shadowOf(getMainLooper()).idle()
        return container to previewView
    }

    private fun resizePreviewView(
        container: FrameLayout,
        previewView: PreviewView,
        width: Int,
        height: Int,
        idleLooper: Boolean = true,
    ) {
        previewView.layoutParams = FrameLayout.LayoutParams(width, height)
        container.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(
                width,
                android.view.View.MeasureSpec.EXACTLY,
            ),
            android.view.View.MeasureSpec.makeMeasureSpec(
                height,
                android.view.View.MeasureSpec.EXACTLY,
            ),
        )
        container.layout(0, 0, width, height)
        if (idleLooper) {
            shadowOf(getMainLooper()).idle()
        }
    }

    private fun rotateDisplayAndSyncView(
        container: FrameLayout,
        previewView: PreviewView,
        rotation: Int,
        viewSize: Size,
    ) {
        val sizeChanges =
            previewView.width != viewSize.width || previewView.height != viewSize.height
        shadowOf(defaultDisplay).setRotation(rotation)
        shadowOf(getMainLooper()).idle()
        if (sizeChanges) {
            resizePreviewView(container, previewView, viewSize.width, viewSize.height)
        } else {
            val newConfig = Configuration(context.resources.configuration)
            previewView.dispatchConfigurationChanged(newConfig)
            shadowOf(getMainLooper()).idle()
        }
    }

    /**
     * Returns the [PreviewTransformation] of a reference [Preview] of a 90-degree back camera with
     * the given target rotation, which PreviewView should match for that display rotation.
     */
    private fun createReferenceTransformation(targetRotation: Int): PreviewTransformation {
        val refCamera = createFakeCamera(sensorRotation = 90, CameraSelector.LENS_FACING_BACK)
        var refInfo: SurfaceRequest.TransformationInfo? = null
        var refResolution: Size? = null
        val refPreview =
            Preview.Builder().setTargetRotation(targetRotation).build().apply {
                setSurfaceProvider { request ->
                    refResolution = request.resolution
                    request.setTransformationInfoListener(mainThreadExecutor()) { refInfo = it }
                }
            }
        bindUseCaseToCamera(refPreview, refCamera, Size(640, 480))
        shadowOf(getMainLooper()).idle()
        return PreviewTransformation().apply {
            setTransformationInfo(
                checkNotNull(refInfo),
                checkNotNull(refResolution),
                /* useFrontCameraRotation= */ false,
            )
        }
    }

    private fun createFakeCamera(
        sensorRotation: Int,
        lensFacing: Int,
        physicalSensorRotation: Int? = null,
    ): FakeCamera {
        val rawBase =
            FakeCameraInfoInternal("0", sensorRotation, lensFacing).apply {
                implementationType = CameraInfo.IMPLEMENTATION_TYPE_CAMERA2
            }
        var physicalInfos: Set<CameraInfo> = emptySet()
        val baseInfo =
            object : ForwardingCameraInfo(rawBase) {
                override fun getSensorRect(): Rect = Rect(0, 0, 640, 480)

                override fun getPhysicalCameraInfos(): Set<CameraInfo> = physicalInfos
            }
        val effectiveInfo: CameraInfoInternal =
            if (physicalSensorRotation != null) {
                val physicalId = "physical-1"
                val physicalSelector =
                    CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .setPhysicalCameraId(physicalId)
                        .build()
                val rawPhysical =
                    FakeCameraInfoInternal(physicalId, physicalSensorRotation, lensFacing)
                val physicalInfo =
                    object : ForwardingCameraInfo(rawPhysical) {
                        override fun getCameraSelector(): CameraSelector = physicalSelector
                    }
                physicalInfos = setOf(physicalInfo)
                AdapterCameraInfo(baseInfo, physicalId, CameraConfigs.defaultConfig())
            } else {
                baseInfo
            }
        return FakeCamera("0", null, effectiveInfo)
    }

    private fun createFakeProcessor(): FakeSurfaceProcessorInternal =
        FakeSurfaceProcessorInternal(mainThreadExecutor()).also { processorsToCleanUp.add(it) }

    private fun bindPreview(previewView: PreviewView, camera: FakeCamera): Preview {
        val preview =
            Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        bindUseCaseToCamera(preview, camera, Size(640, 480))
        return preview
    }

    private fun bindUseCaseToCamera(
        useCase: androidx.camera.core.UseCase,
        camera: FakeCamera,
        resolution: Size,
    ) {
        useCase.bindToCamera(
            camera,
            /* secondaryCamera= */ null,
            /* extendedConfig= */ null,
            useCase.getDefaultConfig(true, useCaseConfigFactory),
        )
        useCase.updateSuggestedStreamSpec(StreamSpec.builder(resolution).build(), null)
        useCase.onSessionStart()
        shadowOf(getMainLooper()).idle()
        unbindActions.add {
            if (useCase.camera != null) {
                useCase.onSessionStop()
                useCase.unbindFromCamera(camera)
            }
        }
    }

    private fun PreviewView.provideSurfaceViewSurface(): SurfaceView {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is SurfaceView) {
                checkNotNull(mImplementation).onSurfaceProvided()
                shadowOf(getMainLooper()).idle()
                return child
            }
        }
        throw AssertionError("Expected SurfaceView child in PreviewView")
    }

    private fun PreviewView.provideTextureViewSurface(): TextureView {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextureView) {
                val surfaceTexture = SurfaceTexture(0)
                unbindActions.add { surfaceTexture.release() }
                checkNotNull(child.surfaceTextureListener)
                    .onSurfaceTextureAvailable(surfaceTexture, 640, 480)
                shadowOf(getMainLooper()).idle()
                return child
            }
        }
        throw AssertionError("Expected TextureView child in PreviewView")
    }

    private fun assertMatrixClose(actual: Matrix, expected: Matrix) {
        val actualValues = FloatArray(9).also { actual.getValues(it) }
        val expectedValues = FloatArray(9).also { expected.getValues(it) }
        for (i in 0 until 9) {
            assertThat(actualValues[i]).isWithin(1e-2f).of(expectedValues[i])
        }
    }

    private fun assertMeteringPointsMatch(
        previewView: PreviewView,
        expectedFactory: PreviewViewMeteringPointFactory,
        viewSize: Size,
    ) {
        val testPoints =
            listOf(
                PointF(viewSize.width * 0.5f, viewSize.height * 0.5f),
                PointF(viewSize.width * 0.2f, viewSize.height * 0.3f),
                PointF(viewSize.width * 0.8f, viewSize.height * 0.7f),
            )
        val actualFactory = previewView.meteringPointFactory
        for (pt in testPoints) {
            val actualPoint = actualFactory.createPoint(pt.x, pt.y)
            val expectedPoint = expectedFactory.createPoint(pt.x, pt.y)
            assertThat(actualPoint.x).isWithin(1e-3f).of(expectedPoint.x)
            assertThat(actualPoint.y).isWithin(1e-3f).of(expectedPoint.y)
        }
    }
}
