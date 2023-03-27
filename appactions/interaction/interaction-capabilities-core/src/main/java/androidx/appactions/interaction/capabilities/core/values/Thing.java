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

import java.util.Optional;

/** Common interface for structured entity. */
public abstract class Thing {
    /** Returns the id of this thing. */
    @NonNull
    public abstract Optional<String> getId();

    /** Returns the name of this thing. */
    @NonNull
    public abstract Optional<String> getName();

    /**
     * Base builder class that can be extended to build objects that extend Thing.
     *
     * @param <T>
     */
    public abstract static class Builder<T extends Builder<T>> {
        /** Sets the id of the Thing to be built. */
        @NonNull
        public abstract T setId(@NonNull String id);

        /** Sets the name of the Thing to be built. */
        @NonNull
        public abstract T setName(@NonNull String name);
    }
}
