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
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.transformingMovable
import androidx.xr.compose.subspace.layout.transformingResizable
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.UnstableNativeResourceApi
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.getNativeInstanceData
import androidx.xr.runtime.getNativeSessionData

class NativeDataActivity : ComponentActivity() {

    private var invalidExtensionInjectResult by mutableStateOf("Not started")
    private var invalidExtensionInjectPassed by mutableStateOf(false)

    private var validExtensionInjectResult by mutableStateOf("Not started")
    private var validExtensionInjectPassed by mutableStateOf(false)

    private var injectExtensionAfterInitResult by mutableStateOf("Not started")
    private var injectExtensionAfterInitPassed by mutableStateOf(false)

    private var getNativeDataResult by mutableStateOf("Not started")
    private var getNativeDataPassed by mutableStateOf(false)

    @OptIn(UnstableNativeResourceApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaunchedEffect(Unit) { runTests() }
            if (validExtensionInjectPassed) {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.size(DpVolumeSize(640.dp, 480.dp, 0.dp))
                                .transformingMovable()
                                .transformingResizable()
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
            }
        }
    }

    @OptIn(UnstableNativeResourceApi::class)
    @Suppress("RestrictedApiAndroidX")
    private fun runTests() {
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
        val extensionsToInject = listOf("XR_ANDROID_trackables_marker")
        val customLifecycleOwner =
            object : LifecycleOwner {
                val registry = LifecycleRegistry(this)
                override val lifecycle = registry
            }
        customLifecycleOwner.registry.currentState = Lifecycle.State.RESUMED

        try {
            XrDevice.getCurrentDevice(this, extensionsToInject)

            val result = Session.create(this, lifecycleOwner = customLifecycleOwner)
            if (result is androidx.xr.runtime.SessionCreateSuccess) {
                validExtensionInjectResult =
                    "Success: Injected extensions & created XR session successfully."
                validExtensionInjectPassed = true

                // Test 3: Inject Extensions After Initialization
                try {
                    XrDevice.getCurrentDevice(this, listOf("XR_ANDROID_trackables_marker"))
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
                    val device = XrDevice.getCurrentDevice(this)
                    val instanceData = device.getNativeInstanceData(this)
                    val sessionData = result.session.getNativeSessionData()

                    if (
                        instanceData.instancePointer != 0L &&
                            instanceData.functionTablePointer != 0L &&
                            sessionData.sessionPointer != 0L
                    ) {
                        getNativeDataResult = "Success: Valid native handles retrieved."
                        getNativeDataPassed = true
                    } else {
                        getNativeDataResult = "Failed: One or more native handles were zero."
                        getNativeDataPassed = false
                    }
                } catch (e: Exception) {
                    Log.e("NativeDataActivity", "Failed to get native data: ${e.message}", e)
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
                "NativeDataActivity",
                "Failed to create session with valid extensions: ${e.message}",
                e,
            )
            validExtensionInjectResult = "Failed to create session with extensions: ${e.message}"
            validExtensionInjectPassed = false
            customLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
