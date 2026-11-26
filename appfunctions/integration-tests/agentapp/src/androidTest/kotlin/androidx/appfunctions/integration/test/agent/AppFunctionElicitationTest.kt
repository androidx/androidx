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
import android.os.Build
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionElicitationRequest
import androidx.appfunctions.AppFunctionElicitationResponse
import androidx.appfunctions.AppFunctionInputRequiredException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import androidx.appfunctions.integration.test.agent.AppSearchMetadataHelper.isDynamicIndexerAvailable
import androidx.appfunctions.integration.test.agent.TestUtil.awaitAppFunctionsIndexed
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppInteractionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppInteractionAccess
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@LargeTest
class AppFunctionElicitationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    @Before
    fun setup() = doBlocking {
        uiAutomation.grantAppFunctionAccess(targetContext, TARGET_APP_PACKAGE)

        appFunctionManager = checkNotNull(AppFunctionManager.getInstance(targetContext))

        uiAutomation.apply {
            adoptShellPermissionIdentity(
                Manifest.permission.INSTALL_PACKAGES,
                Manifest.permission.EXECUTE_APP_FUNCTIONS,
            )
        }
        InstallHelper.install(targetAppApkFile)
        uiAutomation.grantAppInteractionAccess(targetContext, TARGET_APP_PACKAGE)
        targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)
    }

    @After
    fun tearDown() {
        uiAutomation.revokeAppInteractionAccess(targetContext, TARGET_APP_PACKAGE)
        uiAutomation.revokeAppFunctionAccess()
        InstallHelper.uninstall(TARGET_APP_PACKAGE)
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun executeAppFunction_formElicitationRoundTrip() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.BaseTestAppFunctionService#requestFormElicitation"
            )
        val params =
            AppFunctionData.Builder(metadata.parameters, metadata.packageMetadata.components)
                .build()

        val initialResponse =
            appFunctionManager.executeAppFunction(
                request = ExecuteAppFunctionRequest(metadata.packageName, metadata.id, params)
            )

        assertThat(initialResponse).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val errorResponse = initialResponse as ExecuteAppFunctionResponse.Error
        assertThat(errorResponse.error).isInstanceOf(AppFunctionInputRequiredException::class.java)
        val exception = errorResponse.error as AppFunctionInputRequiredException
        assertThat(exception.errorMessage).isEqualTo("Form input required")
        val expectedForm =
            AppFunctionElicitationRequest.Form(
                instruction = "Which note should be deleted?",
                responseDataTypeMetadata =
                    AppFunctionStringTypeMetadata(
                        isNullable = false,
                        enumValues = setOf("Groceries", "Packing List"),
                    ),
            )
        assertThat(exception.inputRequests).containsExactly("form", expectedForm)

        val formRequest = exception.inputRequests["form"] as AppFunctionElicitationRequest.Form
        val responseData =
            AppFunctionData.Builder(formRequest)
                .setString(
                    AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE,
                    "Groceries",
                )
                .build()
        val retryResponse =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest.Builder(metadata.packageName, metadata.id, params)
                        .setInputResponses(
                            mapOf("form" to AppFunctionElicitationResponse.FormData(responseData))
                        )
                        .build()
            )

        assertThat(retryResponse).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = retryResponse as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getString(PROPERTY_RETURN_VALUE))
            .isEqualTo("Deleted: Groceries")
    }

    @Test
    fun executeAppFunction_pendingActionElicitationRoundTrip() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.BaseTestAppFunctionService#requestPendingActionElicitation"
            )
        val params =
            AppFunctionData.Builder(metadata.parameters, metadata.packageMetadata.components)
                .build()

        val initialResponse =
            appFunctionManager.executeAppFunction(
                request = ExecuteAppFunctionRequest(metadata.packageName, metadata.id, params)
            )

        assertThat(initialResponse).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val errorResponse = initialResponse as ExecuteAppFunctionResponse.Error
        assertThat(errorResponse.error).isInstanceOf(AppFunctionInputRequiredException::class.java)
        val exception = errorResponse.error as AppFunctionInputRequiredException
        assertThat(exception.errorMessage).isEqualTo("Pending action required")
        assertThat(exception.inputRequests).hasSize(1)

        val pendingActionRequest = exception.inputRequests["pending_action"]
        assertThat(pendingActionRequest)
            .isInstanceOf(AppFunctionElicitationRequest.PendingAction::class.java)
        pendingActionRequest as AppFunctionElicitationRequest.PendingAction
        assertThat(pendingActionRequest.displayMessage).isEqualTo("Open test app to confirm")
        assertThat(pendingActionRequest.pendingIntent.creatorPackage)
            .isEqualTo(metadata.packageName)
        assertThat(pendingActionRequest.pendingIntent.isActivity).isTrue()

        val retryResponse =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest.Builder(metadata.packageName, metadata.id, params)
                        .setInputResponses(
                            mapOf(
                                "pending_action" to AppFunctionElicitationResponse.ActionFinished()
                            )
                        )
                        .build()
            )

        assertThat(retryResponse).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = retryResponse as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getString(PROPERTY_RETURN_VALUE))
            .isEqualTo("Action finished")
    }

    private suspend fun searchAppFunction(id: String): AppFunctionMetadata {
        return appFunctionManager
            .searchAppFunctions(
                AppFunctionSearchSpec(
                    functionNames =
                        setOf(
                            AppFunctionName(
                                packageName = TARGET_APP_PACKAGE,
                                functionIdentifier = id,
                            )
                        )
                )
            )
            .single()
    }

    private companion object {
        const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"
    }
}
