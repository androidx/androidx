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

/**
 * The emoji picker view that provides up-to-date emojis in a vertical scrollable view with a
 * clickable horizontal header.
 */
// TODO(scduan): API surface and implementation
internal class EmojiPickerView {

    /**
     * MetaVersion will be null if EmojiCompat is not enabled.
     */
    internal data class EmojiCompatMetadata(val metaVersion: Int?, val replaceAll: Boolean)
}