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

package androidx.navigation

public actual abstract class NavUri {
    public actual abstract fun getFragment(): String?

    public actual abstract fun getQuery(): String?

    public actual abstract fun getPathSegments(): List<String>

    public actual open fun getQueryParameters(key: String): List<String> =
        implementedInJetBrainsFork()

    public actual open fun getQueryParameterNames(): Set<String> = implementedInJetBrainsFork()

    actual abstract override fun toString(): String
}

internal actual object NavUriUtils {
    actual fun encode(s: String, allow: String?): String {
        implementedInJetBrainsFork()
    }

    actual fun decode(s: String): String {
        implementedInJetBrainsFork()
    }

    actual fun parse(uriString: String): NavUri {
        implementedInJetBrainsFork()
    }
}
