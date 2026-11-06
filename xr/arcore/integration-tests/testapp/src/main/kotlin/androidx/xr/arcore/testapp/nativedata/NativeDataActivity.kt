/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.testapp.nativedata

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.getNativeInstanceData
import androidx.xr.runtime.getNativeSessionData
import kotlin.coroutines.coroutineContext

open class NativeDataActivity : ComponentActivity() {

    /** OpenXR extension injected with [XrDevice.getCurrentDevice] in tests 2 and 3. */
    protected open val extensionToInject: String = "XR_ANDROID_trackables_marker"

    /**
     * Whether to show the results in a [SpatialPanel] once a session is created. Spatial panels
     * need a headset, so activities on other displays (such as projected displays) show them in the
     * activity window instead.
     */
    protected open val showResultsInSpatialPanel: Boolean = true

    /**
     * Whether to run test 5, which calls a function of the injected extension through the native
     * `xrGetInstanceProcAddr`. Requires [extensionToInject] to be `XR_EXT_debug_utils`.
     */
    protected open val verifyInjectedFunctionCall: Boolean = false

    /**
     * Context passed to [Session.create]. Sessions created with an Activity context also attach a
     * SceneCore Scene, which needs a SceneCore runtime for the current device.
     */
    protected open val sessionContext: Context
        get() = this

    private var invalidExtensionInjectResult by mutableStateOf("Not started")
    private var invalidExtensionInjectPassed by mutableStateOf(false)

    private var validExtensionInjectResult by mutableStateOf("Not started")
    private var validExtensionInjectPassed by mutableStateOf(false)

    private var injectExtensionAfterInitResult by mutableStateOf("Not started")
    private var injectExtensionAfterInitPassed by mutableStateOf(false)

    private var getNativeDataResult by mutableStateOf("Not started")
    private var getNativeDataPassed by mutableStateOf(false)

