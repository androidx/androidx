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
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1TextFieldTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val textFieldComponent =
            object : A2uiBasicCatalogV1.TextField {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String,
                    value: String?,
                    variant: A2uiBasicCatalogV1.TextField.Variant,
                    validationRegexp: String?,
                    onValueChange: (String) -> Unit,
                    enabled: Boolean,
                    modifier: Modifier,
                ) {}
            }

        assertThat(textFieldComponent.name).isEqualTo("TextField")
        assertThat(textFieldComponent.description).isEqualTo("A field for user text input.")
        assertThat(textFieldComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.TextField.LabelProperty,
                A2uiBasicCatalogV1.TextField.ValueProperty,
                A2uiBasicCatalogV1.TextField.VariantProperty,
                A2uiBasicCatalogV1.TextField.ValidationRegexpProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.TextField.LabelProperty.key).isEqualTo("label")
        assertThat(A2uiBasicCatalogV1.TextField.LabelProperty.isRequired).isTrue()
        val labelSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.TextField.LabelProperty.schema)
        assertThat(labelSchema.description).isEqualTo("The text label for the input field.")

        assertThat(A2uiBasicCatalogV1.TextField.ValueProperty.key).isEqualTo("value")
        assertThat(A2uiBasicCatalogV1.TextField.ValueProperty.isRequired).isFalse()
        val valueSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.TextField.ValueProperty.schema)
        assertThat(valueSchema.description).isEqualTo("The value of the text field.")

        assertThat(A2uiBasicCatalogV1.TextField.VariantProperty.key).isEqualTo("variant")
        assertThat(A2uiBasicCatalogV1.TextField.VariantProperty.isRequired).isFalse()
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.TextField.VariantProperty.schema)
        assertThat(variantSchema.description).isEqualTo("The type of input field to display.")
        assertThat(variantSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("longText", "number", "shortText", "obscured")))
        assertThat(variantSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.TextField.Variant.ShortText.value)
            )

        assertThat(A2uiBasicCatalogV1.TextField.ValidationRegexpProperty.key)
            .isEqualTo("validationRegexp")
        assertThat(A2uiBasicCatalogV1.TextField.ValidationRegexpProperty.isRequired).isFalse()
        val validationRegexpSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.TextField.ValidationRegexpProperty.schema)
        assertThat(validationRegexpSchema.description)
            .isEqualTo("A regular expression used for client-side validation of the input.")
        assertThat(validationRegexpSchema.keywords).isEmpty()
    }

    @Test
    fun companionProperties_schemaDefault_matchesDefaultVariant() {
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.TextField.VariantProperty.schema)
        val defaultKeyword =
            variantSchema.keywords
                ?.filterIsInstance<A2uiSchemaKeyword.Default<String>>()
                ?.singleOrNull()
        assertThat(defaultKeyword).isNotNull()
        assertThat(defaultKeyword?.value)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Default.value)
    }

    @Test
    fun companionProperties_schemaKeywords_containsExactlyExpectedKeywords() {
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.TextField.VariantProperty.schema)
        assertThat(variantSchema.keywords)
            .containsExactly(
                A2uiSchemaKeyword.Enum(listOf("longText", "number", "shortText", "obscured")),
                A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.TextField.Variant.Default.value),
            )
    }

    @Test
    fun companionProperties_dynamicStringProperties_haveNoKeywordsOrDefault() {
        val labelSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.TextField.LabelProperty.schema)
        assertThat(labelSchema.keywords).isEmpty()

        val valueSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.TextField.ValueProperty.schema)
        assertThat(valueSchema.keywords).isEmpty()
    }

    @Test
    fun variant_entries_containsExactEnumEntriesInOrder() {
        assertThat(A2uiBasicCatalogV1.TextField.Variant.entries)
            .containsExactly(
                A2uiBasicCatalogV1.TextField.Variant.LongText,
                A2uiBasicCatalogV1.TextField.Variant.Number,
                A2uiBasicCatalogV1.TextField.Variant.ShortText,
                A2uiBasicCatalogV1.TextField.Variant.Obscured,
            )
            .inOrder()
    }

    @Test
    fun variant_entries_haveUniqueValues() {
        val values = A2uiBasicCatalogV1.TextField.Variant.entries.map { it.value }
        assertThat(values).hasSize(4)
        assertThat(values.distinct()).hasSize(4)
    }

    @Test
    fun variant_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.TextField.Variant.LongText.value).isEqualTo("longText")
        assertThat(A2uiBasicCatalogV1.TextField.Variant.Number.value).isEqualTo("number")
        assertThat(A2uiBasicCatalogV1.TextField.Variant.ShortText.value).isEqualTo("shortText")
        assertThat(A2uiBasicCatalogV1.TextField.Variant.Obscured.value).isEqualTo("obscured")
    }

    @Test
    fun variant_default_isShortText() {
        assertThat(A2uiBasicCatalogV1.TextField.Variant.Default)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
    }

    @Test
    fun variant_fromValue_validStrings_returnsCorrespondingVariant() {
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue("longText"))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.LongText)
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue("number"))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Number)
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue("shortText"))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue("obscured"))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Obscured)
    }

    @Test
    fun variant_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue("invalid_variant"))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
        assertThat(A2uiBasicCatalogV1.TextField.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
    }
}
