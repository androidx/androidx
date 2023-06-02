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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static java.lang.Integer.MAX_VALUE;

import android.icu.util.ULocale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.PlatformHealthSources;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator.EvaluationException;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Supplier;

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
    public void timeDataGetsPropagated() throws Exception {
        TestPlatformTimeUpdateNotifier notifier = new TestPlatformTimeUpdateNotifier();
        DynamicTypeEvaluator evaluator = createEvaluatorWithTime(notifier);
        ArrayList<String> results = new ArrayList<>();
        DynamicTypeBindingRequest request =
                createSingleNodeDynamicStringFromTimePlatformRequest(results);
        BoundDynamicType boundDynamicType = evaluator.bind(request);

        // Evaluation hasn't started yet, nothing should be called.
        ListenableFuture<Void> resultFuture = notifier.callReceiver();
        assertThat(resultFuture).isNull();
        assertThat(results).isEmpty();

        // Start evaluation.
        boundDynamicType.startEvaluation();

        // Trigger reevaluation, which should send a result.
        for (int i = 0; i < 5; i++) {
            resultFuture = notifier.callReceiver();
            assertThat(resultFuture).isNotNull();
            assertThat(resultFuture.isDone()).isTrue();
            assertThat(results).hasSize(i + 1);
            assertThat(Integer.parseInt(results.get(i))).isAtLeast(0);
            assertThat(Integer.parseInt(results.get(i))).isLessThan(60);
        }

        boundDynamicType.close();
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

    @Test
    public void platformDataProvider_correctlySet() throws EvaluationException {
        AddToListCallback<Integer> results = new AddToListCallback<>(new ArrayList<>());
        DynamicTypeBindingRequest request =
                DynamicTypeBindingRequest.forDynamicInt32(
                        PlatformHealthSources.dailySteps(),
                        new MainThreadExecutor(), results);
        PlatformDataProvider provider = mock(PlatformDataProvider.class);
        DynamicTypeEvaluator evaluator = createEvaluatorWithProvider(provider,
                PlatformHealthSources.Keys.DAILY_STEPS);

        BoundDynamicType boundDynamicType = evaluator.bind(request);
        boundDynamicType.startEvaluation();

        verify(provider).setReceiver(any(), any());

        boundDynamicType.close();
        verify(provider).clearReceiver();
    }

    @NonNull
    private static DynamicTypeBindingRequest createSingleNodeDynamicBoolRequest(
            ArrayList<Boolean> results) {
        return DynamicTypeBindingRequest.forDynamicBool(
                DynamicBool.constant(false),
                new MainThreadExecutor(),
                new AddToListCallback<>(results));
    }

    @NonNull
    private static DynamicTypeBindingRequest createSingleNodeDynamicStringFromTimePlatformRequest(
            ArrayList<String> results) {
        return DynamicTypeBindingRequest.forDynamicString(
                DynamicBuilders.DynamicInstant.platformTimeWithSecondsPrecision().durationUntil(
                        DynamicBuilders.DynamicInstant
                                .withSecondsPrecision(Instant.now())).getSecondsPart().format(),
                ULocale.ENGLISH,
                new MainThreadExecutor(),
                new AddToListCallback<>(results));
    }

    private static DynamicTypeEvaluator createEvaluator() {
        return createEvaluatorWithQuota(unlimitedQuota(), unlimitedQuota());
    }

    private static DynamicTypeEvaluator createEvaluatorWithProvider(PlatformDataProvider provider
            , PlatformDataKey<?> key) {
        return new DynamicTypeEvaluator(
                new DynamicTypeEvaluator.Config.Builder()
                        .setAnimationQuotaManager(unlimitedQuota())
                        .setDynamicTypesQuotaManager(unlimitedQuota())
                        .addPlatformDataProvider(provider, Collections.singleton(key))
                        .build());
    }

    private static DynamicTypeEvaluator createEvaluatorWithTime(
            PlatformTimeUpdateNotifier notifier) {
        return new DynamicTypeEvaluator(
                new DynamicTypeEvaluator.Config.Builder()
                        .setAnimationQuotaManager(unlimitedQuota())
                        .setDynamicTypesQuotaManager(unlimitedQuota())
                        .setPlatformTimeUpdateNotifier(notifier)
                        .build());
    }

    private static DynamicTypeEvaluator createEvaluatorWithQuota(
            QuotaManager animationQuota, QuotaManager dynamicTypesQuota) {
        StateStore stateStore = new StateStore(new HashMap<>());
        return new DynamicTypeEvaluator(
                new DynamicTypeEvaluator.Config.Builder()
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

    private static final class TestPlatformTimeUpdateNotifier
            extends PlatformTimeUpdateNotifierImpl {
        private Supplier<ListenableFuture<Void>> mRegisteredReceiver;

        @Nullable
        ListenableFuture<Void> callReceiver() {
            if (mRegisteredReceiver != null) {
                return mRegisteredReceiver.get();
            }
            return null;
        }

        @Override
        public void setReceiver(@NonNull Supplier<ListenableFuture<Void>> tick) {
            super.setReceiver(tick);

            mRegisteredReceiver = tick;
        }

        @Override
        public void clearReceiver() {
            super.clearReceiver();

            mRegisteredReceiver = null;
        }
    }
}
