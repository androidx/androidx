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
// @exportToFramework:skipFile()

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TakenAction} is a built-in AppSearch document type that contains different metrics.
 * <ul>
 *     <li>Clients can report the user's actions (e.g. click) on a {@link SearchResult} document.
 *     <li>Also several other types of actions can be analyzed and extracted from fields in
 *     {@link TakenAction}. For example, query abandonment will be derived from
 *     {@link TakenAction#getPreviousQueries} and {@link TakenAction#getFinalQuery}.
 * </ul>
 *
 * <p>In order to use this document type, the client must explicitly set this schema type via
 * {@link SetSchemaRequest.Builder#addDocumentClasses}.
 *
 * <p>These actions can be used as signals to boost ranking via {@link JoinSpec} API in future
 * search requests.
 *
 * <p>Since {@link TakenAction} is an AppSearch document, the client can handle deletion via
 * {@link AppSearchSession#removeAsync} or document time-to-live (TTL). The default TTL is 60 days.
 */
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.JOIN_SPEC_AND_QUALIFIED_ID)
@Document(name = "builtin:TakenAction")
public class TakenAction {
    /** Default TTL for {@link TakenAction} document: 60 days. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final long DEFAULT_DOCUMENT_TTL_MILLIS = 60L * 24 * 60 * 60 * 1000;

    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.StringProperty
    private final String mName;

    @Document.StringProperty(joinableValueType =
            StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
    private final String mReferencedQualifiedId;

    @Document.StringProperty
    private final List<String> mPreviousQueries;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
    private final String mFinalQuery;

    @Document.LongProperty
    private final int mResultRankInBlock;

    @Document.LongProperty
    private final int mResultRankGlobal;

    @Document.LongProperty
    private final long mTimeStayOnResultMillis;

    TakenAction(@NonNull String namespace, @NonNull String id,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            @Nullable String referencedQualifiedId, @Nullable List<String> previousQueries,
            @Nullable String finalQuery, int resultRankInBlock, int resultRankGlobal,
            long timeStayOnResultMillis) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mName = name;
        mReferencedQualifiedId = referencedQualifiedId;
        if (previousQueries == null) {
            mPreviousQueries = Collections.emptyList();
        } else {
            mPreviousQueries = Collections.unmodifiableList(previousQueries);
        }
        mFinalQuery = finalQuery;
        mResultRankInBlock = resultRankInBlock;
        mResultRankGlobal = resultRankGlobal;
        mTimeStayOnResultMillis = timeStayOnResultMillis;
    }

    /** Returns the namespace of the {@link TakenAction}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of the {@link TakenAction}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the creation timestamp of the {@link TakenAction} document, in milliseconds since
     * Unix epoch.
     *
     * <p>This timestamp refers to the creation time of the document, not when it is written
     * into AppSearch.
     *
     * <p>If not set, then the current timestamp will be used.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
     * information on creation timestamp.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) of the {@link TakenAction} document as a duration in
     * milliseconds.
     *
     * <p>The document will be automatically deleted when the TTL expires (since
     * {@link #getCreationTimestampMillis()}).
     *
     * <p>The default TTL for {@link TakenAction} document is 60 days.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on TTL.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /**
     * Returns the name of the {@link TakenAction}.
     *
     * <p>Name is an optional custom field that allows the client to tag and categorize
     * {@link TakenAction}.
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Returns the qualified id of the {@link SearchResult} document that the user takes action on.
     *
     * <p>A qualified id is a string generated by package, database, namespace, and document id. See
     * {@link androidx.appsearch.util.DocumentIdUtil#createQualifiedId(String,String,String,String)}
     * for more details.
     */
    @Nullable
    public String getReferencedQualifiedId() {
        return mReferencedQualifiedId;
    }

    /**
     * Returns the list of all previous user-entered search inputs, without any operators or
     * rewriting, collected during this search session in chronological order.
     */
    @NonNull
    public List<String> getPreviousQueries() {
        return mPreviousQueries;
    }

    /**
     * Returns the final user-entered search input (without any operators or rewriting) that yielded
     * the {@link SearchResult} on which the user took action.
     */
    @Nullable
    public String getFinalQuery() {
        return mFinalQuery;
    }

    /**
     * Returns the rank of the {@link SearchResult} document among the user-defined block.
     *
     * <p>The client can define its own custom definition for block, e.g. corpus name, group, etc.
     *
     * <p>For example, a client defines the block as corpus, and AppSearch returns 5 documents with
     * corpus = ["corpus1", "corpus1", "corpus2", "corpus3", "corpus2"]. Then the block ranks of
     * them = [1, 2, 1, 1, 2].
     *
     * <p>If the client is not presenting the results in multiple blocks, they should set this value
     * to match {@link #getResultRankGlobal}.
     *
     * <p>If unset, then the block rank of the {@link SearchResult} document will be set to -1 to
     * mark invalid.
     */
    public int getResultRankInBlock() {
        return mResultRankInBlock;
    }

    /**
     * Returns the global rank of the {@link SearchResult} document.
     *
     * <p>Global rank reflects the order of {@link SearchResult} documents returned by AppSearch.
     *
     * <p>For example, AppSearch returns 2 pages with 10 {@link SearchResult} documents for each
     * page. Then the global ranks of them will be 1 to 10 for the first page, and 11 to 20 for the
     * second page.
     *
     * <p>If unset, then the global rank of the {@link SearchResult} document will be set to -1 to
     * mark invalid.
     */
    public int getResultRankGlobal() {
        return mResultRankGlobal;
    }

    /**
     * Returns the time in milliseconds that user stays on the {@link SearchResult} document after
     * clicking it.
     */
    public long getTimeStayOnResultMillis() {
        return mTimeStayOnResultMillis;
    }

    // TODO(b/314026345): redesign builder to enable inheritance for TakenAction.
    /** Builder for {@link TakenAction}. */
    @Document.BuilderProducer
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private long mCreationTimestampMillis;
        private long mDocumentTtlMillis;
        private String mName;
        private String mReferencedQualifiedId;
        private List<String> mPreviousQueries = new ArrayList<>();
        private String mFinalQuery;
        private int mResultRankInBlock;
        private int mResultRankGlobal;
        private long mTimeStayOnResultMillis;
        private boolean mBuilt = false;

        /**
         * Constructs {@link TakenAction.Builder} with given {@code namespace} and {@code id}.
         *
         * @param namespace The namespace of the {@link TakenAction} document.
         * @param id        The id of the {@TakenAction} document.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);

            // Default for unset creationTimestampMillis. AppSearch will internally convert this
            // to current time when creating the GenericDocument.
            mCreationTimestampMillis = -1;

            // Default for mDocumentTtlMillis.
            mDocumentTtlMillis = TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS;

            // Default for unset result rank fields. Since negative number is invalid for ranking,
            // -1 is used as an unset value and AppSearch will ignore it.
            mResultRankInBlock = -1;
            mResultRankGlobal = -1;

            // Default for unset timeStayOnResultMillis. Since negative number is invalid for
            // time in millis, -1 is used as an unset value and AppSearch will ignore it.
            mTimeStayOnResultMillis = -1;
        }

        /**
         * Constructs {@link TakenAction.Builder} by copying existing values from the given
         * {@link TakenAction}.
         *
         * @param takenAction an existing {@link TakenAction} object.
         */
        public Builder(@NonNull TakenAction takenAction) {
            this(takenAction.getNamespace(), takenAction.getId());
            mCreationTimestampMillis = takenAction.getCreationTimestampMillis();
            mDocumentTtlMillis = takenAction.getDocumentTtlMillis();
            mName = takenAction.getName();
            mReferencedQualifiedId = takenAction.getReferencedQualifiedId();
            mPreviousQueries = new ArrayList<>(takenAction.getPreviousQueries());
            mFinalQuery = takenAction.getFinalQuery();
            mResultRankInBlock = takenAction.getResultRankInBlock();
            mResultRankGlobal = takenAction.getResultRankGlobal();
            mTimeStayOnResultMillis = takenAction.getTimeStayOnResultMillis();
        }

        /**
         * Sets the creation timestamp of the {@link TakenAction} document, in milliseconds since
         * Unix epoch.
         *
         * <p>This timestamp refers to the creation time of the document, not when it is written
         * into AppSearch.
         *
         * <p>If not set, then the current timestamp will be used.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
         * information on creation timestamp.
         */
        @NonNull
        public Builder setCreationTimestampMillis(long creationTimestampMillis) {
            resetIfBuilt();
            mCreationTimestampMillis = creationTimestampMillis;
            return this;
        }

        /**
         * Sets the time-to-live (TTL) of the {@link TakenAction} document as a duration in
         * milliseconds.
         *
         * <p>The document will be automatically deleted when the TTL expires (since
         * {@link TakenAction#getCreationTimestampMillis()}).
         *
         * <p>The default TTL for {@link TakenAction} document is 60 days.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on
         * TTL.
         */
        @NonNull
        public Builder setDocumentTtlMillis(long documentTtlMillis) {
            resetIfBuilt();
            mDocumentTtlMillis = documentTtlMillis;
            return this;
        }

        /**
         * Sets the action type name of the {@link TakenAction}.
         *
         * @see TakenAction#getName
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            resetIfBuilt();
            mName = name;
            return this;
        }

        /**
         * Sets the qualified id of the {@link SearchResult} document that the user takes action on.
         *
         * <p>A qualified id is a string generated by package, database, namespace, and document id.
         * See {@link androidx.appsearch.util.DocumentIdUtil#createQualifiedId(
         * String,String,String,String)} for more details.
         */
        @NonNull
        public Builder setReferencedQualifiedId(@Nullable String referencedQualifiedId) {
            resetIfBuilt();
            mReferencedQualifiedId = referencedQualifiedId;
            return this;
        }

        /**
         * Adds one previous user-entered search input, without any operators or rewriting,
         * collected during this search session in chronological order.
         */
        @NonNull
        public Builder addPreviousQuery(@NonNull String query) {
            resetIfBuilt();
            mPreviousQueries.add(query);
            return this;
        }

        /**
         * Sets a list of previous user-entered search inputs, without any operators or rewriting,
         * collected during this search session in chronological order.
         */
        @NonNull
        public Builder setPreviousQueries(@Nullable List<String> previousQueries) {
            resetIfBuilt();
            clearPreviousQueries();
            if (previousQueries != null) {
                mPreviousQueries.addAll(previousQueries);
            }
            return this;
        }

        /**
         * Sets the final user-entered search input (without any operators or rewriting) that
         * yielded the {@link SearchResult} on which the user took action.
         */
        @NonNull
        public Builder setFinalQuery(@Nullable String finalQuery) {
            resetIfBuilt();
            mFinalQuery = finalQuery;
            return this;
        }

        /**
         * Sets the rank of the {@link SearchResult} document among the user-defined block.
         *
         * @see TakenAction#getResultRankInBlock
         */
        @NonNull
        public Builder setResultRankInBlock(int resultRankInBlock) {
            resetIfBuilt();
            mResultRankInBlock = resultRankInBlock;
            return this;
        }

        /**
         * Sets the global rank of the {@link SearchResult} document.
         *
         * @see TakenAction#getResultRankGlobal
         */
        @NonNull
        public Builder setResultRankGlobal(int resultRankGlobal) {
            resetIfBuilt();
            mResultRankGlobal = resultRankGlobal;
            return this;
        }

        /**
         * Sets the time in milliseconds that user stays on the {@link SearchResult} document after
         * clicking it.
         */
        @NonNull
        public Builder setTimeStayOnResultMillis(long timeStayOnResultMillis) {
            resetIfBuilt();
            mTimeStayOnResultMillis = timeStayOnResultMillis;
            return this;
        }

        /**
         * Clear all the previous queries which were previously added by {@link #addPreviousQuery}
         * or set by {@link #setPreviousQueries}.
         */
        private void clearPreviousQueries() {
            mPreviousQueries.clear();
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        private void resetIfBuilt() {
            if (mBuilt) {
                mPreviousQueries = new ArrayList<>(mPreviousQueries);
                mBuilt = false;
            }
        }

        /** Builds a {@link TakenAction}. */
        @NonNull
        public TakenAction build() {
            mBuilt = true;
            return new TakenAction(mNamespace, mId, mCreationTimestampMillis, mDocumentTtlMillis,
                    mName, mReferencedQualifiedId, mPreviousQueries, mFinalQuery,
                    mResultRankInBlock, mResultRankGlobal, mTimeStayOnResultMillis);
        }
    }
}
