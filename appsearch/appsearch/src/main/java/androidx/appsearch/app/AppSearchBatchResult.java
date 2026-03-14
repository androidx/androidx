/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.appsearch.app;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Provides results for AppSearch batch operations which encompass multiple documents.
 *
 * <p>Individual results of a batch operation are separated into two maps: one for successes and
 * one for failures. For successes, {@link #getSuccesses()} will return a map of keys to
 * instances of the value type. For failures, {@link #getFailures()} will return a map of keys to
 * {@link AppSearchResult} objects.
 *
 * <p>Alternatively, {@link #getAll()} returns a map of keys to {@link AppSearchResult} objects for
 * both successes and failures.
 *
 * @param <KeyType> The type of the keys for which the results will be reported.
 * @param <ValueType> The type of the result objects for successful results.
 *
 * @see AppSearchSession#putAsync
 * @see AppSearchSession#getByDocumentIdAsync
 * @see AppSearchSession#removeAsync
 */
public final class AppSearchBatchResult<KeyType, ValueType> {
    private final @NonNull Map<KeyType, @Nullable ValueType> mSuccesses;
    private final @NonNull Map<KeyType, AppSearchResult<ValueType>> mFailures;
    private final @NonNull Map<KeyType, AppSearchResult<ValueType>> mAll;

    AppSearchBatchResult(
            @NonNull Map<KeyType, @Nullable ValueType> successes,
            @NonNull Map<KeyType, AppSearchResult<ValueType>> failures,
            @NonNull Map<KeyType, AppSearchResult<ValueType>> all) {
        mSuccesses = Preconditions.checkNotNull(successes);
        mFailures = Preconditions.checkNotNull(failures);
        mAll = Preconditions.checkNotNull(all);
    }

    /** Returns {@code true} if this {@link AppSearchBatchResult} has no failures. */
    public boolean isSuccess() {
        return mFailures.isEmpty();
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of the value type for all successful
     * individual results.
     *
     * <p>Example: {@link AppSearchSession#getByDocumentIdAsync} returns an
     * {@link AppSearchBatchResult}. Each key (the document ID, of {@code String} type) will map to
     * a {@link GenericDocument} object.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    public @NonNull Map<KeyType, ValueType> getSuccesses() {
        return Collections.unmodifiableMap(mSuccesses);
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of {@link AppSearchResult} for all
     * failed individual results.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    public @NonNull Map<KeyType, AppSearchResult<ValueType>> getFailures() {
        return Collections.unmodifiableMap(mFailures);
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of {@link AppSearchResult} for all
     * individual results.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    public @NonNull Map<KeyType, AppSearchResult<ValueType>> getAll() {
        return Collections.unmodifiableMap(mAll);
    }

    /**
     * Asserts that this {@link AppSearchBatchResult} has no failures.
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void checkSuccess() {
        if (!isSuccess()) {
            throw new IllegalStateException("AppSearchBatchResult has failures: " + this);
        }
    }

    /**
     * Returns an {@link AppSearchBatchResult} with the same keys and {@link Void} value type.
     *
     * <p>It is used to convert {@link ValueType} to {@link Void} for safe parcelable if the {@link
     * ValueType} is an internal only type and won't be returned to the client.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull AppSearchBatchResult<KeyType, Void> toVoidBatchResult() {
        Builder<KeyType, Void> builder = new Builder<>();
        for (Map.Entry<KeyType, @Nullable ValueType> entry : mSuccesses.entrySet()) {
            builder.setSuccess(entry.getKey(), /* value= */ null);
        }
        for (Map.Entry<KeyType, AppSearchResult<ValueType>> entry : mFailures.entrySet()) {
            builder.setFailure(
                    entry.getKey(),
                    entry.getValue().getResultCode(),
                    entry.getValue().getErrorMessage());
        }
        // No need to convert mAll since setSuccess and setFailure have already added entries to it.
        return builder.build();
    }

    @Override
    public @NonNull String toString() {
        return "{\n  successes: " + mSuccesses + "\n  failures: " + mFailures + "\n}";
    }

    /**
     * Builder for {@link AppSearchBatchResult} objects.
     *
     * @param <KeyType> The type of the keys for which the results will be reported.
     * @param <ValueType> The type of the result objects for successful results.
     */
    public static final class Builder<KeyType, ValueType> {
        private ArrayMap<KeyType, @Nullable ValueType> mSuccesses = new ArrayMap<>();
        private ArrayMap<KeyType, AppSearchResult<ValueType>> mFailures = new ArrayMap<>();
        private ArrayMap<KeyType, AppSearchResult<ValueType>> mAll = new ArrayMap<>();
        private boolean mBuilt = false;

        /** Creates a new {@link Builder}. */
        public Builder() {
        }

        /** Creates a new {@link Builder} from the given {@link AppSearchBatchResult}. */
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        public Builder(@NonNull AppSearchBatchResult<KeyType, ValueType> appSearchBatchResult) {
            mSuccesses.putAll(appSearchBatchResult.mSuccesses);
            mFailures.putAll(appSearchBatchResult.mFailures);
            mAll.putAll(appSearchBatchResult.mAll);
        }

        /**
         * Associates the {@code key} with the provided successful return value.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * <p>This is a convenience function which is equivalent to
         * {@code setResult(key, AppSearchResult.newSuccessfulResult(value))}.
         *
         * @param key   The key to associate the result with; usually corresponds to some
         *              identifier from the input like an ID or name.
         * @param value An optional value to associate with the successful result of the operation
         *              being performed.
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("MissingGetterMatchingBuilder")  // See getSuccesses
        public @NonNull Builder<KeyType, ValueType> setSuccess(
                @NonNull KeyType key, @Nullable ValueType value) {
            Preconditions.checkNotNull(key);
            resetIfBuilt();
            return setResult(key, AppSearchResult.newSuccessfulResult(value));
        }

        /**
         * Associates the {@code key} with the provided failure code and error message.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * <p>This is a convenience function which is equivalent to
         * {@code setResult(key, AppSearchResult.newFailedResult(resultCode, errorMessage))}.
         *
         * @param key          The key to associate the result with; usually corresponds to some
         *                     identifier from the input like an ID or name.
         * @param resultCode   One of the constants documented in
         *                     {@link AppSearchResult#getResultCode}.
         * @param errorMessage An optional string describing the reason or nature of the failure.
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("MissingGetterMatchingBuilder")  // See getFailures
        public @NonNull Builder<KeyType, ValueType> setFailure(
                @NonNull KeyType key,
                @AppSearchResult.ResultCode int resultCode,
                @Nullable String errorMessage) {
            Preconditions.checkNotNull(key);
            resetIfBuilt();
            return setResult(key, AppSearchResult.newFailedResult(resultCode, errorMessage));
        }

        /**
         * Associates the {@code key} with the provided {@code result}.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * @param key    The key to associate the result with; usually corresponds to some
         *               identifier from the input like an ID or name.
         * @param result The result to associate with the key.
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("MissingGetterMatchingBuilder")  // See getAll
        public @NonNull Builder<KeyType, ValueType> setResult(
                @NonNull KeyType key, @NonNull AppSearchResult<ValueType> result) {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(result);
            resetIfBuilt();
            if (result.isSuccess()) {
                mSuccesses.put(key, result.getResultValue());
                mFailures.remove(key);
            } else {
                mFailures.put(key, result);
                mSuccesses.remove(key);
            }
            mAll.put(key, result);
            return this;
        }

        /**
         * Builds an {@link AppSearchBatchResult} object from the contents of this {@link Builder}.
         */
        public @NonNull AppSearchBatchResult<KeyType, ValueType> build() {
            mBuilt = true;
            return new AppSearchBatchResult<>(mSuccesses, mFailures, mAll);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mSuccesses = new ArrayMap<>(mSuccesses);
                mFailures = new ArrayMap<>(mFailures);
                mAll = new ArrayMap<>(mAll);
                mBuilt = false;
            }
        }
    }
}
