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

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

/**
 * Returns a string if it's available in the ClipEntry. This method must not throw any Exceptions.
 * It can return null if the string can not be retrieved.
 */
internal expect suspend fun ClipEntry.readText(): String?

/**
 * Returns [AnnotatedString] if it's available in the ClipEntry. This method must not throw any
 * Exceptions. It can return null if the string can not be retrieved.
 */
internal expect suspend fun ClipEntry.readAnnotatedString(): AnnotatedString?

/** Creates a [ClipEntry] from the [AnnotatedString] */
internal expect fun AnnotatedString?.toClipEntry(): ClipEntry?

/** Returns true if [ClipEntry] has a valid text representation. Otherwise, it returns false. */
internal expect fun ClipEntry?.hasText(): Boolean

/**
 * All platforms except web always support both read and write operations. On the web, older browser
 * versions have different APIs supported. We use this information for the configuration of context
 * menu items.
 */
internal expect fun Clipboard.isReadSupported(): Boolean

internal expect fun Clipboard.isWriteSupported(): Boolean
