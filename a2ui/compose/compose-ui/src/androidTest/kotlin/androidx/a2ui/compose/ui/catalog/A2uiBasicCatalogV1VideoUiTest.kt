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
class A2uiBasicCatalogV1VideoUiTest {

    private val testVideo =
        object : A2uiBasicCatalogV1.Video {
            var capturedUrl: String? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(url: String, modifier: Modifier) {
                SideEffect { capturedUrl = url }
                BasicText(text = "Video: $url", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testVideo),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/pendingUrl")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading Video...", modifier = modifier) },
            )
        }

        onNodeWithText("Video: https://test.video").assertDoesNotExist()
        onNodeWithText("Loading Video...").assertIsDisplayed()

        controller.updateData("/pendingUrl", "https://test.video")
        controller.waitForIdle()

        onNodeWithText("Loading Video...").assertDoesNotExist()
        onNodeWithText("Video: https://test.video").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/media/video")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("video" to "https://video.start")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Video: https://video.start").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/media/video", null)
        controller.waitForIdle()

        onNodeWithText("Video: https://video.start").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_emptyStaticUrl_returnsTrueAndRendersContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(id = "root", type = "Video", properties = mapOf("url" to ""))
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: ").assertIsDisplayed()
    }

    @Test
    fun content_staticData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://static.video"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://static.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://static.video")
    }

    @Test
    fun content_dynamicData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/media/url")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://dyn.video")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://dyn.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://dyn.video")
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties =
                    mapOf(
                        "url" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "https://cdn.video/\${/media/id}.mp4"),
                            )
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("id" to "video_42")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://cdn.video/video_42.mp4").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://cdn.video/video_42.mp4")
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://tagged.video"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Video: https://tagged.video") and hasTestTag("custom_tag"))
            .assertIsDisplayed()
    }

    @Test
    fun content_urlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://old.video"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://old.video").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("url" to "https://new.video"))
        controller.waitForIdle()

        onNodeWithText("Video: https://old.video").assertDoesNotExist()
        onNodeWithText("Video: https://new.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://new.video")
    }

    @Test
    fun content_dynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/media/url")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://initial.video")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://initial.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://initial.video")

        controller.updateData("/media/url", "https://updated.video")
        controller.waitForIdle()

        onNodeWithText("Video: https://initial.video").assertDoesNotExist()
        onNodeWithText("Video: https://updated.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://updated.video")
    }

    @Test
    fun content_staticToDynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://static.video"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://dynamic.video")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Video: https://static.video").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to mapOf("path" to "/media/url")),
        )
        controller.waitForIdle()

        onNodeWithText("Video: https://static.video").assertDoesNotExist()
        onNodeWithText("Video: https://dynamic.video").assertIsDisplayed()
        assertThat(testVideo.capturedUrl).isEqualTo("https://dynamic.video")
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://test.video"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Video: https://test.video") and hasTestTag("initial_tag"))
            .assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Video: https://test.video") and hasTestTag("updated_tag"))
            .assertIsDisplayed()
    }
}
