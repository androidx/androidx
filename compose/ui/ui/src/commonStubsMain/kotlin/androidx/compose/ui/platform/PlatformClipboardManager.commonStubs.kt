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

package androidx.compose.ui.platform

import androidx.compose.ui.implementedInJetBrainsFork

public actual class ClipEntry {
    public actual val clipMetadata: ClipMetadata
        get() = implementedInJetBrainsFork()

    public actual suspend fun readText(): String? = implementedInJetBrainsFork()

    public actual suspend fun readPlainText(): String? = implementedInJetBrainsFork()

    public actual suspend fun readHtml(): String? = implementedInJetBrainsFork()

    public actual suspend fun readUrl(): String? = implementedInJetBrainsFork()

    public actual companion object {
        public actual fun withText(plainText: String, html: String?): ClipEntry =
            implementedInJetBrainsFork()

        public actual fun withUrl(
            url: String,
            plainText: String,
            html: String?,
        ): ClipEntry = implementedInJetBrainsFork()
    }
}

public actual class ClipMetadata {
    public actual fun hasText(): Boolean = implementedInJetBrainsFork()

    public actual fun hasPlainText(): Boolean = implementedInJetBrainsFork()

    public actual fun hasHtml(): Boolean = implementedInJetBrainsFork()

    public actual fun hasUrl(): Boolean = implementedInJetBrainsFork()
}

@Deprecated("Use direct reference to platform type instead of typealias")
public actual class NativeClipboard
