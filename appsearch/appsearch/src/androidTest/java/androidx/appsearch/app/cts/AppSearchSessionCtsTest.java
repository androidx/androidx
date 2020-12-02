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

package androidx.appsearch.app.cts;

import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.app.util.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchEmail;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.cts.customer.EmailDataClass;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSearchSessionCtsTest {
    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mDb1 = checkIsResultSuccess(LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context)
                        .setDatabaseName("testDb1").build()));
        mDb2 = checkIsResultSuccess(LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context)
                        .setDatabaseName("testDb2").build()));

        // Remove all documents from any instances that may have been created in the tests.
        checkIsResultSuccess(
                mDb1.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()));
        checkIsResultSuccess(
                mDb2.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()));
    }

    @Test
    public void testSetSchema() throws Exception {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.PropertyConfig.Builder("body")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        checkIsResultSuccess(
                mDb1.setSchema(new SetSchemaRequest.Builder().addSchema(emailSchema).build()));
    }

    @Test
    public void testSetSchema_dataClass() throws Exception {
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));
    }

    @Test
    public void testGetSchema() throws Exception {
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.PropertyConfig.Builder("body")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema emailSchema2 = new AppSearchSchema.Builder("Email2")
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Different
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.PropertyConfig.Builder("body")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Different
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SetSchemaRequest request1 = new SetSchemaRequest.Builder()
                .addSchema(emailSchema1).addDataClass(EmailDataClass.class).build();
        SetSchemaRequest request2 = new SetSchemaRequest.Builder()
                .addSchema(emailSchema2).addDataClass(EmailDataClass.class).build();

        checkIsResultSuccess(mDb1.setSchema(request1));
        checkIsResultSuccess(mDb2.setSchema(request2));

        Set<AppSearchSchema> actual1 = checkIsResultSuccess(mDb1.getSchema());
        Set<AppSearchSchema> actual2 = checkIsResultSuccess(mDb2.getSchema());

        assertThat(actual1).isEqualTo(request1.getSchemas());
        assertThat(actual2).isEqualTo(request2.getSchemas());
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("uri1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_dataClass() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass email = new EmailDataClass();
        email.uri = "uri1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(email).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testUpdateSchema() throws Exception {
        // Schema registration
        AppSearchSchema oldEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema newEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("body")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("price")
                        .setDataType(PropertyConfig.DATA_TYPE_INT64)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_NONE)
                        .build())
                .build();
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder().addSchema(oldEmailSchema).build()));

        // Try to index a gift. This should fail as it's not in the schema.
        GenericDocument gift =
                new GenericDocument.Builder<>("gift1", "Gift").setPropertyLong("price", 5).build();
        AppSearchBatchResult<String, Void> result =
                mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(gift).build()).get();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailures().get("gift1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Update the schema to include the gift and update email with a new field
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder()
                                .addSchema(newEmailSchema, giftSchema).build()));

        // Try to index the document again, which should now work
        checkIsBatchResultSuccess(
                mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(gift).build()));

        // Indexing an email with a body should also work
        AppSearchEmail email = new AppSearchEmail.Builder("email1")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email).build()));
    }

    @Test
    public void testRemoveSchema() throws Exception {
        // Schema registration
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder().addSchema(emailSchema).build()));

        // Index an email and check it present.
        AppSearchEmail email = new AppSearchEmail.Builder("email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);

        // Try to remove the email schema. This should fail as it's an incompatible change.
        AppSearchResult<Void> failResult1 =
                mDb1.setSchema(new SetSchemaRequest.Builder().build()).get();
        assertThat(failResult1.isSuccess()).isFalse();
        assertThat(failResult1.getErrorMessage()).contains("Schema is incompatible");
        assertThat(failResult1.getErrorMessage())
                .contains("Deleted types: [testDb1/builtin:Email]");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder().setForceOverride(true).build()));

        // Make sure the indexed email is gone.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder()
                        .setNamespace(GenericDocument.DEFAULT_NAMESPACE)
                        .addUri("email1")
                        .build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email2 = new AppSearchEmail.Builder("email2")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email2).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email2").getErrorMessage())
                .isEqualTo("Schema type config 'testDb1/builtin:Email' not found");
    }

    @Test
    public void testRemoveSchema_twoDatabases() throws Exception {
        // Schema registration in mDb1 and mDb2
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder().addSchema(emailSchema).build()));
        checkIsResultSuccess(
                mDb2.setSchema(
                        new SetSchemaRequest.Builder().addSchema(emailSchema).build()));

        // Index an email and check it present in database1.
        AppSearchEmail email1 = new AppSearchEmail.Builder("email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email1);

        // Index an email and check it present in database2.
        AppSearchEmail email2 = new AppSearchEmail.Builder("email2")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb2.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));
        outDocuments = doGet(mDb2, GenericDocument.DEFAULT_NAMESPACE, "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Try to remove the email schema in database1. This should fail as it's an incompatible
        // change.
        AppSearchResult<Void> failResult1 =
                mDb1.setSchema(new SetSchemaRequest.Builder().build()).get();
        assertThat(failResult1.isSuccess()).isFalse();
        assertThat(failResult1.getErrorMessage()).contains("Schema is incompatible");
        assertThat(failResult1.getErrorMessage())
                .contains("Deleted types: [testDb1/builtin:Email]");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        checkIsResultSuccess(
                mDb1.setSchema(
                        new SetSchemaRequest.Builder().setForceOverride(true).build()));

        // Make sure the indexed email is gone in database 1.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().setNamespace(GenericDocument.DEFAULT_NAMESPACE)
                        .addUri("email1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email3 = new AppSearchEmail.Builder("email3")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email3).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email3").getErrorMessage())
                .isEqualTo("Schema type config 'testDb1/builtin:Email' not found");

        // Make sure email in database 2 still present.
        outDocuments = doGet(mDb2, GenericDocument.DEFAULT_NAMESPACE, "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Make sure email could still be indexed in database 2.
        checkIsBatchResultSuccess(
                mDb2.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));
    }

    @Test
    public void testGetDocuments() throws Exception {
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

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult = mDb2.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(failResult.isSuccess()).isFalse();
        assertThat(failResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testGetDocuments_dataClass() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass inEmail = new EmailDataClass();
        inEmail.uri = "uri1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1");
        assertThat(outDocuments).hasSize(1);
        EmailDataClass outEmail = outDocuments.get(0).toDataClass(EmailDataClass.class);
        assertThat(inEmail.uri).isEqualTo(outEmail.uri);
        assertThat(inEmail.subject).isEqualTo(outEmail.subject);
        assertThat(inEmail.body).isEqualTo(outEmail.body);
    }

    @Test
    public void testQuery() throws Exception {
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
        SearchResults searchResults = mDb1.query("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);

        // Multi-term query
        searchResults = mDb1.query("body email", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_getNextPage() throws Exception {
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
        SearchResults searchResults = mDb1.query("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultCountPerPage(7)
                        .build());
        List<GenericDocument> documents = new ArrayList<>();

        int pageNumber = 0;
        List<SearchResult> results;

        // keep loading next page until it's empty.
        do {
            results = checkIsResultSuccess(searchResults.getNextPage());
            ++pageNumber;
            for (SearchResult result : results) {
                documents.add(result.getDocument());
            }
        } while (results.size() > 0);

        // check all document presents
        assertThat(documents).containsExactlyElementsIn(emailSet);
        assertThat(pageNumber).isEqualTo(6); // 5 (upper(31/7)) + 1 (final empty page)
    }

    @Test
    public void testQuery_typeFilter() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("foo")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchema(AppSearchEmail.SCHEMA)
                        .addSchema(genericSchema)
                        .build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument inDoc = new GenericDocument.Builder<>("uri2", "Generic")
                .setPropertyString("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail, inDoc).build()));

        // Query for the documents
        SearchResults searchResults = mDb1.query("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(inEmail, inDoc);

        // Query only for Document
        searchResults = mDb1.query("body", new SearchSpec.Builder()
                .addSchemaType("Generic", "Generic") // duplicate type in filter won't matter.
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inDoc);
    }

    @Test
    public void testQuery_namespaceFilter() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchema(AppSearchEmail.SCHEMA)
                        .build()));

        // Index two documents
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("expectedNamespace")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail unexpectedEmail =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("unexpectedNamespace")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder()
                        .addGenericDocument(expectedEmail, unexpectedEmail).build()));

        // Query for all namespaces
        SearchResults searchResults = mDb1.query("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(expectedEmail, unexpectedEmail);

        // Query only for expectedNamespace
        searchResults = mDb1.query("body",
                new SearchSpec.Builder()
                        .addNamespace("expectedNamespace")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(expectedEmail);
    }

    @Test
    public void testQuery_twoInstances() throws Exception {
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

        // Query for instance 1.
        SearchResults searchResults = mDb1.query("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail1);

        // Query for instance 2.
        searchResults = mDb2.query("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail2);
    }

    @Test
    public void testSnippet() throws Exception {
        // Schema registration
        // TODO(tytytyww) add property for long and  double.
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("subject")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(genericSchema).build()));

        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("uri", "Generic")
                        .setNamespace("document")
                        .setPropertyString("subject", "A commonly used fake word is foo. "
                                        + "Another nonsense word that’s used a lot is bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.query("foo",
                new SearchSpec.Builder()
                        .addSchemaType("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setMaxSnippetSize(10)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = checkIsResultSuccess(searchResults.getNextPage());
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatches();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo("A commonly used fake word is foo. "
                + "Another nonsense word that’s used a lot is bar");
        assertThat(matchInfo.getExactMatchPosition()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29,  /*upper=*/32));
        assertThat(matchInfo.getExactMatch()).isEqualTo("foo");
        assertThat(matchInfo.getSnippetPosition()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/26,  /*upper=*/33));
        assertThat(matchInfo.getSnippet()).isEqualTo("is foo.");
    }

    @Test
    public void testRemove() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeByUri(
                new RemoveByUriRequest.Builder().addUri("uri1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1", "uri2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);

        // Test if we delete a nonexistent URI.
        AppSearchBatchResult<String, Void> deleteResult = mDb1.removeByUri(
                new RemoveByUriRequest.Builder().addUri("uri1").build()).get();

        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByQuery() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("bar")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the email 1 by query "foo"
        checkIsResultSuccess(mDb1.removeByQuery("foo",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()));
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1", "uri2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);

        // Delete the email 2 by query "bar"
        checkIsResultSuccess(mDb1.removeByQuery("bar",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()));
        getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_twoInstances() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult = mDb2.removeByUri(
                new RemoveByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeByUri(
                new RemoveByUriRequest.Builder().addUri("uri1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent URI.
        deleteResult = mDb1.removeByUri(
                new RemoveByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByTypes() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic").build();
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).addSchema(
                        genericSchema).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("uri3", "Generic").build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1", "uri2",
                "uri3")).hasSize(3);

        // Delete the email type
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addSchemaType(AppSearchEmail.SCHEMA_TYPE)
                        .build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1", "uri2", "uri3").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByTypes_twoInstances() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(mDb2, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the email type in instance 1
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addSchemaType(AppSearchEmail.SCHEMA_TYPE)
                        .build()));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByUri(
                new GetByUriRequest.Builder().addUri("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveByNamespace() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("foo")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        checkIsResultSuccess(mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).addSchema(
                        genericSchema).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("email")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setNamespace("email")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("uri3", "Generic")
                        .setNamespace("document")
                        .setPropertyString("foo", "bar").build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "uri1", "uri2")).hasSize(2);
        assertThat(doGet(mDb1, /*namespace=*/"document", "uri3")).hasSize(1);

        // Delete the email namespace
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addNamespace("email")
                        .build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().setNamespace("email")
                        .addUri("uri1", "uri2").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().setNamespace("document")
                        .addUri("uri3").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByNamespaces_twoInstances() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("email")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setNamespace("email")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "uri1")).hasSize(1);
        assertThat(doGet(mDb2, /*namespace=*/"email", "uri2")).hasSize(1);

        // Delete the email namespace in instance 1
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addNamespace("email")
                        .build()));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().setNamespace("email")
                        .addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByUri(
                new GetByUriRequest.Builder().setNamespace("email")
                        .addUri("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_twoInstances() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(mDb2, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the all document in instance 1
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build()));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByUri(
                new GetByUriRequest.Builder().addUri("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_termMatchType() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email3 =
                new AppSearchEmail.Builder("uri3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 3")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email4 =
                new AppSearchEmail.Builder("uri4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 4")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2).build()));
        checkIsBatchResultSuccess(mDb2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email3, email4).build()));

        // Check the presence of the documents
        SearchResults searchResults = mDb1.query("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        searchResults = mDb2.query("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Delete the all document in instance 1 with TERM_MATCH_PREFIX
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build()));
        searchResults = mDb1.query("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Delete the all document in instance 2 with TERM_MATCH_EXACT_ONLY
        checkIsResultSuccess(mDb2.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        searchResults = mDb2.query("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testRemoveAllAfterEmpty() throws Exception {
        // Schema registration
        checkIsResultSuccess(mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setNamespace("namespace")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "uri1")).hasSize(1);

        // Remove the document
        checkIsBatchResultSuccess(
                mDb1.removeByUri(new RemoveByUriRequest.Builder()
                        .setNamespace("namespace").addUri("uri1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Delete the all documents
        checkIsResultSuccess(mDb1.removeByQuery("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build()));

        // Make sure it's still gone
        getResult = mDb1.getByUri(
                new GetByUriRequest.Builder().addUri("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }
}
