/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.enterprise.feedback;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests {@link KeyedAppStatesCallbackMerger}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStatesCallbackMergerTest {

    private final TestKeyedAppStatesCallback mCallback = new TestKeyedAppStatesCallback();
    private final Throwable mTestThrowable = new IllegalArgumentException();

    @Test
    public void notZeroExpected_noImmediateCallback() {
        new KeyedAppStatesCallbackMerger(1, mCallback);

        assertThat(mCallback.mTotalResults).isEqualTo(0);
    }

    @Test
    public void zeroExpected_successCallbackImmediately() {
        new KeyedAppStatesCallbackMerger(0, mCallback);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(KeyedAppStatesCallback.STATUS_SUCCESS);
    }

    @Test
    public void oneExpected_successCallbackIsPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(1, mCallback);

        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(KeyedAppStatesCallback.STATUS_SUCCESS);
    }

    @Test
    public void twoExpected_firstSuccessCallbackIsNotPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);

        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(0);
    }

    @Test
    public void twoExpected_secondSuccessCallbackIsPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);
        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(KeyedAppStatesCallback.STATUS_SUCCESS);
    }

    @Test
    public void twoExpected_thirdSuccessCallbackIsNotPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);
        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);
        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
    }

    @Test
    public void oneExpected_failureCallbackIsPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(1, mCallback);

        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR);
    }

    @Test
    public void twoExpected_firstFailureCallbackIsPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);

        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR);
    }

    @Test
    public void twoExpected_secondFailureCallbackIsNotPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);
        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR, /* throwable= */ null);

        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR);
    }

    @Test
    public void twoExpected_alreadyFailed_laterSuccessCallbacksAreNotPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(2, mCallback);
        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR, /* throwable= */ null);

        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);
        merger.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR);
    }

    @Test
    public void throwableIsPassedThrough() {
        KeyedAppStatesCallbackMerger merger =
                new KeyedAppStatesCallbackMerger(1, mCallback);

        merger.onResult(
                KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR,
                /* throwable= */ mTestThrowable);

        assertThat(mCallback.mLatestThrowable).isEqualTo(mTestThrowable);
    }
}
