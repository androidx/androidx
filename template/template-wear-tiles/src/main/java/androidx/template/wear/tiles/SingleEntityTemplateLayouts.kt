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

package androidx.template.wear.tiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.template.template.SingleEntityTemplateData
import androidx.template.template.TemplateImageWithDescription
import androidx.template.template.TemplateText

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
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .padding(16.dp)
            .background(androidx.template.template.R.color.background_default)
    ) {
        data.headerIcon?.let { TemplateHeader(it, data.header) }
        Spacer(modifier = GlanceModifier.height(16.dp))
        TextSection(textList(data.text1, data.text2))
    }
}

// TODO: Copied from app widget layouts, wear may have different layout requirements
@Composable
private fun TemplateHeader(
    headerIcon: TemplateImageWithDescription?,
    header: TemplateText?
) {
    if (headerIcon == null && header == null) return

    Row(
        modifier = GlanceModifier.background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        headerIcon?.let {
            Image(
                provider = it.image,
                contentDescription = it.description,
                modifier = GlanceModifier.height(24.dp).width(24.dp)
            )
        }
        header?.let {
            if (headerIcon != null) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }

            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = header.text,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TextSection(textList: List<TemplateText>) {
    if (textList.isEmpty()) return

    Column(modifier = GlanceModifier.background(Color.Transparent)) {
        textList.forEachIndexed { index, item ->
            Text(
                item.text,
                modifier = GlanceModifier.background(Color.Transparent)
            )
            if (index < textList.size - 1) {
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

private fun textList(
    title: TemplateText? = null,
    subtitle: TemplateText? = null,
    body: TemplateText? = null
): List<TemplateText> {
    val result = mutableListOf<TemplateText>()
    title?.let { result.add(TemplateText(it.text, TemplateText.Type.Title)) }
    subtitle?.let { result.add(TemplateText(it.text, TemplateText.Type.Label)) }
    body?.let { result.add(TemplateText(it.text, TemplateText.Type.Body)) }

    return result
}
