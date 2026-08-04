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
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ObserveAppFunctionsEvent
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.ADD_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.CREATE_NOTE_DISABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.CREATE_NOTE_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.DEPRECATED_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.DISABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.ENABLED_BY_DEFAULT_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.SENTINEL_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.TARGET_APP_PACKAGE
import androidx.appfunctions.integration.test.agent.AppSearchMetadataHelper.isDynamicIndexerAvailable
import androidx.appfunctions.integration.test.agent.TestUtil.assertAppFunctionEnabledState
import androidx.appfunctions.integration.test.agent.TestUtil.awaitAppFunctionsIndexed
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.setAppFunctionStateRemoteAsync
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.collections.filter
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Integration tests for observeAppFunctions API. */
// TODO(b/494238381): Add tests for package update scenarios which require multiple versions of the
//  testapp.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES_FULL.BAKLAVA)
@LargeTest
class ObserveAppFunctionsIntegrationTest {
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
    fun observeAppFunctions_onFunctionDisabled_emitsStateChange() = doBlocking {
        val enabledByDefaultFunction =
            AppFunctionName(
                packageName = TARGET_APP_PACKAGE,
                functionIdentifier = ENABLED_BY_DEFAULT_FUNCTION_ID,
            )

        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()
        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)
            try {
                setAppFunctionStateRemoteAsync(
                    enabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )
                retryAssert {
                    appFunctionManager.assertAppFunctionEnabledState(
                        enabledByDefaultFunction,
                        false,
                    )
                }

                retryAssert {
                    // Drain all pending events in the channel at the time of assertion
                    drainEvents(eventChannel, receivedEvents)
                    val hasTargetAppFunction =
                        receivedEvents.any { event ->
                            event is ObserveAppFunctionsEvent.StatesChanged &&
                                event.changedFunctionNames.contains(enabledByDefaultFunction)
                        }
                    assertThat(hasTargetAppFunction).isTrue()
                }
            } finally {
                setAppFunctionStateRemoteAsync(
                    enabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun observeAppFunctions_onFunctionDisabled_emitsStateChange_withSchemaFunction() = doBlocking {
        val enabledByDefaultFunction =
            AppFunctionName(
                packageName = TARGET_APP_PACKAGE,
                functionIdentifier = CREATE_NOTE_FUNCTION_ID,
            )

        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()
        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)
            try {
                setAppFunctionStateRemoteAsync(
                    enabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )
                retryAssert {
                    appFunctionManager.assertAppFunctionEnabledState(
                        enabledByDefaultFunction,
                        false,
                    )
                }

                retryAssert {
                    // Drain all pending events in the channel at the time of assertion
                    drainEvents(eventChannel, receivedEvents)
                    val hasTargetAppFunction =
                        receivedEvents.any { event ->
                            event is ObserveAppFunctionsEvent.StatesChanged &&
                                event.changedFunctionNames.contains(enabledByDefaultFunction)
                        }
                    assertThat(hasTargetAppFunction).isTrue()
                }
            } finally {
                setAppFunctionStateRemoteAsync(
                    enabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun observeAppFunctions_onFunctionEnabled_emitsStateChange() = doBlocking {
        val disabledByDefaultFunction =
            AppFunctionName(
                packageName = TARGET_APP_PACKAGE,
                functionIdentifier = DISABLED_BY_DEFAULT_FUNCTION_ID,
            )

        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()
        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)
            try {
                setAppFunctionStateRemoteAsync(
                    disabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
                )

                retryAssert {
                    appFunctionManager.assertAppFunctionEnabledState(
                        disabledByDefaultFunction,
                        true,
                    )
                }

                retryAssert {
                    // Drain all pending events in the channel at the time of assertion
                    drainEvents(eventChannel, receivedEvents)
                    val hasTargetAppFunction =
                        receivedEvents.any { event ->
                            event is ObserveAppFunctionsEvent.StatesChanged &&
                                event.changedFunctionNames.contains(disabledByDefaultFunction)
                        }
                    assertThat(hasTargetAppFunction).isTrue()
                }

                // Execute the now enabled function
                val searchResult =
                    appFunctionManager.searchAppFunctions(
                        AppFunctionSearchSpec(
                            packageNames = setOf(TARGET_APP_PACKAGE),
                            functionNames = setOf(disabledByDefaultFunction),
                        )
                    )
                val disabledByDefaultFunctionMetadata = searchResult.single()
                val response =
                    appFunctionManager.executeAppFunction(
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            disabledByDefaultFunctionMetadata.id,
                            AppFunctionData.Builder(
                                    disabledByDefaultFunctionMetadata.parameters,
                                    disabledByDefaultFunctionMetadata.components,
                                )
                                .build(),
                        )
                    )
                assertIs<ExecuteAppFunctionResponse.Success>(response)
            } finally {
                setAppFunctionStateRemoteAsync(
                    disabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun observeAppFunctions_onFunctionEnabled_emitsStateChange_withSchemaFunction() = doBlocking {
        val disabledByDefaultFunction =
            AppFunctionName(
                packageName = TARGET_APP_PACKAGE,
                functionIdentifier = CREATE_NOTE_DISABLED_BY_DEFAULT_FUNCTION_ID,
            )

        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()
        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)
            try {
                setAppFunctionStateRemoteAsync(
                    disabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
                )

                retryAssert {
                    appFunctionManager.assertAppFunctionEnabledState(
                        disabledByDefaultFunction,
                        true,
                    )
                }

                retryAssert {
                    // Drain all pending events in the channel at the time of assertion
                    drainEvents(eventChannel, receivedEvents)
                    val hasTargetAppFunction =
                        receivedEvents.any { event ->
                            event is ObserveAppFunctionsEvent.StatesChanged &&
                                event.changedFunctionNames.contains(disabledByDefaultFunction)
                        }
                    assertThat(hasTargetAppFunction).isTrue()
                }

                // Execute the now enabled function
                val searchResult =
                    appFunctionManager.searchAppFunctions(
                        AppFunctionSearchSpec(
                            packageNames = setOf(TARGET_APP_PACKAGE),
                            functionNames = setOf(disabledByDefaultFunction),
                        )
                    )
                val disabledByDefaultFunctionMetadata = searchResult.single()
                val response =
                    appFunctionManager.executeAppFunction(
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            disabledByDefaultFunctionMetadata.id,
                            AppFunctionData.Builder(
                                    disabledByDefaultFunctionMetadata.parameters,
                                    disabledByDefaultFunctionMetadata.components,
                                )
                                .build(),
                        )
                    )
                assertIs<ExecuteAppFunctionResponse.Success>(response)
            } finally {
                setAppFunctionStateRemoteAsync(
                    disabledByDefaultFunction,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun observeAppFunctions_onPackageUninstalled_emitsPackageChange() = doBlocking {
        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()
        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)

            InstallHelper.suspendUninstall(TARGET_APP_PACKAGE)

            retryAssert {
                drainEvents(eventChannel, receivedEvents)
                val targetPackageChangeEvents =
                    receivedEvents
                        .filterIsInstance<ObserveAppFunctionsEvent.MetadataChanged>()
                        .filter { event -> event.changedPackageNames.contains(TARGET_APP_PACKAGE) }
                assertThat(targetPackageChangeEvents).isNotEmpty()
            }

            val searchResult =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
                )
            assertThat(searchResult).isEmpty()
        }
    }

    @Test
    fun observeAppFunctions_onPackageInstalled_emitsPackageChange() = doBlocking {
        val receivedEvents = mutableListOf<ObserveAppFunctionsEvent>()

        observeAppFunctionsWithRegisteredFlow { eventChannel ->
            dispatchSentinelNotification(eventChannel)
            // Uninstall the test package to re-install
            InstallHelper.suspendUninstall(TARGET_APP_PACKAGE)

            // Read the package change event for installation
            retryAssert {
                drainEvents(eventChannel, receivedEvents)

                val targetPackageChangeEvents =
                    receivedEvents
                        .filterIsInstance<ObserveAppFunctionsEvent.MetadataChanged>()
                        .filter { event -> event.changedPackageNames.contains(TARGET_APP_PACKAGE) }
                assertThat(targetPackageChangeEvents).isNotEmpty()
            }
            receivedEvents.clear()

            // Re-install the test package
            InstallHelper.install(targetAppApkFile)
            targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)

            // Read the package change event for installation
            retryAssert {
                drainEvents(eventChannel, receivedEvents)

                val targetPackageChangeEvents =
                    receivedEvents
                        .filterIsInstance<ObserveAppFunctionsEvent.MetadataChanged>()
                        .filter { event -> event.changedPackageNames.contains(TARGET_APP_PACKAGE) }
                assertThat(targetPackageChangeEvents).isNotEmpty()
            }

            // Verify metadata is now searchable and has correct values
            val searchResult =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
                )
            assertThat(searchResult.size).isEqualTo(getTotalFunctionCountInPackage())
        }
    }

    private suspend fun observeAppFunctionsWithRegisteredFlow(
        onObserverRegistered: suspend CoroutineScope.(Channel<ObserveAppFunctionsEvent>) -> Unit
    ) {
        coroutineScope {
            val flow = appFunctionManager.observeAppFunctions()
            val eventChannel = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)
            val collectJob = launch { flow.collect { event -> eventChannel.send(event) } }

            try {
                onObserverRegistered(eventChannel)
            } finally {
                collectJob.cancel()
                try {
                    setAppFunctionStateRemoteAsync(
                        AppFunctionName(TARGET_APP_PACKAGE, SENTINEL_FUNCTION_ID),
                        AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                    )
                } catch (e: Throwable) {}
            }
        }
    }

