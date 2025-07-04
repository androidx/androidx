/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text

import android.R
import android.content.res.Resources
import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
internal actual value class ContextMenuIcons actual constructor(actual val value: Int) {
    actual companion object {
        actual val ActionModeCutDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeCutDrawable)

        actual val ActionModeCopyDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeCopyDrawable)

        actual val ActionModePasteDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModePasteDrawable)

        actual val ActionModeSelectAllDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeSelectAllDrawable)

        actual val ID_NULL: ContextMenuIcons
            get() = ContextMenuIcons(Resources.ID_NULL)
    }
}
