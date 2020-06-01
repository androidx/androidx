/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.test.rule.ActivityTestRule
import androidx.ui.tooling.preview.ComposeViewAdapter
import androidx.ui.tooling.preview.ViewInfo
import androidx.ui.tooling.test.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ComposeViewAdapterTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(TestActivity::class.java)

    private lateinit var composeViewAdapter: ComposeViewAdapter

    @Before
    fun setup() {
        composeViewAdapter =
            activityTestRule.activity.findViewById(R.id.compose_view_adapter)
    }

    /**
     * Asserts that the given Composable method executes correct and outputs some [ViewInfo]s.
     */
    private fun assertRendersCorrectly(className: String, methodName: String): List<ViewInfo> {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(className, methodName, debugViewInfos = true)
        }

        activityTestRule.runOnUiThread {
            assertTrue(composeViewAdapter.viewInfos.isNotEmpty())
        }

        return composeViewAdapter.viewInfos
    }

    @Test
    fun setClockTimeWithAnimationInspection() {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init("androidx.ui.tooling.SimpleComposablePreviewKt",
                "SimpleComposablePreview", animationClockStartTime = 0L)
        }

        activityTestRule.runOnUiThread {
            composeViewAdapter.setClockTime(100) // Sanity-check. The call should succeed.
        }
    }

    @Test
    fun setClockTimeWithoutAnimationInspection() {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init("androidx.ui.tooling.SimpleComposablePreviewKt",
                "SimpleComposablePreview")
        }

        activityTestRule.runOnUiThread {
            try {
                composeViewAdapter.setClockTime(100)
                fail("Expected to throw an Exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("This method is expected to be called from " +
                        "Android Studio via reflection, otherwise 'clock' is expected to be null."))
            }
        }
    }

    @Test
    fun instantiateComposeViewAdapter() {
        val viewInfos = assertRendersCorrectly(
            "androidx.ui.tooling.SimpleComposablePreviewKt",
            "SimpleComposablePreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "SimpleComposablePreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // Verify that valid line numbers are being recorded
            assertTrue(viewInfos.map { it.lineNumber }.all { it > 0 })
            // Verify that method names are being captured
            assertTrue(viewInfos.map { it.methodName }.all {
                it.startsWith("androidx.ui.tooling.")
            })
        }
    }

    @Test
    fun lineNumberMapping() {
        val viewInfos = assertRendersCorrectly(
            "androidx.ui.tooling.LineNumberPreviewKt",
            "LineNumberPreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "LineNumberPreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            assertArrayEquals(arrayOf(36, 37, 38, 40, 43, 44, 45),
                viewInfos
                    .map { it.lineNumber }
                    .sorted()
                    .distinct()
                    .toTypedArray())
        }
    }

    @Test
    fun instantiatePrivateComposeViewAdapter() {
        assertRendersCorrectly(
            "androidx.ui.tooling.SimpleComposablePreviewKt",
            "PrivateSimpleComposablePreview"
        )
    }

    @Test
    fun defaultParametersComposableTest1() {
        assertRendersCorrectly(
            "androidx.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview1"
        )
    }

    @Test
    fun defaultParametersComposableTest2() {
        assertRendersCorrectly(
            "androidx.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview2"
        )
    }

    @Test
    fun defaultParametersComposableTest3() {
        assertRendersCorrectly(
            "androidx.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview3"
        )
    }

    @Test
    fun previewInClass() {
        assertRendersCorrectly(
            "androidx.ui.tooling.TestGroup",
            "InClassPreview"
        )
    }

    companion object {
        class TestActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.compose_adapter_test)
            }
        }
    }
}