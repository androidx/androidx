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

package androidx.camera.core.featuregroup.impl

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featuregroup.impl.ResolvedFeatureGroup.Companion.resolveFeatureGroup
import androidx.camera.core.featuregroup.impl.UseCaseType.IMAGE_CAPTURE
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.Supported
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.Unsupported
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.UseCaseMissing
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolver
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
class ResolvedFeatureGroupTest {
    private val fakeCameraInfo = FakeCameraInfoInternal()

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()

    @Test
    fun resolveFeatureCombination_resultIsSupported_returnsResolvedFeatureCombination() {
        // Arrange: Create a resolver returning a ResolvedFeatureGroup without all the
        // preferred features
        val useCases = setOf(preview, imageCapture)
        val sessionConfig =
            SessionConfig(
                useCases = useCases.toList(),
                preferredFeatureGroup = listOf(HDR_HLG10, IMAGE_ULTRA_HDR),
            )
        val expectedResolvedFeatureGroup = ResolvedFeatureGroup(setOf(HDR_HLG10))
        val resolver =
            object : FeatureGroupResolver {
                override fun resolveFeatureGroup(
                    sessionConfig: SessionConfig
                ): FeatureGroupResolutionResult {
                    return Supported(expectedResolvedFeatureGroup)
                }
            }

        // Act
        val resolvedCombination =
            sessionConfig.resolveFeatureGroup(fakeCameraInfo, resolver = resolver)

        // Assert
        assertThat(resolvedCombination).isEqualTo(expectedResolvedFeatureGroup)
    }

    @Test
    fun resolveFeatureCombination_resultIsUnsupported_throwsException() {
        // Arrange: Create a resolver returning Unsupported result
        val sessionConfig =
            SessionConfig(
                useCases = listOf(preview, imageCapture),
                preferredFeatureGroup = listOf(HDR_HLG10, IMAGE_ULTRA_HDR),
            )
        val resolver =
            object : FeatureGroupResolver {
                override fun resolveFeatureGroup(
                    sessionConfig: SessionConfig
                ): FeatureGroupResolutionResult {
                    return Unsupported
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            sessionConfig.resolveFeatureGroup(fakeCameraInfo, resolver = resolver)
        }
    }

    @Test
    fun resolveFeatureCombination_useCaseMissingForSomeFeature_throwsException() {
        // Arrange: Create a resolver returning UseCaseMissing result
        val sessionConfig =
            SessionConfig(
                useCases = listOf(preview),
                requiredFeatureGroup = setOf(IMAGE_ULTRA_HDR), // but no ImageCapture
                preferredFeatureGroup = listOf(HDR_HLG10, PREVIEW_STABILIZATION),
            )
        val resolver =
            object : FeatureGroupResolver {
                override fun resolveFeatureGroup(
                    sessionConfig: SessionConfig
                ): FeatureGroupResolutionResult {
                    return UseCaseMissing(
                        requiredUseCases = IMAGE_CAPTURE.toString(),
                        featureRequiring = IMAGE_ULTRA_HDR,
                    )
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            sessionConfig.resolveFeatureGroup(fakeCameraInfo, resolver = resolver)
        }
    }

    @Test
    fun resolveFeatureCombination_sessionConfigWithoutAnyFeature_returnsNull() {
        // Arrange: Create a resolver returning a ResolvedFeatureGroup without any feature
        val useCases = setOf(preview, imageCapture)
        val sessionConfig = SessionConfig(useCases = useCases.toList())

        // Act
        val resolvedCombination = sessionConfig.resolveFeatureGroup(fakeCameraInfo)

        // Assert
        assertThat(resolvedCombination).isNull()
    }
}
