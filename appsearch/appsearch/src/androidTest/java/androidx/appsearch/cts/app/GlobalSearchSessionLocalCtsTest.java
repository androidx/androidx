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
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.util.AppSearchEmail;
import androidx.appsearch.app.util.AppSearchTestUtils;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.List;

public class GlobalSearchSessionLocalCtsTest extends GlobalSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSession(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<GlobalSearchSession> createGlobalSearchSession() {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createGlobalSearchSession(
                new LocalStorage.GlobalSearchContext.Builder(context).build());
    }

    // TODO(b/194207451) Following tests can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged() throws Exception {
        AppSearchTestUtils.TestLogger logger =
                new AppSearchTestUtils.TestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();

        // Schema registration
        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(db2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));


        GlobalSearchSession globalSearchSession = LocalStorage.createGlobalSearchSession(
                new LocalStorage.GlobalSearchContext.Builder(context).setLogger(
                        logger).build()).get();
        assertThat(logger.mSearchStats).isNull();

        // Query for the document using global search session.
        String queryStr = "body";
        SearchResults searchResults = globalSearchSession.search(queryStr, new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);

        // Check searchStats has been set. We won't check all the fields here.
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mSearchStats.getDatabase()).isNull();
        assertThat(logger.mSearchStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(logger.mSearchStats.getQueryLength()).isEqualTo(queryStr.length());
    }
}
