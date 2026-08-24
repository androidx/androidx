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
class A2uiBasicCatalogV1RowTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val rowComponent =
            object : A2uiBasicCatalogV1.Row {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Row.Justify,
                    align: A2uiBasicCatalogV1.Row.Align,
                    modifier: Modifier,
                ) {}
            }

        assertThat(rowComponent.name).isEqualTo("Row")
        assertThat(rowComponent.description)
            .isEqualTo(
                "A layout component that arranges its children horizontally. To create a grid " +
                    "layout, nest Columns within this Row."
            )
        assertThat(rowComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.Row.ChildrenProperty,
                A2uiBasicCatalogV1.Row.JustifyProperty,
                A2uiBasicCatalogV1.Row.AlignProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedKeys() {
        assertThat(A2uiBasicCatalogV1.Row.ChildrenProperty.key).isEqualTo("children")
        assertThat(A2uiBasicCatalogV1.Row.JustifyProperty.key).isEqualTo("justify")
        assertThat(A2uiBasicCatalogV1.Row.AlignProperty.key).isEqualTo("align")
    }

    @Test
    fun justify_fromValue_validStrings_returnsCorrespondingJustify() {
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("center"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.Center)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("end"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.End)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("spaceAround"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.SpaceAround)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("spaceBetween"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.SpaceBetween)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("spaceEvenly"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.SpaceEvenly)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("start"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.Start)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("stretch"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.Stretch)
    }

    @Test
    fun justify_fromValue_invalidOrEmptyString_fallsBackToStart() {
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue("invalid_justify"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.Start)
        assertThat(A2uiBasicCatalogV1.Row.Justify.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Row.Justify.Start)
    }

    @Test
    fun align_fromValue_validStrings_returnsCorrespondingAlign() {
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue("center"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.Center)
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue("end"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.End)
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue("start"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.Start)
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue("stretch"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.Stretch)
    }

    @Test
    fun align_fromValue_invalidOrEmptyString_fallsBackToStretch() {
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue("invalid_align"))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.Stretch)
        assertThat(A2uiBasicCatalogV1.Row.Align.fromValue(""))
            .isEqualTo(A2uiBasicCatalogV1.Row.Align.Stretch)
    }
}
