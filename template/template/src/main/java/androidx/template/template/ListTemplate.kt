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
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A basic [GlanceTemplate] implementation based around a list of entities, using [Data].
 */
abstract class ListTemplate : GlanceTemplate<ListTemplate.Data>() {
    // TODO: Placeholder layouts

    @Composable
    override fun WidgetLayoutCollapsed() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { modifier = modifier.background(it) }
            Column(modifier = modifier) {
                it.header?.let { header ->
                    TemplateHeader(it.headerIcon, header)
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.title?.let {
                    Text(it.text, style = TextStyle(fontSize = 20.sp))
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.button?.let {
                    Button(text = it.text, onClick = it.action)
                }
            }
        }
    }

    @Composable
    override fun WidgetLayoutVertical() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { modifier = modifier.background(it) }
            Column(modifier = modifier) {
                it.header?.let { header ->
                    TemplateHeader(it.headerIcon, header)
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.title?.let {
                    Text(it.text, style = TextStyle(fontSize = 20.sp))
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.button?.let {
                    Button(text = it.text, onClick = it.action)
                }
                LazyColumn {
                    itemsIndexed(it.listContent) { _, item ->
                        // TODO: Extract and allow override
                        var itemModifier = GlanceModifier.fillMaxWidth()
                        item.action?.let { itemModifier = itemModifier.clickable(it) }
                        Row(modifier = itemModifier) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(item.title.text, style = TextStyle(fontSize = 18.sp))
                                item.body?.let { Text(item.body.text) }
                            }
                            item.image?.let {
                                Spacer(modifier = GlanceModifier.width(16.dp))
                                Image(provider = it.image, contentDescription = it.description)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun WidgetLayoutHorizontal() {
        getData(currentState()).let {
            var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
            it.backgroundColor?.let { modifier = modifier.background(it) }
            Column(modifier = modifier) {
                it.header?.let { header ->
                    TemplateHeader(it.headerIcon, header)
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.title?.let {
                    Text(it.text, style = TextStyle(fontSize = 20.sp))
                    Spacer(modifier = GlanceModifier.height(16.dp))
                }
                it.button?.let {
                    Button(text = it.text, onClick = it.action)
                }
            }
        }
    }

    /**
     * The data required to display a list item
     *
     * @param title list item title text
     * @param body list item body text
     * @param action list item onClick action
     * @param image list item image
     */
    // TODO: Allow users to define a custom list item
    public class ListItem(
        val title: TemplateText,
        val body: TemplateText?,
        val action: Action?,
        val image: TemplateImageWithDescription?
    ) {

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + (body?.hashCode() ?: 0)
            result = 31 * result + (action?.hashCode() ?: 0)
            result = 31 * result + (image?.hashCode() ?: 0)
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ListItem

            if (title != other.title) return false
            if (body != other.body) return false
            if (action != other.action) return false
            if (image != other.image) return false

            return true
        }
    }

    /**
     * The semantic data required to build [ListTemplate] layouts.
     *
     * @param headerIcon Logo icon, displayed in the glanceable header
     * @param listContent
     * @param header Main header text
     * @param title Text section main title
     * @param button Action button
     * @param backgroundColor Glanceable background color
     */
    class Data(
        val headerIcon: TemplateImageWithDescription,
        val listContent: List<ListItem> = listOf(),
        val header: TemplateText? = null,
        val title: TemplateText? = null,
        val button: TemplateTextButton? = null,
        val backgroundColor: ColorProvider? = null
    ) {

        override fun hashCode(): Int {
            var result = header.hashCode()
            result = 31 * result + headerIcon.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + button.hashCode()
            result = 31 * result + listContent.hashCode()
            result = 31 * result + backgroundColor.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (header != other.header) return false
            if (headerIcon != other.headerIcon) return false
            if (title != other.title) return false
            if (button != other.button) return false
            if (listContent != other.listContent) return false
            if (backgroundColor != other.backgroundColor) return false

            return true
        }
    }
}
