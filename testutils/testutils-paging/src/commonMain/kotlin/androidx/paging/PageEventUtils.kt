/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import kotlin.test.assertEquals

fun <T> assertEvents(expected: List<T>, actual: List<T>) {
    try {
        assertEquals(expected, actual)
    } catch (e: Throwable) {
        val msg = e.message!!
            .replace("),", "),\n")
            .replace("<[", "<[\n ")
            .replace("actual", "\nactual")
            .lines()
            .toMutableList()

        if (expected.count() != actual.count()) throw AssertionError(msg.joinToString("\n"))

        var index = 0
        for (i in 0 until expected.count()) {
            if (expected[i] != actual[i]) {
                index = i
                break
            }
        }
        msg[index + 1] = msg[index + 1].prependIndent(" >")
        msg[msg.count() / 2 + index + 1] = msg[msg.count() / 2 + index + 1].prependIndent(" >")
        throw AssertionError(msg.joinToString("\n"))
    }
}
