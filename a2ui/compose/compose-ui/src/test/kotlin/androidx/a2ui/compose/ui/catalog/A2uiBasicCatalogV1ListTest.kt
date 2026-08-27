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

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiChildListSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1ListTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val listComponent =
            object : A2uiBasicCatalogV1.List {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: kotlin.collections.List<A2uiComponentReference>,
                    direction: A2uiBasicCatalogV1.List.Direction,
                    align: A2uiBasicCatalogV1.List.Align,
                    modifier: Modifier,
                ) {}
            }

        assertThat(listComponent.name).isEqualTo("List")
        assertThat(listComponent.description)
            .isEqualTo("A scrollable list of components laid out vertically or horizontally.")
        assertThat(listComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.List.ChildrenProperty,
                A2uiBasicCatalogV1.List.DirectionProperty,
                A2uiBasicCatalogV1.List.AlignProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.List.ChildrenProperty.key).isEqualTo("children")
        assertThat(A2uiBasicCatalogV1.List.ChildrenProperty.isRequired).isTrue()
        val childrenSchema =
            assertIs<A2uiChildListSchema>(A2uiBasicCatalogV1.List.ChildrenProperty.schema)
        assertThat(childrenSchema.description)
            .isEqualTo(
                "Defines the children. Use an array of strings for a fixed set of " +
                    "children, or a template object to generate children from a data list."
            )

        assertThat(A2uiBasicCatalogV1.List.DirectionProperty.key).isEqualTo("direction")
        assertThat(A2uiBasicCatalogV1.List.DirectionProperty.isRequired).isFalse()
        val directionSchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.List.DirectionProperty.schema)
        assertThat(directionSchema.description)
            .isEqualTo("The direction in which the list items are laid out.")
        assertThat(directionSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("vertical", "horizontal")))
        assertThat(directionSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.List.Direction.Default.value))

        assertThat(A2uiBasicCatalogV1.List.AlignProperty.key).isEqualTo("align")
        assertThat(A2uiBasicCatalogV1.List.AlignProperty.isRequired).isFalse()
        val alignSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.List.AlignProperty.schema)
        assertThat(alignSchema.description)
            .isEqualTo("Defines the alignment of children along the cross axis.")
        assertThat(alignSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("start", "center", "end", "stretch")))
        assertThat(alignSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.List.Align.Default.value))
    }

    @Test
    fun direction_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.List.Direction.Vertical.value).isEqualTo("vertical")
        assertThat(A2uiBasicCatalogV1.List.Direction.Horizontal.value).isEqualTo("horizontal")
    }

    @Test
    fun direction_default_isVertical() {
        assertThat(A2uiBasicCatalogV1.List.Direction.Default)
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)
    }

    @Test
    fun direction_fromValue_validStrings_returnsCorrespondingDirection() {
        assertThat(A2uiBasicCatalogV1.List.Direction.fromValue("vertical"))
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)
        assertThat(A2uiBasicCatalogV1.List.Direction.fromValue("horizontal"))
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Horizontal)
    }

    @Test
    fun direction_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.List.Direction.fromValue("invalid_direction"))
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Default)
        assertThat(A2uiBasicCatalogV1.List.Direction.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Default)
    }

    @Test
    fun align_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.List.Align.Start.value).isEqualTo("start")
        assertThat(A2uiBasicCatalogV1.List.Align.Center.value).isEqualTo("center")
        assertThat(A2uiBasicCatalogV1.List.Align.End.value).isEqualTo("end")
        assertThat(A2uiBasicCatalogV1.List.Align.Stretch.value).isEqualTo("stretch")
    }

    @Test
    fun align_default_isStretch() {
        assertThat(A2uiBasicCatalogV1.List.Align.Default)
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Stretch)
    }

    @Test
    fun align_fromValue_validStrings_returnsCorrespondingAlign() {
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue("start"))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Start)
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue("center"))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Center)
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue("end"))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.End)
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue("stretch"))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Stretch)
    }

    @Test
    fun align_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue("invalid_align"))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Default)
        assertThat(A2uiBasicCatalogV1.List.Align.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.List.Align.Default)
    }
}
