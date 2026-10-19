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

package androidx.a2ui.compose.ui.tooling.preview

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiRequiredFunction
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiPreviewTest {

    private val scope = CoroutineScope(SupervisorJob())

    private val testText =
        object : A2uiBasicCatalogV1.Text {
            @Composable
            override fun A2uiComponentScope.TypedContent(
                text: String,
                variant: A2uiBasicCatalogV1.Text.Variant,
                accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                modifier: Modifier,
            ) {
                BasicText(text = text, modifier = modifier)
            }
        }

    private val uppercaseTestText =
        object : A2uiBasicCatalogV1.Text {
            @Composable
            override fun A2uiComponentScope.TypedContent(
                text: String,
                variant: A2uiBasicCatalogV1.Text.Variant,
                accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                modifier: Modifier,
            ) {
                BasicText(text = text.uppercase(), modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testText),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    private val uppercaseTestCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(uppercaseTestText),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    private val createSurfaceMessage =
        """
        {
          "version": "v0.9",
          "createSurface": {
            "surfaceId": "preview_surface",
            "catalogId": "test_catalog"
          }
        }
        """
            .trimIndent()

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun createA2uiPreviewSurfaceModel_withoutCreateSurface_throwsIllegalArgumentException() {
        val updateComponentsMessage =
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "preview_surface",
                "components": [
                  {
                    "id": "root",
                    "component": "Text",
                    "text": "Hello from Preview"
                  }
                ]
              }
            }
            """
                .trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            createA2uiPreviewSurfaceModel(
                catalog = testCatalog,
                jsonMessages = listOf(updateComponentsMessage),
                scope = scope,
            )
        }
    }

    @Test
    fun createA2uiPreviewSurfaceModel_withExplicitCreateSurface_usesProvidedSurfaceAndRenders() =
        runComposeUiTest {
            val createCustomSurface =
                """
                {
                  "version": "v0.9",
                  "createSurface": {
                    "surfaceId": "custom_surface",
                    "catalogId": "test_catalog"
                  }
                }
                """
                    .trimIndent()
            val updateComponents =
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "custom_surface",
                    "components": [
                      {
                        "id": "root",
                        "component": "Text",
                        "text": "Explicit Surface Content"
                      }
                    ]
                  }
                }
                """
                    .trimIndent()

            val surfaceModel =
                createA2uiPreviewSurfaceModel(
                    catalog = testCatalog,
                    jsonMessages = listOf(createCustomSurface, updateComponents),
                    scope = scope,
                )

            assertThat(surfaceModel.id).isEqualTo("custom_surface")

            setContent { A2uiTestSurface(surface = surfaceModel) }

            onNodeWithText("Explicit Surface Content").assertIsDisplayed()
        }

    @Test
    fun createA2uiPreviewSurfaceModel_withDataModelAndComponents_bindsDataAndRenders() =
        runComposeUiTest {
            val updateDataModel =
                """
                {
                  "version": "v0.9",
                  "updateDataModel": {
                    "surfaceId": "preview_surface",
                    "path": "/",
                    "value": {
                      "greeting": "Dynamic Bound Greeting"
                    }
                  }
                }
                """
                    .trimIndent()
            val updateComponents =
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "preview_surface",
                    "components": [
                      {
                        "id": "root",
                        "component": "Text",
                        "text": { "path": "/greeting" }
                      }
                    ]
                  }
                }
                """
                    .trimIndent()

            val surfaceModel =
                createA2uiPreviewSurfaceModel(
                    catalog = testCatalog,
                    jsonMessages = listOf(createSurfaceMessage, updateDataModel, updateComponents),
                    scope = scope,
                )

            setContent { A2uiTestSurface(surface = surfaceModel) }

            onNodeWithText("Dynamic Bound Greeting").assertIsDisplayed()
        }

    @Test
    fun previewParameterProvider_namedMapConstructor_providesValuesAndDisplayNames() =
        runComposeUiTest {
            val message1 =
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "preview_surface",
                    "components": [
                      { "id": "root", "component": "Text", "text": "Card One" }
                    ]
                  }
                }
                """
                    .trimIndent()
            val message2 =
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "preview_surface",
                    "components": [
                      { "id": "root", "component": "Text", "text": "Card Two" }
                    ]
                  }
                }
                """
                    .trimIndent()

            val provider =
                A2uiPreviewParameterProvider(
                    catalog = testCatalog,
                    namedJsonMessages =
                        mapOf(
                            "First Card" to listOf(createSurfaceMessage, message1),
                            "Second Card" to listOf(createSurfaceMessage, message2),
                        ),
                )

            val factories = provider.values.toList()
            assertThat(factories).hasSize(2)
            assertThat(provider.getDisplayName(0)).isEqualTo("First Card")
            assertThat(provider.getDisplayName(1)).isEqualTo("Second Card")
            assertThat(provider.getDisplayName(2)).isNull()

            setContent { A2uiTestSurface(surface = factories[1].create()) }
            onNodeWithText("Card Two").assertIsDisplayed()
        }

    @Test
    fun previewParameterProvider_listConstructor_providesValuesWithNullDisplayNames() =
        runComposeUiTest {
            val message1 =
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "preview_surface",
                    "components": [
                      { "id": "root", "component": "Text", "text": "Unnamed Card" }
                    ]
                  }
                }
                """
                    .trimIndent()

            val provider =
                A2uiPreviewParameterProvider(
                    catalog = testCatalog,
                    jsonMessages = listOf(createSurfaceMessage, message1),
                )

            val factories = provider.values.toList()
            assertThat(factories).hasSize(1)
            assertThat(provider.getDisplayName(0)).isNull()

            setContent { A2uiTestSurface(surface = factories[0].create()) }
            onNodeWithText("Unnamed Card").assertIsDisplayed()
        }

    @Test
    fun previewParameterProvider_catalogProviderNamedMapConstructor_providesValuesAndDisplayNames() =
        runComposeUiTest {
            val provider =
                A2uiPreviewParameterProvider(
                    catalogProvider = { testCatalog },
                    namedJsonMessages =
                        mapOf(
                            "First Card" to
                                listOf(
                                    createSurfaceMessage,
                                    updateTextComponentMessage("Card One"),
                                ),
                            "Second Card" to
                                listOf(
                                    createSurfaceMessage,
                                    updateTextComponentMessage("Card Two"),
                                ),
                        ),
                )

            val factories = provider.values.toList()
            assertThat(factories).hasSize(2)
            assertThat(provider.getDisplayName(0)).isEqualTo("First Card")
            assertThat(provider.getDisplayName(1)).isEqualTo("Second Card")
            assertThat(provider.getDisplayName(2)).isNull()

            setContent { A2uiTestSurface(surface = factories[1].create()) }
            onNodeWithText("Card Two").assertIsDisplayed()
        }

    @Test
    fun previewParameterProvider_catalogProviderListConstructor_providesValuesWithNullDisplayNames() =
        runComposeUiTest {
            val provider =
                A2uiPreviewParameterProvider(
                    catalogProvider = { testCatalog },
                    jsonMessages =
                        listOf(
                            createSurfaceMessage,
                            updateTextComponentMessage("Unnamed Card"),
                        ),
                )

            val factories = provider.values.toList()
            assertThat(factories).hasSize(1)
            assertThat(provider.getDisplayName(0)).isNull()

            setContent { A2uiTestSurface(surface = factories[0].create()) }
            onNodeWithText("Unnamed Card").assertIsDisplayed()
        }

    @Test
    fun previewParameterProvider_valuesAccessedTwice_returnsSameFactories() {
        val provider =
            A2uiPreviewParameterProvider(
                catalog = testCatalog,
                jsonMessages = listOf(createSurfaceMessage, updateTextComponentMessage("Cached")),
            )

        val firstFactories = provider.values.toList()
        val secondFactories = provider.values.toList()

        assertThat(secondFactories).hasSize(1)
        assertThat(secondFactories[0]).isSameInstanceAs(firstFactories[0])
    }

    @Test
    fun rememberA2uiPreviewSurface_withCreateSurface_rendersSurface() = runComposeUiTest {
        setContent {
            val surfaceModel =
                rememberA2uiPreviewSurface(
                    catalog = testCatalog,
                    jsonMessages =
                        listOf(
                            createSurfaceMessage,
                            updateTextComponentMessage("Remembered Content"),
                        ),
                )
            A2uiTestSurface(surface = surfaceModel)
        }

        onNodeWithText("Remembered Content").assertIsDisplayed()
    }

    @Test
    fun rememberA2uiPreviewSurface_equalJsonMessages_returnsSameSurface() = runComposeUiTest {
        // neverEqualPolicy forces a recomposition even though the new list equals the old one.
        var jsonMessages by
            mutableStateOf(
                listOf(createSurfaceMessage, updateTextComponentMessage("Content")),
                neverEqualPolicy(),
            )
        val surfaceModels = mutableListOf<A2uiSurfaceModel>()
        setContent {
            surfaceModels +=
                rememberA2uiPreviewSurface(catalog = testCatalog, jsonMessages = jsonMessages)
        }

        jsonMessages = listOf(createSurfaceMessage, updateTextComponentMessage("Content"))
        waitForIdle()

        assertThat(surfaceModels).hasSize(2)
        assertThat(surfaceModels[1]).isSameInstanceAs(surfaceModels[0])
    }

    @Test
    fun rememberA2uiPreviewSurface_jsonMessagesChanged_recreatesSurface() = runComposeUiTest {
        var jsonMessages by
            mutableStateOf(
                listOf(createSurfaceMessage, updateTextComponentMessage("First Content"))
            )
        setContent {
            val surfaceModel =
                rememberA2uiPreviewSurface(catalog = testCatalog, jsonMessages = jsonMessages)
            A2uiTestSurface(surface = surfaceModel)
        }
        onNodeWithText("First Content").assertIsDisplayed()

        jsonMessages = listOf(createSurfaceMessage, updateTextComponentMessage("Second Content"))

        onNodeWithText("Second Content").assertIsDisplayed()
        onNodeWithText("First Content").assertDoesNotExist()
    }

    @Test
    fun rememberA2uiPreviewSurface_catalogChanged_recreatesSurface() = runComposeUiTest {
        var catalog by mutableStateOf(testCatalog)
        setContent {
            val surfaceModel =
                rememberA2uiPreviewSurface(
                    catalog = catalog,
                    jsonMessages =
                        listOf(createSurfaceMessage, updateTextComponentMessage("Catalog Content")),
                )
            A2uiTestSurface(surface = surfaceModel)
        }
        onNodeWithText("Catalog Content").assertIsDisplayed()

        catalog = uppercaseTestCatalog

        onNodeWithText("CATALOG CONTENT").assertIsDisplayed()
        onNodeWithText("Catalog Content").assertDoesNotExist()
    }

    @Test
    fun rememberA2uiPreviewSurface_leavesComposition_disposesSurface() = runComposeUiTest {
        var showPreviewSurface by mutableStateOf(true)
        lateinit var surfaceModel: A2uiSurfaceModel
        setContent {
            if (showPreviewSurface) {
                surfaceModel =
                    rememberA2uiPreviewSurface(
                        catalog = testCatalog,
                        jsonMessages =
                            listOf(
                                createSurfaceMessage,
                                updateTextComponentMessage("Disposable Content"),
                            ),
                    )
            }
            // Keeps rendering the surface after rememberA2uiPreviewSurface leaves the composition.
            A2uiTestSurface(surface = surfaceModel)
        }
        onNodeWithText("Disposable Content").assertIsDisplayed()

        showPreviewSurface = false

        // A disposed surface no longer has a root component to render.
        onNodeWithText("Disposable Content").assertDoesNotExist()
    }

    private fun updateTextComponentMessage(text: String): String =
        """
        {
          "version": "v0.9",
          "updateComponents": {
            "surfaceId": "preview_surface",
            "components": [
              { "id": "root", "component": "Text", "text": "$text" }
            ]
          }
        }
        """
            .trimIndent()
}