    private suspend fun dispatchSentinelNotification(
        vararg channels: Channel<ObserveAppFunctionsEvent>
    ) {
        val sentinelFunctionName =
            AppFunctionName(
                packageName = TARGET_APP_PACKAGE,
                functionIdentifier = SENTINEL_FUNCTION_ID,
            )
        val currentState =
            appFunctionManager
                .getAppFunctionStates(
                    listOf(
                        AppFunctionName(
                            sentinelFunctionName.packageName,
                            sentinelFunctionName.functionIdentifier,
                        )
                    )
                )
                .single()
                .isEnabled
        val targetState =
            if (currentState) {
                AppFunctionManager.APP_FUNCTION_STATE_DISABLED
            } else {
                AppFunctionManager.APP_FUNCTION_STATE_ENABLED
            }

        setAppFunctionStateRemoteAsync(sentinelFunctionName, targetState)

        for (channel in channels) {
            val received = mutableListOf<ObserveAppFunctionsEvent>()
            retryAssert {
                drainEvents(channel, received)
                val hasSentinel =
                    received.any { event ->
                        event is ObserveAppFunctionsEvent.StatesChanged &&
                            event.changedFunctionNames.contains(sentinelFunctionName)
                    }
                assertThat(hasSentinel).isTrue()
            }
        }
    }

    private fun drainEvents(
        eventChannel: Channel<ObserveAppFunctionsEvent>,
        receivedEvents: MutableList<ObserveAppFunctionsEvent>,
    ) {
        while (true) {
            val result = eventChannel.tryReceive()
            if (result.isFailure) break
            val event = result.getOrNull()
            if (event != null) {
                receivedEvents.add(event)
            }
        }
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
