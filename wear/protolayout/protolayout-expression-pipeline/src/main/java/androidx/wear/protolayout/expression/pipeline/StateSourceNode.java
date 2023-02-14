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
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import java.util.function.Function;

class StateSourceNode<T>
        implements DynamicDataSourceNode<T>, DynamicTypeValueReceiver<StateEntryValue> {
    private final ObservableStateStore mObservableStateStore;
    private final String mBindKey;
    private final Function<StateEntryValue, T> mStateExtractor;
    private final DynamicTypeValueReceiver<T> mDownstream;

    StateSourceNode(
            ObservableStateStore observableStateStore,
            String bindKey,
            Function<StateEntryValue, T> stateExtractor,
            DynamicTypeValueReceiver<T> downstream) {
        this.mObservableStateStore = observableStateStore;
        this.mBindKey = bindKey;
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
        mObservableStateStore.registerCallback(mBindKey, this);
        StateEntryValue item = mObservableStateStore.getStateEntryValuesProto(mBindKey);

        if (item != null) {
            this.onData(item);
        } else {
            this.onInvalidated();
        }
    }

    @Override
    @UiThread
    public void destroy() {
        mObservableStateStore.unregisterCallback(mBindKey, this);
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
}
