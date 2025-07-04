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
package androidx.appsearch.localstorage.stats;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.SearchSpec;

import org.jspecify.annotations.NonNull;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#search(String, SearchSpec)}.
 *
 * <p> Only valid for first page.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchStats {

    /** Length of the query string. */
    private final int mNativeQueryLength;
    /** Number of terms in the query string. */
    private final int mNativeNumTerms;
    /** Number of namespaces filtered. */
    private final int mNativeNumNamespacesFiltered;
    /** Number of schema types filtered. */
    private final int mNativeNumSchemaTypesFiltered;
    /** Strategy of scoring and ranking. */
    @SearchSpec.RankingStrategy
    private final int mNativeRankingStrategy;
    /** Number of documents scored. */
    private final int mNativeNumDocumentsScored;
    /**
     * Time used to parse the query, including 2 parts: tokenizing and
     * transforming tokens into an iterator tree.
     */
    private final int mNativeParseQueryLatencyMillis;
    /** Time used to score the raw results. */
    private final int mNativeScoringLatencyMillis;
    /**Whether it contains numeric query or not.*/
    private final boolean mNativeIsNumericQuery;
    /** Number of hits fetched by lite index before applying any filters. */
    private final int mNativeNumFetchedHitsLiteIndex;
    /** Number of hits fetched by main index before applying any filters. */
    private final int mNativeNumFetchedHitsMainIndex;
    /** Number of hits fetched by integer index before applying any filters. */
    private final int mNativeNumFetchedHitsIntegerIndex;
    /**Time used in Lexer to extract lexer tokens from the query. */
    private final int mNativeQueryProcessorLexerExtractTokenLatencyMillis;
    /** Time used in Parser to consume lexer tokens extracted from the query. */
    private final int mNativeQueryProcessorParserConsumeQueryLatencyMillis;
    /** Time used in QueryVisitor to visit and build (nested) DocHitInfoIterator. */
    private final int mNativeQueryProcessorQueryVisitorLatencyMillis;

    SearchStats(@NonNull Builder builder) {
        mNativeQueryLength = builder.mNativeQueryLength;
        mNativeNumTerms = builder.mNativeNumTerms;
        mNativeNumNamespacesFiltered = builder.mNativeNumNamespacesFiltered;
        mNativeNumSchemaTypesFiltered = builder.mNativeNumSchemaTypesFiltered;
        mNativeRankingStrategy = builder.mNativeRankingStrategy;
        mNativeNumDocumentsScored = builder.mNativeNumDocumentsScored;
        mNativeParseQueryLatencyMillis = builder.mNativeParseQueryLatencyMillis;
        mNativeScoringLatencyMillis = builder.mNativeScoringLatencyMillis;
        mNativeIsNumericQuery = builder.mNativeIsNumericQuery;
        mNativeNumFetchedHitsLiteIndex = builder.mNativeNumFetchedHitsLiteIndex;
        mNativeNumFetchedHitsMainIndex = builder.mNativeNumFetchedHitsMainIndex;
        mNativeNumFetchedHitsIntegerIndex = builder.mNativeNumFetchedHitsIntegerIndex;
        mNativeQueryProcessorLexerExtractTokenLatencyMillis =
                builder.mNativeQueryProcessorLexerExtractTokenLatencyMillis;
        mNativeQueryProcessorParserConsumeQueryLatencyMillis =
                builder.mNativeQueryProcessorParserConsumeQueryLatencyMillis;
        mNativeQueryProcessorQueryVisitorLatencyMillis =
                builder.mNativeQueryProcessorQueryVisitorLatencyMillis;
    }

    /** Returns the length of the search string. */
    public int getNativeQueryLength() {
        return mNativeQueryLength;
    }

    /** Returns number of terms in the search string. */
    public int getNativeTermCount() {
        return mNativeNumTerms;
    }

    /** Returns number of namespaces filtered. */
    public int getNativeFilteredNamespaceCount() {
        return mNativeNumNamespacesFiltered;
    }

    /** Returns number of schema types filtered. */
    public int getNativeFilteredSchemaTypeCount() {
        return mNativeNumSchemaTypesFiltered;
    }

    /** Returns strategy of scoring and ranking. */
    @SearchSpec.RankingStrategy
    public int getNativeRankingStrategy() {
        return mNativeRankingStrategy;
    }

    /** Returns number of documents scored. */
    public int getNativeScoredDocumentCount() {
        return mNativeNumDocumentsScored;
    }

    /**
     * Returns time used to parse the query, including 2 parts: tokenizing and transforming
     * tokens into an iterator tree.
     */
    public int getNativeParseQueryLatencyMillis() {
        return mNativeParseQueryLatencyMillis;
    }

    /** Returns time used to score the raw results. */
    public int getNativeScoringLatencyMillis() {
        return mNativeScoringLatencyMillis;
    }

    /** Returns whether it contains numeric query or not. */
    public boolean isNativeNumericQuery() {
        return mNativeIsNumericQuery;
    }

    /** Returns number of hits fetched by lite index before applying any filters. */
    public int getNativeNumFetchedHitsLiteIndex() {
        return mNativeNumFetchedHitsLiteIndex;
    }

    /** Returns number of hits fetched by main index before applying any filters. */
    public int getNativeNumFetchedHitsMainIndex() {
        return mNativeNumFetchedHitsMainIndex;
    }

    /** Returns number of hits fetched by integer index before applying any filters. */
    public int getNativeNumFetchedHitsIntegerIndex() {
        return mNativeNumFetchedHitsIntegerIndex;
    }

    /** Returns time used in Lexer to extract lexer tokens from the query. */
    public int getNativeQueryProcessorLexerExtractTokenLatencyMillis() {
        return mNativeQueryProcessorLexerExtractTokenLatencyMillis;
    }

    /** Returns time used in Parser to consume lexer tokens extracted from the query. */
    public int getNativeQueryProcessorParserConsumeQueryLatencyMillis() {
        return mNativeQueryProcessorParserConsumeQueryLatencyMillis;
    }

    /** Returns time used in QueryVisitor to visit and build (nested) DocHitInfoIterator. */
    public int getNativeQueryProcessorQueryVisitorLatencyMillis() {
        return mNativeQueryProcessorQueryVisitorLatencyMillis;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "SearchStats {\n"
                        + "query_length=%d, num_terms=%d, num_namespaces_filtered=%d, "
                        + "num_schema_types_filtered=%d,\n"
                        + "ranking_strategy=%d, num_docs_scored=%d, parse_query_latency=%d, "
                        + "scoring_latency=%d, is_numeric_query=%b,\n"
                        + "num_fetched_hits_lite_index=%d, num_fetched_hits_main_index=%d, "
                        + "num_fetched_hits_integer_index=%d,\n"
                        + "query_processor_lexer_extract_token_latency=%d, "
                        + "query_processor_parser_consume_query_latency=%d,\n"
                        + "query_processor_query_visitor_latency=%d}",
                mNativeQueryLength,
                mNativeNumTerms,
                mNativeNumNamespacesFiltered,
                mNativeNumSchemaTypesFiltered,
                mNativeRankingStrategy,
                mNativeNumDocumentsScored,
                mNativeParseQueryLatencyMillis,
                mNativeScoringLatencyMillis,
                mNativeIsNumericQuery,
                mNativeNumFetchedHitsLiteIndex,
                mNativeNumFetchedHitsMainIndex,
                mNativeNumFetchedHitsIntegerIndex,
                mNativeQueryProcessorLexerExtractTokenLatencyMillis,
                mNativeQueryProcessorParserConsumeQueryLatencyMillis,
                mNativeQueryProcessorQueryVisitorLatencyMillis);
    }

    /** Builder for {@link SearchStats} */
    public static class Builder {
        int mNativeQueryLength;
        int mNativeNumTerms;
        int mNativeNumNamespacesFiltered;
        int mNativeNumSchemaTypesFiltered;
        @SearchSpec.RankingStrategy
        int mNativeRankingStrategy;
        int mNativeNumDocumentsScored;
        int mNativeParseQueryLatencyMillis;
        int mNativeScoringLatencyMillis;
        boolean mNativeIsNumericQuery;
        int mNativeNumFetchedHitsLiteIndex;
        int mNativeNumFetchedHitsMainIndex;
        int mNativeNumFetchedHitsIntegerIndex;
        int mNativeQueryProcessorLexerExtractTokenLatencyMillis;
        int mNativeQueryProcessorParserConsumeQueryLatencyMillis;
        int mNativeQueryProcessorQueryVisitorLatencyMillis;

        /** Sets length of the search string. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQueryLength(int nativeQueryLength) {
            mNativeQueryLength = nativeQueryLength;
            return this;
        }

        /** Sets number of terms in the search string. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeTermCount(int nativeTermCount) {
            mNativeNumTerms = nativeTermCount;
            return this;
        }

        /** Sets number of namespaces filtered. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeFilteredNamespaceCount(int nativeFilteredNamespaceCount) {
            mNativeNumNamespacesFiltered = nativeFilteredNamespaceCount;
            return this;
        }

        /** Sets number of schema types filtered. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeFilteredSchemaTypeCount(
                int nativeFilteredSchemaTypeCount) {
            mNativeNumSchemaTypesFiltered = nativeFilteredSchemaTypeCount;
            return this;
        }

        /** Sets strategy of scoring and ranking. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeRankingStrategy(
                @SearchSpec.RankingStrategy int nativeRankingStrategy) {
            mNativeRankingStrategy = nativeRankingStrategy;
            return this;
        }


        /** Sets number of documents scored. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeScoredDocumentCount(int nativeScoredDocumentCount) {
            mNativeNumDocumentsScored = nativeScoredDocumentCount;
            return this;
        }

        /**
         * Sets time used to parse the query, including 2 parts: tokenizing and
         * transforming tokens into an iterator tree.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeParseQueryLatencyMillis(
                int nativeParseQueryLatencyMillis) {
            mNativeParseQueryLatencyMillis = nativeParseQueryLatencyMillis;
            return this;
        }

        /** Sets time used to score the raw results. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeScoringLatencyMillis(int nativeScoringLatencyMillis) {
            mNativeScoringLatencyMillis = nativeScoringLatencyMillis;
            return this;
        }

        /** Sets whether it contains numeric query or not. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIsNumericQuery(boolean nativeIsNumericQuery) {
            mNativeIsNumericQuery = nativeIsNumericQuery;
            return this;
        }

        /** Sets number of hits fetched by lite index before applying any filters. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumFetchedHitsLiteIndex(
                int nativeNumFetchedHitsLiteIndex) {
            mNativeNumFetchedHitsLiteIndex = nativeNumFetchedHitsLiteIndex;
            return this;
        }

        /** Sets number of hits fetched by main index before applying any filters. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumFetchedHitsMainIndex(
                int nativeNumFetchedHitsMainIndex) {
            mNativeNumFetchedHitsMainIndex = nativeNumFetchedHitsMainIndex;
            return this;
        }

        /** Sets number of hits fetched by integer index before applying any filters. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeNumFetchedHitsIntegerIndex(
                int nativeNumFetchedHitsIntegerIndex) {
            mNativeNumFetchedHitsIntegerIndex = nativeNumFetchedHitsIntegerIndex;
            return this;
        }

        /** Sets time used in Lexer to extract lexer tokens from the query. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                int nativeQueryProcessorLexerExtractTokenLatencyMillis) {
            mNativeQueryProcessorLexerExtractTokenLatencyMillis =
                    nativeQueryProcessorLexerExtractTokenLatencyMillis;
            return this;
        }

        /** Sets time used in Parser to consume lexer tokens extracted from the query. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                int nativeQueryProcessorParserConsumeQueryLatencyMillis) {
            mNativeQueryProcessorParserConsumeQueryLatencyMillis =
                    nativeQueryProcessorParserConsumeQueryLatencyMillis;
            return this;
        }

        /** Sets time used in QueryVisitor to visit and build (nested) DocHitInfoIterator. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeQueryProcessorQueryVisitorLatencyMillis(
                int nativeQueryProcessorQueryVisitorLatencyMillis) {
            mNativeQueryProcessorQueryVisitorLatencyMillis =
                    nativeQueryProcessorQueryVisitorLatencyMillis;
            return this;
        }

        /**
         * Constructs a new {@link SearchStats} from the contents of this
         * {@link SearchStats.Builder}.
         */
        public @NonNull SearchStats build() {
            return new SearchStats(/* builder= */ this);
        }
    }
}
