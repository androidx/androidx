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

package androidx.biometric.integration.testapp

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationRequest.Companion.credentialRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.BiometricManager
import androidx.biometric.PromptContentItemBulletedText
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors

/** Activity presenting focused Jetpack Compose samples for BiometricPrompt integration. */
class BiometricComposeSamplesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BiometricComposeSamplesScreen() }
    }
}

private const val TAB_SIMPLE_BIO_AUTH = "Simple Biometric Auth"
private const val TAB_CREDENTIAL_AUTH = "Credential Only Auth"
private const val TAB_FALLBACK_OPTIONS = "Fallback Options"
private const val TAB_CONTENT_VIEWS = "Content Views"
private const val TAB_ADVANCED = "Advanced"

private val TABS =
    listOf(
        TAB_SIMPLE_BIO_AUTH,
        TAB_CREDENTIAL_AUTH,
        TAB_FALLBACK_OPTIONS,
        TAB_CONTENT_VIEWS,
        TAB_ADVANCED,
    )

private const val AUTH_TYPE_CLASS_2_BIOMETRIC = "Class 2 Biometric"
private const val AUTH_TYPE_CLASS_3_BIOMETRIC = "Class 3 Biometric"

private const val FALLBACK_DEVICE_CREDENTIAL = "Device Credential"
private const val FALLBACK_SINGLE_PASSWORD = "Use Account Password"
private const val FALLBACK_MULTIPLE = "Multiple Fallback Options"

private const val CONTENT_PLAIN_TEXT = "Plain Text"
private const val CONTENT_VERTICAL_LIST = "Vertical List"

@Composable
fun BiometricComposeSamplesScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var logText by rememberSaveable { mutableStateOf("") }

    val appendLog: (String) -> Unit = { message -> logText = "$message\n$logText" }

    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth()) {
        @Suppress("DEPRECATION")
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
        ) {
            TABS.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            when (TABS[selectedTabIndex]) {
                TAB_SIMPLE_BIO_AUTH -> SimpleBiometricAuthSection(appendLog)
                TAB_CREDENTIAL_AUTH -> CredentialOnlySection(appendLog)
                TAB_FALLBACK_OPTIONS -> FallbackOptionsSection(appendLog)
                TAB_CONTENT_VIEWS -> ContentViewsSection(appendLog)
                TAB_ADVANCED -> AdvancedSection(appendLog)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { logText = "" }) { Text("Clear Log") }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Log Output:",
                fontWeight = FontWeight.Bold,
            )
            Text(text = logText, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Sample 1: Minimal standard biometric authentication with Compose. */
@Composable
private fun SimpleBiometricAuthSection(appendLog: (String) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var requireConfirmation by rememberSaveable { mutableStateOf(true) }

    val launcher =
        rememberAuthenticationLauncher(
            resultCallback =
                object : AuthenticationResultCallback {
                    override fun onAuthResult(result: AuthenticationResult) {
                        appendLog("onAuthResult: ${result.toText()}")
                    }

                    override fun onAuthAttemptFailed() {
                        appendLog("onAuthAttemptFailed: Try again")
                    }
                }
        )

    Text("Demonstrates minimal biometric authentication.")

    Spacer(modifier = Modifier.height(8.dp))
    @Suppress("DEPRECATION")
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    @Suppress("DEPRECATION")
    OutlinedTextField(
        value = subtitle,
        onValueChange = { subtitle = it },
        label = { Text("Subtitle") },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = requireConfirmation,
            onCheckedChange = { requireConfirmation = it },
        )
        Text("Require confirmation")
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row {
        Button(
            onClick = {
                val status =
                    BiometricManager.from(context)
                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                appendLog(
                    "canAuthenticate (Class 2/Weak): ${status.toAuthenticationStatusString()}"
                )
            }
        ) {
            Text("Can Authenticate")
        }

        Spacer(modifier = Modifier.padding(start = 8.dp))
        Button(
            onClick = {
                try {
                    val request =
                        biometricRequest(title.ifEmpty { "Simple Biometric Auth" }) {
                            setSubtitle(subtitle.ifEmpty { null })
                            setIsConfirmationRequired(requireConfirmation)
                        }
                    launcher.launch(request)
                } catch (e: Exception) {
                    appendLog("Launch error: $e")
                }
            }
        ) {
            Text("Authenticate")
        }
    }
}

/** Sample 2: Device-credential-only authentication (API 30+) with Compose. */
@Composable
private fun CredentialOnlySection(appendLog: (String) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }

    val launcher =
        rememberAuthenticationLauncher(
            resultCallback =
                object : AuthenticationResultCallback {
                    override fun onAuthResult(result: AuthenticationResult) {
                        appendLog("onAuthResult: ${result.toText()}")
                    }

                    override fun onAuthAttemptFailed() {
                        appendLog("onAuthAttemptFailed: Try again")
                    }
                }
        )

    Text("Demonstrates credential-only authentication (PIN / Pattern / Password).")

    Spacer(modifier = Modifier.height(8.dp))
    @Suppress("DEPRECATION")
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    @Suppress("DEPRECATION")
    OutlinedTextField(
        value = subtitle,
        onValueChange = { subtitle = it },
        label = { Text("Subtitle") },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(12.dp))
    Row {
        Button(
            onClick = {
                val status =
                    BiometricManager.from(context)
                        .canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                appendLog(
                    "canAuthenticate (Device Credential): ${status.toAuthenticationStatusString()}"
                )
            }
        ) {
            Text("Can Authenticate")
        }

        Spacer(modifier = Modifier.padding(start = 8.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val request =
                            credentialRequest(title = title.ifEmpty { "Credential Auth" }) {
                                setSubtitle(subtitle.ifEmpty { null })
                            }
                        launcher.launch(request)
                    } catch (e: Exception) {
                        appendLog("Launch error: $e")
                    }
                } else {
                    appendLog("Credential-only authentication requires API 30+.")
                }
            },
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        ) {
            Text("Authenticate")
        }
    }
}

