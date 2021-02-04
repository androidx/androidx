/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConfigurationErrorTest {
    @Test
    fun constructor_valid() {
        ConfigurationError(
            id = "ID",
            summary = "summary",
            message = "message"
        )
    }

    @Test
    fun constructor_throw() {
        assertFailsWith<IllegalArgumentException> {
            ConfigurationError(
                id = "idCanNotHaveLowercase", // invalid, IDs always uppercase
                summary = "summary",
                message = "message"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            ConfigurationError(
                id = "i_d", // invalid, IDs can't have underscores
                summary = "summary",
                message = "message"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ConfigurationError(
                id = "ID",
                summary = "summary\nsummary", // invalid, summary is single line
                message = "message"
            )
        }
    }

    @Test
    fun throwErrorIfNotEmpty() {
        listOf<ConfigurationError>().throwErrorIfNotEmpty() // no error
        val error = ConfigurationError(
            id = "ID",
            summary = "summary",
            message = "message"
        )
        val exception = assertFailsWith<AssertionError> {
            listOf(error, error).throwErrorIfNotEmpty()
        }

        val expected = """
            |ERRORS: ID, ID
            |ERROR: summary
            |    message

            |ERROR: summary
            |    message

        """.trimMargin()

        assertEquals(expected.split("\n"), exception.message!!.split("\n"))
    }
}
