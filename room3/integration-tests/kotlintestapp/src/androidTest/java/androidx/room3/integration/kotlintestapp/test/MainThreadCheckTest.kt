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

package androidx.room3.integration.kotlintestapp.test

import android.os.Looper
import androidx.kruth.assertThrows
import androidx.kruth.assertWithMessage
import androidx.test.filters.SmallTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class MainThreadCheckTest(driver: UseDriver) : TestDatabaseTest(useDriver = driver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    @Test
    fun suspendQuery() = runTest {
        withContext(Dispatchers.Main) {
            assertWithMessage("Test coroutine should be on the main thread")
                .that(isMainThread)
                .isTrue()
            booksDao.getBooksSuspend()
        }
    }

    @Test
    fun blockingQuery() = runTest {
        withContext(Dispatchers.Main) {
            assertWithMessage("Test coroutine should be on the main thread")
                .that(isMainThread)
                .isTrue()
            assertThrows<IllegalStateException> { booksDao.getAllBooks() }
                .hasMessageThat()
                .isEqualTo(
                    "Cannot access database on the main thread since" +
                        " it may potentially lock the UI for a long period of time."
                )
        }
    }
}
