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
    private TextClassifierFactory mTextClassifierFactory;

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
     * Returns a newly created text classifier.
     * <p>
     * If a factory is set through {@link #setTextClassifierFactory(TextClassifierFactory)},
     * an instance created by the factory will be returned. Otherwise, a default text classifier
     * will be returned.
     */
    @NonNull
    public TextClassifier createTextClassifier(
            @NonNull TextClassificationContext textClassificationContext) {
        Preconditions.checkNotNull(textClassificationContext);
        if (mTextClassifierFactory != null) {
            return mTextClassifierFactory.create(textClassificationContext);
        }
        return defaultTextClassifier(textClassificationContext);
    }

    /**
     * Sets a factory that can create a preferred text classifier.
     * <p>
     * To turn off the feature completely, you can set a factory that returns
     * {@link TextClassifier#NO_OP}.
     */
    public void setTextClassifierFactory(@Nullable TextClassifierFactory factory) {
        mTextClassifierFactory = factory;
    }

    /**
     * Returns the default text classifier.
     */
    private TextClassifier defaultTextClassifier(
            @Nullable TextClassificationContext textClassificationContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PlatformTextClassifierWrapper.create(mContext, textClassificationContext);
        }
        return LegacyTextClassifier.INSTANCE;
    }
}
