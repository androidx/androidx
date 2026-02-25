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

package androidx.camera.integration.featurecombo

import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.integration.featurecombo.AppUseCase.VIDEO_CAPTURE
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FeatureCombinationDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    implName: String,
    cameraXConfig: CameraXConfig,
    private val useCasesToTest: List<AppUseCase>,
) : FeatureGroupTestBase(cameraSelector, implName, cameraXConfig) {
    @Test
    fun bindToLifecycle_allFeaturesPreferred_canBindSuccessfully(): Unit = runBlocking {
        bindAndVerifyFeatures(useCasesToTest.toUseCases(), preferredFeatures = allFeatures.toList())
    }

    @Test
    fun isSessionConfigSupported_queryReturnsFalseWithUnselectedPreferredFeatures(): Unit =
        runBlocking {
            // Arrange: Bind with all features as preferred and store the selected ones.
            val useCases = useCasesToTest.toUseCases()
            val features = allFeatures.toList()
            val selectedFeatures = bindAndVerifyFeatures(useCases, preferredFeatures = features)

            // Act & assert: Ensure query returns false for each of the unselected features added
            //   to the selected ones.
            features.forEach { feature ->
                Log.d(TAG, "selectedFeatures: $selectedFeatures, testing feature: $feature")

                if (selectedFeatures.map { it.featureType }.contains(feature.featureType))
                    return@forEach

                assertWithMessage(
                        "selectedFeatures = $selectedFeatures, newly added feature = $feature"
                    )
                    .that(
                        cameraProvider
                            .getCameraInfo(cameraSelector)
                            .isSessionConfigSupported(
                                SessionConfig(
                                    useCases = useCases,
                                    requiredFeatureGroup = selectedFeatures + feature,
                                )
                            )
                    )
                    .isFalse()
            }
        }

    @Test
    fun recordingFeatureBoundWithSpecificAspectRatioUseCases_aspectRatioMaintained(): Unit =
        runBlocking {
            assumeTrue(useCasesToTest.contains(VIDEO_CAPTURE))

            allFeatures
                .filter { it.featureType == GroupableFeature.FEATURE_TYPE_RECORDING_QUALITY }
                .forEach { feature ->
                    listOf(AspectRatio.RATIO_DEFAULT, AspectRatio.RATIO_4_3, AspectRatio.RATIO_16_9)
                        .forEach { aspectRatio ->
                            val useCases = useCasesToTest.toUseCases(aspectRatio)

                            bindAndVerifyFeatures(
                                useCases,
                                requiredFeatures = setOf(feature),
                                aspectRatio = aspectRatio,
                            )
                        }
                }
        }

    /**
     * A [androidx.camera.core.CameraEffect] targeting only one use case with PRIV format should not
     * change the stream configuration and thus query result should always stay the same.
     */
    @Test
    fun isSessionConfigSupported_effectTargetingPreviewOnly_resultMatchesWithoutEffect(): Unit =
        runBlocking {
            val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
            val useCases = useCasesToTest.toUseCases()
            val effect =
                FakeSurfaceEffect(
                    CameraEffect.PREVIEW,
                    FakeSurfaceProcessorInternal(directExecutor()),
                )

            allFeatures.forEach { feature ->
                val resultWithoutEffect =
                    cameraInfo.isSessionConfigSupported(
                        SessionConfig(useCases = useCases, requiredFeatureGroup = setOf(feature))
                    )

                val resultWithEffect =
                    cameraInfo.isSessionConfigSupported(
                        SessionConfig(
                            useCases = useCases,
                            requiredFeatureGroup = setOf(feature),
                            effects = listOf(effect),
                        )
                    )

                assertWithMessage(
                        "resultWithEffect = $resultWithEffect, resultWithoutEffect = $resultWithoutEffect"
                    )
                    .that(resultWithEffect)
                    .isEqualTo(resultWithoutEffect)
            }
        }

    private suspend fun bindAndVerifyFeatures(
        useCases: List<UseCase>,
        requiredFeatures: Set<GroupableFeature> = emptySet(),
        preferredFeatures: List<GroupableFeature> = emptyList(),
        aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    ): Set<GroupableFeature> {
        val selectedFeatures = CompletableDeferred<Set<GroupableFeature>>()

        val sessionConfig =
            SessionConfig(
                    useCases = useCases,
                    requiredFeatureGroup = requiredFeatures,
                    preferredFeatureGroup = preferredFeatures,
                )
                .apply {
                    setFeatureSelectionListener { features -> selectedFeatures.complete(features) }
                }

        withContext(Dispatchers.Main) {
                assumeTrue(
                    cameraProvider
                        .getCameraInfo(cameraSelector)
                        .isSessionConfigSupported(sessionConfig)
                )

                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, sessionConfig)
            }
            .apply { selectedFeatures.await().verifyFeatures(useCases, cameraInfo, aspectRatio) }

        return selectedFeatures.await()
    }

    companion object {
        private const val TAG = "FeatureCombinationDeviceTest"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing

                    useCaseCombinationsToTest.forEach { useCases ->
                        add(
                            arrayOf(
                                "config=${Camera2Config::class.simpleName} lensFacing={$lens}" +
                                    " useCases = {$useCases}",
                                selector,
                                Camera2Config::class.simpleName,
                                Camera2Config.defaultConfig(),
                                useCases,
                            )
                        )
                    }
                }
            }
    }
}
