/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.runtime.internal;

import androidx.annotation.NonNull;

/**
 * Serializes an enum class to integer IDs.
 * <p>
 * Enum serializers are expected to be stateless singletons. The annotation processor generates
 * serializers in the same package as the enum class with a name derived by replacing the
 * {@code .} of nested classes with {@code _} and adding a {@code Serializer} suffix. In Java,
 * enum serializers are final classes which expose a singleton instance on a static final
 * {@code INSTANCE} field. In Kotlin, they are implemented as an object declaration.
 *
 * @param <T> The enum class this serializer serializes.
 */
public interface EnumSerializerV1<T extends Enum<T>> {
    /**
     * Encode an enum value to an integer.
     *
     * @param value An enum value or null for the default ID.
     * @return The integer ID of the enum value.
     */
    int encode(@NonNull T value);

    /**
     * Decode an enum value from an integer.
     *
     * @param valueId An enum value ID.
     * @return The enum value with the supplied ID or the default enum value.
     */
    @NonNull
    T decode(int valueId);
}
