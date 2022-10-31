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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.camera.testing.mocks.helpers.ArgumentCaptor;
import androidx.camera.testing.mocks.helpers.CallTimes;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;

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

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> mMockConsumer.verifyAcceptCall(Object.class, false, new CallTimes(1), null));
        assertEquals("accept() called 2 time(s) with Object, expected 1 times", error.getMessage());
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

        assertEquals(DUMMY_STRING_2, captor.getValue());
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

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> {
                    mMockConsumer.verifyAcceptCall(String.class, false, 500);
                });

        assertEquals("Test failed for a timeout of 500 ms", error.getMessage());
    }
}
