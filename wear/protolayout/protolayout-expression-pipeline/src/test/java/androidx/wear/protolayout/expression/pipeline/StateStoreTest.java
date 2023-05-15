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

import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class StateStoreTest {
    @Rule public Expect mExpect = Expect.create();

    private static final AppDataKey<DynamicString> KEY_FOO = new AppDataKey<>("foo");
    private static final AppDataKey<DynamicString> KEY_BAZ = new AppDataKey<>("baz");

    private static final PlatformDataKey<DynamicString> KEY_FOO_PLATFORM
            = new PlatformDataKey<>("platform", "foo");
    private static final PlatformDataKey<DynamicString> KEY_BAZ_PLATFORM
            = new PlatformDataKey<>("platform","baz");

    private final StateStore mStateStoreUnderTest =
            new StateStore(
                    ImmutableMap.of(
                            KEY_FOO, buildDynamicDataValue("bar"),
                            KEY_BAZ, buildDynamicDataValue("foobar")));

    public StateStoreTest() {}

    @Test
    public void setBuilderApi() {
        mStateStoreUnderTest.setAppStateEntryValues(
                ImmutableMap.of(
                        KEY_FOO, DynamicDataBuilders.DynamicDataValue.fromString("baz")));

        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO))
                .isEqualTo(buildDynamicDataValue("baz"));

        // This should have been cleared...
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ)).isNull();
    }

    @Test
    public void initState_largeNumberOfEntries_throws() {
        Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.getMaxStateEntryCount() + 10; i++) {
            state.put(
                    new AppDataKey<DynamicString>(Integer.toString(i)),
                    DynamicDataBuilders.DynamicDataValue.fromString("baz"));
        }
        assertThrows(IllegalStateException.class, () -> StateStore.create(state));
    }

    @Test
    public void newState_largeNumberOfEntries_throws() {
        Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue> state = new HashMap<>();
        for (int i = 0; i < StateStore.getMaxStateEntryCount() + 10; i++) {
            state.put(
                    new AppDataKey<DynamicString>(Integer.toString(i)),
                    DynamicDataBuilders.DynamicDataValue.fromString("baz"));
        }
        assertThrows(
                IllegalStateException.class,
                () -> mStateStoreUnderTest.setAppStateEntryValues(state));
    }

    @Test
    public void canReadInitialState() {
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO))
                .isEqualTo(buildDynamicDataValue("bar"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ))
                .isEqualTo(buildDynamicDataValue("foobar"));
    }

    @Test
    public void unsetStateReturnsNull() {
        mExpect.that(
                mStateStoreUnderTest.getDynamicDataValuesProto(new AppDataKey<>("AAAAAA"))
                ).isNull();
    }

    @Test
    public void canSetNewState() {
        AppDataKey<DynamicString> keyNew = new AppDataKey<>("newKey");
        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildDynamicDataValue("test"),
                        keyNew, buildDynamicDataValue("testNewKey")));

        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO))
                .isEqualTo(buildDynamicDataValue("test"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(keyNew))
                .isEqualTo(buildDynamicDataValue("testNewKey"));

        // This should have been cleared...
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ)).isNull();
    }

    @Test
    public void canUpdatePlatformData() {
        mStateStoreUnderTest.updatePlatformDataEntryProto(
                Map.of(KEY_FOO_PLATFORM, buildDynamicDataValue("valueFoo1")));

        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO))
                .isEqualTo(buildDynamicDataValue("bar"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ))
                .isEqualTo(buildDynamicDataValue("foobar"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo1"));

        mStateStoreUnderTest.updatePlatformDataEntryProto(
                Map.of(KEY_FOO_PLATFORM, buildDynamicDataValue("valueFoo2"),
                        KEY_BAZ_PLATFORM, buildDynamicDataValue("valueBaz")));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo2"));

        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO))
                .isEqualTo(buildDynamicDataValue("bar"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ))
                .isEqualTo(buildDynamicDataValue("foobar"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo2"));
        mExpect.that(mStateStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueBaz"));
    }

    @Test
    public void setStateFiresListeners() {
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildDynamicDataValue("test")));

        verify(cb).onPreUpdate();
        verify(cb).onData(buildDynamicDataValue("test"));
    }

    @Test
    public void platformDataProvider_register_updateData_unregister() {
        PlatformDataProviderUnderTest dataProvider = new PlatformDataProviderUnderTest();
        Map<PlatformDataKey<?>, PlatformDataProvider> sourceKeyToDataProvider = new ArrayMap<>();
        sourceKeyToDataProvider.put(KEY_FOO_PLATFORM, dataProvider);
        sourceKeyToDataProvider.put(KEY_BAZ_PLATFORM, dataProvider);
        mStateStoreUnderTest.putAllPlatformProviders(sourceKeyToDataProvider);

        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbFoo =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO_PLATFORM, cbFoo);
        verify(cbFoo).onPreUpdate();
        verify(cbFoo).onData(buildDynamicDataValue("fooValue"));
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(1);

        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbBaz =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_BAZ_PLATFORM, cbBaz);
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(1);

        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbFoo2 =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO_PLATFORM, cbFoo2);
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(1);

        dataProvider.updateValues(
                Map.of(KEY_FOO_PLATFORM,
                        DynamicDataBuilders.DynamicDataValue.fromString("newFooValue"),
                        KEY_BAZ_PLATFORM,
                        DynamicDataBuilders.DynamicDataValue.fromString("newBazValue")
                ));
        verify(cbFoo, times(2)).onPreUpdate();
        verify(cbFoo2).onPreUpdate();
        verify(cbBaz).onPreUpdate();
        verify(cbFoo).onData(buildDynamicDataValue("newFooValue"));
        verify(cbFoo2).onData(buildDynamicDataValue("newFooValue"));
        verify(cbBaz).onData(buildDynamicDataValue("newBazValue"));

        dataProvider.updateValues(
                Map.of(
                        KEY_BAZ_PLATFORM,
                        DynamicDataBuilders.DynamicDataValue.fromString("updatedBazValue")
                ));
        verify(cbFoo, times(1)).onData(buildDynamicDataValue("newFooValue"));
        verify(cbFoo2, times(1)).onData(buildDynamicDataValue("newFooValue"));
        verify(cbBaz).onData(buildDynamicDataValue("newBazValue"));

        mStateStoreUnderTest.unregisterCallback(KEY_FOO_PLATFORM, cbFoo);
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(1);
        mStateStoreUnderTest.unregisterCallback(KEY_FOO_PLATFORM, cbFoo2);
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(1);
        mStateStoreUnderTest.unregisterCallback(KEY_BAZ_PLATFORM, cbBaz);
        mExpect.that(dataProvider.mRegisterCount).isEqualTo(0);
    }

    @Test
    public void setStateFiresOnPreStateUpdateFirst() {
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cb = buildStateUpdateCallbackMock();

        InOrder inOrder = Mockito.inOrder(cb);

        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);
        mStateStoreUnderTest.registerCallback(KEY_BAZ, cb);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildDynamicDataValue("testFoo"),
                        KEY_BAZ, buildDynamicDataValue("testBaz")));

        inOrder.verify(cb, times(2)).onPreUpdate();
        inOrder.verify(cb, times(2)).onData(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void setStateOnlyFiresListenersForChangedData() {
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbFoo =
                buildStateUpdateCallbackMock();
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbBaz =
                buildStateUpdateCallbackMock();

        mStateStoreUnderTest.registerCallback(KEY_FOO, cbFoo);
        mStateStoreUnderTest.registerCallback(KEY_BAZ, cbBaz);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        KEY_FOO, buildDynamicDataValue("test"),
                        KEY_BAZ, buildDynamicDataValue("foobar")));

        verify(cbFoo).onPreUpdate();
        verify(cbFoo).onData(buildDynamicDataValue("test"));
        verify(cbBaz, never()).onPreUpdate();
        verify(cbBaz, never()).onData(buildDynamicDataValue("test"));
    }

    @Test
    public void removeStateFiresInvalidated() {
        AppDataKey<DynamicString> keyInvalidated = new AppDataKey<>("invalidated");
        AppDataKey<DynamicString> keyNotInvalidated = new AppDataKey<>("notInvalidated");
        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyInvalidated,
                        buildDynamicDataValue("value"),
                        keyNotInvalidated,
                        buildDynamicDataValue("value")));
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> invalidated =
                buildStateUpdateCallbackMock();
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> notInvalidated =
                buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(keyInvalidated, invalidated);
        mStateStoreUnderTest.registerCallback(keyNotInvalidated, notInvalidated);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(keyNotInvalidated, buildDynamicDataValue("value")));

        verify(invalidated).onPreUpdate();
        verify(invalidated).onInvalidated();
        verify(invalidated, never()).onData(any());
        verifyNoInteractions(notInvalidated);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canUnregisterListeners() {
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cb = buildStateUpdateCallbackMock();
        mStateStoreUnderTest.registerCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildDynamicDataValue("test")));

        reset(cb);

        mStateStoreUnderTest.unregisterCallback(KEY_FOO, cb);

        mStateStoreUnderTest.setAppStateEntryValuesProto(
                ImmutableMap.of(KEY_FOO, buildDynamicDataValue("testAgain")));

        verifyNoInteractions(cb);
    }

    @SuppressWarnings("unchecked")
    private DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> buildStateUpdateCallbackMock() {
        // This needs an unchecked cast because of the generic; this method just centralizes the
        // warning suppression.
        return mock(DynamicTypeValueReceiverWithPreUpdate.class);
    }

    private DynamicDataValue buildDynamicDataValue(String value) {
        return DynamicDataValue.newBuilder()
                .setStringVal(FixedString.newBuilder().setValue(value))
                .build();
    }

    private class PlatformDataProviderUnderTest implements PlatformDataProvider {

        private PlatformDataReceiver mRegisteredCallback = null;

        private final Map<PlatformDataKey<?>, DynamicDataBuilders.DynamicDataValue> mCurrentValue =
                new ArrayMap<>();

        private final Set<PlatformDataKey<?>> mSupportedKeys= new ArraySet<>();

        int mRegisterCount = 0;

        PlatformDataProviderUnderTest() {
            mSupportedKeys.add(KEY_FOO_PLATFORM);
            mSupportedKeys.add(KEY_BAZ_PLATFORM);
            mCurrentValue.put(
                    KEY_FOO_PLATFORM,
                    DynamicDataBuilders.DynamicDataValue.fromString("fooValue")
            );
            mCurrentValue.put(
                    KEY_BAZ_PLATFORM,
                    DynamicDataBuilders.DynamicDataValue.fromString("bazValue")
            );
        }

        public void updateValues(
                @NonNull Map<PlatformDataKey<?> , DynamicDataBuilders.DynamicDataValue> newData) {
            mCurrentValue.putAll(newData);
            if (mRegisteredCallback != null) {
                mRegisteredCallback.onData(mCurrentValue);
            }
        }

        @Override
        public void registerForData(
                @NonNull Executor executor,
                @NonNull PlatformDataReceiver callback) {
                mRegisterCount++;
                mRegisteredCallback = callback;
                executor.execute(() -> callback.onData(mCurrentValue));
        }

        @Override
        public void unregisterForData() {
            if (mRegisteredCallback != null) {
                mRegisteredCallback.onInvalidated(mSupportedKeys);
                mRegisterCount--;
                mRegisteredCallback = null;
            }
        }
    }
}
