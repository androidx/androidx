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

import kotlin.test.assertEquals

// This mimics truth APis. not that we would merge it but makes moving tests and trying it
// easier
class KruthAssertion<T>(
    val actual: T?
) {
    fun isEqualTo(
        expected: T?
    ) {
        // TODO truth does some clever conversions here. e.g. you can say byte is equal to int
        assertEquals(
            expected = expected,
            actual = actual
        )
    }
}
fun <T> assertThat(
    actual: T?
) = KruthAssertion(actual)