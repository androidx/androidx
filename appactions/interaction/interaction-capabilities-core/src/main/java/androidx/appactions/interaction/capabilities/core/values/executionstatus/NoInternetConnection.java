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

package androidx.appactions.interaction.capabilities.core.values.executionstatus;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.values.Thing;

import com.google.auto.value.AutoValue;

/** Error status for execution failure due to no internet connection. */
@AutoValue
public abstract class NoInternetConnection extends Thing {

    /** Create a new NoInternetConnection instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_NoInternetConnection.Builder();
    }

    /** Create a new default instance. */
    @NonNull
    public static NoInternetConnection getDefaultInstance() {
        return new AutoValue_NoInternetConnection.Builder().build();
    }

    @Override
    public final String toString() {
        return "NoInternetConnection";
    }

    /** Builder class for NoInternetConnection status. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<NoInternetConnection> {
    }
}
