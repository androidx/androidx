/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * To workaround a bug in O, by backporting a fix in P.
 * <p>
 * When {@link TextView#getTextClassifier()} is called for the first time, the cache is set to be
 * the one returned by {@link TextClassificationManager#getTextClassifier()}. This is wrong because
 * later {@link TextClassificationManager#setTextClassifier(TextClassifier)} will then have no
 * effect to this TextView.
 */
final class AppCompatTextClassifierHelper {
    @NonNull
    private TextView mTextView;
    @Nullable
    private TextClassifier mTextClassifier;

    AppCompatTextClassifierHelper(@NonNull TextView textView) {
        mTextView = Preconditions.checkNotNull(textView);
    }

    /**
     * Sets the {@link TextClassifier} for this TextView.
     */
    @RequiresApi(api = 26)
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        mTextClassifier = textClassifier;
    }

    /**
     * Returns the {@link TextClassifier} used by this TextView.
     * If no TextClassifier has been set, this TextView uses the default set by the
     * {@link android.view.textclassifier.TextClassificationManager}.
     */
    @RequiresApi(api = 26)
    @NonNull
    public TextClassifier getTextClassifier() {
        if (mTextClassifier == null) {
            return Api26Impl.getTextClassifier(mTextView);
        }
        return mTextClassifier;
    }

    @RequiresApi(26)
    private static final class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        @NonNull
        static TextClassifier getTextClassifier(@NonNull TextView textView) {
            final TextClassificationManager tcm =
                    textView.getContext().getSystemService(TextClassificationManager.class);
            if (tcm != null) {
                return tcm.getTextClassifier();
            }
            return TextClassifier.NO_OP;
        }
    }
}
