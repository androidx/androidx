/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core

import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SessionConfigTest {
    val useCases = listOf(Preview.Builder().build(), ImageCapture.Builder().build())
    val viewPort = ViewPort.Builder(Rational(4, 3), Surface.ROTATION_0).build()
    val effects =
        listOf(FakeSurfaceEffect(directExecutor(), FakeSurfaceProcessor(directExecutor())))
    val frameRateRange = Range(30, 30)

    @Test
    fun sessionConfig_constructorInitializesFields() {
        val sessionConfig = SessionConfig(useCases, viewPort, effects, frameRateRange)

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).isEqualTo(effects)
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_constructorOverrideFields() {
        class ExtendedSessionConfig(
            useCases: List<UseCase>,
            viewPort: ViewPort,
            effects: List<CameraEffect> = emptyList(),
            frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
        ) : SessionConfig(useCases, viewPort, effects, frameRateRange) {
            override val sessionType: Int = SESSION_TYPE_HIGH_SPEED
        }
        val sessionConfig = ExtendedSessionConfig(useCases, viewPort, effects, frameRateRange)

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).isEqualTo(effects)
        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_HIGH_SPEED)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_defaultConstructorInitializesWithDefaults() {
        val sessionConfig = SessionConfig(useCases)

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isNull()
        assertThat(sessionConfig.effects).isEmpty()
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(sessionConfig.frameRateRange).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_frameRateRangeSetOnSessionConfigAndUseCase_IllegalArgumentExceptionThrown() {
        val useCase = Preview.Builder().setTargetFrameRate(frameRateRange).build()
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(useCase), frameRateRange = frameRateRange)
        }
    }

    @Test
    fun sessionConfig_useCaseHasFrameRateAfterBinding_noExceptionThrown() {
        val useCase = Preview.Builder().build()

        // Binding UseCase with an arbitrary frame rate.
        val configWithFrameRate = Preview.Builder().setTargetFrameRate(Range(15, 15)).useCaseConfig
        useCase.bindToCamera(FakeCamera(), null, null, configWithFrameRate)
        assertThat(useCase.currentConfig.hasTargetFrameRate()).isTrue()

        // No exception is thrown.
        SessionConfig(useCases = listOf(useCase), frameRateRange = frameRateRange)
    }

    @Test
    fun sessionConfig_builderSetsViewPort() {
        val sessionConfig = SessionConfig.Builder(useCases).setViewPort(viewPort).build()

        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
    }

    @Test
    fun sessionConfig_builderAddsEffect() {
        val effect1 = mock(CameraEffect::class.java)
        val effect2 = mock(CameraEffect::class.java)
        val sessionConfig =
            SessionConfig.Builder(useCases).addEffect(effect1).addEffect(effect2).build()

        assertThat(sessionConfig.effects).containsExactly(effect1, effect2)
    }

    @Test
    fun sessionConfig_builderSetsFrameRateRange() {
        val sessionConfig =
            SessionConfig.Builder(useCases).setFrameRateRange(frameRateRange).build()

        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
    }

    @Test
    fun sessionConfig_builderSetsRequiredFeatures() {
        val sessionConfig =
            SessionConfig.Builder(useCases)
                .setRequiredFeatureGroup(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION)
                .build()

        assertThat(sessionConfig.requiredFeatureGroup)
            .containsExactly(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION)
    }

    @Test
    fun sessionConfig_builderSetsPreferredFeatures() {
        val sessionConfig =
            SessionConfig.Builder(useCases)
                .setPreferredFeatureGroup(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION)
                .build()

        assertThat(sessionConfig.preferredFeatureGroup)
            .containsExactly(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION)
            .inOrder()
    }

    @Test
    fun sessionConfig_builderBuildsCorrectSessionConfig() {
        val effect = mock(CameraEffect::class.java)
        val sessionConfig =
            SessionConfig.Builder(useCases)
                .setViewPort(viewPort)
                .addEffect(effect)
                .setFrameRateRange(frameRateRange)
                .build()

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect)
        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_builderBuildsCorrectSessionConfigWithVarargUseCase() {
        val effect = mock(CameraEffect::class.java)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val sessionConfig =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setViewPort(viewPort)
                .addEffect(effect)
                .setFrameRateRange(frameRateRange)
                .build()

        assertThat(sessionConfig.useCases).containsExactly(preview, imageCapture, imageAnalysis)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect)
        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_notAffectedByBuilderAfterBuilt() {
        val mutableUseCasesList = useCases.toMutableList()
        val effect1 = mock(CameraEffect::class.java)
        val effect2 = mock(CameraEffect::class.java)
        val viewPort2 = ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()

        val builder =
            SessionConfig.Builder(mutableUseCasesList).setViewPort(viewPort).addEffect(effect1)

        val sessionConfig = builder.build()
        builder.addEffect(effect2)
        builder.setViewPort(viewPort2)
        builder.setRequiredFeatureGroup(HDR_HLG10)
        mutableUseCasesList.add(ImageAnalysis.Builder().build())

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect1)
    }

    @Test
    fun sessionConfig_featuresModifiedAfterBuild_notAffectedByBuilderAfterBuilt() {
        val mutableUseCasesList = useCases.toMutableList()
        val viewPort2 = ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()

        val builder =
            SessionConfig.Builder(mutableUseCasesList)
                .setViewPort(viewPort)
                .setFrameRateRange(frameRateRange)
                .setRequiredFeatureGroup(FPS_60)
                .setPreferredFeatureGroup(IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION)

        val sessionConfig = builder.build()
        builder.setViewPort(viewPort2)
        builder.setFrameRateRange(Range(60, 60))
        builder.setRequiredFeatureGroup(HDR_HLG10)
        builder.setPreferredFeatureGroup(PREVIEW_STABILIZATION)
        mutableUseCasesList.add(ImageAnalysis.Builder().build())

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.frameRateRange).isEqualTo(frameRateRange)
        assertThat(sessionConfig.requiredFeatureGroup).containsExactly(FPS_60)
        assertThat(sessionConfig.preferredFeatureGroup)
            .containsExactly(IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION)
    }

    @Test
    fun sessionConfig_useCasesRemovedDuplicatesAndKeepOrder() {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val useCases1 = listOf(imageCapture, preview, preview, imageCapture)
        val useCases2 = listOf(imageCapture, preview, preview, imageCapture)

        val sessionConfigViaBuilder = SessionConfig.Builder(useCases1).build()
        val sessionConfigViaConstructor = SessionConfig(useCases2)
        assertThat(sessionConfigViaBuilder.useCases)
            .containsExactly(imageCapture, preview)
            .inOrder()
        assertThat(sessionConfigViaConstructor.useCases)
            .containsExactly(imageCapture, preview)
            .inOrder()
    }

    @Test
    fun sessionConfig_constructorWithVarargUseCase() {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val sessionConfig = SessionConfig(preview, imageCapture, imageAnalysis)

        assertThat(sessionConfig.useCases).containsExactly(preview, imageCapture, imageAnalysis)
        assertThat(sessionConfig.viewPort).isEqualTo(null)
        assertThat(sessionConfig.effects).isEmpty()
        assertThat(sessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(sessionConfig.frameRateRange).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.requiredFeatureGroup).isEmpty()
        assertThat(sessionConfig.preferredFeatureGroup).isEmpty()
        assertThat(sessionConfig.isLegacy).isFalse()
    }

    @Test
    fun sessionConfig_reqFeaturesConflictingForSameType_throwsIllegalArgExceptionWithCorrectMsg() {
        // Arrange
        val requiredFeatures =
            setOf(
                FPS_60,
                FakeDynamicRangeFeature(DynamicRange.SDR),
                FakeDynamicRangeFeature(DynamicRange.HLG_10_BIT),
            )

        // Act & assert
        val exception =
            try {
                SessionConfig(useCases = useCases, requiredFeatureGroup = requiredFeatures)
                null
            } catch (e: Exception) {
                e
            }

        // Assert
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception).hasMessageThat().contains("{encoding=SDR, bitDepth=8}")
        assertThat(exception).hasMessageThat().contains("{encoding=HLG, bitDepth=10}")
    }

    @Test
    fun sessionConfig_noConflictingRequiredFeatures_noExceptionThrown() {
        // Arrange
        val requiredFeatures = setOf(FPS_60, IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION)

        // Act & assert
        SessionConfig(useCases = useCases, requiredFeatureGroup = requiredFeatures)
    }

    @Test
    fun sessionConfig_sameFeatureTwiceInPreferredFeatures_illegalArgumentExceptionThrown() {
        // Arrange
        val features = listOf(FPS_60, IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)

        // Act & assert
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = useCases, preferredFeatureGroup = features)
        }
    }

    @Test
    fun sessionConfig_requiredFeatureAlsoInPreferredFeatures_illegalArgumentExceptionThrown() {
        // Act & assert
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = useCases,
                requiredFeatureGroup = setOf(FPS_60),
                preferredFeatureGroup = listOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION),
            )
        }
    }

    @Test
    fun sessionConfig_imageAnalysisAddedWithFeatureParam_illegalArgumentExceptionThrown() {
        // Arrange
        val features = listOf(FPS_60)

        // Act & assert
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(ImageAnalysis.Builder().build()),
                preferredFeatureGroup = features,
            )
        }
    }

    @Test
    fun sessionConfig_imageAnalysisAddedWithoutFeatureParam_noExceptionThrown() {
        // Act & assert
        SessionConfig(useCases = listOf(ImageAnalysis.Builder().build()))
    }

    @Test
    fun sessionConfig_effectAddedWithFeatureParam_noExceptionThrown() {
        // Arrange
        val features = listOf(FPS_60)

        SessionConfig(useCases = useCases, preferredFeatureGroup = features, effects = effects)
    }

    @Test
    fun sessionConfig_effectAddedWithoutFeatureParam_noExceptionThrown() {
        // Act & assert
        SessionConfig(useCases = useCases, effects = effects)
    }

    @Test
    fun sessionConfig_dynamicRangeSetToUseCaseWithAnyGroupableFeature_illegalArgExceptionThrown() {
        val sdrPreview = Preview.Builder().setDynamicRange(DynamicRange.SDR).build()
        val hdrPreview = Preview.Builder().setDynamicRange(DynamicRange.HLG_10_BIT).build()

        // SDR set to use case, preferred feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(sdrPreview), preferredFeatureGroup = listOf(FPS_60))
        }

        // HDR set to use case, preferred feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(hdrPreview), preferredFeatureGroup = listOf(FPS_60))
        }

        // SDR set to use case, required feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(sdrPreview), requiredFeatureGroup = setOf(FPS_60))
        }

        // HDR set to use case, required feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(hdrPreview), requiredFeatureGroup = setOf(FPS_60))
        }
    }

    @Test
    fun sessionConfig_recordingQualitySetToUseCaseWithAnyFeature_illegalArgExceptionThrown() {
        val sdVideoCapture = createVideoCapture(Quality.SD)
        val hdVideoCapture = createVideoCapture(Quality.HD)
        val unspecifiedQualityVideoCapture = createVideoCapture()

        // Verifications for preferred features

        // SD set to VideoCapture, preferred feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(sdVideoCapture), preferredFeatureGroup = listOf(FPS_60))
        }

        // HD set to VideoCapture, preferred feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(hdVideoCapture), preferredFeatureGroup = listOf(FPS_60))
        }

        // Default VideoCapture, preferred feature group set with another feature, no exception
        SessionConfig(
            useCases = listOf(unspecifiedQualityVideoCapture),
            preferredFeatureGroup = listOf(FPS_60),
        )

        // Verifications for required features

        // SD set to VideoCapture, required feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(sdVideoCapture), requiredFeatureGroup = setOf(FPS_60))
        }

        // HD set to VideoCapture, required feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(hdVideoCapture), requiredFeatureGroup = setOf(FPS_60))
        }

        // Default VideoCapture, required feature group set with another feature, no exception
        SessionConfig(
            useCases = listOf(unspecifiedQualityVideoCapture),
            requiredFeatureGroup = setOf(FPS_60),
        )
    }

    @Test
    fun sessionConfig_fpsRangeSetToUseCaseWithAnyGroupableFeature_illegalArgExceptionThrown() {
        val preview30Fps = Preview.Builder().setTargetFrameRate(Range(30, 30)).build()
        val preview60Fps = Preview.Builder().setTargetFrameRate(Range(60, 60)).build()

        // 30 FPS set to use case, preferred feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(preview30Fps),
                preferredFeatureGroup = listOf(HDR_HLG10),
            )
        }

        // 60 FPS set to use case, preferred feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(preview60Fps),
                preferredFeatureGroup = listOf(HDR_HLG10),
            )
        }

        // 30 FPS set to use case, required feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(preview30Fps), requiredFeatureGroup = setOf(HDR_HLG10))
        }

        // 60 FPS set to use case, required feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(useCases = listOf(preview60Fps), requiredFeatureGroup = setOf(HDR_HLG10))
        }
    }

    @Test
    fun sessionConfig_stabilizationSetToUseCaseWithAnyGroupableFeature_illegalArgExceptionThrown() {
        val noStabilizationPreview = Preview.Builder().setPreviewStabilizationEnabled(false).build()
        val stabilizationEnabledPreview =
            Preview.Builder().setPreviewStabilizationEnabled(true).build()

        // stabilization disabled in use case, preferred feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(noStabilizationPreview),
                preferredFeatureGroup = listOf(HDR_HLG10),
            )
        }

        // stabilization enabled in use case, preferred feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(stabilizationEnabledPreview),
                preferredFeatureGroup = listOf(HDR_HLG10),
            )
        }

        // stabilization disabled in use case, required feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(noStabilizationPreview),
                requiredFeatureGroup = setOf(HDR_HLG10),
            )
        }

        // stabilization enabled in use case, required feature group also set with some feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(stabilizationEnabledPreview),
                requiredFeatureGroup = setOf(HDR_HLG10),
            )
        }
    }

    @Test
    fun sessionConfig_jpegRSetToUseCaseWithAnyGroupableFeature_illegalArgExceptionThrown() {
        val jpegRImageCapture =
            ImageCapture.Builder()
                .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                .build()

        // JPEG_R set to use case, preferred feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(jpegRImageCapture),
                preferredFeatureGroup = listOf(FPS_60),
            )
        }

        // JPEG_R set to use case, required feature group also set with another feature
        assertThrows<IllegalArgumentException> {
            SessionConfig(
                useCases = listOf(jpegRImageCapture),
                requiredFeatureGroup = setOf(FPS_60),
            )
        }
    }

    @Test
    fun legacySessionConfig_constructorInitializesFields() {
        val legacySessionConfig = LegacySessionConfig(useCases, viewPort, effects)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(legacySessionConfig.effects).isEqualTo(effects)
        assertThat(legacySessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(legacySessionConfig.frameRateRange).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(legacySessionConfig.isLegacy).isTrue()
    }

    @Test
    fun legacySessionConfig_defaultConstructorInitializesWithDefaults() {
        val legacySessionConfig = LegacySessionConfig(useCases)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isNull()
        assertThat(legacySessionConfig.effects).isEmpty()
        assertThat(legacySessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(legacySessionConfig.frameRateRange).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(legacySessionConfig.isLegacy).isTrue()
    }

    @Test
    fun legacySessionConfig_secondaryConstructorInitializesFromUseCaseGroup() {
        val useCaseGroup =
            UseCaseGroup.Builder()
                .apply {
                    useCases.forEach { addUseCase(it) }
                    setViewPort(viewPort)
                    effects.forEach { addEffect(it) }
                }
                .build()

        val legacySessionConfig = LegacySessionConfig(useCaseGroup)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(legacySessionConfig.effects).isEqualTo(effects)
        assertThat(legacySessionConfig.sessionType).isEqualTo(SESSION_TYPE_REGULAR)
        assertThat(legacySessionConfig.frameRateRange).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(legacySessionConfig.isLegacy).isTrue()
    }

    @Test
    fun sessionConfig_toStringContainsAllPropertiesCorrectly() {
        assertThat(SessionConfig(useCases, viewPort, effects, frameRateRange).toString()).apply {
            contains("useCases=$useCases")
            contains("viewPort=$viewPort")
            contains("effects=$effects")
            contains("frameRateRange=$frameRateRange")
            contains("requiredFeatureGroup=[]")
            contains("preferredFeatureGroup=[]")
        }

        val requiredFeatures = setOf(HDR_HLG10)
        val preferredFeatures = listOf(FPS_60, PREVIEW_STABILIZATION)

        assertThat(
                SessionConfig(
                        useCases,
                        requiredFeatureGroup = requiredFeatures,
                        preferredFeatureGroup = preferredFeatures,
                    )
                    .toString()
            )
            .apply {
                contains("useCases=$useCases")
                contains("requiredFeatureGroup=$requiredFeatures")
                contains("preferredFeatureGroup=$preferredFeatures")
                contains("viewPort=null")
                contains("effects=[]")
                contains("frameRateRange=[0, 0]")
            }
    }

    @Test
    fun sessionConfig_emptyUseCaseList_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> { SessionConfig(emptyList()) }
    }

    @Test
    fun sessionConfig_emptyUseCaseListWithRequireNonEmptyUseCasesFalse_noException() {
        // Internal constructor to bypass public API restrictions
        class TestSessionConfig : SessionConfig(emptyList()) {
            override val requireNonEmptyUseCases: Boolean = false
        }
        TestSessionConfig() // Should not throw
    }

    @Test
    fun legacySessionConfig_emptyUseCaseList_noException() {
        LegacySessionConfig(emptyList())
    }

    @Test
    fun sessionConfig_nonEmptyUseCaseList_noException() {
        SessionConfig(useCases) // Should not throw
    }

    private fun createVideoCapture(quality: Quality? = null): VideoCapture<Recorder> {
        return VideoCapture.withOutput(
            Recorder.Builder()
                .apply {
                    if (quality != null) {
                        setQualitySelector(QualitySelector.from(quality))
                    }
                }
                .build()
        )
    }

    data class FakeDynamicRangeFeature(private val dynamicRange: DynamicRange) :
        GroupableFeature() {
        override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.DYNAMIC_RANGE
    }
}
