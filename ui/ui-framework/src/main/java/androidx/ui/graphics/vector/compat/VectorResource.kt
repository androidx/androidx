/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector.compat

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Xml
import androidx.annotation.CheckResult
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.ContextAmbient
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.graphics.vector.VectorAsset
import org.xmlpull.v1.XmlPullParserException

/**
 * Convenience method to simultaneously load and draw a vector resource
 */
@Composable
fun DrawVectorResource(resId: Int) = DrawVector(vectorImage = +vectorResource(resId))

/**
 * Effect used to load a [VectorAsset] from an Android resource id
 * This is useful for querying top level properties of the [VectorAsset]
 * such as it's intrinsic width and height to be able to size components
 * based off of it's dimensions appropriately
 */
@CheckResult(suggest = "+")
fun vectorResource(resId: Int) = effectOf<VectorAsset> {
    val context = +ambient(ContextAmbient)
    val res = context.resources
    val theme = context.theme
    +memo(resId) {
        loadVectorResource(theme, res, resId)
    }
}

@Throws(XmlPullParserException::class)
@SuppressWarnings("RestrictedApi")
fun loadVectorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int
): VectorAsset {
    @SuppressLint("ResourceType") val parser = res.getXml(resId)
    val attrs = Xml.asAttributeSet(parser)
    val builder = parser.seekToStartTag().createVectorImageBuilder(res, theme, attrs)

    while (!parser.isAtEnd()) {
        parser.parseCurrentVectorNode(res, attrs, theme, builder)
        parser.next()
    }
    return builder.build()
}