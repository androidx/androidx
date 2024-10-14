/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.ast.query;

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.ast.FunctionNode;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

/**
 * {@link FunctionNode} that represents the semanticSearch function.
 *
 * <p>The semanticSearch function matches all documents that have at least one embedding vector with
 * a matching model signature
 * (see {@link androidx.appsearch.app.EmbeddingVector#getModelSignature()}) and a similarity score
 * within the range specified based on the provided metric.
 *
 * <p>This node can be used to build a query that contains the semanticSearch function. For example,
 * the node {@code SemanticSearchNode(0, -0.5, 0.5, DOT_PRODUCT)} is equivalent
 * to the query `semanticSearch(getEmbeddingParameter(0), -0.5, 0.5, "DOT_PRODUCT")`.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class SemanticSearchNode implements FunctionNode {
    private int mVectorIndex;
    private float mLowerBound;
    private float mUpperBound;
    private @SearchSpec.EmbeddingSearchMetricType int mDistanceMetric;

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * @param vectorIndex The index of the embedding vector in the list of vectors returned by
     *                   {@link SearchSpec#getEmbeddingParameters()} to use in the search.
     * @param lowerBound The lower bound on similarity score for a embedding vector such that the
     *                   associated document will be returned.
     * @param upperBound The upper bound on similarity score for a embedding vector such that the
     *                   associated document will be returned.
     * @param distanceMetric How distance between embedding vectors will be calculated.
     */
    public SemanticSearchNode(int vectorIndex, float lowerBound, float upperBound,
            @SearchSpec.EmbeddingSearchMetricType int distanceMetric) {
        Preconditions.checkArgument(vectorIndex >= 0,
                "Vector index must be non-negative.");
        Preconditions.checkArgument(lowerBound <= upperBound,
                "Provided lower bound must be less than or equal to"
                        + " the provided upper bound.");
        Preconditions.checkArgumentInRange(distanceMetric,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
                "Embedding search metric type");
        mVectorIndex = vectorIndex;
        mLowerBound = lowerBound;
        mUpperBound = upperBound;
        mDistanceMetric = distanceMetric;
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     * <ul>
     *     <li> The default set by the user and returned by
     *     {@link SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine
     *     similarity between embedding vectors. If no default is set, cosine similarity will be
     *     used.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex, float lowerBound, float upperBound) {
        this(vectorIndex, lowerBound, upperBound, SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT);
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     * <ul>
     *     <li> The default set by the user and returned by
     *     {@link SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine
     *     similarity between embedding vectors. If no default is set, cosine similarity will be
     *     used.
     *     <li> The upper bound on similarity scores for an embedding vector such that the
     *     associated document will be returned is positive infinity.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex, float lowerBound) {
        this(vectorIndex, lowerBound, Float.POSITIVE_INFINITY);
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     * <ul>
     *     <li> The default set by the user and returned by
     *      {@link SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine
     *      similarity between embedding vectors. If no default is set, cosine similarity will be
     *      used.
     *     <li> The upper bound on similarity scores for an embedding vector such that the
     *     associated document will be returned is positive infinity.
     *     <li> The lower bound on similarity scores for an embedding vector such that the
     *      associated document will be returned is negative infinity.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex) {
        this(vectorIndex, Float.NEGATIVE_INFINITY);
    }

    /**
     * Returns the name of the function represented by {@link SemanticSearchNode}.
     */
    @NonNull
    @Override
    @FunctionName
    public String getFunctionName() {
        return FUNCTION_NAME_SEMANTIC_SEARCH;
    }

    /**
     * Returns the index of the embedding vector used in semanticSearch.
     */
    public int getVectorIndex() {
        return mVectorIndex;
    }

    /**
     * Returns the lower bound of the range of values similarity scores must fall in.
     */
    public float getLowerBound() {
        return mLowerBound;
    }

    /**
     * Returns the upper bound of the range of values similarity scores must fall in.
     */
    public float getUpperBound() {
        return mUpperBound;
    }

    /**
     * Returns the distance metric used to calculated similarity between embedding vectors.
     */
    @SearchSpec.EmbeddingSearchMetricType
    public int getDistanceMetric() {
        return mDistanceMetric;
    }

    /**
     * Sets the index of the embedding vector that semanticSearch will use.
     */
    public void setVectorIndex(int vectorIndex) {
        Preconditions.checkArgument(vectorIndex >= 0, "Vector Index must be non-negative.");
        mVectorIndex = vectorIndex;
    }

    /**
     * Sets the bounds of the range of values that semanticSearch will search against.
     *
     * @param lowerBound The lower bound of the range of values.
     * @param upperBound The upper bound of the range of values.
     */
    public void setBounds(float lowerBound, float upperBound) {
        Preconditions.checkArgument(lowerBound <= upperBound,
                "Provided lower bound must be less than or equal to"
                        + " the provided upper bound");
        mLowerBound = lowerBound;
        mUpperBound = upperBound;
    }

    /**
     * Sets how similarity is calculated between embedding vectors.
     *
     * @param distanceMetric How similarity is calculated between embedding vectors.
     */
    public void setDistanceMetric(@SearchSpec.EmbeddingSearchMetricType int distanceMetric) {
        Preconditions.checkArgumentInRange(distanceMetric,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
                "Embedding search metric type");
        mDistanceMetric = distanceMetric;
    }
}
