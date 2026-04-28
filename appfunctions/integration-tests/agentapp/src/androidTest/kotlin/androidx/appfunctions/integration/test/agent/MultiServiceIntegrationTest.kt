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

package androidx.appfunctions.integration.test.agent

import android.Manifest
import android.content.Context
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = 37)
@LargeTest
class MultiServiceIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionCaller: AppFunctionCaller
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    @Before
    fun setup() = doBlocking {
        uiAutomation.grantAppFunctionAccess(targetContext, TARGET_APP_PACKAGE)

        appFunctionCaller = AppFunctionCaller(targetContext)

        uiAutomation.apply {
            adoptShellPermissionIdentity(
                Manifest.permission.INSTALL_PACKAGES,
                Manifest.permission.EXECUTE_APP_FUNCTIONS,
            )
        }
        InstallHelper.install(targetAppApkFile)
        targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)
    }

    @After
    fun tearDown() {
        uiAutomation.revokeAppFunctionAccess()
        InstallHelper.uninstall(TARGET_APP_PACKAGE)
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun searchAndInvoke_success() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#add"
            }

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setInt("a", 1)
                            .setInt("b", 2)
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getInt(PROPERTY_RETURN_VALUE)).isEqualTo(3)
    }

    @Test
    fun searchAndInvokeCustomServiceFunction_success() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id == "androidx.appfunctions.integration.testapp.CustomAppFunctionService#add"
            }
        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setInt("a", 1)
                            .setInt("b", 2)
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getInt(PROPERTY_RETURN_VALUE)).isEqualTo(3)
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

    companion object {
        const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"
    }
}
