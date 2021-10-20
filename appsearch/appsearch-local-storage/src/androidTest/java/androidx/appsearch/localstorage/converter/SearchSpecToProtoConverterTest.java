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
import static androidx.appsearch.app.SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class SearchSpecToProtoConverterTest {

    @Test
    public void testToSearchSpecProto() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder().build();

        // Convert SearchSpec to proto.
        SearchSpecProto searchSpecProto = SearchSpecToProtoConverter.toSearchSpecProto(
                /*queryExpression=*/"query", searchSpec,
                ImmutableSet.of("package$database1/typeA", "package$database1/typeB",
                        "package$database2/typeA", "package$database2/typeB"),
                ImmutableSet.of("package$database1/namespace1", "package$database1/namespace2",
                        "package$database2/namespace1", "package$database2/namespace2"));

        assertThat(searchSpecProto.getQuery()).isEqualTo("query");
        assertThat(searchSpecProto.getSchemaTypeFiltersList()).containsExactly(
                "package$database1/typeA", "package$database1/typeB", "package$database2/typeA",
                "package$database2/typeB");
        assertThat(searchSpecProto.getNamespaceFiltersList()).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2",
                "package$database2/namespace1", "package$database2/namespace2");
    }

    @Test
    public void testToScoringSpecProto()  {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setOrder(ORDER_ASCENDING)
                .setRankingStrategy(RANKING_STRATEGY_CREATION_TIMESTAMP).build();

        ScoringSpecProto scoringSpecProto =
                SearchSpecToProtoConverter.toScoringSpecProto(searchSpec);

        assertThat(scoringSpecProto.getOrderBy().getNumber())
                .isEqualTo(ScoringSpecProto.Order.Code.ASC_VALUE);
        assertThat(scoringSpecProto.getRankBy().getNumber())
                .isEqualTo(ScoringSpecProto.RankingStrategy.Code.CREATION_TIMESTAMP_VALUE);
    }

    @Test
    public void testToResultSpecProto()  {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultCountPerPage(123)
                .setSnippetCount(234)
                .setSnippetCountPerProperty(345)
                .setMaxSnippetSize(456)
                .build();

        ResultSpecProto resultSpecProto =
                SearchSpecToProtoConverter.toResultSpecProto(searchSpec,
                        /*prefixes=*/ImmutableSet.of(),
                        /*targetPrefixedSchemaFilters=*/ImmutableSet.of(),
                        /*namespaceMap=*/ImmutableMap.of());

        assertThat(resultSpecProto.getNumPerPage()).isEqualTo(123);
        assertThat(resultSpecProto.getSnippetSpec().getNumToSnippet()).isEqualTo(234);
        assertThat(resultSpecProto.getSnippetSpec().getNumMatchesPerProperty()).isEqualTo(345);
        assertThat(resultSpecProto.getSnippetSpec().getMaxWindowBytes()).isEqualTo(456);
    }

    @Test
    public void testToResultSpecProto_groupByPackage()  {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        ResultSpecProto resultSpecProto =
                SearchSpecToProtoConverter.toResultSpecProto(searchSpec,
                        /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                        /*targetPrefixedSchemaFilters=*/ImmutableSet.of(),
                        /*namespaceMap=*/ImmutableMap.of(
                                prefix1, ImmutableSet.of(
                                        prefix1 + "namespaceA",
                                        prefix1 + "namespaceB"),
                                prefix2, ImmutableSet.of(
                                        prefix2 + "namespaceA",
                                        prefix2 + "namespaceB")));
        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(2);
        // First grouping should have same package name.
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        assertThat(grouping1.getMaxResults()).isEqualTo(5);
        assertThat(grouping1.getNamespacesCount()).isEqualTo(2);
        assertThat(
                PrefixUtil.getPackageName(grouping1.getNamespaces(0)))
                .isEqualTo(
                        PrefixUtil.getPackageName(grouping1.getNamespaces(1)));

        // Second grouping should have same package name.
        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        assertThat(grouping2.getMaxResults()).isEqualTo(5);
        assertThat(grouping2.getNamespacesCount()).isEqualTo(2);
        assertThat(
                PrefixUtil.getPackageName(grouping2.getNamespaces(0)))
                .isEqualTo(
                        PrefixUtil.getPackageName(grouping2.getNamespaces(1)));
    }

    @Test
    public void testToResultSpecProto_groupByNamespace() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE, 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        ResultSpecProto resultSpecProto =
                SearchSpecToProtoConverter.toResultSpecProto(searchSpec,
                        /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                        /*targetPrefixedSchemaFilters=*/ImmutableSet.of(),
                        /*namespaceMap=*/ImmutableMap.of(
                                prefix1, ImmutableSet.of(
                                        prefix1 + "namespaceA",
                                        prefix1 + "namespaceB"),
                                prefix2, ImmutableSet.of(
                                        prefix2 + "namespaceA",
                                        prefix2 + "namespaceB")));
        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(2);
        // First grouping should have same namespace.
        ResultSpecProto.ResultGrouping grouping1 = resultSpecProto.getResultGroupings(0);
        assertThat(grouping1.getNamespacesCount()).isEqualTo(2);
        assertThat(
                PrefixUtil.removePrefix(grouping1.getNamespaces(0)))
                .isEqualTo(
                        PrefixUtil.removePrefix(grouping1.getNamespaces(1)));

        // Second grouping should have same namespace.
        ResultSpecProto.ResultGrouping grouping2 = resultSpecProto.getResultGroupings(1);
        assertThat(grouping2.getNamespacesCount()).isEqualTo(2);
        assertThat(
                PrefixUtil.removePrefix(grouping1.getNamespaces(0)))
                .isEqualTo(
                        PrefixUtil.removePrefix(grouping1.getNamespaces(1)));
    }

    @Test
    public void testToResultSpecProto_groupByNamespaceAndPackage() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setResultGrouping(GROUPING_TYPE_PER_PACKAGE
                        | SearchSpec.GROUPING_TYPE_PER_NAMESPACE , 5)
                .build();

        String prefix1 = PrefixUtil.createPrefix("package1", "database");
        String prefix2 = PrefixUtil.createPrefix("package2", "database");
        ResultSpecProto resultSpecProto =
                SearchSpecToProtoConverter.toResultSpecProto(searchSpec,
                        /*prefixes=*/ImmutableSet.of(prefix1, prefix2),
                        /*targetPrefixedSchemaFilters=*/ImmutableSet.of(),
                        /*namespaceMap=*/ImmutableMap.of(
                                prefix1, ImmutableSet.of(
                                        prefix1 + "namespaceA",
                                        prefix1 + "namespaceB"),
                                prefix2, ImmutableSet.of(
                                        prefix2 + "namespaceA",
                                        prefix2 + "namespaceB")));
        // All namespace should be separated.
        assertThat(resultSpecProto.getResultGroupingsCount()).isEqualTo(4);
        assertThat(resultSpecProto.getResultGroupings(0).getNamespacesCount()).isEqualTo(1);
        assertThat(resultSpecProto.getResultGroupings(1).getNamespacesCount()).isEqualTo(1);
        assertThat(resultSpecProto.getResultGroupings(2).getNamespacesCount()).isEqualTo(1);
        assertThat(resultSpecProto.getResultGroupings(3).getNamespacesCount()).isEqualTo(1);
    }
}
