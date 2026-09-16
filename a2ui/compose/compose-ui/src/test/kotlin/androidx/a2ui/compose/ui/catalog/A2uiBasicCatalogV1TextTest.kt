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
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1TextTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val textComponent =
            object : A2uiBasicCatalogV1.Text {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    text: String,
                    variant: A2uiBasicCatalogV1.Text.Variant,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        assertThat(textComponent.name).isEqualTo("Text")
        assertThat(textComponent.description).isEqualTo("Displays dynamic text.")
        assertThat(textComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Text.AccessibilityProperty,
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Text.TextProperty,
                A2uiBasicCatalogV1.Text.VariantProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Text.AccessibilityProperty.key).isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.Text.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.Text.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)

        assertThat(A2uiBasicCatalogV1.Text.TextProperty.key).isEqualTo("text")
        assertThat(A2uiBasicCatalogV1.Text.VariantProperty.key).isEqualTo("variant")
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Text.VariantProperty.schema)
        assertThat(variantSchema.description).isEqualTo("A hint for the base text style.")
        assertThat(variantSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Enum(listOf("h1", "h2", "h3", "h4", "h5", "caption", "body"))
            )
        assertThat(variantSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Text.Variant.Body.value))
    }

    @Test
    fun variant_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Text.Variant.H1.value).isEqualTo("h1")
        assertThat(A2uiBasicCatalogV1.Text.Variant.H2.value).isEqualTo("h2")
        assertThat(A2uiBasicCatalogV1.Text.Variant.H3.value).isEqualTo("h3")
        assertThat(A2uiBasicCatalogV1.Text.Variant.H4.value).isEqualTo("h4")
        assertThat(A2uiBasicCatalogV1.Text.Variant.H5.value).isEqualTo("h5")
        assertThat(A2uiBasicCatalogV1.Text.Variant.Caption.value).isEqualTo("caption")
        assertThat(A2uiBasicCatalogV1.Text.Variant.Body.value).isEqualTo("body")
    }

    @Test
    fun variant_default_isBody() {
        assertThat(A2uiBasicCatalogV1.Text.Variant.Default)
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Body)
    }

    @Test
    fun variant_fromValue_validStrings_returnsCorrespondingVariant() {
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("h1"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.H1)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("h2"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.H2)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("h3"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.H3)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("h4"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.H4)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("h5"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.H5)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("caption"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Caption)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("body"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Body)
    }

    @Test
    fun variant_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("invalid_variant"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Default)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Default)
    }
}
