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

package androidx.camera.core.featurecombination.impl

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination.Companion.resolveFeatureCombination
import androidx.camera.core.featurecombination.impl.UseCaseType.IMAGE_CAPTURE
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Supported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Unsupported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UseCaseMissing
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolver
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalSessionConfig::class)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class ResolvedFeatureCombinationTest {
    private val fakeCameraInfo = FakeCameraInfoInternal()

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()

    @Test
    fun resolveFeatureCombination_resultIsSupported_returnsResolvedFeatureCombination() {
        // Arrange: Create a resolver returning a ResolvedFeatureCombination without all the
        // preferred features
        val useCases = setOf(preview, imageCapture)
        val sessionConfig =
            SessionConfig(
                useCases = useCases.toList(),
                preferredFeatures = listOf(HDR_HLG10, IMAGE_ULTRA_HDR),
            )
        val expectedResolvedFeatureCombination = ResolvedFeatureCombination(setOf(HDR_HLG10))
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    sessionConfig: SessionConfig
                ): FeatureCombinationResolutionResult {
                    return Supported(expectedResolvedFeatureCombination)
                }
            }

        // Act
        val resolvedCombination =
            sessionConfig.resolveFeatureCombination(fakeCameraInfo, resolver = resolver)

        // Assert
        assertThat(resolvedCombination).isEqualTo(expectedResolvedFeatureCombination)
    }

    @Test
    fun resolveFeatureCombination_resultIsUnsupported_throwsException() {
        // Arrange: Create a resolver returning Unsupported result
        val sessionConfig =
            SessionConfig(
                useCases = listOf(preview, imageCapture),
                preferredFeatures = listOf(HDR_HLG10, IMAGE_ULTRA_HDR),
            )
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    sessionConfig: SessionConfig
                ): FeatureCombinationResolutionResult {
                    return Unsupported
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            sessionConfig.resolveFeatureCombination(fakeCameraInfo, resolver = resolver)
        }
    }

    @Test
    fun resolveFeatureCombination_useCaseMissingForSomeFeature_throwsException() {
        // Arrange: Create a resolver returning UseCaseMissing result
        val sessionConfig =
            SessionConfig(
                useCases = listOf(preview),
                requiredFeatures = setOf(IMAGE_ULTRA_HDR), // but no ImageCapture
                preferredFeatures = listOf(HDR_HLG10, PREVIEW_STABILIZATION),
            )
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    sessionConfig: SessionConfig
                ): FeatureCombinationResolutionResult {
                    return UseCaseMissing(
                        requiredUseCases = IMAGE_CAPTURE.toString(),
                        featureRequiring = IMAGE_ULTRA_HDR,
                    )
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            sessionConfig.resolveFeatureCombination(fakeCameraInfo, resolver = resolver)
        }
    }

    @Test
    fun resolveFeatureCombination_sessionConfigWithoutAnyFeature_returnsNull() {
        // Arrange: Create a resolver returning a ResolvedFeatureCombination without any feature
        val useCases = setOf(preview, imageCapture)
        val sessionConfig = SessionConfig(useCases = useCases.toList())

        // Act
        val resolvedCombination = sessionConfig.resolveFeatureCombination(fakeCameraInfo)

        // Assert
        assertThat(resolvedCombination).isNull()
    }
}
