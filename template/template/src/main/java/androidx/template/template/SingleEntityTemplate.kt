/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.template.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width

/**
 * A basic [GlanceTemplate] implementation based around a single entity, using [Data].
 */
abstract class SingleEntityTemplate : GlanceTemplate<SingleEntityTemplate.Data>() {
    // TODO: Design API for applying style (background color, text color etc.)
    // TODO: Move widget layouts that use corner radius out of top level template package

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            var modifier = modifier()
            it.image?.let { image ->
                modifier = modifier.background(image.image, ContentScale.Crop)
            }
            Column(modifier = modifier) {
                if (it.displayHeader) {
                    TemplateHeader(it.headerIcon, it.header)
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                TextSection(textList(it.text1, it.text2))
            }
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            Column(modifier = modifier()) {
                if (it.displayHeader) {
                    TemplateHeader(it.headerIcon, it.header)
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.image?.let { image ->
                    Image(
                        provider = image.image,
                        contentDescription = image.description,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .cornerRadius(image.cornerRadius),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    TextSection(textList(it.text1, it.text2))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    it.button?.let { button -> TemplateButton(button) }
                }
            }
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            Row(modifier = modifier()) {
                Column(
                    modifier =
                    GlanceModifier.fillMaxHeight().background(Color.Transparent).defaultWeight()
                ) {
                    if (it.displayHeader) {
                        TemplateHeader(it.headerIcon, it.header)
                        Spacer(modifier = GlanceModifier.height(16.dp))
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // TODO: Extract small height as template constant
                    TextSection(textList(
                        it.text1,
                        it.text2,
                        it.text3.takeIf { LocalSize.current.height > 240.dp }
                    ))
                    it.button?.let { button ->
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        TemplateButton(button)
                    }
                }
                it.image?.let { image ->
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    Image(
                        provider = image.image,
                        contentDescription = image.description,
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .defaultWeight()
                            .cornerRadius(image.cornerRadius),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    private fun modifier() =
        GlanceModifier.fillMaxSize().padding(16.dp).background(R.color.background_default)

    private fun textList(
        text1: TemplateText? = null,
        text2: TemplateText? = null,
        text3: TemplateText? = null
    ): List<TemplateText> {
        val result = mutableListOf<TemplateText>()
        text1?.let { result.add(it) }
        text2?.let { result.add(it) }
        text3?.let { result.add(it) }

        return result
    }

    /**
     * The semantic data required to build [SingleEntityTemplate] layouts. The template allows for
     * a header, text section with up to three text items, main image, and single action button.
     *
     * @param displayHeader True if the glanceable header should be displayed
     * @param headerIcon Header logo icon, image corner radius is ignored in default layouts
     * @param header Main header text
     * @param text1 Text section first text item
     * @param text2 Text section second text item
     * @param text3 Text section third text item
     * @param button Action button
     * @param image Main image content
     */
    class Data(
        val displayHeader: Boolean = true,
        val headerIcon: TemplateImageWithDescription? = null,
        val header: TemplateText? = null,
        val text1: TemplateText? = null,
        val text2: TemplateText? = null,
        val text3: TemplateText? = null,
        val button: TemplateButton? = null,
        val image: TemplateImageWithDescription? = null
    ) {

        override fun hashCode(): Int {
            var result = displayHeader.hashCode()
            result = 31 * result + (headerIcon?.hashCode() ?: 0)
            result = 31 * result + (header?.hashCode() ?: 0)
            result = 31 * result + (text1?.hashCode() ?: 0)
            result = 31 * result + (text2?.hashCode() ?: 0)
            result = 31 * result + (text3?.hashCode() ?: 0)
            result = 31 * result + (button?.hashCode() ?: 0)
            result = 31 * result + (image?.hashCode() ?: 0)
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (displayHeader != other.displayHeader) return false
            if (headerIcon != other.headerIcon) return false
            if (header != other.header) return false
            if (text1 != other.text1) return false
            if (text2 != other.text2) return false
            if (text3 != other.text3) return false
            if (button != other.button) return false
            if (image != other.image) return false

            return true
        }
    }
}
