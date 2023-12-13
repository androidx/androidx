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

import org.junit.Test;

/** Tests for private APIs of {@link SearchSpec}. */
public class SearchSpecInternalTest {

    @Test
    public void testSearchSpecBuilder() {
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
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();

        assertThat(searchSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
        assertThat(searchSpec.getFilterNamespaces()).containsExactly(
                "namespace1", "namespace2");
        assertThat(searchSpec.getFilterSchemas()).containsExactly(
                "schemaTypes1", "schemaTypes2");
        assertThat(searchSpec.getFilterPackageNames()).containsExactly(
                "package1", "package2");
        assertThat(searchSpec.getSnippetCount()).isEqualTo(5);
        assertThat(searchSpec.getSnippetCountPerProperty()).isEqualTo(10);
        assertThat(searchSpec.getMaxSnippetSize()).isEqualTo(15);
        assertThat(searchSpec.getResultCountPerPage()).isEqualTo(42);
        assertThat(searchSpec.getOrder()).isEqualTo(SearchSpec.ORDER_ASCENDING);
        assertThat(searchSpec.getRankingStrategy())
                .isEqualTo(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE);
        assertThat(searchSpec.getEnabledFeatures()).containsExactly(
                Features.NUMERIC_SEARCH, Features.VERBATIM_SEARCH,
                Features.LIST_FILTER_QUERY_LANGUAGE);
    }

    // TODO(b/309826655): Flag guard this test.
    @Test
    public void testGetBundle_hasProperty() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .build();

        assertThat(searchSpec.getEnabledFeatures()).containsExactly(
                Features.NUMERIC_SEARCH, Features.VERBATIM_SEARCH,
                Features.LIST_FILTER_QUERY_LANGUAGE, Features.LIST_FILTER_HAS_PROPERTY_FUNCTION);
    }

    @Test
    public void testBuildMultipleSearchSpecs() {
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec1 = builder.build();
        assertThat(searchSpec1.getEnabledFeatures()).isEmpty();

        SearchSpec searchSpec2 = builder.setNumericSearchEnabled(true).build();
        // Check that reusing the builder for new SearchSpec does not change old built SearchSpec.
        assertThat(searchSpec1.getEnabledFeatures()).isEmpty();
        assertThat(searchSpec2.getEnabledFeatures()).containsExactly(Features.NUMERIC_SEARCH);

        SearchSpec searchSpec3 = builder.setNumericSearchEnabled(false)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        assertThat(searchSpec3.getEnabledFeatures()).containsExactly(
                Features.VERBATIM_SEARCH, Features.LIST_FILTER_QUERY_LANGUAGE);
    }
}
