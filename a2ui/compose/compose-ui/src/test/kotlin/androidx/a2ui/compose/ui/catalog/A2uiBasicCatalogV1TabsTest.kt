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
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1TabsTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val tabsComponent =
            object : A2uiBasicCatalogV1.Tabs {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
                    modifier: Modifier,
                ) {}
            }

        assertThat(tabsComponent.name).isEqualTo("Tabs")
        assertThat(tabsComponent.description)
            .isEqualTo("A set of tabs, each with a title and a corresponding child component.")
        assertThat(tabsComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.Tabs.TabsProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.Tabs.TitleProperty.key).isEqualTo("title")
        assertThat(A2uiBasicCatalogV1.Tabs.TitleProperty.isRequired).isTrue()
        val titleSchema =
            assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.Tabs.TitleProperty.schema)
        assertThat(titleSchema.description).isEqualTo("The tab title.")

        assertThat(A2uiBasicCatalogV1.Tabs.ChildProperty.key).isEqualTo("child")
        assertThat(A2uiBasicCatalogV1.Tabs.ChildProperty.isRequired).isTrue()
        val childSchema =
            assertIs<A2uiComponentIdSchema>(A2uiBasicCatalogV1.Tabs.ChildProperty.schema)
        assertThat(childSchema.description).isEqualTo("The ID of the child component.")

        assertThat(A2uiBasicCatalogV1.Tabs.TabsProperty.key).isEqualTo("tabs")
        assertThat(A2uiBasicCatalogV1.Tabs.TabsProperty.isRequired).isTrue()
        val tabsSchema = assertIs<A2uiArraySchema>(A2uiBasicCatalogV1.Tabs.TabsProperty.schema)
        assertThat(tabsSchema.description)
            .isEqualTo(
                "An array of objects, where each object defines a tab with a title and a child " +
                    "component."
            )
        assertThat(tabsSchema.minItems).isEqualTo(1)

        val itemsSchema = assertIs<A2uiObjectSchema>(tabsSchema.items)
        assertThat(itemsSchema.isAdditionalPropertiesAllowed).isFalse()
        assertThat(itemsSchema.required).containsExactly("title", "child")
        assertThat(itemsSchema.properties.keys).containsExactly("title", "child")
        assertThat(itemsSchema.properties["title"])
            .isEqualTo(A2uiBasicCatalogV1.Tabs.TitleProperty.schema)
        assertThat(itemsSchema.properties["child"])
            .isEqualTo(A2uiBasicCatalogV1.Tabs.ChildProperty.schema)
    }

    @Test
    fun tabDataClass_properties_matchConstructorArguments() {
        val tab = A2uiBasicCatalogV1.Tabs.Tab(title = "Overview", childId = "tab_overview_child")
        assertThat(tab.title).isEqualTo("Overview")
        assertThat(tab.childId).isEqualTo("tab_overview_child")
    }

    @Test
    fun tabDataClass_equalsAndHashCode_contracts() {
        val tab1 = A2uiBasicCatalogV1.Tabs.Tab("Title 1", "child_1")
        val tab2 = A2uiBasicCatalogV1.Tabs.Tab("Title 1", "child_1")
        val tab3 = A2uiBasicCatalogV1.Tabs.Tab("Title 2", "child_1")
        val tab4 = A2uiBasicCatalogV1.Tabs.Tab("Title 1", "child_2")

        assertThat(tab1).isEqualTo(tab2)
        assertThat(tab1.hashCode()).isEqualTo(tab2.hashCode())
        assertThat(tab1).isNotEqualTo(tab3)
        assertThat(tab1).isNotEqualTo(tab4)
    }

    @Test
    fun tabDataClass_equals_handlesNullAndOtherTypes() {
        val tab = A2uiBasicCatalogV1.Tabs.Tab("Title 1", "child_1")
        assertThat(tab.equals(tab)).isTrue()
        assertThat(tab.equals(null)).isFalse()
        assertThat(tab.equals("Title 1")).isFalse()
        assertThat(tab.equals(Any())).isFalse()
    }

    @Test
    fun tabDataClass_emptyValues_handledCorrectly() {
        val emptyTab1 = A2uiBasicCatalogV1.Tabs.Tab("", "")
        val emptyTab2 = A2uiBasicCatalogV1.Tabs.Tab("", "")
        val populatedTab = A2uiBasicCatalogV1.Tabs.Tab("Title", "child_1")

        assertThat(emptyTab1.title).isEmpty()
        assertThat(emptyTab1.childId).isEmpty()
        assertThat(emptyTab1).isEqualTo(emptyTab2)
        assertThat(emptyTab1.hashCode()).isEqualTo(emptyTab2.hashCode())
        assertThat(emptyTab1).isNotEqualTo(populatedTab)
        assertThat(emptyTab1.toString()).isEqualTo("Tab(title='', childId='')")
    }

    @Test
    fun tabDataClass_toString_returnsExpectedFormat() {
        val tab = A2uiBasicCatalogV1.Tabs.Tab("Title 1", "child_1")
        assertThat(tab.toString()).isEqualTo("Tab(title='Title 1', childId='child_1')")
    }
}
