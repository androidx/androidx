/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.appsearch.platformstorage;

import androidx.annotation.NonNull;
import androidx.appsearch.app.Features;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        if (Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH.equals(feature)) {
            // TODO(b/201316758) : Update to reflect support in Android T+ once this feature is
            // synced over into service-appsearch.
            return false;
        }
        return false;
    }
}
