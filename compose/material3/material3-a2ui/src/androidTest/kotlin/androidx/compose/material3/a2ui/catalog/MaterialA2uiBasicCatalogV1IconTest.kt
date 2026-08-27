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

import android.os.Build
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.icons.A2uiIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1IconTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.icon),
        )

    @Test
    fun name_staticToken_rendersIcon() = runComposeUiTest {
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
                                    "accessibility" to mapOf("label" to "Favorite Icon"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun name_staticSvgPath_rendersIcon() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("svgPath" to TEST_CIRCLE_SVG_PATH)),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.testTag("container").size(48.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        A2uiTestSurface(surface = surface, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        onNodeWithTag("container").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Inside of the icon should be rendered with the icon color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Black)
            assertWithMessage("Outside of the icon should be rendered with the background color")
                .that(pixels[0, 0].toOpaque())
                .isEqualTo(Color.White)
        }
    }

    @Test
    fun name_dynamicToken_rendersIcon() = runComposeUiTest {
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
                                    "name" to mapOf("path" to "/user/icon"),
                                    "accessibility" to mapOf("label" to "Favorite Icon"),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to "favorite")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun name_dynamicSvgPath_rendersIcon() = runComposeUiTest {
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
                initialData =
                    mapOf("user" to mapOf("icon" to mapOf("svgPath" to TEST_CIRCLE_SVG_PATH))),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.testTag("container").size(48.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        A2uiTestSurface(surface = surface, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        onNodeWithTag("container").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Inside of the icon should be rendered with the icon color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Black)
            assertWithMessage("Outside of the icon should be rendered with the background color")
                .that(pixels[0, 0].toOpaque())
                .isEqualTo(Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun name_dynamicToken_updatesIconWhenDataChanges() = runComposeUiTest {
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
                                    "name" to mapOf("path" to "/user/icon"),
                                    "accessibility" to mapOf("label" to "favorite"),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to "favorite")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    A2uiTestSurface(surface = surface)
                    Icon(
                        imageVector = requireNotNull(A2uiIcon.fromName("star")),
                        contentDescription = null,
                        modifier = Modifier.testTag("expected_icon"),
                    )
                }
            }
        }

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "name" to mapOf("path" to "/user/icon"),
                    "accessibility" to mapOf("label" to "star"),
                ),
        )
        controller.updateData("/user/icon", "star")
        controller.waitForIdle()

        val actualBitmap = onNodeWithContentDescription("star").captureToImage().asAndroidBitmap()
        val expectedBitmap = onNodeWithTag("expected_icon").captureToImage().asAndroidBitmap()

        assertWithMessage("Icon width doesn't match")
            .that(actualBitmap.width)
            .isEqualTo(expectedBitmap.width)
        assertWithMessage("Icon height doesn't match")
            .that(actualBitmap.height)
            .isEqualTo(expectedBitmap.height)
        assertWithMessage("Rendered icon does not match expected icon after data update")
            .that(actualBitmap.sameAs(expectedBitmap))
            .isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun name_dynamicBinding_switchesBetweenTokenAndSvgPath() = runComposeUiTest {
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
                initialData = mapOf("user" to mapOf("icon" to "favorite")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.testTag("container").size(48.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        A2uiTestSurface(surface = surface, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        controller.updateData("/user/icon", mapOf("svgPath" to TEST_CIRCLE_SVG_PATH))
        controller.waitForIdle()

        onNodeWithTag("container").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Inside of the icon should be rendered with the icon color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Black)
            assertWithMessage("Outside of the icon should be rendered with the background color")
                .that(pixels[0, 0].toOpaque())
                .isEqualTo(Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun name_componentPayloadUpdate_updatesDisplayedIcon() = runComposeUiTest {
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
                                    "accessibility" to mapOf("label" to "favorite"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    A2uiTestSurface(surface = surface)
                    Icon(
                        imageVector = requireNotNull(A2uiIcon.fromName("lock")),
                        contentDescription = null,
                        modifier = Modifier.testTag("expected_icon"),
                    )
                }
            }
        }

        controller.updateComponent(
            id = "root",
            properties = mapOf("name" to "lock", "accessibility" to mapOf("label" to "lock")),
        )
        controller.waitForIdle()

        val actualBitmap = onNodeWithContentDescription("lock").captureToImage().asAndroidBitmap()
        val expectedBitmap = onNodeWithTag("expected_icon").captureToImage().asAndroidBitmap()

        assertWithMessage("Icon width doesn't match")
            .that(actualBitmap.width)
            .isEqualTo(expectedBitmap.width)
        assertWithMessage("Icon height doesn't match")
            .that(actualBitmap.height)
            .isEqualTo(expectedBitmap.height)
        assertWithMessage("Rendered icon does not match expected icon after component update")
            .that(actualBitmap.sameAs(expectedBitmap))
            .isTrue()
    }

    @Test
    fun name_unknownDynamicIconToken_reportsError() = runComposeUiTest {
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
                initialData = mapOf("user" to mapOf("icon" to "unknown_icon_token")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithText(
                "Error: Unknown icon 'unknown_icon_token'. Expected a valid icon token or an object with 'svgPath'."
            )
            .assertIsDisplayed()
    }

    @Test
    fun name_invalidSvgPath_reportsError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("svgPath" to "invalid svg path")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithText("Error: Failed to parse SVG path 'invalid svg path'.").assertIsDisplayed()
    }

    @Test
    fun transition_validToInvalidToken_reportsError() = runComposeUiTest {
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
                                    "name" to mapOf("path" to "/user/icon"),
                                    "accessibility" to mapOf("label" to "Favorite Icon"),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("icon" to "favorite")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()

        controller.updateData("/user/icon", "unknown_icon_token")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithContentDescription("Favorite Icon").assertDoesNotExist()
        onNodeWithText(
                "Error: Unknown icon 'unknown_icon_token'. Expected a valid icon token or an object with 'svgPath'."
            )
            .assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun transition_invalidToValidToken_recoversAndRendersIcon() = runComposeUiTest {
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
                initialData = mapOf("user" to mapOf("icon" to "unknown_icon_token")),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    A2uiTestSurface(
                        surface = surface,
                        onError = { exception, _ -> Text("Error: ${exception.message}") },
                    )
                    Icon(
                        imageVector = requireNotNull(A2uiIcon.fromName("favorite")),
                        contentDescription = null,
                        modifier = Modifier.testTag("expected_icon"),
                    )
                }
            }
        }

        onNodeWithText(
                "Error: Unknown icon 'unknown_icon_token'. Expected a valid icon token or an object with 'svgPath'."
            )
            .assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("name" to "favorite", "accessibility" to mapOf("label" to "favorite")),
        )
        controller.waitForIdle()

        val actualBitmap =
            onNodeWithContentDescription("favorite").captureToImage().asAndroidBitmap()
        val expectedBitmap = onNodeWithTag("expected_icon").captureToImage().asAndroidBitmap()

        assertWithMessage("Icon width doesn't match")
            .that(actualBitmap.width)
            .isEqualTo(expectedBitmap.width)
        assertWithMessage("Icon height doesn't match")
            .that(actualBitmap.height)
            .isEqualTo(expectedBitmap.height)
        assertWithMessage("Recovered icon does not match expected icon")
            .that(actualBitmap.sameAs(expectedBitmap))
            .isTrue()
    }

    @Test
    fun transition_validToInvalidSvgPath_reportsError() = runComposeUiTest {
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
                                    "name" to mapOf("path" to "/user/icon"),
                                    "accessibility" to mapOf("label" to "Circle Icon"),
                                ),
                        )
                    ),
                initialData =
                    mapOf("user" to mapOf("icon" to mapOf("svgPath" to TEST_CIRCLE_SVG_PATH))),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithContentDescription("Circle Icon").assertIsDisplayed()

        controller.updateData("/user/icon", mapOf("svgPath" to "invalid svg path"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithContentDescription("Circle Icon").assertDoesNotExist()
        onNodeWithText("Error: Failed to parse SVG path 'invalid svg path'.").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun transition_invalidToValidSvgPath_recoversAndRendersIcon() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to mapOf("svgPath" to "invalid svg path")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.testTag("container").size(48.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        A2uiTestSurface(
                            surface = surface,
                            onError = { exception, _ -> Text("Error: ${exception.message}") },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        onNodeWithText("Error: Failed to parse SVG path 'invalid svg path'.").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("name" to mapOf("svgPath" to TEST_CIRCLE_SVG_PATH)),
        )
        controller.waitForIdle()

        onNodeWithTag("container").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Inside of the icon should be rendered with the icon color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Black)
            assertWithMessage("Outside of the icon should be rendered with the background color")
                .that(pixels[0, 0].toOpaque())
                .isEqualTo(Color.White)
        }
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
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
                                    "name" to mapOf("path" to "/user/icon"),
                                    "accessibility" to mapOf("label" to "Favorite Icon"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithContentDescription("Favorite Icon").assertDoesNotExist()

        controller.updateData("/user/icon", "favorite")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()

        controller.updateData("/user/icon", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithContentDescription("Favorite Icon").assertDoesNotExist()
    }

    @Test
    fun modifier_parentModifier_isApplied() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "favorite"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNodeWithTag("custom_tag").assertIsDisplayed()
    }

    @Test
    fun modifier_parameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties = mapOf("name" to "favorite"),
                        )
                    ),
            )
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
    fun accessibility_staticLabel_setsContentDescription() = runComposeUiTest {
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
                                    "accessibility" to mapOf("label" to "Favorite Icon"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()
    }

    @Test
    fun accessibility_dynamicLabel_updatesContentDescription() = runComposeUiTest {
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
                                        mapOf("label" to mapOf("path" to "/user/label")),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("label" to "Favorite Icon")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithContentDescription("Favorite Icon").assertIsDisplayed()

        controller.updateData("/user/label", "Updated Label")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithContentDescription("Favorite Icon").assertDoesNotExist()
        onNodeWithContentDescription("Updated Label").assertIsDisplayed()
    }

    private companion object {
        const val TEST_CIRCLE_SVG_PATH = "M 12 4 A 8 8 0 1 0 12 20 A 8 8 0 1 0 12 4 Z"
    }

    private fun Color.toOpaque(): Color = copy(alpha = 1.0f)
}
