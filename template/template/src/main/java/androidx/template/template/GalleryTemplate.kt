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
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Spacer
import androidx.glance.layout.width

/**
 * A [GalleryTemplate] implementation, using [Data].
 */
abstract class GalleryTemplate : GlanceTemplate<GalleryTemplate.Data>() {

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            Column(
              modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor),
            ) {
              Row() {
                Image(provider = it.image.image, contentDescription = it.image.description)
                Image(provider = it.image.image, contentDescription = it.image.description)
               }
               Text(it.title)
               Text(it.headline)
            }
        }
    }

    // TODO: Implement when UX has specs.
    @Composable
    override fun WidgetLayoutVertical() {
      getData(currentState()).let {
        Column(
          modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor),
        ) {
        }
    }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
      getData(currentState()).let {
        Row(
          modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(it.backgroundColor),
          verticalAlignment = Alignment.CenterVertically
        ) {
        Column() {
            Image(provider = it.image.image, contentDescription = it.image.description)
        }
        Spacer(GlanceModifier.width(8.dp))
        Column() {
          Text(it.title)
          Text(it.headline)
        }
        Column(verticalAlignment = Alignment.Top) {
          Image(provider = it.image.image, contentDescription = it.image.description)
        }
      }
    }
    }

    /**
     * The semantic data required to build template layouts
     *
     * @param header header of the template
     * @param title title of the template
     * @param headline headline of the template
     * @param image image of the template
     * @param logo logo of the template
     * @param backgroundColor The background color to apply to the template
     */
    class Data(
        val header: String,
        val title: String,
        val headline: String,
        val image: TemplateImageWithDescription,
        val logo: TemplateImageWithDescription,
        val backgroundColor: ColorProvider,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (header != other.header) return false
            if (title != other.title) return false
            if (headline != other.headline) return false
            if (image != other.image) return false
            if (logo != other.logo) return false
            if (backgroundColor != other.backgroundColor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = header.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + headline.hashCode()
            result = 31 * result + image.hashCode()
            result = 31 * result + logo.hashCode()
            result = 31 * result + backgroundColor.hashCode()
            return result
        }
    }
}
