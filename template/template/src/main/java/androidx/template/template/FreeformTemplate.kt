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

package androidx.template.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A [GlanceTemplate] optimized to highlight a single piece of data.
 */
abstract class FreeformTemplate : GlanceTemplate<FreeformTemplate.Data>() {

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            Column(
                modifier = createTopLevelModifier(it.backgroundColor, it.backgroundImage),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TODO: Text block ordering
                it.title?.let { title -> Title(title.text) }
                it.subtitle?.let { subtitle -> Subtitle(subtitle.text) }
            }
            Action(it.actionIcon)
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            Column(
                modifier = createTopLevelModifier(it.backgroundColor, it.backgroundImage),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TemplateHeader(it.headerIcon, it.header)
                it.title?.let { title -> Title(title.text) }
                it.subtitle?.let { subtitle -> Subtitle(subtitle.text) }
            }
            Action(it.actionIcon)
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            Column(
                modifier = createTopLevelModifier(it.backgroundColor, it.backgroundImage),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TemplateHeader(it.headerIcon, it.header)
                it.title?.let { title -> Title(title.text) }
                it.subtitle?.let { subtitle -> Subtitle(subtitle.text) }
            }
            Action(it.actionIcon)
        }
    }

    // TODO: Scale text size for glanceable size
    @Composable
    private fun Title(title: String) {
        Text(title, style = TextStyle(fontSize = 28.sp, textAlign = TextAlign.Center), maxLines = 3)
    }

    @Composable
    private fun Subtitle(subtitle: String) {
        Text(
            subtitle,
            style = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center),
            maxLines = 1
        )
    }

    @Composable
    private fun Action(actionIcon: TemplateImageButton?) {
        actionIcon?.let {
            Box(
                modifier =
                GlanceModifier.fillMaxSize().padding(16.dp).background(Color.Transparent),
                contentAlignment = Alignment.BottomStart
            ) {
                Image(
                    provider = it.image.image,
                    contentDescription = it.image.description,
                    modifier = GlanceModifier.clickable(it.action)
                )
            }
        }
    }

    private fun createTopLevelModifier(
        backgroundColor: ColorProvider,
        backgroundImage: ImageProvider?
    ): GlanceModifier {
        var modifier = GlanceModifier.fillMaxSize().padding(16.dp).background(backgroundColor)
        backgroundImage?.let { image ->
            modifier = modifier.background(image, ContentScale.Crop)
        }

        return modifier
    }

    /**
     * The semantic data required to build [FreeformTemplate] layouts
     *
     * @param backgroundColor Glanceable background color
     * @param headerIcon Logo icon, displayed in the glanceable header
     * @param actionIcon Action icon button
     * @param header Main header text
     * @param title Text section main title, priority ordered
     * @param subtitle Text section subtitle, priority ordered
     * @param backgroundImage Background image, if set replaces the glanceable background
     */
    class Data(
        val backgroundColor: ColorProvider,
        val headerIcon: TemplateImageWithDescription,
        val actionIcon: TemplateImageButton?,
        val header: TemplateText? = null,
        val title: TemplateText? = null,
        val subtitle: TemplateText? = null,
        val backgroundImage: ImageProvider? = null,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (backgroundColor != other.backgroundColor) return false
            if (headerIcon != other.headerIcon) return false
            if (actionIcon != other.actionIcon) return false
            if (header != other.header) return false
            if (title != other.title) return false
            if (subtitle != other.subtitle) return false
            if (backgroundImage != other.backgroundImage) return false

            return true
        }

        override fun hashCode(): Int {
            var result = backgroundColor.hashCode()
            result = 31 * result + headerIcon.hashCode()
            result = 31 * result + (actionIcon?.hashCode() ?: 0)
            result = 31 * result + (header?.hashCode() ?: 0)
            result = 31 * result + (title?.hashCode() ?: 0)
            result = 31 * result + (subtitle?.hashCode() ?: 0)
            result = 31 * result + (backgroundImage?.hashCode() ?: 0)
            return result
        }
    }
}
