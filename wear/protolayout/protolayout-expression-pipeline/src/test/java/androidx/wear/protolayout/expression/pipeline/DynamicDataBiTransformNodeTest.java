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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class DynamicDataBiTransformNodeTest {
    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private DynamicTypeValueReceiverWithPreUpdate<Integer> mMockCallback;

    private DynamicDataBiTransformNode<Integer, Integer, Integer> mSumNodeUnderTest;

    @Before
    public void setUp() {
        mSumNodeUnderTest = new DynamicDataBiTransformNode<>(mMockCallback, Integer::sum);
    }

    @After
    public void assertNoOtherCallbacks() {
        // DynamicDataBiTransformNode is fragile, we want to make sure an unexpected callback wasn't
        // used on one of the behaviors tested here.
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void onPreStateUpdate_propagatesOnce() {
        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        verify(mMockCallback).onPreUpdate();
        reset(mMockCallback);

        // Next call should not cause another call to be passed through.
        mSumNodeUnderTest.getRhsUpstreamCallback().onPreUpdate();
        verify(mMockCallback, never()).onPreUpdate();
    }

    @Test
    public void onPreStateUpdate_twice_ignoresSecondOne() {
        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        verify(mMockCallback).onPreUpdate();
        reset(mMockCallback);

        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        mSumNodeUnderTest.getRhsUpstreamCallback().onPreUpdate();
        // Doesn't pre-update again.
        verify(mMockCallback, never()).onPreUpdate();

        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(6);
        // Still propagates data.
        verify(mMockCallback).onData(11);
    }

    @Test
    public void onStateUpdate_invokesCallback() {
        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        mSumNodeUnderTest.getRhsUpstreamCallback().onPreUpdate();

        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(6);

        verify(mMockCallback).onPreUpdate();
        verify(mMockCallback).onData(11);
    }

    @Test
    public void onStateUpdate_onlyOneSide_doesNotInvokeCallback() {
        // Note: RHS has *not* been seeded with any data here...
        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);

        verify(mMockCallback).onPreUpdate();
        verify(mMockCallback, never()).onData(any());
    }

    @Test
    public void onStateUpdate_onPreStateUpdateNotCalled_stillPropagatesData() {
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(6);
        verify(mMockCallback, never()).onPreUpdate();
        verify(mMockCallback).onData(11);
    }

    @Test
    public void onStateUpdate_onStateUpdate_eachSidePendingUpdateHandledSeparately() {
        // Seed with some real data
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(6);
        reset(mMockCallback);

        mSumNodeUnderTest.getLhsUpstreamCallback().onPreUpdate();
        verify(mMockCallback).onPreUpdate();
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(10);

        // The "is update pending" counters should be evaluated separately here, so the incoming
        // update on the RHS should not count towards the state update from the LHS
        verify(mMockCallback, never()).onData(any());

        // And now, fulfilling the LHS should still cause an update to be fired.
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(20);
        verify(mMockCallback).onData(30);
    }

    @Test
    public void onStateUpdate_secondTimeOnlyOneSide_doesNotWaitForOtherSide() {
        // Seed with some real data
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(5);
        mSumNodeUnderTest.getRhsUpstreamCallback().onData(6);
        reset(mMockCallback);

        // Only on the left.
        mSumNodeUnderTest.getLhsUpstreamCallback().onData(10);

        // Not waiting for the right.
        verify(mMockCallback).onData(16);
    }

    @Test
    public void onInvalidated_propagatesAfterBothSides() {
        mSumNodeUnderTest.getLhsUpstreamCallback().onInvalidated();
        verifyNoMoreInteractions(mMockCallback);

        mSumNodeUnderTest.getRhsUpstreamCallback().onData(5);
        verify(mMockCallback).onInvalidated();
    }

    /** Same as {@link org.mockito.Mockito#reset} but suppresses the unchecked warning. */
    @SuppressWarnings("unchecked")
    private <T> void reset(T mock) {
        org.mockito.Mockito.reset(mock);
    }
}
