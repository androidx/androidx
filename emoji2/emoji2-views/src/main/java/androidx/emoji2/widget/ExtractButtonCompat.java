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

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.widget.Button;

import androidx.annotation.RestrictTo;
import androidx.core.widget.TextViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Support library implementation for ExtractButton. Used by {@link EmojiExtractViewHelper} while
 * inflating {@link EmojiExtractEditText} for keyboard use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExtractButtonCompat extends Button {
    public ExtractButtonCompat(@NonNull Context context) {
        super(context, null);
    }

    public ExtractButtonCompat(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtractButtonCompat(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Pretend like the window this view is in always has focus, so it will
     * highlight when selected.
     */
    @Override
    public boolean hasWindowFocus() {
        return isEnabled() && getVisibility() == VISIBLE;
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            ActionMode.@NonNull Callback actionModeCallback
    ) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }
}
