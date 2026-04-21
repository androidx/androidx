/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.text

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.GenericFontFamily

/**
 * A sealed class that represents the concept of a FontFamily in Remote Compose.
 *
 * @param name The name used by the player to resolve the font (e.g., "serif").
 * @param fontFamilyName A display name used for debugging and logging purposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class RemoteFontFamily(public val name: String, private val fontFamilyName: String) {

    /** Represents the default system font family. */
    public object Default : RemoteFontFamily("default", "RemoteFontFamily.Default")

    /** Represents the sans-serif system font family. */
    public object SansSerif : RemoteFontFamily("sans-serif", "RemoteFontFamily.SansSerif")

    /** Represents the serif system font family. */
    public object Serif : RemoteFontFamily("serif", "RemoteFontFamily.Serif")

    /** Represents the monospace system font family. */
    public object Monospace : RemoteFontFamily("monospace", "RemoteFontFamily.Monospace")

    /** Represents the cursive system font family. */
    public object Cursive : RemoteFontFamily("cursive", "RemoteFontFamily.Cursive")

    /** Represents a custom or system font family referenced by name. */
    public class Named(name: String) : RemoteFontFamily(name, "RemoteFontFamily.Named($name)") {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Named) return false
            if (name != other.name) return false
            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    override fun toString(): String = fontFamilyName

    public companion object {
        /** Maps a Compose [FontFamily] to a [RemoteFontFamily]. */
        public fun fromComposeFontFamily(fontFamily: FontFamily?): RemoteFontFamily? {
            if (fontFamily == null) return null
            return when (fontFamily) {
                FontFamily.Default -> Default
                FontFamily.SansSerif -> SansSerif
                FontFamily.Serif -> Serif
                FontFamily.Monospace -> Monospace
                FontFamily.Cursive -> Cursive
                is GenericFontFamily -> Named(fontFamily.name)
                else -> null
            }
        }
    }
}
