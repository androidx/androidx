/*
 * Copyright 2024 The Android Open Source Project
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

import org.jetbrains.skia.Typeface as SkTypeface
import org.jetbrains.skia.FontStyle as SkFontStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontWidth
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

internal actual fun loadTypeface(font: Font): SkTypeface {
    if (font !is PlatformFont) {
        throw IllegalArgumentException("Unsupported font type: $font")
    }
    return when (font) {
        is LoadedFont -> FontMgr.default.makeFromData(Data.makeFromBytes(font.getData()))
            ?: error("loadTypeface makeFromData failed")
        is SystemFont -> FontMgr.default.legacyMakeTypeface(font.identity, font.skFontStyle)
            ?: error("loadTypeface legacyMakeTypeface failed")
    }
}

private val Font.skFontStyle: SkFontStyle
    get() = SkFontStyle(
        weight = weight.weight,
        width = FontWidth.NORMAL,
        slant = if (style == FontStyle.Italic) FontSlant.ITALIC else FontSlant.UPRIGHT
    )

internal actual fun currentPlatform(): Platform = when (hostOs) {
    OS.Android -> Platform.Android
    OS.Ios -> Platform.IOS
    OS.MacOS -> Platform.MacOS
    OS.Linux -> Platform.Linux
    OS.Windows -> Platform.Windows
    else -> Platform.Unknown
}
