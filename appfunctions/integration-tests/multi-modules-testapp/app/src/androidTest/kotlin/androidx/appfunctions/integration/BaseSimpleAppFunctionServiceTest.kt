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

package androidx.appfunctions.integration

import android.content.Context
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.integration.testapp.SimpleAppFunctionService
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

// Only run this on SDK 37 where multiservice is supported.
@SdkSuppress(minSdkVersion = 37)
class BaseSimpleAppFunctionServiceTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager

    @Before
    fun setup() = doBlocking {
        appFunctionManager = checkNotNull(AppFunctionManager.getInstance(targetContext))
        targetContext.awaitAppFunctionsIndexed(targetContext.packageName)
    }

    @Test
    fun generatedIdShouldMatchDatabaseId() = doBlocking {
        val generatedId = SimpleAppFunctionService.FUNCTION_ID_CREATE_NOTE

        val appFunctionIds =
            appFunctionManager
                .observeAppFunctions(
                    AppFunctionSearchSpec(packageNames = setOf(targetContext.packageName))
                )
                .firstOrNull()
                ?.single { it.packageName == targetContext.packageName }
                ?.appFunctions
                ?.map { it.id }

        assertThat(appFunctionIds).isNotNull()
        assertThat(appFunctionIds).contains(generatedId)
    }

    private suspend fun Context.awaitAppFunctionsIndexed(targetPackage: String) {
        retryAssert {
            val functionIds =
                AppSearchMetadataHelper.collectFunctionIds(
                    this@awaitAppFunctionsIndexed,
                    targetPackage,
                )
            assertThat(functionIds).isNotEmpty()
        }
    }

    private suspend fun retryAssert(runnable: suspend () -> Unit) {
        var lastError: Throwable? = null

        for (attempt in 0 until RETRY_MAX_INTERVALS) {
            try {
                runnable()
                return
            } catch (e: Throwable) {
                lastError = e
                delay(RETRY_CHECK_INTERVAL_MILLIS)
            }
        }
        throw lastError!!
    }

    private fun doBlocking(block: suspend CoroutineScope.() -> Unit) = runBlocking(block = block)

    private companion object {
        const val RETRY_CHECK_INTERVAL_MILLIS: Long = 500
        const val RETRY_MAX_INTERVALS: Long = 10
    }
}
