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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.util.WeakHashMap;

/**
 * Class to handle the creation of {@link TextClassifier}.
 */
public final class TextClassificationManager {
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static final WeakHashMap<Context, TextClassificationManager> sMapping =
            new WeakHashMap<>();

    private final Context mContext;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private TextClassifier mTextClassifier;
    private final TextClassifier mDefaultTextClassifier;

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    @SuppressLint("RestrictedApi")
    TextClassificationManager(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
        mDefaultTextClassifier = defaultTextClassifier(context);
    }

    /**
     * Returns an instance of {@link TextClassificationManager} for the specified context.
     * Each context has its own {@link TextClassificationManager}.
     */
    @SuppressLint("RestrictedApi")
    public static TextClassificationManager of(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        synchronized (sLock) {
            TextClassificationManager textClassificationManager = sMapping.get(context);
            if (textClassificationManager == null) {
                textClassificationManager = new TextClassificationManager(context);
                sMapping.put(context, textClassificationManager);
            }
            return textClassificationManager;
        }
    }

    /**
     * Returns the text classifier set through {@link #setTextClassifier(TextClassifier)},
     * a default text classifier is returned if it is not ever set, or a {@code null} is set.
     * <p>
     * If you are implementing a text classifier, and want to delegate requests to the default
     * text classifier provided by this library, you may want to use
     * {@link #getDefaultTextClassifier()} instead.
     *
     * @see #getDefaultTextClassifier()
     */
    @NonNull
    public TextClassifier getTextClassifier() {
        synchronized (mLock) {
            if (mTextClassifier != null) {
                return mTextClassifier;
            }
        }
        return mDefaultTextClassifier;
    }

    /**
     * Sets a preferred text classifier.
     * <p>
     * To turn off the feature completely, you can set a {@link TextClassifier#NO_OP}. If
     * {@code null} is set, default text classifier is used.
     * <p>
     * Note that the given text classifier is only set to this instance of the
     * {@link TextClassificationManager}.
     */
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        synchronized (mLock) {
            mTextClassifier = textClassifier;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPlatformTextClassifier(textClassifier);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setPlatformTextClassifier(@Nullable TextClassifier textClassifier) {
        android.view.textclassifier.TextClassificationManager textClassificationManager =
                (android.view.textclassifier.TextClassificationManager)
                        mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
        if (textClassificationManager == null) {
            return;
        }
        android.view.textclassifier.TextClassifier platformTextClassifier =
                textClassifier == null
                        ? null
                        : new PlatformTextClassifier(mContext, textClassifier);
        textClassificationManager.setTextClassifier(platformTextClassifier);
    }

    /**
     * Returns the default text classifier.
     */
    private static TextClassifier defaultTextClassifier(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PlatformTextClassifierWrapper.create(context);
        }
        return LegacyTextClassifier.of(context);
    }

    /**
     * Returns the default text classifier provided by this library.
     * <p>
     * This is mainly for text classifier implementation to delegate the request to the default
     * text classifier. Otherwise, in most cases, you shuold consider
     * {@link #getTextClassifier()} instead.
     * <p>
     * Note that the returned text classifier should be only used within the same context that is
     * passed to {@link TextClassificationManager#of(Context)}.
     *
     * @see #getTextClassifier()
     */
    @NonNull
    public TextClassifier getDefaultTextClassifier() {
        return mDefaultTextClassifier;
    }
}
