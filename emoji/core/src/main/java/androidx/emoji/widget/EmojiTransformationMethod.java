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
package androidx.emoji.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.emoji.text.EmojiCompat;

/**
 * TransformationMethod wrapper in order to update transformed text with emojis.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
class EmojiTransformationMethod implements TransformationMethod {
    private final TransformationMethod mTransformationMethod;

    EmojiTransformationMethod(TransformationMethod transformationMethod) {
        mTransformationMethod = transformationMethod;
    }

    @Override
    public CharSequence getTransformation(@Nullable CharSequence source, @NonNull final View view) {
        if (view.isInEditMode()) {
            return source;
        }

        if (mTransformationMethod != null) {
            source = mTransformationMethod.getTransformation(source, view);
        }

        if (source != null) {
            switch (EmojiCompat.get().getLoadState()){
                case EmojiCompat.LOAD_STATE_SUCCEEDED:
                    return EmojiCompat.get().process(source);
                case EmojiCompat.LOAD_STATE_LOADING:
                case EmojiCompat.LOAD_STATE_FAILED:
                case EmojiCompat.LOAD_STATE_DEFAULT:
                default:
                    break;
            }
        }
        return source;
    }

    @Override
    public void onFocusChanged(final View view, final CharSequence sourceText,
            final boolean focused, final int direction, final Rect previouslyFocusedRect) {
        if (mTransformationMethod != null) {
            mTransformationMethod.onFocusChanged(view, sourceText, focused, direction,
                    previouslyFocusedRect);
        }
    }
}
