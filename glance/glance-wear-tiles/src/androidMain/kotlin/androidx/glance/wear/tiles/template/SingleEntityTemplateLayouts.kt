/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.template.SingleEntityTemplateData
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TextType
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.R

/**
 * Composable wearable layout for a single entity template. The template describes a wearable
 * layout based around a single entity.
 *
 * @param data the data that defines the layout
 */
@Composable
public fun SingleEntityTemplate(data: SingleEntityTemplateData) {
    // TODO: Add parameters here for other layout info, such as color preferences
    WearLayout(data)
}

@Composable
private fun WearLayout(data: SingleEntityTemplateData) {
    Box(
        modifier = GlanceModifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            ImageProvider(R.drawable.glance_single_entity_bg),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize())
        Column(
            modifier = GlanceModifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            data.headerIcon?.let { TemplateHeader(it) }
            Spacer(modifier = GlanceModifier.height(4.dp))
            TextSection(textList(data.text1, data.text2, data.text3))
            data.image?.let {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Image(
                    it.image,
                    contentDescription = it.description,
                    modifier = GlanceModifier.height(48.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun TemplateHeader(headerIcon: TemplateImageWithDescription?) {
    if (headerIcon == null) return

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        headerIcon.let {
            Image(
                provider = it.image,
                contentDescription = it.description,
                modifier = GlanceModifier.height(24.dp).width(24.dp)
            )
        }
    }
}

@Composable
private fun TextSection(textList: List<TemplateText>) {
    if (textList.isEmpty()) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        textList.forEach { item ->
            Text(
                item.text,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = if (item.type == TextType.Title) 24.sp else 16.sp,
                    textAlign = TextAlign.Center)
            )
        }
    }
}

private fun textList(
    title: TemplateText? = null,
    subtitle: TemplateText? = null,
    body: TemplateText? = null
): List<TemplateText> {
    val result = mutableListOf<TemplateText>()
    title?.let { result.add(TemplateText(it.text, TextType.Title)) }
    subtitle?.let { result.add(TemplateText(it.text, TextType.Label)) }
    body?.let { result.add(TemplateText(it.text, TextType.Body)) }

    return result
}
