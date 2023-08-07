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

package androidx.glance.appwidget.testing.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.text.Text
import org.junit.Test

@Sampled
@Suppress("unused")
fun isolatedGlanceComposableTestSamples() {
    class TestSample {
        @Test
        fun statusContent_statusFalse_outputsPending() = runGlanceAppWidgetUnitTest {
            provideComposable {
                StatusRow(
                    status = false
                )
            }

            onNode(hasTestTag("status-text"))
                .assert(hasText("Pending"))
        }

        @Test
        fun statusContent_statusTrue_outputsFinished() = runGlanceAppWidgetUnitTest {
            provideComposable {
                StatusRow(
                    status = true
                )
            }

            onNode(hasTestTag("status-text"))
                .assert(hasText("Finished"))
        }

        @Test
        fun header_smallSize_showsShortHeaderText() = runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(width = 50.dp, height = 100.dp))

            provideComposable {
                StatusRow(
                    status = false
                )
            }

            onNode(hasTestTag("header-text"))
                .assert(hasText("MyApp"))
        }

        @Test
        fun header_largeSize_showsLongHeaderText() = runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(width = 150.dp, height = 100.dp))

            provideComposable {
                StatusRow(
                    status = false
                )
            }

            onNode(hasTestTag("header-text"))
                .assert(hasText("MyApp (Last order)"))
        }

        @Composable
        fun WidgetContent(status: Boolean) {
            Column {
                Header()
                Spacer()
                StatusRow(status)
            }
        }

        @Composable
        fun Header() {
            val width = LocalSize.current.width
            Row(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = if (width > 50.dp) {
                        "MyApp (Last order)"
                    } else {
                        "MyApp"
                    },
                    modifier = GlanceModifier.semantics { testTag = "header-text" }
                )
            }
        }

        @Composable
        fun StatusRow(status: Boolean) {
            Row(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = "Status",
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Text(
                    text = if (status) {
                        "Pending"
                    } else {
                        "Finished"
                    },
                    modifier = GlanceModifier.semantics { testTag = "status-text" }
                )
            }
        }
    }
}
