/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException

val Context.globalDs by dataStore("file1", TestingSerializer())

val Context.corruptedDs by dataStore(
    fileName = "file2",
    corruptionHandler = ReplaceFileCorruptionHandler { 123 },
    serializer = TestingSerializer(failReadWithCorruptionException = true),
)

val Context.dsWithMigrationTo123 by dataStore(
    fileName = "file4",
    serializer = TestingSerializer(),
    produceMigrations = {
        listOf(
            object : DataMigration<Byte> {
                override suspend fun shouldMigrate(currentData: Byte) = true
                override suspend fun migrate(currentData: Byte): Byte =
                    currentData.plus(123).toByte()

                override suspend fun cleanUp() {}
            }
        )
    }
)

@ExperimentalCoroutinesApi
class DataStoreDelegateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "/datastore").deleteRecursively()
    }

    @Test
    fun testBasic() = runBlocking<Unit> {
        assertThat(context.globalDs.updateData { 1 }).isEqualTo(1)
        context.globalDs.data.first()
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testBasicWithDifferentContext() = runBlocking<Unit> {
        context.createDeviceProtectedStorageContext().globalDs.updateData { 123 }
        assertThat(context.globalDs.data.first()).isEqualTo(123)
    }

    @Test
    fun testCorruptedDs_runsCorruptionHandler() = runBlocking<Unit> {
        // File needs to exist or we don't actually hit the serializer:
        context.dataStoreFile("file2").let { file ->
            file.parentFile!!.mkdirs()
            FileOutputStream(file).use {
                it.write(0)
            }
        }

        assertThat(context.corruptedDs.data.first()).isEqualTo(123)
    }

    @Test
    fun testDsWithMigrationRunsMigration() = runBlocking<Unit> {
        assertThat(context.dsWithMigrationTo123.data.first()).isEqualTo(123)
    }

    @Test
    fun testCreateWithContextAndName() = runTest {
        coroutineScope {
            with(GlobalDataStoreTestHelper("file_name1", this@coroutineScope)) {
                context.ds.updateData { 123 }
            }
        }

        coroutineScope {
            with(GlobalDataStoreTestHelper("file_name1", this@coroutineScope)) {
                assertThat(context.ds.data.first()).isEqualTo(123)
            }
        }
    }

    @Test
    fun testCreateSameTwiceThrowsException() = runTest {
        val helper1 = GlobalDataStoreTestHelper("file_name2", this)
        val helper2 = GlobalDataStoreTestHelper("file_name2", this)

        with(helper1) {
            context.ds.data.first()
        }

        with(helper2) {
            assertThrows<IllegalStateException> {
                context.ds.data.first()
            }
        }
    }

    internal class GlobalDataStoreTestHelper(fileName: String, scope: CoroutineScope) {
        val Context.ds by dataStore(
            fileName = fileName,
            serializer = TestingSerializer(),
            scope = scope
        )
    }
}