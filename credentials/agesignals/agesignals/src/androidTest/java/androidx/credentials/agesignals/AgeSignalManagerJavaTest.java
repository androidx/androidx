/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.core.os.OutcomeReceiverCompat;
import androidx.credentials.agesignals.exceptions.GetAgeRangeException;
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException;
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AgeSignalManagerJavaTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private final GetAgeRangeRequest mTestRequest = new GetAgeRangeRequest();
    private final GetAgeRangeResponse mExpectedResponse =
            new GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_B);

    private AgeSignalProviderFactory mFactory;

    @Before
    public void setUp() {
        mFactory = new AgeSignalProviderFactory(mContext);
        mFactory.setTestMode(true);
    }

    @Test
    public void create_returnsNonNullInstance() {
        AgeSignalManager manager = AgeSignalManager.create(mContext);
        assertThat(manager).isNotNull();
    }

    @Test
    public void getAgeRangeAsync_successCallbackDeliveredOnExecutor() throws InterruptedException {
        FakeAgeSignalProvider fakeProvider =
                new FakeAgeSignalProvider(mContext, true, mExpectedResponse, null);
        mFactory.setTestProvider(fakeProvider);
        AgeSignalManager manager = new AgeSignalManagerImpl(mFactory);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executedOnCustomThread = new AtomicBoolean(false);
        AtomicReference<GetAgeRangeResponse> receivedResponse = new AtomicReference<>();

        Executor customExecutor = command -> {
            executedOnCustomThread.set(true);
            command.run();
        };

        manager.getAgeRangeAsync(
                mContext,
                mTestRequest,
                null,
                customExecutor,
                new OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>() {
                    @Override
                    public void onResult(GetAgeRangeResponse result) {
                        receivedResponse.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(GetAgeRangeException error) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(executedOnCustomThread.get()).isTrue();
        assertThat(receivedResponse.get()).isEqualTo(mExpectedResponse);
        assertThat(fakeProvider.getLastReceivedRequest()).isEqualTo(mTestRequest);
        assertThat(fakeProvider.getLastReceivedContext()).isSameInstanceAs(mContext);
    }

    @Test
    public void getAgeRangeAsync_providerError_dispatchesErrorOnExecutor()
            throws InterruptedException {
        GetAgeRangeUnknownException expectedError =
                new GetAgeRangeUnknownException("Custom provider error");
        FakeAgeSignalProvider fakeProvider =
                new FakeAgeSignalProvider(mContext, true, null, expectedError);
        mFactory.setTestProvider(fakeProvider);
        AgeSignalManager manager = new AgeSignalManagerImpl(mFactory);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executedOnCustomThread = new AtomicBoolean(false);
        AtomicReference<GetAgeRangeException> receivedError = new AtomicReference<>();

        Executor customExecutor = command -> {
            executedOnCustomThread.set(true);
            command.run();
        };

        manager.getAgeRangeAsync(
                mContext,
                mTestRequest,
                null,
                customExecutor,
                new OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>() {
                    @Override
                    public void onResult(GetAgeRangeResponse result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(GetAgeRangeException error) {
                        receivedError.set(error);
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(executedOnCustomThread.get()).isTrue();
        assertThat(receivedError.get()).isEqualTo(expectedError);
    }

    @Test
    public void getAgeRangeAsync_noProvider_dispatchesConfigurationException()
            throws InterruptedException {
        mFactory.setTestProvider(null);
        AgeSignalManager manager = new AgeSignalManagerImpl(mFactory);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GetAgeRangeException> loadedResult = new AtomicReference<>();
        AtomicBoolean executedOnCustomThread = new AtomicBoolean(false);

        Executor customExecutor = command -> {
            executedOnCustomThread.set(true);
            command.run();
        };

        manager.getAgeRangeAsync(
                mContext,
                mTestRequest,
                null,
                customExecutor,
                new OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>() {
                    @Override
                    public void onResult(GetAgeRangeResponse result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(GetAgeRangeException error) {
                        loadedResult.set(error);
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(executedOnCustomThread.get()).isTrue();
        assertThat(loadedResult.get())
                .isInstanceOf(GetAgeRangeProviderConfigurationException.class);
    }

    @Test
    public void getAgeRangeResponse_gettersAndConstants_workFromJava() {
        GetAgeRangeResponse boundedResponse =
                new GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_B);
        assertThat(boundedResponse.getLowerAgeBound()).isEqualTo(13);
        assertThat(boundedResponse.getUpperAgeBound()).isEqualTo(15);
        assertThat(boundedResponse.getAssuranceTier()).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_B);

        GetAgeRangeResponse openEndedResponse =
                new GetAgeRangeResponse(18, null, GetAgeRangeResponse.ASSURANCE_TIER_D);
        assertThat(openEndedResponse.getLowerAgeBound()).isEqualTo(18);
        assertThat(openEndedResponse.getUpperAgeBound()).isNull();
        assertThat(openEndedResponse.getAssuranceTier()).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_D);
    }

    @Test
    public void ageAssuranceTier_constantsAccessibleFromJava() {
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_A).isEqualTo(1);
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_B).isEqualTo(2);
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_C).isEqualTo(3);
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_D).isEqualTo(4);
    }
}
