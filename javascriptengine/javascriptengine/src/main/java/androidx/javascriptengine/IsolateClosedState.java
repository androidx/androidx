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

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Covers cases where the isolate is explicitly closed or uninitialized.
 * <p>
 * Although being in this state is considered terminated from the app perspective, the service
 * side may still technically be running.
 */
final class IsolateClosedState implements IsolateState {
    @NonNull
    private final String mDescription;
    IsolateClosedState(@NonNull String description) {
        mDescription = description;
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        throw new IllegalStateException(
                "Calling evaluateJavaScriptAsync() when " + mDescription);
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull AssetFileDescriptor afd) {
        throw new IllegalStateException(
                "Calling evaluateJavaScriptAsync() when " + mDescription);
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull ParcelFileDescriptor pfd) {
        throw new IllegalStateException(
                "Calling evaluateJavaScriptAsync() when " + mDescription);
    }

    @Override
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
        throw new IllegalStateException(
                "Calling setConsoleCallback() when " + mDescription);
    }

    @Override
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
        throw new IllegalStateException(
                "Calling setConsoleCallback() when " + mDescription);
    }

    @Override
    public void clearConsoleCallback() {
        throw new IllegalStateException(
                "Calling clearConsoleCallback() when " + mDescription);
    }

    @Override
    public boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes) {
        throw new IllegalStateException(
                "Calling provideNamedData() when " + mDescription);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean canDie() {
        return false;
    }

    @Override
    public void addOnTerminatedCallback(@NonNull Executor executor,
            @NonNull Consumer<TerminationInfo> callback) {
        throw new IllegalStateException(
                "Calling addOnTerminatedCallback() when " + mDescription);
    }

    @Override
    public void removeOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback) {
        throw new IllegalStateException(
                "Calling removeOnTerminatedCallback() when " + mDescription);
    }
}
