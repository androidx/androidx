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
import androidx.a2ui.model.schema.commontypes.A2uiDynamicBooleanSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1CheckBoxTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val checkBoxComponent =
            object : A2uiBasicCatalogV1.CheckBox {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String,
                    value: Boolean,
                    onValueChange: (Boolean) -> Unit,
                    enabled: Boolean,
                    modifier: Modifier,
                ) {}
            }

        assertThat(checkBoxComponent.name).isEqualTo("CheckBox")
        assertThat(checkBoxComponent.description)
            .isEqualTo("A checkbox with a label and a boolean value.")
        assertThat(checkBoxComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.CheckBox.LabelProperty,
                A2uiBasicCatalogV1.CheckBox.ValueProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.CheckBox.LabelProperty.key).isEqualTo("label")
        assertThat(A2uiBasicCatalogV1.CheckBox.LabelProperty.isRequired).isTrue()
        val labelSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.CheckBox.LabelProperty.schema)
        assertThat(labelSchema.description).isEqualTo("The text to display next to the checkbox.")

        assertThat(A2uiBasicCatalogV1.CheckBox.ValueProperty.key).isEqualTo("value")
        assertThat(A2uiBasicCatalogV1.CheckBox.ValueProperty.isRequired).isTrue()
        val valueSchema =
            assertIs<A2uiDynamicBooleanSchema>(A2uiBasicCatalogV1.CheckBox.ValueProperty.schema)
        assertThat(valueSchema.description)
            .isEqualTo("The current state of the checkbox (true for checked, false for unchecked).")
    }
}
