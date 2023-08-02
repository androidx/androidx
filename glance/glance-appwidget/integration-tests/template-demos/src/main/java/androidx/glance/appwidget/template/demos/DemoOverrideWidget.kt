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

package androidx.glance.appwidget.template.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.template.GlanceTemplateAppWidget
import androidx.glance.appwidget.template.SingleEntityTemplate
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.SingleEntityTemplateData
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateMode
import androidx.glance.template.TemplateText
import androidx.glance.template.TextType
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A widget implementation that uses [SingleEntityTemplate] with a custom layout override for
 * [TemplateMode.Horizontal].
 */
class DemoOverrideWidget : GlanceTemplateAppWidget() {

    @Composable
    override fun TemplateContent() {
        if (LocalTemplateMode.current == TemplateMode.Horizontal) {
            MyHorizontalContent()
        } else {
            SingleEntityTemplate(
                SingleEntityTemplateData(
                    header = TemplateText("Single Entity Demo", TextType.Title),
                    headerIcon = TemplateImageWithDescription(
                        ImageProvider(R.drawable.compose),
                        "icon"
                    ),
                    text1 = TemplateText("title", TextType.Title),
                    text2 = TemplateText("Subtitle", TextType.Label),
                    text3 = TemplateText(
                        "Body Lorem ipsum dolor sit amet, consectetur adipiscing",
                        TextType.Label
                    ),
                    image = TemplateImageWithDescription(ImageProvider(R.drawable.compose), "image")
                )
            )
        }
    }

    @Composable
    fun MyHorizontalContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.Red),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "User layout override for horizontal display",
                style = TextStyle(
                    fontSize = 36.sp,
                    color = ColorProvider(Color.White),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

class DemoOverrideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DemoOverrideWidget()
}
