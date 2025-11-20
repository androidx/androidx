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
import androidx.datastore.core.deviceProtectedDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@ExperimentalCoroutinesApi
// A call to deviceProtectedDataStore requires a min SKD level of 24
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
class DeviceProtectedDataStoreDelegateTest {
    private val deviceEncryptedFileName = "deviceStorage"
    val Context.deviceEncryptedDs by
        deviceProtectedDataStore(deviceEncryptedFileName, TestingSerializer())

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "/datastore").deleteRecursively()
    }

    @Test
    fun testInitDeviceEncryptedDelegateWithUserEncryptedContext() =
        runBlocking<Unit> {
            // Initialize a datastore via "by dataStore" delegate, with a regular
            // Context.
            val userEncryptedContext = context
            userEncryptedContext.deviceEncryptedDs.updateData { 123 }

            // Under the hood, the Context is expected to be switched to a
            // DeviceProtectedStorageContext, and the dataStore file should be in the device
            // encrypted storage directory.
            val deviceEncryptedContext = userEncryptedContext.createDeviceProtectedStorageContext()

            val userEncryptedStorageDirectory: List<File> =
                userEncryptedContext.filesDir.resolve("datastore").listFiles()?.toList()
                    ?: emptyList()
            val deviceEncryptedStorageDirectory =
                deviceEncryptedContext.filesDir.resolve("datastore").listFiles()?.toList()
                    ?: emptyList()

            // Assert that the datastore was created in the DE storage.
            assertThat(userEncryptedStorageDirectory).isEmpty()
            assertThat(deviceEncryptedStorageDirectory)
                .contains(
                    userEncryptedContext.deviceProtectedDataStoreFile(deviceEncryptedFileName)
                )
        }
}
