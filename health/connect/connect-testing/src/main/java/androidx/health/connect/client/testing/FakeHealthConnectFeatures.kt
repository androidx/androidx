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
import androidx.health.connect.client.HealthConnectFeatures.Companion.Feature
import androidx.health.connect.client.HealthConnectFeatures.Companion.FeatureStatus
import java.util.Collections

/**
 * A fake [HealthConnectFeatures] that enables full control of feature availability status in tests.
 *
 * @param defaultStatus The fallback status for any feature not explicitly set. Defaults to
 *   [HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE].
 */
public class FakeHealthConnectFeatures
constructor(
    @FeatureStatus private val defaultStatus: Int = HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE
) : HealthConnectFeatures {

    /**
     * Used to override or intercept responses to emulate scenarios that [FakeHealthConnectFeatures]
     * doesn't support directly, such as throwing an exception or applying custom logic to the
     * responses.
     */
    public val overrides: FakeHealthConnectFeaturesOverrides = FakeHealthConnectFeaturesOverrides()

    private val featureStatuses = Collections.synchronizedMap(mutableMapOf<Int, Int>())

    /**
     * Checks whether the given feature is available.
     *
     * If a stub is set in [FakeHealthConnectFeaturesOverrides.getFeatureStatus], it will be checked
     * first. Otherwise, it falls back to the in-memory status map, defaulting to `defaultStatus`
     * passed to the constructor if the feature is not found.
     *
     * @param feature the feature to be checked. One of the "FEATURE_" constants in
     *   [HealthConnectFeatures].
     * @return one of [HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE] or
     *   [HealthConnectFeatures.FEATURE_STATUS_AVAILABLE]
     */
    @FeatureStatus
    override fun getFeatureStatus(@Feature feature: Int): Int {
        overrides.getFeatureStatus?.next(feature)?.let {
            return it
        }
        return featureStatuses[feature] ?: defaultStatus
    }

    /**
     * Dynamically updates the availability status of a feature in tests.
     *
     * @param feature the feature to update. One of the "FEATURE_" constants in
     *   [HealthConnectFeatures].
     * @param status the new status. One of [HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE] or
     *   [HealthConnectFeatures.FEATURE_STATUS_AVAILABLE].
     */
    public fun setFeatureStatus(@Feature feature: Int, @FeatureStatus status: Int) {
        featureStatuses[feature] = status
    }
}
