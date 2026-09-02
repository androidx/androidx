/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1AudioPlayerTest {

    var capturedUrl: String? = null
    var capturedDescription: String? = null

    private val fakeAudioRenderer = A2uiAudioPlayerRenderer { url, description, modifier, onError ->
        capturedUrl = url
        capturedDescription = description
        Box(modifier = modifier.size(100.dp, 40.dp)) {
            Box(
                modifier =
                    Modifier.matchParentSize().semantics {
                        testTag = AudioPlayerPlaceholderTestTag
                        if (description != null) {
                            this.contentDescription = description
                        }
                    }
            )
        }

        if (url == "error") {
            SideEffect(url) { onError(null) }
        } else if (url == "error_with_throwable") {
            SideEffect(url) { onError(IllegalStateException("Network failure")) }
        } else if (url == "error_with_blank_throwable") {
            SideEffect(url) { onError(RuntimeException("")) }
        }
    }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1AudioPlayer(fakeAudioRenderer)),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun url_staticUrl_rendersAudioPlayer() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://example.com/audio.mp3"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/audio.mp3")
    }

    @Test
    fun url_functionExpression_rendersAudioPlayer() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "https://example.com/\${/media/id}.mp3"),
                            )
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("id" to "track_42")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/track_42.mp3")
    }

    @Test
    fun url_componentPayloadUpdate_updatesAudioPlayer() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://example.com/audio.mp3"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/audio.mp3")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://example.com/audio2.mp3"),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/audio2.mp3")
    }

    @Test
    fun url_componentPayloadUpdate_switchesFromStaticToDynamicBinding() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://example.com/static.mp3"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://example.com/dynamic.mp3")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/static.mp3")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to mapOf("path" to "/media/url")),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp3")
    }

    @Test
    fun url_dynamicBinding_externalDataChange_updatesAudioPlayer() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to mapOf("path" to "/media/track")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("track" to "https://example.com/dynamic.mp3")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp3")

        controller.updateData("/media/track", "https://example.com/dynamic2.mp3")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic2.mp3")
    }

    @Test
    fun isReady_unresolvedProperties_remainsInLoadingStateUntilDataArrives() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to mapOf("path" to "/media/track")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Box(modifier = modifier) { Box(Modifier.semantics { testTag = "Loading" }) }
                    },
                )
            }
        }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertDoesNotExist()

        controller.updateData("/media/track", "https://example.com/dynamic.mp3")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp3")
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to mapOf("path" to "/media/track")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("track" to "https://example.com/audio.mp3")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Box(modifier = modifier.size(48.dp).semantics { testTag = "Loading" })
                    },
                )
            }
        }

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
        onNodeWithTag("Loading").assertDoesNotExist()

        controller.updateData("/media/track", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertDoesNotExist()
        onNodeWithTag("Loading").assertIsDisplayed()
    }

    @Test
    fun description_staticDescription_setsContentDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://example.com/audio.mp3",
                        "description" to "Track Description",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Track Description")
        assertThat(capturedDescription).isEqualTo("Track Description")
    }

    @Test
    fun description_componentPayloadUpdate_updatesContentDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://example.com/audio.mp3",
                        "description" to "Initial Description",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Initial Description")
        assertThat(capturedDescription).isEqualTo("Initial Description")

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "url" to "https://example.com/audio.mp3",
                    "description" to "Updated Description",
                ),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Updated Description")
        assertThat(capturedDescription).isEqualTo("Updated Description")
    }

    @Test
    fun description_dynamicBinding_updatesContentDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://example.com/audio.mp3",
                        "description" to mapOf("path" to "/media/title"),
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.updateData("/media/title", "Podcast Episode 1")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assertContentDescriptionEquals("Podcast Episode 1")
    }

    @Test
    fun description_dynamicBindingRemoved_removesContentDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://example.com/audio.mp3",
                        "description" to mapOf("path" to "/media/title"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("title" to "Podcast Episode 1")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assertContentDescriptionEquals("Podcast Episode 1")
        assertThat(capturedDescription).isEqualTo("Podcast Episode 1")

        controller.updateData("/media/title", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(AudioPlayerPlaceholderTestTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        assertThat(capturedDescription).isNull()
    }

    @Test
    fun modifier_parentModifier_isApplied() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://example.com/audio.mp3"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("wrapper"))
            }
        }

        onNodeWithTag("wrapper").assertIsDisplayed()
        onNodeWithTag(AudioPlayerPlaceholderTestTag).assertIsDisplayed()
    }

    @Test
    fun modifier_parameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://example.com/audio.mp3"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { MaterialTheme { A2uiTestSurface(surface = surface, modifier = modifier) } }

        onNodeWithTag("initial_tag").assertIsDisplayed()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNodeWithTag("initial_tag").assertDoesNotExist()
        onNodeWithTag("updated_tag").assertIsDisplayed()
    }

    @Test
    fun validation_missingRequiredUrl_throwsValidationException() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(id = "root", type = "AudioPlayer", properties = emptyMap())

        try {
            val controller =
                A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
            controller.start()
        } catch (e: Exception) {
            assert(e is A2uiException)
            return@runComposeUiTest
        }

        throw AssertionError("Should have thrown validation exception")
    }

    @Test
    fun renderer_error_reportsError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "AudioPlayer",
                            properties = mapOf("url" to "error"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface,
                    onError = { _, _ -> }, // Override onError to avoid throwing if root fails
                )
            }
        }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message).isEqualTo("Audio loading error from renderer: error")
    }

    @Test
    fun renderer_errorWithThrowable_reportsErrorWithMessage() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "AudioPlayer",
                            properties = mapOf("url" to "error_with_throwable"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface,
                    onError = { _, _ -> }, // Override onError to avoid throwing if root fails
                )
            }
        }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .isEqualTo(
                "Audio loading error from renderer for error_with_throwable: Network failure"
            )
    }

    @Test
    fun renderer_errorWithBlankThrowableMessage_reportsDefaultError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "AudioPlayer",
                            properties = mapOf("url" to "error_with_blank_throwable"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface,
                    onError = { _, _ -> }, // Override onError to avoid throwing if root fails
                )
            }
        }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .isEqualTo("Audio loading error from renderer: error_with_blank_throwable")
    }
}

private const val AudioPlayerPlaceholderTestTag = "AudioPlayerPlaceholder"
