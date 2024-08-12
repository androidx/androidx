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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant

internal object JetBrainsRuntimeFontFamilies {

    /**
     * A map of known embedded font family names to the actual
     * [families][FontFamily].
     *
     * Will be empty if not running on the JetBrains Runtime, and/or if the
     * current module cannot access the `sun.font` module.
     */
    val embeddedFamilies: MutableMap<String, FontFamily> = HashMap()

    // Reflection code to access JBR private methods
    private val FontManagerFactoryClass = Class.forName("sun.font.FontManagerFactory")
    private val SunFontManagerClass = Class.forName("sun.font.SunFontManager")

    // FontManagerFactory methods
    private val FontManagerFactory_getInstanceMethod =
        FontManagerFactoryClass.declaredMethods.find { it.name == "getInstance" }
            ?.let { method ->
                kotlin.runCatching { method.isAccessible = true }
                    .fold(
                        onSuccess = { return@fold method },
                        onFailure = { null },
                    )
            }

    // SunFontManagerFactory fields
    private val SunFontManager_jreBundledFontFiles =
        SunFontManagerClass.declaredFields.find { it.name == "jreBundledFontFiles" }
            ?.let { field ->
                kotlin.runCatching { field.isAccessible = true }
                    .fold(
                        onSuccess = { return@fold field },
                        onFailure = { null },
                    )
            }

    private val javaHomePath
        get() = Path(System.getProperty("java.home"))

    private val jbrEmbeddedFontsPath
        get() = javaHomePath.resolve("lib").resolve("fonts")

    init {
        if (
            InternalFontApiChecker.isRunningOnJetBrainsRuntime() &&
            SunFontManager_jreBundledFontFiles != null
        ) {
            cacheJetBrainsRuntimeEmbeddedFonts()
        }
    }

    private fun cacheJetBrainsRuntimeEmbeddedFonts() {
        val field = SunFontManager_jreBundledFontFiles ?: return

        try {
            field.isAccessible = true

            val fontManager = getSunFontManagerInstance()

            @Suppress("UNCHECKED_CAST")
            val embeddedFontFileNames = field.get(fontManager) as HashSet<String>
            val embeddedFontPaths = embeddedFontFileNames.map { jbrEmbeddedFontsPath.resolve(it) }
                .sortedBy { it.absolutePathString() }
                .distinctBy { it.absolutePathString() }

            embeddedFontPaths.asSequence()
                .map { path ->
                    val absolutePath = path.absolutePathString()

                    // We need to parse the typeface to extract its weight and style
                    val typeface = FontMgr.default.makeFromFile(absolutePath)
                        ?: error("makeFromFile $absolutePath failed")
                    val weight = FontWeight(typeface.fontStyle.weight)
                    val style = when (typeface.fontStyle.slant) {
                        FontSlant.UPRIGHT -> FontStyle.Normal
                        FontSlant.ITALIC, FontSlant.OBLIQUE -> FontStyle.Italic
                    }

                    typeface.familyName to FileFont(File(absolutePath), weight, style)
                }
                .distinctBy { (_, font) -> font.file.absolutePath }
                .groupBy { (familyName, _) -> familyName }
                .forEach { (identity, fileFonts) ->
                    val fontFamily = FontListFontFamily(fileFonts.map { it.second })
                    embeddedFamilies += identity.lowercase() to fontFamily
                }
        } finally {
            field.isAccessible = false
        }
    }

    private fun getSunFontManagerInstance() =
        FontManagerFactory_getInstanceMethod?.invoke(null)
}
