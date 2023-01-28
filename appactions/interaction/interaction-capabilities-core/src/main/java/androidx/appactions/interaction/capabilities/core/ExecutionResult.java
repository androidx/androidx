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
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Class that represents the response after an ActionCapability fulfills an action.
 *
 * @param <OutputT>
 */
@AutoValue
public abstract class ExecutionResult<OutputT> {

    /** Create a Builder instance for building a ExecutionResult instance without output. */
    @NonNull
    public static Builder<Void> newBuilder() {
        return new AutoValue_ExecutionResult.Builder<Void>().setStartDictation(false);
    }

    /** Returns a default ExecutionResult instance. */
    @NonNull
    public static ExecutionResult<Void> getDefaultInstance() {
        return ExecutionResult.newBuilder().build();
    }

    /** Create a Builder instance for building a ExecutionResult instance. */
    @NonNull
    public static <OutputT> Builder<OutputT> newBuilderWithOutput() {
        return new AutoValue_ExecutionResult.Builder<OutputT>().setStartDictation(false);
    }

    /** Returns a default ExecutionResult instance with an output type. */
    @NonNull
    public static <OutputT> ExecutionResult<OutputT> getDefaultInstanceWithOutput() {
        return ExecutionResult.<OutputT>newBuilderWithOutput().build();
    }

    /** Whether to start dictation mode after the fulfillment. */
    public abstract boolean getStartDictation();

    /** The execution output. */
    @Nullable
    public abstract OutputT getOutput();

    /**
     * Builder for ExecutionResult.
     *
     * @param <OutputT>
     */
    @AutoValue.Builder
    public abstract static class Builder<OutputT> {
        /** Sets whether or not this fulfillment should start dictation. */
        @NonNull
        public abstract Builder<OutputT> setStartDictation(boolean startDictation);

        /** Sets the execution output. */
        @NonNull
        public abstract Builder<OutputT> setOutput(OutputT output);

        /** Builds and returns the ExecutionResult instance. */
        @NonNull
        public abstract ExecutionResult<OutputT> build();
    }
}
