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
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.core.os.BuildCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
public class AppSearchSessionPlatformCtsTest extends AppSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName)
                        .setWorkerExecutor(executor).build());
    }

    @Test
    public void testFeaturesSupported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, DB_NAME_2).build()).get();
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH))
                .isEqualTo(BuildCompat.isAtLeastT());
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK))
                .isEqualTo(BuildCompat.isAtLeastT());
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA))
                .isEqualTo(BuildCompat.isAtLeastT());
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_BY_ID))
                .isEqualTo(BuildCompat.isAtLeastT());
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY))
                .isEqualTo(BuildCompat.isAtLeastT());
    }

    @Test
    public void testPutDocuments_emptyBytesAndDocuments() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db = PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, DB_NAME_1).build()).get();
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        db.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schema, AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "testSchema")
                .setPropertyBytes("bytes")
                .setPropertyDocument("document")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(db.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1")
                .build();
        List<GenericDocument> outDocuments = doGet(db, request);
        assertThat(outDocuments).hasSize(1);
        GenericDocument outDocument = outDocuments.get(0);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S
                || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
            // We fixed b/204677124 in Android T, so in S and S_V2, getByteArray and
            // getDocumentArray will return null if we set empty properties.
            assertThat(outDocument.getPropertyBytesArray("bytes")).isNull();
            assertThat(outDocument.getPropertyDocumentArray("document")).isNull();
        } else {
            assertThat(outDocument.getPropertyBytesArray("bytes")).isEmpty();
            assertThat(outDocument.getPropertyDocumentArray("document")).isEmpty();
        }
    }

    @Override
    @Test
    public void testPutLargeDocumentBatch() throws Exception {
        // b/185441119 was fixed in Android T, this test will fail on S_V2 devices and below.
        assumeTrue(BuildCompat.isAtLeastT());
        super.testPutLargeDocumentBatch();
    }

    @Override
    @Test
    public void testSetSchemaWithIncompatibleNestedSchema() throws Exception {
        // TODO(b/230879098) This bug was fixed in Android T, but will currently fail on S_V2
        // devices and below. However, we could implement a workaround in platform-storage.
        // Implement that workaround and enable on S and S_V2.
        assumeTrue(BuildCompat.isAtLeastT());
        super.testSetSchemaWithIncompatibleNestedSchema();
    }

    @Override
    @Test
    public void testEmojiSnippet() throws Exception {
        // b/229770338 was fixed in Android T, this test will fail on S_V2 devices and below.
        assumeTrue(BuildCompat.isAtLeastT());
        super.testEmojiSnippet();
    }

    // TODO(b/256022027) Remove this overridden test once the change to setMaxJoinedResultCount
    // is synced over into framework.
    @Override
    @Test
    public void testSimpleJoin() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db = PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, DB_NAME_1).build()).get();
        assumeTrue(db.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        db.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index a document
        // While inEmail2 has a higher document score, we will rank based on the number of joined
        // documents. inEmail1 will have 1 joined document while inEmail2 will have 0 joined
        // documents.
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(10)
                        .build();

        String qualifiedId = DocumentIdUtil.createQualifiedId(context.getPackageName(), DB_NAME_1,
                "namespace", "id1");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setScore(1)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Monday").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id4", "ViewAction")
                .setScore(2)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Tuesday").build();
        checkIsBatchResultSuccess(db.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inEmail2,
                                viewAction1, viewAction2)
                        .build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_ASCENDING)
                        .build();

        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(1)
                .build();

        SearchResults searchResults = db.search("body email", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        // Both email docs are returned, but id1 comes first due to the join
        assertThat(sr).hasSize(2);

        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction1);
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(1.0);

        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(0.0);
        assertThat(sr.get(1).getJoinedResults()).isEmpty();
    }
}
