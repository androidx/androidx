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

package androidx.appfunctions

import android.Manifest
import android.app.UiAutomation
import android.content.Context
import android.os.Build
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.core.AppFunctionMetadataTestHelper.FunctionNames
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class GetAppFunctionStatesTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private lateinit var appFunctionManager: AppFunctionManager

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    private val resetFunctionIds =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
        )

    @Before
    fun setup() {
        val appFunctionManagerOrNull = AppFunctionManager.getInstance(context)
        assumeNotNull(appFunctionManagerOrNull)
        appFunctionManager = checkNotNull(appFunctionManagerOrNull)

        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.INSTALL_PACKAGES,
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )

        runBlocking {
            metadataTestHelper.awaitAppFunctionIndexed(resetFunctionIds)

            // Reset all test ids
            for (functionIds in resetFunctionIds) {
                appFunctionManager.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun getAppFunctionState_functionDoesNotExist_returnsEmpty() {
        val fakeFunction = AppFunctionName("fake.package", "doesNotExist")
        val states = runBlocking { appFunctionManager.getAppFunctionStates(listOf(fakeFunction)) }

        assertThat(states).isEmpty()
    }

    @Test
    fun getAppFunctionStates_returnsCorrectDefaultState() {
        val states = runBlocking {
            appFunctionManager.getAppFunctionStates(
                listOf(
                    FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    FunctionNames.NO_SCHEMA_DISABLED_BY_DEFAULT,
                )
            )
        }

        assertThat(states)
            .containsExactly(
                AppFunctionState(
                    functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    isEnabled = true,
                ),
                AppFunctionState(
                    functionName = FunctionNames.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    isEnabled = false,
                ),
            )
    }

    @Test
    fun getAppFunctionStates_reflectsRuntimeChangesAfterDisabled() {
        val functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT
        try {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )

                val state = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()

                assertThat(state.isEnabled).isFalse()
            }
        } finally {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun getAppFunctionStates_reflectsRuntimeChangesAfterDisabledThenEnabled() {
        val functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT
        try {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
                )

                val state = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()

                assertThat(state.isEnabled).isTrue()
            }
        } finally {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }
}
