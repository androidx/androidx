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
import androidx.core.os.BuildCompat;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {

    @Override
    // TODO(b/201316758): Remove once BuildCompat.isAtLeastT is removed
    @BuildCompat.PrereleaseSdkCheck
    public boolean isFeatureSupported(@NonNull String feature) {
        if (Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH.equals(feature)) {
            // TODO(b/201316758) : Update to reflect support in Android T+ once this feature is
            // synced over into service-appsearch.
            return false;
        }
        if (Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER.equals(feature)) {
            return BuildCompat.isAtLeastT();
        }
        if (Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA.equals(feature)) {
            // TODO(b/215624105) : Update to reflect support in Android T+ once this feature is
            // synced over into service-appsearch.
            return false;
        }
        if (Features.GLOBAL_SEARCH_SESSION_GET_BY_ID.equals(feature)) {
            return BuildCompat.isAtLeastT();
        }
        if (Features.ADD_PERMISSIONS_AND_GET_VISIBILITY.equals(feature)) {
            // TODO(b/205749173) : Update to reflect support in Android T+ once this feature is
            // synced over into service-appsearch.
            return false;
        }
        return false;
    }
}
