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

package androidx.compose.ui.platform

import android.content.ClipData
import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Returns the first non-null [Uri] from the list of [ClipData.Item]s in this [ClipEntry].
 *
 * Do not forget that each [ClipEntry] can contain multiple [ClipData.Item]s in its [ClipData],
 * therefore it can have multiple [Uri]s. Always check whether you are processing all the items in
 * a given [ClipEntry].
 */
@ExperimentalComposeUiApi
fun ClipEntry.firstUriOrNull(): Uri? {
    for (i in 0 until clipData.itemCount) {
        val uri = clipData.getItemAt(i).uri
        if (uri != null) return uri
    }
    return null
}
