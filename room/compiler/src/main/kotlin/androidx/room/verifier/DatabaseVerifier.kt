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
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import androidx.room.vo.FtsOptions
import androidx.room.vo.Warning
import columnInfo
import org.sqlite.JDBC
import java.io.File
import java.nio.channels.FileChannel
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import java.util.regex.Pattern
import javax.lang.model.element.Element

/**
 * Builds an in-memory version of the database and verifies the queries against it.
 * This class is also used to resolve the return types.
 */
class DatabaseVerifier private constructor(
    val connection: Connection,
    val context: Context,
    val entities: List<Entity>,
    views: List<DatabaseView>
) {
    companion object {
        private const val CONNECTION_URL = "jdbc:sqlite::memory:"
        private const val SQLITE_INITIALIZED_FLAG = "room.sqlite.initialized"
        private const val SQLITE_TEMPDIR_FLAG = "org.sqlite.tmpdir"
        private val NATIVE_LIB_RELOAD_RETRY_CNT = 5
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

        /**
         * Native lib extensions for linux, mac and windows
         */
        private val SQLITE_NATIVE_LIB_EXTENSIONS = arrayOf(".so", ".jnilib", ".dll")

        private lateinit var sqliteNativeLibDir: File

        init {
            copyNativeLibs()
        }

        /**
         * Tells whether sqlite was previously initialized successfully
         */
        private fun reusePreviousSqliteTempdir(): Boolean {
            val previouslyInitialized = System.getProperty(SQLITE_INITIALIZED_FLAG) != null
            if (!previouslyInitialized) {
                return false
            }
            val previousTempDirString = System.getProperty(SQLITE_TEMPDIR_FLAG)
            if (previousTempDirString == null) {
                return false
            }
            val previousTempDir = File(previousTempDirString)
            if (!previousTempDir.isDirectory) {
                return false
            }
            // reuse existing temp dir
            sqliteNativeLibDir = previousTempDir
            return true
        }

        /**
         * Copies native libraries into a tmp folder to be loaded.
         */
        private fun copyNativeLibs() {
            // check whether a previous initialization succeeded
            if (reusePreviousSqliteTempdir()) {
                return
            }
            // synchronize on System.getProperties in case this method is called concurrently from multiple classloaders
            synchronized(System.getProperties()) {
                // check again (inside synchronization) whether a previous initialization succeeded
                if (reusePreviousSqliteTempdir()) {
                    return
                }

                // set up sqlite
                // see: https://github.com/xerial/sqlite-jdbc/issues/97
                val baseTempDir = System.getProperty("java.io.tmpdir")
                checkNotNull(baseTempDir) {
                    "Room needs java.io.tmpdir system property to be set to setup sqlite"
                }
                sqliteNativeLibDir = File(baseTempDir, "room-${UUID.randomUUID()}")
                sqliteNativeLibDir.mkdirs()
                sqliteNativeLibDir.deleteOnExit()
                System.setProperty(SQLITE_TEMPDIR_FLAG, sqliteNativeLibDir.absolutePath)
                // dummy call to trigger JDBC initialization so that we can unregister it
                JDBC.isValidURL(CONNECTION_URL)
                // record successful initialization
                System.setProperty(SQLITE_INITIALIZED_FLAG, "true")
            }
        }

        /**
         * Tries to create a verifier but returns null if it cannot find the driver.
         */
        fun create(
            context: Context,
            element: Element,
            entities: List<Entity>,
            views: List<DatabaseView>
        ): DatabaseVerifier? {
            repeat(NATIVE_LIB_RELOAD_RETRY_CNT) {
                try {
                    val connection = JDBC.createConnection(CONNECTION_URL, java.util.Properties())
                    return DatabaseVerifier(connection, context, entities, views)
                } catch (unsatisfied: UnsatisfiedLinkError) {
                    // this is a workaround for an issue w/ sqlite where sometimes it fails to
                    // load the SO. We can manually retry here
                    FileChannel.open(sqliteNativeLibDir.toPath()).use {
                        it.force(true)
                    }
                    val nativeLibs = sqliteNativeLibDir.listFiles { file ->
                        SQLITE_NATIVE_LIB_EXTENSIONS.any { ext ->
                            file.name.endsWith(ext)
                        }
                    }
                    if (nativeLibs.isNotEmpty()) {
                        nativeLibs.forEach {
                            it.setExecutable(true)
                            context.logger.d("reloading the sqlite native file: $it")
                            try {
                                System.load(it.absoluteFile.absolutePath)
                            } catch (unsatisfied: UnsatisfiedLinkError) {
                                // https://issuetracker.google.com/issues/146061836
                                // workaround for b/146061836 where we just copy it again as
                                // another file.
                                copyNativeLibs()
                            }
                        }
                    } else {
                        context.logger.w(Warning.CANNOT_CREATE_VERIFICATION_DATABASE, element,
                            DatabaseVerificationErrors.cannotCreateConnection(unsatisfied))
                        // no reason to retry if file is missing.
                        return null
                    }
                } catch (ex: Exception) {
                    context.logger.w(Warning.CANNOT_CREATE_VERIFICATION_DATABASE, element,
                        DatabaseVerificationErrors.cannotCreateConnection(ex))
                    return null
                } finally {
                    val systemPropertyTempDir = System.getProperty(SQLITE_TEMPDIR_FLAG)
                    if (systemPropertyTempDir != sqliteNativeLibDir.toString()) {
                        throw ConcurrentModificationException("System property " +
                            "org.sqlite.tmpdir changed from $sqliteNativeLibDir to " +
                            "$systemPropertyTempDir inside DatabaseVerifier.create")
                    }
                }
            }
            return null
        }
    }

    init {
        entities.forEach { entity ->
            val stmt = connection.createStatement()
            val createTableQuery = if (entity is FtsEntity &&
                !FtsOptions.defaultTokenizers.contains(entity.ftsOptions.tokenizer)) {
                // Custom FTS tokenizer used, use create statement without custom tokenizer
                // since the DB used for verification probably doesn't have the tokenizer.
                entity.getCreateTableQueryWithoutTokenizer()
            } else {
                entity.createTableQuery
            }
            try {
                stmt.executeUpdate(stripLocalizeCollations(createTableQuery))
            } catch (e: SQLException) {
                context.logger.e(entity.element, "${e.message}")
            }
            entity.indices.forEach {
                stmt.executeUpdate(it.createQuery(entity.tableName))
            }
        }
        views.forEach { view ->
            val stmt = connection.createStatement()
            try {
                stmt.executeUpdate(stripLocalizeCollations(view.createViewQuery))
            } catch (e: SQLException) {
                context.logger.e(view.element, "${e.message}")
            }
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
                // ignore.
                context.logger.d("failed to close the database connection ${t.message}")
            }
        }
    }
}
