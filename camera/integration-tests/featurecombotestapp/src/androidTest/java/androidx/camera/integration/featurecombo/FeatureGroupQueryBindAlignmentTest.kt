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
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.integration.featurecombo.FeatureGroupQueryBindAlignmentTest.VerificationScenario.PREFERRED_FEATURES
import androidx.camera.integration.featurecombo.FeatureGroupQueryBindAlignmentTest.VerificationScenario.REQUIRED_FEATURES
import androidx.camera.testing.impl.CameraUtil
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FeatureGroupQueryBindAlignmentTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
    private val featureGroup: Set<GroupableFeature>,
    private val useCasesToTest: List<AppUseCase>,
) : FeatureGroupTestBase(cameraSelector, implName, cameraXConfig) {
    @Test
    fun testFeatureBindingAsRequiredOrPreferred_alignsWithIsSupportedQuery(): Unit = runBlocking {
        val useCases = useCasesToTest.toUseCases()
        val sessionConfig = SessionConfig(useCases = useCases, requiredFeatureGroup = featureGroup)

        // Query if the FULL feature group is supported when set as REQUIRED. This will be the
        // single source of truth for this test. We will verify that the binding behavior aligns
        // with this initial query result for both when all these features are set as required and
        // when all these features are set as preferred features.
        val isSupported =
            cameraProvider.getCameraInfo(cameraSelector).isSessionConfigSupported(sessionConfig)

        // Scenario 1: Verify binding when all the features are required and supported.
        // Binding should succeed if and only if the full feature group is supported and no
        // exception should be thrown during the binding then.

        if (isSupported) {
            val camera =
                bindAndVerify(
                    sessionConfig,
                    isExpectedToBeSupported = true,
                    verificationScenario = REQUIRED_FEATURES,
                )

            featureGroup.verifyFeatures(useCases, requireNotNull(camera?.cameraInfo))
        }

        // Scenario 2: Verify binding when all the features are preferred.
        // The binding itself should always succeed and no exception should be thrown.
        // - If the full group IS supported, we expect ALL preferred features to be selected.
        // - If the full group IS NOT supported, we expect a SUBSET of features (potentially none)
        //   to be selected.

        val preferredFeatureConfig =
            SessionConfig(useCases = useCases, preferredFeatureGroup = featureGroup.toList())

        bindAndVerify(
            preferredFeatureConfig,
            isExpectedToBeSupported = isSupported,
            verificationScenario = PREFERRED_FEATURES,
        )
    }

    // TODO: b/419766630 - Add tests where FCQ provides extra support compared to non-FCQ bind flow,
    //  e.g. UHD recording + Preview Stabilization (which is probably not supported right now due to
    //  Preview Stabilization guaranteed table not supporting UHD PRIV). But we first need to wait
    //  for adding FCQ-queryable config combinations supported in Baklava, as Android 15 doesn't
    //  support UHD PRIV for FCQ.

    private suspend fun bindAndVerify(
        sessionConfig: SessionConfig,
        isExpectedToBeSupported: Boolean,
        verificationScenario: VerificationScenario,
    ): Camera? {
        Log.d(
            TAG,
            "bindAndVerify: sessionConfig = $sessionConfig, " +
                "isExpectedToBeSupported = $isExpectedToBeSupported, " +
                "verificationScenario = $verificationScenario",
        )

        var caughtException: Exception? = null
        val camera =
            try {
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        fakeLifecycleOwner,
                        cameraSelector,
                        sessionConfig.apply {
                            if (verificationScenario == PREFERRED_FEATURES) {
                                setFeatureSelectionListener { selectedFeatures ->
                                    if (isExpectedToBeSupported) {
                                        // All features should be selected if query result was true
                                        assertThat(selectedFeatures)
                                            .containsExactlyElementsIn(featureGroup)
                                    } else {
                                        // Not all features should be selected if query result was
                                        // false
                                        VerifiableCollection.assertThat(selectedFeatures)
                                            .doesNotContainAllIn(featureGroup)
                                    }
                                }
                            }
                        },
                    )
                }
            } catch (e: IllegalArgumentException) {
                caughtException = e
                null
            }

        if (verificationScenario == REQUIRED_FEATURES) {
            // If binding is expected to be supported, there should be no exception
            assertThat(caughtException == null).isEqualTo(isExpectedToBeSupported)
        }

        return camera
    }

    enum class VerificationScenario {
        REQUIRED_FEATURES,
        PREFERRED_FEATURES,
    }

    class VerifiableCollection<out E>(
        private val base: Collection<E>,
        override val size: Int = base.size,
    ) : Collection<E> {
        fun doesNotContainAllIn(fullCollection: Collection<@UnsafeVariance E>) {
            val intersection = base.intersect(fullCollection.toSet())
            assertThat(intersection.size).isLessThan(fullCollection.size)
        }

        override fun contains(element: @UnsafeVariance E): Boolean = base.contains(element)

        override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean =
            base.containsAll(elements)

        override fun isEmpty(): Boolean = base.isEmpty()

        override fun iterator(): Iterator<E> = base.iterator()

        companion object {
            /** Alignment with Truth library. */
            fun <E> assertThat(c: Collection<E>): VerifiableCollection<E> = VerifiableCollection(c)
        }
    }

    companion object {
        private const val TAG = "FeatureGroupQueryBindAlignmentTest"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing

                    for (featureGroup in
                    // Generates all non-empty subsets of the features to test all combinations
                    allHighQualityFeatures.toPowerSet().filter {
                            // Do not test more than 3 features at once to save time
                            Range(1, 3).contains(it.size) && !it.containsSameTypeFeatures()
                        }) {
                        useCaseCombinationsToTest.forEach { useCases ->
                            add(
                                arrayOf(
                                    "config=${Camera2Config::class.simpleName} lensFacing={$lens}" +
                                        " featureGroup={$featureGroup} useCases = {$useCases}",
                                    selector,
                                    Camera2Config::class.simpleName,
                                    Camera2Config.defaultConfig(),
                                    featureGroup,
                                    useCases,
                                )
                            )
                        }
                    }

                    // Generate combinations of each use case matched with each feature
                    for (feature in allFeatures) {
                        AppUseCase.entries.forEach { useCase ->
                            val featureGroup = setOf(feature)
                            val useCases = listOf(useCase)

                            add(
                                arrayOf(
                                    "config=${Camera2Config::class.simpleName} lensFacing={$lens}" +
                                        " featureGroup={$featureGroup} useCases = {$useCases}",
                                    selector,
                                    Camera2Config::class.simpleName,
                                    Camera2Config.defaultConfig(),
                                    featureGroup,
                                    useCases,
                                )
                            )
                        }
                    }
                }
            }

        /**
         * Returns the power set of the receiver set.
         *
         * The power set of a set S is the set of all subsets of S, including the empty set and S
         * itself.
         *
         * For example, the power set of `{A, B}` is `{{}, {A}, {B}, {A, B}}`.
         *
         * This function iteratively builds the power set. It starts with a set containing just the
         * empty set. Then, for each element in the original set, it creates new subsets by adding
         * the element to all existing subsets in the power set, and adds these new subsets to the
         * power set.
         */
        private fun <T> Set<T>.toPowerSet(): Set<Set<T>> {
            val sets = mutableSetOf<Set<T>>(emptySet())
            for (element in this) {
                sets.addAll(sets.map { it + element })
            }
            return sets
        }

        private fun Set<GroupableFeature>.containsSameTypeFeatures(): Boolean {
            val featureTypes = map { it.featureTypeInternal }.distinct()

            featureTypes.forEach { featureType ->
                val distinctFeaturesPerType = filter { it.featureTypeInternal == featureType }

                if (distinctFeaturesPerType.size > 1) {
                    return true
                }
            }

            return false
        }
    }
}
