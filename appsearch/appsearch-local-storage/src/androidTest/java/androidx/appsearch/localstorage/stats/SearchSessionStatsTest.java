/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Set;

public class SearchSessionStatsTest {
    static final String TEST_PACKAGE_NAME = "package.test";
    static final String TEST_DATA_BASE = "testDataBase";

    @Test
    public void testBuilder() {
        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("")
                        .setCurrQuery("query1")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(10L)
                                        .setTimeStayOnResultMillis(20L)
                                        .setResultRankInBlock(30)
                                        .setResultRankGlobal(40)
                                        .setIsGoodClick(false)
                                        .build(),
                                new ClickStats.Builder()
                                        .setTimestampMillis(11L)
                                        .setTimeStayOnResultMillis(21L)
                                        .setResultRankInBlock(31)
                                        .setResultRankGlobal(41)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(12L)
                                        .setTimeStayOnResultMillis(22L)
                                        .setResultRankInBlock(32)
                                        .setResultRankGlobal(42)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();

        final SearchSessionStats searchSessionStats =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addSearchIntentsStats(searchIntentStats0, searchIntentStats1)
                        .build();

        assertThat(searchSessionStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchSessionStats.getSearchIntentsStats())
                .containsExactly(searchIntentStats0, searchIntentStats1);
    }

    @Test
    public void testBuilder_addSearchIntentsStats_byCollection() {
        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("")
                        .setCurrQuery("query1")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(10L)
                                        .setTimeStayOnResultMillis(20L)
                                        .setResultRankInBlock(30)
                                        .setResultRankGlobal(40)
                                        .setIsGoodClick(false)
                                        .build(),
                                new ClickStats.Builder()
                                        .setTimestampMillis(11L)
                                        .setTimeStayOnResultMillis(21L)
                                        .setResultRankInBlock(31)
                                        .setResultRankGlobal(41)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(12L)
                                        .setTimeStayOnResultMillis(22L)
                                        .setResultRankInBlock(32)
                                        .setResultRankGlobal(42)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        Set<SearchIntentStats> searchIntentsStats =
                ImmutableSet.of(searchIntentStats0, searchIntentStats1);

        final SearchSessionStats searchSessionStats =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addSearchIntentsStats(searchIntentsStats)
                        .build();

        assertThat(searchSessionStats.getSearchIntentsStats())
                .containsExactlyElementsIn(searchIntentsStats);
    }

    @Test
    public void testBuilder_builderReuse() {
        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("")
                        .setCurrQuery("query1")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(10L)
                                        .setTimeStayOnResultMillis(20L)
                                        .setResultRankInBlock(30)
                                        .setResultRankGlobal(40)
                                        .setIsGoodClick(false)
                                        .build(),
                                new ClickStats.Builder()
                                        .setTimestampMillis(11L)
                                        .setTimeStayOnResultMillis(21L)
                                        .setResultRankInBlock(31)
                                        .setResultRankGlobal(41)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(12L)
                                        .setTimeStayOnResultMillis(22L)
                                        .setResultRankInBlock(32)
                                        .setResultRankGlobal(42)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();

        SearchSessionStats.Builder builder =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addSearchIntentsStats(searchIntentStats0, searchIntentStats1);

        final SearchSessionStats searchSessionStats0 = builder.build();

        final SearchIntentStats searchIntentStats2 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query2")
                        .setCurrQuery("query3")
                        .setTimestampMillis(3L)
                        .setNumResultsFetched(6)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(13L)
                                        .setTimeStayOnResultMillis(23L)
                                        .setResultRankInBlock(33)
                                        .setResultRankGlobal(43)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        builder.addSearchIntentsStats(searchIntentStats2);

        final SearchSessionStats searchSessionStats1 = builder.build();

        // Check that searchSessionStats0 wasn't altered.
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchSessionStats0.getSearchIntentsStats())
                .containsExactly(searchIntentStats0, searchIntentStats1);

