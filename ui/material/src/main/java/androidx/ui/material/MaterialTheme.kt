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

import androidx.ui.engine.text.FontWeight
import androidx.ui.painting.Color
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
                <children />
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
    val h1: TypographyStyle = TypographyStyle("Roboto", FontWeight.w100, 96f, Case.SENTENCE, -1.5),
    val h2: TypographyStyle = TypographyStyle("Roboto", FontWeight.w100, 60f, Case.SENTENCE, -0.5),
    val h3: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 48f, Case.SENTENCE, 0.0),
    val h4: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 34f, Case.SENTENCE, 0.25),
    val h5: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 24f, Case.SENTENCE, 0.0),
    val h6: TypographyStyle = TypographyStyle("Roboto", FontWeight.w500, 20f, Case.SENTENCE, 0.15),
    val subtitle1: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 16f, Case.SENTENCE, 0.15),
    val subtitle2: TypographyStyle = TypographyStyle("Roboto", FontWeight.w500, 14f, Case.SENTENCE, 0.1),
    val body1: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 16f, Case.SENTENCE, 0.5),
    val body2: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 14f, Case.SENTENCE, 0.25),
    val button: TypographyStyle = TypographyStyle("Roboto", FontWeight.w500, 14f, Case.ALLCAPS, 1.25),
    val caption: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 12f, Case.SENTENCE, 0.4),
    val overline: TypographyStyle = TypographyStyle("Roboto", FontWeight.normal, 10f, Case.ALLCAPS, 1.5)
)

// TODO(clara): Unify with text core APIs (currently reflects Material spec)
data class TypographyStyle(
    val typeface: String = "Roboto",
    val weight: FontWeight = FontWeight.normal,
    val size: Float = 12f,
    val case: Case = Case.SENTENCE,
    val letterSpacing: Double = 1.0,
    val color : Color? = null
)