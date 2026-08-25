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

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1Test {

    @Test
    fun catalogId_matchesExpectedSpecificationUri() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.catalogId).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.catalogId)
            .isEqualTo("https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json")
    }

    @Test
    fun themeSchema_matchesExpectedStructure() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)
        assertThat(catalog.themeSchema).isInstanceOf(A2uiObjectSchema::class.java)
        val themeObjSchema = catalog.themeSchema as A2uiObjectSchema
        assertThat(themeObjSchema.properties.keys)
            .containsExactly("primaryColor", "iconUrl", "agentDisplayName")
        assertThat(themeObjSchema.isAdditionalPropertiesAllowed).isTrue()
    }

    @Test
    fun properties_initializedWithConstructorArguments() {
        val text = TestTextComponent()
        val image = TestImageComponent()
        val card = TestCardComponent()
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val button = TestButtonComponent()
        val catalog =
            createTestBasicCatalog(
                text = text,
                image = image,
                card = card,
                row = row,
                column = column,
                button = button,
                functions = listOf(A2uiFormatStringFunction.INSTANCE),
            )

        assertThat(catalog.text).isSameInstanceAs(text)
        assertThat(catalog.image).isSameInstanceAs(image)
        assertThat(catalog.card).isSameInstanceAs(card)
        assertThat(catalog.row).isSameInstanceAs(row)
        assertThat(catalog.column).isSameInstanceAs(column)
        assertThat(catalog.button).isSameInstanceAs(button)
        assertThat(catalog.components).containsExactly(text, image, card, row, column, button)
        assertThat(catalog.functions).containsExactly(A2uiFormatStringFunction.INSTANCE)
    }

    @Test
    fun equalsAndHashCode_equalCatalogs_match() {
        val text = TestTextComponent()
        val image = TestImageComponent()
        val card = TestCardComponent()
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val button = TestButtonComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text,
                image = image,
                card = card,
                row = row,
                column = column,
                button = button,
            )
        val catalog2 =
            createTestBasicCatalog(
                text = text,
                image = image,
                card = card,
                row = row,
                column = column,
                button = button,
            )

        assertThat(catalog1).isEqualTo(catalog2)
        assertThat(catalog1.hashCode()).isEqualTo(catalog2.hashCode())
    }

    @Test
    fun equalsAndHashCode_differentCatalogs_doNotMatch() {
        val text1 = TestTextComponent()
        val text2 = TestTextComponent()
        val sharedImage = TestImageComponent()
        val sharedCard = TestCardComponent()
        val sharedRow = TestRowComponent()
        val sharedColumn = TestColumnComponent()
        val sharedButton = TestButtonComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text1,
                image = sharedImage,
                card = sharedCard,
                row = sharedRow,
                column = sharedColumn,
                button = sharedButton,
            )
        val catalog2 =
            createTestBasicCatalog(
                text = text2,
                image = sharedImage,
                card = sharedCard,
                row = sharedRow,
                column = sharedColumn,
                button = sharedButton,
            )

        assertThat(catalog1).isNotEqualTo(catalog2)
        assertThat(catalog1.hashCode()).isNotEqualTo(catalog2.hashCode())
    }

    @Test
    fun toString_containsExpectedProperties() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.toString()).contains("catalogId=${A2uiBasicCatalogV1.CatalogId}")
        assertThat(catalog.toString()).contains("themeSchema=${A2uiBasicCatalogV1.ThemeSchema}")
        assertThat(catalog.toString())
            .containsMatch("components=.*Text.*Image.*Card.*Row.*Column.*Button")
        assertThat(catalog.toString()).contains("functions=[]")
    }

    private fun createTestBasicCatalog(
        text: A2uiBasicCatalogV1.Text = TestTextComponent(),
        image: A2uiBasicCatalogV1.Image = TestImageComponent(),
        card: A2uiBasicCatalogV1.Card = TestCardComponent(),
        row: A2uiBasicCatalogV1.Row = TestRowComponent(),
        column: A2uiBasicCatalogV1.Column = TestColumnComponent(),
        button: A2uiBasicCatalogV1.Button = TestButtonComponent(),
        functions: List<A2uiFunction> = emptyList(),
    ) =
        A2uiBasicCatalogV1(
            text = text,
            image = image,
            card = card,
            row = row,
            column = column,
            button = button,
            functions = functions,
        )

    private class TestTextComponent : A2uiBasicCatalogV1.Text {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            text: String,
            variant: A2uiBasicCatalogV1.Text.Variant,
            modifier: Modifier,
        ) {}
    }

    private class TestImageComponent : A2uiBasicCatalogV1.Image {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            url: String,
            description: String?,
            fit: A2uiBasicCatalogV1.Image.Fit,
            variant: A2uiBasicCatalogV1.Image.Variant,
            modifier: Modifier,
        ) {}
    }

    private class TestCardComponent : A2uiBasicCatalogV1.Card {
        @Composable
        override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
    }

    private class TestRowComponent : A2uiBasicCatalogV1.Row {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: A2uiBasicCatalogV1.Row.Justify,
            align: A2uiBasicCatalogV1.Row.Align,
            modifier: Modifier,
        ) {}
    }

    private class TestColumnComponent : A2uiBasicCatalogV1.Column {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: A2uiBasicCatalogV1.Column.Justify,
            align: A2uiBasicCatalogV1.Column.Align,
            modifier: Modifier,
        ) {}
    }

    private class TestButtonComponent : A2uiBasicCatalogV1.Button {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            childId: String,
            variant: A2uiBasicCatalogV1.Button.Variant,
            action: Map<String, Any?>,
            modifier: Modifier,
        ) {}
    }
}
