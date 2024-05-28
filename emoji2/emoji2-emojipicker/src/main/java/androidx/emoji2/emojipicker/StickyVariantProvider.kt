/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import android.content.Context
import android.content.Context.MODE_PRIVATE

/** A class that handles user's emoji variant selection using SharedPreferences. */
internal class StickyVariantProvider(context: Context) {
    companion object {
        const val PREFERENCES_FILE_NAME = "androidx.emoji2.emojipicker.preferences"
        const val STICKY_VARIANT_PROVIDER_KEY = "pref_key_sticky_variant"
        const val KEY_VALUE_DELIMITER = "="
        const val ENTRY_DELIMITER = "|"
    }

    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE)

    private val stickyVariantMap: MutableMap<String, String> by lazy {
        sharedPreferences
            .getString(STICKY_VARIANT_PROVIDER_KEY, null)
            ?.split(ENTRY_DELIMITER)
            ?.associate { entry ->
                entry
                    .split(KEY_VALUE_DELIMITER, limit = 2)
                    .takeIf { it.size == 2 }
                    ?.let { it[0] to it[1] } ?: ("" to "")
            }
            ?.toMutableMap() ?: mutableMapOf()
    }

    internal operator fun get(emoji: String): String = stickyVariantMap[emoji] ?: emoji

    internal fun update(baseEmoji: String, variantClicked: String) {
        stickyVariantMap.apply {
            if (baseEmoji == variantClicked) {
                this.remove(baseEmoji)
            } else {
                this[baseEmoji] = variantClicked
            }
            sharedPreferences
                .edit()
                .putString(STICKY_VARIANT_PROVIDER_KEY, entries.joinToString(ENTRY_DELIMITER))
                .commit()
        }
    }
}
