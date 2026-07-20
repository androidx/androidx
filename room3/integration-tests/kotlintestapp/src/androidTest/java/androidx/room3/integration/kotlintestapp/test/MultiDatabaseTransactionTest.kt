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

package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.integration.kotlintestapp.vo.Counter
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MultiDatabaseTransactionTest(private val useDriver: UseDriver) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db1: TestDatabase
    private lateinit var db2: TestDatabase

    companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Before
    fun setUp() {
        context.deleteDatabase("db1.db")
        context.deleteDatabase("db2.db")
        val driver =
            when (useDriver) {
                UseDriver.ANDROID -> AndroidSQLiteDriver()
                UseDriver.BUNDLED -> BundledSQLiteDriver()
            }
        db1 = Room.databaseBuilder<TestDatabase>(context, "db1.db").setDriver(driver).build()
        db2 = Room.databaseBuilder<TestDatabase>(context, "db2.db").setDriver(driver).build()
    }

    @After
    fun tearDown() {
        db1.close()
        db2.close()
    }

    @Test
    fun testAccessSecondDatabaseInTransaction() = runBlocking {
        db1.counterDao().upsert(Counter(1L, 10))
        db2.counterDao().upsert(Counter(2L, 20))

        assertThat(db1.counterDao().getCounter(1L).value).isEqualTo(10)
        assertThat(db2.counterDao().getCounter(2L).value).isEqualTo(20)

        db1.withWriteTransaction {
            assertThat(db1.counterDao().getCounter(1L).value).isEqualTo(10)
            assertThat(db2.counterDao().getCounter(2L).value).isEqualTo(20)
        }
    }
}
