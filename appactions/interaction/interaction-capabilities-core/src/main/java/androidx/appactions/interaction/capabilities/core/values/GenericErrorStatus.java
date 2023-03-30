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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;

/** GenericErrorStatus for ExecutionStatus. */
@AutoValue
public abstract class GenericErrorStatus extends Thing {

    /** Create a new GenericErrorStatus instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_GenericErrorStatus.Builder();
    }

    /** Create a new default instance. */
    @NonNull
    public static GenericErrorStatus getDefaultInstance() {
        return new AutoValue_GenericErrorStatus.Builder().build();
    }

    @Override
    public final String toString() {
        return "GenericErrorStatus";
    }

    /** Builder class for GenericErrorStatus status. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<GenericErrorStatus> {
    }
}
