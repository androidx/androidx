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

package androidx.camera.integration.featurecombo.qba

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.camera.integration.featurecombo.FeatureGroupTestBase
import androidx.camera.integration.featurecombo.qba.FeatureGroupQueryBindAlignmentTestBase.VerificationScenario.PREFERRED_FEATURES
import androidx.camera.integration.featurecombo.qba.FeatureGroupQueryBindAlignmentTestBase.VerificationScenario.REQUIRED_FEATURES
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

/**
 * Base class for testing the alignment between Feature Group query API results and actual bind
 * behavior. This class provides the core test logic, while subclasses are expected to provide the
 * parameterized data based on different camera and feature combinations.
 *
 * @property testName The name of the test configuration.
 * @property cameraSelector The CameraSelector to be tested.
 * @property implName The name of the CameraX implementation.
 * @property cameraXConfig The CameraX configuration to use.
 * @property featureGroup The set of features to be tested.
 * @property useCasesToTest The list of use cases to bind.
 */
abstract class FeatureGroupQueryBindAlignmentTestBase(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
    private val featureGroup: Set<GroupableFeature>,
    private val useCasesToTest: List<AppUseCase>,
) : FeatureGroupTestBase(cameraSelector, implName, cameraXConfig) {

    /**
     * Tests that the binding behavior of CameraX aligns with the results from the Feature Group
     * query API. It checks both REQUIRED and PREFERRED feature group settings.
     */
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

    /**
     * Binds the use cases with the given session config and verifies the outcome based on the
     * expected support and verification scenario.
     */
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

    /** Scenario to verify. */
    enum class VerificationScenario {
        REQUIRED_FEATURES,
        PREFERRED_FEATURES,
    }

    /** Collection wrapper to add custom assertions. */
    class VerifiableCollection<out E>(
        private val base: Collection<E>,
        override val size: Int = base.size,
    ) : Collection<E> {
        /** Asserts that the collection does not contain all elements in the given collection. */
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
        private const val TAG = "FeatureGroupQueryBindAlignmentTestBase"

        /** Returns the power set of the receiver set. */
        internal fun <T> Set<T>.toPowerSet(): Set<Set<T>> {
            val sets = mutableSetOf<Set<T>>(emptySet())
            for (element in this) {
                sets.addAll(sets.map { it + element })
            }
            return sets
        }

        internal fun Set<GroupableFeature>.containsSameTypeFeatures(): Boolean {
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
