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

package androidx.compose.foundation.internal

import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

internal actual suspend fun ClipEntry.readText(): String? {
    implementedInJetBrainsFork()
}

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    implementedInJetBrainsFork()
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    implementedInJetBrainsFork()
}

internal actual fun ClipEntry?.hasText(): Boolean {
    implementedInJetBrainsFork()
}

internal actual fun Clipboard.isReadSupported(): Boolean {
    implementedInJetBrainsFork()
}

internal actual fun Clipboard.isWriteSupported(): Boolean {
    implementedInJetBrainsFork()
}
