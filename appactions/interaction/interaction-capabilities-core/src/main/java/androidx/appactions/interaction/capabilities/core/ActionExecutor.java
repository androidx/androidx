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

package androidx.appactions.interaction.capabilities.core;

import androidx.annotation.NonNull;

/**
 * An interface of executing the action.
 *
 * @param <ArgumentT>
 * @param <OutputT>
 */
public interface ActionExecutor<ArgumentT, OutputT> {
    /**
     * Calls to execute the action.
     *
     * @param argument the argument for this action.
     * @param callback the callback to send back the action execution result.
     */
    void execute(@NonNull ArgumentT argument, @NonNull ActionCallback<OutputT> callback);

    /** Reasons for the action execution error. */
    enum ErrorStatus {
        CANCELLED,
        /** The action execution error was caused by a timeout. */
        TIMEOUT,
    }

    /**
     * An interface for receiving the result of action.
     *
     * @param <OutputT>
     */
    interface ActionCallback<OutputT> {

        /** Invoke to set an action result upon success. */
        void onSuccess(@NonNull ExecutionResult<OutputT> executionResult);

        /** Invoke to set an action result upon success. */
        default void onSuccess() {
            onSuccess(ExecutionResult.<OutputT>newBuilderWithOutput().build());
        }

        /**
         * Invokes to set an error status for the action.
         *
         * @deprecated
         */
        @Deprecated
        void onError(@NonNull ErrorStatus errorStatus);
    }

}
