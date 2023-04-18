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
package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.lang.Integer.MAX_VALUE;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator.EvaluationException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class DynamicTypeEvaluatorTest {
    @Test
    public void evaluateBindingRequest_sufficientDynamicNodeQuota_bindSuccessfully()
            throws EvaluationException {
        DynamicTypeEvaluator evaluator = createEvaluator();
        ArrayList<Boolean> results = new ArrayList<>();
        DynamicTypeBindingRequest request = createSingleNodeDynamicBoolRequest(results);
        BoundDynamicType boundDynamicType = evaluator.bind(request);
        assertThat(boundDynamicType.getDynamicNodeCount()).isEqualTo(1);
    }

    @Test
    public void evaluateBindingRequest_insufficientDynamicNodeQuota_throws() {
        DynamicTypeEvaluator evaluator =
                createEvaluatorWithQuota(
                        /* animationQuota= */ unlimitedQuota(), /* dynamicTypesQuota= */ noQuota());
        ArrayList<Boolean> results = new ArrayList<>();
        DynamicTypeBindingRequest request = createSingleNodeDynamicBoolRequest(results);
        assertThrows(EvaluationException.class, () -> evaluator.bind(request));
    }

    @Test
    public void evaluateBindingRequest_insufficientDynamicNodeQuota_canRetryAfterQuotaReleased()
            throws EvaluationException {
        DynamicTypeEvaluator evaluator =
                createEvaluatorWithQuota(
                        /* animationQuota= */ unlimitedQuota(),
                        /* dynamicTypesQuota= */ new FixedQuotaManagerImpl(1));
        ArrayList<Boolean> results = new ArrayList<>();
        DynamicTypeBindingRequest request1 = createSingleNodeDynamicBoolRequest(results);
        DynamicTypeBindingRequest request2 = createSingleNodeDynamicBoolRequest(results);
        // request 1 should bind successfully and request2 should fail.
        BoundDynamicType boundDynamicType1 = evaluator.bind(request1);
        assertThrows(EvaluationException.class, () -> evaluator.bind(request2));
        // Release quota acquired by request1
        boundDynamicType1.close();
        // Retry binding request2
        BoundDynamicType boundDynamicType2 = evaluator.bind(request2);
        assertThat(boundDynamicType2.getDynamicNodeCount()).isEqualTo(1);
    }

    @NonNull
    private static DynamicTypeBindingRequest createSingleNodeDynamicBoolRequest(
            ArrayList<Boolean> results) {
        return DynamicTypeBindingRequest.forDynamicBool(
                DynamicBool.constant(false),
                new MainThreadExecutor(),
                new AddToListCallback<>(results));
    }

    private static DynamicTypeEvaluator createEvaluator() {
        return createEvaluatorWithQuota(unlimitedQuota(), unlimitedQuota());
    }

    private static DynamicTypeEvaluator createEvaluatorWithQuota(
            QuotaManager animationQuota, QuotaManager dynamicTypesQuota) {
        StateStore stateStore = new StateStore(new HashMap<>());
        return new DynamicTypeEvaluator(
                new DynamicTypeEvaluator.Config.Builder()
                        .setPlatformDataSourcesInitiallyEnabled(true)
                        .setStateStore(stateStore)
                        .setAnimationQuotaManager(animationQuota)
                        .setDynamicTypesQuotaManager(dynamicTypesQuota)
                        .build());
    }

    private static QuotaManager unlimitedQuota() {
        return new FixedQuotaManagerImpl(MAX_VALUE);
    }

    private static QuotaManager noQuota() {
        return new FixedQuotaManagerImpl(0);
    }
}
