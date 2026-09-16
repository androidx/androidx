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
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1ColumnTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val columnComponent =
            object : A2uiBasicCatalogV1.Column {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Column.Justify,
                    align: A2uiBasicCatalogV1.Column.Align,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        assertThat(columnComponent.name).isEqualTo("Column")
        assertThat(columnComponent.description)
            .isEqualTo(
                "A layout component that arranges its children vertically. To create a grid " +
                    "layout, nest Rows within this Column."
            )
        assertThat(columnComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Column.AccessibilityProperty,
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Column.ChildrenProperty,
                A2uiBasicCatalogV1.Column.JustifyProperty,
                A2uiBasicCatalogV1.Column.AlignProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        val justifySchema =
            assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Column.JustifyProperty.schema)
        val alignSchema = assertIs<A2uiStringSchema>(A2uiBasicCatalogV1.Column.AlignProperty.schema)

        assertThat(A2uiBasicCatalogV1.Column.AccessibilityProperty.key).isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.Column.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.Column.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)

        assertThat(A2uiBasicCatalogV1.Column.ChildrenProperty.key).isEqualTo("children")
        assertThat(A2uiBasicCatalogV1.Column.JustifyProperty.key).isEqualTo("justify")
        assertThat(justifySchema.keywords)
            .contains(
                A2uiSchemaKeyword.Enum(
                    listOf(
                        "start",
                        "center",
                        "end",
                        "spaceBetween",
                        "spaceAround",
                        "spaceEvenly",
                        "stretch",
                    )
                )
            )
        assertThat(justifySchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Column.Justify.Start.value))
        assertThat(A2uiBasicCatalogV1.Column.AlignProperty.key).isEqualTo("align")
        assertThat(alignSchema.keywords)
            .contains(A2uiSchemaKeyword.Enum(listOf("center", "end", "start", "stretch")))
        assertThat(alignSchema.keywords)
            .contains(A2uiSchemaKeyword.Default(A2uiBasicCatalogV1.Column.Align.Start.value))
    }

    @Test
    fun justify_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Column.Justify.Start.value).isEqualTo("start")
        assertThat(A2uiBasicCatalogV1.Column.Justify.Center.value).isEqualTo("center")
        assertThat(A2uiBasicCatalogV1.Column.Justify.End.value).isEqualTo("end")
        assertThat(A2uiBasicCatalogV1.Column.Justify.SpaceBetween.value).isEqualTo("spaceBetween")
        assertThat(A2uiBasicCatalogV1.Column.Justify.SpaceAround.value).isEqualTo("spaceAround")
        assertThat(A2uiBasicCatalogV1.Column.Justify.SpaceEvenly.value).isEqualTo("spaceEvenly")
        assertThat(A2uiBasicCatalogV1.Column.Justify.Stretch.value).isEqualTo("stretch")
    }

    @Test
    fun justify_fromValue_validStrings_returnsCorrespondingJustify() {
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("center"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Center)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("end"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.End)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("spaceAround"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceAround)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("spaceBetween"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceBetween)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("spaceEvenly"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceEvenly)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("start"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("stretch"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Stretch)
    }

    @Test
    fun justify_default_isStart() {
        assertThat(A2uiBasicCatalogV1.Column.Justify.Default)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)
    }

    @Test
    fun justify_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("invalid_justify"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Default)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Default)
    }

    @Test
    fun align_values_matchSpecificationStrings() {
        assertThat(A2uiBasicCatalogV1.Column.Align.Center.value).isEqualTo("center")
        assertThat(A2uiBasicCatalogV1.Column.Align.End.value).isEqualTo("end")
        assertThat(A2uiBasicCatalogV1.Column.Align.Start.value).isEqualTo("start")
        assertThat(A2uiBasicCatalogV1.Column.Align.Stretch.value).isEqualTo("stretch")
    }

    @Test
    fun align_fromValue_validStrings_returnsCorrespondingAlign() {
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("center"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Center)
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("end"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.End)
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("start"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Start)
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("stretch"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)
    }

    @Test
    fun align_default_isStart() {
        assertThat(A2uiBasicCatalogV1.Column.Align.Default)
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Start)
    }

    @Test
    fun align_fromValue_invalidOrEmptyString_fallsBackToDefault() {
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("invalid_align"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Default)
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Default)
    }
}
