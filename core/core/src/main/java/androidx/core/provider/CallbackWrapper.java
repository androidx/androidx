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

package androidx.core.provider;

import static androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason;

import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.core.provider.FontRequestWorker.TypefaceResult;
import androidx.core.provider.FontsContractCompat.FontRequestCallback;

import java.util.concurrent.Executor;

/**
 * Wraps a {@link FontRequestCallback} and a {@link Executor} in order to call the callback
 * functions on the provided handler.
 *
 * If no Executor is provided, {@link CalleeHandler#create()} is used instead.
 */
class CallbackWrapper {
    @NonNull private final FontRequestCallback mCallback;
    @NonNull private final Executor mExecutor;

    /**
     * Run callbacks in {@param executor}
     */
    CallbackWrapper(
            @NonNull FontRequestCallback callback,
            @NonNull Executor executor
    ) {
        this.mCallback = callback;
        this.mExecutor = executor;
    }

    /**
     * Run callbacks on main thread
     */
    CallbackWrapper(@NonNull FontRequestCallback callback) {
        this(callback, RequestExecutor.createHandlerExecutor(CalleeHandler.create()));
    }

    /**
     * Mirrors {@link FontRequestCallback#onTypefaceRetrieved(Typeface)}
     */
    private void onTypefaceRetrieved(@NonNull final Typeface typeface) {
        final FontRequestCallback callback = this.mCallback;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onTypefaceRetrieved(typeface);
            }
        });
    }

    /**
     * Mirrors {@link FontRequestCallback#onTypefaceRequestFailed(int)}
     */
    private void onTypefaceRequestFailed(@FontRequestFailReason final int reason) {
        final FontRequestCallback callback = this.mCallback;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onTypefaceRequestFailed(reason);
            }
        });
    }

    /**
     * Utility function for TypefaceResult
     */
    void onTypefaceResult(@NonNull TypefaceResult typefaceResult) {
        if (typefaceResult.isSuccess()) {
            onTypefaceRetrieved(typefaceResult.mTypeface);
        } else {
            onTypefaceRequestFailed(typefaceResult.mResult);
        }
    }
}
