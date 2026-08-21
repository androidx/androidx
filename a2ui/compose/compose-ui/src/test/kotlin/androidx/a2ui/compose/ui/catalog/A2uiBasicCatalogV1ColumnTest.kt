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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
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
                A2uiBasicCatalogV1.Column.ChildrenProperty,
                A2uiBasicCatalogV1.Column.JustifyProperty,
                A2uiBasicCatalogV1.Column.AlignProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedKeys() {
        assertThat(A2uiBasicCatalogV1.Column.ChildrenProperty.key).isEqualTo("children")
        assertThat(A2uiBasicCatalogV1.Column.JustifyProperty.key).isEqualTo("justify")
        assertThat(A2uiBasicCatalogV1.Column.AlignProperty.key).isEqualTo("align")
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
    fun justify_fromValue_invalidOrEmptyString_fallsBackToStart() {
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue("invalid_justify"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)
        assertThat(A2uiBasicCatalogV1.Column.Justify.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)
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
    fun align_fromValue_invalidOrEmptyString_fallsBackToStretch() {
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue("invalid_align"))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)
        assertThat(A2uiBasicCatalogV1.Column.Align.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)
    }
}
