/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.FixedFontSize
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.SimpleButtonSample
import androidx.wear.compose.material3.samples.SimpleChildButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
import androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample

val WearMaterial3Demos = DemoCategory(
    "Material3",
    listOf(
        DemoCategory(
            "Button",
            listOf(
                DemoCategory(
                    "Samples",
                    listOf(
                        ComposableDemo("Button") {
                            Centralize {
                                SimpleButtonSample()
                                Spacer(Modifier.height(4.dp))
                                ButtonSample()
                            }
                        },
                        ComposableDemo("FilledTonalButton") {
                            Centralize {
                                SimpleFilledTonalButtonSample()
                                Spacer(Modifier.height(4.dp))
                                FilledTonalButtonSample()
                            }
                        },
                        ComposableDemo("OutlinedButton") {
                            Centralize {
                                SimpleOutlinedButtonSample()
                                Spacer(Modifier.height(4.dp))
                                OutlinedButtonSample()
                            }
                        },
                        ComposableDemo("ChildButton") {
                            Centralize {
                                SimpleChildButtonSample()
                                Spacer(Modifier.height(4.dp))
                                ChildButtonSample()
                            }
                        },
                    )
                ),
                DemoCategory(
                    "Demos",
                    listOf(
                        // Add button demos here
                    )
                )
            )
        ),
        DemoCategory(
            "Theme",
            listOf(
                ComposableDemo(
                    title = "Fixed Font Size",
                    description =
                    "Display1 font size not impacted by changes to user font selection",
                ) { Centralize { FixedFontSize() } },
            )
        )
    )
)