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

import static androidx.appsearch.app.AppSearchManager.PutDocumentsRequest;
import static androidx.appsearch.app.AppSearchManager.SetSchemaRequest;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.customer.EmailDataClass;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class AppSearchManagerTest {
    private final AppSearchManager mAppSearch1 = new AppSearchManager("testInstance1",
            ApplicationProvider.getApplicationContext());
    private final AppSearchManager mAppSearch2 = new AppSearchManager("testInstance2",
            ApplicationProvider.getApplicationContext());

    @After
    public void tearDown() throws Exception {
        Future<AppSearchResult<Void>> future = mAppSearch1.deleteAll();
        future.get();
        future = mAppSearch2.deleteAll();
        future.get();
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
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addSchema(emailSchema).build()));
    }

    @Test
    public void testSetSchema_DataClass() throws Exception {
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("uri1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        Future<AppSearchBatchResult<String, Void>> future = mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email).build());
        checkIsSuccess(future);
        AppSearchBatchResult<String, Void> result = future.get();
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_DataClass() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass email = new EmailDataClass();
        email.uri = "uri1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        Future<AppSearchBatchResult<String, Void>> future = mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(email).build());
        checkIsSuccess(future);
        AppSearchBatchResult<String, Void> result = future.get();
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testGetDocuments() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mAppSearch1, "uri1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult =
                mAppSearch2.getDocuments(Arrays.asList("uri1")).get();

        assertThat(failResult.isSuccess()).isFalse();
        assertThat(failResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testGetDocuments_DataClass() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass inEmail = new EmailDataClass();
        inEmail.uri = "uri1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mAppSearch1, "uri1");
        assertThat(outDocuments).hasSize(1);
        EmailDataClass outEmail = outDocuments.get(0).toDataClass(EmailDataClass.class);
        assertThat(inEmail.uri).isEqualTo(outEmail.uri);
        assertThat(inEmail.subject).isEqualTo(outEmail.subject);
        assertThat(inEmail.body).isEqualTo(outEmail.body);
    }

    @Test
    public void testQuery() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail).build()));

        // Query for the document
        List<GenericDocument> results = doQuery(mAppSearch1, "body");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);

        // Multi-term query
        results = doQuery(mAppSearch1, "body email");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_TypeFilter() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchema(AppSearchEmail.SCHEMA)
                        .addSchema(new AppSearchSchema.Builder("Test").build())
                        .build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument inDoc =
                new GenericDocument.Builder<>("uri2", "Test").setProperty("foo", "body").build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail, inDoc).build()));

        // Query for the documents
        List<GenericDocument> results = doQuery(mAppSearch1, "body");
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(inEmail, inDoc);

        // Query only for Document
        results = doQuery(mAppSearch1, "body", "Test");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inDoc);
    }

    @Test
    public void testQuery_TwoInstances() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsSuccess(mAppSearch2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail2).build()));

        // Query for instance 1.
        List<GenericDocument> results = doQuery(mAppSearch1, "body");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inEmail1);

        // Query for instance 2.
        results = doQuery(mAppSearch2, "body");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inEmail2);
    }

    @Test
    public void testDelete() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
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
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mAppSearch1, "uri1")).hasSize(1);
        assertThat(doGet(mAppSearch1, "uri2")).hasSize(1);

        // Delete the document
        checkIsSuccess(mAppSearch1.delete(ImmutableList.of("uri1")));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch1.getDocuments(ImmutableList.of("uri1", "uri2")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);

        // Test if we delete a nonexistent URI.
        AppSearchBatchResult<String, Void> deleteResult =
                mAppSearch1.delete(ImmutableList.of("uri1")).get();
        assertThat(deleteResult.getFailures()).containsExactly("uri1",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null));
    }

    @Test
    public void testDelete_TwoInstances() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mAppSearch1, "uri1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult =
                mAppSearch2.delete(ImmutableList.of("uri1")).get();
        assertThat(deleteResult.getFailures()).containsExactly("uri1",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null));
        assertThat(doGet(mAppSearch1, "uri1")).hasSize(1);

        // Delete the document
        checkIsSuccess(mAppSearch1.delete(ImmutableList.of("uri1")));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch1.getDocuments(ImmutableList.of("uri1")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent URI.
        deleteResult = mAppSearch1.delete(ImmutableList.of("uri1")).get();
        assertThat(deleteResult.getFailures()).containsExactly("uri1",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null));
    }

    @Test
    public void testDeleteByTypes() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(
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
        GenericDocument document1 =
                new GenericDocument.Builder<>("uri3", "schemaType")
                        .setProperty("foo", "bar").build();
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mAppSearch1, "uri1", "uri2", "uri3")).hasSize(3);

        // Delete the email type
        checkIsSuccess(mAppSearch1.deleteByTypes(ImmutableList.of(AppSearchEmail.SCHEMA_TYPE)));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch1.getDocuments(ImmutableList.of("uri1", "uri2", "uri3")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    @Test
    public void testDeleteByTypes_TwoInstances() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsSuccess(mAppSearch2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsSuccess(mAppSearch1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsSuccess(mAppSearch2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mAppSearch1, "uri1")).hasSize(1);
        assertThat(doGet(mAppSearch2, "uri2")).hasSize(1);

        // Delete the email type in instance 1
        checkIsSuccess(mAppSearch1.deleteByTypes(ImmutableList.of(AppSearchEmail.SCHEMA_TYPE)));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch1.getDocuments(ImmutableList.of("uri1")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mAppSearch2.getDocuments(ImmutableList.of("uri2")).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    private List<GenericDocument> doGet(AppSearchManager instance, String... uris)
            throws Exception {
        Future<AppSearchBatchResult<String, GenericDocument>> future =
                instance.getDocuments(Arrays.asList(uris));
        checkIsSuccess(future);
        AppSearchBatchResult<String, GenericDocument> result = future.get();
        assertThat(result.getSuccesses()).hasSize(uris.length);
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(uris.length);
        for (String uri : uris) {
            list.add(result.getSuccesses().get(uri));
        }
        return list;
    }

    private List<GenericDocument> doQuery(AppSearchManager instance, String queryExpression,
            String... schemaTypes)
            throws Exception {
        AppSearchResult<SearchResults> result = instance.query(
                queryExpression,
                SearchSpec.newBuilder()
                        .setSchemaTypes(schemaTypes)
                        .setTermMatchType(SearchSpec.TERM_MATCH_TYPE_EXACT_ONLY)
                        .build()).get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError(
                    "AppSearch query not successful: " + result.getErrorMessage());
        }
        SearchResults searchResults = result.getResultValue();
        List<GenericDocument> documents = new ArrayList<>();
        while (searchResults.hasNext()) {
            documents.add(searchResults.next().getDocument());
        }
        return documents;
    }

    private <T> void checkIsSuccess(Future<T> future) throws Exception {
        T futureGet = future.get();
        if (futureGet instanceof AppSearchBatchResult) {
            AppSearchBatchResult<?, ?> result = (AppSearchBatchResult<?, ?>) futureGet;
            if (!result.isSuccess()) {
                throw new AssertionFailedError(
                        "AppSearchBatchResult not successful: " + result.getFailures());
            }
        } else if (futureGet instanceof AppSearchResult) {
            AppSearchResult<?> result = (AppSearchResult<?>) futureGet;
            if (!result.isSuccess()) {
                throw new AssertionFailedError(
                        "AppSearchBatchResult not successful: " + result);
            }
        }
    }
}
