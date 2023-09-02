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
package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A class that encapsulates all features that are only supported in certain cases (e.g. only on
 * certain implementations or only at a certain Android API Level).
 *
 * <p>Features do not depend on any runtime state, and features will never be removed. Once
 * {@link #isFeatureSupported} returns {@code true} for a certain feature, it is safe to assume that
 * the feature will be available forever on that AppSearch storage implementation, at that
 * Android API level, on that device.
 */

// @exportToFramework:copyToPath(testing/testutils/src/android/app/appsearch/testutil/external/Features.java)
public interface Features {

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchResult.MatchInfo#getSubmatchRange} and
     * {@link SearchResult.MatchInfo#getSubmatch}.
     */
    String SEARCH_RESULT_MATCH_INFO_SUBMATCH = "SEARCH_RESULT_MATCH_INFO_SUBMATCH";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#registerObserverCallback} and
     * {@link GlobalSearchSession#unregisterObserverCallback}.
     */
    String GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK =
            "GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#getSchemaAsync}.
     */
    String GLOBAL_SEARCH_SESSION_GET_SCHEMA = "GLOBAL_SEARCH_SESSION_GET_SCHEMA";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#getByDocumentIdAsync}.
     */
    String GLOBAL_SEARCH_SESSION_GET_BY_ID = "GLOBAL_SEARCH_SESSION_GET_BY_ID";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SetSchemaRequest.Builder#addAllowedRoleForSchemaTypeVisibility},
     * {@link SetSchemaRequest.Builder#clearAllowedRolesForSchemaTypeVisibility},
     * {@link GetSchemaResponse#getSchemaTypesNotDisplayedBySystem()},
     * {@link GetSchemaResponse#getSchemaTypesVisibleToPackages()},
     * {@link GetSchemaResponse#getRequiredPermissionsForSchemaTypeVisibility()},
     * {@link SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility} and
     * {@link SetSchemaRequest.Builder#clearRequiredPermissionsForSchemaTypeVisibility}
     */
    String ADD_PERMISSIONS_AND_GET_VISIBILITY = "ADD_PERMISSIONS_AND_GET_VISIBILITY";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_RFC822}.
     */
    String TOKENIZER_TYPE_RFC822 = "TOKENIZER_TYPE_RFC822";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} and all other numeric search
     * features.
     *
     * <p>For details on the numeric search expressions in the query language, see
     * {@link AppSearchSession#search}.
     */
    String NUMERIC_SEARCH = FeatureConstants.NUMERIC_SEARCH;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_VERBATIM} and all other
     * verbatim search features within the query language that allows clients to search using the
     * verbatim string operator.
     *
     * <p>For details on the verbatim string operator, see {@link AppSearchSession#search}.
     */
    String VERBATIM_SEARCH = FeatureConstants.VERBATIM_SEARCH;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the expansion of the
     * query language to conform to the definition of the list filters language
     * (https://aip.dev/160).
     *
     * <p>For more details, see {@link AppSearchSession#search}.
     */
    String LIST_FILTER_QUERY_LANGUAGE = FeatureConstants.LIST_FILTER_QUERY_LANGUAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec#GROUPING_TYPE_PER_SCHEMA}
     */
    String SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA = "SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setPropertyWeights}.
     */
    String SEARCH_SPEC_PROPERTY_WEIGHTS = "SEARCH_SPEC_PROPERTY_WEIGHTS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#addFilterProperties}.
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    String SEARCH_SPEC_ADD_FILTER_PROPERTIES = "SEARCH_SPEC_ADD_FILTER_PROPERTIES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setRankingStrategy(String)}.
     */
    String SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION = "SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#JOINABLE_VALUE_TYPE_QUALIFIED_ID},
     * {@link SearchSpec.Builder#setJoinSpec}, and all other join features.
     */
    String JOIN_SPEC_AND_QUALIFIED_ID = "JOIN_SPEC_AND_QUALIFIED_ID";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSession#searchSuggestionAsync}.
     */
    String SEARCH_SUGGESTION = "SEARCH_SUGGESTION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig.Builder#setDeletionPropagation}.
     */
    String SCHEMA_SET_DELETION_PROPAGATION = "SCHEMA_SET_DELETION_PROPAGATION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers setting schemas with
     * circular references for {@link AppSearchSession#setSchemaAsync}.
     */
    String SET_SCHEMA_CIRCULAR_REFERENCES = "SET_SCHEMA_CIRCULAR_REFERENCES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.Builder#addParentType}.
     */
    String SCHEMA_ADD_PARENT_TYPE = "SCHEMA_ADD_PARENT_TYPE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link
     * AppSearchSchema.DocumentPropertyConfig.Builder#addIndexableNestedProperties(String...)}
     */
    String SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES = "SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES";

    /**
     * Returns whether a feature is supported at run-time. Feature support depends on the
     * feature in question, the AppSearch backend being used and the Android version of the
     * device.
     *
     * <p class="note"><b>Note:</b> If this method returns {@code false}, it is not safe to invoke
     * the methods requiring the desired feature.
     *
     * @param feature the feature to be checked
     * @return whether the capability is supported given the Android API level and AppSearch
     * backend.
     */
    boolean isFeatureSupported(@NonNull String feature);
}
