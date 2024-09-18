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
 * - Set an optional message with [withMessage].
 * - For the types of [Subject] built into Kruth, directly specify the value under test with
 *   [withMessage].
 */
@Suppress("StaticFinalBuilder") // Cannot be final for binary compatibility.
expect open class StandardSubjectBuilder internal constructor(metadata: FailureMetadata) {
    internal val metadata: FailureMetadata

    companion object {
        /** Returns a new instance that invokes the given [FailureStrategy] when a check fails. */
        @JvmStatic
        fun forCustomFailureStrategy(failureStrategy: FailureStrategy): StandardSubjectBuilder
    }

    /**
     * Returns a new instance that will output the given message before the main failure message. If
     * this method is called multiple times, the messages will appear in the order that they were
     * specified.
     */
    fun withMessage(messageToPrepend: String?): StandardSubjectBuilder

    fun <T> that(actual: T?): Subject<T>

    // actual cannot be made nullable due to autoboxing and this overload is necessary to allow
    // StandardSubjectBuilder.that(char) from Java to resolve properly as an Object
    // (otherwise it is source-incompatibly interpreted as Int).
    // See: NumericComparisonTest#testNumericPrimitiveTypes_isNotEqual_shouldFail_charToInt
    fun that(actual: Char): Subject<Char>

    fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T>

    fun <T : Throwable> that(actual: T?): ThrowableSubject<T>

    fun that(actual: Boolean?): BooleanSubject

    fun that(actual: Long): LongSubject

    // Workaround for https://youtrack.jetbrains.com/issue/KT-645
    fun <T : Long?> that(actual: T): LongSubject

    fun that(actual: Double?): DoubleSubject

    fun that(actual: Float?): FloatSubject

    fun that(actual: Int): IntegerSubject

    // Workaround for https://youtrack.jetbrains.com/issue/KT-645
    fun <T : Int?> that(actual: T): IntegerSubject

    fun that(actual: String?): StringSubject

    fun <T> that(actual: Iterable<T>?): IterableSubject<T>

    fun <T> that(actual: Array<out T>?): ObjectArraySubject<T>

    fun that(actual: BooleanArray?): PrimitiveBooleanArraySubject

    fun that(actual: ShortArray?): PrimitiveShortArraySubject

    fun that(actual: IntArray?): PrimitiveIntArraySubject

    fun that(actual: LongArray?): PrimitiveLongArraySubject

    fun that(actual: ByteArray?): PrimitiveByteArraySubject

    fun that(actual: CharArray?): PrimitiveCharArraySubject

    fun that(actual: FloatArray?): PrimitiveFloatArraySubject

    fun that(actual: DoubleArray?): PrimitiveDoubleArraySubject

    fun <K, V> that(actual: Map<K, V>?): MapSubject<K, V>

    /**
     * Given a factory for some [Subject] class, returns [SimpleSubjectBuilder] whose
     * [that][SimpleSubjectBuilder.that] method creates instances of that class. Created subjects
     * use the previously set failure strategy and any previously set failure message.
     */
    fun <T, S : Subject<T>> about(
        subjectFactory: Subject.Factory<S, T>,
    ): SimpleSubjectBuilder<S, T>

    /**
     * Reports a failure.
     *
     * To set a message, first call [withMessage] (or, more commonly, use the shortcut
     * [assertWithMessage].
     */
    fun fail()

    internal open fun checkStatePreconditions()
}

internal fun commonForCustomFailureStrategy(
    failureStrategy: FailureStrategy
): StandardSubjectBuilder {
    return StandardSubjectBuilder(FailureMetadata(failureStrategy = failureStrategy))
}

internal fun StandardSubjectBuilder.commonWithMessage(
    messageToPrepend: String?
): StandardSubjectBuilder =
    StandardSubjectBuilder(metadata = metadata.withMessage(message = messageToPrepend))

internal fun <T> StandardSubjectBuilder.commonThat(actual: T?): Subject<T> =
    Subject(actual = actual, metadata = metadata, null)

// actual cannot be made nullable due to autoboxing and this overload is necessary to allow
// StandardSubjectBuilder.that(char) from Java to resolve properly as an Object
// (otherwise it is source-incompatibly interpreted as Int).
// See: NumericComparisonTest#testNumericPrimitiveTypes_isNotEqual_shouldFail_charToInt
internal fun StandardSubjectBuilder.commonThat(actual: Char): Subject<Char> =
    Subject(actual = actual, metadata = metadata, null)

internal fun <T : Comparable<T>> StandardSubjectBuilder.commonThat(
    actual: T?
): ComparableSubject<T> = ComparableSubject(actual = actual, metadata = metadata)

internal fun <T : Throwable> StandardSubjectBuilder.commonThat(actual: T?): ThrowableSubject<T> =
    ThrowableSubject(actual = actual, metadata = metadata, "throwable")

internal fun StandardSubjectBuilder.commonThat(actual: Boolean?): BooleanSubject =
    BooleanSubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: Long): LongSubject =
    LongSubject(actual = actual, metadata = metadata)

// Workaround for https://youtrack.jetbrains.com/issue/KT-645
internal fun <T : Long?> StandardSubjectBuilder.commonThat(actual: T): LongSubject =
    LongSubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: Double?): DoubleSubject =
    DoubleSubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: Float?): FloatSubject =
    FloatSubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: Int): IntegerSubject =
    IntegerSubject(actual = actual, metadata = metadata)

// Workaround for https://youtrack.jetbrains.com/issue/KT-645
internal fun <T : Int?> StandardSubjectBuilder.commonThat(actual: T): IntegerSubject =
    IntegerSubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: String?): StringSubject =
    StringSubject(actual = actual, metadata = metadata)

internal fun <T> StandardSubjectBuilder.commonThat(actual: Iterable<T>?): IterableSubject<T> =
    IterableSubject(actual = actual, metadata = metadata)

internal fun <T> StandardSubjectBuilder.commonThat(actual: Array<out T>?): ObjectArraySubject<T> =
    ObjectArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(
    actual: BooleanArray?
): PrimitiveBooleanArraySubject = PrimitiveBooleanArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: ShortArray?): PrimitiveShortArraySubject =
    PrimitiveShortArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: IntArray?): PrimitiveIntArraySubject =
    PrimitiveIntArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: LongArray?): PrimitiveLongArraySubject =
    PrimitiveLongArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: ByteArray?): PrimitiveByteArraySubject =
    PrimitiveByteArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: CharArray?): PrimitiveCharArraySubject =
    PrimitiveCharArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: FloatArray?): PrimitiveFloatArraySubject =
    PrimitiveFloatArraySubject(actual = actual, metadata = metadata)

internal fun StandardSubjectBuilder.commonThat(actual: DoubleArray?): PrimitiveDoubleArraySubject =
    PrimitiveDoubleArraySubject(actual = actual, metadata = metadata)

internal fun <K, V> StandardSubjectBuilder.commonThat(actual: Map<K, V>?): MapSubject<K, V> =
    MapSubject(actual = actual, metadata = metadata)

internal fun <T, S : Subject<T>> StandardSubjectBuilder.commonAbout(
    subjectFactory: Subject.Factory<S, T>,
): SimpleSubjectBuilder<S, T> =
    SimpleSubjectBuilder(metadata = metadata, subjectFactory = subjectFactory)

internal fun StandardSubjectBuilder.commonFail() {
    metadata.fail()
}
