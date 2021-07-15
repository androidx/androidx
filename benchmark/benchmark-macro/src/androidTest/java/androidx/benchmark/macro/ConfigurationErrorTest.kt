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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@SmallTest
public class ConfigurationErrorTest {
    @Test
    public fun constructor_valid() {
        ConfigurationError(
            id = "ID",
            summary = "summary",
            message = "message"
        )
    }

    @Test
    public fun constructor_throw() {
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
    public fun checkAndGetSuppressionState_empty() {
        // no throw or suppressed error
        assertNull(listOf<ConfigurationError>().checkAndGetSuppressionState(setOf()))
    }

    @Test
    public fun checkAndGetSuppressionState_suppressed() {
        // two suppressed errors
        val suppression = listOf(
            ConfigurationError(
                id = "ID1",
                summary = "summary1",
                message = "message1"
            ),
            ConfigurationError(
                id = "ID2",
                summary = "summary2",
                message = "message2"
            )
        ).checkAndGetSuppressionState(setOf("ID1", "ID2"))

        assertNotNull(suppression)
        assertEquals("ID1_ID2_", suppression.prefix)
        assertEquals(
            """
                |WARNING: summary1
                |    message1

                |WARNING: summary2
                |    message2

            """.trimMargin(),
            suppression.warningMessage
        )
    }

    @Test
    public fun checkAndGetSuppressionState_unsuppressed() {
        // one unsuppressed error, so throw
        val exception = assertFailsWith<AssertionError> {
            listOf(
                ConfigurationError(
                    id = "ID1",
                    summary = "summary1",
                    message = "message1"
                ),
                ConfigurationError(
                    id = "ID2",
                    summary = "summary2",
                    message = "message2"
                )
            ).checkAndGetSuppressionState(setOf("ID1"))
        }

        val message = exception.message!!
        assertTrue(message.contains("ERRORS (not suppressed): ID2"))
        assertTrue(message.contains("WARNINGS (suppressed): ID1"))
        assertTrue(
            message.contains(
                """
                |
                |ERROR: summary2
                |    message2
                |
            """.trimMargin()
            )
        )
        // suppression warning should contain *both* errors to be suppressed
        assertTrue(
            message.contains(
                """testInstrumentationRunnerArguments""" +
                    """["androidx.benchmark.suppressErrors"] = "ID1,ID2""""
            )
        )
    }
}
