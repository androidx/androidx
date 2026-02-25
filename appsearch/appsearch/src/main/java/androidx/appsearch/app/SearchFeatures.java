/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a collection of search features that can be enabled or disabled for specific
 * search operations.
 * @exportToFramework:hide
 */
//TODO(b/387291182) unhide this class when it is supported in SearchSpec
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE)
@SuppressWarnings("HiddenSuperclass")
public class SearchFeatures extends EnabledFeatures {

    SearchFeatures(@NonNull List<String> enabledFeatures) {
        super(enabledFeatures);
    }

    /**
     * Returns whether the NUMERIC_SEARCH feature is enabled.
     */
    public boolean isNumericSearchEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.NUMERIC_SEARCH);
    }

    /**
     * Returns whether the VERBATIM_SEARCH feature is enabled.
     */
    public boolean isVerbatimSearchEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.VERBATIM_SEARCH);
    }

    /**
     * Returns whether the LIST_FILTER_QUERY_LANGUAGE feature is enabled.
     */
    public boolean isListFilterQueryLanguageEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE);
    }

    /**
     * Returns whether the LIST_FILTER_HAS_PROPERTY_FUNCTION feature is enabled.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION)
    public boolean isListFilterHasPropertyFunctionEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.LIST_FILTER_HAS_PROPERTY_FUNCTION);
    }

    /**
     * Returns whether the LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION feature is enabled.
     */
    @ExperimentalAppSearchApi
    @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
    public boolean isListFilterMatchScoreExpressionFunctionEnabled() {
        return mEnabledFeatures.contains(
                FeatureConstants.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchFeatures)) {
            return false;
        }
        SearchFeatures that = (SearchFeatures) o;
        return Objects.equals(mEnabledFeatures, that.mEnabledFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEnabledFeatures);
    }

    /**  Builder class for {@link SearchFeatures}.  */
    public static final class Builder {
        private final ArraySet<String> mEnabledFeatures = new ArraySet<>();

        /**
         * Sets the NUMERIC_SEARCH feature as enabled/disabled according to the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *
         * <p>If disabled, disallows use of
         * {@link AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} and all other numeric
         * querying features.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.NUMERIC_SEARCH)
        public @NonNull Builder setNumericSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.NUMERIC_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the VERBATIM_SEARCH feature as enabled/disabled according to the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         *
         * <p>If disabled, disallows use of
         * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_VERBATIM} and all other
         * verbatim search features within the query language that allows clients to search
         * using the verbatim string operator.
         *
         * <p>For example, The verbatim string operator '"foo/bar" OR baz' will ensure that
         * 'foo/bar' is treated as a single 'verbatim' token.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.VERBATIM_SEARCH)
        public @NonNull Builder setVerbatimSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.VERBATIM_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_QUERY_LANGUAGE feature as enabled/disabled according to the
         * enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *
         * This feature covers the expansion of the query language to conform to the definition
         * of the list filters language (https://aip.dev/160). This includes:
         * <ul>
         * <li>addition of explicit 'AND' and 'NOT' operators</li>
         * <li>property restricts are allowed with grouping (ex. "prop:(a OR b)")</li>
         * <li>addition of custom functions to control matching</li>
         * </ul>
         *
         * <p>The newly added custom functions covered by this feature are:
         * <ul>
         * <li>createList(String...)</li>
         * <li>termSearch(String, {@code List<String>})</li>
         * </ul>
         *
         * <p>createList takes a variable number of strings and returns a list of strings.
         * It is for use with termSearch.
         *
         * <p>termSearch takes a query string that will be parsed according to the supported
         * query language and an optional list of strings that specify the properties to be
         * restricted to. This exists as a convenience for multiple property restricts. So,
         * for example, the query "(subject:foo OR body:foo) (subject:bar OR body:bar)"
         * could be rewritten as "termSearch(\"foo bar\", createList(\"subject\", \"bar\"))"
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.LIST_FILTER_QUERY_LANGUAGE)
        public @NonNull Builder setListFilterQueryLanguageEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_HAS_PROPERTY_FUNCTION feature as enabled/disabled according to
         * the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         *
         * <p>If disabled, disallows the use of the "hasProperty" function. See
         * {@link AppSearchSession#search} for more details about the function.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.LIST_FILTER_HAS_PROPERTY_FUNCTION)
        @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION)
        public @NonNull Builder setListFilterHasPropertyFunctionEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.LIST_FILTER_HAS_PROPERTY_FUNCTION, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION feature as enabled/disabled
         * according to the enabled parameter.
         *
         * <p>If not enabled, the use of the "matchScoreExpression" function is disallowed. See
         * {@link AppSearchSession#search} for more details about the function.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
        public @NonNull Builder setListFilterMatchScoreExpressionFunctionEnabled(boolean enabled) {
            modifyEnabledFeature(
                    FeatureConstants.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION, enabled);
            return this;
        }

        /**  Builds the {@link SearchFeatures} instance.         */
        @NonNull
        public SearchFeatures build() {
            return new SearchFeatures(new ArrayList<>(mEnabledFeatures));
        }

        private void modifyEnabledFeature(@NonNull String feature, boolean enabled) {
            if (enabled) {
                mEnabledFeatures.add(feature);
            } else {
                mEnabledFeatures.remove(feature);
            }
        }
    }
}
