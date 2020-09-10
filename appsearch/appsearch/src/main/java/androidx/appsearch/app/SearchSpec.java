/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.IllegalSearchSpecException;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class represents the specification logic for AppSearch. It can be used to set the type of
 * search, like prefix or exact only or apply filters to search for a specific schema type only etc.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(sidchhabra) : AddResultSpec fields for Snippets etc.
public final class SearchSpec {

    static final String TERM_MATCH_TYPE_FIELD = "termMatchType";
    static final String SCHEMA_TYPES_FIELD = "schemaType";
    static final String NUM_PER_PAGE_FIELD = "numPerPage";
    static final String RANKING_STRATEGY_FIELD = "rankingStrategy";
    static final String ORDER_FILED = "order";
    static final String NUM_TO_SNIPPET_FIELD = "numToSnippet";
    static final String NUM_MATCHED_PER_PROPERTY_FIELD = "numMatchedPerProperty";
    static final String MAX_SNIPPET_FIELD = "maxSnippet";
    static final int DEFAULT_NUM_PER_PAGE = 10;

    private final Bundle mBundle;

    SearchSpec(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
    }

    /** Returns the {@link Bundle} populated by this builder. */
    @NonNull
    Bundle getBundle() {
        return mBundle;
    }

    /** Term Match Type for the query. */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link com.google.android.icing.proto.SearchSpecProto.termMatchType}
    @IntDef(value = {
            TERM_MATCH_TYPE_EXACT_ONLY,
            TERM_MATCH_TYPE_PREFIX
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TermMatchTypeCode {}

    /**
     * Query terms will only match exact tokens in the index.
     * <p>Ex. A query term "foo" will only match indexed token "foo", and not "foot" or "football".
     */
    public static final int TERM_MATCH_TYPE_EXACT_ONLY = 1;
    /**
     * Query terms will match indexed tokens when the query term is a prefix of the token.
     * <p>Ex. A query term "foo" will match indexed tokens like "foo", "foot", and "football".
     */
    public static final int TERM_MATCH_TYPE_PREFIX = 2;

    /** Ranking Strategy for query result.*/
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.RankingStrategy.Code }
    @IntDef(value = {
            RANKING_STRATEGY_NONE,
            RANKING_STRATEGY_DOCUMENT_SCORE,
            RANKING_STRATEGY_CREATION_TIMESTAMP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RankingStrategyCode {}

    /** No Ranking, results are returned in arbitrary order.*/
    public static final int RANKING_STRATEGY_NONE = 0;
    /** Ranked by app-provided document scores. */
    public static final int RANKING_STRATEGY_DOCUMENT_SCORE = 1;
    /** Ranked by document creation timestamps. */
    public static final int RANKING_STRATEGY_CREATION_TIMESTAMP = 2;

    /** Order for query result.*/
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.Order.Code }
    @IntDef(value = {
            ORDER_DESCENDING,
            ORDER_ASCENDING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrderCode {}

    /** Search results will be returned in a descending order. */
    public static final int ORDER_DESCENDING = 0;
    /** Search results will be returned in an ascending order. */
    public static final int ORDER_ASCENDING = 1;

    /** Builder for {@link SearchSpec objects}. */
    public static final class Builder {

        private final Bundle mBundle;
        private boolean mBuilt = false;

        /** Creates a new {@link SearchSpec.Builder}. */
        public Builder() {
            mBundle = new Bundle();
            mBundle.putInt(NUM_PER_PAGE_FIELD, DEFAULT_NUM_PER_PAGE);
        }

        /**
         * Indicates how the query terms should match {@link TermMatchTypeCode} in the index.
         */
        @NonNull
        public Builder setTermMatchType(@TermMatchTypeCode int termMatchTypeCode) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkArgumentInRange(termMatchTypeCode, TERM_MATCH_TYPE_EXACT_ONLY,
                    TERM_MATCH_TYPE_PREFIX, "Term match type");
            mBundle.putInt(TERM_MATCH_TYPE_FIELD, termMatchTypeCode);
            return this;
        }

        /**
         * Adds a Schema type filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified schema types.
         * <p>If unset, the query will search over all schema types.
         */
        @NonNull
        public Builder setSchemaTypes(@NonNull String... schemaTypes) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBundle.putStringArray(SCHEMA_TYPES_FIELD, schemaTypes);
            return this;
        }

        /**
         * Sets the number of results per page in the returned object.
         * <p> The default number of results per page is 10.
         */
        @NonNull
        public SearchSpec.Builder setNumPerPage(int numPerPage) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            if (numPerPage <= 0) {
                throw new IllegalArgumentException("Invalid number per page :" + numPerPage);
            }
            mBundle.putInt(NUM_PER_PAGE_FIELD, numPerPage);
            return this;
        }

        /** Sets ranking strategy for AppSearch results.*/
        @NonNull
        public Builder setRankingStrategy(@RankingStrategyCode int rankingStrategy) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkArgumentInRange(rankingStrategy, RANKING_STRATEGY_NONE,
                    RANKING_STRATEGY_CREATION_TIMESTAMP, "Result ranking strategy");
            mBundle.putInt(RANKING_STRATEGY_FIELD, rankingStrategy);
            return this;
        }

        /**
         * Indicates the order of returned search results, the default is DESC, meaning that results
         * with higher scores come first.
         * <p>This order field will be ignored if RankingStrategy = {@code RANKING_STRATEGY_NONE}.
         */
        @NonNull
        public Builder setOrder(@OrderCode int order) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkArgumentInRange(order, ORDER_DESCENDING, ORDER_ASCENDING,
                    "Result ranking order");
            mBundle.putInt(ORDER_FILED, order);
            return this;
        }

        /**
         * Only the first {@code numToSnippet} documents based on the ranking strategy
         * will have snippet information provided.
         * <p>If set to 0 (default), snippeting is disabled and
         * {@link SearchResults.Result#getMatchInfo} will return {@code null} for that result.
         */
        @NonNull
        public SearchSpec.Builder setNumToSnippet(int numToSnippet) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBundle.putInt(NUM_TO_SNIPPET_FIELD, numToSnippet);
            return this;
        }

        /**
         * Only the first {@code numMatchesPerProperty} matches for a every property of
         * {@link GenericDocument} will contain snippet information.
         * <p>If set to 0, snippeting is disabled and {@link SearchResults.Result#getMatchInfo}
         * will return {@code null} for that result.
         */
        @NonNull
        public SearchSpec.Builder setNumMatchesPerProperty(int numMatchesPerProperty) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBundle.putInt(NUM_MATCHED_PER_PROPERTY_FIELD, numMatchesPerProperty);
            return this;
        }

        /**
         * Sets {@code maxSnippetSize}, the maximum snippet size. Snippet windows start at
         * {@code maxSnippetSize/2} bytes before the middle of the matching token and end at
         * {@code maxSnippetSize/2} bytes after the middle of the matching token. It respects
         * token boundaries, therefore the returned window may be smaller than requested.
         * <p> Setting {@code maxSnippetSize} to 0 will disable windowing and an empty string will
         * be returned. If matches enabled is also set to false, then snippeting is disabled.
         * <p>Ex. {@code maxSnippetSize} = 16. "foo bar baz bat rat" with a query of "baz" will
         * return a window of "bar baz bat" which is only 11 bytes long.
         */
        @NonNull
        public SearchSpec.Builder setMaxSnippetSize(int maxSnippetSize) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBundle.putInt(MAX_SNIPPET_FIELD, maxSnippetSize);
            return this;
        }

        /**
         * Constructs a new {@link SearchSpec} from the contents of this builder.
         *
         * <p>After calling this method, the builder must no longer be used.
         */
        @NonNull
        public SearchSpec build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            if (!mBundle.containsKey(TERM_MATCH_TYPE_FIELD)) {
                throw new IllegalSearchSpecException("Missing termMatchType field.");
            }
            mBuilt = true;
            return new SearchSpec(mBundle);
        }
    }
}
