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

typealias CharSubject = Subject<Char>

/**
 * In a fluent assertion chain, an object with which you can do any of the following:
 *
 * - Set an optional message with [withMessage].
 * - For the types of [Subject] built into Kruth, directly specify the value under test
 * with [withMessage].
 */
@Suppress("StaticFinalBuilder") // Cannot be final for binary compatibility.
open class StandardSubjectBuilder internal constructor(
    metadata: FailureMetadata,
) : PlatformStandardSubjectBuilder by PlatformStandardSubjectBuilderImpl(metadata) {
    internal val metadata = metadata
        get() {
            checkStatePreconditions()
            return field
        }

    companion object {
        /**
         * Returns a new instance that invokes the given [FailureStrategy] when a check fails.
         */
        @JvmStatic
        fun forCustomFailureStrategy(failureStrategy: FailureStrategy): StandardSubjectBuilder {
            return StandardSubjectBuilder(FailureMetadata(failureStrategy = failureStrategy))
        }
    }

    /**
     * Returns a new instance that will output the given message before the main failure message. If
     * this method is called multiple times, the messages will appear in the order that they were
     * specified.
     */
    fun withMessage(messageToPrepend: String): StandardSubjectBuilder =
        StandardSubjectBuilder(metadata = metadata.withMessage(message = messageToPrepend))

    fun <T> that(actual: T?): Subject<T> =
        Subject(actual = actual, metadata = metadata, null)

    // actual cannot be made nullable due to autoboxing and this overload is necessary to allow
    // StandardSubjectBuilder.that(char) from Java to resolve properly as an Object
    // (otherwise it is source-incompatibly interpreted as Int).
    // See: NumericComparisonTest#testNumericPrimitiveTypes_isNotEqual_shouldFail_charToInt
    fun that(actual: Char): Subject<Char> =
        Subject(actual = actual, metadata = metadata, null)

    fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T> =
        ComparableSubject(actual = actual, metadata = metadata)

    fun <T : Throwable> that(actual: T?): ThrowableSubject<T> =
        ThrowableSubject(actual = actual, metadata = metadata, "throwable")

    fun that(actual: Boolean?): BooleanSubject =
        BooleanSubject(actual = actual, metadata = metadata)

    fun that(actual: Long): LongSubject =
        LongSubject(actual = actual, metadata = metadata)

    // Workaround for https://youtrack.jetbrains.com/issue/KT-645
    fun <T : Long?> that(actual: T): LongSubject =
        LongSubject(actual = actual, metadata = metadata)

    fun that(actual: Double?): DoubleSubject =
        DoubleSubject(actual = actual, metadata = metadata)

    fun that(actual: Int): IntegerSubject =
        IntegerSubject(actual = actual, metadata = metadata)

    // Workaround for https://youtrack.jetbrains.com/issue/KT-645
    fun <T : Int?> that(actual: T): IntegerSubject =
        IntegerSubject(actual = actual, metadata = metadata)

    fun that(actual: String?): StringSubject =
        StringSubject(actual = actual, metadata = metadata)

    fun <T> that(actual: Iterable<T>?): IterableSubject<T> =
        IterableSubject(actual = actual, metadata = metadata)

    fun <T> that(actual: Array<out T>?): ObjectArraySubject<T> =
        ObjectArraySubject(actual = actual, metadata = metadata)

    fun that(actual: BooleanArray?): PrimitiveBooleanArraySubject =
        PrimitiveBooleanArraySubject(actual = actual, metadata = metadata)

    fun that(actual: ShortArray?): PrimitiveShortArraySubject =
        PrimitiveShortArraySubject(actual = actual, metadata = metadata)

    fun that(actual: IntArray?): PrimitiveIntArraySubject =
        PrimitiveIntArraySubject(actual = actual, metadata = metadata)

    fun that(actual: LongArray?): PrimitiveLongArraySubject =
        PrimitiveLongArraySubject(actual = actual, metadata = metadata)

    fun that(actual: ByteArray?): PrimitiveByteArraySubject =
        PrimitiveByteArraySubject(actual = actual, metadata = metadata)

    fun that(actual: CharArray?): PrimitiveCharArraySubject =
        PrimitiveCharArraySubject(actual = actual, metadata = metadata)

    fun that(actual: FloatArray?): PrimitiveFloatArraySubject =
        PrimitiveFloatArraySubject(actual = actual, metadata = metadata)

    fun that(actual: DoubleArray?): PrimitiveDoubleArraySubject =
        PrimitiveDoubleArraySubject(actual = actual, metadata = metadata)

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
    fun fail() {
        metadata.fail()
    }

    internal open fun checkStatePreconditions() {}
}

/** Platform-specific additions for [StandardSubjectBuilder]. */
internal expect interface PlatformStandardSubjectBuilder

internal expect class PlatformStandardSubjectBuilderImpl(
    metadata: FailureMetadata,
) : PlatformStandardSubjectBuilder
