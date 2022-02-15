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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
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
                TemplateHeader(it.headerIcon, it.header)
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
                TemplateHeader(it.headerIcon, it.header)
                TextBlock(it.title, it.subtitle, it.bodyText)
                it.mainImage?.let { image ->
                    Image(provider = image.image, contentDescription = image.description)
                }
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
                    TemplateHeader(it.headerIcon, it.header)
                }
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        TextBlock(it.title, it.subtitle, it.bodyText)
                        Button(it)
                    }
                    it.mainImage?.let { image ->
                        Image(
                            provider = image.image,
                            contentDescription = image.description,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TextBlock(title: String?, subtitle: String?, body: String?) {
        // TODO: Text color customization
        val color = ColorProvider(R.color.text_default)
        Column {
            title?.let {
                Text(title, style = TextStyle(fontSize = 36.sp, color = color), maxLines = 2)
            }
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
            else -> {
                Log.e(TAG, "Unrecognized button type: ${data.button}")
            }
        }
    }

    /**
     * The semantic data required to build [SingleEntityTemplate] layouts.
     *
     * @param backgroundColor Glanceable background color
     * @param headerIcon Logo icon, displayed in the glanceable header
     * @param header Main header text
     * @param title Text section main title
     * @param subtitle Text section subtitle
     * @param bodyText Text section body text
     * @param button Action button
     * @param mainImage Main image content
     */
    // TODO: add optional ordering for text vs image sections, determine granularity level for
    //  setting text colors
    class Data(
        val backgroundColor: ColorProvider,
        val headerIcon: TemplateImageWithDescription,
        val header: String? = null,
        val title: String? = null,
        val subtitle: String? = null,
        val bodyText: String? = null,
        val button: TemplateButton? = null,
        val mainImage: TemplateImageWithDescription? = null,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (backgroundColor != other.backgroundColor) return false
            if (headerIcon != other.headerIcon) return false
            if (header != other.header) return false
            if (title != other.title) return false
            if (subtitle != other.subtitle) return false
            if (bodyText != other.bodyText) return false
            if (button != other.button) return false
            if (mainImage != other.mainImage) return false

            return true
        }

        override fun hashCode(): Int {
            var result = backgroundColor.hashCode()
            result = 31 * result + headerIcon.hashCode()
            result = 31 * result + (header?.hashCode() ?: 0)
            result = 31 * result + (title?.hashCode() ?: 0)
            result = 31 * result + (subtitle?.hashCode() ?: 0)
            result = 31 * result + (bodyText?.hashCode() ?: 0)
            result = 31 * result + (button?.hashCode() ?: 0)
            result = 31 * result + (mainImage?.hashCode() ?: 0)
            return result
        }
    }

    private companion object {
        private const val TAG = "SingleEntityTemplate"
    }
}
