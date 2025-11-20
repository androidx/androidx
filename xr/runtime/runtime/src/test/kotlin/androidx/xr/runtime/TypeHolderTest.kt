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

package androidx.xr.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypeHolderTest {
    @Test
    fun typeHolder_safeAccess() {
        // Because TypeHolder is sealed, use NodeHolder to create a StringHolder.
        val stringHolder: TypeHolder<*> = NodeHolder("Hello", String::class.java)

        // Use assertGetValue for a safe retrieval that throws on type mismatch.
        val stringValue: String = TypeHolder.assertGetValue(stringHolder, String::class.java)

        assertThat(stringValue).isEqualTo("Hello")
        // Attempting to get the value as an Int will throw a ClassCastException.
        assertFailsWith<ClassCastException> {
            TypeHolder.assertGetValue(stringHolder, Int::class.java)
        }
    }

    @Test
    fun typeHolder_safeCast() {
        // Because TypeHolder is sealed, use NodeHolder to create a StringHolder.
        val stringHolder: TypeHolder<*> = NodeHolder("Hello", String::class.java)

        // Use safeCast to get a typed holder, or null if the types don't match.
        val typedHolder: TypeHolder<String>? = TypeHolder.safeCast(stringHolder, String::class.java)

        assertThat(typedHolder).isNotNull()
        assertThat(typedHolder?.value).isEqualTo("Hello")

        // Attempting to cast to the wrong type will result in null.
        val nullHolder: TypeHolder<Int>? = TypeHolder.safeCast(stringHolder, Int::class.java)

        assertThat(nullHolder).isNull()
    }
}
