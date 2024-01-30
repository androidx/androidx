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
public class ConditionalOpNodeTest {
    private static final String TRUE_STRING = "TRUE";
    private static final String FALSE_STRING = "FALSE";

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private DynamicTypeValueReceiverWithPreUpdate<String> mUpdateCallback;

    private ConditionalOpNode<String> mOpUnderTest;

    private void standardSetup(boolean initialConditionValue) {
        mOpUnderTest.getConditionIncomingCallback().onPreUpdate();
        mOpUnderTest.getTrueValueIncomingCallback().onPreUpdate();
        mOpUnderTest.getFalseValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getConditionIncomingCallback().onData(initialConditionValue);
        mOpUnderTest.getTrueValueIncomingCallback().onData(TRUE_STRING);
        mOpUnderTest.getFalseValueIncomingCallback().onData(FALSE_STRING);
    }

    @Before
    public void setUp() {
        mOpUnderTest = new ConditionalOpNode<>(mUpdateCallback);
    }

    @Test
    public void testWhenTrue_picksTrueValue() {
        standardSetup(true);

        verify(mUpdateCallback).onData(TRUE_STRING);
    }

    @Test
    public void testWhenTrue_picksFalseValue() {
        standardSetup(false);

        verify(mUpdateCallback).onData(FALSE_STRING);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenConditionChanges_updatesValue() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getConditionIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getConditionIncomingCallback().onData(false);
        verify(mUpdateCallback).onData(FALSE_STRING);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenTrueValueChangesAndConditionTrue_updatesValue() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getTrueValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getTrueValueIncomingCallback().onData("testtest");
        verify(mUpdateCallback).onData("testtest");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenTrueValueChangesAndConditionFalse_valueRetained() {
        standardSetup(false);
        reset(mUpdateCallback);

        mOpUnderTest.getTrueValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getTrueValueIncomingCallback().onData("testtest");
        verify(mUpdateCallback).onData(FALSE_STRING);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenFalseValueChangesAndConditionFalse_updatesValue() {
        standardSetup(false);
        reset(mUpdateCallback);

        mOpUnderTest.getFalseValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getFalseValueIncomingCallback().onData("testtest");
        verify(mUpdateCallback).onData("testtest");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenFalseValueChangesAndConditionTrue_valueRetained() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getFalseValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getFalseValueIncomingCallback().onData("testtest");
        verify(mUpdateCallback).onData(TRUE_STRING);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenConditionalValueInvalid_proxiesInvalid() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getConditionIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getConditionIncomingCallback().onInvalidated();
        verify(mUpdateCallback).onInvalidated();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenTrueValueInvalid_proxiesInvalid() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getTrueValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getTrueValueIncomingCallback().onInvalidated();
        verify(mUpdateCallback).onInvalidated();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenFalseValueInvalid_proxiesInvalid() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getFalseValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getFalseValueIncomingCallback().onInvalidated();
        verify(mUpdateCallback).onInvalidated();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenConditionAndValueChangesSimultaneously_givesExpectedResult() {
        standardSetup(true);
        reset(mUpdateCallback);

        mOpUnderTest.getConditionIncomingCallback().onPreUpdate();
        mOpUnderTest.getTrueValueIncomingCallback().onPreUpdate();
        verify(mUpdateCallback).onPreUpdate();

        mOpUnderTest.getConditionIncomingCallback().onData(false);
        mOpUnderTest.getTrueValueIncomingCallback().onData("World");
        verify(mUpdateCallback).onData(FALSE_STRING);
    }
}
