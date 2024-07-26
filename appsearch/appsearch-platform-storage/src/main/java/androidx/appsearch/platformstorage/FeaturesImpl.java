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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {
    // Context is used to check mainline module version, as support varies by module version.
    private final Context mContext;

    FeaturesImpl(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    @Override
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
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

            // Android U Features
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.VERBATIM_SEARCH:
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

            // Android V Features
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // fall through
            case Features.LIST_FILTER_HAS_PROPERTY_FUNCTION:
                // fall through
            case Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG:
                // fall through
            case Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE:
                // fall through
            case Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG:
                // fall through
            case Features.ENTERPRISE_GLOBAL_SEARCH_SESSION:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;

            // Beyond Android V Features
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // TODO(b/326656531) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_SET_DESCRIPTION:
                // TODO(b/326987971) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.LIST_FILTER_TOKENIZE_FUNCTION:
                // TODO(b/332620561) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS:
                // TODO(b/332642571) : Update when feature is ready in service-appsearch.
                // fall through
            default:
                return false;
        }
    }

    @Override
    public int getMaxIndexedProperties() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return 64;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            // Sixty-four properties were enabled in mainline module of the U base version
            return AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                    >= AppSearchVersionUtil.APPSEARCH_U_BASE_VERSION_CODE ? 64 : 16;
        } else {
            return 16;
        }
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.S)
    @ExperimentalAppSearchApi
    public int getMaxIndexedDocumentCountPerPackage() {
        return AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                >= AppSearchVersionUtil.APPSEARCH_M2024_08_VERSION_CODE ? 80000 : 20000;
    }
}
