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

package androidx.collection

/**
 * Given a series of lists which represent test parameters, produce the cartesian product of all
 * their values for use as [JUnit parameters][org.junit.runners.Parameterized.Parameters].
 *
 * For example,
 * ```kotlin
 * buildParameters(
 *   listOf(1, 2),
 *   listOf("one", "two"))
 * ```
 * will produce
 * ```
 * [
 *   [ 1, "one" ]
 *   [ 1, "two" ]
 *   [ 2, "one" ]
 *   [ 2, "two" ]
 * ]
 * ```
 */
inline fun <reified T> buildParameters(vararg lists: List<T>): List<Array<T>> {
    return lists.fold(listOf(emptyArray())) { partials, list ->
        partials.flatMap { partial ->
            list.map { element -> partial + element }
        }
    }
}
