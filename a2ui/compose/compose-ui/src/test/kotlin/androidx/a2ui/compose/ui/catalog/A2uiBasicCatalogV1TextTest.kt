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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
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
                    modifier: Modifier,
                ) {}
            }

        assertThat(textComponent.name).isEqualTo("Text")
        assertThat(textComponent.description).isEqualTo("Displays dynamic text.")
        assertThat(textComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Text.textProperty,
                A2uiBasicCatalogV1.Text.variantProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedKeys() {
        assertThat(A2uiBasicCatalogV1.Text.textProperty.key).isEqualTo("text")
        assertThat(A2uiBasicCatalogV1.Text.variantProperty.key).isEqualTo("variant")
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
    fun variant_fromValue_invalidOrEmptyString_fallsBackToBody() {
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue("invalid_variant"))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Body)
        assertThat(A2uiBasicCatalogV1.Text.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Text.Variant.Body)
    }
}
