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

import androidx.annotation.NonNull;
import androidx.pdf.data.FutureValue.Callback;
import androidx.pdf.data.FutureValues;
import androidx.pdf.data.FutureValues.SettableFutureValue;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class SettableFutureValueTest {

    private static final class TestCallback implements Callback<String> {
        private Throwable mThrown;
        private String mValue;
        private long mResultCount;

        public static final int LAST_PROGRESS_DONE_DEFAULT = 0;
        private float mLastProgressDone = LAST_PROGRESS_DONE_DEFAULT;

        @Override
        public void failed(@NonNull Throwable thrown) {
            this.mThrown = thrown;
            this.mResultCount++;
        }

        @Override
        public void available(String value) {
            this.mValue = value;
            this.mResultCount++;
        }

        @Override
        public void progress(float progressDone) {
            mLastProgressDone = progressDone;
        }
    }

    @Test
    public void testGetAfterSet() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        String value = "Hello";
        future.set(value);
        TestCallback callback = new TestCallback();
        future.get(callback);
        assertThat(value).isEqualTo(callback.mValue);
        assertThat(callback.mThrown).isNull();
    }

    @Test
    public void testProgressAfterSet() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        String value = "Hello";
        TestCallback callback = new TestCallback();
        float progressValue = 0.3f;

        future.get(callback);

        try {
            future.set(value);
            future.progress(progressValue);
        } catch (Exception e) {
            throw new AssertionError(
                    "Shouldn't bother setting a progress value after a value has already been set.",
                    e);
            // Exception expected
        }
    }

    @Test
    public void testProgressDropped() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        TestCallback callback = new TestCallback();
        float progressValue = 0.6f;

        future.progress(progressValue);
        future.get(callback);
        assertThat(callback.mLastProgressDone).isWithin(0).of(
                TestCallback.LAST_PROGRESS_DONE_DEFAULT);
    }

    @Test
    public void testProgressPassedThrough() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        float progressValue = 0.7f;
        TestCallback callback = new TestCallback();

        future.get(callback);
        future.progress(progressValue);
        assertThat(callback.mLastProgressDone).isWithin(0).of(progressValue);

        future.progress(progressValue * 2);
        assertThat(callback.mLastProgressDone).isWithin(0).of(progressValue * 2);
    }

    @Test
    public void testSetAfterGet() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        TestCallback callback = new TestCallback();
        future.get(callback);
        String value = "Hello";
        future.set(value);
        assertThat(value).isEqualTo(callback.mValue);
        assertThat(callback.mThrown).isNull();
    }

    @Test
    public void testGetAfterFail() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        IllegalArgumentException thrown = new IllegalArgumentException();
        future.fail(thrown);
        TestCallback callback = new TestCallback();
        future.get(callback);
        assertThat(callback.mValue).isNull();
        assertThat(thrown).isEqualTo(callback.mThrown);
    }

    @Test
    public void testNullException() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();
        try {
            // This was necessary because fail(null) was ambiguous between fail(Exception) and
            // fail(String).
            Exception nullValue = null;
            future.fail(nullValue);
            fail("Should have thrown a NPE");
        } catch (NullPointerException e) {
            // Exception expected.
        }
    }

    // Test that calling get again after the value is set does not repeat callback.
    @Test
    public void testGetBeforeAndAfterSet() {
        SettableFutureValue<String> future = FutureValues.newSettableValue();

        TestCallback callback1 = new TestCallback();
        future.get(callback1);

        String value = "Hello";
        future.set(value);

        TestCallback callback2 = new TestCallback();
        future.get(callback2);

        assertThat(value).isEqualTo(callback1.mValue);
        assertThat(callback1.mThrown).isNull();
        assertThat(1).isEqualTo(callback1.mResultCount);

        assertThat(value).isEqualTo(callback2.mValue);
        assertThat(callback2.mThrown).isNull();
        assertThat(1).isEqualTo(callback2.mResultCount);
    }

    @Test
    public void testIsSet() {
        final String aValue = "A";

        SettableFutureValue<String> futureValue = FutureValues.newSettableValue();
        assertThat(futureValue.isSet()).isFalse();

        futureValue = FutureValues.newSettableValue();
        assertThat(futureValue.isSet()).isFalse();
        futureValue.set(aValue);
        assertThat(futureValue.isSet()).isTrue();

        futureValue = FutureValues.newSettableValue();
        assertThat(futureValue.isSet()).isFalse();
        futureValue.fail(new Exception());
        assertThat(futureValue.isSet()).isTrue();
    }
}
