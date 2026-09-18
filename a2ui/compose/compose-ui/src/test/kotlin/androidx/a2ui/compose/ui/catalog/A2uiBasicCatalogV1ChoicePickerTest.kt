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
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiBooleanSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiCheckRuleSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringListSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1ChoicePickerTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val choicePickerComponent =
            object : A2uiBasicCatalogV1.ChoicePicker {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String?,
                    options: List<A2uiBasicCatalogV1.ChoicePicker.Option>,
                    value: List<String>,
                    variant: A2uiBasicCatalogV1.ChoicePicker.Variant,
                    displayStyle: A2uiBasicCatalogV1.ChoicePicker.DisplayStyle,
                    filterable: Boolean,
                    onValueChange: (List<String>) -> Unit,
                    enabled: Boolean,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    checks: List<A2uiBasicCatalogV1.CheckRule>,
                    modifier: Modifier,
                ) {}
            }

        assertThat(choicePickerComponent.name).isEqualTo("ChoicePicker")
        assertThat(choicePickerComponent.description)
            .isEqualTo("A component that allows selecting one or more options from a list.")
        assertThat(choicePickerComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.ChoicePicker.AccessibilityProperty,
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.ChoicePicker.ChecksProperty,
                A2uiBasicCatalogV1.ChoicePicker.LabelProperty,
                A2uiBasicCatalogV1.ChoicePicker.VariantProperty,
                A2uiBasicCatalogV1.ChoicePicker.OptionsProperty,
                A2uiBasicCatalogV1.ChoicePicker.ValueProperty,
                A2uiBasicCatalogV1.ChoicePicker.DisplayStyleProperty,
                A2uiBasicCatalogV1.ChoicePicker.FilterableProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.AccessibilityProperty.key)
            .isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.ChoicePicker.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)

        assertThat(A2uiBasicCatalogV1.ChoicePicker.ChecksProperty.key).isEqualTo("checks")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.ChecksProperty.isRequired).isFalse()
        val checksSchema =
            assertIs<A2uiArraySchema>(A2uiBasicCatalogV1.ChoicePicker.ChecksProperty.schema)
        assertThat(checksSchema.items).isEqualTo(A2uiCheckRuleSchema.DEFAULT_INSTANCE)

        assertThat(A2uiBasicCatalogV1.ChoicePicker.LabelProperty.key).isEqualTo("label")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.LabelProperty.isRequired).isFalse()
        val labelSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.ChoicePicker.LabelProperty.schema)
        assertThat(labelSchema.description).isEqualTo("The label for the group of options.")

        assertThat(A2uiBasicCatalogV1.ChoicePicker.VariantProperty.key).isEqualTo("variant")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.VariantProperty.isRequired).isFalse()
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.ChoicePicker.VariantProperty.schema)
        assertThat(variantSchema.description)
            .isEqualTo("A hint for how the choice picker should be displayed and behave.")
        assertThat(variantSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("multipleSelection", "mutuallyExclusive")))
        assertThat(variantSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Default(
                    A2uiBasicCatalogV1.ChoicePicker.Variant.MutuallyExclusive.value
                )
            )

        assertThat(A2uiBasicCatalogV1.ChoicePicker.OptionsProperty.key).isEqualTo("options")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.OptionsProperty.isRequired).isTrue()
        val optionsSchema =
            assertIs<A2uiArraySchema>(A2uiBasicCatalogV1.ChoicePicker.OptionsProperty.schema)
        assertThat(optionsSchema.description)
            .isEqualTo("The list of available options to choose from.")

        val itemsSchema = assertIs<A2uiObjectSchema>(optionsSchema.items)
        assertThat(itemsSchema.isAdditionalPropertiesAllowed).isFalse()
        assertThat(itemsSchema.required).containsExactly("label", "value")
        assertThat(itemsSchema.properties.keys).containsExactly("label", "value")

        val optionLabelSchema = assertIs<A2uiDynamicStringSchema>(itemsSchema.properties["label"])
        assertThat(optionLabelSchema.description).isEqualTo("The text to display for this option.")

        val optionValueSchema = assertIs<A2uiStringSchema>(itemsSchema.properties["value"])
        assertThat(optionValueSchema.description)
            .isEqualTo("The stable value associated with this option.")

        assertThat(A2uiBasicCatalogV1.ChoicePicker.ValueProperty.key).isEqualTo("value")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.ValueProperty.isRequired).isTrue()
        val valueSchema =
            assertIs<A2uiDynamicStringListSchema>(
                A2uiBasicCatalogV1.ChoicePicker.ValueProperty.schema
            )
        assertThat(valueSchema.description)
            .isEqualTo(
                "The list of currently selected values. This should be bound to a " +
                    "string array in the data model."
            )

        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyleProperty.key)
            .isEqualTo("displayStyle")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyleProperty.isRequired).isFalse()
        val displayStyleSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.ChoicePicker.DisplayStyleProperty.schema)
        assertThat(displayStyleSchema.description).isEqualTo("The display style of the component.")
        assertThat(displayStyleSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("checkbox", "chips")))
        assertThat(displayStyleSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Default(
                    A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Checkbox.value
                )
            )

        assertThat(A2uiBasicCatalogV1.ChoicePicker.FilterableProperty.key).isEqualTo("filterable")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.FilterableProperty.isRequired).isFalse()
        val filterableSchema =
            assertIs<A2uiBooleanSchema>(A2uiBasicCatalogV1.ChoicePicker.FilterableProperty.schema)
        assertThat(filterableSchema.description)
            .isEqualTo("If true, displays a search input to filter the options.")
        assertThat(filterableSchema.keywords).contains(A2uiSchemaKeyword.Default(false))
    }

    @Test
    fun option_properties_matchConstructorArguments() {
        val option = A2uiBasicCatalogV1.ChoicePicker.Option(label = "Label 1", value = "val_1")
        assertThat(option.label).isEqualTo("Label 1")
        assertThat(option.value).isEqualTo("val_1")
    }

    @Test
    fun option_equalsAndHashCode_contracts() {
        val option1 = A2uiBasicCatalogV1.ChoicePicker.Option("Label 1", "val_1")
        val option2 = A2uiBasicCatalogV1.ChoicePicker.Option("Label 1", "val_1")
        val option3 = A2uiBasicCatalogV1.ChoicePicker.Option("Label 2", "val_1")
        val option4 = A2uiBasicCatalogV1.ChoicePicker.Option("Label 1", "val_2")

        assertThat(option1).isEqualTo(option2)
        assertThat(option1.hashCode()).isEqualTo(option2.hashCode())
        assertThat(option1).isNotEqualTo(option3)
        assertThat(option1).isNotEqualTo(option4)
    }

    @Test
    fun option_equals_handlesNullAndOtherTypes() {
        val option = A2uiBasicCatalogV1.ChoicePicker.Option("Label 1", "val_1")
        assertThat(option.equals(option)).isTrue()
        assertThat(option.equals(null)).isFalse()
        assertThat(option.equals("Label 1")).isFalse()
        assertThat(option.equals(Any())).isFalse()
    }

    @Test
    fun option_toString_returnsExpectedFormat() {
        val option = A2uiBasicCatalogV1.ChoicePicker.Option("Label 1", "val_1")
        assertThat(option.toString()).isEqualTo("Option(label='Label 1', value='val_1')")
    }

    @Test
    fun variant_default_isMutuallyExclusive() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.Default)
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.MutuallyExclusive)
    }

    @Test
    fun variant_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.entries)
            .containsExactly(
                A2uiBasicCatalogV1.ChoicePicker.Variant.MultipleSelection,
                A2uiBasicCatalogV1.ChoicePicker.Variant.MutuallyExclusive,
            )
            .inOrder()
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.MultipleSelection.value)
            .isEqualTo("multipleSelection")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.MutuallyExclusive.value)
            .isEqualTo("mutuallyExclusive")
    }

    @Test
    fun variant_fromValue_validStrings_returnsCorrespondingVariant() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.fromValue("mutuallyExclusive"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.MutuallyExclusive)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.fromValue("multipleSelection"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.MultipleSelection)
    }

    @Test
    fun variant_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.fromValue("unknown"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.Default)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.Default)
    }

    @Test
    fun displayStyle_default_isCheckbox() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Default)
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Checkbox)
    }

    @Test
    fun displayStyle_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Checkbox.value)
            .isEqualTo("checkbox")
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Chips.value).isEqualTo("chips")
    }

    @Test
    fun displayStyle_fromValue_validStrings_returnsCorrespondingDisplayStyle() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.fromValue("checkbox"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Checkbox)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.fromValue("chips"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Chips)
    }

    @Test
    fun displayStyle_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.fromValue("unknown"))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Default)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Default)
    }
}
