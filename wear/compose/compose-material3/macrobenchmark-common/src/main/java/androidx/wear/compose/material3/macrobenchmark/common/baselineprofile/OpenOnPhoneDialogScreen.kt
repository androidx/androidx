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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen

val OpenOnPhoneDialogScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column {
                    var showConfirmation by remember { mutableStateOf(false) }

                    Box(Modifier.fillMaxSize()) {
                        FilledTonalButton(
                            modifier =
                                Modifier.align(Alignment.Center).semantics {
                                    contentDescription = OpenOnPhoneDialog
                                },
                            onClick = { showConfirmation = true },
                            label = { Text("Open on phone") }
                        )
                    }

                    val test = ""
                    test.let { ExcludePaths.none { path -> it.contains(path) } }

                    OpenOnPhoneDialog(
                        show = showConfirmation,
                        onDismissRequest = { showConfirmation = false },
                    )
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                device.wait(Until.findObject(By.desc(OpenOnPhoneDialog)), 10_000L).click()
                device.waitForIdle()
                device.wait(Until.findObject(By.desc(OpenOnPhoneDialog)), 10_000L)
            }
    }

private const val OpenOnPhoneDialog = "OpenOnPhoneDialog"

private val ExcludePaths = listOf("/material3/macrobenchmark/", "/material3/samples/")
