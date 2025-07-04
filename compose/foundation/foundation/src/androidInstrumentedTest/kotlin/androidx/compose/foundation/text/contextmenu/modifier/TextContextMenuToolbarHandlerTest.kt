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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.assertItems
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import org.junit.Rule
import org.junit.Test

private val DefaultRect = Rect(Offset.Zero, Size(10f, 10f))

class TextContextMenuToolbarHandlerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun whenCallShow_providerCalled() {
        val toolbarRequester = ToolbarRequesterImpl()

        lateinit var coordinates: LayoutCoordinates
        var callCount = 0

        val fakeProvider = TestTextContextMenuProvider {
            callCount++
            val actualContentBounds = it.contentBounds(coordinates)
            assertThat(actualContentBounds).isEqualTo(DefaultRect)
            it.data().assertItems(listOf(1))
        }

        rule.setContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides fakeProvider) {
                Box(
                    Modifier.onGloballyPositioned { coordinates = it }
                        .appendTextContextMenuComponents { testNumberItem(1) }
                        .textContextMenuToolbarHandler(
                            requester = toolbarRequester,
                            computeContentBounds = { destinationCoordinates ->
                                assertThat(destinationCoordinates).isSameInstanceAs(coordinates)
                                DefaultRect
                            },
                        )
                )
            }
        }

        toolbarRequester.show()

        assertCompletesSuccessfully { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun whenShow_jobKickedOffAndCompletesGracefully() {
        val toolbarRequester = ToolbarRequesterImpl()
        val channel = Channel<Unit>()

        var callCount = 0
        val fakeProvider = TestTextContextMenuProvider {
            callCount++
            channel.receive()
        }

        rule.setContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides fakeProvider) {
                Box(
                    Modifier.textContextMenuToolbarHandler(
                        requester = toolbarRequester,
                        computeContentBounds = { DefaultRect },
                    )
                )
            }
        }

        toolbarRequester.show()

        rule.waitUntil { callCount > 0 }
        assertActive { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)

        channel.trySend(Unit)
        assertCompletesSuccessfully { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun whenCallHide_providerCancelled() {
        val toolbarRequester = ToolbarRequesterImpl()

        var callCount = 0
        val fakeProvider = TestTextContextMenuProvider {
            callCount++
            awaitCancellation()
        }

        rule.setContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides fakeProvider) {
                Box(
                    Modifier.textContextMenuToolbarHandler(
                        requester = toolbarRequester,
                        computeContentBounds = { DefaultRect },
                    )
                )
            }
        }

        toolbarRequester.show()

        rule.waitUntil { callCount > 0 }
        assertActive { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)

        toolbarRequester.hide()
        assertCancels { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun whenRegularCall_onShowAndOnHideInvoked() {
        val toolbarRequester = ToolbarRequesterImpl()

        var callCount = 0
        var showCallCount = 0
        var hideCallCount = 0

        val fakeProvider = TestTextContextMenuProvider {
            callCount++
            awaitCancellation()
        }

        rule.setContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides fakeProvider) {
                Box(
                    Modifier.textContextMenuToolbarHandler(
                        requester = toolbarRequester,
                        onShow = { showCallCount++ },
                        onHide = { hideCallCount++ },
                        computeContentBounds = { DefaultRect },
                    )
                )
            }
        }

        toolbarRequester.show()

        rule.waitUntil { callCount > 0 }
        assertActive { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
        assertThat(showCallCount).isEqualTo(1)
        assertThat(hideCallCount).isEqualTo(0)

        toolbarRequester.hide()
        assertCancels { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
        assertThat(showCallCount).isEqualTo(1)
        assertThat(hideCallCount).isEqualTo(1)
    }

    @Test
    fun whenNeverAttached_showThrows() {
        val toolbarRequester = ToolbarRequesterImpl()
        assertFailsWith(IllegalStateException::class) { toolbarRequester.show() }
    }

    @Test
    fun whenNeverAttached_hideDoesNotThrow() {
        val toolbarRequester = ToolbarRequesterImpl()
        toolbarRequester.hide()
    }

    @Test
    fun whenDetached_showThrows() {
        val toolbarRequester = ToolbarRequesterImpl()
        var show by mutableStateOf(true)
        rule.setContent {
            val modifier =
                if (show) {
                    Modifier.textContextMenuToolbarHandler(toolbarRequester) { DefaultRect }
                } else {
                    Modifier
                }
            Box(modifier)
        }

        show = false
        rule.waitForIdle()

        assertFailsWith(IllegalStateException::class) { toolbarRequester.show() }
    }

    @Test
    fun whenDetached_hideDoesNotThrow() {
        val toolbarRequester = ToolbarRequesterImpl()
        var show by mutableStateOf(true)
        rule.setContent {
            val modifier =
                if (show) {
                    Modifier.textContextMenuToolbarHandler(toolbarRequester) { DefaultRect }
                } else {
                    Modifier
                }
            Box(modifier)
        }

        show = false
        rule.waitForIdle()

        toolbarRequester.hide()
    }

    @Test
    fun whenReattached_showAndHideSucceed() {
        val toolbarRequester = ToolbarRequesterImpl()

        var callCount = 0
        val fakeProvider = TestTextContextMenuProvider {
            callCount++
            awaitCancellation()
        }

        var show by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides fakeProvider) {
                val modifier =
                    if (show) {
                        Modifier.textContextMenuToolbarHandler(toolbarRequester) { DefaultRect }
                    } else {
                        Modifier
                    }
                Box(modifier)
            }
        }

        show = false
        rule.waitForIdle()

        assertFailsWith(IllegalStateException::class) { toolbarRequester.show() }

        show = true
        rule.waitForIdle()

        toolbarRequester.show()

        rule.waitUntil { callCount > 0 }
        assertActive { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)

        toolbarRequester.hide()
        assertCancels { fakeProvider.previousJob }
        assertThat(callCount).isEqualTo(1)
    }

    private fun assertCompletesSuccessfully(jobBlock: () -> Job?) {
        assertWithMessage("Coroutine was unexpectedly cancelled.")
            .that(assertCompletes(jobBlock).isCancelled)
            .isFalse()
    }

    private fun assertCancels(jobBlock: () -> Job?) {
        assertWithMessage("Coroutine was unexpectedly NOT cancelled.")
            .that(assertCompletes(jobBlock).isCancelled)
            .isTrue()
    }

    private fun assertCompletes(jobBlock: () -> Job?): Job {
        var job: Job? = null
        rule.waitUntil("Coroutine never completes.") {
            job = jobBlock()
            job?.isCompleted == true
        }
        return job!!
    }

    private fun assertActive(jobBlock: () -> Job?) {
        assertWithMessage("Coroutine was unexpectedly NOT active.")
            .that(jobBlock()?.isActive == true)
            .isTrue()
    }
}

private class TestTextContextMenuProvider(
    private val onShow: suspend (TextContextMenuDataProvider) -> Unit
) : TextContextMenuProvider {

    var previousJob: Job? = null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        previousJob = currentCoroutineContext().job
        onShow(dataProvider)
    }
}

private fun TextContextMenuBuilderScope.testNumberItem(i: Int) {
    item(key = i, label = "$i") { /* Nothing */ }
}
