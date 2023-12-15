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

import androidx.compose.ui.text.platform.ReflectionUtil.getFieldValueOrNull
import java.awt.Font
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.skiko.hostOs

internal object AwtFontUtils {

    init {
        InternalFontApiChecker.isSunFontApiAccessible()
    }

    private val FontManagerFactoryClass = Class.forName("sun.font.FontManagerFactory")

    private val FontManagerClass = Class.forName("sun.font.FontManager")
    private val Font2DClass = Class.forName("sun.font.Font2D")
    private val CompositeFontClass = Class.forName("sun.font.CompositeFont")
    private val PhysicalFontClass = Class.forName("sun.font.PhysicalFont")
    private val CFontClass = if (hostOs.isMacOS) Class.forName("sun.font.CFont") else null

    // FontManagerFactory methods
    private val FontManagerFactory_getInstanceMethod =
        ReflectionUtil.getDeclaredMethodOrNull(FontManagerFactoryClass, "getInstance")

    // FontManager methods and fields
    private val FontManager_findFont2DMethod = ReflectionUtil.getDeclaredMethodOrNull(
        FontManagerClass,
        "findFont2D",
        String::class.java, // Font name
        Int::class.javaPrimitiveType!!, // Font style (e.g., Font.BOLD)
        Int::class.javaPrimitiveType!! // Fallback (one of the FontManager.*_FALLBACK values)
    )

    // Font2D methods and fields
    private val Font2D_getTypographicFamilyNameMethod =
        getFont2DMethodOrNull("getTypographicFamilyName")
    private val Font2D_getFamilyNameMethod =
        getFont2DMethodOrNull("getFamilyName", Locale::class.java)
    private val Font2D_handleField =
        ReflectionUtil.findFieldInHierarchy(Font2DClass) { it.name == "handle" }

    // Font2DHandle fields
    private val Font2DHandle_font2DField =
        ReflectionUtil.findFieldInHierarchy(Class.forName("sun.font.Font2DHandle")) {
            it.name == "font2D"
        }

    // CompositeFont methods
    private val CompositeFont_getSlotFontMethod =
        ReflectionUtil.getDeclaredMethodOrNull(
            clazz = CompositeFontClass,
            name = "getSlotFont",
            Int::class.javaPrimitiveType!!
        )

    // Copy of FontManager.LOGICAL_FALLBACK
    private const val LOGICAL_FALLBACK = 2

    private val font2DHandlesCache = ConcurrentHashMap<Font, Any>()

    /**
     * Indicate whether the current JVM is able to resolve font properties
     * accurately or not.
     *
     * This value will be `true` if using the JetBrains Runtime. It will be
     * `false` otherwise, indicating that this class is not able to return
     * valid values.
     *
     * If the return value is `false`, you should assume all APIs in this class
     * will return `null` as we can't obtain the necessary information.
     *
     * On other JVMs running on Windows and Linux, the AWT implementation
     * is not enumerating font families correctly. E.g., you may have these
     * entries for JetBrains Mono, instead of a single entry: _JetBrains
     * Mono, JetBrains Mono Bold, JetBrains Mono ExtraBold, JetBrains Mono
     * ExtraLight, JetBrains Mono Light, JetBrains Mono Medium, JetBrains Mono
     * SemiBold, JetBrains Mono Thin_.
     *
     * On the JetBrains Runtime, there are additional APIs that provide the
     * necessary information needed to list the actual font families as single
     * entries, as one would expect.
     */
    private val isAbleToResolveFontProperties: Boolean =
        InternalFontApiChecker.isRunningOnJetBrainsRuntime() &&
            InternalFontApiChecker.isSunFontApiAccessible() &&
            Font2D_getTypographicFamilyNameMethod != null

    fun resolvePhysicalFontFamilyNameOrNull(familyName: String, style: Int = Font.PLAIN): String? {
        if (!isAbleToResolveFontProperties) return null

        val fontManager = getSunFontManagerInstance()
        val font2D =
            FontManager_findFont2DMethod?.invoke(fontManager, familyName, style, LOGICAL_FALLBACK)
                ?: return null

        return when {
            // For Windows
            CompositeFontClass.isInstance(font2D) -> {
                val physicalFontObject = CompositeFont_getSlotFontMethod?.invoke(font2D, 0)
                Font2D_getFamilyNameMethod?.invoke(
                    physicalFontObject,
                    Locale.getDefault()
                ) as String?
            }

            // For macOS
            CFontClass?.isInstance(font2D) == true -> {
                val nativeFontName =
                    getFieldValueOrNull(
                        CFontClass,
                        font2D,
                        String::class.java,
                        "nativeFontName"
                    )
                getPreferredFontFamilyName(Font(nativeFontName, Font.PLAIN, 10))
            }

            // For Linux (covers TrueTypeFont & co.)
            PhysicalFontClass.isInstance(font2D) -> {
                Font2D_getTypographicFamilyNameMethod?.invoke(font2D) as String?
            }

            else -> null
        }
    }

    fun getPreferredFontFamilyName(font: Font): String? {
        if (!isAbleToResolveFontProperties) return null

        val font2D = font.obtainFont2DOrNull() ?: return null
        return Font2D_getTypographicFamilyNameMethod?.invoke(font2D) as? String
    }

    private fun Font.obtainFont2DOrNull(): Any? {
        // Don't store the Font2D instance directly, in case the handle may be changed
        // later on. Logic adopted from java.awt.Font#getFont2D()
        val handle = font2DHandlesCache.getOrPut(this) {
            val fontManager = getSunFontManagerInstance()
            val font2D =
                FontManager_findFont2DMethod?.invoke(fontManager, name, style, LOGICAL_FALLBACK)
            Font2D_handleField?.get(font2D)
        }

        return Font2DHandle_font2DField?.get(handle)
    }

    private fun getSunFontManagerInstance() =
        FontManagerFactory_getInstanceMethod?.invoke(null)

    private fun getFont2DMethodOrNull(methodName: String, vararg parameters: Class<*>): Method? =
        ReflectionUtil.getDeclaredMethodOrNull(Font2DClass, methodName, *parameters)
}
