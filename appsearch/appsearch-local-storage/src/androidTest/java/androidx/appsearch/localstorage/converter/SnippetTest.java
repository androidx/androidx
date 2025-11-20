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

package androidx.appsearch.localstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.LocalStorageIcingOptionsConfig;
import androidx.appsearch.localstorage.SchemaCache;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.EmbeddingMatchSnippetProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.SnippetMatchProto;
import com.google.android.icing.proto.SnippetProto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class SnippetTest {
    private static final String PACKAGE_NAME = "packageName";
    private static final String DATABASE_NAME = "databaseName";
    private static final String PREFIX = PrefixUtil.createPrefix(PACKAGE_NAME, DATABASE_NAME);
    private static final String PREFIXED_SCHEMA_TYPE = PREFIX + "schema1";
    private static final String PREFIXED_NAMESPACE = PREFIX + "";
    private static final SchemaTypeConfigProto SCHEMA_TYPE_CONFIG_PROTO =
            SchemaTypeConfigProto.newBuilder()
                    .setSchemaType(PREFIXED_SCHEMA_TYPE)
                    .build();
    private static final Map<String, Map<String, SchemaTypeConfigProto>> SCHEMA_MAP =
            Collections.singletonMap(PREFIX,
                    Collections.singletonMap(PREFIXED_SCHEMA_TYPE, SCHEMA_TYPE_CONFIG_PROTO));

    @Test
    public void testSingleStringSnippet() throws Exception {
        final String propertyKeyString = "content";
        final String propertyValueString = "A commonly used fake word is foo.\n"
                + "   Another nonsense word that’s used a lot\n"
                + "   is bar.\n";
        final String id = "id1";
        final String exactMatch = "foo";
        final String window = "is foo";

        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName(propertyKeyString)
                        .addStringValues(propertyValueString))
                .build();
        SnippetProto snippetProto = SnippetProto.newBuilder()
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName(propertyKeyString)
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(29)
                                .setExactMatchByteLength(3)
                                .setExactMatchUtf16Position(29)
                                .setExactMatchUtf16Length(3)
                                .setSubmatchByteLength(3)
                                .setSubmatchUtf16Length(3)
                                .setWindowBytePosition(26)
                                .setWindowByteLength(6)
                                .setWindowUtf16Position(26)
                                .setWindowUtf16Length(6)))
                .build();
        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProto)
                        .setSnippet(snippetProto))
                .build();

        // Making ResultReader and getting Snippet values.
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult.MatchInfo match = searchResultPage.getResults().get(0).getMatchInfos().get(0);
        assertThat(match.getPropertyPath()).isEqualTo(propertyKeyString);
        assertThat(match.getFullText()).isEqualTo(propertyValueString);
        assertThat(match.getExactMatch()).isEqualTo(exactMatch);
        assertThat(match.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
        assertThat(match.getSubmatch()).isEqualTo("foo");
        assertThat(match.getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
        assertThat(match.getFullText()).isEqualTo(propertyValueString);
        assertThat(match.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/26, /*upper=*/32));
        assertThat(match.getSnippet()).isEqualTo(window);

        if (Flags.enableEmbeddingMatchInfo()) {
            assertThat(match.getTextMatch()).isNotNull();
            assertThat(match.getTextMatch().getFullText()).isEqualTo(propertyValueString);
            assertThat(match.getTextMatch().getExactMatch()).isEqualTo(exactMatch);
            assertThat(match.getTextMatch().getExactMatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
            assertThat(match.getTextMatch().getSubmatch()).isEqualTo("foo");
            assertThat(match.getTextMatch().getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
            assertThat(match.getTextMatch().getFullText()).isEqualTo(propertyValueString);
            assertThat(match.getTextMatch().getSnippetRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/26, /*upper=*/32));
            assertThat(match.getTextMatch().getSnippet()).isEqualTo(window);
        }
    }

    @Test
    public void testEmbeddingSnippet() throws Exception {
        assumeTrue(Flags.enableSchemaEmbeddingPropertyConfig());
        assumeTrue(Flags.enableEmbeddingMatchInfo());

        final String propertyKeyEmbedding = "embedding1[1]";
        final String id = "id1";
        final double semanticScore = 1.3f;
        final int embeddingQueryVectorIndex = 0;
        final SearchSpecProto.EmbeddingQueryMetricType.Code embeddingQueryMetricType =
                SearchSpecProto.EmbeddingQueryMetricType.Code.COSINE;

        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName(propertyKeyEmbedding)
                        .addVectorValues(PropertyProto.VectorProto.newBuilder()
                                .addAllValues(Arrays.asList(1.1f, 2.2f, 3.3f))
                                .setModelSignature("my_model_v1")
                        )
                        .addVectorValues(PropertyProto.VectorProto.newBuilder()
                                .addAllValues(Arrays.asList(4.4f, 5.5f, 6.6f, 7.7f))
                                .setModelSignature("my_model_v2")
                        ))
                .build();
        SnippetProto snippetProto = SnippetProto.newBuilder()
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName(propertyKeyEmbedding)
                        .addEmbeddingMatches(EmbeddingMatchSnippetProto.newBuilder()
                                .setSemanticScore(semanticScore)
                                .setEmbeddingQueryVectorIndex(embeddingQueryVectorIndex)
                                .setEmbeddingQueryMetricType(embeddingQueryMetricType)))
                .build();

        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProto)
                        .setSnippet(snippetProto))
                .build();

        // Making ResultReader and getting Snippet values.
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult.MatchInfo match = searchResultPage.getResults().get(0).getMatchInfos().get(0);
        assertThat(match.getPropertyPath()).isEqualTo(propertyKeyEmbedding);
        assertThat(match.getEmbeddingMatch()).isNotNull();
        assertThat(match.getEmbeddingMatch().getSemanticScore()).isEqualTo(semanticScore);
        assertThat(match.getEmbeddingMatch().getQueryEmbeddingVectorIndex()).isEqualTo(
                embeddingQueryVectorIndex);
        assertThat(match.getEmbeddingMatch().getEmbeddingSearchMetricType()).isEqualTo(
                embeddingQueryMetricType.getNumber());
    }

    @Test
    public void testHybridSnippet() throws Exception {
        assumeTrue(Flags.enableSchemaEmbeddingPropertyConfig());
        assumeTrue(Flags.enableEmbeddingMatchInfo());

        final String id = "id1";
        final String propertyKeyString = "content";
        final String propertyValueString = "A commonly used fake word is foo.\n"
                + "   Another nonsense word that’s used a lot\n"
                + "   is bar.\n";
        final String exactMatch = "foo";
        final String window = "is foo";

        final String propertyKeyEmbedding = "embedding1[1]";
        final double semanticScore = 1.3f;
        final int embeddingQueryVectorIndex = 0;
        final SearchSpecProto.EmbeddingQueryMetricType.Code embeddingQueryMetricType =
                SearchSpecProto.EmbeddingQueryMetricType.Code.COSINE;

        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName(propertyKeyString)
                        .addVectorValues(PropertyProto.VectorProto.newBuilder()
                                .addAllValues(Arrays.asList(1.1f, 2.2f, 3.3f))
                                .setModelSignature("my_model_v1")
                        )
                        .addVectorValues(PropertyProto.VectorProto.newBuilder()
                                .addAllValues(Arrays.asList(4.4f, 5.5f, 6.6f, 7.7f))
                                .setModelSignature("my_model_v2")
                        ))
                .addProperties(PropertyProto.newBuilder()
                        .setName(propertyKeyString)
                        .addStringValues(propertyValueString))
                .build();

        SnippetProto snippetProto = SnippetProto.newBuilder()
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName(propertyKeyString)
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(29)
                                .setExactMatchByteLength(3)
                                .setExactMatchUtf16Position(29)
                                .setExactMatchUtf16Length(3)
                                .setSubmatchByteLength(3)
                                .setSubmatchUtf16Length(3)
                                .setWindowBytePosition(26)
                                .setWindowByteLength(6)
                                .setWindowUtf16Position(26)
                                .setWindowUtf16Length(6)))
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName(propertyKeyEmbedding)
                        .addEmbeddingMatches(EmbeddingMatchSnippetProto.newBuilder()
                                .setSemanticScore(semanticScore)
                                .setEmbeddingQueryVectorIndex(embeddingQueryVectorIndex)
                                .setEmbeddingQueryMetricType(embeddingQueryMetricType)))
                .build();

        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProto)
                        .setSnippet(snippetProto))
                .build();

        // Making ResultReader and getting Snippet values.
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);

        // TextMatchInfo
        SearchResult.MatchInfo match1 = searchResultPage.getResults().get(0).getMatchInfos().get(0);
        assertThat(match1.getPropertyPath()).isEqualTo(propertyKeyString);
        assertThat(match1.getTextMatch()).isNotNull();
        assertThat(match1.getTextMatch().getFullText()).isEqualTo(propertyValueString);
        assertThat(match1.getTextMatch().getExactMatch()).isEqualTo(exactMatch);
        assertThat(match1.getTextMatch().getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
        assertThat(match1.getTextMatch().getSubmatch()).isEqualTo("foo");
        assertThat(match1.getTextMatch().getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29, /*upper=*/32));
        assertThat(match1.getTextMatch().getFullText()).isEqualTo(propertyValueString);
        assertThat(match1.getTextMatch().getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/26, /*upper=*/32));
        assertThat(match1.getTextMatch().getSnippet()).isEqualTo(window);

        //EmbeddingMatchInfo
        SearchResult.MatchInfo match2 = searchResultPage.getResults().get(0).getMatchInfos().get(1);
        assertThat(match2.getPropertyPath()).isEqualTo(propertyKeyEmbedding);
        assertThat(match2.getEmbeddingMatch()).isNotNull();
        assertThat(match2.getEmbeddingMatch().getSemanticScore()).isEqualTo(semanticScore);
        assertThat(match2.getEmbeddingMatch().getQueryEmbeddingVectorIndex()).isEqualTo(
                embeddingQueryVectorIndex);
        assertThat(match2.getEmbeddingMatch().getEmbeddingSearchMetricType()).isEqualTo(
                embeddingQueryMetricType.getNumber());
    }

    @Test
    public void testNoSnippets() throws Exception {
        final String propertyKeyString = "content";
        final String propertyValueString = "A commonly used fake word is foo.\n"
                + "   Another nonsense word that’s used a lot\n"
                + "   is bar.\n";
        final String id = "id1";

        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName(propertyKeyString)
                        .addStringValues(propertyValueString))
                .build();
        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder().setDocument(documentProto))
                .build();

        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getMatchInfos()).isEmpty();
    }

    @Test
    public void testMultipleStringSnippet() throws Exception {
        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("uri1")
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName("senderName")
                        .addStringValues("Test Name Jr."))
                .addProperties(PropertyProto.newBuilder()
                        .setName("senderEmail")
                        .addStringValues("TestNameJr@gmail.com"))
                .build();
        SnippetProto snippetProto = SnippetProto.newBuilder()
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName("senderName")
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(0)
                                .setExactMatchByteLength(4)
                                .setExactMatchUtf16Position(0)
                                .setExactMatchUtf16Length(4)
                                .setSubmatchByteLength(4)
                                .setSubmatchUtf16Length(4)
                                .setWindowBytePosition(0)
                                .setWindowByteLength(9)
                                .setWindowUtf16Position(0)
                                .setWindowUtf16Length(9)))
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName("senderEmail")
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(0)
                                .setExactMatchByteLength(20)
                                .setExactMatchUtf16Position(0)
                                .setExactMatchUtf16Length(20)
                                .setSubmatchByteLength(4)
                                .setSubmatchUtf16Length(4)
                                .setWindowBytePosition(0)
                                .setWindowByteLength(20)
                                .setWindowUtf16Position(0)
                                .setWindowUtf16Length(20)))
                .build();
        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProto)
                        .setSnippet(snippetProto))
                .build();

        // Making ResultReader and getting Snippet values.
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult.MatchInfo match1 = searchResultPage.getResults().get(0).getMatchInfos().get(0);
        assertThat(match1.getPropertyPath()).isEqualTo("senderName");
        assertThat(match1.getFullText()).isEqualTo("Test Name Jr.");
        assertThat(match1.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match1.getExactMatch()).isEqualTo("Test");
        assertThat(match1.getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match1.getSubmatch()).isEqualTo("Test");
        assertThat(match1.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/9));
        assertThat(match1.getSnippet()).isEqualTo("Test Name");

        if (Flags.enableEmbeddingMatchInfo()) {
            assertThat(match1.getTextMatch()).isNotNull();
            assertThat(match1.getTextMatch().getFullText()).isEqualTo("Test Name Jr.");
            assertThat(match1.getTextMatch().getExactMatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match1.getTextMatch().getExactMatch()).isEqualTo("Test");
            assertThat(match1.getTextMatch().getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match1.getTextMatch().getSubmatch()).isEqualTo("Test");
            assertThat(match1.getTextMatch().getSnippetRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/9));
            assertThat(match1.getTextMatch().getSnippet()).isEqualTo("Test Name");
        }

        SearchResult.MatchInfo match2 = searchResultPage.getResults().get(0).getMatchInfos().get(1);
        assertThat(match2.getPropertyPath()).isEqualTo("senderEmail");
        assertThat(match2.getFullText()).isEqualTo("TestNameJr@gmail.com");
        assertThat(match2.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/20));
        assertThat(match2.getExactMatch()).isEqualTo("TestNameJr@gmail.com");
        assertThat(match2.getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match2.getSubmatch()).isEqualTo("Test");
        assertThat(match2.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/20));
        assertThat(match2.getSnippet()).isEqualTo("TestNameJr@gmail.com");

        if (Flags.enableEmbeddingMatchInfo()) {
            assertThat(match2.getTextMatch()).isNotNull();
            assertThat(match2.getTextMatch().getFullText()).isEqualTo("TestNameJr@gmail.com");
            assertThat(match2.getTextMatch().getExactMatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/20));
            assertThat(match2.getTextMatch().getExactMatch()).isEqualTo("TestNameJr@gmail.com");
            assertThat(match2.getTextMatch().getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match2.getTextMatch().getSubmatch()).isEqualTo("Test");
            assertThat(match2.getTextMatch().getSnippetRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/20));
            assertThat(match2.getTextMatch().getSnippet()).isEqualTo("TestNameJr@gmail.com");
        }
    }

    @Test
    public void testNestedDocumentSnippet() throws Exception {
        // Building the SearchResult received from query.
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("id1")
                .setNamespace(PREFIXED_NAMESPACE)
                .setSchema(PREFIXED_SCHEMA_TYPE)
                .addProperties(PropertyProto.newBuilder()
                        .setName("sender")
                        .addDocumentValues(DocumentProto.newBuilder()
                                .setNamespace(PREFIXED_NAMESPACE)
                                .setSchema(PREFIXED_SCHEMA_TYPE)
                                .addProperties(PropertyProto.newBuilder()
                                        .setName("name")
                                        .addStringValues("Test Name Jr."))
                                .addProperties(PropertyProto.newBuilder()
                                        .setName("email")
                                        .addStringValues("TestNameJr@gmail.com")
                                        .addStringValues("TestNameJr2@gmail.com"))))
                .build();
        SnippetProto snippetProto = SnippetProto.newBuilder()
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName("sender.name")
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(0)
                                .setExactMatchByteLength(4)
                                .setExactMatchUtf16Position(0)
                                .setExactMatchUtf16Length(4)
                                .setSubmatchByteLength(4)
                                .setSubmatchUtf16Length(4)
                                .setWindowBytePosition(0)
                                .setWindowByteLength(9)
                                .setWindowUtf16Position(0)
                                .setWindowUtf16Length(9)))
                .addEntries(SnippetProto.EntryProto.newBuilder()
                        .setPropertyName("sender.email[1]")
                        .addSnippetMatches(SnippetMatchProto.newBuilder()
                                .setExactMatchBytePosition(0)
                                .setExactMatchByteLength(21)
                                .setExactMatchUtf16Position(0)
                                .setExactMatchUtf16Length(21)
                                .setSubmatchByteLength(4)
                                .setSubmatchUtf16Length(4)
                                .setWindowBytePosition(0)
                                .setWindowByteLength(21)
                                .setWindowUtf16Position(0)
                                .setWindowUtf16Length(21)))
                .build();
        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProto)
                        .setSnippet(snippetProto))
                .build();

        // Making ResultReader and getting Snippet values.
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto,
                new SchemaCache(SCHEMA_MAP), new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()));
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult.MatchInfo match1 = searchResultPage.getResults().get(0).getMatchInfos().get(0);
        assertThat(match1.getPropertyPath()).isEqualTo("sender.name");
        assertThat(match1.getPropertyPathObject()).isEqualTo(new PropertyPath("sender.name"));
        assertThat(match1.getFullText()).isEqualTo("Test Name Jr.");
        assertThat(match1.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match1.getExactMatch()).isEqualTo("Test");
        assertThat(match1.getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match1.getSubmatch()).isEqualTo("Test");
        assertThat(match1.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/9));
        assertThat(match1.getSnippet()).isEqualTo("Test Name");

        if (Flags.enableEmbeddingMatchInfo()) {
            assertThat(match1.getTextMatch()).isNotNull();
            assertThat(match1.getTextMatch().getFullText()).isEqualTo("Test Name Jr.");
            assertThat(match1.getTextMatch().getExactMatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match1.getTextMatch().getExactMatch()).isEqualTo("Test");
            assertThat(match1.getTextMatch().getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match1.getTextMatch().getSubmatch()).isEqualTo("Test");
            assertThat(match1.getTextMatch().getSnippetRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/9));
            assertThat(match1.getTextMatch().getSnippet()).isEqualTo("Test Name");
        }

        SearchResult.MatchInfo match2 = searchResultPage.getResults().get(0).getMatchInfos().get(1);
        assertThat(match2.getPropertyPath()).isEqualTo("sender.email[1]");
        assertThat(match2.getPropertyPathObject()).isEqualTo(new PropertyPath("sender.email[1]"));
        assertThat(match2.getFullText()).isEqualTo("TestNameJr2@gmail.com");
        assertThat(match2.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/21));
        assertThat(match2.getExactMatch()).isEqualTo("TestNameJr2@gmail.com");
        assertThat(match2.getSubmatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
        assertThat(match2.getSubmatch()).isEqualTo("Test");
        assertThat(match2.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/0, /*upper=*/21));
        assertThat(match2.getSnippet()).isEqualTo("TestNameJr2@gmail.com");

        if (Flags.enableEmbeddingMatchInfo()) {
            assertThat(match2.getTextMatch()).isNotNull();
            assertThat(match2.getTextMatch().getFullText()).isEqualTo("TestNameJr2@gmail.com");
            assertThat(match2.getTextMatch().getExactMatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/21));
            assertThat(match2.getTextMatch().getExactMatch()).isEqualTo("TestNameJr2@gmail.com");
            assertThat(match2.getTextMatch().getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/4));
            assertThat(match2.getTextMatch().getSubmatch()).isEqualTo("Test");
            assertThat(match2.getTextMatch().getSnippetRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/0, /*upper=*/21));
            assertThat(match2.getTextMatch().getSnippet()).isEqualTo("TestNameJr2@gmail.com");
        }
    }
}
