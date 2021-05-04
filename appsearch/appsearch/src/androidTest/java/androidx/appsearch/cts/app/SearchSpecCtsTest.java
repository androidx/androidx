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

package androidx.appsearch.cts.app;

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.SearchSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchSpecCtsTest {
    @Test
    public void testBuildSearchSpecWithoutTermMatch() {
        SearchSpec searchSpec = new SearchSpec.Builder().addFilterSchemas("testSchemaType").build();
        assertThat(searchSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
    }

    @Test
    public void testBuildSearchSpec() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterNamespaces("namespace1", "namespace2")
                .addFilterNamespaces(ImmutableList.of("namespace3"))
                .addFilterSchemas("schemaTypes1", "schemaTypes2")
                .addFilterSchemas(ImmutableList.of("schemaTypes3"))
                .addFilterPackageNames("package1", "package2")
                .addFilterPackageNames(ImmutableList.of("package3"))
                .setSnippetCount(5)
                .setSnippetCountPerProperty(10)
                .setMaxSnippetSize(15)
                .setResultCountPerPage(42)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*limit=*/ 37)
                .addProjection("schemaType1", expectedPropertyPaths1)
                .addProjection("schemaType2", expectedPropertyPaths2)
                .build();

        assertThat(searchSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
        assertThat(searchSpec.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2", "namespace3").inOrder();
        assertThat(searchSpec.getFilterSchemas())
                .containsExactly("schemaTypes1", "schemaTypes2", "schemaTypes3").inOrder();
        assertThat(searchSpec.getFilterPackageNames())
                .containsExactly("package1", "package2", "package3").inOrder();
        assertThat(searchSpec.getSnippetCount()).isEqualTo(5);
        assertThat(searchSpec.getSnippetCountPerProperty()).isEqualTo(10);
        assertThat(searchSpec.getMaxSnippetSize()).isEqualTo(15);
        assertThat(searchSpec.getResultCountPerPage()).isEqualTo(42);
        assertThat(searchSpec.getOrder()).isEqualTo(SearchSpec.ORDER_ASCENDING);
        assertThat(searchSpec.getRankingStrategy())
                .isEqualTo(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
        assertThat(searchSpec.getResultGroupingTypeFlags())
                .isEqualTo(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE);
        assertThat(searchSpec.getProjections())
                .containsExactly("schemaType1", expectedPropertyPaths1, "schemaType2",
                        expectedPropertyPaths2);
        assertThat(searchSpec.getResultGroupingLimit()).isEqualTo(37);
    }

    @Test
    public void testGetProjectionTypePropertyMasks() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjection("TypeA", ImmutableList.of("field1", "field2.subfield2"))
                .addProjection("TypeB", ImmutableList.of("field7"))
                .addProjection("TypeC", ImmutableList.of())
                .build();

        Map<String, List<String>> typePropertyPathMap = searchSpec.getProjections();
        assertThat(typePropertyPathMap.keySet())
                .containsExactly("TypeA", "TypeB", "TypeC");
        assertThat(typePropertyPathMap.get("TypeA")).containsExactly("field1", "field2.subfield2");
        assertThat(typePropertyPathMap.get("TypeB")).containsExactly("field7");
        assertThat(typePropertyPathMap.get("TypeC")).isEmpty();
    }

// @exportToFramework:startStrip()
    @Document
    static class King extends Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.StringProperty
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;
    }

    static class Card {}

    @Test
    public void testFilterDocumentClasses_byCollection() throws Exception {
        Set<Class<King>> cardClassSet = ImmutableSet.of(King.class);
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterDocumentClasses(cardClassSet)
                .build();

        assertThat(searchSpec.getFilterSchemas()).containsExactly("King");
    }
// @exportToFramework:endStrip()
}
