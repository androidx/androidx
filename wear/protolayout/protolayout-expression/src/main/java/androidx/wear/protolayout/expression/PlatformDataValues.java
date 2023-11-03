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

package androidx.wear.protolayout.expression;

import static java.util.Collections.unmodifiableMap;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;

import java.util.Map;

/** Typed mapping of {@link PlatformDataKey} to {@link DynamicDataValue}. */
public final class PlatformDataValues {
    @NonNull final Map<PlatformDataKey<?>, DynamicDataValue<?>> data;

    /** Builder for {@link PlatformDataValues}. */
    public static final class Builder {
        @NonNull final Map<PlatformDataKey<?>, DynamicDataValue<?>> data = new ArrayMap<>();

        /** Puts a key/value pair. */
        @NonNull
        @SuppressWarnings("BuilderSetStyle") // Map-style builder, getter is generic get().
        public <T extends DynamicType> Builder put(
                @NonNull PlatformDataKey<T> key, @NonNull DynamicDataValue<T> value) {
            data.put(key, value);
            return this;
        }

        /**
         * Puts all values from another {@link PlatformDataValues}.
         *
         * <p>Values not in {@code other} are not removed from this {@link Builder}.
         */
        @NonNull
        @SuppressWarnings("BuilderSetStyle") // Map-style builder, getter is generic get().
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder putAll(@NonNull PlatformDataValues other) {
            data.putAll(other.data);
            return this;
        }

        /** Builds the {@link PlatformDataValues}. */
        @NonNull
        public PlatformDataValues build() {
            return new PlatformDataValues(unmodifiableMap(data));
        }
    }

    /** Creates a {@link PlatformDataValues} from a single key/value pair. */
    @NonNull
    public static <T extends DynamicType> PlatformDataValues of(
            @NonNull PlatformDataKey<T> key, @NonNull DynamicDataValue<T> value) {
        return new PlatformDataValues(Map.of(key, value));
    }

    PlatformDataValues(@NonNull Map<PlatformDataKey<?>, DynamicDataValue<?>> data) {
        this.data = data;
    }

    /** Returns the key-value mapping. */
    @NonNull
    public Map<PlatformDataKey<?>, DynamicDataValue<?>> getAll() {
        return unmodifiableMap(data);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("PlatformDataValues{%s}", data);
    }
}
