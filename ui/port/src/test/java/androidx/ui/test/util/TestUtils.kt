/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.test.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import kotlin.reflect.KClass

fun assertThrows(cls: KClass<*>, func: () -> Unit) = assertThrows(cls.java, func)

fun assertThrows(cls: Class<*>, func: () -> Unit) {
    try {
        func()
        fail("Expected exception of type " + cls.name)
    } catch (e: Throwable) {
        assertThat(e).isInstanceOf(cls)
    }
}

// TODO(Migration/ryanmentley): Is this actually a good idea?
// This is based on equalsIgnoringHashCodes
fun String.normalizeHashCodes(): String {
    return this.replace(Regex("#[0-9a-f]{5}"), "#00000")
}
