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

package androidx.appsearch.playservicesstorage.converter;

import static com.google.android.gms.appsearch.AppSearchResult.RESULT_NOT_FOUND;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Function;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Translates {@link androidx.appsearch.app.AppSearchResult} and
 * {@link androidx.appsearch.app.AppSearchBatchResult} to Gms versions.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchResultToGmsConverter {
    private AppSearchResultToGmsConverter() {
    }

    /**
     * Converts an {@link com.google.android.gms.appsearch.AppSearchResult} into a jetpack
     * {@link androidx.appsearch.app.AppSearchResult}.
     */
    public static <GmsType, JetpackType> @NonNull AppSearchResult<JetpackType>
            gmsAppSearchResultToJetpack(
            com.google.android.gms.appsearch.@NonNull AppSearchResult<GmsType> gmsResult,
            @NonNull Function<GmsType, JetpackType> valueMapper) {
        Preconditions.checkNotNull(gmsResult);
        if (gmsResult.isSuccess()) {
            try {
                JetpackType jetpackType = valueMapper.apply(gmsResult.getResultValue());
                return AppSearchResult.newSuccessfulResult(jetpackType);
            } catch (Throwable t) {
                return AppSearchResult.throwableToFailedResult(t);
            }
        }
        return AppSearchResult.newFailedResult(
                gmsResult.getResultCode(),
                gmsResult.getErrorMessage());
    }

    /**
     * Uses the given {@link com.google.android.gms.appsearch.AppSearchResult} to populate the given
     * {@link ResolvableFuture}, transforming it using {@code valueMapper}.
     */
    public static <GmsType, JetpackType> void
            gmsAppSearchResultToFuture(
            com.google.android.gms.appsearch.@NonNull AppSearchResult<GmsType>
                    gmsResult,
            @NonNull ResolvableFuture<JetpackType> future,
            @NonNull Function<GmsType, JetpackType> valueMapper) {
        Preconditions.checkNotNull(gmsResult);
        Preconditions.checkNotNull(future);
        if (gmsResult.isSuccess()) {
            try {
                JetpackType jetpackType = valueMapper.apply(gmsResult.getResultValue());
                future.set(jetpackType);
            } catch (Throwable t) {
                future.setException(t);
            }
        } else {
            future.setException(new AppSearchException(
                    gmsResult.getResultCode(), gmsResult.getErrorMessage()));
        }
    }

    /**
     * Uses the given {@link com.google.android.gms.appsearch.AppSearchResult}
     * to populate the given {@link ResolvableFuture}.
     */
    public static <T> void gmsAppSearchResultToFuture(
            com.google.android.gms.appsearch.@NonNull AppSearchResult<T> gmsResult,
            @NonNull ResolvableFuture<T> future) {
        gmsAppSearchResultToFuture(gmsResult,
                future,
                /* valueMapper= */ i -> i);
    }

    /**
     * Converts the given Gms
     * {@link com.google.android.gms.appsearch.AppSearchBatchResult} to a Jetpack
     * {@link AppSearchBatchResult}.
     *
     * <p>Each value is translated using the provided {@code valueMapper} function.
     */
    public static <K, GmsValue, JetpackValue> @NonNull AppSearchBatchResult<K,
            JetpackValue> gmsAppSearchBatchResultToJetpack(
            com.google.android.gms.appsearch.@NonNull AppSearchBatchResult<K,
                    GmsValue> gmsResult,
            @NonNull Function<GmsValue, JetpackValue> valueMapper) {
        Preconditions.checkNotNull(gmsResult);
        Preconditions.checkNotNull(valueMapper);
        AppSearchBatchResult.Builder<K, JetpackValue> jetpackResult =
                new AppSearchBatchResult.Builder<>();
        for (Map.Entry<K, GmsValue> success :
                gmsResult.getSuccesses().entrySet()) {
            try {
                JetpackValue jetpackValue = valueMapper.apply(success.getValue());
                jetpackResult.setSuccess(success.getKey(), jetpackValue);
            } catch (Throwable t) {
                jetpackResult.setResult(
                        success.getKey(), AppSearchResult.throwableToFailedResult(t));
            }
        }
        for (Map.Entry<K,
                com.google.android.gms.appsearch.AppSearchResult<GmsValue>> failure :
                gmsResult.getFailures().entrySet()) {
            int resultCode = failure.getValue().getResultCode();
            String errorMessage = failure.getValue().getErrorMessage();
            // In some situations the error message maybe null if the result code is
            // RESULT_NOT_FOUND.
            // In these cases we should populate the error message.
            if (resultCode == RESULT_NOT_FOUND && errorMessage == null) {
                errorMessage = "Document id '" + failure.getKey() + "' doesn't exist";
            }
            jetpackResult.setFailure(
                    failure.getKey(),
                    resultCode,
                    errorMessage);
        }
        return jetpackResult.build();
    }
}

