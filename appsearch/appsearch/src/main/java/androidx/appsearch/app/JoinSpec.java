/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class represents the specifications for the joining operation in search.
 *
 * <p> Joins are only possible for matching on the qualified id of an outer document and a
 * property value within a subquery document. In the subquery documents, these values may be
 * referred to with a property path such as "email.recipient.id" or "entityId" or a property
 * expression. One such property expression is "this.qualifiedId()", which refers to the
 * document's combined package, database, namespace, and id.
 *
 * <p> Take these outer query and subquery results for example:
 *
 * <pre>{@code
 * Outer result {
 *   id: id1
 *   score: 5
 * }
 * Subquery result 1 {
 *   id: id2
 *   score: 2
 *   entityId: pkg$db/ns#id1
 *   notes: This is some doc
 * }
 * Subquery result 2 {
 *   id: id3
 *   score: 3
 *   entityId: pkg$db/ns#id2
 *   notes: This is another doc
 * }
 * }</pre>
 *
 * <p> In this example, subquery result 1 contains a property "entityId" whose value is
 * "pkg$db/ns#id1", referring to the outer result. If you call {@link Builder} with "entityId", we
 * will retrieve the value of the property "entityId" from the child document, which is
 * "pkg$db#ns/id1". Let's say the qualified id of the outer result is "pkg$db#ns/id1". This would
 * mean the subquery result 1 document will be matched to that parent document. This is done by
 * adding a {@link SearchResult} containing the child document to the top-level parent
 * {@link SearchResult#getJoinedResults}.
 *
 * <p> If {@link #getChildPropertyExpression} is "notes", we will check the values of the notes
 * property in the subquery results. In subquery result 1, this values is "This is some doc", which
 * does not equal the qualified id of the outer query result. As such, subquery result 1 will not be
 * joined to the outer query result.
 *
 * <p> In terms of scoring, if {@link SearchSpec#RANKING_STRATEGY_JOIN_AGGREGATE_SCORE} is set in
 * {@link SearchSpec#getRankingStrategy}, the scores of the outer SearchResults can be influenced
 * by the ranking signals of the subquery results. For example, if the
 * {@link JoinSpec#getAggregationScoringStrategy} is set to
 * {@link JoinSpec#AGGREGATION_SCORING_MIN_RANKING_SIGNAL}, the ranking signal of the outer
 * {@link SearchResult} will be set to the minimum of the ranking signals of the subquery results.
 * In this case, it will be the minimum of 2 and 3, which is 2. If the
 * {@link JoinSpec#getAggregationScoringStrategy} is set to
 * {@link JoinSpec#AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL}, the ranking signal of the outer
 * {@link SearchResult} will stay as it is.
 */
// TODO(b/256022027): Update javadoc once "Joinable"/"qualifiedId" type is added to reflect the
//  fact that childPropertyExpression has to point to property of that type.
public final class JoinSpec {
    static final String NESTED_QUERY = "nestedQuery";
    static final String NESTED_SEARCH_SPEC = "nestedSearchSpec";
    static final String CHILD_PROPERTY_EXPRESSION = "childPropertyExpression";
    static final String MAX_JOINED_RESULT_COUNT = "maxJoinedResultCount";
    static final String AGGREGATION_SCORING_STRATEGY = "aggregationScoringStrategy";

    private static final int DEFAULT_MAX_JOINED_RESULT_COUNT = 10;

    /**
     * A property expression referring to the combined package name, database name, namespace, and
     * id of the document.
     *
     * <p> For instance, if a document with an id of "id1" exists in the namespace "ns" within
     * the database "db" created by package "pkg", this would evaluate to "pkg$db/ns#id1".
     *
     * <!--@exportToFramework:hide-->
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String QUALIFIED_ID = "this.qualifiedId()";

    /**
     * Aggregation scoring strategy for join spec.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link JoinSpecProto.AggregationScoreStrategy.Code}
    @IntDef(value = {
            AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL,
            AGGREGATION_SCORING_RESULT_COUNT,
            AGGREGATION_SCORING_MIN_RANKING_SIGNAL,
            AGGREGATION_SCORING_AVG_RANKING_SIGNAL,
            AGGREGATION_SCORING_MAX_RANKING_SIGNAL,
            AGGREGATION_SCORING_SUM_RANKING_SIGNAL
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface AggregationScoringStrategy {
    }

    /** Do not score the aggregation of joined documents. This is for the case where we want to
     * perform a join, but keep the parent ranking signal. */
    public static final int AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL = 0;
    /** Score the aggregation of joined documents by counting the number of results. */
    public static final int AGGREGATION_SCORING_RESULT_COUNT = 1;
    /** Score the aggregation of joined documents using the smallest ranking signal. */
    public static final int AGGREGATION_SCORING_MIN_RANKING_SIGNAL = 2;
    /** Score the aggregation of joined documents using the average ranking signal. */
    public static final int AGGREGATION_SCORING_AVG_RANKING_SIGNAL = 3;
    /** Score the aggregation of joined documents using the largest ranking signal. */
    public static final int AGGREGATION_SCORING_MAX_RANKING_SIGNAL = 4;
    /** Score the aggregation of joined documents using the sum of ranking signal. */
    public static final int AGGREGATION_SCORING_SUM_RANKING_SIGNAL = 5;

    private final Bundle mBundle;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public JoinSpec(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Returns the query to run on the joined documents.
     *
     */
    @NonNull
    public String getNestedQuery() {
        return mBundle.getString(NESTED_QUERY);
    }

    /**
     * The property expression that is used to get values from child documents, returned from the
     * nested search. These values are then used to match them to parent documents. These are
     * analogous to foreign keys.
     *
     * @return the property expression to match in the child documents.
     * @see Builder
     */
    @NonNull
    public String getChildPropertyExpression() {
        return mBundle.getString(CHILD_PROPERTY_EXPRESSION);
    }

    /**
     * Returns the max amount of {@link SearchResult} objects that will be joined to the parent
     * document, with a default of 10 SearchResults.
     */
    public int getMaxJoinedResultCount() {
        return mBundle.getInt(MAX_JOINED_RESULT_COUNT);
    }

    /**
     * Returns the search spec used to retrieve the joined documents.
     *
     * <p> If {@link Builder#setNestedSearch} is never called, this will return a {@link SearchSpec}
     * with all default values. This will match every document, as the nested search query will
     * be "" and no schema will be filtered out.
     */
    @NonNull
    public SearchSpec getNestedSearchSpec() {
        return new SearchSpec(mBundle.getBundle(NESTED_SEARCH_SPEC));
    }

    /**
     * Gets the joined document list scoring strategy.
     *
     * <p> The default scoring strategy is {@link #AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL},
     * which specifies that the score of the outer parent document will be used.
     *
     * @see SearchSpec#RANKING_STRATEGY_JOIN_AGGREGATE_SCORE
     */
    @AggregationScoringStrategy
    public int getAggregationScoringStrategy() {
        return mBundle.getInt(AGGREGATION_SCORING_STRATEGY);
    }

    /** Builder for {@link JoinSpec objects}. */
    public static final class Builder {

        // The default nested SearchSpec.
        private static final SearchSpec EMPTY_SEARCH_SPEC = new SearchSpec.Builder().build();

        private String mNestedQuery = "";
        private SearchSpec mNestedSearchSpec = EMPTY_SEARCH_SPEC;
        private final String mChildPropertyExpression;
        private int mMaxJoinedResultCount = DEFAULT_MAX_JOINED_RESULT_COUNT;
        @AggregationScoringStrategy private int mAggregationScoringStrategy =
                AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL;

        /**
         * Create a specification for the joining operation in search.
         *
         * <p> The child property expressions Specifies how to join documents. Documents with
         * a child property expression equal to the qualified id of the parent will be retrieved.
         *
         * <p> Property expressions differ from {@link PropertyPath} as property expressions may
         * refer to document properties or nested document properties such as "person.business.id"
         * as well as a property expression. Currently the only property expression is
         * "this.qualifiedId()". {@link PropertyPath} objects may only reference document properties
         * and nested document properties.
         *
         * <p> In order to join a child document to a parent document, the child document must
         * contain the parent's qualified id at the property expression specified by this
         * method.
         *
         * @param childPropertyExpression the property to match in the child documents.
         */
        // TODO(b/256022027): Reword comments to reference either "expression" or "PropertyPath"
        //  once wording is finalized.
        // TODO(b/256022027): Add another method to allow providing PropertyPath objects as
        //  equality constraints.
        // TODO(b/256022027): Change to allow for multiple child property expressions if multiple
        //  parent property expressions get supported.
        public Builder(@NonNull String childPropertyExpression) {
            Preconditions.checkNotNull(childPropertyExpression);
            mChildPropertyExpression = childPropertyExpression;
        }

        /**
         * Further filters the documents being joined.
         *
         * <p> If this method is never called, {@link JoinSpec#getNestedQuery} will return an empty
         * string, meaning we will join with every possible document that matches the equality
         * constraints and hasn't been filtered out by the type or namespace filters.
         *
         * @see JoinSpec#getNestedQuery
         * @see JoinSpec#getNestedSearchSpec
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // See getNestedQuery & getNestedSearchSpec
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNestedSearch(@NonNull String nestedQuery,
                @NonNull SearchSpec nestedSearchSpec) {
            Preconditions.checkNotNull(nestedQuery);
            Preconditions.checkNotNull(nestedSearchSpec);
            mNestedQuery = nestedQuery;
            mNestedSearchSpec = nestedSearchSpec;

            return this;
        }

        /**
         * Sets the max amount of {@link SearchResults} to join to the parent document, with a
         * default of 10 SearchResults.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setMaxJoinedResultCount(int maxJoinedResultCount) {
            mMaxJoinedResultCount = maxJoinedResultCount;
            return this;
        }

        /**
         * Sets how we derive a single score from a list of joined documents.
         *
         * <p> The default scoring strategy is
         * {@link #AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL}, which specifies that the
         * ranking signal of the outer parent document will be used.
         *
         * @see SearchSpec#RANKING_STRATEGY_JOIN_AGGREGATE_SCORE
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setAggregationScoringStrategy(
                @AggregationScoringStrategy int aggregationScoringStrategy) {
            Preconditions.checkArgumentInRange(aggregationScoringStrategy,
                    AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL,
                    AGGREGATION_SCORING_SUM_RANKING_SIGNAL, "aggregationScoringStrategy");
            mAggregationScoringStrategy = aggregationScoringStrategy;
            return this;
        }

        /**
         * Constructs a new {@link JoinSpec} from the contents of this builder.
         */
        @NonNull
        public JoinSpec build() {
            Bundle bundle = new Bundle();
            bundle.putString(NESTED_QUERY, mNestedQuery);
            bundle.putBundle(NESTED_SEARCH_SPEC, mNestedSearchSpec.getBundle());
            bundle.putString(CHILD_PROPERTY_EXPRESSION, mChildPropertyExpression);
            bundle.putInt(MAX_JOINED_RESULT_COUNT, mMaxJoinedResultCount);
            bundle.putInt(AGGREGATION_SCORING_STRATEGY, mAggregationScoringStrategy);
            return new JoinSpec(bundle);
        }
    }
}
