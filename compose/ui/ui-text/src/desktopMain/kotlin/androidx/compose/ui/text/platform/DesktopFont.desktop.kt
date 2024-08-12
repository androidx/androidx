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
package androidx.compose.ui.text.platform

import org.jetbrains.skia.FontStyle as SkFontStyle
import org.jetbrains.skia.Typeface as SkTypeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import java.io.File
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontWidth

actual sealed class PlatformFont : Font {
    actual abstract val identity: String
    internal actual val cacheKey: String
        get() = "${this::class.qualifiedName}|$identity|weight=${weight.weight}|style=$style"
}

/**
 * Defines a Font using a resource name.
 *
 * @param name The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a
 *     font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses
 *     this to match a font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @see FontFamily
 */

class ResourceFont internal constructor(
    val name: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : PlatformFont() {
    override val identity
        get() = name

    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceFont

        if (name != other.name) return false
        if (weight != other.weight) return false
        return style == other.style
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResourceFont(name='$name', weight=$weight, style=$style)"
    }
}

/**
 * Creates a Font using a resource name.
 *
 * @param resource The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a
 *     font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses
 *     this to match a font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @see FontFamily
 */
fun Font(
    resource: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = ResourceFont(resource, weight, style)

/**
 * Defines a Font using a file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a
 *     font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses
 *     this to match a font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @see FontFamily
 */
class FileFont internal constructor(
    val file: File,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
) : PlatformFont() {
    override val identity
        get() = file.toString()

    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileFont

        if (file != other.file) return false
        if (weight != other.weight) return false
        return style == other.style
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileFont(file=$file, weight=$weight, style=$style)"
    }
}

/**
 * Creates a Font using a file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a
 *     font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses
 *     this to match a font to a font request that is given in a
 *     [androidx.compose.ui.text.SpanStyle].
 * @see FontFamily
 */
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = FileFont(file, weight, style)

internal actual fun loadTypeface(font: Font): SkTypeface {
    if (font !is PlatformFont) {
        throw IllegalArgumentException("Unsupported font type: $font")
    }
    return when (font) {
        is ResourceFont -> typefaceResource(font.name)
        // TODO: replace with FontMgr.makeFromFile(font.file.toString())
        is FileFont -> FontMgr.default.makeFromFile(font.file.toString())
        is LoadedFont -> FontMgr.default.makeFromData(Data.makeFromBytes(font.getData()))
        is SystemFont -> FontMgr.default.matchFamilyStyle(font.identity, font.skFontStyle)
    } ?: (FontMgr.default.legacyMakeTypeface(font.identity, font.skFontStyle)
        ?: error("loadTypeface legacyMakeTypeface failed"))
}

private fun typefaceResource(resourceName: String): SkTypeface {
    val contextClassLoader = Thread.currentThread().contextClassLoader!!
    val resource = contextClassLoader.getResourceAsStream(resourceName)
        ?: (::typefaceResource.javaClass).getResourceAsStream(resourceName)
        ?: error("Can't load font from $resourceName")

    val bytes = resource.use { it.readAllBytes() }
    return FontMgr.default.makeFromData(Data.makeFromBytes(bytes))!!
}

private val Font.skFontStyle: SkFontStyle
    get() = SkFontStyle(
        weight = weight.weight,
        width = FontWidth.NORMAL,
        slant = if (style == FontStyle.Italic) FontSlant.ITALIC else FontSlant.UPRIGHT
    )

internal actual fun currentPlatform(): Platform {
    val name = System.getProperty("os.name")
    return when {
        name.startsWith("Linux") -> Platform.Linux
        name.startsWith("Win") -> Platform.Windows
        name == "Mac OS X" -> Platform.MacOS
        else -> Platform.Unknown
    }
}
