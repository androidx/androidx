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
// @exportToFramework:skipFile()
package androidx.appsearch.cts.app;

import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.util.AppSearchEmail;
import androidx.appsearch.app.util.AppSearchTestUtils;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AppSearchSessionLocalCtsTest extends AppSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSession(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName, @NonNull ExecutorService executor) {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, dbName)
                        .setWorkerExecutor(executor).build());
    }

    // TODO(b/194207451) Following test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forEmptyFirstPage() throws Exception {
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        assertThat(logger.mSearchStats).isNull();

        // Query for the document
        int resultCountPerPage = 4;
        String queryStr = "bodies";
        SearchResults searchResults = db2.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPage().get();
        assertThat(page).hasSize(0);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mSearchStats.getDatabase()).isEqualTo(DB_NAME_2);
        assertThat(logger.mSearchStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mSearchStats.getQueryLength()).isEqualTo(queryStr.length());
        assertThat(logger.mSearchStats.isFirstPage()).isEqualTo(true);
        assertThat(logger.mSearchStats.getVisibilityScope()).isEqualTo(
                SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(logger.mSearchStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mSearchStats.getCurrentPageReturnedResultCount()).isEqualTo(
                0);
    }

    @Test
    public void testLogger_searchStatsLogged_forNonEmptyFirstPage() throws Exception {
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        assertThat(logger.mSearchStats).isNull();

        // Query for the document
        int resultCountPerPage = 4;
        String queryStr = "body";
        SearchResults searchResults = db2.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPage().get();
        assertThat(page).hasSize(2);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mSearchStats.getDatabase()).isEqualTo(DB_NAME_2);
        assertThat(logger.mSearchStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mSearchStats.getQueryLength()).isEqualTo(queryStr.length());
        assertThat(logger.mSearchStats.isFirstPage()).isEqualTo(true);
        assertThat(logger.mSearchStats.getVisibilityScope()).isEqualTo(
                SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(logger.mSearchStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mSearchStats.getCurrentPageReturnedResultCount()).isEqualTo(
                2);
    }

    @Test
    public void testLogger_searchStatsLogged_forEmptySecondPage() throws Exception {
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        // Query for the document
        int resultCountPerPage = 2;
        String queryStr = "body";
        SearchResults searchResults = db2.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                // include all documents in the 1st page
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPage().get();
        assertThat(page).hasSize(2);

        // Get second(empty) page
        logger.mSearchStats = null;
        page = searchResults.getNextPage().get();
        assertThat(page).hasSize(0);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mSearchStats.getDatabase()).isEqualTo(DB_NAME_2);
        assertThat(logger.mSearchStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // Query length is 0 for getNextPage in IcingLib
        assertThat(logger.mSearchStats.getQueryLength()).isEqualTo(0);
        assertThat(logger.mSearchStats.isFirstPage()).isEqualTo(false);
        assertThat(logger.mSearchStats.getVisibilityScope()).isEqualTo(
                SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(logger.mSearchStats.getRequestedPageSize()).isEqualTo(0);
        assertThat(logger.mSearchStats.getCurrentPageReturnedResultCount()).isEqualTo(0);
    }

    @Test
    public void testLogger_searchStatsLogged_forNonEmptySecondPage() throws Exception {
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        // Query for the document
        int resultCountPerPage = 1;
        String queryStr = "body";
        SearchResults searchResults = db2.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                // only include one result in a page
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPage().get();
        assertThat(page).hasSize(1);

        // Get second page
        logger.mSearchStats = null;
        page = searchResults.getNextPage().get();
        assertThat(page).hasSize(1);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mSearchStats.getDatabase()).isEqualTo(DB_NAME_2);
        assertThat(logger.mSearchStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // Query length is 0 for getNextPage in IcingLib
        assertThat(logger.mSearchStats.getQueryLength()).isEqualTo(0);
        assertThat(logger.mSearchStats.isFirstPage()).isEqualTo(false);
        assertThat(logger.mSearchStats.getVisibilityScope()).isEqualTo(
                SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(logger.mSearchStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mSearchStats.getCurrentPageReturnedResultCount()).isEqualTo(1);
    }

    // TODO(b/185441119) Following test can be moved to CtsTestBase if we fix the binder
    //  transaction limit in framework.
    @Test
    public void testPutLargeDocument() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        char[] chars = new char[16_000_000];
        Arrays.fill(chars, ' ');
        String body = String.valueOf(chars) + "the end.";

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody(body)
                .build();
        AppSearchBatchResult<String, Void> result = db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();
        assertThat(result.isSuccess()).isTrue();

        SearchResults searchResults = db2.search("end", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);
    }

    // TODO(b/185441119) Following test can be moved to CtsTestBase if we fix the binder
    //  transaction limit in framework.
    @Test
    public void testPutLargeDocument_exceedLimit() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchTestUtils.TestLogger logger = new AppSearchTestUtils.TestLogger();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Create a String property that make the document exceed the total size limit.
        char[] chars = new char[17_000_000];
        String body = new StringBuilder().append(chars).toString();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody(body)
                .build();
        AppSearchBatchResult<String, Void> result = db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();
        assertThat(result.getFailures()).containsKey("id1");
        assertThat(result.getFailures().get("id1").getErrorMessage())
                .contains("was too large to write. Max is 16777215");
    }
}
