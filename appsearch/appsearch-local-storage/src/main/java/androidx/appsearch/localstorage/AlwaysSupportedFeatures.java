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
// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/AlwaysSupportedFeatures.java)
package androidx.appsearch.localstorage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.Features;

/**
 * An implementation of {@link Features}. This implementation always returns true. This is
 * sufficient for the use in the local backend because all features are always available on the
 * local backend.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AlwaysSupportedFeatures implements Features {

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        switch (feature) {
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.VERBATIM_SEARCH:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.LIST_FILTER_HAS_PROPERTY_FUNCTION:
                // fall through
            case Features.LIST_FILTER_TOKENIZE_FUNCTION:
                // fall through
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // fall through
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // fall through
            case Features.SCHEMA_SET_DESCRIPTION:
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG:
                // fall through
            case Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE:
                // fall through
            case Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG:
                // fall through
            case Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int getMaxIndexedProperties() {
        return 64;
    }
}
