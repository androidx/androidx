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

import android.content.Context;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.test.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppSearchManagerTest {
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private final AppSearchManager mAppSearch = new AppSearchManager(mContext);

    @After
    public void tearDown() {
        mAppSearch.deleteAll();
    }

    @Test
    public void testSetSchema() {
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
    public void testPutDocuments() {
        // Schema registration
        checkIsSuccess(mAppSearch.setSchema(AppSearchEmail.SCHEMA));

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("uri1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result =
                mAppSearch.putDocuments(ImmutableList.of(email));
        checkIsSuccess(result);
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testGetDocuments() {
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
        List<AppSearchDocument> outDocuments = doGet("uri1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);
    }

    @Test
    public void testQuery() {
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
        List<AppSearchDocument> results = doQuery("body");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);

        // Multi-term query
        results = doQuery("body email");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_TypeFilter() {
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
        AppSearchDocument inDoc =
                new AppSearchDocument.Builder("uri2", "Test").setProperty("foo", "body").build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(inEmail, inDoc)));

        // Query for the documents
        List<AppSearchDocument> results = doQuery("body");
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(inEmail, inDoc);

        // Query only for Document
        results = doQuery("body", "Test");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(inDoc);
    }

    @Test
    public void testDelete() {
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
        AppSearchBatchResult<String, AppSearchDocument> getResult =
                mAppSearch.getDocuments(ImmutableList.of("uri1", "uri2"));
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri2")).isEqualTo(email2);
    }

    @Test
    public void testDeleteByTypes() {
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
        AppSearchDocument document1 =
                new AppSearchDocument.Builder("uri3", "schemaType")
                        .setProperty("foo", "bar").build();
        checkIsSuccess(mAppSearch.putDocuments(ImmutableList.of(email1, email2, document1)));

        // Check the presence of the documents
        assertThat(doGet("uri1", "uri2", "uri3")).hasSize(3);

        // Delete the email type
        checkIsSuccess(mAppSearch.deleteByTypes(ImmutableList.of(AppSearchEmail.SCHEMA_TYPE)));

        // Make sure it's really gone
        AppSearchBatchResult<String, AppSearchDocument> getResult =
                mAppSearch.getDocuments(ImmutableList.of("uri1", "uri2", "uri3"));
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("uri1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("uri2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("uri3")).isEqualTo(document1);
    }

    private List<AppSearchDocument> doGet(String... uris) {
        AppSearchBatchResult<String, AppSearchDocument> result =
                mAppSearch.getDocuments(Arrays.asList(uris));
        checkIsSuccess(result);
        assertThat(result.getSuccesses()).hasSize(uris.length);
        assertThat(result.getFailures()).isEmpty();
        List<AppSearchDocument> list = new ArrayList<>(uris.length);
        for (String uri : uris) {
            list.add(result.getSuccesses().get(uri));
        }
        return list;
    }

    private List<AppSearchDocument> doQuery(String queryExpression, String... schemaTypes) {
        AppSearchResult<SearchResults> result = mAppSearch.query(
                queryExpression,
                SearchSpec.newBuilder()
                        .setSchemaTypes(schemaTypes)
                        .setTermMatchType(SearchSpec.TERM_MATCH_TYPE_EXACT_ONLY)
                        .build());
        checkIsSuccess(result);
        SearchResults searchResults = result.getResultValue();
        List<AppSearchDocument> documents = new ArrayList<>();
        while (searchResults.hasNext()) {
            documents.add(searchResults.next().getDocument());
        }
        return documents;
    }

    private void checkIsSuccess(AppSearchResult<?> result) {
        if (!result.isSuccess()) {
            throw new AssertionFailedError("AppSearchResult not successful: " + result);
        }
    }

    private void checkIsSuccess(AppSearchBatchResult<?, ?> result) {
        if (!result.isSuccess()) {
            throw new AssertionFailedError(
                    "AppSearchBatchResult not successful: " + result.getFailures());
        }
    }
}
