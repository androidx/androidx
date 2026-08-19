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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MaterialA2uiBasicCatalogV1TextParameterizedTest(private val variantToken: String) {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.text),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "variant={0}")
        fun data(): Collection<Array<Any>> =
            listOf(
                arrayOf("h1"),
                arrayOf("h2"),
                arrayOf("h3"),
                arrayOf("h4"),
                arrayOf("h5"),
                arrayOf("caption"),
                arrayOf("body"),
            )
    }

    @Test
    fun eachVariant_appliesCorrespondingMaterialThemeTextStyle() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Sample Styled Text", "variant" to variantToken),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()
        lateinit var expectedStyle: TextStyle

        setContent {
            MaterialTheme {
                expectedStyle =
                    when (variantToken) {
                        "h1" -> MaterialTheme.typography.headlineLarge
                        "h2" -> MaterialTheme.typography.headlineMedium
                        "h3" -> MaterialTheme.typography.headlineSmall
                        "h4" -> MaterialTheme.typography.titleLarge
                        "h5" -> MaterialTheme.typography.titleMedium
                        "caption" -> MaterialTheme.typography.labelMedium
                        "body" -> MaterialTheme.typography.bodyLarge
                        else -> MaterialTheme.typography.bodyLarge
                    }
                A2uiTestSurface(surface)
            }
        }

        onNodeWithText("Sample Styled Text").assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText("Sample Styled Text").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { action ->
            action(results)
        }
        val actualStyle = results.firstOrNull()?.layoutInput?.style

        assertThat(actualStyle?.fontSize).isEqualTo(expectedStyle.fontSize)
        assertThat(actualStyle?.fontWeight).isEqualTo(expectedStyle.fontWeight)
        assertThat(actualStyle?.lineHeight).isEqualTo(expectedStyle.lineHeight)
    }

    @Test
    fun eachVariant_appliesExpectedHeadingSemantics() = runComposeUiTest {
        val isHeading = variantToken in listOf("h1", "h2", "h3", "h4", "h5")
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Variant Text", "variant" to variantToken),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Variant Text").assertIsDisplayed()
        if (isHeading) {
            onNodeWithText("Variant Text")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        } else {
            onNodeWithText("Variant Text")
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        }
    }
}
