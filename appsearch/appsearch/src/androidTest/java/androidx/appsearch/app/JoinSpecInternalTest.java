/*
 * Copyright 2023 The Android Open Source Project
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

public class JoinSpecInternalTest {
    @Test
    public void testJoinSpecBuilderCopyConstructor() {
        JoinSpec joinSpec = new JoinSpec.Builder("childPropertyExpression")
                .setMaxJoinedResultCount(10)
                .setNestedSearch("nestedQuery", new SearchSpec.Builder().build())
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .build();
        JoinSpec joinSpecCopy = new JoinSpec.Builder(joinSpec).build();
        assertThat(joinSpecCopy.getMaxJoinedResultCount()).isEqualTo(
                joinSpec.getMaxJoinedResultCount());
        assertThat(joinSpecCopy.getChildPropertyExpression()).isEqualTo(
                joinSpec.getChildPropertyExpression());
        assertThat(joinSpecCopy.getNestedQuery()).isEqualTo(joinSpec.getNestedQuery());
        assertThat(joinSpecCopy.getNestedSearchSpec()).isNotNull();
        assertThat(joinSpecCopy.getAggregationScoringStrategy()).isEqualTo(
                joinSpec.getAggregationScoringStrategy());
    }
}
