/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchSpec;

import org.junit.Test;

/*@exportToFramework:SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)*/
public class JoinSpecCtsTest {

    @Test
    public void testBuild() {
        SearchSpec originalNestedSearchSpec = new SearchSpec.Builder()
                .addFilterSchemas("Action", "CallAction")
                .build();

        JoinSpec.Builder builder = new JoinSpec.Builder("entityId")
                .setMaxJoinedResultCount(5)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setNestedSearch("joe", originalNestedSearchSpec);

        JoinSpec original = builder.build();

        // The rebuild shouldn't affect the original object.
        assertThat(original.getMaxJoinedResultCount()).isEqualTo(5);
        assertThat(original.getAggregationScoringStrategy())
                .isEqualTo(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT);
        assertThat(original.getNestedQuery()).isEqualTo("joe");
        assertThat(original.getNestedSearchSpec().getFilterSchemas())
                .isEqualTo(originalNestedSearchSpec.getFilterSchemas());
        assertThat(original.getChildPropertyExpression()).isEqualTo("entityId");
    }

    @Test
    public void testDefaultNestedSearchSpec() {
        SearchSpec empty = new SearchSpec.Builder().build();
        JoinSpec joinSpec = new JoinSpec.Builder("entityId").build();

        assertThat(joinSpec.getNestedSearchSpec().getJoinSpec()).isEqualTo(empty.getJoinSpec());
        assertThat(joinSpec.getNestedSearchSpec().getOrder()).isEqualTo(empty.getOrder());
        assertThat(joinSpec.getNestedSearchSpec().getRankingStrategy())
                .isEqualTo(empty.getRankingStrategy());
        assertThat(joinSpec.getNestedSearchSpec().getFilterPackageNames())
                .isEqualTo(empty.getFilterPackageNames());
        assertThat(joinSpec.getNestedSearchSpec().getFilterSchemas())
                .isEqualTo(empty.getFilterSchemas());
        assertThat(joinSpec.getNestedSearchSpec().getFilterNamespaces())
                .isEqualTo(empty.getFilterNamespaces());
    }
}