    private var injectedFunctionCallResult by mutableStateOf("Not started")
    private var injectedFunctionCallPassed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaunchedEffect(Unit) { runTests() }
            if (validExtensionInjectPassed && showResultsInSpatialPanel) {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.size(DpVolumeSize(640.dp, 480.dp, 0.dp))
                                .movable()
                                .resizable()
                    ) {
                        TestResultsView()
                    }
                }
            } else {
                TestResultsView()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        kotlin.system.exitProcess(0)
    }

    @Composable
    private fun TestResultsView() {
        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.xr.arcore.testapp.common.BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Native Data Tests",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.padding(innerPadding)
                        .fillMaxSize()
                        .background(color = Color.White)
                        .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Test 1: Invalid Extension Injection",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = invalidExtensionInjectResult, modifier = Modifier.weight(1f))
                    if (invalidExtensionInjectPassed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Passed",
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp),
                        )
                    } else if (invalidExtensionInjectResult != "Not started") {
                        Text(text = "Failed", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Test 2: Valid Extension Injection",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = validExtensionInjectResult, modifier = Modifier.weight(1f))
                    if (validExtensionInjectPassed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Passed",
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp),
                        )
                    } else if (validExtensionInjectResult != "Not started") {
                        Text(text = "Failed", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Test 3: Inject Extensions After Initialization",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = injectExtensionAfterInitResult, modifier = Modifier.weight(1f))
                    if (injectExtensionAfterInitPassed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Passed",
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp),
                        )
                    } else if (injectExtensionAfterInitResult != "Not started") {
                        Text(text = "Failed", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Test 4: Get Native Data",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = getNativeDataResult, modifier = Modifier.weight(1f))
                    if (getNativeDataPassed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Passed",
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp),
                        )
                    } else if (getNativeDataResult != "Not started") {
                        Text(text = "Failed", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                if (verifyInjectedFunctionCall) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Test 5: Call Injected Extension Function",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = injectedFunctionCallResult, modifier = Modifier.weight(1f))
                        if (injectedFunctionCallPassed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Passed",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp),
                            )
                        } else if (injectedFunctionCallResult != "Not started") {
                            Text(text = "Failed", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    @Suppress("RestrictedApiAndroidX")
    private suspend fun runTests() {
        // Test 1: Invalid Extensions
        val invalidExtensions = listOf("XR_INVALID_EXTENSION_NAME")
        try {
            XrDevice.getCurrentDevice(this, invalidExtensions)
            invalidExtensionInjectResult = "Unexpectedly succeeded injecting invalid extensions."
            invalidExtensionInjectPassed = false
        } catch (e: UnsupportedOperationException) {
            invalidExtensionInjectResult = "Success: Caught expected UnsupportedOperationException."
            invalidExtensionInjectPassed = true
        } catch (e: Exception) {
            invalidExtensionInjectResult =
                "Unexpected error during invalid extension injection: ${e.message}"
            invalidExtensionInjectPassed = false
        }

        // Test 2: Valid Extension and Session Creation
        val extensionsToInject = listOf(extensionToInject)
        val customLifecycleOwner =
            object : LifecycleOwner {
                val registry = LifecycleRegistry(this)
                override val lifecycle = registry
            }
        customLifecycleOwner.registry.currentState = Lifecycle.State.RESUMED

        try {
            val device = XrDevice.getCurrentDevice(this, extensionsToInject)

            // Read the native data for Test 4 before a Session exists: with the non-legacy
            // natives, the runtime library provides the XrSession without one. Test 4 reports
            // any failure.
            val instanceDataResult = runCatching { device.getNativeInstanceData(this) }

            val result =
                Session.create(
                    sessionContext,
                    coroutineContext = coroutineContext,
                    lifecycleOwner = customLifecycleOwner,
                )
            if (result is androidx.xr.runtime.SessionCreateSuccess) {
                validExtensionInjectResult =
                    "Success: Injected extensions & created XR session successfully."
                validExtensionInjectPassed = true

                // Test 3: Inject Extensions After Initialization
                try {
                    XrDevice.getCurrentDevice(this, listOf(extensionToInject))
                    injectExtensionAfterInitResult =
                        "Failed: Unexpectedly succeeded injecting extensions after initialization."
                    injectExtensionAfterInitPassed = false
                } catch (e: IllegalStateException) {
                    injectExtensionAfterInitResult =
                        "Success: Caught expected IllegalStateException."
                    injectExtensionAfterInitPassed = true
                } catch (e: Exception) {
                    injectExtensionAfterInitResult =
                        "Unexpected error during injection after initialization: ${e.message}"
                    injectExtensionAfterInitPassed = false
                }

                // Test 4: Get Native Data
                try {
                    val instanceData = instanceDataResult.getOrThrow()
                    val sessionPointer =
                        if (isUsingLegacyNatives()) {
                            // The legacy natives create the XrSession in arcore-openxr during
                            // Session.create, so it is only exposed through the Session.
                            @Suppress("DEPRECATION")
                            result.session.getNativeSessionData().sessionPointer
                        } else {
                            instanceData.sessionPointer
                        }

                    Log.i(
                        TAG,
                        "Native handles: instance=0x${java.lang.Long.toHexString(instanceData.instancePointer)}, " +
                            "session=0x${java.lang.Long.toHexString(instanceData.sessionPointer)}, " +
                            "getInstanceProcAddr=0x${java.lang.Long.toHexString(instanceData.functionTablePointer)}",
                    )
                    if (
                        instanceData.instancePointer != 0L &&
                            instanceData.functionTablePointer != 0L &&
                            sessionPointer != 0L
                    ) {
                        getNativeDataResult = "Success: Valid native handles retrieved."
                        getNativeDataPassed = true
                        if (verifyInjectedFunctionCall) {
                            runInjectedFunctionCallTest(
                                instanceData.functionTablePointer,
                                instanceData.instancePointer,
                                instanceData.sessionPointer,
                            )
                        }
                    } else {
                        getNativeDataResult = "Failed: One or more native handles were zero."
                        getNativeDataPassed = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get native data: ${e.message}", e)
                    getNativeDataResult = "Failed to get native data: ${e.message}"
                    getNativeDataPassed = false
                }

                // Deinit the session
                customLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
            } else {
                validExtensionInjectResult = "Failed to create session: ${result::class.simpleName}"
                validExtensionInjectPassed = false
                customLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to create session with valid extensions: ${e.message}",
                e,
            )
            validExtensionInjectResult = "Failed to create session with extensions: ${e.message}"
            validExtensionInjectPassed = false
            customLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
        }
        logResults()
    }

    /**
     * Logs every test result so that runs can be checked with `adb logcat -s NativeDataActivity`.
     */
    private fun logResults() {
        Log.i(TAG, "Injected extension: $extensionToInject")
        logResult(
            "Test 1 (invalid extension)",
            invalidExtensionInjectPassed,
            invalidExtensionInjectResult,
        )
        logResult(
            "Test 2 (valid extension + session)",
            validExtensionInjectPassed,
            validExtensionInjectResult,
        )
        logResult(
            "Test 3 (inject after init)",
            injectExtensionAfterInitPassed,
            injectExtensionAfterInitResult,
        )
        logResult("Test 4 (native data)", getNativeDataPassed, getNativeDataResult)
        if (verifyInjectedFunctionCall) {
            logResult(
                "Test 5 (call injected extension function)",
                injectedFunctionCallPassed,
                injectedFunctionCallResult,
            )
        }
    }

    /**
     * Test 5: checks that the injected `XR_EXT_debug_utils` is enabled on the native instance by
     * resolving `xrSetDebugUtilsObjectNameEXT` through the native `xrGetInstanceProcAddr` and
     * calling it on the native session. As a control, `xrLocateSpacesKHR` from
     * `XR_KHR_locate_spaces`, which is not injected, must not resolve.
     */
    private fun runInjectedFunctionCallTest(
        getInstanceProcAddr: Long,
        instance: Long,
        session: Long,
    ) {
        try {
            val callResult =
                OpenXrProcAddrChecker.setSessionDebugName(
                    getInstanceProcAddr,
                    instance,
                    session,
                    "NativeDataActivitySession",
                )
            val controlResult =
                OpenXrProcAddrChecker.resolveFunction(
                    getInstanceProcAddr,
                    instance,
                    "xrLocateSpacesKHR",
                )
            injectedFunctionCallPassed =
                callResult == OpenXrProcAddrChecker.XR_SUCCESS &&
                    controlResult == OpenXrProcAddrChecker.XR_ERROR_FUNCTION_UNSUPPORTED
            injectedFunctionCallResult =
                "xrSetDebugUtilsObjectNameEXT(session) = $callResult (expected 0), " +
                    "xrGetInstanceProcAddr(xrLocateSpacesKHR) = $controlResult (expected -7)"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call the injected extension function: ${e.message}", e)
            injectedFunctionCallResult =
                "Failed to call the injected extension function: ${e.message}"
            injectedFunctionCallPassed = false
        }
    }

    private fun logResult(name: String, passed: Boolean, result: String) {
        Log.i(TAG, "$name: ${if (passed) "PASS" else "FAIL"} - $result")
    }

    private companion object {
        const val TAG = "NativeDataActivity"
    }
}

/**
 * Returns whether the legacy Jetpack XR natives are in use, mirroring how the natives read the
 * `debug.jetpack_xr_natives.use_legacy` system property: legacy unless it is "0" or "false".
 */
// TODO(b/543942634): Remove once the legacy natives are removed.
@Suppress("BanUncheckedReflection")
private fun isUsingLegacyNatives(): Boolean =
    try {
        val value =
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(/* obj= */ null, "debug.jetpack_xr_natives.use_legacy") as String
        value != "0" && value != "false"
    } catch (_: Exception) {
        true
    }