/** Sample 3: Customizing fallback options with Compose. */
@Composable
private fun FallbackOptionsSection(appendLog: (String) -> Unit) {
    var fallbackType by rememberSaveable { mutableStateOf(FALLBACK_MULTIPLE) }

    val launcher =
        rememberAuthenticationLauncher(
            resultCallback =
                object : AuthenticationResultCallback {
                    override fun onAuthResult(result: AuthenticationResult) {
                        appendLog("onAuthResult: ${result.toText()}")
                    }

                    override fun onAuthAttemptFailed() {
                        appendLog("onAuthAttemptFailed: Try again")
                    }
                }
        )

    Text("Demonstrates prompt with fallback options.")

    Spacer(modifier = Modifier.height(8.dp))
    DropdownSelection(
        label = "Fallback Option",
        selectedOption = fallbackType,
        options =
            listOf(
                FALLBACK_DEVICE_CREDENTIAL,
                FALLBACK_SINGLE_PASSWORD,
                FALLBACK_MULTIPLE,
            ),
        onOptionSelected = { fallbackType = it },
    )

    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            try {
                val fallbacks: Array<Biometric.Fallback> =
                    when (fallbackType) {
                        FALLBACK_DEVICE_CREDENTIAL -> arrayOf(Biometric.Fallback.DeviceCredential)
                        FALLBACK_SINGLE_PASSWORD ->
                            arrayOf(
                                Biometric.Fallback.CustomOption(
                                    "Use account password",
                                    Biometric.Fallback.ICON_TYPE_PASSWORD,
                                )
                            )
                        FALLBACK_MULTIPLE ->
                            arrayOf(
                                Biometric.Fallback.DeviceCredential,
                                Biometric.Fallback.CustomOption(
                                    "Use account password",
                                    Biometric.Fallback.ICON_TYPE_PASSWORD,
                                ),
                                Biometric.Fallback.CustomOption(
                                    "Sign in with a QR code",
                                    Biometric.Fallback.ICON_TYPE_QR_CODE,
                                ),
                            )
                        else -> emptyArray()
                    }

                val request =
                    biometricRequest("Biometric Fallbacks", *fallbacks) {
                        setSubtitle("Testing fallback options")
                    }
                launcher.launch(request)
            } catch (e: Exception) {
                appendLog("Launch error: $e")
            }
        }
    ) {
        Text("Authenticate")
    }
}

