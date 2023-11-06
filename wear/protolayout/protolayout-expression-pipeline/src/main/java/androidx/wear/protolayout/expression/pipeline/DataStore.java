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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;

/**
 * {@link DynamicDataKey} to {@link DynamicDataValue} storage for ProtoLayout, which also supports
 * sending callback when data items change.
 */
abstract class DataStore {
    /**
     * Registers the given callback for updates to the data item for the given {@code key}.
     *
     * <p>Note that the callback will be executed on the UI thread.
     */
    abstract void registerCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback);

    /** Unregisters the callback for the given {@code key} from receiving the updates. */
    abstract void unregisterCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback);

    /**
     * Retrieve the {@link DynamicDataValue} associated with the given {@link DynamicDataKey} in
     * the store.
     */
    @Nullable
    abstract DynamicDataValue getDynamicDataValuesProto(@NonNull DynamicDataKey<?> key);
}
