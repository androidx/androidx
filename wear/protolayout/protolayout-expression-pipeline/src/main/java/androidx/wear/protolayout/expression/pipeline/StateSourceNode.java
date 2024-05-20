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
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;

import java.util.function.Function;

class StateSourceNode<T>
        implements DynamicDataSourceNode<T>,
                DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> {
    @NonNull private static final String RESERVED_NAMESPACE = "protolayout";
    private final DataStore mDataStore;
    private final DynamicDataKey<?> mKey;
    private final Function<DynamicDataValue, T> mStateExtractor;
    private final DynamicTypeValueReceiverWithPreUpdate<T> mDownstream;
    private volatile boolean mInitialDataSent = false;

    StateSourceNode(
            DataStore dataStore,
            DynamicDataKey<?> key,
            Function<DynamicDataValue, T> stateExtractor,
            DynamicTypeValueReceiverWithPreUpdate<T> downstream) {
        this.mDataStore = dataStore;
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
        mDataStore.registerCallback(mKey, this);
        if (mInitialDataSent) {
            // During the registration line above, a provider can push a new data causing the
            // onData/onInvalidated to be called. This method runs on Ui thread and the
            // PlatformDataStore uses a MainThreadExecutor, so we don't need additional
            // synchronization for avoiding double data delivery.
            return;
        }

        DynamicDataValue item = mDataStore.getDynamicDataValuesProto(mKey);

        if (item != null) {
            this.onData(item);
        } else {
            this.onInvalidated();
        }
    }

    @Override
    @UiThread
    public void destroy() {
        mDataStore.unregisterCallback(mKey, this);
    }

    @Override
    public void onPreUpdate() {
        if (!mInitialDataSent) {
            // Some PlatformDataProviders push a new data as soon as they are registered with
            // PlatformDataStore in {@link #init()}. This means we might end up calling onPreUpdate
            // twice. The only preUpdate before the first data should be trigged by {@link
            // #preInit()}
            return;
        }
        mDownstream.onPreUpdate();
    }

    @Override
    public void onData(@NonNull DynamicDataValue newData) {
        T actualValue = mStateExtractor.apply(newData);
        if (actualValue == null) {
            this.onInvalidated();
        } else {
            mDownstream.onData(actualValue);
        }
        mInitialDataSent = true;
    }

    @Override
    public void onInvalidated() {
        mDownstream.onInvalidated();
        mInitialDataSent = true;
    }

    @NonNull
    static <T extends DynamicType> DynamicDataKey<T> createKey(
            @NonNull String namespace, @NonNull String key) {
        if (namespace.isEmpty()) {
            return new AppDataKey<T>(key);
        }

        if (RESERVED_NAMESPACE.equalsIgnoreCase(namespace)) {
            return new PlatformDataKey<T>(key);
        }

        return new PlatformDataKey<T>(namespace, key);
    }

    @Override
    public int getCost() {
        return DEFAULT_NODE_COST;
    }
}
