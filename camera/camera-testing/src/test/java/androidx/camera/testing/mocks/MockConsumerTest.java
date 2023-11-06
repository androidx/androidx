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

package androidx.camera.testing.mocks;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.os.Build;

import androidx.camera.testing.impl.mocks.MockConsumer;
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor;
import androidx.camera.testing.impl.mocks.helpers.CallTimes;
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MockConsumerTest {
    public static final String DUMMY_STRING_1 = "dummy1";
    public static final String DUMMY_STRING_2 = "dummy2";

    public static final int DUMMY_INT_1 = 1;

    private MockConsumer<Object> mMockConsumer;

    @Before
    public void setUp() {
        mMockConsumer = new MockConsumer<>();
    }

    @Test
    public void callTimesMatches_verifyAcceptCallPasses() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);
    }

    @Test
    public void callTimesMismatched_verifyAcceptCallFailsWithProperMessage() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());

        checkAssertionError(
                () -> mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(1), null),
                "accept() called 2 time(s) with Object, expected 1 times"
        );
    }

    @Test
    public void verifiedInOrder_verifyAcceptCallSucceeds() {
        mMockConsumer.accept(DUMMY_STRING_1);
        mMockConsumer.accept(DUMMY_STRING_1);
        mMockConsumer.accept(DUMMY_INT_1);
        mMockConsumer.accept(DUMMY_INT_1);
        mMockConsumer.accept(DUMMY_INT_1);

        mMockConsumer.verifyAcceptCall(String.class, true, new CallTimes(2), null);
        mMockConsumer.verifyAcceptCall(Integer.class, true, new CallTimes(3), null);
    }

    @Test
    public void verifiedInOrderWithoutConsecutiveness_verifyAcceptCallSucceeds() {
        mMockConsumer.accept(DUMMY_STRING_1);
        mMockConsumer.accept(DUMMY_INT_1);
        mMockConsumer.accept(DUMMY_STRING_1);

        mMockConsumer.verifyAcceptCall(String.class, true, new CallTimes(2), null);
    }

    @Test
    public void verifiedWithArgumentCaptor_captorHasCorrectValue() {
        ArgumentCaptor<Object> captor = new ArgumentCaptor<>();

        mMockConsumer.accept(DUMMY_STRING_1);
        mMockConsumer.accept(DUMMY_STRING_2);

        mMockConsumer.verifyAcceptCall(String.class, false, new CallTimes(2), captor);

        assertThat(DUMMY_STRING_2).isEqualTo(captor.getValue());
    }

    @Test
    public void acceptCalledWithinTimeout_verifyAcceptCallPasses() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mMockConsumer.accept(DUMMY_STRING_1);
        }, "test thread").start();

        mMockConsumer.verifyAcceptCall(String.class, false, 500);
    }

    @Test
    public void acceptCalledAfterTimeout_verifyAcceptCallFailsWithProperMessage() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mMockConsumer.accept(DUMMY_STRING_1);
        }, "test thread").start();

        checkAssertionError(
                () -> mMockConsumer.verifyAcceptCall(String.class, false, 500),
                "Test failed for a timeout of 500 ms"
        );
    }

    @Test
    public void clearAcceptCalls_canClearAcceptCalls() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);

        mMockConsumer.clearAcceptCalls();
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(1), null);
    }

    @Test
    public void clearAcceptCalls_canVerifyNoMoreAcceptCalls_whenInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, true, new CallTimes(2), null);

        mMockConsumer.clearAcceptCalls();
        mMockConsumer.verifyNoMoreAcceptCalls(true);
    }

    @Test
    public void clearAcceptCalls_verifyNoMoreAcceptCallsCanThrow_whenInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, true, new CallTimes(2), null);

        mMockConsumer.clearAcceptCalls();
        mMockConsumer.accept(new Object());

        checkAssertionError(
                () -> mMockConsumer.verifyNoMoreAcceptCalls(true),
                "There are extra accept() calls after the last in-order verification"
        );
    }

    @Test
    public void clearAcceptCalls_canVerifyNoMoreAcceptCalls_whenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);

        mMockConsumer.clearAcceptCalls();
        mMockConsumer.verifyNoMoreAcceptCalls(false);
    }

    @Test
    public void clearAcceptCalls_verifyNoMoreAcceptCallsCanThrow_whenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);

        mMockConsumer.clearAcceptCalls();
        mMockConsumer.accept(new Object());

        checkAssertionError(
                () -> mMockConsumer.verifyNoMoreAcceptCalls(false),
                "There are extra accept() calls after the last verification"
        );
    }

    @Test
    public void verifyNoMoreAcceptCall_canVerifyWhenInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, true, new CallTimes(2), null);
        mMockConsumer.verifyNoMoreAcceptCalls(true);
    }

    @Test
    public void verifyNoMoreAcceptCall_throwsWhenInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, true, new CallTimes(2), null);
        mMockConsumer.accept(new Object());

        checkAssertionError(
                () -> mMockConsumer.verifyNoMoreAcceptCalls(true),
                "There are extra accept() calls after the last in-order verification"
        );
    }

    @Test
    public void verifyNoMoreAcceptCall_canVerifyWhenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);
        mMockConsumer.verifyNoMoreAcceptCalls(false);
    }

    @Test
    public void verifyNoMoreAcceptCall_canThrowWhenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(2), null);
        mMockConsumer.accept(new Object());

        checkAssertionError(
                () -> mMockConsumer.verifyNoMoreAcceptCalls(false),
                "There are extra accept() calls after the last verification"
        );
    }

    @Test
    public void verifyNoMoreAcceptCall_callTimesAtLeast_canVerifyWhenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimesAtLeast(1), null);
        mMockConsumer.verifyNoMoreAcceptCalls(false);
    }

    @Test
    public void verifyNoMoreAcceptCall_callTimesAtLeast_canThrowWhenNotInOrder() {
        mMockConsumer.accept(new Object());
        mMockConsumer.accept(new Object());
        mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimesAtLeast(1), null);
        mMockConsumer.accept(new Object());

        checkAssertionError(() -> mMockConsumer.verifyNoMoreAcceptCalls(true));
    }

    private void checkAssertionError(Runnable runnable) {
        checkAssertionError(runnable, null);
    }

    private void checkAssertionError(Runnable runnable, String errorMessage) {
        boolean isExpectedErrorThrown = false;
        try {
            runnable.run();
        } catch (AssertionError e) {
            isExpectedErrorThrown = true;
            if (errorMessage != null) {
                assertThat(e).hasMessageThat().contains(errorMessage);
            }
        } finally {
            if (!isExpectedErrorThrown) {
                assertWithMessage("Expected AssertionError was not thrown in test").fail();
            }
        }

    }
}
