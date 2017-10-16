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
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Support library implementation for ExtractButton. Used by {@link EmojiExtractViewHelper} while
 * inflating {@link EmojiExtractEditText} for keyboard use.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ExtractButtonCompat extends Button {
    public ExtractButtonCompat(Context context) {
        super(context, null);
    }

    public ExtractButtonCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtractButtonCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ExtractButtonCompat(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Pretend like the window this view is in always has focus, so it will
     * highlight when selected.
     */
    @Override
    public boolean hasWindowFocus() {
        return isEnabled() && getVisibility() == VISIBLE ? true : false;
    }
}
