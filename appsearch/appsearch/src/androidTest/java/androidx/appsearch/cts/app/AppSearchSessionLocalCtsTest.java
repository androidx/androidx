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
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.localstorage.stats.SchemaMigrationStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.SimpleTestLogger;
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

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forEmptyFirstPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
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

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forNonEmptyFirstPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
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

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forEmptySecondPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
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

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testLogger_searchStatsLogged_forNonEmptySecondPage() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
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

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testSetSchemaStats_withoutSchemaMigration() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();
        AppSearchSchema appSearchSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        db2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(appSearchSchema).build()).get();

        assertThat(logger.mSetSchemaStats).isNotNull();
        assertThat(logger.mSetSchemaStats.getPackageName()).isEqualTo(context.getPackageName());
        assertThat(logger.mSetSchemaStats.getDatabase()).isEqualTo(DB_NAME_2);
        assertThat(logger.mSetSchemaStats.getSchemaMigrationStats()).isNull();
        assertThat(logger.mSetSchemaStats.getNewTypeCount()).isEqualTo(1);
        assertThat(logger.mSetSchemaStats.getDeletedTypeCount()).isEqualTo(0);
        assertThat(logger.mSetSchemaStats.getIndexIncompatibleTypeChangeCount())
                .isEqualTo(0);
        assertThat(logger.mSetSchemaStats.getBackwardsIncompatibleTypeChangeCount())
                .isEqualTo(0);
        assertThat(logger.mSetSchemaStats.getCompatibleTypeChangeCount()).isEqualTo(0);
    }

    // TODO(b/194207451) This test can be moved to CtsTestBase if customized logger is
    //  supported for platform backend.
    @Test
    public void testSetSchemaStats_withSchemaMigration() throws Exception {
        SimpleTestLogger logger = new SimpleTestLogger();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2)
                        .setLogger(logger).build()).get();
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("To")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "testSchema")
                .setPropertyString("subject", "testPut example")
                .setPropertyString("To", "testTo example")
                .build();
        long documentCreationTime = 12345L;
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(document.getNamespace(), document.getId(),
                        document.getSchemaType())
                        .setPropertyString("subject", "testPut example migrated")
                        .setCreationTimestampMillis(documentCreationTime)
                        .build();
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Downgrade should not be triggered for this test");
            }
        };

        db2.setSchema(new SetSchemaRequest.Builder().addSchemas(
                schema).setForceOverride(true).build()).get();
        db2.put(new PutDocumentsRequest.Builder().addGenericDocuments(doc).build());
        db2.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                .setMigrator("testSchema", migrator)
                .setVersion(2)     // upgrade version
                .build()).get();

        assertThat(logger.mSetSchemaStats).isNotNull();
        assertThat(logger.mSetSchemaStats.getSchemaMigrationStats()).isNotNull();
        SchemaMigrationStats schemaMigrationStats =
                logger.mSetSchemaStats.getSchemaMigrationStats();
        assertThat(schemaMigrationStats.getMigratedDocumentCount()).isEqualTo(1);
        assertThat(schemaMigrationStats.getSavedDocumentCount()).isEqualTo(1);
    }

    // TODO(b/185441119) Following test can be moved to CtsTestBase if we fix the binder
    //  transaction limit in framework.
    @Test
    public void testPutLargeDocument() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2).build()).get();

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
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2).build()).get();

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

    @Test
    public void testCapabilities() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context, DB_NAME_2).build()).get();

        assertThat(db2.getCapabilities().isSubmatchSupported()).isTrue();
    }
}
