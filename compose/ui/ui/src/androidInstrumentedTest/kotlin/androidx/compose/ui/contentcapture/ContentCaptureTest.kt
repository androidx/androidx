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

package androidx.compose.ui.contentcapture

import android.os.Build
import android.util.LongSparseArray
import android.view.ViewStructure
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponseValue
import android.view.translation.ViewTranslationRequest
import android.view.translation.ViewTranslationRequest.ID_TEXT
import android.view.translation.ViewTranslationResponse
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.coreshims.ContentCaptureSessionCompat
import androidx.compose.ui.platform.coreshims.ViewStructureCompat
import androidx.compose.ui.semantics.clearTextSubstitution
import androidx.compose.ui.semantics.isShowingTextSubstitution
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setTextSubstitution
import androidx.compose.ui.semantics.showTextSubstitution
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSubstitution
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnDetach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

// TODO(mnuzen): move this test file to a sub package androidx.compose.ui.contentcapture
@MediumTest
@SdkSuppress(minSdkVersion = 31)
@RunWith(AndroidJUnit4::class)
class ContentCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val tag = "tag"
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var contentCaptureSessionCompat: ContentCaptureSessionCompat
    private lateinit var viewStructureCompat: ViewStructureCompat
    private val contentCaptureEventLoopIntervalMs = 100L

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testInitContentCaptureSemanticsStructureChangeEvents_onStart() {
        // Arrange.
        rule.setContentWithContentCaptureEnabled(retainInteractionsDuringInitialization = true) {}

        // Act - Wait for initialization that is triggered by onStart().

        // Assert = verify the root node appeared.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat).newVirtualViewStructure(any(), any())
            verify(contentCaptureSessionCompat).notifyViewsAppeared(any())
            verify(viewStructureCompat).setDimens(any(), any(), any(), any(), any(), any())
            verify(viewStructureCompat).extras
            verify(viewStructureCompat).toViewStructure()
            verifyNoMoreInteractions(contentCaptureSessionCompat)
            verifyNoMoreInteractions(viewStructureCompat)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testInitContentCaptureSemanticsStructureChangeEvents_onStop() {
        // Arrange.
        rule.setContentWithContentCaptureEnabled {}

        // Act.
        rule.runOnIdle {
            androidComposeView.doOnDetach {

                // Assert.
                verify(contentCaptureSessionCompat).notifyViewsDisappeared(any())
                verifyNoMoreInteractions(contentCaptureSessionCompat)
                verifyNoMoreInteractions(viewStructureCompat)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_appeared() {
        // Arrange.
        var appeared by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Row(Modifier.size(100.dp).semantics {}) {
                if (appeared) {
                    Box(
                        Modifier.size(10.dp).semantics {
                            text = AnnotatedString("foo")
                            testTag = "testTagFoo"
                        }
                    )
                    Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") })
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }
        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<CharSequence>()) {
                verify(viewStructureCompat, times(2)).setText(capture())
                assertThat(firstValue).isEqualTo("foo")
                assertThat(secondValue).isEqualTo("bar")
            }
            with(argumentCaptor<String>()) {
                verify(viewStructureCompat, times(1)).setId(anyInt(), isNull(), isNull(), capture())
                assertThat(firstValue).isEqualTo("testTagFoo")
            }
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(2)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_disappeared() {
        // Arrange.
        var disappeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            if (!disappeared) {
                Row(Modifier.size(100.dp).semantics {}) {
                    Box(Modifier.size(10.dp).semantics {})
                    Box(Modifier.size(10.dp).semantics {})
                }
            }
        }

        // Act.
        rule.runOnIdle { disappeared = true }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<LongArray>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsDisappeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
            verify(contentCaptureSessionCompat, times(0)).notifyViewsAppeared(any())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_appearedAndDisappeared() {
        // Arrange.
        var appeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            if (appeared) {
                Row(Modifier.size(100.dp).semantics {}) {
                    Box(Modifier.size(10.dp).semantics {})
                    Box(Modifier.size(10.dp).semantics {})
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }
        // TODO(b/272068594): This test was written to ensure that if the items appeared and
        //  disappeared before the 100ms, it would still report the items that were added and the
        //  items that were removed The items were (As long as the items had different IDs). However
        //  it is not possible for a items with different IDs to disappear as they are not existing.
        //  The mocks also limit us to write this test since we can't mock AutofillIDs since
        //  AutofillId is a final class, and these tests just use the autofill id of the parent
        //  view.
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
        rule.runOnIdle { appeared = false }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for
        //  two invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<LongArray>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsDisappeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_sameNodeAppearedThenDisappeared() {
        // Arrange.
        var appeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics {}) {
                if (appeared) {
                    Box(Modifier.size(10.dp).semantics {})
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(1)
            }
            clearInvocations(contentCaptureSessionCompat)
        }

        rule.runOnIdle { appeared = false }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            verify(contentCaptureSessionCompat, times(0)).notifyViewsAppeared(any())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testUpdateTranslationOnAppeared_showOriginal() {
        // Arrange.
        var appeared by mutableStateOf(false)
        var result = true

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics {}) {
                if (appeared) {
                    Box(
                        Modifier.size(10.dp).semantics {
                            text = AnnotatedString("foo")
                            isShowingTextSubstitution = true
                            showTextSubstitution {
                                result = it
                                true
                            }
                        }
                    )
                }
            }
        }
        rule.runOnIdle { androidComposeView.contentCaptureManager.onHideTranslation() }

        // Act.
        rule.runOnIdle { appeared = true }
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(result).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testUpdateTranslationOnAppeared_showTranslated() {
        // Arrange.
        var appeared by mutableStateOf(false)
        var result = false

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics {}) {
                if (appeared) {
                    Box(
                        Modifier.size(10.dp).semantics {
                            text = AnnotatedString("foo")
                            isShowingTextSubstitution = false
                            showTextSubstitution {
                                result = it
                                true
                            }
                        }
                    )
                }
            }
        }
        rule.runOnIdle { androidComposeView.contentCaptureManager.onShowTranslation() }

        // Act.
        rule.runOnIdle { appeared = true }
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnCreateVirtualViewTranslationRequests() {
        // Arrange.
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = tag
                        text = AnnotatedString("foo")
                    }
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        val ids = LongArray(1).apply { this[0] = virtualViewId.toLong() }
        val requestsCollector: Consumer<ViewTranslationRequest?> = mock()

        // Act.
        rule.runOnIdle {
            androidComposeView.onCreateVirtualViewTranslationRequests(
                ids,
                IntArray(0),
                requestsCollector
            )
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<ViewTranslationRequest>()) {
                verify(requestsCollector).accept(capture())
                assertThat(firstValue)
                    .isEqualTo(
                        ViewTranslationRequest.Builder(
                                androidComposeView.autofillId,
                                virtualViewId.toLong()
                            )
                            .setValue(
                                ID_TEXT,
                                TranslationRequestValue.forText(AnnotatedString("foo"))
                            )
                            .build()
                    )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnVirtualViewTranslationResponses() {
        // Arrange.
        var result: AnnotatedString? = null
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = tag
                        text = AnnotatedString("foo")
                        setTextSubstitution {
                            result = it
                            true
                        }
                    }
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        rule.runOnIdle {
            androidComposeView.onVirtualViewTranslationResponses(
                LongSparseArray<ViewTranslationResponse?>().apply {
                    append(
                        virtualViewId.toLong(),
                        ViewTranslationResponse.Builder(androidComposeView.autofillId)
                            .setValue(
                                ID_TEXT,
                                TranslationResponseValue.Builder(0).setText("bar").build()
                            )
                            .build()
                    )
                }
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(result).isEqualTo(AnnotatedString("bar")) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnShowTranslation() {
        // Arrange.
        var result = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier.size(10.dp).semantics {
                        textSubstitution = AnnotatedString("foo")
                        isShowingTextSubstitution = false
                        showTextSubstitution {
                            result = it
                            true
                        }
                    }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.contentCaptureManager.onShowTranslation() }

        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnHideTranslation() {
        // Arrange.
        var result = true
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier.size(10.dp).semantics {
                        text = AnnotatedString("bar")
                        textSubstitution = AnnotatedString("foo")
                        isShowingTextSubstitution = true
                        showTextSubstitution {
                            result = it
                            true
                        }
                    }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.contentCaptureManager.onHideTranslation() }
        // Assert.
        rule.runOnIdle { assertThat(result).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnClearTranslation() {
        // Arrange.
        var result = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier.size(10.dp).semantics {
                        text = AnnotatedString("bar")
                        isShowingTextSubstitution = true
                        clearTextSubstitution {
                            result = true
                            true
                        }
                    }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.contentCaptureManager.onClearTranslation() }
        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.setContentWithContentCaptureEnabled(
        retainInteractionsDuringInitialization: Boolean = false,
        content: @Composable () -> Unit
    ) {
        contentCaptureSessionCompat = mock()
        viewStructureCompat = mock()
        val viewStructure: ViewStructure = mock()

        whenever(contentCaptureSessionCompat.newVirtualViewStructure(any(), any()))
            .thenReturn(viewStructureCompat)
        whenever(viewStructureCompat.toViewStructure()).thenReturn(viewStructure)

        // TODO(mnuzen): provideContentCaptureManager as a compositionLocal so that instead of
        //  casting view and getting ContentCaptureManager, retrieve it via
        //  `LocalContentCaptureManager.current`
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            androidComposeView.contentCaptureManager.onContentCaptureSession = {
                contentCaptureSessionCompat
            }

            whenever(contentCaptureSessionCompat.newAutofillId(any())).thenAnswer {
                androidComposeView.autofillId
            }

            content()
        }

        // Advance the clock past the first accessibility event loop, and clear the initial
        // as we are want the assertions to check the events that were generated later.
        runOnIdle { mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs) }

        runOnIdle {
            if (!retainInteractionsDuringInitialization) {
                clearInvocations(contentCaptureSessionCompat, viewStructureCompat)
            }
        }
    }

    // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction directly.
    private val SemanticsNodeInteraction.semanticsId: Int
        get() = fetchSemanticsNode().id
}
