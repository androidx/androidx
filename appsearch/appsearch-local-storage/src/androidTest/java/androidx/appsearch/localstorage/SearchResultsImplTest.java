/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.appsearch.localstorage;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchSpec;

import com.google.android.icing.proto.TermMatchType;
import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class SearchResultsImplTest {
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new UnlimitedLimitConfig(),
                new JetpackIcingOptionsConfig(),
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testGetEmptyNextPage() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert one package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();

        SearchResultsImpl searchResults = new SearchResultsImpl(
                mAppSearchImpl,
                Executors.newCachedThreadPool(),
                "package1",
                "database1",
                "",
                searchSpec,
                /*logger=*/ null);

        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(document1);

        // We get all documents, and it shouldn't fail if we keep calling getNextPage().
        results = searchResults.getNextPageAsync().get();
        assertThat(results).isEmpty();
    }

    @Test
    public void testGetEmptyNextPage_multiPages() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert 3 package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        GenericDocument document3 = new GenericDocument.Builder<>("namespace", "id3",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document3,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 2 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(2)
                .build();

        SearchResultsImpl searchResults = new SearchResultsImpl(
                mAppSearchImpl,
                Executors.newCachedThreadPool(),
                "package1",
                "database1",
                "",
                searchSpec,
                /*logger=*/ null);
        List<GenericDocument> outDocs = new ArrayList<>();
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(2);
        outDocs.add(results.get(0).getGenericDocument());
        outDocs.add(results.get(1).getGenericDocument());

        results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);
        outDocs.add(results.get(0).getGenericDocument());
        assertThat(outDocs).containsExactly(document1, document2, document3);

        // We get all documents, and it shouldn't fail if we keep calling getNextPage().
        results = searchResults.getNextPageAsync().get();
        assertThat(results).isEmpty();
    }
}
