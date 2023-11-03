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

// The order of these declarations follows those defined in Truth, which maintains their special
// ordering of which Subject type to prioritize from the general `assertThat` factory method. See:
// https://github.com/google/truth/blob/master/core/src/main/java/com/google/common/truth/Truth.java

fun <T : Comparable<T>> assertThat(actual: T?): ComparableSubject<T> {
    return ComparableSubject(actual)
}

fun <T> assertThat(actual: T?): Subject<T> {
    return Subject(actual)
}

fun <T : Throwable> assertThat(actual: T?): ThrowableSubject<T> {
    return ThrowableSubject(actual)
}

fun assertThat(actual: Boolean?): BooleanSubject {
    return BooleanSubject(actual)
}

fun assertThat(actual: Double?): DoubleSubject {
    return DoubleSubject(actual)
}

fun assertThat(actual: Int?): IntegerSubject {
    return IntegerSubject(actual)
}

fun assertThat(actual: String?): StringSubject {
    return StringSubject(actual)
}

fun <T> assertThat(actual: Iterable<T>?): IterableSubject<T> =
    IterableSubject(actual)

fun <K, V> assertThat(actual: Map<K, V>?): MapSubject<K, V> =
    MapSubject(actual)

/**
 * Begins an assertion that, if it fails, will prepend the given message to the failure message.
 */
fun assertWithMessage(messageToPrepend: String): StandardSubjectBuilder =
    StandardSubjectBuilder(
        metadata = FailureMetadata(messagesToPrepend = listOf(messageToPrepend)),
    )

/**
 * Given a factory for some [Subject] class, returns [SimpleSubjectBuilder] whose
 * [that][SimpleSubjectBuilder.that] method creates instances of that class.
 */
fun <S : Subject<T>, T> assertAbout(
    subjectFactory: Subject.Factory<S, T>,
): SimpleSubjectBuilder<S, T> =
    SimpleSubjectBuilder(subjectFactory = subjectFactory)
