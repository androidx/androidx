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
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import androidx.appfunctions.integration.AppSearchMetadataHelper.isDynamicIndexerAvailable
import androidx.appfunctions.integration.testapp.SecondaryProcessContentProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@LargeTest
class AppFunctionAccessLevelIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager

    @Before
    fun setup() = runBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        appFunctionManager = checkNotNull(AppFunctionManager.getInstance(targetContext))
        targetContext.awaitAppFunctionsIndexed(targetContext.packageName)
    }

    @Test
    fun executeAppFunction_selfAccess_sameApp_sameProcess_success() = runBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        SELF_ACCESS_FUNCTION_ID,
                        AppFunctionData.EMPTY,
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getString(PROPERTY_RETURN_VALUE))
            .isEqualTo("self_success")
    }

    @Test
    fun executeAppFunction_selfAccess_sameApp_differentProcess_success() {
        val uri = Uri.parse("content://${SecondaryProcessContentProvider.AUTHORITY}")
        val resultBundle =
            targetContext.contentResolver.call(
                uri,
                SecondaryProcessContentProvider.METHOD_EXECUTE_APP_FUNCTION,
                SELF_ACCESS_FUNCTION_ID,
                null,
            )

        assertThat(resultBundle).isNotNull()
        assertThat(resultBundle!!.getInt(SecondaryProcessContentProvider.EXTRA_PID))
            .isNotEqualTo(Process.myPid())
        assertThat(resultBundle.getBoolean(SecondaryProcessContentProvider.EXTRA_IS_SUCCESS))
            .isTrue()
        assertThat(resultBundle.getString(SecondaryProcessContentProvider.EXTRA_RETURN_VALUE))
            .isEqualTo("self_success")
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

    private companion object {
        const val SELF_ACCESS_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.BaseTestAppFunctionService#selfAccessFunction"
        const val RETRY_CHECK_INTERVAL_MILLIS: Long = 500
        const val RETRY_MAX_INTERVALS: Long = 10
    }
}
