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
 * ClipEntry can contain single or multiple [ClipData.Item]s. This function is useful when you are
 * only interested in processing just a single [Uri] item inside the [ClipEntry].
 *
 * It's advised that you consider checking all the items inside [ClipEntry.clipData] to thoroughly
 * process a given [ClipEntry].
 */
@ExperimentalComposeUiApi
fun ClipEntry.firstUriOrNull(): Uri? {
    for (i in 0 until clipData.itemCount) {
        val uri = clipData.getItemAt(i).uri
        if (uri != null) return uri
    }
    return null
}
