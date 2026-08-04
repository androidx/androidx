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
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

// TODO(b/494238381): Test packageChanged events when test app installation is supported.
@OptIn(ExperimentalCoroutinesApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ObserveAppFunctionsTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private lateinit var appFunctionManager: AppFunctionManager

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    private val functionsUnderTest =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
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
            metadataTestHelper.awaitAppFunctionIndexed(functionsUnderTest)

            for (functionIds in functionsUnderTest) {
                appFunctionManager.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun onAppFunctionStateChanged_returnsAppFunctionName() =
        runBlocking<Unit> {
            val targetFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                )

            collectEventsWithRegisteredFlow { eventChannel ->
                try {
                    appFunctionManager.setAppFunctionEnabled(
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                    )

                    val event = eventChannel.receive()
                    assertThat(event)
                        .isInstanceOf(ObserveAppFunctionsEvent.StatesChanged::class.java)

                    val stateChangeEvent = event as ObserveAppFunctionsEvent.StatesChanged
                    assertThat(stateChangeEvent.changedFunctionNames).contains(targetFunctionName)

                    assertAppFunctionEnabledState(targetFunctionName, expectedEnabled = false)
                } finally {
                    withContext(NonCancellable) {
                        appFunctionManager.setAppFunctionEnabled(
                            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                            AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                        )
                    }
                }
            }
        }

    @Test
    fun onAppFunctionStateChanged_returnsAppFunctionName_schemalessFunction() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            val targetFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                )

            collectEventsWithRegisteredFlow { eventChannel ->
                try {
                    appFunctionManager.setAppFunctionEnabled(
                        AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                    )

                    val event = eventChannel.receive()
                    assertThat(event)
                        .isInstanceOf(ObserveAppFunctionsEvent.StatesChanged::class.java)

                    val stateChangeEvent = event as ObserveAppFunctionsEvent.StatesChanged
                    assertThat(stateChangeEvent.changedFunctionNames).contains(targetFunctionName)

                    assertAppFunctionEnabledState(targetFunctionName, expectedEnabled = false)
                } finally {
                    withContext(NonCancellable) {
                        appFunctionManager.setAppFunctionEnabled(
                            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                            AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                        )
                    }
                }
            }
        }

    @Test
    fun onAppFunctionStateChanged_sameFunctionBurst_returnsConsolidatedAppFunctionName() =
        runBlocking<Unit> {
            val targetFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                )

            collectEventsWithRegisteredFlow { eventChannel ->
                try {
                    appFunctionManager.setAppFunctionEnabled(
                        targetFunctionName.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                    )
                    appFunctionManager.setAppFunctionEnabled(
                        targetFunctionName.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )

                    val event = eventChannel.receive()
                    assertThat(event)
                        .isInstanceOf(ObserveAppFunctionsEvent.StatesChanged::class.java)

                    val stateChange = event as ObserveAppFunctionsEvent.StatesChanged
                    assertThat(stateChange.changedFunctionNames).containsExactly(targetFunctionName)

                    assertAppFunctionEnabledState(targetFunctionName, expectedEnabled = true)
                } finally {
                    withContext(NonCancellable) {
                        appFunctionManager.setAppFunctionEnabled(
                            targetFunctionName.functionIdentifier,
                            AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                        )
                    }
                }
            }
        }

    @Test
    fun onAppFunctionStateChanged_multipleFunctionsBurst_returnsConsolidatedAppFunctionName() =
        runBlocking<Unit> {
            val firstFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                )
            val secondFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
                )

            collectEventsWithRegisteredFlow { eventChannel ->
                try {
                    appFunctionManager.setAppFunctionEnabled(
                        firstFunctionName.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                    )
                    appFunctionManager.setAppFunctionEnabled(
                        secondFunctionName.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_ENABLED,
                    )

                    val event = eventChannel.receive()
                    assertThat(event)
                        .isInstanceOf(ObserveAppFunctionsEvent.StatesChanged::class.java)

                    val stateChange = event as ObserveAppFunctionsEvent.StatesChanged
                    assertThat(stateChange.changedFunctionNames)
                        .containsExactly(firstFunctionName, secondFunctionName)

                    assertAppFunctionEnabledState(firstFunctionName, expectedEnabled = false)
                    assertAppFunctionEnabledState(secondFunctionName, expectedEnabled = true)
                } finally {
                    withContext(NonCancellable) {
                        appFunctionManager.setAppFunctionEnabled(
                            firstFunctionName.functionIdentifier,
                            AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                        )
                        appFunctionManager.setAppFunctionEnabled(
                            secondFunctionName.functionIdentifier,
                            AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                        )
                    }
                }
            }
        }

    @Test
    fun flowCancelled_stopsObserving() =
        runBlocking<Unit> {
            val flow = appFunctionManager.observeAppFunctions()
            val targetFunctionName =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                )

            val eventChannel = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)

            val collectJob = launch { flow.collect { event -> eventChannel.send(event) } }

            dispatchSentinelNotification(eventChannel)

            try {
                appFunctionManager.setAppFunctionEnabled(
                    targetFunctionName.functionIdentifier,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                )

                val firstEvent = eventChannel.receive() as ObserveAppFunctionsEvent.StatesChanged
                assertThat(firstEvent.changedFunctionNames).contains(targetFunctionName)

                collectJob.cancel()

                val eventChannel2 = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)
                val collectJob2 = launch { flow.collect { event -> eventChannel2.send(event) } }

                dispatchSentinelNotification(eventChannel2)

                appFunctionManager.setAppFunctionEnabled(
                    targetFunctionName.functionIdentifier,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_ENABLED,
                )

                dispatchSentinelNotification(eventChannel2)

                assertThat(eventChannel.isEmpty).isTrue()

                collectJob2.cancel()
            } finally {
                withContext(NonCancellable) {
                    appFunctionManager.setAppFunctionEnabled(
                        targetFunctionName.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                    appFunctionManager.setAppFunctionEnabled(
                        AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                }
            }
        }

    @Test
    fun concurrentCollectors_allReceiveUpdates() =
        runBlocking<Unit> {
            val changeEventsFlow = appFunctionManager.observeAppFunctions()
            val functionEnabledByDefault =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                )
            val functionDisabledByDefault =
                AppFunctionName(
                    packageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
                )

            val channel1 = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)
            val channel2 = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)

            val collectJob1 = launch { changeEventsFlow.collect { event -> channel1.send(event) } }
            val collectJob2 = launch { changeEventsFlow.collect { event -> channel2.send(event) } }

            dispatchSentinelNotification(channel1, channel2)

            try {
                appFunctionManager.setAppFunctionEnabled(
                    functionEnabledByDefault.functionIdentifier,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_DISABLED,
                )
                appFunctionManager.setAppFunctionEnabled(
                    functionDisabledByDefault.functionIdentifier,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_ENABLED,
                )

                val event1 = channel1.receive() as ObserveAppFunctionsEvent.StatesChanged
                assertThat(event1.changedFunctionNames)
                    .containsExactly(functionEnabledByDefault, functionDisabledByDefault)

                val event2 = channel2.receive() as ObserveAppFunctionsEvent.StatesChanged
                assertThat(event2.changedFunctionNames)
                    .containsExactly(functionEnabledByDefault, functionDisabledByDefault)
            } finally {
                collectJob1.cancel()
                collectJob2.cancel()
                withContext(NonCancellable) {
                    appFunctionManager.setAppFunctionEnabled(
                        functionEnabledByDefault.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                    appFunctionManager.setAppFunctionEnabled(
                        functionDisabledByDefault.functionIdentifier,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                    appFunctionManager.setAppFunctionEnabled(
                        AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                }
            }
        }

    private suspend fun assertAppFunctionEnabledState(
        targetFunctionName: AppFunctionName,
        expectedEnabled: Boolean,
    ) {
        val isEnabled =
            appFunctionManager
                .getAppFunctionStates(
                    appFunctionNames =
                        listOf(
                            AppFunctionName(
                                targetFunctionName.packageName,
                                targetFunctionName.functionIdentifier,
                            )
                        )
                )
                .single()
                .isEnabled
        assertThat(isEnabled).isEqualTo(expectedEnabled)
    }

    /**
     * Runs [onObserverRegistered] with a fully registered Flow callback event channel.
     *
     * Because `observeAppFunctions()` creates the AppSearch session asynchronously when collection
     * starts, triggering state changes immediately would race against session creation and lose
     * events.
     *
     * This helper launches the collection in the background, waits for a sentinel state change
     * event to complete (guaranteeing the observer is fully registered), and executes
     * [onObserverRegistered] before canceling the collector and resetting the sentinel function.
     */
    private suspend fun collectEventsWithRegisteredFlow(
        onObserverRegistered: suspend CoroutineScope.(Channel<ObserveAppFunctionsEvent>) -> Unit
    ) {
        coroutineScope {
            val flow = appFunctionManager.observeAppFunctions()
            val eventChannel = Channel<ObserveAppFunctionsEvent>(Channel.UNLIMITED)
            val collectJob = launch { flow.collect { event -> eventChannel.send(event) } }

            dispatchSentinelNotification(eventChannel)

            try {
                onObserverRegistered(eventChannel)
            } finally {
                collectJob.cancel()
                withContext(NonCancellable) {
                    appFunctionManager.setAppFunctionEnabled(
                        AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
                        AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                    )
                }
            }
        }
    }

    private suspend fun dispatchSentinelNotification(
        vararg channels: Channel<ObserveAppFunctionsEvent>
    ) {
        val sentinelFunctionName =
            AppFunctionName(
                packageName = context.packageName,
                functionIdentifier = AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
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

        appFunctionManager.setAppFunctionEnabled(
            sentinelFunctionName.functionIdentifier,
            targetState,
        )

        for (channel in channels) {
            while (true) {
                val event = channel.receive()
                if (
                    event is ObserveAppFunctionsEvent.StatesChanged &&
                        event.changedFunctionNames.contains(sentinelFunctionName)
                ) {
                    break
                }
            }
        }
    }
}
