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

import static org.junit.Assert.assertThrows;

import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchSpecCtsTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildSearchSpecWithoutTermMatch() {
        SearchSpec searchSpec = new SearchSpec.Builder().addFilterSchemas("testSchemaType").build();
        assertThat(searchSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
    }

    @Test
    public void testBuildSearchSpec() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        Map<String, Double> expectedPropertyWeights = ImmutableMap.of("property1", 1.0,
                "property2", 2.0);
        Map<PropertyPath, Double> expectedPropertyWeightPaths =
                ImmutableMap.of(new PropertyPath("property1.nested"), 1.0);

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
                .addProjection("schemaTypes1", expectedPropertyPaths1)
                .addProjection("schemaTypes2", expectedPropertyPaths2)
                .setPropertyWeights("schemaTypes1", expectedPropertyWeights)
                .setPropertyWeightPaths("schemaTypes2", expectedPropertyWeightPaths)
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
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
                .containsExactly("schemaTypes1", expectedPropertyPaths1, "schemaTypes2",
                        expectedPropertyPaths2);
        assertThat(searchSpec.getResultGroupingLimit()).isEqualTo(37);
        assertThat(searchSpec.getPropertyWeights().keySet()).containsExactly("schemaTypes1",
                "schemaTypes2");
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes1"))
                .containsExactly("property1", 1.0, "property2", 2.0);
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes2"))
                .containsExactly("property1.nested", 1.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes1"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                        new PropertyPath("property2"), 2.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes2"))
                .containsExactly(new PropertyPath("property1.nested"), 1.0);
        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
    public void testBuildSearchSpec_scorablePropertyRanking() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        Map<String, Double> expectedPropertyWeights = ImmutableMap.of("property1", 1.0,
                "property2", 2.0);
        Map<PropertyPath, Double> expectedPropertyWeightPaths =
                ImmutableMap.of(new PropertyPath("property1.nested"), 1.0);

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
                .addProjection("schemaTypes1", expectedPropertyPaths1)
                .addProjection("schemaTypes2", expectedPropertyPaths2)
                .setPropertyWeights("schemaTypes1", expectedPropertyWeights)
                .setPropertyWeightPaths("schemaTypes2", expectedPropertyWeightPaths)
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setScorablePropertyRankingEnabled(true)
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
                .containsExactly("schemaTypes1", expectedPropertyPaths1, "schemaTypes2",
                        expectedPropertyPaths2);
        assertThat(searchSpec.getResultGroupingLimit()).isEqualTo(37);
        assertThat(searchSpec.getPropertyWeights().keySet()).containsExactly("schemaTypes1",
                "schemaTypes2");
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes1"))
                .containsExactly("property1", 1.0, "property2", 2.0);
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes2"))
                .containsExactly("property1.nested", 1.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes1"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                        new PropertyPath("property2"), 2.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes2"))
                .containsExactly(new PropertyPath("property1.nested"), 1.0);
        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isScorablePropertyRankingEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
    public void testBuildSearchSpec_retrieveEmbeddingMatchInfo() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        Map<String, Double> expectedPropertyWeights = ImmutableMap.of("property1", 1.0,
                "property2", 2.0);
        Map<PropertyPath, Double> expectedPropertyWeightPaths =
                ImmutableMap.of(new PropertyPath("property1.nested"), 1.0);

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
                .setRetrieveEmbeddingMatchInfos(true)
                .setResultCountPerPage(42)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*limit=*/ 37)
                .addProjection("schemaTypes1", expectedPropertyPaths1)
                .addProjection("schemaTypes2", expectedPropertyPaths2)
                .setPropertyWeights("schemaTypes1", expectedPropertyWeights)
                .setPropertyWeightPaths("schemaTypes2", expectedPropertyWeightPaths)
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
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
        assertThat(searchSpec.shouldRetrieveEmbeddingMatchInfos()).isTrue();
        assertThat(searchSpec.getResultCountPerPage()).isEqualTo(42);
        assertThat(searchSpec.getOrder()).isEqualTo(SearchSpec.ORDER_ASCENDING);
        assertThat(searchSpec.getRankingStrategy())
                .isEqualTo(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
        assertThat(searchSpec.getResultGroupingTypeFlags())
                .isEqualTo(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE);
        assertThat(searchSpec.getProjections())
                .containsExactly("schemaTypes1", expectedPropertyPaths1, "schemaTypes2",
                        expectedPropertyPaths2);
        assertThat(searchSpec.getResultGroupingLimit()).isEqualTo(37);
        assertThat(searchSpec.getPropertyWeights().keySet()).containsExactly("schemaTypes1",
                "schemaTypes2");
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes1"))
                .containsExactly("property1", 1.0, "property2", 2.0);
        assertThat(searchSpec.getPropertyWeights().get("schemaTypes2"))
                .containsExactly("property1.nested", 1.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes1"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                        new PropertyPath("property2"), 2.0);
        assertThat(searchSpec.getPropertyWeightPaths().get("schemaTypes2"))
                .containsExactly(new PropertyPath("property1.nested"), 1.0);
        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
    }

    @Test
    public void testBuildSearchSpec_searchSourceLogTag() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setSearchSourceLogTag("logTag")
                .build();

        assertThat(searchSpec.getSearchSourceLogTag()).isEqualTo("logTag");
    }

    @Test
    public void testBuildSearchSpec_searchSourceLogTag_exceedLengthLimitation() {
        String longTag = new String(new char[110]);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SearchSpec.Builder()
                        .setSearchSourceLogTag(longTag));
        assertThat(e).hasMessageThat().contains(
                "The maximum supported tag length is 100. This tag is too long");
    }

    @Test
    public void testBuildSearchSpec_searchSourceLogTag_defaultIsNull() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build();

        assertThat(searchSpec.getSearchSourceLogTag()).isNull();
    }

    // TODO(b/309826655): Flag guard this test.
    @Test
    public void testBuildSearchSpec_hasProperty() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .build();

        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isListFilterHasPropertyFunctionEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
    public void testBuildSearchSpec_matchScoreExpression() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .setListFilterMatchScoreExpressionFunctionEnabled(true)
                .build();

        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isListFilterHasPropertyFunctionEnabled()).isTrue();
        assertThat(searchSpec.isListFilterMatchScoreExpressionFunctionEnabled()).isTrue();
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
    public void testGetProjectionObjects() {
        PropertyPath path1 = new PropertyPath("field1");
        PropertyPath path2 = new PropertyPath("field2.subfield2");
        PropertyPath path3 = new PropertyPath("field7");

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjectionPaths("TypeA", ImmutableList.of(path1, path2))
                .addProjectionPaths("TypeB", ImmutableList.of(path3))
                .addProjectionPaths("TypeC", ImmutableList.of())
                .build();

        Map<String, List<PropertyPath>> typePropertyPathMap = searchSpec.getProjectionPaths();
        assertThat(typePropertyPathMap.keySet())
                .containsExactly("TypeA", "TypeB", "TypeC");
        assertThat(typePropertyPathMap.get("TypeA")).containsExactly(path1, path2);
        assertThat(typePropertyPathMap.get("TypeB")).containsExactly(path3);
        assertThat(typePropertyPathMap.get("TypeC")).isEmpty();
    }

    @Test
    public void testGetTypePropertyWeights() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeights("TypeA", ImmutableMap.of("property1", 1.0, "property2", 2.0))
                .setPropertyWeights("TypeB", ImmutableMap.of("property1", 1.0, "property2"
                        + ".nested", 2.0))
                .build();

        Map<String, Map<String, Double>> typePropertyWeightsMap = searchSpec.getPropertyWeights();

        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("TypeA", "TypeB");
        assertThat(typePropertyWeightsMap.get("TypeA")).containsExactly("property1", 1.0,
                "property2", 2.0);
        assertThat(typePropertyWeightsMap.get("TypeB")).containsExactly("property1", 1.0,
                "property2.nested", 2.0);
    }

    @Test
    public void testGetTypePropertyWeightPaths() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeightPaths("TypeA",
                    ImmutableMap.of(new PropertyPath("property1"), 1.0,
                                    new PropertyPath("property2"), 2.0))
                .setPropertyWeightPaths("TypeB",
                    ImmutableMap.of(new PropertyPath("property1"), 1.0,
                                    new PropertyPath("property2.nested"), 2.0))
                .build();

        Map<String, Map<PropertyPath, Double>> typePropertyWeightsMap =
                searchSpec.getPropertyWeightPaths();

        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("TypeA", "TypeB");
        assertThat(typePropertyWeightsMap.get("TypeA"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                             new PropertyPath("property2"), 2.0);
        assertThat(typePropertyWeightsMap.get("TypeB"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                             new PropertyPath("property2.nested"), 2.0);
    }

    @Test
    public void testGetTypePropertyWeightsWithAdvancedRanking() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy("sum(this.propertyWeights())")
                .setPropertyWeights("TypeA", ImmutableMap.of("property1", 1.0, "property2", 2.0))
                .setPropertyWeights("TypeB", ImmutableMap.of("property1", 1.0, "property2"
                        + ".nested", 2.0))
                .build();

        Map<String, Map<String, Double>> typePropertyWeightsMap = searchSpec.getPropertyWeights();

        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("TypeA", "TypeB");
        assertThat(typePropertyWeightsMap.get("TypeA")).containsExactly("property1", 1.0,
                "property2", 2.0);
        assertThat(typePropertyWeightsMap.get("TypeB")).containsExactly("property1", 1.0,
                "property2.nested", 2.0);
    }

    @Test
    public void testGetTypePropertyWeightPathsWithAdvancedRanking() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy("sum(this.propertyWeights())")
                .setPropertyWeightPaths("TypeA",
                        ImmutableMap.of(new PropertyPath("property1"), 1.0,
                                new PropertyPath("property2"), 2.0))
                .setPropertyWeightPaths("TypeB",
                        ImmutableMap.of(new PropertyPath("property1"), 1.0,
                                new PropertyPath("property2.nested"), 2.0))
                .build();

        Map<String, Map<PropertyPath, Double>> typePropertyWeightsMap =
                searchSpec.getPropertyWeightPaths();

        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("TypeA", "TypeB");
        assertThat(typePropertyWeightsMap.get("TypeA"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                        new PropertyPath("property2"), 2.0);
        assertThat(typePropertyWeightsMap.get("TypeB"))
                .containsExactly(new PropertyPath("property1"), 1.0,
                        new PropertyPath("property2.nested"), 2.0);
    }

    @Test
    public void testSetPropertyWeights_nonPositiveWeight() {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder();
        Map<String, Double> negativePropertyWeight = ImmutableMap.of("property", -1.0);

        assertThrows(IllegalArgumentException.class,
                () -> searchSpecBuilder.setPropertyWeights("TypeA",
                        negativePropertyWeight));

        Map<String, Double> zeroPropertyWeight = ImmutableMap.of("property", 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> searchSpecBuilder.setPropertyWeights("TypeA", zeroPropertyWeight));
    }

    @Test
    public void testSetPropertyWeightPaths_nonPositiveWeight() {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder();
        Map<PropertyPath, Double> negativePropertyWeight =
                ImmutableMap.of(new PropertyPath("property"), -1.0);

        assertThrows(IllegalArgumentException.class,
                () -> searchSpecBuilder.setPropertyWeightPaths("TypeA",
                        negativePropertyWeight));

        Map<PropertyPath, Double> zeroPropertyWeight =
                ImmutableMap.of(new PropertyPath("property"), 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> searchSpecBuilder.setPropertyWeightPaths("TypeA", zeroPropertyWeight));
    }

    @Test
    public void testSetPropertyWeights_queryIndependentRankingStrategy() throws Exception {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setPropertyWeights("TypeA", ImmutableMap.of("property1", 1.0, "property2", 2.0));

        assertThrows(IllegalArgumentException.class, () -> searchSpecBuilder.build());
    }

    @Test
    public void testSetPropertyWeightPaths_queryIndependentRankingStrategy() throws Exception {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setPropertyWeightPaths("TypeA",
                                        ImmutableMap.of(new PropertyPath("property1"), 1.0,
                                                        new PropertyPath("property2"), 2.0));

        assertThrows(IllegalArgumentException.class, () -> searchSpecBuilder.build());
    }

    @Test
    public void testBuild_builtObjectsAreImmutable() throws Exception {
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeights("TypeA", ImmutableMap.of("property1", 1.0, "property2", 2.0));

        SearchSpec originalSpec = searchSpecBuilder.build();

        // Modify the builder.
        SearchSpec newSpec =
                searchSpecBuilder.setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY).setPropertyWeights(
                        "TypeA", Collections.emptyMap()).build();


        // Verify that 1) the changes took effect on the builder and 2) originalSpec was unaffected.
        assertThat(newSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_EXACT_ONLY);
        assertThat(newSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
        assertThat(newSpec.getPropertyWeights().keySet()).containsExactly("TypeA");
        assertThat(newSpec.getPropertyWeights().get("TypeA")).isEmpty();

        assertThat(originalSpec.getTermMatch()).isEqualTo(SearchSpec.TERM_MATCH_PREFIX);
        assertThat(originalSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
        assertThat(originalSpec.getPropertyWeights().keySet()).containsExactly("TypeA");
        assertThat(originalSpec.getPropertyWeights().get("TypeA").keySet()).containsExactly(
                "property1", "property2");
    }

    @Test
    public void testGetJoinSpec() {
        JoinSpec joinSpec = new JoinSpec.Builder("entityId")
                .setNestedSearch("joe", new SearchSpec.Builder().build())
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(20)
                .build();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(joinSpec)
                .build();

        assertThat(searchSpec.getJoinSpec()).isNotNull();
        assertThat(searchSpec.getJoinSpec().getNestedQuery()).isEqualTo("joe");
        assertThat(searchSpec.getJoinSpec().getAggregationScoringStrategy())
                .isEqualTo(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT);
        assertThat(searchSpec.getJoinSpec().getMaxJoinedResultCount()).isEqualTo(20);
        assertThat(searchSpec.getJoinSpec().getChildPropertyExpression()).isEqualTo("entityId");
    }

    @Test
    public void testAdvancedRanking() {
        SearchSpec emptySearchSpec = new SearchSpec.Builder().build();
        assertThat(emptySearchSpec.getAdvancedRankingExpression()).isEmpty();

        SearchSpec searchSpec = new SearchSpec.Builder().setRankingStrategy(
                "this.documentScore()").build();
        assertThat(searchSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION);
        assertThat(searchSpec.getAdvancedRankingExpression()).isEqualTo("this.documentScore()");
    }

    @Test
    public void testOverwriteRankingStrategy() {
        SearchSpec emptySearchSpec = new SearchSpec.Builder().build();
        assertThat(emptySearchSpec.getAdvancedRankingExpression()).isEmpty();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRankingStrategy("this.documentScore()")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build();
        assertThat(searchSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE);
        assertThat(searchSpec.getAdvancedRankingExpression()).isEmpty();

        searchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setRankingStrategy("this.documentScore()")
                .build();
        assertThat(searchSpec.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION);
        assertThat(searchSpec.getAdvancedRankingExpression()).isEqualTo("this.documentScore()");
    }

    @Test
    public void testSetFeatureEnabledToFalse() {
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec = builder.setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();

        searchSpec = builder.setNumericSearchEnabled(false)
                .setVerbatimSearchEnabled(false)
                .setListFilterQueryLanguageEnabled(false)
                .build();
        assertThat(searchSpec.isNumericSearchEnabled()).isFalse();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isFalse();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isFalse();
    }

    // TODO(b/309826655): Flag guard this test.
    @Test
    public void testSetFeatureEnabledToFalse_hasProperty() {
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec = builder
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .build();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isListFilterHasPropertyFunctionEnabled()).isTrue();

        searchSpec = builder
                .setListFilterQueryLanguageEnabled(false)
                .setListFilterHasPropertyFunctionEnabled(false)
                .build();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isFalse();
        assertThat(searchSpec.isListFilterHasPropertyFunctionEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
    public void testSetFeatureEnabledToFalse_matchScoreExpression() {
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec = builder
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterMatchScoreExpressionFunctionEnabled(true)
                .build();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isListFilterMatchScoreExpressionFunctionEnabled()).isTrue();

        searchSpec = builder
                .setListFilterQueryLanguageEnabled(false)
                .setListFilterMatchScoreExpressionFunctionEnabled(false)
                .build();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isFalse();
        assertThat(searchSpec.isListFilterMatchScoreExpressionFunctionEnabled()).isFalse();
    }

    @Test
    public void testInvalidAdvancedRanking() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchSpec.Builder().setRankingStrategy(""));
        assertThrows(IllegalArgumentException.class,
                () -> new SearchSpec.Builder().setRankingStrategy(
                        SearchSpec.RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION));
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

    @Test
    public void testProjectionsForDocumentClass() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjectionPathsForDocumentClass(King.class, ImmutableList.of(
                        new PropertyPath("field1"), new PropertyPath("field2.subfield2")))
                .build();

        assertThat(searchSpec.getProjections().get("King"))
                .containsExactly("field1", "field2.subfield2");

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addProjectionsForDocumentClass(King.class,
                        ImmutableList.of("field3", "field4.subfield3"))
                .build();

        assertThat(searchSpec.getProjections().get("King"))
                .containsExactly("field3", "field4.subfield3");
    }

    @Test
    public void testTypePropertyWeightsForDocumentClass() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeightsForDocumentClass(
                    King.class,
                    ImmutableMap.of("field1", 1.0, "field2.subfield2", 2.0))
                .build();

        Map<String, Map<String, Double>> typePropertyWeightsMap = searchSpec.getPropertyWeights();
        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("King");
        assertThat(typePropertyWeightsMap.get("King")).containsExactly("field1", 1.0,
                "field2.subfield2", 2.0);

        Map<String, Map<PropertyPath, Double>> typePropertyWeightPathsMap =
                searchSpec.getPropertyWeightPaths();
        assertThat(typePropertyWeightPathsMap.keySet())
                .containsExactly("King");
        assertThat(typePropertyWeightPathsMap.get("King"))
                .containsExactly(new PropertyPath("field1"), 1.0,
                             new PropertyPath("field2.subfield2"), 2.0);
    }

    @Test
    public void testTypePropertyWeightPathsForDocumentClass() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeightPathsForDocumentClass(
                    King.class,
                    ImmutableMap.of(new PropertyPath("field1"), 1.0,
                                    new PropertyPath("field2.subfield2"), 2.0))
                .build();

        Map<String, Map<String, Double>> typePropertyWeightsMap = searchSpec.getPropertyWeights();
        assertThat(typePropertyWeightsMap.keySet())
                .containsExactly("King");
        assertThat(typePropertyWeightsMap.get("King")).containsExactly("field1", 1.0,
                "field2.subfield2", 2.0);

        Map<String, Map<PropertyPath, Double>> typePropertyWeightPathsMap =
                searchSpec.getPropertyWeightPaths();
        assertThat(typePropertyWeightPathsMap.keySet())
                .containsExactly("King");
        assertThat(typePropertyWeightPathsMap.get("King"))
                .containsExactly(new PropertyPath("field1"), 1.0,
                             new PropertyPath("field2.subfield2"), 2.0);
    }

