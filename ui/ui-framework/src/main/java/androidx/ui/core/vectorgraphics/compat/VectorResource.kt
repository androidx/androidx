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

package androidx.ui.core.vectorgraphics.compat

import androidx.compose.composer
import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Xml
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.ContextAmbient
import androidx.ui.core.vectorgraphics.DrawVector
import androidx.ui.core.vectorgraphics.VectorAsset
import org.xmlpull.v1.XmlPullParserException

@Composable
fun VectorResource(resId: Int) {
    val context = +ambient(ContextAmbient)
    val res = context.resources
    val theme = context.theme
    val vectorImage = +memo(resId) {
        loadVectorResource(theme, res, resId)
    }
    DrawVector(vectorImage = vectorImage)
}

@Throws(XmlPullParserException::class)
@SuppressWarnings("RestrictedApi")
fun loadVectorResource(theme: Resources.Theme? = null, res: Resources, resId: Int): VectorAsset {

    @SuppressLint("ResourceType") val parser = res.getXml(resId)
    val attrs = Xml.asAttributeSet(parser)
    val builder = parser.seekToStartTag().createVectorImageBuilder(res, theme, attrs)

    while (!parser.isAtEnd()) {
        parser.parseCurrentVectorNode(res, attrs, theme, builder)
        parser.next()
    }
    return builder.build()
}