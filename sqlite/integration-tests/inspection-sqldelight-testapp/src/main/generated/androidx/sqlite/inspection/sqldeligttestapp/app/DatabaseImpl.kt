/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection.sqldeligttestapp.app

import androidx.sqlite.inspection.test.TestEntity
import androidx.sqlite.inspection.test.TestEntityQueries
import androidx.sqlite.inspection.sqldeligttestapp.Database
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList
import kotlin.reflect.KClass

internal val KClass<Database>.schema: SqlDriver.Schema
    get() = DatabaseImpl.Schema

internal fun KClass<Database>.newInstance(driver: SqlDriver): Database =
    DatabaseImpl(driver)

private class DatabaseImpl(
    driver: SqlDriver
) : TransacterImpl(driver),
    Database {
    override val testEntityQueries: TestEntityQueriesImpl =
        TestEntityQueriesImpl(
            this,
            driver
        )

    object Schema : SqlDriver.Schema {
        override val version: Int
            get() = 1

        override fun create(driver: SqlDriver) {
            driver.execute(
                null, """
          |CREATE TABLE TestEntity(
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    value TEXT NOT NULL
          |)
          """.trimMargin(), 0
            )
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) {
        }
    }
}

private class TestEntityQueriesImpl(
    private val database: DatabaseImpl,
    private val driver: SqlDriver
) : TransacterImpl(driver), TestEntityQueries {
    internal val selectAll: MutableList<Query<*>> = copyOnWriteList()

    override fun <T : Any> selectAll(mapper: (id: Long, value: String) -> T): Query<T> =
        Query(
            186750743,
            selectAll,
            driver,
            "TestEntity.sq",
            "selectAll",
            "SELECT * FROM TestEntity"
        ) { cursor ->
            mapper(
                cursor.getLong(0)!!,
                cursor.getString(1)!!
            )
        }

    override fun selectAll(): Query<TestEntity> = selectAll(TestEntity::Impl)

    override fun insertOrReplace(value: String) {
        driver.execute(
            -2020431062, """
    |INSERT OR REPLACE INTO TestEntity(
    |  value
    |)
    |VALUES (?1)
    """.trimMargin(), 1
        ) {
            bindString(1, value)
        }
        notifyQueries(-2020431062, { database.testEntityQueries.selectAll })
    }
}
