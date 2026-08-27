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

import androidx.health.connect.client.testing.stubs.Stub

/**
 * Used to override or intercept responses to emulate scenarios that [FakeHealthConnectFeatures]
 * doesn't support directly, such as throwing an exception or applying custom logic to the
 * responses.
 *
 * Every call in [FakeHealthConnectFeatures] can be overridden.
 *
 * @param getFeatureStatus A [Stub] used to set the next response used in
 *   [FakeHealthConnectFeatures.getFeatureStatus].
 */
public class FakeHealthConnectFeaturesOverrides(
    /**
     * A [Stub] used to set the next responses used in [getFeatureStatus].
     *
     * Once all stubbed responses are consumed (or if the stub is `null`),
     * [FakeHealthConnectFeatures.getFeatureStatus] will fall back to returning the status set via
     * [FakeHealthConnectFeatures.setFeatureStatus] or the default status.
     */
    public var getFeatureStatus: Stub<Int, Int>? = null
)
