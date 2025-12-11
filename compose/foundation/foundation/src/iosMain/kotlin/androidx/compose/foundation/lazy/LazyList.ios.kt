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

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalPlatformScreenReader

/**
 * A minimum number of preloaded elements to allow the iOS accessibility engine to traverse the
 * lazy list elements without delay for semantic tree reloads after scrolling.
 */
private const val SCREEN_READER_BEYOND_BOUNDS_ITEM_COUNT = 3

@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun defaultLazyListBeyondBoundsItemCount(): Int {
    return if (LocalPlatformScreenReader.current.isActive) {
        SCREEN_READER_BEYOND_BOUNDS_ITEM_COUNT
    } else {
        0
    }
}
