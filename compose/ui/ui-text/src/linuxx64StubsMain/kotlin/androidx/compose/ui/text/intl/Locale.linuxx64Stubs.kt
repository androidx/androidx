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

package androidx.compose.ui.text.intl

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.implementedInJetBrainsFork

@Immutable
public actual class Locale {
    public actual companion object {
        public actual val current: Locale
            get() = implementedInJetBrainsFork()
    }

    public actual constructor(languageTag: String) {
        implementedInJetBrainsFork()
    }

    public actual val language: String
        get() = implementedInJetBrainsFork()

    public actual val script: String
        get() = implementedInJetBrainsFork()

    public actual val region: String
        get() = implementedInJetBrainsFork()

    public actual fun toLanguageTag(): String = implementedInJetBrainsFork()

    actual override operator fun equals(other: Any?): Boolean = implementedInJetBrainsFork()

    actual override fun hashCode(): Int = implementedInJetBrainsFork()

    actual override fun toString(): String = implementedInJetBrainsFork()
}
