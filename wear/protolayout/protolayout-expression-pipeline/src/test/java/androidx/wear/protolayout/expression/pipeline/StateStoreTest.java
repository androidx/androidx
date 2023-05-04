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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.StateEntryBuilders;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class StateStoreTest {
    @Rule public Expect mExpect = Expect.create();

    private final StateStore mStateStoreUnderTest =
            new StateStore(
                    ImmutableMap.of(
                            "foo", buildStateEntry("bar"),
                            "baz", buildStateEntry("foobar")));

    public StateStoreTest() {}

    @Test
    public void setBuilderApi() {
        mStateStoreUnderTest.setStateEntryValues(
                ImmutableMap.of("foo", StateEntryBuilders.StateEntryValue.fromString("baz")));

        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("foo"))
                .isEqualTo(buildStateEntry("baz"));
    }

    @Test
    public void initState_largeNumberOfEntries_throws() {
        Map<String, StateEntryBuilders.StateEntryValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.MAX_STATE_ENTRY_COUNT + 10; i++) {
            state.put(Integer.toString(i), StateEntryBuilders.StateEntryValue.fromString("baz"));
        }
        assertThrows(IllegalStateException.class, () -> StateStore.create(state));
    }

    @Test
    public void newState_largeNumberOfEntries_throws() {
        Map<String, StateEntryBuilders.StateEntryValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.MAX_STATE_ENTRY_COUNT + 10; i++) {
            state.put(Integer.toString(i), StateEntryBuilders.StateEntryValue.fromString("baz"));
        }
        assertThrows(
                IllegalStateException.class, () -> mStateStoreUnderTest.setStateEntryValues(state));
    }

    @Test
    public void canReadInitialState() {
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("foo"))
                .isEqualTo(buildStateEntry("bar"));
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("baz"))
                .isEqualTo(buildStateEntry("foobar"));
    }

    @Test
    public void unsetStateReturnsNull() {
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("AAAAAA")).isNull();
    }

    @Test
    public void canSetNewState() {
        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo", buildStateEntry("test"),
                        "newKey", buildStateEntry("testNewKey")));

        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("foo"))
                .isEqualTo(buildStateEntry("test"));
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("newKey"))
                .isEqualTo(buildStateEntry("testNewKey"));

        // This should have been cleared...
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto("baz")).isNull();
    }

    @Test
    public void setStateFiresListeners() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback("foo", cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of("foo", buildStateEntry("test")));

        verify(cb).onPreUpdate();
        verify(cb).onData(buildStateEntry("test"));
    }

    @Test
    public void setStateFiresOnPreStateUpdateFirst() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();

        InOrder inOrder = Mockito.inOrder(cb);

        mStateStoreUnderTest.registerCallback("foo", cb);
        mStateStoreUnderTest.registerCallback("baz", cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo", buildStateEntry("testFoo"),
                        "baz", buildStateEntry("testBaz")));

        inOrder.verify(cb, times(2)).onPreUpdate();
        inOrder.verify(cb, times(2)).onData(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void setStateOnlyFiresListenersForChangedData() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cbFoo =
                buildStateUpdateCallbackMock();
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cbBaz =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback("foo", cbFoo);
        mStateStoreUnderTest.registerCallback("baz", cbBaz);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        "foo", buildStateEntry("test"),
                        "baz", buildStateEntry("foobar")));

        verify(cbFoo).onPreUpdate();
        verify(cbFoo).onData(buildStateEntry("test"));
        verify(cbBaz, never()).onPreUpdate();
        verify(cbBaz, never()).onData(buildStateEntry("test"));
    }

    @Test
    public void removeStateFiresInvalidated() {
        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        "invalidated",
                        buildStateEntry("value"),
                        "notInvalidated",
                        buildStateEntry("value")));
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> invalidated =
                buildStateUpdateCallbackMock();
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> notInvalidated =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback("invalidated", invalidated);
        mStateStoreUnderTest.registerCallback("notInvalidated", notInvalidated);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of("notInvalidated", buildStateEntry("value")));

        verify(invalidated).onPreUpdate();
        verify(invalidated).onInvalidated();
        verify(invalidated, never()).onData(any());
        verifyNoInteractions(notInvalidated);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canUnregisterListeners() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback("foo", cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of("foo", buildStateEntry("test")));

        reset(cb);
        mStateStoreUnderTest.unregisterCallback("foo", cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of("foo", buildStateEntry("testAgain")));

        verifyNoInteractions(cb);
    }

    @SuppressWarnings("unchecked")
    private DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> buildStateUpdateCallbackMock() {
        // This needs an unchecked cast because of the generic; this method just centralizes the
        // warning suppression.
        return mock(DynamicTypeValueReceiverWithPreUpdate.class);
    }

    private StateEntryValue buildStateEntry(String value) {
        return StateEntryValue.newBuilder()
                .setStringVal(FixedString.newBuilder().setValue(value))
                .build();
    }
}
