/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.text.emoji.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.text.emoji.R;
import android.util.AttributeSet;
import android.view.View;

/**
 * Helper class to parse EmojiCompat EditText attributes.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class EditTextAttributeHelper {
    static final int MAX_EMOJI_COUNT = Integer.MAX_VALUE;
    private int mMaxEmojiCount;

    public EditTextAttributeHelper(@NonNull View view, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        if (attrs != null) {
            final Context context = view.getContext();
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EmojiEditText,
                    defStyleAttr, defStyleRes);
            mMaxEmojiCount = a.getInteger(R.styleable.EmojiEditText_maxEmojiCount, MAX_EMOJI_COUNT);
            a.recycle();
        }
    }

    public int getMaxEmojiCount() {
        return mMaxEmojiCount;
    }
}
