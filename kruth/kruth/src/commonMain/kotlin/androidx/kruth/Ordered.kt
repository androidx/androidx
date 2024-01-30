/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.kruth

/**
 * Returned by calls like [IterableSubject.containsExactly], [Ordered] lets the caller
 * additionally check that the expected elements were present in the order they were passed to the
 * previous calls.
 *
 * ```
 * assertThat(supportedCharsets).containsExactly("UTF-8", "US-ASCII"); // does not check order
 * assertThat(supportedCharsets).containsExactly("UTF-8", "US-ASCII").inOrder(); // does check order
 * ```
 */
interface Ordered {

    /**
     * An additional assertion, implemented by some containment subjects which allows for a further
     * constraint of orderedness.
     */
    fun inOrder()
}
