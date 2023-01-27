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
 * Class that represents the response after all slots are filled and accepted and the task is ready
 * to enter the confirmation turn.
 *
 * @param <ConfirmationT>
 */
@AutoValue
public abstract class ConfirmationOutput<ConfirmationT> {

    /**
     * Create a Builder instance for building a ConfirmationOutput instance without confirmation
     * output.
     */
    @NonNull
    public static Builder<Void> newBuilder() {
        return new AutoValue_ConfirmationOutput.Builder<>();
    }

    /** Returns a default ConfirmationOutput instance. */
    @NonNull
    public static ConfirmationOutput<Void> getDefaultInstance() {
        return ConfirmationOutput.newBuilder().build();
    }

    /** Create a Builder instance for building a ConfirmationOutput instance. */
    @NonNull
    public static <ConfirmationT> Builder<ConfirmationT> newBuilderWithConfirmation() {
        return new AutoValue_ConfirmationOutput.Builder<>();
    }

    /** Returns a default ConfirmationOutput instance with a confirmation output type. */
    @NonNull
    public static <ConfirmationT>
            ConfirmationOutput<ConfirmationT> getDefaultInstanceWithConfirmation() {
        return ConfirmationOutput.<ConfirmationT>newBuilderWithConfirmation().build();
    }

    /** The confirmation output. */
    @Nullable
    public abstract ConfirmationT getConfirmation();

    /**
     * Builder for ConfirmationOutput.
     *
     * @param <ConfirmationT>
     */
    @AutoValue.Builder
    public abstract static class Builder<ConfirmationT> {

        /** Sets the confirmation output. */
        @NonNull
        public abstract Builder<ConfirmationT> setConfirmation(ConfirmationT confirmation);

        /** Builds and returns the ConfirmationOutput instance. */
        @NonNull
        public abstract ConfirmationOutput<ConfirmationT> build();
    }
}
