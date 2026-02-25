/*
 * Copyright 2025 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.SearchFeatures;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Test;

public class SearchFeaturesCtsTest {

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE)
    public void testSetFeatureEnabledToFalse() {
        SearchFeatures.Builder builder = new SearchFeatures.Builder();
        SearchFeatures searchFeatures = builder.setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        assertThat(searchFeatures.isNumericSearchEnabled()).isTrue();
        assertThat(searchFeatures.isVerbatimSearchEnabled()).isTrue();
        assertThat(searchFeatures.isListFilterQueryLanguageEnabled()).isTrue();

        searchFeatures = builder.setNumericSearchEnabled(false)
                .setVerbatimSearchEnabled(false)
                .setListFilterQueryLanguageEnabled(false)
                .build();
        assertThat(searchFeatures.isNumericSearchEnabled()).isFalse();
        assertThat(searchFeatures.isVerbatimSearchEnabled()).isFalse();
        assertThat(searchFeatures.isListFilterQueryLanguageEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE,
            Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION})
    public void testSetFeatureEnabledToFalse_hasProperty() {
        SearchFeatures.Builder builder = new SearchFeatures.Builder();
        SearchFeatures searchFeatures = builder.setListFilterHasPropertyFunctionEnabled(true)
                .build();
        assertThat(searchFeatures.isListFilterHasPropertyFunctionEnabled()).isTrue();

        searchFeatures = builder.setListFilterHasPropertyFunctionEnabled(false)
                .build();
        assertThat(searchFeatures.isListFilterHasPropertyFunctionEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE,
            Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION})
    public void testSetFeatureEnabledToFalse_matchScoreExpression() {
        SearchFeatures.Builder builder = new SearchFeatures.Builder();
        SearchFeatures searchFeatures = builder
                .setListFilterMatchScoreExpressionFunctionEnabled(true)
                .build();
        assertThat(searchFeatures.isListFilterMatchScoreExpressionFunctionEnabled()).isTrue();

        searchFeatures = builder.setListFilterMatchScoreExpressionFunctionEnabled(false).build();
        assertThat(searchFeatures.isListFilterMatchScoreExpressionFunctionEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE,
            Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION,
            Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION})
    public void testEquals() {
        SearchFeatures searchFeatures1 = new SearchFeatures.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .setListFilterMatchScoreExpressionFunctionEnabled(true)
                .build();
        SearchFeatures searchFeatures2 = new SearchFeatures.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .setListFilterMatchScoreExpressionFunctionEnabled(true)
                .build();
        SearchFeatures searchFeatures3 = new SearchFeatures.Builder()
                .setNumericSearchEnabled(true)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();

        assertThat(searchFeatures1).isEqualTo(searchFeatures2);
        assertThat(searchFeatures1).isNotEqualTo(searchFeatures3);
        assertThat(searchFeatures1.hashCode()).isEqualTo(searchFeatures2.hashCode());
        assertThat(searchFeatures1.hashCode()).isNotEqualTo(searchFeatures3.hashCode());
    }
}
