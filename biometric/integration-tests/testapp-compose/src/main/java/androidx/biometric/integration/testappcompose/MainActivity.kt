/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.biometric.integration.testappcompose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars // Import this
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TwoAuthLauncher() }
    }
}

@Composable
private fun TwoAuthLauncher() {
    Column {
        RememberLauncherForAuthResult("1")
        RememberLauncherForAuthResult("2")
        RememberLauncherForAuthResult("3")
        RememberLauncherForAuthResult("4")
    }
}

@Composable
private fun RememberLauncherForAuthResult(id: String) {
    var authResult by rememberSaveable { mutableStateOf("") }
    val resultCallback = remember {
        object : AuthenticationResultCallback {
            override fun onAuthResult(result: AuthenticationResult) {
                authResult = id + result.toText()
            }

            override fun onAuthAttemptFailed() {
                authResult = id + "fail, try again"
            }
        }
    }
    val launcher = rememberAuthenticationLauncher(resultCallback = resultCallback)

    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Button(
            onClick = {
                launcher.launch(
                    biometricRequest(
                        title = "test",
                        AuthenticationRequest.Biometric.Fallback.DeviceCredential,
                    ) {
                        // Optionally set the other configurations. setSubtitle(), setContent(), etc
                    }
                )
            }
        ) {
            Text(text = "Start Authentication")
        }
        Text(text = "Result: $authResult", modifier = Modifier.fillMaxWidth(fraction = 0.5f))
    }
}

private fun AuthenticationResult.toText(): String {
    return when (this) {
        is AuthenticationResult.Success ->
            "AuthenticationResult Success, auth type: $authType, crypto object: $crypto"
        is AuthenticationResult.Error ->
            "AuthenticationResult Error, error code: $errorCode, err string: $errString"
        is AuthenticationResult.CustomFallbackSelected ->
            "AuthenticationResult CustomFallbackSelected, fallback option text: ${fallback.text}"
    }
}
