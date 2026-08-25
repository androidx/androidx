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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class MaterialTextFieldComponentParameterizedTest(private val variantToken: String) {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialTextFieldComponent))

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "variant={0}")
        fun data(): Collection<Array<Any>> =
            listOf(
                arrayOf("shortText"),
                arrayOf("longText"),
                arrayOf("number"),
                arrayOf("obscured"),
            )
    }

    @Test
    fun eachVariant_rendersTextFieldWithLabelAndValue() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to "Field Label",
                        "value" to "Sample Input",
                        "variant" to variantToken,
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Field Label").assertIsDisplayed()
        if (variantToken != "obscured") {
            onNodeWithText("Sample Input").assertIsDisplayed()
        }
    }
}
