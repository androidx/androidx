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
 * Interface for State design pattern.
 * <p>
 * Isolates can be in different states due to events within/outside the control of the developer.
 * This pattern allows us to extract out the state related behaviour without maintaining it all in
 * the JavaScriptIsolate class which proved to be error-prone and hard to read.
 * <p>
 * State specific behaviour are implemented in concrete classes that implements this interface.
 * <p>
 * Refer: https://en.wikipedia.org/wiki/State_pattern
 */
interface IsolateState {
    @NonNull
    ListenableFuture<String> evaluateJavaScriptAsync(@NonNull String code);

    @NonNull
    ListenableFuture<String> evaluateJavaScriptAsync(@NonNull AssetFileDescriptor afd);

    @NonNull
    ListenableFuture<String> evaluateJavaScriptAsync(@NonNull ParcelFileDescriptor pfd);

    void setConsoleCallback(@NonNull Executor executor,
            @NonNull JavaScriptConsoleCallback callback);

    void setConsoleCallback(@NonNull JavaScriptConsoleCallback callback);

    void clearConsoleCallback();

    boolean provideNamedData(@NonNull String name, @NonNull byte[] inputBytes);

    void close();

    /**
     * Check whether the current state is permitted to transition to a dead state
     *
     * @return true iff a transition to a dead state is permitted.
     */
    boolean canDie();

    /**
     * Method to run after this state has been replaced by a dead state.
     *
     * @param terminationInfo The termination info describing the death.
     */
    default void onDied(@NonNull TerminationInfo terminationInfo) {}

    void addOnTerminatedCallback(@NonNull Executor executor,
            @NonNull Consumer<TerminationInfo> callback);

    void removeOnTerminatedCallback(@NonNull Consumer<TerminationInfo> callback);
}
