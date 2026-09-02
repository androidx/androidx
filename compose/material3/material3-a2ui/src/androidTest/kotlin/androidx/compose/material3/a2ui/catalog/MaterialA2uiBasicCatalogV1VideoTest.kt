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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
class MaterialA2uiBasicCatalogV1VideoTest {

    var capturedUrl: String? = null

    private val fakeVideoRenderer = A2uiVideoRenderer { url, modifier, onError ->
        capturedUrl = url
        Box(modifier = modifier.size(100.dp, 60.dp)) {
            Box(
                modifier =
                    Modifier.matchParentSize().semantics { testTag = VideoPlaceholderTestTag }
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
            components = listOf(MaterialA2uiBasicCatalogV1Video(fakeVideoRenderer)),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun url_staticUrl_rendersVideo() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://example.com/video.mp4"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/video.mp4")
    }

    @Test
    fun url_functionExpression_rendersVideo() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties =
                    mapOf(
                        "url" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "https://example.com/\${/media/id}.mp4"),
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

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/track_42.mp4")
    }

    @Test
    fun url_componentPayloadUpdate_updatesVideo() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://example.com/video.mp4"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/video.mp4")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://example.com/video2.mp4"),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/video2.mp4")
    }

    @Test
    fun url_componentPayloadUpdate_switchesFromStaticToDynamicBinding() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://example.com/static.mp4"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("url" to "https://example.com/dynamic.mp4")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/static.mp4")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to mapOf("path" to "/media/url")),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp4")
    }

    @Test
    fun url_dynamicBinding_externalDataChange_updatesVideo() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/media/track")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("track" to "https://example.com/dynamic.mp4")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp4")

        controller.updateData("/media/track", "https://example.com/dynamic2.mp4")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic2.mp4")
    }

    @Test
    fun isReady_unresolvedProperties_remainsInLoadingStateUntilDataArrives() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
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

        onNodeWithTag(VideoPlaceholderTestTag).assertDoesNotExist()

        controller.updateData("/media/track", "https://example.com/dynamic.mp4")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.mp4")
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to mapOf("path" to "/media/track")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("media" to mapOf("track" to "https://example.com/video.mp4")),
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

        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
        onNodeWithTag("Loading").assertDoesNotExist()

        controller.updateData("/media/track", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(VideoPlaceholderTestTag).assertDoesNotExist()
        onNodeWithTag("Loading").assertIsDisplayed()
    }

    @Test
    fun modifier_parentModifier_isApplied() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://example.com/video.mp4"),
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
        onNodeWithTag(VideoPlaceholderTestTag).assertIsDisplayed()
    }

    @Test
    fun modifier_parameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Video",
                properties = mapOf("url" to "https://example.com/video.mp4"),
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
        val payload = A2uiComponentPayload(id = "root", type = "Video", properties = emptyMap())

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
                            type = "Video",
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
        assertThat(error.message).isEqualTo("Video loading error from renderer: error")
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
                            type = "Video",
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
                "Video loading error from renderer for error_with_throwable: Network failure"
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
                            type = "Video",
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
            .isEqualTo("Video loading error from renderer: error_with_blank_throwable")
    }
}

private const val VideoPlaceholderTestTag = "VideoPlaceholder"
