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

package androidx.xr.arcore.testapp.flags

import android.os.Bundle
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow

/**
 * Shows the system properties that select how Jetpack XR uses OpenXR, with the `adb` commands that
 * change them.
 *
 * Apps can read these properties but not set them, so they have to be changed with `adb shell
 * setprop`. Jetpack XR reads them when it creates the OpenXR instance, which happens once per
 * process: test cases that run in their own process (such as the Native Data ones) pick up new
 * values the next time they are started, the others after the app is force-stopped.
 */
class RuntimeFlagsActivity : ComponentActivity() {

    private class RuntimeFlag(val property: String, val description: String)

    private val flags =
        listOf(
            RuntimeFlag(
                "androidx.xr.force_openxr",
                "Use the OpenXR runtime on devices that do not declare the OpenXR system " +
                    "feature, such as phones driving projected XR glasses.",
            ),
            RuntimeFlag(
                "debug.androidx.xr.openxr.activity_binding",
                "Bind the OpenXR instance to the Activity that creates it. Needed by OpenXR " +
                    "runtimes that require an Activity, such as projected ones.",
            ),
            RuntimeFlag(
                "debug.jetpack_xr_natives.use_legacy",
                "Use the legacy OpenXR implementation (the default when not set).",
            ),
            RuntimeFlag(
                "debug.jetpack_xr_natives.use_spatial_entities",
                "Use the OpenXR spatial entities extensions. Takes precedence over the " +
                    "<meta-data> of the same name in the app manifest.",
            ),
        )

    private var values by mutableStateOf(emptyMap<String, String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Refresh every time the activity comes back, e.g. after changing a property with adb.
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refresh()
            }
        )
        setContent { FlagsScreen() }
    }

    private fun refresh() {
        values = flags.associate { it.property to readSystemProperty(it.property) }
    }

    @Composable
    private fun FlagsScreen() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "OpenXR Runtime Flags",
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
                        .padding(16.dp)
            ) {
                Text(
                    "Apps can't set these properties: run the commands below from a computer " +
                        "with adb, then restart the test case. Values are read once per process."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { refresh() }) { Text("Refresh") }
                for (flag in flags) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    FlagItem(flag, values[flag.property].orEmpty())
                }
            }
        }
    }

    @Composable
    private fun FlagItem(flag: RuntimeFlag, value: String) {
        Text(flag.property, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("Current value: ${value.ifEmpty { "not set" }}")
        Text(flag.description, fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Text(
                "adb shell setprop ${flag.property} true\n" +
                    "adb shell setprop ${flag.property} false\n" +
                    "adb shell setprop ${flag.property} \"\"",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }

    private companion object {
        /** Returns the value of the system property [key], or an empty string if not set. */
        @Suppress("BanUncheckedReflection")
        fun readSystemProperty(key: String): String =
            try {
                Class.forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java)
                    .invoke(/* obj= */ null, key) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
    }
}
