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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.EmbeddedFontFamily
import androidx.compose.ui.text.font.FontFamily
import java.awt.Font
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

/**
 * Obtain a [FontFamily] corresponding to this AWT [Font]. The Compose
 * runtime is able to match font families available in the system, and
 * ones embedded in the JetBrains Runtime (see note below). If the [Font]
 * is a logical font (i.e., one of [Font.DIALOG], [Font.DIALOG_INPUT],
 * [Font.SERIF], [Font.SANS_SERIF], or [Font.MONOSPACED]) then the actual
 * physical font will be resolved.
 *
 * If this [Font] is not known to Compose — e.g., because it's been created
 * from a file — the lookup will return a fallback font family. If you need
 * to create a font family from font files instead, you can do it like
 * this:
 * ```kotlin
 * val myFontFamily = FontFamily(
 *     File(myRegularFontFile, FontWeight.Normal, FontStyle.Normal),
 *     File(myItalicFontFile, FontWeight.Normal, FontStyle.Italic),
 *     // ...
 * )
 * ```
 *
 * **Note:** this will return a fallback font family, such as
 * [FontFamily.Default] or [FontFamily.Monospace], when running on Java
 * VMs other than the JetBrains Runtime, and/or if the current module
 * cannot access the `sun.font` module. To ensure the `sun.font` module
 * is accessible, you should pass this argument to the JVM: `--add-opens
 * java.desktop/sun.font=ALL-UNNAMED`.
 *
 * **Note:** on macOS, calling this on a reserved system font, such
 * as `.AppleSystemUI*` families, will return [FontFamily.Default].
 * Reserved system fonts aren't supposed to be accessed directly from
 * apps, and forcing it will not work anyway, as Skia doesn't support it.
 *
 * @see composeFontFamilyNameOrNull
 * @see fontFamilyNameOrNull
 */
@ExperimentalTextApi
fun Font.asComposeFontFamily(): FontFamily {
    val familyName = composeFontFamilyNameOrNull() ?: family
    if (hostOs == OS.MacOS && familyName.startsWith(".AppleSystemUI", ignoreCase = true)) {
        // On macOS, ".AppleSystemUI*" fonts are not directly accessible. Skia struggles
        // to handle them if we load them this way, so for now we return the default instead.
        return FontFamily.Default
    }

    return EmbeddedFontFamily(familyName) ?: FontFamily(familyName)
}

/**
 * Try to resolve a font family name, that could be a logical font face
 * (e.g., [Font.DIALOG]), to the actual physical font family it is an alias
 * for.
 *
 * **Note:** this will return `null` when running on Java VMs other
 * than the JetBrains Runtime, and if the current module cannot
 * access the `sun.font` module. To ensure the `sun.font` module is
 * accessible, you should pass this argument to the JVM: `--add-opens
 * java.desktop/sun.font=ALL-UNNAMED`.
 *
 * @return The resolved physical font name, or `null` if it can't be
 *     resolved (either it's unknown, or [isAbleToResolveFontProperties] is
 *     false)
 */
@ExperimentalTextApi
fun Font.composeFontFamilyNameOrNull(): String? =
    AwtFontUtils.resolvePhysicalFontFamilyNameOrNull(fontFamilyNameOrNull() ?: family, style)

/**
 * Get the _preferred font family name_, which should be used instead of
 * the [Font.getFamily] and `Font2D.familyName` when dealing with Compose.
 *
 * **Why is this better than the AWT API?** On Windows, and potentially in
 * other cases, the [Font.getFamily] returned by AWT contains the style and
 * weight of the [Font], in addition to the _actual_ font family name. This
 * can cause issues when trying to look up the corresponding Compose font
 * families. Besides that, it will result in duplicate entries when listing
 * font families.
 *
 * **Note:** this will return `null` when running on Java VMs other
 * than the JetBrains Runtime, and/or if the current module cannot
 * access the `sun.font` module. To ensure the `sun.font` module is
 * accessible, you should pass this argument to the JVM: `--add-opens
 * java.desktop/sun.font=ALL-UNNAMED`.
 */
@ExperimentalTextApi
internal fun Font.fontFamilyNameOrNull(): String? = AwtFontUtils.getPreferredFontFamilyName(this)
