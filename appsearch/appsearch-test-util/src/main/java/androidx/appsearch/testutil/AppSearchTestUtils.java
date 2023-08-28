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

package androidx.appsearch.testutil;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class with helper functions for testing for AppSearch.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchTestUtils {
    /** Checks batch result. */
    @NonNull
    public static <K, V> AppSearchBatchResult<K, V> checkIsBatchResultSuccess(
            @NonNull Future<AppSearchBatchResult<K, V>> future) throws Exception {
        AppSearchBatchResult<K, V> result = future.get();
        assertWithMessage("AppSearchBatchResult not successful: " + result)
                .that(result.isSuccess()).isTrue();
        return result;
    }

    /** Gets documents from ids. */
    @NonNull
    public static List<GenericDocument> doGet(
            @NonNull AppSearchSession session, @NonNull String namespace, @NonNull String... ids)
            throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                session.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder(namespace).addIds(ids).build()));
        assertThat(result.getSuccesses()).hasSize(ids.length);
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(ids.length);
        for (String id : ids) {
            list.add(result.getSuccesses().get(id));
        }
        return list;
    }

    /** Gets documents from {@link GetByDocumentIdRequest}. */
    @NonNull
    public static List<GenericDocument> doGet(
            @NonNull AppSearchSession session, @NonNull GetByDocumentIdRequest request)
            throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                session.getByDocumentIdAsync(request));
        Set<String> ids = request.getIds();
        assertThat(result.getSuccesses()).hasSize(ids.size());
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            list.add(result.getSuccesses().get(id));
        }
        return list;
    }

    /** Extracts documents from {@link SearchResults}. */
    @NonNull
    public static List<GenericDocument> convertSearchResultsToDocuments(
            @NonNull SearchResults searchResults)
            throws Exception {
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        List<GenericDocument> documents = new ArrayList<>(results.size());
        for (SearchResult result : results) {
            documents.add(result.getGenericDocument());
        }
        return documents;
    }

    /** Extracts all {@link SearchResult} from {@link SearchResults}. */
    @NonNull
    public static List<SearchResult> retrieveAllSearchResults(@NonNull SearchResults searchResults)
            throws Exception {
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        List<SearchResult> results = new ArrayList<>();
        while (!page.isEmpty()) {
            results.addAll(page);
            page = searchResults.getNextPageAsync().get();
        }
        return results;
    }

    /**
     * Creates a mock {@link VisibilityChecker} where schema is searchable if prefixedSchema is
     * one of the provided set of visiblePrefixedSchemas and caller does not have system access.
     *
     * @param visiblePrefixedSchemas Schema types that are accessible to any caller.
     * @return Mocked {@link VisibilityChecker} instance.
     */
    @NonNull
    public static VisibilityChecker createMockVisibilityChecker(
            @NonNull Set<String> visiblePrefixedSchemas) {
        return new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(
                    @NonNull CallerAccess callerAccess,
                    @NonNull String packageName,
                    @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return visiblePrefixedSchemas.contains(prefixedSchema);
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String s) {
                return false;
            }
        };
    }

    /**
     * Creates a mock {@link VisibilityChecker}, where it can be configured if schema is searchable
     * by caller and caller does not have system access.
     *
     * @param isSchemaSearchableByCaller Schema visibility for caller.
     * @return Mocked {@link VisibilityChecker} instance.
     */
    @NonNull
    public static VisibilityChecker createMockVisibilityChecker(
            boolean isSchemaSearchableByCaller) {
        return new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(
                    @NonNull CallerAccess callerAccess,
                    @NonNull String packageName,
                    @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return isSchemaSearchableByCaller;
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String s) {
                return false;
            }
        };
    }

    /** Generate an array contains random bytes for the given length.     */
    @NonNull
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    /** Calculate the sha-256 digest for the given data.     */
    @NonNull
    public static byte[] calculateDigest(@NonNull byte[] data)
            throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(data);
        return messageDigest.digest();
    }

    private AppSearchTestUtils() {
    }
}
