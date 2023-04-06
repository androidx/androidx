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

package androidx.wear.protolayout.expression.pipeline;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import java.util.function.Function;

class StateSourceNode<T>
        implements DynamicDataSourceNode<T>,
        DynamicTypeValueReceiverWithPreUpdate<StateEntryValue> {
    private final StateStore mStateStore;
    private final DynamicDataKey<?> mKey;
    private final Function<StateEntryValue, T> mStateExtractor;
    private final DynamicTypeValueReceiverWithPreUpdate<T> mDownstream;

    StateSourceNode(
            StateStore stateStore,
            DynamicDataKey<?> key,
            Function<StateEntryValue, T> stateExtractor,
            DynamicTypeValueReceiverWithPreUpdate<T> downstream) {
        this.mStateStore = stateStore;
        this.mKey = key;
        this.mStateExtractor = stateExtractor;
        this.mDownstream = downstream;
    }

    @Override
    @UiThread
    public void preInit() {
        mDownstream.onPreUpdate();
    }

    @Override
    @UiThread
    public void init() {
        mStateStore.registerCallback(mKey, this);
        StateEntryValue item = mStateStore.getStateEntryValuesProto(mKey);

        if (item != null) {
            this.onData(item);
        } else {
            this.onInvalidated();
        }
    }

    @Override
    @UiThread
    public void destroy() {
        mStateStore.unregisterCallback(mKey, this);
    }

    @Override
    public void onPreUpdate() {
        mDownstream.onPreUpdate();
    }

    @Override
    public void onData(@NonNull StateEntryValue newData) {
        T actualValue = mStateExtractor.apply(newData);
        mDownstream.onData(actualValue);
    }

    @Override
    public void onInvalidated() {
        mDownstream.onInvalidated();
    }

    @NonNull
    static <T extends DynamicType> DynamicDataKey<T> createKey(
           @NonNull String namespace, @NonNull String key) {
        if (namespace.isEmpty()) {
            return new AppDataKey<T>(key);
        }
        return new PlatformDataKey<T>(namespace, key);
    }
}
