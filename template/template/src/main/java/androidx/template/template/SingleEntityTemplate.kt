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
import androidx.glance.unit.ColorProvider

/**
 * A basic [GlanceTemplate] implementation based around a single entity, using [Data].
 */
abstract class SingleEntityTemplate : GlanceTemplate<SingleEntityTemplate.Data>() {

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { color -> modifier = modifier.background(color) }
            it.image?.let { image ->
                modifier = modifier.background(image.image, ContentScale.Crop)
            }
            Column(modifier = modifier) {
                TemplateHeader(it.headerIcon, it.header)
                Spacer(modifier = GlanceModifier.defaultWeight())
                TextSection(textList(it.title, it.subtitle))
            }
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { color -> modifier = modifier.background(color) }
            Column(modifier = modifier) {
                TemplateHeader(it.headerIcon, it.header)
                Spacer(modifier = GlanceModifier.height(16.dp))
                it.image?.let { image ->
                    Image(
                        provider = image.image,
                        contentDescription = image.description,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    TextSection(textList(it.title, it.subtitle))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    it.button?.let { button -> TemplateButton(button) }
                }
            }
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { color -> modifier = modifier.background(color) }
            Row(modifier = modifier) {
                Column(
                    modifier =
                    GlanceModifier.fillMaxHeight().background(Color.Transparent).defaultWeight()
                ) {
                    TemplateHeader(it.headerIcon, it.header)
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // TODO: Extract small height as template constant
                    val body =
                        if (LocalSize.current.height > 240.dp) {
                            it.body
                        } else {
                            null
                        }
                    TextSection(textList(it.title, it.subtitle, body))
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
                        modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    private fun textList(
        title: TemplateText? = null,
        subtitle: TemplateText? = null,
        body: TemplateText? = null
    ): List<TypedTemplateText> {
        val result = mutableListOf<TypedTemplateText>()
        title?.let {
            result.add(TypedTemplateText(it, TemplateTextType.Title))
        }
        subtitle?.let {
            result.add(TypedTemplateText(it, TemplateTextType.Label))
        }
        body?.let {
            result.add(TypedTemplateText(it, TemplateTextType.Body))
        }

        return result
    }

    /**
     * The semantic data required to build [SingleEntityTemplate] layouts.
     *
     * @param headerIcon Logo icon, displayed in the glanceable header
     * @param header Main header text, text priority is ignored in default layouts
     * @param title Text section main title, priority ordered
     * @param subtitle Text section subtitle, priority ordered
     * @param body Text section body text, priority ordered
     * @param button Action button
     * @param image Main image content
     * @param backgroundColor Glanceable background color
     */
    class Data(
        val headerIcon: TemplateImageWithDescription,
        val header: TemplateText? = null,
        val title: TemplateText? = null,
        val subtitle: TemplateText? = null,
        val body: TemplateText? = null,
        val button: TemplateButton? = null,
        val image: TemplateImageWithDescription? = null,
        val backgroundColor: ColorProvider? = null
    ) {

        override fun hashCode(): Int {
            var result = headerIcon.hashCode()
            result = 31 * result + (header?.hashCode() ?: 0)
            result = 31 * result + (title?.hashCode() ?: 0)
            result = 31 * result + (subtitle?.hashCode() ?: 0)
            result = 31 * result + (body?.hashCode() ?: 0)
            result = 31 * result + (button?.hashCode() ?: 0)
            result = 31 * result + (image?.hashCode() ?: 0)
            result = 31 * result + (backgroundColor?.hashCode() ?: 0)
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (headerIcon != other.headerIcon) return false
            if (header != other.header) return false
            if (title != other.title) return false
            if (subtitle != other.subtitle) return false
            if (body != other.body) return false
            if (button != other.button) return false
            if (image != other.image) return false
            if (backgroundColor != other.backgroundColor) return false

            return true
        }
    }
}
