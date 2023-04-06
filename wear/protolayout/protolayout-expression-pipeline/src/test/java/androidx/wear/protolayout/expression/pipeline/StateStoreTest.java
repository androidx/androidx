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
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
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

    private static final AppDataKey<DynamicString> KEY_FOO = new AppDataKey<>("foo");
    private static final AppDataKey<DynamicString> KEY_BAZ = new AppDataKey<>("baz");

    private final StateStore mStateStoreUnderTest =
            new StateStore(
                    ImmutableMap.of(
                            KEY_FOO, buildStateEntry("bar"),
                            KEY_BAZ, buildStateEntry("foobar")));

    public StateStoreTest() {}

    @Test
    public void setBuilderApi() {
        mStateStoreUnderTest.setStateEntryValues(
                ImmutableMap.of(
                        KEY_FOO, StateEntryBuilders.StateEntryValue.fromString("baz")));

        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(KEY_FOO))
                .isEqualTo(buildStateEntry("baz"));
    }

    @Test
    public void initState_largeNumberOfEntries_throws() {
        Map<AppDataKey<?>, StateEntryBuilders.StateEntryValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.MAX_STATE_ENTRY_COUNT + 10; i++) {
            state.put(
                    new AppDataKey<DynamicString>(Integer.toString(i)),
                    StateEntryBuilders.StateEntryValue.fromString("baz"));
        }
        assertThrows(IllegalStateException.class, () -> StateStore.create(state));
    }

    @Test
    public void newState_largeNumberOfEntries_throws() {
        Map<AppDataKey<?>, StateEntryBuilders.StateEntryValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.MAX_STATE_ENTRY_COUNT + 10; i++) {
            state.put(
                    new AppDataKey<DynamicString>(Integer.toString(i)),
                    StateEntryBuilders.StateEntryValue.fromString("baz"));
        }
        assertThrows(
                IllegalStateException.class, () -> mStateStoreUnderTest.setStateEntryValues(state));
    }

    @Test
    public void canReadInitialState() {
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(KEY_FOO))
                .isEqualTo(buildStateEntry("bar"));
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(KEY_BAZ))
                .isEqualTo(buildStateEntry("foobar"));
    }

    @Test
    public void unsetStateReturnsNull() {
        mExpect.that(
                mStateStoreUnderTest.getStateEntryValuesProto(new AppDataKey<>("AAAAAA"))
                ).isNull();
    }

    @Test
    public void canSetNewState() {
        AppDataKey<DynamicString> keyNew = new AppDataKey<>("newKey");
        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildStateEntry("test"),
                        keyNew, buildStateEntry("testNewKey")));

        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(KEY_FOO))
                .isEqualTo(buildStateEntry("test"));
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(keyNew))
                .isEqualTo(buildStateEntry("testNewKey"));

        // This should have been cleared...
        mExpect.that(mStateStoreUnderTest.getStateEntryValuesProto(KEY_BAZ)).isNull();
    }

    @Test
    public void setStateFiresListeners() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildStateEntry("test")));

        verify(cb).onPreUpdate();
        verify(cb).onData(buildStateEntry("test"));
    }

    @Test
    public void setStateFiresOnPreStateUpdateFirst() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();

        InOrder inOrder = Mockito.inOrder(cb);

        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);
        mStateStoreUnderTest.registerCallback(KEY_BAZ, cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildStateEntry("testFoo"),
                        KEY_BAZ, buildStateEntry("testBaz")));

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
        mStateStoreUnderTest.registerCallback(KEY_FOO, cbFoo);
        mStateStoreUnderTest.registerCallback(KEY_BAZ, cbBaz);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildStateEntry("test"),
                        KEY_BAZ, buildStateEntry("foobar")));

        verify(cbFoo).onPreUpdate();
        verify(cbFoo).onData(buildStateEntry("test"));
        verify(cbBaz, never()).onPreUpdate();
        verify(cbBaz, never()).onData(buildStateEntry("test"));
    }

    @Test
    public void removeStateFiresInvalidated() {
        AppDataKey<DynamicString> keyInvalidated = new AppDataKey<>("invalidated");
        AppDataKey<DynamicString> keyNotInvalidated = new AppDataKey<>("notInvalidated");
        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(
                        keyInvalidated,
                        buildStateEntry("value"),
                        keyNotInvalidated,
                        buildStateEntry("value")));
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> invalidated =
                buildStateUpdateCallbackMock();
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> notInvalidated =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(keyInvalidated, invalidated);
        mStateStoreUnderTest.registerCallback(keyNotInvalidated, notInvalidated);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(keyNotInvalidated, buildStateEntry("value")));

        verify(invalidated).onPreUpdate();
        verify(invalidated).onInvalidated();
        verify(invalidated, never()).onData(any());
        verifyNoInteractions(notInvalidated);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canUnregisterListeners() {
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildStateEntry("test")));

        reset(cb);
        mStateStoreUnderTest.unregisterCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildStateEntry("testAgain")));

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