        // Check that searchSessionStats1 has the new values.
        assertThat(searchSessionStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats1.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchSessionStats1.getSearchIntentsStats())
                .containsExactly(searchIntentStats0, searchIntentStats1, searchIntentStats2);
    }

    @Test
    public void testBuilder_builderReuse_byCollection() {
        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("")
                        .setCurrQuery("query1")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(10L)
                                        .setTimeStayOnResultMillis(20L)
                                        .setResultRankInBlock(30)
                                        .setResultRankGlobal(40)
                                        .setIsGoodClick(false)
                                        .build(),
                                new ClickStats.Builder()
                                        .setTimestampMillis(11L)
                                        .setTimeStayOnResultMillis(21L)
                                        .setResultRankInBlock(31)
                                        .setResultRankGlobal(41)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(12L)
                                        .setTimeStayOnResultMillis(22L)
                                        .setResultRankInBlock(32)
                                        .setResultRankGlobal(42)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();

        SearchSessionStats.Builder builder =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addSearchIntentsStats(
                                ImmutableSet.of(searchIntentStats0, searchIntentStats1));

        final SearchSessionStats searchSessionStats0 = builder.build();

        final SearchIntentStats searchIntentStats2 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query2")
                        .setCurrQuery("query3")
                        .setTimestampMillis(3L)
                        .setNumResultsFetched(6)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(13L)
                                        .setTimeStayOnResultMillis(23L)
                                        .setResultRankInBlock(33)
                                        .setResultRankGlobal(43)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        builder.addSearchIntentsStats(ImmutableSet.of(searchIntentStats2));

        final SearchSessionStats searchSessionStats1 = builder.build();

        // Check that searchSessionStats0 wasn't altered.
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchSessionStats0.getSearchIntentsStats())
                .containsExactly(searchIntentStats0, searchIntentStats1);

        // Check that searchSessionStats1 has the new values.
        assertThat(searchSessionStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats1.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchSessionStats1.getSearchIntentsStats())
                .containsExactly(searchIntentStats0, searchIntentStats1, searchIntentStats2);
    }

    @Test
    public void testGetEndSessionSearchIntentStats() {
        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("")
                        .setCurrQuery("query1")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(10L)
                                        .setTimeStayOnResultMillis(20L)
                                        .setResultRankInBlock(30)
                                        .setResultRankGlobal(40)
                                        .setIsGoodClick(false)
                                        .build(),
                                new ClickStats.Builder()
                                        .setTimestampMillis(11L)
                                        .setTimeStayOnResultMillis(21L)
                                        .setResultRankInBlock(31)
                                        .setResultRankGlobal(41)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(
                                new ClickStats.Builder()
                                        .setTimestampMillis(12L)
                                        .setTimeStayOnResultMillis(22L)
                                        .setResultRankInBlock(32)
                                        .setResultRankGlobal(42)
                                        .setIsGoodClick(true)
                                        .build())
                        .build();

        final SearchSessionStats searchSessionStats =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addSearchIntentsStats(searchIntentStats0, searchIntentStats1)
                        .build();

        SearchIntentStats endSessionSearchIntentStats =
                searchSessionStats.getEndSessionSearchIntentStats();
        // End session SearchIntentStats should be identical to the last added SearchIntentStats,
        // except the previous query is null and query correction type is
        // QUERY_CORRECTION_TYPE_END_SESSION.
        assertThat(endSessionSearchIntentStats).isNotNull();
        assertThat(endSessionSearchIntentStats.getPackageName())
                .isEqualTo(searchIntentStats1.getPackageName());
        assertThat(endSessionSearchIntentStats.getDatabase())
                .isEqualTo(searchIntentStats1.getDatabase());
        assertThat(endSessionSearchIntentStats.getCurrQuery())
                .isEqualTo(searchIntentStats1.getCurrQuery());
        assertThat(endSessionSearchIntentStats.getTimestampMillis())
                .isEqualTo(searchIntentStats1.getTimestampMillis());
        assertThat(endSessionSearchIntentStats.getNumResultsFetched())
                .isEqualTo(searchIntentStats1.getNumResultsFetched());
        assertThat(endSessionSearchIntentStats.getClicksStats())
                .containsExactlyElementsIn(searchIntentStats1.getClicksStats());

        assertThat(endSessionSearchIntentStats.getPrevQuery()).isNull();
        assertThat(endSessionSearchIntentStats.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_END_SESSION);
    }

    @Test
    public void testGetEndSessionSearchIntentStats_emptySearchIntentsShouldReturnNull() {
        // Create a SearchSessionStats without search intents.
        final SearchSessionStats searchSessionStats =
                new SearchSessionStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .build();

        assertThat(searchSessionStats.getEndSessionSearchIntentStats()).isNull();
    }
}
