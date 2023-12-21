/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.appsearch.platformstorage.util;

import android.app.appsearch.BatchResultCallback;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.platformstorage.converter.AppSearchResultToPlatformConverter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import java.util.function.Function;

/**
 * An implementation of the framework API's {@link android.app.appsearch.BatchResultCallback} which
 * return the result as a {@link com.google.common.util.concurrent.ListenableFuture}.
 *
 * @param <K>             The type of key in the batch result (both Framework and Jetpack)
 * @param <PlatformValue> The type of value in the Framework's
 *                        {@link android.app.appsearch.AppSearchBatchResult}.
 * @param <JetpackValue>  The type of value in Jetpack's {@link AppSearchBatchResult}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class BatchResultCallbackAdapter<K, PlatformValue, JetpackValue>
        implements BatchResultCallback<K, PlatformValue> {
    private final ResolvableFuture<AppSearchBatchResult<K, JetpackValue>> mFuture;
    private final Function<PlatformValue, JetpackValue> mValueMapper;

    public BatchResultCallbackAdapter(
            @NonNull ResolvableFuture<AppSearchBatchResult<K, JetpackValue>> future,
            @NonNull Function<PlatformValue, JetpackValue> valueMapper) {
        mFuture = Preconditions.checkNotNull(future);
        mValueMapper = Preconditions.checkNotNull(valueMapper);
    }

    @Override
    public void onResult(
            @NonNull android.app.appsearch.AppSearchBatchResult<K, PlatformValue> platformResult) {
        AppSearchBatchResult<K, JetpackValue> jetpackResult =
                AppSearchResultToPlatformConverter.platformAppSearchBatchResultToJetpack(
                        platformResult, mValueMapper);
        mFuture.set(jetpackResult);
    }

    @Override
    public void onSystemError(@Nullable Throwable t) {
        mFuture.setException(t);
    }

    /**
     * Returns a {@link androidx.appsearch.platformstorage.util.BatchResultCallbackAdapter} where
     * the Platform value is identical to the Jetpack value, needing no transformation.
     */
    @NonNull
    public static <K, V> BatchResultCallbackAdapter<K, V, V> forSameValueType(
            @NonNull ResolvableFuture<AppSearchBatchResult<K, V>> future) {
        return new BatchResultCallbackAdapter<>(future, Function.identity());
    }
}
