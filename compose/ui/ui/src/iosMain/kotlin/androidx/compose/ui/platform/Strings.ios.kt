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

package androidx.compose.ui.platform

import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.l10n.en
import androidx.compose.ui.platform.l10n.translationFor

@Immutable
internal value class Strings private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        val NextPage = Strings(0)
        val PreviousPage = Strings(1)
        val FirstPage = Strings(2)
        val LastPage = Strings(3)
        // When adding values here, make sure to also add them in ui/build.gradle,
        // updateTranslationsIos task (stringByResourceName parameter), and re-run the task
    }
}

private val cache = TranslationsCache(::translationFor)

internal fun getString(string: Strings): String = cache.getString(string)

/**
 * This object is only needed to provide a namespace for the translation map provider functions
 * (e.g. [Translations.en]), to avoid polluting the global namespace.
 */
internal object Translations
