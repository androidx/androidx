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

package androidx.textclassifier;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

/**
 * Class to handle the creation of {@link TextClassifier}.
 */
public final class TextClassificationManager {
    private final Context mContext;
    @Nullable
    private static TextClassificationManager sInstance;
    private TextClassifier mTextClassifier;

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    TextClassificationManager(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    /**
     * Return an instance of {@link TextClassificationManager}.
     */
    public static TextClassificationManager of(@NonNull Context context) {
        if (sInstance == null) {
            Context appContext = context.getApplicationContext();
            sInstance = new TextClassificationManager(appContext);
        }
        return sInstance;
    }

    /**
     * Returns the text classifier set through {@link #setTextClassifier(TextClassifier)},
     * a default text classifier is returned if it is not ever set, or a {@code null} is set.
     */
    @NonNull
    public TextClassifier getTextClassifier() {
        if (mTextClassifier != null) {
            return mTextClassifier;
        }
        return defaultTextClassifier();
    }

    /**
     * Sets a preferred text classifier.
     * <p>
     * To turn off the feature completely, you can set a {@link TextClassifier#NO_OP}.
     */
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        mTextClassifier = textClassifier;
    }

    /**
     * Returns the default text classifier.
     */
    private TextClassifier defaultTextClassifier() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PlatformTextClassifierWrapper.create(mContext);
        }
        return LegacyTextClassifier.of(mContext);
    }
}
