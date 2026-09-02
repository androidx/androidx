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
class A2uiBasicCatalogV1ImageTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val imageComponent =
            object : A2uiBasicCatalogV1.Image {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    url: String,
                    description: String?,
                    fit: A2uiBasicCatalogV1.Image.Fit,
                    variant: A2uiBasicCatalogV1.Image.Variant,
                    modifier: Modifier,
                ) {}
            }

        assertThat(imageComponent.name).isEqualTo("Image")
        assertThat(imageComponent.description).isEqualTo("Displays an image from a URL.")
        assertThat(imageComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Image.UrlProperty,
                A2uiBasicCatalogV1.Image.DescriptionProperty,
                A2uiBasicCatalogV1.Image.FitProperty,
                A2uiBasicCatalogV1.Image.VariantProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Image.UrlProperty.key).isEqualTo("url")
        assertThat(A2uiBasicCatalogV1.Image.UrlProperty.isRequired).isTrue()
        val urlSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.Image.UrlProperty.schema)
        assertThat(urlSchema.description).isEqualTo("The URL of the image to display.")

        assertThat(A2uiBasicCatalogV1.Image.DescriptionProperty.key).isEqualTo("description")
        assertThat(A2uiBasicCatalogV1.Image.DescriptionProperty.isRequired).isFalse()
        val descriptionSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.Image.DescriptionProperty.schema)
        assertThat(descriptionSchema.description).isEqualTo("Accessibility text for the image.")

        assertThat(A2uiBasicCatalogV1.Image.FitProperty.key).isEqualTo("fit")
        assertThat(A2uiBasicCatalogV1.Image.FitProperty.isRequired).isFalse()
        val fitSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Image.FitProperty.schema)
        assertThat(fitSchema.description)
            .isEqualTo(
                "Specifies how the image should be resized to fit its container. " +
                    "This corresponds to the CSS 'object-fit' property."
            )
        assertThat(fitSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Enum(listOf("contain", "cover", "fill", "none", "scaleDown"))
            )
        assertThat(fitSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Image.Fit.Fill.value))

        assertThat(A2uiBasicCatalogV1.Image.VariantProperty.key).isEqualTo("variant")
        assertThat(A2uiBasicCatalogV1.Image.VariantProperty.isRequired).isFalse()
        val variantSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Image.VariantProperty.schema)
        assertThat(variantSchema.description).isEqualTo("A hint for the image size and style.")
        assertThat(variantSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Enum(
                    listOf(
                        "icon",
                        "avatar",
                        "smallFeature",
                        "mediumFeature",
                        "largeFeature",
                        "header",
                    )
                )
            )
        assertThat(variantSchema.keywords)
            .contains(
                A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Image.Variant.MediumFeature.value)
            )
    }

    @Test
    fun fit_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Image.Fit.Contain.value).isEqualTo("contain")
        assertThat(A2uiBasicCatalogV1.Image.Fit.Cover.value).isEqualTo("cover")
        assertThat(A2uiBasicCatalogV1.Image.Fit.Fill.value).isEqualTo("fill")
        assertThat(A2uiBasicCatalogV1.Image.Fit.None.value).isEqualTo("none")
        assertThat(A2uiBasicCatalogV1.Image.Fit.ScaleDown.value).isEqualTo("scaleDown")
    }

    @Test
    fun fit_fromValue_validStrings_returnsCorrespondingFit() {
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("contain"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.Contain)
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("cover"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.Cover)
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("fill"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.Fill)
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("none"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.None)
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("scaleDown"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.ScaleDown)
    }

    @Test
    fun fit_fromValue_invalidOrEmptyString_fallsBackToFill() {
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue("invalid_fit"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.Fill)
        assertThat(A2uiBasicCatalogV1.Image.Fit.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Image.Fit.Fill)
    }

    @Test
    fun variant_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Image.Variant.Icon.value).isEqualTo("icon")
        assertThat(A2uiBasicCatalogV1.Image.Variant.Avatar.value).isEqualTo("avatar")
        assertThat(A2uiBasicCatalogV1.Image.Variant.SmallFeature.value).isEqualTo("smallFeature")
        assertThat(A2uiBasicCatalogV1.Image.Variant.MediumFeature.value).isEqualTo("mediumFeature")
        assertThat(A2uiBasicCatalogV1.Image.Variant.LargeFeature.value).isEqualTo("largeFeature")
        assertThat(A2uiBasicCatalogV1.Image.Variant.Header.value).isEqualTo("header")
    }

    @Test
    fun variant_fromValue_validStrings_returnsCorrespondingVariant() {
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("icon"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.Icon)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("avatar"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.Avatar)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("smallFeature"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.SmallFeature)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("mediumFeature"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.MediumFeature)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("largeFeature"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.LargeFeature)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("header"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.Header)
    }

    @Test
    fun variant_fromValue_invalidOrEmptyString_fallsBackToMediumFeature() {
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue("invalid_variant"))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.MediumFeature)
        assertThat(A2uiBasicCatalogV1.Image.Variant.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Image.Variant.MediumFeature)
    }
}
