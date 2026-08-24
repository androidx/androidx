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
class A2uiBasicCatalogV1ImageUiTest {

    private val testImage =
        object : A2uiBasicCatalogV1.Image {
            var capturedUrl: String? = null
            var capturedDescription: String? = null
            var capturedFit: A2uiBasicCatalogV1.Image.Fit? = null
            var capturedVariant: A2uiBasicCatalogV1.Image.Variant? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                url: String,
                description: String?,
                fit: A2uiBasicCatalogV1.Image.Fit,
                variant: A2uiBasicCatalogV1.Image.Variant,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedUrl = url
                    capturedDescription = description
                    capturedFit = fit
                    capturedVariant = variant
                }
                val descText = description ?: "no-desc"
                BasicText(text = "Image: $url - $descText", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testImage),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to mapOf("path" to "/pendingUrl")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading Image...", modifier = modifier) },
            )
        }

        onNodeWithText("Image: https://test.img - no-desc").assertDoesNotExist()
        onNodeWithText("Loading Image...").assertIsDisplayed()

        controller.updateData("/pendingUrl", "https://test.img")
        controller.waitForIdle()

        onNodeWithText("Loading Image...").assertDoesNotExist()
        onNodeWithText("Image: https://test.img - no-desc").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
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
                initialData = mapOf("user" to mapOf("avatar" to "https://img.start")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Image: https://img.start - no-desc").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/user/avatar", null)
        controller.waitForIdle()

        onNodeWithText("Image: https://img.start - no-desc").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_emptyStaticUrl_returnsTrueAndRendersContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(id = "root", type = "Image", properties = mapOf("url" to ""))
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image:  - no-desc").assertIsDisplayed()
    }

    @Test
    fun content_staticData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf(
                        "url" to "https://static.img",
                        "description" to "A static image",
                        "fit" to "cover",
                        "variant" to "avatar",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://static.img - A static image").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://static.img")
        assertThat(testImage.capturedDescription).isEqualTo("A static image")
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Cover)
        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Avatar)
    }

    @Test
    fun content_dynamicData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf(
                        "url" to mapOf("path" to "/img/url"),
                        "description" to mapOf("path" to "/img/alt"),
                        "fit" to "contain",
                        "variant" to "header",
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData =
                    mapOf("img" to mapOf("url" to "https://dyn.img", "alt" to "Dynamic Alt")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://dyn.img - Dynamic Alt").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://dyn.img")
        assertThat(testImage.capturedDescription).isEqualTo("Dynamic Alt")
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Contain)
        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Header)
    }

    @Test
    fun content_fit_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img", "fit" to "contain"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Contain)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "fit" to "cover"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Cover)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "fit" to "fill"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Fill)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "fit" to "none"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.None)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "fit" to "scaleDown"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.ScaleDown)
    }

    @Test
    fun content_variant_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img", "variant" to "icon"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Icon)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "avatar"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Avatar)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "smallFeature"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.SmallFeature)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "mediumFeature"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.MediumFeature)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "largeFeature"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.LargeFeature)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "header"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Header)
    }

    @Test
    fun content_omittedOptionalProperties_fallsBackToDefaults() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://default.img"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://default.img - no-desc").assertIsDisplayed()
        assertThat(testImage.capturedDescription).isNull()
        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Fill)
        assertThat(testImage.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.MediumFeature)
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf(
                        "url" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "https://cdn.img/\${/user/id}.png"),
                            )
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("id" to "123")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://cdn.img/123.png - no-desc").assertIsDisplayed()
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://tagged.img"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Image: https://tagged.img - no-desc") and hasTestTag("custom_tag"))
            .assertIsDisplayed()
    }

    @Test
    fun content_urlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://old.img"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://old.img - no-desc").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("url" to "https://new.img"))
        controller.waitForIdle()

        onNodeWithText("Image: https://old.img - no-desc").assertDoesNotExist()
        onNodeWithText("Image: https://new.img - no-desc").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://new.img")
    }

    @Test
    fun content_dynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to mapOf("path" to "/img/url")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("img" to mapOf("url" to "https://initial.img")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://initial.img - no-desc").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://initial.img")

        controller.updateData("/img/url", "https://updated.img")
        controller.waitForIdle()

        onNodeWithText("Image: https://initial.img - no-desc").assertDoesNotExist()
        onNodeWithText("Image: https://updated.img - no-desc").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://updated.img")
    }

    @Test
    fun content_staticToDynamicUrlChange_recomposesWithNewUrl() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://static.img"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("img" to mapOf("url" to "https://dynamic.img")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://static.img - no-desc").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to mapOf("path" to "/img/url")),
        )
        controller.waitForIdle()

        onNodeWithText("Image: https://static.img - no-desc").assertDoesNotExist()
        onNodeWithText("Image: https://dynamic.img - no-desc").assertIsDisplayed()
        assertThat(testImage.capturedUrl).isEqualTo("https://dynamic.img")
    }

    @Test
    fun content_descriptionChange_recomposesWithNewDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img", "description" to "Old Alt"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testImage.capturedDescription).isEqualTo("Old Alt")

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "description" to "New Alt"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testImage.capturedDescription).isEqualTo("New Alt")
    }

    @Test
    fun content_dynamicDescriptionChange_recomposesWithNewDescription() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf(
                        "url" to "https://test.img",
                        "description" to mapOf("path" to "/img/desc"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("img" to mapOf("desc" to "Initial Description")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Image: https://test.img - Initial Description").assertIsDisplayed()
        assertThat(testImage.capturedDescription).isEqualTo("Initial Description")

        controller.updateData("/img/desc", "Updated Description")
        controller.waitForIdle()

        onNodeWithText("Image: https://test.img - Initial Description").assertDoesNotExist()
        onNodeWithText("Image: https://test.img - Updated Description").assertIsDisplayed()
        assertThat(testImage.capturedDescription).isEqualTo("Updated Description")
    }

    @Test
    fun content_fitChange_recomposesWithNewFit() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img", "fit" to "cover"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Cover)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "fit" to "contain"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testImage.capturedFit).isEqualTo(A2uiBasicCatalogV1.Image.Fit.Contain)
    }

    @Test
    fun content_variantChange_recomposesWithNewVariant() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img", "variant" to "avatar"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Avatar)

        controller.updateComponent(
            id = "root",
            properties = mapOf("url" to "https://test.img", "variant" to "icon"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testImage.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Image.Variant.Icon)
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties = mapOf("url" to "https://test.img"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Image: https://test.img - no-desc") and hasTestTag("initial_tag"))
            .assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Image: https://test.img - no-desc") and hasTestTag("updated_tag"))
            .assertIsDisplayed()
    }
}
