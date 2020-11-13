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

package androidx.appsearch.app.util;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;

import junit.framework.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class AppSearchTestUtils {

    public static <V> V checkIsResultSuccess(Future<AppSearchResult<V>> future) throws Exception {
        AppSearchResult<V> result = future.get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchResult not successful: " + result);
        }
        return result.getResultValue();
    }

    public static <K, V> AppSearchBatchResult<K, V> checkIsBatchResultSuccess(
            Future<AppSearchBatchResult<K, V>> future) throws Exception {
        AppSearchBatchResult<K, V> result = future.get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchBatchResult not successful: " + result);
        }
        return result;
    }

    public static List<GenericDocument> doGet(
            AppSearchSession session, String namespace, String... uris) throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                session.getByUri(
                        new GetByUriRequest.Builder()
                                .setNamespace(namespace).addUri(uris).build()));
        assertThat(result.getSuccesses()).hasSize(uris.length);
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(uris.length);
        for (String uri : uris) {
            list.add(result.getSuccesses().get(uri));
        }
        return list;
    }

    public static List<GenericDocument> convertSearchResultsToDocuments(SearchResults searchResults)
            throws Exception {
        List<SearchResult> results = checkIsResultSuccess(searchResults.getNextPage());
        List<GenericDocument> documents = new ArrayList<>();
        while (results.size() > 0) {
            for (SearchResult result : results) {
                documents.add(result.getDocument());
            }
            results = checkIsResultSuccess(searchResults.getNextPage());
        }
        return documents;
    }
}
