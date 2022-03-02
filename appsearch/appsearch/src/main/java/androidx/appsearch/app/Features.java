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

/**
 * A class that encapsulates all features that are only supported in certain cases (e.g. only on
 * certain implementations or only at a certain Android API Level).
 *
 * <p>Features do not depend on any runtime state, and features will never be removed. Once
 * {@link #isFeatureSupported} returns {@code true} for a certain feature, it is safe to assume that
 * the feature will be available forever on that AppSearch storage implementation, at that
 * Android API level, on that device form factor.
 * <!--@exportToFramework:hide-->
 */
public interface Features {

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchResult.MatchInfo#getSubmatchRange} and
     * {@link SearchResult.MatchInfo#getSubmatch}.
     */
    String SEARCH_RESULT_MATCH_INFO_SUBMATCH = "SEARCH_RESULT_MATCH_INFO_SUBMATCH";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#addObserver} and
     * {@link GlobalSearchSession#removeObserver}.
     */
    String GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER = "GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#getSchema}.
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
