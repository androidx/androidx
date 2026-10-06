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

package androidx.photopicker.compose

import android.net.Uri
import android.os.Binder
import android.os.Build
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.photopicker.testing.TestEmbeddedPhotoPickerProvider
import androidx.photopicker.testing.TestEmbeddedPhotoPickerSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class EmbeddedPhotoPickerStateImplTest {
    private val WAIT_TIMEOUT_DURATION_MILLIS = 5_000L
    private val TestEmbeddedPhotoPickerProvider.lastTestSession: TestEmbeddedPhotoPickerSession
        get() = sessions.last() as TestEmbeddedPhotoPickerSession

    // Share a single TestDispatcher between the compose test rule effect context and runTest
    // to ensure Compose coroutine effects and test coroutines run on the same test scheduler.
    private val testDispatcher = StandardTestDispatcher()
    @get:Rule
    val composeTestRule = createComposeRule(ComposeUiTestConfig(effectContext = testDispatcher))

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplThrowsSessionError() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val deferredError = CompletableDeferred<Throwable>()
            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState(onError = { deferredError.complete(it) })
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            val throwable = RuntimeException("Test")
            session.notifySessionError(throwable)

            val error = deferredError.await()
            assertThat(error).isNotNull()
            assertThat(error).isEqualTo(throwable)
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testRunSessionDispatchesSessionErrorToCallbackAndExitsCleanly() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val state = EmbeddedPhotoPickerStateImpl(false, emptySet())
            state.surfaceHostToken = Binder()
            state.displayId = 0
            state.notifyResized(IntSize(100, 100))

            val throwable = RuntimeException("Direct session error")
            var capturedError: Throwable? = null
            state.clientCallbacks.onError = { capturedError = it }

            // Launch runSession directly; it should complete normally without throwing
            val sessionJob =
                launch(Dispatchers.Main) { state.runSession(testProvider, DEFAULT_FEATURE_INFO) }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            testProvider.lastTestSession.notifySessionError(throwable)

            // sessionJob should complete cleanly rather than failing with an exception
            sessionJob.join()
            assertThat(sessionJob.isCompleted).isTrue()
            assertThat(sessionJob.isCancelled).isFalse()

            // Session error is dispatched to onError callback and surfacePackage is cleared
            assertThat(capturedError).isEqualTo(throwable)
            assertThat(state.surfacePackage).isNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplEmitsSurfacePackage() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState()
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) { state.surfacePackage != null }
            assertThat(state.surfacePackage).isNotNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplClosesSessionWhenRunSessionIsCancelled() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            var showPicker by mutableStateOf(true)
            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                if (showPicker) {
                    state = rememberEmbeddedPhotoPickerState()
                    EmbeddedPhotoPicker(state = state, provider = testProvider)
                }
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            composeTestRule.runOnUiThread { showPicker = false }
            composeTestRule.waitForIdle()

            assertThat(session.isClosed).isTrue()
            assertThat(state.surfacePackage).isNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplDoesNotEmitErrorOnRunSessionCancellation() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            var capturedError: Throwable? = null
            var showPicker by mutableStateOf(true)

            composeTestRule.setContent {
                if (showPicker) {
                    val state = rememberEmbeddedPhotoPickerState(onError = { capturedError = it })
                    EmbeddedPhotoPicker(state = state, provider = testProvider)
                }
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }

            composeTestRule.runOnUiThread { showPicker = false }
            composeTestRule.waitForIdle()

            assertThat(capturedError).isNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplEmitsSelections() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val grantedUris = mutableListOf<Uri>()

            composeTestRule.setContent {
                EmbeddedPhotoPicker(
                    state =
                        rememberEmbeddedPhotoPickerState(
                            onUriPermissionGranted = { grantedUris.addAll(it) }
                        ),
                    provider = testProvider,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            assertThat(grantedUris).isEmpty()

            val uri1 = Uri.fromParts("content", "1234", null)
            val uri2 = Uri.fromParts("content", "4567", null)
            val uri3 = Uri.fromParts("content", "8900", null)
            val uri4 = Uri.fromParts("content", "9999", null)

            session.selectUris(listOf(uri1, uri2))
            assertThat(grantedUris).containsExactly(uri1, uri2)

            session.selectUris(listOf(uri3, uri4))
            assertThat(grantedUris).containsExactly(uri1, uri2, uri3, uri4)
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplEmitsDeSelections() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            val deselectedUris = mutableListOf<Uri>()

            composeTestRule.setContent {
                EmbeddedPhotoPicker(
                    state =
                        rememberEmbeddedPhotoPickerState(
                            onUriPermissionRevoked = { deselectedUris.addAll(it) }
                        ),
                    provider = testProvider,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            assertThat(deselectedUris).isEmpty()

            val uri1 = Uri.fromParts("content", "1234", null)

            session.selectUris(listOf(uri1))
            session.deselectUris(listOf(uri1))
            assertThat(deselectedUris).containsExactly(uri1)
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplEmitsSelectionComplete() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val deferredSelectionComplete = CompletableDeferred<Boolean>()

            composeTestRule.setContent {
                EmbeddedPhotoPicker(
                    state =
                        rememberEmbeddedPhotoPickerState(
                            onSelectionComplete = { deferredSelectionComplete.complete(true) }
                        ),
                    provider = testProvider,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            session.notifySelectionComplete()
            assertThat(deferredSelectionComplete.await()).isTrue()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplThrowsWhenNotReady() =
        runTest(testDispatcher) {
            lateinit var state: EmbeddedPhotoPickerState

            // No SurfaceView present, so the hostToken and surface size won't get set.
            composeTestRule.setContent { state = rememberEmbeddedPhotoPickerState() }

            assertThrows(AssertionError::class.java) {
                runBlocking {
                    state.runSession(
                        provider = TestEmbeddedPhotoPickerProvider.get(),
                        featureInfo = EmbeddedPhotoPickerFeatureInfo.Builder().build(),
                    )
                }
            }
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplOpensWithPreselectedUris() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val initialMediaSelection = Uri.parse("content://media/picker/1")
            val preSelectedMedia = Uri.parse("content://media/picker/2")
            val featureInfo =
                EmbeddedPhotoPickerFeatureInfo.Builder()
                    .setPreSelectedUris(listOf(preSelectedMedia))
                    .build()

            composeTestRule.setContent {
                val state =
                    rememberEmbeddedPhotoPickerState(
                        initialMediaSelection = setOf(initialMediaSelection)
                    )
                EmbeddedPhotoPicker(
                    state = state,
                    provider = testProvider,
                    embeddedPhotoPickerFeatureInfo = featureInfo,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            assertThat(session.selectedUris)
                .containsExactly(initialMediaSelection, preSelectedMedia)
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplProgrammaticDeselection() =
        runTest(testDispatcher) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            val uri1 = Uri.parse("content://media/picker/1")

            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState(initialMediaSelection = setOf(uri1))
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            assertThat(session.selectedUris).containsExactly(uri1)

            state.deselectUri(uri1)

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                state.selectedMedia.isEmpty()
            }
            assertThat(state.selectedMedia).isEmpty()
            assertThat(session.selectedUris).isEmpty()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerStateImplDeselectBeforeSessionOpenedThrows() =
        runTest(testDispatcher) {
            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent { state = rememberEmbeddedPhotoPickerState() }

            val uri = Uri.parse("content://media/picker/1")
            assertThrows(IllegalStateException::class.java) {
                runBlocking { state.deselectUri(uri) }
            }
        }
}
