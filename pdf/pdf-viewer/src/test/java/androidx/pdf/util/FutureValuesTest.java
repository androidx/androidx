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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import androidx.annotation.NonNull;
import androidx.pdf.data.FutureValue;
import androidx.pdf.data.FutureValue.Callback;
import androidx.pdf.data.FutureValues;
import androidx.pdf.data.FutureValues.BlockingCallback;
import androidx.pdf.data.FutureValues.Converter;
import androidx.pdf.data.FutureValues.DeferredFutureValue;
import androidx.pdf.data.FutureValues.SettableFutureValue;
import androidx.pdf.data.FutureValues.SimpleCallback;
import androidx.pdf.data.Supplier;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;


@SmallTest
@RunWith(RobolectricTestRunner.class)
public class FutureValuesTest {

    @Mock
    private Callback<Integer> mCallback;

    AutoCloseable mOpenMocks;

    @Before
    public void setup() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void testImmediateValue() {
        final String aValue = "A";
        final boolean[] called = new boolean[]{false};

        FutureValue<String> immediateValue = FutureValues.newImmediateValue(aValue);
        immediateValue.get(
                new Callback<String>() {

                    @Override
                    public void available(String value) {
                        assertThat(value).isEqualTo(aValue);
                        called[0] = true;
                    }

                    @Override
                    public void failed(@NonNull Throwable thrown) {
                        fail("Not expected to fail for ImmediateValue");
                    }

                    @Override
                    public void progress(float progress) {
                        fail("No progress expected for ImmediateValue");
                    }
                });
        assertThat(called[0]).isTrue();
    }

    @Test
    public void testConvert() {
        Converter<String, Integer> parseInt = new Converter<String, Integer>() {
            @Override
            public Integer convert(String from) {
                return Integer.parseInt(from);
            }
        };

        final float expectedProgress = 0.5f;
        final Integer expectedValue = -3;

        SettableFutureValue<String> source = FutureValues.newSettableValue();
        FutureValues.convert(source, parseInt).get(mCallback);

        source.progress(expectedProgress);
        source.set("-3");

        verify(mCallback).progress(eq(expectedProgress));
        verify(mCallback).available(eq(expectedValue));
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void testDeferredFutureValue_doesNotStartComputationUntilGet() {
        final boolean[] computed = {false};
        Supplier<FutureValue<Integer>> computation =
                progress -> {
                    computed[0] = true;
                    return FutureValues.newImmediateValue(10);
                };

        DeferredFutureValue<Integer> deferredFutureValue = new DeferredFutureValue<>(computation);

        assertThat(computed[0]).isFalse();

        deferredFutureValue.get(new SimpleCallback<>());

        assertThat(computed[0]).isTrue();
    }

    @Test
    public void testDeferredFutureValue_reportsFailuresDuringSupply() {
        Supplier<FutureValue<Integer>> crashingComputation =
                unused -> {
                    throw new RuntimeException();
                };
        DeferredFutureValue<Integer> deferredFutureValue =
                new DeferredFutureValue<>(crashingComputation);

        final boolean[] failureDetected = {false};
        deferredFutureValue.get(
                new BlockingCallback<Integer>() {
                    @Override
                    public void failed(@NonNull Throwable thrown) {
                        super.failed(thrown);
                        failureDetected[0] = true;
                    }
                });

        assertThat(failureDetected[0]).isTrue();
    }

    @Test
    public void testDeferredFutureValue_pipesAvailableFromDelegateFuture() {
        SettableFutureValue<Integer> delegate = new SettableFutureValue<>();
        Supplier<FutureValue<Integer>> computation = unused -> delegate;
        DeferredFutureValue<Integer> deferredFutureValue = new DeferredFutureValue<>(computation);

        final boolean[] availableDetected = {false};
        deferredFutureValue.get(
                new SimpleCallback<Integer>() {
                    @Override
                    public void available(Integer value) {
                        super.available(value);
                        availableDetected[0] = true;
                    }
                });

        assertThat(availableDetected[0]).isFalse();

        delegate.set(10);

        assertThat(availableDetected[0]).isTrue();
    }

    @Test
    public void testDeferredFutureValue_pipesFailureFromDelegateFuture() {
        SettableFutureValue<Integer> delegate = new SettableFutureValue<>();
        Supplier<FutureValue<Integer>> computation = unused -> delegate;
        DeferredFutureValue<Integer> deferredFutureValue = new DeferredFutureValue<>(computation);

        final boolean[] failureDetected = {false};
        deferredFutureValue.get(
                new SimpleCallback<Integer>() {
                    @Override
                    public void failed(@NonNull Throwable thrown) {
                        super.failed(thrown);
                        failureDetected[0] = true;
                    }
                });

        assertThat(failureDetected[0]).isFalse();

        delegate.fail(new RuntimeException("Error!"));

        assertThat(failureDetected[0]).isTrue();
    }
}
