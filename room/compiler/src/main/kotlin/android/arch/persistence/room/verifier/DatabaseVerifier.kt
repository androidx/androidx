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

package android.arch.persistence.room.verifier

import columnInfo
import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.vo.Entity
import android.arch.persistence.room.vo.Warning
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import javax.lang.model.element.Element

/**
 * Builds an in-memory version of the database and verifies the queries against it.
 * This class is also used to resolve the return types.
 */
class DatabaseVerifier private constructor(
        val connection : Connection, val context : Context, val entities : List<Entity>) {
    companion object {
        /**
         * Tries to create a verifier but returns null if it cannot find the driver.
         */
        fun create(context: Context, element: Element, entities: List<Entity>) : DatabaseVerifier? {
            return try {
                // see: https://github.com/xerial/sqlite-jdbc/issues/97
                val tmpDir = System.getProperty("java.io.tmpdir")
                if (tmpDir == null) {
                    context.logger.w(Warning.MISSING_JAVA_TMP_DIR,
                            element, DatabaseVerificaitonErrors.CANNOT_GET_TMP_JAVA_DIR)
                    return null
                }
                val outDir = File(tmpDir, "room-${UUID.randomUUID()}")
                outDir.mkdirs()
                outDir.deleteOnExit()
                System.setProperty("org.sqlite.tmpdir", outDir.absolutePath)
                //force load:
                Class.forName("org.sqlite.JDBC")
                val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
                DatabaseVerifier(connection, context, entities)
            } catch (ex : Exception) {
                context.logger.w(Warning.CANNOT_CREATE_VERIFICATION_DATABASE, element,
                        DatabaseVerificaitonErrors.cannotCreateConnection(ex))
                null
            }
        }
    }
    init {
        entities.forEach { entity ->
            val stmt = connection.createStatement()
            stmt.executeUpdate(entity.createTableQuery)
        }
    }

    fun analyze(sql : String) : QueryResultInfo {
        return try {
            val stmt = connection.prepareStatement(sql)
            QueryResultInfo(stmt.columnInfo())
        } catch (ex : SQLException) {
            QueryResultInfo(emptyList(), ex)
        }
    }

    fun closeConnection() {
        if (!connection.isClosed) {
            try {
                connection.close()
            } catch (t : Throwable) {
                //ignore.
            }
        }
    }
}
