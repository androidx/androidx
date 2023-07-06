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

package androidx.javascriptengine;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Covers the case where the isolate is explicitly closed by the developer.
 */
final class IsolateClosedState implements IsolateState {
    IsolateClosedState() {
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        throw new IllegalStateException("Calling evaluateJavaScriptAsync() after closing the"
                + "Isolate");
    }

    @Override
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
        throw new IllegalStateException(
                "Calling setConsoleCallback() after closing the Isolate");
    }

    @Override
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
        throw new IllegalStateException(
                "Calling setConsoleCallback() after closing the Isolate");
    }

    @Override
    public void clearConsoleCallback() {
        throw new IllegalStateException(
                "Calling clearConsoleCallback() after closing the Isolate");
    }

    @Override
    public boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes) {
        throw new IllegalStateException(
                "Calling provideNamedData() after closing the Isolate");
    }

    @Override
    public void close() {
    }

    @Override
    public IsolateState setSandboxDead() {
        return this;
    }

    @Override
    public IsolateState setIsolateDead() {
        return this;
    }
}
