/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.room3.migration

import androidx.kruth.assertThat
import androidx.room3.util.ViewInfo
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ViewInfoTest {

    @Test
    fun readSimple() = runTest {
        openDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY, name TEXT)",
                "CREATE VIEW bar AS SELECT id, name FROM foo",
            )
            .use { connection ->
                val info = ViewInfo.read(connection, "bar")
                assertThat(info.name).isEqualTo("bar")
                assertThat(info.sql).isEqualTo("CREATE VIEW bar AS SELECT id, name FROM foo")
            }
    }

    @Test
    fun notExisting() = runTest {
        openDatabase("CREATE TABLE foo (id INTEGER PRIMARY KEY, name TEXT)").use { connection ->
            val info = ViewInfo.read(connection, "bar")
            assertThat(info.name).isEqualTo("bar")
            assertThat(info.sql).isNull()
        }
    }

    @Test
    fun infoEquals() {
        val a = ViewInfo("a", "a")
        val b = ViewInfo("a", null)
        val c = ViewInfo("a", "a")
        assertThat(a).isNotEqualTo(b)
        assertThat(a).isEqualTo(c)
    }

    private fun openDatabase(vararg createQueries: String) =
        AndroidSQLiteDriver().open(":memory:").also { connection ->
            createQueries.forEach { connection.execSQL(it) }
        }
}