/** Sample 4: Demonstrates content view templates (API 35+) with Compose. */
@Composable
private fun ContentViewsSection(appendLog: (String) -> Unit) {
    var contentType by rememberSaveable { mutableStateOf(CONTENT_PLAIN_TEXT) }

    val launcher =
        rememberAuthenticationLauncher(
            resultCallback =
                object : AuthenticationResultCallback {
                    override fun onAuthResult(result: AuthenticationResult) {
                        appendLog("onAuthResult: ${result.toText()}")
                    }

                    override fun onAuthAttemptFailed() {
                        appendLog("onAuthAttemptFailed: Try again")
                    }
                }
        )

    Text("Demonstrates prompt with content view.")

    Spacer(modifier = Modifier.height(8.dp))
    DropdownSelection(
        label = "Content Type",
        selectedOption = contentType,
        options = listOf(CONTENT_PLAIN_TEXT, CONTENT_VERTICAL_LIST),
        isOptionEnabled = { option ->
            option == CONTENT_PLAIN_TEXT ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        },
        onOptionSelected = { contentType = it },
    )

    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            try {
                val bodyContent: AuthenticationRequest.BodyContent? =
                    when (contentType) {
                        CONTENT_PLAIN_TEXT ->
                            AuthenticationRequest.BodyContent.PlainText(
                                "This is a sample plain text description to display " +
                                    "as body content on the prompt."
                            )
                        CONTENT_VERTICAL_LIST ->
                            AuthenticationRequest.BodyContent.VerticalList(
                                "This is a sample vertical list description to " +
                                    "display as body content on the prompt.",
                                listOf(
                                    PromptContentItemBulletedText("Item 1"),
                                    PromptContentItemBulletedText("Item 2"),
                                    PromptContentItemBulletedText("Item 3"),
                                    PromptContentItemBulletedText("Item 4"),
                                ),
                            )
                        else -> null
                    }

                val request =
                    biometricRequest("Content View Auth") {
                        setSubtitle("Testing prompt content views")
                        setContent(bodyContent)
                    }
                launcher.launch(request)
            } catch (e: Exception) {
                appendLog("Launch error: $e")
            }
        }
    ) {
        Text("Authenticate")
    }
}

/** Sample 5: Advanced features (Custom background executor) with Compose. */
@Composable
private fun AdvancedSection(appendLog: (String) -> Unit) {
    val customExecutor = remember { Executors.newSingleThreadExecutor() }
    var authStrength by rememberSaveable { mutableStateOf(AUTH_TYPE_CLASS_3_BIOMETRIC) }
    var useCustomExecutor by rememberSaveable { mutableStateOf(false) }

    val callback =
        remember(appendLog) {
            object : AuthenticationResultCallback {
                override fun onAuthResult(result: AuthenticationResult) {
                    appendLog("onAuthResult: ${result.toText()}")
                }

                override fun onAuthAttemptFailed() {
                    appendLog("onAuthAttemptFailed: Try again")
                }
            }
        }

    val defaultLauncher = rememberAuthenticationLauncher(resultCallback = callback)
    val executorLauncher =
        rememberAuthenticationLauncher(
            callbackExecutor = customExecutor,
            resultCallback = callback,
        )

    val launcher = if (useCustomExecutor) executorLauncher else defaultLauncher

    Text("Demonstrates advanced features: Strength selection & custom callback executor.")

    Spacer(modifier = Modifier.height(8.dp))
    DropdownSelection(
        label = "Biometric Strength",
        selectedOption = authStrength,
        options = listOf(AUTH_TYPE_CLASS_2_BIOMETRIC, AUTH_TYPE_CLASS_3_BIOMETRIC),
        onOptionSelected = { authStrength = it },
    )

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = useCustomExecutor,
            onCheckedChange = { useCustomExecutor = it },
        )
        Text("Run callback on custom background Executor")
    }

    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            try {
                val request =
                    biometricRequest("Advanced Auth") {
                        setSubtitle("Testing executor and strength")
                        setMinStrength(
                            if (authStrength == AUTH_TYPE_CLASS_2_BIOMETRIC) {
                                Biometric.Strength.Class2
                            } else {
                                Biometric.Strength.Class3()
                            }
                        )
                    }
                launcher.launch(request)
            } catch (e: Exception) {
                appendLog("Launch error: $e")
            }
        }
    ) {
        Text("Authenticate")
    }
}

private fun AuthenticationResult.toText(): String {
    return when (this) {
        is AuthenticationResult.Success -> "Success, auth type: $authType, crypto: $crypto"
        is AuthenticationResult.Error -> "Error, code: $errorCode, message: $errString"
        is AuthenticationResult.CustomFallbackSelected ->
            "CustomFallbackSelected: ${fallback.text} (iconType: ${fallback.iconType})"
    }
}

@Composable
private fun DropdownSelection(
    label: String,
    selectedOption: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOptionEnabled: (String) -> Boolean = { true },
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("$label:", fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedOption)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    val optionEnabled = isOptionEnabled(option)
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        enabled = optionEnabled,
                    )
                }
            }
        }
    }
}
