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

/**
 * Internal API for generated serialization code.
 * <p>
 * The Serialization annotation processor generates stateless singleton implementations of
 * {@link androidx.serialization.runtime.internal.SerializerV1} for message classes and unions
 * and {@link androidx.serialization.runtime.internal.EnumSerializerV1} for enum classes. Each
 * implementation is placed in the same package as the source class with a name derived by
 * replacing the {@code .} of nested classes with {@code _} and adding a {@code Serializer} suffix.
 * In Java, serializers expose a singleton instance on a static final {@code INSTANCE} field. In
 * Kotlin, they are implemented as an object declaration.
 * <p>
 * Message serializers consume {@link androidx.serialization.runtime.internal.EncoderV1} and
 * {@link androidx.serialization.runtime.internal.DecoderV1}, which are provided by serialization
 * backends. This avoids serializers having a dependencies on a specific backend.
 * <p>
 * Every interface in this package has a version number suffix. This allows new methods to be
 * added without breaking binary compatibility with existing generated code, even for Java 7
 * targets without default methods. To add to an interface, add a new interface with an
 * incremented version number which extends the existing interface.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
package androidx.serialization.runtime.internal;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.RestrictTo;
