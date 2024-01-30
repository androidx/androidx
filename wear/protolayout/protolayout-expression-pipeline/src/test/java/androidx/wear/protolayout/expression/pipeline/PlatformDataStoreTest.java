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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.FixedProto;

import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class PlatformDataStoreTest {
    @Rule public Expect mExpect = Expect.create();
    private static final PlatformDataKey<DynamicBuilders.DynamicString> KEY_FOO_PLATFORM =
            new PlatformDataKey<>("platform", "foo");
    private static final PlatformDataKey<DynamicBuilders.DynamicString> KEY_BAZ_PLATFORM =
            new PlatformDataKey<>("platform", "baz");

    private final PlatformDataProviderUnderTest mDataProvider = new PlatformDataProviderUnderTest();
    private final PlatformDataStore mDataStoreUnderTest = new PlatformDataStore(
            Map.of(
                    KEY_FOO_PLATFORM, mDataProvider,
                    KEY_BAZ_PLATFORM, mDataProvider
            )
    );

    public PlatformDataStoreTest() {}

    @Test
    public void canUpdatePlatformData() {
        mDataStoreUnderTest.updatePlatformDataEntriesProto(
                Map.of(KEY_FOO_PLATFORM, buildDynamicDataValue("valueFoo1")));

        mExpect.that(mDataStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo1"));

        mDataStoreUnderTest.updatePlatformDataEntriesProto(
                Map.of(KEY_FOO_PLATFORM, buildDynamicDataValue("valueFoo2"),
                        KEY_BAZ_PLATFORM, buildDynamicDataValue("valueBaz")));
        mExpect.that(mDataStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo2"));

        mExpect.that(mDataStoreUnderTest.getDynamicDataValuesProto(KEY_FOO_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueFoo2"));
        mExpect.that(mDataStoreUnderTest.getDynamicDataValuesProto(KEY_BAZ_PLATFORM))
                .isEqualTo(buildDynamicDataValue("valueBaz"));
    }
    @Test
    public void platformDataProvider_register_updateData_unregister() {
        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbFoo =
                buildStateUpdateCallbackMock();
        mDataStoreUnderTest.registerCallback(KEY_FOO_PLATFORM, cbFoo);
        verify(cbFoo).onPreUpdate();
        verify(cbFoo).onData(buildDynamicDataValue("fooValue"));
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(1);

        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbBaz =
                buildStateUpdateCallbackMock();
        mDataStoreUnderTest.registerCallback(KEY_BAZ_PLATFORM, cbBaz);
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(1);

        DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> cbFoo2 =
                buildStateUpdateCallbackMock();
        mDataStoreUnderTest.registerCallback(KEY_FOO_PLATFORM, cbFoo2);
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(1);

        mDataProvider.updateValues(
                new PlatformDataValues.Builder()
                        .put(
                                KEY_FOO_PLATFORM,
                                DynamicDataBuilders.DynamicDataValue.fromString("newFooValue"))
                        .put(
                                KEY_BAZ_PLATFORM,
                                DynamicDataBuilders.DynamicDataValue.fromString("newBazValue"))
                        .build());
        verify(cbFoo, times(2)).onPreUpdate();
        verify(cbFoo2).onPreUpdate();
        verify(cbBaz).onPreUpdate();
        verify(cbFoo).onData(buildDynamicDataValue("newFooValue"));
        verify(cbFoo2).onData(buildDynamicDataValue("newFooValue"));
        verify(cbBaz).onData(buildDynamicDataValue("newBazValue"));

        mDataProvider.updateValues(
                PlatformDataValues.of(
                        KEY_BAZ_PLATFORM,
                        DynamicDataBuilders.DynamicDataValue.fromString("updatedBazValue")));
        verify(cbFoo, times(1)).onData(buildDynamicDataValue("newFooValue"));
        verify(cbFoo2, times(1)).onData(buildDynamicDataValue("newFooValue"));
        verify(cbBaz).onData(buildDynamicDataValue("newBazValue"));

        mDataStoreUnderTest.unregisterCallback(KEY_FOO_PLATFORM, cbFoo);
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(1);
        mDataStoreUnderTest.unregisterCallback(KEY_FOO_PLATFORM, cbFoo2);
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(1);
        mDataStoreUnderTest.unregisterCallback(KEY_BAZ_PLATFORM, cbBaz);
        mExpect.that(mDataProvider.mRegisterCount).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    private DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> buildStateUpdateCallbackMock() {
        // This needs an unchecked cast because of the generic; this method just centralizes the
        // warning suppression.
        return mock(DynamicTypeValueReceiverWithPreUpdate.class);
    }

    private DynamicDataValue buildDynamicDataValue(String value) {
        return DynamicDataValue.newBuilder()
                .setStringVal(FixedProto.FixedString.newBuilder().setValue(value))
                .build();
    }

    private class PlatformDataProviderUnderTest implements PlatformDataProvider {

        private PlatformDataReceiver mRegisteredCallback = null;

        private final PlatformDataValues.Builder mCurrentValue = new PlatformDataValues.Builder();

        private final Set<PlatformDataKey<?>> mSupportedKeys = new ArraySet<>();

        int mRegisterCount = 0;

        PlatformDataProviderUnderTest() {
            mSupportedKeys.add(KEY_FOO_PLATFORM);
            mSupportedKeys.add(KEY_BAZ_PLATFORM);
            mCurrentValue.put(
                    KEY_FOO_PLATFORM, DynamicDataBuilders.DynamicDataValue.fromString("fooValue"));
            mCurrentValue.put(
                    KEY_BAZ_PLATFORM, DynamicDataBuilders.DynamicDataValue.fromString("bazValue"));
        }

        public void updateValues(
                @NonNull PlatformDataValues newData) {
            mCurrentValue.putAll(newData);
            if (mRegisteredCallback != null) {
                mRegisteredCallback.onData(mCurrentValue.build());
            }
        }

        @Override
        public void setReceiver(
                @NonNull Executor executor,
                @NonNull PlatformDataReceiver callback) {
            mRegisterCount++;
            mRegisteredCallback = callback;
            executor.execute(() -> callback.onData(mCurrentValue.build()));
        }

        @Override
        public void clearReceiver() {
            if (mRegisteredCallback != null) {
                mRegisteredCallback.onInvalidated(mSupportedKeys);
                mRegisterCount--;
                mRegisteredCallback = null;
            }
        }
    }
}
