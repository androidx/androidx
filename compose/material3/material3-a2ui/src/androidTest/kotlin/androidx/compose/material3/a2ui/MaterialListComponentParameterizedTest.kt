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
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class MaterialListComponentParameterizedTest(private val alignToken: String) {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialListComponent, MaterialTextComponent),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "align={0}")
        fun data(): Collection<Array<Any>> =
            listOf(arrayOf("start"), arrayOf("center"), arrayOf("end"), arrayOf("stretch"))
    }

    @Test
    fun eachAlignTokens_appliesCorrectAlignment() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to listOf("item_1"),
                        "direction" to "vertical",
                        "align" to alignToken,
                    ),
            )
        val itemPayload =
            A2uiComponentPayload(id = "item_1", type = "Text", properties = mapOf("text" to "Item"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("custom_list_tag").width(200.dp),
                )
            }
        }

        val listBounds = onNodeWithTag("custom_list_tag").getUnclippedBoundsInRoot()
        val itemBounds = onNodeWithText("Item").getUnclippedBoundsInRoot()

        when (alignToken) {
            "start" -> {
                assertThat(itemBounds.left).isEqualTo(listBounds.left)
                assertThat(itemBounds.right - itemBounds.left)
                    .isLessThan(listBounds.right - listBounds.left)
            }
            "center" -> {
                val itemCenter = (itemBounds.left + itemBounds.right) / 2
                val listCenter = (listBounds.left + listBounds.right) / 2

                assertThat(itemCenter.value).isWithin(0.5f).of(listCenter.value)
                assertThat(itemBounds.right - itemBounds.left)
                    .isLessThan(listBounds.right - listBounds.left)
            }
            "end" -> {
                assertThat(itemBounds.right).isEqualTo(listBounds.right)
                assertThat(itemBounds.right - itemBounds.left)
                    .isLessThan(listBounds.right - listBounds.left)
            }
            "stretch" -> {
                assertThat(itemBounds.right - itemBounds.left)
                    .isEqualTo(listBounds.right - listBounds.left)
            }
        }
    }
}
