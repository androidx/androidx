/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
import kotlin.reflect.KProperty
import kotlin.test.Test

@Stable
@Suppress("unused")
class CompositionJvmTests {
    /* TODO: Move this test back to commonTest after updating to Kotlin 2.2
       Due to a bug in Kotlin 2.1.2x https://youtrack.jetbrains.com/issue/KT-77508, compilation of
       the tests for K/JS and K/Native fails with
       "Wrong number of parameters in wrapper: expected: 0 bound and 2 unbound, but 0 found".
       So ignoring doesn't really work for this case.
    */
    @Test
    fun composableDelegates() = compositionTest {
        val local = compositionLocalOf { "Default" }
        val delegatedLocal by local
        compose {
            Text(delegatedLocal)

            CompositionLocalProvider(local provides "Scoped") { Text(delegatedLocal) }
        }
        validate {
            Text("Default")
            Text("Scoped")
        }
    }
}

@Composable
private operator fun <T> CompositionLocal<T>.getValue(thisRef: Any?, property: KProperty<*>) =
    current
