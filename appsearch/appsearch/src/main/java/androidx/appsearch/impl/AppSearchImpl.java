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

package androidx.appsearch.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;

import java.util.Set;

/**
 * Manages interaction with {@link FakeIcing} and other components to implement AppSearch
 * functionality.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchImpl {
    private final FakeIcing mFakeIcing = new FakeIcing();

    /**
     * Updates the AppSearch schema for this app.
     *
     * @param origSchema The schema to set for this app.
     * @param forceOverride Whether to force-apply the schema even if it is incompatible. Documents
     *     which do not comply with the new schema will be deleted.
     */
    public void setSchema(@NonNull SchemaProto origSchema, boolean forceOverride) {
        // TODO(b/145635424): Save in schema type map
        // TODO(b/145635424): Apply the schema to Icing and report results
    }

    /**
     * Adds a document to the AppSearch index.
     *
     * @param document The document to index.
     */
    public void putDocument(@NonNull DocumentProto document) {
        mFakeIcing.put(document);
    }

    /**
     * Retrieves a document from the AppSearch index by URI.
     *
     * @param uri The URI of the document to get.
     * @return The Document contents, or {@code null} if no such URI exists in the system.
     */
    @Nullable
    public DocumentProto getDocument(@NonNull String uri) {
        return mFakeIcing.get(uri);
    }

    /**
     * Executes a query against the AppSearch index and returns results.
     *
     * @param searchSpec Defines what and how to search
     * @param resultSpec Defines what results to show
     * @param scoringSpec Defines how to order results
     * @return The results of performing this search  The proto might have no {@code results} if no
     *     documents matched the query.
     */
    @NonNull
    public SearchResultProto query(
            @NonNull SearchSpecProto searchSpec,
            @NonNull ResultSpecProto resultSpec,
            @NonNull ScoringSpecProto scoringSpec) {
        SearchResultProto searchResults = mFakeIcing.query(searchSpec.getQuery());
        if (searchResults.getResultsCount() == 0) {
            return searchResults;
        }
        Set<String> qualifiedSearchFilters = null;
        if (searchSpec.getSchemaTypeFiltersCount() > 0) {
            qualifiedSearchFilters = new ArraySet<>(searchSpec.getSchemaTypeFiltersList());
        }
        SearchResultProto.Builder searchResultsBuilder = searchResults.toBuilder();
        for (int i = 0; i < searchResultsBuilder.getResultsCount(); i++) {
            if (searchResults.getResults(i).hasDocument()) {
                // TODO(b/145631811): Since FakeIcing doesn't currently handle type names, we
                //  perform a post-filter to make sure we don't return documents we shouldn't. This
                //  should be removed once the real Icing Lib is implemented.
                if (qualifiedSearchFilters != null
                        && !qualifiedSearchFilters.contains(
                        searchResultsBuilder.getResults(i).getDocument().getSchema())) {
                    searchResultsBuilder.removeResults(i);
                    i--;
                }
            }
        }
        return searchResultsBuilder.build();
    }

    /** Deletes the given document by URI */
    public boolean delete(@NonNull String uri) {
        DocumentProto document = mFakeIcing.get(uri);
        if (document == null) {
            return false;
        }
        return mFakeIcing.delete(uri);
    }

    /** Deletes all documents having the given {@code schemaType}. */
    public boolean deleteByType(@NonNull String schemaType) {
        return mFakeIcing.deleteByType(schemaType);
    }

    /**  Deletes all documents owned by the calling app. */
    public void deleteAll() {
        mFakeIcing.deleteAll();
    }
}
