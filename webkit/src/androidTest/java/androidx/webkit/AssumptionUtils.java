/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.webkit;

import org.junit.Assume;

/**
 * Helper methods for specifying test assumptions.
 */
public final class AssumptionUtils {

    /**
     * Throws {@link org.junit.AssumptionViolatedException} if the device does not support the
     * particular feature, otherwise returns.
     *
     * <p>
     * This provides a more descriptive error message than a bare {@code assumeTrue} call.
     *
     * @param featureName the feature to be checked
     */
    public static void checkFeature(String featureName) {
        final String msg = "This device does not have the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeTrue(msg, hasFeature);
    }

    // Do not instantiate this class.
    private AssumptionUtils() {}
}
