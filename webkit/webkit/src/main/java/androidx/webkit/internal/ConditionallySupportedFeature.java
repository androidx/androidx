/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.annotation.NonNull;

/**
 * An interface to represent a feature which may or may not be supported. This should generally be
 * used via {@link WebViewFeatureInternal}, but this is factored out for testing.
 */
public interface ConditionallySupportedFeature {
    /**
     * Get the name of the public feature this matches.
     */
    @NonNull
    String getPublicFeatureName();

    /**
     * Return whether this feature is supported.
     */
    boolean isSupported();
}
