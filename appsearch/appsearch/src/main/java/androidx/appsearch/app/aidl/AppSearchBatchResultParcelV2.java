/*
 * Copyright 2024 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app.aidl;

import android.os.ParcelFileDescriptor;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * A dummy version of AppSearchBatchResultParcelV2 in jetpack.
 * @param <KeyType> The type of keys in the batch result, such as {@link AppSearchBlobHandle}.
 * @param <ValueType> The type of values in the batch result, such as {@link ParcelFileDescriptor}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchBatchResultParcelV2<KeyType, ValueType> {
    private final AppSearchBatchResult<KeyType, ValueType> mResult;

    private AppSearchBatchResultParcelV2(
            @NonNull AppSearchBatchResult<KeyType, ValueType> result) {
        mResult = Preconditions.checkNotNull(result);
    }

    /**
     * Creates an instance of {@link AppSearchBatchResultParcelV2} with key type
     * {@link AppSearchBlobHandle} and value type {@link ParcelFileDescriptor}.
     */
    public static @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, ParcelFileDescriptor>
            fromBlobHandleToPfd(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> result) {
        return new AppSearchBatchResultParcelV2<>(result);
    }

    /**
     * Creates an instance of {@link AppSearchBatchResultParcelV2} with key type
     * {@link AppSearchBlobHandle} and value type {@link Void}.
     */
    public static @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void>
            fromBlobHandleToVoid(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> result) {
        return new AppSearchBatchResultParcelV2<>(result);
    }

    /** Returns the wrapped batch result.  */
    public @NonNull AppSearchBatchResult<KeyType, ValueType> getResult() {
        return mResult;
    }
}
