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
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.ADD_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.CREATE_NOTE_DISABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.CREATE_NOTE_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.DEPRECATED_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.DISABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.ENABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.SENTINEL_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.TARGET_APP_PACKAGE
import androidx.appfunctions.integration.test.agent.AppSearchMetadataHelper.isDynamicIndexerAvailable
import androidx.appfunctions.integration.test.agent.TestUtil.awaitAppFunctionsIndexed
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.setAppFunctionStateRemoteAsync
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Integration tests for searchAppFunctions API. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@LargeTest
class SearchAppFunctionsIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    private val functionsUnderTest =
        setOf(
            CREATE_NOTE_FUNCTION_ID,
            CREATE_NOTE_DISABLED_BY_DEFAULT_FUNCTION_ID,
            ADD_FUNCTION_ID,
            DEPRECATED_FUNCTION_ID,
            SENTINEL_FUNCTION_ID,
            DISABLED_BY_DEFAULT_FUNCTION_ID,
            ENABLED_BY_DEFAULT_FUNCTION_ID,
        )

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
        targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)

        for (functionId in functionsUnderTest) {
            setAppFunctionStateRemoteAsync(
                AppFunctionName(TARGET_APP_PACKAGE, functionId),
                AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
            )
        }
    }

    @After
    fun tearDown() {
        uiAutomation.revokeAppFunctionAccess()
        InstallHelper.uninstall(TARGET_APP_PACKAGE)
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun searchAppFunctions_returnsAllAppFunction_withDynamicIndexer() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        assertThat(appFunctions.size).isEqualTo(getTotalFunctionCountInPackage())
    }

    @Test
    fun searchAppFunctions_returnsCorrectMetadata_withDynamicIndexer() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        // Validate schemaless AppFunctionMetadata.
        val addMetadata = appFunctions.single { it.id == ADD_FUNCTION_ID }
        assertThat(addMetadata).isEqualTo(AppFunctionMetadataHelper.FunctionMetadata.ADD)

        // Validate schema AppFunctionMetadata.
        val createNoteMetadata = appFunctions.single { it.id == CREATE_NOTE_FUNCTION_ID }
        assertThat(createNoteMetadata)
            .isEqualTo(AppFunctionMetadataHelper.FunctionMetadata.CREATE_NOTE)
    }

    @Test
    fun searchAppFunctions_returnsAllSchemaAppFunction_withLegacyIndexer() = doBlocking {
        assumeFalse(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        assertThat(appFunctions.size).isEqualTo(getTotalFunctionCountInPackage())
    }

    @Test
    fun searchAppFunctions_returnsCorrectMetadata_withLegacyIndexer() = doBlocking {
        assumeFalse(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        // Validate schema AppFunctionMetadata.
        val createNoteMetadata = appFunctions.single { it.id == CREATE_NOTE_FUNCTION_ID }
        assertThat(createNoteMetadata)
            .isEqualTo(AppFunctionMetadataHelper.FunctionMetadata.CREATE_NOTE_LEGACY_INDEXER)
    }

    private suspend fun getTotalFunctionCountInPackage(): Int {
        return if (isDynamicIndexerAvailable(targetContext)) {
            val baseFunctionCount = 19
            val multiServiceFunctionCount = 6
            val dynamicFunctionsCount = 5
            if (Build.VERSION.SDK_INT >= 37) {
                baseFunctionCount + multiServiceFunctionCount + dynamicFunctionsCount
            } else {
                baseFunctionCount
            }
        } else {
            1
        }
    }
}
