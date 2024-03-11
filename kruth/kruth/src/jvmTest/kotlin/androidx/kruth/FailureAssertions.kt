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

package androidx.kruth

/** Convenience methods for Truth Subject tests. */

fun ExpectFailure.assertFailureKeys(vararg keys: String?) {
    assertThatFailure().factKeys().containsExactlyElementsIn(keys).inOrder()
}

fun ExpectFailure.assertFailureValue(key: String?, value: String?) {
    assertThatFailure().factValue(key!!).isEqualTo(value)
}

fun ExpectFailure.assertFailureValueIndexed(key: String?, index: Int, value: String?) {
    assertThatFailure().factValue(key!!, index).isEqualTo(value)
}

fun ExpectFailure.assertThatFailure(): TruthFailureSubject<AssertionError> {
    return ExpectFailure.assertThat(getFailure())
}
