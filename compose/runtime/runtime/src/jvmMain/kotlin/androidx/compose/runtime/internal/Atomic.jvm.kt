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

package androidx.compose.runtime.internal

import java.util.concurrent.atomic.AtomicInteger

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

internal actual class AtomicInt actual constructor(value: Int) : AtomicInteger(value) {
    actual fun add(amount: Int): Int = addAndGet(amount)

    // These are implemented by Number, but Kotlin fails to resolve them
    override fun toByte(): Byte = toInt().toByte()

    override fun toShort(): Short = toInt().toShort()

    @Deprecated(
        "Direct conversion to Char is deprecated. Use toInt().toChar() or Char " +
            "constructor instead.\nIf you override toChar() function in your Number inheritor, " +
            "it's recommended to gradually deprecate the overriding function and then " +
            "remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about " +
            "the migration",
        replaceWith = ReplaceWith("this.toInt().toChar()")
    )
    override fun toChar(): Char = toInt().toChar()
}
