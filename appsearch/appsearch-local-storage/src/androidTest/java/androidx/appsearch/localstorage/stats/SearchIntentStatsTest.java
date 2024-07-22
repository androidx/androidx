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

public class SearchIntentStatsTest {
    static final String TEST_PACKAGE_NAME = "package.test";
    static final String TEST_DATA_BASE = "testDataBase";

    @Test
    public void testBuilder() {
        String prevQuery = "prev";
        String currQuery = "curr";
        long searchIntentTimestampMillis = 1L;
        int numResultsFetched = 2;
        int queryCorrectionType = SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT;

        // Clicks associated with the search intent.
        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();

        final SearchIntentStats searchIntentStats =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery(prevQuery)
                        .setCurrQuery(currQuery)
                        .setTimestampMillis(searchIntentTimestampMillis)
                        .setNumResultsFetched(numResultsFetched)
                        .setQueryCorrectionType(queryCorrectionType)
                        .addClicksStats(clickStats0, clickStats1)
                        .build();

        assertThat(searchIntentStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats.getClicksStats()).containsExactly(clickStats0, clickStats1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        String prevQuery = "prev";
        String currQuery = "curr";
        long searchIntentTimestampMillis = 1L;
        int numResultsFetched = 2;
        int queryCorrectionType = SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT;

        // Clicks associated with the search intent.
        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();

        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery(prevQuery)
                        .setCurrQuery(currQuery)
                        .setTimestampMillis(searchIntentTimestampMillis)
                        .setNumResultsFetched(numResultsFetched)
                        .setQueryCorrectionType(queryCorrectionType)
                        .addClicksStats(clickStats0, clickStats1)
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(searchIntentStats0).build();

        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats1.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats1.getClicksStats()).containsExactly(clickStats0, clickStats1);
    }

    @Test
    public void testBuilderCopy_copiedFieldsCanBeUpdated() {
        // Clicks associated with the search intent.
        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();

        final SearchIntentStats searchIntentStats0 =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery("query1")
                        .setCurrQuery("query2")
                        .setTimestampMillis(1L)
                        .setNumResultsFetched(2)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT)
                        .addClicksStats(clickStats0, clickStats1)
                        .build();

        // Build another SearchIntentStats based on the previous one, with fields changed.
        final ClickStats clickStats2 =
                new ClickStats.Builder()
                        .setTimestampMillis(12L)
                        .setTimeStayOnResultMillis(22L)
                        .setResultRankInBlock(32)
                        .setResultRankGlobal(42)
                        .setIsGoodClick(true)
                        .build();
        final SearchIntentStats searchIntentStats1 =
                new SearchIntentStats.Builder(searchIntentStats0)
                        .setDatabase("database2")
                        .setPrevQuery("query3")
                        .setCurrQuery("query4")
                        .setTimestampMillis(2L)
                        .setNumResultsFetched(4)
                        .setQueryCorrectionType(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT)
                        .addClicksStats(clickStats2)
                        .build();

        // Check that searchIntentStats0 wasn't altered.
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats0.getPrevQuery()).isEqualTo("query1");
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("query2");
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1L);
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(2);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(searchIntentStats0.getClicksStats()).containsExactly(clickStats0, clickStats1);

        // Check that searchIntentStats1 has the new values.
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo("database2");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("query3");
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("query4");
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(2L);
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(4);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats())
                .containsExactly(clickStats0, clickStats1, clickStats2);
    }

    @Test
    public void testBuilder_addClicksStats_byCollection() {
        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();
        Set<ClickStats> clicksStats = ImmutableSet.of(clickStats0, clickStats1);

        final SearchIntentStats searchIntentStats =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .addClicksStats(clicksStats)
                        .build();

        assertThat(searchIntentStats.getClicksStats()).containsExactlyElementsIn(clicksStats);
    }

    @Test
    public void testBuilder_builderReuse() {
        String prevQuery = "prev";
        String currQuery = "curr";
        long searchIntentTimestampMillis = 1;
        int numResultsFetched = 2;
        int queryCorrectionType = SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT;

        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();

        SearchIntentStats.Builder builder =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery(prevQuery)
                        .setCurrQuery(currQuery)
                        .setTimestampMillis(searchIntentTimestampMillis)
                        .setNumResultsFetched(numResultsFetched)
                        .setQueryCorrectionType(queryCorrectionType)
                        .addClicksStats(clickStats0, clickStats1);

        final SearchIntentStats searchIntentStats0 = builder.build();

        final ClickStats clickStats2 =
                new ClickStats.Builder()
                        .setTimestampMillis(12L)
                        .setTimeStayOnResultMillis(22L)
                        .setResultRankInBlock(32)
                        .setResultRankGlobal(42)
                        .setIsGoodClick(true)
                        .build();
        builder.addClicksStats(clickStats2);

        final SearchIntentStats searchIntentStats1 = builder.build();

        // Check that searchIntentStats0 wasn't altered.
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats0.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats0.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats0.getClicksStats()).containsExactly(clickStats0, clickStats1);

        // Check that searchIntentStats1 has the new values.
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats1.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats1.getClicksStats())
                .containsExactly(clickStats0, clickStats1, clickStats2);
    }

    @Test
    public void testBuilder_builderReuse_byCollection() {
        String prevQuery = "prev";
        String currQuery = "curr";
        long searchIntentTimestampMillis = 1;
        int numResultsFetched = 2;
        int queryCorrectionType = SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT;

        final ClickStats clickStats0 =
                new ClickStats.Builder()
                        .setTimestampMillis(10L)
                        .setTimeStayOnResultMillis(20L)
                        .setResultRankInBlock(30)
                        .setResultRankGlobal(40)
                        .setIsGoodClick(false)
                        .build();
        final ClickStats clickStats1 =
                new ClickStats.Builder()
                        .setTimestampMillis(11L)
                        .setTimeStayOnResultMillis(21L)
                        .setResultRankInBlock(31)
                        .setResultRankGlobal(41)
                        .setIsGoodClick(true)
                        .build();

        SearchIntentStats.Builder builder =
                new SearchIntentStats.Builder(TEST_PACKAGE_NAME)
                        .setDatabase(TEST_DATA_BASE)
                        .setPrevQuery(prevQuery)
                        .setCurrQuery(currQuery)
                        .setTimestampMillis(searchIntentTimestampMillis)
                        .setNumResultsFetched(numResultsFetched)
                        .setQueryCorrectionType(queryCorrectionType)
                        .addClicksStats(ImmutableSet.of(clickStats0, clickStats1));

        final SearchIntentStats searchIntentStats0 = builder.build();

        final ClickStats clickStats2 =
                new ClickStats.Builder()
                        .setTimestampMillis(12L)
                        .setTimeStayOnResultMillis(22L)
                        .setResultRankInBlock(32)
                        .setResultRankGlobal(42)
                        .setIsGoodClick(true)
                        .build();
        builder.addClicksStats(ImmutableSet.of(clickStats2));

        final SearchIntentStats searchIntentStats1 = builder.build();

        // Check that searchIntentStats0 wasn't altered.
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats0.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats0.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats0.getClicksStats()).containsExactly(clickStats0, clickStats1);

        // Check that searchIntentStats1 has the new values.
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo(prevQuery);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo(currQuery);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(searchIntentTimestampMillis);
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(numResultsFetched);
        assertThat(searchIntentStats1.getQueryCorrectionType()).isEqualTo(queryCorrectionType);
        assertThat(searchIntentStats1.getClicksStats())
                .containsExactly(clickStats0, clickStats1, clickStats2);
    }
}
