/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import java.io.IOException
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackupServiceLoggerTest {

    private val julLogger = Logger.getLogger(LOGGER_NAME)
    private val records = mutableListOf<LogRecord>()
    private val recorder =
        object : Handler() {
            override fun publish(record: LogRecord) {
                records += record
            }

            override fun flush() {}

            override fun close() {}
        }

    @Before
    fun recordLogs() {
        julLogger.addHandler(recorder)
        julLogger.useParentHandlers = false
    }

    @After
    fun stopRecordingLogs() {
        julLogger.removeHandler(recorder)
        julLogger.useParentHandlers = true
        julLogger.level = null
    }

    @Test
    fun logsEachSeverityAtTheMatchingLevel() {
        julLogger.level = Level.ALL
        val failure = IOException("Device offline")
        val logger = BackupServiceLogger(LOGGER_NAME)

        logger.error("error", failure)
        logger.warn("warning")
        logger.info("info")
        logger.debug("debug")

        assertEquals(
            listOf(Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE),
            records.map { it.level },
        )
        assertEquals(listOf("error", "warning", "info", "debug"), records.map { it.message })
        assertEquals(listOf(failure, null, null, null), records.map { it.thrown })
    }

    @Test
    fun debugIsEnabledOnlyWhenFineMessagesAreLogged() {
        val logger = BackupServiceLogger(LOGGER_NAME)

        julLogger.level = Level.INFO
        assertFalse(logger.isDebugEnabled)
        logger.debug("dropped")
        assertEquals(emptyList(), records)

        julLogger.level = Level.FINE
        assertTrue(logger.isDebugEnabled)
    }

    @Test
    fun theStudioLoggerInterfaceComesOnlyFromBackupRestore() {
        val copies =
            javaClass.classLoader
                .getResources("com/android/tools/environment/Logger.class")
                .toList()

        assertEquals(1, copies.size, "$copies")
        assertTrue(copies.single().path.contains("backup-restore"), "${copies.single()}")
    }

    private companion object {
        const val LOGGER_NAME = "androidx.test.backup.host.BackupServiceLoggerTest"
    }
}
