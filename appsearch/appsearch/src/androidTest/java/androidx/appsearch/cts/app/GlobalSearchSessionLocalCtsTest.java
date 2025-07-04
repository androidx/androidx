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

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.localstorage.stats.QueryStats;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.SimpleTestLogger;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.List;

public class GlobalSearchSessionLocalCtsTest extends GlobalSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync() {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createGlobalSearchSessionAsync(
                new LocalStorage.GlobalSearchContext.Builder(context).build());
    }

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forEmptyFirstPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchemaAsync(
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
        checkIsBatchResultSuccess(db2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        GlobalSearchSession globalSearchSession = LocalStorage.createGlobalSearchSessionAsync(
                new LocalStorage.GlobalSearchContext.Builder(context).setLogger(
                        logger).build()).get();
        assertThat(logger.mQueryStats).isNull();

        // Query for the document using global search session.
        int resultCountPerPage = 1;
        String queryStr = "bodies";
        SearchResults searchResults = globalSearchSession.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(0);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mQueryStats).isNotNull();
        assertThat(logger.mQueryStats.getDatabase()).isNull();
        assertThat(logger.mQueryStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mQueryStats.isFirstPage()).isEqualTo(true);
        assertThat(logger.mQueryStats.getVisibilityScope()).isEqualTo(
                QueryStats.VISIBILITY_SCOPE_GLOBAL);
        assertThat(logger.mQueryStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mQueryStats.getCurrentPageReturnedResultCount()).isEqualTo(0);
        assertThat(logger.mQueryStats.getParentSearchStats().getNativeQueryLength())
                .isEqualTo(queryStr.length());
    }

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forNonEmptyFirstPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchemaAsync(
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
        checkIsBatchResultSuccess(db2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        GlobalSearchSession globalSearchSession = LocalStorage.createGlobalSearchSessionAsync(
                new LocalStorage.GlobalSearchContext.Builder(context).setLogger(
                        logger).build()).get();
        assertThat(logger.mQueryStats).isNull();

        // Query for the document using global search session.
        int resultCountPerPage = 1;
        String queryStr = "body";
        SearchResults searchResults = globalSearchSession.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mQueryStats).isNotNull();
        assertThat(logger.mQueryStats.getDatabase()).isNull();
        assertThat(logger.mQueryStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mQueryStats.isFirstPage()).isEqualTo(true);
        assertThat(logger.mQueryStats.getVisibilityScope()).isEqualTo(
                QueryStats.VISIBILITY_SCOPE_GLOBAL);
        assertThat(logger.mQueryStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mQueryStats.getCurrentPageReturnedResultCount()).isEqualTo(1);
        assertThat(logger.mQueryStats.getParentSearchStats().getNativeQueryLength())
                .isEqualTo(queryStr.length());
    }

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forEmptySecondPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchemaAsync(
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
        checkIsBatchResultSuccess(db2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        GlobalSearchSession globalSearchSession = LocalStorage.createGlobalSearchSessionAsync(
                new LocalStorage.GlobalSearchContext.Builder(context).setLogger(
                        logger).build()).get();
        assertThat(logger.mQueryStats).isNull();

        // Query for the document using global search session.
        int resultCountPerPage = 2;
        String queryStr = "body";
        SearchResults searchResults = globalSearchSession.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page, search stats will only generate in the first page.
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(2);
        assertThat(logger.mQueryStats.getParentSearchStats().getNativeQueryLength())
                .isEqualTo(queryStr.length());

        // Get second(empty) page
        logger.mQueryStats = null;
        page = searchResults.getNextPageAsync().get();
        assertThat(page).isEmpty();

        // Check searchStats won't be set because now empty page token will avoid binder call.
        assertThat(logger.mQueryStats).isNull();
    }

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forNonEmptySecondPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchemaAsync(
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
        checkIsBatchResultSuccess(db2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1, inEmail2).build()));

        GlobalSearchSession globalSearchSession = LocalStorage.createGlobalSearchSessionAsync(
                new LocalStorage.GlobalSearchContext.Builder(context).setLogger(
                        logger).build()).get();
        assertThat(logger.mQueryStats).isNull();

        // Query for the document using global search session.
        int resultCountPerPage = 1;
        String queryStr = "body";
        SearchResults searchResults = globalSearchSession.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(resultCountPerPage)
                .build());

        // Get first page, search stats will only generate in the first page.
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(logger.mQueryStats.getParentSearchStats().getNativeQueryLength())
                .isEqualTo(queryStr.length());

        // Get second page
        logger.mQueryStats = null;
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mQueryStats).isNotNull();
        assertThat(logger.mQueryStats.getDatabase()).isNull();
        assertThat(logger.mQueryStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mQueryStats.isFirstPage()).isEqualTo(false);
        assertThat(logger.mQueryStats.getVisibilityScope()).isEqualTo(
                QueryStats.VISIBILITY_SCOPE_GLOBAL);
        assertThat(logger.mQueryStats.getRequestedPageSize()).isEqualTo(resultCountPerPage);
        assertThat(logger.mQueryStats.getCurrentPageReturnedResultCount()).isEqualTo(1);
    }

    // LocalStorage does not support registering observers for packages besides the local package
    @Override
    @Test
    public void testRemoveObserver_multiplePackages() throws Exception {}
}
