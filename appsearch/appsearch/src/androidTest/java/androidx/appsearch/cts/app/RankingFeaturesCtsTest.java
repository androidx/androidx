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

import androidx.appsearch.app.RankingFeatures;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Test;

public class RankingFeaturesCtsTest {

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE,
            Flags.FLAG_ENABLE_SCORABLE_PROPERTY})
    public void testSetFeatureEnabledToFalse_matchScoreExpression() {
        RankingFeatures.Builder builder = new RankingFeatures.Builder();
        RankingFeatures rankingFeatures = builder
                .setScorablePropertyRankingEnabled(true)
                .build();
        assertThat(rankingFeatures.isScorablePropertyRankingEnabled()).isTrue();

        rankingFeatures = builder.setScorablePropertyRankingEnabled(false).build();
        assertThat(rankingFeatures.isScorablePropertyRankingEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE,
            Flags.FLAG_ENABLE_SCORABLE_PROPERTY})
    public void testEquals() {
        RankingFeatures rankingFeatures1 = new RankingFeatures.Builder()
                .setScorablePropertyRankingEnabled(true)
                .build();
        RankingFeatures rankingFeatures2 = new RankingFeatures.Builder()
                .setScorablePropertyRankingEnabled(true)
                .build();
        RankingFeatures rankingFeatures3 = new RankingFeatures.Builder().build();
        assertThat(rankingFeatures1).isEqualTo(rankingFeatures2);
        assertThat(rankingFeatures1).isNotEqualTo(rankingFeatures3);
        assertThat(rankingFeatures1.hashCode()).isEqualTo(rankingFeatures2.hashCode());
        assertThat(rankingFeatures1.hashCode()).isNotEqualTo(rankingFeatures3.hashCode());
    }
}
