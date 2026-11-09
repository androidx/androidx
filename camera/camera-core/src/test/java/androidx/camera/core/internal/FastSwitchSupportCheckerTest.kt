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

package androidx.camera.core.internal

import android.graphics.ImageFormat
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraUseCaseAdapterProvider
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.impl.resolver.DefaultFeatureGroupResolver
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.HardwareConfigQueryResult
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

private const val PRIV = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
private const val JPEG = ImageFormat.JPEG
private const val YUV = ImageFormat.YUV_420_888

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class FastSwitchSupportCheckerTest {
    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder().build()

    private val surfaceManager = FakeCameraDeviceSurfaceManager()
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()

    private val photoAndScan =
        SessionConfig.Builder(preview, imageCapture, imageAnalysis)
            .setInitialEnabledUseCases(preview, imageCapture)
            .build()

    @Test
    fun supported_whenTheUnionOfAllUseCasesIsSupported() {
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isTrue()
    }

    @Test
    fun unsupported_whenStreamCombinationIsNotSupported() {
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG), listOf(PRIV, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
    }

    @Test
    fun supported_whenMultiplePreviewsCanShareAStream() {
        val secondPreview = Preview.Builder().build()
        val config =
            SessionConfig.Builder(preview, secondPreview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(preview, imageCapture)
                .build()
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isTrue()
    }

    @Test
    fun unsupported_whenInitialEnabledUseCasesRequiresMeteringRepeatingAndComboUnsupported() {
        val config =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(imageCapture)
                .build()
        // PRIV + JPEG + YUV is supported, but adding a 2nd PRIV stream for MeteringRepeating is
        // not.
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun supported_whenInitialEnabledUseCasesRequiresMeteringRepeatingAndComboSupported() {
        val config =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(imageCapture)
                .build()
        val cameraInfo =
            createCameraInfo(setOf(listOf(PRIV, JPEG, YUV), listOf(PRIV, JPEG, YUV, PRIV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isTrue()
    }

    @Test
    fun unsupported_whenConfigHasACameraFilter() {
        val cameraFilter = CameraFilter { it }
        val config =
            SessionConfig(
                useCases = listOf(preview, imageCapture, imageAnalysis),
                cameraFilter = cameraFilter,
                initialEnabledUseCases = listOf(preview, imageCapture),
            )
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun unsupported_whenAnExtensionIsEnabled() {
        val cameraInfo =
            AdapterCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))),
                FakeCameraConfig(sessionProcessor = FakeSessionProcessor()),
            )

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
    }

    @Test
    fun supported_adapterCameraInfoWithoutExtension() {
        val cameraInfo =
            AdapterCameraInfo(createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))), FakeCameraConfig())

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isTrue()
    }

    @Test
    fun unsupported_whenSessionTypeIsNotRegular() {
        val config =
            SessionConfig(
                useCases = listOf(preview, imageCapture, imageAnalysis),
                sessionType = SESSION_TYPE_HIGH_SPEED,
                initialEnabledUseCases = listOf(preview, imageCapture),
            )
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun unsupported_whenTheHardwareQueryReportsUnsupported() {
        val cameraInfo =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))),
                HardwareConfigQueryResult.UNSUPPORTED,
            )

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
        assertThat(cameraInfo.queriedConfig).isSameInstanceAs(photoAndScan)
        assertThat(cameraInfo.queriedStreams!!.map { it.useCaseConfig.javaClass })
            .containsExactly(
                PreviewConfig::class.java,
                ImageCaptureConfig::class.java,
                ImageAnalysisConfig::class.java,
            )
    }

    @Test
    fun supported_whenTheHardwareQueryReportsUnknown() {
        val cameraInfo =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))),
                HardwareConfigQueryResult.UNKNOWN,
            )

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isTrue()
        assertThat(cameraInfo.queriedStreams).hasSize(3)
    }

    @Test
    fun supported_whenTheHardwareQueryThrows() {
        val cameraInfo =
            HardwareQueryCameraInfo(createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))), null)

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isTrue()
    }

    @Test
    fun hardwareQueryIsNotCalled_whenStreamCombinationFails() {
        val cameraInfo =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG))),
                HardwareConfigQueryResult.UNSUPPORTED,
            )

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
        assertThat(cameraInfo.queriedConfig).isNull()
    }

    @Test
    fun forwardingCameraInfo_forwardsTheFastSwitchHardwareQuery() {
        val delegate =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))),
                HardwareConfigQueryResult.SUPPORTED,
            )
        val forwarding = ForwardingCameraInfo(delegate)

        assertThat(forwarding.queryFastSwitchHardwareSupport(photoAndScan, emptyList()))
            .isEqualTo(HardwareConfigQueryResult.SUPPORTED)
    }

    @Test
    fun defaultHardwareQueryHook_returnsUnknown() {
        val cameraInfo = FakeCameraInfoInternal()

        assertThat(cameraInfo.queryFastSwitchHardwareSupport(photoAndScan, emptyList()))
            .isEqualTo(HardwareConfigQueryResult.UNKNOWN)
    }

    @Test
    fun unsupported_whenRequiredFeatureGroupIsSet() {
        val config =
            SessionConfig.Builder(preview, imageCapture)
                .setRequiredFeatureGroup(HDR_HLG10)
                .setInitialEnabledUseCases(preview)
                .build()
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG), listOf(PRIV, JPEG, PRIV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
        assertThat(DefaultFeatureGroupResolver(cameraInfo).resolveFeatureGroup(config))
            .isInstanceOf(FeatureGroupResolutionResult.Unsupported::class.java)
    }

    @Test
    fun unsupported_whenPreferredFeatureGroupIsSet() {
        val config =
            SessionConfig.Builder(preview, imageCapture)
                .setPreferredFeatureGroup(FPS_60)
                .setInitialEnabledUseCases(preview)
                .build()
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG), listOf(PRIV, JPEG, PRIV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
        assertThat(DefaultFeatureGroupResolver(cameraInfo).resolveFeatureGroup(config))
            .isInstanceOf(FeatureGroupResolutionResult.Unsupported::class.java)
    }

    @Test
    fun hardwareQueryIncludesMeteringRepeating_whenSupportedAlongsideBoundUseCases() {
        // When the 4-stream combination (PRIV + JPEG + YUV + PRIV) is supported in the table,
        // UseCaseManager will pre-attach MeteringRepeating even when initialEnabledUseCases
        // includes Preview; verify queryFastSwitchHardwareSupport receives all 4 streams.
        val cameraInfo =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV), listOf(PRIV, JPEG, YUV, PRIV))),
                HardwareConfigQueryResult.SUPPORTED,
            )

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isTrue()
        assertThat(cameraInfo.queriedStreams).hasSize(4)
    }

    @Test
    fun hardwareQueryOmitsMeteringRepeating_whenRepeatingStreamNotForced() {
        val config =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(imageCapture)
                .build()
        val delegate =
            object :
                ForwardingCameraInfo(
                    // 4th PRIV stream is NOT supported, but repeating stream is not forced.
                    createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))
                ) {
                override fun isRepeatingStreamForced(): Boolean = false
            }
        val cameraInfo = HardwareQueryCameraInfo(delegate, HardwareConfigQueryResult.SUPPORTED)

        assertThat(cameraInfo.isSessionConfigSupported(config)).isTrue()
        assertThat(cameraInfo.queriedStreams).hasSize(3)
    }

    @Test
    fun unsupported_whenResolveRepeatingStreamThrowsAndInitialSubsetRequiresMetering() {
        val config =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(imageCapture)
                .build()
        val cameraInfo =
            object : ForwardingCameraInfo(createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))) {
                override fun resolveRepeatingStream(
                    unionInfo: CalculatedUseCaseInfo,
                    streams: List<ResolvedStreamInfo>,
                ): ResolvedStreamInfo? {
                    throw IllegalStateException("Unexpected error")
                }
            }

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun unsupported_whenZeroShutterLagIsEnabled() {
        val zslImageCapture =
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .build()
        val config =
            SessionConfig.Builder(preview, zslImageCapture)
                .setInitialEnabledUseCases(preview)
                .build()
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG), listOf(PRIV, JPEG, PRIV)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun unsupported_whenConcurrentCameraModeIsActive() {
        val coordinator =
            FakeCameraCoordinator().apply {
                cameraOperatingMode =
                    androidx.camera.core.concurrent.CameraCoordinator
                        .CAMERA_OPERATING_MODE_CONCURRENT
            }
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)), coordinator)

        assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
    }

    @Test
    fun sessionInteropConfig_isPreservedOnQueriedConfigWithoutMutatingUseCaseConfigs() {
        val customOption =
            androidx.camera.core.impl.Config.Option.create<String>(
                "camera2.captureRequest.option.testKey",
                String::class.java,
            )
        val previewWithPhysicalId =
            Preview.Builder().build().apply { setPhysicalCameraId("physical-2") }
        val config =
            SessionConfig.Builder(previewWithPhysicalId, imageCapture, imageAnalysis)
                .setInterop { builder ->
                    builder.interopMutableConfig.insertOption(customOption, "testValue")
                }
                .setInitialEnabledUseCases(previewWithPhysicalId, imageCapture)
                .build()
        val cameraInfo =
            HardwareQueryCameraInfo(
                createCameraInfo(setOf(listOf(PRIV, JPEG, YUV))),
                HardwareConfigQueryResult.SUPPORTED,
            )

        assertThat(cameraInfo.isSessionConfigSupported(config)).isTrue()
        assertThat(cameraInfo.queriedConfig!!.interopConfig.retrieveOption(customOption, null))
            .isEqualTo("testValue")
        assertThat(
                cameraInfo.queriedStreams!!.map {
                    it.useCaseConfig.retrieveOption(customOption, null)
                }
            )
            .containsExactly(null, null, null)
        assertThat(cameraInfo.queriedStreams!!.map { it.physicalCameraId })
            .containsExactly("physical-2", null, null)
    }

    @Test
    fun unsupported_whenCamera2InteropSetsNonRegularSessionType() {
        val sessionTypeOption =
            androidx.camera.core.impl.Config.Option.create<Int>(
                "camera2.cameraCaptureSession.sessionType",
                Int::class.javaObjectType,
            )
        val config =
            SessionConfig.Builder(preview, imageCapture)
                .setInterop { builder ->
                    builder.interopMutableConfig.insertOption(sessionTypeOption, 1)
                }
                .setInitialEnabledUseCases(preview)
                .build()
        val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG)))

        assertThat(cameraInfo.isSessionConfigSupported(config)).isFalse()
    }

    @Test
    fun getSupportedFrameRateRanges_filtersByFastSwitchHardwareSupportAndPreconditions() {
        val fps15 = Range(15, 15)
        val fps30 = Range(30, 30)
        val fps60 = Range(60, 60)
        val sizeUhd = Size(3840, 2160)
        val sizeFhd = Size(1920, 1080)

        // Calculator resolves 4K when targetFrameRate is UNSPECIFIED or <= 30fps, and 1080p when
        // targetFrameRate is 60fps.
        val fpsAwareCalculator =
            object : StreamSpecsCalculator {
                override fun calculateSuggestedStreamSpecs(
                    cameraMode: Int,
                    cameraInfoInternal: CameraInfoInternal,
                    newUseCases: List<androidx.camera.core.UseCase>,
                    attachedUseCases: List<androidx.camera.core.UseCase>,
                    cameraConfig: androidx.camera.core.impl.CameraConfig,
                    sessionType: Int,
                    targetFrameRate: Range<Int>,
                    isFeatureComboInvocation: Boolean,
                    findSupportedFrameRateRanges: Boolean,
                ): StreamSpecQueryResult {
                    if (newUseCases.size + attachedUseCases.size > 3) {
                        throw IllegalArgumentException("More than 3 streams not supported")
                    }
                    val chosenSize = if (targetFrameRate == fps60) sizeFhd else sizeUhd
                    val specs = newUseCases.associateWith {
                        androidx.camera.core.impl.StreamSpec.builder(chosenSize)
                            .setExpectedFrameRateRange(targetFrameRate)
                            .build()
                    }
                    return StreamSpecQueryResult(
                        streamSpecs = specs,
                        supportedFrameRateRanges =
                            if (findSupportedFrameRateRanges) setOf(fps15, fps30, fps60) else null,
                    )
                }
            }
        val delegate = FakeCameraInfoInternal(fpsAwareCalculator)
        val adapter =
            CameraUseCaseAdapter(
                FakeCamera(FakeCameraControl(), delegate),
                FakeCameraCoordinator(),
                fpsAwareCalculator,
                useCaseConfigFactory,
            )
        delegate.setCameraUseCaseAdapterProvider(
            object : CameraUseCaseAdapterProvider {
                override fun provide(cameraId: String): CameraUseCaseAdapter = adapter

                override fun provide(
                    camera: CameraInternal,
                    secondaryCamera: CameraInternal?,
                    adapterCameraInfo: AdapterCameraInfo,
                    secondaryAdapterCameraInfo: AdapterCameraInfo?,
                    compositionSettings: CompositionSettings,
                    secondaryCompositionSettings: CompositionSettings,
                ): CameraUseCaseAdapter = adapter
            }
        )

        val cameraInfo =
            object : ForwardingCameraInfo(delegate) {
                override fun getSupportedFrameRateRanges(): Set<Range<Int>> =
                    setOf(fps15, fps30, fps60)

                override fun queryFastSwitchHardwareSupport(
                    config: SessionConfig,
                    streams: List<ResolvedStreamInfo>,
                ): HardwareConfigQueryResult {
                    val spec = streams.first().streamSpec
                    val fps = spec.expectedFrameRateRange
                    // Hardware rejects 15fps, and rejects 4K at 60fps, but supports 1080p at 60fps
                    // and 4K at 30fps.
                    return if (fps == fps15 || (fps == fps60 && spec.resolution == sizeUhd)) {
                        HardwareConfigQueryResult.UNSUPPORTED
                    } else {
                        HardwareConfigQueryResult.SUPPORTED
                    }
                }
            }

        // fps30 (at 4K) and fps60 (re-simulated at 1080p) are supported; fps15 is rejected.
        assertThat(cameraInfo.getSupportedFrameRateRanges(photoAndScan))
            .containsExactly(fps30, fps60)

        // When initialEnabledUseCases has no repeating stream and MeteringRepeating cannot fit,
        // getSupportedFrameRateRanges returns an empty set.
        val nonRepeatingInitial =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setInitialEnabledUseCases(imageCapture)
                .build()
        assertThat(cameraInfo.getSupportedFrameRateRanges(nonRepeatingInitial)).isEmpty()

        // When Extensions SessionProcessor is active on AdapterCameraInfo, returns an empty set.
        val extensionsCameraInfo =
            AdapterCameraInfo(
                cameraInfo,
                FakeCameraConfig(sessionProcessor = FakeSessionProcessor()),
            )
        assertThat(extensionsCameraInfo.getSupportedFrameRateRanges(photoAndScan)).isEmpty()
    }

    private fun createCameraInfo(
        validSurfaceCombos: Set<List<Int>>,
        cameraCoordinator: FakeCameraCoordinator = FakeCameraCoordinator(),
    ): FakeCameraInfoInternal {
        surfaceManager.setValidSurfaceCombos(validSurfaceCombos)
        val calculator = StreamSpecsCalculatorImpl(useCaseConfigFactory, surfaceManager)
        val cameraInfo =
            FakeCameraInfoInternal(calculator).apply {
                val sizes = listOf(Size(1920, 1080), Size(640, 480))
                setSupportedResolutions(PRIV, sizes)
                setSupportedResolutions(JPEG, sizes)
                setSupportedResolutions(YUV, sizes)
            }
        val adapter =
            CameraUseCaseAdapter(
                FakeCamera(FakeCameraControl(), cameraInfo),
                cameraCoordinator,
                calculator,
                useCaseConfigFactory,
            )
        cameraInfo.setCameraUseCaseAdapterProvider(
            object : CameraUseCaseAdapterProvider {
                override fun provide(cameraId: String): CameraUseCaseAdapter = adapter

                override fun provide(
                    camera: CameraInternal,
                    secondaryCamera: CameraInternal?,
                    adapterCameraInfo: AdapterCameraInfo,
                    secondaryAdapterCameraInfo: AdapterCameraInfo?,
                    compositionSettings: CompositionSettings,
                    secondaryCompositionSettings: CompositionSettings,
                ): CameraUseCaseAdapter = adapter
            }
        )
        return cameraInfo
    }

    @Test
    fun isFastSwitchPreconditionsMet_returnsFalse_whenFastSessionSwitchDisablerQuirkLoaded() {
        val initialBrand = Build.BRAND
        val initialModel = Build.MODEL
        try {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "motorola")
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "moto c")

            val cameraInfo = createCameraInfo(setOf(listOf(PRIV, JPEG, YUV)))

            assertThat(
                    FastSwitchSupportChecker.isFastSwitchPreconditionsMet(cameraInfo, photoAndScan)
                )
                .isFalse()
            assertThat(cameraInfo.isSessionConfigSupported(photoAndScan)).isFalse()
            assertThat(
                    FastSwitchSupportChecker.filterFastSwitchSupportedFrameRates(
                        cameraInfo,
                        photoAndScan,
                        setOf(Range(30, 30)),
                    )
                )
                .isEmpty()
        } finally {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", initialBrand)
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", initialModel)
        }
    }

    /**
     * Reports [result] from the hardware query, or throws if [result] is null, and records the
     * query arguments.
     */
    private class HardwareQueryCameraInfo(
        cameraInfo: CameraInfoInternal,
        private val result: HardwareConfigQueryResult?,
    ) : ForwardingCameraInfo(cameraInfo) {
        var queriedConfig: SessionConfig? = null
        var queriedStreams: List<ResolvedStreamInfo>? = null

        override fun queryFastSwitchHardwareSupport(
            config: SessionConfig,
            streams: List<ResolvedStreamInfo>,
        ): HardwareConfigQueryResult {
            queriedConfig = config
            queriedStreams = streams
            return result ?: throw IllegalStateException("Query failed")
        }
    }
}
