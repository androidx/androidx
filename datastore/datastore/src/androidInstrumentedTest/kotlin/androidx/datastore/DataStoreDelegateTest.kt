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
import android.os.Build
import androidx.datastore.core.DataMigration
import androidx.datastore.core.deviceProtectedDataStoreFile
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

val Context.globalDs by dataStore("file1", TestingSerializer())

val Context.corruptedDs by
    dataStore(
        fileName = "file2",
        corruptionHandler = ReplaceFileCorruptionHandler { 123 },
        serializer = TestingSerializer(failReadWithCorruptionException = true),
    )

val Context.dsWithMigrationTo123 by
    dataStore(
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
        },
    )

const val USER_ENCRYPTED_FILE_NAME = "userStorage"

val Context.userEncryptedDs by dataStore(USER_ENCRYPTED_FILE_NAME, TestingSerializer())

@ExperimentalCoroutinesApi
class DataStoreDelegateTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "/datastore").deleteRecursively()
    }

    @Test
    fun testBasic() =
        runBlocking<Unit> {
            assertThat(context.globalDs.updateData { 1 }).isEqualTo(1)
            context.globalDs.data.first()
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun testInitUserEncryptedDelegateWithDeviceEncryptedContext() =
        runBlocking<Unit> {
            // Initialize a datastore via "by dataStore" delegate, with a
            // DeviceProtectedStorageContext.
            val deviceEncryptedContext = context.createDeviceProtectedStorageContext()
            deviceEncryptedContext.userEncryptedDs.updateData { 123 }

            // Under the hood, the DeviceProtectedStorageContext is expected to be switched to a
            // user encrypted Context, and the dataStore file should be in the user encrypted
            // storage directory.
            val userEncryptedContext = deviceEncryptedContext.applicationContext

            val userEncryptedStorageDirectory: List<File> =
                userEncryptedContext.filesDir.resolve("datastore").listFiles()?.toList()
                    ?: emptyList()
            val deviceEncryptedStorageDirectory =
                deviceEncryptedContext.filesDir.resolve("datastore").listFiles()?.toList()
                    ?: emptyList()

            // Assert that the datastore was not created in the DE storage.
            assertThat(deviceEncryptedStorageDirectory)
                .doesNotContain(
                    deviceEncryptedContext.deviceProtectedDataStoreFile(USER_ENCRYPTED_FILE_NAME)
                )
            // Assert that the datastore was created in the UE storage.
            assertThat(userEncryptedStorageDirectory)
                .contains(userEncryptedContext.dataStoreFile(USER_ENCRYPTED_FILE_NAME))
        }

    @Test
    fun testCorruptedDs_runsCorruptionHandler() =
        runBlocking<Unit> {
            // File needs to exist or we don't actually hit the serializer:
            context.dataStoreFile("file2").let { file ->
                file.parentFile!!.mkdirs()
                FileOutputStream(file).use { it.write(0) }
            }

            assertThat(context.corruptedDs.data.first()).isEqualTo(123)
        }

    @Test
    fun testDsWithMigrationRunsMigration() =
        runBlocking<Unit> { assertThat(context.dsWithMigrationTo123.data.first()).isEqualTo(123) }

    @Test
    fun testCreateWithContextAndName() {
        runTest {
            with(GlobalDataStoreTestHelper("file_name1", backgroundScope)) {
                context.ds.updateData { 123 }
            }
        }

        runTest {
            with(GlobalDataStoreTestHelper("file_name1", backgroundScope)) {
                assertThat(context.ds.data.first()).isEqualTo(123)
            }
        }
    }

    @Test
    fun testCreateSameTwiceThrowsException() = runTest {
        val helper1 = GlobalDataStoreTestHelper("file_name2", backgroundScope)
        val helper2 = GlobalDataStoreTestHelper("file_name2", backgroundScope)

        with(helper1) { context.ds.data.first() }

        with(helper2) { assertThrows<IllegalStateException> { context.ds.data.first() } }
    }

    internal class GlobalDataStoreTestHelper(fileName: String, scope: CoroutineScope) {
        val Context.ds by
            dataStore(fileName = fileName, serializer = TestingSerializer(), scope = scope)
    }
}
