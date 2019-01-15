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

package androidx.ui.material

import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.painting.Color
import androidx.ui.painting.TextStyle
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component

class MaterialTheme(
    @Children
    val children: () -> Unit
) : Component() {

    var colors: MaterialColors = MaterialColors()
    var typography: MaterialTypography = MaterialTypography()

    override fun compose() {
        <Colors.Provider value=colors>
            <Typography.Provider value=typography>
                <CurrentTextStyleProvider value=typography.body1>
                    <children />
                </CurrentTextStyleProvider>
            </Typography.Provider>
        </Colors.Provider>
    }
}

val Colors = Ambient<MaterialColors>("colors") { error("No colors found!") }

val Typography = Ambient<MaterialTypography>("typography") { error("No typography found!") }

data class MaterialColors(
    val primary: Color = Color(0xFF6200EE.toInt()),
    val primaryVariant: Color = Color(0xFF3700B3.toInt()),
    val secondary: Color = Color(0xFF03DAC6.toInt()),
    val secondaryVariant: Color = Color(0xFF018786.toInt()),
    val background: Color = Color(0xFFFFFFFF.toInt()),
    val surface: Color = Color(0xFFFFFFFF.toInt()),
    val error: Color = Color(0xFFB00020.toInt()),
    val onPrimary: Color = Color(0xFFFFFFFF.toInt()),
    val onSecondary: Color = Color(0xFF000000.toInt()),
    val onBackground: Color = Color(0xFF000000.toInt()),
    val onSurface: Color = Color(0xFF000000.toInt()),
    val onError: Color = Color(0xFFFFFFFF.toInt())
)

data class MaterialTypography(
    // TODO(clara): case
    // TODO(clara): letter spacing (specs don't match)
    // TODO(clara): b/123001228 need a font abstraction layer
    // TODO(clara): fontSize should be a Dimension, translating here will loose context changes
    val h1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w100,
        fontSize = 96f),
    val h2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w100,
        fontSize = 60f),
    val h3: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 48f),
    val h4: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 34f),
    val h5: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 24f),
    val h6: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 20f),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 16f),
    val subtitle2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 14f),
    val body1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 16f),
    val body2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 14f),
    val button: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 14f),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 12f),
    val overline: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 10f)
)
