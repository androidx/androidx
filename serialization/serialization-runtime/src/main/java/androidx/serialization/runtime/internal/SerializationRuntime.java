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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection factories for lists and sets.
 * <p>
 * This utility class provides type-safe methods for obtaining collection factories for lists and
 * sets. {@link List} and {@link Set} are the most common subinterfaces of
 * {@link java.util.Collection}, these factories reduce the volume of lambdas that need to be
 * created in generated code.
 */
public final class SerializationRuntime {
    private static final CollectionFactory<? extends List<?>> LIST_FACTORY =
            new CollectionFactory<List<?>>() {
                @NonNull
                @Override
                public List<?> create(int capacity) {
                    if (capacity == 0) {
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<>(capacity);
                    }
                }
            };

    private static final CollectionFactory<? extends Set<?>> SET_FACTORY =
            new CollectionFactory<Set<?>>() {
                @NonNull
                @Override
                public Set<?> create(int capacity) {
                    if (capacity == 0) {
                        return Collections.emptySet();
                    } else {
                        return new LinkedHashSet<>(capacity);
                    }
                }
            };

    private SerializationRuntime() {
    }

    /**
     * Obtain a default instance of a message class from its serializer.
     *
     * @param serializer the serializer for the message class.
     * @param <T>        a message class.
     * @return a new message with all fields set to default values.
     */
    @NonNull
    public static <T> T defaultInstanceOf(@NonNull SerializerV1<T> serializer) {
        return serializer.decode(EmptyDecoder.INSTANCE, null);
    }

    /**
     * A collection factory that creates lists.
     *
     * @param <T> element type of the factory.
     * @return a list factory.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> CollectionFactory<List<T>> getListFactory() {
        return (CollectionFactory<List<T>>) LIST_FACTORY;
    }

    /**
     * A collection factory that creates sets.
     *
     * @param <T> element type of the factory.
     * @return a set factory.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> CollectionFactory<Set<T>> getSetFactory() {
        return (CollectionFactory<Set<T>>) SET_FACTORY;
    }
}
