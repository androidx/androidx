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

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;

import com.google.common.collect.ImmutableList;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class AppSearchManagerTest {
    private final AppSearchManager mAppSearch = new AppSearchManager();

    @After
    public void tearDown() throws Exception {
        Future<AppSearchResult<Void>> future = mAppSearch.deleteAll();
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
        checkIsSuccess(mAppSearch.setSchema(emailSchema));
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("uri1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        Future<AppSearchBatchResult<String, Void>> future =
                mAppSearch.putDocuments(ImmutableList.of(email));
        checkIsSuccess(future);
        AppSearchBatchResult<String, Void> result = future.get();
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testGetDocuments() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(inEmail)));

        // Get the document
        List<GenericDocument> outDocuments = doGet("uri1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);
    }

    @Test
    public void testQuery() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(inEmail)));

        // Query for the document
        List<GenericDocument> results = doQuery("body");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);

        // Multi-term query
        results = doQuery("body email");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_TypeFilter() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("uri1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument inDoc =
                new GenericDocument.Builder("uri2", "Test").setProperty("foo", "body").build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(inEmail, inDoc)));

        // Query for the documents
        List<GenericDocument> results = doQuery("body");
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(inEmail, inDoc);

        // Query only for Document
        results = doQuery("body", "Test");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inDoc);
    }

    @Test
    public void testDelete() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

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
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(email1, email2)));

        // Check the presence of the documents
        assertThat(doGet("uri1")).hasSize(1);
        assertThat(doGet("uri2")).hasSize(1);

        // Delete the document
        checkIsSuccess(mAppSearch.delete(ImmutableList.of("uri1")));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch.getDocuments(ImmutableList.of("uri1", "uri2")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);

        // Test if we delete a nonexistent URI.
        AppSearchBatchResult<String, Void> deleteResult =
                mAppSearch.delete(ImmutableList.of("uri1")).get();
        assertThat(deleteResult.getFailures()).containsExactly("uri1",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/ null));
    }

    @Test
    public void testDeleteByTypes() throws Exception {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

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
                new GenericDocument.Builder("uri3", "schemaType")
                        .setProperty("foo", "bar").build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(email1, email2, document1)));

        // Check the presence of the documents
        assertThat(doGet("uri1", "uri2", "uri3")).hasSize(3);

        // Delete the email type
        checkIsSuccess(mAppSearch.deleteByTypes(ImmutableList.of(AppSearchEmail.SCHEMA_TYPE)));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult =
                mAppSearch.getDocuments(ImmutableList.of("uri1", "uri2", "uri3")).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    private List<GenericDocument> doGet(String... uris) throws Exception {
        Future<AppSearchBatchResult<String, GenericDocument>> future =
                mAppSearch.getDocuments(Arrays.asList(uris));
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

    private List<GenericDocument> doQuery(String queryExpression, String... schemaTypes)
            throws Exception {
        AppSearchResult<SearchResults> result = mAppSearch.query(
                queryExpression,
                SearchSpec.newBuilder()
                        .setSchemaTypes(schemaTypes)
                        .setTermMatchType(SearchSpec.TERM_MATCH_TYPE_EXACT_ONLY)
                        .build()).get();
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
            AppSearchBatchResult<?, ?> result = (AppSearchBatchResult) futureGet;
            if (!result.isSuccess()) {
                throw new AssertionFailedError(
                        "AppSearchBatchResult not successful: " + result.getFailures());
            }
        } else if (futureGet instanceof AppSearchResult) {
            AppSearchResult<?> result = (AppSearchResult) futureGet;
            if (!result.isSuccess()) {
                throw new AssertionFailedError(
                        "AppSearchBatchResult not successful: " + result);
            }
        }
    }
}
