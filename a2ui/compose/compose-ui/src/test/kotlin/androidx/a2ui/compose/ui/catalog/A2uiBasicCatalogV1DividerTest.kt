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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1DividerTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val dividerComponent =
            object : A2uiBasicCatalogV1.Divider {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    axis: A2uiBasicCatalogV1.Divider.Axis,
                    modifier: Modifier,
                ) {}
            }

        assertThat(dividerComponent.name).isEqualTo("Divider")
        assertThat(dividerComponent.description)
            .isEqualTo("A horizontal or vertical dividing line.")
        assertThat(dividerComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Divider.AxisProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Divider.AxisProperty.key).isEqualTo("axis")
        assertThat(A2uiBasicCatalogV1.Divider.AxisProperty.isRequired).isFalse()
        val axisSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Divider.AxisProperty.schema)
        assertThat(axisSchema.description).isEqualTo("The orientation of the divider.")
        assertThat(axisSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("horizontal", "vertical")))
        assertThat(axisSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Divider.Axis.Default.value))
    }

    @Test
    fun companionProperties_schemaDefault_matchesDefaultAxis() {
        val axisSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Divider.AxisProperty.schema)
        val defaultKeyword =
            axisSchema.keywords
                ?.filterIsInstance<A2uiSchemaKeyword.Default<String>>()
                ?.singleOrNull()
        assertThat(defaultKeyword).isNotNull()
        assertThat(defaultKeyword?.value).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Default.value)
    }

    @Test
    fun companionProperties_schemaKeywords_containsExactlyExpectedKeywords() {
        val axisSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Divider.AxisProperty.schema)
        assertThat(axisSchema.keywords)
            .containsExactly(
                A2uiSchemaKeyword.Enum(listOf("horizontal", "vertical")),
                A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Divider.Axis.Default.value),
            )
    }

    @Test
    fun axis_entries_containsExactEnumEntriesInOrder() {
        assertThat(A2uiBasicCatalogV1.Divider.Axis.entries)
            .containsExactly(
                A2uiBasicCatalogV1.Divider.Axis.Horizontal,
                A2uiBasicCatalogV1.Divider.Axis.Vertical,
            )
            .inOrder()
    }

    @Test
    fun axis_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Divider.Axis.Horizontal.value).isEqualTo("horizontal")
        assertThat(A2uiBasicCatalogV1.Divider.Axis.Vertical.value).isEqualTo("vertical")
    }

    @Test
    fun axis_default_isHorizontal() {
        assertThat(A2uiBasicCatalogV1.Divider.Axis.Default)
            .isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)
    }

    @Test
    fun axis_fromValue_validStrings_returnsCorrespondingAxis() {
        assertThat(A2uiBasicCatalogV1.Divider.Axis.fromValue("horizontal"))
            .isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)
        assertThat(A2uiBasicCatalogV1.Divider.Axis.fromValue("vertical"))
            .isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Vertical)
    }

    @Test
    fun axis_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.Divider.Axis.fromValue("invalid_axis"))
            .isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Default)
        assertThat(A2uiBasicCatalogV1.Divider.Axis.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Default)
    }
}
