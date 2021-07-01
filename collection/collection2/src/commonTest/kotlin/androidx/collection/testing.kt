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

@file:JvmName("TestingCommonKt")

package androidx.collection

import kotlin.jvm.JvmName

inline fun <reified T> assertThrows(body: () -> Any) {
    var fail = true
    try {
        body()
        fail = false
    } catch (t: Throwable) {
        if (t !is T) {
            throw t
        }
    }
    if (!fail) {
        throw AssertionError("Expected ${T::class} but body completed successfully")
    }
}

expect fun testBody(body: () -> Unit)
