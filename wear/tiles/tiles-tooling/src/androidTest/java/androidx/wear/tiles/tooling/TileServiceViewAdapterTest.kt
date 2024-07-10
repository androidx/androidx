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

package androidx.wear.tiles.tooling

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.wear.tiles.tooling.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val TEST_TILE_PREVIEWS_KOTLIN_FILE = "androidx.wear.tiles.tooling.TestTilePreviewsKt"
private const val TEST_TILE_PREVIEWS_JAVA_FILE = "androidx.wear.tiles.tooling.TestTilePreviews"

@RunWith(Parameterized::class)
class TileServiceViewAdapterTest(
    private val testFile: String,
) {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    private lateinit var tileServiceViewAdapter: TileServiceViewAdapter

    @Before
    fun setup() {
        tileServiceViewAdapter =
            activityTestRule.activity.findViewById(R.id.tile_service_view_adapter)
    }

    private fun initAndInflate(
        methodFqn: String,
    ) {
        activityTestRule.runOnUiThread {
            tileServiceViewAdapter.init(methodFqn)
            tileServiceViewAdapter.requestLayout()
        }
    }

    @Test
    fun testTilePreview() {
        initAndInflate("$testFile.tilePreview")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTileLayoutPreview() {
        initAndInflate("$testFile.tileLayoutPreview")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTileLayoutElementPreview() {
        initAndInflate("$testFile.tileLayoutElementPreview")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTilePreviewDeclaredWithPrivateMethod() {
        initAndInflate("$testFile.tilePreviewWithPrivateVisibility")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTilePreviewThatHasSharedFunctionName() {
        initAndInflate("$testFile.duplicateFunctionName")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTilePreviewWithContextParameter() {
        initAndInflate("$testFile.tilePreviewWithContextParameter")

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTileWithWrongReturnTypeIsNotInflated() {
        initAndInflate("$testFile.tilePreviewWithWrongReturnType")

        assertThatTileHasNotInflated()
    }

    @Test
    fun testTilePreviewWithNonContextParameterIsNotInflated() {
        initAndInflate("$testFile.tilePreviewWithNonContextParameter")

        assertThatTileHasNotInflated()
    }

    @Test
    fun testNonStaticPreviewMethodWithDefaultConstructor() {
        if (testFile == TEST_TILE_PREVIEWS_KOTLIN_FILE) {
            initAndInflate("androidx.wear.tiles.tooling.SomeClass.nonStaticMethod")
        } else {
            initAndInflate("$testFile.nonStaticMethod")
        }

        assertThatTileHasInflatedSuccessfully()
    }

    @Test
    fun testTilePreviewWithDefaultPlatformData() {
        initAndInflate("$testFile.tilePreviewWithDefaultPlatformData")

        assertThatTileHasInflatedSuccessfully(expectedText = "80")
    }

    @Test
    fun testTilePreviewWithOverriddenPlatformData() {
        initAndInflate("$testFile.tilePreviewWithOverriddenPlatformData")

        assertThatTileHasInflatedSuccessfully(expectedText = "180")
    }

    @Test
    fun testGetAnimations() {
        initAndInflate("$testFile.testGetAnimations")
        val animations = tileServiceViewAdapter.getAnimations()

        assertEquals(2, animations.size)
        assertEquals(2000L, animations[0].durationMs)
        assertEquals(2000L, animations[1].durationMs)
    }

    @Test
    fun testGetTerminalAndNotTerminalAnimation() {
        initAndInflate("$testFile.testGetTerminalAndNotTerminalAnimation")
        val animations = tileServiceViewAdapter.getAnimations()

        assertEquals(2, animations.size)
        assertEquals(false, animations[0].isTerminal)
        assertEquals(true, animations[1].isTerminal)
    }

    @Test
    fun testGetAnimationsWithCondition() {
        initAndInflate("$testFile.testGetAnimationsWithCondition")
        val animations = tileServiceViewAdapter.getAnimations()

        assertEquals(2, animations.size)
        assertEquals(false, animations[0].isTerminal)
        assertEquals(true, animations[1].isTerminal)
    }

    private fun assertThatTileHasInflatedSuccessfully(expectedText: String = "Hello world!") {
        activityTestRule.runOnUiThread {
            val textView =
                when (
                    val child = (tileServiceViewAdapter.getChildAt(0) as ViewGroup).getChildAt(0)
                ) {
                    is TextView -> child
                    // layout elements are wrapped with a FrameLayout
                    else -> (child as? FrameLayout)?.getChildAt(0) as? TextView
                }
            assertNotNull(textView)
            assertEquals(expectedText, textView?.text.toString())
        }
    }

    private fun assertThatTileHasNotInflated() {
        activityTestRule.runOnUiThread { assertEquals(0, tileServiceViewAdapter.childCount) }
    }

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun parameters() =
            listOf(
                TEST_TILE_PREVIEWS_KOTLIN_FILE,
                TEST_TILE_PREVIEWS_JAVA_FILE,
            )

        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.tile_service_adapter_test)
            }
        }
    }
}
