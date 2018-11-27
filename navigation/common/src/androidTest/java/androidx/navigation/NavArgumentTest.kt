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

package androidx.navigation

import android.os.Bundle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SmallTest
class NavArgumentTest {
    @Test
    fun putDefaultValue() {
        val bundle = Bundle()
        val argument = NavArgument.Builder()
            .setDefaultValue("abc")
            .setType(NavType.StringType)
            .build()
        argument.putDefaultValue("name", bundle)
        assertThat(bundle.get("name"))
            .isEqualTo("abc")
    }

    @Test
    fun verify() {
        val bundle = Bundle().apply {
            putString("stringArg", "abc")
            putInt("intArg", 123)
            putIntArray("intArrayArg", null)
        }

        val stringArgument = NavArgument.Builder()
            .setType(NavType.StringType)
            .build()
        val intArgument = NavArgument.Builder()
            .setType(NavType.IntType)
            .build()
        val intArrArgument = NavArgument.Builder()
            .setType(NavType.IntArrayType)
            .setIsNullable(true)
            .build()
        val intArrNonNullArgument = NavArgument.Builder()
            .setType(NavType.IntArrayType)
            .setIsNullable(false)
            .build()

        assertThat(stringArgument.verify("stringArg", bundle)).isTrue()
        assertThat(intArgument.verify("intArg", bundle)).isTrue()
        assertThat(intArrArgument.verify("intArrayArg", bundle)).isTrue()
        assertThat(intArrNonNullArgument.verify("intArrayArg", bundle)).isFalse()
    }
}
