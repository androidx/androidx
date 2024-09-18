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

import com.google.common.base.Optional
import com.google.common.collect.Multimap
import com.google.common.collect.Multiset
import com.google.common.collect.Table
import java.math.BigDecimal

/**
 * In a fluent assertion chain, an object with which you can do any of the following:
 * - Set an optional message with [withMessage].
 * - For the types of [Subject] built into Kruth, directly specify the value under test with
 *   [withMessage].
 */
@Suppress("StaticFinalBuilder") // Cannot be final for binary compatibility.
actual open class StandardSubjectBuilder internal actual constructor(metadata: FailureMetadata) {
    internal actual val metadata: FailureMetadata = metadata
        get() {
            checkStatePreconditions()
            return field
        }

    actual companion object {
        /** Returns a new instance that invokes the given [FailureStrategy] when a check fails. */
        @JvmStatic
        actual fun forCustomFailureStrategy(
            failureStrategy: FailureStrategy
        ): StandardSubjectBuilder = commonForCustomFailureStrategy(failureStrategy)
    }

    /**
     * Returns a new instance that will output the given message before the main failure message. If
     * this method is called multiple times, the messages will appear in the order that they were
     * specified.
     */
    actual fun withMessage(messageToPrepend: String?): StandardSubjectBuilder =
        commonWithMessage(messageToPrepend)

    actual fun <T> that(actual: T?): Subject<T> = commonThat(actual)

    actual fun that(actual: Char): Subject<Char> = commonThat(actual)

    actual fun <T : Comparable<T>> that(actual: T?): ComparableSubject<T> = commonThat(actual)

    actual fun <T : Throwable> that(actual: T?): ThrowableSubject<T> = commonThat(actual)

    actual fun that(actual: Boolean?): BooleanSubject = commonThat(actual)

    actual fun that(actual: Long): LongSubject = commonThat(actual)

    actual fun <T : Long?> that(actual: T): LongSubject = commonThat(actual)

    actual fun that(actual: Double?): DoubleSubject = commonThat(actual)

    actual fun that(actual: Float?): FloatSubject = commonThat(actual)

    actual fun that(actual: Int): IntegerSubject = commonThat(actual)

    actual fun <T : Int?> that(actual: T): IntegerSubject = commonThat(actual)

    actual fun that(actual: String?): StringSubject = commonThat(actual)

    actual fun <T> that(actual: Iterable<T>?): IterableSubject<T> = commonThat(actual)

    actual fun <T> that(actual: Array<out T>?): ObjectArraySubject<T> = commonThat(actual)

    actual fun that(actual: BooleanArray?): PrimitiveBooleanArraySubject = commonThat(actual)

    actual fun that(actual: ShortArray?): PrimitiveShortArraySubject = commonThat(actual)

    actual fun that(actual: IntArray?): PrimitiveIntArraySubject = commonThat(actual)

    actual fun that(actual: LongArray?): PrimitiveLongArraySubject = commonThat(actual)

    actual fun that(actual: ByteArray?): PrimitiveByteArraySubject = commonThat(actual)

    actual fun that(actual: CharArray?): PrimitiveCharArraySubject = commonThat(actual)

    actual fun that(actual: FloatArray?): PrimitiveFloatArraySubject = commonThat(actual)

    actual fun that(actual: DoubleArray?): PrimitiveDoubleArraySubject = commonThat(actual)

    actual fun <K, V> that(actual: Map<K, V>?): MapSubject<K, V> = commonThat(actual)

    /**
     * Given a factory for some [Subject] class, returns [SimpleSubjectBuilder] whose
     * [that][SimpleSubjectBuilder.that] method creates instances of that class. Created subjects
     * use the previously set failure strategy and any previously set failure message.
     */
    actual fun <T, S : Subject<T>> about(
        subjectFactory: Subject.Factory<S, T>
    ): SimpleSubjectBuilder<S, T> = commonAbout(subjectFactory)

    /**
     * Reports a failure.
     *
     * To set a message, first call [withMessage] (or, more commonly, use the shortcut
     * [assertWithMessage].
     */
    actual fun fail() = commonFail()

    internal actual open fun checkStatePreconditions() {}

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun that(actual: Class<*>?): ClassSubject = ClassSubject(actual = actual, metadata = metadata)

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun <T : Any> that(actual: Optional<T>): GuavaOptionalSubject<T> =
        GuavaOptionalSubject(actual = actual, metadata = metadata)

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun that(actual: BigDecimal?): BigDecimalSubject =
        BigDecimalSubject(actual = actual, metadata = metadata)

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun <T> that(actual: Multiset<T>): MultisetSubject<T> =
        MultisetSubject(actual = actual, metadata = metadata)

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun <K, V> that(actual: Multimap<K, V>): MultimapSubject<K, V> =
        MultimapSubject(actual = actual, metadata = metadata)

    @Suppress("BuilderSetStyle") // Necessary for compatibility
    fun <R, C, V> that(actual: Table<R, C, V>): TableSubject<R, C, V> =
        TableSubject(actual = actual, metadata = metadata)
}
