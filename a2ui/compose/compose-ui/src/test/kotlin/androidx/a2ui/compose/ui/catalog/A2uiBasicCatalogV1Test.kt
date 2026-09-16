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
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiCheckRuleSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
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
            .isEqualTo("https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json")
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
    fun accessibilityProperty_hasExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.AccessibilityProperty.key).isEqualTo("accessibility")
        assertThat(A2uiBasicCatalogV1.AccessibilityProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.AccessibilityProperty.schema)
            .isEqualTo(A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE)
    }

    @Test
    fun components_allIncludeAccessibilityProperty() {
        val catalog = createTestBasicCatalog()
        val componentsWithAccessibility =
            catalog.components.filter {
                it.properties.contains(A2uiBasicCatalogV1.AccessibilityProperty)
            }

        assertThat(componentsWithAccessibility).containsExactlyElementsIn(catalog.components)
    }

    @Test
    fun componentCompanionAccessibilityProperties_referenceSharedInstance() {
        assertThat(A2uiBasicCatalogV1.Text.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Image.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Icon.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Video.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.AudioPlayer.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Card.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Row.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Column.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.List.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Tabs.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Divider.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Button.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.TextField.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.CheckBox.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.Slider.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
        assertThat(A2uiBasicCatalogV1.DateTimeInput.AccessibilityProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.AccessibilityProperty)
    }

    @Test
    fun weightProperty_hasExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.WeightProperty.key).isEqualTo("weight")
        assertThat(A2uiBasicCatalogV1.WeightProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.WeightProperty.schema)
            .isInstanceOf(A2uiNumberSchema::class.java)
        assertThat(A2uiBasicCatalogV1.WeightProperty.schema.description)
            .isEqualTo(
                "The relative weight of this component within a Row or Column. " +
                    "This is similar to the CSS 'flex-grow' property. " +
                    "Note: this may ONLY be set when the component is a direct descendant of a " +
                    "Row or Column."
            )
    }

    @Test
    fun checksProperty_hasExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.ChecksProperty.key).isEqualTo("checks")
        assertThat(A2uiBasicCatalogV1.ChecksProperty.isRequired).isFalse()
        val checksSchema = assertIs<A2uiArraySchema>(A2uiBasicCatalogV1.ChecksProperty.schema)
        assertThat(checksSchema.items).isEqualTo(A2uiCheckRuleSchema.DEFAULT_INSTANCE)
    }

    @Test
    fun components_relevantIncludeChecksProperty() {
        val catalog = createTestBasicCatalog()
        val componentsWithChecks =
            catalog.components.filter { it.properties.contains(A2uiBasicCatalogV1.ChecksProperty) }

        assertThat(componentsWithChecks)
            .containsExactly(
                catalog.button,
                catalog.textField,
                catalog.slider,
                catalog.dateTimeInput,
                catalog.checkBox,
                catalog.choicePicker,
            )
    }

    @Test
    fun componentCompanionChecksProperties_referenceSharedInstance() {
        assertThat(A2uiBasicCatalogV1.Button.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
        assertThat(A2uiBasicCatalogV1.TextField.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
        assertThat(A2uiBasicCatalogV1.Slider.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
        assertThat(A2uiBasicCatalogV1.DateTimeInput.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
        assertThat(A2uiBasicCatalogV1.CheckBox.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
        assertThat(A2uiBasicCatalogV1.ChoicePicker.ChecksProperty)
            .isSameInstanceAs(A2uiBasicCatalogV1.ChecksProperty)
    }

    @Test
    fun properties_initializedWithConstructorArguments() {
        val text = TestTextComponent()
        val image = TestImageComponent()
        val icon = TestIconComponent()
        val video = TestVideoComponent()
        val audioPlayer = TestAudioPlayerComponent()
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val list = TestListComponent()
        val card = TestCardComponent()
        val tabs = TestTabsComponent()
        val modal = TestModalComponent()
        val divider = TestDividerComponent()
        val button = TestButtonComponent()
        val textField = TestTextFieldComponent()
        val checkBox = TestCheckBoxComponent()
        val choicePicker = TestChoicePickerComponent()
        val slider = TestSliderComponent()
        val dateTimeInput = TestDateTimeInputComponent()
        val catalog =
            createTestBasicCatalog(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                row = row,
                column = column,
                list = list,
                card = card,
                tabs = tabs,
                modal = modal,
                divider = divider,
                button = button,
                textField = textField,
                checkBox = checkBox,
                choicePicker = choicePicker,
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
        assertThat(catalog.modal).isSameInstanceAs(modal)
        assertThat(catalog.divider).isSameInstanceAs(divider)
        assertThat(catalog.button).isSameInstanceAs(button)
        assertThat(catalog.textField).isSameInstanceAs(textField)
        assertThat(catalog.checkBox).isSameInstanceAs(checkBox)
        assertThat(catalog.choicePicker).isSameInstanceAs(choicePicker)
        assertThat(catalog.slider).isSameInstanceAs(slider)
        assertThat(catalog.dateTimeInput).isSameInstanceAs(dateTimeInput)
        assertThat(catalog.components)
            .containsExactly(
                text,
                image,
                icon,
                video,
                audioPlayer,
                row,
                column,
                list,
                card,
                tabs,
                modal,
                divider,
                button,
                textField,
                checkBox,
                choicePicker,
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
        val row = TestRowComponent()
        val column = TestColumnComponent()
        val list = TestListComponent()
        val card = TestCardComponent()
        val tabs = TestTabsComponent()
        val modal = TestModalComponent()
        val divider = TestDividerComponent()
        val button = TestButtonComponent()
        val textField = TestTextFieldComponent()
        val checkBox = TestCheckBoxComponent()
        val choicePicker = TestChoicePickerComponent()
        val slider = TestSliderComponent()
        val dateTimeInput = TestDateTimeInputComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                row = row,
                column = column,
                list = list,
                card = card,
                tabs = tabs,
                modal = modal,
                divider = divider,
                button = button,
                textField = textField,
                checkBox = checkBox,
                choicePicker = choicePicker,
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
                row = row,
                column = column,
                list = list,
                card = card,
                tabs = tabs,
                modal = modal,
                divider = divider,
                button = button,
                textField = textField,
                checkBox = checkBox,
                choicePicker = choicePicker,
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
        val sharedModal = TestModalComponent()
        val sharedDivider = TestDividerComponent()
        val sharedButton = TestButtonComponent()
        val sharedTextField = TestTextFieldComponent()
        val sharedCheckBox = TestCheckBoxComponent()
        val sharedChoicePicker = TestChoicePickerComponent()
        val sharedSlider = TestSliderComponent()
        val sharedDateTimeInput = TestDateTimeInputComponent()
        val catalog1 =
            createTestBasicCatalog(
                text = text1,
                image = sharedImage,
                icon = sharedIcon,
                video = sharedVideo,
                audioPlayer = sharedAudioPlayer,
                row = sharedRow,
                column = sharedColumn,
                list = sharedList,
                card = sharedCard,
                tabs = sharedTabs,
                modal = sharedModal,
                divider = sharedDivider,
                button = sharedButton,
                textField = sharedTextField,
                checkBox = sharedCheckBox,
                choicePicker = sharedChoicePicker,
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
                row = sharedRow,
                column = sharedColumn,
                list = sharedList,
                card = sharedCard,
                tabs = sharedTabs,
                modal = sharedModal,
                divider = sharedDivider,
                button = sharedButton,
                textField = sharedTextField,
                checkBox = sharedCheckBox,
                choicePicker = sharedChoicePicker,
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
                "components=.*Text.*Image.*Icon.*Video.*AudioPlayer.*Row.*Column.*List.*Card." +
                    "*Tabs.*Modal.*Divider.*Button.*TextField.*CheckBox.*ChoicePicker.*Slider" +
                    ".*DateTimeInput"
            )
        assertThat(catalog.toString()).contains("functions=[]")
    }

    @Test
    fun toA2uiCatalog_createsCatalogSuccessfully() {
        val testFunction = A2uiFormatStringFunction.INSTANCE
        val basicCatalog = createTestBasicCatalog(functions = listOf(testFunction))

        val catalog = basicCatalog.toA2uiCatalog()

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)
        assertThat(catalog.components["Text"]).isSameInstanceAs(basicCatalog.text)
        assertThat(catalog.components["Image"]).isSameInstanceAs(basicCatalog.image)
        assertThat(catalog.components["Icon"]).isSameInstanceAs(basicCatalog.icon)
        assertThat(catalog.components["Video"]).isSameInstanceAs(basicCatalog.video)
        assertThat(catalog.components["AudioPlayer"]).isSameInstanceAs(basicCatalog.audioPlayer)
        assertThat(catalog.components["Card"]).isSameInstanceAs(basicCatalog.card)
        assertThat(catalog.components["Row"]).isSameInstanceAs(basicCatalog.row)
        assertThat(catalog.components["Column"]).isSameInstanceAs(basicCatalog.column)
        assertThat(catalog.components["List"]).isSameInstanceAs(basicCatalog.list)
        assertThat(catalog.components["Tabs"]).isSameInstanceAs(basicCatalog.tabs)
        assertThat(catalog.components["Modal"]).isSameInstanceAs(basicCatalog.modal)
        assertThat(catalog.components["Divider"]).isSameInstanceAs(basicCatalog.divider)
        assertThat(catalog.components["Button"]).isSameInstanceAs(basicCatalog.button)
        assertThat(catalog.components["TextField"]).isSameInstanceAs(basicCatalog.textField)
        assertThat(catalog.components["CheckBox"]).isSameInstanceAs(basicCatalog.checkBox)
        assertThat(catalog.components["ChoicePicker"]).isSameInstanceAs(basicCatalog.choicePicker)
        assertThat(catalog.components["Slider"]).isSameInstanceAs(basicCatalog.slider)
        assertThat(catalog.components["DateTimeInput"]).isSameInstanceAs(basicCatalog.dateTimeInput)
        assertThat(catalog.functions[testFunction.definition.name]).isSameInstanceAs(testFunction)
        assertThat(catalog.isInline).isFalse()
    }

    @Test
    fun toA2uiCatalog_withIsInlineTrue_createsInlineCatalog() {
        val basicCatalog = createTestBasicCatalog()

        val catalog = basicCatalog.toA2uiCatalog(isInline = true)

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.isInline).isTrue()
    }
}

internal fun createTestBasicCatalog(
    text: A2uiBasicCatalogV1.Text = TestTextComponent(),
    image: A2uiBasicCatalogV1.Image = TestImageComponent(),
    icon: A2uiBasicCatalogV1.Icon = TestIconComponent(),
    video: A2uiBasicCatalogV1.Video = TestVideoComponent(),
    audioPlayer: A2uiBasicCatalogV1.AudioPlayer = TestAudioPlayerComponent(),
    row: A2uiBasicCatalogV1.Row = TestRowComponent(),
    column: A2uiBasicCatalogV1.Column = TestColumnComponent(),
    list: A2uiBasicCatalogV1.List = TestListComponent(),
    card: A2uiBasicCatalogV1.Card = TestCardComponent(),
    tabs: A2uiBasicCatalogV1.Tabs = TestTabsComponent(),
    modal: A2uiBasicCatalogV1.Modal = TestModalComponent(),
    divider: A2uiBasicCatalogV1.Divider = TestDividerComponent(),
    button: A2uiBasicCatalogV1.Button = TestButtonComponent(),
    textField: A2uiBasicCatalogV1.TextField = TestTextFieldComponent(),
    checkBox: A2uiBasicCatalogV1.CheckBox = TestCheckBoxComponent(),
    choicePicker: A2uiBasicCatalogV1.ChoicePicker = TestChoicePickerComponent(),
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
        row = row,
        column = column,
        list = list,
        card = card,
        tabs = tabs,
        modal = modal,
        divider = divider,
        button = button,
        textField = textField,
        checkBox = checkBox,
        choicePicker = choicePicker,
        slider = slider,
        dateTimeInput = dateTimeInput,
        functions = functions,
    )

internal class TestTextComponent : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestImageComponent : A2uiBasicCatalogV1.Image {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        fit: A2uiBasicCatalogV1.Image.Fit,
        variant: A2uiBasicCatalogV1.Image.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestIconComponent : A2uiBasicCatalogV1.Icon {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        source: A2uiBasicCatalogV1.Icon.Source,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestVideoComponent : A2uiBasicCatalogV1.Video {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestAudioPlayerComponent : A2uiBasicCatalogV1.AudioPlayer {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestCardComponent : A2uiBasicCatalogV1.Card {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestRowComponent : A2uiBasicCatalogV1.Row {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Row.Justify,
        align: A2uiBasicCatalogV1.Row.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestColumnComponent : A2uiBasicCatalogV1.Column {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Column.Justify,
        align: A2uiBasicCatalogV1.Column.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestListComponent : A2uiBasicCatalogV1.List {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        direction: A2uiBasicCatalogV1.List.Direction,
        align: A2uiBasicCatalogV1.List.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestTabsComponent : A2uiBasicCatalogV1.Tabs {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestModalComponent : A2uiBasicCatalogV1.Modal {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        triggerId: String,
        contentId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestDividerComponent : A2uiBasicCatalogV1.Divider {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        axis: A2uiBasicCatalogV1.Divider.Axis,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {}
}

internal class TestButtonComponent : A2uiBasicCatalogV1.Button {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}

internal class TestTextFieldComponent : A2uiBasicCatalogV1.TextField {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        value: String?,
        variant: A2uiBasicCatalogV1.TextField.Variant,
        validationRegexp: String?,
        onValueChange: (String) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}

internal class TestCheckBoxComponent : A2uiBasicCatalogV1.CheckBox {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}

internal class TestChoicePickerComponent : A2uiBasicCatalogV1.ChoicePicker {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        options: List<A2uiBasicCatalogV1.ChoicePicker.Option>,
        value: List<String>,
        variant: A2uiBasicCatalogV1.ChoicePicker.Variant,
        displayStyle: A2uiBasicCatalogV1.ChoicePicker.DisplayStyle,
        filterable: Boolean,
        onValueChange: (List<String>) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}

internal class TestSliderComponent : A2uiBasicCatalogV1.Slider {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        min: Float,
        max: Float,
        value: Float,
        onValueChange: (Float) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}

internal class TestDateTimeInputComponent : A2uiBasicCatalogV1.DateTimeInput {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        value: Long?,
        onValueChange: ((Long?) -> Unit)?,
        enableDate: Boolean,
        enableTime: Boolean,
        min: Long?,
        max: Long?,
        label: String?,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {}
}
