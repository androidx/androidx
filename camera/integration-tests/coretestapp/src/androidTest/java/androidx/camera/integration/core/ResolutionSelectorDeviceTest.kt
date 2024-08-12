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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.RestrictedCameraControl
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * ResolutionSelector related test on the real device.
 *
 * Make the ResolutionSelectorDeviceTest focus on the generic ResolutionSelector selection results
 * for all the normal devices. Skips the tests when the devices have any of the quirks that might
 * affect the selected resolution.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ResolutionSelectorDeviceTest(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName.contains(CameraPipeConfig::class.simpleName!!),
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val useCaseFormatMap =
        mapOf(
            Pair(Preview::class.java, ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE),
            Pair(ImageCapture::class.java, ImageFormat.JPEG),
            Pair(ImageAnalysis::class.java, ImageFormat.YUV_420_888)
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(
                    "back+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var cameraInfoInternal: CameraInfoInternal

    @Before
    fun initializeCameraX() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        instrumentation.runOnMainSync {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()

            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfoInternal = camera.cameraInfo as CameraInfoInternal
        }

        assumeNotAspectRatioQuirkDevice()
        assumeNotOutputSizeQuirkDevice()
    }

    @After
    fun shutdownCameraX() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun canSelect4x3ResolutionForPreviewImageCaptureAndImageAnalysis() {
        canSelectTargetAspectRatioResolutionForPreviewImageCaptureAndImageAnalysis(RATIO_4_3)
    }

    @Test
    fun canSelect16x9ResolutionForPreviewImageCaptureAndImageAnalysis() {
        canSelectTargetAspectRatioResolutionForPreviewImageCaptureAndImageAnalysis(RATIO_16_9)
    }

    private fun canSelectTargetAspectRatioResolutionForPreviewImageCaptureAndImageAnalysis(
        targetAspectRatio: Int
    ) {
        val preview = createUseCaseWithResolutionSelector(Preview::class.java, targetAspectRatio)
        val imageCapture =
            createUseCaseWithResolutionSelector(ImageCapture::class.java, targetAspectRatio)
        val imageAnalysis =
            createUseCaseWithResolutionSelector(ImageAnalysis::class.java, targetAspectRatio)
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
        }
        assertThat(isResolutionAspectRatioBestMatched(preview, targetAspectRatio)).isTrue()
        assertThat(isResolutionAspectRatioBestMatched(imageCapture, targetAspectRatio)).isTrue()
        assertThat(isResolutionAspectRatioBestMatched(imageAnalysis, targetAspectRatio)).isTrue()
    }

    private fun isResolutionAspectRatioBestMatched(
        useCase: UseCase,
        targetAspectRatio: Int
    ): Boolean {
        val isMatched =
            hasMatchingAspectRatio(
                useCase.attachedSurfaceResolution!!,
                aspectRatioToRational(targetAspectRatio)
            )

        if (isMatched) {
            return true
        }

        // PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM will be used to select resolutions for the
        // combination of Preview + ImageAnalysis + ImageCapture
        val closestAspectRatioSizes =
            if (useCase is Preview || useCase is ImageAnalysis) {
                getClosestAspectRatioSizesUnderPreviewSize(targetAspectRatio, useCase.javaClass)
            } else {
                getClosestAspectRatioSizes(targetAspectRatio, useCase.javaClass)
            }

        Log.d(
            "ResolutionSelectorDeviceTest",
            "The selected resolution (${useCase.attachedSurfaceResolution!!}) does not exactly" +
                " match the target aspect ratio. It is selected from the closest aspect ratio" +
                " sizes: $closestAspectRatioSizes"
        )

        return closestAspectRatioSizes.contains(useCase.attachedSurfaceResolution!!)
    }

    @Test
    fun canSelect4x3ResolutionForPreviewByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(Preview::class.java, RATIO_4_3)

    @Test
    fun canSelect16x9ResolutionForPreviewByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(Preview::class.java, RATIO_16_9)

    @Test
    fun canSelect4x3ResolutionForImageCaptureByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(ImageCapture::class.java, RATIO_4_3)

    @Test
    fun canSelect16x9ResolutionForImageCaptureByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(ImageCapture::class.java, RATIO_16_9)

    @Test
    fun canSelect4x3ResolutionForImageAnalysisByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(ImageAnalysis::class.java, RATIO_4_3)

    @Test
    fun canSelect16x9ResolutionForImageAnalysisByResolutionStrategy() =
        canSelectResolutionByResolutionStrategy(ImageAnalysis::class.java, RATIO_16_9)

    private fun <T : UseCase> canSelectResolutionByResolutionStrategy(
        useCaseClass: Class<T>,
        ratio: Int
    ) {
        // Filters the output sizes matching the target aspect ratio
        cameraInfoInternal
            .getSupportedResolutions(useCaseFormatMap[useCaseClass]!!)
            .filter { size -> hasMatchingAspectRatio(size, aspectRatioToRational(ratio)) }
            .let {
                // Picks the item in the middle of the list to run the test
                it.elementAtOrNull(it.size / 2)?.let { boundSize ->
                    {
                        val useCase =
                            createUseCaseWithResolutionSelector(
                                useCaseClass,
                                aspectRatio = ratio,
                                aspectRatioStrategyFallbackRule = FALLBACK_RULE_AUTO,
                                boundSize = boundSize,
                                resolutionStrategyFallbackRule =
                                    FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        instrumentation.runOnMainSync {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
                        }
                        assertThat(useCase.attachedSurfaceResolution).isEqualTo(boundSize)
                    }
                }
            }
    }

    @Test
    fun canSelectAnyResolutionForPreviewByResolutionFilter() =
        canSelectAnyResolutionByResolutionFilter(
            Preview::class.java,
            // For Preview, need to override resolution strategy so that the output sizes larger
            // than PREVIEW size can be selected.
            cameraInfoInternal
                .getSupportedResolutions(useCaseFormatMap[Preview::class.java]!!)
                .maxWithOrNull(CompareSizesByArea())
        )

    @Test
    fun canSelectAnyHighResolutionForPreviewByResolutionFilter() =
        canSelectAnyHighResolutionByResolutionFilter(
            Preview::class.java,
            // For Preview, need to override resolution strategy so that the output sizes larger
            // than PREVIEW size can be selected.
            cameraInfoInternal
                .getSupportedHighResolutions(useCaseFormatMap[Preview::class.java]!!)
                .maxWithOrNull(CompareSizesByArea())
        )

    @Test
    fun canSelectAnyResolutionForImageCaptureByResolutionFilter() =
        canSelectAnyResolutionByResolutionFilter(ImageCapture::class.java)

    @Test
    fun canSelectAnyHighResolutionForImageCaptureByResolutionFilter() =
        canSelectAnyHighResolutionByResolutionFilter(ImageCapture::class.java)

    @Test
    fun canSelectAnyResolutionForImageAnalysisByResolutionFilter() =
        canSelectAnyResolutionByResolutionFilter(ImageAnalysis::class.java)

    @Test
    fun canSelectAnyHighResolutionForImageAnalysisByResolutionFilter() =
        canSelectAnyHighResolutionByResolutionFilter(ImageAnalysis::class.java)

    private fun <T : UseCase> canSelectAnyResolutionByResolutionFilter(
        useCaseClass: Class<T>,
        boundSize: Size? = null,
        resolutionStrategyFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
    ) =
        canSelectAnyResolutionByResolutionFilter(
            useCaseClass,
            cameraInfoInternal.getSupportedResolutions(useCaseFormatMap[useCaseClass]!!),
            boundSize,
            resolutionStrategyFallbackRule
        )

    private fun <T : UseCase> canSelectAnyHighResolutionByResolutionFilter(
        useCaseClass: Class<T>,
        boundSize: Size? = null,
        resolutionStrategyFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
    ) =
        canSelectAnyResolutionByResolutionFilter(
            useCaseClass,
            cameraInfoInternal.getSupportedHighResolutions(useCaseFormatMap[useCaseClass]!!),
            boundSize,
            resolutionStrategyFallbackRule,
            PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
        )

    private fun <T : UseCase> canSelectAnyResolutionByResolutionFilter(
        useCaseClass: Class<T>,
        outputSizes: List<Size>,
        boundSize: Size? = null,
        resolutionStrategyFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
        allowedResolutionMode: Int = PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
    ) {
        outputSizes.forEach { targetResolution ->
            val useCase =
                createUseCaseWithResolutionSelector(
                    useCaseClass,
                    boundSize = boundSize,
                    resolutionStrategyFallbackRule = resolutionStrategyFallbackRule,
                    resolutionFilter = { _, _ -> mutableListOf(targetResolution) },
                    allowedResolutionMode = allowedResolutionMode
                )
            instrumentation.runOnMainSync {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            }
            assertThat(useCase.attachedSurfaceResolution).isEqualTo(targetResolution)
        }
    }

    @Test
    fun canSelectResolutionForSixtyFpsPreview() {
        assumeTrue(isSixtyFpsSupported())

        val preview = Preview.Builder().setTargetFrameRate(Range.create(60, 60)).build()
        val imageCapture = ImageCapture.Builder().build()

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        }

        assertThat(getMaxFrameRate(preview.attachedSurfaceResolution!!)).isEqualTo(60)
    }

    private fun isSixtyFpsSupported() =
        CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
            ?.get(CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.any { range -> range.upper == 60 } ?: false

    private fun getMaxFrameRate(size: Size) =
        (1_000_000_000.0 /
                CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)!!.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )!!
                    .getOutputMinFrameDuration(SurfaceTexture::class.java, size) + 0.5)
            .toInt()

    private fun <T : UseCase> createUseCaseWithResolutionSelector(
        useCaseClass: Class<T>,
        aspectRatio: Int? = null,
        aspectRatioStrategyFallbackRule: Int = FALLBACK_RULE_AUTO,
        boundSize: Size? = null,
        resolutionStrategyFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
        resolutionFilter: ResolutionFilter? = null,
        allowedResolutionMode: Int = PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
    ): UseCase {
        val builder =
            when (useCaseClass) {
                Preview::class.java -> Preview.Builder()
                ImageCapture::class.java -> ImageCapture.Builder()
                ImageAnalysis::class.java -> ImageAnalysis.Builder()
                else -> throw IllegalArgumentException("Unsupported class type!!")
            }

        (builder as ImageOutputConfig.Builder<*>).setResolutionSelector(
            createResolutionSelector(
                aspectRatio,
                aspectRatioStrategyFallbackRule,
                boundSize,
                resolutionStrategyFallbackRule,
                resolutionFilter,
                allowedResolutionMode
            )
        )

        return builder.build()
    }

    private fun createResolutionSelector(
        aspectRatio: Int? = null,
        aspectRatioFallbackRule: Int = FALLBACK_RULE_AUTO,
        boundSize: Size? = null,
        resolutionFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
        resolutionFilter: ResolutionFilter? = null,
        allowedResolutionMode: Int = PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
    ) =
        ResolutionSelector.Builder()
            .apply {
                aspectRatio?.let {
                    setAspectRatioStrategy(
                        AspectRatioStrategy(aspectRatio, aspectRatioFallbackRule)
                    )
                }
                boundSize?.let {
                    setResolutionStrategy(ResolutionStrategy(boundSize, resolutionFallbackRule))
                }
                resolutionFilter?.let { setResolutionFilter(resolutionFilter) }
                setAllowedResolutionMode(allowedResolutionMode)
            }
            .build()

    private fun aspectRatioToRational(ratio: Int) =
        if (ratio == RATIO_16_9) {
            ASPECT_RATIO_16_9
        } else {
            ASPECT_RATIO_4_3
        }

    private fun <T : UseCase> getClosestAspectRatioSizesUnderPreviewSize(
        targetAspectRatio: Int,
        useCaseClass: Class<T>
    ): List<Size> {
        val outputSizes =
            cameraInfoInternal.getSupportedResolutions(useCaseFormatMap[useCaseClass]!!)
        return outputSizes
            .getSmallerThanOrEqualToPreviewScaleSizeSublist()
            .getClosestAspectRatioSublist(targetAspectRatio)
    }

    private fun <T : UseCase> getClosestAspectRatioSizes(
        targetAspectRatio: Int,
        useCaseClass: Class<T>
    ): List<Size> {
        val outputSizes =
            cameraInfoInternal.getSupportedResolutions(useCaseFormatMap[useCaseClass]!!)
        return outputSizes.getClosestAspectRatioSublist(targetAspectRatio)
    }

    private fun List<Size>.getSmallerThanOrEqualToPreviewScaleSizeSublist() = filter { size ->
        SizeUtil.getArea(size) <= SizeUtil.getArea(getPreviewScaleSize())
    }

    @Suppress("DEPRECATION")
    private fun getPreviewScaleSize(): Size {
        val point = Point()
        DisplayInfoManager.getInstance(context).getMaxSizeDisplay(false).getRealSize(point)
        val displaySize = Size(point.x, point.y)
        return if (SizeUtil.isSmallerByArea(RESOLUTION_1080P, displaySize)) {
            RESOLUTION_1080P
        } else {
            displaySize
        }
    }

    private fun List<Size>.getClosestAspectRatioSublist(targetAspectRatio: Int): List<Size> {
        val sensorRect = (camera.cameraControl as RestrictedCameraControl).sensorRect
        val aspectRatios = getResolutionListGroupingAspectRatioKeys(this)
        val sortedAspectRatios =
            aspectRatios.sortedWith(
                AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                    aspectRatioToRational(targetAspectRatio),
                    Rational(sensorRect.width(), sensorRect.height())
                )
            )
        val groupedRatioToSizesMap = groupSizesByAspectRatio(this)

        for (ratio in sortedAspectRatios) {
            groupedRatioToSizesMap[ratio]?.let {
                if (it.isNotEmpty()) {
                    return it
                }
            }
        }

        fail("There should have one non-empty size list returned.")
    }

    /**
     * Returns the grouping aspect ratio keys of the input resolution list.
     *
     * Some sizes might be mod16 case. When grouping, those sizes will be grouped into an existing
     * aspect ratio group if the aspect ratio can match by the mod16 rule.
     */
    private fun getResolutionListGroupingAspectRatioKeys(
        resolutionCandidateList: List<Size>
    ): List<Rational> {
        val aspectRatios = mutableListOf<Rational>()

        // Adds the default 4:3 and 16:9 items first to avoid their mod16 sizes to create
        // additional items.
        aspectRatios.add(ASPECT_RATIO_4_3)
        aspectRatios.add(ASPECT_RATIO_16_9)

        // Tries to find the aspect ratio which the target size belongs to.
        for (size in resolutionCandidateList) {
            val newRatio = Rational(size.width, size.height)
            val aspectRatioFound = aspectRatios.contains(newRatio)

            // The checking size might be a mod16 size which can be mapped to an existing aspect
            // ratio group.
            if (!aspectRatioFound) {
                var hasMatchingAspectRatio = false
                for (aspectRatio in aspectRatios) {
                    if (hasMatchingAspectRatio(size, aspectRatio)) {
                        hasMatchingAspectRatio = true
                        break
                    }
                }
                if (!hasMatchingAspectRatio) {
                    aspectRatios.add(newRatio)
                }
            }
        }

        return aspectRatios
    }

    /** Groups the input sizes into an aspect ratio to size list map. */
    private fun groupSizesByAspectRatio(sizes: List<Size>): Map<Rational, MutableList<Size>> {
        val aspectRatioSizeListMap = mutableMapOf<Rational, MutableList<Size>>()
        val aspectRatioKeys = getResolutionListGroupingAspectRatioKeys(sizes)

        for (aspectRatio in aspectRatioKeys) {
            aspectRatioSizeListMap[aspectRatio] = mutableListOf()
        }

        for (outputSize in sizes) {
            for (key in aspectRatioSizeListMap.keys) {
                // Put the size into all groups that is matched in mod16 condition since a size
                // may match multiple aspect ratio in mod16 algorithm.
                if (hasMatchingAspectRatio(outputSize, key)) {
                    aspectRatioSizeListMap[key]!!.add(outputSize)
                }
            }
        }

        return aspectRatioSizeListMap
    }

    // Skips the tests when the devices have any of the quirks that might affect the selected
    // resolution.
    private fun assumeNotAspectRatioQuirkDevice() {
        assumeFalse(hasAspectRatioLegacyApi21Quirk())
        assumeFalse(hasNexus4AndroidLTargetAspectRatioQuirk())
        assumeFalse(hasExtraCroppingQuirk())
    }

    // Checks whether it is the device for AspectRatioLegacyApi21Quirk
    private fun hasAspectRatioLegacyApi21Quirk(): Boolean {
        val quirks = cameraInfoInternal.cameraQuirks

        return if (implName == CameraPipeConfig::class.simpleName) {
            quirks.contains(
                androidx.camera.camera2.pipe.integration.compat.quirk
                        .AspectRatioLegacyApi21Quirk::class
                    .java
            )
        } else {
            quirks.contains(
                androidx.camera.camera2.internal.compat.quirk.AspectRatioLegacyApi21Quirk::class
                    .java
            )
        }
    }

    // Checks whether it is the device for Nexus4AndroidLTargetAspectRatioQuirk
    private fun hasNexus4AndroidLTargetAspectRatioQuirk() =
        if (implName == CameraPipeConfig::class.simpleName) {
            hasDeviceQuirk(
                androidx.camera.camera2.pipe.integration.compat.quirk
                        .Nexus4AndroidLTargetAspectRatioQuirk::class
                    .java
            )
        } else {
            hasDeviceQuirk(
                androidx.camera.camera2.internal.compat.quirk
                        .Nexus4AndroidLTargetAspectRatioQuirk::class
                    .java
            )
        }

    // Checks whether it is the device for ExtraCroppingQuirk
    private fun hasExtraCroppingQuirk() =
        if (implName == CameraPipeConfig::class.simpleName) {
            hasDeviceQuirk(
                androidx.camera.camera2.pipe.integration.compat.quirk.ExtraCroppingQuirk::class.java
            )
        } else {
            hasDeviceQuirk(
                androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk::class.java
            )
        }

    // Skips the tests when the devices have any of the quirks that might affect the selected
    // resolution.
    private fun assumeNotOutputSizeQuirkDevice() {
        assumeFalse(hasExcludedSupportedSizesQuirk())
        assumeFalse(hasExtraSupportedOutputSizeQuirk())
    }

    private fun hasExcludedSupportedSizesQuirk() =
        if (implName == CameraPipeConfig::class.simpleName) {
            hasDeviceQuirk(
                androidx.camera.camera2.pipe.integration.compat.quirk
                        .ExcludedSupportedSizesQuirk::class
                    .java
            )
        } else {
            hasDeviceQuirk(
                androidx.camera.camera2.internal.compat.quirk.ExcludedSupportedSizesQuirk::class
                    .java
            )
        }

    private fun hasExtraSupportedOutputSizeQuirk() =
        if (implName == CameraPipeConfig::class.simpleName) {
            hasDeviceQuirk(
                androidx.camera.camera2.pipe.integration.compat.quirk
                        .ExtraSupportedOutputSizeQuirk::class
                    .java
            )
        } else {
            hasDeviceQuirk(
                androidx.camera.camera2.internal.compat.quirk.ExtraSupportedOutputSizeQuirk::class
                    .java
            )
        }

    private fun <T : Quirk?> hasDeviceQuirk(quirkClass: Class<T>) =
        if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks.get(quirkClass)
        } else {
            androidx.camera.camera2.internal.compat.quirk.DeviceQuirks.get(quirkClass)
        } != null
}
