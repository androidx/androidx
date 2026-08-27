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

package androidx.health.connect.client.testing

import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.testing.stubs.stub
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows

/** Unit tests for [FakeHealthConnectFeatures]. */
class FakeHealthConnectFeaturesTest {

    @Test
    fun default_allFeaturesUnavailable() {
        val features = FakeHealthConnectFeatures()

        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
        assertThat(
                features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
            )
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
    }

    @Test
    fun constructor_defaultStatus_initializesDefaults() {
        val features = FakeHealthConnectFeatures(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)

        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
        assertThat(
                features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
            )
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
    }

    @Test
    fun setFeatureStatus_updatesStatus() {
        val features = FakeHealthConnectFeatures()

        features.setFeatureStatus(
            HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE,
        )
        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)

        features.setFeatureStatus(
            HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
            HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE,
        )
        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
    }

    @Test
    fun getFeatureStatus_stubbedElement() = runTest {
        val features = FakeHealthConnectFeatures()
        features.overrides.getFeatureStatus = stub(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)

        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
    }

    @Test
    fun getFeatureStatus_stubbedException_throws() = runTest {
        val features = FakeHealthConnectFeatures()
        val expectedException = Exception("Stubbed feature error")
        features.overrides.getFeatureStatus = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking {
                features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE)
            }
        }
    }

    @Test
    fun getFeatureStatus_nullStub_fallsBackToMap() {
        val features =
            FakeHealthConnectFeatures().apply {
                setFeatureStatus(
                    HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                    HealthConnectFeatures.FEATURE_STATUS_AVAILABLE,
                )
            }
        features.overrides.getFeatureStatus = null

        assertThat(features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
            .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
    }
}
