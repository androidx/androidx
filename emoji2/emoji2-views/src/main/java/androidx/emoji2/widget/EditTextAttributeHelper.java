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

package androidx.emoji2.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.emoji2.text.EmojiDefaults;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper class to parse EmojiCompat EditText attributes.
 *
 */
@RestrictTo(LIBRARY)
public class EditTextAttributeHelper {
    private int mMaxEmojiCount;

    public EditTextAttributeHelper(@NonNull View view, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        if (attrs != null) {
            final Context context = view.getContext();
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EmojiEditText,
                    defStyleAttr, defStyleRes);
            mMaxEmojiCount = a.getInteger(R.styleable.EmojiEditText_maxEmojiCount,
                    EmojiDefaults.MAX_EMOJI_COUNT);
            a.recycle();
        }
    }

    public int getMaxEmojiCount() {
        return mMaxEmojiCount;
    }
}
