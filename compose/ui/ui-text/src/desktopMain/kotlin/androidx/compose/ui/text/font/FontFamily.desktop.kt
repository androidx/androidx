/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.font

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.JetBrainsRuntimeFontFamilies
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.text.platform.asComposeFontFamily
import java.awt.Font

/**
 * Load a [FontFamily] from a system font family name. If the [familyName]
 * doesn't match any available family in the system, the lookup will return
 * a fallback font family.
 *
 * If you're trying to use an AWT [Font] in Compose, use the
 * [Font.asComposeFontFamily] function instead, which will take care of
 * some AWT-specific quirks, too. If you want to load a font family
 * embedded in the JetBrains Runtime, you can use [EmbeddedFontFamily].
 *
 * @param familyName The name of the system font family to load.
 * @return the requested system font family, or a fallback if [familyName]
 *     doesn't match any available system font family.
 * @see Font.asComposeFontFamily
 * @see EmbeddedFontFamily
 */
@ExperimentalTextApi
@Stable
fun FontFamily(familyName: String): FontFamily =
    FontListFontFamily(
        listOf(
            SystemFont(familyName, FontWeight.W100, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W200, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W300, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W400, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W500, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W600, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W700, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W800, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W900, FontStyle.Normal),
            SystemFont(familyName, FontWeight.W100, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W200, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W300, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W400, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W500, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W600, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W700, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W800, FontStyle.Italic),
            SystemFont(familyName, FontWeight.W900, FontStyle.Italic),
        )
    )

/**
 * Load a [FontFamily] embedded in the JetBrains Runtime. It will return
 * `null` if the font family isn't embedded in the JetBrains Runtime,
 * or if using any Java runtime other than the JetBrains Runtime.
 *
 * Using this requires the current module to have access to the `sun.font`
 * APIs, which are closed by default. To open the access to those APIs, you
 * need to pass this argument to the JVM:
 * ```
 * --add-opens java.desktop/sun.font=ALL-UNNAMED
 * ```
 *
 * If the `sun.font` API is not accessible, this will always return `null`.
 *
 * @param familyName The case-insensitive font family name to load.
 * @return the requested font family, if running on the JetBrains Runtime,
 *     and the `sun.font` API is accessible, and the font family exists in
 *     the runtime. Otherwise, `null`.
 */
@ExperimentalTextApi
@Stable
fun EmbeddedFontFamily(familyName: String): FontFamily? =
    JetBrainsRuntimeFontFamilies.embeddedFamilies[familyName.lowercase()]
