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

import kotlin.jvm.JvmStatic

/**
 * In a fluent assertion chain, an object with which you can do any of the following:
 *
 * - Set an optional message with [withMessage].
 * - For the types of [Subject] built into Kruth, directly specify the value under test
 * with [withMessage].
 */
class StandardSubjectBuilder internal constructor(
    private val metadata: FailureMetadata = FailureMetadata(),
) {
    companion object {
        /**
         * Returns a new instance that invokes the given [FailureStrategy] when a check fails.
         */
        @JvmStatic
        fun forCustomFailureStrategy(failureStrategy: FailureStrategy): StandardSubjectBuilder? {
            return StandardSubjectBuilder(FailureMetadata.forFailureStrategy(failureStrategy))
        }
    }

    /**
     * Returns a new instance that will output the given message before the main failure message. If
     * this method is called multiple times, the messages will appear in the order that they were
     * specified.
     */
    fun withMessage(messageToPrepend: String): StandardSubjectBuilder =
        StandardSubjectBuilder(metadata = metadata.withMessage(messageToPrepend = messageToPrepend))

    fun <T> that(actual: T): Subject<T> =
        Subject(actual = actual, metadata = metadata)

    fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T> =
        ComparableSubject(actual = actual, metadata = metadata)

    fun <T : Throwable> that(actual: T?): ThrowableSubject<T> =
        ThrowableSubject(actual = actual, metadata = metadata)

    fun that(actual: Boolean?): BooleanSubject =
        BooleanSubject(actual = actual, metadata = metadata)

    fun that(actual: Double?): DoubleSubject =
        DoubleSubject(actual = actual, metadata = metadata)

    fun that(actual: String?): StringSubject =
        StringSubject(actual = actual, metadata = metadata)

    fun <T> that(actual: Iterable<T>?): IterableSubject<T> =
        IterableSubject(actual = actual, metadata = metadata)

    fun <K, V> that(actual: Map<K, V>?): MapSubject<K, V> =
        MapSubject(actual = actual, metadata = metadata)

    /**
     * Given a factory for some [Subject] class, returns [SimpleSubjectBuilder] whose
     * [that][SimpleSubjectBuilder.that] method creates instances of that class. Created subjects
     * use the previously set failure strategy and any previously set failure message.
     */
    fun <T, S : Subject<T>> about(
        subjectFactory: Subject.Factory<S, T>,
    ): SimpleSubjectBuilder<S, T> =
        SimpleSubjectBuilder(metadata = metadata, subjectFactory = subjectFactory)

    /**
     * Reports a failure.
     *
     * To set a message, first call [withMessage] (or, more commonly, use the shortcut
     * [assertWithMessage].
     */
    fun fail(): Nothing {
        kotlin.test.fail(metadata.formatMessage())
    }
}
