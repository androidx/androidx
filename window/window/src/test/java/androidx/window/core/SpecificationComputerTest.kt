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

package androidx.window.core

import androidx.window.core.SpecificationComputer.Companion.startSpecification
import androidx.window.core.VerificationMode.LOG
import androidx.window.core.VerificationMode.QUIET
import androidx.window.core.VerificationMode.STRICT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.contains
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Tests [SpecificationComputer] to verify the following behaviors.
 * The key use-cases are when [SpecificationComputer.require] has failed and the expected behaviors
 * are as follows. For [STRICT] mode, [SpecificationComputer.compute] should throw an exception.
 * For [QUIET] mode, [SpecificationComputer.compute] should return a null value. For [LOG] mode,
 * [SpecificationComputer.compute] should write a log and return a null value.
 */
class SpecificationComputerTest {

    @Test
    fun run_returnsValidValue() {
        val actual = DATA.startSpecification(TAG, QUIET).compute()

        assertEquals(DATA, actual)
    }

    @Test
    fun run_returnsNullIfRequiredCheckFails() {
        val actual = DATA.startSpecification(TAG, QUIET)
            .require(MESSAGE) { this.isEmpty() }
            .compute()

        assertNull(actual)
    }

    @Test(expected = WindowStrictModeException::class)
    fun run_throwsIfStrictModeEnabled() {
        DATA.startSpecification(TAG, STRICT)
            .require(MESSAGE) { this.isEmpty() }
            .compute()
    }

    @Test
    fun run_exceptionStackAtRunCaller() {
        var actual: WindowStrictModeException? = null
        try {
            DATA.startSpecification(TAG, STRICT)
                .require(MESSAGE) { this.isEmpty() }
                .compute()
        } catch (e: WindowStrictModeException) {
            actual = e
        }
        assertNotNull(actual)
        assertEquals("run_exceptionStackAtRunCaller", actual!!.stackTrace.first().methodName)
        assertTrue(actual.message!!.contains(MESSAGE))
    }

    @Test
    fun run_logsIfLoggingModeEnabled() {
        val logger = mock<Logger>()
        DATA.startSpecification(TAG, LOG, logger)
            .require(MESSAGE) { this.isEmpty() }
            .compute()

        verify(logger).debug(eq(TAG), contains(MESSAGE))
    }

    @Test
    fun check_logsIfLoggingModeEnabled() {
        val logger = mock<Logger>()
        DATA.startSpecification(TAG, LOG, logger)
            .require(MESSAGE) { this.isEmpty() }
            .compute()

        verify(logger).debug(eq(TAG), contains(MESSAGE))
    }

    companion object {
        const val DATA = "test-data-23849573"
        const val TAG = "test-tag-3462353414123412"
        const val MESSAGE = "test-message-123412431241235"
    }
}
