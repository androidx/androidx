/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.text;

import androidx.annotation.RestrictTo;

/**
 * Defaults for emojicompat
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EmojiDefaults {

    private EmojiDefaults() {}

    /**
     * Default value for maxEmojiCount if not specified.
     */
    public static final int MAX_EMOJI_COUNT = Integer.MAX_VALUE;
}
