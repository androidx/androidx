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

package androidx.compose.ui.text.googlefonts

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.FontRes
import androidx.core.content.res.FontResourcesParserCompat
import java.lang.IllegalArgumentException

/**
 * Load a Google Font from XML
 *
 * This will load only the name and besteffort parameters.
 *
 * Compared to the string constructor, this loader adds additional overhead. New code should prefer
 * to use `GoogleFont(name: String)`.
 *
 * @param context to load font from
 * @param fontXml fontRes to load
 * @throws IllegalArgumentException if the fontRes does not exist or is not an xml GoogleFont
 */
// This is an API for accessing Google Fonts at fonts.google.com
@Suppress("MentionsGoogle")
@SuppressLint("ResourceType")
fun GoogleFont(context: Context, @FontRes fontXml: Int): GoogleFont {
    val resources = context.resources
    val xml = resources.getXml(fontXml)
    val loaded =
        try {
            FontResourcesParserCompat.parse(xml, resources)
                as? FontResourcesParserCompat.ProviderResourceEntry
        } catch (cause: Exception) {
            val resName = resources.getResourceName(fontXml)
            throw IllegalArgumentException("Unable to load XML fontRes $resName", cause)
        }

    requireNotNull(loaded) { "Unable to load XML fontRes ${resources.getResourceName(fontXml)}" }

    val query = Uri.parse("?" + loaded.request.query)
    val name =
        query.getQueryParameter("name")
            ?: throw IllegalArgumentException(
                "No google font name provided in fontRes:" +
                    " ${resources.getResourceName(fontXml)}"
            )
    val bestEffort = query.getQueryParameter("besteffort") ?: "true"
    return GoogleFont(name, bestEffort == "true")
}
