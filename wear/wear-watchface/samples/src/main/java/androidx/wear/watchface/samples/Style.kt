/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.samples

import android.content.Context
import android.graphics.Color
import androidx.wear.watchface.complications.rendering.ComplicationDrawable

private fun Context.getStyleResourceId(
    styleResourceId: Int,
    attributeId: Int,
    defaultResourceId: Int
): Int {
    val styleArray = obtainStyledAttributes(
        styleResourceId,
        intArrayOf(attributeId)
    )
    return styleArray.getResourceId(0, defaultResourceId).also { styleArray.recycle() }
}

private fun Context.getStyleColor(
    styleResourceId: Int,
    attributeId: Int,
    defaultColor: Int
): Int {
    val colorArray = obtainStyledAttributes(
        styleResourceId,
        intArrayOf(attributeId)
    )
    return colorArray.getColor(0, defaultColor).also { colorArray.recycle() }
}

class ColorStyle(
    val primaryColor: Int,
    val secondaryColor: Int,
    val backgroundColor: Int,
    val outerElementColor: Int
) {
    companion object {
        fun create(context: Context, styleName: String): ColorStyle {
            val styleResourceId =
                context.resources.getIdentifier(styleName, "style", context.packageName)
            return ColorStyle(
                context.getStyleColor(styleResourceId, R.attr.primary_color, Color.WHITE),
                context.getStyleColor(styleResourceId, R.attr.secondary_color, Color.WHITE),
                context.getStyleColor(styleResourceId, R.attr.background_color, Color.BLACK),
                context.getStyleColor(styleResourceId, R.attr.outer_element_color, Color.WHITE)
            )
        }
    }
}

class WatchFaceColorStyle(
    val activeStyle: ColorStyle,
    val ambientStyle: ColorStyle,
    private val complicationResourceId: Int
) {
    companion object {
        fun create(context: Context, baseStyleName: String) =
            WatchFaceColorStyle(
                ColorStyle.create(context, baseStyleName + "_active"),
                ColorStyle.create(context, baseStyleName + "_ambient"),
                getComplicationResourceId(context, baseStyleName)
            )

        private fun getComplicationResourceId(context: Context, styleName: String): Int {
            val styleResourceId =
                context.resources.getIdentifier(styleName, "style", context.packageName)
            return context.getStyleResourceId(
                styleResourceId, R.attr.complication, R.drawable.complication_green_style
            )
        }
    }

    fun getDrawable(context: Context) =
        ComplicationDrawable.getDrawable(context, complicationResourceId)
}
