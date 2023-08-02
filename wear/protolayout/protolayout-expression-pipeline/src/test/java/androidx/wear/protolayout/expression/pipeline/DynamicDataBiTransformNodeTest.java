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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;

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

    private DynamicDataBiTransformNode<Integer, Integer, Integer> mNodeUnderTest;

    @Before
    public void setUp() {
        mNodeUnderTest = new DynamicDataBiTransformNode<>(mMockCallback, Integer::sum);
    }

    @Test
    public void onPreStateUpdate_propagatesOnce() {
        mNodeUnderTest.getLhsIncomingCallback().onPreUpdate();
        verify(mMockCallback).onPreUpdate();

        // Next call should not cause another call to be passed through.
        mNodeUnderTest.getRhsIncomingCallback().onPreUpdate();
        verify(mMockCallback).onPreUpdate();
    }

    @Test
    public void onStateUpdate_invokesCallback() {
        mNodeUnderTest.getLhsIncomingCallback().onPreUpdate();
        mNodeUnderTest.getRhsIncomingCallback().onPreUpdate();

        mNodeUnderTest.getLhsIncomingCallback().onData(5);
        mNodeUnderTest.getRhsIncomingCallback().onData(6);

        verify(mMockCallback).onData(11);
    }

    @Test
    public void onStateUpdate_onlyOneSide_doesntInvokeCallback() {
        // Note: RHS has *not* been seeded with any data here...
        mNodeUnderTest.getLhsIncomingCallback().onPreUpdate();
        mNodeUnderTest.getLhsIncomingCallback().onData(5);

        verify(mMockCallback, never()).onData(any());
    }

    @Test
    public void onStateUpdate_onPreStateUpdateNotCalled_stillPropagatesData() {
        mNodeUnderTest.getLhsIncomingCallback().onData(5);
        mNodeUnderTest.getRhsIncomingCallback().onData(6);
        verify(mMockCallback, never()).onPreUpdate();
        verify(mMockCallback).onData(11);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onStateUpdate_onStateUpdate_eachSidePendingUpdateHandledSeparately() {
        // Seed with some real data
        mNodeUnderTest.getLhsIncomingCallback().onData(5);
        mNodeUnderTest.getRhsIncomingCallback().onData(6);
        reset(mMockCallback);

        mNodeUnderTest.getLhsIncomingCallback().onPreUpdate();
        mNodeUnderTest.getRhsIncomingCallback().onData(10);

        // The "is update pending" counters should be evaluated separately here, so the incoming
        // update on the RHS should not count towards the state update from the LHS
        verify(mMockCallback, never()).onData(any());

        // And now, fulfilling the LHS should still cause an update to be fired.
        mNodeUnderTest.getLhsIncomingCallback().onData(20);
        verify(mMockCallback).onData(30);
    }
}
