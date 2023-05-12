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

class StandardSubjectBuilder(private val messageToPrepend: String) {

    fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T> =
        ComparableSubject(actual, messageToPrepend)

    fun <T> that(actual: T?): Subject<T> =
        Subject(actual, messageToPrepend)

    fun <T : Throwable> that(actual: T?): ThrowableSubject<T> =
        ThrowableSubject(actual, messageToPrepend)

    fun that(actual: Boolean?): BooleanSubject =
        BooleanSubject(actual, messageToPrepend)

    fun that(actual: String?): StringSubject =
        StringSubject(actual, messageToPrepend)

    fun <T> that(actual: Iterable<T>?): IterableSubject<T> =
        IterableSubject(actual, messageToPrepend)

    fun <K, V> that(actual: Map<K, V>?): MapSubject<K, V> =
        MapSubject(actual, messageToPrepend)
}
