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

package androidx.appactions.interaction.capabilities.core.impl.converters;

import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;
import com.google.protobuf.Value;

import java.util.Optional;
import java.util.function.Function;

@AutoValue
abstract class FieldBinding<T, BuilderT extends BuilderOf<T>> {

    static <T, BuilderT extends BuilderOf<T>> FieldBinding<T, BuilderT> create(
            String name,
            Function<T, Optional<Value>> valueGetter,
            CheckedInterfaces.BiConsumer<BuilderT, Optional<Value>> valueSetter) {
        return new AutoValue_FieldBinding<>(name, valueGetter, valueSetter);
    }

    abstract String name();

    abstract Function<T, Optional<Value>> valueGetter();

    abstract CheckedInterfaces.BiConsumer<BuilderT, Optional<Value>> valueSetter();
}
