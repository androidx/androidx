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

package androidx.wear.protolayout.material3

import java.nio.charset.StandardCharsets

/** Returns byte array representation of tag from String. */
internal fun String.toTagBytes(): ByteArray = toByteArray(StandardCharsets.UTF_8)

internal fun <T> Iterable<T>.addBetween(newItem: T): Sequence<T> = sequence {
    var isFirst = true
    for (element in this@addBetween) {
        if (!isFirst) {
            yield(newItem)
        } else {
            isFirst = false
        }
        yield(element)
    }
}
