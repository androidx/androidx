/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.verifier

import androidx.room.processor.Context
import androidx.room.vo.Entity
import androidx.room.vo.Warning
import columnInfo
import org.sqlite.JDBC
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import java.util.regex.Pattern
import javax.lang.model.element.Element

/**
 * Builds an in-memory version of the database and verifies the queries against it.
 * This class is also used to resolve the return types.
 */
class DatabaseVerifier private constructor(
        val connection: Connection, val context: Context, val entities: List<Entity>) {
    companion object {
        private const val CONNECTION_URL = "jdbc:sqlite::memory:"
        /**
         * Taken from:
         * https://github.com/robolectric/robolectric/blob/master/shadows/framework/
         * src/main/java/org/robolectric/shadows/ShadowSQLiteConnection.java#L94
         *
         * This is actually not accurate because it might swap anything since it does not parse
         * SQL. That being said, for the verification purposes, it does not matter and clearly
         * much easier than parsing and rebuilding the query.
         */
        private val COLLATE_LOCALIZED_UNICODE_PATTERN = Pattern.compile(
                "\\s+COLLATE\\s+(LOCALIZED|UNICODE)", Pattern.CASE_INSENSITIVE)

        init {
            // see: https://github.com/xerial/sqlite-jdbc/issues/97
            val tmpDir = System.getProperty("java.io.tmpdir")
            if (tmpDir != null) {
                val outDir = File(tmpDir, "room-${UUID.randomUUID()}")
                outDir.mkdirs()
                outDir.deleteOnExit()
                System.setProperty("org.sqlite.tmpdir", outDir.absolutePath)
                // dummy call to trigger JDBC initialization so that we can unregister it
                JDBC.isValidURL(CONNECTION_URL)
                unregisterDrivers()
            }
        }

        /**
         * Tries to create a verifier but returns null if it cannot find the driver.
         */
        fun create(context: Context, element: Element, entities: List<Entity>): DatabaseVerifier? {
            return try {
                val connection = JDBC.createConnection(CONNECTION_URL, java.util.Properties())
                DatabaseVerifier(connection, context, entities)
            } catch (ex: Exception) {
                context.logger.w(Warning.CANNOT_CREATE_VERIFICATION_DATABASE, element,
                        DatabaseVerificaitonErrors.cannotCreateConnection(ex))
                null
            }
        }

        /**
         * Unregisters the JDBC driver. If we don't do this, we'll leak the driver which leaks a
         * whole class loader.
         * see: https://github.com/xerial/sqlite-jdbc/issues/267
         * see: https://issuetracker.google.com/issues/62473121
         */
        private fun unregisterDrivers() {
            try {
                DriverManager.getDriver(CONNECTION_URL)?.let {
                    DriverManager.deregisterDriver(it)
                }
            } catch (t: Throwable) {
                System.err.println("Room: cannot unregister driver ${t.message}")
            }
        }
    }
    init {
        entities.forEach { entity ->
            val stmt = connection.createStatement()
            stmt.executeUpdate(stripLocalizeCollations(entity.createTableQuery))
        }
    }

    fun analyze(sql: String): QueryResultInfo {
        return try {
            val stmt = connection.prepareStatement(stripLocalizeCollations(sql))
            QueryResultInfo(stmt.columnInfo())
        } catch (ex: SQLException) {
            QueryResultInfo(emptyList(), ex)
        }
    }

    private fun stripLocalizeCollations(sql: String) =
        COLLATE_LOCALIZED_UNICODE_PATTERN.matcher(sql).replaceAll(" COLLATE NOCASE")

    fun closeConnection(context: Context) {
        if (!connection.isClosed) {
            try {
                connection.close()
            } catch (t: Throwable) {
                //ignore.
                context.logger.d("failed to close the database connection ${t.message}")
            }
        }
    }
}
