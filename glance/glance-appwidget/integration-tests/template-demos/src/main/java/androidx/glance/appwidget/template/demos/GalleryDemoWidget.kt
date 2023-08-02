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
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.template.GalleryTemplate
import androidx.glance.appwidget.template.GlanceTemplateAppWidget
import androidx.glance.template.ActionBlock
import androidx.glance.template.AspectRatio
import androidx.glance.template.GalleryTemplateData
import androidx.glance.template.HeaderBlock
import androidx.glance.template.ImageBlock
import androidx.glance.template.ImageSize
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TemplateTextButton
import androidx.glance.template.TextBlock
import androidx.glance.template.TextType

/**
 * Gallery demo for the default Small sized images with 1:1 aspect ratio and left-to-right main
 * text/image block flow using data and gallery template from [BaseGalleryTemplateWidget].
 */
class SmallGalleryTemplateDemoWidget : BaseGalleryTemplateWidget() {
    @Composable
    override fun TemplateContent() = GalleryTemplateContent()
}

/**
 * Gallery demo for the Medium sized images with 16:9 aspect ratio and right-to-left main
 * text/image block flow using data and gallery template from [BaseGalleryTemplateWidget].
 */
class MediumGalleryTemplateDemoWidget : BaseGalleryTemplateWidget() {
    @Composable
    override fun TemplateContent() =
        GalleryTemplateContent(ImageSize.Medium, AspectRatio.Ratio16x9, false)
}

/**
 * Gallery demo for the Large sized images with 2:3 aspect ratio and left-to-right main
 * text/image block flow using data and gallery template from [BaseGalleryTemplateWidget].
 */
class LargeGalleryTemplateDemoWidget : BaseGalleryTemplateWidget() {
    @Composable
    override fun TemplateContent() = GalleryTemplateContent(ImageSize.Large, AspectRatio.Ratio2x3)
}

class SmallImageGalleryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallGalleryTemplateDemoWidget()
}

class MediumImageGalleryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumGalleryTemplateDemoWidget()
}

class LargeImageGalleryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeGalleryTemplateDemoWidget()
}

/**
 * Base Gallery Demo widget binding [GalleryTemplateData] to [GalleryTemplate] layout.
 * It is overridable by gallery image aspect ratio, image size, and main blocks ordering.
 */
abstract class BaseGalleryTemplateWidget : GlanceTemplateAppWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable
    internal fun GalleryTemplateContent(
        imageSize: ImageSize = ImageSize.Small,
        aspectRatio: AspectRatio = AspectRatio.Ratio1x1,
        isMainTextBlockFirst: Boolean = true,
    ) {
        val galleryContent = mutableListOf<TemplateImageWithDescription>()
        for (i in 1..30) {
            galleryContent.add(
                TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "gallery image $i"
                )
            )
        }
        GalleryTemplate(
            GalleryTemplateData(
                header = HeaderBlock(
                    text = TemplateText("Gallery Template example"),
                    icon = TemplateImageWithDescription(
                        ImageProvider(R.drawable.compose),
                        "test logo"
                    ),
                ),
                mainTextBlock = TextBlock(
                    text1 = TemplateText("Title1", TextType.Title),
                    text2 = TemplateText("Headline1", TextType.Headline),
                    text3 = TemplateText("Label1", TextType.Label),
                    priority = if (isMainTextBlockFirst) 0 else 1,
                ),
                mainImageBlock = ImageBlock(
                    images = listOf(
                        TemplateImageWithDescription(
                            ImageProvider(R.drawable.compose),
                            "test image"
                        )
                    ),
                    priority = if (isMainTextBlockFirst) 1 else 0,
                ),
                mainActionBlock = ActionBlock(
                    actionButtons = listOf(
                        TemplateTextButton(
                            actionRunCallback<DefaultNoopAction>(),
                            "Act1"
                        ),
                        TemplateTextButton(
                            actionRunCallback<DefaultNoopAction>(),
                            "Act2"
                        ),
                    ),
                ),
                galleryImageBlock = ImageBlock(
                    images = galleryContent,
                    aspectRatio = aspectRatio,
                    size = imageSize,
                ),
            )
        )
    }
}
