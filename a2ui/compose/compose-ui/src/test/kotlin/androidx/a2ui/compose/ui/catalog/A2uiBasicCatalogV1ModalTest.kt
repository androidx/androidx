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
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1ModalTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val modalComponent =
            object : A2uiBasicCatalogV1.Modal {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    triggerId: String,
                    contentId: String,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        assertThat(modalComponent.name).isEqualTo("Modal")
        assertThat(modalComponent.description).isEqualTo("A dialog window.")
        assertThat(modalComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Modal.AccessibilityProperty,
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Modal.TriggerProperty,
                A2uiBasicCatalogV1.Modal.ContentProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Modal.AccessibilityProperty.key).isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.Modal.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.Modal.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)

        assertThat(A2uiBasicCatalogV1.Modal.TriggerProperty.key).isEqualTo("trigger")
        assertThat(A2uiBasicCatalogV1.Modal.TriggerProperty.isRequired).isTrue()
        assertIs<A2uiComponentIdSchema>(A2uiBasicCatalogV1.Modal.TriggerProperty.schema)

        assertThat(A2uiBasicCatalogV1.Modal.ContentProperty.key).isEqualTo("content")
        assertThat(A2uiBasicCatalogV1.Modal.ContentProperty.isRequired).isTrue()
        assertIs<A2uiComponentIdSchema>(A2uiBasicCatalogV1.Modal.ContentProperty.schema)
    }
}
