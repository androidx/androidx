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

@file:JvmName("Kruth")

package androidx.kruth

import kotlin.jvm.JvmName

private val ASSERT = StandardSubjectBuilder.forCustomFailureStrategy { throw it }

/**
 * Begins a call chain with the fluent Truth API. If the check made by the chain fails, it will
 * throw [AssertionError].
 */
@Suppress("FunctionName") // The underscore is a weird but intentional choice.
fun assert_() = ASSERT

// The order of these declarations follows those defined in Truth, which maintains their special
// ordering of which Subject type to prioritize from the general `assertThat` factory method. See:
// https://github.com/google/truth/blob/master/core/src/main/java/com/google/common/truth/Truth.java

fun <T : Comparable<T>> assertThat(actual: T?): ComparableSubject<T> = assert_().that(actual)

fun <T> assertThat(actual: T?): Subject<T> = assert_().that(actual)

fun <T : Throwable> assertThat(actual: T?): ThrowableSubject<T> = assert_().that(actual)

fun assertThat(actual: Boolean?): BooleanSubject = assert_().that(actual)

fun assertThat(actual: Long): LongSubject = assert_().that(actual)

// Workaround for https://youtrack.jetbrains.com/issue/KT-645
fun <T : Long?> assertThat(actual: T): LongSubject = assert_().that(actual)

fun assertThat(actual: Double?): DoubleSubject = assert_().that(actual)

fun assertThat(actual: Int): IntegerSubject = assert_().that(actual)

// Workaround for https://youtrack.jetbrains.com/issue/KT-645
fun <T : Int?> assertThat(actual: T): IntegerSubject = assert_().that(actual)

fun assertThat(actual: String?): StringSubject = assert_().that(actual)

fun <T> assertThat(actual: Iterable<T>?): IterableSubject<T> = assert_().that(actual)

fun <T> assertThat(actual: Array<out T>?): ObjectArraySubject<T> = assert_().that(actual)

fun assertThat(actual: BooleanArray?): PrimitiveBooleanArraySubject = assert_().that(actual)

fun assertThat(actual: ShortArray?): PrimitiveShortArraySubject = assert_().that(actual)

fun assertThat(actual: IntArray?): PrimitiveIntArraySubject = assert_().that(actual)

fun assertThat(actual: LongArray?): PrimitiveLongArraySubject = assert_().that(actual)

fun assertThat(actual: ByteArray?): PrimitiveByteArraySubject = assert_().that(actual)

fun assertThat(actual: CharArray?): PrimitiveCharArraySubject = assert_().that(actual)

fun assertThat(actual: FloatArray?): PrimitiveFloatArraySubject = assert_().that(actual)

fun assertThat(actual: DoubleArray?): PrimitiveDoubleArraySubject = assert_().that(actual)

fun <K, V> assertThat(actual: Map<K, V>?): MapSubject<K, V> = assert_().that(actual)

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
    subjectFactory: Subject.Factory<S, T>
): SimpleSubjectBuilder<S, T> {
    return assert_().about(subjectFactory)
}