// @exportToFramework:endStrip()

    @Test
    public void testInvalidJoinSpecConfig() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                        .build());

        assertThat(e)
                .hasMessageThat()
                .isEqualTo("Attempting to rank based on joined documents, but"
                                + " no JoinSpec provided");

        JoinSpec joinSpec = new JoinSpec.Builder("childProp")
                .setAggregationScoringStrategy(
                        JoinSpec.AGGREGATION_SCORING_SUM_RANKING_SIGNAL)
                .build();
        e = assertThrows(IllegalStateException.class, () -> new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setJoinSpec(joinSpec)
                .build());
        assertThat(e)
                .hasMessageThat()
                .isEqualTo(
                        "Aggregate scoring strategy has been set in the "
                                + "nested JoinSpec, but ranking strategy is not "
                                + "RANKING_STRATEGY_JOIN_AGGREGATE_SCORE");
    }

    @Test
    public void testGetPropertyFiltersTypePropertyMasks() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterProperties("TypeA", ImmutableList.of("field1", "field2.subfield2"))
                .addFilterProperties("TypeB", ImmutableList.of("field7"))
                .addFilterProperties("TypeC", ImmutableList.of())
                .build();

        Map<String, List<String>> typePropertyPathMap = searchSpec.getFilterProperties();
        assertThat(typePropertyPathMap.keySet())
                .containsExactly("TypeA", "TypeB", "TypeC");
        assertThat(typePropertyPathMap.get("TypeA")).containsExactly("field1", "field2.subfield2");
        assertThat(typePropertyPathMap.get("TypeB")).containsExactly("field7");
        assertThat(typePropertyPathMap.get("TypeC")).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
    public void testAddFilterPropertyPaths() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterProperties("TypeA", ImmutableList.of("field1", "field2.subfield2"))
                .addFilterPropertyPaths(
                        "TypeB",
                        ImmutableList.of(
                                new PropertyPath("field3"), new PropertyPath("field4.field5")))
                .addFilterPropertyPaths("TypeC", ImmutableList.of(new PropertyPath("field6")))
                .addFilterProperties("TypeD", ImmutableList.of("field7"))
                .addFilterPropertyPaths("TypeE", ImmutableList.of(new PropertyPath("field8")))
                .addFilterProperties("TypeE", ImmutableList.of())
                .build();

        Map<String, List<String>> typePropertyPathMap = searchSpec.getFilterProperties();
        assertThat(typePropertyPathMap.keySet())
                .containsExactly("TypeA", "TypeB", "TypeC", "TypeD", "TypeE");
        assertThat(typePropertyPathMap.get("TypeA")).containsExactly("field1", "field2.subfield2");
        assertThat(typePropertyPathMap.get("TypeB")).containsExactly("field3", "field4.field5");
        assertThat(typePropertyPathMap.get("TypeC")).containsExactly("field6");
        assertThat(typePropertyPathMap.get("TypeD")).containsExactly("field7");
        assertThat(typePropertyPathMap.get("TypeE")).isEmpty();
    }

    @Test
    public void testFilterSchemas_wildcardProjection() {
        // Should not crash
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterSchemas("ParentType")
                .addProjection(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("TypeA"))
                .addFilterProperties(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("TypeB"))
                .build();

        assertThat(searchSpec.getFilterSchemas()).containsExactly("ParentType");
        assertThat(searchSpec.getProjections())
                .containsExactly(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("TypeA"));
        assertThat(searchSpec.getFilterProperties())
                .containsExactly(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("TypeB"));
    }

    @Test
    public void testRebuild() {
        JoinSpec originalJoinSpec = new JoinSpec.Builder("entityId")
                .setNestedSearch("joe", new SearchSpec.Builder().addFilterSchemas("Action").build())
                .build();

        JoinSpec newJoinSpec = new JoinSpec.Builder("entitySchema")
                .setNestedSearch("",
                        new SearchSpec.Builder().addFilterSchemas("CallAction").build())
                .build();

        SearchSpec.Builder searchSpecBuilder =
                new SearchSpec.Builder().setJoinSpec(originalJoinSpec);

        SearchSpec original = searchSpecBuilder.build();
        SearchSpec rebuild = searchSpecBuilder
                .setJoinSpec(newJoinSpec)
                .build();

        assertThat(original.getJoinSpec()).isNotNull();
        assertThat(original.getJoinSpec().getChildPropertyExpression()).isEqualTo("entityId");
        assertThat(original.getJoinSpec().getNestedQuery()).isEqualTo("joe");
        assertThat(original.getJoinSpec().getNestedSearchSpec().getFilterSchemas())
                .containsExactly("Action");

        assertThat(rebuild.getJoinSpec()).isNotNull();
        assertThat(rebuild.getJoinSpec().getChildPropertyExpression()).isEqualTo("entitySchema");
        assertThat(rebuild.getJoinSpec().getNestedQuery()).isEmpty();
        assertThat(rebuild.getJoinSpec().getNestedSearchSpec().getFilterSchemas())
                .containsExactly("CallAction");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch() {
        EmbeddingVector embedding1 = new EmbeddingVector(
                new float[]{1.1f, 2.2f, 3.3f}, "my_model_v1");
        EmbeddingVector embedding2 = new EmbeddingVector(
                new float[]{4.4f, 5.5f, 6.6f, 7.7f}, "my_model_v2");
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(embedding1, embedding2)
                .build();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.getDefaultEmbeddingSearchMetricType()).isEqualTo(
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT);
        assertThat(searchSpec.getEmbeddingParameters()).containsExactly(embedding1, embedding2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testRebuild_embeddingSearch() {
        EmbeddingVector embedding1 = new EmbeddingVector(
                new float[]{1.1f, 2.2f, 3.3f}, "my_model_v1");
        EmbeddingVector embedding2 = new EmbeddingVector(
                new float[]{4.4f, 5.5f, 6.6f, 7.7f}, "my_model_v2");

        // Create a builder
        SearchSpec.Builder searchSpecBuilder = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(embedding1);
        SearchSpec searchSpec1 = searchSpecBuilder.build();

        // Add a new embedding to the builder and rebuild. We should see that the new embedding
        // is only added to searchSpec2.
        searchSpecBuilder.addEmbeddingParameters(embedding2);
        SearchSpec searchSpec2 = searchSpecBuilder.build();

        assertThat(searchSpec1.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec1.getDefaultEmbeddingSearchMetricType()).isEqualTo(
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT);
        assertThat(searchSpec1.getEmbeddingParameters()).containsExactly(embedding1);

        assertThat(searchSpec2.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec2.getDefaultEmbeddingSearchMetricType()).isEqualTo(
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT);
        assertThat(searchSpec2.getEmbeddingParameters()).containsExactly(embedding1, embedding2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testBuildSearchSpec_embeddingSearch() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .build();

        assertThat(searchSpec.isNumericSearchEnabled()).isTrue();
        assertThat(searchSpec.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchSpec.isListFilterQueryLanguageEnabled()).isTrue();
        assertThat(searchSpec.isListFilterHasPropertyFunctionEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public void testInformationalRankingExpressions() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setRankingStrategy("this.documentScore()")
                .addInformationalRankingExpressions("this.relevanceScore()")
                .addInformationalRankingExpressions(
                        ImmutableSet.of("this.documentScore() * this.relevanceScore()", "1 + 1"))
                .build();
        assertThat(searchSpec.getOrder()).isEqualTo(SearchSpec.ORDER_ASCENDING);
        assertThat(searchSpec.getRankingStrategy())
                .isEqualTo(SearchSpec.RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION);
        assertThat(searchSpec.getAdvancedRankingExpression())
                .isEqualTo("this.documentScore()");
        assertThat(searchSpec.getInformationalRankingExpressions()).containsExactly(
                "this.relevanceScore()",
                "this.documentScore() * this.relevanceScore()", "1 + 1").inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public void testRebuild_informationalRankingExpressions() {
        SearchSpec.Builder searchSpecBuilder =
                new SearchSpec.Builder().addInformationalRankingExpressions(
                        "this.relevanceScore()");

        SearchSpec original = searchSpecBuilder.build();
        SearchSpec rebuild = searchSpecBuilder
                .addInformationalRankingExpressions("this.documentScore()")
                .addInformationalRankingExpressions(
                        ImmutableSet.of("this.documentScore() * this.relevanceScore()", "1 + 1"))
                .build();

        // Rebuild won't effect the original object
        assertThat(original.getInformationalRankingExpressions())
                .containsExactly("this.relevanceScore()");

        assertThat(rebuild.getInformationalRankingExpressions())
                .containsExactly("this.relevanceScore()", "this.documentScore()",
                        "this.documentScore() * this.relevanceScore()", "1 + 1").inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public void testSearchSpecStrings_default_isEmpty() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        assertThat(searchSpec.getSearchStringParameters()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public void testSearchSpecStrings_addValues_areCumulative() {
        SearchSpec.Builder searchSpecBuilder =
                new SearchSpec.Builder().addSearchStringParameters("A", "b");
        SearchSpec spec = searchSpecBuilder.build();
        assertThat(spec.getSearchStringParameters()).containsExactly("A", "b").inOrder();

        searchSpecBuilder.addSearchStringParameters(Arrays.asList("C", "d"));
        spec = searchSpecBuilder.build();
        assertThat(spec.getSearchStringParameters()).containsExactly("A", "b", "C", "d").inOrder();

        searchSpecBuilder.addSearchStringParameters("e");
        spec = searchSpecBuilder.build();
        assertThat(spec.getSearchStringParameters())
                .containsExactly("A", "b", "C", "d", "e").inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public void testSearchSpecStrings_rebuild_doesntAffectOriginal() {
        SearchSpec.Builder searchSpecBuilder =
                new SearchSpec.Builder().addSearchStringParameters("A", "b");

        SearchSpec original = searchSpecBuilder.build();
        SearchSpec rebuild =
                searchSpecBuilder.addSearchStringParameters(Arrays.asList("C", "d")).build();

        // Rebuild won't effect the original object
        assertThat(original.getSearchStringParameters()).containsExactly("A", "b").inOrder();
        assertThat(rebuild.getSearchStringParameters())
                .containsExactly("A", "b", "C", "d").inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
    public void testFilterDocumentIds() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .addFilterDocumentIds(ImmutableList.of("uri1", "uri2"))
                .addFilterDocumentIds(ImmutableList.of("uri3"))
                .addFilterDocumentIds("uri4", "uri5")
                .addFilterDocumentIds(ImmutableList.of())
                .build();

        List<String> filterDocumentIds = searchSpec.getFilterDocumentIds();
        assertThat(filterDocumentIds)
                .containsExactly("uri1", "uri2", "uri3", "uri4", "uri5");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
    public void testFilterDocumentIds_default_isEmpty() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        assertThat(searchSpec.getFilterDocumentIds()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
    public void testFilterDocumentIds_rebuild_doesntAffectOriginal() {
        SearchSpec.Builder searchSpecBuilder =
                new SearchSpec.Builder().addFilterDocumentIds(ImmutableList.of("uri1", "uri2"));

        SearchSpec original = searchSpecBuilder.build();
        SearchSpec rebuild = searchSpecBuilder
                .addFilterDocumentIds(ImmutableList.of("uri3"))
                .build();

        // Rebuild won't effect the original object
        assertThat(original.getFilterDocumentIds()).containsExactly("uri1", "uri2");
        assertThat(rebuild.getFilterDocumentIds()).containsExactly("uri1", "uri2", "uri3");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testSearchSpecBuilder_copyConstructor() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        Map<String, Double> expectedPropertyWeights = ImmutableMap.of("property1", 1.0,
                "property2", 2.0);
        Map<PropertyPath, Double> expectedPropertyWeightPaths =
                ImmutableMap.of(new PropertyPath("property1.nested"), 1.0);

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
                .addProjection("schemaTypes1", expectedPropertyPaths1)
                .addProjection("schemaTypes2", expectedPropertyPaths2)
                .setPropertyWeights("schemaTypes1", expectedPropertyWeights)
                .setPropertyWeightPaths("schemaTypes2", expectedPropertyWeightPaths)
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getTermMatch()).isEqualTo(searchSpec.getTermMatch());
        assertThat(searchSpecCopy.getFilterNamespaces()).isEqualTo(
                searchSpec.getFilterNamespaces());
        assertThat(searchSpecCopy.getFilterSchemas()).isEqualTo(searchSpec.getFilterSchemas());
        assertThat(searchSpecCopy.getFilterPackageNames()).isEqualTo(
                searchSpec.getFilterPackageNames());
        assertThat(searchSpecCopy.getSnippetCount()).isEqualTo(searchSpec.getSnippetCount());
        assertThat(searchSpecCopy.getSnippetCountPerProperty()).isEqualTo(
                searchSpec.getSnippetCountPerProperty());
        assertThat(searchSpecCopy.getMaxSnippetSize()).isEqualTo(searchSpec.getMaxSnippetSize());
        assertThat(searchSpecCopy.getResultCountPerPage()).isEqualTo(
                searchSpec.getResultCountPerPage());
        assertThat(searchSpecCopy.getOrder()).isEqualTo(searchSpec.getOrder());
        assertThat(searchSpecCopy.getRankingStrategy()).isEqualTo(searchSpec.getRankingStrategy());
        assertThat(searchSpecCopy.getResultGroupingTypeFlags()).isEqualTo(
                searchSpec.getResultGroupingTypeFlags());
        assertThat(searchSpecCopy.getProjections()).isEqualTo(searchSpec.getProjections());
        assertThat(searchSpecCopy.getResultGroupingLimit()).isEqualTo(
                searchSpec.getResultGroupingLimit());
        assertThat(searchSpecCopy.getPropertyWeights()).isEqualTo(searchSpec.getPropertyWeights());
        assertThat(searchSpecCopy.getPropertyWeightPaths()).isEqualTo(
                searchSpec.getPropertyWeightPaths());
        assertThat(searchSpecCopy.isNumericSearchEnabled()).isEqualTo(
                searchSpec.isNumericSearchEnabled());
        assertThat(searchSpecCopy.isVerbatimSearchEnabled()).isEqualTo(
                searchSpec.isVerbatimSearchEnabled());
        assertThat(searchSpecCopy.isListFilterQueryLanguageEnabled()).isEqualTo(
                searchSpec.isListFilterQueryLanguageEnabled());
    }

    @Test
    @RequiresFlagsEnabled({
            Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SCORABLE_PROPERTY
    })
    public void testSearchSpecBuilder_copyConstructorWithScorableProperty() {
        List<String> expectedPropertyPaths1 = ImmutableList.of("path1", "path2");
        List<String> expectedPropertyPaths2 = ImmutableList.of("path3", "path4");
        Map<String, Double> expectedPropertyWeights = ImmutableMap.of("property1", 1.0,
                "property2", 2.0);
        Map<PropertyPath, Double> expectedPropertyWeightPaths =
                ImmutableMap.of(new PropertyPath("property1.nested"), 1.0);

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
                .addProjection("schemaTypes1", expectedPropertyPaths1)
                .addProjection("schemaTypes2", expectedPropertyPaths2)
                .setPropertyWeights("schemaTypes1", expectedPropertyWeights)
                .setPropertyWeightPaths("schemaTypes2", expectedPropertyWeightPaths)
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setScorablePropertyRankingEnabled(true)
                .build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getTermMatch()).isEqualTo(searchSpec.getTermMatch());
        assertThat(searchSpecCopy.getFilterNamespaces()).isEqualTo(
                searchSpec.getFilterNamespaces());
        assertThat(searchSpecCopy.getFilterSchemas()).isEqualTo(searchSpec.getFilterSchemas());
        assertThat(searchSpecCopy.getFilterPackageNames()).isEqualTo(
                searchSpec.getFilterPackageNames());
        assertThat(searchSpecCopy.getSnippetCount()).isEqualTo(searchSpec.getSnippetCount());
        assertThat(searchSpecCopy.getSnippetCountPerProperty()).isEqualTo(
                searchSpec.getSnippetCountPerProperty());
        assertThat(searchSpecCopy.getMaxSnippetSize()).isEqualTo(searchSpec.getMaxSnippetSize());
        assertThat(searchSpecCopy.getResultCountPerPage()).isEqualTo(
                searchSpec.getResultCountPerPage());
        assertThat(searchSpecCopy.getOrder()).isEqualTo(searchSpec.getOrder());
        assertThat(searchSpecCopy.getRankingStrategy()).isEqualTo(searchSpec.getRankingStrategy());
        assertThat(searchSpecCopy.getResultGroupingTypeFlags()).isEqualTo(
                searchSpec.getResultGroupingTypeFlags());
        assertThat(searchSpecCopy.getProjections()).isEqualTo(searchSpec.getProjections());
        assertThat(searchSpecCopy.getResultGroupingLimit()).isEqualTo(
                searchSpec.getResultGroupingLimit());
        assertThat(searchSpecCopy.getPropertyWeights()).isEqualTo(searchSpec.getPropertyWeights());
        assertThat(searchSpecCopy.getPropertyWeightPaths()).isEqualTo(
                searchSpec.getPropertyWeightPaths());
        assertThat(searchSpecCopy.isNumericSearchEnabled()).isEqualTo(
                searchSpec.isNumericSearchEnabled());
        assertThat(searchSpecCopy.isVerbatimSearchEnabled()).isEqualTo(
                searchSpec.isVerbatimSearchEnabled());
        assertThat(searchSpecCopy.isListFilterQueryLanguageEnabled()).isEqualTo(
                searchSpec.isListFilterQueryLanguageEnabled());
        assertThat(searchSpecCopy.isScorablePropertyRankingEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES})
    public void testSearchSpecBuilder_clearBuilderParameters() {
        JoinSpec joinSpec = new JoinSpec.Builder("query").build();
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterNamespaces("namespace1", "namespace2")
                .addFilterSchemas("schemaTypes1", "schemaTypes2")
                .addFilterPackageNames("package1", "package2")
                .addFilterProperties("schemaTypes1", ImmutableList.of("path1", "path2"))
                .addFilterProperties("schemaTypes2", ImmutableList.of("path3", "path4"))
                .addProjection("schemaTypes1", ImmutableList.of("path1", "path2"))
                .addProjection("schemaTypes2", ImmutableList.of("path3", "path4"))
                .setPropertyWeights("schemaTypes1", ImmutableMap.of("property1", 1.0))
                .setPropertyWeights("schemaTypes2", ImmutableMap.of("property2", 2.0))
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*limit=*/ 37)
                .setJoinSpec(joinSpec)
                .clearFilterNamespaces()
                .clearFilterPackageNames()
                .clearFilterProperties()
                .clearFilterSchemas()
                .clearJoinSpec()
                .clearProjections()
                .clearPropertyWeights()
                .clearResultGrouping()
                .build();
        assertThat(searchSpec.getFilterNamespaces()).isEmpty();
        assertThat(searchSpec.getFilterPackageNames()).isEmpty();
        assertThat(searchSpec.getFilterProperties()).isEmpty();
        assertThat(searchSpec.getFilterSchemas()).isEmpty();
        assertThat(searchSpec.getProjections()).isEmpty();
        assertThat(searchSpec.getPropertyWeights()).isEmpty();
        assertThat(searchSpec.getResultGroupingLimit()).isEqualTo(0);
        assertThat(searchSpec.getResultGroupingTypeFlags()).isEqualTo(0);
        assertThat(searchSpec.getJoinSpec()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG})
    public void testSearchSpecBuilder_copyConstructor_embeddingParameters() {
        EmbeddingVector embedding1 = new EmbeddingVector(
                new float[]{1.1f, 2.2f, 3.3f}, "my_model_v1");
        EmbeddingVector embedding2 = new EmbeddingVector(
                new float[]{4.4f, 5.5f, 6.6f, 7.7f}, "my_model_v2");
        SearchSpec searchSpec = new SearchSpec.Builder().addEmbeddingParameters(embedding1,
                embedding2).build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getEmbeddingParameters()).isEqualTo(
                searchSpec.getEmbeddingParameters());
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG})
    public void testSearchSpecBuilder_copyConstructor_clearEmbeddingParameters() {
        EmbeddingVector embedding1 = new EmbeddingVector(
                new float[]{1.1f, 2.2f, 3.3f}, "my_model_v1");
        EmbeddingVector embedding2 = new EmbeddingVector(
                new float[]{4.4f, 5.5f, 6.6f, 7.7f}, "my_model_v2");
        SearchSpec searchSpec = new SearchSpec.Builder().addEmbeddingParameters(embedding1,
                embedding2).clearEmbeddingParameters().build();
        assertThat(searchSpec.getEmbeddingParameters()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS})
    public void testSearchSpecBuilder_copyConstructor_informationalRankingExpressions() {
        SearchSpec searchSpec = new SearchSpec.Builder().addInformationalRankingExpressions("info1",
                "info2").build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getInformationalRankingExpressions()).isEqualTo(
                searchSpec.getInformationalRankingExpressions());
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS})
    public void testSearchSpecBuilder_clearInformationalRankingExpressions() {
        SearchSpec searchSpec = new SearchSpec.Builder().addInformationalRankingExpressions("info1",
                "info2").clearInformationalRankingExpressions().build();
        assertThat(searchSpec.getInformationalRankingExpressions()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG})
    public void testSearchSpecBuilder_copyConstructor_searchSourceLogTag() {
        SearchSpec searchSpec = new SearchSpec.Builder().setSearchSourceLogTag("source").build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getSearchSourceLogTag()).isEqualTo(
                searchSpec.getSearchSourceLogTag());
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG})
    public void testSearchSpecBuilder_clearSearchSourceLogTag() {
        SearchSpec searchSpec = new SearchSpec.Builder().setSearchSourceLogTag(
                "source").clearSearchSourceLogTag().build();
        assertThat(searchSpec.getSearchSourceLogTag()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS})
    public void testSearchSpecBuilder_copyConstructor_searchStringParameters() {
        SearchSpec searchSpec = new SearchSpec.Builder().addSearchStringParameters("param1",
                "param2").build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getSearchStringParameters()).isEqualTo(
                searchSpec.getSearchStringParameters());
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS})
    public void testSearchSpecBuilder_clearSearchStringParameters() {
        SearchSpec searchSpec = new SearchSpec.Builder().addSearchStringParameters("param1",
                "param2").clearSearchStringParameters().build();
        assertThat(searchSpec.getSearchStringParameters()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS})
    public void testSearchSpecBuilder_copyConstructor_filterDocumentIds() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterDocumentIds(ImmutableList.of("uri1", "uri2")).build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.getFilterDocumentIds()).isEqualTo(
                searchSpec.getFilterDocumentIds());
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS})
    public void testSearchSpecBuilder_clearFilterDocumentIds() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterDocumentIds(ImmutableList.of("uri1", "uri2"))
                .clearFilterDocumentIds()
                .build();
        assertThat(searchSpec.getFilterDocumentIds()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
    public void testSetAndGetEnableScorablePropertyRanking() {
        SearchSpec defaultSearchSpec = new SearchSpec.Builder().build();
        assertThat(defaultSearchSpec.isScorablePropertyRankingEnabled()).isFalse();

        SearchSpec searchSpecWithScorablePropertyEnabled = new SearchSpec.Builder()
                .setScorablePropertyRankingEnabled(true).build();
        assertThat(
                searchSpecWithScorablePropertyEnabled.isScorablePropertyRankingEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
    public void testGetIsScorablePropertyRankingEnabled_copyFromSearchSpecBuilder() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setScorablePropertyRankingEnabled(true)
                .build();

        SearchSpec.Builder newSearchSpecBuilder = new SearchSpec.Builder(searchSpec);
        SearchSpec newSearchSpecCopied = newSearchSpecBuilder.build();
        assertThat(newSearchSpecCopied.isScorablePropertyRankingEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS,
            Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO})
    public void testSearchSpecBuilder_copyConstructor_embeddingMatchInfo() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRetrieveEmbeddingMatchInfos(true)
                .build();
        SearchSpec searchSpecCopy = new SearchSpec.Builder(searchSpec).build();
        assertThat(searchSpecCopy.shouldRetrieveEmbeddingMatchInfos()).isTrue();
    }
}
