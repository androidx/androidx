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
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.provider.FontRequestWorker.TypefaceResult;
import androidx.core.provider.FontsContractCompat.FontRequestCallback;

/**
 * Wraps a {@link FontRequestCallback} and a {@link Handler} in order to call the callback
 * functions on the provided handler.
 *
 * If no Handler is provided, {@link CalleeHandler#create()} is used instead.
 */
class CallbackWithHandler {
    @NonNull private final FontRequestCallback mCallback;
    @NonNull private final Handler mCallbackHandler;

    CallbackWithHandler(
            @NonNull FontRequestCallback callback,
            @NonNull Handler callbackHandler) {
        this.mCallback = callback;
        this.mCallbackHandler = callbackHandler;
    }

    CallbackWithHandler(@NonNull FontRequestCallback callback) {
        this.mCallback = callback;
        this.mCallbackHandler = CalleeHandler.create();
    }

    /**
     * Mirrors {@link FontRequestCallback#onTypefaceRetrieved(Typeface)}
     */
    private void onTypefaceRetrieved(@NonNull final Typeface typeface) {
        final FontRequestCallback callback = this.mCallback;
        mCallbackHandler.post(new Runnable() {
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
        mCallbackHandler.post(new Runnable() {
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
