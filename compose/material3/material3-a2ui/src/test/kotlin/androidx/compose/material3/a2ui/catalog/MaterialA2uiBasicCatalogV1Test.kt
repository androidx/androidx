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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MaterialA2uiBasicCatalogV1Test {

    private val fakeImageRenderer = A2uiImageRenderer { _, _, _, _, _ -> }
    private val fakeVideoRenderer = A2uiVideoRenderer { _, _, _ -> }
    private val fakeAudioPlayerRenderer = A2uiAudioPlayerRenderer { _, _, _, _ -> }
    private val fakeUrlOpener = A2uiUrlOpener { _ -> }
    private val fakeMessageFormatter = A2uiMessageFormatter { _, _, _ -> "" }
    private val fakeLocaleProvider = A2uiLocaleProvider { Locale.US }

    @Test
    fun factory_withDefaults_createsCatalogWithDefaultComponentsAndBasicFunctions() {
        val image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer)
        val video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer)
        val audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer)
        val catalog =
            materialA2uiBasicCatalogV1(
                image = image,
                video = video,
                audioPlayer = audioPlayer,
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
            )

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)

        // Verifies the default components are populated
        assertThat(catalog.components["Text"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.text)
        assertThat(catalog.components["Image"]).isSameInstanceAs(image)
        assertThat(catalog.components["Icon"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.icon)
        assertThat(catalog.components["Video"]).isSameInstanceAs(video)
        assertThat(catalog.components["AudioPlayer"]).isSameInstanceAs(audioPlayer)
        assertThat(catalog.components["Card"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.card)
        assertThat(catalog.components["Row"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.row)
        assertThat(catalog.components["Column"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.column)
        assertThat(catalog.components["List"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.list)
        assertThat(catalog.components["Tabs"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.tabs)
        assertThat(catalog.components["Divider"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.divider)
        assertThat(catalog.components["Button"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.button)
        assertThat(catalog.components["CheckBox"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.checkBox)
        assertThat(catalog.components["Slider"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.slider)
        assertThat(catalog.components["DateTimeInput"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.dateTimeInput)

        // Verifies standard basic functions are populated
        assertThat(catalog.functions["formatString"]).isNotNull()
        assertThat(catalog.functions["openUrl"]).isNotNull()
    }

    @Test
    fun factory_withCustomTextComponent_overridesDefaultMaterialText() {
        val customText =
            object : A2uiBasicCatalogV1.Text {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    text: String,
                    variant: A2uiBasicCatalogV1.Text.Variant,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                text = customText,
            )

        assertThat(catalog.components["Text"]).isSameInstanceAs(customText)
        assertThat(catalog.components["Text"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.text)
    }

    @Test
    fun factory_withCustomIconComponent_overridesDefaultMaterialIcon() {
        val customIcon =
            object : A2uiBasicCatalogV1.Icon {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    source: A2uiBasicCatalogV1.Icon.Source,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                icon = customIcon,
            )

        assertThat(catalog.components["Icon"]).isSameInstanceAs(customIcon)
        assertThat(catalog.components["Icon"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.icon)
    }

    @Test
    fun factory_withCustomImageComponent_overridesDefaultMaterialImage() {
        val customImage =
            object : A2uiBasicCatalogV1.Image {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    url: String,
                    description: String?,
                    fit: A2uiBasicCatalogV1.Image.Fit,
                    variant: A2uiBasicCatalogV1.Image.Variant,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = customImage,
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
            )

        assertThat(catalog.components["Image"]).isSameInstanceAs(customImage)
        assertThat(catalog.components["Image"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer))
    }

    @Test
    fun factory_withCustomVideoComponent_overridesDefaultMaterialVideo() {
        val customVideo =
            object : A2uiBasicCatalogV1.Video {
                @Composable
                override fun A2uiComponentScope.TypedContent(url: String, modifier: Modifier) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = customVideo,
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
            )

        assertThat(catalog.components["Video"]).isSameInstanceAs(customVideo)
        assertThat(catalog.components["Video"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer))
    }

    @Test
    fun factory_withCustomAudioPlayerComponent_overridesDefaultMaterialAudioPlayer() {
        val customAudioPlayer =
            object : A2uiBasicCatalogV1.AudioPlayer {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    url: String,
                    description: String?,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer = customAudioPlayer,
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
            )

        assertThat(catalog.components["AudioPlayer"]).isSameInstanceAs(customAudioPlayer)
        assertThat(catalog.components["AudioPlayer"])
            .isNotSameInstanceAs(
                MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer)
            )
    }

    @Test
    fun factory_withCustomCardComponent_overridesDefaultMaterialCard() {
        val customCard =
            object : A2uiBasicCatalogV1.Card {
                @Composable
                override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                card = customCard,
            )

        assertThat(catalog.components["Card"]).isSameInstanceAs(customCard)
        assertThat(catalog.components["Card"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.card)
    }

    @Test
    fun factory_withCustomRowComponent_overridesDefaultMaterialRow() {
        val customRow =
            object : A2uiBasicCatalogV1.Row {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Row.Justify,
                    align: A2uiBasicCatalogV1.Row.Align,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                row = customRow,
            )

        assertThat(catalog.components["Row"]).isSameInstanceAs(customRow)
        assertThat(catalog.components["Row"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.row)
    }

    @Test
    fun factory_withCustomColumnComponent_overridesDefaultMaterialColumn() {
        val customColumn =
            object : A2uiBasicCatalogV1.Column {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Column.Justify,
                    align: A2uiBasicCatalogV1.Column.Align,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                column = customColumn,
            )

        assertThat(catalog.components["Column"]).isSameInstanceAs(customColumn)
        assertThat(catalog.components["Column"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.column)
    }

    @Test
    fun factory_withCustomListComponent_overridesDefaultMaterialList() {
        val customList =
            object : A2uiBasicCatalogV1.List {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    direction: A2uiBasicCatalogV1.List.Direction,
                    align: A2uiBasicCatalogV1.List.Align,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                list = customList,
            )

        assertThat(catalog.components["List"]).isSameInstanceAs(customList)
        assertThat(catalog.components["List"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.list)
    }

    @Test
    fun factory_withCustomTabsComponent_overridesDefaultMaterialTabs() {
        val customTabs =
            object : A2uiBasicCatalogV1.Tabs {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                tabs = customTabs,
            )

        assertThat(catalog.components["Tabs"]).isSameInstanceAs(customTabs)
        assertThat(catalog.components["Tabs"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.tabs)
    }

    @Test
    fun factory_withCustomDividerComponent_overridesDefaultMaterialDivider() {
        val customDivider =
            object : A2uiBasicCatalogV1.Divider {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    axis: A2uiBasicCatalogV1.Divider.Axis,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                divider = customDivider,
            )

        assertThat(catalog.components["Divider"]).isSameInstanceAs(customDivider)
        assertThat(catalog.components["Divider"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.divider)
    }

    @Test
    fun factory_withCustomButtonComponent_overridesDefaultMaterialButton() {
        val customButton =
            object : A2uiBasicCatalogV1.Button {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    childId: String,
                    variant: A2uiBasicCatalogV1.Button.Variant,
                    action: Map<String, Any?>,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                button = customButton,
            )

        assertThat(catalog.components["Button"]).isSameInstanceAs(customButton)
        assertThat(catalog.components["Button"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.button)
    }

    @Test
    fun factory_withCustomCheckBoxComponent_overridesDefaultMaterialCheckBox() {
        val customCheckBox =
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

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                checkBox = customCheckBox,
            )

        assertThat(catalog.components["CheckBox"]).isSameInstanceAs(customCheckBox)
        assertThat(catalog.components["CheckBox"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.checkBox)
    }

    @Test
    fun factory_withCustomSliderComponent_overridesDefaultMaterialSlider() {
        val customSlider =
            object : A2uiBasicCatalogV1.Slider {
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

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                slider = customSlider,
            )

        assertThat(catalog.components["Slider"]).isSameInstanceAs(customSlider)
        assertThat(catalog.components["Slider"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.slider)
    }

    @Test
    fun factory_withCustomDateTimeInputComponent_overridesDefaultMaterialDateTimeInput() {
        val customDateTimeInput =
            object : A2uiBasicCatalogV1.DateTimeInput {
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

        val catalog =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer),
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                dateTimeInput = customDateTimeInput,
            )

        assertThat(catalog.components["DateTimeInput"]).isSameInstanceAs(customDateTimeInput)
        assertThat(catalog.components["DateTimeInput"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.dateTimeInput)
    }

    @Test
    fun materialA2uiBasicCatalogV1Defaults_providesExpectedObjects() {
        assertThat(MaterialA2uiBasicCatalogV1Defaults.text)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Text)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer))
            .isInstanceOf(MaterialA2uiBasicCatalogV1Image::class.java)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.video(fakeVideoRenderer))
            .isInstanceOf(MaterialA2uiBasicCatalogV1Video::class.java)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.audioPlayer(fakeAudioPlayerRenderer))
            .isInstanceOf(MaterialA2uiBasicCatalogV1AudioPlayer::class.java)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.icon)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Icon)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.card)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Card)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.row)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Row)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.column)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Column)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.list)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1List)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.tabs)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Tabs)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.divider)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Divider)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.button)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Button)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.checkBox)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1CheckBox)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.slider)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Slider)
        assertThat(MaterialA2uiBasicCatalogV1Defaults.dateTimeInput)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1DateTimeInput)
    }
}
