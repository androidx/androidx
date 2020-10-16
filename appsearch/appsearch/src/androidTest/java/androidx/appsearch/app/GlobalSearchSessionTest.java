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

import static androidx.appsearch.app.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.AppSearchTestUtils.checkIsResultSuccess;
import static androidx.appsearch.app.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.localbackend.LocalBackend;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalSearchSessionTest {
    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    private GlobalSearchSession mGlobalAppSearchManager;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mDb1 = checkIsResultSuccess(LocalBackend.createSearchSession(
                new LocalBackend.SearchContext.Builder(context)
                        .setDatabaseName("testDb1").build()));
        mDb2 = checkIsResultSuccess(LocalBackend.createSearchSession(
                new LocalBackend.SearchContext.Builder(context)
                        .setDatabaseName("testDb2").build()));

        mGlobalAppSearchManager = checkIsResultSuccess(LocalBackend.createGlobalSearchSession(
                new LocalBackend.GlobalSearchContext.Builder(context).build()));

        // Remove all documents from any instances that may have been created in the tests.
        checkIsResultSuccess(
                mDb1.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()));
        checkIsResultSuccess(
                mDb2.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()));
    }

    @Test
    public void testGlobalQuery_OneInstance() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail).build()));

        // Query for the document
        SearchResultsHack searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail);

        // Multi-term query
        searchResults = mGlobalAppSearchManager.globalQuery("body email", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testGlobalQuery_TwoInstances() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail2).build()));

        // Query across all instances
        SearchResultsHack searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1, inEmail2);
    }

    @Test
    public void testGlobalQuery_GetNextPage() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));
        Set<AppSearchEmail> emailSet = new HashSet<>();
        PutDocumentsRequest.Builder putDocumentsRequestBuilder = new PutDocumentsRequest.Builder();

        // Index 31 documents
        for (int i = 0; i < 31; i++) {
            AppSearchEmail inEmail =
                    new AppSearchEmail.Builder("uri" + i)
                            .setFrom("from@example.com")
                            .setTo("to1@example.com", "to2@example.com")
                            .setSubject("testPut example")
                            .setBody("This is the body of the testPut email")
                            .build();
            emailSet.add(inEmail);
            putDocumentsRequestBuilder.addGenericDocument(inEmail);
        }
        checkIsBatchResultSuccess(mDb1.putDocuments(putDocumentsRequestBuilder.build()));

        // Set number of results per page is 7.
        SearchResultsHack searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setNumPerPage(7)
                        .build());
        List<GenericDocument> documents = new ArrayList<>();

        int pageNumber = 0;
        List<SearchResults.Result> results;

        // keep loading next page until it's empty.
        do {
            results = checkIsResultSuccess(searchResults.getNextPage());
            ++pageNumber;
            for (SearchResults.Result result : results) {
                documents.add(result.getDocument());
            }
        } while (results.size() > 0);

        // check all document presents
        assertThat(documents).containsExactlyElementsIn(emailSet);
        assertThat(pageNumber).isEqualTo(6); // 5 (upper(31/7)) + 1 (final empty page)
    }

    @Test
    public void testGlobalQuery_AcrossTypes() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("foo")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();

        // db1 has both "Generic" and "builtin:Email"
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(genericSchema).addSchema(AppSearchEmail.SCHEMA).build()));

        // db2 only has "builtin:Email"
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a generic document into db1
        GenericDocument genericDocument =
                new GenericDocument.Builder<>("uri2", "Generic").setProperty("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder()
                        .addGenericDocument(genericDocument).build()));

        AppSearchEmail email =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("namespace")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        // Put the email in both databases
        checkIsBatchResultSuccess((mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email).build())));
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email).build()));

        // Query for all documents across types
        SearchResultsHack searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(genericDocument, email, email);

        // Query only for email documents
        searchResults = mGlobalAppSearchManager.globalQuery("body", new SearchSpec.Builder()
                .setSchemaTypes(AppSearchEmail.SCHEMA_TYPE)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(email, email);
    }

    @Test
    public void testGlobalQuery_NamespaceFilter() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index two documents
        AppSearchEmail document1 =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("namespace1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder()
                        .addGenericDocument(document1).build()));

        AppSearchEmail document2 =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("namespace2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(document2).build()));

        // Query for all namespaces
        SearchResultsHack searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(document1, document2);

        // Query only for "namespace1"
        searchResults = mGlobalAppSearchManager.globalQuery("body",
                new SearchSpec.Builder()
                        .setNamespaces("namespace1")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(document1);
    }
}
