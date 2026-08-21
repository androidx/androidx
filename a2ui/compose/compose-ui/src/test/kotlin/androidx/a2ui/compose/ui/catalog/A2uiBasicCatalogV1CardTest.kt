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
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1CardTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val cardComponent =
            object : A2uiBasicCatalogV1.Card {
                @Composable
                override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
            }

        assertThat(cardComponent.name).isEqualTo("Card")
        assertThat(cardComponent.description).contains("styled card container")
        assertThat(cardComponent.properties)
            .containsExactly(A2uiBasicCatalogV1.Card.ChildProperty)
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Card.ChildProperty.key).isEqualTo("child")
        assertThat(A2uiBasicCatalogV1.Card.ChildProperty.isRequired).isTrue()
        assertIs<A2uiComponentIdSchema>(A2uiBasicCatalogV1.Card.ChildProperty.schema)
    }
}
