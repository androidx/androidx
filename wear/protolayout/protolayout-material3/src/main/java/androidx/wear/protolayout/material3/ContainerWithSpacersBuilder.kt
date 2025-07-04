/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.material3

import java.util.function.Consumer

/** Adds elements to the container with corresponding spacing between when needed. */
internal class ContainerWithSpacersBuilder<T>
/**
 * Creates an object with the given function that should be called for adding new elements in the
 * given container, and the first element that should be added if it exists.
 */
internal constructor(private val add: Consumer<T>, firstElement: T?) {
    /** Returns whether this container is empty or it has any elements inside. */
    var isEmpty: Boolean = firstElement == null
        private set

    init {
        firstElement?.let { add.accept(it) }
    }

    /**
     * Adds the given element to the parent defined in constructor and adds the Spacer above it if
     * there was previous content in the parent.
     */
    fun addElement(element: T?, space: T? = null): ContainerWithSpacersBuilder<T> {
        if (element == null) {
            return this
        }

        if (!isEmpty && space != null) {
            add.accept(space)
        }

        add.accept(element)
        isEmpty = false

        return this
    }
}
