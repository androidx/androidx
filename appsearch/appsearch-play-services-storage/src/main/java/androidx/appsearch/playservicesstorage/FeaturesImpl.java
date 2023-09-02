/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage;

import androidx.annotation.NonNull;
import androidx.appsearch.app.Features;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level and GMSCore AppSearch module.
 */
final class FeaturesImpl implements Features {
    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        // TODO(b/274986359): Update based on features available in {@link Features} and those
        //  supported by play-services-appsearch.
        switch (feature) {
            // Android T Features
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                // fall through
                return true; // AppSearch features in T, present in GMSCore AppSearch.

            // RegisterObserver and UnregisterObserver are not yet supported by GMSCore AppSearch.
            // TODO(b/208654892) : Update to reflect support once this feature is supported.
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
            // Android U Features
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // TODO(b/203700301) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // TODO(b/259294369) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
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
                //  synced over into service-appsearch.
                // fall through
            case Features.VERBATIM_SEARCH:
                // TODO(b/204333391) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // TODO(b/208654892) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // TODO(b/258715421) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SEARCH_SUGGESTION:
                // TODO(b/227356108) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SCHEMA_SET_DELETION_PROPAGATION:
                // TODO(b/268521214) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                // TODO(b/280698121) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // TODO(b/269295094) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // TODO(b/289150947) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // TODO(b/296088047) : Update to reflect support in Android U+ once this feature is
                //  synced over into service-appsearch.
                return false;
            default:
                return false; // AppSearch features in U+, absent in GMSCore AppSearch.
        }
    }
}
