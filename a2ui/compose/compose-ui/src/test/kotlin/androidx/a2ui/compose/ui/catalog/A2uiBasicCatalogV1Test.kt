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
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1Test {

    @Test
    fun catalogId_matchesExpectedSpecificationUri() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.catalogId).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.catalogId)
            .isEqualTo("https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json")
    }

    @Test
    fun themeSchema_matchesExpectedStructure() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)
        assertThat(catalog.themeSchema).isInstanceOf(A2uiObjectSchema::class.java)
        val themeObjSchema = catalog.themeSchema as A2uiObjectSchema
        assertThat(themeObjSchema.properties.keys)
            .containsExactly("primaryColor", "iconUrl", "agentDisplayName")
        assertThat(themeObjSchema.isAdditionalPropertiesAllowed).isTrue()
    }

    @Test
    fun weightProperty_hasExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.WeightProperty.key).isEqualTo("weight")
        assertThat(A2uiBasicCatalogV1.WeightProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.WeightProperty.schema)
            .isInstanceOf(A2uiNumberSchema::class.java)
    }

    @Test
    fun properties_initializedWithConstructorArguments() {
        val text = TestTextComponent()
        val image = TestImageComponent()
        val icon = TestIconComponent()
        val video = TestVideoComponent()
        val audioPlayer = TestAudioPlayerComponent()
        val card = TestCardComponent()
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val list = TestListComponent()
        val tabs = TestTabsComponent()
        val divider = TestDividerComponent()
        val button = TestButtonComponent()
        val checkBox = TestCheckBoxComponent()
        val slider = TestSliderComponent()
        val dateTimeInput = TestDateTimeInputComponent()
        val catalog =
            createTestBasicCatalog(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                card = card,
                row = row,
                column = column,
                list = list,
                tabs = tabs,
                divider = divider,
                button = button,
                checkBox = checkBox,
                slider = slider,
                dateTimeInput = dateTimeInput,
                functions = listOf(A2uiFormatStringFunction.INSTANCE),
            )

        assertThat(catalog.text).isSameInstanceAs(text)
        assertThat(catalog.image).isSameInstanceAs(image)
        assertThat(catalog.icon).isSameInstanceAs(icon)
        assertThat(catalog.video).isSameInstanceAs(video)
        assertThat(catalog.audioPlayer).isSameInstanceAs(audioPlayer)
        assertThat(catalog.card).isSameInstanceAs(card)
        assertThat(catalog.row).isSameInstanceAs(row)
        assertThat(catalog.column).isSameInstanceAs(column)
        assertThat(catalog.list).isSameInstanceAs(list)
        assertThat(catalog.tabs).isSameInstanceAs(tabs)
        assertThat(catalog.divider).isSameInstanceAs(divider)
        assertThat(catalog.button).isSameInstanceAs(button)
        assertThat(catalog.checkBox).isSameInstanceAs(checkBox)
        assertThat(catalog.slider).isSameInstanceAs(slider)
        assertThat(catalog.dateTimeInput).isSameInstanceAs(dateTimeInput)
        assertThat(catalog.components)
            .containsExactly(
                text,
                image,
                icon,
                video,
                audioPlayer,
                card,
                row,
                column,
                list,
                tabs,
                divider,
                button,
                checkBox,
                slider,
                dateTimeInput,
            )
        assertThat(catalog.functions).containsExactly(A2uiFormatStringFunction.INSTANCE)
    }

    @Test
    fun equalsAndHashCode_equalCatalogs_match() {
        val text = TestTextComponent()
        val image = TestImageComponent()
        val icon = TestIconComponent()
        val video = TestVideoComponent()
        val audioPlayer = TestAudioPlayerComponent()
        val card = TestCardComponent()
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val list = TestListComponent()
        val tabs = TestTabsComponent()
        val divider = TestDividerComponent()
        val button = TestButtonComponent()
        val checkBox = TestCheckBoxComponent()
        val slider = TestSliderComponent()
        val dateTimeInput = TestDateTimeInputComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                card = card,
                row = row,
                column = column,
                list = list,
                tabs = tabs,
                divider = divider,
                button = button,
                checkBox = checkBox,
                slider = slider,
                dateTimeInput = dateTimeInput,
            )
        val catalog2 =
            createTestBasicCatalog(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                card = card,
                row = row,
                column = column,
                list = list,
                tabs = tabs,
                divider = divider,
                button = button,
                checkBox = checkBox,
                slider = slider,
                dateTimeInput = dateTimeInput,
            )

        assertThat(catalog1).isEqualTo(catalog2)
        assertThat(catalog1.hashCode()).isEqualTo(catalog2.hashCode())
    }

    @Test
    fun equalsAndHashCode_differentCatalogs_doNotMatch() {
        val text1 = TestTextComponent()
        val text2 = TestTextComponent()
        val sharedImage = TestImageComponent()
        val sharedIcon = TestIconComponent()
        val sharedVideo = TestVideoComponent()
        val sharedAudioPlayer = TestAudioPlayerComponent()
        val sharedCard = TestCardComponent()
        val sharedRow = TestRowComponent()
        val sharedColumn = TestColumnComponent()
        val sharedList = TestListComponent()
        val sharedTabs = TestTabsComponent()
        val sharedDivider = TestDividerComponent()
        val sharedButton = TestButtonComponent()
        val sharedCheckBox = TestCheckBoxComponent()
        val sharedSlider = TestSliderComponent()
        val sharedDateTimeInput = TestDateTimeInputComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text1,
                image = sharedImage,
                icon = sharedIcon,
                video = sharedVideo,
                audioPlayer = sharedAudioPlayer,
                card = sharedCard,
                row = sharedRow,
                column = sharedColumn,
                list = sharedList,
                tabs = sharedTabs,
                divider = sharedDivider,
                button = sharedButton,
                checkBox = sharedCheckBox,
                slider = sharedSlider,
                dateTimeInput = sharedDateTimeInput,
            )
        val catalog2 =
            createTestBasicCatalog(
                text = text2,
                image = sharedImage,
                icon = sharedIcon,
                video = sharedVideo,
                audioPlayer = sharedAudioPlayer,
                card = sharedCard,
                row = sharedRow,
                column = sharedColumn,
                list = sharedList,
                tabs = sharedTabs,
                divider = sharedDivider,
                button = sharedButton,
                checkBox = sharedCheckBox,
                slider = sharedSlider,
                dateTimeInput = sharedDateTimeInput,
            )

        assertThat(catalog1).isNotEqualTo(catalog2)
        assertThat(catalog1.hashCode()).isNotEqualTo(catalog2.hashCode())
    }

    @Test
    fun toString_containsExpectedProperties() {
        val catalog = createTestBasicCatalog()

        assertThat(catalog.toString()).contains("catalogId=${A2uiBasicCatalogV1.CatalogId}")
        assertThat(catalog.toString()).contains("themeSchema=${A2uiBasicCatalogV1.ThemeSchema}")
        assertThat(catalog.toString())
            .containsMatch(
                "components=.*Text.*Image.*Icon.*Video.*AudioPlayer.*Card.*Row.*Column.*List." +
                    "*Tabs.*Divider.*Button.*CheckBox.*Slider.*DateTimeInput"
            )
        assertThat(catalog.toString()).contains("functions=[]")
    }

    private fun createTestBasicCatalog(
        text: A2uiBasicCatalogV1.Text = TestTextComponent(),
        image: A2uiBasicCatalogV1.Image = TestImageComponent(),
        icon: A2uiBasicCatalogV1.Icon = TestIconComponent(),
        video: A2uiBasicCatalogV1.Video = TestVideoComponent(),
        audioPlayer: A2uiBasicCatalogV1.AudioPlayer = TestAudioPlayerComponent(),
        card: A2uiBasicCatalogV1.Card = TestCardComponent(),
        row: A2uiBasicCatalogV1.Row = TestRowComponent(),
        column: A2uiBasicCatalogV1.Column = TestColumnComponent(),
        list: A2uiBasicCatalogV1.List = TestListComponent(),
        tabs: A2uiBasicCatalogV1.Tabs = TestTabsComponent(),
        divider: A2uiBasicCatalogV1.Divider = TestDividerComponent(),
        button: A2uiBasicCatalogV1.Button = TestButtonComponent(),
        checkBox: A2uiBasicCatalogV1.CheckBox = TestCheckBoxComponent(),
        slider: A2uiBasicCatalogV1.Slider = TestSliderComponent(),
        dateTimeInput: A2uiBasicCatalogV1.DateTimeInput = TestDateTimeInputComponent(),
        functions: List<A2uiFunction> = emptyList(),
    ) =
        A2uiBasicCatalogV1(
            text = text,
            image = image,
            icon = icon,
            video = video,
            audioPlayer = audioPlayer,
            card = card,
            row = row,
            column = column,
            list = list,
            tabs = tabs,
            divider = divider,
            button = button,
            checkBox = checkBox,
            slider = slider,
            dateTimeInput = dateTimeInput,
            functions = functions,
        )

    private class TestTextComponent : A2uiBasicCatalogV1.Text {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            text: String,
            variant: A2uiBasicCatalogV1.Text.Variant,
            modifier: Modifier,
        ) {}
    }

    private class TestImageComponent : A2uiBasicCatalogV1.Image {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            url: String,
            description: String?,
            fit: A2uiBasicCatalogV1.Image.Fit,
            variant: A2uiBasicCatalogV1.Image.Variant,
            modifier: Modifier,
        ) {}
    }

    private class TestIconComponent : A2uiBasicCatalogV1.Icon {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            source: A2uiBasicCatalogV1.Icon.Source,
            accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
            modifier: Modifier,
        ) {}
    }

    private class TestVideoComponent : A2uiBasicCatalogV1.Video {
        @Composable override fun A2uiComponentScope.TypedContent(url: String, modifier: Modifier) {}
    }

    private class TestAudioPlayerComponent : A2uiBasicCatalogV1.AudioPlayer {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            url: String,
            description: String?,
            modifier: Modifier,
        ) {}
    }

    private class TestCardComponent : A2uiBasicCatalogV1.Card {
        @Composable
        override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
    }

    private class TestRowComponent : A2uiBasicCatalogV1.Row {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: A2uiBasicCatalogV1.Row.Justify,
            align: A2uiBasicCatalogV1.Row.Align,
            modifier: Modifier,
        ) {}
    }

    private class TestColumnComponent : A2uiBasicCatalogV1.Column {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: A2uiBasicCatalogV1.Column.Justify,
            align: A2uiBasicCatalogV1.Column.Align,
            modifier: Modifier,
        ) {}
    }

    private class TestListComponent : A2uiBasicCatalogV1.List {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            direction: A2uiBasicCatalogV1.List.Direction,
            align: A2uiBasicCatalogV1.List.Align,
            modifier: Modifier,
        ) {}
    }

    private class TestTabsComponent : A2uiBasicCatalogV1.Tabs {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
            modifier: Modifier,
        ) {}
    }

    private class TestDividerComponent : A2uiBasicCatalogV1.Divider {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            axis: A2uiBasicCatalogV1.Divider.Axis,
            modifier: Modifier,
        ) {}
    }

    private class TestButtonComponent : A2uiBasicCatalogV1.Button {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            childId: String,
            variant: A2uiBasicCatalogV1.Button.Variant,
            action: Map<String, Any?>,
            modifier: Modifier,
        ) {}
    }

    private class TestCheckBoxComponent : A2uiBasicCatalogV1.CheckBox {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            label: String,
            value: Boolean,
            onValueChange: (Boolean) -> Unit,
            enabled: Boolean,
            modifier: Modifier,
        ) {}
    }

    private class TestSliderComponent : A2uiBasicCatalogV1.Slider {
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

    private class TestDateTimeInputComponent : A2uiBasicCatalogV1.DateTimeInput {
        @Composable
        override fun A2uiComponentScope.TypedContent(
            value: Long?,
            onValueChange: ((Long?) -> Unit)?,
            enableDate: Boolean,
            enableTime: Boolean,
            min: Long?,
            max: Long?,
            label: String?,
            modifier: Modifier,
        ) {}
    }
}
