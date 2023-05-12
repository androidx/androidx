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

package androidx.kruth

class MapSubject<K, V>(
    actual: Map<K, V>?,
    messageToPrepend: String? = null,
) : Subject<Map<K, V>>(actual, messageToPrepend) {

    /** Fails if the map is not empty. */
    fun isEmpty() {
        requireNonNull(actual) { "Expected to be empty, but was null" }

        if (actual.isNotEmpty()) {
            fail("Expected to be empty, but was $actual")
        }
    }

    /** Fails if the map does not contain the given key. */
    fun containsKey(key: Any?) {
        requireNonNull(actual) { "Expected to contain $key, but was null" }

        if (!actual.containsKey(key)) {
            fail("Expected to contain $key, but was ${actual.keys}")
        }
    }
}
