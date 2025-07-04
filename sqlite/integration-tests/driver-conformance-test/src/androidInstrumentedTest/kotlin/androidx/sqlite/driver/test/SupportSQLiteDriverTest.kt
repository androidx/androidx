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

package androidx.sqlite.driver.test

import androidx.kruth.assertThrows
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.driver.SupportSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Ignore
import kotlin.test.Test

class SupportSQLiteDriverTest : BaseConformanceTest() {

    private val context = InstrumentationRegistry.getInstrumentation().context

    override val driverType = TestDriverType.ANDROID_FRAMEWORK

    override fun getDriver(): SQLiteDriver {
        return SupportSQLiteDriver(
            FrameworkSQLiteOpenHelperFactory().create(getDefaultConfigBuilder().name(null).build())
        )
    }

    @Ignore // TODO(b/304297717): Align exception checking test with native.
    override fun bindInvalidParam() {}

    @Test
    fun openHelperInMemoryNameMatch() {
        val configBuilder = getDefaultConfigBuilder()
        val inMemoryDriver =
            SupportSQLiteDriver(
                FrameworkSQLiteOpenHelperFactory().create(configBuilder.name(null).build())
            )
        assertThrows<IllegalArgumentException> { inMemoryDriver.open("file-name") }
            .hasMessageThat()
            .isEqualTo(
                "This driver is configured to open an in-memory database but a " +
                    "file-based named 'file-name' was requested."
            )

        inMemoryDriver.open(":memory:").close()
    }

    @Test
    fun openHelperFileNameMatch() {
        val configBuilder = getDefaultConfigBuilder()
        val path = context.getDatabasePath("file_database.db").path
        val fileDriver =
            SupportSQLiteDriver(
                FrameworkSQLiteOpenHelperFactory().create(configBuilder.name(path).build())
            )

        assertThrows<IllegalArgumentException> { fileDriver.open(":memory:") }
            .hasMessageThat()
            .isEqualTo(
                "This driver is configured to open a database named " +
                    "'$path' but ':memory:' was requested."
            )

        assertThrows<IllegalArgumentException> { fileDriver.open("/bad/path") }
            .hasMessageThat()
            .isEqualTo(
                "This driver is configured to open a database named " +
                    "'$path' but '/bad/path' was requested."
            )

        fileDriver.open(path).close()
    }

    @Test
    fun openHelperShortFileNameMatch() {
        val configBuilder = getDefaultConfigBuilder()
        val path = context.getDatabasePath("file_database.db").path
        val fileDriver =
            SupportSQLiteDriver(
                FrameworkSQLiteOpenHelperFactory()
                    .create(configBuilder.name("file_database.db").build())
            )

        fileDriver.open(path).close()
    }

    @Test
    fun openHelperShortFileNameMatch_reverse() {
        val configBuilder = getDefaultConfigBuilder()
        val path = context.getDatabasePath("file_database.db").path
        val fileDriver =
            SupportSQLiteDriver(
                FrameworkSQLiteOpenHelperFactory().create(configBuilder.name(path).build())
            )

        fileDriver.open("file_database.db").close()
    }

    private fun getDefaultConfigBuilder() =
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {}
                }
            )
}
