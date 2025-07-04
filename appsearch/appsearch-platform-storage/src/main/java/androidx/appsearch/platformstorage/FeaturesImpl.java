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

import androidx.annotation.OptIn;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

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
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public boolean isFeatureSupported(@NonNull String feature) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // AppSearch landed in platform in S, however it was not updatable via mainline until T.
            // So all features here are not available below T.
            return false;
        }
        switch (feature) {
            // Aliases for other features
            case Features.SEARCH_AND_CLICK_ACCUMULATOR:
                // Requires JoinSpec to create the Click schema. TakenAction API is optional as we
                // can index search and click as regular documents if TakenActions aren't available.
                return isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID);

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

            // SDK extension U Base features
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                return BuildCompat.T_EXTENSION_INT
                        >= AppSearchVersionUtil.TExtensionVersions.U_BASE;

            // Android U Features
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
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                return BuildCompat.T_EXTENSION_INT
                        >= AppSearchVersionUtil.TExtensionVersions.V_BASE;

            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
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

            // M-2024-08 Features
            case Features.SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT:
                // fall through
            case Features.SEARCH_SPEC_RANKING_FUNCTION_FILTER_BY_RANGE:
                // For devices that receive mainline updates, this will be available in M-2024-08,
                // and in V for devices that don't receive mainline updates.
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                        || AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                        >= AppSearchVersionUtil.APPSEARCH_V_BASE_VERSION_CODE;

            // M-2024-11 Features
            case Features.INDEXER_MOBILE_APPLICATIONS:
                // For devices that receive mainline updates, this will be available in M-2024-11,
                // and in B for devices that don't receive mainline updates.
                return AppSearchVersionUtil.isAtLeastB()
                        || AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                        >= AppSearchVersionUtil.APPSEARCH_M2024_11_VERSION_CODE;

            // Android B Features
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // fall through
            case Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS:
                // fall through
            case Features.SEARCH_RESULT_PARENT_TYPES:
                return AppSearchVersionUtil.isAtLeastB();

            // Pending Android B Features
            case Features.SCHEMA_EMBEDDING_QUANTIZATION:
                // TODO(b/359959345) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS:
                // TODO(b/332620561) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_DOCUMENT_IDS:
                // TODO(b/367464836) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION:
                // TODO(b/377215223) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_SCORABLE_PROPERTY_CONFIG:
                // TODO(b/357105837) : Update when feature is ready in service-appsearch.
                // fall through

            // Beyond Android B Features
            case Features.SCHEMA_SET_DESCRIPTION:
                // TODO(b/326987971) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM:
                // TODO(b/384947619) : Update when feature is ready in service-appsearch.
            case Features.SEARCH_EMBEDDING_MATCH_INFO:
                // TODO(395128139) : Update when feature is ready in service-appsearch.
                return false;

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
}
