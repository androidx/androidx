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

import static com.google.common.truth.Truth.assertThat;

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

    public static List<GenericDocument> doGet(
            AppSearchManager instance, String namespace, String... uris) throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                instance.getByUri(
                        new GetByUriRequest.Builder()
                                .setNamespace(namespace).addUris(uris).build()));
        assertThat(result.getSuccesses()).hasSize(uris.length);
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(uris.length);
        for (String uri : uris) {
            list.add(result.getSuccesses().get(uri));
        }
        return list;
    }

    public static List<GenericDocument> doQuery(
            AppSearchManager instance, String queryExpression, SearchSpec spec)
            throws Exception {
        SearchResults searchResults = instance.query(queryExpression, spec);
        List<SearchResults.Result> results = checkIsResultSuccess(searchResults.getNextPage());
        List<GenericDocument> documents = new ArrayList<>();
        while (results.size() > 0) {
            for (SearchResults.Result result : results) {
                documents.add(result.getDocument());
            }
            results = checkIsResultSuccess(searchResults.getNextPage());
        }
        return documents;
    }

    public static List<GenericDocument> doQuery(AppSearchManager instance, String queryExpression)
            throws Exception {
        return doQuery(instance, queryExpression, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
    }

    public static <K, V> AppSearchBatchResult<K, V> checkIsBatchResultSuccess(
            Future<AppSearchBatchResult<K, V>> future) throws Exception {
        AppSearchBatchResult<K, V> result = future.get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchBatchResult not successful: " + result);
        }
        return result;
    }
}
