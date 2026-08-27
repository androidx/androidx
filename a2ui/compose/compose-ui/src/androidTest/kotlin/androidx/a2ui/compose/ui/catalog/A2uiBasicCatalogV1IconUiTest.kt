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
class A2uiBasicCatalogV1IconUiTest {

    private val testIcon =
        object : A2uiBasicCatalogV1.Icon {
            var capturedSource: A2uiBasicCatalogV1.Icon.Source? = null
            var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                source: A2uiBasicCatalogV1.Icon.Source,
                accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedSource = source
                    capturedAccessibility = accessibility
                }
                val textStr =
                    when (source) {
                        is A2uiBasicCatalogV1.Icon.BuiltIn -> "Icon: ${source.value}"
                        is A2uiBasicCatalogV1.Icon.SvgPath -> "SVG: ${source.svgPath}"
                        is A2uiBasicCatalogV1.Icon.Unrecognized -> "Unrecognized: ${source.name}"
                    }
                val labelStr = accessibility?.label?.let { " ($it)" } ?: ""
                BasicText(text = "$textStr$labelStr", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testIcon),
            functions = emptyList(),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/pendingData")),
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

        onNodeWithText("Icon: favorite").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingData", "favorite")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Icon: favorite").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/iconName")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("iconName" to "home")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Icon: home").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/user/iconName", null)
        controller.waitForIdle()

        onNodeWithText("Icon: home").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_invalidDynamicDataType_returnsFalseAndReportsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/icon")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("icon" to 12345)),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
                onError = { _, _ -> },
            )
        }

        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        assertThat(testIcon.capturedSource).isNull()
        val error = controller.outboundErrors.single()
        assertThat(error.message).contains("Type mismatch for key 'name' in component 'root'")
        assertThat(error.context["path"]).isEqualTo("name")
    }

    @Test
    fun isReady_dynamicMapWithoutValidSvgPath_returnsFalseAndReportsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/icon")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("icon" to mapOf("svgPath" to 999))),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
                onError = { _, _ -> },
            )
        }

        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        assertThat(testIcon.capturedSource).isNull()
        val error = controller.outboundErrors.single()
        assertThat(error.message).contains("Type mismatch for key 'name' in component 'root'")
        assertThat(error.context["path"]).isEqualTo("name")
    }

    @Test
    fun content_staticBuiltIn_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "add"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: add").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Add)
    }

    @Test
    fun content_staticSvgPath_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val svgStr = "M10 10 H 90 V 90 H 10 Z"
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("svgPath" to svgStr)),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("SVG: $svgStr").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath(svgStr))
    }

    @Test
    fun content_dynamicBuiltIn_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("path" to "/user/icon")),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to "settings")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: settings").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Settings)
    }

    @Test
    fun content_dynamicSvgPath_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val svgStr = "M1 1h22v22H1z"
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("path" to "/user/icon")),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to mapOf("svgPath" to svgStr))),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("SVG: $svgStr").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath(svgStr))
    }

    @Test
    fun content_dynamicUnrecognizedToken_passesUnrecognizedToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/icon")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("icon" to "dynamic_unknown_token")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Unrecognized: dynamic_unknown_token").assertIsDisplayed()
        assertThat(testIcon.capturedSource)
            .isEqualTo(A2uiBasicCatalogV1.Icon.Unrecognized("dynamic_unknown_token"))
    }

    @Test
    fun content_dynamicEmptyStringToken_passesUnrecognizedToTypedContent() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/icon")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("icon" to "")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Unrecognized: ").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.Unrecognized(""))
    }

    @Test
    fun content_staticAccessibility_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties =
                                mapOf(
                                    "name" to "favorite",
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Favorite Icon",
                                            "description" to "Mark as favorite",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: favorite (Favorite Icon)").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Favorite)
        assertThat(testIcon.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Favorite Icon",
                    description = "Mark as favorite",
                )
            )
    }

    @Test
    fun content_dynamicAccessibility_resolvesPropertiesAndPassesToTypedContent() =
        runComposeUiTest {
            val payload =
                A2uiComponentPayload(
                    id = "root",
                    type = "Icon",
                    properties =
                        mapOf(
                            "name" to "settings",
                            "accessibility" to mapOf("label" to mapOf("path" to "/icon/label")),
                        ),
                )
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents = listOf(payload),
                    initialData = mapOf("icon" to mapOf("label" to "Settings Label")),
                )
            val surface = controller.start()

            setContent { A2uiTestSurface(surface) }

            onNodeWithText("Icon: settings (Settings Label)").assertIsDisplayed()
            assertThat(testIcon.capturedAccessibility)
                .isEqualTo(A2uiBasicCatalogV1.AccessibilityAttributes(label = "Settings Label"))

            controller.updateData("/icon/label", "Updated Label")
            controller.waitForIdle()

            onNodeWithText("Icon: settings (Settings Label)").assertDoesNotExist()
            onNodeWithText("Icon: settings (Updated Label)").assertIsDisplayed()
            assertThat(testIcon.capturedAccessibility)
                .isEqualTo(A2uiBasicCatalogV1.AccessibilityAttributes(label = "Updated Label"))
        }

    @Test
    fun content_omittedAccessibility_passesNullToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "delete"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: delete").assertIsDisplayed()
        assertThat(testIcon.capturedAccessibility).isNull()
    }

    @Test
    fun content_invalidDynamicAccessibilityType_reportsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties =
                    mapOf(
                        "name" to "settings",
                        "accessibility" to mapOf("path" to "/user/accessibility"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("accessibility" to 12345)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, onError = { _, _ -> }) }

        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .contains("Type mismatch for key 'accessibility' in component 'root'")
        assertThat(error.context["path"]).isEqualTo("accessibility")
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "check"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Icon: check") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_staticNameChange_recomposesWithNewName() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "home"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: home").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("name" to "search"))
        controller.waitForIdle()

        onNodeWithText("Icon: home").assertDoesNotExist()
        onNodeWithText("Icon: search").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Search)
    }

    @Test
    fun content_dynamicNameChange_recomposesWithNewName() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/icon/name")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("icon" to mapOf("name" to "home")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: home").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Home)

        controller.updateData("/icon/name", "person")
        controller.waitForIdle()

        onNodeWithText("Icon: home").assertDoesNotExist()
        onNodeWithText("Icon: person").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Person)
    }

    @Test
    fun content_dynamicSvgPathChange_recomposesWithNewPath() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/icon/name")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("icon" to mapOf("name" to mapOf("svgPath" to "M1 1"))),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("SVG: M1 1").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath("M1 1"))

        controller.updateData("/icon/name", mapOf("svgPath" to "M2 2"))
        controller.waitForIdle()

        onNodeWithText("SVG: M1 1").assertDoesNotExist()
        onNodeWithText("SVG: M2 2").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath("M2 2"))
    }

    @Test
    fun content_staticToDynamicNameChange_recomposesWithNewName() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "add"),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to "settings")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: add").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("name" to mapOf("path" to "/user/icon")),
        )
        controller.waitForIdle()

        onNodeWithText("Icon: add").assertDoesNotExist()
        onNodeWithText("Icon: settings").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Settings)
    }

    @Test
    fun content_staticTokenToSvgPathChange_recomposesWithNewSource() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "home"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: home").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Home)

        controller.updateComponent(
            id = "root",
            properties = mapOf("name" to mapOf("svgPath" to "M10 10")),
        )
        controller.waitForIdle()

        onNodeWithText("Icon: home").assertDoesNotExist()
        onNodeWithText("SVG: M10 10").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath("M10 10"))
    }

    @Test
    fun content_dynamicTokenToSvgPathSwitch_recomposesWithNewSource() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/icon/source")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("icon" to mapOf("source" to "home")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: home").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Home)

        controller.updateData("/icon/source", mapOf("svgPath" to "M5 5h10v10H5z"))
        controller.waitForIdle()

        onNodeWithText("Icon: home").assertDoesNotExist()
        onNodeWithText("SVG: M5 5h10v10H5z").assertIsDisplayed()
        assertThat(testIcon.capturedSource)
            .isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath("M5 5h10v10H5z"))
    }

    @Test
    fun content_dynamicSvgPathToTokenSwitch_recomposesWithNewSource() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/icon/source")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("icon" to mapOf("source" to mapOf("svgPath" to "M1 1"))),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("SVG: M1 1").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.SvgPath("M1 1"))

        controller.updateData("/icon/source", "call")
        controller.waitForIdle()

        onNodeWithText("SVG: M1 1").assertDoesNotExist()
        onNodeWithText("Icon: call").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Call)
    }

    @Test
    fun content_dynamicValidToUnrecognizedTokenChange_recomposesWithUnrecognized() =
        runComposeUiTest {
            val payload =
                A2uiComponentPayload(
                    id = "root",
                    type = "Icon",
                    properties = mapOf("name" to mapOf("path" to "/user/icon")),
                )
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents = listOf(payload),
                    initialData = mapOf("user" to mapOf("icon" to "favorite")),
                )
            val surface = controller.start()

            setContent { A2uiTestSurface(surface) }

            onNodeWithText("Icon: favorite").assertIsDisplayed()
            assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Favorite)

            controller.updateData("/user/icon", "unknown_icon_token")
            controller.waitForIdle()

            onNodeWithText("Icon: favorite").assertDoesNotExist()
            onNodeWithText("Unrecognized: unknown_icon_token").assertIsDisplayed()
            assertThat(testIcon.capturedSource)
                .isEqualTo(A2uiBasicCatalogV1.Icon.Unrecognized("unknown_icon_token"))
        }

    @Test
    fun content_dynamicUnrecognizedToValidTokenChange_recomposesWithBuiltIn() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties = mapOf("name" to mapOf("path" to "/user/icon")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("icon" to "unknown_icon_token")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Unrecognized: unknown_icon_token").assertIsDisplayed()
        assertThat(testIcon.capturedSource)
            .isEqualTo(A2uiBasicCatalogV1.Icon.Unrecognized("unknown_icon_token"))

        controller.updateData("/user/icon", "check")
        controller.waitForIdle()

        onNodeWithText("Unrecognized: unknown_icon_token").assertDoesNotExist()
        onNodeWithText("Icon: check").assertIsDisplayed()
        assertThat(testIcon.capturedSource).isEqualTo(A2uiBasicCatalogV1.Icon.BuiltIn.Check)
    }

    @Test
    fun content_staticAccessibilityChange_recomposesWithNewAccessibility() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties =
                                mapOf(
                                    "name" to "home",
                                    "accessibility" to mapOf("label" to "Home Icon"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: home (Home Icon)").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("name" to "home", "accessibility" to mapOf("label" to "Dashboard Icon")),
        )
        controller.waitForIdle()

        onNodeWithText("Icon: home (Home Icon)").assertDoesNotExist()
        onNodeWithText("Icon: home (Dashboard Icon)").assertIsDisplayed()
        assertThat(testIcon.capturedAccessibility)
            .isEqualTo(A2uiBasicCatalogV1.AccessibilityAttributes(label = "Dashboard Icon"))
    }

    @Test
    fun content_dynamicAccessibilityErased_recomposesWithNullAccessibility() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Icon",
                properties =
                    mapOf(
                        "name" to "settings",
                        "accessibility" to mapOf("label" to mapOf("path" to "/icon/label")),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("icon" to mapOf("label" to "Initial Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Icon: settings (Initial Label)").assertIsDisplayed()
        assertThat(testIcon.capturedAccessibility)
            .isEqualTo(A2uiBasicCatalogV1.AccessibilityAttributes(label = "Initial Label"))

        controller.updateData("/icon/label", null)
        controller.waitForIdle()

        onNodeWithText("Icon: settings (Initial Label)").assertDoesNotExist()
        onNodeWithText("Icon: settings").assertIsDisplayed()
        assertThat(testIcon.capturedAccessibility).isNull()
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "add"),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Icon: add") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Icon: add") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
