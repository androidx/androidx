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

package androidx.appsearch.localstorage.converter;

import static androidx.appsearch.app.SearchSpec.GROUPING_TYPE_PER_PACKAGE;
import static androidx.appsearch.app.SearchSpec.ORDER_ASCENDING;
import static androidx.appsearch.app.SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.DefaultIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.testutil.AppSearchTestUtils;

import com.google.android.icing.proto.JoinSpecProto;
import com.google.android.icing.proto.PropertyWeight;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.TypePropertyWeights;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchSpecToProtoConverterTest {
    /** An optimize strategy that always triggers optimize. */
    public static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new UnlimitedLimitConfig(),
                new DefaultIcingOptionsConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testToSearchSpecProto() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");

        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of(
                        prefix1 + "namespace1",
                        prefix1 + "namespace2"),
                prefix2, ImmutableSet.of(
                        prefix2 + "namespace1",
                        prefix2 + "namespace2")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        prefix1 + "typeA", configProto,
                        prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                        prefix2 + "typeA", configProto,
                        prefix2 + "typeB", configProto)));
        // Convert SearchSpec to proto.
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();

        assertThat(searchSpecProto.getQuery()).isEqualTo("query");
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA", "package$database1/typeB", "package$database2/typeA",
                "package$database2/typeB");
        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2",
                "package$database2/namespace1", "package$database2/namespace2");
    }

    @Test
    public void testToSearchSpecProtoWithJoinSpec() throws Exception {
        SearchSpec nestedSearchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP).build();
        SearchSpec.Builder searchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE);

        // Create a JoinSpec object and set it in the converter
        JoinSpec joinSpec = new JoinSpec.Builder("childPropertyExpression")
                .setNestedSearch("nestedQuery", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_SUM_RANKING_SIGNAL)
                .setMaxJoinedResultCount(10)
                .build();

        searchSpec.setJoinSpec(joinSpec);
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");

        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec.build(),
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of(
                        prefix1 + "namespace1",
                        prefix1 + "namespace2"),
                prefix2, ImmutableSet.of(
                        prefix2 + "namespace1",
                        prefix2 + "namespace2")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        prefix1 + "typeA", configProto,
                        prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                        prefix2 + "typeA", configProto,
                        prefix2 + "typeB", configProto)));

        // Convert SearchSpec to proto.
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();

        assertThat(searchSpecProto.getQuery()).isEqualTo("query");
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA", "package$database1/typeB", "package$database2/typeA",
                "package$database2/typeB");
        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2",
                "package$database2/namespace1", "package$database2/namespace2");

        // Assert that the joinSpecProto is set correctly in the searchSpecProto
        assertThat(searchSpecProto.hasJoinSpec()).isTrue();

        JoinSpecProto joinSpecProto = searchSpecProto.getJoinSpec();
        assertThat(joinSpecProto.hasNestedSpec()).isTrue();
        assertThat(joinSpecProto.getParentPropertyExpression()).isEqualTo("this.qualifiedId()");
        assertThat(joinSpecProto.getChildPropertyExpression()).isEqualTo("childPropertyExpression");
        assertThat(joinSpecProto.getAggregationScoringStrategy())
                .isEqualTo(JoinSpecProto.AggregationScoringStrategy.Code.SUM);
        assertThat(joinSpecProto.getMaxJoinedChildCount()).isEqualTo(10);

        JoinSpecProto.NestedSpecProto nestedSpecProto = joinSpecProto.getNestedSpec();
        assertThat(nestedSpecProto.getSearchSpec().getQuery()).isEqualTo("nestedQuery");
        assertThat(nestedSpecProto.getScoringSpec().getRankBy()).isEqualTo(
                ScoringSpecProto.RankingStrategy.Code.CREATION_TIMESTAMP);
    }

    @Test
    public void testToSearchSpec_withJoinSpec_childSearchesOtherSchema() throws Exception {
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .addFilterPackageNames("package")
                        .addFilterSchemas("typeA")
                        .build();
        SearchSpec.Builder searchSpec =
                new SearchSpec.Builder()
                        .addFilterPackageNames("package")
                        .addFilterSchemas("typeB");

        // Create a JoinSpec object and set it in the converter
        JoinSpec joinSpec =
                new JoinSpec.Builder("childPropertyExpression")
                        .setNestedSearch("nestedQuery", nestedSearchSpec)
                        .setMaxJoinedResultCount(10)
                        .build();

        searchSpec.setJoinSpec(joinSpec);

        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSpecToProtoConverter converter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/ "query",
                        searchSpec.build(),
                        /*prefixes=*/ ImmutableSet.of(prefix1, prefix2),
                        /*namespaceMap=*/ ImmutableMap.of(
                        prefix1,
                        ImmutableSet.of(
                                prefix1 + "namespace1", prefix1 + "namespace2"),
                        prefix2,
                        ImmutableSet.of(
                                prefix2 + "namespace1", prefix2 + "namespace2")),
                        /*schemaMap=*/ ImmutableMap.of(
                        prefix1,
                        ImmutableMap.of(
                                prefix1 + "typeA", configProto,
                                prefix1 + "typeB", configProto),
                        prefix2,
                        ImmutableMap.of(
                                prefix2 + "typeA", configProto,
                                prefix2 + "typeB", configProto)));

        VisibilityStore visibilityStore = new VisibilityStore(mAppSearchImpl);
        converter.removeInaccessibleSchemaFilter(
                new CallerAccess(/*callingPackageName=*/"package"),
                visibilityStore,
                AppSearchTestUtils.createMockVisibilityChecker(
                        /*visiblePrefixedSchemas=*/ ImmutableSet.of(
                                prefix1 + "typeA", prefix1 + "typeB", prefix2 + "typeA",
                                prefix2 + "typeB")));

        // Convert SearchSpec to proto.
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();

        assertThat(searchSpecProto.getQuery()).isEqualTo("query");
        assertThat(searchSpecProto.getSchemaTypeFiltersList())
                .containsExactly(
                        "package$database1/typeB",
                        "package$database2/typeB");
        assertThat(searchSpecProto.getNamespaceFiltersList())
                .containsExactly(
                        "package$database1/namespace1", "package$database1/namespace2",
                        "package$database2/namespace1", "package$database2/namespace2");

        // Assert that the joinSpecProto is set correctly in the searchSpecProto
        assertThat(searchSpecProto.hasJoinSpec()).isTrue();

        JoinSpecProto joinSpecProto = searchSpecProto.getJoinSpec();
        assertThat(joinSpecProto.hasNestedSpec()).isTrue();

        JoinSpecProto.NestedSpecProto nestedSpecProto = joinSpecProto.getNestedSpec();
        assertThat(nestedSpecProto.getSearchSpec().getSchemaTypeFiltersList())
                .containsExactly(
                        "package$database1/typeA",
                        "package$database2/typeA");
    }

    @Test
    public void testToScoringSpecProto() {
        String prefix = PrefixUtil.createPrefix("package", "database1");
        String schemaType = "schemaType";
        String namespace = "namespace";
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setOrder(ORDER_ASCENDING)
                .setRankingStrategy(RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeights(schemaType, ImmutableMap.of("property1", 2.0)).build();

        ScoringSpecProto scoringSpecProto = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec, /*prefixes=*/ImmutableSet.of(prefix),
                /*namespaceMap=*/ImmutableMap.of(prefix, ImmutableSet.of(prefix + namespace)),
                /*schemaMap=*/ImmutableMap.of(prefix, ImmutableMap.of(prefix + schemaType,
                SchemaTypeConfigProto.getDefaultInstance()))).toScoringSpecProto();
        TypePropertyWeights typePropertyWeights = TypePropertyWeights.newBuilder()
                .setSchemaType(prefix + schemaType)
                .addPropertyWeights(PropertyWeight.newBuilder()
                        .setPath("property1")
                        .setWeight(2.0)
                        .build())
                .build();

        assertThat(scoringSpecProto.getOrderBy().getNumber())
                .isEqualTo(ScoringSpecProto.Order.Code.ASC_VALUE);
        assertThat(scoringSpecProto.getRankBy().getNumber())
                .isEqualTo(ScoringSpecProto.RankingStrategy.Code.RELEVANCE_SCORE.getNumber());
        assertThat(scoringSpecProto.getTypePropertyWeightsList()).containsExactly(
                typePropertyWeights);
    }

    @Test
    public void testToAdvancedRankingSpecProto() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setOrder(ORDER_ASCENDING)
                .setRankingStrategy("this.documentScore()").build();

        ScoringSpecProto scoringSpecProto = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(),
                /*namespaceMap=*/ImmutableMap.of(),
                /*schemaMap=*/ImmutableMap.of()).toScoringSpecProto();

        assertThat(scoringSpecProto.getOrderBy().getNumber())
                .isEqualTo(ScoringSpecProto.Order.Code.ASC_VALUE);
        assertThat(scoringSpecProto.getRankBy().getNumber())
                .isEqualTo(ScoringSpecProto.RankingStrategy.Code.ADVANCED_SCORING_EXPRESSION_VALUE);
        assertThat(scoringSpecProto.getAdvancedScoringExpression())
                .isEqualTo("this.documentScore()");
    }

    @Test
    public void testToResultSpecProto() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultCountPerPage(123)
                .setSnippetCount(234)
                .setSnippetCountPerProperty(345)
                .setMaxSnippetSize(456)
                .build();

        SearchSpecToProtoConverter convert = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(),
                /*namespaceMap=*/ImmutableMap.of(),
                /*schemaMap=*/ImmutableMap.of());
        ResultSpecProto resultSpecProto = convert.toResultSpecProto(
                /*namespaceMap=*/ImmutableMap.of(),
                /*schemaMap=*/ImmutableMap.of());

        assertThat(resultSpecProto.getNumPerPage()).isEqualTo(123);
        assertThat(resultSpecProto.getSnippetSpec().getNumToSnippet()).isEqualTo(234);
        assertThat(resultSpecProto.getSnippetSpec().getNumMatchesPerProperty()).isEqualTo(345);
        assertThat(resultSpecProto.getSnippetSpec().getMaxWindowUtf32Length()).isEqualTo(456);
    }

    @Test
    public void testToResultSpecProto_groupByPackage() {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(),
                /*schemaMap=*/ImmutableMap.of());
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(
                /*namespaceMap=*/ImmutableMap.of(
                        prefix1, ImmutableSet.of(
                                prefix1 + "namespaceA",
                                prefix1 + "namespaceB"),
                        prefix2, ImmutableSet.of(
                                prefix2 + "namespaceA",
                                prefix2 + "namespaceB")),
                /*schemaMap=*/ImmutableMap.of());

        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(2);
        // First grouping should have same package name.
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        assertThat(grouping1.getMaxResults()).isEqualTo(5);
        assertThat(grouping1.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.getPackageName(grouping1.getEntryGroupings(0).getNamespace()))
                .isEqualTo(
                        PrefixUtil.getPackageName(grouping1.getEntryGroupings(1).getNamespace()));

        // Second grouping should have same package name.
        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        assertThat(grouping2.getMaxResults()).isEqualTo(5);
        assertThat(grouping2.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.getPackageName(grouping2.getEntryGroupings(0).getNamespace()))
                .isEqualTo(
                        PrefixUtil.getPackageName(grouping2.getEntryGroupings(1).getNamespace()));
    }

    @Test
    public void testToResultSpecProto_groupByNamespace() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        Map<String, Set<String>> namespaceMap = ImmutableMap.of(
                prefix1, ImmutableSet.of(
                        prefix1 + "namespaceA",
                        prefix1 + "namespaceB"),
                prefix2, ImmutableSet.of(
                        prefix2 + "namespaceA",
                        prefix2 + "namespaceB"));
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                namespaceMap,
                /*schemaMap=*/ImmutableMap.of());
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(
                namespaceMap,
                /*schemaMap=*/ImmutableMap.of());

        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(2);
        // First grouping should have same namespace.
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        assertThat(grouping1.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.removePrefix(grouping1.getEntryGroupings(0).getNamespace()))
                .isEqualTo(
                        PrefixUtil.removePrefix(grouping1.getEntryGroupings(1).getNamespace()));

        // Second grouping should have same namespace.
        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        assertThat(grouping2.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.removePrefix(grouping2.getEntryGroupings(0).getNamespace()))
                .isEqualTo(
                        PrefixUtil.removePrefix(grouping2.getEntryGroupings(1).getNamespace()));
    }

    @Test
    public void testToResultSpecProto_groupBySchema() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_SCHEMA, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix1, ImmutableMap.of(
                    prefix1 + "typeA", configProto,
                    prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                    prefix2 + "typeA", configProto,
                    prefix2 + "typeB", configProto));

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(),
                schemaMap);
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(
                /*namespaceMap=*/ImmutableMap.of(),
                schemaMap);

        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(2);
        // First grouping should have the same schema type.
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        assertThat(grouping1.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.removePrefix(grouping1.getEntryGroupings(0).getSchema()))
                .isEqualTo(
                    PrefixUtil.removePrefix(grouping1.getEntryGroupings(1).getSchema()));

        // Second grouping should have the same schema type.
        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        assertThat(grouping2.getEntryGroupingsList()).hasSize(2);
        assertThat(
                PrefixUtil.removePrefix(grouping2.getEntryGroupings(0).getSchema()))
                .isEqualTo(
                    PrefixUtil.removePrefix(grouping2.getEntryGroupings(1).getSchema()));
    }

    @Test
    public void testToResultSpecProto_groupByNamespaceAndPackage() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(GROUPING_TYPE_PER_PACKAGE
                    | SearchSpec.GROUPING_TYPE_PER_NAMESPACE, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        Map<String, Set<String>> namespaceMap = /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of(
                        prefix1 + "namespaceA",
                        prefix1 + "namespaceB"),
                prefix2, ImmutableSet.of(
                        prefix2 + "namespaceA",
                        prefix2 + "namespaceB"));

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                namespaceMap, /*schemaMap=*/ImmutableMap.of());
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(
                namespaceMap,
                /*schemaMap=*/ImmutableMap.of());

        // All namespace should be separated.
        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(4);
        assertThat(resultSpecProto.getResultGroupings(0).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(1).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(2).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(3).getEntryGroupingsList()).hasSize(1);
    }

    @Test
    public void testToResultSpecProto_groupBySchemaAndPackage() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(GROUPING_TYPE_PER_PACKAGE
                    | SearchSpec.GROUPING_TYPE_PER_SCHEMA, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix1, ImmutableMap.of(
                    prefix1 + "typeA", configProto,
                    prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                    prefix2 + "typeA", configProto,
                    prefix2 + "typeB", configProto));

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(),
                schemaMap);
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(
                /*namespaceMap=*/ImmutableMap.of(),
                schemaMap);

        // All schema should be separated.
        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(4);
        assertThat(resultSpecProto.getResultGroupings(0).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(1).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(2).getEntryGroupingsList()).hasSize(1);
        assertThat(resultSpecProto.getResultGroupings(3).getEntryGroupingsList()).hasSize(1);
    }

    @Test
    public void testToResultSpecProto_groupByNamespaceAndSchema() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                    | SearchSpec.GROUPING_TYPE_PER_SCHEMA, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        Map<String, Set<String>> namespaceMap = /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of(
                    prefix1 + "namespaceA",
                    prefix1 + "namespaceB"),
                prefix2, ImmutableSet.of(
                    prefix2 + "namespaceA",
                    prefix2 + "namespaceB"));
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix1, ImmutableMap.of(
                    prefix1 + "typeA", configProto,
                    prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                    prefix2 + "typeA", configProto,
                    prefix2 + "typeB", configProto));

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                namespaceMap,
                schemaMap);
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(namespaceMap, schemaMap);

        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(4);
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        // Each grouping should have a size of 2.
        assertThat(grouping1.getEntryGroupingsList()).hasSize(2);
        // Each grouping should have the same namespace and schema type.
        // Each entry should have the same package and database.
        assertThat(grouping1.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceA");
        assertThat(grouping1.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeA");
        assertThat(grouping1.getEntryGroupings(1).getNamespace())
                .isEqualTo("package2$database/namespaceA");
        assertThat(grouping1.getEntryGroupings(1).getSchema())
                .isEqualTo("package2$database/typeA");

        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        // Each grouping should have a size of 2.
        assertThat(grouping2.getEntryGroupingsList()).hasSize(2);
        // Each grouping should have the same namespace and schema type.
        // Each entry should have the same package and database.
        assertThat(grouping2.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceA");
        assertThat(grouping2.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeB");
        assertThat(grouping2.getEntryGroupings(1).getNamespace())
                .isEqualTo("package2$database/namespaceA");
        assertThat(grouping2.getEntryGroupings(1).getSchema())
                .isEqualTo("package2$database/typeB");

        ResultSpecProto.ResultGrouping grouping3 = resultSpecProto.getResultGroupings(2);
        // Each grouping should have a size of 2.
        assertThat(grouping3.getEntryGroupingsList()).hasSize(2);
        // Each grouping should have the same namespace and schema type.
        // Each entry should have the same package and database.
        assertThat(grouping3.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceB");
        assertThat(grouping3.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeA");
        assertThat(grouping3.getEntryGroupings(1).getNamespace())
                .isEqualTo("package2$database/namespaceB");
        assertThat(grouping3.getEntryGroupings(1).getSchema())
                .isEqualTo("package2$database/typeA");

        ResultSpecProto.ResultGrouping grouping4 = resultSpecProto.getResultGroupings(3);
        // Each grouping should have a size of 2.
        assertThat(grouping4.getEntryGroupingsList()).hasSize(2);
        // Each grouping should have the same namespace and schema type.
        // Each entry should have the same package and database.
        assertThat(grouping4.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceB");
        assertThat(grouping4.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeB");
        assertThat(grouping4.getEntryGroupings(1).getNamespace())
                .isEqualTo("package2$database/namespaceB");
        assertThat(grouping4.getEntryGroupings(1).getSchema())
                .isEqualTo("package2$database/typeB");
    }

    @Test
    public void testToResultSpecProto_groupByNamespaceAndSchemaAndPackage() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_PACKAGE
                    | SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                    | SearchSpec.GROUPING_TYPE_PER_SCHEMA, 5)
                .build();
        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        Map<String, Set<String>> namespaceMap = /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of(
                    prefix1 + "namespaceA",
                    prefix1 + "namespaceB"),
                prefix2, ImmutableSet.of(
                    prefix2 + "namespaceA",
                    prefix2 + "namespaceB"));
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix1, ImmutableMap.of(
                    prefix1 + "typeA", configProto,
                    prefix1 + "typeB", configProto),
                prefix2, ImmutableMap.of(
                    prefix2 + "typeA", configProto,
                    prefix2 + "typeB", configProto));

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"query",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                namespaceMap,
                schemaMap);
        ResultSpecProto resultSpecProto = converter.toResultSpecProto(namespaceMap, schemaMap);

        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(8);
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        //assertThat(grouping1.getEntryGroupingsList()).containsExactly();
        // Each grouping should have the size of 1.
        assertThat(grouping1.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping1.getEntryGroupings(0).getNamespace())
                .isEqualTo("package2$database/namespaceA");
        assertThat(grouping1.getEntryGroupings(0).getSchema())
                .isEqualTo("package2$database/typeA");

        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        // Each grouping should have the size of 1.
        assertThat(grouping2.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping2.getEntryGroupings(0).getNamespace())
                .isEqualTo("package2$database/namespaceA");
        assertThat(grouping2.getEntryGroupings(0).getSchema())
                .isEqualTo("package2$database/typeB");

        ResultSpecProto.ResultGrouping grouping3 = resultSpecProto.getResultGroupings(2);
        // Each grouping should have the size of 1.
        assertThat(grouping3.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping3.getEntryGroupings(0).getNamespace())
                .isEqualTo("package2$database/namespaceB");
        assertThat(grouping3.getEntryGroupings(0).getSchema())
                .isEqualTo("package2$database/typeA");

        ResultSpecProto.ResultGrouping grouping4 = resultSpecProto.getResultGroupings(3);
        // Each grouping should have the size of 1.
        assertThat(grouping4.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping4.getEntryGroupings(0).getNamespace())
                .isEqualTo("package2$database/namespaceB");
        assertThat(grouping4.getEntryGroupings(0).getSchema())
                .isEqualTo("package2$database/typeB");

        ResultSpecProto.ResultGrouping grouping5 = resultSpecProto.getResultGroupings(4);
        // Each grouping should have the size of 1.
        assertThat(grouping5.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping5.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceA");
        assertThat(grouping5.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeA");

        ResultSpecProto.ResultGrouping grouping6 = resultSpecProto.getResultGroupings(5);
        // Each grouping should have the size of 1.
        assertThat(grouping6.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping6.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceA");
        assertThat(grouping6.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeB");

        ResultSpecProto.ResultGrouping grouping7 = resultSpecProto.getResultGroupings(6);
        // Each grouping should have the size of 1.
        assertThat(grouping7.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping7.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceB");
        assertThat(grouping7.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeA");

        ResultSpecProto.ResultGrouping grouping8 = resultSpecProto.getResultGroupings(7);
        // Each grouping should have the size of 1.
        assertThat(grouping8.getEntryGroupingsList()).hasSize(1);
        // Each entry should have the same package.
        assertThat(grouping8.getEntryGroupings(0).getNamespace())
                .isEqualTo("package1$database/namespaceB");
        assertThat(grouping8.getEntryGroupings(0).getSchema())
                .isEqualTo("package1$database/typeB");
    }

    @Test
    public void testGetTargetNamespaceFilters_emptySearchingFilter() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");
        // search both prefixes
        Map<String, Set<String>> namespaceMap = ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1",
                        "package$database1/namespace2"),
                prefix2, ImmutableSet.of("package$database2/namespace3",
                        "package$database2/namespace4"));
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                namespaceMap, /*schemaMap=*/ImmutableMap.of());

        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();

        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2",
                "package$database2/namespace3", "package$database2/namespace4");
    }

    @Test
    public void testGetTargetNamespaceFilters_searchPartialPrefix() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");

        // Only search for prefix1
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1",
                        "package$database1/namespace2"),
                prefix2, ImmutableSet.of("package$database2/namespace3",
                        "package$database2/namespace4")),
                /*schemaMap=*/ImmutableMap.of());

        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // Only search prefix1 will return namespace 1 and 2.
        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2");
    }

    @Test
    public void testGetTargetNamespaceFilters_intersectionWithSearchingFilter() {
        // Put some searching namespaces.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterNamespaces("namespace1", "nonExist").build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1",
                        "package$database1/namespace2")),
                /*schemaMap=*/ImmutableMap.of());
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // If the searching namespace filter is not empty, the target namespace filter will be the
        // intersection of the searching namespace filters that users want to search over and
        // those candidates which are stored in AppSearch.
        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1");
    }

    @Test
    public void testGetTargetNamespaceFilters_intersectionWithNonExistFilter() {
        // Search in non-exist namespaces
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterNamespaces("nonExist").build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1",
                        "package$database1/namespace2")),
                /*schemaMap=*/ImmutableMap.of());
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // If the searching namespace filter is not empty, the target namespace filter will be the
        // intersection of the searching namespace filters that users want to search over and
        // those candidates which are stored in AppSearch.
        assertThat(searchSpecProto.getNamespaceFiltersList()).isEmpty();
    }

    @Test
    public void testGetTargetSchemaFilters_emptySearchingFilter() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = createPrefix("package", "database1");
        String prefix2 = createPrefix("package", "database2");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        "package$database1/typeA", schemaTypeConfigProto,
                        "package$database1/typeB", schemaTypeConfigProto),
                prefix2, ImmutableMap.of(
                        "package$database2/typeC", schemaTypeConfigProto,
                        "package$database2/typeD", schemaTypeConfigProto)));
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // Empty searching filter will get all types for target filter
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA", "package$database1/typeB",
                "package$database2/typeC", "package$database2/typeD");
    }

    @Test
    public void testGetTargetSchemaFilters_searchPartialFilter() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = createPrefix("package", "database1");
        String prefix2 = createPrefix("package", "database2");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        // only search in prefix1
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        "package$database1/typeA", schemaTypeConfigProto,
                        "package$database1/typeB", schemaTypeConfigProto),
                prefix2, ImmutableMap.of(
                        "package$database2/typeC", schemaTypeConfigProto,
                        "package$database2/typeD", schemaTypeConfigProto)));
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // Only search prefix1 will return typeA and B.
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA", "package$database1/typeB");
    }

    @Test
    public void testGetTargetSchemaFilters_intersectionWithSearchingFilter() {
        // Put some searching schemas.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterSchemas("typeA", "nonExist").build();
        String prefix1 = createPrefix("package", "database1");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        "package$database1/typeA", schemaTypeConfigProto,
                        "package$database1/typeB", schemaTypeConfigProto)));
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // If the searching schema filter is not empty, the target schema filter will be the
        // intersection of the schema filters that users want to search over and those candidates
        // which are stored in AppSearch.
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA");
    }

    @Test
    public void testGetTargetSchemaFilters_intersectionWithNonExistFilter() {
        // Put non-exist searching schema.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterSchemas("nonExist").build();
        String prefix1 = createPrefix("package", "database1");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                searchSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                /*namespaceMap=*/ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        "package$database1/typeA", schemaTypeConfigProto,
                        "package$database1/typeB", schemaTypeConfigProto)));
        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // If there is no intersection of the schema filters that user want to search over and
        // those filters which are stored in AppSearch, return empty.
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).isEmpty();
    }

    @Test
    public void testRemoveInaccessibleSchemaFilter() throws Exception {
        VisibilityStore visibilityStore = new VisibilityStore(mAppSearchImpl);

        final String prefix = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();

        SearchSpec nestedSearchSpec = new SearchSpec.Builder().build();
        JoinSpec joinSpec = new JoinSpec.Builder("entity")
                .setNestedSearch("", nestedSearchSpec).build();

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                new SearchSpec.Builder().setJoinSpec(joinSpec).build(),
                /*prefixes=*/ImmutableSet.of(prefix),
                /*namespaceMap=*/ImmutableMap.of(
                prefix, ImmutableSet.of("package$database/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix, ImmutableMap.of(
                        "package$database/schema1", schemaTypeConfigProto,
                        "package$database/schema2", schemaTypeConfigProto,
                        "package$database/schema3", schemaTypeConfigProto)));

        converter.removeInaccessibleSchemaFilter(
                new CallerAccess(/*callingPackageName=*/"otherPackageName"),
                visibilityStore,
                AppSearchTestUtils.createMockVisibilityChecker(
                        /*visiblePrefixedSchemas=*/ ImmutableSet.of(
                                prefix + "schema1", prefix + "schema3")));

        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        // schema 2 is filtered out since it is not searchable for user.
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                prefix + "schema1", prefix + "schema3");

        SearchSpecProto nestedSearchProto =
                searchSpecProto.getJoinSpec().getNestedSpec().getSearchSpec();
        assertThat(nestedSearchProto.getSchemaTypeFiltersList()).containsExactly(
                prefix + "schema1", prefix + "schema3");
    }

    @Test
    public void testIsNothingToSearch() {
        String prefix = PrefixUtil.createPrefix("package", "database");
        SearchSpec nestedSearchSpec = new SearchSpec.Builder().build();
        JoinSpec joinSpec = new JoinSpec.Builder("entity")
                .setNestedSearch("nested", nestedSearchSpec).build();
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setJoinSpec(joinSpec)
                .addFilterSchemas("schema").addFilterNamespaces("namespace").build();

        // build maps
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix, ImmutableMap.of(
                        "package$database/schema", schemaTypeConfigProto)
        );
        Map<String, Set<String>> namespaceMap = ImmutableMap.of(
                prefix, ImmutableSet.of("package$database/namespace")
        );

        SearchSpecToProtoConverter emptySchemaConverter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/"",
                        searchSpec, /*prefixes=*/ImmutableSet.of(prefix),
                        /*namespaceMap=*/namespaceMap,
                        /*schemaMap=*/ImmutableMap.of());
        assertThat(emptySchemaConverter.hasNothingToSearch()).isTrue();

        SearchSpecToProtoConverter emptyNamespaceConverter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/"",
                        searchSpec, /*prefixes=*/ImmutableSet.of(prefix),
                        /*namespaceMap=*/ImmutableMap.of(),
                        schemaMap);
        assertThat(emptyNamespaceConverter.hasNothingToSearch()).isTrue();

        SearchSpecToProtoConverter nonEmptyConverter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/"",
                        searchSpec, /*prefixes=*/ImmutableSet.of(prefix),
                        namespaceMap, schemaMap);
        assertThat(nonEmptyConverter.hasNothingToSearch()).isFalse();

        // remove all target schema filter, and the query becomes nothing to search.
        nonEmptyConverter.removeInaccessibleSchemaFilter(
                new CallerAccess(/*callingPackageName=*/"otherPackageName"),
                /*visibilityStore=*/null,
                /*visibilityChecker=*/null);
        assertThat(nonEmptyConverter.hasNothingToSearch()).isTrue();
        // As the JoinSpec has nothing to search, it should not be part of the SearchSpec
        assertThat(nonEmptyConverter.toSearchSpecProto().hasJoinSpec()).isFalse();
    }

    @Test
    public void testRemoveInaccessibleSchemaFilterWithEmptyNestedFilter() throws Exception {
        VisibilityStore visibilityStore = new VisibilityStore(mAppSearchImpl);

        final String prefix = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();

        SearchSpec nestedSearchSpec = new SearchSpec.Builder()
                .addFilterSchemas(ImmutableSet.of(prefix + "schema1", prefix + "schema2"))
                .build();
        JoinSpec joinSpec = new JoinSpec.Builder("entity")
                .setNestedSearch("nested", nestedSearchSpec).build();

        SearchSpecToProtoConverter converter = new SearchSpecToProtoConverter(
                /*queryExpression=*/"",
                new SearchSpec.Builder().setJoinSpec(joinSpec).build(),
                /*prefixes=*/ImmutableSet.of(prefix),
                /*namespaceMap=*/ImmutableMap.of(
                prefix, ImmutableSet.of("package$database/namespace1")),
                /*schemaMap=*/ImmutableMap.of(
                prefix, ImmutableMap.of(
                        "package$database/schema1", schemaTypeConfigProto,
                        "package$database/schema2", schemaTypeConfigProto,
                        "package$database/schema3", schemaTypeConfigProto)));

        converter.removeInaccessibleSchemaFilter(
                new CallerAccess(/*callingPackageName=*/"otherPackageName"),
                visibilityStore,
                AppSearchTestUtils.createMockVisibilityChecker(
                        /*visiblePrefixedSchemas=*/ ImmutableSet.of(prefix + "schema3")));

        SearchSpecProto searchSpecProto = converter.toSearchSpecProto();
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(prefix + "schema3");

        // Schema 1 and 2 are filtered out of the nested spec. As the JoinSpec has nothing to
        // search, it should not be part of the SearchSpec.
        assertThat(searchSpecProto.hasJoinSpec()).isFalse();
    }

    @Test
    public void testConvertPropertyWeights() {
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        String prefix2 = PrefixUtil.createPrefix("package", "database2");
        String schemaTypeA = "typeA";
        String schemaTypeB = "typeB";

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setPropertyWeights(schemaTypeA, ImmutableMap.of("property1", 1.0, "property2",
                        2.0))
                .setPropertyWeights(schemaTypeB, ImmutableMap.of("nested.property", 0.5))
                .build();

        Map<String, Set<String>> namespaceMap = ImmutableMap.of(
                prefix1, ImmutableSet.of(prefix1 + "namespace1"),
                prefix2, ImmutableSet.of(prefix2 + "namespace1")
        );
        Map<String, Map<String, SchemaTypeConfigProto>> schemaTypeMap = ImmutableMap.of(
                prefix1,
                ImmutableMap.of(prefix1 + schemaTypeA, SchemaTypeConfigProto.getDefaultInstance(),
                        prefix1 + schemaTypeB, SchemaTypeConfigProto.getDefaultInstance()),
                prefix2,
                ImmutableMap.of(prefix2 + schemaTypeA, SchemaTypeConfigProto.getDefaultInstance())
        );

        SearchSpecToProtoConverter converter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/"",
                        searchSpec, /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                        namespaceMap,
                        schemaTypeMap);

        TypePropertyWeights expectedTypePropertyWeight1 =
                TypePropertyWeights.newBuilder().setSchemaType(prefix1 + schemaTypeA)
                        .addPropertyWeights(PropertyWeight.newBuilder()
                                .setPath("property1")
                                .setWeight(1.0))
                        .addPropertyWeights(PropertyWeight.newBuilder()
                                .setPath("property2")
                                .setWeight(2.0))
                        .build();
        TypePropertyWeights expectedTypePropertyWeight2 =
                TypePropertyWeights.newBuilder().setSchemaType(prefix2 + schemaTypeA)
                        .addPropertyWeights(PropertyWeight.newBuilder()
                                .setPath("property1")
                                .setWeight(1.0))
                        .addPropertyWeights(PropertyWeight.newBuilder()
                                .setPath("property2")
                                .setWeight(2.0))
                        .build();
        TypePropertyWeights expectedTypePropertyWeight3 =
                TypePropertyWeights.newBuilder().setSchemaType(prefix1 + schemaTypeB)
                        .addPropertyWeights(PropertyWeight.newBuilder()
                                .setPath("nested.property")
                                .setWeight(0.5))
                        .build();

        List<TypePropertyWeights> convertedTypePropertyWeights =
                converter.toScoringSpecProto().getTypePropertyWeightsList();

        assertThat(convertedTypePropertyWeights).containsExactly(expectedTypePropertyWeight1,
                expectedTypePropertyWeight2, expectedTypePropertyWeight3);
    }

    @Test
    public void testConvertPropertyWeights_whenNoWeightsSet() {
        SearchSpec searchSpec = new SearchSpec.Builder().build();
        String prefix1 = PrefixUtil.createPrefix("package", "database1");
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();

        SearchSpecToProtoConverter converter =
                new SearchSpecToProtoConverter(
                        /*queryExpression=*/"",
                        searchSpec, /*prefixes=*/ImmutableSet.of(prefix1),
                        /*namespaceMap=*/ImmutableMap.of(
                        prefix1,
                        ImmutableSet.of(prefix1 + "namespace1")),
                        /*schemaMap=*/ImmutableMap.of(
                        prefix1,
                        ImmutableMap.of(prefix1 + "typeA", schemaTypeConfigProto)));

        ScoringSpecProto convertedScoringSpecProto = converter.toScoringSpecProto();

        assertThat(convertedScoringSpecProto.getTypePropertyWeightsList()).isEmpty();
    }
}
