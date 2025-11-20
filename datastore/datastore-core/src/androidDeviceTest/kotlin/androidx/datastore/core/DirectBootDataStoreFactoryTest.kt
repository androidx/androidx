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

package androidx.datastore.core

import android.os.Build
import androidx.datastore.core.util.requireDeviceProtectedStorageContext
import androidx.kruth.assertThat
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

// DeviceProtectedStorageContext requires API 24.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
@OptIn(ExperimentalCoroutinesApi::class)
class DirectBootDataStoreFactoryTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val timeout = Timeout(10, TimeUnit.SECONDS)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestScope

    @BeforeTest
    fun setUp() {
        testFile = tmp.newFile()
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    fun testRequireDeviceProtectedStorageContext() = runTest {
        assertThat(context.isDeviceProtectedStorage).isFalse()
        assertThat(context.requireDeviceProtectedStorageContext().isDeviceProtectedStorage).isTrue()
    }

    @Test
    fun testDeviceEncryptedToUserEncrypted() = runTest {
        assertThat(context.isDeviceProtectedStorage).isFalse()
        val deContext = context.createDeviceProtectedStorageContext()
        assertThat(deContext.isDeviceProtectedStorage).isTrue()
        // This test should pass if app context derived from a DeviceProtectedStorageContext IS NOT
        // also device protected
        assertThat(deContext.applicationContext.isDeviceProtectedStorage).isFalse()
    }

    @Test
    fun testNewInstance() = runTest {
        val deContext = context.createDeviceProtectedStorageContext()
        assertThat(deContext.isDeviceProtectedStorage).isTrue()
        val store =
            DataStoreFactory.createInDeviceProtectedStorage(
                context = deContext,
                serializer = TestingSerializer(),
                scope = dataStoreScope,
                fileName = "testFile",
            )
        val expectedByte = 123.toByte()

        assertThat(store.updateData { expectedByte }).isEqualTo(expectedByte)
        assertThat(store.data.first()).isEqualTo(expectedByte)
    }

    @Test
    fun testCreateDeviceProtectedDataStoreFile() {
        assertThat(context.isDeviceProtectedStorage).isFalse()
        val fileFromUeContext = context.deviceProtectedDataStoreFile("testFile")
        val fileFromDeContext =
            context.createDeviceProtectedStorageContext().deviceProtectedDataStoreFile("testFile")

        assertThat(fileFromUeContext.path)
            .startsWith(context.createDeviceProtectedStorageContext().filesDir.absolutePath)
        assertThat(fileFromDeContext.path)
            .isEqualTo(
                context
                    .createDeviceProtectedStorageContext()
                    .deviceProtectedDataStoreFile("testFile")
                    .path
            )
    }

    @Test
    fun testPhoneUnlockedReturnsOriginalException() {
        // Get a path to a datastore file in CE storage
        assertThat(context.isDeviceProtectedStorage).isFalse()
        val filePath = File(context.filesDir, "testFile")
        val noOpException = FileNotFoundException()

        if (noOpException.isDeviceUnlocked()) {
            // If the phone is UNLOCKED and the path is in CE storage, the exception should
            // be returned as is.
            assertThat(wrapExceptionIfDueToDirectBoot(filePath.parent, noOpException))
                .isSameInstanceAs(noOpException)
        }
    }
}
