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
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.commontypes.A2uiDynamicNumberSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1SliderTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val sliderComponent =
            object : A2uiBasicCatalogV1.Slider {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String?,
                    min: Float,
                    max: Float,
                    value: Float,
                    onValueChange: (Float) -> Unit,
                    enabled: Boolean,
                    modifier: Modifier,
                ) {}
            }

        assertThat(sliderComponent.name).isEqualTo("Slider")
        assertThat(sliderComponent.description)
            .isEqualTo("A slider for selecting a numeric value within a range.")
        assertThat(sliderComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Slider.LabelProperty,
                A2uiBasicCatalogV1.Slider.MinProperty,
                A2uiBasicCatalogV1.Slider.MaxProperty,
                A2uiBasicCatalogV1.Slider.ValueProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Slider.LabelProperty.key).isEqualTo("label")
        assertThat(A2uiBasicCatalogV1.Slider.LabelProperty.isRequired).isFalse()
        val labelSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.Slider.LabelProperty.schema)
        assertThat(labelSchema.description).isEqualTo("The label for the slider.")

        assertThat(A2uiBasicCatalogV1.Slider.MinProperty.key).isEqualTo("min")
        assertThat(A2uiBasicCatalogV1.Slider.MinProperty.isRequired).isFalse()
        val minSchema = assertIs<A2uiNumberSchema>(A2uiBasicCatalogV1.Slider.MinProperty.schema)
        assertThat(minSchema.description).isEqualTo("The minimum value of the slider.")
        assertThat(minSchema.keywords).containsExactly(A2uiSchemaKeyword.Default(0))

        assertThat(A2uiBasicCatalogV1.Slider.MaxProperty.key).isEqualTo("max")
        assertThat(A2uiBasicCatalogV1.Slider.MaxProperty.isRequired).isTrue()
        val maxSchema = assertIs<A2uiNumberSchema>(A2uiBasicCatalogV1.Slider.MaxProperty.schema)
        assertThat(maxSchema.description).isEqualTo("The maximum value of the slider.")

        assertThat(A2uiBasicCatalogV1.Slider.ValueProperty.key).isEqualTo("value")
        assertThat(A2uiBasicCatalogV1.Slider.ValueProperty.isRequired).isTrue()
        val valueSchema =
            assertIs<A2uiDynamicNumberSchema>(A2uiBasicCatalogV1.Slider.ValueProperty.schema)
        assertThat(valueSchema.description).isEqualTo("The current value of the slider.")
    }
}
