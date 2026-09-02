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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1AudioPlayerUiTest {

    private val testAudioPlayer =
        object : A2uiBasicCatalogV1.AudioPlayer {
            var capturedUrl: String? = null
            var capturedDescription: String? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                url: String,
                description: String?,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedUrl = url
                    capturedDescription = description
                }
                val descText = description ?: "no-desc"
                BasicText(text = "Audio: $url - $descText", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testAudioPlayer),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to mapOf("path" to "/pendingUrl")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading Audio...", modifier = modifier) },
            )
        }

        onNodeWithText("Audio: https://test.audio - no-desc").assertDoesNotExist()
        onNodeWithText("Loading Audio...").assertIsDisplayed()

        controller.updateData("/pendingUrl", "https://test.audio")
        controller.waitForIdle()

        onNodeWithText("Loading Audio...").assertDoesNotExist()
        onNodeWithText("Audio: https://test.audio - no-desc").assertIsDisplayed()
    }

    @Test
    fun isReady_pendingOptionalDynamicDescription_returnsTrueAndRendersWithNullDescription() =
        runComposeUiTest {
            val payload =
                A2uiComponentPayload(
                    id = "root",
                    type = "AudioPlayer",
                    properties =
                        mapOf(
                            "url" to "https://test.audio",
                            "description" to mapOf("path" to "/pendingDesc"),
                        ),
                )
            val controller =
                A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
            val surface = controller.start()

            setContent {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
                )
            }

            onNodeWithText("Loading...").assertDoesNotExist()
            onNodeWithText("Audio: https://test.audio - no-desc").assertIsDisplayed()
            assertThat(testAudioPlayer.capturedDescription).isNull()

            controller.updateData("/pendingDesc", "Now Available")
            controller.waitForIdle()

            onNodeWithText("Audio: https://test.audio - Now Available").assertIsDisplayed()
            assertThat(testAudioPlayer.capturedDescription).isEqualTo("Now Available")
        }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
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
                initialData = mapOf("media" to mapOf("track" to "https://audio.start")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Audio: https://audio.start - no-desc").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/media/track", null)
        controller.waitForIdle()

        onNodeWithText("Audio: https://audio.start - no-desc").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_emptyStaticUrl_returnsTrueAndRendersContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(id = "root", type = "AudioPlayer", properties = mapOf("url" to ""))
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio:  - no-desc").assertIsDisplayed()
    }

    @Test
    fun content_staticData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf("url" to "https://static.audio", "description" to "A static track"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://static.audio - A static track").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://static.audio")
        assertThat(testAudioPlayer.capturedDescription).isEqualTo("A static track")
    }

    @Test
    fun content_dynamicData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to mapOf("path" to "/media/url"),
                        "description" to mapOf("path" to "/media/title"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData =
                    mapOf(
                        "media" to mapOf("url" to "https://dyn.audio", "title" to "Dynamic Title")
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://dyn.audio - Dynamic Title").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://dyn.audio")
        assertThat(testAudioPlayer.capturedDescription).isEqualTo("Dynamic Title")
    }

    @Test
    fun content_omittedOptionalProperties_fallsBackToDefaults() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://default.audio"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://default.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedDescription).isNull()
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "https://cdn.audio/\${/media/id}.mp3"),
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

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://cdn.audio/track_42.mp3 - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://cdn.audio/track_42.mp3")
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://tagged.audio"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Audio: https://tagged.audio - no-desc") and hasTestTag("custom_tag"))
            .assertIsDisplayed()
    }

    @Test
    fun content_urlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://old.audio"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://old.audio - no-desc").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("url" to "https://new.audio"))
        controller.waitForIdle()

        onNodeWithText("Audio: https://old.audio - no-desc").assertDoesNotExist()
        onNodeWithText("Audio: https://new.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://new.audio")
    }

    @Test
    fun content_dynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to mapOf("path" to "/media/url")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://initial.audio")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://initial.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://initial.audio")

        controller.updateData("/media/url", "https://updated.audio")
        controller.waitForIdle()

        onNodeWithText("Audio: https://initial.audio - no-desc").assertDoesNotExist()
        onNodeWithText("Audio: https://updated.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://updated.audio")
    }

    @Test
    fun content_staticToDynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://static.audio"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://dynamic.audio")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://static.audio - no-desc").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to mapOf("path" to "/media/url")),
        )
        controller.waitForIdle()

        onNodeWithText("Audio: https://static.audio - no-desc").assertDoesNotExist()
        onNodeWithText("Audio: https://dynamic.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedUrl).isEqualTo("https://dynamic.audio")
    }

    @Test
    fun content_descriptionChange_recomposesWithNewDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://test.audio", "description" to "Old Desc"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testAudioPlayer.capturedDescription).isEqualTo("Old Desc")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.audio", "description" to "New Desc"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testAudioPlayer.capturedDescription).isEqualTo("New Desc")
    }

    @Test
    fun content_dynamicDescriptionChange_recomposesWithNewDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://test.audio",
                        "description" to mapOf("path" to "/media/desc"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("desc" to "Initial Description")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://test.audio - Initial Description").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedDescription).isEqualTo("Initial Description")

        controller.updateData("/media/desc", "Updated Description")
        controller.waitForIdle()

        onNodeWithText("Audio: https://test.audio - Initial Description").assertDoesNotExist()
        onNodeWithText("Audio: https://test.audio - Updated Description").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedDescription).isEqualTo("Updated Description")
    }

    @Test
    fun content_dynamicDescriptionErased_recomposesWithNullDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties =
                    mapOf(
                        "url" to "https://test.audio",
                        "description" to mapOf("path" to "/media/desc"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("desc" to "Initial Description")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Audio: https://test.audio - Initial Description").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedDescription).isEqualTo("Initial Description")

        controller.updateData("/media/desc", null)
        controller.waitForIdle()

        onNodeWithText("Audio: https://test.audio - Initial Description").assertDoesNotExist()
        onNodeWithText("Audio: https://test.audio - no-desc").assertIsDisplayed()
        assertThat(testAudioPlayer.capturedDescription).isNull()
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "AudioPlayer",
                properties = mapOf("url" to "https://test.audio"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Audio: https://test.audio - no-desc") and hasTestTag("initial_tag"))
            .assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Audio: https://test.audio - no-desc") and hasTestTag("updated_tag"))
            .assertIsDisplayed()
    }
}
