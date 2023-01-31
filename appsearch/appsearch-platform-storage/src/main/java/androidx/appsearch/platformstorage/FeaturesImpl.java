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
        switch (feature) {
            // Android T Features
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                // fall through
                return BuildCompat.isAtLeastT();

            // Android U Features
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // TODO(b/203700301) : Update to reflect support in Android U+ once this feature is
                // synced over into service-appsearch.
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // TODO(b/259294369) : Update to reflect support in Android U+ once this feature is
                // synced over into service-appsearch.
                // fall through
            case Features.NUMERIC_SEARCH:
                // TODO(b/259744228) : Update to reflect support in Android U+ once this feature is
                // synced over into service-appsearch.
                // fall through
            case SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // TODO(b/261474063) : Update to reflect support in Android U+ once advanced
                //  ranking becomes available.
                // fall through
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // TODO(b/256022027) : Update to reflect support in Android U+ once this feature is
                // synced over into service-appsearch.
                // fall through
            case Features.VERBATIM_SEARCH:
                // TODO(b/204333391) : Update to reflect support in Android U+ once this feature is
                // synced over into service-appsearch.
                return false;
            default:
                return false;
        }
    }
}
