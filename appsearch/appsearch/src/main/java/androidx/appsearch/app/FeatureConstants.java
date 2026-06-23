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

package androidx.appsearch.app;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.collection.ArraySet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * A class that encapsulates all feature constants that are accessible in AppSearch framework.
 *
 * <p>All fields in this class is referring in {@link Features}. If you add/remove any field in this
 * class, you should also change {@link Features}.
 * @see Features
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class FeatureConstants {
    /** Feature constants for {@link Features#NUMERIC_SEARCH}. */
    public static final String NUMERIC_SEARCH = "NUMERIC_SEARCH";

    /**  Feature constants for {@link Features#VERBATIM_SEARCH}.   */
    public static final String VERBATIM_SEARCH = "VERBATIM_SEARCH";

    /**  Feature constants for {@link Features#LIST_FILTER_QUERY_LANGUAGE}.  */
    public static final String LIST_FILTER_QUERY_LANGUAGE = "LIST_FILTER_QUERY_LANGUAGE";

    /**  Feature constants for {@link Features#LIST_FILTER_HAS_PROPERTY_FUNCTION}.  */
    public static final String LIST_FILTER_HAS_PROPERTY_FUNCTION =
            "LIST_FILTER_HAS_PROPERTY_FUNCTION";

    /** Feature constants for {@link Features#LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION}. */
    public static final String LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION =
            "LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION";

    /** A feature constant for the "semanticSearch" function in {@link AppSearchSession#search}. */
    public static final String EMBEDDING_SEARCH = "EMBEDDING_SEARCH";

    /** A feature constant for the "getScorableProperty" function in
     * {@link AppSearchSession#search}.
     */
    public static final String SCHEMA_SCORABLE_PROPERTY_CONFIG = "SCHEMA_SCORABLE_PROPERTY_CONFIG";

    /** A set of scoring features.*/
    public static final Set<String> SCORABLE_FEATURE_SET = Collections.unmodifiableSet(
            new ArraySet<>(Arrays.asList(SCHEMA_SCORABLE_PROPERTY_CONFIG)));

    private FeatureConstants() {}
}
