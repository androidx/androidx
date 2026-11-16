/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3

import androidx.kruth.assertThat
import androidx.room3.util.SQLiteVersion
import androidx.room3.util.getSQLiteVersion
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class EntityUpsertAdapterTest {

    private data class Item(val id: Long, val name: String)

    private data class PkOnly(val id: Long)

    private val rawConnection = BundledSQLiteDriver().open(":memory:")

    private val itemInsertAdapter =
        object : EntityInsertAdapter<Item>() {
            override fun createQuery(): String = "INSERT INTO `Item` (`id`,`name`) VALUES (?,?)"

            override fun bind(statement: SQLiteStatement, entity: Item) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.name)
            }
        }

    private val itemUpdateAdapter =
        object : EntityDeleteOrUpdateAdapter<Item>() {
            override fun createQuery(): String =
                "UPDATE `Item` SET `id` = ?,`name` = ? WHERE `id` = ?"

            override fun bind(statement: SQLiteStatement, entity: Item) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.name)
                statement.bindLong(3, entity.id)
            }
        }

    private val pkOnlyInsertAdapter =
        object : EntityInsertAdapter<PkOnly>() {
            override fun createQuery(): String = "INSERT INTO `PkOnly` (`id`) VALUES (?)"

            override fun bind(statement: SQLiteStatement, entity: PkOnly) {
                statement.bindLong(1, entity.id)
            }
        }

    private val pkOnlyUpdateAdapter =
        object : EntityDeleteOrUpdateAdapter<PkOnly>() {
            override fun createQuery(): String = "UPDATE `PkOnly` SET `id` = ? WHERE `id` = ?"

            override fun bind(statement: SQLiteStatement, entity: PkOnly) {
                statement.bindLong(1, entity.id)
                statement.bindLong(2, entity.id)
            }
        }

    private val itemUpsertQuery =
        "INSERT INTO `Item` (`id`,`name`) VALUES (?,?) ON CONFLICT (`id`) DO UPDATE SET `name` = excluded.`name`"

    @BeforeTest
    fun setUp() {
        rawConnection.execSQL(
            "CREATE TABLE `Item` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        rawConnection.execSQL("CREATE TABLE `PkOnly` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }

    @AfterTest
    fun tearDown() {
        rawConnection.close()
    }

    @Test
    fun getSQLiteVersionParsesAndCompares() = runTest {
        assertThat(getSQLiteVersion(RecordingConnection(rawConnection, "3.24.1-alpha1")))
            .isEqualTo(SQLiteVersion(3, 24, 1))
        assertThat(SQLiteVersion(3, 24, 0) >= SQLiteVersion(3, 24, 0)).isTrue()
        assertThat(SQLiteVersion(3, 23, 9) >= SQLiteVersion(3, 24, 0)).isFalse()
        assertThat(SQLiteVersion(4, 0, 0) >= SQLiteVersion(3, 51, 0)).isTrue()
    }

    @Test
    fun nativeUpsertUsesOnConflictDoUpdateAndCachesVersionCheck() = runTest {
        val recordingConn = RecordingConnection(rawConnection, versionOverride = "3.24.0")
        val adapter = EntityUpsertAdapter(itemInsertAdapter, itemUpdateAdapter, itemUpsertQuery)

        adapter.upsert(recordingConn, null as Item?)
        assertThat(recordingConn.preparedSqls).isEmpty()

        adapter.upsert(recordingConn, Item(1L, "first"))
        adapter.upsert(recordingConn, listOf(Item(1L, "updated"), Item(2L, "second")))
        adapter.upsert(recordingConn, arrayOf(Item(2L, "secondUpdated")))

        assertThat(recordingConn.preparedSqls.count { it == "SELECT sqlite_version()" })
            .isEqualTo(1)
        assertThat(recordingConn.preparedSqls).contains(itemUpsertQuery)
        assertThat(recordingConn.preparedSqls.none { it.startsWith("UPDATE ") }).isTrue()
        assertThat(readAllItems()).isEqualTo(listOf(Item(1L, "updated"), Item(2L, "secondUpdated")))
    }

    @Test
    fun legacyFallbackWhenSqliteVersionBelow324() = runTest {
        val recordingConn = RecordingConnection(rawConnection, versionOverride = "3.22.0")
        val adapter = EntityUpsertAdapter(itemInsertAdapter, itemUpdateAdapter, itemUpsertQuery)

        adapter.upsert(recordingConn, Item(1L, "first"))
        adapter.upsert(recordingConn, Item(1L, "updated"))

        assertThat(recordingConn.preparedSqls.count { it == "SELECT sqlite_version()" })
            .isEqualTo(1)
        assertThat(recordingConn.preparedSqls.none { it.contains("ON CONFLICT") }).isTrue()
        assertThat(recordingConn.preparedSqls.any { it.startsWith("UPDATE ") }).isTrue()
        assertThat(readAllItems()).isEqualTo(listOf(Item(1L, "updated")))
    }

    @Test
    fun legacyTwoArgConstructorSkipsVersionCheck() = runTest {
        val recordingConn = RecordingConnection(rawConnection, versionOverride = null)
        val adapter = EntityUpsertAdapter(itemInsertAdapter, itemUpdateAdapter)

        adapter.upsert(recordingConn, Item(1L, "first"))
        adapter.upsert(recordingConn, Item(1L, "updated"))

        assertThat(recordingConn.preparedSqls.none { it == "SELECT sqlite_version()" }).isTrue()
        assertThat(recordingConn.preparedSqls.none { it.contains("ON CONFLICT") }).isTrue()
        assertThat(readAllItems()).isEqualTo(listOf(Item(1L, "updated")))
    }

    @Test
    fun pkOnlyEntityUsesDoNothingWithoutUpdate() = runTest {
        val recordingConn = RecordingConnection(rawConnection, versionOverride = null)
        val doNothingQuery = "INSERT INTO `PkOnly` (`id`) VALUES (?) ON CONFLICT (`id`) DO NOTHING"
        val adapter = EntityUpsertAdapter(pkOnlyInsertAdapter, pkOnlyUpdateAdapter, doNothingQuery)

        adapter.upsert(recordingConn, PkOnly(10L))
        adapter.upsert(recordingConn, PkOnly(10L))

        assertThat(recordingConn.preparedSqls).contains(doNothingQuery)
        assertThat(recordingConn.preparedSqls.none { it.startsWith("UPDATE ") }).isTrue()
    }

    private fun readAllItems(): List<Item> = buildList {
        rawConnection.prepare("SELECT `id`, `name` FROM `Item` ORDER BY `id`").use { stmt ->
            while (stmt.step()) {
                add(Item(stmt.getLong(0), stmt.getText(1)))
            }
        }
    }

    private class RecordingConnection(
        private val delegate: SQLiteConnection,
        private val versionOverride: String?,
    ) : SQLiteConnection by delegate {
        val preparedSqls = mutableListOf<String>()

        override fun prepare(sql: String): SQLiteStatement {
            preparedSqls.add(sql)
            if (versionOverride != null && sql == "SELECT sqlite_version()") {
                return delegate.prepare("SELECT '$versionOverride'")
            }
            return delegate.prepare(sql)
        }
    }
}
