/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

/** A utility class to hold various constants used by the Emoji Picker library.  */
internal object EmojiPickerConstants {

    // The default number of body columns.
    const val DEFAULT_BODY_COLUMNS = 9

    // The default number of rows of recent items held.
    const val DEFAULT_MAX_RECENT_ITEM_ROWS = 3

    // The max pool size of the Emoji ItemType in RecyclerViewPool.
    const val EMOJI_VIEW_POOL_SIZE = 100

    const val ADD_VIEW_EXCEPTION_MESSAGE = "Adding views to the EmojiPickerView is unsupported"

    const val REMOVE_VIEW_EXCEPTION_MESSAGE =
        "Removing views from the EmojiPickerView is unsupported"
}
