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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1ImageTest {

    var capturedUrl: String? = null
    var capturedContentScale: ContentScale? = null

    private val fakeImageRenderer =
        A2uiImageRenderer { url, contentDescription, contentScale, modifier, onError ->
            capturedUrl = url
            capturedContentScale = contentScale
            Box(modifier = modifier) {
                Box(
                    Modifier.matchParentSize().semantics {
                        testTag = IMAGE_PLACEHOLDER_TEST_TAG
                        if (contentDescription != null) {
                            this.contentDescription = contentDescription
                        }
                    }
                )
            }

            if (url == "error") {
                SideEffect(url) { onError(null) }
            } else if (url == "error_with_throwable") {
                SideEffect(url) { onError(IllegalStateException("Network failure")) }
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer)),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun urlProperty_staticUrlProvided_rendersImage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://example.com/image.png"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/image.png")
        assertThat(capturedContentScale).isEqualTo(ContentScale.FillBounds)
    }

    @Test
    fun urlProperty_staticUrlUpdated_rendersNewImage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://example.com/image.png"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/image.png")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://example.com/image2.png"),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/image2.png")
    }

    @Test
    fun component_missingUrl_throwsValidationException() = runComposeUiTest {
        val payload = A2uiComponentPayload(id = "root", type = "Image", properties = emptyMap())

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
    fun urlProperty_dynamicDataProvided_rendersImage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to mapOf("path" to "/user/avatar")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Box(modifier = modifier) {
                            Box(Modifier.matchParentSize().semantics { testTag = "Loading" })
                        }
                    },
                )
            }
        }

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertDoesNotExist()

        controller.updateData("/user/avatar", "https://example.com/dynamic.png")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()

        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.png")
    }

    @Test
    fun urlProperty_dynamicDataUpdated_rendersNewImage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to mapOf("path" to "/user/avatar")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("avatar" to "https://example.com/dynamic.png")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic.png")

        controller.updateData("/user/avatar", "https://example.com/dynamic2.png")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
        assertThat(capturedUrl).isEqualTo("https://example.com/dynamic2.png")
    }

    @Test
    fun variantProperty_variantChanged_updatesSize() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://example.com/image.png", "variant" to "icon"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG)
            .assertIsDisplayed()
            .assertHeightIsEqualTo(24.dp)
            .assertWidthIsEqualTo(24.dp)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://example.com/image.png", "variant" to "header"),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed().assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun descriptionProperty_dynamicDataProvided_appliesSemantics() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf(
                        "url" to "https://example.com/image.png",
                        "description" to mapOf("path" to "/user/altText"),
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.updateData("/user/altText", "A beautiful scenery")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG)
            .assertContentDescriptionEquals("A beautiful scenery")
    }

    @Test
    fun modifier_isAppliedToComponent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://example.com/image.png"),
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
        onNodeWithTag(IMAGE_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun component_urlError_reportsError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Image",
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
        assertThat(error.message).isEqualTo("Image loading error from renderer: error")
    }

    @Test
    fun component_urlError_withThrowable_reportsErrorWithMessage() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Image",
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
                "Image loading error from renderer for error_with_throwable: Network failure"
            )
    }

    companion object {
        private const val IMAGE_PLACEHOLDER_TEST_TAG = "ImagePlaceholder"
    }
}
