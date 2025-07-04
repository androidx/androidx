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

package androidx.compose.foundation.text.contextmenu.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.FakeTextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

private const val MESSAGE = "FakeTextContextMenuProvider.showTextContextMenu called unexpectedly."

class PlatformDefaultTextContextMenuProvidersTest {
    @get:Rule val rule = createComposeRule()

    private val fakeTextContextMenuProvider = FakeTextContextMenuProvider {
        throw AssertionError(MESSAGE)
    }

    @Test
    fun testProvidersCreated_noneDefined() =
        runTest(
            outerContent = { content ->
                content() // No CompositionLocals to provide, just call the content.
            }
        ) {
            assertToolbarInstanceOf<AndroidTextContextMenuToolbarProvider>()
            assertDropdownInstanceOf<BasicTextContextMenuProvider>()
            assertOnlyOneAddedLayout()
        }

    @Test
    fun testDropdownProviderCreated_toolbarDefined() =
        runTest(
            outerContent = { content ->
                CompositionLocalProvider(
                    LocalTextContextMenuDropdownProvider provides fakeTextContextMenuProvider,
                    content = content,
                )
            }
        ) {
            assertToolbarInstanceOf<AndroidTextContextMenuToolbarProvider>()
            assertDropdownInstanceOf<FakeTextContextMenuProvider>()
            assertOnlyOneAddedLayout()
        }

    @Test
    fun testToolbarProviderCreated_dropdownDefined() =
        runTest(
            outerContent = { content ->
                CompositionLocalProvider(
                    LocalTextContextMenuToolbarProvider provides fakeTextContextMenuProvider,
                    content = content,
                )
            }
        ) {
            assertDropdownInstanceOf<BasicTextContextMenuProvider>()
            assertToolbarInstanceOf<FakeTextContextMenuProvider>()
            assertOnlyOneAddedLayout()
        }

    @Test
    fun testNoProvidersCreated_bothDefined() =
        runTest(
            outerContent = { content ->
                CompositionLocalProvider(
                    LocalTextContextMenuToolbarProvider provides fakeTextContextMenuProvider,
                    LocalTextContextMenuDropdownProvider provides fakeTextContextMenuProvider,
                    content = content,
                )
            }
        ) {
            assertToolbarInstanceOf<FakeTextContextMenuProvider>()
            assertDropdownInstanceOf<FakeTextContextMenuProvider>()
            assertOnlyOneAddedLayout()
        }

    private fun runTest(
        outerContent: @Composable (content: @Composable () -> Unit) -> Unit,
        testBlock: TestScope.() -> Unit,
    ) {
        TestScope(outerContent).testBlock()
    }

    private inner class TestScope(
        outerContent: @Composable (content: @Composable () -> Unit) -> Unit
    ) {
        var toolbarProvider: TextContextMenuProvider? = null
        var dropdownProvider: TextContextMenuProvider? = null

        private val boxTag = "box"

        init {
            rule.setContent {
                outerContent {
                    ProvideDefaultPlatformTextContextMenuProviders {
                        toolbarProvider = LocalTextContextMenuToolbarProvider.current
                        dropdownProvider = LocalTextContextMenuDropdownProvider.current
                        Box(Modifier.testTag(boxTag))
                    }
                }
            }
        }

        fun assertOnlyOneAddedLayout() {
            val node = rule.onNodeWithTag(boxTag).fetchSemanticsNode()
            val depthCount = getLayoutDepth(node)
            // Root Layout -> Provider Layout -> Box Layout with our test tag.
            assertThat(depthCount).isEqualTo(3)
        }

        private fun getLayoutDepth(node: SemanticsNode): Int {
            var depthCount = 0
            var layoutInfo: LayoutInfo? = node.layoutInfo
            while (layoutInfo != null) {
                layoutInfo = layoutInfo.parentInfo
                depthCount++
            }
            return depthCount
        }

        inline fun <reified T> assertToolbarInstanceOf() {
            assertThat(toolbarProvider).isInstanceOf<T>()
        }

        inline fun <reified T> assertDropdownInstanceOf() {
            assertThat(dropdownProvider).isInstanceOf<T>()
        }

        private inline fun <reified T> Subject.isInstanceOf() {
            isNotNull()
            isInstanceOf(T::class.java)
        }
    }
}
