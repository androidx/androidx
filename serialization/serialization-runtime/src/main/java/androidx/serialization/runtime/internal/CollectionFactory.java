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

import java.util.Collection;

/**
 * A factory that creates pre-sized collections.
 * <p>
 * This functional interface wraps a collection constructor, such as
 * {@link java.util.ArrayList#ArrayList(int)}, for Java 7 targets. In Java 8 and later, this will
 * likely be implemented using a method reference to the collection's constructor.
 * <p>
 * Collection decoding methods in {@link DecoderV1} accept this interface. This enables the
 * decoder to pre-size collections when possible, to avoid additional allocations as the
 * collection is being populated.
 *
 * @param <C> The type of collection created by this factory.
 */
public interface CollectionFactory<C extends Collection<?>> {
    /**
     * Create a new collection with the specified capacity if applicable.
     *
     * @param capacity The initial capacity of the new collection.
     * @return A new collection.
     */
    @NonNull
    C create(int capacity);
}
