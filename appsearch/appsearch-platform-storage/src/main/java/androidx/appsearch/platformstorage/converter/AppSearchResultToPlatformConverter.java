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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import java.util.Map;
import java.util.function.Function;

/**
 * Translates {@link androidx.appsearch.app.AppSearchResult} and
 * {@link androidx.appsearch.app.AppSearchBatchResult} to platform versions.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class AppSearchResultToPlatformConverter {
    private AppSearchResultToPlatformConverter() {}

    /**
     * Converts an {@link android.app.appsearch.AppSearchResult} into a jetpack
     * {@link androidx.appsearch.app.AppSearchResult}.
     */
    @NonNull
    public static <PlatformType, JetpackType> AppSearchResult<JetpackType>
            platformAppSearchResultToJetpack(
            @NonNull android.app.appsearch.AppSearchResult<PlatformType> platformResult,
            @NonNull Function<PlatformType, JetpackType> valueMapper) {
        Preconditions.checkNotNull(platformResult);
        if (platformResult.isSuccess()) {
            try {
                JetpackType jetpackType = valueMapper.apply(platformResult.getResultValue());
                return AppSearchResult.newSuccessfulResult(jetpackType);
            } catch (Throwable t) {
                return AppSearchResult.throwableToFailedResult(t);
            }
        }
        return AppSearchResult.newFailedResult(
                platformResult.getResultCode(), platformResult.getErrorMessage());
    }

    /**
     * Uses the given {@link android.app.appsearch.AppSearchResult} to populate the given
     * {@link ResolvableFuture}, transforming it using {@code valueMapper}.
     */
    public static <PlatformType, JetpackType> void platformAppSearchResultToFuture(
            @NonNull android.app.appsearch.AppSearchResult<PlatformType> platformResult,
            @NonNull ResolvableFuture<JetpackType> future,
            @NonNull Function<PlatformType, JetpackType> valueMapper) {
        Preconditions.checkNotNull(platformResult);
        Preconditions.checkNotNull(future);
        if (platformResult.isSuccess()) {
            try {
                JetpackType jetpackType = valueMapper.apply(platformResult.getResultValue());
                future.set(jetpackType);
            } catch (Throwable t) {
                future.setException(t);
            }
        } else {
            future.setException(
                    new AppSearchException(
                            platformResult.getResultCode(), platformResult.getErrorMessage()));
        }
    }

    /**
     * Uses the given {@link android.app.appsearch.AppSearchResult} to populate the given
     * {@link ResolvableFuture}.
     */
    public static <T> void platformAppSearchResultToFuture(
            @NonNull android.app.appsearch.AppSearchResult<T> platformResult,
            @NonNull ResolvableFuture<T> future) {
        platformAppSearchResultToFuture(platformResult, future, Function.identity());
    }

    /**
     * Converts the given platform {@link android.app.appsearch.AppSearchBatchResult} to a Jetpack
     * {@link AppSearchBatchResult}.
     *
     * <p>Each value is translated using the provided {@code valueMapper} function.
     */
    @NonNull
    public static <K, PlatformValue, JetpackValue> AppSearchBatchResult<K, JetpackValue>
            platformAppSearchBatchResultToJetpack(
            @NonNull android.app.appsearch.AppSearchBatchResult<K, PlatformValue> platformResult,
            @NonNull Function<PlatformValue, JetpackValue> valueMapper) {
        Preconditions.checkNotNull(platformResult);
        Preconditions.checkNotNull(valueMapper);
        AppSearchBatchResult.Builder<K, JetpackValue> jetpackResult =
                new AppSearchBatchResult.Builder<>();
        for (Map.Entry<K, PlatformValue> success : platformResult.getSuccesses().entrySet()) {
            try {
                JetpackValue jetpackValue = valueMapper.apply(success.getValue());
                jetpackResult.setSuccess(success.getKey(), jetpackValue);
            } catch (Throwable t) {
                jetpackResult.setResult(
                        success.getKey(), AppSearchResult.throwableToFailedResult(t));
            }
        }
        for (Map.Entry<K, android.app.appsearch.AppSearchResult<PlatformValue>> failure :
                platformResult.getFailures().entrySet()) {
            jetpackResult.setFailure(
                    failure.getKey(),
                    failure.getValue().getResultCode(),
                    failure.getValue().getErrorMessage());
        }
        return jetpackResult.build();
    }
}
