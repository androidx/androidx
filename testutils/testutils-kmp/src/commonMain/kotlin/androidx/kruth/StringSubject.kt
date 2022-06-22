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

import kotlin.test.assertContains
import kotlin.test.assertNotNull

/**
 * Propositions for string subjects.
 */
class StringSubject(actual: String?) : ComparableSubject<String>(actual) {

    /**
     * Fails if the string does not contain the given sequence.
     */
    fun contains(charSequence: CharSequence) {
        assertNotNull(actual)
        assertContains(actual, charSequence)
    }
}
