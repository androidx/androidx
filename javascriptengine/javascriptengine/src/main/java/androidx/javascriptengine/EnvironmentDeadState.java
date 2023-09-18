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
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Covers the case where the environment is dead.
 * <p>
 * This state covers cases where the developer explicitly closes the sandbox or sandbox/isolate
 * being dead outside of the control of the developer.
 * <p>
 * Although being in this state is considered terminated from the app perspective, the service
 * side may still technically be running.
 */
final class EnvironmentDeadState implements IsolateState {
    @NonNull
    private final TerminationInfo mTerminationInfo;

    EnvironmentDeadState(@NonNull TerminationInfo terminationInfo) {
        mTerminationInfo = terminationInfo;
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code) {
        return getEnvironmentDeadFuture();
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull AssetFileDescriptor afd) {
        return getEnvironmentDeadFuture();
    }

    @NonNull
    @Override
    public ListenableFuture<String> evaluateJavaScriptAsync(@NonNull ParcelFileDescriptor pfd) {
        return getEnvironmentDeadFuture();
    }

    @Override
    public void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback) {
    }

    @Override
    public void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback) {
    }

    @Override
    public void clearConsoleCallback() {
    }

    @Override
    public boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes) {
        return false;
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
        executor.execute(() -> callback.accept(mTerminationInfo));
    }

    @Override
    public void removeOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback) {}

    private ListenableFuture<String> getEnvironmentDeadFuture() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            final String futureDebugMessage = "evaluateJavascript Future";
            completer.setException(mTerminationInfo.toJavaScriptException());
            return futureDebugMessage;
        });
    }
}
