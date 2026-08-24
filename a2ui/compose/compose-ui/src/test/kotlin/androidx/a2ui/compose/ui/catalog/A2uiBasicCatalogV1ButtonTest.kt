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
import androidx.a2ui.model.schema.commontypes.A2uiActionSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1ButtonTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val buttonComponent =
            object : A2uiBasicCatalogV1.Button {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    childId: String,
                    variant: A2uiBasicCatalogV1.Button.Variant,
                    action: Map<String, Any?>,
                    modifier: Modifier,
                ) {}
            }

        assertThat(buttonComponent.name).isEqualTo("Button")
        assertThat(buttonComponent.description)
            .isEqualTo("A clickable button that dispatches an action.")
        assertThat(buttonComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Button.ChildProperty,
                A2uiBasicCatalogV1.Button.VariantProperty,
                A2uiBasicCatalogV1.Button.ActionProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Button.ChildProperty.key).isEqualTo("child")
        assertThat(A2uiBasicCatalogV1.Button.ChildProperty.isRequired).isTrue()
        assertIs<A2uiComponentIdSchema>(A2uiBasicCatalogV1.Button.ChildProperty.schema)

        assertThat(A2uiBasicCatalogV1.Button.VariantProperty.key).isEqualTo("variant")
        assertThat(A2uiBasicCatalogV1.Button.VariantProperty.isRequired).isFalse()
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Button.VariantProperty.schema)
        assertThat(variantSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Button.Variant.Default.value))

        assertThat(A2uiBasicCatalogV1.Button.ActionProperty.key).isEqualTo("action")
        assertThat(A2uiBasicCatalogV1.Button.ActionProperty.isRequired).isTrue()
        assertIs<A2uiActionSchema>(A2uiBasicCatalogV1.Button.ActionProperty.schema)
    }

    @Test
    fun variant_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Button.Variant.Default.value).isEqualTo("default")
        assertThat(A2uiBasicCatalogV1.Button.Variant.Primary.value).isEqualTo("primary")
        assertThat(A2uiBasicCatalogV1.Button.Variant.Borderless.value).isEqualTo("borderless")
    }

    @Test
    fun variant_fromValue_validStrings_returnsCorrespondingVariant() {
        assertThat(A2uiBasicCatalogV1.Button.Variant.fromValue("default"))
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Default)
        assertThat(A2uiBasicCatalogV1.Button.Variant.fromValue("primary"))
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Primary)
        assertThat(A2uiBasicCatalogV1.Button.Variant.fromValue("borderless"))
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Borderless)
    }

    @Test
    fun variant_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.Button.Variant.fromValue("invalid_variant"))
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Default)
        assertThat(A2uiBasicCatalogV1.Button.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Default)
    }
}
