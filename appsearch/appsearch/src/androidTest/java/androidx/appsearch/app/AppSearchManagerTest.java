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

import static androidx.appsearch.app.AppSearchManager.GetDocumentsRequest;
import static androidx.appsearch.app.AppSearchManager.PutDocumentsRequest;
import static androidx.appsearch.app.AppSearchManager.RemoveDocumentsRequest;
import static androidx.appsearch.app.AppSearchManager.SetSchemaRequest;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.customer.EmailDataClass;
import androidx.test.core.app.ApplicationProvider;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class AppSearchManagerTest {
    @Before
    public void setUp() throws Exception {
        // Remove all documents from any instances that may have been created in the tests.
        Future<AppSearchResult<AppSearchManager>> appSearchManagerFuture =
                AppSearchManager.getInstance(/*instanceName=*/ "",
                        ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(appSearchManagerFuture);
        checkIsResultSuccess(appSearchManager.resetAllInstances());
    }

    @Test
    public void testGetInstance() throws Exception {
        checkIsResultSuccess(AppSearchManager.getInstance("testInstance1",
                ApplicationProvider.getApplicationContext()));
        checkIsResultSuccess(AppSearchManager.getInstance("testInstance2",
                ApplicationProvider.getApplicationContext()));
    }

    @Test
    public void testSetSchema() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

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
        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addSchema(emailSchema).build()));
    }

    @Test
    public void testSetSchema_DataClass() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));
    }

    @Test
    public void testPutDocuments() throws Exception {
        AppSearchManager appSearchManager = checkIsResultSuccess(
                AppSearchManager.getInstance(
                        "instance1",
                        ApplicationProvider.getApplicationContext()));

        // Schema registration
        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("uri1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        Future<AppSearchBatchResult<String, Void>> future = appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email).build());
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(future);
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_DataClass() throws Exception {
        Future<AppSearchResult<AppSearchManager>> appSearchManagerFuture =
                AppSearchManager.getInstance("instance1",
                        ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(appSearchManagerFuture);

        // Schema registration
        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass email = new EmailDataClass();
        email.uri = "uri1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        Future<AppSearchBatchResult<String, Void>> future = appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(email).build());
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(future);
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testUpdateSchema() throws Exception {
        Future<AppSearchResult<AppSearchManager>> appSearchManagerFuture =
                AppSearchManager.getInstance("instance1",
                        ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(appSearchManagerFuture);

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
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        checkIsResultSuccess(
                appSearchManager.setSchema(
                        new SetSchemaRequest.Builder().addSchema(oldEmailSchema).build()));

        // Try to index a gift. This should fail as it's not in the schema.
        GenericDocument gift =
                new GenericDocument.Builder<>("gift1", "Gift").setProperty("price", 5).build();
        AppSearchBatchResult<String, Void> result =
                appSearchManager.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(gift).build()).get();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailures().get("gift1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Update the schema to include the gift and update email with a new field
        checkIsResultSuccess(
                appSearchManager.setSchema(
                        new SetSchemaRequest.Builder()
                                .addSchema(newEmailSchema, giftSchema).build()));

        // Try to index the document again, which should now work
        checkIsBatchResultSuccess(
                appSearchManager.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(gift).build()));

        // Indexing an email with a body should also work
        AppSearchEmail email = new AppSearchEmail.Builder("email1")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();
        checkIsBatchResultSuccess(
                appSearchManager.putDocuments(
                        new PutDocumentsRequest.Builder().addGenericDocument(email).build()));
    }

    @Test
    public void testGetDocuments() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(appSearchManager1,
                GenericDocument.DEFAULT_NAMESPACE, "uri1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult = appSearchManager2.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(failResult.isSuccess()).isFalse();
        assertThat(failResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testGetDocuments_DataClass() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addDataClass(EmailDataClass.class).build()));

        // Index a document
        EmailDataClass inEmail = new EmailDataClass();
        inEmail.uri = "uri1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(appSearchManager,
                GenericDocument.DEFAULT_NAMESPACE, "uri1");
        assertThat(outDocuments).hasSize(1);
        EmailDataClass outEmail = outDocuments.get(0).toDataClass(EmailDataClass.class);
        assertThat(inEmail.uri).isEqualTo(outEmail.uri);
        assertThat(inEmail.subject).isEqualTo(outEmail.subject);
        assertThat(inEmail.body).isEqualTo(outEmail.body);
    }

    @Test
    public void testQuery() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager.setSchema(
                new SetSchemaRequest.Builder().addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail).build()));

        // Query for the document
        List<GenericDocument> results = doQuery(appSearchManager, "body");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);

        // Multi-term query
        results = doQuery(appSearchManager, "body email");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_TypeFilter() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("foo")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        checkIsResultSuccess(appSearchManager.setSchema(
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
        GenericDocument inDoc =
                new GenericDocument.Builder<>("uri2", "Generic").setProperty("foo", "body").build();
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail, inDoc).build()));

        // Query for the documents
        List<GenericDocument> results = doQuery(appSearchManager, "body");
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(inEmail, inDoc);

        // Query only for Document
        results = doQuery(appSearchManager, "body", "Generic");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inDoc);
    }

    @Test
    public void testQuery_TwoInstances() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(appSearchManager2.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("uri2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(appSearchManager2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(inEmail2).build()));

        // Query for instance 1.
        List<GenericDocument> results = doQuery(appSearchManager1, "body");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inEmail1);

        // Query for instance 2.
        results = doQuery(appSearchManager2, "body");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inEmail2);
    }

    @Test
    public void testRemove() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager.setSchema(
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
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(appSearchManager, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(appSearchManager.removeDocuments(
                new RemoveDocumentsRequest.Builder().addUris("uri1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1", "uri2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);

        // Test if we delete a nonexistent URI.
        AppSearchBatchResult<String, Void> deleteResult = appSearchManager.removeDocuments(
                new RemoveDocumentsRequest.Builder().addUris("uri1").build()).get();

        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_TwoInstances() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult = appSearchManager2.removeDocuments(
                new RemoveDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
        assertThat(doGet(appSearchManager1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(appSearchManager1.removeDocuments(
                new RemoveDocumentsRequest.Builder().addUris("uri1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager1.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent URI.
        deleteResult = appSearchManager1.removeDocuments(
                new RemoveDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(deleteResult.getFailures().get("uri1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByTypes() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic").build();
        checkIsResultSuccess(appSearchManager.setSchema(
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
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager, GenericDocument.DEFAULT_NAMESPACE, "uri1", "uri2",
                "uri3")).hasSize(3);

        // Delete the email type
        checkIsBatchResultSuccess(appSearchManager.removeByType(AppSearchEmail.SCHEMA_TYPE));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1", "uri2", "uri3").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByTypes_TwoInstances() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(appSearchManager2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(appSearchManager2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(appSearchManager2, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the email type in instance 1
        checkIsBatchResultSuccess(appSearchManager1.removeByType(AppSearchEmail.SCHEMA_TYPE));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager1.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = appSearchManager2.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveByNamespace() throws Exception {
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager = checkIsResultSuccess(future);

        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new PropertyConfig.Builder("foo")
                        .setDataType(PropertyConfig.DATA_TYPE_STRING)
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        checkIsResultSuccess(appSearchManager.setSchema(
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
                        .setProperty("foo", "bar").build();
        checkIsBatchResultSuccess(appSearchManager.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager, /*namespace=*/"email", "uri1", "uri2")).hasSize(2);
        assertThat(doGet(appSearchManager, /*namespace=*/"document", "uri3")).hasSize(1);

        // Delete the email namespace
        checkIsBatchResultSuccess(appSearchManager.removeByNamespace("email"));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager.getDocuments(
                new GetDocumentsRequest.Builder().setNamespace("email")
                        .addUris("uri1", "uri2").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        getResult = appSearchManager.getDocuments(
                new GetDocumentsRequest.Builder().setNamespace("document")
                        .addUris("uri3").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByNamespaces_TwoInstances() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(appSearchManager2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(appSearchManager2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager1, /*namespace=*/"email", "uri1")).hasSize(1);
        assertThat(doGet(appSearchManager2, /*namespace=*/"email", "uri2")).hasSize(1);

        // Delete the email namespace in instance 1
        checkIsBatchResultSuccess(appSearchManager1.removeByNamespace("email"));

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager1.getDocuments(
                new GetDocumentsRequest.Builder().setNamespace("email")
                        .addUris("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = appSearchManager2.getDocuments(
                new GetDocumentsRequest.Builder().setNamespace("email")
                        .addUris("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testDeleteAll_TwoInstances() throws Exception {
        // Create 2 instances
        Future<AppSearchResult<AppSearchManager>> future = AppSearchManager.getInstance("instance1",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager1 = checkIsResultSuccess(future);

        future = AppSearchManager.getInstance("instance2",
                ApplicationProvider.getApplicationContext());
        AppSearchManager appSearchManager2 = checkIsResultSuccess(future);

        // Schema registration
        checkIsResultSuccess(appSearchManager1.setSchema(new SetSchemaRequest.Builder()
                .addSchema(AppSearchEmail.SCHEMA).build()));
        checkIsResultSuccess(appSearchManager2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(appSearchManager1.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email1).build()));
        checkIsBatchResultSuccess(appSearchManager2.putDocuments(
                new PutDocumentsRequest.Builder().addGenericDocument(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(appSearchManager1, GenericDocument.DEFAULT_NAMESPACE, "uri1")).hasSize(1);
        assertThat(doGet(appSearchManager2, GenericDocument.DEFAULT_NAMESPACE, "uri2")).hasSize(1);

        // Delete the all document in instance 1
        checkIsResultSuccess(appSearchManager1.removeAll());

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = appSearchManager1.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = appSearchManager2.getDocuments(
                new GetDocumentsRequest.Builder().addUris("uri2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    private static List<GenericDocument> doGet(AppSearchManager instance,
            String namespace, String... uris)
            throws Exception {
        Future<AppSearchBatchResult<String, GenericDocument>> future =
                instance.getDocuments(new GetDocumentsRequest.Builder()
                        .setNamespace(namespace).addUris(uris).build());
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(future);
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

    private static <K, V> AppSearchBatchResult<K, V> checkIsBatchResultSuccess(
            Future<AppSearchBatchResult<K, V>> future) throws Exception {
        AppSearchBatchResult<K, V> result = future.get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchBatchResult not successful: " + result);
        }
        return result;
    }

    private static <ValueType> ValueType checkIsResultSuccess(
            Future<AppSearchResult<ValueType>> future) throws Exception {
        AppSearchResult<ValueType> result = future.get();
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchResult not successful: " + result);
        }
        return result.getResultValue();
    }
}
