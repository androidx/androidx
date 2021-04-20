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

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.appsearch.annotation.Document;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchSpecTest {

    @Test
    public void testGetBundle() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterNamespaces("namespace1", "namespace2")
                .addFilterSchemas("schemaTypes1", "schemaTypes2")
                .addFilterPackageNames("package1", "package2")
                .setSnippetCount(5)
                .setSnippetCountPerProperty(10)
                .setMaxSnippetSize(15)
                .setResultCountPerPage(42)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                .build();

        Bundle bundle = searchSpec.getBundle();
        assertThat(bundle.getInt(SearchSpec.TERM_MATCH_TYPE_FIELD))
                .isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
        assertThat(bundle.getStringArrayList(SearchSpec.NAMESPACE_FIELD)).containsExactly(
                "namespace1", "namespace2");
        assertThat(bundle.getStringArrayList(SearchSpec.SCHEMA_FIELD)).containsExactly(
                "schemaTypes1", "schemaTypes2");
        assertThat(bundle.getStringArrayList(SearchSpec.PACKAGE_NAME_FIELD)).containsExactly(
                "package1", "package2");
        assertThat(bundle.getInt(SearchSpec.SNIPPET_COUNT_FIELD)).isEqualTo(5);
        assertThat(bundle.getInt(SearchSpec.SNIPPET_COUNT_PER_PROPERTY_FIELD)).isEqualTo(10);
        assertThat(bundle.getInt(SearchSpec.MAX_SNIPPET_FIELD)).isEqualTo(15);
        assertThat(bundle.getInt(SearchSpec.NUM_PER_PAGE_FIELD)).isEqualTo(42);
        assertThat(bundle.getInt(SearchSpec.ORDER_FIELD)).isEqualTo(SearchSpec.ORDER_ASCENDING);
        assertThat(bundle.getInt(SearchSpec.RANKING_STRATEGY_FIELD))
                .isEqualTo(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE);
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

    @Test
    public void testGetRankingStrategy() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build();
        assertThat(searchSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
    }

// @exportToFramework:startStrip()
    @Document
    static class King extends Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.Property
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

        Bundle bundle = searchSpec.getBundle();
        assertThat(bundle.getStringArrayList(SearchSpec.SCHEMA_FIELD)).containsExactly(
                "King");
    }
// @exportToFramework:endStrip()
}
