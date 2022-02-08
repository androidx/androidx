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
package androidx.appsearch.localstorage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.Features;

/**
 * An implementation of {@link Features}. This implementation always returns true. This is
 * sufficient for the use in the local backend because all features are always available on the
 * local backend.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AlwaysSupportedFeatures implements Features {

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        if (Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH.equals(feature)) {
            return true;
        }
        if (Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER.equals(feature)) {
            return true;
        }
        if (Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA.equals(feature)) {
            return true;
        }
        if (Features.SET_SCHEMA_REQUEST_VISIBILITY_PERMISSIONS_AND_GET_VISIBILITY.equals(feature)) {
            return true;
        }
        return false;
    }
}
