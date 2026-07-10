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

import android.util.Log as AndroidLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.junit.rules.ExpectedLogMessagesRule

@RunWith(AndroidJUnit4::class)
class XrLogTest {

    companion object {
        const val TEST_TAG: String = XrLog.TAG
        const val MESSAGE_VERBOSE: String = "Verbose message"
        const val MESSAGE_DEBUG: String = "Debug message"
        const val MESSAGE_INFO: String = "Info message"
        const val MESSAGE_WARN: String = "Warn message"
        const val MESSAGE_ERROR: String = "Error message"
    }

    @Rule @JvmField val expectedLogMessagesRule = ExpectedLogMessagesRule()

    @Test
    fun log_whenEnabledWithLevelVERBOSE_logsVerboseAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.VERBOSE

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }

        expectedLogMessagesRule.expectLogMessage(AndroidLog.VERBOSE, TEST_TAG, MESSAGE_VERBOSE)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.DEBUG, TEST_TAG, MESSAGE_DEBUG)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithStringAndLevelVERBOSE_logsVerboseAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.VERBOSE

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)

        expectedLogMessagesRule.expectLogMessage(AndroidLog.VERBOSE, TEST_TAG, MESSAGE_VERBOSE)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.DEBUG, TEST_TAG, MESSAGE_DEBUG)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithLevelDEBUG_logsDebugAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.DEBUG

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }

        expectedLogMessagesRule.expectLogMessage(AndroidLog.DEBUG, TEST_TAG, MESSAGE_DEBUG)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithStringAndLevelDEBUG_logsDebugAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.DEBUG

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)

        expectedLogMessagesRule.expectLogMessage(AndroidLog.DEBUG, TEST_TAG, MESSAGE_DEBUG)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithLevelINFO_logsInfoAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.INFO

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }

        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithStringAndLevelINFO_logsInfoAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.INFO

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)

        expectedLogMessagesRule.expectLogMessage(AndroidLog.INFO, TEST_TAG, MESSAGE_INFO)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithLevelWARN_logsWarnAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.WARN

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }

        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithStringAndLevelWARN_logsWarnAndAbove() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.WARN

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)

        expectedLogMessagesRule.expectLogMessage(AndroidLog.WARN, TEST_TAG, MESSAGE_WARN)
        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithLevelERROR_logsErrorOnly() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.ERROR

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }

        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenEnabledWithStringAndLevelERROR_logsErrorOnly() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.ERROR

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)

        expectedLogMessagesRule.expectLogMessage(AndroidLog.ERROR, TEST_TAG, MESSAGE_ERROR)
    }

    @Test
    fun log_whenDisabled_logsNothing() {
        XrLog.isEnabled = false
        XrLog.level = XrLog.Level.VERBOSE

        XrLog.verbose { MESSAGE_VERBOSE }
        XrLog.debug { MESSAGE_DEBUG }
        XrLog.info { MESSAGE_INFO }
        XrLog.warn { MESSAGE_WARN }
        XrLog.error { MESSAGE_ERROR }
    }

    @Test
    fun log_withStringWhenDisabled_logsNothing() {
        XrLog.isEnabled = false
        XrLog.level = XrLog.Level.VERBOSE

        XrLog.verbose(MESSAGE_VERBOSE)
        XrLog.debug(MESSAGE_DEBUG)
        XrLog.info(MESSAGE_INFO)
        XrLog.warn(MESSAGE_WARN)
        XrLog.error(MESSAGE_ERROR)
    }

    @Test
    fun log_withThrowable_logsMessagesAndThrowable() {
        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.VERBOSE
        val message = "Something went wrong"
        val throwable = IllegalArgumentException("Test Exception")

        XrLog.verbose(throwable) { message }
        XrLog.debug(throwable) { message }
        XrLog.info(throwable) { message }
        XrLog.warn(throwable) { message }
        XrLog.error(throwable) { message }

        expectedLogMessagesRule.expectLogMessageWithThrowable(
            AndroidLog.VERBOSE,
            TEST_TAG,
            message,
            throwable,
        )
        expectedLogMessagesRule.expectLogMessageWithThrowable(
            AndroidLog.DEBUG,
            TEST_TAG,
            message,
            throwable,
        )
        expectedLogMessagesRule.expectLogMessageWithThrowable(
            AndroidLog.INFO,
            TEST_TAG,
            message,
            throwable,
        )
        expectedLogMessagesRule.expectLogMessageWithThrowable(
            AndroidLog.WARN,
            TEST_TAG,
            message,
            throwable,
        )
        expectedLogMessagesRule.expectLogMessageWithThrowable(
            AndroidLog.ERROR,
            TEST_TAG,
            message,
            throwable,
        )
    }
}
