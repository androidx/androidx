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

package androidx.camera.view;

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Pending value assignment that wait for event like camera initialization.
 *
 * <p> For example, one cannot call {@link CameraControl#setZoomRatio} before camera initialization
 * completes. To work around this, in {@link CameraController} when app calls
 * {@link CameraController#setZoomRatio}, we will cache the value with this class and propagate it
 * when {@link CameraControl} becomes ready.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class PendingValue<T> {

    @Nullable
    private T mValue;
    @Nullable
    private ListenableFuture<Void> mListenableFuture;
    @Nullable
    private CallbackToFutureAdapter.Completer<Void> mCompleter;

    /**
     * Assigns the pending value.
     *
     * @return a {@link ListenableFuture} that completes when the assignment completes.
     */
    @MainThread
    ListenableFuture<Void> setValue(@NonNull T value) {
        checkMainThread();
        if (mListenableFuture != null) {
            checkState(!mListenableFuture.isDone(),
                    "#setValue() is called after the value is propagated.");
            // Cancel the previous ListenableFuture.
            mListenableFuture.cancel(false);
        }
        // Track the pending value and the ListenableFuture.
        mValue = value;
        mListenableFuture = CallbackToFutureAdapter.getFuture(completer -> {
            mCompleter = completer;
            return "PendingValue " + value;
        });
        return mListenableFuture;
    }

    /**
     * Propagates the value if a pending value exists.
     *
     * <p> This method no-ops if there is no pending value.
     */
    @MainThread
    void propagateIfHasValue(Function<T, ListenableFuture<Void>> setValueFunction) {
        checkMainThread();
        if (mValue != null) {
            Futures.propagate(setValueFunction.apply(mValue), requireNonNull(mCompleter));
        }
    }
}
