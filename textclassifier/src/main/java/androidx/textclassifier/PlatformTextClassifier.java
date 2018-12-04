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
import android.os.LocaleList;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * An implementation of a platform TextClassifier, by wrapping an androidx textclassifier.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = Build.VERSION_CODES.O)
final class PlatformTextClassifier implements TextClassifier {
    private final androidx.textclassifier.TextClassifier mTextClassifier;
    private final Context mContext;

    @SuppressLint("RestrictedApi")
    PlatformTextClassifier(
            @NonNull Context context,
            @NonNull androidx.textclassifier.TextClassifier textClassifier) {
        mContext = Preconditions.checkNotNull(context.getApplicationContext());
        mTextClassifier = Preconditions.checkNotNull(textClassifier);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        androidx.textclassifier.TextClassification.Request androidxRequest =
                androidx.textclassifier.TextClassification.Request.fromPlatform(request);
        return (TextClassification)
                mTextClassifier.classifyText(androidxRequest).toPlatform(mContext);
    }

    @Override
    public TextClassification classifyText(CharSequence text,
                                           int startIndex,
                                           int endIndex,
                                           LocaleList defaultLocales) {
        androidx.textclassifier.TextClassification.Request androidxRequest =
                new androidx.textclassifier.TextClassification.Request.Builder(
                        text, startIndex, endIndex)
                        .setDefaultLocales(ConvertUtils.wrapLocalList(defaultLocales))
                        .build();
        return (TextClassification)
                mTextClassifier.classifyText(androidxRequest).toPlatform(mContext);
    }

    @Override
    public TextSelection suggestSelection(CharSequence text, int selectionStartIndex,
            int selectionEndIndex, LocaleList defaultLocales) {
        androidx.textclassifier.TextSelection.Request androidxRequest =
                new androidx.textclassifier.TextSelection.Request.Builder(
                        text, selectionStartIndex, selectionEndIndex)
                        .setDefaultLocales(ConvertUtils.wrapLocalList(defaultLocales))
                        .build();
        return (TextSelection) mTextClassifier.suggestSelection(androidxRequest).toPlatform();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.P)
    public TextSelection suggestSelection(TextSelection.Request request) {
        androidx.textclassifier.TextSelection.Request androidxRequest =
                androidx.textclassifier.TextSelection.Request.fromPlatfrom(request);
        return (TextSelection) mTextClassifier.suggestSelection(androidxRequest).toPlatform();
    }
}
