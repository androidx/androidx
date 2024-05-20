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
// @exportToFramework:skipFile()

package androidx.appsearch.usagereporting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

/**
 * {@link SearchAction} is a built-in AppSearch document type that contains different metrics.
 * <ul>
 *     <li>Clients can report the user's search actions.
 *     <li>Usually {@link SearchAction} is reported together with {@link ClickAction}, since the
 *     user clicks on {@link androidx.appsearch.app.SearchResult} documents after searching.
 * </ul>
 *
 * <p>In order to use this document type, the client must explicitly set this schema type via
 * {@link androidx.appsearch.app.SetSchemaRequest.Builder#addDocumentClasses}.
 *
 * <p>Since {@link SearchAction} is an AppSearch document, the client can handle deletion via
 * {@link androidx.appsearch.app.AppSearchSession#removeAsync} or document time-to-live (TTL). The
 * default TTL is 60 days.
 */
@Document(name = "builtin:SearchAction")
public class SearchAction extends TakenAction {
    @Nullable
    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mQuery;

    @Document.LongProperty
    private final int mFetchedResultCount;

    SearchAction(@NonNull String namespace, @NonNull String id, long documentTtlMillis,
            long actionTimestampMillis, @TakenAction.ActionType int actionType,
            @Nullable String query, int fetchedResultCount) {
        super(namespace, id, documentTtlMillis, actionTimestampMillis, actionType);

        mQuery = query;
        mFetchedResultCount = fetchedResultCount;
    }

    /** Returns the user-entered search input (without any operators or rewriting). */
    @Nullable
    public String getQuery() {
        return mQuery;
    }

    /**
     * Returns total number of results fetched from AppSearch by the client in this
     * {@link SearchAction}.
     *
     * <p>If unset, then it will be set to -1 to mark invalid.
     */
    public int getFetchedResultCount() {
        return mFetchedResultCount;
    }

    // TODO(b/314026345): redesign builder to enable inheritance for SearchAction.
    /** Builder for {@link SearchAction}. */
    @Document.BuilderProducer
    public static final class Builder extends BuilderImpl<Builder> {
        private String mQuery;
        private int mFetchedResultCount;

        /**
         * Constructor for {@link SearchAction.Builder}.
         *
         * @param namespace             Namespace for the Document. See {@link Document.Namespace}.
         * @param id                    Unique identifier for the Document. See {@link Document.Id}.
         * @param actionTimestampMillis The timestamp when the user took the action, in milliseconds
         *                              since Unix epoch.
         */
        public Builder(@NonNull String namespace, @NonNull String id, long actionTimestampMillis) {
            this(namespace, id, actionTimestampMillis, ActionConstants.ACTION_TYPE_SEARCH);
        }

        /**
         * Constructor for {@link Builder} with all the existing values.
         */
        public Builder(@NonNull SearchAction searchAction) {
            super(Preconditions.checkNotNull(searchAction));

            mQuery = searchAction.getQuery();
            mFetchedResultCount = searchAction.getFetchedResultCount();
        }

        /**
         * Constructor for {@link SearchAction.Builder}.
         *
         * <p>It is required by {@link Document.BuilderProducer}.
         *
         * @param namespace             Namespace for the Document. See {@link Document.Namespace}.
         * @param id                    Unique identifier for the Document. See {@link Document.Id}.
         * @param actionTimestampMillis The timestamp when the user took the action, in milliseconds
         *                              since Unix epoch.
         * @param actionType            Action type enum for the Document. See
         *                              {@link TakenAction.ActionType}.
         */
        Builder(@NonNull String namespace, @NonNull String id, long actionTimestampMillis,
                @TakenAction.ActionType int actionType) {
            super(namespace, id, actionTimestampMillis, actionType);

            // Default for unset fetchedResultCount. Since negative number is invalid for fetched
            // result count, -1 is used as an unset value and AppSearch will ignore it.
            mFetchedResultCount = -1;
        }

        /** Sets the user-entered search input (without any operators or rewriting). */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setQuery(@Nullable String query) {
            mQuery = query;
            return this;
        }

        /**
         * Sets total number of results fetched from AppSearch by the client in this
         * {@link SearchAction}.
         *
         * @see SearchAction#getFetchedResultCount
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setFetchedResultCount(int fetchedResultCount) {
            mFetchedResultCount = fetchedResultCount;
            return this;
        }

        /** Builds a {@link SearchAction}. */
        @Override
        @NonNull
        public SearchAction build() {
            return new SearchAction(mNamespace, mId, mDocumentTtlMillis, mActionTimestampMillis,
                    mActionType, mQuery, mFetchedResultCount);
        }
    }
}
