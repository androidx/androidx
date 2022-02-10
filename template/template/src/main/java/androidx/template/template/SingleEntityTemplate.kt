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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A basic [GlanceTemplate] implementation based around a single entity, using [Data].
 */
abstract class SingleEntityTemplate : GlanceTemplate<SingleEntityTemplate.Data>() {

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                HeaderBlock(it)
                TextBlock(it.title, it.subtitle, null)
                Button(it)
            }
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                HeaderBlock(it)
                TextBlock(it.title, it.subtitle, it.bodyText)
                Image(provider = it.mainImage.image, contentDescription = it.mainImage.description)
                Button(it)
            }
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    HeaderBlock(it)
                }
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        TextBlock(it.title, it.subtitle, it.bodyText)
                        Button(it)
                    }
                    Image(
                        provider = it.mainImage.image,
                        contentDescription = it.mainImage.description,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun HeaderBlock(data: Data) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.Horizontal.Start,
                modifier = GlanceModifier.defaultWeight()
            ) {
                // TODO: Text color customization
                val color = ColorProvider(R.color.text_default)
                Text(
                    data.header,
                    style = TextStyle(fontSize = 20.sp, color = color),
                    maxLines = 2
                )
            }
            data.headerIcon?.let {
                Column(
                    horizontalAlignment = Alignment.Horizontal.End,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Image(
                        provider = it.image,
                        contentDescription = it.description,
                        modifier = GlanceModifier.height(25.dp).width(25.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun TextBlock(title: String, subtitle: String?, body: String?) {
        // TODO: Text color customization
        val color = ColorProvider(R.color.text_default)
        Column {
            Text(title, style = TextStyle(fontSize = 36.sp, color = color), maxLines = 2)
            subtitle?.let {
                Text(it, style = TextStyle(fontSize = 20.sp, color = color), maxLines = 2)
            }
            body?.let { Text(it, style = TextStyle(fontSize = 18.sp, color = color), maxLines = 5) }
        }
    }

    @Composable
    private fun Button(data: Data) {
        // If a button image is provided, use an image button
        when (data.button) {
            is TemplateImageButton -> {
                // TODO: specify sizing for image button
                val image = data.button.image
                Image(
                    provider = image.image,
                    contentDescription = image.description,
                    modifier = GlanceModifier.clickable(data.button.action)
                )
            }
            is TemplateTextButton -> {
                Button(text = data.button.text, onClick = data.button.action)
            }
        }
    }

    /**
     * The semantic data required to build template layouts
     */
    // TODO: add optional ordering for text vs image sections, determine granularity level for
    //  setting text colors
    open class Data(
        val header: String,
        val title: String,
        val bodyText: String,
        val button: TemplateButton,
        val mainImage: TemplateImageWithDescription,
        val backgroundColor: ColorProvider,
        val headerIcon: TemplateImageWithDescription? = null,
        val subtitle: String? = null,
    )
}
