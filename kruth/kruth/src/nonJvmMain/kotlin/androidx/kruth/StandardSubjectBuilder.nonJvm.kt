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

import kotlin.jvm.JvmStatic

/**
 * In a fluent assertion chain, an object with which you can do any of the following:
 * - Set an optional message with [withMessage].
 * - For the types of [Subject] built into Kruth, directly specify the value under test with
 *   [withMessage].
 */
@Suppress("StaticFinalBuilder") // Cannot be final for binary compatibility.
public actual open class StandardSubjectBuilder
internal actual constructor(metadata: FailureMetadata) {
    internal actual val metadata: FailureMetadata = metadata
        get() {
            checkStatePreconditions()
            return field
        }

    public actual companion object {
        /** Returns a new instance that invokes the given [FailureStrategy] when a check fails. */
        @JvmStatic
        public actual fun forCustomFailureStrategy(
            failureStrategy: FailureStrategy
        ): StandardSubjectBuilder = commonForCustomFailureStrategy(failureStrategy)
    }

    /**
     * Returns a new instance that will output the given message before the main failure message. If
     * this method is called multiple times, the messages will appear in the order that they were
     * specified.
     */
    public actual fun withMessage(messageToPrepend: String?): StandardSubjectBuilder =
        commonWithMessage(messageToPrepend)

    public actual fun <T> that(actual: T?): Subject<T> = commonThat(actual)

    public actual fun that(actual: Char): Subject<Char> = commonThat(actual)

    public actual fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T> =
        commonThat(actual)

    public actual fun <T : Throwable> that(actual: T?): ThrowableSubject<T> = commonThat(actual)

    public actual fun that(actual: Boolean?): BooleanSubject = commonThat(actual)

    public actual fun that(actual: Long): LongSubject = commonThat(actual)

    public actual fun <T : Long?> that(actual: T): LongSubject = commonThat(actual)

    public actual fun that(actual: Double?): DoubleSubject = commonThat(actual)

    public actual fun that(actual: Float?): FloatSubject = commonThat(actual)

    public actual fun that(actual: Int): IntegerSubject = commonThat(actual)

    public actual fun <T : Int?> that(actual: T): IntegerSubject = commonThat(actual)

    public actual fun that(actual: String?): StringSubject = commonThat(actual)

    public actual fun <T> that(actual: Iterable<T>?): IterableSubject<T> = commonThat(actual)

    public actual fun <T> that(actual: Array<out T>?): ObjectArraySubject<T> = commonThat(actual)

    public actual fun that(actual: BooleanArray?): PrimitiveBooleanArraySubject = commonThat(actual)

    public actual fun that(actual: ShortArray?): PrimitiveShortArraySubject = commonThat(actual)

    public actual fun that(actual: IntArray?): PrimitiveIntArraySubject = commonThat(actual)

    public actual fun that(actual: LongArray?): PrimitiveLongArraySubject = commonThat(actual)

    public actual fun that(actual: ByteArray?): PrimitiveByteArraySubject = commonThat(actual)

    public actual fun that(actual: CharArray?): PrimitiveCharArraySubject = commonThat(actual)

    public actual fun that(actual: FloatArray?): PrimitiveFloatArraySubject = commonThat(actual)

    public actual fun that(actual: DoubleArray?): PrimitiveDoubleArraySubject = commonThat(actual)

    public actual fun <K, V> that(actual: Map<K, V>?): MapSubject<K, V> = commonThat(actual)

    /**
     * Given a factory for some [Subject] class, returns [SimpleSubjectBuilder] whose
     * [that][SimpleSubjectBuilder.that] method creates instances of that class. Created subjects
     * use the previously set failure strategy and any previously set failure message.
     */
    public actual fun <T, S : Subject<T>> about(
        subjectFactory: Subject.Factory<S, T>
    ): SimpleSubjectBuilder<S, T> = commonAbout(subjectFactory)

    /**
     * Reports a failure.
     *
     * To set a message, first call [withMessage] (or, more commonly, use the shortcut
     * [assertWithMessage].
     */
    public actual fun fail(): Unit = commonFail()

    internal actual open fun checkStatePreconditions() {}
}
