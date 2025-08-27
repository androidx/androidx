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

@file:Suppress("INVISIBLE_REFERENCE") // b/407931696

package androidx.compose.ui.benchmark.contentcapture

import android.view.View
import androidx.activity.ComponentActivity
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.benchmark.input.pointer.TestActivity
import androidx.compose.ui.contentcapture.AndroidContentCaptureManager
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidContentCaptureBenchmark {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule =
        androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = BenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var androidComposeView: View
    private lateinit var contentCaptureManager: AndroidContentCaptureManager
    private val layoutNodeList = List(10) { LayoutNode() }
    private val semanticsConfig = SemanticsConfiguration()

    @OptIn(ExperimentalComposeUiApi::class)
    @Before
    fun setup() {
        rule.setContent {
            semanticsConfig[SemanticsProperties.Text] = listOf(AnnotatedString("test"))
            androidComposeView = LocalView.current as AndroidComposeView
            contentCaptureManager =
                AndroidContentCaptureManager(
                    androidComposeView,
                    { FakeContentCaptureSession(androidComposeView) },
                )
            contentCaptureManager.onStart(androidComposeView.findViewTreeLifecycleOwner())
            for (node in layoutNodeList) {
                node.attach(androidComposeView)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun semanticsAdded() {
        benchmarkRule.measureRepeated {
            for (node in layoutNodeList) {
                contentCaptureManager.onSemanticsAdded(node)
            }
            contentCaptureManager.sendPendingContentCaptureEvents()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun semanticsRemoved() {
        benchmarkRule.measureRepeated {
            for (node in layoutNodeList) {
                contentCaptureManager.onSemanticsRemoved(node, null)
            }
            contentCaptureManager.sendPendingContentCaptureEvents()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun semanticsChanged() {
        benchmarkRule.measureRepeated {
            for (node in layoutNodeList) {
                contentCaptureManager.onSemanticsChanged(node, semanticsConfig)
            }
            contentCaptureManager.sendPendingContentCaptureEvents()
        }
    }
}
