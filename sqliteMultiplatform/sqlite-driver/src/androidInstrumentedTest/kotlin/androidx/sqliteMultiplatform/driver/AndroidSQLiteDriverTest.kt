/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqliteMultiplatform.driver

import android.content.Context
import androidx.kruth.assertThat
import androidx.sqliteMultiplatform.BaseConformanceTest
import androidx.sqliteMultiplatform.SQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.BeforeTest

class AndroidSQLiteDriverTest : BaseConformanceTest() {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @BeforeTest
    fun before() {
        context.deleteDatabase("test.db")
    }

    override fun getDriver(): SQLiteDriver {
        val file = context.getDatabasePath("test.db")
            .also { it.parentFile?.mkdirs() }
        assertThat(file.exists()).isFalse()
        return AndroidSQLiteDriver(file.absolutePath)
    }
}
