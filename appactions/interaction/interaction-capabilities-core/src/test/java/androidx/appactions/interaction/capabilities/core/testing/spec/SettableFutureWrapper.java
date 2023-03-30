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

package androidx.appactions.interaction.capabilities.core.testing.spec;

import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

/** Utility class to make working with callbacks in tests easier. */
public final class SettableFutureWrapper<V> {
    private final ListenableFuture<V> mFuture;
    private CallbackToFutureAdapter.Completer<V> mCompleter;

    public SettableFutureWrapper() {
        this.mFuture =
                CallbackToFutureAdapter.getFuture(
                        completer -> {
                            this.mCompleter = completer;
                            return "SettableFutureWrapper";
                        });
    }

    public ListenableFuture<V> getFuture() {
        return mFuture;
    }

    public boolean set(V result) {
        return mCompleter.set(result);
    }

    public boolean setException(Throwable t) {
        return mCompleter.setException(t);
    }
}
